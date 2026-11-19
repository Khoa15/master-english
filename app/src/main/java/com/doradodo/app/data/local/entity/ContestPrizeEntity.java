package com.doradodo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Table ent_contest_prizes: a reward for a range of ranks. rank_to is null for "from rank_from onwards". */
@Entity(
        tableName = "ent_contest_prizes",
        primaryKeys = {"contest_id", "rank_from"},
        foreignKeys = @ForeignKey(entity = ContestEntity.class, parentColumns = "id",
                childColumns = "contest_id", onDelete = ForeignKey.CASCADE))
public class ContestPrizeEntity {
    @ColumnInfo(name = "contest_id")
    @NonNull
    public String contestId = "";
    @ColumnInfo(name = "rank_from")
    public int rankFrom;
    @ColumnInfo(name = "rank_to")
    @Nullable
    public Integer rankTo;
    @ColumnInfo(name = "reward_gems")
    public int rewardGems;
}
