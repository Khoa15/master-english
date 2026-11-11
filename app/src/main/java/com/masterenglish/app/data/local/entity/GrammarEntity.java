package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.JsonElement;

/** Table grammar_lessons, seeded from grammar.json. */
@Entity(
        tableName = "grammar_lessons",
        foreignKeys = @ForeignKey(
                entity = RoadmapNodeEntity.class,
                parentColumns = "id",
                childColumns = "node_id",
                onDelete = ForeignKey.SET_NULL),
        indices = @Index("node_id"))
public class GrammarEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @ColumnInfo(name = "node_id")
    @Nullable
    public String nodeId;

    @ColumnInfo(name = "topic_name")
    @NonNull
    public String topicName = "";

    /** Vietnamese explanation followed by usage context (Markdown). */
    @ColumnInfo(name = "rule_explanation_markdown")
    @NonNull
    public String ruleExplanationMarkdown = "";

    @ColumnInfo(name = "formula")
    @Nullable
    public String formula;

    /** JSON object: {"incorrect": "...", "correct": "...", "reason_vi": "..."}. */
    @ColumnInfo(name = "common_mistakes_json")
    @Nullable
    public JsonElement commonMistakes;

    /** JSON array of {"en": "...", "vi": "..."}. */
    @ColumnInfo(name = "examples_json")
    @Nullable
    public JsonElement examples;

    @ColumnInfo(name = "cefr_level")
    @NonNull
    public String cefrLevel = "A1";

    @ColumnInfo(name = "domain")
    @Nullable
    public String domain;

    @ColumnInfo(name = "version")
    public int version = 1;
}
