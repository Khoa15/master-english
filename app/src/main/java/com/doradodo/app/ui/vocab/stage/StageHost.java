package com.doradodo.app.ui.vocab.stage;

import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.doradodo.app.ui.vocab.PracticeViewModel;
import com.doradodo.app.ui.vocab.PracticeViewModel.Stage;
import com.doradodo.app.ui.vocab.VocabViewModel;

/** What a stage needs from the practice screen that shows it: sound, answers and the views it created. */
public interface StageHost {
    PracticeViewModel viewModel();

    void speak(String text);

    /** Speaks without a message when text-to-speech is not ready (used for the automatic read-out). */
    void speak(String text, boolean quiet);

    void startListening();

    boolean isAnswered();

    void markAnswered(Stage stage, VocabViewModel.Word word, boolean correct, String message);

    void bindInput(EditText input);

    void bindOptionBox(LinearLayout optionBox);

    void bindFeedback(TextView feedback);
}
