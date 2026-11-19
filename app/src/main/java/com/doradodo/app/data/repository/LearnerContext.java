package com.doradodo.app.data.repository;

import androidx.annotation.Nullable;

import com.doradodo.app.data.local.prefs.AppPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Who is learning what: the signed-in learner, the language being learned and the language the app is shown in. */
@Singleton
public class LearnerContext {

    private final AppPreferences preferences;
    private final LanguageRepository languages;

    @Inject
    public LearnerContext(AppPreferences preferences, LanguageRepository languages) {
        this.preferences = preferences;
        this.languages = languages;
    }

    /** The current learner, or null when nobody is signed in. */
    @Nullable
    public String currentUserId() {
        return preferences.getCurrentUserId();
    }

    /** The current learner, or an empty id (which matches no rows) when nobody is signed in. */
    public String userId() {
        String id = currentUserId();
        return id == null ? "" : id;
    }

    /** The language being learned: words, games, synonyms, packs and contests follow it. */
    public String learningLanguage() {
        return languages.learningLanguage();
    }

    /** The language the app is shown in: meanings and catalogue texts follow it. */
    public String displayLanguage() {
        return languages.displayLanguage();
    }
}
