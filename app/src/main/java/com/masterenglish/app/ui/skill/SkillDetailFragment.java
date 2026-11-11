package com.masterenglish.app.ui.skill;

import android.graphics.drawable.GradientDrawable;
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
import com.masterenglish.app.data.repository.SkillProgress;
import com.masterenglish.app.databinding.FragmentSkillDetailBinding;
import com.masterenglish.app.ui.redesign.Rows;

import dagger.hilt.android.AndroidEntryPoint;

/** Detail for one skill from the Profile screen: level, what works, what to focus on, recent mistakes. */
@AndroidEntryPoint
public class SkillDetailFragment extends Fragment {

    private FragmentSkillDetailBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSkillDetailBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String skillId = requireArguments().getString("skillId", SkillProgress.LISTENING);
        int nameRes = nameRes(skillId);
        String name = getString(nameRes);

        binding.skillName.setText(name);
        binding.backButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.practiceButton.setText(getString(R.string.skill_practice, name));
        binding.practiceButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(R.id.reviewFragment));

        // The summary sentence was placeholder copy; the real figures are the progress bar and label.
        binding.skillSummary.setVisibility(View.GONE);

        SkillDetailViewModel viewModel = new ViewModelProvider(this).get(SkillDetailViewModel.class);
        viewModel.skills().observe(getViewLifecycleOwner(), skills -> {
            SkillProgress.Summary skill = skills.get(skillId);
            if (skill != null) {
                bind(skill);
            }
        });
        viewModel.load();
    }

    private void bind(SkillProgress.Summary skill) {
        if (skill.level == null) {
            binding.skillLevel.setVisibility(View.GONE);
        } else {
            binding.skillLevel.setVisibility(View.VISIBLE);
            binding.skillLevel.setText(skill.level);
            GradientDrawable levelBg = new GradientDrawable();
            levelBg.setCornerRadius(999f);
            levelBg.setColor(ContextCompat.getColor(requireContext(), R.color.ds_chip));
            binding.skillLevel.setBackground(levelBg);
        }

        binding.skillProgress.setProgressCompat(skill.percent(), false);
        binding.skillProgressLabel.setText(skill.total == 0
                ? getString(R.string.practice_section_empty)
                : getString(R.string.skill_progress_label, skill.completed, skill.total));

        int green = ContextCompat.getColor(requireContext(), R.color.ds_green_ink);
        int greenTint = ContextCompat.getColor(requireContext(), R.color.ds_green_tint);
        int amberInk = ContextCompat.getColor(requireContext(), R.color.ds_amber_ink);
        int amberTint = ContextCompat.getColor(requireContext(), R.color.ds_amber_tint);
        binding.workingContainer.removeAllViews();
        for (String item : skill.working) {
            Rows.add(binding.workingContainer, item, null, null, null, null, "✓", green, greenTint);
        }
        if (skill.working.isEmpty()) {
            Rows.add(binding.workingContainer, getString(R.string.skill_none), null, null, null, null, null, null, null);
        }
        binding.focusContainer.removeAllViews();
        for (String item : skill.focus) {
            Rows.add(binding.focusContainer, item, null, null, null, null, "!", amberInk, amberTint);
        }
        if (skill.focus.isEmpty()) {
            Rows.add(binding.focusContainer, getString(R.string.skill_none), null, null, null, null, null, null, null);
        }

        binding.recentTitle.setText(R.string.skill_recent_title);
        binding.recentContainer.removeAllViews();
        int red = ContextCompat.getColor(requireContext(), R.color.ds_red);
        int redTint = ContextCompat.getColor(requireContext(), R.color.ds_red_tint);
        for (SkillProgress.Mistake mistake : skill.recent) {
            Rows.add(binding.recentContainer, mistake.tag, null,
                    getString(R.string.skill_mistake_count, mistake.count), red, redTint, null, null, null);
        }
        if (skill.recent.isEmpty()) {
            Rows.add(binding.recentContainer, getString(R.string.skill_none), null, null, null, null, null, null, null);
        }
    }

    private static int nameRes(String skillId) {
        switch (skillId) {
            case SkillProgress.READING:
                return R.string.onboard_skill_reading;
            case SkillProgress.GRAMMAR:
                return R.string.onboard_skill_grammar;
            case SkillProgress.SPEAKING:
                return R.string.onboard_skill_speaking;
            case SkillProgress.WRITING:
                return R.string.onboard_skill_writing;
            default:
                return R.string.onboard_skill_listening;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
