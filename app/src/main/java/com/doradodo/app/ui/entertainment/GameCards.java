package com.doradodo.app.ui.entertainment;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.doradodo.app.R;
import com.doradodo.app.data.repository.EntertainmentRepository.ActivityCard;
import com.doradodo.app.ui.ds.Ui;

/** The game card of the Entertainment page, shared with the "all games" screen. */
final class GameCards {

    private GameCards() {
    }

    static View gameCard(Context c, ActivityCard item, GameUi.Accent accent, Runnable onPlay) {
        LinearLayout card = Ui.column(c);
        card.setBackground(Ui.shape(c, Ui.CARD, 20, Ui.LINE, 2));
        card.setPadding(Ui.dp(c, 2), Ui.dp(c, 2), Ui.dp(c, 2), Ui.dp(c, 2));
        FrameLayout image = new FrameLayout(c);
        float r = Ui.dp(c, 18);
        GradientDrawable top = new GradientDrawable();
        top.setColor(accent.tint);
        top.setCornerRadii(new float[]{r, r, r, r, 0, 0, 0, 0});
        image.setBackground(top);
        image.addView(Ui.icon(c, GameUi.drawable(c, item.activity.image, R.drawable.ic_ent_star), 46, accent.main),
                new FrameLayout.LayoutParams(Ui.dp(c, 46), Ui.dp(c, 46), Gravity.CENTER));
        card.addView(image, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(c, 96)));

        LinearLayout info = Ui.column(c);
        info.setPadding(Ui.dp(c, 10), Ui.dp(c, 10), Ui.dp(c, 10), Ui.dp(c, 10));
        info.addView(Ui.text(c, item.activity.title, 15, 700, Ui.INK));
        if (item.learners > 0) {
            LinearLayout meta = Ui.row(c);
            meta.addView(Ui.icon(c, R.drawable.ic_ent_users, 14, Ui.MUTED));
            LinearLayout.LayoutParams cp = Ui.wrap();
            cp.leftMargin = Ui.dp(c, 4);
            meta.addView(Ui.text(c, c.getString(R.string.ent_learners_count, item.learners), 12, 600, Ui.MUTED), cp);
            Ui.add(info, meta, 6);
        } else if (item.activity.subtitle != null) {
            Ui.add(info, Ui.text(c, item.activity.subtitle, 12, 400, Ui.MUTED), 6);
        }
        View spacer = new View(c);
        spacer.setMinimumHeight(Ui.dp(c, 12));
        info.addView(spacer, new LinearLayout.LayoutParams(1, 0, 1f));
        boolean amber = "AMBER".equals(item.activity.accent);
        TextView play = GameUi.button3d(c, c.getString(R.string.ent_play), amber ? Ui.INK : android.graphics.Color.WHITE,
                amber ? Ui.AMBER : accent.main, accent.edge == 0xFF8A5A0B ? GameUi.AMBER_EDGE : accent.edge, 12, 14);
        play.setPadding(0, Ui.dp(c, 8), 0, Ui.dp(c, 11));
        play.setOnClickListener(v -> onPlay.run());
        info.addView(play, Ui.matchWidth());
        card.addView(info, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return card;
    }
}
