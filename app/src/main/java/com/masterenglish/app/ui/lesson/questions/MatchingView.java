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
import com.masterenglish.app.ui.lesson.QuestionModels.MatchPair;
import com.masterenglish.app.ui.lesson.QuestionView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Pair each term on the left with its meaning on the right. Tap left, then right; tap a pair to undo. */
public final class MatchingView implements QuestionView {

    private final View root;
    private final List<MatchPair> pairs;
    private final List<String> rightOrder = new ArrayList<>();
    private final Map<String, MaterialButton> leftButtons = new HashMap<>();
    private final Map<String, MaterialButton> rightButtons = new HashMap<>();
    private final Map<String, String> answer = new HashMap<>();
    private final Runnable onChanged;
    private String selectedLeft;
    private boolean locked;

    public MatchingView(LayoutInflater inflater, ViewGroup parent, LessonQuestionEntity question,
                        Runnable onChanged) {
        this.onChanged = onChanged;
        this.pairs = QuestionModels.matchPairs(question);
        root = inflater.inflate(R.layout.question_matching, parent, false);
        ((TextView) root.findViewById(R.id.prompt)).setText(question.prompt);

        LinearLayout left = root.findViewById(R.id.leftColumn);
        LinearLayout right = root.findViewById(R.id.rightColumn);
        for (MatchPair pair : pairs) {
            rightOrder.add(pair.right);
        }
        Collections.shuffle(rightOrder, new Random(question.id.hashCode()));

        for (MatchPair pair : pairs) {
            MaterialButton button = newButton(inflater, left);
            button.setOnClickListener(v -> tapLeft(pair.left));
            leftButtons.put(pair.left, button);
            left.addView(button);
        }
        for (String meaning : rightOrder) {
            MaterialButton button = newButton(inflater, right);
            button.setOnClickListener(v -> tapRight(meaning));
            rightButtons.put(meaning, button);
            right.addView(button);
        }
        refresh();
    }

    private static MaterialButton newButton(LayoutInflater inflater, ViewGroup parent) {
        MaterialButton button = (MaterialButton) inflater.inflate(R.layout.item_choice, parent, false);
        button.setTextSize(15f);
        button.setPadding(20, 20, 20, 20);
        return button;
    }

    private void tapLeft(String term) {
        if (locked) {
            return;
        }
        if (answer.containsKey(term)) {
            answer.remove(term);
            selectedLeft = null;
        } else {
            selectedLeft = term;
        }
        refresh();
    }

    private void tapRight(String meaning) {
        if (locked || selectedLeft == null) {
            return;
        }
        answer.values().removeIf(meaning::equals);
        answer.put(selectedLeft, meaning);
        selectedLeft = null;
        refresh();
    }

    /** Re-applies text and colours from the current state. */
    private void refresh() {
        for (MatchPair pair : pairs) {
            MaterialButton button = leftButtons.get(pair.left);
            boolean paired = answer.containsKey(pair.left);
            button.setText(paired ? pair.left + "  ✓" : pair.left);
            boolean selected = pair.left.equals(selectedLeft);
            paint(button, selected, paired);
        }
        for (String meaning : rightOrder) {
            MaterialButton button = rightButtons.get(meaning);
            boolean used = answer.containsValue(meaning);
            button.setText(used ? meaning + "  ✓" : meaning);
            paint(button, false, used);
        }
        onChanged.run();
    }

    private static void paint(MaterialButton button, boolean selected, boolean paired) {
        int fill = selected ? Color.parseColor("#DDF4FF") : paired ? Color.parseColor("#D7FFB8") : Color.WHITE;
        int stroke = selected ? Color.parseColor("#1CB0F6") : paired ? Color.parseColor("#58CC02")
                : Color.parseColor("#E5E5E5");
        button.setBackgroundTintList(ColorStateList.valueOf(fill));
        button.setStrokeColor(ColorStateList.valueOf(stroke));
    }

    @Override
    public View getView() {
        return root;
    }

    @Override
    public boolean hasAnswer() {
        return answer.size() == pairs.size();
    }

    @Override
    public Object getAnswer() {
        return new HashMap<>(answer);
    }

    @Override
    public void lock() {
        locked = true;
        for (MaterialButton button : leftButtons.values()) {
            button.setEnabled(false);
        }
        for (MaterialButton button : rightButtons.values()) {
            button.setEnabled(false);
        }
    }

    @Override
    public void reveal(Object correctAnswer) {
        // Show the correct pairing on the left buttons, which are already disabled.
        for (MatchPair pair : pairs) {
            MaterialButton button = leftButtons.get(pair.left);
            button.setText(pair.left + "  →  " + pair.right);
            button.setBackgroundTintList(ColorStateList.valueOf(answer.containsKey(pair.left)
                    && pair.right.equals(answer.get(pair.left)) ? Color.parseColor("#D7FFB8") : Color.parseColor("#FFDFE0")));
        }
    }
}
