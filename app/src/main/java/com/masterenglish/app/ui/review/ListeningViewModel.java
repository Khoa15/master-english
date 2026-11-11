package com.masterenglish.app.ui.review;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.masterenglish.app.data.local.Enums.ParentType;
import com.masterenglish.app.data.local.entity.ListeningEntity;
import com.masterenglish.app.data.local.entity.LessonQuestionEntity;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.repository.ContentRepository;
import com.masterenglish.app.data.repository.ProgressRepository;

import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

@HiltViewModel
public class ListeningViewModel extends ViewModel {

    private final ContentRepository content;
    private final ProgressRepository progress;
    private final String userId;
    private final MutableLiveData<ListeningEntity> lesson = new MutableLiveData<>();
    private final MutableLiveData<List<LessonQuestionEntity>> questions = new MutableLiveData<>();

    @Inject
    public ListeningViewModel(ContentRepository content, ProgressRepository progress, AppPreferences preferences) {
        this.content = content;
        this.progress = progress;
        this.userId = preferences.getCurrentUserId() == null ? "" : preferences.getCurrentUserId();
    }

    public void load(String listeningId) {
        if (lesson.getValue() != null) {
            return;
        }
        content.getListening(listeningId, entity -> {
            lesson.setValue(entity);
            if (entity != null) {
                content.loadQuestions(ParentType.LISTENING, entity.id, questions::setValue);
            }
        });
    }

    public LiveData<ListeningEntity> lesson() {
        return lesson;
    }

    public LiveData<List<LessonQuestionEntity>> questions() {
        return questions;
    }

    public String userId() {
        return userId;
    }

    public ProgressRepository progress() {
        return progress;
    }

    public void saveDuration(String id, int seconds) {
        content.saveListeningDuration(id, seconds);
    }
}
