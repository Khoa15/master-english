package com.masterenglish.app.ui.lesson.questions;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.masterenglish.app.R;
import com.masterenglish.app.data.local.entity.LessonQuestionEntity;
import com.masterenglish.app.ui.lesson.QuestionModels;
import com.masterenglish.app.ui.lesson.QuestionView;

import java.util.ArrayList;
import java.util.List;

/** Single-choice question: tap one option, the selection is highlighted in brand green. */
public final class MultipleChoiceView implements QuestionView {

    private final View root;
    private final List<MaterialButton> buttons = new ArrayList<>();
    private final List<String> options;
    private final Runnable onChanged;
    private String selected;
    private boolean locked;

    public MultipleChoiceView(LayoutInflater inflater, ViewGroup parent, LessonQuestionEntity question,
                              Runnable onChanged) {
        this.onChanged = onChanged;
        this.options = QuestionModels.stringOptions(question);
        root = inflater.inflate(R.layout.question_multiple_choice, parent, false);
        ((TextView) root.findViewById(R.id.prompt)).setText(question.prompt);

        LinearLayout container = root.findViewById(R.id.optionsContainer);
        for (String option : options) {
            MaterialButton button = (MaterialButton) inflater.inflate(R.layout.item_choice, container, false);
            button.setText(option);
            button.setOnClickListener(v -> select(option));
            buttons.add(button);
            container.addView(button);
        }
    }

    private void select(String option) {
        if (locked) {
            return;
        }
        selected = option;
        for (int i = 0; i < buttons.size(); i++) {
            paint(buttons.get(i), options.get(i).equals(selected), false, false);
        }
        onChanged.run();
    }

    private static void paint(MaterialButton button, boolean selected, boolean correct, boolean wrong) {
        int fill = correct ? Color.parseColor("#D7FFB8") : wrong ? Color.parseColor("#FFDFE0")
                : selected ? Color.parseColor("#DDF4FF") : Color.WHITE;
        int stroke = correct ? Color.parseColor("#58CC02") : wrong ? Color.parseColor("#FF4B4B")
                : selected ? Color.parseColor("#1CB0F6") : Color.parseColor("#E5E5E5");
        button.setBackgroundTintList(ColorStateList.valueOf(fill));
        button.setStrokeColor(ColorStateList.valueOf(stroke));
    }

    @Override
    public View getView() {
        return root;
    }

    @Override
    public boolean hasAnswer() {
        return selected != null;
    }

    @Override
    public Object getAnswer() {
        return selected == null ? "" : selected;
    }

    @Override
    public void lock() {
        locked = true;
        for (MaterialButton button : buttons) {
            button.setEnabled(false);
        }
    }

    @Override
    public void reveal(Object correctAnswer) {
        String correct = String.valueOf(correctAnswer);
        for (int i = 0; i < buttons.size(); i++) {
            String option = options.get(i);
            boolean isCorrect = option.equals(correct);
            boolean isWrongPick = option.equals(selected) && !isCorrect;
            paint(buttons.get(i), false, isCorrect, isWrongPick);
        }
    }
}
