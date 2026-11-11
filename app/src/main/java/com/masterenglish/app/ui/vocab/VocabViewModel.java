package com.masterenglish.app.ui.vocab;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.masterenglish.app.data.local.Enums.QuestionType;
import com.masterenglish.app.data.local.entity.LessonQuestionEntity;
import com.masterenglish.app.data.local.entity.UserItemRetentionEntity;
import com.masterenglish.app.data.local.entity.VocabularyEntity;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.repository.ContentRepository;
import com.masterenglish.app.data.repository.ProgressRepository;
import com.masterenglish.app.ui.lesson.QuestionModels;
import com.masterenglish.app.utils.Callback;
import com.masterenglish.app.utils.TextUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Vocabulary screens. Topics are the CEFR levels of the seeded words; learned and mastered come
 * from the learner's SM-2 cards (mastered once a card has been repeated enough times).
 */
@HiltViewModel
public class VocabViewModel extends ViewModel {

    /** Words per practice round. */
    public static final int PRACTICE_SIZE = 10;

    /** Words still to review on the completion screen. */
    public static final int REVIEW_LIMIT = 5;

    public enum Status { NEW, LEARNING, MASTERED }

    /** A word as shown in a list. */
    public static final class Word {
        public final String id;
        public final String word;
        public final String meaning;
        public final Status status;

        Word(String id, String word, String meaning, Status status) {
            this.id = id;
            this.word = word;
            this.meaning = meaning;
            this.status = status;
        }
    }

    /** One CEFR level of words. */
    public static final class Topic {
        public final String level;
        public final String description;
        public final int total;
        public final int learned;
        public final int mastered;
        public final List<Word> words;

        Topic(String level, String description, int total, int learned, int mastered, List<Word> words) {
            this.level = level;
            this.description = description;
            this.total = total;
            this.learned = learned;
            this.mastered = mastered;
            this.words = words;
        }
    }

    /** Totals across every level, plus each level as a topic. */
    public static final class Overview {
        public final int total;
        public final int learned;
        public final int mastered;
        public final List<Topic> topics;

        Overview(int total, int learned, int mastered, List<Topic> topics) {
            this.total = total;
            this.learned = learned;
            this.mastered = mastered;
            this.topics = topics;
        }

        @Nullable
        public Topic topic(String level) {
            for (Topic topic : topics) {
                if (topic.level.equals(level)) {
                    return topic;
                }
            }
            return null;
        }
    }

    /** One multiple-choice question about a word. */
    public static final class Challenge {
        public final LessonQuestionEntity question;
        public final String wordId;
        public final String prompt;
        public final String context;
        public final List<String> options;
        public final int correct;
        public final String explanation;

        Challenge(LessonQuestionEntity question, String wordId, String prompt, String context,
                  List<String> options, int correct, String explanation) {
            this.question = question;
            this.wordId = wordId;
            this.prompt = prompt;
            this.context = context;
            this.options = options;
            this.correct = correct;
            this.explanation = explanation;
        }
    }

    /** What the completion screen shows after a round. */
    public static final class Summary {
        public final List<Word> stillToReview;
        public final int masteredCount;

        Summary(List<Word> stillToReview, int masteredCount) {
            this.stillToReview = stillToReview;
            this.masteredCount = masteredCount;
        }
    }

    private static final class Snapshot {
        final List<VocabularyEntity> words;
        final Map<String, UserItemRetentionEntity> rows;

        Snapshot(List<VocabularyEntity> words, Map<String, UserItemRetentionEntity> rows) {
            this.words = words;
            this.rows = rows;
        }
    }

    private final String userId;
    private final ContentRepository contentRepository;
    private final ProgressRepository progressRepository;
    private final MutableLiveData<Overview> overview = new MutableLiveData<>();
    private final MutableLiveData<List<Challenge>> challenges = new MutableLiveData<>();
    private final MutableLiveData<Summary> summary = new MutableLiveData<>();

    @Inject
    public VocabViewModel(AppPreferences preferences, ContentRepository contentRepository,
                          ProgressRepository progressRepository) {
        this.userId = preferences.getCurrentUserId() == null ? "" : preferences.getCurrentUserId();
        this.contentRepository = contentRepository;
        this.progressRepository = progressRepository;
    }

    public LiveData<Overview> overview() {
        return overview;
    }

    public LiveData<List<Challenge>> challenges() {
        return challenges;
    }

    public LiveData<Summary> summary() {
        return summary;
    }

    /** Loads totals and topics. */
    public void loadOverview() {
        loadSnapshot(snapshot -> overview.setValue(buildOverview(snapshot)));
    }

    /**
     * Builds the questions for a practice round: the words of one level (or of every level when
     * {@code topicId} is "all") that are not mastered yet.
     */
    public void startPractice(String topicId) {
        loadSnapshot(snapshot -> {
            List<VocabularyEntity> pool = new ArrayList<>();
            for (VocabularyEntity word : snapshot.words) {
                boolean inTopic = "all".equals(topicId) || topicId.equals(word.cefrLevel);
                if (inTopic && statusOf(snapshot.rows.get(word.id)) != Status.MASTERED) {
                    pool.add(word);
                }
            }
            pool = pool.subList(0, Math.min(PRACTICE_SIZE, pool.size()));
            Map<String, VocabularyEntity> byId = new HashMap<>();
            List<String> ids = new ArrayList<>();
            for (VocabularyEntity word : pool) {
                byId.put(word.id, word);
                ids.add(word.id);
            }
            contentRepository.loadVocabularyQuestions(ids, questions ->
                    challenges.setValue(toChallenges(questions, byId)));
        });
    }

    /** Records one answer; wrong answers also count against the word's weakness and SM-2 card. */
    public void recordAnswer(Challenge challenge, boolean correct) {
        progressRepository.recordAnswer(userId, challenge.question, correct);
    }

    /**
     * Loads the completion screen: words answered wrongly, and how many words from the round
     * are now mastered. Call after the answers have been recorded.
     */
    public void loadSummary(List<String> roundWordIds, List<String> wrongWordIds) {
        loadSnapshot(snapshot -> {
            Map<String, VocabularyEntity> byId = new HashMap<>();
            for (VocabularyEntity word : snapshot.words) {
                byId.put(word.id, word);
            }
            List<Word> review = new ArrayList<>();
            for (String id : wrongWordIds) {
                VocabularyEntity word = byId.get(id);
                if (word != null && review.size() < REVIEW_LIMIT) {
                    review.add(toWord(word, snapshot.rows.get(id)));
                }
            }
            int mastered = 0;
            for (String id : roundWordIds) {
                if (statusOf(snapshot.rows.get(id)) == Status.MASTERED) {
                    mastered++;
                }
            }
            summary.setValue(new Summary(review, mastered));
        });
    }

    private void loadSnapshot(Callback<Snapshot> callback) {
        contentRepository.getAllVocabulary(words ->
                progressRepository.loadVocabularyRetention(userId, rows -> callback.onResult(new Snapshot(words, rows))));
    }

    private static Overview buildOverview(Snapshot snapshot) {
        Map<String, List<VocabularyEntity>> byLevel = new TreeMap<>();
        for (VocabularyEntity word : snapshot.words) {
            byLevel.computeIfAbsent(word.cefrLevel, level -> new ArrayList<>()).add(word);
        }
        List<Topic> topics = new ArrayList<>();
        int total = 0;
        int learned = 0;
        int mastered = 0;
        for (Map.Entry<String, List<VocabularyEntity>> entry : byLevel.entrySet()) {
            List<Word> words = new ArrayList<>();
            int levelLearned = 0;
            int levelMastered = 0;
            for (VocabularyEntity word : entry.getValue()) {
                UserItemRetentionEntity row = snapshot.rows.get(word.id);
                Status status = statusOf(row);
                if (row != null) {
                    levelLearned++;
                }
                if (status == Status.MASTERED) {
                    levelMastered++;
                }
                words.add(toWord(word, row));
            }
            words.sort(Comparator.comparing((Word w) -> w.word.toLowerCase()));
            int size = words.size();
            topics.add(new Topic(entry.getKey(), mostCommonDomain(entry.getValue()), size, levelLearned,
                    levelMastered, words));
            total += size;
            learned += levelLearned;
            mastered += levelMastered;
        }
        return new Overview(total, learned, mastered, topics);
    }

    private static List<Challenge> toChallenges(List<LessonQuestionEntity> questions,
                                                Map<String, VocabularyEntity> words) {
        List<Challenge> out = new ArrayList<>();
        for (LessonQuestionEntity question : questions) {
            VocabularyEntity word = words.get(question.parentId);
            if (question.questionType != QuestionType.MULTIPLE_CHOICE || word == null) {
                continue;
            }
            List<String> options = QuestionModels.stringOptions(question);
            String answer = TextUtils.normalizeAnswer(String.valueOf(QuestionModels.correctAnswer(question)));
            int correct = -1;
            for (int i = 0; i < options.size(); i++) {
                if (TextUtils.normalizeAnswer(options.get(i)).equals(answer)) {
                    correct = i;
                }
            }
            if (options.size() < 2 || correct < 0) {
                continue;
            }
            String context = word.word + (word.phonetic == null ? "" : " " + word.phonetic);
            out.add(new Challenge(question, word.id, question.prompt, context, options, correct,
                    question.explanation == null ? word.meaningVi : question.explanation));
        }
        return out;
    }

    private static Word toWord(VocabularyEntity word, @Nullable UserItemRetentionEntity row) {
        return new Word(word.id, word.word, word.meaningVi, statusOf(row));
    }

    static Status statusOf(@Nullable UserItemRetentionEntity row) {
        if (row == null) {
            return Status.NEW;
        }
        return row.repetitionNumber >= ProgressRepository.MASTERED_REPETITIONS ? Status.MASTERED : Status.LEARNING;
    }

    private static String mostCommonDomain(List<VocabularyEntity> words) {
        Map<String, Integer> counts = new HashMap<>();
        String best = "";
        int bestCount = 0;
        for (VocabularyEntity word : words) {
            if (word.domain == null) {
                continue;
            }
            int count = counts.merge(word.domain, 1, Integer::sum);
            if (count > bestCount) {
                best = word.domain;
                bestCount = count;
            }
        }
        return best;
    }
}
