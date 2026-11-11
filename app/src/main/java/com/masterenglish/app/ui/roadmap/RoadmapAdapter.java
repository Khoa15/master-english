package com.masterenglish.app.ui.roadmap;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.masterenglish.app.R;
import com.masterenglish.app.data.local.Enums.NodeStatus;
import com.masterenglish.app.data.local.Enums.NodeType;
import com.masterenglish.app.data.repository.RoadmapItem;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Renders roadmap nodes in a zig-zag. Each node is offset horizontally along a sine
 * curve so the path winds left and right, and {@link RoadmapPathDecoration} draws the trail.
 * Tapping an unlocked node reveals a single action button beneath it: START for the current
 * node, PRACTICE for completed nodes and CHALLENGE for later nodes that are already open.
 */
public class RoadmapAdapter extends ListAdapter<RoadmapItem, RoadmapAdapter.NodeHolder> {

    /** Horizontal swing of the zig-zag, in dp. */
    static final float SWING_DP = 64f;

    public interface OnNodeClick {
        /** Called when the action button under a node is tapped. */
        void onActionClicked(RoadmapItem item);

        /** Called when a node is expanded or collapsed by a tap. */
        void onNodeToggled(RoadmapItem item, boolean expanded);
    }

    private final OnNodeClick listener;
    private final RoadmapSelection selection = new RoadmapSelection();
    /** Ids of nodes whose next bind should play the expand/collapse animation. */
    private final Set<String> pendingAnimations = new HashSet<>();
    /** Id of the current node (see {@link RoadmapActionPolicy#currentNodeId}). */
    private String currentNodeId;

    public RoadmapAdapter(OnNodeClick listener) {
        super(new DiffUtil.ItemCallback<RoadmapItem>() {
            @Override
            public boolean areItemsTheSame(@NonNull RoadmapItem a, @NonNull RoadmapItem b) {
                return a.node.id.equals(b.node.id);
            }

            @Override
            public boolean areContentsTheSame(@NonNull RoadmapItem a, @NonNull RoadmapItem b) {
                return a.status.equals(b.status) && a.stars == b.stars && a.node.title.equals(b.node.title);
            }
        });
        this.listener = listener;
    }

    @Override
    public void onCurrentListChanged(@NonNull List<RoadmapItem> previousList, @NonNull List<RoadmapItem> currentList) {
        super.onCurrentListChanged(previousList, currentList);
        currentNodeId = RoadmapActionPolicy.currentNodeId(currentList);
    }

    @NonNull
    @Override
    public NodeHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_roadmap_node, parent, false);
        return new NodeHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NodeHolder holder, int position) {
        RoadmapItem item = getItem(position);
        float density = holder.itemView.getResources().getDisplayMetrics().density;
        holder.itemView.setTranslationX((float) (Math.sin(position * 0.9) * SWING_DP * density));
        holder.bind(item,
                selection.isSelected(item.node.id),
                labelFor(RoadmapActionPolicy.actionFor(item, currentNodeId)),
                pendingAnimations.remove(item.node.id),
                () -> onNodeTapped(item),
                () -> listener.onActionClicked(item));
    }

    private void onNodeTapped(RoadmapItem item) {
        String previous = selection.toggle(item.node.id);
        String selected = selection.selectedId();
        if (previous != null) {
            pendingAnimations.add(previous);
        }
        if (selected != null) {
            pendingAnimations.add(selected);
        }
        refreshNode(previous);
        refreshNode(selected);
        listener.onNodeToggled(item, selection.isSelected(item.node.id));
    }

    /** Re-binds the node with the given id so its action button shows or hides. */
    private void refreshNode(String id) {
        if (id == null) {
            return;
        }
        List<RoadmapItem> list = getCurrentList();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).node.id.equals(id)) {
                notifyItemChanged(i);
                return;
            }
        }
    }

    @StringRes
    private static int labelFor(RoadmapActionPolicy.Action action) {
        switch (action) {
            case START:
                return R.string.lesson_start;
            case PRACTICE:
                return R.string.roadmap_action_practice;
            default:
                return R.string.roadmap_action_challenge;
        }
    }

    static final class NodeHolder extends RecyclerView.ViewHolder {
        private final View circle;
        private final ImageView icon;
        private final TextView title;
        private final LinearLayout starRow;
        private final TextView stageHeader;
        private final MaterialButton actionButton;

        NodeHolder(@NonNull View itemView) {
            super(itemView);
            circle = itemView.findViewById(R.id.nodeCircle);
            icon = itemView.findViewById(R.id.nodeIcon);
            title = itemView.findViewById(R.id.nodeTitle);
            starRow = itemView.findViewById(R.id.starRow);
            stageHeader = itemView.findViewById(R.id.stageHeader);
            actionButton = itemView.findViewById(R.id.nodeActionButton);
        }

        void bind(RoadmapItem item, boolean expanded, @StringRes int actionRes, boolean animate,
                  Runnable onTap, Runnable onAction) {
            title.setText(item.node.title);
            stageHeader.setText(item.stageTitle == null ? "" : item.stageTitle);
            stageHeader.setVisibility(item.stageTitle == null ? View.GONE : View.VISIBLE);
            // Vector drawables are costly to load, so only swap them when the resource really changes.
            setImageIfChanged(icon, iconFor(item.node.nodeType, item.status));
            circle.setBackgroundResource(backgroundFor(item.node.nodeType, item.status));
            icon.setImageTintList(null);
            for (int i = 0; i < starRow.getChildCount(); i++) {
                setImageIfChanged((ImageView) starRow.getChildAt(i), i < item.stars ? R.drawable.ic_star : R.drawable.ic_star_empty);
            }
            starRow.setVisibility(item.status == NodeStatus.LOCKED ? View.INVISIBLE : View.VISIBLE);
            itemView.setAlpha(item.status == NodeStatus.LOCKED ? 0.85f : 1f);
            // Only the node itself is tappable. The row spans the full width, so a listener on
            // itemView would also fire for taps on empty space beside the node.
            itemView.setOnClickListener(null);
            circle.setOnClickListener(v -> onTap.run());
            title.setOnClickListener(v -> onTap.run());

            boolean showButton = expanded && actionRes != 0;
            if (showButton) {
                actionButton.setText(actionRes);
                actionButton.setOnClickListener(v -> onAction.run());
            } else {
                actionButton.setOnClickListener(null);
            }
            // The button is INVISIBLE, not GONE, when hidden: the space is reserved so showing
            // it never moves the nodes below.
            RoadmapToggleAnimator.applyButton(actionButton, showButton, animate);
            if (animate) {
                RoadmapToggleAnimator.pulse(circle);
            }
        }

        private static void setImageIfChanged(ImageView view, int resId) {
            Object current = view.getTag(R.id.roadmapImageRes);
            if (!(current instanceof Integer) || (Integer) current != resId) {
                view.setImageResource(resId);
                view.setTag(R.id.roadmapImageRes, resId);
            }
        }

        private static int iconFor(NodeType type, NodeStatus status) {
            if (status == NodeStatus.LOCKED) {
                return R.drawable.ic_lock;
            }
            switch (type) {
                case GRAMMAR:
                    return R.drawable.ic_skill_grammar;
                case LISTENING:
                    return R.drawable.ic_skill_listening;
                case READING:
                    return R.drawable.ic_skill_reading;
                case CHECKPOINT_BOSS:
                    return R.drawable.ic_skill_boss;
                default:
                    return R.drawable.ic_skill_vocab;
            }
        }

        private static int backgroundFor(NodeType type, NodeStatus status) {
            if (status == NodeStatus.LOCKED) {
                return R.drawable.bg_node_locked;
            }
            if (status == NodeStatus.COMPLETED || status == NodeStatus.MASTERED) {
                return R.drawable.bg_node_completed;
            }
            switch (type) {
                case GRAMMAR:
                case READING:
                    return R.drawable.bg_node_blue;
                case LISTENING:
                    return R.drawable.bg_node_orange;
                case CHECKPOINT_BOSS:
                    return R.drawable.bg_node_boss;
                default:
                    return R.drawable.bg_node_available;
            }
        }
    }
}
