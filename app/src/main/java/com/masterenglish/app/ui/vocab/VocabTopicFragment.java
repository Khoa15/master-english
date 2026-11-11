package com.masterenglish.app.ui.vocab;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.masterenglish.app.R;
import com.masterenglish.app.databinding.FragmentVocabTopicBinding;
import com.masterenglish.app.ui.redesign.Rows;

import dagger.hilt.android.AndroidEntryPoint;

/** Detail page for one vocabulary topic (one CEFR level): counts, sample words and a Start button. */
@AndroidEntryPoint
public class VocabTopicFragment extends Fragment {

    /** Words listed before the "+ N more" line. */
    private static final int SAMPLE_WORDS = 5;

    private FragmentVocabTopicBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVocabTopicBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String topicId = requireArguments().getString("topicId", "A1");
        VocabViewModel viewModel = new ViewModelProvider(this).get(VocabViewModel.class);

        binding.backButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.startButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("topicId", topicId);
            NavHostFragment.findNavController(this).navigate(R.id.action_vocab_topic_to_practice, args);
        });

        viewModel.overview().observe(getViewLifecycleOwner(), overview -> {
            VocabViewModel.Topic topic = overview.topic(topicId);
            if (topic == null) {
                return;
            }
            binding.topicTitle.setText(getString(R.string.vocab_topic_level, topic.level));
            binding.topicDescription.setText(topic.description);
            binding.countTotal.statValue.setText(String.valueOf(topic.total));
            binding.countTotal.statLabel.setText(R.string.vocab_count_total);
            binding.countLearned.statValue.setText(String.valueOf(topic.learned));
            binding.countLearned.statLabel.setText(R.string.vocab_count_learned);
            binding.countMastered.statValue.setText(String.valueOf(topic.mastered));
            binding.countMastered.statLabel.setText(R.string.vocab_count_mastered);

            binding.wordsContainer.removeAllViews();
            int shown = Math.min(SAMPLE_WORDS, topic.words.size());
            for (VocabViewModel.Word w : topic.words.subList(0, shown)) {
                int[] colors = statusColors(w.status);
                Rows.add(binding.wordsContainer, w.word, w.meaning, statusLabel(w.status), colors[0], colors[1],
                        null, null, null);
            }
            binding.moreText.setText(getString(R.string.vocab_more_words, topic.total - shown));
        });
        viewModel.loadOverview();
    }

    private String statusLabel(VocabViewModel.Status status) {
        switch (status) {
            case MASTERED:
                return getString(R.string.vocab_status_mastered);
            case LEARNING:
                return getString(R.string.vocab_status_learning);
            default:
                return getString(R.string.vocab_status_new);
        }
    }

    /** Returns {ink, tint} for a word status. */
    private int[] statusColors(VocabViewModel.Status status) {
        int ink;
        int tint;
        switch (status) {
            case MASTERED:
                ink = R.color.ds_green_ink;
                tint = R.color.ds_green_tint;
                break;
            case LEARNING:
                ink = R.color.ds_amber_ink;
                tint = R.color.ds_amber_tint;
                break;
            default:
                ink = R.color.ds_muted;
                tint = R.color.ds_chip;
                break;
        }
        return new int[]{ContextCompat.getColor(requireContext(), ink), ContextCompat.getColor(requireContext(), tint)};
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
