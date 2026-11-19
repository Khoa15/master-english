package com.doradodo.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.doradodo.app.data.local.entity.GameWord;

import java.util.Collections;

import org.junit.Test;

public class MeaningShortenerTest {

    private static String cut(String meaning) {
        GameWord w = new GameWord();
        w.meaning = meaning;
        MeaningShortener.shorten(Collections.singletonList(w));
        return w.meaning;
    }

    @Test
    public void keepsAShortMeaning() {
        assertEquals("a small dog", cut("  a small dog  "));
    }

    @Test
    public void cutsAtTheFirstClause() {
        assertEquals("a large animal", cut("a large animal; it lives in water"));
        assertEquals("a large animal", cut("a large animal. It lives in water"));
        assertEquals("a large animal", cut("a large animal (mammal)"));
    }

    @Test
    public void ignoresASeparatorTooCloseToTheStart() {
        assertEquals("a.b", cut("a.b"));
    }

    @Test
    public void cutsALongMeaningAtAWordBoundary() {
        String out = cut("one two three four five six seven eight nine ten eleven twelve thirteen fourteen");
        assertTrue(out.endsWith("…"));
        assertTrue(out.length() <= MeaningShortener.MAX_MEANING + 1);
        assertTrue(out.startsWith("one two three"));
        assertEquals(' ', "one two three four five six seven eight nine ten eleven twelve thirteen fourteen"
                .charAt(out.length() - 1));
    }

    @Test
    public void leavesANullMeaningAlone() {
        assertNull(cut(null));
    }
}
