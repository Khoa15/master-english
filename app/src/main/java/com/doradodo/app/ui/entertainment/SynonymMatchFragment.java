package com.doradodo.app.ui.entertainment;

import com.doradodo.app.R;
import com.doradodo.app.data.repository.EntertainmentRepository;
import com.doradodo.app.data.repository.SynonymPair;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * "Ghép cặp từ đồng nghĩa": match each word with a synonym of the same language, read from the synonym tables.
 * Synonymy never crosses languages, so the game is built for the languages listed in its design_for.
 */
@AndroidEntryPoint
public class SynonymMatchFragment extends MatchFragment {

    private static final int PAIRS = 5;

    @Override
    protected Config config() {
        Config c = new Config();
        c.title = getString(R.string.ent_syn_title);
        c.leftLabel = getString(R.string.ent_col_word);
        c.rightLabel = getString(R.string.ent_col_synonym);
        c.idle = getString(R.string.ent_pick_a_word);
        c.selectedRes = R.string.ent_pick_synonym_of;
        c.emptyTitle = getString(R.string.ent_no_synonyms);
        c.emptyMessage = getString(R.string.ent_no_synonyms_msg);
        c.badgeBg = GameUi.PURPLE_TINT;
        c.badgeFg = GameUi.PURPLE_EDGE;
        c.progressColor = GameUi.PURPLE;
        c.selBg = GameUi.PURPLE_TINT;
        c.selBorder = GameUi.PURPLE;
        c.selInk = GameUi.PURPLE_EDGE;
        return c;
    }

    @Override
    protected List<String[]> loadPairs(EntertainmentRepository repo) {
        List<String[]> pairs = new ArrayList<>();
        for (SynonymPair p : repo.synonymPairs(PAIRS)) {
            pairs.add(new String[]{p.left, p.right});
        }
        return pairs;
    }
}
