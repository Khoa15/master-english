package com.masterenglish.app.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SrsAlgorithmTest {

    @Test
    public void firstTwoSuccessfulReviewsScheduleOneThenSixDays() {
        SrsAlgorithm.State first = SrsAlgorithm.review(SrsAlgorithm.State.initial(), SrsAlgorithm.QUALITY_EASY);
        assertEquals(1, first.repetitions);
        assertEquals(1, first.intervalDays);
        assertEquals(2.6, first.easinessFactor, 1e-9);

        SrsAlgorithm.State second = SrsAlgorithm.review(first, SrsAlgorithm.QUALITY_EASY);
        assertEquals(2, second.repetitions);
        assertEquals(6, second.intervalDays);
        assertEquals(2.7, second.easinessFactor, 1e-9);
    }

    @Test
    public void laterReviewsMultiplyIntervalByEasiness() {
        SrsAlgorithm.State state = new SrsAlgorithm.State(2.7, 2, 6);
        SrsAlgorithm.State next = SrsAlgorithm.review(state, SrsAlgorithm.QUALITY_GOOD);
        assertEquals(3, next.repetitions);
        assertEquals(16, next.intervalDays); // round(6 * 2.7)
    }

    @Test
    public void failedRecallResetsRepetitionsAndLowersEasiness() {
        SrsAlgorithm.State state = new SrsAlgorithm.State(2.7, 5, 30);
        SrsAlgorithm.State failed = SrsAlgorithm.review(state, SrsAlgorithm.QUALITY_FAILED);
        assertEquals(0, failed.repetitions);
        assertEquals(1, failed.intervalDays);
        assertTrue(failed.easinessFactor < 2.7);
    }

    @Test
    public void easinessNeverDropsBelowMinimum() {
        SrsAlgorithm.State state = SrsAlgorithm.State.initial();
        for (int i = 0; i < 50; i++) {
            state = SrsAlgorithm.review(state, 0);
        }
        assertEquals(SrsAlgorithm.MIN_EASINESS, state.easinessFactor, 1e-9);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsOutOfRangeQuality() {
        SrsAlgorithm.review(SrsAlgorithm.State.initial(), 6);
    }

    @Test
    public void flashcardButtonsMapToQualityGrades() {
        assertEquals(1, SrsAlgorithm.qualityForButton(SrsAlgorithm.Button.AGAIN));
        assertEquals(3, SrsAlgorithm.qualityForButton(SrsAlgorithm.Button.HARD));
        assertEquals(4, SrsAlgorithm.qualityForButton(SrsAlgorithm.Button.GOOD));
        assertEquals(5, SrsAlgorithm.qualityForButton(SrsAlgorithm.Button.EASY));
    }
}
