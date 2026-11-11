package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.masterenglish.app.data.local.Enums.ItemType;

/** Table user_item_retention: SM-2 state per (user, vocabulary or grammar item). */
@Entity(
        tableName = "user_item_retention",
        foreignKeys = @ForeignKey(entity = UserEntity.class, parentColumns = "id",
                childColumns = "user_id", onDelete = ForeignKey.CASCADE),
        indices = {
                @Index(value = {"user_id", "item_type", "item_id"}, unique = true),
                @Index("next_review_at")
        })
public class UserItemRetentionEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @ColumnInfo(name = "user_id")
    @NonNull
    public String userId = "";

    @ColumnInfo(name = "item_type")
    @NonNull
    public ItemType itemType = ItemType.VOCABULARY;

    @ColumnInfo(name = "item_id")
    @NonNull
    public String itemId = "";

    /** SM-2 easiness factor (EF), starts at 2.5 and never drops below 1.3. */
    @ColumnInfo(name = "stability_factor")
    public double stabilityFactor = 2.5;

    @ColumnInfo(name = "repetition_number")
    public int repetitionNumber;

    @ColumnInfo(name = "interval_days")
    public int intervalDays = 1;

    @ColumnInfo(name = "next_review_at")
    public long nextReviewAt;

    @ColumnInfo(name = "last_reviewed_at")
    public long lastReviewedAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}
