package com.doradodo.app.ui.analytics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/** Radar (spider) chart: one axis per label, each value is 0..1 along its axis. */
public class RadarChartView extends View {

    private static final int GRID = 0xFFECE9E1;
    private static final int ACCENT = 0xFF2E7D5B;
    private static final int INK = 0xFF1E2A24;

    private float[] values = new float[0];
    private String[] labels = new String[0];
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public RadarChartView(Context context) {
        this(context, null);
    }

    public RadarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float d = getResources().getDisplayMetrics().density;
        gridPaint.setColor(GRID);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(d);
        fillPaint.setColor(ACCENT);
        fillPaint.setAlpha(46);
        fillPaint.setStyle(Paint.Style.FILL);
        strokePaint.setColor(ACCENT);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(2 * d);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        textPaint.setColor(INK);
        textPaint.setTextSize(12 * d);
        textPaint.setFakeBoldText(true);
        dotPaint.setColor(ACCENT);
        dotPaint.setStyle(Paint.Style.FILL);
    }

    /** Sets the values (0..1) and axis labels; both arrays must have the same length. */
    public void setData(float[] values, String[] labels) {
        this.values = values;
        this.labels = labels;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = values.length;
        if (n < 3) {
            return;
        }
        float d = getResources().getDisplayMetrics().density;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = Math.min(getWidth(), getHeight()) / 2f - 34 * d;
        if (radius <= 0) {
            return;
        }
        // Concentric grid polygons at 25, 50, 75 and 100 percent.
        for (int ring = 1; ring <= 4; ring++) {
            Path path = new Path();
            for (int i = 0; i < n; i++) {
                float[] p = point(cx, cy, radius * ring / 4f, i, n);
                if (i == 0) {
                    path.moveTo(p[0], p[1]);
                } else {
                    path.lineTo(p[0], p[1]);
                }
            }
            path.close();
            canvas.drawPath(path, gridPaint);
        }
        for (int i = 0; i < n; i++) {
            float[] p = point(cx, cy, radius, i, n);
            canvas.drawLine(cx, cy, p[0], p[1], gridPaint);
        }
        // Data polygon.
        Path data = new Path();
        for (int i = 0; i < n; i++) {
            float v = Math.max(0f, Math.min(1f, values[i]));
            float[] p = point(cx, cy, radius * v, i, n);
            if (i == 0) {
                data.moveTo(p[0], p[1]);
            } else {
                data.lineTo(p[0], p[1]);
            }
        }
        data.close();
        canvas.drawPath(data, fillPaint);
        canvas.drawPath(data, strokePaint);
        for (int i = 0; i < n; i++) {
            float v = Math.max(0f, Math.min(1f, values[i]));
            float[] p = point(cx, cy, radius * v, i, n);
            canvas.drawCircle(p[0], p[1], 4 * d, dotPaint);
        }
        // Labels with their percentage, pushed outside the outer ring.
        textPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < n; i++) {
            float[] p = point(cx, cy, radius + 22 * d, i, n);
            String text = (labels[i] == null ? "" : labels[i]) + " " + Math.round(values[i] * 100) + "%";
            canvas.drawText(text, p[0], p[1], textPaint);
        }
    }

    /** Point on the axis i of n, starting at the top and going clockwise. */
    private static float[] point(float cx, float cy, float r, int i, int n) {
        double angle = -Math.PI / 2 + 2 * Math.PI * i / n;
        return new float[]{(float) (cx + r * Math.cos(angle)), (float) (cy + r * Math.sin(angle))};
    }
}
