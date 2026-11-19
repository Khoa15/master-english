package com.doradodo.app.ui.vocab;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.doradodo.app.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PackArtTest {

    @Test
    public void theImageNameFollowsThePackId() {
        assertEquals("art_pack_a1", PackArt.nameFor("A1"));
        assertEquals("art_pack_c2", PackArt.nameFor("C2"));
        assertEquals("art_pack_topik_1", PackArt.nameFor("TOPIK 1"));
        assertEquals("art_pack_hsk_3_b", PackArt.nameFor("HSK-3/b"));
    }

    @Test
    public void everyCefrPackHasItsOwnImage() {
        Context c = ApplicationProvider.getApplicationContext();
        int[] all = new int[6];
        String[] ids = {"A1", "A2", "B1", "B2", "C1", "C2"};
        for (int i = 0; i < ids.length; i++) {
            all[i] = PackArt.resolve(c, ids[i]);
            assertNotEquals(ids[i], R.drawable.art_pack_default, all[i]);
        }
        assertEquals(R.drawable.art_pack_a1, all[0]);
        assertEquals(R.drawable.art_pack_c2, all[5]);
    }

    @Test
    public void aPackWithoutItsOwnImageUsesTheDefault() {
        Context c = ApplicationProvider.getApplicationContext();
        assertEquals(R.drawable.art_pack_default, PackArt.resolve(c, "NO_SUCH_PACK"));
    }
}
