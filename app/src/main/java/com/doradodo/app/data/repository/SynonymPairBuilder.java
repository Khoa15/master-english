package com.doradodo.app.data.repository;

import com.doradodo.app.data.local.entity.SynonymRow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Builds the pairs of the synonym game from the members of the synonym groups. Groups with a word the learner
 * studied come first, and no text appears twice, so every answer is unambiguous.
 */
public final class SynonymPairBuilder {

    private SynonymPairBuilder() {
    }

    public static List<SynonymPair> build(List<SynonymRow> members, int limit, Random random) {
        Map<Long, List<SynonymRow>> groups = new LinkedHashMap<>();
        for (SynonymRow r : members) {
            groups.computeIfAbsent(r.groupId, k -> new ArrayList<>()).add(r);
        }
        List<List<SynonymRow>> usable = new ArrayList<>();
        for (List<SynonymRow> group : groups.values()) {
            if (group.size() >= 2) {
                usable.add(group);
            }
        }
        Collections.shuffle(usable, random);
        // Stable sort: groups the learner knows move to the front, the shuffle order is kept inside each half.
        Collections.sort(usable, (a, b) -> Boolean.compare(knows(b), knows(a)));
        List<SynonymPair> pairs = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (List<SynonymRow> group : usable) {
            if (pairs.size() >= limit) {
                break;
            }
            List<SynonymRow> shuffled = new ArrayList<>(group);
            Collections.shuffle(shuffled, random);
            // Prefer a studied word on the left.
            Collections.sort(shuffled, (a, b) -> Boolean.compare(b.learned, a.learned));
            SynonymRow left = null;
            SynonymRow right = null;
            for (SynonymRow m : shuffled) {
                if (used.contains(m.text.toLowerCase())) {
                    continue;
                }
                if (left == null) {
                    left = m;
                } else {
                    right = m;
                    break;
                }
            }
            if (left != null && right != null) {
                used.add(left.text.toLowerCase());
                used.add(right.text.toLowerCase());
                pairs.add(new SynonymPair(left.text, right.text));
            }
        }
        return pairs;
    }

    private static boolean knows(List<SynonymRow> group) {
        for (SynonymRow m : group) {
            if (m.learned) {
                return true;
            }
        }
        return false;
    }
}
