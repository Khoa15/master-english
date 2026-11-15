package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;

/** An example sentence of one sense, in display order. A sense can have any number of them. */
@Entity(
        tableName = "sense_examples",
        primaryKeys = {"word_id", "sense_position", "position"},
        foreignKeys = @ForeignKey(entity = WordEntity.class, parentColumns = "id",
                childColumns = "word_id", onDelete = ForeignKey.CASCADE))
public class SenseExampleEntity {
    @ColumnInfo(name = "word_id")
    @NonNull
    public String wordId = "";

    @ColumnInfo(name = "sense_position")
    public int sensePosition;

    @ColumnInfo(name = "position")
    public int position;

    @ColumnInfo(name = "text")
    @NonNull
    public String text = "";

}
