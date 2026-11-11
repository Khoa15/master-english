package com.masterenglish.app.data.repository;

import java.util.Collections;
import java.util.List;

/** What the learner earned for finishing a lesson. */
public final class LessonReward {

    public final int stars;
    public final int experience;
    public final int gems;
    public final int streak;
    public final boolean firstCompletion;
    public final List<String> newBadges;

    public LessonReward(int stars, int experience, int gems, int streak, boolean firstCompletion,
                        List<String> newBadges) {
        this.stars = stars;
        this.experience = experience;
        this.gems = gems;
        this.streak = streak;
        this.firstCompletion = firstCompletion;
        this.newBadges = newBadges == null ? Collections.emptyList() : newBadges;
    }
}
