package com.doradodo.app.ui.entertainment;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.doradodo.app.R;
import com.doradodo.app.data.repository.WordSource;
import com.doradodo.app.data.local.entity.GameWord;
import com.doradodo.app.ui.ds.Ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * "Tụng siêu cấp": a word is read aloud again and again, each time appearing at a new spot on the board;
 * after the chosen number of repetitions the board clears and the next word starts.
 */
@dagger.hilt.android.AndroidEntryPoint
public class TungFragment extends GameFragment {

    private static final long BEAT_MS = 700L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final List<Rect> spots = new ArrayList<>();
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            tick();
            handler.postDelayed(this, BEAT_MS);
        }
    };

    private Speaker speaker;
    private GameUi.Page page;
    private FrameLayout stage;
    private LinearLayout chips;
    private ImageButtonHolder playHolder;
    private int reps = 10;
    private int perRound = 5;
    private List<WordSource> sources = new ArrayList<>();
    private String sourceId = WordSource.LEARNED;
    private List<String[]> round = new ArrayList<>();
    private int wi;
    private int rep;
    private boolean playing = true;
    private boolean finished;

    /** The play/pause button: a round amber button whose icon changes. */
    private static final class ImageButtonHolder {
        FrameLayout root;
        android.widget.ImageView icon;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        android.content.SharedPreferences prefs =
                c.getSharedPreferences(TungSetupFragment.PREFS, Context.MODE_PRIVATE);
        sourceId = prefs.getString("source", WordSource.LEARNED);
        reps = prefs.getInt("reps", 10);
        int words = prefs.getInt("words", 5);
        perRound = words <= 0 ? 999 : words;
        speaker = new Speaker(c);
        load(() -> vm().repo().learningLanguage(), lang -> {
            if (speaker != null) {
                speaker.setLanguage(lang);
            }
        });

        page = GameUi.page(c, true, onBack(), c.getString(R.string.ent_tung_title), "", 0, 0, 0);
        page.screen.scroll.setFillViewport(true);
        page.screen.body.setPadding(Ui.dp(c, 20), Ui.dp(c, 6), Ui.dp(c, 20), 0);

        HorizontalScrollView scroller = new HorizontalScrollView(c);
        scroller.setHorizontalScrollBarEnabled(false);
        chips = Ui.row(c);
        scroller.addView(chips);
        page.screen.body.addView(scroller, Ui.matchWidth());

        stage = new FrameLayout(c);
        stage.setBackground(Ui.shape(c, Ui.CARD, 24, Ui.LINE, 2));
        stage.setClipChildren(true);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        sp.topMargin = Ui.dp(c, 14);
        page.screen.body.addView(stage, sp);

        LinearLayout controls = Ui.row(c);
        controls.setGravity(Gravity.CENTER);
        FrameLayout restartBtn = new FrameLayout(c);
        restartBtn.setBackground(Ui.shape(c, Ui.CARD, 26, Ui.LINE, 2));
        restartBtn.setClickable(true);
        restartBtn.setContentDescription(getString(R.string.ent_new_round));
        restartBtn.addView(Ui.icon(c, R.drawable.ic_ent_restart, 22, Ui.INK),
                new FrameLayout.LayoutParams(Ui.dp(c, 22), Ui.dp(c, 22), Gravity.CENTER));
        restartBtn.setOnClickListener(v -> startRound(sourceId));
        controls.addView(restartBtn, new LinearLayout.LayoutParams(Ui.dp(c, 52), Ui.dp(c, 52)));

        playHolder = new ImageButtonHolder();
        playHolder.root = new FrameLayout(c);
        android.graphics.drawable.GradientDrawable under = Ui.shape(c, GameUi.AMBER_EDGE, 38);
        android.graphics.drawable.GradientDrawable top = Ui.shape(c, Ui.AMBER, 38);
        android.graphics.drawable.LayerDrawable layers =
                new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{under, top});
        layers.setLayerInset(1, 0, 0, 0, Ui.dp(c, 5));
        playHolder.root.setBackground(layers);
        playHolder.root.setClickable(true);
        playHolder.icon = Ui.icon(c, R.drawable.ic_ent_pause, 28, Ui.INK);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(Ui.dp(c, 28), Ui.dp(c, 28), Gravity.CENTER);
        ip.bottomMargin = Ui.dp(c, 5);
        playHolder.root.addView(playHolder.icon, ip);
        playHolder.root.setOnClickListener(v -> toggle());
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(Ui.dp(c, 76), Ui.dp(c, 76));
        pp.leftMargin = Ui.dp(c, 22);
        controls.addView(playHolder.root, pp);
        page.screen.setFooter(controls);

        load(() -> vm().words().wordSources(), list -> {
            sources = list == null ? new ArrayList<>() : list;
            boolean known = false;
            for (WordSource w : sources) {
                known |= w.id.equals(sourceId);
            }
            if (!known && !sources.isEmpty()) {
                sourceId = sources.get(0).id;
            }
            renderChips();
            stage.post(() -> startRound(sourceId));
        });
        return page.screen.root;
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.postDelayed(tick, BEAT_MS);
    }

    @Override
    public void onPause() {
        handler.removeCallbacks(tick);
        if (speaker != null) {
            speaker.stop();
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(tick);
        if (speaker != null) {
            speaker.release();
            speaker = null;
        }
        super.onDestroyView();
    }

    private void renderChips() {
        Context c = requireContext();
        chips.removeAllViews();
        for (int i = 0; i < sources.size(); i++) {
            final WordSource src = sources.get(i);
            boolean on = src.id.equals(sourceId);
            TextView t = Ui.text(c, GameUi.sourceTitle(c, src), 13, 700, on ? android.graphics.Color.WHITE : Ui.INK);
            t.setBackground(Ui.shape(c, on ? Ui.BLUE : Ui.CARD, 999, on ? Ui.BLUE : Ui.LINE, 2));
            t.setPadding(Ui.dp(c, 14), Ui.dp(c, 8), Ui.dp(c, 14), Ui.dp(c, 8));
            t.setClickable(true);
            t.setOnClickListener(v -> startRound(src.id));
            LinearLayout.LayoutParams p = Ui.wrap();
            if (i > 0) {
                p.leftMargin = Ui.dp(c, 8);
            }
            chips.addView(t, p);
        }
    }

    private void startRound(String id) {
        sourceId = id;
        playing = false;
        finished = false;
        round = new ArrayList<>();
        clearStage();
        renderChips();
        load(() -> {
            List<GameWord> all = new ArrayList<>(vm().words().sourceWords(id, 400));
            Collections.shuffle(all, random);
            List<String[]> out = new ArrayList<>();
            for (GameWord w : all.subList(0, Math.min(perRound, all.size()))) {
                out.add(new String[]{w.term, w.meaning == null ? "" : w.meaning});
            }
            return out;
        }, words -> {
            round = words == null ? new ArrayList<>() : words;
            wi = 0;
            rep = 0;
            finished = false;
            playing = !round.isEmpty();
            updateStatus();
            if (speaker != null && !round.isEmpty()) {
                speaker.say(round.get(0)[0], 1.2f);
            }
            if (round.isEmpty()) {
                showMessage(getString(R.string.ent_pack_empty));
            }
        });
    }

    private void showMessage(String text) {
        clearStage();
        TextView t = GameUi.centered(requireContext(), text, 16, 600, Ui.MUTED);
        stage.addView(t, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
    }

    private void clearStage() {
        stage.removeAllViews();
        spots.clear();
    }

    private void toggle() {
        if (finished) {
            startRound(sourceId);
            return;
        }
        playing = !playing;
        if (!playing && speaker != null) {
            speaker.stop();
        }
        updateStatus();
    }

    private void tick() {
        if (!playing || finished || round.isEmpty() || stage.getWidth() == 0) {
            return;
        }
        if (rep >= reps) {
            int next = wi + 1;
            if (next >= round.size()) {
                finished = true;
                playing = false;
                updateStatus();
                return;
            }
            wi = next;
            rep = 0;
            clearStage();
            speaker.say(round.get(wi)[0], 1.2f);
            updateStatus();
            return;
        }
        String[] w = round.get(wi);
        speaker.say(w[0], 1.2f);
        addWord(w);
        rep++;
        updateStatus();
    }

    /** Puts the word at a free spot of the board; falls back to the least overlapping one when it is crowded. */
    private void addWord(String[] w) {
        Context c = requireContext();
        LinearLayout tile = Ui.column(c);
        tile.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView en = Ui.text(c, w[0], 26, 700, Ui.BLUE);
        en.setSingleLine(true);
        TextView vi = Ui.text(c, w[1], 13, 600, Ui.MUTED);
        vi.setSingleLine(true);
        vi.setMaxWidth(Ui.dp(c, 150));
        vi.setEllipsize(android.text.TextUtils.TruncateAt.END);
        tile.addView(en, Ui.wrap());
        Ui.add(tile, vi, 4);
        tile.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int bw = tile.getMeasuredWidth();
        int bh = tile.getMeasuredHeight();
        int pad = Ui.dp(c, 6);
        int maxX = Math.max(1, stage.getWidth() - bw - 2 * pad);
        int maxY = Math.max(1, stage.getHeight() - bh - 2 * pad);
        Rect best = null;
        long bestOverlap = Long.MAX_VALUE;
        for (int t = 0; t < 300; t++) {
            int x = pad + random.nextInt(maxX);
            int y = pad + random.nextInt(maxY);
            Rect cand = new Rect(x, y, x + bw, y + bh);
            long overlap = 0;
            for (Rect r : spots) {
                if (Rect.intersects(cand, r)) {
                    Rect o = new Rect(cand);
                    o.intersect(r);
                    overlap += (long) o.width() * o.height();
                }
            }
            if (overlap == 0) {
                best = cand;
                break;
            }
            if (overlap < bestOverlap) {
                bestOverlap = overlap;
                best = cand;
            }
        }
        spots.add(best);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = best.left;
        lp.topMargin = best.top;
        stage.addView(tile, lp);
    }

    private void updateStatus() {
        if (page == null) {
            return;
        }
        String state = playing ? getString(R.string.ent_chanting) : (finished ? getString(R.string.ent_round_complete) : getString(R.string.ent_paused));
        String name = "";
        for (WordSource w : sources) {
            if (w.id.equals(sourceId)) {
                name = GameUi.sourceTitle(requireContext(), w);
            }
        }
        page.subtitle.setText(state + " · " + name);
        playHolder.icon.setImageResource(playing ? R.drawable.ic_ent_pause : R.drawable.ic_ent_play);
        playHolder.root.setContentDescription(playing ? getString(R.string.ent_pause) : getString(R.string.ent_resume));
        if (finished && stage.getChildCount() == 0 || finished) {
            showDone();
        }
    }

    private void showDone() {
        if (stage.findViewWithTag("done") != null) {
            return;
        }
        Context c = requireContext();
        clearStage();
        TextView t = GameUi.centered(c, getString(R.string.ent_chant_done, round.size(), reps), 20, 700, Ui.GREEN);
        t.setTag("done");
        stage.addView(t, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
    }
}
