package com.masterenglish.app.data.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.os.Looper;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.masterenglish.app.data.local.AppDatabase;
import com.masterenglish.app.data.local.entity.PackWordEntity;
import com.masterenglish.app.data.local.entity.WordEntity;
import com.masterenglish.app.data.local.entity.WordProgressEntity;
import com.masterenglish.app.data.local.entity.WordRow;
import com.masterenglish.app.data.local.entity.WordSenseEntity;
import com.masterenglish.app.data.local.seed.LocalDataSeeder;
import com.masterenglish.app.data.repository.ProgressRepository;
import com.masterenglish.app.data.repository.UserRepository;
import com.masterenglish.app.service.gamification.BadgeEvaluator;
import com.masterenglish.app.utils.AppExecutors;
import com.masterenglish.app.utils.Callback;
import com.masterenglish.app.utils.DateUtils;
import com.masterenglish.app.utils.SrsAlgorithm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** The word database end to end, against a real in-memory Room database: seeding, packs, SM-2 cards, badges. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LearningPipelineTest {

    private static final String USER = "guest-test-user";

    private AppDatabase db;
    private ProgressRepository progress;
    private UserRepository users;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).allowMainThreadQueries().build();
        AppExecutors executors = new AppExecutors();
        new LocalDataSeeder(context, db).seedIfEmpty();
        users = new UserRepository(db.userDao(), db.badgeDao(), executors);
        progress = new ProgressRepository(db, db.progressDao(),
                new BadgeEvaluator(db.badgeDao(), db.userDao(), db.progressDao()), executors);
        users.ensureUserNow(USER, null, "Tester", null);
    }

    @After
    public void tearDown() {
        db.close();
    }

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

    private WordEntity word(String id) {
        return db.wordDao().getWord(id);
    }

    // ----- Content ---------------------------------------------------------

    @Test
    public void seedingLoadsWordsSensesAndPacks() {
        // 4,883 Oxford words plus 99 common words that are not on the Oxford lists.
        assertEquals(4982, db.wordDao().countWords());
        assertEquals(6, db.wordDao().countPacks());
        // 4,899 Oxford pack entries (16 words are in two packs) plus the 1,000 common words.
        assertEquals(5899, db.wordDao().countPackWords());
        assertTrue(db.wordDao().countSenses() > 4982);
        assertEquals(2, db.badgeDao().getBadges().size());
    }

    @Test
    public void everyWordHasAtLeastOneDefinition() {
        List<WordRow> rows = db.wordDao().getWordRows();
        assertEquals(4982, rows.size());
        for (WordRow row : rows) {
            assertNotNull("no definition for " + row.term, row.definition);
            assertFalse("empty definition for " + row.term, row.definition.isEmpty());
        }
    }

    @Test
    public void packsHoldEveryWordOfTheirLevel() {
        Map<String, Integer> size = new HashMap<>();
        for (PackWordEntity link : db.wordDao().getPackWords()) {
            size.merge(link.packId, 1, Integer::sum);
        }
        assertEquals(Integer.valueOf(874), size.get("A1"));
        assertEquals(Integer.valueOf(765), size.get("A2"));
        assertEquals(Integer.valueOf(665), size.get("B1"));
        assertEquals(Integer.valueOf(1284), size.get("B2"));
        assertEquals(Integer.valueOf(1311), size.get("C1"));
        assertEquals(Integer.valueOf(1000), size.get("COMMON"));
    }

    @Test
    public void aWordTaggedB2AndC1IsOneWordInBothPacks() {
        Set<String> packs = new HashSet<>();
        for (PackWordEntity link : db.wordDao().getPackWords()) {
            if (link.wordId.equals("trigger")) {
                packs.add(link.packId);
            }
        }
        assertEquals(new HashSet<>(java.util.Arrays.asList("B2", "C1")), packs);
        assertEquals("B2", word("trigger").cefrLevel);
    }

    @Test
    public void headwordsThatHadNoDictionaryEntryAreFilledIn() {
        for (String id : new String[]{"look", "be", "monday", "january", "o_clock", "wi_fi", "net"}) {
            List<WordSenseEntity> senses = db.wordDao().getSenses(id);
            assertFalse("no sense for " + id, senses.isEmpty());
        }
        assertEquals("verb", db.wordDao().getSenses("look").get(0).pos);
    }

    @Test
    public void seedingIsIdempotent() {
        int words = db.wordDao().countWords();
        int senses = db.wordDao().countSenses();
        new LocalDataSeeder(ApplicationProvider.getApplicationContext(), db).seedIfEmpty();
        assertEquals(words, db.wordDao().countWords());
        assertEquals(senses, db.wordDao().countSenses());
    }

    // ----- Progress --------------------------------------------------------

    @Test
    public void flashcardGradingUpdatesTheSchedule() throws Exception {
        String id = "look";
        await((Start<Void>) cb -> progress.reviewCard(USER, id, SrsAlgorithm.Button.GOOD, cb));
        WordProgressEntity card = db.progressDao().get(USER, id);
        assertEquals(1, card.repetitions);
        assertEquals(1, card.intervalDays);
        assertEquals(1, card.timesCorrect);

        await((Start<Void>) cb -> progress.reviewCard(USER, id, SrsAlgorithm.Button.GOOD, cb));
        card = db.progressDao().get(USER, id);
        assertEquals(2, card.repetitions);
        assertEquals(6, card.intervalDays);

        await((Start<Void>) cb -> progress.reviewCard(USER, id, SrsAlgorithm.Button.AGAIN, cb));
        card = db.progressDao().get(USER, id);
        assertEquals(0, card.repetitions);
        assertEquals(1, card.intervalDays);
        assertEquals(2, card.timesCorrect);
        assertEquals(1, card.timesWrong);
    }

    @Test
    public void aSharedWordKeepsOneCardForBothPacks() throws Exception {
        await((Start<Void>) cb -> progress.reviewCard(USER, "trigger", SrsAlgorithm.Button.GOOD, cb));
        Map<String, WordProgressEntity> cards = await(cb -> progress.loadProgress(USER, cb));
        assertEquals(1, cards.size());
        assertNotNull(cards.get("trigger"));
    }

    @Test
    public void reloadingContentRemovesProgressOfDeletedWords() throws Exception {
        await((Start<Void>) cb -> progress.reviewCard(USER, "look", SrsAlgorithm.Button.GOOD, cb));
        assertNotNull(db.progressDao().get(USER, "look"));
        db.wordDao().deleteAllWords();
        assertNull(db.progressDao().get(USER, "look"));
        assertEquals(0, db.wordDao().countSenses());
        assertEquals(0, db.wordDao().countPackWords());
    }

    @Test
    public void aContentUpdateKeepsProgressOfWordsThatAreStillThere() throws Exception {
        await((Start<Void>) cb -> progress.reviewCard(USER, "look", SrsAlgorithm.Button.GOOD, cb));
        Context context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("content_seed", Context.MODE_PRIVATE).edit().putInt("version", 0).commit();
        new LocalDataSeeder(context, db).seedIfEmpty();
        assertNotNull(db.progressDao().get(USER, "look"));
        assertEquals(4982, db.wordDao().countWords());
        assertEquals(5899, db.wordDao().countPackWords());
    }

    @Test
    public void commonWordsPackIsInFrequencyOrderAndSharesOxfordWords() {
        List<PackWordEntity> common = new java.util.ArrayList<>();
        for (PackWordEntity link : db.wordDao().getPackWords()) {
            if (link.packId.equals("COMMON")) {
                common.add(link);
            }
        }
        assertEquals(1000, common.size());
        assertEquals("the", common.get(0).wordId);
        assertEquals("of", common.get(1).wordId);
        // "people" is an Oxford A1 word: the common pack links to it instead of making a second card.
        boolean shared = false;
        for (PackWordEntity link : common) {
            if (link.wordId.equals("people")) {
                shared = true;
            }
        }
        assertTrue(shared);
        assertEquals("A1", word("people").cefrLevel);
        // Words that are not Oxford headwords are words of their own.
        assertEquals("COMMON", word("is").cefrLevel);
    }

    @Test
    public void masteredWordsAreCountedAfterThreeGoodReviews() throws Exception {
        for (int i = 0; i < 3; i++) {
            await((Start<Void>) cb -> progress.reviewCard(USER, "tight", SrsAlgorithm.Button.GOOD, cb));
        }
        assertEquals(1, db.progressDao().countMastered(USER, ProgressRepository.MASTERED_REPETITIONS));
    }

    @Test
    public void studyMinutesAccumulateInOneDayRow() {
        String today = DateUtils.todayKey();
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
        db.userDao().updateStreak(USER, 30, DateUtils.todayKey(), System.currentTimeMillis());
        BadgeEvaluator evaluator = new BadgeEvaluator(db.badgeDao(), db.userDao(), db.progressDao());
        List<String> first = evaluator.evaluateNow(USER);
        List<String> second = evaluator.evaluateNow(USER);
        assertEquals(1, first.size());
        assertEquals("Chiến thần 30 ngày Streak", first.get(0));
        assertTrue(second.isEmpty());
    }
}
