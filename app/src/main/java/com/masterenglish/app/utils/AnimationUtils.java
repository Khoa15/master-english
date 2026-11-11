package com.masterenglish.app.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ProgressBar;

/** Spring (overshoot) and tween (ease) animations used across the screens. */
public final class AnimationUtils {

    private AnimationUtils() {
    }

    /** Springy pop-in: the view grows past its size and settles back. */
    public static void springPop(View view) {
        view.setScaleX(0.6f);
        view.setScaleY(0.6f);
        view.animate().scaleX(1f).scaleY(1f).setDuration(450)
                .setInterpolator(new OvershootInterpolator(2.2f)).start();
    }

    /** Endless breathing pulse, used for the active roadmap node. */
    public static AnimatorSet pulse(View view) {
        ObjectAnimator sx = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.12f, 1f);
        ObjectAnimator sy = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.12f, 1f);
        for (ObjectAnimator animator : new ObjectAnimator[]{sx, sy}) {
            animator.setDuration(1400);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
        }
        AnimatorSet set = new AnimatorSet();
        set.playTogether(sx, sy);
        set.start();
        return set;
    }

    /** Horizontal shake for a wrong answer. */
    public static void shake(View view) {
        view.animate().cancel();
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, View.TRANSLATION_X,
                0f, 18f, -16f, 12f, -8f, 4f, 0f);
        animator.setDuration(420);
        animator.start();
    }

    /** Tween the progress bar smoothly to a new percentage (0..100). */
    public static void tweenProgress(ProgressBar bar, int target) {
        ValueAnimator animator = ValueAnimator.ofInt(bar.getProgress(), target);
        animator.setDuration(500);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> bar.setProgress((Integer) a.getAnimatedValue()));
        animator.start();
    }

    /** 3D card flip around the Y axis, swapping content at the midpoint through the callback. */
    public static void flip(View view, Runnable atMidpoint) {
        view.setCameraDistance(view.getResources().getDisplayMetrics().density * 8000);
        view.animate().rotationY(90f).setDuration(180).withEndAction(() -> {
            if (atMidpoint != null) {
                atMidpoint.run();
            }
            view.setRotationY(-90f);
            view.animate().rotationY(0f).setDuration(180).start();
        }).start();
    }
}
