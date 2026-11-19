package com.doradodo.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.doradodo.app.data.local.entity.ContestEntity;
import com.doradodo.app.data.local.entity.GameWord;

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
        repo = new ContestRepository(fx.db.entertainmentDao(), fx.learner);
    }

    @After
    public void tearDown() {
        fx.close();
    }

    @Test
    public void registersInsideTheWindow() {
        fx.addOpenContest("c1");
        assertTrue(repo.register("c1"));
        assertEquals(1, fx.db.entertainmentDao().isRegistered("c1", EntertainmentTestFixture.USER));
    }

    @Test
    public void doesNotRegisterOutsideTheWindow() {
        ContestEntity c = fx.addOpenContest("c1");
        c.registrationClosesAt = System.currentTimeMillis() - 1;
        fx.db.entertainmentDao().upsertContests(Collections.singletonList(c));
        assertFalse(repo.register("c1"));
        assertEquals(0, fx.db.entertainmentDao().isRegistered("c1", EntertainmentTestFixture.USER));
    }

    @Test
    public void doesNotRegisterAnUnknownContestOrWithoutALearner() {
        assertFalse(repo.register("missing"));
        fx.addOpenContest("c1");
        fx.preferences.setCurrentUserId(null);
        assertFalse(repo.register("c1"));
    }

    @Test
    public void loadContestIsNullForAnUnknownId() {
        assertNull(repo.loadContest("missing"));
    }

    @Test
    public void loadContestReportsRegistrationAndState() {
        fx.addOpenContest("c1");
        ContestDetail before = repo.loadContest("c1");
        assertNotNull(before);
        assertEquals(ContestState.REGISTRATION, before.card.state);
        assertFalse(before.card.registered);
        repo.register("c1");
        ContestDetail after = repo.loadContest("c1");
        assertTrue(after.card.registered);
        assertEquals(1, after.card.participants);
    }

    @Test
    public void openCardsOnlyForTheLanguageOfThePacks() {
        fx.addOpenContest("c1");
        assertEquals(1, repo.openCards("en", System.currentTimeMillis()).size());
        assertTrue(repo.openCards("ko", System.currentTimeMillis()).isEmpty());
    }

    @Test
    public void contestWordsComeFromThePacksOfTheContest() {
        fx.addOpenContest("c1");
        List<GameWord> words = repo.contestWords("c1", 3);
        assertEquals(3, words.size());
        assertTrue(repo.contestWords("missing", 3).isEmpty());
    }
}
