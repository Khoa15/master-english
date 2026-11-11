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
        progress = new ProgressRepository(db.progressDao(), db.contentDao(), db.userDao(), users,
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

    private RoadmapNodeEntity firstNodeOfType(NodeType type) {
        for (RoadmapNodeEntity node : db.contentDao().getAllNodes()) {
            if (node.nodeType == type) {
                return node;
            }
        }
        throw new AssertionError("no node of type " + type);
    }

    @Test
    public void seedingPopulatesEveryContentTable() {
        assertEquals(120, db.contentDao().countVocabulary());
        assertEquals(36, db.contentDao().countGrammar());
        assertEquals(18, db.contentDao().countReading());
        assertEquals(18, db.contentDao().countListening());
        assertEquals(6, db.contentDao().getAllStages().size());
        assertTrue(db.contentDao().countQuestions() > 300);
        assertEquals(4, db.badgeDao().getBadges().size());
    }

    @Test
    public void seedingIsIdempotent() {
        int nodes = db.contentDao().countNodes();
        new LocalDataSeeder(ApplicationProvider.getApplicationContext(), db).seedIfEmpty();
        assertEquals(nodes, db.contentDao().countNodes());
    }

    @Test
    public void vocabularyLessonLoadsCompletesAndSchedulesCards() throws Exception {
        RoadmapNodeEntity node = firstNodeOfType(NodeType.VOCABULARY);
        List<LessonQuestionEntity> questions = await(cb -> content.loadLessonQuestions(node, cb));
        assertFalse(questions.isEmpty());
        assertTrue(questions.size() <= 12);

        LessonReward reward = await(cb ->
                progress.completeLesson(USER, node, 4, 4, 3, cb));
        assertEquals(3, reward.stars);
        assertTrue(reward.firstCompletion);
        assertEquals(4 * 10 + 30, reward.experience);
        assertEquals(5 + 5, reward.gems);
        assertEquals(1, reward.streak);

        UserNodeProgressEntity saved = db.progressDao().getNodeProgressFor(USER, node.id);
        assertEquals(NodeStatus.MASTERED, saved.status);

        for (VocabularyEntity word : db.contentDao().getVocabularyByNode(node.id)) {
            UserItemRetentionEntity card = db.progressDao().getRetention(USER, ItemType.VOCABULARY, word.id);
            assertNotNull("no SM-2 card for " + word.id, card);
            assertEquals(1, card.intervalDays);
        }
    }

    @Test
    public void roadmapUnlocksTheNextNodeAfterCompletion() throws Exception {
        List<RoadmapNodeEntity> nodes = db.contentDao().getAllNodes();
        RoadmapNodeEntity first = nodes.get(0);
        await((Start<LessonReward>) cb -> progress.completeLesson(USER, first, 2, 4, 2, cb));

        List<RoadmapItem> items = ProgressRepository.buildRoadmap(nodes, db.progressDao().getNodeProgress(USER));
        assertEquals(NodeStatus.COMPLETED, items.get(0).status);
        assertEquals(NodeStatus.AVAILABLE, items.get(1).status);
        assertEquals(NodeStatus.LOCKED, items.get(2).status);
    }

    @Test
    public void wrongAnswersCreateWeaknessesThatRecover() throws Exception {
        RoadmapNodeEntity node = firstNodeOfType(NodeType.GRAMMAR);
        List<LessonQuestionEntity> questions = await(cb -> content.loadLessonQuestions(node, cb));
        LessonQuestionEntity question = questions.get(0);

        progress.recordAnswer(USER, question, false);
        Thread.sleep(200);
        UserWeaknessEntity weak = waitForWeakness(question.skillTag);
        assertEquals(0f, weak.masteryScore, 1e-6);
        assertEquals(1, weak.errorCount);

        progress.recordAnswer(USER, question, true);
        Thread.sleep(200);
        UserWeaknessEntity recovering = db.progressDao().getWeakness(USER, question.skillTag);
        assertTrue(recovering.masteryScore > 0f);
    }

    private UserWeaknessEntity waitForWeakness(String tag) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        UserWeaknessEntity row = db.progressDao().getWeakness(USER, tag);
        while (row == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
            row = db.progressDao().getWeakness(USER, tag);
        }
        assertNotNull("weakness " + tag + " was not recorded", row);
        return row;
    }

    @Test
    public void flashcardGradingUpdatesTheSchedule() throws Exception {
        RoadmapNodeEntity node = firstNodeOfType(NodeType.VOCABULARY);
        await((Start<LessonReward>) cb -> progress.completeLesson(USER, node, 4, 4, 1, cb));
        VocabularyEntity word = db.contentDao().getVocabularyByNode(node.id).get(0);

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

    @Test
    public void lessonQuestionsKeepTheirParentLinkage() {
        List<LessonQuestionEntity> listening = db.contentDao().getQuestions(ParentType.LISTENING,
                db.contentDao().getListeningByNode(firstNodeOfType(NodeType.LISTENING).id).id);
        assertEquals(2, listening.size());
        assertEquals("raw/listen_001", listening.get(0).mediaUrl);
    }

    @Test
    public void listeningRowsReferenceTheirTranscriptAndAudio() {
        com.masterenglish.app.data.local.entity.ListeningEntity one = db.contentDao().getListening("list_001");
        assertNotNull(one);
        assertEquals("raw/listen_001", one.audioUrl);
        assertTrue(one.transcript.isJsonArray());
    }
}
