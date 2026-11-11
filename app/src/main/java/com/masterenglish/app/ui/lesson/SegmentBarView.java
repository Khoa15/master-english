package com.masterenglish.app.ui.lesson;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Lesson progress as separate pills, one per question: green when answered, amber for the current
 * question, grey for the rest. Matches the Greetings lesson artboard.
 */
public class SegmentBarView extends View {

    private static final int GREEN = 0xFF2E7D5B;
    private static final int AMBER = 0xFFE0A030;
    private static final int GREY = 0xFFE9E6DE;
    private static final float GAP_DP = 4f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int total = 1;
    private int done = 0;

    public SegmentBarView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** total: number of questions; done: how many are already answered (the current one is index done). */
    public void setSteps(int total, int done) {
        this.total = Math.max(1, total);
        this.done = Math.max(0, Math.min(done, this.total));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float gap = GAP_DP * density;
        float segment = (getWidth() - gap * (total - 1)) / total;
        float radius = getHeight() / 2f;
        for (int i = 0; i < total; i++) {
            paint.setColor(i < done ? GREEN : i == done && done < total ? AMBER : GREY);
            float left = i * (segment + gap);
            rect.set(left, 0, left + segment, getHeight());
            canvas.drawRoundRect(rect, radius, radius, paint);
        }
    }
}
