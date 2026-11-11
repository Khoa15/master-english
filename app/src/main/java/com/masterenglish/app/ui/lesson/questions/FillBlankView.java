package com.masterenglish.app.ui.lesson.questions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.masterenglish.app.R;
import com.masterenglish.app.data.local.entity.LessonQuestionEntity;
import com.masterenglish.app.ui.lesson.QuestionModels;
import com.masterenglish.app.ui.lesson.QuestionView;

import java.util.List;

/** Fill the blank: pick the word from the word bank that completes the sentence. */
public final class FillBlankView implements QuestionView {

    private final View root;
    private final ChipGroup bank;
    private final Runnable onChanged;
    private String selected;
    private boolean locked;

    public FillBlankView(LayoutInflater inflater, ViewGroup parent, LessonQuestionEntity question,
                         Runnable onChanged) {
        this.onChanged = onChanged;
        root = inflater.inflate(R.layout.question_fill_blank, parent, false);
        ((TextView) root.findViewById(R.id.prompt)).setText(question.prompt);
        bank = root.findViewById(R.id.bank);

        List<String> words = QuestionModels.stringOptions(question);
        for (String word : words) {
            Chip chip = new Chip(inflater.getContext());
            chip.setText(word);
            chip.setCheckable(true);
            chip.setId(View.generateViewId());
            chip.setTag(word);
            bank.addView(chip);
        }
        bank.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (locked) {
                return;
            }
            selected = null;
            for (int id : checkedIds) {
                View chip = group.findViewById(id);
                if (chip != null) {
                    selected = String.valueOf(chip.getTag());
                }
            }
            onChanged.run();
        });
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
        for (int i = 0; i < bank.getChildCount(); i++) {
            bank.getChildAt(i).setEnabled(false);
        }
    }

    @Override
    public void reveal(Object correctAnswer) {
        String correct = String.valueOf(correctAnswer);
        for (int i = 0; i < bank.getChildCount(); i++) {
            View child = bank.getChildAt(i);
            if (correct.equals(child.getTag())) {
                child.setBackgroundColor(0x3358CC02);
            }
        }
    }
}
