package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;

/** A short meaning of one sense in one language (BCP-47 code, e.g. "vi"). Keyed by the sense's word and position, so it survives senses being rebuilt. */
@Entity(
        tableName = "sense_translations",
        primaryKeys = {"word_id", "sense_position", "lang"},
        foreignKeys = @ForeignKey(entity = WordEntity.class, parentColumns = "id",
                childColumns = "word_id", onDelete = ForeignKey.CASCADE))
public class SenseTranslationEntity {
    @ColumnInfo(name = "word_id")
    @NonNull
    public String wordId = "";

    @ColumnInfo(name = "sense_position")
    public int sensePosition;

    @ColumnInfo(name = "lang")
    @NonNull
    public String lang = "";

    @ColumnInfo(name = "text")
    @NonNull
    public String text = "";

}
