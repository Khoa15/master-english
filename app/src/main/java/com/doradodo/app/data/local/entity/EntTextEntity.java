package com.doradodo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Table ent_texts: the text of an Entertainment row in one language (activity, banner, event or contest title,
 * subtitle, description...). The columns on the row itself hold the default (English) text; a row here overrides
 * it for a learner whose display language matches {@code lang}.
 */
@Entity(tableName = "ent_texts", primaryKeys = {"owner_type", "owner_id", "field", "lang"},
        indices = @Index("lang"))
public class EntTextEntity {
    /** ACTIVITY, BANNER, EVENT or CONTEST. */
    @ColumnInfo(name = "owner_type")
    @NonNull
    public String ownerType = "";
    @ColumnInfo(name = "owner_id")
    @NonNull
    public String ownerId = "";
    /** title, subtitle, description, tag or button_label. */
    @NonNull
    public String field = "";
    @NonNull
    public String lang = "";
    @NonNull
    public String text = "";
}
