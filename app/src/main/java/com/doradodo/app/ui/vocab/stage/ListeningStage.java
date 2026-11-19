package com.doradodo.app.ui.vocab.stage;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.doradodo.app.R;
import com.doradodo.app.ui.ds.Ui;
import com.doradodo.app.ui.vocab.VocabViewModel;

/** Listening: hear the word and type it. */
public final class ListeningStage implements StageRenderer {

    private final StageHost host;

    public ListeningStage(StageHost host) {
        this.host = host;
    }

    @Override
    public void render(Context c, LinearLayout body, VocabViewModel.Word w) {
        StageViews.heading(c, body, c.getString(R.string.practice_listen_type), c.getString(R.string.practice_tap_play));
        ImageView icon = Ui.icon(c, R.drawable.ic_ds_play, 36, Color.WHITE);
        FrameLayout circle = new FrameLayout(c);
        circle.setBackground(Ui.shape(c, Ui.GREEN, 999));
        circle.setElevation(Ui.dp(c, 4));
        circle.setClickable(true);
        circle.setContentDescription(c.getString(R.string.practice_play_audio));
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(Ui.dp(c, 36), Ui.dp(c, 36), Gravity.CENTER);
        circle.addView(icon, ip);
        circle.setOnClickListener(v -> host.speak(w.term));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(Ui.dp(c, 96), Ui.dp(c, 96));
        cp.gravity = Gravity.CENTER_HORIZONTAL;
        cp.topMargin = Ui.dp(c, 32);
        body.addView(circle, cp);
        StageViews.label(c, body, c.getString(R.string.practice_your_answer));
        EditText input = StageViews.editor(c, c.getString(R.string.practice_type_word));
        host.bindInput(input);
        Ui.add(body, input, 8);
        Ui.add(body, Ui.text(c, c.getString(R.string.practice_hint_letters, w.term.codePointCount(0, w.term.length()),
                new String(Character.toChars(w.term.codePointAt(0)))), 12, 400, Ui.MUTED), 8);
        StageViews.feedbackView(host, c, body);
        host.speak(w.term, true);
    }
}
