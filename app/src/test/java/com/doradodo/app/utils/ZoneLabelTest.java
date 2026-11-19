package com.doradodo.app.utils;

import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneId;

import org.junit.Test;

public class ZoneLabelTest {

    private static final Instant JANUARY = Instant.parse("2026-01-15T12:00:00Z");
    private static final Instant JULY = Instant.parse("2026-07-15T12:00:00Z");

    @Test
    public void wholeHourOffsets() {
        assertEquals("UTC+7", ZoneLabel.format(ZoneId.of("Asia/Ho_Chi_Minh"), JANUARY));
        assertEquals("UTC-5", ZoneLabel.format(ZoneId.of("America/New_York"), JANUARY));
    }

    @Test
    public void zeroOffsetIsJustUtc() {
        assertEquals("UTC", ZoneLabel.format(ZoneId.of("UTC"), JANUARY));
    }

    @Test
    public void halfHourOffsetsShowTheMinutes() {
        assertEquals("UTC+5:30", ZoneLabel.format(ZoneId.of("Asia/Kolkata"), JANUARY));
        assertEquals("UTC-3:30", ZoneLabel.format(ZoneId.of("America/St_Johns"), JANUARY));
    }

    @Test
    public void daylightSavingFollowsTheInstant() {
        assertEquals("UTC-5", ZoneLabel.format(ZoneId.of("America/New_York"), JANUARY));
        assertEquals("UTC-4", ZoneLabel.format(ZoneId.of("America/New_York"), JULY));
    }
}
