package com.doradodo.app.ui.entertainment;

import com.doradodo.app.R;
import com.doradodo.app.data.local.entity.GameWord;
import com.doradodo.app.data.repository.EntertainmentRepository;
import com.doradodo.app.ui.ds.Ui;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/** "Nối từ tiếng Anh": match words the learner has studied with their Vietnamese meaning. */
@AndroidEntryPoint
public class WordMatchFragment extends MatchFragment {

    private static final int PAIRS = 6;

    @Override
    protected Config config() {
        Config c = new Config();
        c.title = getString(R.string.ent_match_title);
        c.leftLabel = getString(R.string.ent_col_word);
        c.rightLabel = getString(R.string.ent_col_meaning);
        c.idle = getString(R.string.ent_pick_a_word);
        c.selectedRes = R.string.ent_pick_meaning_for;
        c.emptyTitle = getString(R.string.ent_learn_more);
        c.emptyMessage = getString(R.string.ent_learn_more_msg);
        c.badgeBg = Ui.AMBER_TINT;
        c.badgeFg = Ui.AMBER_INK;
        c.progressColor = Ui.GREEN;
        c.selBg = Ui.BLUE_TINT;
        c.selBorder = Ui.BLUE;
        c.selInk = Ui.BLUE_INK;
        return c;
    }

    @Override
    protected List<String[]> loadPairs(EntertainmentRepository repo) {
        List<String[]> pairs = new ArrayList<>();
        for (GameWord w : repo.learnedWords(PAIRS, false)) {
            pairs.add(new String[]{w.term, w.meaning});
        }
        return pairs;
    }
}
