package com.masterenglish.app.ui.tutor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.masterenglish.app.data.local.dao.SpeakingScenarioDao;
import com.masterenglish.app.data.local.entity.SpeakingScenarioEntity;
import com.masterenglish.app.data.local.prefs.AppPreferences;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

/** Speaking situations the learner created in the Tutor tab, newest first. */
@HiltViewModel
public class TutorViewModel extends ViewModel {

    private static final ExecutorService DISK = Executors.newSingleThreadExecutor();

    private final SpeakingScenarioDao scenarioDao;
    private final String userId;
    private final LiveData<List<SpeakingScenarioEntity>> scenarios;

    @Inject
    public TutorViewModel(SpeakingScenarioDao scenarioDao, AppPreferences preferences) {
        this.scenarioDao = scenarioDao;
        this.userId = preferences.getCurrentUserId() == null ? "" : preferences.getCurrentUserId();
        this.scenarios = scenarioDao.observeForUser(userId);
    }

    public LiveData<List<SpeakingScenarioEntity>> scenarios() {
        return scenarios;
    }

    /** Saves a new situation off the main thread. */
    public void saveScenario(String description) {
        SpeakingScenarioEntity row = new SpeakingScenarioEntity();
        row.id = UUID.randomUUID().toString();
        row.userId = userId;
        row.description = description;
        row.createdAt = System.currentTimeMillis();
        DISK.execute(() -> scenarioDao.insert(row));
    }
}
