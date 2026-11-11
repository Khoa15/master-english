package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.masterenglish.app.data.local.Enums.NodeStatus;

/** Table user_node_progress. Unique per (user, node). */
@Entity(
        tableName = "user_node_progress",
        foreignKeys = {
                @ForeignKey(entity = UserEntity.class, parentColumns = "id",
                        childColumns = "user_id", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = RoadmapNodeEntity.class, parentColumns = "id",
                        childColumns = "node_id", onDelete = ForeignKey.CASCADE)
        },
        indices = {
                @Index(value = {"user_id", "node_id"}, unique = true),
                @Index("node_id")
        })
public class UserNodeProgressEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @ColumnInfo(name = "user_id")
    @NonNull
    public String userId = "";

    @ColumnInfo(name = "node_id")
    @NonNull
    public String nodeId = "";

    @ColumnInfo(name = "status")
    @NonNull
    public NodeStatus status = NodeStatus.LOCKED;

    @ColumnInfo(name = "stars_earned")
    public int starsEarned;

    @ColumnInfo(name = "completed_at")
    @Nullable
    public Long completedAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}
