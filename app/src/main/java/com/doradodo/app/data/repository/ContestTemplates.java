package com.doradodo.app.data.repository;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dagger.hilt.android.qualifiers.ApplicationContext;

import javax.inject.Inject;
import javax.inject.Singleton;

/** The weekly contests the app runs, read once from {@code contest_templates} in assets/data/entertainment.json. */
@Singleton
public class ContestTemplates {

    private static final String TAG = "ContestTemplates";
    private static final List<String> DAYS = Arrays.asList("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");

    private final Context context;
    private List<ContestTemplate> templates;

    @Inject
    public ContestTemplates(@ApplicationContext Context context) {
        this.context = context;
    }

    public synchronized List<ContestTemplate> all() {
        if (templates == null) {
            try (InputStream stream = context.getAssets().open("data/entertainment.json");
                 InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                templates = parse(reader);
            } catch (IOException e) {
                Log.e(TAG, "Could not read the weekly contests", e);
                templates = new ArrayList<>();
            }
        }
        return templates;
    }

    static List<ContestTemplate> parse(String json) {
        return parse(new java.io.StringReader(json));
    }

    private static List<ContestTemplate> parse(java.io.Reader reader) {
        File file = new Gson().fromJson(reader, File.class);
        List<ContestTemplate> out = new ArrayList<>();
        if (file == null || file.templates == null) {
            return out;
        }
        for (Json j : file.templates) {
            ContestTemplate t = new ContestTemplate(j.id, minute(j.schedule.registrationOpens),
                    minute(j.schedule.registrationCloses), minute(j.schedule.starts), minute(j.schedule.ends));
            t.title = j.title;
            t.description = j.description;
            t.image = j.image;
            if (j.accent != null) {
                t.accent = j.accent;
            }
            t.questionCount = j.questionCount;
            t.durationMinutes = j.durationMinutes;
            t.packs = j.packs == null ? new ArrayList<>() : j.packs;
            for (List<Integer> p : j.prizes == null ? new ArrayList<List<Integer>>() : j.prizes) {
                t.prizes.add(new int[]{p.get(0), p.get(1) == null ? 0 : p.get(1), p.get(2)});
            }
            out.add(t);
        }
        return out;
    }

    /** "SAT 13:00" -> minutes from Monday 00:00. */
    static int minute(String slot) {
        String[] parts = slot.trim().split("\\s+");
        int day = DAYS.indexOf(parts[0].toUpperCase());
        if (day < 0 || parts.length != 2) {
            throw new IllegalArgumentException("Bad contest slot: " + slot);
        }
        String[] time = parts[1].split(":");
        return day * 24 * 60 + Integer.parseInt(time[0]) * 60 + Integer.parseInt(time[1]);
    }

    private static final class File {
        @SerializedName("contest_templates") List<Json> templates;
    }

    private static final class Json {
        String id;
        String title;
        String description;
        String image;
        String accent;
        Schedule schedule;
        @SerializedName("question_count") int questionCount;
        @SerializedName("duration_minutes") int durationMinutes;
        List<String> packs;
        List<List<Integer>> prizes;
    }

    private static final class Schedule {
        @SerializedName("registration_opens") String registrationOpens;
        @SerializedName("registration_closes") String registrationCloses;
        String starts;
        String ends;
    }
}
