package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Table users. id is the Firebase UID, or "guest-<uuid>" in Guest Mode. */
@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @ColumnInfo(name = "email")
    @NonNull
    public String email = "";

    @ColumnInfo(name = "google_id")
    @Nullable
    public String googleId;

    @ColumnInfo(name = "display_name")
    @NonNull
    public String displayName = "";

    @ColumnInfo(name = "avatar_url")
    @Nullable
    public String avatarUrl;

    @ColumnInfo(name = "streak_count")
    public int streakCount;

    /** ISO date (yyyy-MM-dd) of the last day with learning activity. */
    @ColumnInfo(name = "last_active_date")
    @Nullable
    public String lastActiveDate;

    @ColumnInfo(name = "gem_count")
    public int gemCount;

    @ColumnInfo(name = "experience_points")
    public int experiencePoints;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public boolean isGuest() {
        return id.startsWith("guest-");
    }
}
