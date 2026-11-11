package com.masterenglish.app.ui.roadmap;

import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;

/**
 * Visual feedback for expanding and collapsing a roadmap node. Only scale, alpha and
 * translationY are animated, never translationX: that property carries the zig-zag offset.
 */
final class RoadmapToggleAnimator {

    private static final long EXPAND_MS = 260L;
    private static final long COLLAPSE_MS = 180L;
    private static final long PULSE_MS = 200L;
    private static final float HIDDEN_SCALE = 0.6f;
    private static final float DROP_DP = 12f;
    private static final float PULSE_SCALE = 1.12f;

    private RoadmapToggleAnimator() {
    }

    /**
     * Shows or hides the action button. With {@code animate} false the button jumps to its
     * final state; with it true the button springs in or fades out.
     */
    static void applyButton(View button, boolean shown, boolean animate) {
        button.animate().cancel();
        if (!animate) {
            resetButton(button);
            button.setVisibility(shown ? View.VISIBLE : View.INVISIBLE);
            return;
        }
        if (shown) {
            expand(button);
        } else {
            collapse(button);
        }
    }

    /** Short scale bump on the node circle, played on every toggle. */
    static void pulse(View circle) {
        circle.animate().cancel();
        circle.animate().scaleX(PULSE_SCALE).scaleY(PULSE_SCALE).setDuration(PULSE_MS / 2)
                .withEndAction(() -> circle.animate().scaleX(1f).scaleY(1f).setDuration(PULSE_MS / 2).start())
                .start();
    }

    private static void expand(View button) {
        float drop = DROP_DP * button.getResources().getDisplayMetrics().density;
        button.setVisibility(View.VISIBLE);
        button.setAlpha(0f);
        button.setScaleX(HIDDEN_SCALE);
        button.setScaleY(HIDDEN_SCALE);
        button.setTranslationY(-drop);
        button.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                .setDuration(EXPAND_MS)
                .setInterpolator(new OvershootInterpolator(1.6f))
                .start();
    }

    private static void collapse(View button) {
        button.animate().alpha(0f).scaleX(HIDDEN_SCALE).scaleY(HIDDEN_SCALE)
                .setDuration(COLLAPSE_MS)
                .setInterpolator(new AccelerateInterpolator())
                .withEndAction(() -> {
                    resetButton(button);
                    button.setVisibility(View.INVISIBLE);
                })
                .start();
    }

    private static void resetButton(View button) {
        button.setAlpha(1f);
        button.setScaleX(1f);
        button.setScaleY(1f);
        button.setTranslationY(0f);
    }
}
