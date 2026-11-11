package com.masterenglish.app.ui.review;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.masterenglish.app.data.local.Enums.ItemType;
import com.masterenglish.app.data.local.entity.VocabularyEntity;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.repository.ProgressRepository;
import com.masterenglish.app.utils.SrsAlgorithm;

import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

/** A session of SM-2 flashcards: the words that are due now. */
@HiltViewModel
public class FlashcardViewModel extends ViewModel {

    private static final int SESSION_SIZE = 20;

    private final ProgressRepository progressRepository;
    private final String userId;
    private final MutableLiveData<List<VocabularyEntity>> deck = new MutableLiveData<>();
    private int position;

    @Inject
    public FlashcardViewModel(ProgressRepository progressRepository, AppPreferences preferences) {
        this.progressRepository = progressRepository;
        this.userId = preferences.getCurrentUserId() == null ? "" : preferences.getCurrentUserId();
    }

    public void load() {
        if (deck.getValue() != null) {
            return;
        }
        progressRepository.loadDueVocabulary(userId, SESSION_SIZE, list -> deck.setValue(list));
    }

    public LiveData<List<VocabularyEntity>> deck() {
        return deck;
    }

    public int position() {
        return position;
    }

    public VocabularyEntity current() {
        List<VocabularyEntity> list = deck.getValue();
        return list == null || position >= list.size() ? null : list.get(position);
    }

    /** Grades the current card and moves on. */
    public void grade(SrsAlgorithm.Button button) {
        VocabularyEntity card = current();
        if (card == null) {
            return;
        }
        progressRepository.reviewCard(userId, ItemType.VOCABULARY, card.id, button, null);
        position++;
    }

    public boolean finished() {
        List<VocabularyEntity> list = deck.getValue();
        return list != null && position >= list.size();
    }

    public int total() {
        List<VocabularyEntity> list = deck.getValue();
        return list == null ? 0 : list.size();
    }
}
