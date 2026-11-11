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
import com.masterenglish.app.data.local.dao.SpeakingScenarioDao;
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

    @Provides
    @Singleton
    static AppDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, AppDatabase.NAME)
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
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

    @Provides
    static SpeakingScenarioDao provideSpeakingScenarioDao(AppDatabase database) {
        return database.speakingScenarioDao();
    }
}
