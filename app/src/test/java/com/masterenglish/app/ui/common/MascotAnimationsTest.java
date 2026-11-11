package com.masterenglish.app.ui.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieCompositionFactory;
import com.masterenglish.app.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Confirms the generated mascot animations are valid Lottie documents with the expected length. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MascotAnimationsTest {

    private static void assertLoops(int rawRes) {
        Context context = ApplicationProvider.getApplicationContext();
        LottieComposition composition = LottieCompositionFactory.fromRawResSync(context, rawRes).getValue();
        assertNotNull(composition);
        assertEquals(60f, composition.getDurationFrames(), 0.5f);
        assertTrue(composition.getLayers().size() >= 11);
    }

    @Test
    public void celebrateAnimationParses() {
        assertLoops(R.raw.mascot_celebrate);
    }

    @Test
    public void thinkingAnimationParses() {
        assertLoops(R.raw.mascot_thinking);
    }

    @Test
    public void waveAnimationParses() {
        assertLoops(R.raw.mascot_wave);
    }
}
