package com.masterenglish.app.ui.review;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.masterenglish.app.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Rows for the four skill tabs. Grammar and vocabulary expand in place; listening/reading open a screen. */
public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.RowHolder> {

    public interface OnRowOpen {
        void onOpen(ReviewRow row);
    }

    private final OnRowOpen openListener;
    private final Set<String> expanded = new HashSet<>();
    private final List<ReviewRow> rows = new ArrayList<>();

    public ReviewAdapter(OnRowOpen openListener) {
        this.openListener = openListener;
    }

    public void submit(List<ReviewRow> items) {
        rows.clear();
        rows.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review_row, parent, false);
        return new RowHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RowHolder holder, int position) {
        ReviewRow row = rows.get(position);
        boolean open = expanded.contains(row.id);
        holder.title.setText(row.title);
        holder.subtitle.setText(row.subtitle);
        holder.body.setText(row.body);
        holder.body.setVisibility(open && row.body != null && !row.body.isEmpty() ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> {
            if (row.kind == ReviewRow.Kind.LISTENING || row.kind == ReviewRow.Kind.READING) {
                openListener.onOpen(row);
                return;
            }
            if (!expanded.remove(row.id)) {
                expanded.add(row.id);
            }
            notifyItemChanged(holder.getBindingAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static final class RowHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;
        final TextView body;

        RowHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.rowTitle);
            subtitle = itemView.findViewById(R.id.rowSubtitle);
            body = itemView.findViewById(R.id.rowBody);
        }
    }
}
