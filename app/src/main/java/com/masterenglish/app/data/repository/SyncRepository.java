package com.masterenglish.app.data.repository;

import com.masterenglish.app.data.local.entity.UserNodeProgressEntity;
import com.masterenglish.app.data.local.entity.UserWeaknessEntity;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.remote.ApiService;
import com.masterenglish.app.data.remote.dto.SyncAck;
import com.masterenglish.app.data.remote.dto.SyncPayload;
import com.masterenglish.app.data.local.entity.UserItemRetentionEntity;

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
    private final AiRepository aiRepository;
    private final AppPreferences preferences;

    @Inject
    public SyncRepository(ApiService api, ProgressRepository progressRepository, AiRepository aiRepository,
                          AppPreferences preferences) {
        this.api = api;
        this.progressRepository = progressRepository;
        this.aiRepository = aiRepository;
        this.preferences = preferences;
    }

    public void syncNow(String userId) throws IOException {
        SyncPayload outgoing = new SyncPayload();
        outgoing.userId = userId;
        outgoing.sentAt = System.currentTimeMillis();
        outgoing.nodeProgress = progressRepository.getNodeProgressNow(userId);
        outgoing.retention = progressRepository.getRetentionForSyncNow(userId);
        outgoing.weaknesses = progressRepository.getWeaknessesForSyncNow(userId);
        outgoing.memories = aiRepository.getMemoriesNow(userId);

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
        for (UserNodeProgressEntity row : remote.nodeProgress) {
            UserNodeProgressEntity local = progressRepository.getNodeProgressForNow(userId, row.nodeId);
            if (local == null || row.updatedAt > local.updatedAt) {
                row.userId = userId;
                progressRepository.saveRemoteNodeProgress(java.util.Collections.singletonList(row));
            }
        }
        for (UserWeaknessEntity row : remote.weaknesses) {
            UserWeaknessEntity local = progressRepository.getWeaknessNow(userId, row.errorTag);
            if (local == null || row.updatedAt > local.updatedAt) {
                row.userId = userId;
                progressRepository.saveRemoteWeakness(row);
            }
        }
        for (UserItemRetentionEntity row : remote.retention) {
            UserItemRetentionEntity local = progressRepository.getRetentionNow(userId, row.itemType, row.itemId);
            if (local == null || row.updatedAt > local.updatedAt) {
                row.userId = userId;
                progressRepository.saveRemoteRetention(row);
            }
        }
    }
}
