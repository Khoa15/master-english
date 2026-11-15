package com.masterenglish.app.data.local;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.masterenglish.app.data.local.Enums.BadgeCategory;

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
    public static String fromBadgeCategory(BadgeCategory value) {
        return value == null ? null : value.name();
    }

    @TypeConverter
    public static BadgeCategory toBadgeCategory(String value) {
        return value == null ? null : BadgeCategory.valueOf(value);
    }
}
