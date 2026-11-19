package com.doradodo.app.data.repository;

import com.doradodo.app.data.local.entity.ContestPrizeEntity;

import java.util.List;

public final class ContestDetail {
    public final ContestCard card;
    public final List<ContestPrizeEntity> prizes;

    ContestDetail(ContestCard card, List<ContestPrizeEntity> prizes) {
        this.card = card;
        this.prizes = prizes;
    }
}
