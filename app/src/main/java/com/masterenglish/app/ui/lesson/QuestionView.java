package com.masterenglish.app.ui.lesson;

import android.view.View;

/** One interactive question. Each question type implements its own input widget. */
public interface QuestionView {

    View getView();

    /** True once the learner has given enough input to check the answer. */
    boolean hasAnswer();

    /** String for most types; Map of left to right for MATCHING. */
    Object getAnswer();

    /** Stops further input after the answer is checked. */
    void lock();

    /** Highlights the correct answer after a wrong attempt. */
    default void reveal(Object correctAnswer) {
    }

    /** Releases system resources such as the speech recognizer. */
    default void release() {
    }
}
