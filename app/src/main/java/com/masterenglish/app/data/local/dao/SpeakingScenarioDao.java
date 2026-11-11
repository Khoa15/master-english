package com.masterenglish.app.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.masterenglish.app.data.local.entity.SpeakingScenarioEntity;

import java.util.List;

/** Speaking situations the learner created for their own practice. */
@Dao
public interface SpeakingScenarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SpeakingScenarioEntity scenario);

    @Query("SELECT * FROM speaking_scenarios WHERE user_id = :userId ORDER BY created_at DESC")
    LiveData<List<SpeakingScenarioEntity>> observeForUser(String userId);
}
