package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.JsonElement;

/** Table listening_lessons, seeded from listening.json. */
@Entity(
        tableName = "listening_lessons",
        foreignKeys = @ForeignKey(
                entity = RoadmapNodeEntity.class,
                parentColumns = "id",
                childColumns = "node_id",
                onDelete = ForeignKey.SET_NULL),
        indices = @Index("node_id"))
public class ListeningEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @ColumnInfo(name = "node_id")
    @Nullable
    public String nodeId;

    @ColumnInfo(name = "title")
    @NonNull
    public String title = "";

    @ColumnInfo(name = "context")
    @Nullable
    public String context;

    /** Raw resource name, e.g. "raw/listen_001". Resolved to android.resource:// at runtime. */
    @ColumnInfo(name = "audio_url")
    @NonNull
    public String audioUrl = "";

    /** JSON array of {"speaker": "...", "text": "..."}. */
    @ColumnInfo(name = "transcript")
    @NonNull
    public JsonElement transcript;

    @ColumnInfo(name = "duration_seconds")
    public int durationSeconds;

    @ColumnInfo(name = "cefr_level")
    @NonNull
    public String cefrLevel = "A1";

    @ColumnInfo(name = "domain")
    @Nullable
    public String domain;

    @ColumnInfo(name = "version")
    public int version = 1;
}
