package com.doradodo.app.ui.vocab.stage;

import android.content.Context;
import android.widget.LinearLayout;

import com.doradodo.app.R;
import com.doradodo.app.ui.ds.Ui;
import com.doradodo.app.ui.vocab.PracticeViewModel.Stage;
import com.doradodo.app.ui.vocab.VocabViewModel;

/** Choice: pick the right meaning of the word. */
public final class ChoiceStage implements StageRenderer {

    private final StageHost host;

    public ChoiceStage(StageHost host) {
        this.host = host;
    }

    @Override
    public void render(Context c, LinearLayout body, VocabViewModel.Word w) {
        StageViews.heading(c, body, c.getString(R.string.practice_what_does_mean, w.term), c.getString(R.string.practice_choose_one));
        LinearLayout optionBox = Ui.column(c);
        host.bindOptionBox(optionBox);
        Ui.add(body, optionBox, 20);
        StageViews.options(host, c, optionBox, host.viewModel().meanings(w, 4), w.meaning, w, Stage.CHOICE);
        StageViews.feedbackView(host, c, body);
    }
}
