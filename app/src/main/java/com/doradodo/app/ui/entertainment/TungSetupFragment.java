package com.doradodo.app.ui.entertainment;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.doradodo.app.R;
import com.doradodo.app.data.repository.EntertainmentRepository.WordSource;
import java.util.ArrayList;
import java.util.List;
import com.doradodo.app.ui.ds.Ui;

/** "Tụng siêu cấp" setup: word pack, number of words and how many times each word is chanted. */
@dagger.hilt.android.AndroidEntryPoint
public class TungSetupFragment extends GameFragment {

    static final String PREFS = "tung_cfg";

    /** "5", "10", then the string resources of the last choices; a number is shown as it is. */
    private String[] wordLabels() {
        return new String[]{"5", "10", getString(R.string.ent_custom), getString(R.string.ent_all)};
    }

    private String[] repLabels() {
        return new String[]{"5", "10", getString(R.string.ent_custom)};
    }

    private LinearLayout body;
    private List<WordSource> sources = new ArrayList<>();
    private int pack;
    private boolean loaded;
    private int wordMode = 0;   // 0 = 5, 1 = 10, 2 = custom, 3 = all
    private int wordCustom = 6;
    private int repMode = 1;    // 0 = 5, 1 = 10, 2 = custom
    private int repCustom = 20;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        GameUi.Page page = GameUi.page(c, false, onBack(), c.getString(R.string.ent_tung_title), c.getString(R.string.ent_chant_setup), 0, 0, 0);
        body = page.screen.body;
        TextView start = GameUi.footerButton(c, getString(R.string.ent_start_chant), android.graphics.Color.WHITE, Ui.BLUE, GameUi.BLUE_EDGE);
        start.setOnClickListener(v -> {
            if (sources.isEmpty()) {
                return;
            }
            int wordsRaw = wordMode == 3 ? 0 : (wordMode == 0 ? 5 : (wordMode == 1 ? 10 : clamp(wordCustom)));
            int reps = repMode == 0 ? 5 : (repMode == 1 ? 10 : clamp(repCustom));
            requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString("source", sources.get(pack).id).putInt("words", wordsRaw).putInt("reps", reps).apply();
            go(R.id.tungFragment, null);
        });
        GameUi.setFooter(c, page.screen, start);
        render();
        load(() -> vm().repo().wordSources(), list -> {
            sources = list == null ? new ArrayList<>() : list;
            loaded = true;
            render();
        });
        return page.screen.root;
    }

    private static int clamp(int v) {
        return Math.min(100, Math.max(1, v));
    }

    private void render() {
        Context c = requireContext();
        body.removeAllViews();

        Ui.add(body, Ui.text(c, getString(R.string.ent_word_pack), 15, 700, Ui.INK), 8);
        if (!loaded) {
            Ui.add(body, loadingState(c), 8);
        } else if (sources.isEmpty()) {
            Ui.add(body, emptyState(c, getString(R.string.ent_no_wordset), getString(R.string.ent_no_wordset_chant)), 8);
        }
        for (int i = 0; i < sources.size(); i++) {
            final int index = i;
            WordSource p = sources.get(i);
            LinearLayout row = Ui.row(c);
            boolean on = i == pack;
            row.setBackground(Ui.shape(c, on ? GameUi.PURPLE_TINT : Ui.CARD, 16, on ? GameUi.PURPLE : Ui.LINE, 2));
            row.setPadding(Ui.dp(c, 14), Ui.dp(c, 12), Ui.dp(c, 14), Ui.dp(c, 12));
            row.setClickable(true);
            row.addView(Ui.text(c, GameUi.sourceTitle(c, p), 15, 700, Ui.INK), Ui.weight(1f));
            row.addView(Ui.text(c, getString(R.string.ent_words_n, p.count), 12, 600, Ui.MUTED), Ui.wrap());
            row.setOnClickListener(v -> {
                pack = index;
                render();
            });
            Ui.add(body, row, 8);
        }

        Ui.add(body, Ui.text(c, getString(R.string.ent_number_of_words), 15, 700, Ui.INK), 22);
        chips(c, wordLabels(), wordMode, i -> {
            wordMode = i;
            render();
        });
        if (wordMode == 2) {
            stepper(c, wordCustom, d -> {
                wordCustom = clamp(wordCustom + d);
                render();
            }, R.string.ent_decrease_words, R.string.ent_increase_words);
        }

        LinearLayout reps = Ui.row(c);
        reps.addView(Ui.text(c, getString(R.string.ent_chant_frequency) + " ", 15, 700, Ui.INK), Ui.wrap());
        reps.addView(Ui.text(c, getString(R.string.ent_per_word), 12, 600, Ui.MUTED), Ui.wrap());
        Ui.add(body, reps, 22);
        chips(c, repLabels(), repMode, i -> {
            repMode = i;
            render();
        });
        if (repMode == 2) {
            stepper(c, repCustom, d -> {
                repCustom = clamp(repCustom + d);
                render();
            }, R.string.ent_decrease_freq, R.string.ent_increase_freq);
        }
    }

    private void chips(Context c, String[] labels, int selected, java.util.function.IntConsumer onPick) {
        LinearLayout row = Ui.row(c);
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            boolean on = i == selected;
            TextView t = Ui.text(c, labels[i], 14, 700, on ? android.graphics.Color.WHITE : Ui.INK);
            t.setGravity(Gravity.CENTER);
            t.setBackground(Ui.shape(c, on ? Ui.BLUE : Ui.CARD, 14, on ? Ui.BLUE : Ui.LINE, 2));
            t.setClickable(true);
            t.setOnClickListener(v -> onPick.accept(index));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, Ui.dp(c, 44), 1f);
            if (i > 0) {
                p.leftMargin = Ui.dp(c, 8);
            }
            row.addView(t, p);
        }
        Ui.add(body, row, 10);
    }

    private void stepper(Context c, int value, java.util.function.IntConsumer onStep, int decreaseRes, int increaseRes) {
        LinearLayout row = Ui.row(c);
        row.setGravity(Gravity.CENTER);
        row.addView(stepButton(c, "−", getString(decreaseRes), () -> onStep.accept(-1)));
        TextView v = Ui.text(c, String.valueOf(value), 22, 700, Ui.INK);
        v.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(Ui.dp(c, 90), ViewGroup.LayoutParams.WRAP_CONTENT);
        vp.leftMargin = Ui.dp(c, 16);
        vp.rightMargin = Ui.dp(c, 16);
        row.addView(v, vp);
        row.addView(stepButton(c, "+", getString(increaseRes), () -> onStep.accept(1)));
        Ui.add(body, row, 10);
    }

    private View stepButton(Context c, String label, String description, Runnable action) {
        TextView t = Ui.text(c, label, 20, 700, Ui.INK);
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.shape(c, Ui.CARD, 14, Ui.LINE, 2));
        t.setClickable(true);
        t.setContentDescription(description);
        t.setOnClickListener(v -> action.run());
        t.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(c, 44), Ui.dp(c, 44)));
        return t;
    }
}
