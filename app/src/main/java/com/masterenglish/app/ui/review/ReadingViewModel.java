package com.masterenglish.app.ui.review;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.masterenglish.app.data.local.Enums.ParentType;
import com.masterenglish.app.data.local.entity.LessonQuestionEntity;
import com.masterenglish.app.data.local.entity.ReadingEntity;
import com.masterenglish.app.data.local.entity.VocabularyEntity;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.repository.ContentRepository;
import com.masterenglish.app.data.repository.ProgressRepository;
import com.masterenglish.app.utils.Callback;

import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

@HiltViewModel
public class ReadingViewModel extends ViewModel {

    private final ContentRepository content;
    private final ProgressRepository progress;
    private final String userId;
    private final MutableLiveData<ReadingEntity> passage = new MutableLiveData<>();
    private final MutableLiveData<List<LessonQuestionEntity>> questions = new MutableLiveData<>();

    @Inject
    public ReadingViewModel(ContentRepository content, ProgressRepository progress, AppPreferences preferences) {
        this.content = content;
        this.progress = progress;
        this.userId = preferences.getCurrentUserId() == null ? "" : preferences.getCurrentUserId();
    }

    public void load(String readingId) {
        if (passage.getValue() != null) {
            return;
        }
        content.getReading(readingId, entity -> {
            passage.setValue(entity);
            if (entity != null) {
                content.loadQuestions(ParentType.READING, entity.id, questions::setValue);
            }
        });
    }

    public LiveData<ReadingEntity> passage() {
        return passage;
    }

    public LiveData<List<LessonQuestionEntity>> questions() {
        return questions;
    }

    public void lookUp(String word, Callback<VocabularyEntity> callback) {
        content.findVocabulary(word, callback);
    }

    public ProgressRepository progress() {
        return progress;
    }

    public String userId() {
        return userId;
    }
}
