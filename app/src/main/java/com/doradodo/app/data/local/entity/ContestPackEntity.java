package com.doradodo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Table ent_contest_packs: the word packs a contest draws its questions from. */
@Entity(
        tableName = "ent_contest_packs",
        primaryKeys = {"contest_id", "pack_id"},
        foreignKeys = {
                @ForeignKey(entity = ContestEntity.class, parentColumns = "id",
                        childColumns = "contest_id", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = PackEntity.class, parentColumns = "id",
                        childColumns = "pack_id", onDelete = ForeignKey.CASCADE)},
        indices = @Index("pack_id"))
public class ContestPackEntity {
    @ColumnInfo(name = "contest_id")
    @NonNull
    public String contestId = "";
    @ColumnInfo(name = "pack_id")
    @NonNull
    public String packId = "";
    public int position;
}
