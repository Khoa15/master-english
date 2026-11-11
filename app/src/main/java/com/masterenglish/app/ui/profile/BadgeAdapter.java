package com.masterenglish.app.ui.profile;

import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.masterenglish.app.R;
import com.masterenglish.app.data.local.Enums.BadgeCategory;
import com.masterenglish.app.data.local.entity.BadgeEntity;
import com.masterenglish.app.utils.AnimationUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Badge grid. Unlocked badges show full colour on a gradient card per category. Locked badges are a
 * dark grey card with a desaturated icon. Tapping a badge flips it to show the unlock condition.
 */
public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.Holder> {

    /** Profile shows at most this many badges; the rest are reachable through the unlocked count. */
    public static final int PREVIEW_LIMIT = 6;

    /** Desaturates and darkens the artwork so a locked badge reads as an unearned silhouette. */
    private static final ColorFilter LOCKED_FILTER;

    static {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0f);
        // Scale RGB by 0.45 to push the grey towards dark grey.
        ColorMatrix dim = new ColorMatrix(new float[]{
                0.45f, 0, 0, 0, 0,
                0, 0.45f, 0, 0, 0,
                0, 0, 0.45f, 0, 0,
                0, 0, 0, 1, 0});
        matrix.postConcat(dim);
        LOCKED_FILTER = new ColorMatrixColorFilter(matrix);
    }

    private final List<BadgeEntity> badges = new ArrayList<>();
    private final Set<String> unlocked = new HashSet<>();
    private final Set<String> flipped = new HashSet<>();

    public void submit(List<BadgeEntity> catalogue, Set<String> unlockedIds) {
        badges.clear();
        badges.addAll(catalogue.subList(0, Math.min(PREVIEW_LIMIT, catalogue.size())));
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
        View card = holder.itemView;
        float density = card.getResources().getDisplayMetrics().density;

        card.setBackground(isUnlocked ? unlockedCard(card, badge.category, density) : lockedCard(card, density));
        holder.icon.setImageResource(iconFor(card, badge.iconUrl));
        if (isUnlocked) {
            holder.icon.clearColorFilter();
            holder.icon.setAlpha(1f);
        } else {
            holder.icon.setColorFilter(LOCKED_FILTER);
        }

        int titleColor = isUnlocked ? Color.WHITE : ContextCompat.getColor(card.getContext(), R.color.badge_locked_text);
        int subColor = isUnlocked ? 0xDDFFFFFF
                : ContextCompat.getColor(card.getContext(), R.color.badge_locked_sub);
        holder.title.setText(badge.title);
        holder.title.setTextColor(titleColor);
        holder.subtitle.setTextColor(subColor);
        holder.subtitle.setText(showBack
                ? card.getContext().getString(R.string.profile_badge_condition_prefix) + badge.description
                : isUnlocked ? card.getContext().getString(R.string.profile_badge_unlocked)
                : card.getContext().getString(R.string.profile_badge_locked));
        card.setOnClickListener(v -> {
            if (!flipped.remove(badge.id)) {
                flipped.add(badge.id);
            }
            AnimationUtils.flip(card, () -> notifyItemChanged(holder.getBindingAdapterPosition()));
        });
    }

    private static GradientDrawable unlockedCard(View view, BadgeCategory category, float density) {
        int[] colors = gradientFor(category);
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
        drawable.setCornerRadius(16 * density);
        return drawable;
    }

    private static GradientDrawable lockedCard(View view, float density) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(ContextCompat.getColor(view.getContext(), R.color.badge_locked_bg));
        drawable.setCornerRadius(16 * density);
        return drawable;
    }

    /** Full-colour gradient per category, so each family of badges is easy to tell apart. */
    private static int[] gradientFor(BadgeCategory category) {
        if (category == null) {
            return new int[]{0xFF4C8DFF, 0xFF2D62C4};
        }
        switch (category) {
            case STREAK:
                return new int[]{0xFFFF8A3D, 0xFFE0561B};
            case VOCAB_MASTER:
                return new int[]{0xFF4C8DFF, 0xFF2D62C4};
            case GRAMMAR_PRO:
                return new int[]{0xFF9A74D6, 0xFF6B4FA0};
            case AI_CHALLENGER:
                return new int[]{0xFFFFD84D, 0xFFE0A030};
            default:
                return new int[]{0xFF2E7D5B, 0xFF1F5C41};
        }
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
