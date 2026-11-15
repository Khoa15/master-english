package com.masterenglish.app.ui.vocab;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.masterenglish.app.data.local.entity.UserEntity;
import com.masterenglish.app.data.local.entity.UserItemRetentionEntity;
import com.masterenglish.app.data.local.entity.VocabularyEntity;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.repository.ContentRepository;
import com.masterenglish.app.data.repository.ProgressRepository;
import com.masterenglish.app.data.repository.UserRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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
        public final String packId;
        public final Status status;
        /** Learned and due for a review now. */
        public final boolean due;
        /** 0..100, how close the card is to mastered. */
        public final int mastery;
        public final long lastReviewedAt;

        Word(VocabularyEntity e, String packId, Status status, boolean due, int mastery, long lastReviewedAt) {
            this.id = e.id;
            this.term = e.word;
            this.phonetic = e.phonetic;
            this.partOfSpeech = e.partOfSpeech;
            this.meaning = e.meaningVi;
            this.example = e.exampleSentence;
            this.exampleTranslation = e.exampleTranslation;
            this.packId = packId;
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

    /** A pack: the words that share a domain. */
    public static final class Pack {
        public final String id;
        public final String title;
        public final String category;
        public final int tone;
        public final List<Word> words;
        public final int learned;

        Pack(String id, List<Word> words) {
            this.id = id;
            int slash = id.indexOf(" / ");
            String sub = slash > 0 ? id.substring(slash + 3) : id;
            this.title = sub.equals("Other") ? "More " + id.substring(0, slash) : sub;
            this.category = slash > 0 ? id.substring(0, slash) : "General";
            this.tone = Math.abs(id.hashCode()) % 3;
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
        public final List<String> categories;
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
            TreeSet<String> cats = new TreeSet<>();
            for (Pack p : packs) {
                cats.add(p.category);
            }
            this.categories = new ArrayList<>(cats);
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
    private final ContentRepository contentRepository;
    private final ProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final MutableLiveData<Library> library = new MutableLiveData<>();

    @Inject
    public VocabViewModel(AppPreferences preferences, ContentRepository contentRepository,
                          ProgressRepository progressRepository, UserRepository userRepository) {
        this.userId = preferences.getCurrentUserId() == null ? "" : preferences.getCurrentUserId();
        this.contentRepository = contentRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
    }

    public LiveData<Library> library() {
        return library;
    }

    public LiveData<UserEntity> user() {
        return userRepository.observeUser(userId);
    }

    /** Reloads words and SM-2 rows. */
    public void load() {
        contentRepository.getAllVocabulary(entities ->
                progressRepository.loadVocabularyRetention(userId, rows -> library.setValue(build(entities, rows))));
    }

    private static final int MIN_PACK_WORDS = 6;

    static String categoryOf(String domain) {
        int slash = domain.indexOf(" / ");
        return slash > 0 ? domain.substring(0, slash) : domain;
    }

    static String packIdOf(VocabularyEntity e) {
        return e.domain == null || e.domain.isEmpty() ? "General English" : e.domain;
    }

    static Library build(List<VocabularyEntity> entities, Map<String, UserItemRetentionEntity> rows) {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfDay = cal.getTimeInMillis();

        // Domains with only a few words are folded into "<category> / Other" so packs stay worth opening.
        Map<String, Integer> domainSize = new java.util.HashMap<>();
        for (VocabularyEntity e : entities) {
            domainSize.merge(packIdOf(e), 1, Integer::sum);
        }
        List<Word> words = new ArrayList<>();
        Map<String, List<Word>> byPack = new LinkedHashMap<>();
        int due = 0;
        int today = 0;
        for (VocabularyEntity e : entities) {
            UserItemRetentionEntity row = rows.get(e.id);
            Status status = row == null ? Status.NEW
                    : row.repetitionNumber >= ProgressRepository.MASTERED_REPETITIONS ? Status.MASTERED : Status.LEARNING;
            boolean isDue = row != null && status != Status.MASTERED && row.nextReviewAt <= now;
            int mastery = row == null ? 0
                    : Math.min(100, 20 + row.repetitionNumber * 80 / ProgressRepository.MASTERED_REPETITIONS);
            if (status == Status.MASTERED) {
                mastery = 100;
            }
            String domain = packIdOf(e);
            String packId = domainSize.get(domain) >= MIN_PACK_WORDS ? domain : categoryOf(domain) + " / Other";
            Word w = new Word(e, packId, status, isDue, mastery, row == null ? 0 : row.lastReviewedAt);
            words.add(w);
            byPack.computeIfAbsent(w.packId, k -> new ArrayList<>()).add(w);
            if (isDue) {
                due++;
            }
            if (row != null && row.lastReviewedAt >= startOfDay) {
                today++;
            }
        }
        words.sort(Comparator.comparing((Word w) -> w.term.toLowerCase()));
        List<Pack> packs = new ArrayList<>();
        for (Map.Entry<String, List<Word>> entry : byPack.entrySet()) {
            List<Word> list = entry.getValue();
            list.sort(Comparator.comparing((Word w) -> w.term.toLowerCase()));
            packs.add(new Pack(entry.getKey(), list));
        }
        packs.sort(Comparator.comparing((Pack p) -> p.title.toLowerCase()));
        return new Library(words, packs, due, today);
    }
}
