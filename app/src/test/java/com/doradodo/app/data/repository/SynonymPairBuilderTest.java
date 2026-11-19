package com.doradodo.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.doradodo.app.data.local.entity.SynonymRow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

public class SynonymPairBuilderTest {

    private static SynonymRow row(long group, String text, boolean learned) {
        SynonymRow r = new SynonymRow();
        r.groupId = group;
        r.text = text;
        r.learned = learned;
        return r;
    }

    @Test
    public void aGroupOfOneMakesNoPair() {
        List<SynonymPair> pairs = SynonymPairBuilder.build(Arrays.asList(row(1, "big", false)), 5, new Random(1));
        assertTrue(pairs.isEmpty());
    }

    @Test
    public void aPairComesFromOneGroup() {
        List<SynonymRow> members = Arrays.asList(row(1, "big", false), row(1, "large", false),
                row(2, "fast", false), row(2, "quick", false));
        List<SynonymPair> pairs = SynonymPairBuilder.build(members, 5, new Random(7));
        assertEquals(2, pairs.size());
        for (SynonymPair p : pairs) {
            Set<String> two = new HashSet<>(Arrays.asList(p.left, p.right));
            assertTrue(two.equals(new HashSet<>(Arrays.asList("big", "large")))
                    || two.equals(new HashSet<>(Arrays.asList("fast", "quick"))));
        }
    }

    @Test
    public void respectsTheLimit() {
        List<SynonymRow> members = new ArrayList<>();
        for (long g = 1; g <= 6; g++) {
            members.add(row(g, "a" + g, false));
            members.add(row(g, "b" + g, false));
        }
        assertEquals(3, SynonymPairBuilder.build(members, 3, new Random(3)).size());
    }

    @Test
    public void noTextAppearsTwice() {
        // Group 2 repeats "big" (case differs), so its only free word cannot make a pair.
        List<SynonymRow> members = Arrays.asList(row(1, "big", true), row(1, "large", true),
                row(2, "BIG", false), row(2, "huge", false));
        List<SynonymPair> pairs = SynonymPairBuilder.build(members, 5, new Random(5));
        Set<String> seen = new HashSet<>();
        for (SynonymPair p : pairs) {
            assertTrue(seen.add(p.left.toLowerCase()));
            assertTrue(seen.add(p.right.toLowerCase()));
        }
    }

    @Test
    public void groupsTheLearnerKnowsComeFirstAndWordsStudiedGoLeft() {
        List<SynonymRow> members = Arrays.asList(row(1, "cold", false), row(1, "chilly", false),
                row(2, "happy", false), row(2, "glad", true));
        for (long seed = 0; seed < 20; seed++) {
            List<SynonymPair> pairs = SynonymPairBuilder.build(members, 5, new Random(seed));
            assertEquals("glad", pairs.get(0).left);
            assertEquals("happy", pairs.get(0).right);
        }
    }
}
