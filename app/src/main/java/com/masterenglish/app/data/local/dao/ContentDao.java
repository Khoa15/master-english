package com.masterenglish.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.masterenglish.app.data.local.Enums.ParentType;
import com.masterenglish.app.data.local.entity.GrammarEntity;
import com.masterenglish.app.data.local.entity.LessonQuestionEntity;
import com.masterenglish.app.data.local.entity.ListeningEntity;
import com.masterenglish.app.data.local.entity.ReadingEntity;
import com.masterenglish.app.data.local.entity.RoadmapNodeEntity;
import com.masterenglish.app.data.local.entity.RoadmapStageEntity;
import com.masterenglish.app.data.local.entity.VocabularyEntity;

import java.util.List;

/** Read/write access to the master content seeded from the four JSON files. */
@Dao
public interface ContentDao {

    // ----- Roadmap -------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStages(List<RoadmapStageEntity> stages);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNodes(List<RoadmapNodeEntity> nodes);

    @Query("SELECT COUNT(*) FROM roadmap_nodes")
    int countNodes();

    @Query("SELECT * FROM roadmap_nodes ORDER BY position_index ASC")
    LiveData<List<RoadmapNodeEntity>> observeNodes();

    @Query("SELECT * FROM roadmap_nodes ORDER BY position_index ASC")
    List<RoadmapNodeEntity> getAllNodes();

    @Query("SELECT * FROM roadmap_nodes WHERE id = :nodeId LIMIT 1")
    RoadmapNodeEntity getNode(String nodeId);

    @Query("SELECT * FROM roadmap_stages WHERE id = :stageId LIMIT 1")
    RoadmapStageEntity getStage(int stageId);

    @Query("SELECT * FROM roadmap_stages ORDER BY stage_number ASC")
    List<RoadmapStageEntity> getAllStages();

    // ----- Vocabulary ----------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertVocabulary(List<VocabularyEntity> items);

    @Query("SELECT COUNT(*) FROM vocabulary_lessons")
    int countVocabulary();

    @Query("SELECT * FROM vocabulary_lessons")
    List<VocabularyEntity> getAllVocabulary();

    @Query("SELECT * FROM vocabulary_lessons WHERE node_id = :nodeId ORDER BY id ASC")
    List<VocabularyEntity> getVocabularyByNode(String nodeId);

    @Query("SELECT * FROM vocabulary_lessons WHERE id IN (:ids)")
    List<VocabularyEntity> getVocabularyByIds(List<String> ids);

    @Query("SELECT * FROM vocabulary_lessons WHERE LOWER(word) = LOWER(:word) LIMIT 1")
    VocabularyEntity findVocabularyByWord(String word);

    // ----- Grammar -------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertGrammar(List<GrammarEntity> items);

    @Query("SELECT COUNT(*) FROM grammar_lessons")
    int countGrammar();

    @Query("SELECT * FROM grammar_lessons ORDER BY id ASC")
    List<GrammarEntity> getAllGrammar();

    @Query("SELECT * FROM grammar_lessons WHERE id = :id LIMIT 1")
    GrammarEntity getGrammar(String id);

    @Query("SELECT * FROM grammar_lessons WHERE node_id = :nodeId LIMIT 1")
    GrammarEntity getGrammarByNode(String nodeId);

    // ----- Reading -------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertReading(List<ReadingEntity> items);

    @Query("SELECT COUNT(*) FROM reading_lessons")
    int countReading();

    @Query("SELECT * FROM reading_lessons ORDER BY id ASC")
    LiveData<List<ReadingEntity>> observeReading();

    @Query("SELECT * FROM reading_lessons WHERE id = :id LIMIT 1")
    ReadingEntity getReading(String id);

    @Query("SELECT * FROM reading_lessons WHERE node_id = :nodeId LIMIT 1")
    ReadingEntity getReadingByNode(String nodeId);

    // ----- Listening -----------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertListening(List<ListeningEntity> items);

    @Query("SELECT COUNT(*) FROM listening_lessons")
    int countListening();

    @Query("SELECT * FROM listening_lessons ORDER BY id ASC")
    LiveData<List<ListeningEntity>> observeListening();

    @Query("SELECT * FROM listening_lessons WHERE id = :id LIMIT 1")
    ListeningEntity getListening(String id);

    @Query("SELECT * FROM listening_lessons WHERE node_id = :nodeId LIMIT 1")
    ListeningEntity getListeningByNode(String nodeId);

    @Query("UPDATE listening_lessons SET duration_seconds = :seconds WHERE id = :id")
    void updateListeningDuration(String id, int seconds);

    // ----- Questions -----------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertQuestions(List<LessonQuestionEntity> questions);

    @Query("SELECT COUNT(*) FROM lesson_questions")
    int countQuestions();

    @Query("SELECT * FROM lesson_questions WHERE parent_type = :type AND parent_id = :parentId ORDER BY id ASC")
    List<LessonQuestionEntity> getQuestions(ParentType type, String parentId);

    @Query("SELECT * FROM lesson_questions WHERE parent_type = :type AND parent_id IN (:parentIds)")
    List<LessonQuestionEntity> getQuestionsForParents(ParentType type, List<String> parentIds);

    @Query("SELECT * FROM lesson_questions WHERE id = :id LIMIT 1")
    LessonQuestionEntity getQuestion(String id);
}
