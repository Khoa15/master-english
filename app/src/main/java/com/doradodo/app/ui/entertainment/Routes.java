package com.doradodo.app.ui.entertainment;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.doradodo.app.R;

/** Maps the activity codes stored in the database to the screens that implement them. */
final class Routes {

    static final String ARG_CONTEST = "contestId";

    private Routes() {
    }

    /** The navigation destination of an activity code, or 0 when this build has no such game or mode. */
    static int destination(@Nullable String code) {
        if (code == null) {
            return 0;
        }
        switch (code) {
            case "WORD_MATCH":
                return R.id.wordMatchFragment;
            case "GUESS_WORD":
                return R.id.guessWordFragment;
            case "SYNONYM_MATCH":
                return R.id.synonymMatchFragment;
            case "COPY_SPEED":
                return R.id.copyFragment;
            case "MISTAKES":
                return R.id.mistakesFragment;
            case "TUNG":
                return R.id.tungSetupFragment;
            case "SLEEPING":
                return R.id.sleepingFragment;
            default:
                return 0;
        }
    }

    static Bundle contestArgs(String contestId) {
        Bundle b = new Bundle();
        b.putString(ARG_CONTEST, contestId);
        return b;
    }
}
