package com.doradodo.app.ui.vocab;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class AnswerCheckerTest {

    @Test
    public void typedAnswerIgnoresCaseAndSurroundingSpace() {
        assertTrue(AnswerChecker.typedMatches("  Apple ", "apple"));
        assertFalse(AnswerChecker.typedMatches("apples", "apple"));
    }

    @Test
    public void spokenAnswerMatchesAnyGuessThatContainsTheWord() {
        assertTrue(AnswerChecker.spokenMatches(Arrays.asList("a pull", "I said Apple"), "apple"));
        assertFalse(AnswerChecker.spokenMatches(Arrays.asList("banana"), "apple"));
    }

    @Test
    public void nothingHeardIsWrong() {
        assertFalse(AnswerChecker.spokenMatches(null, "apple"));
        assertFalse(AnswerChecker.spokenMatches(Collections.emptyList(), "apple"));
    }
}
