package com.masterenglish.app.di;

import android.content.Context;

import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.masterenglish.app.data.local.AppDatabase;
import com.masterenglish.app.data.local.dao.BadgeDao;
import com.masterenglish.app.data.local.dao.WordDao;
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

    /**
     * Replaces the lesson/roadmap content model with words, senses, packs and per-word progress.
     * Users, settings, study logs and unlocked badges are kept; the old content and the progress that
     * pointed at it are dropped, and the words are loaded again by LocalDataSeeder.
     */
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `words` (`id` TEXT NOT NULL, `term` TEXT NOT NULL, `ipa` TEXT, "
                    + "`cefr_level` TEXT NOT NULL, `rank` INTEGER NOT NULL, PRIMARY KEY(`id`))");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_words_term` ON `words` (`term`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_words_cefr_level_rank` ON `words` (`cefr_level`, `rank`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `word_senses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`word_id` TEXT NOT NULL, `position` INTEGER NOT NULL, `pos` TEXT, `definition_en` TEXT NOT NULL, "
                    + "`example_en` TEXT, `definition_vi` TEXT, FOREIGN KEY(`word_id`) REFERENCES `words`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_word_senses_word_id_position` "
                    + "ON `word_senses` (`word_id`, `position`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `packs` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, "
                    + "`cefr_level` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`id`))");
            db.execSQL("CREATE TABLE IF NOT EXISTS `pack_words` (`pack_id` TEXT NOT NULL, `word_id` TEXT NOT NULL, "
                    + "`position` INTEGER NOT NULL, PRIMARY KEY(`pack_id`, `word_id`), "
                    + "FOREIGN KEY(`pack_id`) REFERENCES `packs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , "
                    + "FOREIGN KEY(`word_id`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_pack_words_word_id` ON `pack_words` (`word_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_pack_words_pack_id_position` ON `pack_words` (`pack_id`, `position`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `word_progress` (`user_id` TEXT NOT NULL, `word_id` TEXT NOT NULL, "
                    + "`ease_factor` REAL NOT NULL, `repetitions` INTEGER NOT NULL, `interval_days` INTEGER NOT NULL, "
                    + "`next_review_at` INTEGER NOT NULL, `last_reviewed_at` INTEGER NOT NULL, "
                    + "`times_correct` INTEGER NOT NULL, `times_wrong` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`user_id`, `word_id`), "
                    + "FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , "
                    + "FOREIGN KEY(`word_id`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_word_progress_word_id` ON `word_progress` (`word_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_word_progress_user_id_next_review_at` "
                    + "ON `word_progress` (`user_id`, `next_review_at`)");

            // Children before parents, so foreign keys never point at a dropped table.
            for (String table : new String[]{"ai_chat_messages", "ai_chat_sessions", "ai_persona_memories",
                    "user_weaknesses", "user_item_retention", "user_node_progress", "lesson_questions",
                    "vocabulary_lessons", "grammar_lessons", "reading_lessons", "listening_lessons",
                    "roadmap_nodes", "roadmap_stages"}) {
                db.execSQL("DROP TABLE IF EXISTS `" + table + "`");
            }
            // Badges that depended on the removed grammar and AI features.
            db.execSQL("DELETE FROM `user_badges` WHERE `badge_id` IN ('badge_mistake_hunter', 'badge_grammar_pro')");
            db.execSQL("DELETE FROM `badges` WHERE `id` IN ('badge_mistake_hunter', 'badge_grammar_pro')");
        }
    };

    /**
     * Adds the per-language tables: sense translations, any number of examples per sense with their
     * translations, and pack titles. The Vietnamese meanings already in word_senses are copied over;
     * nothing is dropped (word_senses keeps definition_vi and example_en, which the seeder still fills).
     */
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `sense_translations` (`word_id` TEXT NOT NULL, "
                    + "`sense_position` INTEGER NOT NULL, `lang` TEXT NOT NULL, `text` TEXT NOT NULL, "
                    + "PRIMARY KEY(`word_id`, `sense_position`, `lang`), "
                    + "FOREIGN KEY(`word_id`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE TABLE IF NOT EXISTS `sense_examples` (`word_id` TEXT NOT NULL, "
                    + "`sense_position` INTEGER NOT NULL, `position` INTEGER NOT NULL, `text` TEXT NOT NULL, "
                    + "PRIMARY KEY(`word_id`, `sense_position`, `position`), "
                    + "FOREIGN KEY(`word_id`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE TABLE IF NOT EXISTS `example_translations` (`word_id` TEXT NOT NULL, "
                    + "`sense_position` INTEGER NOT NULL, `example_position` INTEGER NOT NULL, `lang` TEXT NOT NULL, "
                    + "`text` TEXT NOT NULL, PRIMARY KEY(`word_id`, `sense_position`, `example_position`, `lang`), "
                    + "FOREIGN KEY(`word_id`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE TABLE IF NOT EXISTS `pack_titles` (`pack_id` TEXT NOT NULL, `lang` TEXT NOT NULL, "
                    + "`text` TEXT NOT NULL, PRIMARY KEY(`pack_id`, `lang`), "
                    + "FOREIGN KEY(`pack_id`) REFERENCES `packs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("INSERT OR IGNORE INTO `sense_translations` (`word_id`, `sense_position`, `lang`, `text`) "
                    + "SELECT `word_id`, `position`, 'vi', `definition_vi` FROM `word_senses` "
                    + "WHERE `definition_vi` IS NOT NULL AND `definition_vi` != ''");
            db.execSQL("INSERT OR IGNORE INTO `sense_examples` (`word_id`, `sense_position`, `position`, `text`) "
                    + "SELECT `word_id`, `position`, 0, `example_en` FROM `word_senses` "
                    + "WHERE `example_en` IS NOT NULL AND `example_en` != ''");
        }
    };

    @Provides
    @Singleton
    static AppDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, AppDatabase.NAME)
                // No destructive fallback: a version bump without a migration must fail loudly in development
                // instead of silently wiping the learner's progress. Add a Migration for every new version.
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build();
    }

    @Provides
    static UserDao provideUserDao(AppDatabase database) {
        return database.userDao();
    }

    @Provides
    static WordDao provideWordDao(AppDatabase database) {
        return database.wordDao();
    }

    @Provides
    static ProgressDao provideProgressDao(AppDatabase database) {
        return database.progressDao();
    }

    @Provides
    static BadgeDao provideBadgeDao(AppDatabase database) {
        return database.badgeDao();
    }

}
