package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

/** Table pack_words: which words are in which pack, in order. A word can be in several packs. */
@Entity(
        tableName = "pack_words",
        primaryKeys = {"pack_id", "word_id"},
        foreignKeys = {
                @ForeignKey(entity = PackEntity.class, parentColumns = "id",
                        childColumns = "pack_id", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = WordEntity.class, parentColumns = "id",
                        childColumns = "word_id", onDelete = ForeignKey.CASCADE)},
        indices = {@Index("word_id"), @Index(value = {"pack_id", "position"})})
public class PackWordEntity {
    @ColumnInfo(name = "pack_id")
    @NonNull
    public String packId = "";

    @ColumnInfo(name = "word_id")
    @NonNull
    public String wordId = "";

    public int position;
}
