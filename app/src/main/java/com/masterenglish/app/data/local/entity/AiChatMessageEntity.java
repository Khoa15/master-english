package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.JsonElement;
import com.masterenglish.app.data.local.Enums.Sender;

/** Table ai_chat_messages. detected_mistakes_json holds the error tags found in a user message. */
@Entity(
        tableName = "ai_chat_messages",
        foreignKeys = @ForeignKey(entity = AiChatSessionEntity.class, parentColumns = "id",
                childColumns = "session_id", onDelete = ForeignKey.CASCADE),
        indices = @Index("session_id"))
public class AiChatMessageEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @ColumnInfo(name = "session_id")
    @NonNull
    public String sessionId = "";

    @ColumnInfo(name = "sender")
    @NonNull
    public Sender sender = Sender.AI_MASCOT;

    @ColumnInfo(name = "message_text")
    @NonNull
    public String messageText = "";

    @ColumnInfo(name = "detected_mistakes_json")
    @Nullable
    public JsonElement detectedMistakes;

    @ColumnInfo(name = "correction_suggestions")
    @Nullable
    public String correctionSuggestions;

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
