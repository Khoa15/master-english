package com.masterenglish.app.data.repository;

import com.masterenglish.app.data.local.Enums.NodeStatus;
import com.masterenglish.app.data.local.entity.RoadmapNodeEntity;

/** A roadmap node joined with the learner's status. Status is derived, not stored, for LOCKED/AVAILABLE. */
public final class RoadmapItem {

    public final RoadmapNodeEntity node;
    public final NodeStatus status;
    public final int stars;

    public RoadmapItem(RoadmapNodeEntity node, NodeStatus status, int stars) {
        this.node = node;
        this.status = status;
        this.stars = stars;
    }
}
