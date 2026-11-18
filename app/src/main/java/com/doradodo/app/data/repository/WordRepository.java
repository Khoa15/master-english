package com.doradodo.app.data.repository;

import com.doradodo.app.data.local.dao.WordDao;
import com.doradodo.app.data.local.entity.PackEntity;
import com.doradodo.app.data.local.entity.PackWordEntity;
import com.doradodo.app.data.local.entity.WordRow;
import com.doradodo.app.data.local.entity.WordSenseEntity;
import com.doradodo.app.utils.AppExecutors;
import com.doradodo.app.utils.Callback;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Offline-first access to the word content. Every call runs on the disk executor and answers on the main thread. */
@Singleton
public class WordRepository {

    /** All words, packs and pack membership, loaded together. */
    public static final class Content {
        public final List<WordRow> words;
        public final List<PackEntity> packs;
        public final List<PackWordEntity> links;

        Content(List<WordRow> words, List<PackEntity> packs, List<PackWordEntity> links) {
            this.words = words;
            this.packs = packs;
            this.links = links;
        }
    }

    private final WordDao wordDao;
    private final LanguageRepository languages;
    private final AppExecutors executors;

    @Inject
    public WordRepository(WordDao wordDao, LanguageRepository languages, AppExecutors executors) {
        this.wordDao = wordDao;
        this.languages = languages;
        this.executors = executors;
    }

    public void loadContent(Callback<Content> callback) {
        executors.diskIO().execute(() -> {
            // Words of the language being learned, with their meanings in the language the app is shown in.
            String learning = languages.learningLanguage();
            Content content = new Content(wordDao.getWordRows(learning, languages.displayLanguage()),
                    wordDao.getPacks(learning), wordDao.getPackWords(learning));
            executors.mainThread().execute(() -> callback.onResult(content));
        });
    }

    public void loadSenses(String wordId, Callback<List<WordSenseEntity>> callback) {
        executors.diskIO().execute(() -> {
            List<WordSenseEntity> senses = wordDao.getSenses(wordId);
            executors.mainThread().execute(() -> callback.onResult(senses));
        });
    }
}
