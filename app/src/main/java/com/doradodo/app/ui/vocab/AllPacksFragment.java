package com.doradodo.app.ui.vocab;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.doradodo.app.R;
import com.doradodo.app.ui.ds.DsFragment;
import com.doradodo.app.ui.ds.Ui;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/** All word packs: sort over a two-column grid. */
@AndroidEntryPoint
public class AllPacksFragment extends DsFragment {

    private String[] sorts() {
        return new String[]{"A1 → C1", "C1 → A1", getString(R.string.vocab_sort_most_words),
                getString(R.string.vocab_sort_progress)};
    }

    private Ui.Screen screen;
    private TextView count;
    private TextView sortView;
    private LinearLayout grid;
    private VocabViewModel.Library library;
    private int sort;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        screen = new Ui.Screen(c, 110);
        LinearLayout bar = Ui.titleBar(c, onBack(), c.getString(R.string.vocab_word_packs), "");
        count = (TextView) ((LinearLayout) bar.getChildAt(1)).getChildAt(1);
        screen.top.addView(bar, Ui.matchWidth());

        LinearLayout filters = Ui.row(c);
        sortView = dropdown(c);
        filters.addView(sortView, new LinearLayout.LayoutParams(0, Ui.dp(c, 44), 1f));
        Ui.add(screen.top, filters, 14);
        screen.top.setPadding(Ui.dp(c, 20), Ui.dp(c, 24), Ui.dp(c, 20), Ui.dp(c, 4));

        grid = Ui.column(c);
        screen.body.addView(grid, Ui.matchWidth());
        return screen.root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        VocabViewModel vm = new ViewModelProvider(this).get(VocabViewModel.class);
        vm.library().observe(getViewLifecycleOwner(), l -> {
            library = l;
            render();
        });
        vm.load();
        sortView.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(requireContext(), v);
            for (int i = 0; i < sorts().length; i++) {
                menu.getMenu().add(0, i, i, getString(R.string.vocab_sort_by, sorts()[i]));
            }
            menu.setOnMenuItemClickListener(item -> {
                sort = item.getItemId();
                render();
                return true;
            });
            menu.show();
        });
    }

    private TextView dropdown(Context c) {
        TextView t = Ui.text(c, "", 14, 600, Ui.INK);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(Ui.dp(c, 12), 0, Ui.dp(c, 12), 0);
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t.setBackground(Ui.shape(c, Ui.CARD, 12, Ui.LINE, 1));
        t.setClickable(true);
        return t;
    }

    private void render() {
        Context c = requireContext();
        if (library == null) {
            return;
        }
        sortView.setText(getString(R.string.vocab_sort_by, sorts()[sort]));
        List<VocabViewModel.Pack> packs = new ArrayList<>(library.packs);
        switch (sort) {
            case 1:
                packs.sort((a, b) -> b.id.compareTo(a.id));
                break;
            case 2:
                packs.sort((a, b) -> b.total() - a.total());
                break;
            case 3:
                packs.sort((a, b) -> b.percent() - a.percent());
                break;
            default:
                packs.sort((a, b) -> a.id.compareTo(b.id));
        }
        count.setText(Ui.plural(requireContext(), R.plurals.plural_packs, packs.size()));
        grid.removeAllViews();
        for (int i = 0; i < packs.size(); i += 2) {
            LinearLayout line = Ui.row(c);
            line.setBaselineAligned(false);
            line.setWeightSum(2f);
            for (int j = 0; j < 2; j++) {
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                if (j == 1) {
                    p.leftMargin = Ui.dp(c, 12);
                }
                if (i + j < packs.size()) {
                    line.addView(card(c, packs.get(i + j)), p);
                } else {
                    line.addView(new View(c), p);
                }
            }
            Ui.add(grid, line, i == 0 ? 0 : 12);
        }
    }

    private View card(Context c, VocabViewModel.Pack pack) {
        int[] tone = Ui.TONES[pack.tone];
        LinearLayout card = Ui.card(c, 12, 16);
        card.setClickable(true);
        card.setOnClickListener(v -> go(R.id.action_allpacks_to_pack, packArgs(pack.id)));
        TextView name = Ui.text(c, pack.title, 14, 700, Ui.INK);
        name.setLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        card.addView(name, Ui.matchWidth());
        Ui.add(card, Ui.text(c, Ui.plural(c, R.plurals.plural_words, pack.total()) + " · " + c.getString(R.string.vocab_learned_n, pack.learned), 11, 400, Ui.MUTED), 8);
        Ui.add(card, Ui.progress(c, pack.percent(), tone[0]), 8);
        TextView cat = Ui.pill(c, c.getString(R.string.vocab_percent_learned, pack.percent()), tone[1], tone[2], 11);
        LinearLayout.LayoutParams p = Ui.wrap();
        p.topMargin = Ui.dp(c, 8);
        card.addView(cat, p);
        return card;
    }
}
