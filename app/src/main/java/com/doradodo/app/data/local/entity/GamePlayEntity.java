package com.doradodo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Table ent_game_plays: one row each time a learner opens an activity. Feeds the "learners" counter. */
@Entity(
        tableName = "ent_game_plays",
        foreignKeys = @ForeignKey(entity = UserEntity.class, parentColumns = "id",
                childColumns = "user_id", onDelete = ForeignKey.CASCADE),
        indices = {@Index("user_id"), @Index("activity_code")})
public class GamePlayEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    @ColumnInfo(name = "user_id")
    @NonNull
    public String userId = "";
    @ColumnInfo(name = "activity_code")
    @NonNull
    public String activityCode = "";
    @ColumnInfo(name = "played_at")
    public long playedAt;
}
