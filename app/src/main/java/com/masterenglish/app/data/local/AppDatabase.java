package com.masterenglish.app.data.local;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.masterenglish.app.data.local.dao.BadgeDao;
import com.masterenglish.app.data.local.dao.ProgressDao;
import com.masterenglish.app.data.local.dao.UserDao;
import com.masterenglish.app.data.local.dao.WordDao;
import com.masterenglish.app.data.local.entity.BadgeEntity;
import com.masterenglish.app.data.local.entity.DailyStudyLogEntity;
import com.masterenglish.app.data.local.entity.PackEntity;
import com.masterenglish.app.data.local.entity.PackWordEntity;
import com.masterenglish.app.data.local.entity.UserBadgeEntity;
import com.masterenglish.app.data.local.entity.UserEntity;
import com.masterenglish.app.data.local.entity.UserSettingsEntity;
import com.masterenglish.app.data.local.entity.WordEntity;
import com.masterenglish.app.data.local.entity.WordProgressEntity;
import com.masterenglish.app.data.local.entity.WordSenseEntity;

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
                PackEntity.class,
                PackWordEntity.class,
                WordProgressEntity.class,
                BadgeEntity.class,
                UserBadgeEntity.class,
                DailyStudyLogEntity.class
        },
        version = 4,
        exportSchema = true)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {

    public static final String NAME = "master_english.db";

    public abstract UserDao userDao();

    public abstract WordDao wordDao();

    public abstract ProgressDao progressDao();

    public abstract BadgeDao badgeDao();
}
