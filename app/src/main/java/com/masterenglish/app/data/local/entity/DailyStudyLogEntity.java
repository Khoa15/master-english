package com.masterenglish.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Table daily_study_log (added to the MySQL schema): minutes studied per local day. */
@Entity(
        tableName = "daily_study_log",
        foreignKeys = @ForeignKey(entity = UserEntity.class, parentColumns = "id",
                childColumns = "user_id", onDelete = ForeignKey.CASCADE),
        indices = @Index(value = {"user_id", "date_key"}, unique = true))
public class DailyStudyLogEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "user_id")
    @NonNull
    public String userId = "";

    /** ISO date, yyyy-MM-dd. */
    @ColumnInfo(name = "date_key")
    @NonNull
    public String dateKey = "";

    @ColumnInfo(name = "minutes")
    public int minutes;
}
