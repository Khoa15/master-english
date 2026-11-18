package com.doradodo.app.i18n;

import android.content.Context;
import android.content.res.Resources;

import androidx.annotation.NonNull;

import com.doradodo.app.R;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * The languages the app is translated into: the one place the code asks "which languages, and what are they
 * called". The list itself is not written here; it comes from res/values/languages.xml, generated from
 * tools/i18n/langs.py, so adding or removing a language never touches this class or any screen.
 */
public final class AppLanguages {

    /** English is the default for both the display and the learning language. */
    public static final String DEFAULT = "en";

    private final List<String> codes;
    private final List<String> names;

    private AppLanguages(List<String> codes, List<String> names) {
        this.codes = Collections.unmodifiableList(codes);
        this.names = Collections.unmodifiableList(names);
    }

    public static AppLanguages of(@NonNull Context context) {
        Resources r = context.getResources();
        return new AppLanguages(Arrays.asList(r.getStringArray(R.array.language_codes)),
                Arrays.asList(r.getStringArray(R.array.language_names)));
    }

    /** BCP-47 codes in display order. */
    public List<String> codes() {
        return codes;
    }

    /**
     * The one fallback rule: a language the app has no translation for is shown in English. Everything that
     * depends on the display language (UI strings, catalogue text, word meanings) asks for it through here.
     */
    public String resolve(String code) {
        return code != null && codes.contains(code) ? code : DEFAULT;
    }

    public boolean supports(String code) {
        return codes.contains(code);
    }

    /**
     * A language written in itself ("Tiếng Việt"). A language the app is not translated into (one that is only
     * learned, such as Korean) is named by the platform instead.
     */
    public String nameOf(String code) {
        int i = codes.indexOf(code);
        if (i >= 0) {
            return names.get(i);
        }
        Locale l = Locale.forLanguageTag(code);
        String name = l.getDisplayLanguage(l);
        return name.isEmpty() ? code : name.substring(0, 1).toUpperCase(l) + name.substring(1);
    }
}
