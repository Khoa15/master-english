package com.masterenglish.app.data.repository;

import androidx.lifecycle.LiveData;

import com.masterenglish.app.data.local.dao.AiDao;
import com.masterenglish.app.data.local.entity.AiChatMessageEntity;
import com.masterenglish.app.data.local.entity.AiChatSessionEntity;
import com.masterenglish.app.data.local.entity.AiPersonaMemoryEntity;
import com.masterenglish.app.utils.AppExecutors;
import com.masterenglish.app.utils.Callback;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Long-term mascot memory and challenge conversations. */
@Singleton
public class AiRepository {

    private final AiDao aiDao;
    private final AppExecutors executors;

    @Inject
    public AiRepository(AiDao aiDao, AppExecutors executors) {
        this.aiDao = aiDao;
        this.executors = executors;
    }

    public void remember(String userId, String key, String value, float confidence) {
        executors.diskIO().execute(() -> rememberNow(userId, key, value, confidence));
    }

    public void rememberNow(String userId, String key, String value, float confidence) {
        long now = System.currentTimeMillis();
        AiPersonaMemoryEntity memory = aiDao.getMemory(userId, key);
        if (memory == null) {
            memory = new AiPersonaMemoryEntity();
            memory.id = UUID.randomUUID().toString();
            memory.userId = userId;
            memory.memoryKey = key;
            memory.createdAt = now;
        }
        memory.memoryValue = value;
        memory.confidenceScore = confidence;
        memory.updatedAt = now;
        aiDao.upsertMemory(memory);
    }

    public List<AiPersonaMemoryEntity> getMemoriesNow(String userId) {
        return aiDao.getMemories(userId);
    }

    /** Returns the latest chat session, creating one when the learner has none. */
    public String ensureSessionNow(String userId) {
        AiChatSessionEntity session = aiDao.getLatestSession(userId);
        if (session == null) {
            session = new AiChatSessionEntity();
            session.id = UUID.randomUUID().toString();
            session.userId = userId;
            session.sessionTitle = "Thử thách với Mascot";
            session.startedAt = System.currentTimeMillis();
            aiDao.insertSession(session);
        }
        return session.id;
    }

    public void saveMessage(AiChatMessageEntity message) {
        executors.diskIO().execute(() -> aiDao.insertMessage(message));
    }

    public void saveMessageNow(AiChatMessageEntity message) {
        aiDao.insertMessage(message);
    }

    public LiveData<List<AiChatMessageEntity>> observeMessages(String sessionId) {
        return aiDao.observeMessages(sessionId);
    }

    public void ensureSession(String userId, Callback<String> callback) {
        executors.diskIO().execute(() -> {
            String sessionId = ensureSessionNow(userId);
            executors.mainThread().execute(() -> callback.onResult(sessionId));
        });
    }
}
