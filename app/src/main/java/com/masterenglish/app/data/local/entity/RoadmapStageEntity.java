package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Table roadmap_stages: one stage per CEFR level. */
@Entity(tableName = "roadmap_stages")
public class RoadmapStageEntity {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "stage_number")
    public int stageNumber;

    @ColumnInfo(name = "title")
    @NonNull
    public String title = "";

    @ColumnInfo(name = "description")
    @NonNull
    public String description = "";

    @ColumnInfo(name = "min_exp_required")
    public int minExpRequired;
}
