package com.doradodo.app.data.local.entity;

import androidx.annotation.Nullable;

/** A word as the games need it: the term, its English definition, a short meaning in one language and an example. */
public class GameWord {
    public String id;
    public String term;
    @Nullable public String definition;
    @Nullable public String translation;
    /** The translation when the word has one in the learner's language, otherwise the English definition. */
    @Nullable public String meaning;
    @Nullable public String example;
    public int timesWrong;
}
