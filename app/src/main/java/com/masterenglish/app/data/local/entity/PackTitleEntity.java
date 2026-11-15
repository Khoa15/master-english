package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;

/** The title of a pack in one language. packs.title stays the English one. */
@Entity(
        tableName = "pack_titles",
        primaryKeys = {"pack_id", "lang"},
        foreignKeys = @ForeignKey(entity = PackEntity.class, parentColumns = "id",
                childColumns = "pack_id", onDelete = ForeignKey.CASCADE))
public class PackTitleEntity {
    @ColumnInfo(name = "pack_id")
    @NonNull
    public String packId = "";

    @ColumnInfo(name = "lang")
    @NonNull
    public String lang = "";

    @ColumnInfo(name = "text")
    @NonNull
    public String text = "";

}
