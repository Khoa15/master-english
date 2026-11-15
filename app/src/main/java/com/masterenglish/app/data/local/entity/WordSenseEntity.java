package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Table word_senses: the meanings of a word, in display order. Position 0 is the one shown first. */
@Entity(
        tableName = "word_senses",
        foreignKeys = @ForeignKey(entity = WordEntity.class, parentColumns = "id",
                childColumns = "word_id", onDelete = ForeignKey.CASCADE),
        indices = @Index(value = {"word_id", "position"}, unique = true))
public class WordSenseEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "word_id")
    @NonNull
    public String wordId = "";

    public int position;

    /** Part of speech: noun, verb, adjective, ... */
    @Nullable
    public String pos;

    @ColumnInfo(name = "definition_en")
    @NonNull
    public String definitionEn = "";

    @ColumnInfo(name = "example_en")
    @Nullable
    public String exampleEn;

    @ColumnInfo(name = "definition_vi")
    @Nullable
    public String definitionVi;
}
