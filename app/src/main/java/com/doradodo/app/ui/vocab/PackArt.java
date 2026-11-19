package com.doradodo.app.ui.vocab;

import android.content.Context;

import androidx.annotation.DrawableRes;

import com.doradodo.app.R;

import java.util.Locale;

/**
 * The header picture of a pack card. A pack's picture is the drawable named after its id ({@code art_pack_a1} for
 * pack "A1"), so a new pack gets its own picture by adding a file with that name, with no code change.
 */
public final class PackArt {

    private PackArt() {
    }

    static String nameFor(String packId) {
        return "art_pack_" + packId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
    }

    /** The picture of the pack, or the default one when the pack has none of its own. */
    @DrawableRes
    public static int resolve(Context context, String packId) {
        int own = context.getResources().getIdentifier(nameFor(packId), "drawable", context.getPackageName());
        return own != 0 ? own : R.drawable.art_pack_default;
    }
}
