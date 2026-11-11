package com.masterenglish.app.ui.review;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.masterenglish.app.R;
import com.masterenglish.app.data.local.entity.UserWeaknessEntity;
import com.masterenglish.app.utils.AnimationUtils;

/** Weakness Clinic rows: the tag, a mastery bar and how often it was missed. */
public class WeaknessAdapter extends ListAdapter<UserWeaknessEntity, WeaknessAdapter.Holder> {

    public WeaknessAdapter() {
        super(new DiffUtil.ItemCallback<UserWeaknessEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull UserWeaknessEntity a, @NonNull UserWeaknessEntity b) {
                return a.errorTag.equals(b.errorTag);
            }

            @Override
            public boolean areContentsTheSame(@NonNull UserWeaknessEntity a, @NonNull UserWeaknessEntity b) {
                return a.masteryScore == b.masteryScore && a.errorCount == b.errorCount;
            }
        });
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_weakness, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        UserWeaknessEntity item = getItem(position);
        holder.tag.setText(item.errorTag.replace('_', ' '));
        holder.count.setText(holder.itemView.getContext().getString(R.string.weakness_count_format, item.errorCount));
        AnimationUtils.tweenProgress(holder.mastery, Math.round(item.masteryScore * 100));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView tag;
        final TextView count;
        final ProgressBar mastery;

        Holder(@NonNull View itemView) {
            super(itemView);
            tag = itemView.findViewById(R.id.weaknessTag);
            count = itemView.findViewById(R.id.weaknessCount);
            mastery = itemView.findViewById(R.id.weaknessMastery);
        }
    }
}
