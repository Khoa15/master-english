package com.masterenglish.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.masterenglish.app.data.local.entity.BadgeEntity;
import com.masterenglish.app.data.local.entity.UserBadgeEntity;

import java.util.List;

@Dao
public interface BadgeDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertBadges(List<BadgeEntity> badges);

    @Query("SELECT * FROM badges ORDER BY id ASC")
    LiveData<List<BadgeEntity>> observeBadges();

    @Query("SELECT * FROM badges ORDER BY id ASC")
    List<BadgeEntity> getBadges();

    /** Returns the row id, or -1 when the badge was already unlocked (UNIQUE user_id + badge_id). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long unlock(UserBadgeEntity userBadge);

    @Query("SELECT * FROM user_badges WHERE user_id = :userId")
    LiveData<List<UserBadgeEntity>> observeUnlocked(String userId);

    @Query("SELECT * FROM user_badges WHERE user_id = :userId")
    List<UserBadgeEntity> getUnlocked(String userId);
}
