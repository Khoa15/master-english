package com.doradodo.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class ContestTemplatesTest {

    private static final String JSON = "{\"version\":1,\"contest_templates\":[{"
            + "\"id\":\"weekly\",\"title\":\"Weekly Cup\",\"description\":\"d\",\"image\":\"ic_ent_trophy\","
            + "\"accent\":\"AMBER\",\"question_count\":25,\"duration_minutes\":10,"
            + "\"schedule\":{\"registration_opens\":\"MON 00:00\",\"registration_closes\":\"SAT 12:50\","
            + "\"starts\":\"SAT 13:00\",\"ends\":\"SUN 13:00\"},"
            + "\"packs\":[\"A1\",\"A2\"],\"prizes\":[[1,1,500],[4,10,50],[20,null,10]]}]}";

    @Test
    public void readsTheScheduleAsMinutesFromMondayMidnight() {
        ContestTemplate t = ContestTemplates.parse(JSON).get(0);
        int day = 24 * 60;
        assertEquals(0, t.registrationOpensMinute);
        assertEquals(5 * day + 12 * 60 + 50, t.registrationClosesMinute);
        assertEquals(5 * day + 13 * 60, t.startsMinute);
        assertEquals(6 * day + 13 * 60, t.endsMinute);
    }

    @Test
    public void readsTheDescriptionPacksAndPrizes() {
        ContestTemplate t = ContestTemplates.parse(JSON).get(0);
        assertEquals("weekly", t.id);
        assertEquals("Weekly Cup", t.title);
        assertEquals("ic_ent_trophy", t.image);
        assertEquals(25, t.questionCount);
        assertEquals(10, t.durationMinutes);
        assertEquals(2, t.packs.size());
        assertEquals(3, t.prizes.size());
        // An open ended prize range ("null") is stored as 0.
        assertEquals(0, t.prizes.get(2)[1]);
        assertEquals(10, t.prizes.get(2)[2]);
    }

    @Test
    public void noTemplatesGivesAnEmptyList() {
        assertTrue(ContestTemplates.parse("{\"version\":1}").isEmpty());
    }

    @Test
    public void theBundledFileIsValid() throws Exception {
        java.io.File file = new java.io.File("src/main/assets/data/entertainment.json");
        String json = new String(java.nio.file.Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
        List<ContestTemplate> templates = ContestTemplates.parse(json);
        assertTrue(templates.size() >= 1);
        for (ContestTemplate t : templates) {
            assertTrue(t.id, t.registrationOpensMinute < t.registrationClosesMinute);
            assertTrue(t.id, t.registrationClosesMinute <= t.startsMinute);
            assertTrue(t.id, t.startsMinute < t.endsMinute);
            assertTrue(t.id, t.endsMinute - t.registrationOpensMinute <= 7 * 24 * 60);
            assertTrue(t.id, !t.packs.isEmpty());
        }
    }
}
