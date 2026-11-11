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
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.masterenglish.app.R;
import com.masterenglish.app.databinding.FragmentVocabCompleteBinding;
import com.masterenglish.app.ui.redesign.Rows;

import java.util.ArrayList;

import dagger.hilt.android.AndroidEntryPoint;

/** Shown after a vocabulary practice round: score, progress and the words still to review. */
@AndroidEntryPoint
public class VocabCompleteFragment extends Fragment {

    private FragmentVocabCompleteBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVocabCompleteBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        int correct = args.getInt("correct");
        int total = args.getInt("total");
        ArrayList<String> roundWordIds = args.getStringArrayList("roundWordIds");
        ArrayList<String> wrongWordIds = args.getStringArrayList("wrongWordIds");
        VocabViewModel viewModel = new ViewModelProvider(this).get(VocabViewModel.class);

        binding.scoreTitle.setText(R.string.vocab_complete_title);
        binding.scoreBody.setText(getString(R.string.vocab_complete_body, correct, total));
        binding.statCorrect.statValue.setText(getString(R.string.vocab_stat_fraction, correct, total));
        binding.statCorrect.statLabel.setText(R.string.vocab_stat_correct);
        binding.statMoved.statValue.setText(String.valueOf(correct));
        binding.statMoved.statLabel.setText(R.string.vocab_stat_moved);

        viewModel.summary().observe(getViewLifecycleOwner(), summary -> {
            binding.statMastered.statValue.setText(String.valueOf(summary.masteredCount));
            binding.statMastered.statLabel.setText(R.string.vocab_stat_mastered);

            binding.reviewContainer.removeAllViews();
            for (VocabViewModel.Word w : summary.stillToReview) {
                int[] colors = statusColors(w.status);
                Rows.add(binding.reviewContainer, w.word, w.meaning, statusLabel(w.status), colors[0], colors[1],
                        null, null, null);
            }
            int more = wrongWordIds == null ? 0 : wrongWordIds.size() - summary.stillToReview.size();
            binding.moreText.setText(getString(R.string.vocab_more_words, more));
            binding.moreText.setVisibility(more > 0 ? View.VISIBLE : View.GONE);
        });
        viewModel.loadSummary(roundWordIds == null ? new ArrayList<>() : roundWordIds,
                wrongWordIds == null ? new ArrayList<>() : wrongWordIds);

        binding.closeButton.setOnClickListener(v -> popToVocabulary());
        binding.doneButton.setOnClickListener(v -> popToVocabulary());
        binding.practiceAgainButton.setOnClickListener(v -> {
            Bundle next = new Bundle();
            next.putString("topicId", "all");
            NavOptions options = new NavOptions.Builder()
                    .setPopUpTo(R.id.vocabCompleteFragment, true)
                    .build();
            NavHostFragment.findNavController(this).navigate(R.id.vocabPracticeFragment, next, options);
        });
    }

    private void popToVocabulary() {
        NavHostFragment.findNavController(this).popBackStack(R.id.vocabularyFragment, false);
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
