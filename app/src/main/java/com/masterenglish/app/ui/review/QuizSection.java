package com.masterenglish.app.ui.review;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.masterenglish.app.R;
import com.masterenglish.app.data.local.entity.LessonQuestionEntity;
import com.masterenglish.app.data.repository.ProgressRepository;
import com.masterenglish.app.ui.lesson.QuestionModels;

import java.util.List;

/**
 * Multiple-choice questions shown under a listening or reading passage. Answering
 * checks immediately, colours the choices and feeds the Weakness Clinic.
 */
public final class QuizSection {

    private QuizSection() {
    }

    public static void render(LayoutInflater inflater, ViewGroup container, List<LessonQuestionEntity> questions,
                              ProgressRepository progress, String userId) {
        container.removeAllViews();
        for (LessonQuestionEntity question : questions) {
            LinearLayout block = new LinearLayout(container.getContext());
            block.setOrientation(LinearLayout.VERTICAL);
            block.setPadding(0, 16, 0, 8);

            TextView prompt = new TextView(container.getContext());
            prompt.setText(question.prompt);
            prompt.setTextColor(container.getResources().getColor(R.color.neutral_text, null));
            prompt.setTextSize(16f);
            prompt.setTypeface(prompt.getTypeface(), android.graphics.Typeface.BOLD);
            block.addView(prompt);

            for (String option : QuestionModels.stringOptions(question)) {
                MaterialButton choice = (MaterialButton) inflater.inflate(R.layout.item_choice, block, false);
                choice.setText(option);
                choice.setOnClickListener(v -> answer(block, question, option, progress, userId));
                block.addView(choice);
            }
            container.addView(block);
        }
    }

    private static void answer(LinearLayout block, LessonQuestionEntity question, String picked,
                               ProgressRepository progress, String userId) {
        if (block.getTag() != null) {
            return;
        }
        block.setTag(picked);
        boolean correct = QuestionModels.isCorrect(question, picked);
        progress.recordAnswer(userId, question, correct);
        String expected = String.valueOf(QuestionModels.correctAnswer(question));
        for (int i = 1; i < block.getChildCount(); i++) {
            MaterialButton choice = (MaterialButton) block.getChildAt(i);
            String text = choice.getText().toString();
            choice.setEnabled(false);
            if (text.equals(expected)) {
                paint(choice, "#D7FFB8", "#58CC02");
            } else if (text.equals(picked)) {
                paint(choice, "#FFDFE0", "#FF4B4B");
            }
        }
    }

    private static void paint(MaterialButton button, String fill, String stroke) {
        button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(fill)));
        button.setStrokeColor(ColorStateList.valueOf(Color.parseColor(stroke)));
    }
}
