package com.masterenglish.app.data.repository;

import androidx.lifecycle.LiveData;

import com.masterenglish.app.data.local.AppDatabase;
import com.masterenglish.app.data.local.dao.ProgressDao;
import com.masterenglish.app.data.local.entity.WordProgressEntity;
import com.masterenglish.app.service.gamification.BadgeEvaluator;
import com.masterenglish.app.utils.AppExecutors;
import com.masterenglish.app.utils.Callback;
import com.masterenglish.app.utils.SrsAlgorithm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * SM-2 review scheduling per word. Every write runs on the disk executor inside one Room transaction, so the
 * read-modify-write step of concurrent answers cannot overwrite each other.
 */
@Singleton
public class ProgressRepository {

    /** Correct reviews in a row before a word counts as mastered. */
    public static final int MASTERED_REPETITIONS = 3;
    private static final long ONE_DAY_MILLIS = 24L * 60 * 60 * 1000;

    private final AppDatabase database;
    private final ProgressDao progressDao;
    private final BadgeEvaluator badgeEvaluator;
    private final AppExecutors executors;

    @Inject
    public ProgressRepository(AppDatabase database, ProgressDao progressDao, BadgeEvaluator badgeEvaluator,
                              AppExecutors executors) {
        this.database = database;
        this.progressDao = progressDao;
        this.badgeEvaluator = badgeEvaluator;
        this.executors = executors;
    }

    /** Applies one SM-2 review for a word and unlocks any badge it earns. Answers on the main thread. */
    public void reviewCard(String userId, String wordId, SrsAlgorithm.Button button, Callback<Void> callback) {
        executors.diskIO().execute(() -> {
            database.runInTransaction(() -> {
                applyReviewNow(userId, wordId, SrsAlgorithm.qualityForButton(button),
                        button != SrsAlgorithm.Button.AGAIN, System.currentTimeMillis());
                badgeEvaluator.evaluateNow(userId);
            });
            if (callback != null) {
                executors.mainThread().execute(() -> callback.onResult(null));
            }
        });
    }

    private void applyReviewNow(String userId, String wordId, int quality, boolean correct, long now) {
        WordProgressEntity row = progressDao.get(userId, wordId);
        if (row == null) {
            row = new WordProgressEntity();
            row.userId = userId;
            row.wordId = wordId;
            row.easeFactor = SrsAlgorithm.DEFAULT_EASINESS;
            row.intervalDays = 1;
        }
        SrsAlgorithm.State next = SrsAlgorithm.review(
                new SrsAlgorithm.State(row.easeFactor, row.repetitions, row.intervalDays), quality);
        row.easeFactor = next.easinessFactor;
        row.repetitions = next.repetitions;
        row.intervalDays = next.intervalDays;
        row.lastReviewedAt = now;
        row.nextReviewAt = now + next.intervalDays * ONE_DAY_MILLIS;
        row.updatedAt = now;
        if (correct) {
            row.timesCorrect += 1;
        } else {
            row.timesWrong += 1;
        }
        progressDao.upsert(row);
    }

    /** The learner's cards, keyed by word id. */
    public void loadProgress(String userId, Callback<Map<String, WordProgressEntity>> callback) {
        executors.diskIO().execute(() -> {
            Map<String, WordProgressEntity> byWord = new HashMap<>();
            for (WordProgressEntity row : progressDao.getAll(userId)) {
                byWord.put(row.wordId, row);
            }
            if (callback != null) {
                executors.mainThread().execute(() -> callback.onResult(byWord));
            }
        });
    }

    public LiveData<Integer> observeDueCount(String userId) {
        return progressDao.observeDueCount(userId, System.currentTimeMillis(), MASTERED_REPETITIONS);
    }

    public LiveData<Integer> observeMastered(String userId) {
        return progressDao.observeMastered(userId, MASTERED_REPETITIONS);
    }

    // ----- Sync ----------------------------------------------------------

    public List<WordProgressEntity> getProgressForSyncNow(String userId) {
        return progressDao.getAll(userId);
    }

    public WordProgressEntity getProgressNow(String userId, String wordId) {
        return progressDao.get(userId, wordId);
    }

    public void saveRemoteProgress(WordProgressEntity row) {
        progressDao.upsert(row);
    }
}
