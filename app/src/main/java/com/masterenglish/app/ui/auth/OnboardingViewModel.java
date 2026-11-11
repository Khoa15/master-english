package com.masterenglish.app.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.masterenglish.app.data.local.prefs.AppPreferences;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

/** Holds the three onboarding answers while the pager is open. */
@HiltViewModel
public class OnboardingViewModel extends ViewModel {

    private final AppPreferences preferences;
    private final MutableLiveData<String> goal = new MutableLiveData<>("Giao tiếp trong công việc");
    private final MutableLiveData<String> level = new MutableLiveData<>("A1");
    private final MutableLiveData<Integer> minutes = new MutableLiveData<>(10);

    @Inject
    public OnboardingViewModel(AppPreferences preferences) {
        this.preferences = preferences;
    }

    public LiveData<String> goal() {
        return goal;
    }

    public LiveData<String> level() {
        return level;
    }

    public LiveData<Integer> minutes() {
        return minutes;
    }

    public void setGoal(String value) {
        goal.setValue(value);
    }

    public void setLevel(String value) {
        level.setValue(value);
    }

    public void setMinutes(int value) {
        minutes.setValue(value);
    }

    public void saveAnswers() {
        preferences.saveOnboardingAnswers(goal.getValue(), level.getValue(), minutes.getValue());
    }
}
