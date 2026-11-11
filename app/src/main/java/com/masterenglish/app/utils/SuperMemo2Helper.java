package com.masterenglish.app.utils;

/**
 * Stateless SuperMemo-2 calculator. Used by {@link SrsAlgorithm}, which keeps the state object
 * and the flashcard button mapping; the formula lives only here.
 */
public final class SuperMemo2Helper {

    public static final double DEFAULT_FACTOR = 2.5;
    public static final double MIN_FACTOR = 1.3;

    private SuperMemo2Helper() {
    }

    public static final class SrsResult {
        public final double stabilityFactor;
        public final int intervalDays;
        public final int repetitionNumber;

        public SrsResult(double stabilityFactor, int intervalDays, int repetitionNumber) {
            this.stabilityFactor = stabilityFactor;
            this.intervalDays = intervalDays;
            this.repetitionNumber = repetitionNumber;
        }
    }

    /**
     * @param quality        recall grade, 0 (complete blackout) to 5 (perfect response)
     * @param prevRepetition number of successful repetitions so far
     * @param prevFactor     easiness factor (EF), default 2.5
     * @param prevInterval   previous interval in days
     * @throws IllegalArgumentException if quality is outside 0..5
     */
    public static SrsResult calculateNextReview(int quality, int prevRepetition, double prevFactor, int prevInterval) {
        if (quality < 0 || quality > 5) {
            throw new IllegalArgumentException("SM-2 quality must be within 0..5, was " + quality);
        }
        // EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)), never below 1.3
        double factor = prevFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
        if (factor < MIN_FACTOR) {
            factor = MIN_FACTOR;
        }

        if (quality < 3) {
            // Failed recall: restart the repetition chain, review tomorrow.
            return new SrsResult(factor, 1, 0);
        }
        int repetition = prevRepetition + 1;
        int interval;
        if (repetition == 1) {
            interval = 1;
        } else if (repetition == 2) {
            interval = 6;
        } else {
            interval = (int) Math.round(prevInterval * factor);
        }
        return new SrsResult(factor, interval, repetition);
    }
}
