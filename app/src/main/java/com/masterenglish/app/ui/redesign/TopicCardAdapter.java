package com.masterenglish.app.ui.redesign;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.masterenglish.app.databinding.ItemTopicCardBinding;

import java.util.ArrayList;
import java.util.List;

/** Two-column card grid used for speaking topics and vocabulary topics. */
public class TopicCardAdapter extends RecyclerView.Adapter<TopicCardAdapter.Holder> {

    /** A card: header, meta line, stat line and whether it shows the badge and action button. */
    public static final class Card {
        public final String id;
        public final String title;
        public final String meta;
        public final String stat;
        public final boolean badge;
        public final boolean action;

        public Card(String id, String title, String meta, String stat, boolean badge, boolean action) {
            this.id = id;
            this.title = title;
            this.meta = meta;
            this.stat = stat;
            this.badge = badge;
            this.action = action;
        }
    }

    public interface OnCard {
        void onCard(Card card);
    }

    private final List<Card> cards = new ArrayList<>();
    private final OnCard onOpen;
    @Nullable
    private final OnCard onAction;

    public TopicCardAdapter(OnCard onOpen, @Nullable OnCard onAction) {
        this.onOpen = onOpen;
        this.onAction = onAction;
    }

    public void submit(List<Card> items) {
        cards.clear();
        cards.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemTopicCardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Card card = cards.get(position);
        ItemTopicCardBinding b = holder.binding;
        b.topicTitle.setText(card.title);
        b.topicMeta.setText(card.meta);
        b.topicStat.setText(card.stat);
        b.topicBadge.setVisibility(card.badge ? android.view.View.VISIBLE : android.view.View.GONE);
        b.topicAction.setVisibility(card.action ? android.view.View.VISIBLE : android.view.View.GONE);
        b.card.setOnClickListener(v -> onOpen.onCard(card));
        b.topicAction.setOnClickListener(v -> {
            if (onAction != null) {
                onAction.onCard(card);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cards.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ItemTopicCardBinding binding;

        Holder(ItemTopicCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
