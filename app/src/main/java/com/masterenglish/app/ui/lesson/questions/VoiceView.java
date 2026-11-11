package com.masterenglish.app.ui.lesson.questions;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.masterenglish.app.R;
import com.masterenglish.app.data.local.entity.LessonQuestionEntity;
import com.masterenglish.app.ui.lesson.QuestionView;

import java.util.ArrayList;

/**
 * Speaking practice with Android's SpeechRecognizer. A text field is always available
 * as a fallback, so the question still works without a microphone or recognizer.
 */
public final class VoiceView implements QuestionView {

    private final View root;
    private final Context context;
    private final MaterialButton micButton;
    private final TextView recognized;
    private final TextInputEditText typed;
    private final Runnable onChanged;
    private final Runnable onMicNeeded;
    private SpeechRecognizer recognizer;
    private String spoken = "";
    private boolean locked;

    public VoiceView(LayoutInflater inflater, ViewGroup parent, LessonQuestionEntity question,
                     Runnable onChanged, Runnable onMicNeeded) {
        this.onChanged = onChanged;
        this.onMicNeeded = onMicNeeded;
        root = inflater.inflate(R.layout.question_voice, parent, false);
        context = inflater.getContext();
        ((TextView) root.findViewById(R.id.prompt)).setText(question.prompt);
        ((TextView) root.findViewById(R.id.target)).setText(question.correctAnswer.getAsString());
        micButton = root.findViewById(R.id.micButton);
        recognized = root.findViewById(R.id.recognized);
        typed = root.findViewById(R.id.typed);

        micButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                this.onMicNeeded.run();
            }
        });
        typed.addTextChangedListener(new SimpleTextWatcher(() -> onChanged.run()));
    }

    /** Called by the fragment once the microphone permission is granted. */
    public void startListening() {
        if (locked) {
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            recognized.setText(R.string.voice_no_permission);
            return;
        }
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context);
            recognizer.setRecognitionListener(new Listener());
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        recognized.setText(R.string.voice_listening);
        recognizer.startListening(intent);
    }

    private final class Listener implements RecognitionListener {
        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            spoken = matches == null || matches.isEmpty() ? "" : matches.get(0);
            recognized.setText(spoken.isEmpty() ? context.getString(R.string.voice_hint) : spoken);
            onChanged.run();
        }

        @Override
        public void onError(int error) {
            recognized.setText(R.string.voice_hint);
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
        }

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    }

    @Override
    public View getView() {
        return root;
    }

    @Override
    public boolean hasAnswer() {
        return !getAnswer().toString().isEmpty();
    }

    @Override
    public Object getAnswer() {
        String text = typed.getText() == null ? "" : typed.getText().toString().trim();
        return text.isEmpty() ? spoken : text;
    }

    @Override
    public void lock() {
        locked = true;
        micButton.setEnabled(false);
        typed.setEnabled(false);
    }

    @Override
    public void release() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
    }
}
