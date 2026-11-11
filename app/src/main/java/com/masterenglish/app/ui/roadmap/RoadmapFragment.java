package com.masterenglish.app.ui.roadmap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.masterenglish.app.R;
import com.masterenglish.app.data.local.Enums.NodeStatus;
import com.masterenglish.app.data.local.entity.UserEntity;
import com.masterenglish.app.data.repository.RoadmapItem;
import com.masterenglish.app.databinding.FragmentRoadmapBinding;
import com.masterenglish.app.utils.Levels;
import com.masterenglish.app.utils.AnimationUtils;

import dagger.hilt.android.AndroidEntryPoint;

/** Main tab: streak, gems, XP and the winding roadmap. */
@AndroidEntryPoint
public class RoadmapFragment extends Fragment implements LessonPreviewSheet.Listener {

    private FragmentRoadmapBinding binding;
    private RoadmapViewModel viewModel;
    private RoadmapAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRoadmapBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(RoadmapViewModel.class);

        adapter = new RoadmapAdapter(this::onNodeClicked);
        binding.roadmapList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.roadmapList.setAdapter(adapter);
        binding.roadmapList.addItemDecoration(new RoadmapPathDecoration(
                getResources().getDimension(R.dimen.path_stroke)));

        binding.mascotButton.setOnClickListener(v -> showTip());

        viewModel.user().observe(getViewLifecycleOwner(), this::bindHeader);
        viewModel.roadmap().observe(getViewLifecycleOwner(), this::bindRoadmap);
        AnimationUtils.springPop(binding.mascotButton);
    }

    private void bindHeader(UserEntity user) {
        if (user == null) {
            return;
        }
        binding.streakText.setText(getString(R.string.roadmap_streak_format, user.streakCount));
        binding.gemsText.setText(String.valueOf(user.gemCount));
        binding.expText.setText(String.valueOf(user.experiencePoints));
        binding.tierText.setText(Levels.tierName(user.experiencePoints));
        binding.tierProgress.setProgress(Levels.progressPercent(user.experiencePoints));
    }

    private void bindRoadmap(java.util.List<RoadmapItem> items) {
        if (items == null || items.isEmpty()) {
            binding.emptyText.setVisibility(View.VISIBLE);
            return;
        }
        binding.emptyText.setVisibility(View.GONE);
        adapter.submitList(items);
        // Scroll to the first node the learner can work on.
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).status == NodeStatus.AVAILABLE) {
                binding.roadmapList.scrollToPosition(Math.max(0, i - 1));
                break;
            }
        }
    }

    private void onNodeClicked(RoadmapItem item) {
        if (item.status == NodeStatus.LOCKED) {
            Snackbar.make(binding.getRoot(), R.string.roadmap_locked_hint, Snackbar.LENGTH_SHORT).show();
            return;
        }
        LessonPreviewSheet.newInstance(item).show(getChildFragmentManager(), "lesson_preview");
    }

    @Override
    public void onStartLesson(String nodeId) {
        Bundle args = new Bundle();
        args.putString("nodeId", nodeId);
        NavHostFragment.findNavController(this).navigate(R.id.action_roadmap_to_lesson, args);
    }

    private void showTip() {
        viewModel.dailyTip(tip -> {
            if (binding != null) {
                Snackbar.make(binding.getRoot(), tip, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
