package com.masterenglish.app.ui.common;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;
import com.masterenglish.app.R;

/** Wraps a screen's content in a pull-to-refresh container. */
public final class PullToRefresh {

    private PullToRefresh() {
    }

    /**
     * Wraps {@code content} so a pull from the top refreshes the screen. The pull only starts while
     * {@code scrollTarget} is at its top, so scrolling back up never triggers a refresh.
     */
    public static SwipeRefreshLayout wrap(Context context, View content, View scrollTarget, Runnable onRefresh) {
        SwipeRefreshLayout layout = new SwipeRefreshLayout(context);
        layout.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        layout.setColorSchemeResources(R.color.brand_green_dark);
        layout.setOnChildScrollUpCallback((parent, child) -> scrollTarget.canScrollVertically(-1));
        layout.setOnRefreshListener(onRefresh::run);
        return layout;
    }

    /** Stops the spinner once a refresh finishes. A failed sync is reported with a message. */
    public static void finish(@Nullable SwipeRefreshLayout layout, boolean success, @Nullable View anchor) {
        if (layout != null) {
            layout.setRefreshing(false);
        }
        if (!success && anchor != null) {
            Snackbar.make(anchor, R.string.sync_failed, Snackbar.LENGTH_SHORT).show();
        }
    }
}
