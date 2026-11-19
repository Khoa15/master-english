package com.doradodo.app.data.repository;

import com.doradodo.app.data.local.entity.ContestEntity;

/** Time rules of a contest. Pure functions of the contest's dates and "now". */
public final class ContestRules {

    private ContestRules() {
    }

    public static ContestState stateOf(ContestEntity c, long now) {
        if (now >= c.endsAt) {
            return ContestState.ENDED;
        }
        if (now >= c.startsAt) {
            return ContestState.LIVE;
        }
        return now < c.registrationOpensAt ? ContestState.REGISTRATION_SOON : ContestState.REGISTRATION;
    }

    /** True when the registration window is open right now. */
    public static boolean canRegister(ContestEntity c, long now) {
        return now >= c.registrationOpensAt && now < c.registrationClosesAt && now < c.endsAt;
    }
}
