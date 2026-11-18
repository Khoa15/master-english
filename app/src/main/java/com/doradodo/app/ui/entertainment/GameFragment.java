package com.doradodo.app.ui.entertainment;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.doradodo.app.R;
import com.doradodo.app.ui.ds.DsFragment;
import com.doradodo.app.ui.ds.Ui;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Base of the Entertainment screens: loads data off the main thread and builds the "not enough words" state. */
public abstract class GameFragment extends DsFragment {

    private EntertainmentViewModel viewModel;

    protected final EntertainmentViewModel vm() {
        if (viewModel == null) {
            viewModel = new ViewModelProvider(this).get(EntertainmentViewModel.class);
        }
        return viewModel;
    }

    /** Loads on a disk thread and calls {@code done} on the main thread, but only while the screen is still shown. */
    protected final <T> void load(Callable<T> work, Consumer<T> done) {
        vm().async(work, result -> {
            if (getView() != null) {
                done.accept(result);
            }
        });
    }

    /** A card explaining why there is nothing to play yet. */
    protected final View emptyState(Context c, @NonNull String title, @NonNull String message) {
        LinearLayout hero = GameUi.hero(c, Ui.AMBER_TINT, GameUi.AMBER_BORDER, c.getString(R.string.ent_not_enough_data), GameUi.AMBER_EDGE);
        Ui.add(hero, GameUi.centered(c, title, 20, 700, Ui.INK), 10);
        TextView m = GameUi.centered(c, message, 14, 400, Ui.MUTED);
        m.setLineSpacing(0, 1.2f);
        Ui.add(hero, m, 8);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        return hero;
    }

    protected final View loadingState(Context c) {
        TextView t = GameUi.centered(c, c.getString(R.string.ent_loading), 14, 600, Ui.MUTED);
        t.setPadding(0, Ui.dp(c, 40), 0, 0);
        return t;
    }
}
