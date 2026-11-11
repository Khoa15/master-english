package com.masterenglish.app.ui.roadmap;

import com.masterenglish.app.data.local.Enums.NodeStatus;
import com.masterenglish.app.data.repository.RoadmapItem;

import java.util.List;

/**
 * Decides which action a roadmap node offers. Pure logic with no Android dependencies,
 * so it can be unit tested without a device.
 */
public final class RoadmapActionPolicy {

    public enum Action { START, PRACTICE, CHALLENGE }

    private RoadmapActionPolicy() {
    }

    /** Id of the current node: the first unlocked node the learner has not finished, or null. */
    public static String currentNodeId(List<RoadmapItem> items) {
        for (RoadmapItem item : items) {
            if (item.status == NodeStatus.AVAILABLE) {
                return item.node.id;
            }
        }
        return null;
    }

    /**
     * Learned nodes offer PRACTICE, the current node offers START,
     * and every other node that is not learned yet offers CHALLENGE.
     */
    public static Action actionFor(RoadmapItem item, String currentNodeId) {
        if (item.status == NodeStatus.COMPLETED || item.status == NodeStatus.MASTERED) {
            return Action.PRACTICE;
        }
        return item.node.id.equals(currentNodeId) ? Action.START : Action.CHALLENGE;
    }
}
