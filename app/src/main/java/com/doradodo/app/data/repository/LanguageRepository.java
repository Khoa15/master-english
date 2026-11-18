package com.doradodo.app.data.repository;

import androidx.annotation.Nullable;

import com.doradodo.app.data.local.dao.UserDao;
import com.doradodo.app.i18n.AppLanguages;
import com.doradodo.app.data.local.dao.WordDao;
import com.doradodo.app.data.local.entity.UserSettingsEntity;
import com.doradodo.app.data.local.prefs.AppPreferences;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * The two languages of a learner: the language the app is shown in (display) and the one being learned.
 * They are independent (learning Korean with an English display is normal). A learner studies one language at
 * a time and may switch; progress is kept per word, so switching back finds everything as it was.
 * All calls hit Room: run them off the main thread.
 */
@Singleton
public class LanguageRepository {

    /** Both default to English: the app is designed for English today. */
    public static final String DEFAULT = AppLanguages.DEFAULT;

    private static final String PENDING_PREFS = "language_pending";

    private final UserDao userDao;
    private final WordDao wordDao;
    private final AppPreferences preferences;
    private final AppLanguages languages;

    @Inject
    public LanguageRepository(@dagger.hilt.android.qualifiers.ApplicationContext android.content.Context context,
                              UserDao userDao, WordDao wordDao, AppPreferences preferences) {
        this.languages = AppLanguages.of(context);
        this.userDao = userDao;
        this.wordDao = wordDao;
        this.preferences = preferences;
    }

    @Nullable
    private String uid() {
        return preferences.getCurrentUserId();
    }

    public String displayLanguage() {
        String id = uid();
        String lang = id == null ? null : userDao.getDisplayLanguage(id);
        return languages.resolve(lang);
    }

    /** The language whose words, packs, games and contests the learner sees. */
    public String learningLanguage() {
        String id = uid();
        String lang = id == null ? null : userDao.getLearningLanguage(id);
        return lang == null || lang.isEmpty() ? DEFAULT : lang;
    }

    /** Languages that have vocabulary in this build: the ones the learner can switch to. */
    public List<String> learnableLanguages() {
        return wordDao.getWordLanguages();
    }

    /** Switches the language being learned. Returns false when there is no such vocabulary or no learner. */
    public boolean setLearningLanguage(String lang) {
        String id = uid();
        if (id == null || !wordDao.getWordLanguages().contains(lang)) {
            return false;
        }
        if (userDao.setLearningLanguage(id, lang) == 0) {
            UserSettingsEntity settings = new UserSettingsEntity();
            settings.userId = id;
            settings.learningLanguage = lang;
            userDao.upsertSettings(settings);
        }
        return true;
    }

    /** Changes the language the app is shown in. Returns false when it is not a supported display language. */
    public boolean setDisplayLanguage(String lang) {
        String id = uid();
        if (id == null || !languages.supports(lang)) {
            return false;
        }
        if (userDao.setDisplayLanguage(id, lang) == 0) {
            UserSettingsEntity settings = new UserSettingsEntity();
            settings.userId = id;
            settings.displayLanguage = lang;
            userDao.upsertSettings(settings);
        }
        return true;
    }

    /**
     * The language chosen on the first onboarding screen is kept in the preferences until the learner has an
     * account; this copies it into the learner's settings once, and returns the display language to use.
     */
    public String applyOnboardingChoice() {
        String chosen = preferences.getOnboardingAnswers().language;
        if (!chosen.isEmpty() && !preferences.isOnboardingLanguageApplied() && languages.supports(chosen)) {
            if (setDisplayLanguage(chosen)) {
                preferences.setOnboardingLanguageApplied(true);
            }
        }
        return displayLanguage();
    }
}
