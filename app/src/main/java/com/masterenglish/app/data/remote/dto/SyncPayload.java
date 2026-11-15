package com.masterenglish.app.data.remote.dto;

import com.masterenglish.app.data.local.entity.WordProgressEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Body of POST /sync/progress and GET /sync/progress. Rows use the same field
 * names as the word_progress table, so the server can store them without a translation layer.
 */
public final class SyncPayload {

    public String userId;
    public long sentAt;
    public List<WordProgressEntity> wordProgress = new ArrayList<>();
}
