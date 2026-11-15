package com.masterenglish.app.data.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.os.Looper;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.masterenglish.app.data.local.AppDatabase;
import com.masterenglish.app.data.local.Enums.ItemType;
import com.masterenglish.app.data.local.Enums.NodeStatus;
import com.masterenglish.app.data.local.Enums.NodeType;
import com.masterenglish.app.data.local.Enums.ParentType;
import com.masterenglish.app.data.local.entity.LessonQuestionEntity;
import com.masterenglish.app.data.local.entity.RoadmapNodeEntity;
import com.masterenglish.app.data.local.entity.UserItemRetentionEntity;
import com.masterenglish.app.data.local.entity.UserNodeProgressEntity;
import com.masterenglish.app.data.local.entity.UserWeaknessEntity;
import com.masterenglish.app.data.local.entity.VocabularyEntity;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.local.seed.LocalDataSeeder;
import com.masterenglish.app.data.repository.ContentRepository;
import com.masterenglish.app.data.repository.LessonReward;
import com.masterenglish.app.data.repository.ProgressRepository;
import com.masterenglish.app.data.repository.RoadmapItem;
import com.masterenglish.app.data.repository.UserRepository;
import com.masterenglish.app.service.ai.WeaknessDetector;
import com.masterenglish.app.service.gamification.BadgeEvaluator;
import com.masterenglish.app.utils.AppExecutors;
import com.masterenglish.app.utils.Callback;
import com.masterenglish.app.utils.SrsAlgorithm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * End-to-end check of the offline pipeline against a real Room database:
 * seeding from the bundled JSON, lesson questions, rewards, SM-2 cards, weaknesses and badges.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LearningPipelineTest {

    private static final String USER = "guest-test-user";

    private AppDatabase db;
    private AppExecutors executors;
    private ContentRepository content;
    private ProgressRepository progress;
    private UserRepository users;
    private AppPreferences preferences;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).allowMainThreadQueries().build();
        executors = new AppExecutors();
        new LocalDataSeeder(context, db).seedIfEmpty();

        content = new ContentRepository(db.contentDao(), executors);
        users = new UserRepository(db.userDao(), db.badgeDao(), executors);
        progress = new ProgressRepository(db, db.progressDao(), db.contentDao(), db.userDao(), users,
                new WeaknessDetector(), new BadgeEvaluator(db.badgeDao(), db.userDao(), db.progressDao()), executors);
        preferences = new AppPreferences(context);
        preferences.setCurrentUserId(USER);
        users.ensureUserNow(USER, null, "Tester", null);
    }

    @After
    public void tearDown() {
        db.close();
    }

    /** A repository call that reports its result through a Callback. */
    private interface Start<T> {
        void run(Callback<T> callback);
    }

    /** Runs a background-executor callback to completion while pumping the main looper. */
    private static <T> T await(Start<T> start) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        start.run(value -> {
            result.set(value);
            latch.countDown();
        });
        long deadline = System.currentTimeMillis() + 10_000;
        while (latch.getCount() > 0 && System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(5);
        }
        assertEquals("callback never fired", 0, latch.getCount());
        return result.get();
    }

    @Test
    public void seedingLoadsTheOxfordWordsByCefrLevel() {
        assertEquals(4899, db.contentDao().countVocabulary());
        java.util.Map<String, Integer> byLevel = new java.util.HashMap<>();
        for (VocabularyEntity w : db.contentDao().getAllVocabulary()) {
            byLevel.merge(w.cefrLevel, 1, Integer::sum);
        }
        assertEquals(Integer.valueOf(874), byLevel.get("A1"));
        assertEquals(Integer.valueOf(765), byLevel.get("A2"));
        assertEquals(Integer.valueOf(665), byLevel.get("B1"));
        assertEquals(Integer.valueOf(1284), byLevel.get("B2"));
        assertEquals(Integer.valueOf(1311), byLevel.get("C1"));
        assertEquals(4, db.badgeDao().getBadges().size());
    }

    @Test
    public void aWordTaggedB2AndC1LivesInBothPacks() {
        VocabularyEntity b2 = null;
        VocabularyEntity c1 = null;
        for (VocabularyEntity w : db.contentDao().getAllVocabulary()) {
            if (w.word.equals("trigger")) {
                if (w.cefrLevel.equals("B2")) {
                    b2 = w;
                } else if (w.cefrLevel.equals("C1")) {
                    c1 = w;
                }
            }
        }
        assertNotNull(b2);
        assertNotNull(c1);
    }

    @Test
    public void flashcardGradingUpdatesTheSchedule() throws Exception {
        VocabularyEntity word = db.contentDao().getAllVocabulary().get(0);
        await((Start<Void>) cb -> progress.reviewCard(USER, ItemType.VOCABULARY, word.id, SrsAlgorithm.Button.GOOD, cb));
        UserItemRetentionEntity card = db.progressDao().getRetention(USER, ItemType.VOCABULARY, word.id);
        assertEquals(1, card.repetitionNumber);
        assertEquals(1, card.intervalDays);

        await((Start<Void>) cb -> progress.reviewCard(USER, ItemType.VOCABULARY, word.id, SrsAlgorithm.Button.GOOD, cb));
        card = db.progressDao().getRetention(USER, ItemType.VOCABULARY, word.id);
        assertEquals(2, card.repetitionNumber);
        assertEquals(6, card.intervalDays);

        await((Start<Void>) cb -> progress.reviewCard(USER, ItemType.VOCABULARY, word.id, SrsAlgorithm.Button.AGAIN, cb));
        card = db.progressDao().getRetention(USER, ItemType.VOCABULARY, word.id);
        assertEquals(0, card.repetitionNumber);
        assertEquals(1, card.intervalDays);
    }

    @Test
    public void seedingIsIdempotent() {
        int words = db.contentDao().countVocabulary();
        new LocalDataSeeder(ApplicationProvider.getApplicationContext(), db).seedIfEmpty();
        assertEquals(words, db.contentDao().countVocabulary());
    }

    @Test
    public void studyMinutesAccumulateInOneDayRow() {
        String today = com.masterenglish.app.utils.DateUtils.todayKey();
        db.userDao().addStudyMinutes(USER, today, 5);
        db.userDao().addStudyMinutes(USER, today, 7);
        try (Cursor cursor = db.getOpenHelper().getReadableDatabase()
                .query("SELECT minutes FROM daily_study_log WHERE user_id = ? AND date_key = ?",
                        new Object[]{USER, today})) {
            assertTrue(cursor.moveToFirst());
            assertEquals(12, cursor.getInt(0));
            assertEquals(1, cursor.getCount());
        }
    }

    @Test
    public void streakBadgeUnlocksOnceAtThirtyDays() {
        db.userDao().updateStreak(USER, 30, com.masterenglish.app.utils.DateUtils.todayKey(), System.currentTimeMillis());
        BadgeEvaluator evaluator = new BadgeEvaluator(db.badgeDao(), db.userDao(), db.progressDao());
        List<String> first = evaluator.evaluateNow(USER);
        List<String> second = evaluator.evaluateNow(USER);
        assertEquals(1, first.size());
        assertEquals("Chiến thần 30 ngày Streak", first.get(0));
        assertTrue(second.isEmpty());
    }

}
