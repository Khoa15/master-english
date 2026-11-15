package com.masterenglish.app.ui.feedback;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;
import com.masterenglish.app.R;
import com.masterenglish.app.ui.ds.DsFragment;
import com.masterenglish.app.ui.ds.Ui;

import dagger.hilt.android.AndroidEntryPoint;

/** Feedback form: star rating, type and message, sent through the learner's own mail or chat app. */
@AndroidEntryPoint
public class FeedbackFragment extends DsFragment {

    private static final String[] TYPES = {"Bug", "Idea", "Question", "Other"};

    private Ui.Screen screen;
    private LinearLayout stars;
    private LinearLayout types;
    private EditText message;
    private int rating;
    private int type = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        screen = new Ui.Screen(c, 24);
        screen.top.addView(Ui.titleBar(c, onBack(), "Feedback", null), Ui.matchWidth());
        LinearLayout body = screen.body;
        TextView intro = Ui.text(c, "Tell us what works and what doesn't. We read every message.", 14, 400, Ui.MUTED);
        intro.setLineSpacing(Ui.dp(c, 3), 1f);
        body.addView(intro, Ui.matchWidth());

        Ui.add(body, Ui.text(c, "How is your experience?", 13, 700, Ui.MUTED), 20);
        stars = Ui.row(c);
        Ui.add(body, stars, 8);
        Ui.add(body, Ui.text(c, "Type of feedback", 13, 700, Ui.MUTED), 20);
        types = Ui.row(c);
        Ui.add(body, types, 8);
        Ui.add(body, Ui.text(c, "Your message", 13, 700, Ui.MUTED), 20);
        message = new EditText(c);
        message.setHint("Describe your idea or the problem you ran into");
        message.setMinLines(5);
        message.setGravity(Gravity.TOP | Gravity.START);
        message.setTextSize(15);
        message.setTextColor(Ui.INK);
        message.setHintTextColor(0xFF8B948E);
        message.setBackground(Ui.shape(c, Ui.CARD, 16, Ui.LINE, 1.5f));
        message.setPadding(Ui.dp(c, 16), Ui.dp(c, 14), Ui.dp(c, 16), Ui.dp(c, 14));
        Ui.font(message, 400);
        Ui.add(body, message, 8);
        Ui.add(body, Ui.text(c, "Optional: include steps to reproduce if you found a bug.", 12, 400, Ui.MUTED), 6);

        TextView send = Ui.primaryButton(c, "Send feedback");
        send.setOnClickListener(v -> send());
        screen.setFooter(send);
        drawStars();
        drawTypes();
        return screen.root;
    }

    private void drawStars() {
        Context c = requireContext();
        stars.removeAllViews();
        for (int i = 1; i <= 5; i++) {
            final int value = i;
            boolean on = i == rating;
            ImageView star = Ui.icon(c, R.drawable.ic_ds_star, 22, i <= rating ? Ui.AMBER : 0xFFC9C4B8);
            star.setPadding(Ui.dp(c, 11), Ui.dp(c, 11), Ui.dp(c, 11), Ui.dp(c, 11));
            star.setBackground(Ui.shape(c, on ? Ui.GREEN_TINT : Ui.CARD, 12, on ? Ui.GREEN : Ui.LINE, on ? 1.5f : 1));
            star.setContentDescription(i + (i == 1 ? " star" : " stars"));
            star.setOnClickListener(v -> {
                rating = value;
                drawStars();
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(Ui.dp(c, 44), Ui.dp(c, 44));
            p.rightMargin = Ui.dp(c, 8);
            stars.addView(star, p);
        }
    }

    private void drawTypes() {
        Context c = requireContext();
        types.removeAllViews();
        for (int i = 0; i < TYPES.length; i++) {
            final int value = i;
            boolean on = i == type;
            TextView chip = Ui.text(c, TYPES[i], 13, on ? 700 : 600, on ? 0xFFFFFFFF : Ui.INK);
            chip.setBackground(Ui.shape(c, on ? Ui.GREEN : Ui.CARD, 999, on ? Ui.GREEN : Ui.LINE, 1));
            chip.setPadding(Ui.dp(c, 14), Ui.dp(c, 8), Ui.dp(c, 14), Ui.dp(c, 8));
            chip.setMinHeight(Ui.dp(c, 36));
            chip.setGravity(Gravity.CENTER);
            chip.setOnClickListener(v -> {
                type = value;
                drawTypes();
            });
            LinearLayout.LayoutParams p = Ui.wrap();
            p.rightMargin = Ui.dp(c, 8);
            types.addView(chip, p);
        }
    }

    private void send() {
        String text = message.getText().toString().trim();
        if (text.isEmpty()) {
            message.setError("Write a few words first");
            return;
        }
        String body = "Rating: " + (rating == 0 ? "not given" : rating + "/5") + "\nType: " + TYPES[type] + "\n\n" + text;
        Intent intent = new Intent(Intent.ACTION_SEND).setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, "Master English feedback: " + TYPES[type])
                .putExtra(Intent.EXTRA_TEXT, body);
        try {
            startActivity(Intent.createChooser(intent, "Send feedback"));
            message.setText("");
        } catch (android.content.ActivityNotFoundException e) {
            Snackbar.make(screen.root, "No app available to send feedback", Snackbar.LENGTH_LONG).show();
        }
    }
}
