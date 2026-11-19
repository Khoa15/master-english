package com.doradodo.app.ui.vocab;

import androidx.annotation.Nullable;

import com.doradodo.app.utils.TextUtils;

import java.util.List;

/** Decides whether what the learner typed or said is the word. Pure, so it is tested without a screen. */
public final class AnswerChecker {

    private AnswerChecker() {
    }

    /** Typed answers must equal the word once case, accents and punctuation are ignored. */
    public static boolean typedMatches(String typed, String term) {
        return TextUtils.normalizeAnswer(typed).equals(TextUtils.normalizeAnswer(term));
    }

    /** A spoken answer counts when any of the speech recogniser's guesses contains the word. */
    public static boolean spokenMatches(@Nullable List<String> heard, String term) {
        if (heard == null) {
            return false;
        }
        String wanted = TextUtils.normalizeAnswer(term);
        for (String h : heard) {
            if (TextUtils.normalizeAnswer(h).contains(wanted)) {
                return true;
            }
        }
        return false;
    }
}
