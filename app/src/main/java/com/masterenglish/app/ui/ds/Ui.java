package com.masterenglish.app.ui.ds;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.ImageViewCompat;
import android.content.res.ColorStateList;

import com.masterenglish.app.R;

/** Builds the Master English design (390dp phone canvas) from code: palette, text, cards, pills, bars. */
public final class Ui {

    // Palette from the design canvas.
    public static final int PAPER = 0xFFFAF9F5;
    public static final int CARD = 0xFFFFFFFF;
    public static final int INK = 0xFF1E2A24;
    public static final int MUTED = 0xFF5B665F;
    public static final int LINE = 0xFFECE9E1;
    public static final int CHIP = 0xFFF3F1EB;
    public static final int GREEN = 0xFF2E7D5B;
    public static final int GREEN_INK = 0xFF1F5C41;
    public static final int GREEN_TINT = 0xFFDDF0E6;
    public static final int BLUE = 0xFF2D62C4;
    public static final int BLUE_INK = 0xFF214E9C;
    public static final int BLUE_TINT = 0xFFE3ECFA;
    public static final int ORANGE = 0xFFB3541E;
    public static final int ORANGE_INK = 0xFF8A3E14;
    public static final int ORANGE_TINT = 0xFFFBE9DD;
    public static final int AMBER = 0xFFE0A030;
    public static final int AMBER_INK = 0xFF8A5A0B;
    public static final int AMBER_TINT = 0xFFFFF3DC;
    public static final int RED = 0xFFC2412D;
    public static final int RED_TINT = 0xFFFBE4DF;

    /** accent, tint, ink for the three pack colours (green, blue, orange). */
    public static final int[][] TONES = {
            {GREEN, GREEN_TINT, GREEN_INK},
            {BLUE, BLUE_TINT, BLUE_INK},
            {ORANGE, ORANGE_TINT, ORANGE_INK}};

    private Ui() {
    }

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    // ----- Shapes -------------------------------------------------------

    public static GradientDrawable shape(Context c, int fill, float radiusDp, int stroke, float strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(c, radiusDp));
        if (stroke != 0) {
            g.setStroke(Math.max(1, dp(c, strokeDp)), stroke);
        }
        return g;
    }

    public static GradientDrawable shape(Context c, int fill, float radiusDp) {
        return shape(c, fill, radiusDp, 0, 0);
    }

    // ----- Text ---------------------------------------------------------

    public static TextView text(Context c, CharSequence s, float sp, int weight, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        font(t, weight);
        return t;
    }

    /** Inter at the given weight (400 regular, 600 semibold, 700 bold). */
    public static void font(TextView t, int weight) {
        Typeface base = ResourcesCompat.getFont(t.getContext(), R.font.inter);
        if (base != null && android.os.Build.VERSION.SDK_INT >= 28) {
            t.setTypeface(Typeface.create(base, weight, false));
        } else {
            t.setTypeface(base, weight >= 600 ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    public static TextView pill(Context c, CharSequence s, int bg, int fg, float sp) {
        TextView t = text(c, s, sp, 600, fg);
        t.setBackground(shape(c, bg, 999));
        t.setPadding(dp(c, 10), dp(c, 5), dp(c, 10), dp(c, 5));
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    // ----- Containers ---------------------------------------------------

    public static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout card(Context c, float paddingDp, float radiusDp) {
        LinearLayout l = column(c);
        l.setBackground(shape(c, CARD, radiusDp, LINE, 1));
        int p = dp(c, paddingDp);
        l.setPadding(p, p, p, p);
        return l;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
    }

    public static <T extends View> T margin(Context c, T v, ViewGroup.LayoutParams base, float l, float t, float r, float b) {
        ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) base;
        p.setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
        v.setLayoutParams(p);
        return v;
    }

    /** Adds {@code child} to a vertical/horizontal LinearLayout with top margin. */
    public static void add(LinearLayout parent, View child, float topDp) {
        LinearLayout.LayoutParams p = child.getLayoutParams() instanceof LinearLayout.LayoutParams
                ? (LinearLayout.LayoutParams) child.getLayoutParams() : matchWidth();
        p.topMargin = dp(parent.getContext(), topDp);
        parent.addView(child, p);
    }

    public static View space(Context c, float dpH) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(c, dpH)));
        return v;
    }

    public static ImageView icon(Context c, @DrawableRes int res, float sizeDp, int tint) {
        ImageView i = new ImageView(c);
        i.setImageResource(res);
        ImageViewCompat.setImageTintList(i, ColorStateList.valueOf(tint));
        i.setLayoutParams(new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return i;
    }

    /** A rounded track with a coloured fill, {@code percent} 0..100. */
    public static View progress(Context c, int percent, int color) {
        FrameLayout track = new FrameLayout(c);
        track.setBackground(shape(c, LINE, 999));
        View fill = new View(c);
        fill.setBackground(shape(c, color, 999));
        track.addView(fill, new FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT));
        track.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 6)));
        setProgress(track, percent);
        return track;
    }

    /** Sizes the fill of a view made by {@link #progress}. */
    public static void setProgress(View track, int percent) {
        FrameLayout f = (FrameLayout) track;
        View fill = f.getChildAt(0);
        int p = Math.max(0, Math.min(100, percent));
        f.post(() -> {
            ViewGroup.LayoutParams lp = fill.getLayoutParams();
            lp.width = f.getWidth() * p / 100;
            fill.setLayoutParams(lp);
        });
    }

    // ----- Buttons ------------------------------------------------------

    public static TextView primaryButton(Context c, CharSequence label) {
        TextView b = text(c, label, 16, 700, Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setBackground(shape(c, GREEN, 16));
        b.setMinHeight(dp(c, 52));
        b.setClickable(true);
        b.setFocusable(true);
        b.setElevation(dp(c, 3));
        return b;
    }

    public static TextView ghostLink(Context c, CharSequence label) {
        TextView b = text(c, label, 14, 600, MUTED);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(c, 44));
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    public static ImageView backButton(Context c, int iconRes, View.OnClickListener l, CharSequence description) {
        ImageView b = new ImageView(c);
        b.setImageResource(iconRes);
        int pad = dp(c, 10);
        b.setPadding(pad, pad, pad, pad);
        ImageViewCompat.setImageTintList(b, ColorStateList.valueOf(INK));
        b.setBackground(shape(c, CARD, 12, LINE, 1));
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 40), dp(c, 40)));
        b.setContentDescription(description);
        b.setOnClickListener(l);
        return b;
    }

    // ----- Screens ------------------------------------------------------

    /** Paper-coloured screen: top bar area, scrolling body, optional pinned footer. */
    public static final class Screen {
        public final FrameLayout root;
        public final LinearLayout top;
        public final ScrollView scroll;
        public final LinearLayout body;
        public final FrameLayout footer;

        public Screen(Context c, int bottomInsetDp) {
            root = new FrameLayout(c);
            root.setBackgroundColor(PAPER);
            LinearLayout frame = column(c);
            top = column(c);
            top.setPadding(dp(c, 20), dp(c, 24), dp(c, 20), 0);
            frame.addView(top, matchWidth());
            scroll = new ScrollView(c);
            scroll.setFillViewport(false);
            scroll.setVerticalScrollBarEnabled(false);
            scroll.setClipToPadding(false);
            body = column(c);
            body.setPadding(dp(c, 20), dp(c, 12), dp(c, 20), dp(c, bottomInsetDp));
            scroll.addView(body, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            frame.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            footer = new FrameLayout(c);
            footer.setPadding(dp(c, 20), dp(c, 8), dp(c, 20), dp(c, 24));
            frame.addView(footer, matchWidth());
            footer.setVisibility(View.GONE);
            root.addView(frame, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        public void setFooter(View v) {
            footer.removeAllViews();
            footer.addView(v, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            footer.setVisibility(View.VISIBLE);
        }
    }

    /** Back button, title and optional subtitle on one row. */
    public static LinearLayout titleBar(Context c, View.OnClickListener onBack, CharSequence title,
                                        @Nullable CharSequence subtitle) {
        LinearLayout bar = row(c);
        bar.addView(backButton(c, R.drawable.ic_ds_back, onBack, c.getString(android.R.string.cancel)));
        LinearLayout titles = column(c);
        titles.addView(text(c, title, 22, 700, INK));
        if (subtitle != null) {
            add(titles, text(c, subtitle, 12, 400, MUTED), 2);
        }
        LinearLayout.LayoutParams p = weight(1f);
        p.leftMargin = dp(c, 12);
        bar.addView(titles, p);
        return bar;
    }

    /** Segmented control; the selected tab is a white pill. */
    public static LinearLayout segmented(Context c, String[] labels, int selected, int selectedColor,
                                         java.util.function.IntConsumer onSelect) {
        LinearLayout box = row(c);
        box.setBackground(shape(c, LINE, 14));
        box.setPadding(dp(c, 4), dp(c, 4), dp(c, 4), dp(c, 4));
        for (int i = 0; i < labels.length; i++) {
            TextView t = text(c, labels[i], 13, i == selected ? 700 : 600, i == selected ? selectedColor : MUTED);
            t.setGravity(Gravity.CENTER);
            t.setBackground(i == selected ? shape(c, CARD, 11) : null);
            t.setClickable(true);
            final int index = i;
            t.setOnClickListener(v -> onSelect.accept(index));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(c, 36), 1f);
            box.addView(t, p);
        }
        return box;
    }

    /** Status chip text colours: [background, foreground]. */
    public static int[] statusColors(String status) {
        switch (status) {
            case "Learned":
                return new int[]{GREEN_TINT, GREEN_INK};
            case "Review":
                return new int[]{ORANGE_TINT, ORANGE_INK};
            default:
                return new int[]{CHIP, MUTED};
        }
    }
}
