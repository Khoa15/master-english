package com.masterenglish.app.ui.vocab;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.masterenglish.app.R;
import com.masterenglish.app.databinding.FragmentVocabularyBinding;
import com.masterenglish.app.ui.redesign.TopicCardAdapter;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/** Vocabulary overview: learned and mastered totals, review button and topics. */
@AndroidEntryPoint
public class VocabularyFragment extends Fragment {

    private FragmentVocabularyBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVocabularyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        VocabViewModel viewModel = new ViewModelProvider(this).get(VocabViewModel.class);

        binding.backButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        TopicCardAdapter adapter = new TopicCardAdapter(card -> openTopic(card.id),
                card -> openPractice(card.id));
        GridLayoutManager grid = new GridLayoutManager(requireContext(), 2);
        binding.topicList.setLayoutManager(grid);
        binding.topicList.setAdapter(adapter);

        viewModel.overview().observe(getViewLifecycleOwner(), overview -> {
            int notMastered = overview.total - overview.mastered;
            binding.totalLearned.statValue.setText(String.valueOf(overview.learned));
            binding.totalLearned.statLabel.setText(getString(R.string.vocab_learned_of, overview.total));
            binding.totalMastered.statValue.setText(String.valueOf(overview.mastered));
            binding.totalMastered.statLabel.setText(getString(R.string.vocab_mastered_of, overview.total));
            binding.reviewButton.setText(getString(R.string.vocab_review_all, notMastered));
            binding.reviewButton.setOnClickListener(v -> openPractice("all"));

            List<TopicCardAdapter.Card> cards = new ArrayList<>();
            for (VocabViewModel.Topic t : overview.topics) {
                cards.add(new TopicCardAdapter.Card(t.level, t.level, getString(R.string.vocab_word_count, t.total),
                        t.description, false, true));
            }
            adapter.submit(cards);
            // An odd last topic takes the full row instead of leaving an empty half.
            grid.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    boolean lastOfOdd = cards.size() % 2 == 1 && position == cards.size() - 1;
                    return lastOfOdd ? 2 : 1;
                }
            });
        });
        viewModel.loadOverview();
    }

    private void openTopic(String topicId) {
        Bundle args = new Bundle();
        args.putString("topicId", topicId);
        NavHostFragment.findNavController(this).navigate(R.id.action_vocab_to_topic, args);
    }

    private void openPractice(String topicId) {
        Bundle args = new Bundle();
        args.putString("topicId", topicId);
        NavHostFragment.findNavController(this).navigate(R.id.action_vocab_to_practice, args);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
