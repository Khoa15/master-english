package com.doradodo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Table words: one row per headword of one language. {@code lang} is the language the word belongs to (BCP-47).
 * Ids of English words are the word itself ("bed"); words of any other language use "&lt;lang&gt;:&lt;slug&gt;"
 * ("ko:annyeong"), so ids never collide across languages. The level is a pair (level_system, level_code) that
 * points at the levels table, so every language can use its own grading.
 */
@Entity(
        tableName = "words",
        foreignKeys = @ForeignKey(entity = LevelEntity.class, parentColumns = {"system", "code"},
                childColumns = {"level_system", "level_code"}),
        indices = {@Index({"lang", "term"}), @Index({"level_system", "level_code", "rank"})})
public class WordEntity {
    @PrimaryKey
    @NonNull
    public String id = "";
    @NonNull
    public String term = "";
    @Nullable
    public String ipa;
    @NonNull
    public String lang = "en";
    @ColumnInfo(name = "level_system")
    @NonNull
    public String levelSystem = "CEFR";
    @ColumnInfo(name = "level_code")
    @NonNull
    public String levelCode = "A1";
    /** Position of the word inside its level, starting at 1. */
    public int rank;
}
