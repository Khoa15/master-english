package com.doradodo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;

/** The translation of one example sentence into one language. */
@Entity(
        tableName = "example_translations",
        primaryKeys = {"word_id", "sense_position", "example_position", "lang"},
        foreignKeys = @ForeignKey(entity = WordEntity.class, parentColumns = "id",
                childColumns = "word_id", onDelete = ForeignKey.CASCADE))
public class ExampleTranslationEntity {
    @ColumnInfo(name = "word_id")
    @NonNull
    public String wordId = "";

    @ColumnInfo(name = "sense_position")
    public int sensePosition;

    @ColumnInfo(name = "example_position")
    public int examplePosition;

    @ColumnInfo(name = "lang")
    @NonNull
    public String lang = "";

    @ColumnInfo(name = "text")
    @NonNull
    public String text = "";

}
