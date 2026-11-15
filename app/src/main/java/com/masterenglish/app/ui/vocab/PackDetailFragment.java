package com.masterenglish.app.ui.vocab;

import android.content.Context;
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

import com.masterenglish.app.R;
import com.masterenglish.app.ui.ds.DsFragment;
import com.masterenglish.app.ui.ds.Ui;

import dagger.hilt.android.AndroidEntryPoint;

/** One word pack: progress and its words, with a Practice now button. */
@AndroidEntryPoint
public class PackDetailFragment extends DsFragment {

    private Ui.Screen screen;
    private static final int PAGE = 100;

    private String packId;
    private int shown = PAGE;
    private LinearLayout wordsBox;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        screen = new Ui.Screen(requireContext(), 24);
        packId = getArguments() == null ? null : getArguments().getString(ARG_PACK);
        return screen.root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        VocabViewModel vm = new ViewModelProvider(this).get(VocabViewModel.class);
        vm.library().observe(getViewLifecycleOwner(), library -> {
            VocabViewModel.Pack pack = packId == null ? null : library.pack(packId);
            if (pack != null) {
                bind(pack);
            }
        });
        vm.load();
    }

    private void bind(VocabViewModel.Pack pack) {
        Context c = requireContext();
        int[] tone = Ui.TONES[pack.tone];
        screen.top.removeAllViews();
        screen.top.addView(Ui.titleBar(c, onBack(), pack.title,
                pack.total() + (pack.total() == 1 ? " word" : " words")), Ui.matchWidth());
        LinearLayout stat = Ui.row(c);
        stat.addView(Ui.text(c, pack.learned + " learned", 13, 600, Ui.INK), Ui.weight(1f));
        stat.addView(Ui.text(c, pack.percent() + "%", 13, 700, tone[2]), Ui.wrap());
        Ui.add(screen.top, stat, 16);
        Ui.add(screen.top, Ui.progress(c, pack.percent(), tone[0]), 8);

        LinearLayout body = screen.body;
        body.removeAllViews();
        body.addView(Ui.text(c, "Words in this pack", 16, 700, Ui.INK), Ui.matchWidth());
        shown = PAGE;
        wordsBox = Ui.column(c);
        body.addView(wordsBox, Ui.matchWidth());
        drawWords(c, pack);

        TextView practice = Ui.primaryButton(c, "Practice now");
        practice.setOnClickListener(v -> go(R.id.action_pack_to_practice, packArgs(pack.id)));
        screen.setFooter(practice);
        screen.footer.setBackgroundColor(Ui.PAPER);
        screen.top.setGravity(Gravity.TOP);
    }

    /** Draws the first {@code shown} words and a button for the next page: packs hold up to 1,300 words. */
    private void drawWords(Context c, VocabViewModel.Pack pack) {
        wordsBox.removeAllViews();
        int end = Math.min(shown, pack.words.size());
        for (int i = 0; i < end; i++) {
            VocabViewModel.Word w = pack.words.get(i);
            LinearLayout row = Ui.row(c);
            row.setPadding(0, Ui.dp(c, 12), 0, Ui.dp(c, 12));
            LinearLayout names = Ui.column(c);
            names.addView(Ui.text(c, w.term, 15, 600, Ui.INK));
            TextView sub = Ui.text(c, w.subtitle(), 12, 400, Ui.MUTED);
            sub.setMaxLines(2);
            sub.setEllipsize(android.text.TextUtils.TruncateAt.END);
            Ui.add(names, sub, 2);
            row.addView(names, Ui.weight(1f));
            String status = w.learned() ? "Learned" : "New";
            int[] colors = Ui.statusColors(status);
            LinearLayout.LayoutParams cp = Ui.wrap();
            cp.leftMargin = Ui.dp(c, 8);
            row.addView(Ui.pill(c, status, colors[0], colors[1], 11), cp);
            wordsBox.addView(row, Ui.matchWidth());
            View line = new View(c);
            line.setBackgroundColor(Ui.LINE);
            wordsBox.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }
        if (end < pack.words.size()) {
            TextView more = Ui.text(c, "Show more (" + (pack.words.size() - end) + " left)", 14, 700, Ui.GREEN);
            more.setGravity(Gravity.CENTER);
            more.setMinHeight(Ui.dp(c, 52));
            more.setOnClickListener(v -> {
                shown += PAGE;
                drawWords(c, pack);
            });
            wordsBox.addView(more, Ui.matchWidth());
        }
    }
}
