package com.masterenglish.app.ui.roadmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.masterenglish.app.data.local.Enums.NodeStatus;
import com.masterenglish.app.data.local.Enums.NodeType;
import com.masterenglish.app.data.local.entity.RoadmapNodeEntity;
import com.masterenglish.app.data.repository.RoadmapItem;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RoadmapActionPolicyTest {

    private static RoadmapItem item(String id, NodeStatus status) {
        RoadmapNodeEntity node = new RoadmapNodeEntity();
        node.id = id;
        node.title = id;
        node.nodeType = NodeType.VOCABULARY;
        return new RoadmapItem(node, status, 0);
    }

    @Test
    public void currentNodeIsTheFirstAvailableNode() {
        List<RoadmapItem> items = Arrays.asList(
                item("a", NodeStatus.MASTERED),
                item("b", NodeStatus.AVAILABLE),
                item("c", NodeStatus.LOCKED));
        assertEquals("b", RoadmapActionPolicy.currentNodeId(items));
    }

    @Test
    public void currentNodeIsNullWhenNothingIsAvailable() {
        List<RoadmapItem> items = Arrays.asList(item("a", NodeStatus.COMPLETED), item("b", NodeStatus.LOCKED));
        assertNull(RoadmapActionPolicy.currentNodeId(items));
        assertNull(RoadmapActionPolicy.currentNodeId(Collections.emptyList()));
    }

    @Test
    public void learnedNodesOfferPractice() {
        assertEquals(RoadmapActionPolicy.Action.PRACTICE,
                RoadmapActionPolicy.actionFor(item("a", NodeStatus.COMPLETED), "b"));
        assertEquals(RoadmapActionPolicy.Action.PRACTICE,
                RoadmapActionPolicy.actionFor(item("a", NodeStatus.MASTERED), "b"));
    }

    @Test
    public void currentNodeOffersStart() {
        assertEquals(RoadmapActionPolicy.Action.START,
                RoadmapActionPolicy.actionFor(item("b", NodeStatus.AVAILABLE), "b"));
    }

    @Test
    public void otherNotLearnedNodesOfferChallenge() {
        assertEquals(RoadmapActionPolicy.Action.CHALLENGE,
                RoadmapActionPolicy.actionFor(item("c", NodeStatus.AVAILABLE), "b"));
        assertEquals(RoadmapActionPolicy.Action.CHALLENGE,
                RoadmapActionPolicy.actionFor(item("d", NodeStatus.LOCKED), "b"));
    }
}
