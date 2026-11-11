package com.masterenglish.app.data.repository;

import static org.junit.Assert.assertEquals;

import com.masterenglish.app.data.local.Enums.NodeStatus;
import com.masterenglish.app.data.local.Enums.NodeType;
import com.masterenglish.app.data.local.entity.RoadmapNodeEntity;
import com.masterenglish.app.data.local.entity.UserNodeProgressEntity;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RoadmapStatusTest {

    private static RoadmapNodeEntity node(String id, int position) {
        RoadmapNodeEntity n = new RoadmapNodeEntity();
        n.id = id;
        n.positionIndex = position;
        n.title = id;
        n.nodeType = NodeType.VOCABULARY;
        return n;
    }

    private static UserNodeProgressEntity done(String nodeId, NodeStatus status, int stars) {
        UserNodeProgressEntity p = new UserNodeProgressEntity();
        p.nodeId = nodeId;
        p.status = status;
        p.starsEarned = stars;
        return p;
    }

    @Test
    public void firstNodeIsAvailableAndTheRestAreLockedUntilCompleted() {
        List<RoadmapNodeEntity> nodes = Arrays.asList(node("a", 0), node("b", 1), node("c", 2));
        List<RoadmapItem> items = ProgressRepository.buildRoadmap(nodes, new ArrayList<>());
        assertEquals(NodeStatus.AVAILABLE, items.get(0).status);
        assertEquals(NodeStatus.LOCKED, items.get(1).status);
        assertEquals(NodeStatus.LOCKED, items.get(2).status);
    }

    @Test
    public void completingANodeUnlocksTheNext() {
        List<RoadmapNodeEntity> nodes = Arrays.asList(node("a", 0), node("b", 1), node("c", 2));
        List<RoadmapItem> items = ProgressRepository.buildRoadmap(nodes,
                Collections.singletonList(done("a", NodeStatus.MASTERED, 3)));
        assertEquals(NodeStatus.MASTERED, items.get(0).status);
        assertEquals(3, items.get(0).stars);
        assertEquals(NodeStatus.AVAILABLE, items.get(1).status);
        assertEquals(NodeStatus.LOCKED, items.get(2).status);
    }

    @Test
    public void nullProgressStillProducesAValidPath() {
        List<RoadmapItem> items = ProgressRepository.buildRoadmap(Arrays.asList(node("a", 0)), null);
        assertEquals(NodeStatus.AVAILABLE, items.get(0).status);
        assertEquals(0, ProgressRepository.buildRoadmap(null, null).size());
    }
}
