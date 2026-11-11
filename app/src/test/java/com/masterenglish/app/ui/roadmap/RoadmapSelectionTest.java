package com.masterenglish.app.ui.roadmap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RoadmapSelectionTest {

    @Test
    public void togglingAnUnselectedNodeExpandsIt() {
        RoadmapSelection selection = new RoadmapSelection();
        assertNull(selection.toggle("a"));
        assertEquals("a", selection.selectedId());
        assertTrue(selection.isSelected("a"));
    }

    @Test
    public void togglingTheExpandedNodeCollapsesIt() {
        RoadmapSelection selection = new RoadmapSelection();
        selection.toggle("a");
        assertEquals("a", selection.toggle("a"));
        assertNull(selection.selectedId());
    }

    @Test
    public void expandingAnotherNodeCollapsesThePreviousOne() {
        RoadmapSelection selection = new RoadmapSelection();
        selection.toggle("a");
        assertEquals("a", selection.toggle("b"));
        assertEquals("b", selection.selectedId());
        assertFalse(selection.isSelected("a"));
    }
}
