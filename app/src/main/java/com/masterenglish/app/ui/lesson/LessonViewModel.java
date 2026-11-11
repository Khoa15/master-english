package com.masterenglish.app.ui.lesson;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.masterenglish.app.data.local.entity.LessonQuestionEntity;
import com.masterenglish.app.data.local.entity.RoadmapNodeEntity;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.repository.ContentRepository;
import com.masterenglish.app.data.repository.LessonReward;
import com.masterenglish.app.data.repository.ProgressRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

/** Holds the questions and the running score for one lesson. Survives rotation. */
@HiltViewModel
public class LessonViewModel extends ViewModel {

    private final ContentRepository contentRepository;
    private final ProgressRepository progressRepository;
    private final String userId;

    private final MutableLiveData<RoadmapNodeEntity> node = new MutableLiveData<>();
    private final MutableLiveData<List<LessonQuestionEntity>> questions = new MutableLiveData<>();
    private final MutableLiveData<LessonReward> reward = new MutableLiveData<>();

    private int correctCount;
    private long startedAt;
    private boolean finishing;

    @Inject
    public LessonViewModel(ContentRepository contentRepository, ProgressRepository progressRepository,
                           AppPreferences preferences) {
        this.contentRepository = contentRepository;
        this.progressRepository = progressRepository;
        this.userId = preferences.getCurrentUserId() == null ? "" : preferences.getCurrentUserId();
    }

    public void load(String nodeId) {
        if (questions.getValue() != null) {
            return;
        }
        contentRepository.getNode(nodeId, loaded -> {
            if (loaded == null) {
                questions.setValue(new ArrayList<>());
                return;
            }
            node.setValue(loaded);
            contentRepository.loadLessonQuestions(loaded, list -> {
                List<LessonQuestionEntity> shuffled = new ArrayList<>(list);
                Collections.shuffle(shuffled, new Random(System.nanoTime()));
                startedAt = System.currentTimeMillis();
                questions.setValue(shuffled);
            });
        });
    }

    public LiveData<RoadmapNodeEntity> node() {
        return node;
    }

    public LiveData<List<LessonQuestionEntity>> questions() {
        return questions;
    }

    public LiveData<LessonReward> reward() {
        return reward;
    }

    public void recordAnswer(LessonQuestionEntity question, boolean correct) {
        if (correct) {
            correctCount++;
        }
        progressRepository.recordAnswer(userId, question, correct);
    }

    public int correctCount() {
        return correctCount;
    }

    /** Saves the result once. Calling it again while saving is ignored. */
    public void finish() {
        RoadmapNodeEntity current = node.getValue();
        List<LessonQuestionEntity> list = questions.getValue();
        if (finishing || current == null || list == null) {
            return;
        }
        finishing = true;
        long minutes = Math.max(1, Math.round((System.currentTimeMillis() - startedAt) / 60000.0));
        progressRepository.completeLesson(userId, current, correctCount, list.size(), (int) minutes, reward::setValue);
    }
}
