package com.masterenglish.app.data.remote.dto;

import com.masterenglish.app.data.local.entity.AiPersonaMemoryEntity;
import com.masterenglish.app.data.local.entity.UserItemRetentionEntity;
import com.masterenglish.app.data.local.entity.UserNodeProgressEntity;
import com.masterenglish.app.data.local.entity.UserWeaknessEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Body of POST /sync/progress and GET /sync/progress. Rows use the same field
 * names as the MySQL tables, so the server can store them without a translation layer.
 */
public final class SyncPayload {

    public String userId;
    public long sentAt;
    public List<UserNodeProgressEntity> nodeProgress = new ArrayList<>();
    public List<UserItemRetentionEntity> retention = new ArrayList<>();
    public List<UserWeaknessEntity> weaknesses = new ArrayList<>();
    public List<AiPersonaMemoryEntity> memories = new ArrayList<>();
}
