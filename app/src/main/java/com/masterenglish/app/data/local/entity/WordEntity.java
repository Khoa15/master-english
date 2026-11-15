package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Table words: one row per headword. {@code cefrLevel} is the lowest level the word is listed at;
 * the packs it belongs to are in pack_words (a word tagged B2/C1 is in both packs but is still one row).
 */
@Entity(tableName = "words", indices = {@Index("term"), @Index({"cefr_level", "rank"})})
public class WordEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @NonNull
    public String term = "";

    @Nullable
    public String ipa;

    @ColumnInfo(name = "cefr_level")
    @NonNull
    public String cefrLevel = "A1";

    /** Position of the word inside its level, starting at 1. */
    public int rank;
}
