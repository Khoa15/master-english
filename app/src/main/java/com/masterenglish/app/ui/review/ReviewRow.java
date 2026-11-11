package com.masterenglish.app.ui.review;

/** One row in a Review tab. body is revealed when the row is expanded. */
public final class ReviewRow {

    public enum Kind { VOCABULARY, GRAMMAR, LISTENING, READING }

    public final String id;
    public final Kind kind;
    public final String title;
    public final String subtitle;
    public final String body;

    public ReviewRow(String id, Kind kind, String title, String subtitle, String body) {
        this.id = id;
        this.kind = kind;
        this.title = title;
        this.subtitle = subtitle;
        this.body = body;
    }
}
