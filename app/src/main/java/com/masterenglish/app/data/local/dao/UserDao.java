package com.masterenglish.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.masterenglish.app.data.local.entity.DailyStudyLogEntity;
import com.masterenglish.app.data.local.entity.UserEntity;
import com.masterenglish.app.data.local.entity.UserSettingsEntity;

import java.util.List;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertUser(UserEntity user);

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    UserEntity getUser(String userId);

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    LiveData<UserEntity> observeUser(String userId);

    @Query("UPDATE users SET streak_count = :streak, last_active_date = :date, updated_at = :now WHERE id = :userId")
    void updateStreak(String userId, int streak, String date, long now);

    @Query("UPDATE users SET experience_points = experience_points + :xp, gem_count = gem_count + :gems, "
            + "updated_at = :now WHERE id = :userId")
    void addRewards(String userId, int xp, int gems, long now);

    @Query("UPDATE users SET display_name = :displayName, email = :email, google_id = :googleId, "
            + "updated_at = :now WHERE id = :userId")
    void updateProfile(String userId, String displayName, String email, String googleId, long now);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertSettings(UserSettingsEntity settings);

    @Query("SELECT * FROM user_settings WHERE user_id = :userId LIMIT 1")
    UserSettingsEntity getSettings(String userId);

    @Query("SELECT * FROM user_settings WHERE user_id = :userId LIMIT 1")
    LiveData<UserSettingsEntity> observeSettings(String userId);

    @Query("INSERT INTO daily_study_log (user_id, date_key, minutes) VALUES (:userId, :dateKey, :minutes) "
            + "ON CONFLICT(user_id, date_key) DO UPDATE SET minutes = minutes + excluded.minutes")
    void addStudyMinutes(String userId, String dateKey, int minutes);

    @Query("SELECT * FROM daily_study_log WHERE user_id = :userId AND date_key BETWEEN :fromKey AND :toKey "
            + "ORDER BY date_key ASC")
    LiveData<List<DailyStudyLogEntity>> observeStudyLog(String userId, String fromKey, String toKey);
}
