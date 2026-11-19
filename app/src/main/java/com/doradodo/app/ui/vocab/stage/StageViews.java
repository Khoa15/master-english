package com.doradodo.app.ui.vocab.stage;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.doradodo.app.R;
import com.doradodo.app.ui.ds.Ui;
import com.doradodo.app.ui.vocab.PracticeViewModel.Stage;
import com.doradodo.app.ui.vocab.VocabViewModel;

import java.util.List;

/** The pieces the practice stages share: heading, label, text field, feedback box and answer rows. */
final class StageViews {

    private StageViews() {
    }

    static void heading(Context c, LinearLayout body, String title, String subtitle) {
        body.addView(Ui.text(c, title, 20, 700, Ui.INK), Ui.matchWidth());
        Ui.add(body, Ui.text(c, subtitle, 14, 400, Ui.MUTED), 6);
    }

    static void label(Context c, LinearLayout body, String text) {
        Ui.add(body, Ui.text(c, text, 13, 600, Ui.MUTED), 24);
    }

    static EditText editor(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        e.setTextSize(16);
        e.setTextColor(Ui.INK);
        e.setHintTextColor(0xFF8B948E);
        e.setPadding(Ui.dp(c, 16), 0, Ui.dp(c, 16), 0);
        e.setBackground(Ui.shape(c, Ui.CARD, 14, Ui.LINE, 1.5f));
        e.setMinHeight(Ui.dp(c, 52));
        Ui.font(e, 400);
        e.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int d) { e.setError(null); }
            @Override public void afterTextChanged(Editable s) { }
        });
        e.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(c, 52)));
        return e;
    }

    static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static TextView feedbackView(StageHost host, Context c, LinearLayout body) {
        TextView f = Ui.text(c, "", 14, 600, Ui.GREEN_INK);
        f.setPadding(Ui.dp(c, 14), Ui.dp(c, 12), Ui.dp(c, 14), Ui.dp(c, 12));
        f.setVisibility(View.GONE);
        Ui.add(body, f, 16);
        host.bindFeedback(f);
        return f;
    }

    /** Tappable answer rows; the answer is revealed as soon as one is picked. */
    static void options(StageHost host, Context c, LinearLayout optionBox, List<String> list, String correct,
                        VocabViewModel.Word w, Stage s) {
        for (int i = 0; i < list.size(); i++) {
            final int pos = i;
            LinearLayout row = Ui.row(c);
            row.setBackground(Ui.shape(c, Ui.CARD, 14, Ui.LINE, 1.5f));
            row.setPadding(Ui.dp(c, 16), Ui.dp(c, 14), Ui.dp(c, 16), Ui.dp(c, 14));
            row.setMinimumHeight(Ui.dp(c, 56));
            row.setClickable(true);
            row.setTag(list.get(i));
            row.addView(Ui.text(c, list.get(i), 16, 600, Ui.INK), Ui.weight(1f));
            row.setOnClickListener(v -> {
                if (host.isAnswered()) {
                    return;
                }
                boolean ok = list.get(pos).equals(correct);
                for (int k = 0; k < optionBox.getChildCount(); k++) {
                    View r = optionBox.getChildAt(k);
                    boolean isRight = correct.equals(r.getTag());
                    boolean isPicked = k == pos;
                    if (isRight) {
                        r.setBackground(Ui.shape(c, Ui.GREEN_TINT, 14, Ui.GREEN, 1.5f));
                    } else if (isPicked) {
                        r.setBackground(Ui.shape(c, Ui.RED_TINT, 14, Ui.RED, 1.5f));
                    }
                }
                host.markAnswered(s, w, ok, ok ? c.getString(R.string.practice_correct)
                        : c.getString(R.string.practice_wrong, correct));
            });
            Ui.add(optionBox, row, i == 0 ? 0 : 10);
        }
    }
}
