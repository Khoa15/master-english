package com.doradodo.app.ui.entertainment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.doradodo.app.R;
import com.doradodo.app.data.local.entity.GameWord;
import com.doradodo.app.data.repository.EntertainmentRepository.WordSource;
import java.util.ArrayList;
import java.util.List;
import com.doradodo.app.ui.ds.Ui;

/** "Sleeping": words are read slowly aloud while the learner falls asleep, with a sleep timer. */
@dagger.hilt.android.AndroidEntryPoint
public class SleepingFragment extends GameFragment {

    /** The tracks are the learner's own words and every pack; read from the database. */
    private List<WordSource> tracks = new ArrayList<>();
    private List<String[]> words = new ArrayList<>();

    private static final int[] TIMERS = {15, 30, 60};
    private static final long GAP_MS = 4000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable speak = new Runnable() {
        @Override
        public void run() {
            if (!playing || speaker == null) {
                return;
            }
            if (SystemClock.elapsedRealtime() >= stopAt) {
                playing = false;
                render();
                return;
            }
            if (words.isEmpty()) {
                return;
            }
            String[] w = words.get(wordIndex % words.size());
            wordIndex++;
            speaker.say(w[0], 0.7f);
            handler.postDelayed(this, GAP_MS);
        }
    };

    private Speaker speaker;
    private LinearLayout body;
    private GameUi.Page page;
    private boolean playing;
    private int track;
    private int timer = 30;
    private int wordIndex;
    private long stopAt;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        speaker = new Speaker(c);
        load(() -> vm().repo().learningLanguage(), lang -> {
            if (speaker != null) {
                speaker.setLanguage(lang);
            }
        });
        page = GameUi.page(c, true, onBack(), c.getString(R.string.ent_sleeping_title), "", 0, 0, 0);
        body = page.screen.body;
        body.setPadding(Ui.dp(c, 20), Ui.dp(c, 18), Ui.dp(c, 20), Ui.dp(c, 16));
        body.addView(loadingState(c));
        load(() -> vm().repo().wordSources(), list -> {
            tracks = list == null ? new ArrayList<>() : list;
            render();
        });
        return page.screen.root;
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(speak);
        if (speaker != null) {
            speaker.release();
            speaker = null;
        }
        super.onDestroyView();
    }

    /** Reads the words of the chosen track, then starts the loop. */
    private void play() {
        WordSource src = tracks.get(track);
        load(() -> {
            List<String[]> out = new ArrayList<>();
            for (GameWord w : vm().repo().sourceWords(src.id, 300)) {
                out.add(new String[]{w.term});
            }
            return out;
        }, list -> {
            words = list == null ? new ArrayList<>() : list;
            wordIndex = 0;
            start();
            render();
        });
    }

    private void start() {
        stopAt = SystemClock.elapsedRealtime() + timer * 60_000L;
        playing = true;
        handler.removeCallbacks(speak);
        handler.post(speak);
    }

    private void stop() {
        playing = false;
        handler.removeCallbacks(speak);
        speaker.stop();
    }

    private void render() {
        Context c = requireContext();
        body.removeAllViews();
        if (tracks.isEmpty()) {
            body.addView(emptyState(c, getString(R.string.ent_no_wordset), getString(R.string.ent_no_wordset_listen)), Ui.matchWidth());
            return;
        }
        String[] cur = {GameUi.sourceTitle(c, tracks.get(track)), getString(R.string.ent_words_n, tracks.get(track).count)};
        page.subtitle.setText(getString(R.string.ent_sleep_subtitle, timer));

        // Now playing
        LinearLayout hero = Ui.column(c);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setBackground(Ui.shape(c, GameUi.PURPLE_TINT, 24, GameUi.PURPLE_BORDER, 2));
        hero.setPadding(Ui.dp(c, 18), Ui.dp(c, 26), Ui.dp(c, 18), Ui.dp(c, 26));
        hero.addView(Ui.icon(c, R.drawable.ic_ent_moon, 64, GameUi.PURPLE),
                new LinearLayout.LayoutParams(Ui.dp(c, 64), Ui.dp(c, 64)));
        Ui.add(hero, GameUi.centered(c, cur[0], 18, 700, Ui.INK), 12);
        Ui.add(hero, GameUi.centered(c, (playing ? getString(R.string.ent_playing) : getString(R.string.ent_paused)) + " · " + cur[1], 13, 600, Ui.MUTED), 12);
        FrameLayout play = new FrameLayout(c);
        android.graphics.drawable.GradientDrawable under = Ui.shape(c, GameUi.PURPLE_EDGE, 36);
        android.graphics.drawable.GradientDrawable top = Ui.shape(c, GameUi.PURPLE, 36);
        android.graphics.drawable.LayerDrawable layers =
                new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{under, top});
        layers.setLayerInset(1, 0, 0, 0, Ui.dp(c, 5));
        play.setBackground(layers);
        play.setClickable(true);
        play.setContentDescription(playing ? getString(R.string.ent_pause) : getString(R.string.ent_play_audio));
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(Ui.dp(c, 26), Ui.dp(c, 26), Gravity.CENTER);
        ip.bottomMargin = Ui.dp(c, 5);
        play.addView(Ui.icon(c, playing ? R.drawable.ic_ent_pause : R.drawable.ic_ent_play, 26,
                android.graphics.Color.WHITE), ip);
        play.setOnClickListener(v -> {
            if (playing) {
                stop();
                render();
            } else {
                play();
            }
        });
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(Ui.dp(c, 72), Ui.dp(c, 72));
        pp.topMargin = Ui.dp(c, 18);
        hero.addView(play, pp);
        body.addView(hero, Ui.matchWidth());

        // Timer
        Ui.add(body, GameUi.sectionTitle(c, getString(R.string.ent_sleep_timer)), 22);
        LinearLayout timers = Ui.row(c);
        for (int i = 0; i < TIMERS.length; i++) {
            final int minutes = TIMERS[i];
            boolean on = minutes == timer;
            TextView t = Ui.text(c, getString(R.string.ent_unit_min, minutes), 14, 700, on ? GameUi.PURPLE_EDGE : Ui.INK);
            t.setGravity(Gravity.CENTER);
            t.setBackground(Ui.shape(c, on ? GameUi.PURPLE_TINT : Ui.CARD, 14, on ? GameUi.PURPLE : Ui.LINE, 2));
            t.setClickable(true);
            t.setOnClickListener(v -> {
                timer = minutes;
                if (playing) {
                    stopAt = SystemClock.elapsedRealtime() + timer * 60_000L;
                }
                render();
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, Ui.dp(c, 44), 1f);
            if (i > 0) {
                p.leftMargin = Ui.dp(c, 8);
            }
            timers.addView(t, p);
        }
        Ui.add(body, timers, 10);

        // Track list
        Ui.add(body, GameUi.sectionTitle(c, getString(R.string.ent_tracklist)), 22);
        for (int k = 0; k < tracks.size(); k++) {
            final int index = k;
            boolean on = k == track;
            LinearLayout row = Ui.row(c);
            row.setBackground(Ui.shape(c, on ? GameUi.PURPLE_TINT : Ui.CARD, 16, on ? GameUi.PURPLE : Ui.LINE, 2));
            row.setPadding(Ui.dp(c, 12), Ui.dp(c, 12), Ui.dp(c, 12), Ui.dp(c, 12));
            row.setClickable(true);
            row.addView(Ui.text(c, String.valueOf(k + 1), 15, 700, GameUi.PURPLE),
                    new LinearLayout.LayoutParams(Ui.dp(c, 30), ViewGroup.LayoutParams.WRAP_CONTENT));
            row.addView(Ui.text(c, GameUi.sourceTitle(c, tracks.get(k)), 15, 700, Ui.INK), Ui.weight(1f));
            row.addView(Ui.text(c, getString(R.string.ent_words_n, tracks.get(k).count), 12, 600, Ui.MUTED), Ui.wrap());
            row.setOnClickListener(v -> {
                track = index;
                play();
            });
            Ui.add(body, row, 10);
        }
    }
}
