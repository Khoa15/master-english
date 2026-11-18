package com.doradodo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Table synonym_groups: a set of words of ONE language that mean the same or nearly the same (go / travel /
 * proceed, or go / went). Two words are synonyms when they share a group. The language belongs to the group,
 * so a group can never mix languages: synonymy is within a language, and a translation (go / đi) is not one.
 */
@Entity(tableName = "synonym_groups", indices = @Index("lang"))
public class SynonymGroupEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    /** BCP-47 code of the language every member is written in. */
    @NonNull
    public String lang = "en";
    /** Optional remark, e.g. a difference in tone. */
    @Nullable
    public String note;
}
