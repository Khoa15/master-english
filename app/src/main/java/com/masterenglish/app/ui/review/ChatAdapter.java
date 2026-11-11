package com.masterenglish.app.ui.review;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.masterenglish.app.R;
import com.masterenglish.app.service.ai.AiTutorService;

import java.util.ArrayList;
import java.util.List;

/** Chat bubbles. Mascot replies also show the correction and each detected mistake with its rule. */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.Holder> {

    private final List<ChatItem> items = new ArrayList<>();

    public void submit(List<ChatItem> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ChatItem item = items.get(position);
        holder.bubble.setText(item.text);
        holder.bubble.setBackgroundResource(item.fromUser ? R.drawable.bg_chip_selected : R.drawable.bg_bubble);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.bubble.getLayoutParams();
        params.gravity = item.fromUser ? Gravity.END : Gravity.START;
        holder.bubble.setLayoutParams(params);

        boolean hasCorrection = item.correction != null && !item.correction.isEmpty();
        holder.correction.setVisibility(hasCorrection ? View.VISIBLE : View.GONE);
        if (hasCorrection) {
            holder.correction.setText(holder.itemView.getContext().getString(R.string.ai_correction, item.correction));
        }
        StringBuilder mistakes = new StringBuilder();
        for (AiTutorService.Mistake mistake : item.mistakes) {
            if (mistakes.length() > 0) {
                mistakes.append('\n');
            }
            mistakes.append("• ").append(mistake.tag.replace('_', ' ')).append(": ").append(mistake.explanation);
        }
        holder.mistakes.setVisibility(mistakes.length() == 0 ? View.GONE : View.VISIBLE);
        holder.mistakes.setText(mistakes);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView bubble;
        final TextView correction;
        final TextView mistakes;

        Holder(@NonNull View itemView) {
            super(itemView);
            bubble = itemView.findViewById(R.id.bubble);
            correction = itemView.findViewById(R.id.correction);
            mistakes = itemView.findViewById(R.id.mistakes);
        }
    }
}
