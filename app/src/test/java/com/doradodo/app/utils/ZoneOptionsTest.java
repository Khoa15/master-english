package com.doradodo.app.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

public class ZoneOptionsTest {

    private static final Instant NOW = Instant.parse("2026-01-15T12:00:00Z");

    @Test
    public void listsTheLearnersLikelyZones() {
        List<String> ids = ZoneOptions.build(NOW);
        assertTrue(ids.contains("Asia/Ho_Chi_Minh"));
        assertTrue(ids.contains("Europe/Paris"));
        assertTrue(ids.contains("America/New_York"));
    }

    @Test
    public void onlyRegionCityIds() {
        for (String id : ZoneOptions.build(NOW)) {
            assertTrue(id, id.matches("(Africa|America|Asia|Atlantic|Australia|Europe|Indian|Pacific)/.+"));
        }
    }

    @Test
    public void noDuplicates() {
        List<String> ids = ZoneOptions.build(NOW);
        assertEquals(ids.size(), new HashSet<>(ids).size());
    }

    @Test
    public void everyIdIsAValidZone() {
        for (String id : ZoneOptions.build(NOW)) {
            ZoneId.of(id);
        }
    }

    @Test
    public void sortedByOffsetThenName() {
        List<String> ids = ZoneOptions.build(NOW);
        int previous = Integer.MIN_VALUE;
        String previousId = "";
        for (String id : ids) {
            int offset = ZoneId.of(id).getRules().getOffset(NOW).getTotalSeconds();
            assertTrue(id + " after " + previousId, offset >= previous);
            if (offset == previous) {
                assertTrue(previousId.compareTo(id) <= 0);
            }
            previous = offset;
            previousId = id;
        }
        assertFalse(ids.isEmpty());
    }
}
