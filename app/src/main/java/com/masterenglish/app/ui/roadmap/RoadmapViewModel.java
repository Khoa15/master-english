package com.masterenglish.app.ui.roadmap;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.masterenglish.app.data.local.entity.UserEntity;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.repository.ProgressRepository;
import com.masterenglish.app.data.repository.RoadmapItem;
import com.masterenglish.app.data.repository.UserRepository;
import com.masterenglish.app.service.ai.AiTutorService;
import com.masterenglish.app.utils.Callback;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

@HiltViewModel
public class RoadmapViewModel extends ViewModel {

    private final String userId;
    private final AiTutorService tutor;
    private final LiveData<List<RoadmapItem>> roadmap;
    private final LiveData<UserEntity> user;

    @Inject
    public RoadmapViewModel(AppPreferences preferences, ProgressRepository progressRepository,
                            UserRepository userRepository, AiTutorService tutor) {
        this.userId = preferences.getCurrentUserId() == null ? "" : preferences.getCurrentUserId();
        this.tutor = tutor;
        this.roadmap = userId.isEmpty()
                ? new MutableLiveData<>(new ArrayList<>())
                : progressRepository.observeRoadmap(userId);
        this.user = userRepository.observeUser(userId);
    }

    public LiveData<List<RoadmapItem>> roadmap() {
        return roadmap;
    }

    public LiveData<UserEntity> user() {
        return user;
    }

    public String userId() {
        return userId;
    }

    public void dailyTip(Callback<String> callback) {
        tutor.dailyTip(userId, callback);
    }
}
