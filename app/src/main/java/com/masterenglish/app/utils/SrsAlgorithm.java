package com.masterenglish.app.utils;

/**
 * SuperMemo SM-2 spaced repetition.
 *
 * <p>Quality grades (0..5): 0-2 = failed recall, 3 = hard, 4 = good, 5 = easy.
 * A failed recall resets the repetition count and schedules a review tomorrow.
 * A successful recall schedules the next review after 1 day, then 6 days, then
 * {@code previousInterval * easinessFactor} days.
 */
public final class SrsAlgorithm {

    public static final double DEFAULT_EASINESS = SuperMemo2Helper.DEFAULT_FACTOR;
    public static final double MIN_EASINESS = SuperMemo2Helper.MIN_FACTOR;
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

    /** Computes the next state after the learner answered with {@code quality}. */
    public static State review(State current, int quality) {
        SuperMemo2Helper.SrsResult next = SuperMemo2Helper.calculateNextReview(
                quality, current.repetitions, current.easinessFactor, current.intervalDays);
        return new State(next.stabilityFactor, next.repetitionNumber, next.intervalDays);
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
