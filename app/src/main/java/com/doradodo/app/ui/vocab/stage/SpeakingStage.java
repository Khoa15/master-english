package com.doradodo.app.ui.vocab.stage;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.doradodo.app.R;
import com.doradodo.app.ui.ds.Ui;
import com.doradodo.app.ui.vocab.VocabViewModel;

/** Speaking: say the word aloud. */
public final class SpeakingStage implements StageRenderer {

    private final StageHost host;

    public SpeakingStage(StageHost host) {
        this.host = host;
    }

    @Override
    public void render(Context c, LinearLayout body, VocabViewModel.Word w) {
        StageViews.heading(c, body, c.getString(R.string.practice_say_aloud), c.getString(R.string.practice_listen_repeat));
        TextView term = Ui.text(c, w.term, 36, 700, Ui.INK);
        term.setGravity(Gravity.CENTER);
        Ui.add(body, term, 32);
        if (w.phonetic != null) {
            TextView ph = Ui.text(c, w.phonetic, 16, 400, Ui.MUTED);
            ph.setGravity(Gravity.CENTER);
            Ui.add(body, ph, 6);
        }
        TextView listen = Ui.pill(c, c.getString(R.string.practice_listen), Ui.GREEN_TINT, Ui.GREEN_INK, 13);
        listen.setOnClickListener(v -> host.speak(w.term));
        LinearLayout.LayoutParams lp = Ui.wrap();
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.topMargin = Ui.dp(c, 14);
        body.addView(listen, lp);
        FrameLayout mic = new FrameLayout(c);
        mic.setBackground(Ui.shape(c, Ui.GREEN, 999));
        mic.setElevation(Ui.dp(c, 4));
        mic.setClickable(true);
        mic.setContentDescription(c.getString(R.string.practice_start_recording));
        mic.addView(Ui.icon(c, R.drawable.ic_ds_mic, 36, Color.WHITE),
                new FrameLayout.LayoutParams(Ui.dp(c, 36), Ui.dp(c, 36), Gravity.CENTER));
        mic.setOnClickListener(v -> host.startListening());
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(Ui.dp(c, 96), Ui.dp(c, 96));
        mp.gravity = Gravity.CENTER_HORIZONTAL;
        mp.topMargin = Ui.dp(c, 36);
        body.addView(mic, mp);
        TextView tap = Ui.text(c, c.getString(R.string.practice_tap_speak), 14, 600, Ui.MUTED);
        tap.setGravity(Gravity.CENTER);
        Ui.add(body, tap, 12);
        StageViews.feedbackView(host, c, body);
    }
}
