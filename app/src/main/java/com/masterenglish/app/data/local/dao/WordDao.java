package com.masterenglish.app.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Upsert;

import com.masterenglish.app.data.local.entity.ExampleTranslationEntity;
import com.masterenglish.app.data.local.entity.PackEntity;
import com.masterenglish.app.data.local.entity.PackTitleEntity;
import com.masterenglish.app.data.local.entity.PackWordEntity;
import com.masterenglish.app.data.local.entity.SenseExampleEntity;
import com.masterenglish.app.data.local.entity.SenseTranslationEntity;
import com.masterenglish.app.data.local.entity.WordEntity;
import com.masterenglish.app.data.local.entity.WordRow;
import com.masterenglish.app.data.local.entity.WordSenseEntity;

import java.util.List;

/** Read/write access to the word content: words, their senses, packs and pack membership. */
@Dao
public interface WordDao {

    /** Upsert (not REPLACE): replacing a word would cascade-delete the learners' progress for it. */
    @Upsert
    void upsertWords(List<WordEntity> words);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSenses(List<WordSenseEntity> senses);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSenseTranslations(List<SenseTranslationEntity> rows);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertSenseExamples(List<SenseExampleEntity> rows);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertExampleTranslations(List<ExampleTranslationEntity> rows);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPackTitles(List<PackTitleEntity> rows);

    @Upsert
    void upsertPacks(List<PackEntity> packs);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPackWords(List<PackWordEntity> links);

    /** Senses and pack membership are rebuilt on every content load. */
    @Query("DELETE FROM word_senses")
    void deleteSenses();

    @Query("DELETE FROM pack_words")
    void deletePackWords();

    /** The per-language content is rebuilt on every content load, like the senses. */
    @Query("DELETE FROM sense_translations")
    void deleteSenseTranslations();

    @Query("DELETE FROM sense_examples")
    void deleteSenseExamples();

    @Query("DELETE FROM example_translations")
    void deleteExampleTranslations();

    @Query("DELETE FROM pack_titles")
    void deletePackTitles();

    /** Deleting words and packs cascades to senses, pack_words and every learner's word_progress. */
    @Query("DELETE FROM words WHERE id IN (:ids)")
    void deleteWords(List<String> ids);

    @Query("DELETE FROM words")
    void deleteAllWords();

    @Query("DELETE FROM packs WHERE id IN (:ids)")
    void deletePacks(List<String> ids);

    @Query("SELECT id FROM words")
    List<String> getWordIds();

    @Query("SELECT id FROM packs")
    List<String> getPackIds();

    @Query("SELECT COUNT(*) FROM words")
    int countWords();

    @Query("SELECT COUNT(*) FROM packs")
    int countPacks();

    @Query("SELECT COUNT(*) FROM word_senses")
    int countSenses();

    @Query("SELECT COUNT(*) FROM pack_words")
    int countPackWords();

    /**
     * Every word with its first sense, level by level in list order. {@code lang} picks the translation of the
     * meaning and of the first example; a word without one in that language gets null (the English text stays).
     */
    @Query("SELECT w.id AS id, w.term AS term, w.ipa AS ipa, w.cefr_level AS cefrLevel, w.rank AS rank, "
            + "s.pos AS pos, s.definition_en AS definition, "
            + "COALESCE(x.text, s.example_en) AS example, t.text AS translation, xt.text AS exampleTranslation "
            + "FROM words w LEFT JOIN word_senses s ON s.word_id = w.id AND s.position = 0 "
            + "LEFT JOIN sense_translations t ON t.word_id = w.id AND t.sense_position = 0 AND t.lang = :lang "
            + "LEFT JOIN sense_examples x ON x.word_id = w.id AND x.sense_position = 0 AND x.position = 0 "
            + "LEFT JOIN example_translations xt ON xt.word_id = w.id AND xt.sense_position = 0 "
            + "AND xt.example_position = 0 AND xt.lang = :lang "
            + "ORDER BY w.cefr_level ASC, w.rank ASC")
    List<WordRow> getWordRows(String lang);

    @Query("SELECT * FROM packs ORDER BY position ASC")
    List<PackEntity> getPacks();

    @Query("SELECT * FROM pack_words ORDER BY pack_id ASC, position ASC")
    List<PackWordEntity> getPackWords();

    @Query("SELECT * FROM word_senses WHERE word_id = :wordId ORDER BY position ASC")
    List<WordSenseEntity> getSenses(String wordId);

    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    WordEntity getWord(String id);

    @Query("SELECT * FROM sense_translations WHERE word_id = :wordId ORDER BY sense_position ASC")
    List<SenseTranslationEntity> getSenseTranslations(String wordId);

    @Query("SELECT * FROM sense_examples WHERE word_id = :wordId ORDER BY sense_position ASC, position ASC")
    List<SenseExampleEntity> getSenseExamples(String wordId);

    @Query("SELECT * FROM example_translations WHERE word_id = :wordId")
    List<ExampleTranslationEntity> getExampleTranslations(String wordId);

    @Query("SELECT * FROM pack_titles WHERE pack_id = :packId")
    List<PackTitleEntity> getPackTitles(String packId);
}
