package com.masterenglish.app.ui.tutor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;

import com.masterenglish.app.R;
import com.masterenglish.app.databinding.FragmentSpeakingPracticeBinding;

/**
 * Voice chat with the tutor: tutor messages on the left, the learner's voice messages on the right.
 * Recording and transcription are not wired yet; the stop button only toggles its state.
 */
public class SpeakingPracticeFragment extends Fragment {

    private FragmentSpeakingPracticeBinding binding;
    private boolean recording = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSpeakingPracticeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String topicId = requireArguments().getString("topicId", "job-interview");

        binding.backButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.endButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("topicId", topicId);
            NavHostFragment.findNavController(this).navigate(R.id.action_speaking_to_summary, args);
        });
        binding.stopButton.setOnClickListener(v -> {
            recording = !recording;
            binding.stopButton.setIconResource(recording ? R.drawable.ic_pause : R.drawable.ic_mic);
        });
        binding.hintButton.setOnClickListener(v -> Toast.makeText(requireContext(),
                R.string.speaking_hint_text, Toast.LENGTH_LONG).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
