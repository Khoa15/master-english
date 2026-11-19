package com.doradodo.app.ui.vocab;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.doradodo.app.R;
import com.doradodo.app.ui.ds.DsFragment;
import com.doradodo.app.ui.ds.Ui;
import com.doradodo.app.ui.vocab.PracticeViewModel.Stage;
import com.doradodo.app.ui.vocab.stage.ChoiceStage;
import com.doradodo.app.ui.vocab.stage.FillBlankStage;
import com.doradodo.app.ui.vocab.stage.FlashcardStage;
import com.doradodo.app.ui.vocab.stage.ListeningStage;
import com.doradodo.app.ui.vocab.stage.ReadingStage;
import com.doradodo.app.ui.vocab.stage.SpeakingStage;
import com.doradodo.app.ui.vocab.stage.StageHost;
import com.doradodo.app.ui.vocab.stage.StageRenderer;
import com.doradodo.app.utils.TextUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/** A practice round: every word goes through Flashcard, Listening, Speaking, Reading, Fill in the blank and Single choice. */
@AndroidEntryPoint
public class PracticeFragment extends DsFragment implements StageHost {

    private static final Stage[] STAGES = Stage.values();

    private final Map<Stage, StageRenderer> renderers = new EnumMap<>(Stage.class);
    {
        renderers.put(Stage.FLASHCARD, new FlashcardStage(this));
        renderers.put(Stage.LISTENING, new ListeningStage(this));
        renderers.put(Stage.SPEAKING, new SpeakingStage(this));
        renderers.put(Stage.READING, new ReadingStage(this));
        renderers.put(Stage.FILL_BLANK, new FillBlankStage(this));
        renderers.put(Stage.CHOICE, new ChoiceStage(this));
    }

    private PracticeViewModel viewModel;
    private Ui.Screen screen;
    private TextView stageLabel;
    private TextView wordLabel;
    private View progress;
    private ImageView leading;
    private TextView action;
    private TextView skip;
    private TextToSpeech tts;
    private boolean ttsReady;
    private ActivityResultLauncher<Intent> speechLauncher;

    private List<VocabViewModel.Word> words = new ArrayList<>();
    private int stage;
    private int index;
    /** The current step has been answered (or revealed), so the button now continues. */
    private boolean answered;
    private TextView feedback;
    private EditText input;
    private LinearLayout optionBox;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        screen = new Ui.Screen(c, 16);
        LinearLayout bar = Ui.row(c);
        leading = new ImageView(c);
        leading.setPadding(Ui.dp(c, 10), Ui.dp(c, 10), Ui.dp(c, 10), Ui.dp(c, 10));
        leading.setBackground(Ui.shape(c, Ui.CARD, 12, Ui.LINE, 1));
        leading.setColorFilter(Ui.INK);
        leading.setOnClickListener(v -> stepBack());
        bar.addView(leading, new LinearLayout.LayoutParams(Ui.dp(c, 40), Ui.dp(c, 40)));
        LinearLayout meta = Ui.column(c);
        LinearLayout labels = Ui.row(c);
        stageLabel = Ui.text(c, "", 12, 600, Ui.MUTED);
        wordLabel = Ui.text(c, "", 12, 600, Ui.MUTED);
        labels.addView(stageLabel, Ui.weight(1f));
        labels.addView(wordLabel, Ui.wrap());
        meta.addView(labels, Ui.matchWidth());
        progress = Ui.progress(c, 0, Ui.GREEN);
        Ui.add(meta, progress, 8);
        LinearLayout.LayoutParams mp = Ui.weight(1f);
        mp.leftMargin = Ui.dp(c, 12);
        bar.addView(meta, mp);
        screen.top.addView(bar, Ui.matchWidth());

        LinearLayout foot = Ui.column(c);
        skip = Ui.ghostLink(c, c.getString(R.string.practice_skip));
        skip.setOnClickListener(v -> skipStep());
        foot.addView(skip, Ui.matchWidth());
        action = Ui.primaryButton(c, c.getString(R.string.practice_next));
        action.setOnClickListener(v -> onAction());
        Ui.add(foot, action, 4);
        screen.setFooter(foot);

        speechLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            if (r.getResultCode() == Activity.RESULT_OK && r.getData() != null) {
                ArrayList<String> heard = r.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                onSpoken(heard);
            }
        });
        return screen.root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(PracticeViewModel.class);
        tts = new TextToSpeech(requireContext().getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS && tts != null) {
                tts.setLanguage(Locale.US);
                ttsReady = true;
            }
        });
        viewModel.words().observe(getViewLifecycleOwner(), list -> {
            if (list == null) {
                return;
            }
            boolean first = words.isEmpty();
            words = list;
            if (first) {
                render();
            }
        });
        String packId = getArguments() == null ? null : getArguments().getString(ARG_PACK);
        viewModel.start(packId);
    }

    @Override
    public void onDestroyView() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        ttsReady = false;
        super.onDestroyView();
    }

    // ----- Flow -------------------------------------------------------------

    private void stepBack() {
        if (stage == 0 && index == 0) {
            back();
            return;
        }
        if (index > 0) {
            index--;
        } else {
            stage--;
            index = words.size() - 1;
        }
        render();
    }

    private void skipStep() {
        if (!answered) {
            viewModel.record(STAGES[stage], words.get(index).id, false);
        }
        advance();
    }

    private void advance() {
        hideKeyboard();
        if (index + 1 < words.size()) {
            index++;
        } else if (stage + 1 < STAGES.length) {
            stage++;
            index = 0;
        } else {
            viewModel.finish();
            nav().navigate(R.id.action_practice_to_summary);
            return;
        }
        render();
    }

    private void onAction() {
        Stage s = STAGES[stage];
        VocabViewModel.Word w = words.get(index);
        if (answered) {
            advance();
            return;
        }
        if (s == Stage.FLASHCARD) {
            advance();
        } else if (s.typed()) {
            checkTyped(s, w);
        } else {
            // Speaking, reading and choice reveal themselves; Next without an answer counts as skipped.
            skipStep();
        }
    }

    private boolean isLastStep() {
        return stage == STAGES.length - 1 && index == words.size() - 1;
    }

    @Override
    public void markAnswered(Stage s, VocabViewModel.Word w, boolean correct, String message) {
        answered = true;
        viewModel.record(s, w.id, correct);
        if (feedback != null) {
            feedback.setVisibility(View.VISIBLE);
            feedback.setText(message);
            feedback.setTextColor(correct ? Ui.GREEN_INK : Ui.RED);
            feedback.setBackground(Ui.shape(requireContext(), correct ? Ui.GREEN_TINT : Ui.RED_TINT, 12));
        }
        action.setText(isLastStep() ? getString(R.string.practice_finish) : getString(R.string.practice_continue));
        skip.setVisibility(View.INVISIBLE);
        hideKeyboard();
    }

    private void checkTyped(Stage s, VocabViewModel.Word w) {
        String typed = TextUtils.normalizeAnswer(input.getText().toString());
        boolean ok = typed.equals(TextUtils.normalizeAnswer(w.term));
        if (typed.isEmpty()) {
            input.setError(getString(R.string.practice_type_first));
            return;
        }
        markAnswered(s, w, ok, ok ? getString(R.string.practice_correct) : getString(R.string.practice_wrong, w.term));
        input.setEnabled(false);
        input.setBackground(Ui.shape(requireContext(), Ui.CARD, 14, ok ? Ui.GREEN : Ui.RED, 1.5f));
    }

    private void onSpoken(@Nullable List<String> heard) {
        if (STAGES[stage] != Stage.SPEAKING || answered) {
            return;
        }
        VocabViewModel.Word w = words.get(index);
        boolean ok = false;
        if (heard != null) {
            for (String h : heard) {
                if (TextUtils.normalizeAnswer(h).contains(TextUtils.normalizeAnswer(w.term))) {
                    ok = true;
                    break;
                }
            }
        }
        markAnswered(Stage.SPEAKING, w, ok, ok ? getString(R.string.practice_great_pron)
                : getString(R.string.practice_heard, heard == null || heard.isEmpty() ? getString(R.string.practice_nothing) : heard.get(0)));
    }

    @Override
    public void speak(String text) {
        speak(text, false);
    }

    @Override
    public void speak(String text, boolean quiet) {
        if (tts != null && ttsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "word");
        } else if (!quiet) {
            Snackbar.make(screen.root, getString(R.string.practice_tts_not_ready), Snackbar.LENGTH_SHORT).show();
        }
    }

    private void hideKeyboard() {
        View f = requireActivity().getCurrentFocus();
        if (f != null) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(f.getWindowToken(), 0);
            f.clearFocus();
        }
    }

    // ----- Rendering --------------------------------------------------------

    private void render() {
        Context c = requireContext();
        if (words.isEmpty()) {
            screen.body.removeAllViews();
            screen.body.addView(Ui.text(c, c.getString(R.string.practice_no_words), 16, 600, Ui.MUTED), Ui.matchWidth());
            action.setText(getString(R.string.vocab_back));
            action.setOnClickListener(v -> back());
            skip.setVisibility(View.GONE);
            return;
        }
        Stage s = STAGES[stage];
        VocabViewModel.Word w = words.get(index);
        answered = false;
        feedback = null;
        input = null;
        optionBox = null;

        leading.setImageResource(stage == 0 && index == 0 ? R.drawable.ic_ds_close : R.drawable.ic_ds_back);
        stageLabel.setText(getString(R.string.practice_stage_label, getString(s.label), stage + 1, STAGES.length));
        wordLabel.setText(getString(R.string.practice_word_label, index + 1, words.size()));
        Ui.setProgress(progress, (stage * words.size() + index + 1) * 100 / (STAGES.length * words.size()));
        skip.setVisibility(s == Stage.FLASHCARD ? View.GONE : View.VISIBLE);
        action.setText(s.typed() ? getString(R.string.practice_check_continue)
                : isLastStep() ? getString(R.string.practice_finish) : getString(R.string.practice_next));
        action.setOnClickListener(v -> onAction());

        LinearLayout body = screen.body;
        body.removeAllViews();
        body.setGravity(Gravity.TOP);
        renderers.get(s).render(c, body, w);
        screen.scroll.scrollTo(0, 0);
    }






    @Override
    public void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        try {
            speechLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Snackbar.make(screen.root, getString(R.string.practice_no_speech), Snackbar.LENGTH_LONG).show();
        }
    }

    // ----- StageHost ---------------------------------------------------------

    @Override
    public PracticeViewModel viewModel() {
        return viewModel;
    }

    @Override
    public boolean isAnswered() {
        return answered;
    }

    @Override
    public void bindInput(EditText field) {
        input = field;
    }

    @Override
    public void bindOptionBox(LinearLayout box) {
        optionBox = box;
    }

    @Override
    public void bindFeedback(TextView view) {
        feedback = view;
    }
}
