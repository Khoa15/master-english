package com.masterenglish.app.ui.vocab;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.masterenglish.app.data.local.entity.UserEntity;
import com.masterenglish.app.data.local.entity.PackEntity;
import com.masterenglish.app.data.local.entity.PackWordEntity;
import com.masterenglish.app.data.local.entity.WordProgressEntity;
import com.masterenglish.app.data.local.entity.WordRow;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.repository.WordRepository;
import com.masterenglish.app.data.repository.ProgressRepository;
import com.masterenglish.app.data.repository.UserRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** The learner's vocabulary: every word with its SM-2 status, grouped into packs by domain. */
@HiltViewModel
public class VocabViewModel extends ViewModel {

    public enum Status { NEW, LEARNING, MASTERED }

    /** A word as shown in lists and practised in a round. */
    public static final class Word {
        public final String id;
        public final String term;
        @Nullable public final String phonetic;
        @Nullable public final String partOfSpeech;
        public final String meaning;
        @Nullable public final String example;
        @Nullable public final String exampleTranslation;
        /** The meaning in the learner's language, or null when the word has none in it. */
        @Nullable public final String translation;
        public final String cefrLevel;
        public final Status status;
        /** Learned and due for a review now. */
        public final boolean due;
        /** 0..100, how close the card is to mastered. */
        public final int mastery;
        public final long lastReviewedAt;

        Word(WordRow e, Status status, boolean due, int mastery, long lastReviewedAt) {
            this.id = e.id;
            this.term = e.term;
            this.phonetic = e.ipa;
            this.partOfSpeech = e.pos;
            this.meaning = e.definition == null || e.definition.isEmpty() ? "No definition available" : e.definition;
            this.example = e.example;
            this.exampleTranslation = e.exampleTranslation;
            this.translation = e.translation;
            this.cefrLevel = e.cefrLevel;
            this.status = status;
            this.due = due;
            this.mastery = mastery;
            this.lastReviewedAt = lastReviewedAt;
        }

        public boolean learned() {
            return status != Status.NEW;
        }

        /** "n." style short part of speech, e.g. "n.", "adj.". */
        public String shortPos() {
            if (partOfSpeech == null || partOfSpeech.isEmpty()) {
                return "";
            }
            String p = partOfSpeech.toLowerCase();
            switch (p) {
                case "noun": return "n.";
                case "verb": return "v.";
                case "adjective": return "adj.";
                case "adverb": return "adv.";
                default: return p.length() > 4 ? p.substring(0, 3) + "." : p;
            }
        }

        /** "meaning · n." as the lists show it. */
        public String subtitle() {
            String pos = shortPos();
            return pos.isEmpty() ? meaning : meaning + " · " + pos;
        }
    }

    /** A pack: a named set of words (today, every word of one CEFR level). */
    public static final class Pack {
        public final String id;
        public final String title;
        public final int tone;
        public final List<Word> words;
        public final int learned;

        Pack(PackEntity entity, List<Word> words) {
            this.id = entity.id;
            this.title = entity.title;
            this.tone = entity.position % 3;
            this.words = words;
            int n = 0;
            for (Word w : words) {
                if (w.learned()) {
                    n++;
                }
            }
            this.learned = n;
        }

        public int total() {
            return words.size();
        }

        public int percent() {
            return words.isEmpty() ? 0 : learned * 100 / words.size();
        }
    }

    /** Everything the vocabulary screens show. */
    public static final class Library {
        public final List<Word> words;
        public final List<Pack> packs;
        public final int dueCount;
        public final int learnedCount;
        public final int wordsToday;

        Library(List<Word> words, List<Pack> packs, int dueCount, int wordsToday) {
            this.words = words;
            this.packs = packs;
            this.dueCount = dueCount;
            this.wordsToday = wordsToday;
            int learned = 0;
            for (Word w : words) {
                if (w.learned()) {
                    learned++;
                }
            }
            this.learnedCount = learned;
        }

        @Nullable
        public Pack pack(String id) {
            for (Pack p : packs) {
                if (p.id.equals(id)) {
                    return p;
                }
            }
            return null;
        }
    }

    private final String userId;
    private final WordRepository wordRepository;
    private final ProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final MutableLiveData<Library> library = new MutableLiveData<>();

    @Inject
    public VocabViewModel(AppPreferences preferences, WordRepository wordRepository,
                          ProgressRepository progressRepository, UserRepository userRepository) {
        this.userId = preferences.getCurrentUserId() == null ? "" : preferences.getCurrentUserId();
        this.wordRepository = wordRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
    }

    public LiveData<Library> library() {
        return library;
    }

    public LiveData<UserEntity> user() {
        return userRepository.observeUser(userId);
    }

    /** Reloads words, packs and SM-2 cards. */
    public void load() {
        wordRepository.loadContent(content ->
                progressRepository.loadProgress(userId, rows -> library.setValue(build(content, rows))));
    }

    static Library build(WordRepository.Content content, Map<String, WordProgressEntity> rows) {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();

        List<Word> words = new ArrayList<>(content.words.size());
        Map<String, Word> byId = new HashMap<>();
        int due = 0;
        int today = 0;
        for (WordRow e : content.words) {
            WordProgressEntity row = rows.get(e.id);
            Status status = row == null ? Status.NEW
                    : row.repetitions >= ProgressRepository.MASTERED_REPETITIONS ? Status.MASTERED : Status.LEARNING;
            boolean isDue = row != null && status != Status.MASTERED && row.nextReviewAt <= now;
            int mastery = row == null ? 0
                    : Math.min(100, 20 + row.repetitions * 80 / ProgressRepository.MASTERED_REPETITIONS);
            if (status == Status.MASTERED) {
                mastery = 100;
            }
            Word w = new Word(e, status, isDue, mastery, row == null ? 0 : row.lastReviewedAt);
            words.add(w);
            byId.put(w.id, w);
            if (isDue) {
                due++;
            }
            if (row != null && row.lastReviewedAt >= startOfDay) {
                today++;
            }
        }
        Map<String, List<Word>> byPack = new LinkedHashMap<>();
        for (PackEntity p : content.packs) {
            byPack.put(p.id, new ArrayList<>());
        }
        for (PackWordEntity link : content.links) {
            Word w = byId.get(link.wordId);
            List<Word> list = byPack.get(link.packId);
            if (w != null && list != null) {
                list.add(w);
            }
        }
        List<Pack> packs = new ArrayList<>();
        for (PackEntity p : content.packs) {
            packs.add(new Pack(p, byPack.get(p.id)));
        }
        return new Library(words, packs, due, today);
    }
}
