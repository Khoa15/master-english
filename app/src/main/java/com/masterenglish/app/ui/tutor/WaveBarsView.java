package com.masterenglish.app.ui.tutor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/** Static audio waveform shown inside a voice message bubble, as on the Speaking practice artboard. */
public class WaveBarsView extends View {

    private static final int[] HEIGHTS = {6, 12, 18, 10, 22, 14, 8, 20, 16, 10, 24, 12, 6, 16, 20, 8, 14, 10};
    private static final float BAR_WIDTH_DP = 3f;
    private static final float GAP_DP = 2f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public WaveBarsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(0xFF8A867C);
    }

    public void setBarColor(int color) {
        paint.setColor(color);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float barWidth = BAR_WIDTH_DP * density;
        float gap = GAP_DP * density;
        float scale = getHeight() / 24f;
        float top = 0;
        for (int i = 0; i < HEIGHTS.length; i++) {
            float h = HEIGHTS[i] * scale;
            float left = i * (barWidth + gap);
            float cy = getHeight() / 2f;
            rect.set(left, cy - h / 2f, left + barWidth, cy + h / 2f);
            canvas.drawRoundRect(rect, barWidth / 2f, barWidth / 2f, paint);
            top = Math.max(top, h);
        }
    }
}
