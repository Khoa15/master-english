package com.masterenglish.app.ui.tutor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.masterenglish.app.R;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.local.prefs.OnboardingAnswers;
import com.masterenglish.app.databinding.FragmentTutorBinding;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Tutor tab: the learner's onboarding goal and a custom speaking practice. */
@AndroidEntryPoint
public class TutorFragment extends Fragment {

    @Inject
    AppPreferences preferences;

    private FragmentTutorBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTutorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        OnboardingAnswers answers = preferences.getOnboardingAnswers();
        binding.onboardingCard.setVisibility(answers == null ? View.GONE : View.VISIBLE);
        if (answers != null) {
            binding.onboardingBody.setText(getString(R.string.tutor_onboarding_body, answers.goal));
        }

        binding.startCustomButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("topicId", "custom");
            NavHostFragment.findNavController(this).navigate(R.id.action_tutor_to_speaking, args);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
