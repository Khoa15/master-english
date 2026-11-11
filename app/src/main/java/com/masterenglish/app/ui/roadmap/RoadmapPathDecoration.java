package com.masterenglish.app.ui.roadmap;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.SparseArray;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/** Draws the dashed trail between consecutive nodes as cubic curves. */
public class RoadmapPathDecoration extends RecyclerView.ItemDecoration {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    public RoadmapPathDecoration(float strokePx) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(strokePx);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(Color.parseColor("#E5E5E5"));
        float dash = strokePx * 2.2f;
        paint.setPathEffect(new android.graphics.DashPathEffect(new float[]{dash, dash}, 0f));
    }

    @Override
    public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        SparseArray<View> centers = new SparseArray<>();
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            int position = parent.getChildAdapterPosition(child);
            if (position != RecyclerView.NO_POSITION) {
                centers.put(position, child);
            }
        }
        for (int position = 0; position < centers.size() - 1; position++) {
            View from = centers.get(centers.keyAt(position));
            View to = centers.get(centers.keyAt(position + 1));
            if (from == null || to == null || centers.keyAt(position + 1) != centers.keyAt(position) + 1) {
                continue;
            }
            float x1 = from.getX() + from.getWidth() / 2f;
            float y1 = from.getY() + from.getHeight() / 2f;
            float x2 = to.getX() + to.getWidth() / 2f;
            float y2 = to.getY() + to.getHeight() / 2f;
            float midY = (y1 + y2) / 2f;
            path.reset();
            path.moveTo(x1, y1);
            path.cubicTo(x1, midY, x2, midY, x2, y2);
            canvas.drawPath(path, paint);
        }
    }
}
