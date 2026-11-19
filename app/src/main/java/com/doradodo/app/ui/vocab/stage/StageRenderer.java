package com.doradodo.app.ui.vocab.stage;

import android.content.Context;
import android.widget.LinearLayout;

import com.doradodo.app.ui.vocab.VocabViewModel;

/** Builds the screen of one practice stage for one word. */
public interface StageRenderer {
    void render(Context c, LinearLayout body, VocabViewModel.Word w);
}
