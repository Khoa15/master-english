package com.doradodo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Table ent_banners: the cards of the Entertainment carousel. Outside starts_at..ends_at (when set) a card is hidden. */
@Entity(tableName = "ent_banners", indices = @Index("position"))
public class BannerEntity {
    public static final String ACTIVITY = "ACTIVITY";
    public static final String CONTEST = "CONTEST";
    public static final String NONE = "NONE";

    @PrimaryKey
    @NonNull
    public String id = "";
    @NonNull
    public String tag = "";
    @NonNull
    public String title = "";
    @Nullable
    public String subtitle;
    @ColumnInfo(name = "button_label")
    @Nullable
    public String buttonLabel;
    @Nullable
    public String image;
    @NonNull
    public String accent = "GREEN";
    /** {@link #ACTIVITY}, {@link #CONTEST} or {@link #NONE}. */
    @ColumnInfo(name = "action_type")
    @NonNull
    public String actionType = NONE;
    /** An activity code or a contest id, depending on action_type. */
    @ColumnInfo(name = "action_target")
    @Nullable
    public String actionTarget;
    /** Only shown to learners of this language (BCP-47); null means every learner. */
    @ColumnInfo(name = "audience_lang")
    @Nullable
    public String audienceLang;
    public int position;
    @ColumnInfo(name = "starts_at")
    @Nullable
    public Long startsAt;
    @ColumnInfo(name = "ends_at")
    @Nullable
    public Long endsAt;
    public boolean enabled = true;
}
