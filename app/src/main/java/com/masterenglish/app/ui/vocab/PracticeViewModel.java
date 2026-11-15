package com.masterenglish.app.ui.vocab;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.masterenglish.app.data.local.Enums.ItemType;
import com.masterenglish.app.data.local.entity.UserItemRetentionEntity;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.repository.ContentRepository;
import com.masterenglish.app.data.repository.ProgressRepository;
import com.masterenglish.app.data.repository.UserRepository;
import com.masterenglish.app.utils.AppExecutors;
import com.masterenglish.app.utils.SrsAlgorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * One practice round: up to {@link #ROUND_SIZE} words, each taken through six stages (stage by stage, like the design).
 * Shared by the practice and summary screens through the activity.
 */
@HiltViewModel
public class PracticeViewModel extends ViewModel {

    public static final int ROUND_SIZE = 5;

    public enum Stage {
        FLASHCARD("Flashcard"), LISTENING("Listening"), SPEAKING("Speaking"),
        READING("Reading"), FILL_BLANK("Fill in the blank"), CHOICE("Single choice");

        public final String label;

        Stage(String label) {
            this.label = label;
        }

        /** Whether the stage is marked right or wrong (the flashcard only shows the word). */
        public boolean graded() {
            return this != FLASHCARD;
        }
    }

    /** What the summary screen shows. */
    public static final class Result {
        public final String title;
        public final List<VocabViewModel.Word> correct;
        public final List<VocabViewModel.Word> review;
        public final int minutes;

        Result(String title, List<VocabViewModel.Word> correct, List<VocabViewModel.Word> review, int minutes) {
            this.title = title;
            this.correct = correct;
            this.review = review;
            this.minutes = minutes;
        }

        public int total() {
            return correct.size() + review.size();
        }
    }

    private static final String STATS = "practice_stats";

    private final String userId;
    private final Context context;
    private final ContentRepository contentRepository;
    private final ProgressRepository progressRepository;
    private final UserRepository userRepository;
    private final AppExecutors executors;

    private final MutableLiveData<List<VocabViewModel.Word>> words = new MutableLiveData<>();
    private final MutableLiveData<Result> result = new MutableLiveData<>();
    private List<VocabViewModel.Word> pool = new ArrayList<>();
    private final Set<String> wrongIds = new HashSet<>();
    private String title = "";
    private long startedAt;
    private String startedFor = "";

    @Inject
    public PracticeViewModel(@ApplicationContext Context context, AppPreferences preferences,
                             ContentRepository contentRepository, ProgressRepository progressRepository,
                             UserRepository userRepository, AppExecutors executors) {
        this.context = context;
        this.userId = preferences.getCurrentUserId() == null ? "" : preferences.getCurrentUserId();
        this.contentRepository = contentRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
        this.executors = executors;
    }

    public LiveData<List<VocabViewModel.Word>> words() {
        return words;
    }

    public LiveData<Result> result() {
        return result;
    }

    public List<VocabViewModel.Word> pool() {
        return pool;
    }

    /**
     * Starts a round for one pack, or for the words due for review when {@code packId} is null.
     * Words not yet mastered come first; a round of the same target is not rebuilt while it is running.
     */
    public void start(@Nullable String packId) {
        String key = packId == null ? "\u0000review" : packId;
        if (key.equals(startedFor) && words.getValue() != null && result.getValue() == null) {
            return;
        }
        startedFor = key;
        wrongIds.clear();
        result.setValue(null);
        words.setValue(null);
        startedAt = System.currentTimeMillis();
        contentRepository.getAllVocabulary(entities ->
                progressRepository.loadVocabularyRetention(userId, rows -> {
                    VocabViewModel.Library library = VocabViewModel.build(entities, rows);
                    pool = library.words;
                    List<VocabViewModel.Word> picked = new ArrayList<>();
                    if (packId == null) {
                        title = "Review";
                        for (VocabViewModel.Word w : library.words) {
                            if (w.due && picked.size() < ROUND_SIZE) {
                                picked.add(w);
                            }
                        }
                    } else {
                        VocabViewModel.Pack pack = library.pack(packId);
                        title = pack == null ? "Practice" : pack.title;
                        if (pack != null) {
                            // Unlearned words first, then learning, then mastered.
                            List<VocabViewModel.Word> ordered = new ArrayList<>(pack.words);
                            ordered.sort((a, b) -> a.status.compareTo(b.status));
                            picked.addAll(ordered.subList(0, Math.min(ROUND_SIZE, ordered.size())));
                        }
                    }
                    if (picked.size() < ROUND_SIZE) {
                        for (VocabViewModel.Word w : library.words) {
                            if (picked.size() >= ROUND_SIZE) {
                                break;
                            }
                            if (!picked.contains(w) && w.status != VocabViewModel.Status.MASTERED) {
                                picked.add(w);
                            }
                        }
                    }
                    words.setValue(picked);
                }));
    }

    public String title() {
        return title;
    }

    /** Records one graded answer. */
    public void record(Stage stage, String wordId, boolean correct) {
        if (!stage.graded()) {
            return;
        }
        if (!correct) {
            wrongIds.add(wordId);
        }
        SharedPreferences p = context.getSharedPreferences(STATS, Context.MODE_PRIVATE);
        p.edit().putInt(stage.name() + "_total", p.getInt(stage.name() + "_total", 0) + 1)
                .putInt(stage.name() + "_right", p.getInt(stage.name() + "_right", 0) + (correct ? 1 : 0))
                .apply();
    }

    /** Accuracy 0..100 per stage over all rounds, or -1 when the stage was never graded. */
    public static int accuracy(Context context, Stage stage) {
        SharedPreferences p = context.getSharedPreferences(STATS, Context.MODE_PRIVATE);
        int total = p.getInt(stage.name() + "_total", 0);
        return total == 0 ? -1 : Math.round(p.getInt(stage.name() + "_right", 0) * 100f / total);
    }

    /** Schedules every word on its SM-2 card, adds XP and streak, and publishes the result. */
    public void finish() {
        List<VocabViewModel.Word> round = words.getValue();
        if (round == null || result.getValue() != null) {
            return;
        }
        List<VocabViewModel.Word> right = new ArrayList<>();
        List<VocabViewModel.Word> wrong = new ArrayList<>();
        for (VocabViewModel.Word w : round) {
            (wrongIds.contains(w.id) ? wrong : right).add(w);
        }
        int minutes = Math.max(1, Math.round((System.currentTimeMillis() - startedAt) / 60000f));
        for (VocabViewModel.Word w : round) {
            progressRepository.reviewCard(userId, ItemType.VOCABULARY, w.id,
                    wrongIds.contains(w.id) ? SrsAlgorithm.Button.AGAIN : SrsAlgorithm.Button.GOOD, null);
        }
        int xp = right.size() * 10 + 5 * round.size();
        executors.diskIO().execute(() -> {
            userRepository.touchActivityNow(userId, minutes);
            userRepository.addRewardsNow(userId, xp, 0);
        });
        result.setValue(new Result(title, right, wrong, minutes));
    }

    public void clear() {
        words.setValue(null);
        result.setValue(null);
        startedFor = "";
        wrongIds.clear();
    }

    // ----- Question material -------------------------------------------------

    /** The example sentence split around the word: [before, matched, after], or null when it does not contain it. */
    @Nullable
    public static String[] splitExample(VocabViewModel.Word w) {
        if (w.example == null) {
            return null;
        }
        int i = w.example.toLowerCase().indexOf(w.term.toLowerCase());
        if (i < 0) {
            return null;
        }
        return new String[]{w.example.substring(0, i), w.example.substring(i, i + w.term.length()),
                w.example.substring(i + w.term.length())};
    }

    /** {@code count} meanings: the right one plus distractors from other words, in a stable shuffled order. */
    public List<String> meanings(VocabViewModel.Word word, int count) {
        List<String> options = new ArrayList<>();
        options.add(word.meaning);
        List<VocabViewModel.Word> others = new ArrayList<>(pool);
        Collections.shuffle(others, new Random(word.id.hashCode()));
        // Prefer words from the same pack so the distractors are plausible.
        others.sort((a, b) -> Boolean.compare(!b.packId.equals(word.packId), !a.packId.equals(word.packId)));
        for (VocabViewModel.Word o : others) {
            if (options.size() >= count) {
                break;
            }
            if (!o.id.equals(word.id) && !options.contains(o.meaning)) {
                options.add(o.meaning);
            }
        }
        Collections.shuffle(options, new Random(word.id.hashCode() * 31L + count));
        return options;
    }
}
