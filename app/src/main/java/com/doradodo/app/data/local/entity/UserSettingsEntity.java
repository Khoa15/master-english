package com.doradodo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Table user_settings: one row per user. */
@Entity(tableName = "user_settings")
public class UserSettingsEntity {
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    @NonNull
    public String userId = "";

    /** Local time of the daily reminder, HH:mm:ss. */
    @ColumnInfo(name = "daily_reminder_time")
    @NonNull
    public String dailyReminderTime = "20:00:00";

    @ColumnInfo(name = "notifications_enabled")
    public boolean notificationsEnabled = true;

    @ColumnInfo(name = "sound_fx_enabled")
    public boolean soundFxEnabled = true;

    /** EASY, MEDIUM, HARD or ADAPTIVE. */
    @ColumnInfo(name = "ai_challenge_difficulty")
    @NonNull
    public String aiChallengeDifficulty = "ADAPTIVE";

    /** Language the learner is learning (BCP-47); one at a time, and the learner can switch. Words, packs and games follow it. */
    @ColumnInfo(name = "learning_language", defaultValue = "'en'")
    @NonNull
    public String learningLanguage = "en";
    /** Language the app is shown in (BCP-47). The Entertainment games offered depend on it. */
    @ColumnInfo(name = "display_language", defaultValue = "'en'")
    @NonNull
    public String displayLanguage = "en";
    @ColumnInfo(name = "target_learning_minutes")
    public int targetLearningMinutes = 15;
}
