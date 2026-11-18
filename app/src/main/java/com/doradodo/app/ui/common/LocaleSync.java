package com.doradodo.app.ui.common;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

/**
 * Makes the language of the app follow the learner's display language. Android picks the matching
 * values-xx/strings.xml; the change recreates the visible activity. Calling it with the language that is already
 * active does nothing, so it is safe to call on every start.
 */
public final class LocaleSync {

    private LocaleSync() {
    }

    public static void apply(String languageTag) {
        LocaleListCompat target = LocaleListCompat.forLanguageTags(languageTag);
        if (!AppCompatDelegate.getApplicationLocales().toLanguageTags().equals(target.toLanguageTags())) {
            AppCompatDelegate.setApplicationLocales(target);
        }
    }
}
