package com.doradodo.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.doradodo.app.data.local.entity.GameWord;
import com.doradodo.app.data.local.entity.UserSettingsEntity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class GameWordRepositoryTest {

    private EntertainmentTestFixture fx;
    private GameWordRepository repo;

    @Before
    public void setUp() {
        fx = new EntertainmentTestFixture();
        repo = new GameWordRepository(fx.db.entertainmentDao(), fx.db.wordDao(), fx.learner);
    }

    @After
    public void tearDown() {
        fx.close();
    }

    @Test
    public void distractorsNeverIncludeAnExcludedWord() {
        Set<String> exclude = new HashSet<>(Arrays.asList("apple", "banana"));
        for (int i = 0; i < 10; i++) {
            for (GameWord w : repo.distractors(exclude, 3)) {
                assertFalse(exclude.contains(w.id));
            }
        }
    }

    @Test
    public void distractorsRespectTheLimit() {
        assertEquals(2, repo.distractors(new HashSet<>(), 2).size());
    }

    @Test
    public void learnedWordsHaveAShortMeaningAndRespectTheLimit() {
        List<GameWord> learned = repo.learnedWords(3, false);
        assertEquals(3, learned.size());
        for (GameWord w : learned) {
            assertTrue(w.meaning.startsWith("meaning of"));
        }
        assertEquals(5, repo.learnedCount());
    }

    @Test
    public void learnedCountFollowsTheLearningLanguage() {
        UserSettingsEntity s = new UserSettingsEntity();
        s.userId = EntertainmentTestFixture.USER;
        s.displayLanguage = "en";
        s.learningLanguage = "ko";
        fx.db.userDao().upsertSettings(s);
        assertEquals(0, repo.learnedCount());
    }

    @Test
    public void wordSourcesListLearnedWordsThenPacksWithSizes() {
        List<WordSource> sources = repo.wordSources();
        assertEquals(WordSource.LEARNED, sources.get(0).id);
        assertEquals(5, sources.get(0).count);
        assertEquals("P1", sources.get(1).id);
        assertEquals(5, sources.get(1).count);
    }

    @Test
    public void sourceWordsOfAPackFollowTheListOrderAndLimit() {
        assertEquals(2, repo.sourceWords("P1", 2).size());
    }
}
