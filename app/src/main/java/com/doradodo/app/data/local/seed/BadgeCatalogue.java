package com.doradodo.app.data.local.seed;

import com.google.gson.JsonParser;
import com.doradodo.app.data.local.Enums.BadgeCategory;
import com.doradodo.app.data.local.entity.BadgeEntity;

import java.util.ArrayList;
import java.util.List;

/** The achievements a learner can unlock. Criteria are evaluated by BadgeEvaluator. */
public final class BadgeCatalogue {

    private BadgeCatalogue() {
    }

    public static List<BadgeEntity> all() {
        List<BadgeEntity> out = new ArrayList<>();
        out.add(badge("badge_streak_30", "Chiến thần 30 ngày Streak",
                "Duy trì chuỗi học 30 ngày liên tiếp.", "ic_badge_streak",
                BadgeCategory.STREAK, "{\"type\":\"STREAK\",\"days\":30}"));
        out.add(badge("badge_vocab_master", "Bậc thầy từ vựng",
                "Ghi nhớ 50 từ vựng với SM-2 (đã ôn đúng liên tiếp từ 3 lần).", "ic_badge_vocab",
                BadgeCategory.VOCAB_MASTER, "{\"type\":\"VOCAB_MASTERED\",\"count\":50}"));
        return out;
    }

    private static BadgeEntity badge(String id, String title, String description, String icon,
                                     BadgeCategory category, String criteriaJson) {
        BadgeEntity b = new BadgeEntity();
        b.id = id;
        b.title = title;
        b.description = description;
        b.iconUrl = icon;
        b.category = category;
        b.criteria = JsonParser.parseString(criteriaJson);
        return b;
    }
}
