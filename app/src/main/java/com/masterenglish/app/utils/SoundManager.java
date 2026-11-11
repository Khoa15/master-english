package com.masterenglish.app.utils;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import com.masterenglish.app.R;

import java.util.EnumMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/** Low-latency UI sound effects through SoundPool. Honours the "Âm thanh" setting. */
@Singleton
public class SoundManager {

    public enum Effect { DING, BUZZ_ERROR, POP, LEVEL_UP, VICTORY }

    private final SoundPool soundPool;
    private final Map<Effect, Integer> ids = new EnumMap<>(Effect.class);
    private boolean enabled = true;

    @Inject
    public SoundManager(@ApplicationContext Context context) {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attributes).build();
        ids.put(Effect.DING, soundPool.load(context, R.raw.ding, 1));
        ids.put(Effect.BUZZ_ERROR, soundPool.load(context, R.raw.buzz_error, 1));
        ids.put(Effect.POP, soundPool.load(context, R.raw.pop, 1));
        ids.put(Effect.LEVEL_UP, soundPool.load(context, R.raw.level_up, 1));
        ids.put(Effect.VICTORY, soundPool.load(context, R.raw.victory, 1));
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Inverse of {@link #setEnabled(boolean)}, for call sites that think in terms of mute. */
    public void setMuted(boolean muted) {
        this.enabled = !muted;
    }

    public void playCorrect() {
        play(Effect.DING);
    }

    public void playWrong() {
        play(Effect.BUZZ_ERROR);
    }

    public void playVictory() {
        play(Effect.VICTORY);
    }

    public void playTap() {
        play(Effect.POP);
    }

    public void play(Effect effect) {
        Integer id = ids.get(effect);
        if (enabled && id != null) {
            soundPool.play(id, 1f, 1f, 1, 0, 1f);
        }
    }

    public void release() {
        soundPool.release();
    }
}
