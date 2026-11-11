package com.masterenglish.app.ui.tutor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.masterenglish.app.R;
import com.masterenglish.app.databinding.FragmentSpeakingSummaryBinding;

/**
 * Shown after a speaking session ends: time, turns, corrections and words used. Speech capture and
 * transcription are not wired yet, so there is nothing to report: the figures stay empty.
 */
public class SpeakingSummaryFragment extends Fragment {

    private FragmentSpeakingSummaryBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSpeakingSummaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String topicId = requireArguments().getString("topicId", "job-interview");

        binding.statTime.statValue.setText(R.string.summary_no_value);
        binding.statTime.statLabel.setText(R.string.summary_stat_time);
        binding.statTurns.statValue.setText(R.string.summary_no_value);
        binding.statTurns.statLabel.setText(R.string.summary_stat_turns);
        binding.statClear.statValue.setText(R.string.summary_no_value);
        binding.statClear.statLabel.setText(R.string.summary_stat_clear);

        binding.closeButton.setOnClickListener(v -> popToTutor());
        binding.doneButton.setOnClickListener(v -> popToTutor());
        binding.practiceAgainButton.setOnClickListener(v -> {
            NavController nav = NavHostFragment.findNavController(this);
            Bundle args = new Bundle();
            args.putString("topicId", topicId);
            NavOptions options = new NavOptions.Builder()
                    .setPopUpTo(R.id.speakingSummaryFragment, true)
                    .build();
            nav.navigate(R.id.speakingPracticeFragment, args, options);
        });
    }

    private void popToTutor() {
        NavHostFragment.findNavController(this).popBackStack(R.id.tutorFragment, false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
