package com.masterenglish.app.ui.vocab;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.masterenglish.app.R;
import com.masterenglish.app.ui.ds.DsFragment;
import com.masterenglish.app.ui.ds.Ui;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/** End of a round: score, time and the words that were right or still need review. */
@AndroidEntryPoint
public class SummaryFragment extends DsFragment {

    private Ui.Screen screen;
    private PracticeViewModel practice;
    private boolean showReview = true;
    private PracticeViewModel.Result result;
    private LinearLayout listBox;
    private LinearLayout tabsHost;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        screen = new Ui.Screen(requireContext(), 16);
        return screen.root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        practice = new ViewModelProvider(requireActivity()).get(PracticeViewModel.class);
        practice.result().observe(getViewLifecycleOwner(), r -> {
            if (r != null) {
                result = r;
                showReview = !r.review.isEmpty();
                bind();
            }
        });
    }

    private void bind() {
        Context c = requireContext();
        screen.top.removeAllViews();
        screen.body.removeAllViews();
        screen.top.setGravity(Gravity.CENTER_HORIZONTAL);

        FrameLayout badge = new FrameLayout(c);
        badge.setBackground(Ui.shape(c, Ui.GREEN_TINT, 999));
        badge.addView(Ui.icon(c, R.drawable.ic_ds_check, 36, Ui.GREEN),
                new FrameLayout.LayoutParams(Ui.dp(c, 36), Ui.dp(c, 36), Gravity.CENTER));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(Ui.dp(c, 72), Ui.dp(c, 72));
        bp.gravity = Gravity.CENTER_HORIZONTAL;
        bp.topMargin = Ui.dp(c, 16);
        screen.top.addView(badge, bp);
        TextView title = Ui.text(c, "Round complete!", 26, 700, Ui.INK);
        title.setGravity(Gravity.CENTER);
        Ui.add(screen.top, title, 16);
        TextView sub = Ui.text(c, result.title + " · " + result.total() + " words practiced", 14, 400, Ui.MUTED);
        sub.setGravity(Gravity.CENTER);
        Ui.add(screen.top, sub, 6);

        LinearLayout stats = Ui.row(c);
        stats.setBaselineAligned(false);
        stat(c, stats, result.correct.size() + "/" + result.total(), "Correct", Ui.GREEN_INK, 0);
        stat(c, stats, String.valueOf(PracticeViewModel.Stage.values().length), "Stages done", Ui.BLUE_INK, 10);
        stat(c, stats, result.minutes + " min", "Time", Ui.AMBER_INK, 10);
        Ui.add(screen.top, stats, 24);

        tabsHost = Ui.column(c);
        Ui.add(screen.top, tabsHost, 20);
        listBox = Ui.column(c);
        screen.body.addView(listBox, Ui.matchWidth());
        drawTabs();
        drawList();

        LinearLayout actions = Ui.row(c);
        ImageView home = Ui.icon(c, R.drawable.ic_ds_home, 22, Ui.INK);
        home.setScaleType(ImageView.ScaleType.CENTER);
        home.setBackground(Ui.shape(c, Ui.CARD, 16, Ui.LINE, 1));
        home.setContentDescription("Back to Home");
        home.setOnClickListener(v -> {
            practice.clear();
            go(R.id.action_summary_to_home, null);
        });
        actions.addView(home, new LinearLayout.LayoutParams(0, Ui.dp(c, 52), 3f));
        TextView back = Ui.primaryButton(c, "Back");
        back.setOnClickListener(v -> {
            practice.clear();
            back();
        });
        LinearLayout.LayoutParams bk = new LinearLayout.LayoutParams(0, Ui.dp(c, 52), 9f);
        bk.leftMargin = Ui.dp(c, 12);
        actions.addView(back, bk);
        screen.setFooter(actions);
    }

    private void stat(Context c, LinearLayout row, String value, String label, int color, float leftDp) {
        LinearLayout box = Ui.column(c);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        box.setBackground(Ui.shape(c, Ui.CARD, 16, Ui.LINE, 1));
        box.setPadding(Ui.dp(c, 8), Ui.dp(c, 14), Ui.dp(c, 8), Ui.dp(c, 14));
        box.addView(Ui.text(c, value, 20, 700, color));
        Ui.add(box, Ui.text(c, label, 11, 600, Ui.MUTED), 4);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        p.leftMargin = Ui.dp(c, leftDp);
        row.addView(box, p);
    }

    private void drawTabs() {
        tabsHost.removeAllViews();
        String[] labels = {"Correct (" + result.correct.size() + ")", "Review (" + result.review.size() + ")"};
        tabsHost.addView(Ui.segmented(requireContext(), labels, showReview ? 1 : 0,
                showReview ? Ui.ORANGE_INK : Ui.GREEN_INK, i -> {
                    showReview = i == 1;
                    drawTabs();
                    drawList();
                }), Ui.matchWidth());
    }

    private void drawList() {
        Context c = requireContext();
        listBox.removeAllViews();
        List<VocabViewModel.Word> list = showReview ? result.review : result.correct;
        if (list.isEmpty()) {
            Ui.add(listBox, Ui.text(c, showReview ? "Nothing to review. Great work!" : "No correct words this time.",
                    14, 400, Ui.MUTED), 16);
            return;
        }
        for (VocabViewModel.Word w : list) {
            LinearLayout row = Ui.row(c);
            row.setPadding(Ui.dp(c, 4), Ui.dp(c, 12), Ui.dp(c, 4), Ui.dp(c, 12));
            LinearLayout names = Ui.column(c);
            names.addView(Ui.text(c, w.term, 15, 600, Ui.INK));
            Ui.add(names, Ui.text(c, w.subtitle(), 12, 400, Ui.MUTED), 2);
            row.addView(names, Ui.weight(1f));
            row.addView(Ui.pill(c, showReview ? "Review" : "Correct",
                    showReview ? Ui.ORANGE_TINT : Ui.GREEN_TINT,
                    showReview ? Ui.ORANGE_INK : Ui.GREEN_INK, 11), Ui.wrap());
            listBox.addView(row, Ui.matchWidth());
            View line = new View(c);
            line.setBackgroundColor(Ui.LINE);
            listBox.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }
    }
}
