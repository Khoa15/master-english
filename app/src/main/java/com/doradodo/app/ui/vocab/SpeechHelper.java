package com.doradodo.app.ui.vocab;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/** Text-to-speech for the practice screen: starts up once, speaks when ready, and is released with the screen. */
final class SpeechHelper {

    private TextToSpeech tts;
    private boolean ready;

    SpeechHelper(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS && tts != null) {
                tts.setLanguage(Locale.US);
                ready = true;
            }
        });
    }

    /** Speaks {@code text}; false when text-to-speech is not ready yet. */
    boolean speak(String text) {
        if (tts == null || !ready) {
            return false;
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "word");
        return true;
    }

    void release() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        ready = false;
    }
}
