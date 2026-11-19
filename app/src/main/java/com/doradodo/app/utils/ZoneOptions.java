package com.doradodo.app.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/** The zones a learner can pick from: city zones of the world's regions, from west to east. */
public final class ZoneOptions {

    private static final Pattern CITY_ZONE =
            Pattern.compile("(Africa|America|Asia|Atlantic|Australia|Europe|Indian|Pacific)/.+");

    private ZoneOptions() {
    }

    /** Zone ids sorted by their offset at {@code at}, then by name. */
    public static List<String> build(Instant at) {
        List<String> ids = new ArrayList<>();
        for (String id : ZoneId.getAvailableZoneIds()) {
            if (CITY_ZONE.matcher(id).matches()) {
                ids.add(id);
            }
        }
        Collections.sort(ids, Comparator
                .comparingInt((String id) -> ZoneId.of(id).getRules().getOffset(at).getTotalSeconds())
                .thenComparing(Comparator.naturalOrder()));
        return ids;
    }
}
