package com.doradodo.app.data.local.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.google.gson.Gson;


import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Small device-level state: who is signed in, onboarding answers and reminder settings.
 * Learner data itself lives in Room.
 */
@Singleton
public class AppPreferences {

    private static final String PREFS = "app_prefs";
    private static final String KEY_USER_ID = "current_user_id";
    private static final String KEY_ONBOARDED = "onboarding_done";
    private static final String KEY_ONBOARDING_ANSWERS = "onboarding_answers";
    /** Legacy plain-text Gemini key from earlier builds; removed on startup, never read again. */
    private static final String LEGACY_GEMINI_KEY = "gemini_api_key";
    private static final String KEY_LAST_SYNC = "last_sync_at";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_REMINDER_HOUR = "reminder_hour";
    private static final String KEY_REMINDER_MINUTE = "reminder_minute";
    private static final String KEY_TIME_ZONE = "time_zone_id";

    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    @Inject
    public AppPreferences(@ApplicationContext Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        // Drop the plain-text key left by earlier builds so it does not linger on the device.
        prefs.edit().remove(LEGACY_GEMINI_KEY).apply();
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

    /** True once the language chosen in onboarding has been copied into the learner's settings. */
    public boolean isOnboardingLanguageApplied() {
        return prefs.getBoolean("onboarding_language_applied", false);
    }

    public void setOnboardingLanguageApplied(boolean applied) {
        prefs.edit().putBoolean("onboarding_language_applied", applied).apply();
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

    /** The zone the learner picked in the profile, or null to follow the device. */
    @Nullable
    public String getTimeZoneId() {
        return prefs.getString(KEY_TIME_ZONE, null);
    }

    public void setTimeZoneId(@Nullable String zoneId) {
        prefs.edit().putString(KEY_TIME_ZONE, zoneId).apply();
    }

    public void clearSession() {
        prefs.edit().remove(KEY_USER_ID).apply();
    }
}
