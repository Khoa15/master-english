package com.masterenglish.app.ui.roadmap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.List;

import com.google.android.material.snackbar.Snackbar;
import com.masterenglish.app.R;
import com.masterenglish.app.data.local.Enums.NodeStatus;
import com.masterenglish.app.data.local.entity.UserEntity;
import com.masterenglish.app.data.repository.RoadmapItem;
import com.masterenglish.app.databinding.FragmentRoadmapBinding;
import com.masterenglish.app.ui.common.PullToRefresh;
import com.masterenglish.app.ui.common.SkeletonView;
import com.masterenglish.app.ui.common.SmoothScroll;
import com.masterenglish.app.ui.common.TabFragment;
import com.masterenglish.app.utils.Levels;
import com.masterenglish.app.utils.AnimationUtils;
import com.masterenglish.app.utils.SoundManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Main tab: streak, gems, XP and the winding roadmap. */
@AndroidEntryPoint
public class RoadmapFragment extends TabFragment {

    @Inject
    SoundManager soundManager;

    private FragmentRoadmapBinding binding;
    private RoadmapViewModel viewModel;
    private RoadmapAdapter adapter;
    private SwipeRefreshLayout refreshLayout;
    /** Scroll position saved when the tab is hidden, restored when its content is rebuilt. */
    private Parcelable listState;
    private boolean scrolledToCurrent;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRoadmapBinding.inflate(inflater, container, false);
        refreshLayout = PullToRefresh.wrap(requireContext(), binding.getRoot(), binding.roadmapList, this::onRefresh);
        return withSkeleton(refreshLayout, SkeletonView.Variant.ROADMAP);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(RoadmapViewModel.class);

        adapter = new RoadmapAdapter(new RoadmapAdapter.OnNodeClick() {
            @Override
            public void onActionClicked(RoadmapItem item) {
                soundManager.play(SoundManager.Effect.POP);
                onStartLesson(item.node.id);
            }

            @Override
            public void onNodeToggled(RoadmapItem item, boolean expanded) {
                soundManager.play(SoundManager.Effect.POP);
            }
        });
        binding.roadmapList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.roadmapList.setAdapter(adapter);
        // Items are the same height and each bind is not free, so keep more rows alive while scrolling.
        binding.roadmapList.setHasFixedSize(true);
        binding.roadmapList.setItemViewCacheSize(10);
        // The roadmap is positioned by its own layout code, so RecyclerView must not run move or
        // change animations on it: those animators reset the per-node translationX and shift nodes.
        binding.roadmapList.setItemAnimator(null);
        binding.roadmapList.addItemDecoration(new RoadmapPathDecoration(
                getResources().getDimension(R.dimen.path_stroke)));

        viewModel.user().observe(getViewLifecycleOwner(), this::bindHeader);
        viewModel.roadmap().observe(getViewLifecycleOwner(), this::bindRoadmap);
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
        if (binding == null) {
            return;
        }
        hideSkeleton();
        if (items == null || items.isEmpty()) {
            binding.emptyText.setVisibility(View.VISIBLE);
            return;
        }
        binding.emptyText.setVisibility(View.GONE);
        adapter.submitList(items, () -> restoreListPosition(items));
    }

    /**
     * Puts the list back where the learner left it. The first time the roadmap is shown there is no
     * saved position, so it scrolls to the current node instead.
     */
    private void restoreListPosition(List<RoadmapItem> items) {
        if (binding == null) {
            return;
        }
        RecyclerView.LayoutManager layout = binding.roadmapList.getLayoutManager();
        if (listState != null) {
            layout.onRestoreInstanceState(listState);
            listState = null;
            return;
        }
        if (scrolledToCurrent) {
            return;
        }
        scrolledToCurrent = true;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).status == NodeStatus.AVAILABLE) {
                SmoothScroll.toPosition(binding.roadmapList, Math.max(0, i - 1));
                break;
            }
        }
    }

    @Override
    public void onTabHidden() {
        if (binding == null) {
            return;
        }
        // The content is released below, so remember the scroll position for the next show.
        listState = binding.roadmapList.getLayoutManager().onSaveInstanceState();
        adapter.submitList(null);
        binding.emptyText.setVisibility(View.GONE);
        showSkeleton();
    }

    @Override
    public void onTabShown() {
        if (binding == null) {
            return;
        }
        bindHeader(viewModel.user().getValue());
        java.util.List<RoadmapItem> cached = viewModel.roadmap().getValue();
        if (cached != null) {
            bindRoadmap(cached);
        }
    }

    private void onRefresh() {
        viewModel.refresh(success -> PullToRefresh.finish(refreshLayout, success, binding == null ? null : binding.getRoot()));
    }

    private void onStartLesson(String nodeId) {
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
