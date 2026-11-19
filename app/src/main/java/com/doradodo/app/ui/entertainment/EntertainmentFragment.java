package com.doradodo.app.ui.entertainment;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.doradodo.app.R;
import com.doradodo.app.data.local.entity.BannerEntity;
import com.doradodo.app.data.local.entity.EventEntity;
import com.doradodo.app.data.repository.EntertainmentRepository.ActivityCard;
import com.doradodo.app.data.repository.ContestCard;
import com.doradodo.app.data.repository.ContestState;
import com.doradodo.app.data.repository.EntertainmentRepository.Home;
import com.doradodo.app.ui.ds.Ui;
import com.doradodo.app.ui.vocab.VocabViewModel;

import java.util.ArrayList;
import java.time.ZoneId;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Entertainment tab ("Giải trí"), built from the "Trang Trò chơi" design. Every list on it (carousel, event,
 * contests, games, practice modes) is read from the database; the games and modes themselves are code.
 */
@AndroidEntryPoint
public class EntertainmentFragment extends GameFragment {

    /** The page shows this many games; more than that adds the "Tất cả" link. */
    private static final int GAMES_ON_PAGE = 4;
    private static final long CAROUSEL_MS = 4000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView streakText;
    private TextView gemText;
    private LinearLayout content;
    private ViewPager2 pager;
    private LinearLayout dots;
    private final Runnable advance = new Runnable() {
        @Override
        public void run() {
            if (pager != null && pager.getAdapter() != null && pager.getAdapter().getItemCount() > 1) {
                pager.setCurrentItem((pager.getCurrentItem() + 1) % pager.getAdapter().getItemCount(), true);
                handler.postDelayed(this, CAROUSEL_MS);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        Ui.Screen screen = new Ui.Screen(c, 110);
        LinearLayout header = Ui.row(c);
        header.addView(Ui.text(c, getString(R.string.ent_title), 26, 700, Ui.INK), Ui.weight(1f));
        streakText = stat(c, header, R.drawable.ic_ds_fire, Ui.AMBER, Ui.ORANGE);
        gemText = stat(c, header, R.drawable.ic_ent_gem, Ui.BLUE, Ui.BLUE);
        screen.top.addView(header, Ui.matchWidth());
        screen.top.setPadding(Ui.dp(c, 20), Ui.dp(c, 24), Ui.dp(c, 20), Ui.dp(c, 4));
        content = screen.body;
        content.setPadding(Ui.dp(c, 20), Ui.dp(c, 8), Ui.dp(c, 20), Ui.dp(c, 110));
        return screen.root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        VocabViewModel user = new ViewModelProvider(this).get(VocabViewModel.class);
        user.user().observe(getViewLifecycleOwner(), u -> {
            streakText.setText(String.valueOf(u == null ? 0 : u.streakCount));
            gemText.setText(String.valueOf(u == null ? 0 : u.gemCount));
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
        handler.postDelayed(advance, CAROUSEL_MS);
    }

    @Override
    public void onPause() {
        handler.removeCallbacks(advance);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(advance);
        pager = null;
        dots = null;
        content = null;
        super.onDestroyView();
    }

    private void reload() {
        load(() -> vm().repo().loadHome(), home -> {
            if (home != null && content != null) {
                render(home);
            }
        });
    }

    private void render(Home home) {
        Context c = requireContext();
        long now = System.currentTimeMillis();
        content.removeAllViews();
        pager = null;
        dots = null;

        if (!home.banners.isEmpty()) {
            buildCarousel(c, home.banners);
        }
        if (home.event != null) {
            buildEvent(c, home.event, now);
        }
        if (!home.contests.isEmpty()) {
            Ui.add(content, sectionTitle(c, getString(R.string.ent_contests), null), 22);
            buildContests(c, home.contests, now);
        }
        List<ActivityCard> games = playable(home.games);
        if (!games.isEmpty()) {
            boolean more = games.size() > GAMES_ON_PAGE;
            Ui.add(content, sectionTitle(c, getString(R.string.ent_game_list), more ? getString(R.string.ent_see_all) : null), 22);
            buildGames(c, games.subList(0, Math.min(games.size(), GAMES_ON_PAGE)));
        }
        List<ActivityCard> practice = playable(home.practice);
        if (!practice.isEmpty()) {
            Ui.add(content, sectionTitle(c, getString(R.string.ent_practice_modes), null), 22);
            for (ActivityCard card : practice) {
                Ui.add(content, practiceRow(c, card), 10);
            }
        }
        if (home.banners.isEmpty() && home.event == null && home.contests.isEmpty() && games.isEmpty()
                && practice.isEmpty()) {
            Ui.add(content, GameUi.centered(c, getString(R.string.ent_empty), 14, 600, Ui.MUTED), 40);
        }
    }

    /** Rows whose code this build has no screen for are left out instead of crashing. */
    private static List<ActivityCard> playable(List<ActivityCard> cards) {
        List<ActivityCard> out = new ArrayList<>();
        for (ActivityCard card : cards) {
            if (Routes.destination(card.activity.code) != 0) {
                out.add(card);
            }
        }
        return out;
    }

    // ----- Header ----------------------------------------------------------

    private TextView stat(Context c, LinearLayout header, @DrawableRes int icon, int iconTint, int textColor) {
        LinearLayout box = Ui.row(c);
        box.addView(Ui.icon(c, icon, 18, iconTint));
        TextView t = Ui.text(c, "0", 15, 700, textColor);
        LinearLayout.LayoutParams p = Ui.wrap();
        p.leftMargin = Ui.dp(c, 4);
        box.addView(t, p);
        LinearLayout.LayoutParams bp = Ui.wrap();
        bp.leftMargin = Ui.dp(c, 12);
        header.addView(box, bp);
        return t;
    }

    // ----- Carousel --------------------------------------------------------

    private void buildCarousel(Context c, List<BannerEntity> banners) {
        pager = new ViewPager2(c);
        pager.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                FrameLayout cell = new FrameLayout(parent.getContext());
                cell.setLayoutParams(new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                return new RecyclerView.ViewHolder(cell) { };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                FrameLayout cell = (FrameLayout) holder.itemView;
                cell.removeAllViews();
                cell.addView(slideView(cell.getContext(), banners.get(position)), new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }

            @Override
            public int getItemCount() {
                return banners.size();
            }
        });
        content.addView(pager, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(c, 196)));

        dots = Ui.row(c);
        dots.setGravity(Gravity.CENTER);
        for (int i = 0; i < banners.size(); i++) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(Ui.dp(c, 6), Ui.dp(c, 6));
            p.leftMargin = Ui.dp(c, 3);
            p.rightMargin = Ui.dp(c, 3);
            dots.addView(new View(c), p);
        }
        Ui.add(content, dots, 10);
        dots.setVisibility(banners.size() > 1 ? View.VISIBLE : View.GONE);
        paintDots(c, 0);
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                paintDots(c, position);
                handler.removeCallbacks(advance);
                handler.postDelayed(advance, CAROUSEL_MS);
            }
        });
    }

    private void paintDots(Context c, int selected) {
        if (dots == null) {
            return;
        }
        for (int i = 0; i < dots.getChildCount(); i++) {
            View dot = dots.getChildAt(i);
            dot.setBackground(Ui.shape(c, i == selected ? Ui.GREEN : GameUi.DOT_OFF, 3));
            ViewGroup.LayoutParams lp = dot.getLayoutParams();
            lp.width = Ui.dp(c, i == selected ? 20 : 6);
            dot.setLayoutParams(lp);
        }
    }

    private View slideView(Context c, BannerEntity b) {
        GameUi.Accent accent = GameUi.accent(b.accent);
        FrameLayout frame = new FrameLayout(c);
        frame.setBackground(Ui.shape(c, accent.main, 24));
        ImageView art = Ui.icon(c, GameUi.drawable(c, b.image, R.drawable.ic_ent_star), 96, Color.WHITE);
        art.setAlpha(0.22f);
        art.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams ap = new FrameLayout.LayoutParams(Ui.dp(c, 96), Ui.dp(c, 96),
                Gravity.END | Gravity.BOTTOM);
        ap.setMargins(0, 0, Ui.dp(c, 18), Ui.dp(c, 14));
        frame.addView(art, ap);

        LinearLayout card = Ui.column(c);
        card.setPadding(Ui.dp(c, 20), Ui.dp(c, 20), Ui.dp(c, 96), Ui.dp(c, 16));
        TextView tag = Ui.text(c, b.tag, 12, 700, Color.WHITE);
        tag.setAlpha(0.85f);
        tag.setLetterSpacing(0.06f);
        card.addView(tag, Ui.wrap());
        Ui.add(card, Ui.text(c, b.title, 24, 700, Color.WHITE), 6);
        if (b.subtitle != null) {
            TextView sub = Ui.text(c, b.subtitle, 14, 400, Color.WHITE);
            sub.setAlpha(0.9f);
            Ui.add(card, sub, 4);
        }
        boolean actionable = b.buttonLabel != null && !BannerEntity.NONE.equals(b.actionType);
        if (actionable) {
            boolean light = b.accent != null && b.accent.equals("GREEN");
            TextView button = light
                    ? GameUi.button3d(c, b.buttonLabel, Ui.INK, Ui.AMBER, GameUi.AMBER_EDGE, 14, 15)
                    : GameUi.button3d(c, b.buttonLabel, accent.edge, Color.WHITE, GameUi.DOT_OFF, 14, 15);
            button.setPadding(Ui.dp(c, 18), Ui.dp(c, 10), Ui.dp(c, 18), Ui.dp(c, 13));
            button.setOnClickListener(v -> openBanner(b));
            LinearLayout.LayoutParams bp = Ui.wrap();
            bp.topMargin = Ui.dp(c, 14);
            card.addView(button, bp);
        }
        frame.addView(card, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return frame;
    }

    private void openBanner(BannerEntity b) {
        if (BannerEntity.CONTEST.equals(b.actionType) && b.actionTarget != null) {
            go(R.id.contestFragment, Routes.contestArgs(b.actionTarget));
        } else if (BannerEntity.ACTIVITY.equals(b.actionType)) {
            openActivity(b.actionTarget);
        }
    }

    // ----- Event -----------------------------------------------------------

    private void buildEvent(Context c, EventEntity e, long now) {
        GameUi.Accent accent = GameUi.accent(e.accent);
        LinearLayout banner = Ui.row(c);
        banner.setBackground(Ui.shape(c, accent.tint, 20, "RED".equals(e.accent) ? GameUi.RED_BORDER : accent.main, 2));
        banner.setPadding(Ui.dp(c, 16), Ui.dp(c, 14), Ui.dp(c, 16), Ui.dp(c, 14));
        FrameLayout badge = new FrameLayout(c);
        badge.setBackground(Ui.shape(c, accent.main, 14));
        badge.addView(Ui.icon(c, GameUi.drawable(c, e.image, R.drawable.ic_ent_trophy), 26, Color.WHITE),
                new FrameLayout.LayoutParams(Ui.dp(c, 26), Ui.dp(c, 26), Gravity.CENTER));
        banner.addView(badge, new LinearLayout.LayoutParams(Ui.dp(c, 48), Ui.dp(c, 48)));
        LinearLayout texts = Ui.column(c);
        texts.addView(Ui.text(c, getString(R.string.ent_event_ends, e.tag,
                ContestAction.remaining(c, e.endsAt - now).toUpperCase()), 12, 700, accent.ink));
        Ui.add(texts, Ui.text(c, e.title, 16, 700, Ui.INK), 2);
        if (e.description != null) {
            Ui.add(texts, Ui.text(c, e.description, 13, 400, Ui.MUTED), 2);
        }
        LinearLayout.LayoutParams tp = Ui.weight(1f);
        tp.leftMargin = Ui.dp(c, 12);
        banner.addView(texts, tp);
        Ui.add(content, banner, 16);
    }

    // ----- Contests --------------------------------------------------------

    private void buildContests(Context c, List<ContestCard> contests, long now) {
        List<ContestCard> shown = contests.subList(0, Math.min(contests.size(), 4));
        for (int i = 0; i < shown.size(); i += 2) {
            LinearLayout row = Ui.row(c);
            row.setBaselineAligned(false);
            row.setGravity(Gravity.TOP);
            row.addView(contestCard(c, shown.get(i), now), cell(c, true));
            if (i + 1 < shown.size()) {
                row.addView(contestCard(c, shown.get(i + 1), now), cell(c, false));
            } else {
                row.addView(new View(c), cell(c, false));
            }
            if (i == 0) {
                content.addView(row, Ui.matchWidth());
            } else {
                Ui.add(content, row, 12);
            }
        }
    }

    private LinearLayout.LayoutParams cell(Context c, boolean first) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        if (first) {
            p.rightMargin = Ui.dp(c, 6);
        } else {
            p.leftMargin = Ui.dp(c, 6);
        }
        return p;
    }

    private View contestCard(Context c, ContestCard card, long now) {
        GameUi.Accent accent = GameUi.accent(card.contest.accent);
        LinearLayout box = Ui.card(c, 14, 20);
        box.setBackground(Ui.shape(c, Ui.CARD, 20, Ui.LINE, 2));
        box.setClickable(true);
        box.setOnClickListener(v -> go(R.id.contestFragment, Routes.contestArgs(card.contest.id)));

        LinearLayout top = Ui.row(c);
        String status;
        int chipBg;
        int chipInk;
        String meta;
        switch (card.state) {
            case LIVE:
                status = c.getString(R.string.ent_st_live);
                chipBg = GameUi.AMBER_SOFT;
                chipInk = GameUi.AMBER_EDGE;
                meta = card.participants > 0
                        ? c.getString(R.string.ent_joined_left, card.participants,
                                ContestAction.remaining(c, card.contest.endsAt - now))
                        : c.getString(R.string.ent_time_left, ContestAction.remaining(c, card.contest.endsAt - now));
                break;
            case REGISTRATION:
                status = c.getString(R.string.ent_st_registering);
                chipBg = Ui.GREEN_TINT;
                chipInk = Ui.GREEN_INK;
                meta = card.participants > 0
                        ? c.getString(R.string.ent_joined_starts, card.participants,
                                ContestAction.remaining(c, card.contest.startsAt - now))
                        : c.getString(R.string.ent_starts_in, ContestAction.remaining(c, card.contest.startsAt - now));
                break;
            default:
                status = c.getString(R.string.ent_st_soon);
                chipBg = 0xFFD9E5F7;
                chipInk = GameUi.BLUE_EDGE;
                meta = c.getString(R.string.ent_reg_opens_in, ContestAction.remaining(c, card.contest.registrationOpensAt - now));
                break;
        }
        TextView chip = Ui.text(c, status, 11, 700, chipInk);
        chip.setBackground(Ui.shape(c, chipBg, 8));
        chip.setPadding(Ui.dp(c, 8), Ui.dp(c, 3), Ui.dp(c, 8), Ui.dp(c, 3));
        top.addView(chip, Ui.wrap());
        top.addView(new View(c), new LinearLayout.LayoutParams(0, 1, 1f));
        top.addView(Ui.icon(c, GameUi.drawable(c, card.contest.image, R.drawable.ic_ent_trophy), 22, accent.main));
        box.addView(top, Ui.matchWidth());
        Ui.add(box, Ui.text(c, card.contest.title, 15, 700, Ui.INK), 10);
        Ui.add(box, Ui.text(c, meta, 12, 400, Ui.MUTED), 6);
        if (card.contest.startsAt > now) {
            ZoneId zone = vm().timeZone().zone();
            Ui.add(box, Ui.text(c, c.getString(R.string.ent_starts_on,
                    ContestTimes.format(card.contest.startsAt, zone, ContestTimes.WEEKDAY_TIME),
                    ContestTimes.zone(card.contest.startsAt, zone)), 12, 400, Ui.MUTED), 6);
        }
        if (card.topPrizeGems > 0) {
            Ui.add(box, Ui.text(c, c.getString(R.string.ent_first_prize, card.topPrizeGems), 12, 400, Ui.MUTED), 6);
        }
        box.addView(new View(c), new LinearLayout.LayoutParams(1, 0, 1f));

        ContestAction action = ContestAction.of(c, card, now);
        TextView button;
        if (action.kind == ContestAction.Kind.NONE) {
            button = Ui.text(c, action.label, 14, 700, Ui.MUTED);
            button.setGravity(Gravity.CENTER);
            button.setBackground(Ui.shape(c, Ui.CARD, 12, GameUi.DOT_OFF, 2));
            button.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 8));
        } else {
            button = GameUi.button3d(c, action.label, Ui.INK, Ui.AMBER, GameUi.AMBER_EDGE, 12, 14);
            button.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 11));
            button.setOnClickListener(v -> runContestAction(card, action));
        }
        LinearLayout.LayoutParams bp = Ui.matchWidth();
        bp.topMargin = Ui.dp(c, 14);
        box.addView(button, bp);
        return box;
    }

    private void runContestAction(ContestCard card, ContestAction action) {
        if (action.kind == ContestAction.Kind.PLAY) {
            go(R.id.contestPlayFragment, Routes.contestArgs(card.contest.id));
        } else if (action.kind == ContestAction.Kind.REGISTER) {
            load(() -> vm().contests().register(card.contest.id), ok -> {
                Toast.makeText(requireContext(), Boolean.TRUE.equals(ok)
                        ? getString(R.string.ent_registered_for, card.contest.title) : getString(R.string.ent_register_failed), Toast.LENGTH_SHORT).show();
                reload();
            });
        }
    }

    // ----- Games -----------------------------------------------------------

    private void buildGames(Context c, List<ActivityCard> games) {
        for (int i = 0; i < games.size(); i += 2) {
            LinearLayout row = Ui.row(c);
            row.setBaselineAligned(false);
            row.setGravity(Gravity.TOP);
            row.addView(gameCard(c, games.get(i)), cell(c, true));
            if (i + 1 < games.size()) {
                row.addView(gameCard(c, games.get(i + 1)), cell(c, false));
            } else {
                row.addView(new View(c), cell(c, false));
            }
            if (i == 0) {
                content.addView(row, Ui.matchWidth());
            } else {
                Ui.add(content, row, 12);
            }
        }
    }

    private View gameCard(Context c, ActivityCard item) {
        GameUi.Accent accent = GameUi.accent(item.activity.accent);
        return GameCards.gameCard(c, item, accent, () -> openActivity(item.activity.code));
    }

    // ----- Practice --------------------------------------------------------

    private View practiceRow(Context c, ActivityCard item) {
        GameUi.Accent accent = GameUi.accent(item.activity.accent);
        LinearLayout row = Ui.row(c);
        row.setBackground(Ui.shape(c, Ui.CARD, 20, Ui.LINE, 2));
        row.setPadding(Ui.dp(c, 12), Ui.dp(c, 12), Ui.dp(c, 12), Ui.dp(c, 12));
        row.setClickable(true);
        row.setOnClickListener(v -> openActivity(item.activity.code));
        FrameLayout badge = new FrameLayout(c);
        badge.setBackground(Ui.shape(c, accent.tint, 14));
        badge.addView(Ui.icon(c, GameUi.drawable(c, item.activity.image, R.drawable.ic_ent_star), 24, accent.ink),
                new FrameLayout.LayoutParams(Ui.dp(c, 24), Ui.dp(c, 24), Gravity.CENTER));
        row.addView(badge, new LinearLayout.LayoutParams(Ui.dp(c, 48), Ui.dp(c, 48)));
        LinearLayout texts = Ui.column(c);
        texts.addView(Ui.text(c, item.activity.title, 15, 700, Ui.INK));
        String sub = item.activity.subtitle;
        if (item.count > 0) {
            sub = (sub == null ? "" : sub + " · ") + getString(R.string.ent_words_n, item.count);
        }
        if (sub != null) {
            Ui.add(texts, Ui.text(c, sub, 12, 400, Ui.MUTED), 2);
        }
        LinearLayout.LayoutParams tp = Ui.weight(1f);
        tp.leftMargin = Ui.dp(c, 12);
        row.addView(texts, tp);
        row.addView(Ui.icon(c, R.drawable.ic_ds_chevron, 18, Ui.MUTED));
        return row;
    }

    // ----- Shared ----------------------------------------------------------

    private View sectionTitle(Context c, String title, @Nullable String link) {
        LinearLayout row = Ui.row(c);
        row.addView(Ui.text(c, title, 19, 700, Ui.INK), Ui.weight(1f));
        if (link != null) {
            TextView t = Ui.text(c, link, 14, 600, Ui.BLUE);
            t.setClickable(true);
            t.setOnClickListener(v -> go(R.id.allGamesFragment, null));
            row.addView(t, Ui.wrap());
        }
        return row;
    }

    /** Opens the screen of an activity code and records the play. */
    private void openActivity(@Nullable String code) {
        int destination = Routes.destination(code);
        if (destination == 0) {
            return;
        }
        vm().async(() -> vm().repo().recordPlay(code));
        go(destination, null);
    }
}
