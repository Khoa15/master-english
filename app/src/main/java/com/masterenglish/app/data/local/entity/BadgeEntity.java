package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.JsonElement;
import com.masterenglish.app.data.local.Enums.BadgeCategory;

/** Table badges: static catalogue of achievements. */
@Entity(tableName = "badges")
public class BadgeEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @ColumnInfo(name = "title")
    @NonNull
    public String title = "";

    @ColumnInfo(name = "description")
    @NonNull
    public String description = "";

    /** Drawable resource name for the badge artwork. */
    @ColumnInfo(name = "icon_url")
    @NonNull
    public String iconUrl = "";

    @ColumnInfo(name = "category")
    @NonNull
    public BadgeCategory category = BadgeCategory.STREAK;

    /** JSON, e.g. {"type": "STREAK", "days": 30}. Evaluated by BadgeEvaluator. */
    @ColumnInfo(name = "criteria_json")
    @NonNull
    public JsonElement criteria;
}
