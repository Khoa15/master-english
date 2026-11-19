package com.doradodo.app.ui.vocab;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PracticeCursorTest {

    @Test
    public void walksEveryWordOfAStageBeforeTheNextStage() {
        PracticeCursor c = new PracticeCursor(3);
        assertTrue(c.atStart());
        assertTrue(c.advance(2));
        assertEquals(0, c.stage());
        assertEquals(1, c.index());
        assertTrue(c.advance(2));
        assertEquals(1, c.stage());
        assertEquals(0, c.index());
    }

    @Test
    public void advanceStopsAfterTheLastStepOfTheLastStage() {
        PracticeCursor c = new PracticeCursor(2);
        int steps = 1;
        while (c.advance(3)) {
            steps++;
        }
        assertEquals(6, steps);
        assertTrue(c.isLast(3));
        assertEquals(1, c.stage());
        assertEquals(2, c.index());
    }

    @Test
    public void stepBackAtTheStartDoesNotMove() {
        PracticeCursor c = new PracticeCursor(2);
        assertFalse(c.stepBack(3));
        assertTrue(c.atStart());
    }

    @Test
    public void stepBackFromTheFirstWordGoesToTheLastWordOfThePreviousStage() {
        PracticeCursor c = new PracticeCursor(2);
        for (int i = 0; i < 3; i++) {
            c.advance(3);
        }
        assertEquals(1, c.stage());
        assertEquals(0, c.index());
        assertTrue(c.stepBack(3));
        assertEquals(0, c.stage());
        assertEquals(2, c.index());
    }

    @Test
    public void isLastOnlyOnTheFinalCell() {
        PracticeCursor c = new PracticeCursor(2);
        assertFalse(c.isLast(1));
        c.advance(1);
        assertTrue(c.isLast(1));
    }

    @Test
    public void progressGrowsToOneHundredPercent() {
        PracticeCursor c = new PracticeCursor(2);
        assertEquals(25, c.progressPercent(2));
        while (c.advance(2)) {
            // walk to the end
        }
        assertEquals(100, c.progressPercent(2));
    }
}
