package com.masterenglish.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.masterenglish.app.data.local.Enums.ItemType;
import com.masterenglish.app.data.local.Enums.SkillType;
import com.masterenglish.app.data.local.entity.UserItemRetentionEntity;
import com.masterenglish.app.data.local.entity.UserNodeProgressEntity;
import com.masterenglish.app.data.local.entity.UserWeaknessEntity;

import java.util.List;

/** Roadmap progress, SM-2 retention state and the weakness tracker. */
@Dao
public interface ProgressDao {

    // ----- Node progress -------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertNodeProgress(UserNodeProgressEntity progress);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertAllNodeProgress(List<UserNodeProgressEntity> progress);

    @Query("SELECT * FROM user_node_progress WHERE user_id = :userId")
    LiveData<List<UserNodeProgressEntity>> observeNodeProgress(String userId);

    @Query("SELECT * FROM user_node_progress WHERE user_id = :userId")
    List<UserNodeProgressEntity> getNodeProgress(String userId);

    @Query("SELECT * FROM user_node_progress WHERE user_id = :userId AND node_id = :nodeId LIMIT 1")
    UserNodeProgressEntity getNodeProgressFor(String userId, String nodeId);

    @Query("SELECT COUNT(*) FROM user_node_progress WHERE user_id = :userId AND status IN ('COMPLETED', 'MASTERED')")
    int countCompletedNodes(String userId);

    @Query("SELECT COUNT(*) FROM user_node_progress p JOIN roadmap_nodes n ON n.id = p.node_id "
            + "WHERE p.user_id = :userId AND n.node_type = 'GRAMMAR' AND p.status IN ('COMPLETED', 'MASTERED')")
    int countCompletedGrammarNodes(String userId);

    // ----- Spaced repetition (SM-2) --------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertRetention(UserItemRetentionEntity retention);

    @Query("SELECT * FROM user_item_retention WHERE user_id = :userId AND item_type = :type AND item_id = :itemId LIMIT 1")
    UserItemRetentionEntity getRetention(String userId, ItemType type, String itemId);

    @Query("SELECT * FROM user_item_retention WHERE user_id = :userId AND next_review_at <= :now "
            + "ORDER BY next_review_at ASC LIMIT :limit")
    List<UserItemRetentionEntity> getDueRetention(String userId, long now, int limit);

    @Query("SELECT COUNT(*) FROM user_item_retention WHERE user_id = :userId AND next_review_at <= :now")
    LiveData<Integer> observeDueCount(String userId, long now);

    @Query("SELECT COUNT(*) FROM user_item_retention WHERE user_id = :userId AND item_type = :type "
            + "AND repetition_number >= :minRepetitions")
    int countMastered(String userId, ItemType type, int minRepetitions);

    @Query("SELECT COUNT(*) FROM user_item_retention WHERE user_id = :userId AND item_type = :type "
            + "AND repetition_number >= :minRepetitions")
    LiveData<Integer> observeMastered(String userId, ItemType type, int minRepetitions);

    // ----- Weaknesses ----------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertWeakness(UserWeaknessEntity weakness);

    @Query("SELECT * FROM user_weaknesses WHERE user_id = :userId AND error_tag = :tag LIMIT 1")
    UserWeaknessEntity getWeakness(String userId, String tag);

    @Query("SELECT * FROM user_weaknesses WHERE user_id = :userId AND mastery_score < :threshold "
            + "ORDER BY mastery_score ASC, error_count DESC LIMIT :limit")
    LiveData<List<UserWeaknessEntity>> observeWeaknesses(String userId, float threshold, int limit);

    @Query("SELECT * FROM user_weaknesses WHERE user_id = :userId AND mastery_score < :threshold "
            + "ORDER BY mastery_score ASC, error_count DESC LIMIT :limit")
    List<UserWeaknessEntity> getWeaknesses(String userId, float threshold, int limit);

    @Query("SELECT COUNT(*) FROM user_weaknesses WHERE user_id = :userId AND mastery_score >= :threshold")
    int countFixedWeaknesses(String userId, float threshold);

    @Query("SELECT * FROM user_weaknesses WHERE user_id = :userId AND skill_type = :skill ORDER BY error_count DESC")
    List<UserWeaknessEntity> getWeaknessesBySkill(String userId, SkillType skill);

    @Query("SELECT * FROM user_weaknesses WHERE user_id = :userId ORDER BY error_count DESC LIMIT :limit")
    List<UserWeaknessEntity> getTopMistakes(String userId, int limit);
}
