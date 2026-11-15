package com.masterenglish.app.ui.analytics;

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
import com.masterenglish.app.data.local.entity.UserEntity;
import com.masterenglish.app.ui.ds.DsFragment;
import com.masterenglish.app.ui.ds.Ui;
import com.masterenglish.app.ui.vocab.PracticeViewModel;
import com.masterenglish.app.ui.vocab.VocabViewModel;
import com.masterenglish.app.utils.CefrLevel;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/** Analytics: CEFR level and XP, words practised over time, word lists and accuracy by stage. */
@AndroidEntryPoint
public class AnalyticsFragment extends DsFragment {

    private static final String[] PERIODS = {"Week", "Month", "All time"};

    private Ui.Screen screen;
    private LinearLayout periodHost;
    private LinearLayout content;
    private VocabViewModel.Library library;
    private UserEntity user;
    private int period = 1;
    private boolean showReview = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        screen = new Ui.Screen(c, 24);
        screen.top.addView(Ui.titleBar(c, onBack(), "Analytics", null), Ui.matchWidth());
        periodHost = Ui.column(c);
        Ui.add(screen.top, periodHost, 14);
        content = Ui.column(c);
        screen.body.addView(content, Ui.matchWidth());
        return screen.root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        VocabViewModel vm = new ViewModelProvider(this).get(VocabViewModel.class);
        vm.user().observe(getViewLifecycleOwner(), u -> {
            user = u;
            render();
        });
        vm.library().observe(getViewLifecycleOwner(), l -> {
            library = l;
            render();
        });
        vm.load();
    }

    private long periodStart() {
        LocalDate today = LocalDate.now();
        LocalDate from = period == 0 ? today.minusDays(6) : period == 1 ? today.minusDays(27) : LocalDate.of(1970, 1, 1);
        return from.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private void render() {
        if (library == null || user == null) {
            return;
        }
        Context c = requireContext();
        periodHost.removeAllViews();
        periodHost.addView(Ui.segmented(c, PERIODS, period, Ui.GREEN_INK, i -> {
            period = i;
            render();
        }), Ui.matchWidth());

        content.removeAllViews();
        long start = periodStart();
        int inPeriod = 0;
        for (VocabViewModel.Word w : library.words) {
            if (w.lastReviewedAt >= start) {
                inPeriod++;
            }
        }

        LinearLayout top = Ui.row(c);
        top.setBaselineAligned(false);
        top.addView(metric(c, "CEFR level", CefrLevel.of(user.experiencePoints), CefrLevel.nextLabel(user.experiencePoints), Ui.AMBER_INK),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        p.leftMargin = Ui.dp(c, 10);
        top.addView(metric(c, "XP", String.format(Locale.US, "%,d", user.experiencePoints),
                inPeriod + " words " + PERIODS[period].toLowerCase(Locale.ENGLISH), Ui.INK), p);
        content.addView(top, Ui.matchWidth());

        Ui.add(content, practicedChart(c, start), 12);
        Ui.add(content, wordList(c), 16);
        Ui.add(content, accuracy(c), 16);
    }

    private View metric(Context c, String label, String value, String note, int color) {
        LinearLayout box = Ui.card(c, 14, 16);
        box.addView(Ui.text(c, label, 11, 600, Ui.MUTED));
        Ui.add(box, Ui.text(c, value, 24, 700, color), 4);
        Ui.add(box, Ui.text(c, note, 11, 400, Ui.MUTED), 2);
        return box;
    }

    /** Words practised per bucket: days for a week, weeks for a month, months for all time. */
    private View practicedChart(Context c, long start) {
        LinearLayout card = Ui.card(c, 16, 18);
        LinearLayout head = Ui.row(c);
        head.addView(Ui.text(c, "Words practiced", 15, 700, Ui.INK), Ui.weight(1f));
        card.addView(head, Ui.matchWidth());

        int buckets = period == 0 ? 7 : period == 1 ? 4 : 6;
        int[] counts = new int[buckets];
        String[] labels = new String[buckets];
        LocalDate today = LocalDate.now();
        for (int i = 0; i < buckets; i++) {
            labels[i] = period == 0 ? today.minusDays(buckets - 1 - i).getDayOfWeek()
                    .getDisplayName(java.time.format.TextStyle.NARROW, Locale.ENGLISH)
                    : period == 1 ? "W" + (i + 1)
                    : today.minusMonths(buckets - 1 - i).getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH);
        }
        for (VocabViewModel.Word w : library.words) {
            if (w.lastReviewedAt <= 0) {
                continue;
            }
            LocalDate d = java.time.Instant.ofEpochMilli(w.lastReviewedAt).atZone(ZoneId.systemDefault()).toLocalDate();
            int bucket;
            if (period == 0) {
                bucket = buckets - 1 - (int) java.time.temporal.ChronoUnit.DAYS.between(d, today);
            } else if (period == 1) {
                bucket = buckets - 1 - (int) (java.time.temporal.ChronoUnit.DAYS.between(d, today) / 7);
            } else {
                bucket = buckets - 1 - (int) java.time.temporal.ChronoUnit.MONTHS.between(d.withDayOfMonth(1), today.withDayOfMonth(1));
            }
            if (bucket >= 0 && bucket < buckets) {
                counts[bucket]++;
            }
        }
        int max = 1;
        for (int n : counts) {
            max = Math.max(max, n);
        }
        card.setTag(null);
        LinearLayout bars = Ui.row(c);
        bars.setGravity(Gravity.BOTTOM);
        bars.setPadding(0, 0, 0, 0);
        for (int i = 0; i < buckets; i++) {
            LinearLayout col = Ui.column(c);
            col.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
            col.addView(Ui.text(c, String.valueOf(counts[i]), 11, 600, i == buckets - 1 ? Ui.GREEN_INK : Ui.INK));
            View bar = new View(c);
            bar.setBackground(Ui.shape(c, i == buckets - 1 ? Ui.GREEN : Ui.GREEN_TINT, 8));
            int h = Math.max(4, Ui.dp(c, 80f * counts[i] / max));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(Ui.dp(c, 28), h);
            bp.topMargin = Ui.dp(c, 6);
            col.addView(bar, bp);
            bars.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        }
        Ui.add(card, bars, 14);
        card.getChildAt(card.getChildCount() - 1).setLayoutParams(margin(c, Ui.dp(c, 112), 14));
        LinearLayout axis = Ui.row(c);
        for (String l : labels) {
            TextView t = Ui.text(c, l, 11, 400, Ui.MUTED);
            t.setGravity(Gravity.CENTER);
            axis.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        }
        Ui.add(card, axis, 6);
        return card;
    }

    private LinearLayout.LayoutParams margin(Context c, int heightPx, int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx);
        p.topMargin = Ui.dp(c, topDp);
        return p;
    }

    private View wordList(Context c) {
        LinearLayout card = Ui.card(c, 16, 18);
        LinearLayout head = Ui.row(c);
        head.addView(Ui.text(c, "Word list", 15, 700, Ui.INK), Ui.weight(1f));
        TextView all = Ui.text(c, "See all", 13, 600, Ui.GREEN);
        all.setMinHeight(Ui.dp(c, 44));
        all.setGravity(Gravity.CENTER_VERTICAL);
        all.setOnClickListener(v -> go(R.id.action_analytics_to_wordlist, null));
        head.addView(all, Ui.wrap());
        card.addView(head, Ui.matchWidth());

        List<VocabViewModel.Word> review = new ArrayList<>();
        for (VocabViewModel.Word w : library.words) {
            if (w.due) {
                review.add(w);
            }
        }
        String[] labels = {"Total (" + library.words.size() + ")", "Review (" + review.size() + ")"};
        Ui.add(card, Ui.segmented(c, labels, showReview ? 1 : 0, showReview ? Ui.ORANGE_INK : Ui.GREEN_INK, i -> {
            showReview = i == 1;
            render();
        }), 4);
        List<VocabViewModel.Word> shown = showReview ? review : library.words;
        for (int i = 0; i < Math.min(3, shown.size()); i++) {
            VocabViewModel.Word w = shown.get(i);
            LinearLayout row = Ui.row(c);
            row.setPadding(Ui.dp(c, 4), Ui.dp(c, 12), Ui.dp(c, 4), Ui.dp(c, 12));
            LinearLayout names = Ui.column(c);
            names.addView(Ui.text(c, w.term, 15, 600, Ui.INK));
            Ui.add(names, Ui.text(c, w.subtitle(), 11, 400, Ui.MUTED), 2);
            row.addView(names, Ui.weight(1f));
            if (showReview) {
                row.addView(Ui.pill(c, w.mastery + "%", Ui.ORANGE_TINT, Ui.ORANGE_INK, 11), Ui.wrap());
            } else {
                int[] colors = Ui.statusColors(w.learned() ? "Learned" : "New");
                row.addView(Ui.pill(c, w.learned() ? "Learned" : "New", colors[0], colors[1], 11), Ui.wrap());
            }
            Ui.add(card, row, i == 0 ? 6 : 0);
        }
        if (shown.isEmpty()) {
            Ui.add(card, Ui.text(c, "Nothing due for review.", 13, 400, Ui.MUTED), 14);
        }
        return card;
    }

    private View accuracy(Context c) {
        LinearLayout card = Ui.card(c, 16, 18);
        LinearLayout head = Ui.row(c);
        head.addView(Ui.text(c, "Accuracy by stage", 15, 700, Ui.INK), Ui.weight(1f));
        head.addView(Ui.text(c, "% correct", 12, 400, Ui.MUTED), Ui.wrap());
        card.addView(head, Ui.matchWidth());
        List<PracticeViewModel.Stage> stages = new ArrayList<>();
        for (PracticeViewModel.Stage s : PracticeViewModel.Stage.values()) {
            if (s.graded()) {
                stages.add(s);
            }
        }
        float[] values = new float[stages.size()];
        String[] names = new String[stages.size()];
        boolean any = false;
        for (int i = 0; i < stages.size(); i++) {
            int a = PracticeViewModel.accuracy(c, stages.get(i));
            any |= a >= 0;
            values[i] = Math.max(0, a) / 100f;
            names[i] = stages.get(i) == PracticeViewModel.Stage.FILL_BLANK ? "Fill blank"
                    : stages.get(i) == PracticeViewModel.Stage.CHOICE ? "Choice" : stages.get(i).label;
        }
        if (!any) {
            Ui.add(card, Ui.text(c, "Finish a practice round to see your accuracy.", 13, 400, Ui.MUTED), 14);
            return card;
        }
        RadarChartView radar = new RadarChartView(c);
        radar.setData(values, names);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(c, 260));
        rp.topMargin = Ui.dp(c, 6);
        card.addView(radar, rp);
        return card;
    }
}
