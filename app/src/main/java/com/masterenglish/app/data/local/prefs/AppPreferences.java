package com.masterenglish.app.data.local.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.google.gson.Gson;


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
    private static final String KEY_ONBOARDING_ANSWERS = "onboarding_answers";
    private static final String KEY_GEMINI_KEY = "gemini_api_key";
    private static final String KEY_LAST_SYNC = "last_sync_at";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

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

    public void saveOnboardingAnswers(OnboardingAnswers answers) {
        prefs.edit().putString(KEY_ONBOARDING_ANSWERS, gson.toJson(answers)).apply();
    }

    @Nullable
    public OnboardingAnswers getOnboardingAnswers() {
        String json = prefs.getString(KEY_ONBOARDING_ANSWERS, null);
        return json == null ? null : gson.fromJson(json, OnboardingAnswers.class);
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
