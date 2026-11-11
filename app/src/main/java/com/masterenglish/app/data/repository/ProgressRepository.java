package com.masterenglish.app.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.masterenglish.app.data.local.Enums.ItemType;
import com.masterenglish.app.data.local.Enums.NodeStatus;
import com.masterenglish.app.data.local.Enums.NodeType;
import com.masterenglish.app.data.local.Enums.SkillType;
import com.masterenglish.app.data.local.dao.ContentDao;
import com.masterenglish.app.data.local.dao.ProgressDao;
import com.masterenglish.app.data.local.entity.LessonQuestionEntity;
import com.masterenglish.app.data.local.entity.RoadmapNodeEntity;
import com.masterenglish.app.data.local.entity.UserItemRetentionEntity;
import com.masterenglish.app.data.local.entity.UserNodeProgressEntity;
import com.masterenglish.app.data.local.entity.UserWeaknessEntity;
import com.masterenglish.app.data.local.entity.VocabularyEntity;
import com.masterenglish.app.data.local.entity.GrammarEntity;
import com.masterenglish.app.service.ai.WeaknessDetector;
import com.masterenglish.app.service.gamification.BadgeEvaluator;
import com.masterenglish.app.utils.AppExecutors;
import com.masterenglish.app.utils.Callback;
import com.masterenglish.app.utils.SrsAlgorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.masterenglish.app.data.local.dao.UserDao;

/**
 * Roadmap progress, rewards, the weakness tracker and SM-2 review scheduling.
 * Every write is applied in a single transaction on the disk executor.
 */
@Singleton
public class ProgressRepository {

    /** Mastery at or above this value counts as fixed and leaves the Weakness Clinic. */
    public static final float WEAK_THRESHOLD = 0.6f;
    private static final int WEAKNESS_LIMIT = 10;
    private static final long ONE_DAY_MILLIS = 24L * 60 * 60 * 1000;
    /** Repetitions in a row before a vocabulary card counts as mastered. */
    public static final int MASTERED_REPETITIONS = 3;

    private final ProgressDao progressDao;
    private final ContentDao contentDao;
    private final UserDao userDao;
    private final UserRepository userRepository;
    private final WeaknessDetector weaknessDetector;
    private final BadgeEvaluator badgeEvaluator;
    private final AppExecutors executors;

    @Inject
    public ProgressRepository(ProgressDao progressDao, ContentDao contentDao, UserDao userDao,
                              UserRepository userRepository, WeaknessDetector weaknessDetector,
                              BadgeEvaluator badgeEvaluator, AppExecutors executors) {
        this.progressDao = progressDao;
        this.contentDao = contentDao;
        this.userDao = userDao;
        this.userRepository = userRepository;
        this.weaknessDetector = weaknessDetector;
        this.badgeEvaluator = badgeEvaluator;
        this.executors = executors;
    }

    // ----- Roadmap -------------------------------------------------------

    /** Nodes joined with progress. LOCKED and AVAILABLE are derived from the node order. */
    public LiveData<List<RoadmapItem>> observeRoadmap(String userId) {
        LiveData<List<RoadmapNodeEntity>> nodes = contentDao.observeNodes();
        LiveData<List<UserNodeProgressEntity>> progress = progressDao.observeNodeProgress(userId);
        MediatorLiveData<List<RoadmapItem>> merged = new MediatorLiveData<>();
        merged.addSource(nodes, n -> merged.setValue(buildRoadmap(n, progress.getValue())));
        merged.addSource(progress, p -> merged.setValue(buildRoadmap(nodes.getValue(), p)));
        return merged;
    }

    public static List<RoadmapItem> buildRoadmap(List<RoadmapNodeEntity> nodes, List<UserNodeProgressEntity> progress) {
        List<RoadmapItem> items = new ArrayList<>();
        if (nodes == null) {
            return items;
        }
        Map<String, UserNodeProgressEntity> byNode = new HashMap<>();
        if (progress != null) {
            for (UserNodeProgressEntity p : progress) {
                byNode.put(p.nodeId, p);
            }
        }
        boolean previousDone = true;
        for (RoadmapNodeEntity node : nodes) {
            UserNodeProgressEntity p = byNode.get(node.id);
            boolean done = p != null && (p.status == NodeStatus.COMPLETED || p.status == NodeStatus.MASTERED);
            NodeStatus status = done ? p.status : previousDone ? NodeStatus.AVAILABLE : NodeStatus.LOCKED;
            items.add(new RoadmapItem(node, status, p == null ? 0 : p.starsEarned));
            previousDone = done;
        }
        return items;
    }

    // ----- Lesson completion ---------------------------------------------

    /**
     * Records a finished lesson: stars, XP, gems, streak, study minutes, SM-2 cards for
     * the words or grammar topic in it, and any badges that were just unlocked.
     */
    public void completeLesson(String userId, RoadmapNodeEntity node, int correct, int total, int minutes,
                               Callback<LessonReward> callback) {
        executors.diskIO().execute(() -> {
            LessonReward reward = completeLessonNow(userId, node, correct, total, minutes);
            if (callback != null) {
                executors.mainThread().execute(() -> callback.onResult(reward));
            }
        });
    }

    private LessonReward completeLessonNow(String userId, RoadmapNodeEntity node, int correct, int total,
                                           int minutes) {
        long now = System.currentTimeMillis();
        double ratio = total == 0 ? 0 : (double) correct / total;
        int stars = ratio >= 0.9 ? 3 : ratio >= 0.6 ? 2 : 1;

        UserNodeProgressEntity existing = progressDao.getNodeProgressFor(userId, node.id);
        boolean firstCompletion = existing == null || existing.completedAt == null;
        int previousStars = existing == null ? 0 : existing.starsEarned;

        UserNodeProgressEntity progress = existing != null ? existing : new UserNodeProgressEntity();
        if (existing == null) {
            progress.id = UUID.randomUUID().toString();
            progress.userId = userId;
            progress.nodeId = node.id;
        }
        progress.starsEarned = Math.max(previousStars, stars);
        progress.status = progress.starsEarned == 3 ? NodeStatus.MASTERED : NodeStatus.COMPLETED;
        if (firstCompletion) {
            progress.completedAt = now;
        }
        progress.updatedAt = now;
        progressDao.upsertNodeProgress(progress);

        if (node.nodeType == NodeType.VOCABULARY) {
            scheduleVocabulary(userId, node, now);
        } else if (node.nodeType == NodeType.GRAMMAR) {
            scheduleGrammar(userId, node, now);
        }

        int xp = correct * 10 + (firstCompletion ? 30 : 0);
        int gems = (firstCompletion ? 5 : 0) + (stars == 3 && previousStars < 3 ? 5 : 0);
        int streak = userRepository.touchActivityNow(userId, Math.max(1, minutes));
        userRepository.addRewardsNow(userId, xp, gems);

        List<String> badges = firstCompletion ? badgeEvaluator.evaluateNow(userId) : new ArrayList<>();
        return new LessonReward(stars, xp, gems, streak, firstCompletion, badges);
    }

    private void scheduleVocabulary(String userId, RoadmapNodeEntity node, long now) {
        for (VocabularyEntity v : contentDao.getVocabularyByNode(node.id)) {
            ensureRetention(userId, ItemType.VOCABULARY, v.id, now);
        }
    }

    private void scheduleGrammar(String userId, RoadmapNodeEntity node, long now) {
        GrammarEntity grammar = contentDao.getGrammarByNode(node.id);
        if (grammar != null) {
            ensureRetention(userId, ItemType.GRAMMAR, grammar.id, now);
        }
    }

    private void ensureRetention(String userId, ItemType type, String itemId, long now) {
        if (progressDao.getRetention(userId, type, itemId) != null) {
            return;
        }
        UserItemRetentionEntity row = new UserItemRetentionEntity();
        row.id = UUID.randomUUID().toString();
        row.userId = userId;
        row.itemType = type;
        row.itemId = itemId;
        row.stabilityFactor = SrsAlgorithm.DEFAULT_EASINESS;
        row.repetitionNumber = 0;
        row.intervalDays = 1;
        row.nextReviewAt = now + ONE_DAY_MILLIS;
        row.lastReviewedAt = now;
        row.updatedAt = now;
        progressDao.upsertRetention(row);
    }

    // ----- Answers, weaknesses and SM-2 ----------------------------------

    /** Updates mastery for the answered question's tag and, for vocabulary/grammar, its SM-2 card. */
    public void recordAnswer(String userId, LessonQuestionEntity question, boolean correct) {
        executors.diskIO().execute(() -> {
            WeaknessDetector.Detection detection = weaknessDetector.detect(question);
            applyDetectionNow(userId, detection, correct);
        });
    }

    /** Records a mistake reported by the AI mascot (error_tag, skill). */
    public void recordMistake(String userId, String errorTag, SkillType skill) {
        executors.diskIO().execute(() -> applyDetectionNow(userId,
                new WeaknessDetector.Detection(errorTag, skill, null, null), false));
    }

    private void applyDetectionNow(String userId, WeaknessDetector.Detection detection, boolean correct) {
        if (detection.errorTag.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        UserWeaknessEntity weakness = progressDao.getWeakness(userId, detection.errorTag);
        if (weakness == null) {
            if (correct) {
                return;
            }
            weakness = new UserWeaknessEntity();
            weakness.id = UUID.randomUUID().toString();
            weakness.userId = userId;
            weakness.errorTag = detection.errorTag;
            weakness.skillType = detection.skill;
            weakness.masteryScore = 0f;
            weakness.errorCount = 0;
        }
        weakness.relatedItemId = detection.relatedItemId != null ? detection.relatedItemId : weakness.relatedItemId;
        weakness.updatedAt = now;
        if (correct) {
            weakness.masteryScore = Math.min(1f, weakness.masteryScore + 0.2f);
        } else {
            weakness.masteryScore = Math.max(0f, weakness.masteryScore - 0.2f);
            weakness.errorCount += 1;
            weakness.lastFailedAt = now;
        }
        progressDao.upsertWeakness(weakness);

        if (detection.relatedItemType != null && detection.relatedItemId != null) {
            applySrsNow(userId, detection.relatedItemType, detection.relatedItemId,
                    correct ? SrsAlgorithm.QUALITY_GOOD : 2, now);
        }
        badgeEvaluator.evaluateNow(userId);
    }

    /** Applies one SM-2 review. quality follows {@link SrsAlgorithm}. */
    public void reviewCard(String userId, ItemType type, String itemId, SrsAlgorithm.Button button,
                           Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            applySrsNow(userId, type, itemId, SrsAlgorithm.qualityForButton(button), System.currentTimeMillis());
            badgeEvaluator.evaluateNow(userId);
            if (callback != null) {
                executors.mainThread().execute(() -> callback.onResult(null));
            }
        });
    }

    private void applySrsNow(String userId, ItemType type, String itemId, int quality, long now) {
        UserItemRetentionEntity row = progressDao.getRetention(userId, type, itemId);
        if (row == null) {
            ensureRetention(userId, type, itemId, now);
            row = progressDao.getRetention(userId, type, itemId);
        }
        SrsAlgorithm.State next = SrsAlgorithm.review(
                new SrsAlgorithm.State(row.stabilityFactor, row.repetitionNumber, row.intervalDays), quality);
        row.stabilityFactor = next.easinessFactor;
        row.repetitionNumber = next.repetitions;
        row.intervalDays = next.intervalDays;
        row.lastReviewedAt = now;
        row.nextReviewAt = now + next.intervalDays * ONE_DAY_MILLIS;
        row.updatedAt = now;
        progressDao.upsertRetention(row);
    }

    // ----- Weakness Clinic and flashcards --------------------------------

    public LiveData<List<UserWeaknessEntity>> observeWeaknesses(String userId) {
        return progressDao.observeWeaknesses(userId, WEAK_THRESHOLD, WEAKNESS_LIMIT);
    }

    public LiveData<Integer> observeDueCount(String userId) {
        return progressDao.observeDueCount(userId, System.currentTimeMillis());
    }

    public LiveData<Integer> observeMasteredVocabulary(String userId) {
        return progressDao.observeMastered(userId, ItemType.VOCABULARY, MASTERED_REPETITIONS);
    }

    /** Retention rows for the learner's vocabulary cards, keyed by vocabulary id. */
    public void loadVocabularyRetention(String userId, Callback<Map<String, UserItemRetentionEntity>> callback) {
        executors.diskIO().execute(() -> {
            Map<String, UserItemRetentionEntity> byItem = new HashMap<>();
            for (UserItemRetentionEntity row : progressDao.getRetentionByType(userId, ItemType.VOCABULARY)) {
                byItem.put(row.itemId, row);
            }
            if (callback != null) {
                executors.mainThread().execute(() -> callback.onResult(byItem));
            }
        });
    }

    /** Per-skill progress, lessons and weaknesses, keyed by the skill ids used on Profile. */
    public void loadSkillSummaries(String userId, Callback<Map<String, SkillProgress.Summary>> callback) {
        executors.diskIO().execute(() -> {
            Map<String, SkillProgress.Summary> summaries = SkillProgress.build(
                    contentDao.getAllNodes(),
                    progressDao.getNodeProgress(userId),
                    progressDao.getWeaknesses(userId, Float.MAX_VALUE, Integer.MAX_VALUE));
            if (callback != null) {
                executors.mainThread().execute(() -> callback.onResult(summaries));
            }
        });
    }

    /** Vocabulary cards due for review now, oldest first. */
    public void loadDueVocabulary(String userId, int limit, Callback<List<VocabularyEntity>> callback) {
        executors.diskIO().execute(() -> {
            List<String> ids = new ArrayList<>();
            for (UserItemRetentionEntity row : progressDao.getDueRetention(userId, System.currentTimeMillis(), limit)) {
                if (row.itemType == ItemType.VOCABULARY) {
                    ids.add(row.itemId);
                }
            }
            List<VocabularyEntity> cards = ids.isEmpty() ? new ArrayList<>() : contentDao.getVocabularyByIds(ids);
            if (callback != null) {
                executors.mainThread().execute(() -> callback.onResult(cards));
            }
        });
    }

    /** Count of retention rows that are due, read synchronously by background workers. */
    public int countDueNow(String userId) {
        return progressDao.getDueRetention(userId, System.currentTimeMillis(), Integer.MAX_VALUE).size();
    }

    // ----- Helpers for background workers and the AI service -------------

    public List<UserWeaknessEntity> getTopWeaknessesNow(String userId, int limit) {
        return progressDao.getTopMistakes(userId, limit);
    }

    public int countFixedWeaknessesNow(String userId) {
        return progressDao.countFixedWeaknesses(userId, WEAK_THRESHOLD);
    }

    public int countMasteredVocabularyNow(String userId) {
        return progressDao.countMastered(userId, ItemType.VOCABULARY, MASTERED_REPETITIONS);
    }

    public int countCompletedGrammarNow(String userId) {
        return progressDao.countCompletedGrammarNodes(userId);
    }

    public int countCompletedNodesNow(String userId) {
        return progressDao.countCompletedNodes(userId);
    }

    public List<UserNodeProgressEntity> getNodeProgressNow(String userId) {
        return progressDao.getNodeProgress(userId);
    }

    public List<UserItemRetentionEntity> getRetentionForSyncNow(String userId) {
        return progressDao.getDueRetention(userId, Long.MAX_VALUE, Integer.MAX_VALUE);
    }

    public List<UserWeaknessEntity> getWeaknessesForSyncNow(String userId) {
        return progressDao.getWeaknesses(userId, Float.MAX_VALUE, Integer.MAX_VALUE);
    }

    public void saveRemoteNodeProgress(List<UserNodeProgressEntity> rows) {
        progressDao.upsertAllNodeProgress(rows);
    }

    public void saveRemoteWeakness(UserWeaknessEntity row) {
        progressDao.upsertWeakness(row);
    }

    public void saveRemoteRetention(UserItemRetentionEntity row) {
        progressDao.upsertRetention(row);
    }

    public UserNodeProgressEntity getNodeProgressForNow(String userId, String nodeId) {
        return progressDao.getNodeProgressFor(userId, nodeId);
    }

    public UserItemRetentionEntity getRetentionNow(String userId, ItemType type, String itemId) {
        return progressDao.getRetention(userId, type, itemId);
    }

    public UserWeaknessEntity getWeaknessNow(String userId, String tag) {
        return progressDao.getWeakness(userId, tag);
    }
}
