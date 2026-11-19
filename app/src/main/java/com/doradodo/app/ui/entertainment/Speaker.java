package com.doradodo.app.ui.entertainment;

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;

/** Thin wrapper over TextToSpeech for the games: reads English words and sentences aloud. */
final class Speaker {

    private TextToSpeech tts;
    private boolean ready;
    private String pending;
    private float pendingRate = 1f;
    private Locale locale = Locale.US;

    Speaker(Context context) {
        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS && tts != null) {
                ready = true;
                tts.setLanguage(locale);
                if (pending != null) {
                    say(pending, pendingRate);
                    pending = null;
                }
            }
        });
    }

    /** Reads in the language being learned (BCP-47 code such as "en" or "ko"). */
    void setLanguage(String lang) {
        locale = lang == null || lang.isEmpty() ? Locale.US : Locale.forLanguageTag(lang);
        if (tts != null && ready) {
            tts.setLanguage(locale);
        }
    }

    void say(String text, float rate) {
        if (tts == null) {
            return;
        }
        if (!ready) {
            pending = text;
            pendingRate = rate;
            return;
        }
        tts.setSpeechRate(rate);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "game");
    }

    void stop() {
        pending = null;
        if (tts != null && ready) {
            tts.stop();
        }
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
