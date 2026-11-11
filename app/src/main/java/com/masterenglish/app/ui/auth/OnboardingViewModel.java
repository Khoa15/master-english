package com.masterenglish.app.ui.auth;

import androidx.lifecycle.ViewModel;

import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.local.prefs.OnboardingAnswers;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

/** Holds the onboarding answers while the flow is open, so they survive rotation. */
@HiltViewModel
public class OnboardingViewModel extends ViewModel {

    private final AppPreferences preferences;
    private final OnboardingAnswers answers = new OnboardingAnswers();

    @Inject
    public OnboardingViewModel(AppPreferences preferences) {
        this.preferences = preferences;
    }

    public OnboardingAnswers answers() {
        return answers;
    }

    public void saveAnswers() {
        preferences.saveOnboardingAnswers(answers);
    }
}
