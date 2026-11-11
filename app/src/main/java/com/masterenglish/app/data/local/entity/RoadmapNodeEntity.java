package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.masterenglish.app.data.local.Enums.NodeType;

/** Table roadmap_nodes: one clickable lesson on the winding path. */
@Entity(
        tableName = "roadmap_nodes",
        foreignKeys = @ForeignKey(
                entity = RoadmapStageEntity.class,
                parentColumns = "id",
                childColumns = "stage_id",
                onDelete = ForeignKey.CASCADE),
        indices = @Index("stage_id"))
public class RoadmapNodeEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @ColumnInfo(name = "stage_id")
    public int stageId;

    @ColumnInfo(name = "title")
    @NonNull
    public String title = "";

    /** Short description shown in the lesson preview sheet. */
    @ColumnInfo(name = "summary")
    @NonNull
    public String summary = "";

    @ColumnInfo(name = "node_type")
    @NonNull
    public NodeType nodeType = NodeType.VOCABULARY;

    @ColumnInfo(name = "position_index")
    public int positionIndex;

    /** Name of a drawable resource used as the skill icon, e.g. ic_skill_vocab. */
    @ColumnInfo(name = "icon_res_name")
    @NonNull
    public String iconResName = "";
}
