package com.masterenglish.app.data.repository;

import androidx.annotation.Nullable;

import com.masterenglish.app.data.local.Enums.NodeStatus;
import com.masterenglish.app.data.local.Enums.NodeType;
import com.masterenglish.app.data.local.Enums.SkillType;
import com.masterenglish.app.data.local.entity.RoadmapNodeEntity;
import com.masterenglish.app.data.local.entity.UserNodeProgressEntity;
import com.masterenglish.app.data.local.entity.UserWeaknessEntity;
import com.masterenglish.app.data.local.seed.ContentBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Derives the per-skill numbers shown on Profile and the skill detail screen from the roadmap,
 * the learner's node progress and the weakness tracker. Skills without lessons in the database
 * (speaking, writing) come back with no level and zero totals.
 */
public final class SkillProgress {

    public static final String LISTENING = "listening";
    public static final String READING = "reading";
    public static final String GRAMMAR = "grammar";
    public static final String SPEAKING = "speaking";
    public static final String WRITING = "writing";

    private static final int LIST_LIMIT = 3;

    private SkillProgress() {
    }

    /** One recurring error for a skill. */
    public static final class Mistake {
        public final String tag;
        public final int count;

        Mistake(String tag, int count) {
            this.tag = tag;
            this.count = count;
        }
    }

    /** Everything a skill screen needs. */
    public static final class Summary {
        public final String skillId;
        /** CEFR level of the stage the learner is working in, or null when there are no lessons. */
        @Nullable
        public final String level;
        public final int completed;
        public final int total;
        public final List<String> working;
        public final List<String> focus;
        public final List<Mistake> recent;

        Summary(String skillId, @Nullable String level, int completed, int total, List<String> working,
                List<String> focus, List<Mistake> recent) {
            this.skillId = skillId;
            this.level = level;
            this.completed = completed;
            this.total = total;
            this.working = working;
            this.focus = focus;
            this.recent = recent;
        }

        public int percent() {
            return total == 0 ? 0 : completed * 100 / total;
        }
    }

    /** Builds one summary per skill id. Nodes must be in roadmap order. */
    public static Map<String, Summary> build(List<RoadmapNodeEntity> nodes, List<UserNodeProgressEntity> progress,
                                             List<UserWeaknessEntity> weaknesses) {
        Map<String, UserNodeProgressEntity> byNode = new HashMap<>();
        for (UserNodeProgressEntity p : progress) {
            byNode.put(p.nodeId, p);
        }
        Map<String, Summary> out = new LinkedHashMap<>();
        out.put(LISTENING, summarize(LISTENING, NodeType.LISTENING, SkillType.LISTENING, nodes, byNode, weaknesses));
        out.put(READING, summarize(READING, NodeType.READING, SkillType.READING, nodes, byNode, weaknesses));
        out.put(GRAMMAR, summarize(GRAMMAR, NodeType.GRAMMAR, SkillType.GRAMMAR, nodes, byNode, weaknesses));
        out.put(SPEAKING, summarize(SPEAKING, null, null, nodes, byNode, weaknesses));
        out.put(WRITING, summarize(WRITING, null, null, nodes, byNode, weaknesses));
        return out;
    }

    private static Summary summarize(String skillId, @Nullable NodeType nodeType, @Nullable SkillType skillType,
                                     List<RoadmapNodeEntity> nodes, Map<String, UserNodeProgressEntity> byNode,
                                     List<UserWeaknessEntity> weaknesses) {
        List<RoadmapNodeEntity> lessons = new ArrayList<>();
        for (RoadmapNodeEntity node : nodes) {
            if (nodeType != null && node.nodeType == nodeType) {
                lessons.add(node);
            }
        }

        List<String> working = new ArrayList<>();
        String level = null;
        int completed = 0;
        for (RoadmapNodeEntity node : lessons) {
            UserNodeProgressEntity p = byNode.get(node.id);
            boolean done = p != null && (p.status == NodeStatus.COMPLETED || p.status == NodeStatus.MASTERED);
            if (done) {
                completed++;
                if (working.size() < LIST_LIMIT) {
                    working.add(node.title);
                }
            } else if (level == null) {
                level = levelOf(node);
            }
        }
        if (level == null && !lessons.isEmpty()) {
            level = levelOf(lessons.get(lessons.size() - 1));
        }

        List<String> focus = new ArrayList<>();
        List<Mistake> recent = new ArrayList<>();
        if (skillType != null) {
            List<UserWeaknessEntity> mine = new ArrayList<>();
            for (UserWeaknessEntity w : weaknesses) {
                if (w.skillType == skillType) {
                    mine.add(w);
                }
            }
            for (UserWeaknessEntity w : byMistakes(mine)) {
                if (focus.size() < LIST_LIMIT && w.masteryScore < ProgressRepository.WEAK_THRESHOLD) {
                    focus.add(humanize(w.errorTag));
                }
            }
            Collections.sort(mine, (a, b) -> Long.compare(b.lastFailedAt, a.lastFailedAt));
            for (UserWeaknessEntity w : mine) {
                if (recent.size() < LIST_LIMIT && w.lastFailedAt > 0) {
                    recent.add(new Mistake(humanize(w.errorTag), w.errorCount));
                }
            }
        }
        return new Summary(skillId, level, completed, lessons.size(), working, focus, recent);
    }

    private static List<UserWeaknessEntity> byMistakes(List<UserWeaknessEntity> items) {
        List<UserWeaknessEntity> copy = new ArrayList<>(items);
        copy.sort(Comparator.comparingInt((UserWeaknessEntity w) -> w.errorCount).reversed());
        return copy;
    }

    /** Stages are numbered in CEFR order, one per level, so the stage number picks the level. */
    @Nullable
    private static String levelOf(RoadmapNodeEntity node) {
        int index = node.stageId - 1;
        return index >= 0 && index < ContentBuilder.LEVELS.length ? ContentBuilder.LEVELS[index] : null;
    }

    private static String humanize(String tag) {
        return tag.replace('_', ' ').replace('-', ' ').trim();
    }
}
