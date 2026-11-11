package com.masterenglish.app.ui.lesson;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.masterenglish.app.data.local.entity.LessonQuestionEntity;
import com.masterenglish.app.ui.lesson.questions.FillBlankView;
import com.masterenglish.app.ui.lesson.questions.MatchingView;
import com.masterenglish.app.ui.lesson.questions.MultipleChoiceView;
import com.masterenglish.app.ui.lesson.questions.ReorderView;
import com.masterenglish.app.ui.lesson.questions.VoiceView;

/** Builds the right widget for a question's type. */
public final class QuestionViews {

    private QuestionViews() {
    }

    /**
     * @param onChanged     called whenever the learner's input changes
     * @param onMicNeeded   called when voice input is requested without the RECORD_AUDIO permission
     */
    public static QuestionView create(LayoutInflater inflater, ViewGroup parent, LessonQuestionEntity question,
                                      Runnable onChanged, Runnable onMicNeeded) {
        switch (question.questionType) {
            case FILL_BLANK:
                return new FillBlankView(inflater, parent, question, onChanged);
            case MATCHING:
                return new MatchingView(inflater, parent, question, onChanged);
            case REORDER:
                return new ReorderView(inflater, parent, question, onChanged);
            case VOICE_INPUT:
                return new VoiceView(inflater, parent, question, onChanged, onMicNeeded);
            case MULTIPLE_CHOICE:
            default:
                return new MultipleChoiceView(inflater, parent, question, onChanged);
        }
    }
}
