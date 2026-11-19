package com.doradodo.app.data.repository;

import androidx.annotation.Nullable;

import com.doradodo.app.data.local.dao.EntertainmentDao;
import com.doradodo.app.data.local.dao.WordDao;
import com.doradodo.app.data.local.entity.ContestPackEntity;
import com.doradodo.app.data.local.entity.PackEntity;
import com.doradodo.app.data.local.entity.ContestEntity;
import com.doradodo.app.data.local.entity.ContestPrizeEntity;
import com.doradodo.app.data.local.entity.ContestRegistrationEntity;
import com.doradodo.app.data.local.entity.GameWord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Contests: what is open, the detail of one, registering, and the words a contest asks. Run off the main thread. */
@Singleton
public class ContestRepository {

    /** Only this many contests are shown at once: the one running (or next) and the one after it. */
    public static final int MAX_ACTIVE_CONTESTS = 2;

    /** Contests that ended longer ago than this are removed. */
    private static final long KEEP_ENDED_MS = 14L * 24 * 60 * 60 * 1000;

    private final EntertainmentDao dao;
    private final WordDao wordDao;
    private final LearnerContext learner;
    private final ContestTemplates templates;

    @Inject
    public ContestRepository(EntertainmentDao dao, WordDao wordDao, LearnerContext learner,
                             ContestTemplates templates) {
        this.dao = dao;
        this.wordDao = wordDao;
        this.learner = learner;
        this.templates = templates;
    }

    /**
     * Writes the weekly contests of the templates that fit {@code lang}: this week's (or the running one) and the next.
     * The same week always gives the same row, so this can run on every visit; registrations are kept.
     */
    private void syncWeekly(String lang, long now) {
        dao.deleteLegacyContests();
        dao.deleteContestsEndedBefore(now - KEEP_ENDED_MS);
        Set<String> available = new HashSet<>();
        for (PackEntity p : wordDao.getPacks(lang)) {
            available.add(p.id);
        }
        for (ContestTemplate t : templates.all()) {
            List<String> packs = new ArrayList<>();
            for (String id : t.packs) {
                if (available.contains(id)) {
                    packs.add(id);
                }
            }
            if (packs.isEmpty()) {
                continue;
            }
            List<ContestEntity> weeks = ContestSchedule.occurrences(t, now, 2);
            dao.upsertContests(weeks);
            List<ContestPackEntity> packRows = new ArrayList<>();
            List<ContestPrizeEntity> prizeRows = new ArrayList<>();
            for (ContestEntity c : weeks) {
                for (int i = 0; i < packs.size(); i++) {
                    ContestPackEntity p = new ContestPackEntity();
                    p.contestId = c.id;
                    p.packId = packs.get(i);
                    p.position = i;
                    packRows.add(p);
                }
                for (int[] prize : t.prizes) {
                    ContestPrizeEntity p = new ContestPrizeEntity();
                    p.contestId = c.id;
                    p.rankFrom = prize[0];
                    p.rankTo = prize[1] == 0 ? null : prize[1];
                    p.rewardGems = prize[2];
                    prizeRows.add(p);
                }
            }
            dao.insertContestPacks(packRows);
            dao.insertContestPrizes(prizeRows);
        }
    }

    /** Open contests for a language (a contest is for the languages of the packs it draws from), translated. */
    List<ContestCard> openCards(String lang, long now) {
        syncWeekly(lang, now);
        List<ContestEntity> open = new ArrayList<>();
        for (ContestEntity c : dao.getOpenContests(now)) {
            if (open.size() < MAX_ACTIVE_CONTESTS && dao.getContestLanguages(c.id).contains(lang)) {
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
