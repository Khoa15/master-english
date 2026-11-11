package com.masterenglish.app.ui.tutor;

import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.card.MaterialCardView;
import com.masterenglish.app.R;
import com.masterenglish.app.data.local.entity.SpeakingScenarioEntity;
import com.masterenglish.app.databinding.FragmentCustomPracticeBinding;

import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/** Page behind the Tutor "Create your own practice" card: create a situation and reuse saved ones. */
@AndroidEntryPoint
public class CustomPracticeFragment extends Fragment {

    private FragmentCustomPracticeBinding binding;
    private TutorViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCustomPracticeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(TutorViewModel.class);
        viewModel.scenarios().observe(getViewLifecycleOwner(), this::bindScenarios);

        binding.backButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.createNewButton.setOnClickListener(v -> {
            binding.createNewButton.setVisibility(View.GONE);
            binding.createPanel.setVisibility(View.VISIBLE);
        });
        binding.startCustomButton.setOnClickListener(v -> {
            String description = binding.customTopic.getText().toString().trim();
            if (description.isEmpty()) {
                return;
            }
            viewModel.saveScenario(description);
            binding.customTopic.setText("");
            binding.createPanel.setVisibility(View.GONE);
            binding.createNewButton.setVisibility(View.VISIBLE);
            openScenario(description);
        });
    }

    /** Lists saved situations, newest first; tapping one starts speaking with it. */
    private void bindScenarios(List<SpeakingScenarioEntity> scenarios) {
        if (binding == null) {
            return;
        }
        binding.savedScenarios.removeAllViews();
        binding.savedEmpty.setVisibility(scenarios == null || scenarios.isEmpty() ? View.VISIBLE : View.GONE);
        if (scenarios == null) {
            return;
        }
        int pad = dp(16);
        for (SpeakingScenarioEntity scenario : scenarios) {
            MaterialCardView card = new MaterialCardView(requireContext());
            card.setCardBackgroundColor(requireContext().getColor(R.color.ds_card));
            card.setRadius(dp(14));
            card.setCardElevation(0f);
            card.setClickable(true);
            card.setFocusable(true);
            TextView row = new TextView(requireContext());
            row.setText(scenario.description);
            row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            row.setTextColor(requireContext().getColor(R.color.ds_ink));
            row.setPadding(pad, pad, pad, pad);
            card.addView(row);
            card.setOnClickListener(v -> openScenario(scenario.description));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.topMargin = dp(8);
            binding.savedScenarios.addView(card, params);
        }
    }

    private void openScenario(String description) {
        Bundle args = new Bundle();
        args.putString("topicId", "custom");
        args.putString("customText", description);
        NavHostFragment.findNavController(this).navigate(R.id.action_custom_to_speaking, args);
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
