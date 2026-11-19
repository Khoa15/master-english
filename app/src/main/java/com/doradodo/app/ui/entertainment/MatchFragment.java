package com.doradodo.app.ui.entertainment;

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

import com.doradodo.app.R;
import com.doradodo.app.data.repository.GameWordRepository;
import com.doradodo.app.ui.ds.Ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Two columns of tiles: pick a word on the left, then its match on the right. */
public abstract class MatchFragment extends GameFragment {

    /** What differs between the matching games. */
    protected static final class Config {
        String title;
        String leftLabel;
        String rightLabel;
        String idle;
        /** Format string with one argument: the picked word. */
        int selectedRes;
        String emptyTitle;
        String emptyMessage;
        /** Filled in once the pairs are loaded. */
        String[] left = new String[0];
        String[] right = new String[0];
        int[] order = new int[0];
        int badgeBg;
        int badgeFg;
        int progressColor;
        int selBg;
        int selBorder;
        int selInk;
    }

    protected abstract Config config();

    /** The pairs to match ({left, right}), read from the database off the main thread. */
    protected abstract List<String[]> loadPairs(GameWordRepository repo);

    /** Fewest pairs that make a round worth playing. */
    private static final int MIN_PAIRS = 3;

    private Config cfg;
    private GameUi.Page page;
    private TextView counter;
    private LinearLayout leftCol;
    private LinearLayout rightCol;
    private int selected = -1;
    private int wrong = -1;
    private final Set<Integer> done = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        cfg = config();
        page = GameUi.page(c, true, onBack(), cfg.title, cfg.idle, cfg.badgeBg, cfg.badgeFg, cfg.progressColor);
        counter = Ui.text(c, "", 12, 600, Ui.MUTED);
        Ui.add(page.screen.top, counter, 6);

        LinearLayout columns = Ui.row(c);
        columns.setGravity(Gravity.TOP);
        columns.setBaselineAligned(false);
        leftCol = Ui.column(c);
        rightCol = Ui.column(c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = Ui.dp(c, 6);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        rp.leftMargin = Ui.dp(c, 6);
        columns.addView(leftCol, lp);
        columns.addView(rightCol, rp);
        columns.setVisibility(View.GONE);
        page.screen.body.addView(columns, Ui.matchWidth());
        board = columns;
        status = loadingState(c);
        page.screen.body.addView(status, 0);

        TextView again = GameUi.footerButton(c, getString(R.string.ent_play_again), Color_WHITE, Ui.GREEN, GameUi.GREEN_EDGE);
        again.setOnClickListener(v -> startRound());
        GameUi.setFooter(c, page.screen, again);
        page.screen.footer.setVisibility(View.GONE);
        startRound();
        return page.screen.root;
    }

    private View board;
    private View status;

    /** Loads a fresh set of pairs and starts over. */
    private void startRound() {
        selected = -1;
        wrong = -1;
        done.clear();
        page.screen.footer.setVisibility(View.GONE);
        load(() -> loadPairs(vm().words()), pairs -> {
            Context c = requireContext();
            ViewGroup parent = page.screen.body;
            parent.removeView(status);
            if (pairs == null || pairs.size() < MIN_PAIRS) {
                board.setVisibility(View.GONE);
                status = emptyState(c, cfg.emptyTitle, cfg.emptyMessage);
                parent.addView(status, 0, Ui.matchWidth());
                page.badge.setText(getString(R.string.ent_score_badge, 0));
                return;
            }
            cfg.left = new String[pairs.size()];
            cfg.right = new String[pairs.size()];
            List<Integer> order = new ArrayList<>();
            for (int i = 0; i < pairs.size(); i++) {
                cfg.left[i] = pairs.get(i)[0];
                cfg.right[i] = pairs.get(i)[1];
                order.add(i);
            }
            Collections.shuffle(order);
            cfg.order = new int[order.size()];
            for (int i = 0; i < order.size(); i++) {
                cfg.order[i] = order.get(i);
            }
            board.setVisibility(View.VISIBLE);
            render();
        });
    }

    private static final int Color_WHITE = 0xFFFFFFFF;

    private void render() {
        Context c = requireContext();
        int n = cfg.left.length;
        if (n == 0) {
            return;
        }
        boolean won = done.size() == n;
        leftCol.removeAllViews();
        rightCol.removeAllViews();
        leftCol.addView(GameUi.label(c, cfg.leftLabel));
        rightCol.addView(GameUi.label(c, cfg.rightLabel));

        for (int i = 0; i < n; i++) {
            final int index = i;
            TextView t = tile(c, cfg.left[i]);
            if (done.contains(i)) {
                GameUi.style(c, t, Ui.GREEN_TINT, Ui.GREEN, Ui.GREEN_INK);
            } else if (selected == i) {
                GameUi.style(c, t, cfg.selBg, cfg.selBorder, cfg.selInk);
            }
            t.setOnClickListener(v -> {
                if (!done.contains(index) && done.size() < n) {
                    selected = index;
                    wrong = -1;
                    render();
                }
            });
            Ui.add(leftCol, t, 10);
        }
        for (int k = 0; k < n; k++) {
            final int pi = cfg.order[k];
            final int slot = k;
            TextView t = tile(c, cfg.right[pi]);
            if (done.contains(pi)) {
                GameUi.style(c, t, Ui.GREEN_TINT, Ui.GREEN, Ui.GREEN_INK);
            } else if (wrong == k) {
                GameUi.style(c, t, Ui.RED_TINT, Ui.RED, GameUi.RED_EDGE);
            }
            t.setOnClickListener(v -> {
                if (done.contains(pi) || done.size() == n || selected < 0) {
                    return;
                }
                if (selected == pi) {
                    done.add(pi);
                    selected = -1;
                    wrong = -1;
                } else {
                    wrong = slot;
                }
                render();
            });
            Ui.add(rightCol, t, 10);
        }

        String status;
        if (won) {
            status = getString(R.string.ent_completed);
        } else if (selected < 0) {
            status = cfg.idle;
        } else {
            status = getString(cfg.selectedRes, cfg.left[selected]);
        }
        page.subtitle.setText(status);
        page.badge.setText(getString(R.string.ent_score_badge, done.size() * 100));
        counter.setText(getString(R.string.ent_pairs_done, done.size(), n));
        Ui.setProgress(page.progress, done.size() * 100 / n);
        page.screen.footer.setVisibility(won ? View.VISIBLE : View.GONE);
    }

    private TextView tile(Context c, String text) {
        TextView t = Ui.text(c, text, 15, 700, Ui.INK);
        t.setGravity(Gravity.CENTER);
        t.setMinHeight(Ui.dp(c, 58));
        t.setPadding(Ui.dp(c, 12), Ui.dp(c, 10), Ui.dp(c, 12), Ui.dp(c, 10));
        t.setClickable(true);
        t.setFocusable(true);
        GameUi.style(c, t, Ui.CARD, Ui.LINE, Ui.INK);
        return t;
    }
}
