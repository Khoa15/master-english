package com.masterenglish.app.ui.profile;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

import com.masterenglish.app.data.local.entity.DailyStudyLogEntity;
import com.masterenglish.app.data.local.entity.UserEntity;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.repository.AuthRepository;
import com.masterenglish.app.data.repository.UserRepository;

import java.time.YearMonth;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;
import dagger.hilt.android.qualifiers.ApplicationContext;

/** Profile data: the user, the month shown on the calendar, personal info and preferences. */
@HiltViewModel
public class ProfileViewModel extends ViewModel {

    public static final String LANGUAGE = "language";
    public static final String COUNTRY = "country";
    public static final String GENDER = "gender";
    public static final String BIRTHDAY = "birthday";
    public static final String DAILY_GOAL = "daily_goal";

    private final AppPreferences preferences;
    private final AuthRepository authRepository;
    private final SharedPreferences info;
    private final String userId;
    private final LiveData<UserEntity> user;
    private final MutableLiveData<YearMonth> month = new MutableLiveData<>(YearMonth.now());
    private final LiveData<List<DailyStudyLogEntity>> monthLog;

    @Inject
    public ProfileViewModel(@ApplicationContext Context context, AppPreferences preferences,
                            UserRepository userRepository, AuthRepository authRepository) {
        this.preferences = preferences;
        this.authRepository = authRepository;
        this.info = context.getSharedPreferences("profile_info", Context.MODE_PRIVATE);
        this.userId = preferences.getCurrentUserId() == null ? "" : preferences.getCurrentUserId();
        this.user = userRepository.observeUser(userId);
        this.monthLog = Transformations.switchMap(month,
                m -> userRepository.observeStudyLog(userId, m.atDay(1), m.atEndOfMonth()));
    }

    public LiveData<UserEntity> user() {
        return user;
    }

    public LiveData<YearMonth> month() {
        return month;
    }

    public LiveData<List<DailyStudyLogEntity>> monthLog() {
        return monthLog;
    }

    public void shiftMonth(int delta) {
        YearMonth m = month.getValue() == null ? YearMonth.now() : month.getValue();
        month.setValue(m.plusMonths(delta));
    }

    public String info(String key, String fallback) {
        return info.getString(key, fallback);
    }

    public void setInfo(String key, String value) {
        info.edit().putString(key, value).apply();
    }

    public int dailyGoal() {
        return info.getInt(DAILY_GOAL, 20);
    }

    public void setDailyGoal(int words) {
        info.edit().putInt(DAILY_GOAL, words).apply();
    }

    public AppPreferences preferences() {
        return preferences;
    }

    public boolean isGuest() {
        return userId.startsWith("guest-");
    }

    public void signOut() {
        authRepository.signOut();
    }
}
