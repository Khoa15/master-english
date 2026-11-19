package com.doradodo.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.doradodo.app.data.local.entity.ContestEntity;
import com.doradodo.app.data.local.entity.GameWord;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ContestRepositoryTest {

    private EntertainmentTestFixture fx;
    private ContestRepository repo;

    @Before
    public void setUp() {
        fx = new EntertainmentTestFixture();
        repo = repositoryWith(Collections.emptyList());
    }

    private ContestRepository repositoryWith(List<ContestTemplate> templates) {
        return new ContestRepository(fx.db.entertainmentDao(), fx.db.wordDao(), fx.learner,
                new ContestTemplates(templates));
    }

    private static ContestTemplate weekly(String id, String... packs) {
        int day = 24 * 60;
        ContestTemplate t = new ContestTemplate(id, 0, 5 * day + 770, 5 * day + 780, 6 * day + 780);
        t.title = "Weekly " + id;
        t.questionCount = 25;
        t.durationMinutes = 10;
        t.packs = Arrays.asList(packs);
        t.prizes = Arrays.asList(new int[]{1, 1, 500}, new int[]{2, 0, 50});
        return t;
    }

    @After
    public void tearDown() {
        fx.close();
    }

    @Test
    public void registersInsideTheWindow() {
        fx.addOpenContest("c1@w1");
        assertTrue(repo.register("c1@w1"));
        assertEquals(1, fx.db.entertainmentDao().isRegistered("c1@w1", EntertainmentTestFixture.USER));
    }

    @Test
    public void doesNotRegisterOutsideTheWindow() {
        ContestEntity c = fx.addOpenContest("c1@w1");
        c.registrationClosesAt = System.currentTimeMillis() - 1;
        fx.db.entertainmentDao().upsertContests(Collections.singletonList(c));
        assertFalse(repo.register("c1@w1"));
        assertEquals(0, fx.db.entertainmentDao().isRegistered("c1@w1", EntertainmentTestFixture.USER));
    }

    @Test
    public void doesNotRegisterAnUnknownContestOrWithoutALearner() {
        assertFalse(repo.register("missing"));
        fx.addOpenContest("c1@w1");
        fx.preferences.setCurrentUserId(null);
        assertFalse(repo.register("c1@w1"));
    }

    @Test
    public void loadContestIsNullForAnUnknownId() {
        assertNull(repo.loadContest("missing"));
    }

    @Test
    public void loadContestReportsRegistrationAndState() {
        fx.addOpenContest("c1@w1");
        ContestDetail before = repo.loadContest("c1@w1");
        assertNotNull(before);
        assertEquals(ContestState.REGISTRATION, before.card.state);
        assertFalse(before.card.registered);
        repo.register("c1@w1");
        ContestDetail after = repo.loadContest("c1@w1");
        assertTrue(after.card.registered);
        assertEquals(1, after.card.participants);
    }

    @Test
    public void openCardsOnlyForTheLanguageOfThePacks() {
        fx.addOpenContest("c1@w1");
        assertEquals(1, repo.openCards("en", System.currentTimeMillis()).size());
        assertTrue(repo.openCards("ko", System.currentTimeMillis()).isEmpty());
    }

    @Test
    public void contestWordsComeFromThePacksOfTheContest() {
        fx.addOpenContest("c1@w1");
        List<GameWord> words = repo.contestWords("c1@w1", 3);
        assertEquals(3, words.size());
        assertTrue(repo.contestWords("missing", 3).isEmpty());
    }

    // ----- Weekly contests -----------------------------------------------------

    @Test
    public void showsAtMostTwoActiveContestsEvenWithManyTemplates() {
        ContestRepository weekly = repositoryWith(Arrays.asList(weekly("a", "P1"), weekly("b", "P1"), weekly("c", "P1")));
        assertEquals(ContestRepository.MAX_ACTIVE_CONTESTS, weekly.openCards("en", System.currentTimeMillis()).size());
        assertEquals(2, ContestRepository.MAX_ACTIVE_CONTESTS);
    }

    @Test
    public void oneWeeklyTemplateGivesTheCurrentAndTheNextWeek() {
        ContestRepository weekly = repositoryWith(Collections.singletonList(weekly("cup", "P1")));
        List<ContestCard> cards = weekly.openCards("en", System.currentTimeMillis());
        assertEquals(2, cards.size());
        assertTrue(cards.get(0).contest.startsAt < cards.get(1).contest.startsAt);
        assertTrue(cards.get(0).contest.id.startsWith("cup@"));
    }

    @Test
    public void openingTheListTwiceDoesNotDuplicateContests() {
        ContestRepository weekly = repositoryWith(Collections.singletonList(weekly("cup", "P1")));
        weekly.openCards("en", System.currentTimeMillis());
        weekly.openCards("en", System.currentTimeMillis());
        assertEquals(2, fx.db.entertainmentDao().getOpenContests(System.currentTimeMillis()).size());
    }

    @Test
    public void aRegistrationSurvivesTheNextSync() {
        ContestRepository weekly = repositoryWith(Collections.singletonList(weekly("cup", "P1")));
        String id = weekly.openCards("en", System.currentTimeMillis()).get(0).contest.id;
        fx.db.entertainmentDao().register(registration(id));
        weekly.openCards("en", System.currentTimeMillis());
        assertEquals(1, fx.db.entertainmentDao().isRegistered(id, EntertainmentTestFixture.USER));
    }

    @Test
    public void aTemplateOfAnotherLanguageIsNotOffered() {
        ContestRepository weekly = repositoryWith(Collections.singletonList(weekly("cup", "P1")));
        assertTrue(weekly.openCards("ko", System.currentTimeMillis()).isEmpty());
        assertTrue(fx.db.entertainmentDao().getOpenContests(System.currentTimeMillis()).isEmpty());
    }

    @Test
    public void aGeneratedContestHasItsPacksAndPrizes() {
        ContestRepository weekly = repositoryWith(Collections.singletonList(weekly("cup", "P1", "Missing")));
        ContestCard card = weekly.openCards("en", System.currentTimeMillis()).get(0);
        assertEquals(500, card.topPrizeGems);
        assertEquals(Collections.singletonList("P1"), fx.db.entertainmentDao().getContestPackIds(card.contest.id));
        assertEquals(3, weekly.contestWords(card.contest.id, 3).size());
    }

    @Test
    public void contestsSeededBeforeWeeklyOnesAreRemoved() {
        fx.addOpenContest("old_contest");
        repositoryWith(Collections.emptyList()).openCards("en", System.currentTimeMillis());
        assertNull(fx.db.entertainmentDao().getContest("old_contest"));
    }

    @Test
    public void contestsEndedLongAgoAreCleanedUp() {
        ContestEntity old = fx.addOpenContest("cup@2020-W01");
        old.endsAt = System.currentTimeMillis() - 15L * 24 * 3600 * 1000;
        old.startsAt = old.endsAt - 3600_000L;
        old.registrationClosesAt = old.startsAt;
        fx.db.entertainmentDao().upsertContests(Collections.singletonList(old));
        ContestEntity recent = fx.addOpenContest("cup@2020-W02");
        recent.endsAt = System.currentTimeMillis() - 2L * 24 * 3600 * 1000;
        recent.startsAt = recent.endsAt - 3600_000L;
        recent.registrationClosesAt = recent.startsAt;
        fx.db.entertainmentDao().upsertContests(Collections.singletonList(recent));
        repositoryWith(Collections.emptyList()).openCards("en", System.currentTimeMillis());
        assertNull(fx.db.entertainmentDao().getContest("cup@2020-W01"));
        assertNotNull(fx.db.entertainmentDao().getContest("cup@2020-W02"));
    }

    private static com.doradodo.app.data.local.entity.ContestRegistrationEntity registration(String contestId) {
        com.doradodo.app.data.local.entity.ContestRegistrationEntity r =
                new com.doradodo.app.data.local.entity.ContestRegistrationEntity();
        r.contestId = contestId;
        r.userId = EntertainmentTestFixture.USER;
        r.registeredAt = System.currentTimeMillis();
        return r;
    }
}
