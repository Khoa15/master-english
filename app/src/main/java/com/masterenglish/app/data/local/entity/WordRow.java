package com.masterenglish.app.data.local.entity;

import androidx.annotation.Nullable;

/** A word joined with its first sense: what the lists and practice rounds need. */
public class WordRow {
    public String id;
    public String term;
    @Nullable public String ipa;
    public String cefrLevel;
    public int rank;
    @Nullable public String pos;
    @Nullable public String definition;
    @Nullable public String example;
    /** Short meaning of the first sense in the requested language, or null when there is none. */
    @Nullable public String translation;
    /** The first example's translation in the requested language, or null. */
    @Nullable public String exampleTranslation;
}
