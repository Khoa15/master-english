package com.masterenglish.app.ui.review;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.masterenglish.app.R;
import com.masterenglish.app.data.local.entity.GrammarEntity;
import com.masterenglish.app.data.local.entity.ListeningEntity;
import com.masterenglish.app.data.local.entity.ReadingEntity;
import com.masterenglish.app.databinding.FragmentReviewBinding;
import com.masterenglish.app.ui.redesign.Rows;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Practice tab: lessons grouped by skill (Listening, Reading, Grammar, Writing, Speaking), an AI
 * challenge card and the SRS flashcard entry.
 */
@AndroidEntryPoint
public class ReviewFragment extends Fragment {

    private static final int ROWS_PER_SECTION = 3;

    private FragmentReviewBinding binding;
    private ReviewViewModel viewModel;
    private List<GrammarEntity> grammar = new ArrayList<>();
    private List<ListeningEntity> listening = new ArrayList<>();
    private List<ReadingEntity> reading = new ArrayList<>();

    /** One lesson card in a section. */
    private static final class Item {
        final String primary;
        final String secondary;
        final String tag;
        final Runnable onClick;

        Item(String primary, String secondary, String tag, Runnable onClick) {
            this.primary = primary;
            this.secondary = secondary;
            this.tag = tag;
            this.onClick = onClick;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ReviewViewModel.class);

        binding.flashcardButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_review_to_flashcards));
        binding.aiCard.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_review_to_ai));

        viewModel.dueCount().observe(getViewLifecycleOwner(), count ->
                binding.dueText.setText(getString(R.string.review_due_format, count == null ? 0 : count)));
        viewModel.listening().observe(getViewLifecycleOwner(), items -> {
            listening = items == null ? new ArrayList<>() : items;
            render();
        });
        viewModel.reading().observe(getViewLifecycleOwner(), items -> {
            reading = items == null ? new ArrayList<>() : items;
            render();
        });
        viewModel.grammar().observe(getViewLifecycleOwner(), items -> {
            grammar = items == null ? new ArrayList<>() : items;
            render();
        });
        render();
    }

    private void render() {
        if (binding == null) {
            return;
        }
        binding.sectionsContainer.removeAllViews();

        List<Item> listeningItems = new ArrayList<>();
        for (ListeningEntity l : listening) {
            listeningItems.add(new Item(l.title, l.context, l.cefrLevel, () -> openListening(l.id)));
        }
        addSection(R.string.practice_section_listening, R.color.ds_blue, R.color.ds_blue_tint, listeningItems);

        List<Item> readingItems = new ArrayList<>();
        for (ReadingEntity r : reading) {
            readingItems.add(new Item(r.title, null, r.cefrLevel, () -> openReading(r.id)));
        }
        addSection(R.string.practice_section_reading, R.color.ds_green_ink, R.color.ds_green_tint, readingItems);

        List<Item> grammarItems = new ArrayList<>();
        for (GrammarEntity g : grammar) {
            grammarItems.add(new Item(g.topicName, g.domain, g.cefrLevel, null));
        }
        addSection(R.string.practice_section_grammar, R.color.ds_amber_ink, R.color.ds_amber_tint, grammarItems);

        // Writing and speaking have no lessons in the database yet, so their sections stay empty.
        addSection(R.string.practice_section_writing, R.color.ds_purple, R.color.ds_purple_tint, new ArrayList<>());
        addSection(R.string.practice_section_speaking, R.color.ds_orange, R.color.ds_orange_tint, new ArrayList<>());
    }

    private void addSection(int titleRes, int inkRes, int tintRes, List<Item> items) {
        int ink = ContextCompat.getColor(requireContext(), inkRes);
        int tint = ContextCompat.getColor(requireContext(), tintRes);

        LinearLayout section = new LinearLayout(requireContext());
        section.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sectionLp.topMargin = dp(24);
        section.setLayoutParams(sectionLp);

        TextView header = new TextView(requireContext());
        header.setText(titleRes);
        header.setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_ink));
        header.setTextSize(18);
        header.setTypeface(null, android.graphics.Typeface.BOLD);
        section.addView(header);

        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_ds_card_outline);
        card.setPadding(dp(16), dp(4), dp(16), dp(4));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.topMargin = dp(8);
        card.setLayoutParams(cardLp);

        if (items.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText(R.string.practice_section_empty);
            empty.setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_muted));
            empty.setPadding(0, dp(12), 0, dp(12));
            card.addView(empty);
        }
        for (Item item : items.subList(0, Math.min(ROWS_PER_SECTION, items.size()))) {
            View row = Rows.add(card, item.primary, item.secondary, item.tag, ink, tint, null, null, null);
            if (item.onClick != null) {
                row.setOnClickListener(v -> item.onClick.run());
            }
        }
        section.addView(card);
        binding.sectionsContainer.addView(section);
    }

    private void openListening(String id) {
        Bundle args = new Bundle();
        args.putString("listeningId", id);
        NavHostFragment.findNavController(this).navigate(R.id.action_review_to_listening, args);
    }

    private void openReading(String id) {
        Bundle args = new Bundle();
        args.putString("readingId", id);
        NavHostFragment.findNavController(this).navigate(R.id.action_review_to_reading, args);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
