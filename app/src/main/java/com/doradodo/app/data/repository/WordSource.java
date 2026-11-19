package com.doradodo.app.data.repository;

/** A pack (or the learner's own studied words) that a practice mode can read from. */
public final class WordSource {
    public static final String LEARNED = "LEARNED";
    public final String id;
    public final String title;
    public final int count;

    WordSource(String id, String title, int count) {
        this.id = id;
        this.title = title;
        this.count = count;
    }
}
