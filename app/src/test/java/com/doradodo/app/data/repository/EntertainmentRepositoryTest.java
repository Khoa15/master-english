package com.doradodo.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.doradodo.app.data.local.entity.ActivityEntity;
import com.doradodo.app.data.local.entity.BannerEntity;
import com.doradodo.app.data.local.entity.EventEntity;

import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class EntertainmentRepositoryTest {

    private EntertainmentTestFixture fx;
    private EntertainmentRepository repo;

    @Before
    public void setUp() {
        fx = new EntertainmentTestFixture();
        repo = new EntertainmentRepository(fx.db.entertainmentDao(), fx.learner,
                new ContestRepository(fx.db.entertainmentDao(), fx.db.wordDao(), fx.learner,
                        new ContestTemplates(java.util.Collections.emptyList())));
    }

    @After
    public void tearDown() {
        fx.close();
    }

    private static ActivityEntity activity(String code, String... designFor) {
        ActivityEntity a = new ActivityEntity();
        a.id = code;
        a.code = code;
        a.title = code;
        a.designFor = Arrays.asList(designFor);
        return a;
    }

    private static BannerEntity banner(String id, String actionTarget, String audienceLang) {
        BannerEntity b = new BannerEntity();
        b.id = id;
        b.actionType = BannerEntity.ACTIVITY;
        b.actionTarget = actionTarget;
        b.audienceLang = audienceLang;
        return b;
    }

    @Test
    public void homeOffersOnlyTheGamesDesignedForTheLearningLanguage() {
        fx.db.entertainmentDao().upsertActivities(Arrays.asList(activity("QUIZ", "en"), activity("HANJA", "ko")));
        EntertainmentRepository.Home home = repo.loadHome();
        assertEquals(1, home.games.size());
        assertEquals("QUIZ", home.games.get(0).activity.code);
    }

    @Test
    public void aBannerThatOpensAMissingGameIsDropped() {
        fx.db.entertainmentDao().upsertActivities(Collections.singletonList(activity("QUIZ", "en")));
        fx.db.entertainmentDao().upsertBanners(Arrays.asList(banner("b1", "QUIZ", null), banner("b2", "HANJA", null)));
        EntertainmentRepository.Home home = repo.loadHome();
        assertEquals(1, home.banners.size());
        assertEquals("b1", home.banners.get(0).id);
    }

    @Test
    public void aBannerForAnotherAudienceLanguageIsDropped() {
        fx.db.entertainmentDao().upsertActivities(Collections.singletonList(activity("QUIZ", "en")));
        fx.db.entertainmentDao().upsertBanners(Arrays.asList(banner("b1", "QUIZ", "en"), banner("b2", "QUIZ", "ko")));
        assertEquals(1, repo.loadHome().banners.size());
    }

    @Test
    public void anEventForAnotherAudienceLanguageIsDropped() {
        long now = System.currentTimeMillis();
        EventEntity e = new EventEntity();
        e.id = "e1";
        e.startsAt = now - 1000;
        e.endsAt = now + 100000;
        e.audienceLang = "ko";
        fx.db.entertainmentDao().upsertEvents(Collections.singletonList(e));
        assertNull(repo.loadHome().event);
        e.audienceLang = "en";
        fx.db.entertainmentDao().upsertEvents(Collections.singletonList(e));
        assertNotNull(repo.loadHome().event);
    }

    @Test
    public void recordPlayLogsOnlyWhenALearnerIsSignedIn() {
        fx.db.entertainmentDao().upsertActivities(Collections.singletonList(activity("QUIZ", "en")));
        fx.preferences.setCurrentUserId(null);
        repo.recordPlay("QUIZ");
        assertTrue(fx.db.entertainmentDao().countLearnersPerActivity().isEmpty());
        fx.preferences.setCurrentUserId(EntertainmentTestFixture.USER);
        repo.recordPlay("QUIZ");
        assertEquals(1, fx.db.entertainmentDao().countLearnersPerActivity().size());
    }
}
