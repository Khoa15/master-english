package com.doradodo.app.data.local.seed;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.doradodo.app.data.local.AppDatabase;
import com.doradodo.app.data.local.dao.WordDao;
import com.doradodo.app.data.local.entity.ExampleTranslationEntity;
import com.doradodo.app.data.local.entity.LevelEntity;
import com.doradodo.app.data.local.entity.PackEntity;
import com.doradodo.app.data.local.entity.PackTitleEntity;
import com.doradodo.app.data.local.entity.PackWordEntity;
import com.doradodo.app.data.local.entity.SenseExampleEntity;
import com.doradodo.app.data.local.entity.SenseTranslationEntity;
import com.doradodo.app.data.local.entity.WordEntity;
import com.doradodo.app.data.local.entity.WordSenseEntity;

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
 * Fills words, word_senses, their translations and examples, packs, pack titles and pack_words from assets/data/vocabulary.<lang>.json, one file per language (the Oxford word lists for English).
 * The content is versioned: when {@link #CONTENT_VERSION} changes the old content is replaced in one
 * transaction. Learners' word_progress rows for words that no longer exist go with them (foreign key cascade).
 */
@Singleton
public class LocalDataSeeder {

    /** Raise this when any assets/data/vocabulary.<lang>.json changes. */
    public static final int CONTENT_VERSION = 9;

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
            Content content = readAll();
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
        dao.upsertLevels(content.levels);
        dao.upsertPacks(packs);
        dao.upsertWords(words);
        dao.insertSenses(content.senses);
        dao.insertSenseTranslations(content.senseTranslations);
        dao.insertSenseExamples(content.senseExamples);
        dao.insertExampleTranslations(content.exampleTranslations);
        dao.insertPackTitles(content.packTitles);
        dao.insertPackWords(content.links);
    }

    private static final java.util.regex.Pattern FILE = java.util.regex.Pattern.compile("vocabulary\\.([A-Za-z-]+)\\.json");

    /** Reads every data/vocabulary.<lang>.json and merges them, so words of other languages are never deleted as missing. */
    private Content readAll() throws IOException {
        String[] names = context.getAssets().list("data");
        java.util.Arrays.sort(names == null ? new String[0] : names);
        Content all = new Content();
        int files = 0;
        for (String name : names == null ? new String[0] : names) {
            java.util.regex.Matcher m = FILE.matcher(name);
            if (!m.matches()) {
                continue;
            }
            SeedModels.VocabularyFile file = read(name);
            if (file.lang == null || file.lang.isEmpty()) {
                file.lang = m.group(1);
            }
            merge(all, build(file));
            files++;
        }
        if (files == 0) {
            throw new IOException("no data/vocabulary.<lang>.json asset");
        }
        return all;
    }

    private static void merge(Content to, Content from) {
        to.levels.addAll(from.levels);
        to.packs.addAll(from.packs);
        to.words.addAll(from.words);
        to.senses.addAll(from.senses);
        to.senseTranslations.addAll(from.senseTranslations);
        to.senseExamples.addAll(from.senseExamples);
        to.exampleTranslations.addAll(from.exampleTranslations);
        to.packTitles.addAll(from.packTitles);
        to.links.addAll(from.links);
    }

    private SeedModels.VocabularyFile read(String name) throws IOException {
        try (InputStream stream = context.getAssets().open("data/" + name);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            SeedModels.VocabularyFile file = gson.fromJson(reader, SeedModels.VocabularyFile.class);
            if (file == null || file.words == null || file.packs == null) {
                throw new IOException(name + " is empty");
            }
            return file;
        }
    }

    /** Everything {@link #build} reads out of the asset, as table rows. */
    static final class Content {
        final List<LevelEntity> levels = new ArrayList<>();
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
        String lang = file.lang == null || file.lang.isEmpty() ? "en" : file.lang;
        if (file.levels != null) {
            for (SeedModels.LevelSeed l : file.levels) {
                LevelEntity e = new LevelEntity();
                e.system = l.system;
                e.code = l.code;
                e.position = l.position;
                out.levels.add(e);
            }
        }
        for (SeedModels.PackSeed p : file.packs) {
            PackEntity e = new PackEntity();
            e.id = p.id;
            e.title = p.title;
            e.lang = lang;
            e.levelSystem = p.level_system;
            e.levelCode = p.level_code;
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
            e.lang = lang;
            e.levelSystem = w.level_system;
            e.levelCode = w.level_code;
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
        se.definition = s.definition == null ? "" : s.definition;
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
