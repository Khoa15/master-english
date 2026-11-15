package com.masterenglish.app.data.local.seed;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.masterenglish.app.data.local.AppDatabase;
import com.masterenglish.app.data.local.dao.ContentDao;
import com.masterenglish.app.data.local.entity.VocabularyEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Fills the local database from assets/data/vocabulary.json (the Oxford word lists by CEFR level).
 * The content is versioned: when {@link #CONTENT_VERSION} changes, the old content and the progress
 * that referred to it are removed and the words are loaded again, all in one transaction.
 */
@Singleton
public class LocalDataSeeder {

    /** Raise this when assets/data/vocabulary.json changes. */
    public static final int CONTENT_VERSION = 2;

    private static final String TAG = "LocalDataSeeder";
    private static final String PREFS = "content_seed";
    private static final String KEY_VERSION = "version";

    private final Context context;
    private final AppDatabase database;
    private final Gson gson = new Gson();

    @Inject
    public LocalDataSeeder(@ApplicationContext Context context, AppDatabase database) {
        this.context = context;
        this.database = database;
    }

    public synchronized void seedIfEmpty() {
        // Badges are idempotent (IGNORE on conflict) and must exist even when content was seeded by an older build.
        database.badgeDao().insertBadges(ContentBuilder.badgeCatalogue());
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        ContentDao content = database.contentDao();
        if (prefs.getInt(KEY_VERSION, 0) == CONTENT_VERSION && content.countVocabulary() > 0) {
            return;
        }
        try {
            List<VocabularyEntity> words = toEntities(read());
            database.runInTransaction(() -> {
                content.deleteQuestions();
                content.deleteNodeProgress();
                content.deleteWeaknesses();
                content.deleteRetention();
                content.deleteVocabulary();
                content.deleteGrammar();
                content.deleteReading();
                content.deleteListening();
                content.deleteNodes();
                content.deleteStages();
                content.insertVocabulary(words);
            });
            prefs.edit().putInt(KEY_VERSION, CONTENT_VERSION).apply();
            Log.i(TAG, "Seeded " + words.size() + " words");
        } catch (IOException e) {
            Log.e(TAG, "Could not read bundled vocabulary", e);
        }
    }

    private List<SeedModels.WordSeed> read() throws IOException {
        try (InputStream stream = context.getAssets().open("data/vocabulary.json");
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            List<SeedModels.WordSeed> list = gson.fromJson(reader, new TypeToken<List<SeedModels.WordSeed>>() { }.getType());
            return list == null ? new ArrayList<>() : list;
        }
    }

    static List<VocabularyEntity> toEntities(List<SeedModels.WordSeed> seeds) {
        List<VocabularyEntity> out = new ArrayList<>(seeds.size());
        for (SeedModels.WordSeed w : seeds) {
            VocabularyEntity e = new VocabularyEntity();
            e.id = w.id;
            e.word = w.term;
            e.phonetic = w.ipa;
            e.partOfSpeech = w.part_of_speech;
            e.cefrLevel = w.cefr_level;
            // The Oxford lists only have English definitions; they go in the meaning column the UI shows.
            e.meaningVi = w.definition == null ? "" : w.definition;
            e.definitionEn = w.definition;
            e.domain = w.cefr_level;
            e.synonyms = new ArrayList<>();
            e.antonyms = new ArrayList<>();
            out.add(e);
        }
        return out;
    }
}
