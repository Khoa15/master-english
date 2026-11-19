package com.doradodo.app.data.local.entity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Table ent_activities: one row per game or practice mode, display data only. The rules and screens live in
 * the app; {@code code} is the key the app maps to a screen ({@code WORD_MATCH}, {@code MISTAKES}, ...).
 */
@Entity(tableName = "ent_activities", indices = {@Index(value = "code", unique = true), @Index({"section", "position"})})
public class ActivityEntity {
    public static final String GAME = "GAME";
    public static final String PRACTICE = "PRACTICE";

    @PrimaryKey
    @NonNull
    public String id = "";
    /** {@link #GAME} or {@link #PRACTICE}. */
    @NonNull
    public String section = GAME;
    @NonNull
    public String code = "";
    @NonNull
    public String title = "";
    @Nullable
    public String subtitle;
    /** Name of a drawable resource. */
    @Nullable
    public String image;
    /** GREEN, BLUE, PURPLE or AMBER. */
    @NonNull
    public String accent = "GREEN";
    /**
     * Languages this game or mode is designed for, as BCP-47 codes. It is only offered to a learner whose display
     * language is in the list: a game built around English spelling or English synonyms does not suit another language.
     */
    @ColumnInfo(name = "design_for", defaultValue = "'[\"en\"]'")
    @NonNull
    public List<String> designFor = new ArrayList<>(Collections.singletonList("en"));
    public int position;
    public boolean enabled = true;
}
