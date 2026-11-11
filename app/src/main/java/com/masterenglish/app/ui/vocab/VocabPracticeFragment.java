package com.masterenglish.app.ui.vocab;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.masterenglish.app.R;
import com.masterenglish.app.databinding.FragmentVocabPracticeBinding;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Vocabulary practice: one question per screen, drawn from the learner's words. Each answer gets
 * feedback straight away; the last question opens the completion screen.
 */
@AndroidEntryPoint
public class VocabPracticeFragment extends Fragment {

    private FragmentVocabPracticeBinding binding;
    private VocabViewModel viewModel;
    private List<VocabViewModel.Challenge> challenges = new ArrayList<>();
    private final List<String> roundWordIds = new ArrayList<>();
    private final List<String> wrongWordIds = new ArrayList<>();
    private int correctCount;
    private int index;
    private int selected = -1;
    private boolean checked;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentVocabPracticeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String topicId = requireArguments().getString("topicId", "all");
        viewModel = new ViewModelProvider(this).get(VocabViewModel.class);

        binding.closeButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.actionButton.setOnClickListener(v -> onAction());
        binding.actionButton.setEnabled(false);
        binding.typeChip.setText(R.string.vocab_chip_word);

        viewModel.challenges().observe(getViewLifecycleOwner(), list -> {
            challenges = list;
            roundWordIds.clear();
            for (VocabViewModel.Challenge c : list) {
                roundWordIds.add(c.wordId);
            }
            index = 0;
            if (list.isEmpty()) {
                showEmpty();
            } else {
                render();
            }
        });
        viewModel.startPractice(topicId);
    }

    private void showEmpty() {
        binding.progress.setProgressCompat(0, false);
        binding.challengeTitle.setText(R.string.vocab_practice_empty);
        binding.challengeContext.setVisibility(View.GONE);
        binding.feedbackText.setVisibility(View.GONE);
        binding.choicesContainer.removeAllViews();
        binding.actionButton.setEnabled(false);
    }

    private void render() {
        VocabViewModel.Challenge c = challenges.get(index);
        selected = -1;
        checked = false;

        binding.progress.setProgressCompat(index * 100 / challenges.size(), true);
        binding.challengeTitle.setText(c.prompt);
        binding.challengeContext.setText(c.context);
        binding.challengeContext.setVisibility(View.VISIBLE);
        binding.feedbackText.setVisibility(View.GONE);
        binding.actionButton.setText(R.string.ds_check);
        binding.actionButton.setEnabled(false);

        binding.choicesContainer.removeAllViews();
        for (int i = 0; i < c.options.size(); i++) {
            final int position = i;
            MaterialButton option = optionButton(c.options.get(i));
            option.setOnClickListener(v -> {
                if (!checked) {
                    selected = position;
                    binding.actionButton.setEnabled(true);
                    for (int j = 0; j < binding.choicesContainer.getChildCount(); j++) {
                        styleOption((MaterialButton) binding.choicesContainer.getChildAt(j), j == position ? R.color.ds_ink : R.color.ds_line);
                    }
                }
            });
            binding.choicesContainer.addView(option);
        }
    }

    private void onAction() {
        VocabViewModel.Challenge c = challenges.get(index);
        if (!checked) {
            checked = true;
            boolean correct = selected == c.correct;
            viewModel.recordAnswer(c, correct);
            if (correct) {
                correctCount++;
            } else {
                wrongWordIds.add(c.wordId);
            }
            binding.feedbackText.setText((correct ? getString(R.string.ds_correct) + " " : getString(R.string.ds_not_quite) + " ")
                    + c.explanation);
            binding.feedbackText.setTextColor(ContextCompat.getColor(requireContext(),
                    correct ? R.color.ds_green_ink : R.color.ds_red));
            binding.feedbackText.setBackgroundResource(correct ? R.drawable.bg_ds_feedback_ok : R.drawable.bg_ds_feedback_bad);
            binding.feedbackText.setVisibility(View.VISIBLE);
            for (int j = 0; j < binding.choicesContainer.getChildCount(); j++) {
                MaterialButton option = (MaterialButton) binding.choicesContainer.getChildAt(j);
                if (j == c.correct) {
                    styleOption(option, R.color.ds_green);
                } else if (j == selected) {
                    styleOption(option, R.color.ds_red);
                }
            }
            binding.actionButton.setText(R.string.ds_continue);
            binding.actionButton.setEnabled(true);
            return;
        }
        index++;
        if (index >= challenges.size()) {
            Bundle args = new Bundle();
            args.putInt("correct", correctCount);
            args.putInt("total", challenges.size());
            args.putStringArrayList("roundWordIds", new ArrayList<>(roundWordIds));
            args.putStringArrayList("wrongWordIds", new ArrayList<>(wrongWordIds));
            NavOptions options = new NavOptions.Builder()
                    .setPopUpTo(R.id.vocabPracticeFragment, true)
                    .build();
            NavHostFragment.findNavController(this).navigate(R.id.action_vocab_practice_to_complete, args, options);
            return;
        }
        render();
    }

    private MaterialButton optionButton(String text) {
        MaterialButton button = new MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(ContextCompat.getColor(requireContext(), R.color.ds_ink));
        styleOption(button, R.color.ds_line);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 10;
        button.setLayoutParams(lp);
        return button;
    }

    private void styleOption(MaterialButton button, int strokeColor) {
        button.setStrokeColor(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(requireContext(), strokeColor)));
        button.setStrokeWidth(2);
        button.setCornerRadius(getResources().getDimensionPixelSize(R.dimen.ds_option_radius));
        button.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.ds_option_height));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
