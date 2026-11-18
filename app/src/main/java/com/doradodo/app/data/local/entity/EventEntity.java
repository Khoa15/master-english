package com.doradodo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Table ent_events: time-limited events shown as a banner on the Entertainment page. */
@Entity(tableName = "ent_events", indices = @Index({"starts_at", "ends_at"}))
public class EventEntity {
    @PrimaryKey
    @NonNull
    public String id = "";
    @NonNull
    public String tag = "";
    @NonNull
    public String title = "";
    @Nullable
    public String description;
    @Nullable
    public String image;
    @NonNull
    public String accent = "RED";
    /** Only shown to learners of this language (BCP-47); null means every learner. */
    @ColumnInfo(name = "audience_lang")
    @Nullable
    public String audienceLang;
    @ColumnInfo(name = "starts_at")
    public long startsAt;
    @ColumnInfo(name = "ends_at")
    public long endsAt;
    public boolean enabled = true;
}
