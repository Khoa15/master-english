package com.masterenglish.app.ui.redesign;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;

import com.masterenglish.app.databinding.ItemDetailRowBinding;

/** Adds simple "title, subtitle, tag" rows to a vertical container. */
public final class Rows {

    private Rows() {
    }

    public static View add(LinearLayout container, String primary, @Nullable String secondary,
                    @Nullable String tag, @Nullable @ColorInt Integer tagInk, @Nullable @ColorInt Integer tagTint,
                    @Nullable String mark, @Nullable @ColorInt Integer markInk, @Nullable @ColorInt Integer markTint) {
        ItemDetailRowBinding b = ItemDetailRowBinding.inflate(LayoutInflater.from(container.getContext()), container, false);
        b.primary.setText(primary);
        if (secondary != null) {
            b.secondary.setText(secondary);
            b.secondary.setVisibility(View.VISIBLE);
        }
        if (tag != null) {
            b.tag.setText(tag);
            b.tag.setVisibility(View.VISIBLE);
            if (tagInk != null) {
                b.tag.setTextColor(tagInk);
            }
            if (tagTint != null) {
                b.tag.setBackground(pill(tagTint));
            }
        }
        if (mark != null) {
            b.mark.setText(mark);
            b.mark.setVisibility(View.VISIBLE);
            if (markInk != null) {
                b.mark.setTextColor(markInk);
            }
            if (markTint != null) {
                b.mark.setBackground(circle(markTint));
            }
        }
        container.addView(b.getRoot());
        return b.getRoot();
    }

    public static GradientDrawable pill(@ColorInt int color) {
        GradientDrawable d = new GradientDrawable();
        d.setCornerRadius(999f);
        d.setColor(color);
        return d;
    }

    static GradientDrawable circle(@ColorInt int color) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(color);
        return d;
    }

}
