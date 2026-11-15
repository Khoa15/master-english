package com.masterenglish.app.ui.subscription;

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

import com.google.android.material.snackbar.Snackbar;
import com.masterenglish.app.R;
import com.masterenglish.app.ui.ds.DsFragment;
import com.masterenglish.app.ui.ds.Ui;
import com.masterenglish.app.ui.vocab.VocabViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/** Subscription: current plan and the plans on offer. Purchases are not wired to a store yet. */
@AndroidEntryPoint
public class SubscriptionFragment extends DsFragment {

    private static final int FREE_DAILY_WORDS = 20;

    private Ui.Screen screen;
    private LinearLayout current;
    private LinearLayout plans;
    private boolean yearly = true;
    private boolean premium = true;
    private int wordsToday;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        screen = new Ui.Screen(c, 16);
        screen.top.addView(Ui.titleBar(c, onBack(), "Subscription", null), Ui.matchWidth());
        current = Ui.card(c, 16, 18);
        screen.body.addView(current, Ui.matchWidth());
        Ui.add(screen.body, Ui.text(c, "Choose a plan", 18, 700, Ui.INK), 22);
        plans = Ui.column(c);
        Ui.add(screen.body, plans, 10);

        LinearLayout foot = Ui.column(c);
        TextView start = Ui.primaryButton(c, "Start Premium");
        start.setOnClickListener(v -> Snackbar.make(screen.root,
                "Payments are not available yet. You are on the Free plan.", Snackbar.LENGTH_LONG).show());
        foot.addView(start, Ui.matchWidth());
        TextView small = Ui.text(c, "Restore purchase · Cancel anytime", 12, 400, Ui.MUTED);
        small.setGravity(Gravity.CENTER);
        Ui.add(foot, small, 10);
        screen.setFooter(foot);
        return screen.root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        VocabViewModel vm = new ViewModelProvider(this).get(VocabViewModel.class);
        vm.library().observe(getViewLifecycleOwner(), l -> {
            wordsToday = l.wordsToday;
            drawCurrent();
        });
        vm.load();
        drawCurrent();
        drawPlans();
    }

    private void drawCurrent() {
        Context c = requireContext();
        current.removeAllViews();
        LinearLayout head = Ui.row(c);
        head.addView(Ui.text(c, "Current plan", 12, 600, Ui.MUTED), Ui.weight(1f));
        head.addView(Ui.pill(c, "Free", Ui.CHIP, Ui.MUTED, 11), Ui.wrap());
        current.addView(head, Ui.matchWidth());
        Ui.add(current, Ui.text(c, "Free plan", 20, 700, Ui.INK), 6);
        LinearLayout used = Ui.row(c);
        used.addView(Ui.text(c, "Words per day", 13, 400, Ui.MUTED), Ui.weight(1f));
        used.addView(Ui.text(c, Math.min(wordsToday, FREE_DAILY_WORDS) + " / " + FREE_DAILY_WORDS, 13, 700, Ui.INK), Ui.wrap());
        Ui.add(current, used, 14);
        Ui.add(current, Ui.progress(c, wordsToday * 100 / FREE_DAILY_WORDS, Ui.GREEN), 8);
    }

    private void drawPlans() {
        Context c = requireContext();
        plans.removeAllViews();
        plans.addView(Ui.segmented(c, new String[]{"Monthly", "Yearly  Save 40%"}, yearly ? 1 : 0, Ui.GREEN_INK, i -> {
            yearly = i == 1;
            drawPlans();
        }), Ui.matchWidth());
        Ui.add(plans, plan(c, "Premium", yearly ? "$49.99" : "$6.99", yearly ? "per year" : "per month",
                new String[]{"Unlimited daily words", "Speaking feedback with AI tutor", "All word packs and offline mode",
                        "Detailed analytics"}, premium, yearly ? "Best value" : null, true), 12);
        Ui.add(plans, plan(c, "Plus", "$7.99", "per month",
                new String[]{"50 words per day", "Listening and reading practice"}, !premium, null, false), 12);
    }

    private View plan(Context c, String name, String price, String per, String[] perks, boolean selected,
                      @Nullable String ribbon, boolean isPremium) {
        LinearLayout card = Ui.column(c);
        card.setBackground(Ui.shape(c, selected ? Ui.GREEN_TINT : Ui.CARD, 18, selected ? Ui.GREEN : Ui.LINE, selected ? 2 : 1));
        card.setPadding(Ui.dp(c, 16), Ui.dp(c, 16), Ui.dp(c, 16), Ui.dp(c, 16));
        card.setClickable(true);
        card.setOnClickListener(v -> {
            premium = isPremium;
            drawPlans();
        });
        if (ribbon != null) {
            card.addView(Ui.pill(c, ribbon, Ui.GREEN, 0xFFFFFFFF, 11), Ui.wrap());
        }
        LinearLayout head = Ui.row(c);
        head.addView(Ui.text(c, name, 18, 700, Ui.INK), Ui.weight(1f));
        LinearLayout priceBox = Ui.column(c);
        priceBox.setGravity(Gravity.END);
        priceBox.addView(Ui.text(c, price, 18, 700, Ui.GREEN_INK));
        priceBox.addView(Ui.text(c, per, 11, 400, Ui.MUTED));
        head.addView(priceBox, Ui.wrap());
        Ui.add(card, head, ribbon == null ? 0 : 8);
        for (String perk : perks) {
            LinearLayout row = Ui.row(c);
            row.addView(Ui.icon(c, R.drawable.ic_ds_check, 16, Ui.GREEN));
            LinearLayout.LayoutParams p = Ui.weight(1f);
            p.leftMargin = Ui.dp(c, 8);
            row.addView(Ui.text(c, perk, 14, 400, Ui.INK), p);
            Ui.add(card, row, 10);
        }
        return card;
    }
}
