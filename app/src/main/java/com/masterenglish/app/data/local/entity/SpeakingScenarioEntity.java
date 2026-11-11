package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Table speaking_scenarios: a practice situation the learner described in the Tutor tab. */
@Entity(
        tableName = "speaking_scenarios",
        foreignKeys = @ForeignKey(entity = UserEntity.class, parentColumns = "id",
                childColumns = "user_id", onDelete = ForeignKey.CASCADE),
        indices = @Index("user_id"))
public class SpeakingScenarioEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @ColumnInfo(name = "user_id")
    @NonNull
    public String userId = "";

    @ColumnInfo(name = "description")
    @NonNull
    public String description = "";

    @ColumnInfo(name = "created_at")
    public long createdAt;
}
