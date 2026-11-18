package com.doradodo.app.ui.entertainment;

import com.doradodo.app.R;
import com.doradodo.app.data.local.entity.GameWord;
import com.doradodo.app.data.repository.EntertainmentRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;

/** "Đoán từ bí ẩn": read the English definition of a word the learner studied and pick the word. */
@AndroidEntryPoint
public class GuessWordFragment extends QuizFragment {

    private static final int QUESTIONS = 5;

    @Override
    protected Config config() {
        Config c = new Config();
        c.title = getString(R.string.ent_guess_title);
        c.scoreLabelRes = R.string.ent_label_score;
        c.heroLabel = getString(R.string.ent_definition);
        c.optionsLabel = getString(R.string.ent_pick_word);
        c.correctMsg = getString(R.string.ent_correct_100);
        c.finishedPrompt = getString(R.string.ent_finished_score);
        c.restartLabel = getString(R.string.ent_play_again);
        c.bigPrompt = false;
        c.progressColor = GameUi.PURPLE;
        c.heroBg = GameUi.PURPLE_TINT;
        c.heroBorder = GameUi.PURPLE_BORDER;
        c.heroInk = GameUi.PURPLE_EDGE;
        c.points = 100;
        c.emptyTitle = getString(R.string.ent_learn_more);
        c.emptyMessage = getString(R.string.ent_learn_more_msg);
        return c;
    }

    @Override
    protected Q[] buildQuestions(EntertainmentRepository repo) {
        List<GameWord> words = repo.learnedWords(QUESTIONS, true);
        if (words.size() < 3) {
            return new Q[0];
        }
        Set<String> ids = new HashSet<>();
        for (GameWord w : words) {
            ids.add(w.id);
        }
        List<GameWord> others = repo.distractors(ids, 6);
        List<Q> out = new ArrayList<>();
        for (GameWord w : words) {
            List<String> options = new ArrayList<>();
            options.add(w.term);
            for (GameWord o : words) {
                if (!o.id.equals(w.id) && options.size() < 3) {
                    options.add(o.term);
                }
            }
            for (GameWord o : others) {
                if (options.size() < 3 && !options.contains(o.term)) {
                    options.add(o.term);
                }
            }
            Collections.shuffle(options);
            out.add(new Q(w.definition, options.toArray(new String[0]), options.indexOf(w.term)));
        }
        return out.toArray(new Q[0]);
    }
}
