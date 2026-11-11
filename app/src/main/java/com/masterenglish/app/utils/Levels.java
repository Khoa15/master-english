package com.masterenglish.app.utils;

/** Learner tiers derived from experience points: Bronze, Silver, Gold, Diamond. */
public final class Levels {

    public static final int SILVER_XP = 1000;
    public static final int GOLD_XP = 3000;
    public static final int DIAMOND_XP = 7000;

    private Levels() {
    }

    public static String tierName(int experiencePoints) {
        if (experiencePoints >= DIAMOND_XP) {
            return "Diamond";
        }
        if (experiencePoints >= GOLD_XP) {
            return "Gold";
        }
        if (experiencePoints >= SILVER_XP) {
            return "Silver";
        }
        return "Bronze";
    }

    /** Progress 0..100 toward the next tier. */
    public static int progressPercent(int experiencePoints) {
        int floor;
        int ceiling;
        if (experiencePoints >= DIAMOND_XP) {
            return 100;
        } else if (experiencePoints >= GOLD_XP) {
            floor = GOLD_XP;
            ceiling = DIAMOND_XP;
        } else if (experiencePoints >= SILVER_XP) {
            floor = SILVER_XP;
            ceiling = GOLD_XP;
        } else {
            floor = 0;
            ceiling = SILVER_XP;
        }
        return (int) (100.0 * (experiencePoints - floor) / (ceiling - floor));
    }
}
