package com.masterenglish.app.ui.vocab;

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
import com.masterenglish.app.R;
import com.masterenglish.app.ui.ds.DsFragment;
import com.masterenglish.app.ui.ds.Ui;
import com.masterenglish.app.ui.vocab.PracticeViewModel.Stage;
import com.masterenglish.app.utils.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

/** A practice round: every word goes through Flashcard, Listening, Speaking, Reading, Fill in the blank and Single choice. */
@AndroidEntryPoint
public class PracticeFragment extends DsFragment {

    private static final Stage[] STAGES = Stage.values();

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
    private int chosen = -1;

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
        skip = Ui.ghostLink(c, "Skip");
        skip.setOnClickListener(v -> skipStep());
        foot.addView(skip, Ui.matchWidth());
        action = Ui.primaryButton(c, "Next");
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
        switch (s) {
            case FLASHCARD:
                advance();
                break;
            case LISTENING:
            case FILL_BLANK:
                checkTyped(s, w);
                break;
            default:
                // Speaking, reading and choice reveal themselves; Next without an answer counts as skipped.
                skipStep();
        }
    }

    private boolean isLastStep() {
        return stage == STAGES.length - 1 && index == words.size() - 1;
    }

    private void markAnswered(Stage s, VocabViewModel.Word w, boolean correct, String message) {
        answered = true;
        viewModel.record(s, w.id, correct);
        if (feedback != null) {
            feedback.setVisibility(View.VISIBLE);
            feedback.setText(message);
            feedback.setTextColor(correct ? Ui.GREEN_INK : Ui.RED);
            feedback.setBackground(Ui.shape(requireContext(), correct ? Ui.GREEN_TINT : Ui.RED_TINT, 12));
        }
        action.setText(isLastStep() ? "Finish round" : "Continue");
        skip.setVisibility(View.INVISIBLE);
        hideKeyboard();
    }

    private void checkTyped(Stage s, VocabViewModel.Word w) {
        String typed = TextUtils.normalizeAnswer(input.getText().toString());
        boolean ok = typed.equals(TextUtils.normalizeAnswer(w.term));
        if (typed.isEmpty()) {
            input.setError("Type your answer first");
            return;
        }
        markAnswered(s, w, ok, ok ? "Correct!" : "Not quite. The answer is “" + w.term + "”");
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
        markAnswered(Stage.SPEAKING, w, ok, ok ? "Great pronunciation!"
                : "We heard “" + (heard == null || heard.isEmpty() ? "nothing" : heard.get(0)) + "”. Try again later.");
    }

    private void speak(String text) {
        speak(text, false);
    }

    private void speak(String text, boolean quiet) {
        if (tts != null && ttsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "word");
        } else if (!quiet) {
            Snackbar.make(screen.root, "Text to speech is not ready yet", Snackbar.LENGTH_SHORT).show();
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
            screen.body.addView(Ui.text(c, "There are no words to practise right now.", 16, 600, Ui.MUTED), Ui.matchWidth());
            action.setText("Back");
            action.setOnClickListener(v -> back());
            skip.setVisibility(View.GONE);
            return;
        }
        Stage s = STAGES[stage];
        VocabViewModel.Word w = words.get(index);
        answered = false;
        chosen = -1;
        feedback = null;
        input = null;
        optionBox = null;

        leading.setImageResource(stage == 0 && index == 0 ? R.drawable.ic_ds_close : R.drawable.ic_ds_back);
        stageLabel.setText(s.label + " · Stage " + (stage + 1) + " of " + STAGES.length);
        wordLabel.setText("Word " + (index + 1) + " of " + words.size());
        Ui.setProgress(progress, (stage * words.size() + index + 1) * 100 / (STAGES.length * words.size()));
        skip.setVisibility(s == Stage.FLASHCARD ? View.GONE : View.VISIBLE);
        action.setText(s == Stage.LISTENING || s == Stage.FILL_BLANK ? "Check & continue"
                : isLastStep() ? "Finish round" : "Next");
        action.setOnClickListener(v -> onAction());

        LinearLayout body = screen.body;
        body.removeAllViews();
        body.setGravity(Gravity.TOP);
        switch (s) {
            case FLASHCARD: flashcard(c, body, w); break;
            case LISTENING: listening(c, body, w); break;
            case SPEAKING: speaking(c, body, w); break;
            case READING: reading(c, body, w); break;
            case FILL_BLANK: fillBlank(c, body, w); break;
            default: choice(c, body, w);
        }
        screen.scroll.scrollTo(0, 0);
    }

    private void heading(Context c, LinearLayout body, String title, String subtitle) {
        body.addView(Ui.text(c, title, 20, 700, Ui.INK), Ui.matchWidth());
        Ui.add(body, Ui.text(c, subtitle, 14, 400, Ui.MUTED), 6);
    }

    private TextView feedbackView(Context c, LinearLayout body) {
        TextView f = Ui.text(c, "", 14, 600, Ui.GREEN_INK);
        f.setPadding(Ui.dp(c, 14), Ui.dp(c, 12), Ui.dp(c, 14), Ui.dp(c, 12));
        f.setVisibility(View.GONE);
        Ui.add(body, f, 16);
        feedback = f;
        return f;
    }

    private void flashcard(Context c, LinearLayout body, VocabViewModel.Word w) {
        LinearLayout card = Ui.column(c);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setBackground(Ui.shape(c, Ui.CARD, 24, Ui.LINE, 1));
        card.setPadding(Ui.dp(c, 24), Ui.dp(c, 32), Ui.dp(c, 24), Ui.dp(c, 32));
        card.setMinimumHeight(Ui.dp(c, 420));
        card.setClickable(true);
        String pos = w.partOfSpeech == null ? "Word" : capitalize(w.partOfSpeech);
        TextView badge = Ui.pill(c, pos, Ui.GREEN_TINT, Ui.GREEN_INK, 12);
        card.addView(badge, Ui.wrap());
        TextView term = Ui.text(c, w.term, 36, 700, Ui.INK);
        term.setGravity(Gravity.CENTER);
        Ui.add(card, term, 36);
        if (w.phonetic != null) {
            TextView ph = Ui.text(c, w.phonetic, 16, 400, Ui.MUTED);
            ph.setGravity(Gravity.CENTER);
            Ui.add(card, ph, 8);
        }
        ImageView sound = Ui.icon(c, R.drawable.ic_ds_volume, 22, Ui.GREEN);
        sound.setPadding(Ui.dp(c, 11), Ui.dp(c, 11), Ui.dp(c, 11), Ui.dp(c, 11));
        sound.setBackground(Ui.shape(c, Ui.GREEN_TINT, 999));
        sound.setContentDescription("Play pronunciation");
        sound.setOnClickListener(v -> speak(w.term));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(Ui.dp(c, 44), Ui.dp(c, 44));
        sp.topMargin = Ui.dp(c, 14);
        card.addView(sound, sp);

        LinearLayout back = Ui.column(c);
        back.setGravity(Gravity.CENTER_HORIZONTAL);
        back.setVisibility(View.GONE);
        View rule = new View(c);
        rule.setBackgroundColor(Ui.LINE);
        back.addView(rule, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        TextView meaning = Ui.text(c, w.meaning, 24, 700, Ui.GREEN_INK);
        meaning.setGravity(Gravity.CENTER);
        Ui.add(back, meaning, 20);
        if (w.translation != null) {
            TextView tr = Ui.text(c, w.translation, 18, 600, Ui.INK);
            tr.setGravity(Gravity.CENTER);
            Ui.add(back, tr, 8);
        }
        if (w.example != null) {
            TextView ex = Ui.text(c, "“" + w.example + "”", 15, 400, Ui.MUTED);
            ex.setGravity(Gravity.CENTER);
            Ui.add(back, ex, 12);
            if (w.exampleTranslation != null) {
                TextView ext = Ui.text(c, w.exampleTranslation, 14, 400, Ui.MUTED);
                ext.setGravity(Gravity.CENTER);
                Ui.add(back, ext, 6);
            }
        }
        Ui.add(card, back, 24);
        TextView hint = Ui.text(c, "Tap the card to see the meaning", 13, 400, Ui.MUTED);
        hint.setGravity(Gravity.CENTER);
        card.setOnClickListener(v -> {
            boolean show = back.getVisibility() != View.VISIBLE;
            back.setVisibility(show ? View.VISIBLE : View.GONE);
            hint.setVisibility(show ? View.GONE : View.VISIBLE);
        });
        body.addView(card, Ui.matchWidth());
        Ui.add(body, hint, 14);
        speak(w.term, true);
    }

    private void listening(Context c, LinearLayout body, VocabViewModel.Word w) {
        heading(c, body, "Listen and type the word", "Tap play as many times as you need");
        ImageView icon = Ui.icon(c, R.drawable.ic_ds_play, 36, Color.WHITE);
        FrameLayout circle = new FrameLayout(c);
        circle.setBackground(Ui.shape(c, Ui.GREEN, 999));
        circle.setElevation(Ui.dp(c, 4));
        circle.setClickable(true);
        circle.setContentDescription("Play audio");
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(Ui.dp(c, 36), Ui.dp(c, 36), Gravity.CENTER);
        circle.addView(icon, ip);
        circle.setOnClickListener(v -> speak(w.term));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(Ui.dp(c, 96), Ui.dp(c, 96));
        cp.gravity = Gravity.CENTER_HORIZONTAL;
        cp.topMargin = Ui.dp(c, 32);
        body.addView(circle, cp);
        label(c, body, "Your answer");
        input = editor(c, "Type the word");
        Ui.add(body, input, 8);
        Ui.add(body, Ui.text(c, "Hint: " + w.term.length() + " letters, starts with “"
                + w.term.charAt(0) + "”", 12, 400, Ui.MUTED), 8);
        feedbackView(c, body);
        speak(w.term, true);
    }

    private void speaking(Context c, LinearLayout body, VocabViewModel.Word w) {
        heading(c, body, "Say the word out loud", "Listen to the model, then repeat");
        TextView term = Ui.text(c, w.term, 36, 700, Ui.INK);
        term.setGravity(Gravity.CENTER);
        Ui.add(body, term, 32);
        if (w.phonetic != null) {
            TextView ph = Ui.text(c, w.phonetic, 16, 400, Ui.MUTED);
            ph.setGravity(Gravity.CENTER);
            Ui.add(body, ph, 6);
        }
        TextView listen = Ui.pill(c, "Listen", Ui.GREEN_TINT, Ui.GREEN_INK, 13);
        listen.setOnClickListener(v -> speak(w.term));
        LinearLayout.LayoutParams lp = Ui.wrap();
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.topMargin = Ui.dp(c, 14);
        body.addView(listen, lp);
        FrameLayout mic = new FrameLayout(c);
        mic.setBackground(Ui.shape(c, Ui.GREEN, 999));
        mic.setElevation(Ui.dp(c, 4));
        mic.setClickable(true);
        mic.setContentDescription("Start recording");
        mic.addView(Ui.icon(c, R.drawable.ic_ds_mic, 36, Color.WHITE),
                new FrameLayout.LayoutParams(Ui.dp(c, 36), Ui.dp(c, 36), Gravity.CENTER));
        mic.setOnClickListener(v -> startListening());
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(Ui.dp(c, 96), Ui.dp(c, 96));
        mp.gravity = Gravity.CENTER_HORIZONTAL;
        mp.topMargin = Ui.dp(c, 36);
        body.addView(mic, mp);
        TextView tap = Ui.text(c, "Tap to speak", 14, 600, Ui.MUTED);
        tap.setGravity(Gravity.CENTER);
        Ui.add(body, tap, 12);
        feedbackView(c, body);
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        try {
            speechLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            Snackbar.make(screen.root, "Speech recognition is not available on this device", Snackbar.LENGTH_LONG).show();
        }
    }

    private void reading(Context c, LinearLayout body, VocabViewModel.Word w) {
        boolean hasExample = w.example != null;
        heading(c, body, "Read and answer", hasExample ? "Read the short text, then answer the question"
                : "Read the meaning, then pick the word");
        TextView passage = Ui.text(c, "", 17, 400, Ui.INK);
        passage.setLineSpacing(Ui.dp(c, 6), 1f);
        // The Oxford lists have no example sentences: the passage is then the definition and the answers are words.
        String text = hasExample ? w.example : w.meaning;
        SpannableString span = new SpannableString(text);
        int at = hasExample ? text.toLowerCase().indexOf(w.term.toLowerCase()) : -1;
        if (at >= 0) {
            span.setSpan(new ForegroundColorSpan(Ui.GREEN), at, at + w.term.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            span.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), at, at + w.term.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        passage.setText(span);
        passage.setBackground(Ui.shape(c, Ui.CARD, 18, Ui.LINE, 1));
        passage.setPadding(Ui.dp(c, 18), Ui.dp(c, 18), Ui.dp(c, 18), Ui.dp(c, 18));
        Ui.add(body, passage, 20);
        Ui.add(body, Ui.text(c, hasExample ? "What does \u201c" + w.term + "\u201d mean in the text?"
                : "Which word matches this meaning?", 16, 700, Ui.INK), 20);
        optionBox = Ui.column(c);
        Ui.add(body, optionBox, 12);
        if (hasExample) {
            options(c, viewModel.meanings(w, 2), w.meaning, w, Stage.READING);
        } else {
            options(c, viewModel.terms(w, 2), w.term, w, Stage.READING);
        }
        feedbackView(c, body);
    }

    private void fillBlank(Context c, LinearLayout body, VocabViewModel.Word w) {
        heading(c, body, "Fill in the blank", "Type the missing word");
        String[] parts = PracticeViewModel.splitExample(w);
        TextView sentence = Ui.text(c, "", 18, 400, Ui.INK);
        sentence.setLineSpacing(Ui.dp(c, 8), 1f);
        if (parts != null) {
            sentence.setText(parts[0] + "_______" + parts[2]);
        } else {
            sentence.setText("_______  =  " + w.meaning);
        }
        sentence.setBackground(Ui.shape(c, Ui.CARD, 18, Ui.LINE, 1));
        sentence.setPadding(Ui.dp(c, 20), Ui.dp(c, 20), Ui.dp(c, 20), Ui.dp(c, 20));
        Ui.add(body, sentence, 24);
        label(c, body, "Your answer");
        input = editor(c, "Type the missing word");
        Ui.add(body, input, 8);
        feedbackView(c, body);
    }

    private void choice(Context c, LinearLayout body, VocabViewModel.Word w) {
        heading(c, body, "What does “" + w.term + "” mean?", "Choose one answer");
        optionBox = Ui.column(c);
        Ui.add(body, optionBox, 20);
        options(c, viewModel.meanings(w, 4), w.meaning, w, Stage.CHOICE);
        feedbackView(c, body);
    }

    /** Tappable answer rows; the answer is revealed as soon as one is picked. */
    private void options(Context c, List<String> list, String correct, VocabViewModel.Word w, Stage s) {
        for (int i = 0; i < list.size(); i++) {
            final int pos = i;
            LinearLayout row = Ui.row(c);
            row.setBackground(Ui.shape(c, Ui.CARD, 14, Ui.LINE, 1.5f));
            row.setPadding(Ui.dp(c, 16), Ui.dp(c, 14), Ui.dp(c, 16), Ui.dp(c, 14));
            row.setMinimumHeight(Ui.dp(c, 56));
            row.setClickable(true);
            row.setTag(list.get(i));
            row.addView(Ui.text(c, list.get(i), 16, 600, Ui.INK), Ui.weight(1f));
            row.setOnClickListener(v -> {
                if (answered) {
                    return;
                }
                chosen = pos;
                boolean ok = list.get(pos).equals(correct);
                for (int k = 0; k < optionBox.getChildCount(); k++) {
                    View r = optionBox.getChildAt(k);
                    boolean isRight = correct.equals(r.getTag());
                    boolean isPicked = k == pos;
                    if (isRight) {
                        r.setBackground(Ui.shape(c, Ui.GREEN_TINT, 14, Ui.GREEN, 1.5f));
                    } else if (isPicked) {
                        r.setBackground(Ui.shape(c, Ui.RED_TINT, 14, Ui.RED, 1.5f));
                    }
                }
                markAnswered(s, w, ok, ok ? "Correct!" : "Not quite. The answer is “" + correct + "”");
            });
            Ui.add(optionBox, row, i == 0 ? 0 : 10);
        }
    }

    private void label(Context c, LinearLayout body, String text) {
        Ui.add(body, Ui.text(c, text, 13, 600, Ui.MUTED), 24);
    }

    private EditText editor(Context c, String hint) {
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        e.setTextSize(16);
        e.setTextColor(Ui.INK);
        e.setHintTextColor(0xFF8B948E);
        e.setPadding(Ui.dp(c, 16), 0, Ui.dp(c, 16), 0);
        e.setBackground(Ui.shape(c, Ui.CARD, 14, Ui.LINE, 1.5f));
        e.setMinHeight(Ui.dp(c, 52));
        Ui.font(e, 400);
        e.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int d) { e.setError(null); }
            @Override public void afterTextChanged(Editable s) { }
        });
        e.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(c, 52)));
        return e;
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
