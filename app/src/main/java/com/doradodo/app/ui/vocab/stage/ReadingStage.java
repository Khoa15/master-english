package com.doradodo.app.ui.vocab.stage;

import android.content.Context;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.doradodo.app.R;
import com.doradodo.app.ui.ds.Ui;
import com.doradodo.app.ui.vocab.PracticeViewModel.Stage;
import com.doradodo.app.ui.vocab.VocabViewModel;

/** Reading: pick the meaning of the highlighted word, or the word for a definition. */
public final class ReadingStage implements StageRenderer {

    private final StageHost host;

    public ReadingStage(StageHost host) {
        this.host = host;
    }

    @Override
    public void render(Context c, LinearLayout body, VocabViewModel.Word w) {
        boolean hasExample = w.example != null;
        StageViews.heading(c, body, c.getString(R.string.practice_read_answer), hasExample ? c.getString(R.string.practice_read_short)
                : c.getString(R.string.practice_read_meaning));
        TextView passage = Ui.text(c, "", 17, 400, Ui.INK);
        passage.setLineSpacing(Ui.dp(c, 6), 1f);
        // The Oxford lists have no example sentences: the passage is then the definition and the answers are words.
        String text = hasExample ? w.example : w.meaning;
        SpannableString span = new SpannableString(text);
        int at = hasExample ? text.toLowerCase().indexOf(w.term.toLowerCase()) : -1;
        if (at >= 0) {
            span.setSpan(new ForegroundColorSpan(Ui.GREEN), at, at + w.term.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), at, at + w.term.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        passage.setText(span);
        passage.setBackground(Ui.shape(c, Ui.CARD, 18, Ui.LINE, 1));
        passage.setPadding(Ui.dp(c, 18), Ui.dp(c, 18), Ui.dp(c, 18), Ui.dp(c, 18));
        Ui.add(body, passage, 20);
        Ui.add(body, Ui.text(c, hasExample ? c.getString(R.string.practice_what_does_mean_text, w.term)
                : c.getString(R.string.practice_which_word), 16, 700, Ui.INK), 20);
        LinearLayout optionBox = Ui.column(c);
        host.bindOptionBox(optionBox);
        Ui.add(body, optionBox, 12);
        if (hasExample) {
            StageViews.options(host, c, optionBox, host.viewModel().meanings(w, 2), w.meaning, w, Stage.READING);
        } else {
            StageViews.options(host, c, optionBox, host.viewModel().terms(w, 2), w.term, w, Stage.READING);
        }
        StageViews.feedbackView(host, c, body);
    }
}
