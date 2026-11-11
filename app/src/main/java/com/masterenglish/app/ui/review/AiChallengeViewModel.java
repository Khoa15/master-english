package com.masterenglish.app.ui.review;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.service.ai.AiTutorService;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

/** Conversation with the mascot. Messages are kept in memory for the screen and persisted by the service. */
@HiltViewModel
public class AiChallengeViewModel extends ViewModel {

    private final AiTutorService tutor;
    private final String userId;
    private final MutableLiveData<List<ChatItem>> messages = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> thinking = new MutableLiveData<>(false);

    @Inject
    public AiChallengeViewModel(AiTutorService tutor, AppPreferences preferences) {
        this.tutor = tutor;
        this.userId = preferences.getCurrentUserId() == null ? "" : preferences.getCurrentUserId();
    }

    public LiveData<List<ChatItem>> messages() {
        return messages;
    }

    public LiveData<Boolean> thinking() {
        return thinking;
    }

    public void start() {
        if (!messages.getValue().isEmpty()) {
            return;
        }
        thinking.setValue(true);
        tutor.startChallenge(userId, evaluation -> {
            append(ChatItem.fromMascot(evaluation));
            thinking.setValue(false);
        });
    }

    public void send(String text) {
        append(ChatItem.fromUser(text));
        thinking.setValue(true);
        tutor.sendMessage(userId, text, evaluation -> {
            append(ChatItem.fromMascot(evaluation));
            thinking.setValue(false);
        });
    }

    private void append(ChatItem item) {
        List<ChatItem> copy = new ArrayList<>(messages.getValue());
        copy.add(item);
        messages.setValue(copy);
    }
}
