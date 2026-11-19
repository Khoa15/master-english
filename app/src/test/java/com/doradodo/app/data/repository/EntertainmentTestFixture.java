package com.doradodo.app.data.repository;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.doradodo.app.data.local.AppDatabase;
import com.doradodo.app.data.local.entity.ContestEntity;
import com.doradodo.app.data.local.entity.ContestPackEntity;
import com.doradodo.app.data.local.entity.LevelEntity;
import com.doradodo.app.data.local.entity.PackEntity;
import com.doradodo.app.data.local.entity.PackWordEntity;
import com.doradodo.app.data.local.entity.UserEntity;
import com.doradodo.app.data.local.entity.UserSettingsEntity;
import com.doradodo.app.data.local.entity.WordEntity;
import com.doradodo.app.data.local.entity.WordProgressEntity;
import com.doradodo.app.data.local.entity.WordSenseEntity;
import com.doradodo.app.data.local.prefs.AppPreferences;

import java.util.Arrays;
import java.util.Collections;

/** An in-memory database with one learner (app in English, learning English) and a few English words. */
final class EntertainmentTestFixture {

    static final String USER = "learner-1";

    final Context context = ApplicationProvider.getApplicationContext();
    final AppDatabase db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).allowMainThreadQueries().build();
    final AppPreferences preferences = new AppPreferences(context);
    final LearnerContext learner = new LearnerContext(preferences,
            new LanguageRepository(context, db.userDao(), db.wordDao(), preferences));

    EntertainmentTestFixture() {
        UserEntity user = new UserEntity();
        user.id = USER;
        db.userDao().upsertUser(user);
        UserSettingsEntity settings = new UserSettingsEntity();
        settings.userId = USER;
        settings.displayLanguage = "en";
        settings.learningLanguage = "en";
        db.userDao().upsertSettings(settings);
        preferences.setCurrentUserId(USER);

        LevelEntity level = new LevelEntity();
        level.system = "CEFR";
        level.code = "A1";
        db.wordDao().upsertLevels(Collections.singletonList(level));
        PackEntity pack = new PackEntity();
        pack.id = "P1";
        pack.title = "P1";
        pack.lang = "en";
        pack.levelSystem = "CEFR";
        pack.levelCode = "A1";
        db.wordDao().upsertPacks(Collections.singletonList(pack));
        for (String term : Arrays.asList("apple", "banana", "cherry", "date", "elder")) {
            addWord(term);
        }
    }

    /** A studied English word in pack P1, with a definition. */
    private void addWord(String term) {
        WordEntity w = new WordEntity();
        w.id = term;
        w.term = term;
        w.lang = "en";
        w.levelSystem = "CEFR";
        w.levelCode = "A1";
        db.wordDao().upsertWords(Collections.singletonList(w));
        WordSenseEntity sense = new WordSenseEntity();
        sense.wordId = term;
        sense.definition = "meaning of " + term;
        db.wordDao().insertSenses(Collections.singletonList(sense));
        PackWordEntity link = new PackWordEntity();
        link.packId = "P1";
        link.wordId = term;
        db.wordDao().insertPackWords(Collections.singletonList(link));
        WordProgressEntity p = new WordProgressEntity();
        p.userId = USER;
        p.wordId = term;
        db.progressDao().upsert(p);
    }

    /** A contest of pack P1 whose registration is open now (closes in an hour, starts in two). */
    ContestEntity addOpenContest(String id) {
        long now = System.currentTimeMillis();
        long hour = 60L * 60 * 1000;
        ContestEntity c = new ContestEntity();
        c.id = id;
        c.title = "Contest " + id;
        c.registrationOpensAt = now - hour;
        c.registrationClosesAt = now + hour;
        c.startsAt = now + 2 * hour;
        c.endsAt = now + 3 * hour;
        db.entertainmentDao().upsertContests(Collections.singletonList(c));
        ContestPackEntity link = new ContestPackEntity();
        link.contestId = id;
        link.packId = "P1";
        db.entertainmentDao().insertContestPacks(Collections.singletonList(link));
        return c;
    }

    void close() {
        db.close();
    }
}
