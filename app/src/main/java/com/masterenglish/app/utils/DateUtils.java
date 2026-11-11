package com.masterenglish.app.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Calendar helpers. Learning days follow the device's local time zone. */
public final class DateUtils {

    private static final DateTimeFormatter KEY_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private DateUtils() {
    }

    public static String dayKey(LocalDate date) {
        return date.format(KEY_FORMAT);
    }

    public static String todayKey() {
        return dayKey(LocalDate.now(ZoneId.systemDefault()));
    }

    public static LocalDate parseDayKey(String key) {
        return LocalDate.parse(key, KEY_FORMAT);
    }

    public static long startOfDayMillis(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
