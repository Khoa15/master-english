package com.masterenglish.app.data.repository;

import com.masterenglish.app.data.local.dao.WordDao;
import com.masterenglish.app.data.local.entity.PackEntity;
import com.masterenglish.app.data.local.entity.PackWordEntity;
import com.masterenglish.app.data.local.entity.WordRow;
import com.masterenglish.app.data.local.entity.WordSenseEntity;
import com.masterenglish.app.utils.AppExecutors;
import com.masterenglish.app.utils.Callback;

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

    /** The language the translations are shown in until the learner can choose one. */
    public static final String DEFAULT_LANGUAGE = "vi";

    private final WordDao wordDao;
    private final AppExecutors executors;

    @Inject
    public WordRepository(WordDao wordDao, AppExecutors executors) {
        this.wordDao = wordDao;
        this.executors = executors;
    }

    public void loadContent(Callback<Content> callback) {
        executors.diskIO().execute(() -> {
            Content content = new Content(wordDao.getWordRows(DEFAULT_LANGUAGE), wordDao.getPacks(), wordDao.getPackWords());
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
