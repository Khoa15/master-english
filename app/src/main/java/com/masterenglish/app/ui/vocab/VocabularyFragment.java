package com.masterenglish.app.ui.vocab;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.masterenglish.app.R;
import com.masterenglish.app.ui.common.PullToRefresh;
import com.masterenglish.app.ui.ds.DsFragment;
import com.masterenglish.app.ui.ds.Ui;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/** Vocabulary home: review card, hot packs and the first word packs. */
@AndroidEntryPoint
public class VocabularyFragment extends DsFragment {

    private Ui.Screen screen;
    private TextView streakText;
    private SwipeRefreshLayout refresh;
    private VocabViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        screen = new Ui.Screen(c, 110);

        LinearLayout header = Ui.row(c);
        TextView title = Ui.text(c, "Vocabulary", 26, 700, Ui.INK);
        header.addView(title, Ui.weight(1f));
        LinearLayout streak = Ui.row(c);
        streak.setBackground(Ui.shape(c, Ui.AMBER_TINT, 999));
        streak.setPadding(Ui.dp(c, 11), Ui.dp(c, 7), Ui.dp(c, 11), Ui.dp(c, 7));
        streak.addView(Ui.icon(c, R.drawable.ic_ds_fire, 16, Ui.AMBER));
        streakText = Ui.text(c, "", 13, 700, Ui.AMBER_INK);
        LinearLayout.LayoutParams sp = Ui.wrap();
        sp.leftMargin = Ui.dp(c, 6);
        streak.addView(streakText, sp);
        header.addView(streak, Ui.wrap());
        screen.top.addView(header, Ui.matchWidth());
        screen.top.setPadding(Ui.dp(c, 20), Ui.dp(c, 24), Ui.dp(c, 20), Ui.dp(c, 4));

        refresh = PullToRefresh.wrap(c, screen.root, screen.scroll, this::reload);
        return refresh;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(VocabViewModel.class);
        viewModel.user().observe(getViewLifecycleOwner(), user -> {
            int n = user == null ? 0 : user.streakCount;
            streakText.setText(n + " day streak");
        });
        viewModel.library().observe(getViewLifecycleOwner(), library -> {
            bind(library);
            PullToRefresh.finish(refresh, true, null);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.load();
    }

    private void reload() {
        viewModel.load();
    }

    private void bind(VocabViewModel.Library library) {
        Context c = requireContext();
        LinearLayout body = screen.body;
        body.removeAllViews();

        // Review card
        LinearLayout review = Ui.column(c);
        review.setBackground(Ui.shape(c, Ui.GREEN, 20));
        review.setPadding(Ui.dp(c, 18), Ui.dp(c, 18), Ui.dp(c, 18), Ui.dp(c, 18));
        review.setElevation(Ui.dp(c, 4));
        LinearLayout label = Ui.row(c);
        label.setGravity(Gravity.BOTTOM);
        TextView small = Ui.text(c, "REVIEW: ", 12, 600, 0xD9FFFFFF);
        small.setLetterSpacing(0.05f);
        label.addView(small, Ui.wrap());
        label.addView(Ui.text(c, library.dueCount + (library.dueCount == 1 ? " word" : " words"), 20, 700, Color.WHITE), Ui.wrap());
        review.addView(label, Ui.wrap());
        TextView reviewNow = Ui.text(c, library.dueCount == 0 ? "Nothing due. Learn new words" : "Review now", 14, 700, Ui.GREEN_INK);
        reviewNow.setGravity(Gravity.CENTER);
        reviewNow.setBackground(Ui.shape(c, Color.WHITE, 12));
        reviewNow.setMinHeight(Ui.dp(c, 44));
        reviewNow.setClickable(true);
        reviewNow.setOnClickListener(v -> {
            if (library.dueCount > 0) {
                go(R.id.action_vocab_to_practice, packArgs(null));
            } else if (!library.packs.isEmpty()) {
                go(R.id.action_vocab_to_pack, packArgs(library.packs.get(0).id));
            }
        });
        Ui.add(review, reviewNow, 14);
        body.addView(review, Ui.matchWidth());

        if (library.packs.isEmpty()) {
            return;
        }

        // Hot packs: the packs with the most words still to learn.
        List<VocabViewModel.Pack> hot = new ArrayList<>(library.packs);
        hot.sort((a, b) -> (b.total() - b.learned) - (a.total() - a.learned));
        LinearLayout hotHeader = Ui.row(c);
        hotHeader.addView(Ui.icon(c, R.drawable.ic_ds_fire, 18, Ui.ORANGE));
        LinearLayout.LayoutParams tp = Ui.weight(1f);
        tp.leftMargin = Ui.dp(c, 6);
        hotHeader.addView(Ui.text(c, "Hot packs", 18, 700, Ui.INK), tp);
        hotHeader.addView(seeAll(c));
        Ui.add(body, hotHeader, 22);

        LinearLayout hotRow = Ui.row(c);
        hotRow.setBaselineAligned(false);
        hotRow.setWeightSum(2f);
        for (int i = 0; i < Math.min(2, hot.size()); i++) {
            VocabViewModel.Pack pack = hot.get(i);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            if (i == 1) {
                p.leftMargin = Ui.dp(c, 8);
            }
            hotRow.addView(hotCard(c, pack, i), p);
        }
        Ui.add(body, hotRow, 10);

        // Word packs
        LinearLayout packsHeader = Ui.row(c);
        packsHeader.addView(Ui.text(c, "Word packs", 18, 700, Ui.INK), Ui.weight(1f));
        packsHeader.addView(seeAll(c));
        Ui.add(body, packsHeader, 22);
        for (int i = 0; i < Math.min(3, library.packs.size()); i++) {
            Ui.add(body, packCard(c, library.packs.get(i)), 12);
        }
    }

    private TextView seeAll(Context c) {
        TextView t = Ui.text(c, "See all", 13, 600, Ui.GREEN);
        t.setMinHeight(Ui.dp(c, 44));
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setOnClickListener(v -> go(R.id.action_vocab_to_all_packs, null));
        return t;
    }

    private View hotCard(Context c, VocabViewModel.Pack pack, int position) {
        int[] tone = Ui.TONES[position == 0 ? 2 : 1];
        int tint = tone[1];
        int ink = tone[2];
        LinearLayout card = Ui.column(c);
        card.setBackground(Ui.shape(c, tint, 16));
        card.setPadding(Ui.dp(c, 10), Ui.dp(c, 10), Ui.dp(c, 10), Ui.dp(c, 10));
        card.setClickable(true);
        card.setOnClickListener(v -> go(R.id.action_vocab_to_pack, packArgs(pack.id)));
        TextView name = Ui.text(c, pack.title, 14, 700, ink);
        name.setMaxLines(3);
        card.addView(name, Ui.matchWidth());
        TextView sub = Ui.text(c, (pack.total() - pack.learned) + " to go", 11, 400, ink);
        Ui.add(card, sub, 3);
        View grow = new View(c);
        card.addView(grow, new LinearLayout.LayoutParams(1, 0, 1f));
        LinearLayout foot = Ui.row(c);
        foot.addView(Ui.text(c, pack.total() + (pack.total() == 1 ? " word" : " words"), 10, 600, ink), Ui.weight(1f));
        TextView go = Ui.text(c, "Practice now", 11, 700, ink);
        go.setBackground(Ui.shape(c, Color.WHITE, 999));
        go.setPadding(Ui.dp(c, 8), Ui.dp(c, 5), Ui.dp(c, 8), Ui.dp(c, 5));
        go.setSingleLine(true);
        go.setOnClickListener(v -> go(R.id.action_vocab_to_practice, packArgs(pack.id)));
        foot.addView(go, Ui.wrap());
        Ui.add(card, foot, 10);
        return card;
    }

    private View packCard(Context c, VocabViewModel.Pack pack) {
        int[] tone = Ui.TONES[pack.tone];
        LinearLayout card = Ui.card(c, 16, 18);
        card.setClickable(true);
        card.setOnClickListener(v -> go(R.id.action_vocab_to_pack, packArgs(pack.id)));
        LinearLayout top = Ui.row(c);
        top.addView(Ui.text(c, pack.title, 16, 700, Ui.INK), Ui.weight(1f));
        top.addView(Ui.text(c, pack.learned + " / " + pack.total() + " learned", 12, 400, Ui.MUTED), Ui.wrap());
        card.addView(top, Ui.matchWidth());
        Ui.add(card, Ui.progress(c, pack.percent(), tone[0]), 10);
        LinearLayout chips = Ui.row(c);
        for (int i = 0; i < Math.min(2, pack.words.size()); i++) {
            LinearLayout.LayoutParams p = Ui.wrap();
            p.rightMargin = Ui.dp(c, 6);
            chips.addView(Ui.pill(c, pack.words.get(i).term, tone[1], tone[2], 12), p);
        }
        if (pack.total() > 2) {
            chips.addView(Ui.pill(c, "+" + (pack.total() - 2) + " more", Ui.CHIP, Ui.MUTED, 12), Ui.wrap());
        }
        Ui.add(card, chips, 12);
        return card;
    }
}
