package com.masterenglish.app.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.masterenglish.app.data.local.dao.AiDao;
import com.masterenglish.app.data.local.dao.BadgeDao;
import com.masterenglish.app.data.local.dao.ContentDao;
import com.masterenglish.app.data.local.dao.ProgressDao;
import com.masterenglish.app.data.local.dao.UserDao;
import com.masterenglish.app.data.local.entity.AiChatMessageEntity;
import com.masterenglish.app.data.local.entity.AiChatSessionEntity;
import com.masterenglish.app.data.local.entity.AiPersonaMemoryEntity;
import com.masterenglish.app.data.local.entity.BadgeEntity;
import com.masterenglish.app.data.local.entity.DailyStudyLogEntity;
import com.masterenglish.app.data.local.entity.GrammarEntity;
import com.masterenglish.app.data.local.entity.LessonQuestionEntity;
import com.masterenglish.app.data.local.entity.ListeningEntity;
import com.masterenglish.app.data.local.entity.ReadingEntity;
import com.masterenglish.app.data.local.entity.RoadmapNodeEntity;
import com.masterenglish.app.data.local.entity.RoadmapStageEntity;
import com.masterenglish.app.data.local.entity.UserBadgeEntity;
import com.masterenglish.app.data.local.entity.UserEntity;
import com.masterenglish.app.data.local.entity.UserItemRetentionEntity;
import com.masterenglish.app.data.local.entity.UserNodeProgressEntity;
import com.masterenglish.app.data.local.entity.UserSettingsEntity;
import com.masterenglish.app.data.local.entity.UserWeaknessEntity;
import com.masterenglish.app.data.local.entity.VocabularyEntity;

/**
 * Offline-first local store. The MySQL server is the long-term source of truth;
 * the app always reads from here and syncs in the background via WorkManager.
 */
@Database(
        entities = {
                UserEntity.class,
                UserSettingsEntity.class,
                RoadmapStageEntity.class,
                RoadmapNodeEntity.class,
                VocabularyEntity.class,
                GrammarEntity.class,
                ReadingEntity.class,
                ListeningEntity.class,
                LessonQuestionEntity.class,
                UserNodeProgressEntity.class,
                UserItemRetentionEntity.class,
                UserWeaknessEntity.class,
                AiPersonaMemoryEntity.class,
                AiChatSessionEntity.class,
                AiChatMessageEntity.class,
                BadgeEntity.class,
                UserBadgeEntity.class,
                DailyStudyLogEntity.class
        },
        version = 1,
        exportSchema = false)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    public static final String NAME = "master_english.db";

    public abstract UserDao userDao();

    public abstract ContentDao contentDao();

    public abstract ProgressDao progressDao();

    public abstract AiDao aiDao();

    public abstract BadgeDao badgeDao();
}
