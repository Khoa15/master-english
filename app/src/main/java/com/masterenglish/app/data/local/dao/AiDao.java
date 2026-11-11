package com.masterenglish.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.masterenglish.app.data.local.entity.AiChatMessageEntity;
import com.masterenglish.app.data.local.entity.AiChatSessionEntity;
import com.masterenglish.app.data.local.entity.AiPersonaMemoryEntity;

import java.util.List;

/** Long-term mascot memory and challenge conversations. */
@Dao
public interface AiDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertMemory(AiPersonaMemoryEntity memory);

    @Query("SELECT * FROM ai_persona_memories WHERE user_id = :userId ORDER BY updated_at DESC")
    List<AiPersonaMemoryEntity> getMemories(String userId);

    @Query("SELECT * FROM ai_persona_memories WHERE user_id = :userId AND memory_key = :key LIMIT 1")
    AiPersonaMemoryEntity getMemory(String userId, String key);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSession(AiChatSessionEntity session);

    @Query("SELECT * FROM ai_chat_sessions WHERE user_id = :userId ORDER BY started_at DESC LIMIT 1")
    AiChatSessionEntity getLatestSession(String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(AiChatMessageEntity message);

    @Query("SELECT * FROM ai_chat_messages WHERE session_id = :sessionId ORDER BY created_at ASC")
    LiveData<List<AiChatMessageEntity>> observeMessages(String sessionId);

    @Query("SELECT * FROM ai_chat_messages WHERE session_id = :sessionId ORDER BY created_at DESC LIMIT :limit")
    List<AiChatMessageEntity> getRecentMessages(String sessionId, int limit);
}
