package com.masterenglish.app.data.local.seed;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.masterenglish.app.data.local.AppDatabase;
import com.masterenglish.app.data.local.dao.WordDao;
import com.masterenglish.app.data.local.entity.ExampleTranslationEntity;
import com.masterenglish.app.data.local.entity.PackEntity;
import com.masterenglish.app.data.local.entity.PackTitleEntity;
import com.masterenglish.app.data.local.entity.PackWordEntity;
import com.masterenglish.app.data.local.entity.SenseExampleEntity;
import com.masterenglish.app.data.local.entity.SenseTranslationEntity;
import com.masterenglish.app.data.local.entity.WordEntity;
import com.masterenglish.app.data.local.entity.WordSenseEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Fills words, word_senses, their translations and examples, packs, pack titles and pack_words from assets/data/vocabulary.json (the Oxford word lists).
 * The content is versioned: when {@link #CONTENT_VERSION} changes the old content is replaced in one
 * transaction. Learners' word_progress rows for words that no longer exist go with them (foreign key cascade).
 */
@Singleton
public class LocalDataSeeder {

    /** Raise this when assets/data/vocabulary.json changes. */
    public static final int CONTENT_VERSION = 7;

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
            Content content = build(file);
            database.runInTransaction(() -> replaceContent(dao, content));
            prefs.edit().putInt(KEY_VERSION, CONTENT_VERSION).apply();
            Log.i(TAG, "Seeded " + content.words.size() + " words in " + content.packs.size() + " packs");
        } catch (IOException e) {
            Log.e(TAG, "Could not read bundled vocabulary", e);
        }
    }

    /**
     * Words and packs are upserted and only the ones missing from the new file are deleted, so a content update
     * keeps the progress of every word that is still there. Senses and pack membership are rebuilt.
     */
    static void replaceContent(WordDao dao, Content content) {
        List<PackEntity> packs = content.packs;
        List<WordEntity> words = content.words;
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
        dao.deleteSenseTranslations();
        dao.deleteSenseExamples();
        dao.deleteExampleTranslations();
        dao.deletePackTitles();
        dao.deletePackWords();
        dao.upsertPacks(packs);
        dao.upsertWords(words);
        dao.insertSenses(content.senses);
        dao.insertSenseTranslations(content.senseTranslations);
        dao.insertSenseExamples(content.senseExamples);
        dao.insertExampleTranslations(content.exampleTranslations);
        dao.insertPackTitles(content.packTitles);
        dao.insertPackWords(content.links);
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

    /** Everything {@link #build} reads out of the asset, as table rows. */
    static final class Content {
        final List<PackEntity> packs = new ArrayList<>();
        final List<WordEntity> words = new ArrayList<>();
        final List<WordSenseEntity> senses = new ArrayList<>();
        final List<SenseTranslationEntity> senseTranslations = new ArrayList<>();
        final List<SenseExampleEntity> senseExamples = new ArrayList<>();
        final List<ExampleTranslationEntity> exampleTranslations = new ArrayList<>();
        final List<PackTitleEntity> packTitles = new ArrayList<>();
        final List<PackWordEntity> links = new ArrayList<>();
    }

    static Content build(SeedModels.VocabularyFile file) {
        Content out = new Content();
        for (SeedModels.PackSeed p : file.packs) {
            PackEntity e = new PackEntity();
            e.id = p.id;
            e.title = p.title;
            e.cefrLevel = p.cefr_level;
            e.position = p.position;
            out.packs.add(e);
            if (p.titles != null) {
                for (Map.Entry<String, String> t : p.titles.entrySet()) {
                    if (t.getValue() != null && !t.getValue().isEmpty()) {
                        PackTitleEntity row = new PackTitleEntity();
                        row.packId = p.id;
                        row.lang = t.getKey();
                        row.text = t.getValue();
                        out.packTitles.add(row);
                    }
                }
            }
            if (p.words != null) {
                int position = 1;
                for (String wordId : p.words) {
                    PackWordEntity link = new PackWordEntity();
                    link.packId = p.id;
                    link.wordId = wordId;
                    link.position = position++;
                    out.links.add(link);
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
            out.words.add(e);
            int position = 0;
            if (w.senses != null) {
                for (SeedModels.SenseSeed s : w.senses) {
                    addSense(out, w.id, position++, s);
                }
            }
        }
        return out;
    }

    private static void addSense(Content out, String wordId, int position, SeedModels.SenseSeed s) {
        // Older assets carry only the Vietnamese fields; read them as the "vi" translation of the first example.
        Map<String, String> translations = s.translations;
        if (translations == null && s.definition_vi != null) {
            translations = java.util.Collections.singletonMap("vi", s.definition_vi);
        }
        List<SeedModels.ExampleSeed> examples = s.examples;
        if (examples == null && s.example != null) {
            SeedModels.ExampleSeed legacy = new SeedModels.ExampleSeed();
            legacy.text = s.example;
            if (s.example_vi != null) {
                legacy.translations = java.util.Collections.singletonMap("vi", s.example_vi);
            }
            examples = java.util.Collections.singletonList(legacy);
        }
        WordSenseEntity se = new WordSenseEntity();
        se.wordId = wordId;
        se.position = position;
        se.pos = s.pos;
        se.definitionEn = s.definition == null ? "" : s.definition;
        // The legacy columns keep the first Vietnamese meaning and the first example.
        se.definitionVi = translations == null ? null : translations.get("vi");
        se.exampleEn = examples == null || examples.isEmpty() ? null : examples.get(0).text;
        out.senses.add(se);
        if (translations != null) {
            for (Map.Entry<String, String> t : translations.entrySet()) {
                if (t.getValue() != null && !t.getValue().isEmpty()) {
                    SenseTranslationEntity row = new SenseTranslationEntity();
                    row.wordId = wordId;
                    row.sensePosition = position;
                    row.lang = t.getKey();
                    row.text = t.getValue();
                    out.senseTranslations.add(row);
                }
            }
        }
        if (examples != null) {
            int ex = 0;
            for (SeedModels.ExampleSeed x : examples) {
                if (x.text == null || x.text.isEmpty()) {
                    continue;
                }
                SenseExampleEntity row = new SenseExampleEntity();
                row.wordId = wordId;
                row.sensePosition = position;
                row.position = ex;
                row.text = x.text;
                out.senseExamples.add(row);
                if (x.translations != null) {
                    for (Map.Entry<String, String> t : x.translations.entrySet()) {
                        if (t.getValue() != null && !t.getValue().isEmpty()) {
                            ExampleTranslationEntity tr = new ExampleTranslationEntity();
                            tr.wordId = wordId;
                            tr.sensePosition = position;
                            tr.examplePosition = ex;
                            tr.lang = t.getKey();
                            tr.text = t.getValue();
                            out.exampleTranslations.add(tr);
                        }
                    }
                }
                ex++;
            }
        }
    }
}
