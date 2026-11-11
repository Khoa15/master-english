package com.masterenglish.app.ui.review;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.masterenglish.app.data.local.entity.GrammarEntity;
import com.masterenglish.app.data.local.entity.ListeningEntity;
import com.masterenglish.app.data.local.entity.ReadingEntity;
import com.masterenglish.app.data.local.entity.UserWeaknessEntity;
import com.masterenglish.app.data.local.entity.VocabularyEntity;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.repository.ContentRepository;
import com.masterenglish.app.data.repository.ProgressRepository;
import com.masterenglish.app.utils.Callback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

@HiltViewModel
public class ReviewViewModel extends ViewModel {

    private final String userId;
    private final ProgressRepository progressRepository;
    private final ContentRepository contentRepository;

    private final LiveData<List<UserWeaknessEntity>> weaknesses;
    private final LiveData<Integer> dueCount;
    private final LiveData<List<ListeningEntity>> listening;
    private final LiveData<List<ReadingEntity>> reading;
    private final MutableLiveData<List<VocabularyEntity>> vocabulary = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<GrammarEntity>> grammar = new MutableLiveData<>(new ArrayList<>());

    @Inject
    public ReviewViewModel(AppPreferences preferences, ProgressRepository progressRepository,
                           ContentRepository contentRepository) {
        this.userId = preferences.getCurrentUserId() == null ? "" : preferences.getCurrentUserId();
        this.progressRepository = progressRepository;
        this.contentRepository = contentRepository;
        this.weaknesses = progressRepository.observeWeaknesses(userId);
        this.dueCount = progressRepository.observeDueCount(userId);
        this.listening = contentRepository.observeListening();
        this.reading = contentRepository.observeReading();
        contentRepository.getAllVocabulary(list -> vocabulary.setValue(list));
        contentRepository.getAllGrammar(list -> grammar.setValue(list));
    }

    public String userId() {
        return userId;
    }

    public LiveData<List<UserWeaknessEntity>> weaknesses() {
        return weaknesses;
    }

    public LiveData<Integer> dueCount() {
        return dueCount;
    }

    public LiveData<List<ListeningEntity>> listening() {
        return listening;
    }

    public LiveData<List<ReadingEntity>> reading() {
        return reading;
    }

    public LiveData<List<VocabularyEntity>> vocabulary() {
        return vocabulary;
    }

    public LiveData<List<GrammarEntity>> grammar() {
        return grammar;
    }

    public void findWord(String word, Callback<VocabularyEntity> callback) {
        contentRepository.findVocabulary(word, callback);
    }

    static List<VocabularyEntity> sorted(List<VocabularyEntity> items) {
        List<VocabularyEntity> copy = new ArrayList<>(items);
        Collections.sort(copy, (a, b) -> a.word.compareToIgnoreCase(b.word));
        return copy;
    }
}
