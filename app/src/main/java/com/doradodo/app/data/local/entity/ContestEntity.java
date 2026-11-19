package com.doradodo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Table ent_contests. Its state is never stored: it follows from the four timestamps
 * (registration_opens_at <= registration_closes_at <= starts_at < ends_at) and the current time.
 */
@Entity(tableName = "ent_contests", indices = {@Index({"starts_at", "ends_at"})})
public class ContestEntity {
    @PrimaryKey
    @NonNull
    public String id = "";
    @NonNull
    public String title = "";
    @Nullable
    public String description;
    @Nullable
    public String image;
    @NonNull
    public String accent = "AMBER";
    @ColumnInfo(name = "registration_opens_at")
    public long registrationOpensAt;
    @ColumnInfo(name = "registration_closes_at")
    public long registrationClosesAt;
    @ColumnInfo(name = "starts_at")
    public long startsAt;
    @ColumnInfo(name = "ends_at")
    public long endsAt;
    @ColumnInfo(name = "question_count")
    public int questionCount;
    @ColumnInfo(name = "duration_minutes")
    public int durationMinutes;
    public boolean enabled = true;
}
