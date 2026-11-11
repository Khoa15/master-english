package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Table user_badges. Unique per (user, badge). */
@Entity(
        tableName = "user_badges",
        foreignKeys = {
                @ForeignKey(entity = UserEntity.class, parentColumns = "id",
                        childColumns = "user_id", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = BadgeEntity.class, parentColumns = "id",
                        childColumns = "badge_id", onDelete = ForeignKey.CASCADE)
        },
        indices = {
                @Index(value = {"user_id", "badge_id"}, unique = true),
                @Index("badge_id")
        })
public class UserBadgeEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @ColumnInfo(name = "user_id")
    @NonNull
    public String userId = "";

    @ColumnInfo(name = "badge_id")
    @NonNull
    public String badgeId = "";

    @ColumnInfo(name = "unlocked_at")
    public long unlockedAt;
}
