package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.masterenglish.app.data.local.Enums.SkillType;

/** Table user_weaknesses. Unique per (user, error_tag). mastery_score 0.0 (weak) to 1.0 (fixed). */
@Entity(
        tableName = "user_weaknesses",
        foreignKeys = @ForeignKey(entity = UserEntity.class, parentColumns = "id",
                childColumns = "user_id", onDelete = ForeignKey.CASCADE),
        indices = {
                @Index(value = {"user_id", "error_tag"}, unique = true),
                @Index("user_id")
        })
public class UserWeaknessEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @ColumnInfo(name = "user_id")
    @NonNull
    public String userId = "";

    @ColumnInfo(name = "skill_type")
    @NonNull
    public SkillType skillType = SkillType.GRAMMAR;

    @ColumnInfo(name = "error_tag")
    @NonNull
    public String errorTag = "";

    @ColumnInfo(name = "related_item_id")
    @Nullable
    public String relatedItemId;

    @ColumnInfo(name = "error_count")
    public int errorCount = 1;

    @ColumnInfo(name = "mastery_score")
    public float masteryScore;

    @ColumnInfo(name = "last_failed_at")
    public long lastFailedAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}
