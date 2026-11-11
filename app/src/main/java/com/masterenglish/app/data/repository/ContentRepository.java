package com.masterenglish.app.data.repository;

import androidx.lifecycle.LiveData;

import com.masterenglish.app.data.local.Enums.NodeType;
import com.masterenglish.app.data.local.Enums.ParentType;
import com.masterenglish.app.data.local.dao.ContentDao;
import com.masterenglish.app.data.local.entity.GrammarEntity;
import com.masterenglish.app.data.local.entity.LessonQuestionEntity;
import com.masterenglish.app.data.local.entity.ListeningEntity;
import com.masterenglish.app.data.local.entity.ReadingEntity;
import com.masterenglish.app.data.local.entity.RoadmapNodeEntity;
import com.masterenglish.app.data.local.entity.VocabularyEntity;
import com.masterenglish.app.utils.AppExecutors;
import com.masterenglish.app.utils.Callback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Read-only access to the seeded lesson content. */
@Singleton
public class ContentRepository {

    /** Caps keep a single lesson short; the boss draws from every lesson of its stage. */
    private static final int MAX_QUESTIONS_PER_LESSON = 12;
    private static final int BOSS_QUESTIONS = 10;

    private final ContentDao contentDao;
    private final AppExecutors executors;

    @Inject
    public ContentRepository(ContentDao contentDao, AppExecutors executors) {
        this.contentDao = contentDao;
        this.executors = executors;
    }

    public LiveData<List<ReadingEntity>> observeReading() {
        return contentDao.observeReading();
    }

    public LiveData<List<ListeningEntity>> observeListening() {
        return contentDao.observeListening();
    }

    public void getNode(String nodeId, Callback<RoadmapNodeEntity> callback) {
        executors.diskIO().execute(() -> deliver(callback, contentDao.getNode(nodeId)));
    }

    public void getGrammar(String id, Callback<GrammarEntity> callback) {
        executors.diskIO().execute(() -> deliver(callback, contentDao.getGrammar(id)));
    }

    public void getReading(String id, Callback<ReadingEntity> callback) {
        executors.diskIO().execute(() -> deliver(callback, contentDao.getReading(id)));
    }

    public void getListening(String id, Callback<ListeningEntity> callback) {
        executors.diskIO().execute(() -> deliver(callback, contentDao.getListening(id)));
    }

    public void getAllGrammar(Callback<List<GrammarEntity>> callback) {
        executors.diskIO().execute(() -> deliver(callback, contentDao.getAllGrammar()));
    }

    public void getAllVocabulary(Callback<List<VocabularyEntity>> callback) {
        executors.diskIO().execute(() -> deliver(callback, contentDao.getAllVocabulary()));
    }

    public void loadQuestions(ParentType type, String parentId, Callback<List<LessonQuestionEntity>> callback) {
        executors.diskIO().execute(() -> deliver(callback, contentDao.getQuestions(type, parentId)));
    }

    public void findVocabulary(String word, Callback<VocabularyEntity> callback) {
        executors.diskIO().execute(() -> deliver(callback, contentDao.findVocabularyByWord(word)));
    }

    public void saveListeningDuration(String id, int seconds) {
        executors.diskIO().execute(() -> contentDao.updateListeningDuration(id, seconds));
    }

    /** Loads the questions for one lesson node. Must not be called on the main thread. */
    public void loadLessonQuestions(RoadmapNodeEntity node, Callback<List<LessonQuestionEntity>> callback) {
        executors.diskIO().execute(() -> {
            List<LessonQuestionEntity> questions = node.nodeType == NodeType.CHECKPOINT_BOSS
                    ? bossQuestions(node)
                    : questionsForNodeNow(node);
            deliver(callback, questions);
        });
    }

    private List<LessonQuestionEntity> questionsForNodeNow(RoadmapNodeEntity node) {
        List<LessonQuestionEntity> questions = new ArrayList<>();
        switch (node.nodeType) {
            case VOCABULARY: {
                List<String> ids = new ArrayList<>();
                ids.add(node.id);
                for (VocabularyEntity v : contentDao.getVocabularyByNode(node.id)) {
                    ids.add(v.id);
                }
                questions.addAll(contentDao.getQuestionsForParents(ParentType.VOCABULARY, ids));
                break;
            }
            case GRAMMAR: {
                GrammarEntity grammar = contentDao.getGrammarByNode(node.id);
                if (grammar != null) {
                    questions.addAll(contentDao.getQuestions(ParentType.GRAMMAR, grammar.id));
                }
                break;
            }
            case LISTENING: {
                ListeningEntity listening = contentDao.getListeningByNode(node.id);
                if (listening != null) {
                    questions.addAll(contentDao.getQuestions(ParentType.LISTENING, listening.id));
                }
                break;
            }
            case READING: {
                ReadingEntity reading = contentDao.getReadingByNode(node.id);
                if (reading != null) {
                    questions.addAll(contentDao.getQuestions(ParentType.READING, reading.id));
                }
                break;
            }
            default:
                break;
        }
        if (questions.size() > MAX_QUESTIONS_PER_LESSON) {
            Collections.shuffle(questions, new Random(node.id.hashCode()));
            return new ArrayList<>(questions.subList(0, MAX_QUESTIONS_PER_LESSON));
        }
        return questions;
    }

    private List<LessonQuestionEntity> bossQuestions(RoadmapNodeEntity boss) {
        List<LessonQuestionEntity> pool = new ArrayList<>();
        for (RoadmapNodeEntity node : contentDao.getAllNodes()) {
            if (node.stageId == boss.stageId && node.nodeType != NodeType.CHECKPOINT_BOSS) {
                pool.addAll(questionsForNodeNow(node));
            }
        }
        Collections.shuffle(pool, new Random(boss.id.hashCode() + System.currentTimeMillis()));
        return new ArrayList<>(pool.subList(0, Math.min(BOSS_QUESTIONS, pool.size())));
    }

    /** Hands a value back on the main thread, where callers update the UI. */
    private <T> void deliver(Callback<T> callback, T value) {
        if (callback != null) {
            executors.mainThread().execute(() -> callback.onResult(value));
        }
    }
}
