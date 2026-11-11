package com.masterenglish.app.ui.roadmap;

import android.animation.AnimatorSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.masterenglish.app.R;
import com.masterenglish.app.data.local.Enums.NodeStatus;
import com.masterenglish.app.data.local.Enums.NodeType;
import com.masterenglish.app.data.repository.RoadmapItem;
import com.masterenglish.app.utils.AnimationUtils;

/**
 * Renders roadmap nodes in a zig-zag. Each node is offset horizontally along a sine
 * curve so the path winds left and right, and {@link RoadmapPathDecoration} draws the trail.
 */
public class RoadmapAdapter extends ListAdapter<RoadmapItem, RoadmapAdapter.NodeHolder> {

    /** Horizontal swing of the zig-zag, in dp. */
    static final float SWING_DP = 64f;

    public interface OnNodeClick {
        void onNodeClicked(RoadmapItem item);
    }

    private final OnNodeClick listener;

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
        holder.bind(item, listener);
    }

    @Override
    public void onViewRecycled(@NonNull NodeHolder holder) {
        holder.stopPulse();
        super.onViewRecycled(holder);
    }

    static final class NodeHolder extends RecyclerView.ViewHolder {
        private final View circle;
        private final ImageView icon;
        private final TextView title;
        private final LinearLayout starRow;
        private AnimatorSet pulse;

        NodeHolder(@NonNull View itemView) {
            super(itemView);
            circle = itemView.findViewById(R.id.nodeCircle);
            icon = itemView.findViewById(R.id.nodeIcon);
            title = itemView.findViewById(R.id.nodeTitle);
            starRow = itemView.findViewById(R.id.starRow);
        }

        void bind(RoadmapItem item, OnNodeClick listener) {
            stopPulse();
            title.setText(item.node.title);
            icon.setImageResource(iconFor(item.node.nodeType, item.status));
            circle.setBackgroundResource(backgroundFor(item.node.nodeType, item.status));
            icon.setImageTintList(null);
            for (int i = 0; i < starRow.getChildCount(); i++) {
                ((ImageView) starRow.getChildAt(i)).setImageResource(i < item.stars ? R.drawable.ic_star : R.drawable.ic_star_empty);
            }
            starRow.setVisibility(item.status == NodeStatus.LOCKED ? View.INVISIBLE : View.VISIBLE);
            itemView.setAlpha(item.status == NodeStatus.LOCKED ? 0.85f : 1f);
            itemView.setOnClickListener(v -> listener.onNodeClicked(item));
            if (item.status == NodeStatus.AVAILABLE) {
                pulse = AnimationUtils.pulse(circle);
            }
        }

        void stopPulse() {
            if (pulse != null) {
                pulse.cancel();
                pulse = null;
            }
            circle.setScaleX(1f);
            circle.setScaleY(1f);
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
