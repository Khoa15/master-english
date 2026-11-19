package com.doradodo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Table packs: a named set of words of one language, at one level. */
@Entity(
        tableName = "packs",
        foreignKeys = @ForeignKey(entity = LevelEntity.class, parentColumns = {"system", "code"},
                childColumns = {"level_system", "level_code"}),
        indices = {@Index("lang"), @Index({"level_system", "level_code"})})
public class PackEntity {
    @PrimaryKey
    @NonNull
    public String id = "";
    @NonNull
    public String title = "";
    @NonNull
    public String lang = "en";
    @ColumnInfo(name = "level_system")
    @NonNull
    public String levelSystem = "CEFR";
    @ColumnInfo(name = "level_code")
    @NonNull
    public String levelCode = "A1";
    public int position;
}
