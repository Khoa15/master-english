package com.doradodo.app.utils;

/**
 * SuperMemo SM-2 spaced repetition.
 *
 * <p>Quality grades (0..5): 0-2 = failed recall, 3 = hard, 4 = good, 5 = easy.
 * A failed recall resets the repetition count and schedules a review tomorrow.
 * A successful recall schedules the next review after 1 day, then 6 days, then
 * {@code previousInterval * easinessFactor} days.
 */
public final class SrsAlgorithm {

    public static final double DEFAULT_EASINESS = 2.5;
    public static final double MIN_EASINESS = 1.3;
    public static final int QUALITY_FAILED = 1;
    public static final int QUALITY_HARD = 3;
    public static final int QUALITY_GOOD = 4;
    public static final int QUALITY_EASY = 5;

    private SrsAlgorithm() {
    }

    /** Immutable scheduling state of one item. */
    public static final class State {
        public final double easinessFactor;
        public final int repetitions;
        public final int intervalDays;

        public State(double easinessFactor, int repetitions, int intervalDays) {
            this.easinessFactor = easinessFactor;
            this.repetitions = repetitions;
            this.intervalDays = intervalDays;
        }

        public static State initial() {
            return new State(DEFAULT_EASINESS, 0, 1);
        }
    }

    /**
     * Computes the next state after the learner answered with {@code quality} (0 = blackout .. 5 = perfect).
     *
     * @throws IllegalArgumentException if quality is outside 0..5
     */
    public static State review(State current, int quality) {
        if (quality < 0 || quality > 5) {
            throw new IllegalArgumentException("SM-2 quality must be within 0..5, was " + quality);
        }
        // EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)), never below the minimum
        double factor = current.easinessFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        if (factor < MIN_EASINESS) {
            factor = MIN_EASINESS;
        }
        if (quality < 3) {
            // Failed recall: restart the repetition chain, review tomorrow.
            return new State(factor, 0, 1);
        }
        int repetitions = current.repetitions + 1;
        int interval;
        if (repetitions == 1) {
            interval = 1;
        } else if (repetitions == 2) {
            interval = 6;
        } else {
            interval = (int) Math.round(current.intervalDays * factor);
        }
        return new State(factor, repetitions, interval);
    }

    /** Maps the four flashcard buttons (Again, Hard, Good, Easy) to SM-2 quality grades. */
    public static int qualityForButton(Button button) {
        switch (button) {
            case AGAIN:
                return QUALITY_FAILED;
            case HARD:
                return QUALITY_HARD;
            case GOOD:
                return QUALITY_GOOD;
            default:
                return QUALITY_EASY;
        }
    }

    public enum Button { AGAIN, HARD, GOOD, EASY }
}
