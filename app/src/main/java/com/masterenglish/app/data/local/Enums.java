package com.masterenglish.app.data.local;

/**
 * Enumerations shared by entities, DAOs and the UI. Each is persisted by name
 * through {@link Converters}, matching the ENUM columns in db/mysql/schema.sql.
 */
public final class Enums {

    private Enums() {
    }

    public enum NodeType { VOCABULARY, GRAMMAR, LISTENING, READING, CHECKPOINT_BOSS }

    public enum NodeStatus { LOCKED, AVAILABLE, COMPLETED, MASTERED }

    public enum SkillType { VOCABULARY, GRAMMAR, LISTENING, READING }

    public enum ParentType { VOCABULARY, GRAMMAR, READING, LISTENING }

    public enum QuestionType { MULTIPLE_CHOICE, FILL_BLANK, MATCHING, REORDER, VOICE_INPUT }

    public enum ItemType { VOCABULARY, GRAMMAR }

    public enum Sender { USER, AI_MASCOT }

    public enum BadgeCategory { STREAK, VOCAB_MASTER, GRAMMAR_PRO, AI_CHALLENGER }
}
