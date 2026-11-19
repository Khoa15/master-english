package com.doradodo.app.utils;

import java.time.Instant;
import java.time.ZoneId;

/** A short offset label for a zone, such as "UTC+7" or "UTC-3:30", correct for the given moment. */
public final class ZoneLabel {

    private ZoneLabel() {
    }

    public static String format(ZoneId zone, Instant at) {
        int seconds = zone.getRules().getOffset(at).getTotalSeconds();
        if (seconds == 0) {
            return "UTC";
        }
        int abs = Math.abs(seconds);
        int hours = abs / 3600;
        int minutes = (abs % 3600) / 60;
        String sign = seconds < 0 ? "-" : "+";
        return minutes == 0 ? "UTC" + sign + hours : String.format("UTC%s%d:%02d", sign, hours, minutes);
    }
}
