package com.masterenglish.app.ui.profile;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.masterenglish.app.data.local.entity.BadgeEntity;
import com.masterenglish.app.data.local.entity.DailyStudyLogEntity;
import com.masterenglish.app.data.local.entity.UserBadgeEntity;
import com.masterenglish.app.data.local.entity.UserEntity;
import com.masterenglish.app.data.local.entity.UserSettingsEntity;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.repository.AuthRepository;
import com.masterenglish.app.data.repository.ProgressRepository;
import com.masterenglish.app.data.repository.SkillProgress;
import com.masterenglish.app.data.repository.UserRepository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

@HiltViewModel
public class ProfileViewModel extends ViewModel {

    private final AppPreferences preferences;
    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final ProgressRepository progressRepository;
    private final String userId;
    private final LiveData<UserEntity> user;
    private final LiveData<UserSettingsEntity> settings;
    private final LiveData<List<DailyStudyLogEntity>> studyLog;
    private final LiveData<List<BadgeEntity>> badges;
    private final LiveData<List<UserBadgeEntity>> unlocked;
    private final MutableLiveData<Map<String, SkillProgress.Summary>> skills = new MutableLiveData<>();

    @Inject
    public ProfileViewModel(AppPreferences preferences, UserRepository userRepository, AuthRepository authRepository,
                            ProgressRepository progressRepository) {
        this.preferences = preferences;
        this.userRepository = userRepository;
        this.authRepository = authRepository;
        this.progressRepository = progressRepository;
        this.userId = preferences.getCurrentUserId() == null ? "" : preferences.getCurrentUserId();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        this.user = userRepository.observeUser(userId);
        this.settings = userRepository.observeSettings(userId);
        this.studyLog = userRepository.observeStudyLog(userId, today.minusDays(6), today);
        this.badges = userRepository.observeBadges();
        this.unlocked = userRepository.observeUnlockedBadges(userId);
        progressRepository.loadSkillSummaries(userId, skills::setValue);
    }

    public LiveData<Map<String, SkillProgress.Summary>> skills() {
        return skills;
    }

    public String userId() {
        return userId;
    }

    public LiveData<UserEntity> user() {
        return user;
    }

    public LiveData<UserSettingsEntity> settings() {
        return settings;
    }

    public LiveData<List<DailyStudyLogEntity>> studyLog() {
        return studyLog;
    }

    public LiveData<List<BadgeEntity>> badges() {
        return badges;
    }

    public LiveData<List<UserBadgeEntity>> unlocked() {
        return unlocked;
    }

    public void saveSettings(UserSettingsEntity updated) {
        userRepository.saveSettings(updated);
    }

    public void saveGeminiKey(String key) {
        preferences.setGeminiApiKey(key);
    }

    public String geminiKey() {
        String key = preferences.getGeminiApiKey();
        return key == null ? "" : key;
    }

    public boolean isGuest() {
        return userId.startsWith("guest-");
    }

    public void signOut() {
        authRepository.signOut();
    }
}
