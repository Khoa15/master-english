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

/** "Ôn lỗi sai": quiz on the words the learner answered wrongly most often (word_progress.times_wrong). */
@AndroidEntryPoint
public class MistakesFragment extends QuizFragment {

    private static final int QUESTIONS = 8;

    @Override
    protected Config config() {
        Config c = new Config();
        c.title = getString(R.string.ent_mistakes_title);
        c.scoreLabelRes = R.string.ent_label_correct;
        c.heroLabel = getString(R.string.ent_often_wrong);
        c.heroHint = getString(R.string.ent_pick_meaning);
        c.correctMsg = getString(R.string.ent_correct);
        c.finishedPrompt = getString(R.string.ent_done);
        c.restartLabel = getString(R.string.ent_review_again);
        c.bigPrompt = true;
        c.progressColor = GameUi.AMBER_EDGE;
        c.heroBg = GameUi.AMBER_SOFT;
        c.heroBorder = GameUi.AMBER_BORDER;
        c.heroInk = 0xFF8A5A0B;
        c.points = 1;
        c.emptyTitle = getString(R.string.ent_no_mistakes);
        c.emptyMessage = getString(R.string.ent_no_mistakes_msg);
        return c;
    }

    @Override
    protected Q[] buildQuestions(EntertainmentRepository repo) {
        List<GameWord> words = repo.mistakeWords(QUESTIONS);
        Set<String> ids = new HashSet<>();
        for (GameWord w : words) {
            ids.add(w.id);
        }
        List<GameWord> others = repo.distractors(ids, 6);
        List<Q> out = new ArrayList<>();
        for (GameWord w : words) {
            List<String> wrong = new ArrayList<>();
            for (GameWord o : others) {
                if (o.meaning != null && !o.meaning.equalsIgnoreCase(w.meaning)) {
                    wrong.add(o.meaning);
                }
            }
            Collections.shuffle(wrong);
            List<String> options = new ArrayList<>();
            options.add(w.meaning);
            options.addAll(wrong.subList(0, Math.min(2, wrong.size())));
            Collections.shuffle(options);
            out.add(new Q(w.term, options.toArray(new String[0]), options.indexOf(w.meaning)));
        }
        return out.toArray(new Q[0]);
    }
}
