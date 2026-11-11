package com.masterenglish.app.data.local.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;


import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Small device-level state: who is signed in, onboarding answers and the optional
 * Gemini key entered in Settings. Learner data itself lives in Room.
 */
@Singleton
public class AppPreferences {

    private static final String PREFS = "master_english_prefs";
    private static final String KEY_USER_ID = "current_user_id";
    private static final String KEY_ONBOARDED = "onboarding_done";
    private static final String KEY_GOAL = "onboarding_goal";
    private static final String KEY_LEVEL = "onboarding_level";
    private static final String KEY_MINUTES = "onboarding_minutes";
    private static final String KEY_GEMINI_KEY = "gemini_api_key";
    private static final String KEY_LAST_SYNC = "last_sync_at";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";

    private final SharedPreferences prefs;

    @Inject
    public AppPreferences(@ApplicationContext Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Nullable
    public String getCurrentUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }

    public void setCurrentUserId(@Nullable String userId) {
        prefs.edit().putString(KEY_USER_ID, userId).apply();
    }

    public boolean isOnboardingDone() {
        return prefs.getBoolean(KEY_ONBOARDED, false);
    }

    public void setOnboardingDone(boolean done) {
        prefs.edit().putBoolean(KEY_ONBOARDED, done).apply();
    }

    public void saveOnboardingAnswers(String goal, String level, int dailyMinutes) {
        prefs.edit()
                .putString(KEY_GOAL, goal)
                .putString(KEY_LEVEL, level)
                .putInt(KEY_MINUTES, dailyMinutes)
                .apply();
    }

    @Nullable
    public String getOnboardingGoal() {
        return prefs.getString(KEY_GOAL, null);
    }

    @Nullable
    public String getOnboardingLevel() {
        return prefs.getString(KEY_LEVEL, null);
    }

    public int getOnboardingMinutes() {
        return prefs.getInt(KEY_MINUTES, 15);
    }

    @Nullable
    public String getGeminiApiKey() {
        return prefs.getString(KEY_GEMINI_KEY, null);
    }

    public void setGeminiApiKey(@Nullable String key) {
        prefs.edit().putString(KEY_GEMINI_KEY, key == null || key.trim().isEmpty() ? null : key.trim()).apply();
    }

    public long getLastSyncAt() {
        return prefs.getLong(KEY_LAST_SYNC, 0L);
    }

    public void setLastSyncAt(long millis) {
        prefs.edit().putLong(KEY_LAST_SYNC, millis).apply();
    }

    /** Mirrors the reminder settings so BootReceiver can re-arm the alarm without a database read. */
    public void saveReminder(boolean enabled, int hour, int minute) {
        prefs.edit()
                .putBoolean(KEY_REMINDER_ENABLED, enabled)
                .putInt(KEY_REMINDER_HOUR, hour)
                .putInt(KEY_REMINDER_MINUTE, minute)
                .apply();
    }

    public boolean isReminderEnabled() {
        return prefs.getBoolean(KEY_REMINDER_ENABLED, true);
    }

    public int getReminderHour() {
        return prefs.getInt(KEY_REMINDER_HOUR, 20);
    }

    public int getReminderMinute() {
        return prefs.getInt(KEY_REMINDER_MINUTE, 0);
    }

    public void clearSession() {
        prefs.edit().remove(KEY_USER_ID).apply();
    }
}
