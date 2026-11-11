package com.masterenglish.app.ui.common;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;

/**
 * Loading placeholder for a tab: grey blocks in the shape of the real screen with a soft light sweep.
 * It draws everything itself (no child views), and its animator only runs while it is visible.
 */
public final class SkeletonView extends View {

    public enum Variant { ROADMAP, SECTIONS, GRID, PROFILE }

    private static final int PAPER = 0xFFF6F4EE;
    private static final int BASE = 0xFFEAE7DE;
    private static final int HIGHLIGHT = 0xFFF8F6F1;

    private final Variant variant;
    private final float d;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Matrix matrix = new Matrix();
    private LinearGradient shader;
    private ValueAnimator animator;
    private float phase;

    public SkeletonView(Context context, Variant variant) {
        super(context);
        this.variant = variant;
        this.d = context.getResources().getDisplayMetrics().density;
        // Opaque: the skeleton fully covers the (empty) tab content underneath.
        setClickable(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        shader = new LinearGradient(0, 0, w * 0.6f, 0, new int[]{BASE, HIGHLIGHT, BASE},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP);
        paint.setShader(shader);
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        updateAnimator();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateAnimator();
    }

    @Override
    protected void onDetachedFromWindow() {
        stopAnimator();
        super.onDetachedFromWindow();
    }

    private void updateAnimator() {
        if (isShown() && isAttachedToWindow()) {
            if (animator == null) {
                animator = ValueAnimator.ofFloat(0f, 1f);
                animator.setDuration(1300);
                animator.setRepeatCount(ValueAnimator.INFINITE);
                animator.setInterpolator(new LinearInterpolator());
                animator.addUpdateListener(a -> {
                    phase = (float) a.getAnimatedValue();
                    invalidate();
                });
                animator.start();
            }
        } else {
            stopAnimator();
        }
    }

    private void stopAnimator() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    private void block(Canvas c, float l, float t, float w, float h, float radius) {
        rect.set(l * d, t * d, (l + w) * d, (t + h) * d);
        c.drawRoundRect(rect, radius * d, radius * d, paint);
    }

    private void circle(Canvas c, float cx, float cy, float r) {
        c.drawCircle(cx * d, cy * d, r * d, paint);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        canvas.drawColor(PAPER);
        if (shader == null) {
            return;
        }
        float w = getWidth();
        matrix.setTranslate(-w * 0.6f + phase * (w * 1.6f), 0f);
        shader.setLocalMatrix(matrix);

        float wd = w / d;
        float hd = getHeight() / d;
        // Screen title and the round button at the top right, shared by every tab.
        block(canvas, 20, 24, 130, 28, 8);
        circle(canvas, wd - 40, 38, 20);

        switch (variant) {
            case ROADMAP:
                block(canvas, wd / 2 - 70, 100, 140, 28, 14);
                for (int i = 0; i < 6; i++) {
                    float cx = wd / 2 + (float) Math.sin(i * 0.9) * 56f;
                    float cy = 190 + i * 150;
                    if (cy - 45 > hd) {
                        break;
                    }
                    circle(canvas, cx, cy, 44);
                    block(canvas, cx - 60, cy + 56, 120, 14, 7);
                }
                break;
            case SECTIONS:
                block(canvas, 20, 76, wd - 40, 84, 20);
                float y = 184;
                for (int s = 0; s < 3 && y < hd; s++) {
                    block(canvas, 20, y, 120, 18, 9);
                    y += 34;
                    for (int r = 0; r < 3 && y < hd; r++) {
                        block(canvas, 20, y, wd - 40, 62, 16);
                        y += 74;
                    }
                    y += 10;
                }
                break;
            case GRID:
                block(canvas, 20, 76, 120, 36, 18);
                block(canvas, 150, 76, 130, 36, 18);
                block(canvas, 20, 140, 170, 22, 10);
                float cw = (wd - 40 - 12) / 2f;
                for (int row = 0; row < 4; row++) {
                    float top = 180 + row * 138;
                    if (top > hd) {
                        break;
                    }
                    block(canvas, 20, top, cw, 126, 18);
                    block(canvas, 20 + cw + 12, top, cw, 126, 18);
                }
                break;
            case PROFILE:
                circle(canvas, 56, 120, 36);
                block(canvas, 108, 100, 150, 20, 10);
                block(canvas, 108, 130, 110, 14, 7);
                float sw = (wd - 40 - 24) / 3f;
                for (int i = 0; i < 3; i++) {
                    block(canvas, 20 + i * (sw + 12), 180, sw, 64, 16);
                }
                block(canvas, 20, 268, wd - 40, 150, 20);
                float bs = (wd - 40 - 24) / 3f;
                for (int row = 0; row < 3; row++) {
                    float top = 440 + row * (bs + 12);
                    if (top > hd) {
                        break;
                    }
                    for (int i = 0; i < 3; i++) {
                        block(canvas, 20 + i * (bs + 12), top, bs, bs, 18);
                    }
                }
                break;
            default:
                break;
        }
    }
}
