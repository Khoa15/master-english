package com.masterenglish.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.masterenglish.app.data.local.entity.WordProgressEntity;

import java.util.List;

/** The learner's SM-2 cards and answer counts, one row per word. */
@Dao
public interface ProgressDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(WordProgressEntity progress);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAll(List<WordProgressEntity> progress);

    @Query("SELECT * FROM word_progress WHERE user_id = :userId AND word_id = :wordId LIMIT 1")
    WordProgressEntity get(String userId, String wordId);

    @Query("SELECT * FROM word_progress WHERE user_id = :userId")
    List<WordProgressEntity> getAll(String userId);

    @Query("SELECT * FROM word_progress WHERE user_id = :userId AND next_review_at <= :now "
            + "ORDER BY next_review_at ASC LIMIT :limit")
    List<WordProgressEntity> getDue(String userId, long now, int limit);

    @Query("SELECT COUNT(*) FROM word_progress WHERE user_id = :userId AND next_review_at <= :now "
            + "AND repetitions < :masteredRepetitions")
    LiveData<Integer> observeDueCount(String userId, long now, int masteredRepetitions);

    @Query("SELECT COUNT(*) FROM word_progress WHERE user_id = :userId AND repetitions >= :minRepetitions")
    int countMastered(String userId, int minRepetitions);

    @Query("SELECT COUNT(*) FROM word_progress WHERE user_id = :userId AND repetitions >= :minRepetitions")
    LiveData<Integer> observeMastered(String userId, int minRepetitions);
}
