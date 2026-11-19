package com.doradodo.app.data.repository;

import com.doradodo.app.data.local.prefs.AppPreferences;
import com.doradodo.app.data.local.entity.WordProgressEntity;
import com.doradodo.app.data.remote.ApiService;
import com.doradodo.app.data.remote.dto.SyncAck;
import com.doradodo.app.data.remote.dto.SyncPayload;
import com.doradodo.app.utils.AppExecutors;
import com.doradodo.app.utils.Callback;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

/**
 * Pushes local progress to the MySQL backend and pulls the server copy back.
 * Conflicts are resolved by last-write-wins on updated_at, per row.
 * Blocking: call from WorkManager or another background thread only.
 */
@Singleton
public class SyncRepository {

    private final ApiService api;
    private final ProgressRepository progressRepository;
    private final AppPreferences preferences;
    private final AppExecutors executors;

    @Inject
    public SyncRepository(ApiService api, ProgressRepository progressRepository,
                          AppPreferences preferences, AppExecutors executors) {
        this.api = api;
        this.progressRepository = progressRepository;
        this.preferences = preferences;
        this.executors = executors;
    }

    /**
     * Non-blocking {@link #syncNow}, for pull-to-refresh. Runs on a background thread and reports
     * whether the sync succeeded on the main thread.
     */
    public void syncInBackground(String userId, Callback<Boolean> onDone) {
        executors.diskIO().execute(() -> {
            boolean success;
            try {
                syncNow(userId);
                success = true;
            } catch (Exception e) {
                success = false;
            }
            boolean result = success;
            executors.mainThread().execute(() -> onDone.onResult(result));
        });
    }

    public void syncNow(String userId) throws IOException {
        // Guest accounts only exist on this device, so there is nothing to sync with the server.
        if (userId == null || userId.startsWith("guest-")) {
            return;
        }
        SyncPayload outgoing = new SyncPayload();
        outgoing.userId = userId;
        outgoing.sentAt = System.currentTimeMillis();
        outgoing.wordProgress = progressRepository.getProgressForSyncNow(userId);

        Response<SyncAck> pushed = api.pushProgress(outgoing).execute();
        if (!pushed.isSuccessful()) {
            throw new IOException("Push failed with HTTP " + pushed.code());
        }

        Response<SyncPayload> pulled = api.pullProgress(userId).execute();
        if (pulled.isSuccessful() && pulled.body() != null) {
            mergeRemote(userId, pulled.body());
        }
        preferences.setLastSyncAt(outgoing.sentAt);
    }

    private void mergeRemote(String userId, SyncPayload remote) {
        for (WordProgressEntity row : remote.wordProgress) {
            WordProgressEntity local = progressRepository.getProgressNow(userId, row.wordId);
            if (local == null || row.updatedAt > local.updatedAt) {
                row.userId = userId;
                progressRepository.saveRemoteProgress(row);
            }
        }
    }
}
