package com.doradodo.app.ui.entertainment;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.doradodo.app.R;
import com.doradodo.app.data.local.entity.ContestEntity;
import com.doradodo.app.data.local.entity.ContestPrizeEntity;
import com.doradodo.app.data.repository.ContestDetail;
import com.doradodo.app.data.repository.ContestState;
import com.doradodo.app.ui.ds.Ui;

import java.time.ZoneId;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/** "Giải đấu": one contest read from the database, with its times, prizes and the learner's registration. */
@AndroidEntryPoint
public class ContestFragment extends GameFragment {

    private Ui.Screen screen;
    private LinearLayout body;
    private String contestId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        contestId = requireArguments().getString(Routes.ARG_CONTEST, "");
        screen = new Ui.Screen(c, 16);
        screen.top.setPadding(Ui.dp(c, 20), Ui.dp(c, 16), Ui.dp(c, 20), 0);
        LinearLayout bar = Ui.row(c);
        bar.addView(Ui.backButton(c, R.drawable.ic_ds_back, onBack(), getString(R.string.ent_back)));
        LinearLayout.LayoutParams tp = Ui.wrap();
        tp.leftMargin = Ui.dp(c, 12);
        bar.addView(Ui.text(c, getString(R.string.ent_contest_title), 22, 700, Ui.INK), tp);
        screen.top.addView(bar, Ui.matchWidth());
        body = screen.body;
        body.addView(loadingState(c));
        reload();
        return screen.root;
    }

    private void reload() {
        load(() -> vm().contests().loadContest(contestId), this::render);
    }

    private void render(@Nullable ContestDetail d) {
        Context c = requireContext();
        body.removeAllViews();
        if (d == null) {
            body.addView(emptyState(c, getString(R.string.ent_contest_not_found), getString(R.string.ent_contest_gone)), Ui.matchWidth());
            return;
        }
        long now = System.currentTimeMillis();
        ContestEntity k = d.card.contest;
        ContestState state = d.card.state;
        GameUi.Accent accent = GameUi.accent(k.accent);

        LinearLayout hero = Ui.card(c, 16, 20);
        hero.setBackground(Ui.shape(c, Ui.CARD, 20, Ui.LINE, 2));
        LinearLayout head = Ui.row(c);
        String status;
        String countLabel;
        long target;
        int chipBg;
        int chipInk;
        switch (state) {
            case LIVE:
                status = getString(R.string.ent_st_live);
                countLabel = getString(R.string.ent_cd_ends);
                target = k.endsAt;
                chipBg = GameUi.AMBER_SOFT;
                chipInk = GameUi.AMBER_EDGE;
                break;
            case REGISTRATION:
                status = getString(R.string.ent_st_registering);
                countLabel = getString(R.string.ent_cd_starts);
                target = k.startsAt;
                chipBg = Ui.GREEN_TINT;
                chipInk = Ui.GREEN_INK;
                break;
            case REGISTRATION_SOON:
                status = getString(R.string.ent_st_soon);
                countLabel = getString(R.string.ent_cd_reg_opens);
                target = k.registrationOpensAt;
                chipBg = 0xFFD9E5F7;
                chipInk = GameUi.BLUE_EDGE;
                break;
            default:
                status = getString(R.string.ent_st_ended);
                countLabel = "";
                target = now;
                chipBg = Ui.CHIP;
                chipInk = Ui.MUTED;
                break;
        }
        TextView chip = Ui.text(c, status, 11, 700, chipInk);
        chip.setBackground(Ui.shape(c, chipBg, 8));
        chip.setPadding(Ui.dp(c, 8), Ui.dp(c, 3), Ui.dp(c, 8), Ui.dp(c, 3));
        head.addView(chip, Ui.wrap());
        head.addView(new View(c), new LinearLayout.LayoutParams(0, 1, 1f));
        head.addView(Ui.text(c, countLabel, 13, 600, Ui.MUTED), Ui.wrap());
        hero.addView(head, Ui.matchWidth());
        Ui.add(hero, Ui.text(c, k.title, 22, 700, Ui.INK), 10);
        if (k.description != null) {
            TextView desc = Ui.text(c, k.description, 14, 400, Ui.MUTED);
            desc.setLineSpacing(0, 1.2f);
            Ui.add(hero, desc, 4);
        }

        if (state != ContestState.ENDED) {
            long minutes = Math.max(0, target - now) / 60_000L;
            String[][] cd = {{String.format(Locale.US, "%02d", minutes / 1440), getString(R.string.ent_days_caps)},
                    {String.format(Locale.US, "%02d", (minutes % 1440) / 60), getString(R.string.ent_hours_caps)},
                    {String.format(Locale.US, "%02d", minutes % 60), getString(R.string.ent_minutes_caps)}};
            LinearLayout countdown = Ui.row(c);
            for (int i = 0; i < cd.length; i++) {
                LinearLayout box = Ui.column(c);
                box.setGravity(Gravity.CENTER_HORIZONTAL);
                box.setBackground(Ui.shape(c, Ui.CARD, 14, Ui.LINE, 2));
                box.setPadding(0, Ui.dp(c, 10), 0, Ui.dp(c, 10));
                box.addView(Ui.text(c, cd[i][0], 20, 700, Ui.INK), Ui.wrap());
                box.addView(Ui.text(c, cd[i][1], 11, 600, Ui.MUTED), Ui.wrap());
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                if (i > 0) {
                    p.leftMargin = Ui.dp(c, 8);
                }
                countdown.addView(box, p);
            }
            Ui.add(hero, countdown, 14);
        }

        LinearLayout meta = Ui.row(c);
        meta.addView(Ui.text(c, d.card.participants > 0 ? getString(R.string.ent_participants, d.card.participants)
                : getString(R.string.ent_no_participants), 13, 600, Ui.MUTED), Ui.weight(1f));
        meta.addView(Ui.text(c, getString(R.string.ent_questions_duration, k.questionCount, k.durationMinutes), 13, 600, Ui.MUTED), Ui.wrap());
        Ui.add(hero, meta, 14);

        ZoneId zone = vm().timeZone().zone();
        String zoneName = ContestTimes.zone(k.startsAt, zone);
        Ui.add(hero, Ui.text(c, getString(R.string.ent_registration_window,
                ContestTimes.format(k.registrationOpensAt, zone, ContestTimes.DATE_TIME),
                ContestTimes.format(k.registrationClosesAt, zone, ContestTimes.DATE_TIME)) + " (" + zoneName + ")",
                12, 400, Ui.MUTED), 10);
        Ui.add(hero, Ui.text(c, getString(R.string.ent_contest_window,
                ContestTimes.format(k.startsAt, zone, ContestTimes.DATE_TIME),
                ContestTimes.format(k.endsAt, zone, ContestTimes.DATE_TIME)) + " (" + zoneName + ")",
                12, 400, Ui.MUTED), 4);
        if (d.card.registered) {
            Ui.add(hero, Ui.text(c, getString(R.string.ent_you_registered), 13, 700, Ui.GREEN), 10);
        }
        body.addView(hero, Ui.matchWidth());

        if (!d.prizes.isEmpty()) {
            Ui.add(body, Ui.text(c, getString(R.string.ent_prizes), 19, 700, Ui.INK), 22);
            int index = 0;
            for (ContestPrizeEntity p : d.prizes) {
                prize(c, p, index++, accent);
            }
        }
        ContestAction action = ContestAction.of(c, d.card, now);
        TextView button = GameUi.footerButton(c, action.label, Ui.INK, Ui.AMBER, GameUi.AMBER_EDGE);
        if (action.kind == ContestAction.Kind.NONE) {
            GameUi.setReady(button, false);
            button.setClickable(false);
        } else {
            button.setOnClickListener(v -> {
                if (action.kind == ContestAction.Kind.PLAY) {
                    go(R.id.contestPlayFragment, Routes.contestArgs(contestId));
                } else {
                    load(() -> vm().contests().register(contestId), ok -> {
                        Toast.makeText(requireContext(), Boolean.TRUE.equals(ok) ? getString(R.string.ent_registered_ok)
                                : getString(R.string.ent_register_failed), Toast.LENGTH_SHORT).show();
                        reload();
                    });
                }
            });
        }
        screen.setFooter(button);
    }

    private void prize(Context c, ContestPrizeEntity p, int index, GameUi.Accent accent) {
        String badgeText;
        String name;
        if (p.rankTo == null) {
            badgeText = p.rankFrom + "+";
            name = getString(R.string.ent_top_from, p.rankFrom);
        } else if (p.rankFrom == p.rankTo) {
            badgeText = String.valueOf(p.rankFrom);
            name = p.rankFrom == 1 ? getString(R.string.ent_rank_first) : p.rankFrom == 2 ? getString(R.string.ent_rank_second)
                    : p.rankFrom == 3 ? getString(R.string.ent_rank_third) : getString(R.string.ent_rank_n, p.rankFrom);
        } else {
            badgeText = p.rankFrom + "–" + p.rankTo;
            name = getString(R.string.ent_top_range, p.rankFrom, p.rankTo);
        }
        int badge;
        int bg;
        int border;
        int ink;
        if (p.rankFrom == 1 && p.rankTo != null && p.rankTo == 1) {
            badge = 0xFFE0A030;
            bg = 0xFFFDF6E3;
            border = GameUi.AMBER_BORDER;
            ink = GameUi.AMBER_EDGE;
        } else if (p.rankFrom == 2 && p.rankTo != null && p.rankTo == 2) {
            badge = 0xFFB9BCC4;
            bg = 0xFFF4F5F7;
            border = 0xFFD5D8DE;
            ink = Ui.MUTED;
        } else if (p.rankFrom == 3 && p.rankTo != null && p.rankTo == 3) {
            badge = 0xFFC98A52;
            bg = 0xFFFBF1EA;
            border = 0xFFE9C3A3;
            ink = 0xFF9A5F32;
        } else if (index % 2 == 0) {
            badge = Ui.BLUE;
            bg = GameUi.BLUE_SOFT;
            border = GameUi.BLUE_BORDER;
            ink = GameUi.BLUE_EDGE;
        } else {
            badge = GameUi.PURPLE;
            bg = 0xFFF1ECFA;
            border = GameUi.PURPLE_BORDER;
            ink = GameUi.PURPLE_EDGE;
        }
        LinearLayout row = Ui.row(c);
        row.setBackground(Ui.shape(c, bg, 20, border, 2));
        row.setPadding(Ui.dp(c, 12), Ui.dp(c, 12), Ui.dp(c, 12), Ui.dp(c, 12));
        TextView r = Ui.text(c, badgeText, badgeText.length() > 2 ? 13 : 18, 700, android.graphics.Color.WHITE);
        r.setGravity(Gravity.CENTER);
        r.setBackground(Ui.shape(c, badge, 22));
        row.addView(r, new LinearLayout.LayoutParams(Ui.dp(c, 44), Ui.dp(c, 44)));
        LinearLayout.LayoutParams np = Ui.weight(1f);
        np.leftMargin = Ui.dp(c, 12);
        row.addView(Ui.text(c, name, 15, 700, Ui.INK), np);
        row.addView(Ui.text(c, getString(R.string.ent_gems, p.rewardGems), 14, 700, ink), Ui.wrap());
        Ui.add(body, row, 10);
    }
}
