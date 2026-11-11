package com.masterenglish.app.service.sync;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/** Schedules the periodic background sync and on-demand "Đồng bộ ngay" runs. */
public final class SyncScheduler {

    private static final String PERIODIC_NAME = "sync_progress_periodic";
    private static final String ONE_TIME_NAME = "sync_progress_now";

    private SyncScheduler() {
    }

    private static Constraints connected() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }

    public static void schedulePeriodic(Context context) {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(SyncWorker.class, 6, TimeUnit.HOURS)
                .setConstraints(connected())
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    public static void syncNow(Context context) {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(connected())
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(ONE_TIME_NAME, ExistingWorkPolicy.REPLACE, request);
    }
}
