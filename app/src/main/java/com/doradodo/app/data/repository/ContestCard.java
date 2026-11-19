package com.doradodo.app.data.repository;

import com.doradodo.app.data.local.entity.ContestEntity;

public final class ContestCard {
    public final ContestEntity contest;
    public final ContestState state;
    public final boolean registered;
    public final int participants;
    public final int topPrizeGems;

    ContestCard(ContestEntity contest, ContestState state, boolean registered, int participants, int topPrizeGems) {
        this.contest = contest;
        this.state = state;
        this.registered = registered;
        this.participants = participants;
        this.topPrizeGems = topPrizeGems;
    }
}
