package com.doradodo.app.ui.vocab.stage;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.doradodo.app.R;
import com.doradodo.app.ui.ds.Ui;
import com.doradodo.app.ui.vocab.VocabViewModel;

/** The flashcard: the word on the front, a tap reveals its meaning and examples. */
public final class FlashcardStage implements StageRenderer {

    private final StageHost host;

    public FlashcardStage(StageHost host) {
        this.host = host;
    }

    @Override
    public void render(Context c, LinearLayout body, VocabViewModel.Word w) {
        LinearLayout card = Ui.column(c);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setBackground(Ui.shape(c, Ui.CARD, 24, Ui.LINE, 1));
        card.setPadding(Ui.dp(c, 24), Ui.dp(c, 32), Ui.dp(c, 24), Ui.dp(c, 32));
        card.setMinimumHeight(Ui.dp(c, 420));
        card.setClickable(true);
        String pos = w.partOfSpeech == null ? c.getString(R.string.practice_word_default) : StageViews.capitalize(w.partOfSpeech);
        TextView badge = Ui.pill(c, pos, Ui.GREEN_TINT, Ui.GREEN_INK, 12);
        card.addView(badge, Ui.wrap());
        TextView term = Ui.text(c, w.term, 36, 700, Ui.INK);
        term.setGravity(Gravity.CENTER);
        Ui.add(card, term, 36);
        if (w.phonetic != null) {
            TextView ph = Ui.text(c, w.phonetic, 16, 400, Ui.MUTED);
            ph.setGravity(Gravity.CENTER);
            Ui.add(card, ph, 8);
        }
        ImageView sound = Ui.icon(c, R.drawable.ic_ds_volume, 22, Ui.GREEN);
        sound.setPadding(Ui.dp(c, 11), Ui.dp(c, 11), Ui.dp(c, 11), Ui.dp(c, 11));
        sound.setBackground(Ui.shape(c, Ui.GREEN_TINT, 999));
        sound.setContentDescription(c.getString(R.string.practice_play_pron));
        sound.setOnClickListener(v -> host.speak(w.term));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(Ui.dp(c, 44), Ui.dp(c, 44));
        sp.topMargin = Ui.dp(c, 14);
        card.addView(sound, sp);

        LinearLayout back = Ui.column(c);
        back.setGravity(Gravity.CENTER_HORIZONTAL);
        back.setVisibility(View.GONE);
        View rule = new View(c);
        rule.setBackgroundColor(Ui.LINE);
        back.addView(rule, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        TextView meaning = Ui.text(c, w.meaning, 24, 700, Ui.GREEN_INK);
        meaning.setGravity(Gravity.CENTER);
        Ui.add(back, meaning, 20);
        if (w.translation != null) {
            TextView tr = Ui.text(c, w.translation, 18, 600, Ui.INK);
            tr.setGravity(Gravity.CENTER);
            Ui.add(back, tr, 8);
        }
        if (w.example != null) {
            TextView ex = Ui.text(c, "“" + w.example + "”", 15, 400, Ui.MUTED);
            ex.setGravity(Gravity.CENTER);
            Ui.add(back, ex, 12);
            if (w.exampleTranslation != null) {
                TextView ext = Ui.text(c, w.exampleTranslation, 14, 400, Ui.MUTED);
                ext.setGravity(Gravity.CENTER);
                Ui.add(back, ext, 6);
            }
        }
        Ui.add(card, back, 24);
        TextView hint = Ui.text(c, c.getString(R.string.practice_tap_card), 13, 400, Ui.MUTED);
        hint.setGravity(Gravity.CENTER);
        card.setOnClickListener(v -> {
            boolean show = back.getVisibility() != View.VISIBLE;
            back.setVisibility(show ? View.VISIBLE : View.GONE);
            hint.setVisibility(show ? View.GONE : View.VISIBLE);
        });
        body.addView(card, Ui.matchWidth());
        Ui.add(body, hint, 14);
        host.speak(w.term, true);
    }
}
