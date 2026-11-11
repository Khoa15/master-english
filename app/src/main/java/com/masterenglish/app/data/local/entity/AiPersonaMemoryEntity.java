package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Table ai_persona_memories: long-term facts the AI mascot keeps about the learner. */
@Entity(
        tableName = "ai_persona_memories",
        foreignKeys = @ForeignKey(entity = UserEntity.class, parentColumns = "id",
                childColumns = "user_id", onDelete = ForeignKey.CASCADE),
        indices = @Index(value = {"user_id", "memory_key"}, unique = true))
public class AiPersonaMemoryEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @ColumnInfo(name = "user_id")
    @NonNull
    public String userId = "";

    /** e.g. learning_goal, start_level, weak_tag:present_perfect_experience. */
    @ColumnInfo(name = "memory_key")
    @NonNull
    public String memoryKey = "";

    @ColumnInfo(name = "memory_value")
    @NonNull
    public String memoryValue = "";

    @ColumnInfo(name = "confidence_score")
    public float confidenceScore = 1.0f;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}
