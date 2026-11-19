package com.doradodo.app.ui.entertainment;

import android.text.format.DateFormat;

import com.doradodo.app.utils.ZoneLabel;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Contest moments written in the learner's time zone, with the zone named so the time is unambiguous. */
final class ContestTimes {

    /** Weekday, day, month and time, e.g. "Sat 21 Nov, 20:00". */
    static final String WEEKDAY_TIME = "EEEdMMMHm";
    /** Day, month and time, e.g. "21 Nov, 20:00". */
    static final String DATE_TIME = "dMMMHm";

    private ContestTimes() {
    }

    static String format(long millis, ZoneId zone, String skeleton) {
        Locale locale = Locale.getDefault();
        String pattern = DateFormat.getBestDateTimePattern(locale, skeleton);
        return DateTimeFormatter.ofPattern(pattern, locale).withZone(zone).format(Instant.ofEpochMilli(millis));
    }

    /** "UTC+7" for the moment given. */
    static String zone(long millis, ZoneId zone) {
        return ZoneLabel.format(zone, Instant.ofEpochMilli(millis));
    }
}
