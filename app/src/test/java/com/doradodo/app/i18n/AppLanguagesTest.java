package com.doradodo.app.i18n;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AppLanguagesTest {

    private final AppLanguages languages = AppLanguages.of(ApplicationProvider.getApplicationContext());

    @Test
    public void supportedLanguageIsKept() {
        assertEquals("zh", languages.resolve("zh"));
        assertTrue(languages.supports("vi"));
    }

    @Test
    public void unsupportedOrMissingLanguageFallsBackToEnglish() {
        assertEquals("en", languages.resolve("ko"));
        assertEquals("en", languages.resolve("ar"));
        assertEquals("en", languages.resolve(""));
        assertEquals("en", languages.resolve(null));
        assertFalse(languages.supports("ar"));
    }

    @Test
    public void unlistedLanguageStillGetsAName() {
        assertEquals("Tiếng Việt", languages.nameOf("vi"));
        assertFalse(languages.nameOf("ko").isEmpty());
    }
}
