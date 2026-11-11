package com.masterenglish.app.service.ai;

import com.masterenglish.app.data.local.Enums.ItemType;
import com.masterenglish.app.data.local.Enums.SkillType;
import com.masterenglish.app.data.local.entity.LessonQuestionEntity;
import com.masterenglish.app.utils.TextUtils;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Turns a wrong answer into an error tag for user_weaknesses. Pure logic, no I/O.
 *
 * <p>Tags are snake_case and stable across sessions: lesson questions carry
 * their tag in {@code skill_tag}; AI-detected mistakes are normalised with the same slug rules.
 */
@Singleton
public class WeaknessDetector {

    private static final String VOCAB_ID_PREFIX = "vocab_";

    @Inject
    public WeaknessDetector() {
    }

    /** What went wrong, and which item (if any) should be rescheduled. */
    public static final class Detection {
        public final String errorTag;
        public final SkillType skill;
        public final String relatedItemId;
        public final ItemType relatedItemType;

        public Detection(String errorTag, SkillType skill, String relatedItemId, ItemType relatedItemType) {
            this.errorTag = errorTag == null ? "" : errorTag;
            this.skill = skill;
            this.relatedItemId = relatedItemId;
            this.relatedItemType = relatedItemType;
        }
    }

    public Detection detect(LessonQuestionEntity question) {
        String tag = question.skillTag == null || question.skillTag.isEmpty()
                ? TextUtils.slug(question.parentType.name() + "_" + question.questionType.name())
                : question.skillTag;
        switch (question.parentType) {
            case VOCABULARY: {
                boolean wordItem = question.parentId.startsWith(VOCAB_ID_PREFIX);
                return new Detection(tag, SkillType.VOCABULARY,
                        wordItem ? question.parentId : null,
                        wordItem ? ItemType.VOCABULARY : null);
            }
            case GRAMMAR:
                return new Detection(tag, SkillType.GRAMMAR, question.parentId, ItemType.GRAMMAR);
            case LISTENING:
                return new Detection(tag, SkillType.LISTENING, null, null);
            case READING:
            default:
                return new Detection(tag, SkillType.READING, null, null);
        }
    }

    /** Normalises a tag returned by the AI so it matches the lesson tags. */
    public static String normalizeTag(String rawTag) {
        return TextUtils.slug(rawTag);
    }

    public static SkillType skillFrom(String raw, SkillType fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return SkillType.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
