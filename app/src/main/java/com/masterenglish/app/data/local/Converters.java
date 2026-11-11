package com.masterenglish.app.data.local;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.masterenglish.app.data.local.Enums.BadgeCategory;
import com.masterenglish.app.data.local.Enums.ItemType;
import com.masterenglish.app.data.local.Enums.NodeStatus;
import com.masterenglish.app.data.local.Enums.NodeType;
import com.masterenglish.app.data.local.Enums.ParentType;
import com.masterenglish.app.data.local.Enums.QuestionType;
import com.masterenglish.app.data.local.Enums.Sender;
import com.masterenglish.app.data.local.Enums.SkillType;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Room type converters. JSON columns (ARRAY/JSON in MySQL) are stored as TEXT
 * in SQLite and round-tripped through Gson.
 */
public final class Converters {

    private static final Gson GSON = new Gson();
    private static final Type STRING_LIST = new TypeToken<List<String>>() { }.getType();

    // ----- JSON -----------------------------------------------------------

    @TypeConverter
    public static String fromStringList(List<String> value) {
        return value == null ? "[]" : GSON.toJson(value, STRING_LIST);
    }

    @TypeConverter
    public static List<String> toStringList(String value) {
        if (value == null || value.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> parsed = GSON.fromJson(value, STRING_LIST);
        return parsed == null ? Collections.emptyList() : parsed;
    }

    @TypeConverter
    public static String fromJsonElement(JsonElement value) {
        return value == null ? null : GSON.toJson(value);
    }

    @TypeConverter
    public static JsonElement toJsonElement(String value) {
        return value == null ? null : GSON.fromJson(value, JsonElement.class);
    }

    // ----- Enums (persisted by constant name) -----------------------------

    @TypeConverter
    public static String fromNodeType(NodeType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static NodeType toNodeType(String value) {
        return value == null ? null : NodeType.valueOf(value);
    }

    @TypeConverter
    public static String fromNodeStatus(NodeStatus value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static NodeStatus toNodeStatus(String value) {
        return value == null ? null : NodeStatus.valueOf(value);
    }

    @TypeConverter
    public static String fromSkillType(SkillType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static SkillType toSkillType(String value) {
        return value == null ? null : SkillType.valueOf(value);
    }

    @TypeConverter
    public static String fromParentType(ParentType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static ParentType toParentType(String value) {
        return value == null ? null : ParentType.valueOf(value);
    }

    @TypeConverter
    public static String fromQuestionType(QuestionType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static QuestionType toQuestionType(String value) {
        return value == null ? null : QuestionType.valueOf(value);
    }

    @TypeConverter
    public static String fromItemType(ItemType value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static ItemType toItemType(String value) {
        return value == null ? null : ItemType.valueOf(value);
    }

    @TypeConverter
    public static String fromSender(Sender value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static Sender toSender(String value) {
        return value == null ? null : Sender.valueOf(value);
    }

    @TypeConverter
    public static String fromBadgeCategory(BadgeCategory value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static BadgeCategory toBadgeCategory(String value) {
        return value == null ? null : BadgeCategory.valueOf(value);
    }
}
