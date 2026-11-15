package com.masterenglish.app.data.local;

/**
 * Enumerations shared by entities, DAOs and the UI. Each is persisted by name
 * through {@link Converters}, matching the ENUM columns in db/mysql/schema.sql.
 */
public final class Enums {

    private Enums() {
    }

    public enum BadgeCategory { STREAK, VOCAB_MASTER, GRAMMAR_PRO, AI_CHALLENGER }
}
