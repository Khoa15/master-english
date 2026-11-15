package com.masterenglish.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LessonScorerTest {

    @Test
    public void emptyLessonIsLowestLevel() {
        assertEquals(1, LessonScorer.starsFor(0, 0));
    }

    @Test
    public void belowSixtyPercentIsOneStar() {
        assertEquals(1, LessonScorer.starsFor(0, 10));
        assertEquals(1, LessonScorer.starsFor(5, 10));
        assertEquals(1, LessonScorer.starsFor(59, 100));
    }

    @Test
    public void sixtyToEightyNinePercentIsTwoStars() {
        assertEquals(2, LessonScorer.starsFor(3, 5));
        assertEquals(2, LessonScorer.starsFor(8, 10));
        assertEquals(2, LessonScorer.starsFor(89, 100));
    }

    @Test
    public void ninetyPercentOrMoreIsThreeStars() {
        assertEquals(3, LessonScorer.starsFor(9, 10));
        assertEquals(3, LessonScorer.starsFor(10, 10));
        assertEquals(3, LessonScorer.starsFor(90, 100));
    }

    @Test
    public void onlyThreeStarsIsMastered() {
        assertFalse(LessonScorer.isMastered(1));
        assertFalse(LessonScorer.isMastered(2));
        assertTrue(LessonScorer.isMastered(3));
    }
}
