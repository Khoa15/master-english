package com.masterenglish.app.di;

import android.content.Context;

import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.masterenglish.app.data.local.AppDatabase;
import com.masterenglish.app.data.local.dao.AiDao;
import com.masterenglish.app.data.local.dao.BadgeDao;
import com.masterenglish.app.data.local.dao.ContentDao;
import com.masterenglish.app.data.local.dao.ProgressDao;
import com.masterenglish.app.data.local.dao.UserDao;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class DatabaseModule {

    /** Adds the speaking_scenarios table without dropping existing local data. */
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `speaking_scenarios` ("
                    + "`id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `description` TEXT NOT NULL, "
                    + "`created_at` INTEGER NOT NULL, PRIMARY KEY(`id`), "
                    + "FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_speaking_scenarios_user_id` "
                    + "ON `speaking_scenarios` (`user_id`)");
        }
    };

    /** Drops the speaking_scenarios table: its feature (Tutor / custom practice) has been removed. */
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS `speaking_scenarios`");
        }
    };

    @Provides
    @Singleton
    static AppDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, AppDatabase.NAME)
                // No destructive fallback: a version bump without a migration must fail loudly in development
                // instead of silently wiping the learner's progress. Add a Migration for every new version.
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build();
    }

    @Provides
    static UserDao provideUserDao(AppDatabase database) {
        return database.userDao();
    }

    @Provides
    static ContentDao provideContentDao(AppDatabase database) {
        return database.contentDao();
    }

    @Provides
    static ProgressDao provideProgressDao(AppDatabase database) {
        return database.progressDao();
    }

    @Provides
    static AiDao provideAiDao(AppDatabase database) {
        return database.aiDao();
    }

    @Provides
    static BadgeDao provideBadgeDao(AppDatabase database) {
        return database.badgeDao();
    }

}
