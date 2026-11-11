package com.masterenglish.app;

import android.app.Application;

import androidx.hilt.work.HiltWorkerFactory;
import androidx.work.Configuration;

import com.masterenglish.app.data.local.seed.LocalDataSeeder;
import com.masterenglish.app.service.reminder.NotificationHelper;
import com.masterenglish.app.service.sync.SyncScheduler;
import com.masterenglish.app.utils.AppExecutors;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

/** Application entry point: Hilt graph, WorkManager with Hilt workers, first-run seeding. */
@HiltAndroidApp
public class MasterEnglishApp extends Application implements Configuration.Provider {

    @Inject
    HiltWorkerFactory workerFactory;

    @Inject
    LocalDataSeeder seeder;

    @Inject
    AppExecutors executors;

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.createChannels(this);
        executors.diskIO().execute(seeder::seedIfEmpty);
        SyncScheduler.schedulePeriodic(this);
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder().setWorkerFactory(workerFactory).build();
    }
}
