package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Table packs: a named set of words. Today there is one pack per CEFR level, with the level as its id. */
@Entity(tableName = "packs")
public class PackEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @NonNull
    public String title = "";

    @ColumnInfo(name = "cefr_level")
    @NonNull
    public String cefrLevel = "A1";

    public int position;
}
