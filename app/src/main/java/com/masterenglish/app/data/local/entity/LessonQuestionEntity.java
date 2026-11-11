package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.google.gson.JsonElement;
import com.masterenglish.app.data.local.Enums.ParentType;
import com.masterenglish.app.data.local.Enums.QuestionType;

/**
 * Table lesson_questions, shared by the four skills. Polymorphic parent
 * (parent_type + parent_id) points at a vocabulary/grammar/reading/listening row.
 */
@Entity(
        tableName = "lesson_questions",
        indices = @Index(value = {"parent_type", "parent_id"}))
public class LessonQuestionEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @ColumnInfo(name = "parent_type")
    @NonNull
    public ParentType parentType = ParentType.VOCABULARY;

    @ColumnInfo(name = "parent_id")
    @NonNull
    public String parentId = "";

    @ColumnInfo(name = "question_type")
    @NonNull
    public QuestionType questionType = QuestionType.MULTIPLE_CHOICE;

    @ColumnInfo(name = "prompt")
    @NonNull
    public String prompt = "";

    @ColumnInfo(name = "media_url")
    @Nullable
    public String mediaUrl;

    /**
     * JSON array. MULTIPLE_CHOICE / FILL_BLANK / REORDER: strings.
     * MATCHING: array of {"left": "...", "right": "..."}.
     */
    @ColumnInfo(name = "options_json")
    @NonNull
    public JsonElement options;

    /**
     * JSON answer. MULTIPLE_CHOICE: option text or "A".."D"; FILL_BLANK / REORDER / VOICE_INPUT:
     * string; MATCHING: the same array as options.
     */
    @ColumnInfo(name = "correct_answer_json")
    @NonNull
    public JsonElement correctAnswer;

    @ColumnInfo(name = "explanation")
    @Nullable
    public String explanation;

    @ColumnInfo(name = "points")
    public int points = 10;

    /** error_tag recorded in user_weaknesses when the answer is wrong (e.g. grammar slug). */
    @ColumnInfo(name = "skill_tag")
    @NonNull
    public String skillTag = "";
}
