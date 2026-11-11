package com.masterenglish.app.ui.profile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.masterenglish.app.R;
import com.masterenglish.app.data.local.entity.BadgeEntity;
import com.masterenglish.app.utils.AnimationUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Badge grid. Tapping a badge flips it to show the unlock condition. */
public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.Holder> {

    private final List<BadgeEntity> badges = new ArrayList<>();
    private final Set<String> unlocked = new HashSet<>();
    private final Set<String> flipped = new HashSet<>();

    public void submit(List<BadgeEntity> catalogue, Set<String> unlockedIds) {
        badges.clear();
        badges.addAll(catalogue);
        unlocked.clear();
        unlocked.addAll(unlockedIds);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_badge, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        BadgeEntity badge = badges.get(position);
        boolean isUnlocked = unlocked.contains(badge.id);
        boolean showBack = flipped.contains(badge.id);
        holder.title.setText(badge.title);
        holder.icon.setImageResource(iconFor(holder.itemView, badge.iconUrl));
        holder.icon.setAlpha(isUnlocked ? 1f : 0.35f);
        holder.subtitle.setText(showBack
                ? holder.itemView.getContext().getString(R.string.profile_badge_condition_prefix) + badge.description
                : isUnlocked ? holder.itemView.getContext().getString(R.string.profile_badge_unlocked)
                : holder.itemView.getContext().getString(R.string.profile_badge_locked));
        holder.itemView.setOnClickListener(v -> {
            if (!flipped.remove(badge.id)) {
                flipped.add(badge.id);
            }
            AnimationUtils.flip(holder.itemView, () -> notifyItemChanged(holder.getBindingAdapterPosition()));
        });
    }

    private static int iconFor(View view, String name) {
        int id = view.getResources().getIdentifier(name, "drawable", view.getContext().getPackageName());
        return id == 0 ? R.drawable.ic_badge_streak : id;
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        final TextView subtitle;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.badgeIcon);
            title = itemView.findViewById(R.id.badgeTitle);
            subtitle = itemView.findViewById(R.id.badgeSubtitle);
        }
    }
}
