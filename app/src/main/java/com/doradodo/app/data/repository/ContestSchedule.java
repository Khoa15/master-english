package com.doradodo.app.data.repository;

import com.doradodo.app.data.local.entity.ContestEntity;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/** Turns a weekly {@link ContestTemplate} into the dated contests that are not over yet. Everything is UTC. */
public final class ContestSchedule {

    private ContestSchedule() {
    }

    /**
     * The first {@code count} weekly contests that have not ended at {@code nowMillis}, soonest first. The id is
     * {@code <template id>@<ISO year>-W<ISO week>}, so the same week always gives the same contest.
     */
    public static List<ContestEntity> occurrences(ContestTemplate template, long nowMillis, int count) {
        ZonedDateTime now = Instant.ofEpochMilli(nowMillis).atZone(ZoneOffset.UTC);
        ZonedDateTime monday = now.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay(ZoneOffset.UTC);
        List<ContestEntity> out = new ArrayList<>();
        // Start one week back so a contest that runs past Sunday midnight is still found.
        for (int week = -1; out.size() < count; week++) {
            ZonedDateTime weekStart = monday.plusWeeks(week);
            ContestEntity c = occurrence(template, weekStart);
            if (c.endsAt > nowMillis) {
                out.add(c);
            }
        }
        return out;
    }

    private static ContestEntity occurrence(ContestTemplate t, ZonedDateTime weekStart) {
        LocalDate date = weekStart.toLocalDate();
        ContestEntity c = new ContestEntity();
        c.id = String.format("%s@%d-W%02d", t.id, date.get(IsoFields.WEEK_BASED_YEAR),
                date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        c.title = t.title;
        c.description = t.description;
        c.image = t.image;
        c.accent = t.accent;
        c.registrationOpensAt = millis(weekStart, t.registrationOpensMinute);
        c.registrationClosesAt = millis(weekStart, t.registrationClosesMinute);
        c.startsAt = millis(weekStart, t.startsMinute);
        c.endsAt = millis(weekStart, t.endsMinute);
        c.questionCount = t.questionCount;
        c.durationMinutes = t.durationMinutes;
        return c;
    }

    private static long millis(ZonedDateTime weekStart, int minute) {
        return weekStart.plus(minute, ChronoUnit.MINUTES).toInstant().toEpochMilli();
    }
}
