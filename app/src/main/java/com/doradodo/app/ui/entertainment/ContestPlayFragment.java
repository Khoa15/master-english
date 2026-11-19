package com.doradodo.app.ui.entertainment;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import com.doradodo.app.data.local.entity.GameWord;
import com.doradodo.app.data.repository.ContestState;
import com.doradodo.app.data.repository.ContestDetail;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.doradodo.app.R;
import com.doradodo.app.ui.ds.Ui;

/** The contest run: 5 rounds (listen, speak, read, write, choose) of 5 words each. */
@dagger.hilt.android.AndroidEntryPoint
public class ContestPlayFragment extends GameFragment {

    private static final class Word {
        final String en;
        final String vi;
        final String read;
        final String blank;
        final String[] choices;
        final int ans;
        final String[] listen;

        Word(String en, String vi, String read, String blank, String[] choices, int ans, String[] listen) {
            this.en = en;
            this.vi = vi;
            this.read = read;
            this.blank = blank;
            this.choices = choices;
            this.ans = ans;
            this.listen = listen;
        }
    }

    private Word[] words = new Word[0];

    /** The rounds in play order; a new round is one more constant here. */
    private enum Round {
        LISTEN(R.string.ent_r_listen, ContestPlayFragment::renderListen),
        SPEAK(R.string.ent_r_speak, ContestPlayFragment::renderSpeak),
        READ(R.string.ent_r_read, ContestPlayFragment::renderRead),
        WRITE(R.string.ent_r_write, ContestPlayFragment::renderWrite),
        CHOICE(R.string.ent_r_choice, ContestPlayFragment::renderChoice);

        interface Renderer {
            void render(ContestPlayFragment fragment, Context c, Word w);
        }

        final int label;
        final Renderer renderer;

        Round(int label, Renderer renderer) {
            this.label = label;
            this.renderer = renderer;
        }
    }

    private static final Round[] ROUNDS = Round.values();
    private static final int MAX_READS = 3;

    private GameUi.Page page;
    private LinearLayout body;
    private TextView nextButton;
    private Speaker speaker;
    private int ri;
    private int wi;
    private boolean answered;
    private int reads;
    private int picked = -1;
    private boolean spoke;
    private boolean finished;

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
        page = GameUi.page(c, true, onBack(), c.getString(R.string.ent_contest_title), "", Ui.AMBER_TINT, Ui.AMBER_INK, Ui.AMBER);
        body = page.screen.body;
        nextButton = GameUi.footerButton(c, "", Ui.INK, Ui.AMBER, GameUi.AMBER_EDGE);
        nextButton.setOnClickListener(v -> advance());
        page.badge.setVisibility(View.GONE);
        body.addView(loadingState(c));
        String contestId = requireArguments().getString(Routes.ARG_CONTEST, "");
        load(() -> prepare(contestId), this::start);
        return page.screen.root;
    }

    /** What the contest run needs: the contest (to check the learner may enter) and its questions. */
    private static final class Prepared {
        ContestDetail detail;
        Word[] words;
    }

    private Prepared prepare(String contestId) {
        Prepared p = new Prepared();
        p.detail = vm().contests().loadContest(contestId);
        if (p.detail == null || !p.detail.card.registered
                || p.detail.card.state != ContestState.LIVE) {
            p.words = new Word[0];
            return p;
        }
        List<GameWord> list = vm().contests().contestWords(contestId, 5);
        List<GameWord> others = vm().words().distractors(idsOf(list), 6);
        p.words = new Word[list.size()];
        for (int i = 0; i < list.size(); i++) {
            p.words[i] = buildWord(list.get(i), list, others);
        }
        return p;
    }

    private static Set<String> idsOf(List<GameWord> list) {
        Set<String> ids = new HashSet<>();
        for (GameWord w : list) {
            ids.add(w.id);
        }
        return ids;
    }

    /** Three terms and three meanings, the right one among them at a random place. */
    private static Word buildWord(GameWord w, List<GameWord> pool, List<GameWord> others) {
        List<GameWord> wrong = new ArrayList<>();
        for (GameWord o : pool) {
            if (!o.id.equals(w.id)) {
                wrong.add(o);
            }
        }
        wrong.addAll(others);
        Collections.shuffle(wrong);
        List<GameWord> three = new ArrayList<>();
        three.add(w);
        for (int i = 0; i < wrong.size() && three.size() < 3; i++) {
            three.add(wrong.get(i));
        }
        Collections.shuffle(three);
        String[] terms = new String[three.size()];
        String[] meanings = new String[three.size()];
        int ans = 0;
        for (int i = 0; i < three.size(); i++) {
            terms[i] = three.get(i).term;
            meanings[i] = three.get(i).meaning == null ? three.get(i).term : three.get(i).meaning;
            if (three.get(i).id.equals(w.id)) {
                ans = i;
            }
        }
        String sentence = w.example != null && !w.example.isEmpty() ? w.example
                : (w.definition == null ? w.term : w.definition);
        String blank = w.example != null && w.example.toLowerCase().contains(w.term.toLowerCase())
                ? w.example.replaceAll("(?i)" + java.util.regex.Pattern.quote(w.term), "___")
                : (w.meaning == null ? "" : w.meaning) + " = ___";
        return new Word(w.term, w.meaning == null ? "" : w.meaning, sentence, blank, terms, ans, meanings);
    }

    private void start(@Nullable Prepared p) {
        Context c = requireContext();
        body.removeAllViews();
        if (p == null || p.detail == null) {
            body.addView(emptyState(c, getString(R.string.ent_cant_enter), getString(R.string.ent_contest_missing)), Ui.matchWidth());
            return;
        }
        page.subtitle.setText(p.detail.card.contest.title);
        if (p.words.length < 3) {
            boolean eligible = p.detail.card.registered
                    && p.detail.card.state == ContestState.LIVE;
            body.addView(emptyState(c, eligible ? getString(R.string.ent_not_enough_words) : getString(R.string.ent_cant_enter_yet),
                    eligible ? getString(R.string.ent_pack_too_small) : getString(R.string.ent_need_register)), Ui.matchWidth());
            return;
        }
        words = p.words;
        page.badge.setVisibility(View.VISIBLE);
        GameUi.setFooter(c, page.screen, nextButton);
        render();
    }

    @Override
    public void onDestroyView() {
        if (speaker != null) {
            speaker.release();
            speaker = null;
        }
        super.onDestroyView();
    }

    private boolean canNext() {
        return finished || (ROUNDS[ri] == Round.READ ? reads >= 1 : answered);
    }

    private void advance() {
        if (finished) {
            back();
            return;
        }
        if (!canNext()) {
            return;
        }
        if (wi + 1 < words.length) {
            wi++;
        } else if (ri + 1 < ROUNDS.length) {
            ri++;
            wi = 0;
        } else {
            finished = true;
            render();
            return;
        }
        answered = false;
        reads = 0;
        picked = -1;
        spoke = false;
        render();
    }

    private void render() {
        Context c = requireContext();
        Word w = words[Math.min(wi, words.length - 1)];
        body.removeAllViews();
        if (finished) {
            LinearLayout hero = GameUi.hero(c, Ui.GREEN_TINT, GameUi.GREEN_BORDER, getString(R.string.ent_completed_caps), Ui.GREEN_INK);
            Ui.add(hero, GameUi.centered(c, getString(R.string.ent_all_rounds_done), 20, 700, Ui.INK), 10);
            body.addView(hero, Ui.matchWidth());
        } else {
            ROUNDS[ri].renderer.render(this, c, w);
        }
        int rounds = ROUNDS.length;
        int total = words.length;
        page.subtitle.setText(getString(R.string.ent_round_label, finished ? rounds : ri + 1, rounds,
                finished ? getString(R.string.ent_finish) : getString(ROUNDS[ri].label)));
        page.badge.setText((finished ? total : wi + 1) + "/" + total);
        int doneUnits = finished ? rounds * total
                : ri * total + wi + (canNext() ? 1 : 0);
        Ui.setProgress(page.progress, doneUnits * 100 / (rounds * total));
        nextButton.setText(finished ? getString(R.string.ent_back_to_contest)
                : (wi + 1 == total && ri + 1 == rounds ? getString(R.string.ent_finish) : getString(R.string.ent_next)));
        nextButton.setTextColor(Ui.INK);
        boolean ready = canNext();
        GameUi.setReady(nextButton, ready);
    }

    private View roundButton(Context c, @DrawableRes int icon, int face, int edge, int sizeDp, int iconDp,
                             View.OnClickListener l, String description) {
        FrameLayout f = new FrameLayout(c);
        android.graphics.drawable.GradientDrawable under = Ui.shape(c, edge, sizeDp / 2f);
        android.graphics.drawable.GradientDrawable top = Ui.shape(c, face, sizeDp / 2f);
        android.graphics.drawable.LayerDrawable layers =
                new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{under, top});
        layers.setLayerInset(1, 0, 0, 0, Ui.dp(c, 5));
        f.setBackground(layers);
        f.setClickable(true);
        f.setFocusable(true);
        f.setContentDescription(description);
        f.setOnClickListener(l);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(Ui.dp(c, iconDp), Ui.dp(c, iconDp), Gravity.CENTER);
        ip.bottomMargin = Ui.dp(c, 5);
        f.addView(Ui.icon(c, icon, iconDp, android.graphics.Color.WHITE), ip);
        f.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(c, sizeDp), Ui.dp(c, sizeDp)));
        return f;
    }

    // ----- Rounds ----------------------------------------------------------

    private void renderListen(Context c, Word w) {
        LinearLayout hero = GameUi.hero(c, GameUi.BLUE_SOFT, GameUi.BLUE_BORDER, "", GameUi.BLUE_EDGE);
        hero.removeViewAt(0);
        hero.setPadding(Ui.dp(c, 18), Ui.dp(c, 22), Ui.dp(c, 18), Ui.dp(c, 22));
        hero.addView(roundButton(c, R.drawable.ic_ent_speaker, Ui.BLUE, GameUi.BLUE_EDGE, 84, 36,
                v -> speaker.say(w.en, 0.9f), getString(R.string.ent_listen_word)));
        Ui.add(hero, GameUi.centered(c, getString(R.string.ent_tap_to_listen), 14, 700, GameUi.BLUE_EDGE), 12);
        body.addView(hero, Ui.matchWidth());
        for (int k = 0; k < w.listen.length; k++) {
            addOption(c, w.listen[k], k, -1, GameUi.BLUE_SOFT, Ui.BLUE, k == 0 ? 16 : 10);
        }
    }

    private void renderSpeak(Context c, Word w) {
        LinearLayout hero = GameUi.hero(c, GameUi.AMBER_SOFT, GameUi.AMBER_BORDER, getString(R.string.ent_say_this_word), 0xFF8A5A0B);
        Ui.add(hero, GameUi.centered(c, w.en, 30, 700, Ui.INK), 8);
        Ui.add(hero, GameUi.centered(c, w.vi, 14, 400, Ui.MUTED), 4);
        body.addView(hero, Ui.matchWidth());
        LinearLayout mic = Ui.column(c);
        mic.setGravity(Gravity.CENTER_HORIZONTAL);
        mic.addView(roundButton(c, R.drawable.ic_ent_mic, spoke ? Ui.GREEN : Ui.RED,
                spoke ? GameUi.GREEN_EDGE : GameUi.RED_EDGE, 96, 40, v -> {
                    if (!answered) {
                        speaker.say(w.en, 0.9f);
                        spoke = true;
                        answered = true;
                        render();
                    }
                }, getString(R.string.ent_record)));
        Ui.add(mic, GameUi.centered(c, spoke ? getString(R.string.ent_recorded) : getString(R.string.ent_tap_mic), 14, 700, Ui.MUTED), 10);
        Ui.add(body, mic, 22);
    }

    private void renderRead(Context c, Word w) {
        LinearLayout hero = GameUi.hero(c, Ui.GREEN_TINT, GameUi.GREEN_BORDER, getString(R.string.ent_read_this), Ui.GREEN_INK);
        TextView s = GameUi.centered(c, w.read, 20, 700, Ui.INK);
        s.setLineSpacing(0, 1.2f);
        Ui.add(hero, s, 10);
        body.addView(hero, Ui.matchWidth());
        LinearLayout box = Ui.column(c);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        boolean max = reads >= MAX_READS;
        TextView b = GameUi.button3d(c, max ? getString(R.string.ent_read_max) : (reads == 0 ? getString(R.string.ent_read_sentence) : getString(R.string.ent_read_again)),
                android.graphics.Color.WHITE, max ? GameUi.GREY : Ui.GREEN, max ? GameUi.GREY_EDGE : GameUi.GREEN_EDGE,
                14, 15);
        b.setPadding(Ui.dp(c, 22), Ui.dp(c, 12), Ui.dp(c, 22), Ui.dp(c, 15));
        b.setOnClickListener(v -> {
            if (reads < MAX_READS) {
                speaker.say(w.read, 0.9f);
                reads++;
                render();
            }
        });
        box.addView(b, Ui.wrap());
        Ui.add(box, GameUi.centered(c, getString(R.string.ent_reads_left, Math.max(0, MAX_READS - reads)), 13, 600, Ui.MUTED), 8);
        Ui.add(body, box, 20);
    }

    private void renderWrite(Context c, Word w) {
        LinearLayout hero = GameUi.hero(c, GameUi.PURPLE_TINT, GameUi.PURPLE_BORDER, getString(R.string.ent_write_word), GameUi.PURPLE_EDGE);
        Ui.add(hero, GameUi.centered(c, w.vi, 22, 700, Ui.INK), 8);
        TextView hint = GameUi.centered(c, getString(R.string.ent_hint_prefix, GameUi.hint(w.en)), 16, 700, Ui.MUTED);
        Ui.add(hero, hint, 10);
        body.addView(hero, Ui.matchWidth());
        EditText input = GameUi.input(c, getString(R.string.ent_type_enter));
        GameUi.borderInput(c, input, GameUi.PURPLE);
        input.setEnabled(!answered);
        input.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_DONE && !answered && !input.getText().toString().trim().isEmpty()) {
                answered = true;
                GameUi.hideKeyboard(input);
                // Rebuild after the key is released: rebuilding now would move focus to the exit button, which
                // then takes the key-up of Enter as a click and closes the screen.
                input.post(this::render);
            }
            return true;
        });
        LinearLayout.LayoutParams lp = GameUi.height(c, 58);
        lp.topMargin = Ui.dp(c, 18);
        body.addView(input, lp);
        TextView st = GameUi.centered(c, answered ? getString(R.string.ent_recorded_next) : getString(R.string.ent_not_typed), 13, 700, Ui.MUTED);
        Ui.add(body, st, 10);
    }

    private void renderChoice(Context c, Word w) {
        LinearLayout hero = GameUi.hero(c, Ui.RED_TINT, GameUi.RED_BORDER, getString(R.string.ent_fill_blank), Ui.RED);
        TextView s = GameUi.centered(c, w.blank, 20, 700, Ui.INK);
        s.setLineSpacing(0, 1.2f);
        Ui.add(hero, s, 10);
        body.addView(hero, Ui.matchWidth());
        for (int k = 0; k < w.choices.length; k++) {
            addOption(c, w.choices[k], k, -1, Ui.RED_TINT, Ui.RED, k == 0 ? 16 : 10);
        }
    }

    /** An answer that is recorded but not marked right or wrong, as in the contest rules. */
    private void addOption(Context c, String text, int k, int unused, int selBg, int selBorder, float top) {
        TextView o = GameUi.option(c, text);
        if (picked == k) {
            GameUi.style(c, o, selBg, selBorder, Ui.INK);
        }
        o.setOnClickListener(v -> {
            if (!answered) {
                picked = k;
                answered = true;
                render();
            }
        });
        Ui.add(body, o, top);
    }
}
