package com.masterenglish.app.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.masterenglish.app.MainActivity;
import com.masterenglish.app.R;
import com.masterenglish.app.databinding.FragmentOnboardingBinding;

import dagger.hilt.android.AndroidEntryPoint;

/** Three-page introduction with the mascot and a short learning survey. */
@AndroidEntryPoint
public class OnboardingFragment extends Fragment {

    private static final int PAGE_COUNT = 3;
    private static final int[] PAGE_LAYOUTS = {
            R.layout.layout_onboarding_intro,
            R.layout.layout_onboarding_goal,
            R.layout.layout_onboarding_level
    };

    private static final String[] GOAL_VALUES = {"Giao tiếp trong công việc", "Phỏng vấn IT", "Đọc tài liệu kỹ thuật"};
    private static final String[] LEVEL_VALUES = {"A1", "A2", "B1", "B2"};

    private FragmentOnboardingBinding binding;
    private OnboardingViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);
        binding.pager.setAdapter(new PagesAdapter());
        binding.pager.setUserInputEnabled(true);
        binding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                binding.nextButton.setText(position == PAGE_COUNT - 1 ? R.string.onboard_start : R.string.onboard_next);
            }
        });
        binding.nextButton.setOnClickListener(v -> {
            int current = binding.pager.getCurrentItem();
            if (current < PAGE_COUNT - 1) {
                binding.pager.setCurrentItem(current + 1, true);
            } else {
                finishOnboarding();
            }
        });
    }

    private void finishOnboarding() {
        viewModel.saveAnswers();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).requestNotificationPermissionIfNeeded();
        }
        NavHostFragment.findNavController(this).navigate(R.id.action_onboarding_to_auth);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    /** Pages are inflated by hand because each one is a different layout with its own controls. */
    private final class PagesAdapter extends RecyclerView.Adapter<PageHolder> {

        @NonNull
        @Override
        public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View page = LayoutInflater.from(parent.getContext()).inflate(PAGE_LAYOUTS[viewType], parent, false);
            page.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            return new PageHolder(page);
        }

        @Override
        public void onBindViewHolder(@NonNull PageHolder holder, int position) {
            if (position == 1) {
                bindGoalPage(holder.itemView);
            } else if (position == 2) {
                bindLevelPage(holder.itemView);
            }
        }

        @Override
        public int getItemCount() {
            return PAGE_COUNT;
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }
    }

    private void bindGoalPage(View page) {
        MaterialButtonToggleGroup group = page.findViewById(R.id.goalGroup);
        int[] buttons = {R.id.goalWork, R.id.goalInterview, R.id.goalDocs};
        group.check(buttons[indexOf(GOAL_VALUES, viewModel.goal().getValue())]);
        group.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (isChecked) {
                for (int i = 0; i < buttons.length; i++) {
                    if (buttons[i] == checkedId) {
                        viewModel.setGoal(GOAL_VALUES[i]);
                    }
                }
            }
        });
    }

    private void bindLevelPage(View page) {
        MaterialButtonToggleGroup levels = page.findViewById(R.id.levelGroup);
        int[] levelButtons = {R.id.levelA1, R.id.levelA2, R.id.levelB1, R.id.levelB2};
        levels.check(levelButtons[indexOf(LEVEL_VALUES, viewModel.level().getValue())]);
        levels.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (isChecked) {
                for (int i = 0; i < levelButtons.length; i++) {
                    if (levelButtons[i] == checkedId) {
                        viewModel.setLevel(LEVEL_VALUES[i]);
                    }
                }
            }
        });

        MaterialButtonToggleGroup minutes = page.findViewById(R.id.minutesGroup);
        int[] minuteButtons = {R.id.min5, R.id.min10, R.id.min15};
        int[] minuteValues = {5, 10, 15};
        Integer current = viewModel.minutes().getValue();
        for (int i = 0; i < minuteValues.length; i++) {
            if (current != null && current == minuteValues[i]) {
                minutes.check(minuteButtons[i]);
            }
        }
        minutes.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (isChecked) {
                for (int i = 0; i < minuteButtons.length; i++) {
                    if (minuteButtons[i] == checkedId) {
                        viewModel.setMinutes(minuteValues[i]);
                    }
                }
            }
        });
    }

    private static int indexOf(String[] values, String value) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(value)) {
                return i;
            }
        }
        return 0;
    }

    private static final class PageHolder extends RecyclerView.ViewHolder {
        PageHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
