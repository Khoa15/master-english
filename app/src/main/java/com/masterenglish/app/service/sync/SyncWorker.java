package com.masterenglish.app.service.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.repository.SyncRepository;

import java.io.IOException;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

/**
 * Background sync with the MySQL backend. Runs only with a network connection,
 * retries with back-off on failure, and skips Guest Mode (guest data stays on-device).
 */
@HiltWorker
public class SyncWorker extends Worker {

    private final AppPreferences preferences;
    private final SyncRepository syncRepository;

    @AssistedInject
    public SyncWorker(@Assisted @NonNull Context context, @Assisted @NonNull WorkerParameters params,
                      AppPreferences preferences, SyncRepository syncRepository) {
        super(context, params);
        this.preferences = preferences;
        this.syncRepository = syncRepository;
    }

    @NonNull
    @Override
    public Result doWork() {
        String userId = preferences.getCurrentUserId();
        if (userId == null || userId.startsWith("guest-")) {
            return Result.success();
        }
        try {
            syncRepository.syncNow(userId);
            return Result.success();
        } catch (IOException e) {
            return Result.retry();
        }
    }
}
