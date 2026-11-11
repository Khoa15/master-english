package com.masterenglish.app.data.repository;

import androidx.lifecycle.LiveData;

import com.masterenglish.app.data.local.dao.BadgeDao;
import com.masterenglish.app.data.local.entity.DailyStudyLogEntity;
import com.masterenglish.app.data.local.dao.UserDao;
import com.masterenglish.app.data.local.entity.BadgeEntity;
import com.masterenglish.app.data.local.entity.UserBadgeEntity;
import com.masterenglish.app.data.local.entity.UserEntity;
import com.masterenglish.app.data.local.entity.UserSettingsEntity;
import com.masterenglish.app.utils.AppExecutors;
import com.masterenglish.app.utils.Callback;
import com.masterenglish.app.utils.DateUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;


/** Learner profile, settings, streak and the daily study log. */
@Singleton
public class UserRepository {

    private final UserDao userDao;
    private final BadgeDao badgeDao;
    private final AppExecutors executors;

    @Inject
    public UserRepository(UserDao userDao, BadgeDao badgeDao, AppExecutors executors) {
        this.userDao = userDao;
        this.badgeDao = badgeDao;
        this.executors = executors;
    }

    public LiveData<List<BadgeEntity>> observeBadges() {
        return badgeDao.observeBadges();
    }

    public LiveData<List<UserBadgeEntity>> observeUnlockedBadges(String userId) {
        return badgeDao.observeUnlocked(userId);
    }

    public LiveData<UserEntity> observeUser(String userId) {
        return userDao.observeUser(userId);
    }

    public LiveData<UserSettingsEntity> observeSettings(String userId) {
        return userDao.observeSettings(userId);
    }

    public LiveData<List<DailyStudyLogEntity>> observeStudyLog(String userId, LocalDate from, LocalDate to) {
        return userDao.observeStudyLog(userId, DateUtils.dayKey(from), DateUtils.dayKey(to));
    }

    /** Creates the user and default settings the first time, otherwise refreshes the profile. */
    public void ensureUser(String userId, String email, String displayName, String googleId,
                           Callback<Void> done) {
        executors.diskIO().execute(() -> {
            ensureUserNow(userId, email, displayName, googleId);
            if (done != null) {
                executors.mainThread().execute(() -> done.onResult(null));
            }
        });
    }

    public void ensureUserNow(String userId, String email, String displayName, String googleId) {
        long now = System.currentTimeMillis();
        UserEntity existing = userDao.getUser(userId);
        if (existing == null) {
            UserEntity user = new UserEntity();
            user.id = userId;
            user.email = email == null ? userId + "@guest.local" : email;
            user.displayName = displayName == null || displayName.isEmpty() ? "Learner" : displayName;
            user.googleId = googleId;
            user.createdAt = now;
            user.updatedAt = now;
            userDao.upsertUser(user);
        } else {
            userDao.updateProfile(userId,
                    displayName == null || displayName.isEmpty() ? existing.displayName : displayName,
                    email == null ? existing.email : email,
                    googleId == null ? existing.googleId : googleId, now);
        }
        if (userDao.getSettings(userId) == null) {
            UserSettingsEntity settings = new UserSettingsEntity();
            settings.userId = userId;
            userDao.upsertSettings(settings);
        }
    }

    public void saveSettings(UserSettingsEntity settings) {
        executors.diskIO().execute(() -> userDao.upsertSettings(settings));
    }

    public void saveTargetMinutes(String userId, int minutes) {
        executors.diskIO().execute(() -> {
            UserSettingsEntity settings = userDao.getSettings(userId);
            if (settings != null) {
                settings.targetLearningMinutes = minutes;
                userDao.upsertSettings(settings);
            }
        });
    }

    public void updateDisplayName(String userId, String displayName) {
        executors.diskIO().execute(() ->
                userDao.updateProfile(userId, displayName, null, null, System.currentTimeMillis()));
    }

    public UserEntity getUserNow(String userId) {
        return userDao.getUser(userId);
    }

    public UserSettingsEntity getSettingsNow(String userId) {
        return userDao.getSettings(userId);
    }

    /**
     * Must run on a background thread. Advances the streak once per local day and
     * records study minutes. A gap of more than one day resets the streak to 1.
     *
     * @return the streak after this activity
     */
    public int touchActivityNow(String userId, int minutes) {
        UserEntity user = userDao.getUser(userId);
        if (user == null) {
            return 0;
        }
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        String todayKey = DateUtils.dayKey(today);
        int streak = user.streakCount;
        if (!todayKey.equals(user.lastActiveDate)) {
            boolean continued = user.lastActiveDate != null
                    && DateUtils.parseDayKey(user.lastActiveDate).plusDays(1).equals(today);
            streak = continued ? streak + 1 : 1;
            userDao.updateStreak(userId, streak, todayKey, System.currentTimeMillis());
        }
        if (minutes > 0) {
            userDao.addStudyMinutes(userId, todayKey, minutes);
        }
        return streak;
    }

    public void addRewardsNow(String userId, int experience, int gems) {
        userDao.addRewards(userId, experience, gems, System.currentTimeMillis());
    }
}
