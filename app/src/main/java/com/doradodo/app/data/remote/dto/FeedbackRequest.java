package com.doradodo.app.data.remote.dto;

/** One feedback message from the Góp ý screen, as sent to POST v1/feedback. */
public final class FeedbackRequest {

    // No user id here: the server takes the identity from the verified ID token, never from the body.
    /** 1..5, or 0 when the learner gave no rating. */
    public int rating;
    /** "Bug", "Idea", "Question" or "Other". */
    public String type;
    public String message;
    public String appVersion;
    public int sdkInt;
    public String locale;
    public long sentAt;
}
