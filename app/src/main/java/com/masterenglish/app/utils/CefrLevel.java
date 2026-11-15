package com.masterenglish.app.utils;

/** CEFR level estimated from experience points. */
public final class CefrLevel {

    private static final String[] NAMES = {"A1", "A2", "B1", "B2", "C1", "C2"};
    private static final int[] FROM_XP = {0, 300, 800, 1500, 2500, 4000};

    private CefrLevel() {
    }

    private static int index(int xp) {
        int i = 0;
        while (i + 1 < FROM_XP.length && xp >= FROM_XP[i + 1]) {
            i++;
        }
        return i;
    }

    public static String of(int xp) {
        return NAMES[index(xp)];
    }

    /** "Next: B2 at 1,500 XP", or "Top level" at C2. */
    public static String nextLabel(int xp) {
        int i = index(xp);
        if (i + 1 >= NAMES.length) {
            return "Top level";
        }
        return "Next: " + NAMES[i + 1] + " at " + String.format(java.util.Locale.US, "%,d", FROM_XP[i + 1]) + " XP";
    }
}
