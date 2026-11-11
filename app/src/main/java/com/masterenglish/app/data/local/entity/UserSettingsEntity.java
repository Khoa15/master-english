package com.masterenglish.app.data.local.entity;

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

    @ColumnInfo(name = "target_learning_minutes")
    public int targetLearningMinutes = 15;
}
