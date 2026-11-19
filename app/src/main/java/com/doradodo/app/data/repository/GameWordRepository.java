package com.doradodo.app.data.repository;

import com.doradodo.app.data.local.dao.EntertainmentDao;
import com.doradodo.app.data.local.dao.WordDao;
import com.doradodo.app.data.local.entity.GameWord;
import com.doradodo.app.data.local.entity.PackEntity;
import com.doradodo.app.data.local.entity.PackWordEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

/** The words the games and practice modes ask, in the language being learned. Run off the main thread. */
@Singleton
public class GameWordRepository {

    private final EntertainmentDao dao;
    private final WordDao wordDao;
    private final LearnerContext learner;
    private final Random random = new Random();

    @Inject
    public GameWordRepository(EntertainmentDao dao, WordDao wordDao, LearnerContext learner) {
        this.dao = dao;
        this.wordDao = wordDao;
        this.learner = learner;
    }

    public int learnedCount() {
        return dao.countLearned(learner.userId(), learner.learningLanguage());
    }

    /** Studied words that have a meaning (and, if asked, a definition), random order. */
    public List<GameWord> learnedWords(int limit, boolean needDefinition) {
        return MeaningShortener.shorten(dao.getLearnedWords(learner.userId(), learner.learningLanguage(),
                learner.displayLanguage(), false, limit));
    }

    public List<GameWord> mistakeWords(int limit) {
        return MeaningShortener.shorten(dao.getMistakeWords(learner.userId(), learner.learningLanguage(),
                learner.displayLanguage(), limit));
    }

    /** Words to use as wrong answers; never one of {@code exclude}. */
    public List<GameWord> distractors(Set<String> exclude, int limit) {
        List<GameWord> out = new ArrayList<>();
        for (GameWord w : MeaningShortener.shorten(dao.getRandomWords(learner.userId(), learner.learningLanguage(),
                learner.displayLanguage(), limit + exclude.size() + 8))) {
            if (!exclude.contains(w.id) && out.size() < limit) {
                out.add(w);
            }
        }
        return out;
    }

    /** Pairs of synonyms in the language being learned; synonymy never crosses languages. */
    public List<SynonymPair> synonymPairs(int limit) {
        return SynonymPairBuilder.build(
                dao.getSynonymMembers(learner.userId(), learner.learningLanguage()), limit, random);
    }

    /** The learner's own studied words first, then every pack. */
    public List<WordSource> wordSources() {
        String lang = learner.learningLanguage();
        List<WordSource> out = new ArrayList<>();
        int learned = dao.countLearned(learner.userId(), lang);
        if (learned > 0) {
            out.add(new WordSource(WordSource.LEARNED, "", learned));
        }
        Map<String, Integer> sizes = new HashMap<>();
        for (PackWordEntity pw : wordDao.getPackWords(lang)) {
            sizes.merge(pw.packId, 1, Integer::sum);
        }
        for (PackEntity p : wordDao.getPacks(lang)) {
            out.add(new WordSource(p.id, p.title, sizes.getOrDefault(p.id, 0)));
        }
        return out;
    }

    /** Words of a source with their meaning; a pack keeps its list order, studied words come shuffled. */
    public List<GameWord> sourceWords(String sourceId, int limit) {
        if (WordSource.LEARNED.equals(sourceId)) {
            return learnedWords(limit, false);
        }
        return MeaningShortener.shorten(
                dao.getPackWordsInOrder(learner.userId(), sourceId, learner.displayLanguage(), limit));
    }
}
