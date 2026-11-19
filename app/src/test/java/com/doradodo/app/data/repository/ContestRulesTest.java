package com.doradodo.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.doradodo.app.data.local.entity.ContestEntity;

import org.junit.Test;

public class ContestRulesTest {

    /** Registration opens at 100, closes at 200, the contest runs 300..400. */
    private static ContestEntity contest() {
        ContestEntity c = new ContestEntity();
        c.registrationOpensAt = 100;
        c.registrationClosesAt = 200;
        c.startsAt = 300;
        c.endsAt = 400;
        return c;
    }

    @Test
    public void stateFollowsTheTimeline() {
        ContestEntity c = contest();
        assertEquals(ContestState.REGISTRATION_SOON, ContestRules.stateOf(c, 99));
        assertEquals(ContestState.REGISTRATION, ContestRules.stateOf(c, 100));
        assertEquals(ContestState.REGISTRATION, ContestRules.stateOf(c, 299));
        assertEquals(ContestState.LIVE, ContestRules.stateOf(c, 300));
        assertEquals(ContestState.LIVE, ContestRules.stateOf(c, 399));
        assertEquals(ContestState.ENDED, ContestRules.stateOf(c, 400));
    }

    @Test
    public void canRegisterOnlyInsideTheWindow() {
        ContestEntity c = contest();
        assertFalse(ContestRules.canRegister(c, 99));
        assertTrue(ContestRules.canRegister(c, 100));
        assertTrue(ContestRules.canRegister(c, 199));
        assertFalse(ContestRules.canRegister(c, 200));
    }

    @Test
    public void cannotRegisterAfterTheContestEnded() {
        ContestEntity c = contest();
        c.registrationClosesAt = 500;
        assertTrue(ContestRules.canRegister(c, 399));
        assertFalse(ContestRules.canRegister(c, 400));
    }
}
