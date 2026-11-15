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
}
