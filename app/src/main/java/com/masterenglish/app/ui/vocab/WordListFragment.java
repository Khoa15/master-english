package com.masterenglish.app.ui.vocab;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.masterenglish.app.R;
import com.masterenglish.app.ui.ds.DsFragment;
import com.masterenglish.app.ui.ds.Ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/** Every word: search and status filter over a two-column grid. */
@AndroidEntryPoint
public class WordListFragment extends DsFragment {

    private static final String[] STATUS = {"All", "Learned", "Review", "New"};

    private Ui.Screen screen;
    private TextView count;
    private TextView statusView;
    private LinearLayout grid;
    private VocabViewModel.Library library;
    private String query = "";
    private int status;
    private static final int PAGE = 120;
    private int shown = PAGE;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        screen = new Ui.Screen(c, 24);
        LinearLayout bar = Ui.titleBar(c, onBack(), "All words", "");
        count = (TextView) ((LinearLayout) bar.getChildAt(1)).getChildAt(1);
        screen.top.addView(bar, Ui.matchWidth());

        LinearLayout filters = Ui.row(c);
        LinearLayout search = Ui.row(c);
        search.setBackground(Ui.shape(c, Ui.CARD, 12, Ui.LINE, 1));
        search.setPadding(Ui.dp(c, 12), 0, Ui.dp(c, 8), 0);
        search.addView(Ui.icon(c, R.drawable.ic_ds_search, 18, Ui.MUTED));
        EditText input = new EditText(c);
        input.setHint("Search words or meanings");
        input.setSingleLine(true);
        input.setBackground(null);
        input.setTextSize(14);
        input.setTextColor(Ui.INK);
        input.setHintTextColor(0xFF8B948E);
        input.setPadding(Ui.dp(c, 8), 0, 0, 0);
        Ui.font(input, 400);
        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int d) { }
            @Override public void afterTextChanged(Editable s) {
                query = s.toString().trim().toLowerCase(Locale.ROOT);
                shown = PAGE;
                render();
            }
        });
        search.addView(input, Ui.weight(1f));
        filters.addView(search, new LinearLayout.LayoutParams(0, Ui.dp(c, 44), 1.6f));
        statusView = Ui.text(c, "", 14, 600, Ui.INK);
        statusView.setGravity(Gravity.CENTER_VERTICAL);
        statusView.setPadding(Ui.dp(c, 12), 0, Ui.dp(c, 12), 0);
        statusView.setBackground(Ui.shape(c, Ui.CARD, 12, Ui.LINE, 1));
        statusView.setClickable(true);
        statusView.setSingleLine(true);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, Ui.dp(c, 44), 1f);
        sp.leftMargin = Ui.dp(c, 10);
        filters.addView(statusView, sp);
        Ui.add(screen.top, filters, 14);

        grid = Ui.column(c);
        screen.body.addView(grid, Ui.matchWidth());
        return screen.root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        statusView.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(requireContext(), v);
            for (int i = 0; i < STATUS.length; i++) {
                menu.getMenu().add(0, i, i, "Status: " + STATUS[i]);
            }
            menu.setOnMenuItemClickListener(item -> {
                status = item.getItemId();
                shown = PAGE;
                render();
                return true;
            });
            menu.show();
        });
        VocabViewModel vm = new ViewModelProvider(this).get(VocabViewModel.class);
        vm.library().observe(getViewLifecycleOwner(), l -> {
            library = l;
            render();
        });
        vm.load();
    }

    private void render() {
        if (library == null) {
            return;
        }
        Context c = requireContext();
        statusView.setText("Status: " + STATUS[status]);
        List<VocabViewModel.Word> list = new ArrayList<>();
        for (VocabViewModel.Word w : library.words) {
            boolean match = query.isEmpty() || w.term.toLowerCase(Locale.ROOT).contains(query)
                    || w.meaning.toLowerCase(Locale.ROOT).contains(query);
            boolean inStatus = status == 0
                    || (status == 1 && w.learned())
                    || (status == 2 && w.due)
                    || (status == 3 && !w.learned());
            if (match && inStatus) {
                list.add(w);
            }
        }
        count.setText(String.format(Locale.US, "%,d words", list.size()));
        grid.removeAllViews();
        int end = Math.min(shown, list.size());
        for (int i = 0; i < end; i += 2) {
            LinearLayout line = Ui.row(c);
            line.setBaselineAligned(false);
            line.setWeightSum(2f);
            for (int j = 0; j < 2; j++) {
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
                if (j == 1) {
                    p.leftMargin = Ui.dp(c, 12);
                }
                line.addView(i + j < end ? card(c, list.get(i + j)) : new View(c), p);
            }
            Ui.add(grid, line, i == 0 ? 0 : 12);
        }
        if (list.isEmpty()) {
            Ui.add(grid, Ui.text(c, "No words match.", 14, 400, Ui.MUTED), 16);
        } else if (end < list.size()) {
            TextView more = Ui.text(c, "Show more (" + (list.size() - end) + " left)", 14, 700, Ui.GREEN);
            more.setGravity(Gravity.CENTER);
            more.setMinHeight(Ui.dp(c, 52));
            more.setOnClickListener(v -> {
                shown += PAGE;
                render();
            });
            Ui.add(grid, more, 8);
        }
    }

    private View card(Context c, VocabViewModel.Word w) {
        LinearLayout card = Ui.card(c, 12, 16);
        card.addView(Ui.text(c, w.term, 15, 700, Ui.INK));
        Ui.add(card, Ui.text(c, w.subtitle(), 12, 400, Ui.MUTED), 3);
        String label = w.due ? "Review" : w.learned() ? "Learned" : "New";
        int[] colors = Ui.statusColors(label);
        TextView chip = Ui.pill(c, label, colors[0], colors[1], 11);
        LinearLayout.LayoutParams p = Ui.wrap();
        p.topMargin = Ui.dp(c, 8);
        card.addView(chip, p);
        return card;
    }
}
