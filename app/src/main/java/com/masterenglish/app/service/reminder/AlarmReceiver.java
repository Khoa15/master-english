package com.masterenglish.app.service.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.masterenglish.app.data.local.prefs.AppPreferences;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

/** Fires the daily reminder notification and re-arms tomorrow's alarm. */
@AndroidEntryPoint
public class AlarmReceiver extends BroadcastReceiver {

    @Inject
    AppPreferences preferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (preferences == null || !preferences.isReminderEnabled()) {
            return;
        }
        NotificationHelper.showDailyReminder(context);
        int hour = intent.getIntExtra(ReminderScheduler.EXTRA_HOUR, preferences.getReminderHour());
        int minute = intent.getIntExtra(ReminderScheduler.EXTRA_MINUTE, preferences.getReminderMinute());
        ReminderScheduler.schedule(context, hour, minute);
    }
}
