package com.doradodo.app.data.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.doradodo.app.data.local.AppDatabase;
import com.doradodo.app.data.local.dao.EntertainmentDao;
import com.doradodo.app.data.local.dao.WordDao;
import com.doradodo.app.data.local.entity.GameWord;
import com.doradodo.app.data.local.entity.LevelEntity;
import com.doradodo.app.data.local.entity.PackEntity;
import com.doradodo.app.data.local.entity.SenseTranslationEntity;
import com.doradodo.app.data.local.entity.SynonymGroupEntity;
import com.doradodo.app.data.local.entity.SynonymMemberEntity;
import com.doradodo.app.data.local.entity.UserEntity;
import com.doradodo.app.data.local.entity.UserSettingsEntity;
import com.doradodo.app.data.local.entity.WordEntity;
import com.doradodo.app.data.local.entity.WordProgressEntity;
import com.doradodo.app.data.local.entity.WordSenseEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A learner who studies Korean with the app shown in English must only get Korean words, Korean packs and Korean
 * synonyms, with meanings in English, while English progress stays untouched for when they switch back.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LanguageScopingTest {

    private static final String USER = "guest-ko-learner";

    private AppDatabase db;
    private WordDao words;
    private EntertainmentDao ent;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).allowMainThreadQueries().build();
        words = db.wordDao();
        ent = db.entertainmentDao();

        UserEntity user = new UserEntity();
        user.id = USER;
        db.userDao().upsertUser(user);
        UserSettingsEntity settings = new UserSettingsEntity();
        settings.userId = USER;
        settings.displayLanguage = "en";
        settings.learningLanguage = "ko";
        db.userDao().upsertSettings(settings);

        words.upsertLevels(Arrays.asList(level("CEFR", "A1", 0), level("TOPIK", "1", 0)));
        words.upsertPacks(Arrays.asList(pack("A1", "en", "CEFR", "A1"), pack("TOPIK1", "ko", "TOPIK", "1")));
        words.upsertWords(Arrays.asList(word("hello", "hello", "en", "CEFR", "A1"),
                word("ko:annyeong", "안녕", "ko", "TOPIK", "1"), word("ko:gamsa", "감사", "ko", "TOPIK", "1")));
        words.insertSenses(Arrays.asList(sense("hello", "A greeting."), sense("ko:annyeong", "인사말"),
                sense("ko:gamsa", "고마움")));
        words.insertSenseTranslations(Collections.singletonList(translation("ko:annyeong", "en", "hello")));
        for (String id : new String[]{"hello", "ko:annyeong", "ko:gamsa"}) {
            WordProgressEntity p = new WordProgressEntity();
            p.userId = USER;
            p.wordId = id;
            db.progressDao().upsert(p);
        }
    }

    @After
    public void tearDown() {
        db.close();
    }

    private static LevelEntity level(String system, String code, int position) {
        LevelEntity l = new LevelEntity();
        l.system = system;
        l.code = code;
        l.position = position;
        return l;
    }

    private static PackEntity pack(String id, String lang, String system, String code) {
        PackEntity p = new PackEntity();
        p.id = id;
        p.title = id;
        p.lang = lang;
        p.levelSystem = system;
        p.levelCode = code;
        return p;
    }

    private static WordEntity word(String id, String term, String lang, String system, String code) {
        WordEntity w = new WordEntity();
        w.id = id;
        w.term = term;
        w.lang = lang;
        w.levelSystem = system;
        w.levelCode = code;
        return w;
    }

    private static WordSenseEntity sense(String wordId, String definition) {
        WordSenseEntity s = new WordSenseEntity();
        s.wordId = wordId;
        s.definition = definition;
        return s;
    }

    private static SenseTranslationEntity translation(String wordId, String lang, String text) {
        SenseTranslationEntity t = new SenseTranslationEntity();
        t.wordId = wordId;
        t.lang = lang;
        t.text = text;
        return t;
    }

    @Test
    public void learnedWordsAreOnlyKorean_withMeaningsInEnglish() {
        List<GameWord> learned = ent.getLearnedWords(USER, "ko", "en", false, 10);
        assertEquals(2, learned.size());
        for (GameWord w : learned) {
            assertTrue(w.id.startsWith("ko:"));
            // A translation in the display language wins; otherwise the Korean definition stays.
            assertEquals("ko:annyeong".equals(w.id) ? "hello" : "고마움", w.meaning);
        }
        assertEquals(2, ent.countLearned(USER, "ko"));
        assertEquals(1, ent.countLearned(USER, "en"));
    }

    @Test
    public void vocabularyAndPacksFollowTheLearningLanguage() {
        assertEquals(2, words.getWordRows("ko", "en").size());
        assertEquals(1, words.getWordRows("en", "en").size());
        assertEquals(Collections.singletonList("TOPIK1"), ids(words.getPacks("ko")));
        assertEquals(Arrays.asList("en", "ko"), words.getWordLanguages());
    }

    @Test
    public void synonymsOfOneLanguageNeverMixWithAnother() {
        SynonymGroupEntity en = new SynonymGroupEntity();
        en.lang = "en";
        long enId = ent.insertSynonymGroup(en);
        SynonymGroupEntity ko = new SynonymGroupEntity();
        ko.lang = "ko";
        long koId = ent.insertSynonymGroup(ko);
        ent.insertSynonymMembers(Arrays.asList(member(enId, "go"), member(enId, "travel"),
                member(koId, "가다"), member(koId, "떠나다")));
        assertEquals(2, ent.getSynonymMembers(USER, "ko").size());
        assertEquals(2, ent.getSynonymMembers(USER, "en").size());
        for (com.doradodo.app.data.local.entity.SynonymRow r : ent.getSynonymMembers(USER, "ko")) {
            assertEquals(koId, r.groupId);
        }
    }

    private static SynonymMemberEntity member(long group, String text) {
        SynonymMemberEntity m = new SynonymMemberEntity();
        m.groupId = group;
        m.text = text;
        return m;
    }

    private static List<String> ids(List<PackEntity> packs) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (PackEntity p : packs) {
            out.add(p.id);
        }
        return out;
    }
}
