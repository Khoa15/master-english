package com.masterenglish.app.ui.roadmap;

/** Tracks which roadmap node is expanded to show its action button. At most one at a time. */
public final class RoadmapSelection {

    private String selectedId;

    /** Expands the node, or collapses it if it is already expanded. Returns the previously expanded id. */
    public String toggle(String nodeId) {
        String previous = selectedId;
        selectedId = nodeId.equals(previous) ? null : nodeId;
        return previous;
    }

    /** Id of the expanded node, or null if none is expanded. */
    public String selectedId() {
        return selectedId;
    }

    public boolean isSelected(String nodeId) {
        return nodeId.equals(selectedId);
    }
}
