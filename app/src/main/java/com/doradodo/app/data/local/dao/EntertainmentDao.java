package com.doradodo.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Upsert;

import com.doradodo.app.data.local.entity.ActivityEntity;
import com.doradodo.app.data.local.entity.BannerEntity;
import com.doradodo.app.data.local.entity.ContestEntity;
import com.doradodo.app.data.local.entity.ContestPackEntity;
import com.doradodo.app.data.local.entity.ContestPrizeEntity;
import com.doradodo.app.data.local.entity.ContestRegistrationEntity;
import com.doradodo.app.data.local.entity.CountRow;
import com.doradodo.app.data.local.entity.EntTextEntity;
import com.doradodo.app.data.local.entity.EventEntity;
import com.doradodo.app.data.local.entity.GamePlayEntity;
import com.doradodo.app.data.local.entity.GameWord;
import com.doradodo.app.data.local.entity.SynonymGroupEntity;
import com.doradodo.app.data.local.entity.SynonymMemberEntity;
import com.doradodo.app.data.local.entity.SynonymRow;

import java.util.List;

/** Everything the Entertainment page and its games read and write. */
@Dao
public interface EntertainmentDao {

    // ----- Catalogue writes (seeding) --------------------------------------

    @Upsert
    void upsertActivities(List<ActivityEntity> rows);

    @Upsert
    void upsertBanners(List<BannerEntity> rows);

    @Upsert
    void upsertEvents(List<EventEntity> rows);

    /** Upsert, never REPLACE: replacing a contest would cascade-delete its registrations. */
    @Upsert
    void upsertContests(List<ContestEntity> rows);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertContestPacks(List<ContestPackEntity> rows);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertContestPrizes(List<ContestPrizeEntity> rows);

    @Query("DELETE FROM ent_contest_packs")
    void deleteContestPacks();

    @Query("DELETE FROM ent_contest_prizes")
    void deleteContestPrizes();

    @Query("DELETE FROM ent_activities WHERE id NOT IN (:keep)")
    void deleteActivitiesExcept(List<String> keep);

    @Query("DELETE FROM ent_banners WHERE id NOT IN (:keep)")
    void deleteBannersExcept(List<String> keep);

    @Query("DELETE FROM ent_events WHERE id NOT IN (:keep)")
    void deleteEventsExcept(List<String> keep);

    @Query("DELETE FROM ent_contests WHERE id NOT IN (:keep)")
    void deleteContestsExcept(List<String> keep);

    @Insert
    long insertSynonymGroup(SynonymGroupEntity group);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertSynonymMembers(List<SynonymMemberEntity> rows);

    @Query("DELETE FROM synonym_groups")
    void deleteSynonymGroups();

    @Query("SELECT id FROM words WHERE term = :term COLLATE NOCASE LIMIT 1")
    String findWordId(String term);

    // ----- Page reads ------------------------------------------------------

    @Query("SELECT * FROM ent_activities WHERE section = :section AND enabled = 1 ORDER BY position ASC")
    List<ActivityEntity> getActivities(String section);

    @Query("SELECT * FROM ent_banners WHERE enabled = 1 AND (starts_at IS NULL OR starts_at <= :now) "
            + "AND (ends_at IS NULL OR ends_at > :now) ORDER BY position ASC")
    List<BannerEntity> getBanners(long now);

    @Query("SELECT * FROM ent_events WHERE enabled = 1 AND starts_at <= :now AND ends_at > :now "
            + "ORDER BY ends_at ASC LIMIT 1")
    EventEntity getCurrentEvent(long now);

    /** Contests that have not ended yet, the soonest first. */
    @Query("SELECT * FROM ent_contests WHERE enabled = 1 AND ends_at > :now ORDER BY starts_at ASC")
    List<ContestEntity> getOpenContests(long now);

    @Query("SELECT * FROM ent_contests WHERE id = :id LIMIT 1")
    ContestEntity getContest(String id);

    @Query("SELECT * FROM ent_contest_prizes WHERE contest_id = :contestId ORDER BY rank_from ASC")
    List<ContestPrizeEntity> getPrizes(String contestId);

    /** The languages of the packs a contest draws from: the contest is for learners of these. */
    @Query("SELECT DISTINCT p.lang FROM ent_contest_packs cp JOIN packs p ON p.id = cp.pack_id "
            + "WHERE cp.contest_id = :contestId")
    List<String> getContestLanguages(String contestId);

    @Query("SELECT * FROM ent_texts WHERE lang = :lang")
    List<EntTextEntity> getTexts(String lang);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTexts(List<EntTextEntity> rows);

    @Query("DELETE FROM ent_texts")
    void deleteTexts();

    @Query("SELECT pack_id FROM ent_contest_packs WHERE contest_id = :contestId ORDER BY position ASC")
    List<String> getContestPackIds(String contestId);

    // ----- Registrations and plays -----------------------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long register(ContestRegistrationEntity row);

    @Query("SELECT COUNT(*) FROM ent_contest_registrations WHERE contest_id = :contestId AND user_id = :userId")
    int isRegistered(String contestId, String userId);

    @Query("SELECT contest_id AS ref, COUNT(*) AS n FROM ent_contest_registrations GROUP BY contest_id")
    List<CountRow> countRegistrations();

    @Insert
    void insertPlay(GamePlayEntity play);

    @Query("SELECT activity_code AS ref, COUNT(DISTINCT user_id) AS n FROM ent_game_plays GROUP BY activity_code")
    List<CountRow> countLearnersPerActivity();

    // ----- Words for the games ---------------------------------------------

    String WORD_COLUMNS = "SELECT w.id AS id, w.term AS term, s.definition AS definition, t.text AS translation, "
            + "COALESCE(t.text, s.definition) AS meaning, x.text AS example, COALESCE(p.times_wrong, 0) AS timesWrong ";
    String WORD_JOINS = "LEFT JOIN word_senses s ON s.word_id = w.id AND s.position = 0 "
            + "LEFT JOIN sense_translations t ON t.word_id = w.id AND t.sense_position = 0 AND t.lang = :meaningLang "
            + "LEFT JOIN sense_examples x ON x.word_id = w.id AND x.sense_position = 0 AND x.position = 0 ";

    /**
     * Words of {@code wordLang} the learner has studied (a word_progress row), in random order. A word always has
     * a meaning: its translation in {@code meaningLang}, else its definition in its own language.
     * {@code needTranslation} keeps only words that have a translation.
     */
    @Query(WORD_COLUMNS + "FROM word_progress p JOIN words w ON w.id = p.word_id " + WORD_JOINS
            + "WHERE p.user_id = :userId AND w.lang = :wordLang AND (:needTranslation = 0 OR t.text IS NOT NULL) "
            + "AND s.definition IS NOT NULL ORDER BY RANDOM() LIMIT :limit")
    List<GameWord> getLearnedWords(String userId, String wordLang, String meaningLang, boolean needTranslation, int limit);

    /** Words the learner got wrong at least once, the most mistakes first. */
    @Query(WORD_COLUMNS + "FROM word_progress p JOIN words w ON w.id = p.word_id " + WORD_JOINS
            + "WHERE p.user_id = :userId AND w.lang = :wordLang AND p.times_wrong > 0 AND s.definition IS NOT NULL "
            + "ORDER BY p.times_wrong DESC, p.last_reviewed_at DESC LIMIT :limit")
    List<GameWord> getMistakeWords(String userId, String wordLang, String meaningLang, int limit);

    @Query("SELECT COUNT(*) FROM word_progress p JOIN words w ON w.id = p.word_id "
            + "JOIN word_senses s ON s.word_id = p.word_id AND s.position = 0 "
            + "WHERE p.user_id = :userId AND w.lang = :wordLang AND p.times_wrong > 0 AND s.definition != ''")
    int countMistakeWords(String userId, String wordLang);

    /** Words of some packs (a contest draws its questions from them), in random order. */
    @Query(WORD_COLUMNS + "FROM pack_words pw JOIN words w ON w.id = pw.word_id "
            + "LEFT JOIN word_progress p ON p.word_id = w.id AND p.user_id = :userId " + WORD_JOINS
            + "WHERE pw.pack_id IN (:packIds) AND s.definition IS NOT NULL "
            + "GROUP BY w.id ORDER BY RANDOM() LIMIT :limit")
    List<GameWord> getPackWords(String userId, List<String> packIds, String meaningLang, int limit);

    /** A pack's words in list order, for the chanting and sleeping modes. */
    @Query(WORD_COLUMNS + "FROM pack_words pw JOIN words w ON w.id = pw.word_id "
            + "LEFT JOIN word_progress p ON p.word_id = w.id AND p.user_id = :userId " + WORD_JOINS
            + "WHERE pw.pack_id = :packId AND s.definition IS NOT NULL ORDER BY pw.position ASC LIMIT :limit")
    List<GameWord> getPackWordsInOrder(String userId, String packId, String meaningLang, int limit);

    /** Random words of {@code wordLang}, used as wrong answers next to the right one. */
    @Query(WORD_COLUMNS + "FROM words w LEFT JOIN word_progress p ON p.word_id = w.id AND p.user_id = :userId "
            + WORD_JOINS + "WHERE w.lang = :wordLang AND s.definition IS NOT NULL ORDER BY RANDOM() LIMIT :limit")
    List<GameWord> getRandomWords(String userId, String wordLang, String meaningLang, int limit);

    @Query("SELECT COUNT(*) FROM word_progress p JOIN words w ON w.id = p.word_id "
            + "WHERE p.user_id = :userId AND w.lang = :wordLang")
    int countLearned(String userId, String wordLang);

    // ----- Synonyms --------------------------------------------------------

    /** Every member of the synonym groups of one language; the game pairs words of the same group itself. */
    @Query("SELECT m.group_id AS groupId, m.text AS text, "
            + "EXISTS (SELECT 1 FROM word_progress p WHERE p.word_id = m.word_id AND p.user_id = :userId) AS learned "
            + "FROM synonym_members m JOIN synonym_groups g ON g.id = m.group_id WHERE g.lang = :lang")
    List<SynonymRow> getSynonymMembers(String userId, String lang);
}
