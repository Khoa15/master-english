package com.doradodo.app.ui.vocab.stage;

import android.content.Context;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.doradodo.app.R;
import com.doradodo.app.ui.ds.Ui;
import com.doradodo.app.ui.vocab.PracticeViewModel;
import com.doradodo.app.ui.vocab.VocabViewModel;

/** Fill in the blank: type the missing word. */
public final class FillBlankStage implements StageRenderer {

    private final StageHost host;

    public FillBlankStage(StageHost host) {
        this.host = host;
    }

    @Override
    public void render(Context c, LinearLayout body, VocabViewModel.Word w) {
        StageViews.heading(c, body, c.getString(R.string.practice_fill_blank), c.getString(R.string.practice_type_missing));
        String[] parts = PracticeViewModel.splitExample(w);
        TextView sentence = Ui.text(c, "", 18, 400, Ui.INK);
        sentence.setLineSpacing(Ui.dp(c, 8), 1f);
        if (parts != null) {
            sentence.setText(parts[0] + "_______" + parts[2]);
        } else {
            sentence.setText("_______  =  " + w.meaning);
        }
        sentence.setBackground(Ui.shape(c, Ui.CARD, 18, Ui.LINE, 1));
        sentence.setPadding(Ui.dp(c, 20), Ui.dp(c, 20), Ui.dp(c, 20), Ui.dp(c, 20));
        Ui.add(body, sentence, 24);
        StageViews.label(c, body, c.getString(R.string.practice_your_answer));
        EditText input = StageViews.editor(c, c.getString(R.string.practice_type_missing));
        host.bindInput(input);
        Ui.add(body, input, 8);
        StageViews.feedbackView(host, c, body);
    }
}
