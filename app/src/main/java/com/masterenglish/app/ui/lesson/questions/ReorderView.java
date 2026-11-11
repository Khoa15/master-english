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
import com.masterenglish.app.utils.TextUtils;

import java.util.ArrayList;
import java.util.List;

/** Sentence building: tap a word in the bank to append it; tap an answer word to return it. */
public final class ReorderView implements QuestionView {

    private final View root;
    private final ChipGroup answerArea;
    private final ChipGroup bankArea;
    private final List<String> bank = new ArrayList<>();
    private final List<String> answer = new ArrayList<>();
    private final Runnable onChanged;
    private boolean locked;

    public ReorderView(LayoutInflater inflater, ViewGroup parent, LessonQuestionEntity question,
                       Runnable onChanged) {
        this.onChanged = onChanged;
        root = inflater.inflate(R.layout.question_reorder, parent, false);
        ((TextView) root.findViewById(R.id.prompt)).setText(question.prompt);
        answerArea = root.findViewById(R.id.answerArea);
        bankArea = root.findViewById(R.id.bankArea);
        bank.addAll(QuestionModels.stringOptions(question));
        render();
    }

    private void moveToAnswer(int bankIndex) {
        if (locked) {
            return;
        }
        answer.add(bank.remove(bankIndex));
        render();
    }

    private void moveToBank(int answerIndex) {
        if (locked) {
            return;
        }
        bank.add(answer.remove(answerIndex));
        render();
    }

    private void render() {
        answerArea.removeAllViews();
        bankArea.removeAllViews();
        for (int i = 0; i < answer.size(); i++) {
            int index = i;
            answerArea.addView(chip(answer.get(i), v -> moveToBank(index)));
        }
        for (int i = 0; i < bank.size(); i++) {
            int index = i;
            bankArea.addView(chip(bank.get(i), v -> moveToAnswer(index)));
        }
        onChanged.run();
    }

    private Chip chip(String text, View.OnClickListener listener) {
        Chip chip = new Chip(root.getContext());
        chip.setText(text);
        chip.setOnClickListener(listener);
        chip.setTextSize(16f);
        return chip;
    }

    @Override
    public View getView() {
        return root;
    }

    @Override
    public boolean hasAnswer() {
        return !answer.isEmpty() && bank.isEmpty();
    }

    @Override
    public Object getAnswer() {
        return TextUtils.joinWords(answer);
    }

    @Override
    public void lock() {
        locked = true;
        for (int i = 0; i < answerArea.getChildCount(); i++) {
            answerArea.getChildAt(i).setEnabled(false);
        }
        for (int i = 0; i < bankArea.getChildCount(); i++) {
            bankArea.getChildAt(i).setEnabled(false);
        }
    }
}
