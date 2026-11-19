package com.doradodo.app.ui.entertainment;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.doradodo.app.data.repository.WordSource;
import com.doradodo.app.R;
import com.doradodo.app.ui.ds.Ui;

/** Shared pieces of the Entertainment screens: palette, top bar, hero cards, options and 3D buttons. */
final class GameUi {

    static final int PURPLE = 0xFF6B4FA0;
    static final int PURPLE_EDGE = 0xFF4F3A7D;
    static final int PURPLE_TINT = 0xFFEEE7F7;
    static final int PURPLE_BORDER = 0xFFD3C4EE;
    static final int AMBER_EDGE = 0xFFB07A16;
    static final int AMBER_SOFT = 0xFFFBF0D9;
    static final int AMBER_BORDER = 0xFFF3D28A;
    static final int BLUE_EDGE = 0xFF214E9C;
    static final int BLUE_SOFT = 0xFFEAF1FC;
    static final int BLUE_BORDER = 0xFFBFD3F2;
    static final int RED_BORDER = 0xFFE9B9AA;
    static final int RED_EDGE = 0xFF8E2E1F;
    static final int GREEN_BORDER = 0xFFA9D7BD;
    static final int GREEN_EDGE = 0xFF1F5C41;
    static final int GREY = 0xFFB9BCC4;
    static final int GREY_EDGE = 0xFF8F949D;
    static final int OFF = 0xFFECE9E1;
    static final int DOT_OFF = 0xFFCFC9BC;

    private GameUi() {
    }

    /** The four colours of an accent token stored in the database. */
    static final class Accent {
        final int main;
        final int tint;
        final int edge;
        final int ink;

        Accent(int main, int tint, int edge, int ink) {
            this.main = main;
            this.tint = tint;
            this.edge = edge;
            this.ink = ink;
        }
    }

    static Accent accent(String token) {
        switch (token == null ? "" : token) {
            case "BLUE":
                return new Accent(Ui.BLUE, 0xFFD9E5F7, BLUE_EDGE, BLUE_EDGE);
            case "PURPLE":
                return new Accent(PURPLE, PURPLE_TINT, PURPLE_EDGE, PURPLE_EDGE);
            case "AMBER":
                return new Accent(Ui.AMBER, AMBER_SOFT, AMBER_EDGE, 0xFF8A5A0B);
            case "RED":
                return new Accent(Ui.RED, Ui.RED_TINT, RED_EDGE, Ui.RED);
            default:
                return new Accent(Ui.GREEN, Ui.GREEN_TINT, GREEN_EDGE, Ui.GREEN_INK);
        }
    }

    /** A drawable stored by name in the database; {@code fallback} when the name is empty or unknown. */
    static int drawable(Context c, @Nullable String name, int fallback) {
        if (name == null || name.isEmpty()) {
            return fallback;
        }
        int id = c.getResources().getIdentifier(name, "drawable", c.getPackageName());
        return id == 0 ? fallback : id;
    }

    /** A screen with the standard top bar (exit or back, title, subtitle, optional badge) and progress bar. */
    static final class Page {
        final Ui.Screen screen;
        final TextView subtitle;
        @Nullable
        final TextView badge;
        @Nullable
        final View progress;

        Page(Ui.Screen screen, TextView subtitle, @Nullable TextView badge, @Nullable View progress) {
            this.screen = screen;
            this.subtitle = subtitle;
            this.badge = badge;
            this.progress = progress;
        }
    }

    static Page page(Context c, boolean close, View.OnClickListener onExit, String title, String subtitle,
                     int badgeBg, int badgeFg, int progressColor) {
        Ui.Screen s = new Ui.Screen(c, 16);
        s.top.setPadding(Ui.dp(c, 20), Ui.dp(c, 16), Ui.dp(c, 20), 0);
        LinearLayout bar = Ui.row(c);
        android.widget.ImageView exit = Ui.backButton(c, close ? R.drawable.ic_ds_close : R.drawable.ic_ds_back,
                onExit, c.getString(close ? R.string.ent_exit : R.string.ent_back));
        // A rebuilt screen hands focus to its first focusable view; if that were this button, the key-up of Enter
        // (typing an answer) would click it and close the game.
        exit.setFocusable(false);
        bar.addView(exit);
        LinearLayout titles = Ui.column(c);
        titles.addView(Ui.text(c, title, 18, 700, Ui.INK));
        TextView sub = Ui.text(c, subtitle, 12, 600, Ui.MUTED);
        Ui.add(titles, sub, 2);
        LinearLayout.LayoutParams tp = Ui.weight(1f);
        tp.leftMargin = Ui.dp(c, 12);
        bar.addView(titles, tp);
        TextView badge = null;
        if (badgeBg != 0) {
            badge = Ui.pill(c, "", badgeBg, badgeFg, 14);
            badge.setPadding(Ui.dp(c, 12), Ui.dp(c, 6), Ui.dp(c, 12), Ui.dp(c, 6));
            bar.addView(badge, Ui.wrap());
        }
        s.top.addView(bar, Ui.matchWidth());
        View progress = null;
        if (progressColor != 0) {
            progress = Ui.progress(c, 0, progressColor);
            progress.getLayoutParams().height = Ui.dp(c, 8);
            Ui.add(s.top, progress, 10);
        }
        return new Page(s, sub, badge, progress);
    }

    /** Rounded tinted card with a small uppercase label, used for the question of each game. */
    static LinearLayout hero(Context c, int bg, int border, String label, int labelColor) {
        LinearLayout card = Ui.column(c);
        card.setBackground(Ui.shape(c, bg, 24, border, 2));
        card.setPadding(Ui.dp(c, 18), Ui.dp(c, 22), Ui.dp(c, 18), Ui.dp(c, 22));
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView l = Ui.text(c, label, 12, 700, labelColor);
        l.setLetterSpacing(0.06f);
        card.addView(l, Ui.wrap());
        return card;
    }

    static TextView centered(Context c, CharSequence s, float sp, int weight, int color) {
        TextView t = Ui.text(c, s, sp, weight, color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    /** Answer button: full width, white with a light edge. */
    static TextView option(Context c, CharSequence text) {
        TextView t = Ui.text(c, text, 16, 700, Ui.INK);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setMinHeight(Ui.dp(c, 56));
        t.setPadding(Ui.dp(c, 16), Ui.dp(c, 12), Ui.dp(c, 16), Ui.dp(c, 12));
        t.setClickable(true);
        t.setFocusable(true);
        style(c, t, Ui.CARD, Ui.LINE, Ui.INK);
        return t;
    }

    static void style(Context c, TextView t, int bg, int border, int color) {
        t.setBackground(Ui.shape(c, bg, 16, border, 2));
        t.setTextColor(color);
    }

    /** The name of a word source: the learner's own words are named here, packs carry their own title. */
    static String sourceTitle(Context c, com.doradodo.app.data.repository.WordSource s) {
        return com.doradodo.app.data.repository.WordSource.LEARNED.equals(s.id)
                ? c.getString(R.string.ent_learned_words) : s.title;
    }

    static TextView label(Context c, String s) {
        TextView t = Ui.text(c, s, 12, 700, Ui.MUTED);
        t.setLetterSpacing(0.06f);
        return t;
    }

    static TextView sectionTitle(Context c, String s) {
        return Ui.text(c, s, 17, 700, Ui.INK);
    }

    /** Flat button with a darker edge below it, like the 3D buttons of the design. */
    static TextView button3d(Context c, String label, int fg, int face, int edge, float radiusDp, float sp) {
        TextView b = Ui.text(c, label, sp, 700, fg);
        b.setGravity(Gravity.CENTER);
        b.setClickable(true);
        b.setFocusable(true);
        style3d(c, b, face, edge, radiusDp);
        b.setPadding(0, 0, 0, Ui.dp(c, 3));
        return b;
    }

    static void style3d(Context c, TextView b, int face, int edge, float radiusDp) {
        GradientDrawable under = Ui.shape(c, edge, radiusDp);
        GradientDrawable top = Ui.shape(c, face, radiusDp);
        LayerDrawable layers = new LayerDrawable(new android.graphics.drawable.Drawable[]{under, top});
        layers.setLayerInset(1, 0, 0, 0, Ui.dp(c, 3));
        b.setBackground(layers);
    }

    /** The standard floating bottom button (see {@link Ui.Screen#setFooter}); {@code edge} is unused. */
    static TextView footerButton(Context c, String label, int fg, int face, int edge) {
        return Ui.floatingButton(c, label, face, fg);
    }

    /** A footer button that cannot be used yet is shown faded. */
    static void setReady(TextView b, boolean ready) {
        b.setAlpha(ready ? 1f : 0.45f);
    }

    /** Same as {@link Ui.Screen#setFooter}: kept so the game screens read the same as every other screen. */
    static void setFooter(Context c, Ui.Screen s, View button) {
        s.setFooter(button);
    }

    /** Large centred text field for typed answers. */
    static EditText input(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setGravity(Gravity.CENTER);
        e.setTextSize(22);
        e.setTextColor(Ui.INK);
        e.setHintTextColor(0xFFA7ABA3);
        Ui.font(e, 700);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
               );
        e.setImeOptions(EditorInfo.IME_ACTION_DONE);
        e.setPadding(Ui.dp(c, 16), 0, Ui.dp(c, 16), 0);
        borderInput(c, e, Ui.BLUE);
        return e;
    }

    static void borderInput(Context c, EditText e, int border) {
        e.setBackground(Ui.shape(c, Ui.CARD, 16, border, 2));
    }

    static LinearLayout.LayoutParams height(Context c, float dp) {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(c, dp));
    }

    /**
     * "ap _ _ _ _": the first letters of the word, then one underscore per remaining letter. Short words (and
     * scripts where one character is a whole syllable, such as Hangul) reveal one letter, longer words two.
     */
    static String hint(String word) {
        int[] cps = word.codePoints().toArray();
        int reveal = cps.length >= 5 ? 2 : 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cps.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.appendCodePoint(i < reveal ? cps[i] : '_');
        }
        return sb.toString();
    }

    static void hideKeyboard(View v) {
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }
}
