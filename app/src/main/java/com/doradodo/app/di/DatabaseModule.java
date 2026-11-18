package com.doradodo.app.di;

import android.content.Context;

import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.doradodo.app.data.local.AppDatabase;
import com.doradodo.app.data.local.dao.BadgeDao;
import com.doradodo.app.data.local.dao.EntertainmentDao;
import com.doradodo.app.data.local.dao.WordDao;
import com.doradodo.app.data.local.dao.ProgressDao;
import com.doradodo.app.data.local.dao.UserDao;

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

    /**
     * Adds the Entertainment tables: the activity catalogue, plays, carousel, events, contests (with packs, prizes,
     * rules and registrations) and the language-neutral synonym groups. Nothing existing is touched; the
     * catalogue is loaded by EntertainmentSeeder.
     */
    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS `ent_activities` (`id` TEXT NOT NULL, `section` TEXT NOT NULL, `code` TEXT NOT NULL, `title` TEXT NOT NULL, `subtitle` TEXT, `image` TEXT, `accent` TEXT NOT NULL, `position` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, PRIMARY KEY(`id`))");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ent_activities_code` ON `ent_activities` (`code`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ent_activities_section_position` ON `ent_activities` (`section`, `position`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `ent_game_plays` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `user_id` TEXT NOT NULL, `activity_code` TEXT NOT NULL, `played_at` INTEGER NOT NULL, FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ent_game_plays_user_id` ON `ent_game_plays` (`user_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ent_game_plays_activity_code` ON `ent_game_plays` (`activity_code`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `ent_banners` (`id` TEXT NOT NULL, `tag` TEXT NOT NULL, `title` TEXT NOT NULL, `subtitle` TEXT, `button_label` TEXT, `image` TEXT, `accent` TEXT NOT NULL, `action_type` TEXT NOT NULL, `action_target` TEXT, `position` INTEGER NOT NULL, `starts_at` INTEGER, `ends_at` INTEGER, `enabled` INTEGER NOT NULL, PRIMARY KEY(`id`))");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ent_banners_position` ON `ent_banners` (`position`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `ent_events` (`id` TEXT NOT NULL, `tag` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT, `image` TEXT, `accent` TEXT NOT NULL, `starts_at` INTEGER NOT NULL, `ends_at` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, PRIMARY KEY(`id`))");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ent_events_starts_at_ends_at` ON `ent_events` (`starts_at`, `ends_at`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `ent_contests` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT, `image` TEXT, `accent` TEXT NOT NULL, `registration_opens_at` INTEGER NOT NULL, `registration_closes_at` INTEGER NOT NULL, `starts_at` INTEGER NOT NULL, `ends_at` INTEGER NOT NULL, `question_count` INTEGER NOT NULL, `duration_minutes` INTEGER NOT NULL, `enabled` INTEGER NOT NULL, PRIMARY KEY(`id`))");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ent_contests_starts_at_ends_at` ON `ent_contests` (`starts_at`, `ends_at`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `ent_contest_packs` (`contest_id` TEXT NOT NULL, `pack_id` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`contest_id`, `pack_id`), FOREIGN KEY(`contest_id`) REFERENCES `ent_contests`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`pack_id`) REFERENCES `packs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ent_contest_packs_pack_id` ON `ent_contest_packs` (`pack_id`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `ent_contest_prizes` (`contest_id` TEXT NOT NULL, `rank_from` INTEGER NOT NULL, `rank_to` INTEGER, `reward_gems` INTEGER NOT NULL, PRIMARY KEY(`contest_id`, `rank_from`), FOREIGN KEY(`contest_id`) REFERENCES `ent_contests`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE TABLE IF NOT EXISTS `ent_contest_registrations` (`contest_id` TEXT NOT NULL, `user_id` TEXT NOT NULL, `registered_at` INTEGER NOT NULL, PRIMARY KEY(`contest_id`, `user_id`), FOREIGN KEY(`contest_id`) REFERENCES `ent_contests`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ent_contest_registrations_user_id` ON `ent_contest_registrations` (`user_id`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `synonym_groups` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `note` TEXT)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `synonym_members` (`group_id` INTEGER NOT NULL, `lang` TEXT NOT NULL, `text` TEXT NOT NULL, `word_id` TEXT, PRIMARY KEY(`group_id`, `lang`, `text`), FOREIGN KEY(`group_id`) REFERENCES `synonym_groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`word_id`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_synonym_members_lang_text` ON `synonym_members` (`lang`, `text`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_synonym_members_word_id` ON `synonym_members` (`word_id`)");
        }
    };

    /** Drops the contest rules table: the contest page no longer shows rules. */
    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS `ent_contest_rules`");
        }
    };

/**
     * Games are designed per language and synonyms never cross languages. Adds ent_activities.design_for and
     * user_settings.display_language, and rebuilds the synonym tables so the language belongs to the group
     * (synonym_groups.lang) instead of to each member. The synonym rows are catalogue data: EntertainmentSeeder
     * loads them again.
     */
    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE `ent_activities` ADD COLUMN `design_for` TEXT NOT NULL DEFAULT '[\"en\"]'");
            db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `display_language` TEXT NOT NULL DEFAULT 'en'");
            db.execSQL("DROP TABLE IF EXISTS `synonym_members`");
            db.execSQL("DROP TABLE IF EXISTS `synonym_groups`");
            db.execSQL("CREATE TABLE IF NOT EXISTS `synonym_groups` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `lang` TEXT NOT NULL, `note` TEXT)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_synonym_groups_lang` ON `synonym_groups` (`lang`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `synonym_members` (`group_id` INTEGER NOT NULL, `text` TEXT NOT NULL, `word_id` TEXT, PRIMARY KEY(`group_id`, `text`), FOREIGN KEY(`group_id`) REFERENCES `synonym_groups`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`word_id`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_synonym_members_text` ON `synonym_members` (`text`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_synonym_members_word_id` ON `synonym_members` (`word_id`)");
        }
    };

/**
     * Multi-language content. Adds user_settings.learning_language (one language at a time, switchable),
     * words.lang and packs.lang, a levels table that grades each language its own way (words and packs point at it
     * with level_system + level_code instead of a CEFR-only column), word_senses.definition (in the word's own
     * language) in place of definition_en, ent_texts for per-language catalogue text, and an audience language on
     * banners and events.
     * SQLite here cannot rename or drop columns, so the content tables are rebuilt. The learner's word_progress is
     * copied to a backup table first (dropping words cascades to it) and restored with the words afterwards; the
     * senses, translations and pack membership are catalogue data that LocalDataSeeder loads again.
     */
    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("CREATE TABLE `_bak_progress` AS SELECT * FROM `word_progress`");
            db.execSQL("CREATE TABLE `_bak_words` AS SELECT `id`, `term`, `ipa`, `cefr_level`, `rank` FROM `words`");
            db.execSQL("CREATE TABLE `_bak_packs` AS SELECT `id`, `title`, `cefr_level`, `position` FROM `packs`");
            db.execSQL("DROP TABLE IF EXISTS `word_progress`");
            db.execSQL("DROP TABLE IF EXISTS `sense_examples`");
            db.execSQL("DROP TABLE IF EXISTS `example_translations`");
            db.execSQL("DROP TABLE IF EXISTS `sense_translations`");
            db.execSQL("DROP TABLE IF EXISTS `word_senses`");
            db.execSQL("DROP TABLE IF EXISTS `pack_words`");
            db.execSQL("DROP TABLE IF EXISTS `pack_titles`");
            db.execSQL("DROP TABLE IF EXISTS `ent_contest_packs`");
            db.execSQL("DROP TABLE IF EXISTS `words`");
            db.execSQL("DROP TABLE IF EXISTS `packs`");
            db.execSQL("CREATE TABLE IF NOT EXISTS `levels` (`system` TEXT NOT NULL, `code` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`system`, `code`))");
            db.execSQL("CREATE TABLE IF NOT EXISTS `words` (`id` TEXT NOT NULL, `term` TEXT NOT NULL, `ipa` TEXT, `lang` TEXT NOT NULL, `level_system` TEXT NOT NULL, `level_code` TEXT NOT NULL, `rank` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`level_system`, `level_code`) REFERENCES `levels`(`system`, `code`) ON UPDATE NO ACTION ON DELETE NO ACTION )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_words_lang_term` ON `words` (`lang`, `term`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_words_level_system_level_code_rank` ON `words` (`level_system`, `level_code`, `rank`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `packs` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `lang` TEXT NOT NULL, `level_system` TEXT NOT NULL, `level_code` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`level_system`, `level_code`) REFERENCES `levels`(`system`, `code`) ON UPDATE NO ACTION ON DELETE NO ACTION )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_packs_lang` ON `packs` (`lang`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_packs_level_system_level_code` ON `packs` (`level_system`, `level_code`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `word_senses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `word_id` TEXT NOT NULL, `position` INTEGER NOT NULL, `pos` TEXT, `definition` TEXT NOT NULL, FOREIGN KEY(`word_id`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_word_senses_word_id_position` ON `word_senses` (`word_id`, `position`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `sense_translations` (`word_id` TEXT NOT NULL, `sense_position` INTEGER NOT NULL, `lang` TEXT NOT NULL, `text` TEXT NOT NULL, PRIMARY KEY(`word_id`, `sense_position`, `lang`), FOREIGN KEY(`word_id`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE TABLE IF NOT EXISTS `sense_examples` (`word_id` TEXT NOT NULL, `sense_position` INTEGER NOT NULL, `position` INTEGER NOT NULL, `text` TEXT NOT NULL, PRIMARY KEY(`word_id`, `sense_position`, `position`), FOREIGN KEY(`word_id`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE TABLE IF NOT EXISTS `example_translations` (`word_id` TEXT NOT NULL, `sense_position` INTEGER NOT NULL, `example_position` INTEGER NOT NULL, `lang` TEXT NOT NULL, `text` TEXT NOT NULL, PRIMARY KEY(`word_id`, `sense_position`, `example_position`, `lang`), FOREIGN KEY(`word_id`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE TABLE IF NOT EXISTS `pack_titles` (`pack_id` TEXT NOT NULL, `lang` TEXT NOT NULL, `text` TEXT NOT NULL, PRIMARY KEY(`pack_id`, `lang`), FOREIGN KEY(`pack_id`) REFERENCES `packs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE TABLE IF NOT EXISTS `pack_words` (`pack_id` TEXT NOT NULL, `word_id` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`pack_id`, `word_id`), FOREIGN KEY(`pack_id`) REFERENCES `packs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`word_id`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_pack_words_word_id` ON `pack_words` (`word_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_pack_words_pack_id_position` ON `pack_words` (`pack_id`, `position`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `word_progress` (`user_id` TEXT NOT NULL, `word_id` TEXT NOT NULL, `ease_factor` REAL NOT NULL, `repetitions` INTEGER NOT NULL, `interval_days` INTEGER NOT NULL, `next_review_at` INTEGER NOT NULL, `last_reviewed_at` INTEGER NOT NULL, `times_correct` INTEGER NOT NULL, `times_wrong` INTEGER NOT NULL, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`user_id`, `word_id`), FOREIGN KEY(`user_id`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`word_id`) REFERENCES `words`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_word_progress_word_id` ON `word_progress` (`word_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_word_progress_user_id_next_review_at` ON `word_progress` (`user_id`, `next_review_at`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `ent_contest_packs` (`contest_id` TEXT NOT NULL, `pack_id` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`contest_id`, `pack_id`), FOREIGN KEY(`contest_id`) REFERENCES `ent_contests`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`pack_id`) REFERENCES `packs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ent_contest_packs_pack_id` ON `ent_contest_packs` (`pack_id`)");
            db.execSQL("CREATE TABLE IF NOT EXISTS `ent_texts` (`owner_type` TEXT NOT NULL, `owner_id` TEXT NOT NULL, `field` TEXT NOT NULL, `lang` TEXT NOT NULL, `text` TEXT NOT NULL, PRIMARY KEY(`owner_type`, `owner_id`, `field`, `lang`))");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_ent_texts_lang` ON `ent_texts` (`lang`)");
            db.execSQL("INSERT INTO `levels` (`system`, `code`, `position`) VALUES ('CEFR','A1',0),('CEFR','A2',1),('CEFR','B1',2),('CEFR','B2',3),('CEFR','C1',4),('CEFR','C2',5),('THEME','COMMON',6),('THEME','TET',7),('THEME','XMAS',8)");
            db.execSQL("INSERT OR IGNORE INTO `levels` (`system`, `code`, `position`) SELECT 'THEME', `cefr_level`, 100 FROM (SELECT `cefr_level` FROM `_bak_words` UNION SELECT `cefr_level` FROM `_bak_packs`) WHERE `cefr_level` NOT IN ('A1','A2','B1','B2','C1','C2')");
            db.execSQL("INSERT INTO `words` (`id`, `term`, `ipa`, `lang`, `level_system`, `level_code`, `rank`) SELECT `id`, `term`, `ipa`, 'en', CASE WHEN `cefr_level` IN ('A1','A2','B1','B2','C1','C2') THEN 'CEFR' ELSE 'THEME' END, `cefr_level`, `rank` FROM `_bak_words`");
            db.execSQL("INSERT INTO `packs` (`id`, `title`, `lang`, `level_system`, `level_code`, `position`) SELECT `id`, `title`, 'en', CASE WHEN `cefr_level` IN ('A1','A2','B1','B2','C1','C2') THEN 'CEFR' ELSE 'THEME' END, `cefr_level`, `position` FROM `_bak_packs`");
            db.execSQL("INSERT INTO `word_progress` SELECT * FROM `_bak_progress`");
            db.execSQL("DROP TABLE `_bak_progress`");
            db.execSQL("DROP TABLE `_bak_words`");
            db.execSQL("DROP TABLE `_bak_packs`");
            db.execSQL("ALTER TABLE `user_settings` ADD COLUMN `learning_language` TEXT NOT NULL DEFAULT 'en'");
            db.execSQL("ALTER TABLE `ent_banners` ADD COLUMN `audience_lang` TEXT");
            db.execSQL("ALTER TABLE `ent_events` ADD COLUMN `audience_lang` TEXT");
        }
    };

    @Provides
    @Singleton
    static AppDatabase provideDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, AppDatabase.NAME)
                // No destructive fallback: a version bump without a migration must fail loudly in development
                // instead of silently wiping the learner's progress. Add a Migration for every new version.
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
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
    static EntertainmentDao provideEntertainmentDao(AppDatabase database) {
        return database.entertainmentDao();
    }

    @Provides
    static BadgeDao provideBadgeDao(AppDatabase database) {
        return database.badgeDao();
    }

}
