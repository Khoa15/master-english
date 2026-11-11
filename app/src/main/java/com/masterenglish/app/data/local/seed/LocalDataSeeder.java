package com.masterenglish.app.data.local.seed;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.masterenglish.app.data.local.AppDatabase;
import com.masterenglish.app.data.local.dao.ContentDao;
import com.masterenglish.app.data.local.seed.SeedModels.GrammarSeed;
import com.masterenglish.app.data.local.seed.SeedModels.ListeningSeed;
import com.masterenglish.app.data.local.seed.SeedModels.ReadingSeed;
import com.masterenglish.app.data.local.seed.SeedModels.VocabularySeed;
import com.masterenglish.app.utils.AudioResources;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Fills the local database from the four bundled JSON files the first time the app
 * runs (or whenever the vocabulary table is empty). Runs in one transaction, so a
 * crash mid-way leaves the database empty and the next launch simply retries.
 */
@Singleton
public class LocalDataSeeder {

    private static final String TAG = "LocalDataSeeder";
    private static final String ASSET_DIR = "data/";

    private final Context context;
    private final AppDatabase database;
    private final Gson gson = new Gson();

    @Inject
    public LocalDataSeeder(@ApplicationContext Context context, AppDatabase database) {
        this.context = context;
        this.database = database;
    }

    public synchronized void seedIfEmpty() {
        ContentDao content = database.contentDao();
        if (content.countVocabulary() > 0) {
            return;
        }
        try {
            List<VocabularySeed> vocabulary = read("vocabulary.json", new TypeToken<List<VocabularySeed>>() { });
            List<GrammarSeed> grammar = read("grammar.json", new TypeToken<List<GrammarSeed>>() { });
            List<ReadingSeed> reading = read("reading.json", new TypeToken<List<ReadingSeed>>() { });
            List<ListeningSeed> listening = read("listening.json", new TypeToken<List<ListeningSeed>>() { });

            ContentBuilder.Bundle bundle = ContentBuilder.build(vocabulary, grammar, reading, listening,
                    measureListeningDurations(listening));
            database.runInTransaction(() -> persist(bundle));
            Log.i(TAG, "Seeded " + bundle.nodes.size() + " nodes and " + bundle.questions.size() + " questions");
        } catch (IOException e) {
            Log.e(TAG, "Could not read bundled content", e);
        }
    }

    private void persist(ContentBuilder.Bundle bundle) {
        ContentDao content = database.contentDao();
        content.insertStages(bundle.stages);
        content.insertNodes(bundle.nodes);
        content.insertVocabulary(bundle.vocabulary);
        content.insertGrammar(bundle.grammar);
        content.insertReading(bundle.reading);
        content.insertListening(bundle.listening);
        content.insertQuestions(bundle.questions);
        database.badgeDao().insertBadges(bundle.badges);
    }

    private <T> T read(String fileName, TypeToken<T> type) throws IOException {
        try (InputStream stream = context.getAssets().open(ASSET_DIR + fileName);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, type.getType());
        }
    }

    private Map<String, Integer> measureListeningDurations(List<ListeningSeed> listening) {
        Map<String, Integer> durations = new HashMap<>();
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            for (ListeningSeed item : listening) {
                Uri uri = AudioResources.resolve(context, ContentBuilder.audioPathFor(item.id));
                if (uri == null) {
                    continue;
                }
                try {
                    retriever.setDataSource(context, uri);
                    String millis = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if (millis != null) {
                        durations.put(item.id, (int) Math.round(Long.parseLong(millis) / 1000.0));
                    }
                } catch (RuntimeException e) {
                    Log.w(TAG, "No duration for " + item.id, e);
                }
            }
        } finally {
            try {
                retriever.release();
            } catch (IOException e) {
                Log.w(TAG, "Could not release metadata retriever", e);
            }
        }
        return durations;
    }
}
