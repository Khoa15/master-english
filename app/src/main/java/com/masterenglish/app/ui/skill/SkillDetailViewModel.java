package com.masterenglish.app.ui.skill;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.repository.ProgressRepository;
import com.masterenglish.app.data.repository.SkillProgress;

import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** Loads the progress summary for one skill on the skill detail screen. */
@HiltViewModel
public class SkillDetailViewModel extends ViewModel {

    private final MutableLiveData<Map<String, SkillProgress.Summary>> skills = new MutableLiveData<>();
    private final ProgressRepository progressRepository;
    private final String userId;

    @Inject
    public SkillDetailViewModel(AppPreferences preferences, ProgressRepository progressRepository) {
        this.progressRepository = progressRepository;
        this.userId = preferences.getCurrentUserId() == null ? "" : preferences.getCurrentUserId();
    }

    public LiveData<Map<String, SkillProgress.Summary>> skills() {
        return skills;
    }

    public void load() {
        if (skills.getValue() == null) {
            progressRepository.loadSkillSummaries(userId, skills::setValue);
        }
    }
}
