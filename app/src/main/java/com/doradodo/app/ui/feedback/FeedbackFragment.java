package com.doradodo.app.ui.feedback;

import android.content.Context;
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
import com.doradodo.app.R;
import com.doradodo.app.data.repository.FeedbackRepository;
import com.doradodo.app.ui.ds.DsFragment;
import com.doradodo.app.ui.ds.Ui;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Feedback form: star rating, type and message, posted to the backend. */
@AndroidEntryPoint
public class FeedbackFragment extends DsFragment {

    /** English names go into the message to the developers; the chips show them in the learner's language. */
    private static final String[] TYPES = {"Bug", "Idea", "Question", "Other"};
    private static final int[] TYPE_LABELS = {R.string.fb_type_bug, R.string.fb_type_idea, R.string.fb_type_question,
            R.string.fb_type_other};

    @Inject FeedbackRepository feedbackRepository;

    private Ui.Screen screen;
    private TextView sendButton;
    private boolean sending;
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
        screen.top.addView(Ui.titleBar(c, onBack(), c.getString(R.string.fb_title), null), Ui.matchWidth());
        LinearLayout body = screen.body;
        TextView intro = Ui.text(c, c.getString(R.string.fb_intro), 14, 400, Ui.MUTED);
        intro.setLineSpacing(Ui.dp(c, 3), 1f);
        body.addView(intro, Ui.matchWidth());

        Ui.add(body, Ui.text(c, c.getString(R.string.fb_experience), 13, 700, Ui.MUTED), 20);
        stars = Ui.row(c);
        Ui.add(body, stars, 8);
        Ui.add(body, Ui.text(c, c.getString(R.string.fb_type), 13, 700, Ui.MUTED), 20);
        types = Ui.row(c);
        Ui.add(body, types, 8);
        Ui.add(body, Ui.text(c, c.getString(R.string.fb_message), 13, 700, Ui.MUTED), 20);
        message = new EditText(c);
        message.setHint(R.string.fb_hint);
        message.setMinLines(5);
        message.setGravity(Gravity.TOP | Gravity.START);
        message.setTextSize(15);
        message.setTextColor(Ui.INK);
        message.setHintTextColor(0xFF8B948E);
        message.setBackground(Ui.shape(c, Ui.CARD, 16, Ui.LINE, 1.5f));
        message.setPadding(Ui.dp(c, 16), Ui.dp(c, 14), Ui.dp(c, 16), Ui.dp(c, 14));
        Ui.font(message, 400);
        Ui.add(body, message, 8);
        Ui.add(body, Ui.text(c, c.getString(R.string.fb_optional), 12, 400, Ui.MUTED), 6);

        sendButton = Ui.primaryButton(c, c.getString(R.string.fb_send));
        sendButton.setOnClickListener(v -> send());
        screen.setFooter(sendButton);
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
            star.setContentDescription(Ui.plural(c, R.plurals.plural_stars, i));
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
            TextView chip = Ui.text(c, c.getString(TYPE_LABELS[i]), 13, on ? 700 : 600, on ? 0xFFFFFFFF : Ui.INK);
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
        if (sending) {
            return;
        }
        String text = message.getText().toString().trim();
        if (text.isEmpty()) {
            message.setError(getString(R.string.fb_write_first));
            return;
        }
        sending = true;
        sendButton.setAlpha(0.6f);
        feedbackRepository.send(rating, TYPES[type], text, result -> {
            sending = false;
            if (!isAdded()) {
                return;
            }
            sendButton.setAlpha(1f);
            boolean ok = result == FeedbackRepository.OK;
            if (ok) {
                message.setText("");
                rating = 0;
                drawStars();
            }
            // On failure the text stays in the box so the learner can retry.
            int messageRes = ok ? R.string.fb_sent
                    : result == FeedbackRepository.SIGN_IN_REQUIRED ? R.string.fb_sign_in
                    : result == FeedbackRepository.RATE_LIMITED ? R.string.fb_rate_limited : R.string.fb_failed;
            Snackbar.make(screen.root, getString(messageRes), Snackbar.LENGTH_LONG).show();
        });
    }
}
