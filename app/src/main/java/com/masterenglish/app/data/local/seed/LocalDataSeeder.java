package com.masterenglish.app.data.local.seed;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.masterenglish.app.data.local.AppDatabase;
import com.masterenglish.app.data.local.dao.WordDao;
import com.masterenglish.app.data.local.entity.PackEntity;
import com.masterenglish.app.data.local.entity.PackWordEntity;
import com.masterenglish.app.data.local.entity.WordEntity;
import com.masterenglish.app.data.local.entity.WordSenseEntity;

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
 * Fills words, word_senses, packs and pack_words from assets/data/vocabulary.json (the Oxford word lists).
 * The content is versioned: when {@link #CONTENT_VERSION} changes the old content is replaced in one
 * transaction. Learners' word_progress rows for words that no longer exist go with them (foreign key cascade).
 */
@Singleton
public class LocalDataSeeder {

    /** Raise this when assets/data/vocabulary.json changes. */
    public static final int CONTENT_VERSION = 5;

    /** The old content and progress tables were replaced in schema version 4; the first load starts from scratch. */
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
        database.badgeDao().insertBadges(BadgeCatalogue.all());
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        WordDao dao = database.wordDao();
        if (prefs.getInt(KEY_VERSION, 0) == CONTENT_VERSION && dao.countWords() > 0) {
            return;
        }
        try {
            SeedModels.VocabularyFile file = read();
            List<PackEntity> packs = new ArrayList<>();
            List<WordEntity> words = new ArrayList<>();
            List<WordSenseEntity> senses = new ArrayList<>();
            List<PackWordEntity> links = new ArrayList<>();
            build(file, packs, words, senses, links);
            database.runInTransaction(() -> replaceContent(dao, packs, words, senses, links));
            prefs.edit().putInt(KEY_VERSION, CONTENT_VERSION).apply();
            Log.i(TAG, "Seeded " + words.size() + " words in " + packs.size() + " packs");
        } catch (IOException e) {
            Log.e(TAG, "Could not read bundled vocabulary", e);
        }
    }

    /**
     * Words and packs are upserted and only the ones missing from the new file are deleted, so a content update
     * keeps the progress of every word that is still there. Senses and pack membership are rebuilt.
     */
    static void replaceContent(WordDao dao, List<PackEntity> packs, List<WordEntity> words,
                               List<WordSenseEntity> senses, List<PackWordEntity> links) {
        java.util.Set<String> keepWords = new java.util.HashSet<>();
        for (WordEntity w : words) {
            keepWords.add(w.id);
        }
        java.util.Set<String> keepPacks = new java.util.HashSet<>();
        for (PackEntity p : packs) {
            keepPacks.add(p.id);
        }
        List<String> goneWords = new ArrayList<>(dao.getWordIds());
        goneWords.removeAll(keepWords);
        for (int i = 0; i < goneWords.size(); i += 500) {
            dao.deleteWords(goneWords.subList(i, Math.min(goneWords.size(), i + 500)));
        }
        List<String> gonePacks = new ArrayList<>(dao.getPackIds());
        gonePacks.removeAll(keepPacks);
        if (!gonePacks.isEmpty()) {
            dao.deletePacks(gonePacks);
        }
        dao.deleteSenses();
        dao.deletePackWords();
        dao.upsertPacks(packs);
        dao.upsertWords(words);
        dao.insertSenses(senses);
        dao.insertPackWords(links);
    }

    private SeedModels.VocabularyFile read() throws IOException {
        try (InputStream stream = context.getAssets().open("data/vocabulary.json");
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            SeedModels.VocabularyFile file = gson.fromJson(reader, SeedModels.VocabularyFile.class);
            if (file == null || file.words == null || file.packs == null) {
                throw new IOException("vocabulary.json is empty");
            }
            return file;
        }
    }

    static void build(SeedModels.VocabularyFile file, List<PackEntity> packs, List<WordEntity> words,
                      List<WordSenseEntity> senses, List<PackWordEntity> links) {
        for (SeedModels.PackSeed p : file.packs) {
            PackEntity e = new PackEntity();
            e.id = p.id;
            e.title = p.title;
            e.cefrLevel = p.cefr_level;
            e.position = p.position;
            packs.add(e);
            if (p.words != null) {
                int position = 1;
                for (String wordId : p.words) {
                    PackWordEntity link = new PackWordEntity();
                    link.packId = p.id;
                    link.wordId = wordId;
                    link.position = position++;
                    links.add(link);
                }
            }
        }
        for (SeedModels.WordSeed w : file.words) {
            WordEntity e = new WordEntity();
            e.id = w.id;
            e.term = w.term;
            e.ipa = w.ipa;
            e.cefrLevel = w.cefr_level;
            e.rank = w.rank;
            words.add(e);
            int position = 0;
            if (w.senses != null) {
                for (SeedModels.SenseSeed s : w.senses) {
                    WordSenseEntity se = new WordSenseEntity();
                    se.wordId = w.id;
                    se.position = position++;
                    se.pos = s.pos;
                    se.definitionEn = s.definition == null ? "" : s.definition;
                    senses.add(se);
                }
            }
        }
    }
}
