package com.masterenglish.app.utils;

import android.content.Context;
import android.net.Uri;

/** Maps the "raw/listen_001" style names stored in the database to playable URIs. */
public final class AudioResources {

    private AudioResources() {
    }

    /** Returns android.resource:// URI, or null when the raw resource does not exist. */
    public static Uri resolve(Context context, String rawPath) {
        if (rawPath == null || rawPath.isEmpty()) {
            return null;
        }
        String name = rawPath.startsWith("raw/") ? rawPath.substring(4) : rawPath;
        int id = context.getResources().getIdentifier(name, "raw", context.getPackageName());
        if (id == 0) {
            return null;
        }
        return Uri.parse("android.resource://" + context.getPackageName() + "/" + id);
    }
}
