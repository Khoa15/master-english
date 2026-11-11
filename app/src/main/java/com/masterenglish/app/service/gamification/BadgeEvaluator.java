package com.masterenglish.app.service.gamification;

import com.google.gson.JsonObject;
import com.masterenglish.app.data.local.Enums.ItemType;
import com.masterenglish.app.data.local.dao.BadgeDao;
import com.masterenglish.app.data.local.dao.ProgressDao;
import com.masterenglish.app.data.local.dao.UserDao;
import com.masterenglish.app.data.local.entity.BadgeEntity;
import com.masterenglish.app.data.local.entity.UserBadgeEntity;
import com.masterenglish.app.data.local.entity.UserEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Checks every badge criterion against the learner's current numbers and unlocks
 * the ones newly met. Criteria are JSON, e.g. {"type":"STREAK","days":30}.
 * Must run on a background thread.
 */
@Singleton
public class BadgeEvaluator {

    private static final int MASTERED_REPETITIONS = 3;
    private static final float FIXED_MASTERY = 0.6f;

    private final BadgeDao badgeDao;
    private final UserDao userDao;
    private final ProgressDao progressDao;

    @Inject
    public BadgeEvaluator(BadgeDao badgeDao, UserDao userDao, ProgressDao progressDao) {
        this.badgeDao = badgeDao;
        this.userDao = userDao;
        this.progressDao = progressDao;
    }

    /** Returns the titles of badges unlocked by this call. */
    public List<String> evaluateNow(String userId) {
        List<String> unlocked = new ArrayList<>();
        UserEntity user = userDao.getUser(userId);
        if (user == null) {
            return unlocked;
        }
        Map<String, Integer> metrics = new HashMap<>();
        metrics.put("STREAK", user.streakCount);
        metrics.put("VOCAB_MASTERED", progressDao.countMastered(userId, ItemType.VOCABULARY, MASTERED_REPETITIONS));
        metrics.put("CORRECTED_MISTAKES", progressDao.countFixedWeaknesses(userId, FIXED_MASTERY));
        metrics.put("GRAMMAR_LESSONS", progressDao.countCompletedGrammarNodes(userId));

        for (BadgeEntity badge : badgeDao.getBadges()) {
            if (!meets(badge.criteria.getAsJsonObject(), metrics)) {
                continue;
            }
            UserBadgeEntity row = new UserBadgeEntity();
            row.id = userId + ":" + badge.id;
            row.userId = userId;
            row.badgeId = badge.id;
            row.unlockedAt = System.currentTimeMillis();
            if (badgeDao.unlock(row) != -1) {
                unlocked.add(badge.title);
            }
        }
        return unlocked;
    }

    private static boolean meets(JsonObject criteria, Map<String, Integer> metrics) {
        String type = criteria.get("type").getAsString();
        Integer current = metrics.get(type);
        if (current == null) {
            return false;
        }
        String threshold = criteria.has("days") ? "days" : "count";
        return current >= criteria.get(threshold).getAsInt();
    }
}
