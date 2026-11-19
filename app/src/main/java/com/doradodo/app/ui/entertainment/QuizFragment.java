package com.doradodo.app.ui.entertainment;

import android.content.Context;
import android.os.Bundle;
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

/** One question per screen with three or four answers, instant feedback and a score. */
public abstract class QuizFragment extends GameFragment {

    protected static final class Q {
        final String prompt;
        final String[] opts;
        final int ans;
        /** Optional explanation, shown under the feedback once the learner has answered. */
        final String explain;

        Q(String prompt, String[] opts, int ans) {
            this(prompt, opts, ans, null);
        }

        Q(String prompt, String[] opts, int ans, String explain) {
            this.prompt = prompt;
            this.opts = opts;
            this.ans = ans;
            this.explain = explain;
        }
    }

    protected static final class Config {
        String title;
        /** String resource of the score label ("Correct" or "Score"). */
        int scoreLabelRes;
        String heroLabel;
        String heroHint;
        String optionsLabel;
        String correctMsg;
        String finishedPrompt;
        String restartLabel;
        boolean bigPrompt;
        int progressColor;
        int heroBg;
        int heroBorder;
        int heroInk;
        int points;
        String emptyTitle;
        String emptyMessage;
    }

    protected abstract Config config();

    /** The questions, built from the database off the main thread. */
    protected abstract Q[] buildQuestions(GameWordRepository repo);

    private Q[] questions = new Q[0];

    private Config cfg;
    private GameUi.Page page;
    private LinearLayout body;
    private TextView button;
    private int index;
    private int picked = -1;
    private int score;
    private boolean finished;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        cfg = config();
        page = GameUi.page(c, true, onBack(), cfg.title, "", 0, 0, cfg.progressColor);
        body = page.screen.body;
        button = GameUi.footerButton(c, "", Ui.INK, Ui.AMBER, GameUi.AMBER_EDGE);
        button.setOnClickListener(v -> next());
        body.addView(loadingState(c));
        load(() -> buildQuestions(vm().words()), qs -> {
            body.removeAllViews();
            if (qs == null || qs.length == 0) {
                body.addView(emptyState(requireContext(), cfg.emptyTitle, cfg.emptyMessage), Ui.matchWidth());
                return;
            }
            questions = qs;
            GameUi.setFooter(requireContext(), page.screen, button);
            render();
        });
        return page.screen.root;
    }

    private void next() {
        int total = questions.length;
        if (finished) {
            index = 0;
            picked = -1;
            score = 0;
            finished = false;
        } else if (picked < 0) {
            return;
        } else if (index >= total - 1) {
            finished = true;
        } else {
            index++;
            picked = -1;
        }
        render();
    }

    private void render() {
        Context c = requireContext();
        Q[] qs = questions;
        int total = qs.length;
        Q q = qs[Math.min(index, total - 1)];
        boolean answered = picked >= 0;
        body.removeAllViews();

        LinearLayout hero = GameUi.hero(c, cfg.heroBg, cfg.heroBorder, cfg.heroLabel, cfg.heroInk);
        String prompt = finished ? cfg.finishedPrompt == null ? "" : String.format(cfg.finishedPrompt, score) : q.prompt;
        TextView p = GameUi.centered(c, prompt, cfg.bigPrompt ? 30 : 18, cfg.bigPrompt ? 700 : 600, Ui.INK);
        p.setLineSpacing(0, 1.25f);
        Ui.add(hero, p, 8);
        if (cfg.heroHint != null && !finished) {
            Ui.add(hero, GameUi.centered(c, cfg.heroHint, 13, 400, Ui.MUTED), 6);
        }
        body.addView(hero, Ui.matchWidth());

        if (!finished) {
            if (cfg.optionsLabel != null) {
                Ui.add(body, Ui.text(c, cfg.optionsLabel, 13, 600, Ui.MUTED), 18);
            }
            for (int k = 0; k < q.opts.length; k++) {
                final int choice = k;
                TextView o = GameUi.option(c, q.opts[k]);
                if (answered && k == q.ans) {
                    GameUi.style(c, o, Ui.GREEN_TINT, Ui.GREEN, Ui.GREEN_INK);
                } else if (answered && k == picked) {
                    GameUi.style(c, o, Ui.RED_TINT, Ui.RED, GameUi.RED_EDGE);
                }
                o.setOnClickListener(v -> {
                    if (picked < 0) {
                        picked = choice;
                        if (choice == q.ans) {
                            score += cfg.points;
                        }
                        render();
                    }
                });
                Ui.add(body, o, k == 0 && cfg.optionsLabel == null ? 16 : 10);
            }
            String feedback = !answered ? "" : (picked == q.ans ? cfg.correctMsg
                    : getString(R.string.ent_wrong_answer_is, q.opts[q.ans]));
            TextView f = Ui.text(c, feedback, 14, 700, picked == q.ans ? Ui.GREEN : Ui.RED);
            f.setMinHeight(Ui.dp(c, 20));
            Ui.add(body, f, 12);
            // if (answered && q.explain != null) {
            //     TextView e = GameUi.centered(c, q.explain, 14, 400, Ui.MUTED);
            //     e.setGravity(android.view.Gravity.START);
            //     e.setLineSpacing(0, 1.2f);
            //     Ui.add(body, e, 4);
            // }
        }

        int shown = finished ? total : index + 1;
        page.subtitle.setText(getString(R.string.ent_quiz_subtitle, shown, total, getString(cfg.scoreLabelRes), score));
        Ui.setProgress(page.progress, (finished ? total : index + (answered ? 1 : 0)) * 100 / total);

        boolean ready = answered || finished;
        button.setText(finished ? cfg.restartLabel
                : (answered ? (index == total - 1 ? getString(R.string.ent_see_result) : getString(R.string.ent_next_question)) : getString(R.string.ent_choose_answer)));
        button.setTextColor(Ui.INK);
        GameUi.setReady(button, ready);
    }
}
