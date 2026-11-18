package com.doradodo.app.ui.entertainment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.doradodo.app.R;
import com.doradodo.app.data.local.entity.GameWord;
import java.util.ArrayList;
import java.util.List;
import com.doradodo.app.ui.ds.Ui;

/** "Chép siêu tốc": read the meaning and the first two letters, then type the English word. */
@dagger.hilt.android.AndroidEntryPoint
public class CopyFragment extends GameFragment {

    private static final int ROUND_COUNT = 5;
    private static final int MIN_ROUNDS = 3;

    /** {term, meaning} of the words to type, from the words the learner studied. */
    private List<String[]> rounds = new ArrayList<>();

    private GameUi.Page page;
    private LinearLayout body;
    private EditText input;
    private TextView feedback;
    private TextView again;
    private int round;
    private int score;
    private boolean wrong;
    private boolean finished;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        page = GameUi.page(c, true, onBack(), getString(R.string.ent_copy_title), "", 0, 0, Ui.AMBER);
        body = page.screen.body;
        again = GameUi.footerButton(c, getString(R.string.ent_play_again), Ui.INK, Ui.AMBER, GameUi.AMBER_EDGE);
        again.setOnClickListener(v -> startRound());
        GameUi.setFooter(c, page.screen, again);
        startRound();
        return page.screen.root;
    }

    private void startRound() {
        Context c = requireContext();
        round = 0;
        score = 0;
        wrong = false;
        finished = false;
        page.screen.footer.setVisibility(View.GONE);
        body.removeAllViews();
        body.addView(loadingState(c));
        load(() -> {
            List<String[]> out = new ArrayList<>();
            for (GameWord w : vm().repo().learnedWords(ROUND_COUNT * 3, false)) {
                // Single words of any script (letters only, two or more) can be typed back.
                if (w.term.matches("\\p{L}{2,}") && out.size() < ROUND_COUNT) {
                    out.add(new String[]{w.term.toLowerCase(java.util.Locale.ROOT), w.meaning});
                }
            }
            return out;
        }, words -> {
            rounds = words == null ? new ArrayList<>() : words;
            if (rounds.size() < MIN_ROUNDS) {
                body.removeAllViews();
                body.addView(emptyState(requireContext(), getString(R.string.ent_learn_more),
                        getString(R.string.ent_learn_more_msg)),
                        Ui.matchWidth());
                page.subtitle.setText("");
                return;
            }
            render();
        });
    }

    private void render() {
        Context c = requireContext();
        String[] cur = rounds.get(Math.min(round, rounds.size() - 1));
        body.removeAllViews();
        LinearLayout hero = GameUi.hero(c, GameUi.AMBER_SOFT, GameUi.AMBER_BORDER, getString(R.string.ent_type_back), 0xFF8A5A0B);
        Ui.add(hero, GameUi.centered(c, finished ? "Xong!" : cur[1], 24, 700, Ui.INK), 8);
        if (!finished) {
            Ui.add(hero, GameUi.centered(c, getString(R.string.ent_hint), 14, 600, Ui.MUTED), 12);
            TextView hint = GameUi.centered(c, GameUi.hint(cur[0]), 26, 700, Ui.INK);
            hint.setLetterSpacing(0.14f);
            Ui.add(hero, hint, 4);
            Ui.add(hero, GameUi.centered(c, getString(R.string.ent_letters, cur[0].codePointCount(0, cur[0].length())), 12, 400, Ui.MUTED), 4);
        }
        body.addView(hero, Ui.matchWidth());

        if (!finished) {
            input = GameUi.input(c, getString(R.string.ent_type_word_enter));
            GameUi.borderInput(c, input, wrong ? Ui.RED : Ui.BLUE);
            input.addTextChangedListener(new android.text.TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int a, int b, int d) { }

                @Override
                public void onTextChanged(CharSequence s, int a, int b, int d) {
                    if (wrong) {
                        wrong = false;
                        GameUi.borderInput(requireContext(), input, Ui.BLUE);
                        feedback.setText("");
                    }
                }

                @Override
                public void afterTextChanged(android.text.Editable s) { }
            });
            input.setOnEditorActionListener((v, action, event) -> {
                if (action == EditorInfo.IME_ACTION_DONE) {
                    check(cur[0]);
                }
                return true;
            });
            LinearLayout.LayoutParams lp = GameUi.height(c, 58);
            lp.topMargin = Ui.dp(c, 18);
            body.addView(input, lp);
            feedback = Ui.text(c, wrong ? getString(R.string.ent_try_again) : "", 14, 700, Ui.RED);
            feedback.setGravity(android.view.Gravity.CENTER);
            Ui.add(body, feedback, 12);
        } else {
            Ui.add(body, GameUi.centered(c, getString(R.string.ent_done_correct, score, rounds.size()), 16, 700, Ui.GREEN), 18);
        }

        int total = rounds.size();
        page.subtitle.setText(getString(R.string.ent_copy_subtitle, finished ? total : round + 1, total, score));
        Ui.setProgress(page.progress, (finished ? total : round) * 100 / total);
        page.screen.footer.setVisibility(finished ? View.VISIBLE : View.GONE);
        if (!finished) {
            input.requestFocus();
        }
    }

    private void check(String word) {
        String typed = input.getText().toString().trim().toLowerCase(java.util.Locale.ROOT);
        if (!typed.equals(word)) {
            wrong = true;
            GameUi.borderInput(requireContext(), input, Ui.RED);
            feedback.setText(getString(R.string.ent_try_again));
            return;
        }
        score++;
        wrong = false;
        if (round >= rounds.size() - 1) {
            finished = true;
            GameUi.hideKeyboard(input);
        } else {
            round++;
        }
        // After the key is released, so focus does not jump to the exit button and trigger it (see ContestPlay).
        input.post(this::render);
    }
}
