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
    private String packId;

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
                pack.category + " · " + pack.total() + (pack.total() == 1 ? " word" : " words")), Ui.matchWidth());
        LinearLayout stat = Ui.row(c);
        stat.addView(Ui.text(c, pack.learned + " learned", 13, 600, Ui.INK), Ui.weight(1f));
        stat.addView(Ui.text(c, pack.percent() + "%", 13, 700, tone[2]), Ui.wrap());
        Ui.add(screen.top, stat, 16);
        Ui.add(screen.top, Ui.progress(c, pack.percent(), tone[0]), 8);

        LinearLayout body = screen.body;
        body.removeAllViews();
        body.addView(Ui.text(c, "Words in this pack", 16, 700, Ui.INK), Ui.matchWidth());
        for (VocabViewModel.Word w : pack.words) {
            LinearLayout row = Ui.row(c);
            row.setPadding(0, Ui.dp(c, 12), 0, Ui.dp(c, 12));
            LinearLayout names = Ui.column(c);
            names.addView(Ui.text(c, w.term, 15, 600, Ui.INK));
            Ui.add(names, Ui.text(c, w.subtitle(), 12, 400, Ui.MUTED), 2);
            row.addView(names, Ui.weight(1f));
            String status = w.learned() ? "Learned" : "New";
            int[] colors = Ui.statusColors(status);
            row.addView(Ui.pill(c, status, colors[0], colors[1], 11), Ui.wrap());
            Ui.add(body, row, 0);
            View line = new View(c);
            line.setBackgroundColor(Ui.LINE);
            body.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }

        TextView practice = Ui.primaryButton(c, "Practice now");
        practice.setOnClickListener(v -> go(R.id.action_pack_to_practice, packArgs(pack.id)));
        screen.setFooter(practice);
        screen.footer.setBackgroundColor(Ui.PAPER);
        screen.top.setGravity(Gravity.TOP);
    }
}
