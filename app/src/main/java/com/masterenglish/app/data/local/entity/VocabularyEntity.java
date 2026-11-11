package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.List;

/** Table vocabulary_lessons, seeded from vocabulary.json. */
@Entity(
        tableName = "vocabulary_lessons",
        foreignKeys = @ForeignKey(
                entity = RoadmapNodeEntity.class,
                parentColumns = "id",
                childColumns = "node_id",
                onDelete = ForeignKey.SET_NULL),
        indices = @Index("node_id"))
public class VocabularyEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @ColumnInfo(name = "node_id")
    @Nullable
    public String nodeId;

    @ColumnInfo(name = "word")
    @NonNull
    public String word = "";

    @ColumnInfo(name = "phonetic")
    @Nullable
    public String phonetic;

    @ColumnInfo(name = "part_of_speech")
    @Nullable
    public String partOfSpeech;

    @ColumnInfo(name = "meaning_vi")
    @NonNull
    public String meaningVi = "";

    @ColumnInfo(name = "definition_en")
    @Nullable
    public String definitionEn;

    @ColumnInfo(name = "example_sentence")
    @Nullable
    public String exampleSentence;

    @ColumnInfo(name = "example_translation")
    @Nullable
    public String exampleTranslation;

    @ColumnInfo(name = "audio_url")
    @Nullable
    public String audioUrl;

    @ColumnInfo(name = "image_url")
    @Nullable
    public String imageUrl;

    @ColumnInfo(name = "cefr_level")
    @NonNull
    public String cefrLevel = "A1";

    @ColumnInfo(name = "domain")
    @Nullable
    public String domain;

    @ColumnInfo(name = "synonyms_json")
    @NonNull
    public List<String> synonyms;

    @ColumnInfo(name = "antonyms_json")
    @NonNull
    public List<String> antonyms;

    @ColumnInfo(name = "version")
    public int version = 1;
}
