package com.masterenglish.app.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TextUtilsTest {

    @Test
    public void slugKeepsOnlyLowercaseAlphanumerics() {
        assertEquals("present_perfect_for_experience", TextUtils.slug("Present Perfect for Experience"));
        assertEquals("verb_to_be", TextUtils.slug("Verb 'to be'"));
        assertEquals("", TextUtils.slug(null));
    }

    @Test
    public void normalizeAnswerIgnoresCaseAndPunctuation() {
        assertEquals("i am a developer", TextUtils.normalizeAnswer("  I am a  Developer. "));
        assertEquals("it's fine", TextUtils.normalizeAnswer("It's fine!"));
    }

    @Test
    public void blankOutReplacesFirstWholeWordOnly() {
        assertEquals("I am a _____ and a developer.", TextUtils.blankOut("I am a tester and a developer.", "tester", "_____"));
        assertEquals("____ is job.", TextUtils.blankOut("Job is job.", "Job", "____"));
    }

    @Test
    public void containsWordRespectsWordBoundaries() {
        assertTrue(TextUtils.containsWord("My job is fun", "job"));
        assertFalse(TextUtils.containsWord("My jobless life", "job"));
    }
}
