package com.masterenglish.app.ui.common;

import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

/** Animated scrolls for lists, so a programmatic jump reads as motion rather than a cut. */
public final class SmoothScroll {

    private SmoothScroll() {
    }

    /** Animates the list until the row at {@code position} sits at the top. */
    public static void toPosition(RecyclerView list, int position) {
        LinearSmoothScroller scroller = new LinearSmoothScroller(list.getContext()) {
            @Override
            protected int getVerticalSnapPreference() {
                return SNAP_TO_START;
            }
        };
        scroller.setTargetPosition(position);
        list.getLayoutManager().startSmoothScroll(scroller);
    }
}
