package com.masterenglish.app.data.repository;

/**
 * Turns a lesson result into roadmap stars. Pure logic with no Android dependencies,
 * so it can be unit tested without a device.
 *
 * <ul>
 *   <li>at least 90% correct: 3 stars (mastered)</li>
 *   <li>at least 60% correct: 2 stars</li>
 *   <li>below 60% correct: 1 star</li>
 * </ul>
 */
public final class LessonScorer {

    public static final int MAX_STARS = 3;
    public static final int MIN_STARS = 1;

    /** Ratio of correct answers needed for each star level. */
    static final double THREE_STAR_RATIO = 0.9;
    static final double TWO_STAR_RATIO = 0.6;

    private LessonScorer() {
    }

    /**
     * Stars earned for {@code correct} right answers out of {@code total}.
     * A lesson with no questions counts as the lowest level.
     */
    public static int starsFor(int correct, int total) {
        if (total <= 0) {
            return MIN_STARS;
        }
        double ratio = (double) correct / total;
        if (ratio >= THREE_STAR_RATIO) {
            return 3;
        }
        if (ratio >= TWO_STAR_RATIO) {
            return 2;
        }
        return MIN_STARS;
    }

    /** True when the stars mean the node is mastered. */
    public static boolean isMastered(int stars) {
        return stars >= MAX_STARS;
    }
}
