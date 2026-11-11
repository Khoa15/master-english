package com.masterenglish.app.service.reminder;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Arms one exact alarm per day at the learner's chosen time. The alarm is
 * re-armed by {@link AlarmReceiver} after it fires and by {@link BootReceiver} after a reboot.
 */
public final class ReminderScheduler {

    static final String EXTRA_HOUR = "reminder_hour";
    static final String EXTRA_MINUTE = "reminder_minute";
    private static final int REQUEST_CODE = 4101;

    private ReminderScheduler() {
    }

    public static void schedule(Context context, int hour, int minute) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }
        long triggerAt = nextTriggerMillis(hour, minute);
        PendingIntent pending = pendingIntent(context, hour, minute);
        alarmManager.cancel(pending);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // The user revoked exact alarms: fall back to an inexact alarm rather than skipping the reminder.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending);
            return;
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending);
    }

    public static void cancel(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent(context, 0, 0));
        }
    }

    /** The next occurrence of hour:minute, today if still ahead, otherwise tomorrow. */
    static long nextTriggerMillis(int hour, int minute) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
        if (!target.isAfter(now)) {
            target = target.plusDays(1);
        }
        return target.atZone(zone).toInstant().toEpochMilli();
    }

    static PendingIntent pendingIntent(Context context, int hour, int minute) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra(EXTRA_HOUR, hour);
        intent.putExtra(EXTRA_MINUTE, minute);
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
