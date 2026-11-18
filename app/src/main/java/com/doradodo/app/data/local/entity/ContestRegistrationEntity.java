package com.doradodo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Table ent_contest_registrations: which learner registered for which contest. */
@Entity(
        tableName = "ent_contest_registrations",
        primaryKeys = {"contest_id", "user_id"},
        foreignKeys = {
                @ForeignKey(entity = ContestEntity.class, parentColumns = "id",
                        childColumns = "contest_id", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = UserEntity.class, parentColumns = "id",
                        childColumns = "user_id", onDelete = ForeignKey.CASCADE)},
        indices = @Index("user_id"))
public class ContestRegistrationEntity {
    @ColumnInfo(name = "contest_id")
    @NonNull
    public String contestId = "";
    @ColumnInfo(name = "user_id")
    @NonNull
    public String userId = "";
    @ColumnInfo(name = "registered_at")
    public long registeredAt;
}
