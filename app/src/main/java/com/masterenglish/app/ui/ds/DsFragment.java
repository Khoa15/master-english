package com.masterenglish.app.ui.ds;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

/** Base for the design screens: navigation shortcuts. */
public abstract class DsFragment extends Fragment {

    public static final String ARG_PACK = "packId";

    protected NavController nav() {
        return NavHostFragment.findNavController(this);
    }

    protected void go(@IdRes int action, @Nullable Bundle args) {
        try {
            nav().navigate(action, args);
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            // Double tap while the previous navigation is still running.
        }
    }

    protected void back() {
        nav().navigateUp();
    }

    protected View.OnClickListener onBack() {
        return v -> back();
    }

    protected static Bundle packArgs(@Nullable String packId) {
        Bundle b = new Bundle();
        if (packId != null) {
            b.putString(ARG_PACK, packId);
        }
        return b;
    }
}
