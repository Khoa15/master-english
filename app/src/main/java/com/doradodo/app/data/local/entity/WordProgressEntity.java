package com.doradodo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

/** Table word_progress: the learner's SM-2 card and answer counts for one word. One row per (user, word). */
@Entity(
        tableName = "word_progress",
        primaryKeys = {"user_id", "word_id"},
        foreignKeys = {
                @ForeignKey(entity = UserEntity.class, parentColumns = "id",
                        childColumns = "user_id", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = WordEntity.class, parentColumns = "id",
                        childColumns = "word_id", onDelete = ForeignKey.CASCADE)},
        indices = {@Index("word_id"), @Index(value = {"user_id", "next_review_at"})})
public class WordProgressEntity {
    @ColumnInfo(name = "user_id")
    @NonNull
    public String userId = "";

    @ColumnInfo(name = "word_id")
    @NonNull
    public String wordId = "";

    /** SM-2 easiness factor, starts at 2.5 and never drops below 1.3. */
    @ColumnInfo(name = "ease_factor")
    public double easeFactor = 2.5;

    /** Correct reviews in a row; a wrong answer resets it to 0. */
    public int repetitions;

    @ColumnInfo(name = "interval_days")
    public int intervalDays = 1;

    @ColumnInfo(name = "next_review_at")
    public long nextReviewAt;

    @ColumnInfo(name = "last_reviewed_at")
    public long lastReviewedAt;

    @ColumnInfo(name = "times_correct")
    public int timesCorrect;

    @ColumnInfo(name = "times_wrong")
    public int timesWrong;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
}
