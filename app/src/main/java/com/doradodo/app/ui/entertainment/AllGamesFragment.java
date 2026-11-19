package com.doradodo.app.ui.entertainment;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.doradodo.app.R;
import com.doradodo.app.data.repository.EntertainmentRepository.ActivityCard;
import com.doradodo.app.ui.ds.Ui;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/** Every game of the catalogue, two to a row. Reached from "Tất cả" when there are more than four. */
@AndroidEntryPoint
public class AllGamesFragment extends GameFragment {

    private LinearLayout body;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        GameUi.Page page = GameUi.page(c, false, onBack(), c.getString(R.string.ent_all_games), "", 0, 0, 0);
        page.subtitle.setVisibility(View.GONE);
        body = page.screen.body;
        body.addView(loadingState(c));
        load(() -> vm().repo().loadGames(), this::render);
        return page.screen.root;
    }

    private void render(@Nullable List<ActivityCard> games) {
        Context c = requireContext();
        body.removeAllViews();
        List<ActivityCard> playable = new ArrayList<>();
        if (games != null) {
            for (ActivityCard g : games) {
                if (Routes.destination(g.activity.code) != 0) {
                    playable.add(g);
                }
            }
        }
        for (int i = 0; i < playable.size(); i += 2) {
            LinearLayout row = Ui.row(c);
            row.setBaselineAligned(false);
            row.setGravity(Gravity.TOP);
            row.addView(card(c, playable.get(i), true), cell(c, true));
            if (i + 1 < playable.size()) {
                row.addView(card(c, playable.get(i + 1), false), cell(c, false));
            } else {
                row.addView(new View(c), cell(c, false));
            }
            Ui.add(body, row, i == 0 ? 0 : 12);
        }
    }

    private View card(Context c, ActivityCard item, boolean first) {
        return GameCards.gameCard(c, item, GameUi.accent(item.activity.accent), () -> {
            vm().async(() -> vm().repo().recordPlay(item.activity.code));
            go(Routes.destination(item.activity.code), null);
        });
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
}
