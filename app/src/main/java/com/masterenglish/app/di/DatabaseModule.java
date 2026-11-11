package com.masterenglish.app.di;

import android.content.Context;

import androidx.room.Room;

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

    @Provides
    @Singleton
    static AppDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, AppDatabase.NAME)
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
}
