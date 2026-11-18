package com.doradodo.app.ui.common;

import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

/**
 * A main-tab fragment that stays alive for the whole session. Its view hierarchy is kept, but the content
 * is released while the tab is hidden ({@link #onTabHidden()}) and rebuilt from cached data when it is
 * shown again ({@link #onTabShown()}), so a hidden tab only costs an empty shell in memory.
 */
public abstract class TabFragment extends Fragment {

    private SkeletonView skeleton;

    /** Wraps the screen in a frame with a skeleton on top, shown until the content is bound. */
    protected final View withSkeleton(@NonNull View content, @NonNull SkeletonView.Variant variant) {
        FrameLayout shell = new FrameLayout(content.getContext());
        shell.addView(content, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        skeleton = new SkeletonView(content.getContext(), variant);
        shell.addView(skeleton, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return shell;
    }

    protected final void showSkeleton() {
        if (skeleton != null) {
            skeleton.setVisibility(View.VISIBLE);
        }
    }

    protected final void hideSkeleton() {
        if (skeleton != null) {
            skeleton.setVisibility(View.GONE);
        }
    }

    /** The tab was hidden: drop lists, rows, chart data and show the skeleton again. */
    public abstract void onTabHidden();

    /** The tab was shown: rebuild the content from cached data (the skeleton hides once it is bound). */
    public abstract void onTabShown();

    @Override
    public void onDestroyView() {
        skeleton = null;
        super.onDestroyView();
    }
}
