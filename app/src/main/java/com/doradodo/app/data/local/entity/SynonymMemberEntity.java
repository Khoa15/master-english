package com.doradodo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Table synonym_members: one word of a synonym group, in the language of that group. {@code wordId} links it to
 * the app's own vocabulary when the word exists there (that is how the game prefers words the learner knows).
 */
@Entity(
        tableName = "synonym_members",
        primaryKeys = {"group_id", "text"},
        foreignKeys = {
                @ForeignKey(entity = SynonymGroupEntity.class, parentColumns = "id",
                        childColumns = "group_id", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = WordEntity.class, parentColumns = "id",
                        childColumns = "word_id", onDelete = ForeignKey.SET_NULL)},
        indices = {@Index("text"), @Index("word_id")})
public class SynonymMemberEntity {
    @ColumnInfo(name = "group_id")
    public long groupId;
    @NonNull
    public String text = "";
    @ColumnInfo(name = "word_id")
    @Nullable
    public String wordId;
}
