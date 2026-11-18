package com.doradodo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Table levels: the proficiency levels of every level system, in order. A system is how a language grades its
 * words: CEFR (A1..C2) for European languages, TOPIK (1..6) for Korean, HSK for Chinese, THEME for thematic
 * packs that have no level. {@code position} orders the levels of one system, easiest first.
 */
@Entity(tableName = "levels", primaryKeys = {"system", "code"})
public class LevelEntity {
    @NonNull
    public String system = "CEFR";
    @NonNull
    public String code = "";
    public int position;
}
