package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Table ai_chat_sessions: one challenge conversation with the AI mascot. */
@Entity(
        tableName = "ai_chat_sessions",
        foreignKeys = @ForeignKey(entity = UserEntity.class, parentColumns = "id",
                childColumns = "user_id", onDelete = ForeignKey.CASCADE),
        indices = @Index("user_id"))
public class AiChatSessionEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @ColumnInfo(name = "user_id")
    @NonNull
    public String userId = "";

    @ColumnInfo(name = "session_title")
    @Nullable
    public String sessionTitle;

    @ColumnInfo(name = "started_at")
    public long startedAt;
}
