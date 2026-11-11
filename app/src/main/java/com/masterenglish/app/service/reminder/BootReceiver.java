package com.masterenglish.app.service.reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.masterenglish.app.data.local.prefs.AppPreferences;

import dagger.hilt.android.AndroidEntryPoint;

import javax.inject.Inject;

/** Alarms are cleared on reboot, so re-arm the daily reminder once the device starts. */
@AndroidEntryPoint
public class BootReceiver extends BroadcastReceiver {

    @Inject
    AppPreferences preferences;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }
        if (preferences.isReminderEnabled()) {
            ReminderScheduler.schedule(context, preferences.getReminderHour(), preferences.getReminderMinute());
        }
    }
}
