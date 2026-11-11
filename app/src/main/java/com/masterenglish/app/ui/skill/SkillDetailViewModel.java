package com.masterenglish.app.ui.skill;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.repository.ProgressRepository;
import com.masterenglish.app.data.repository.SkillProgress;
import com.masterenglish.app.data.repository.SyncRepository;
import com.masterenglish.app.utils.Callback;

import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** Loads the progress summary for one skill on the skill detail screen. */
@HiltViewModel
public class SkillDetailViewModel extends ViewModel {

    private final MutableLiveData<Map<String, SkillProgress.Summary>> skills = new MutableLiveData<>();
    private final ProgressRepository progressRepository;
    private final String userId;
    private final SyncRepository syncRepository;

    @Inject
    public SkillDetailViewModel(AppPreferences preferences, ProgressRepository progressRepository,
                                SyncRepository syncRepository) {
        this.syncRepository = syncRepository;
        this.progressRepository = progressRepository;
        this.userId = preferences.getCurrentUserId() == null ? "" : preferences.getCurrentUserId();
    }

    /** Pull-to-refresh: syncs with the server, then reloads the skill summaries. */
    public void refresh(Callback<Boolean> onDone) {
        syncRepository.syncInBackground(userId, success -> {
            load();
            onDone.onResult(success);
        });
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
