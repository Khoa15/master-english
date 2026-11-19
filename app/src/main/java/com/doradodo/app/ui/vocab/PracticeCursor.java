package com.doradodo.app.ui.vocab;

/** Where the learner is in a practice round: stage by stage, every word of a stage before the next stage. */
public final class PracticeCursor {

    private final int stageCount;
    private int stage;
    private int index;

    public PracticeCursor(int stageCount) {
        this.stageCount = stageCount;
    }

    public int stage() {
        return stage;
    }

    public int index() {
        return index;
    }

    public boolean atStart() {
        return stage == 0 && index == 0;
    }

    public boolean isLast(int wordCount) {
        return stage == stageCount - 1 && index == wordCount - 1;
    }

    /** Moves to the next word, or to the first word of the next stage; false (and no move) after the last one. */
    public boolean advance(int wordCount) {
        if (index + 1 < wordCount) {
            index++;
        } else if (stage + 1 < stageCount) {
            stage++;
            index = 0;
        } else {
            return false;
        }
        return true;
    }

    /** Moves to the previous word, or to the last word of the previous stage; false (and no move) at the start. */
    public boolean stepBack(int wordCount) {
        if (atStart()) {
            return false;
        }
        if (index > 0) {
            index--;
        } else {
            stage--;
            index = wordCount - 1;
        }
        return true;
    }

    /** How much of the round is behind the learner, counting the current step, as a percentage. */
    public int progressPercent(int wordCount) {
        return (stage * wordCount + index + 1) * 100 / (stageCount * wordCount);
    }
}
