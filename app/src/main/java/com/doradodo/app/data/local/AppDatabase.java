package com.doradodo.app.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.doradodo.app.data.local.dao.BadgeDao;
import com.doradodo.app.data.local.dao.EntertainmentDao;
import com.doradodo.app.data.local.dao.ProgressDao;
import com.doradodo.app.data.local.dao.UserDao;
import com.doradodo.app.data.local.dao.WordDao;
import com.doradodo.app.data.local.entity.BadgeEntity;
import com.doradodo.app.data.local.entity.ActivityEntity;
import com.doradodo.app.data.local.entity.GamePlayEntity;
import com.doradodo.app.data.local.entity.BannerEntity;
import com.doradodo.app.data.local.entity.EventEntity;
import com.doradodo.app.data.local.entity.ContestEntity;
import com.doradodo.app.data.local.entity.ContestPackEntity;
import com.doradodo.app.data.local.entity.ContestPrizeEntity;
import com.doradodo.app.data.local.entity.ContestRegistrationEntity;
import com.doradodo.app.data.local.entity.SynonymGroupEntity;
import com.doradodo.app.data.local.entity.SynonymMemberEntity;
import com.doradodo.app.data.local.entity.DailyStudyLogEntity;
import com.doradodo.app.data.local.entity.EntTextEntity;
import com.doradodo.app.data.local.entity.ExampleTranslationEntity;
import com.doradodo.app.data.local.entity.LevelEntity;
import com.doradodo.app.data.local.entity.PackEntity;
import com.doradodo.app.data.local.entity.PackTitleEntity;
import com.doradodo.app.data.local.entity.PackWordEntity;
import com.doradodo.app.data.local.entity.SenseExampleEntity;
import com.doradodo.app.data.local.entity.SenseTranslationEntity;
import com.doradodo.app.data.local.entity.UserBadgeEntity;
import com.doradodo.app.data.local.entity.UserEntity;
import com.doradodo.app.data.local.entity.UserSettingsEntity;
import com.doradodo.app.data.local.entity.WordEntity;
import com.doradodo.app.data.local.entity.WordProgressEntity;
import com.doradodo.app.data.local.entity.WordSenseEntity;

/**
 * Offline-first local store. The MySQL server is the long-term source of truth;
 * the app always reads from here and syncs in the background via WorkManager.
 */
@Database(
        entities = {
                UserEntity.class,
                UserSettingsEntity.class,
                WordEntity.class,
                WordSenseEntity.class,
                SenseTranslationEntity.class,
                SenseExampleEntity.class,
                ExampleTranslationEntity.class,
                PackTitleEntity.class,
                PackEntity.class,
                PackWordEntity.class,
                WordProgressEntity.class,
                BadgeEntity.class,
                UserBadgeEntity.class,
                DailyStudyLogEntity.class,
                ActivityEntity.class,
                GamePlayEntity.class,
                BannerEntity.class,
                EventEntity.class,
                ContestEntity.class,
                ContestPackEntity.class,
                ContestPrizeEntity.class,
                ContestRegistrationEntity.class,
                SynonymGroupEntity.class,
                SynonymMemberEntity.class,
                LevelEntity.class,
                EntTextEntity.class
        },
        version = 9,
        exportSchema = true)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    public static final String NAME = "app.db";

    public abstract UserDao userDao();

    public abstract WordDao wordDao();

    public abstract ProgressDao progressDao();

    public abstract BadgeDao badgeDao();

    public abstract EntertainmentDao entertainmentDao();
}
