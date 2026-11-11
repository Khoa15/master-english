package com.masterenglish.app.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.masterenglish.app.data.local.entity.UserSettingsEntity;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.repository.AuthRepository;
import com.masterenglish.app.data.repository.UserRepository;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

@HiltViewModel
public class SettingsViewModel extends ViewModel {

    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final String userId;
    private final LiveData<UserSettingsEntity> settings;

    @Inject
    public SettingsViewModel(AppPreferences preferences, UserRepository userRepository,
                             AuthRepository authRepository) {
        this.userRepository = userRepository;
        this.authRepository = authRepository;
        this.userId = preferences.getCurrentUserId() == null ? "" : preferences.getCurrentUserId();
        this.settings = userRepository.observeSettings(userId);
    }

    public LiveData<UserSettingsEntity> settings() {
        return settings;
    }

    public void saveSettings(UserSettingsEntity updated) {
        userRepository.saveSettings(updated);
    }

    public boolean isGuest() {
        return userId.startsWith("guest-");
    }

    public void signOut() {
        authRepository.signOut();
    }
}
