package com.doradodo.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.doradodo.app.data.local.entity.DailyStudyLogEntity;
import com.doradodo.app.data.local.entity.UserEntity;
import com.doradodo.app.data.local.entity.UserSettingsEntity;

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

    /** The two language settings; null when the learner has no settings row yet. */
    @Query("SELECT learning_language FROM user_settings WHERE user_id = :userId LIMIT 1")
    String getLearningLanguage(String userId);

    @Query("SELECT display_language FROM user_settings WHERE user_id = :userId LIMIT 1")
    String getDisplayLanguage(String userId);

    @Query("UPDATE user_settings SET display_language = :lang WHERE user_id = :userId")
    int setDisplayLanguage(String userId, String lang);

    @Query("UPDATE user_settings SET learning_language = :lang WHERE user_id = :userId")
    int setLearningLanguage(String userId, String lang);

    /** Updates one column in place, so it never overwrites a setting that changed in the meantime. */
    @Query("UPDATE user_settings SET target_learning_minutes = :minutes WHERE user_id = :userId")
    int setTargetLearningMinutes(String userId, int minutes);

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
