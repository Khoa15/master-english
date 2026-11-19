package com.doradodo.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.doradodo.app.data.local.entity.ContestEntity;

import java.time.Instant;
import java.util.List;

import org.junit.Test;

public class ContestScheduleTest {

    /**
     * The weekly slot, in UTC: registration opens Monday 00:00 and closes Saturday 12:50, the contest starts
     * Saturday 13:00 and ends Sunday 13:00.
     */
    private static ContestTemplate template() {
        int day = 24 * 60;
        return new ContestTemplate("weekly", 0, 5 * day + 12 * 60 + 50, 5 * day + 13 * 60, 6 * day + 13 * 60);
    }

    private static long at(String iso) {
        return Instant.parse(iso).toEpochMilli();
    }

    private static List<ContestEntity> occurrences(String now) {
        return ContestSchedule.occurrences(template(), at(now), 2);
    }

    @Test
    public void midWeekGivesThisWeekAndNextWeek() {
        // Wednesday 2026-11-18, ISO week 47.
        List<ContestEntity> c = occurrences("2026-11-18T09:00:00Z");
        assertEquals(2, c.size());
        assertEquals("weekly@2026-W47", c.get(0).id);
        assertEquals("weekly@2026-W48", c.get(1).id);
        assertEquals(at("2026-11-21T13:00:00Z"), c.get(0).startsAt);
        assertEquals(at("2026-11-22T13:00:00Z"), c.get(0).endsAt);
        assertEquals(at("2026-11-16T00:00:00Z"), c.get(0).registrationOpensAt);
        assertEquals(at("2026-11-21T12:50:00Z"), c.get(0).registrationClosesAt);
        assertEquals(at("2026-11-28T13:00:00Z"), c.get(1).startsAt);
    }

    @Test
    public void whileTheContestIsLiveItIsStillFirst() {
        List<ContestEntity> c = occurrences("2026-11-22T08:00:00Z");
        assertEquals("weekly@2026-W47", c.get(0).id);
        assertEquals("weekly@2026-W48", c.get(1).id);
        assertEquals(ContestState.LIVE, ContestRules.stateOf(c.get(0), at("2026-11-22T08:00:00Z")));
    }

    @Test
    public void afterTheContestEndedTheNextTwoWeeksFollow() {
        List<ContestEntity> c = occurrences("2026-11-22T13:00:00Z");
        assertEquals(2, c.size());
        assertEquals("weekly@2026-W48", c.get(0).id);
        assertEquals("weekly@2026-W49", c.get(1).id);
    }

    @Test
    public void theIdFollowsTheIsoWeekAcrossTheYearEnd() {
        // 2026 has 53 ISO weeks; week 53 runs 2026-12-28 .. 2027-01-03.
        List<ContestEntity> c = occurrences("2026-12-30T10:00:00Z");
        assertEquals("weekly@2026-W53", c.get(0).id);
        assertEquals("weekly@2027-W01", c.get(1).id);
        assertEquals(at("2027-01-09T13:00:00Z"), c.get(1).startsAt);
    }

    @Test
    public void everyOccurrenceIsOneWeekApartAndInOrder() {
        List<ContestEntity> c = ContestSchedule.occurrences(template(), at("2026-11-18T09:00:00Z"), 4);
        assertEquals(4, c.size());
        for (int i = 1; i < c.size(); i++) {
            assertEquals(7L * 24 * 3600 * 1000, c.get(i).startsAt - c.get(i - 1).startsAt);
        }
    }

    @Test
    public void theContestCarriesItsDescription() {
        ContestTemplate t = template();
        t.title = "Weekly Cup";
        t.accent = "AMBER";
        t.questionCount = 25;
        t.durationMinutes = 10;
        ContestEntity c = ContestSchedule.occurrences(t, at("2026-11-18T09:00:00Z"), 1).get(0);
        assertEquals("Weekly Cup", c.title);
        assertEquals("AMBER", c.accent);
        assertEquals(25, c.questionCount);
        assertEquals(10, c.durationMinutes);
        assertTrue(c.enabled);
    }
}
