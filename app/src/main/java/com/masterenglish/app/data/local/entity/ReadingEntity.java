package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.List;

/** Table reading_lessons, seeded from reading.json. */
@Entity(
        tableName = "reading_lessons",
        foreignKeys = @ForeignKey(
                entity = RoadmapNodeEntity.class,
                parentColumns = "id",
                childColumns = "node_id",
                onDelete = ForeignKey.SET_NULL),
        indices = @Index("node_id"))
public class ReadingEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @ColumnInfo(name = "node_id")
    @Nullable
    public String nodeId;

    @ColumnInfo(name = "title")
    @NonNull
    public String title = "";

    @ColumnInfo(name = "passage_text")
    @NonNull
    public String passageText = "";

    @ColumnInfo(name = "word_count")
    public int wordCount;

    @ColumnInfo(name = "key_vocabulary_json")
    @NonNull
    public List<String> keyVocabulary;

    @ColumnInfo(name = "cefr_level")
    @NonNull
    public String cefrLevel = "A1";

    @ColumnInfo(name = "domain")
    @Nullable
    public String domain;

    @ColumnInfo(name = "version")
    public int version = 1;
}
