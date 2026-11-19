package com.doradodo.app.data.repository;

import androidx.annotation.Nullable;

import com.doradodo.app.data.local.dao.EntertainmentDao;
import com.doradodo.app.data.local.entity.ContestEntity;
import com.doradodo.app.data.local.entity.ContestPrizeEntity;
import com.doradodo.app.data.local.entity.ContestRegistrationEntity;
import com.doradodo.app.data.local.entity.GameWord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Contests: what is open, the detail of one, registering, and the words a contest asks. Run off the main thread. */
@Singleton
public class ContestRepository {

    private final EntertainmentDao dao;
    private final LearnerContext learner;

    @Inject
    public ContestRepository(EntertainmentDao dao, LearnerContext learner) {
        this.dao = dao;
        this.learner = learner;
    }

    /** Open contests for a language (a contest is for the languages of the packs it draws from), translated. */
    List<ContestCard> openCards(String lang, long now) {
        List<ContestEntity> open = new ArrayList<>();
        for (ContestEntity c : dao.getOpenContests(now)) {
            if (dao.getContestLanguages(c.id).contains(lang)) {
                open.add(c);
            }
        }
        localize(open);
        Map<String, Integer> registrations = CountRows.toMap(dao.countRegistrations());
        List<ContestCard> cards = new ArrayList<>();
        for (ContestEntity c : open) {
            cards.add(card(c, now, registrations.getOrDefault(c.id, 0)));
        }
        return cards;
    }

    @Nullable
    public ContestDetail loadContest(String contestId) {
        ContestEntity c = dao.getContest(contestId);
        if (c == null) {
            return null;
        }
        int participants = CountRows.toMap(dao.countRegistrations()).getOrDefault(c.id, 0);
        localize(Collections.singletonList(c));
        return new ContestDetail(card(c, System.currentTimeMillis(), participants), dao.getPrizes(c.id));
    }

    /** Registers the learner; false when the window is closed or there is no learner. */
    public boolean register(String contestId) {
        String id = learner.currentUserId();
        ContestEntity c = dao.getContest(contestId);
        long now = System.currentTimeMillis();
        if (id == null || c == null || !ContestRules.canRegister(c, now)) {
            return false;
        }
        ContestRegistrationEntity r = new ContestRegistrationEntity();
        r.contestId = contestId;
        r.userId = id;
        r.registeredAt = now;
        dao.register(r);
        return true;
    }

    /** Questions of a contest: words of its packs, in random order. */
    public List<GameWord> contestWords(String contestId, int limit) {
        List<String> packs = dao.getContestPackIds(contestId);
        if (packs.isEmpty()) {
            return new ArrayList<>();
        }
        return MeaningShortener.shorten(
                dao.getPackWords(learner.userId(), packs, learner.displayLanguage(), limit));
    }

    private ContestCard card(ContestEntity c, long now, int participants) {
        int top = 0;
        for (ContestPrizeEntity p : dao.getPrizes(c.id)) {
            top = Math.max(top, p.rewardGems);
        }
        return new ContestCard(c, ContestRules.stateOf(c, now), dao.isRegistered(c.id, learner.userId()) > 0,
                participants, top);
    }

    private void localize(List<ContestEntity> contests) {
        new EntTextLocalizer(dao.getTexts(learner.displayLanguage()))
                .localize(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), contests);
    }
}
