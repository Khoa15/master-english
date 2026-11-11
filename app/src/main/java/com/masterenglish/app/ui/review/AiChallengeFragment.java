package com.masterenglish.app.ui.review;

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

import com.airbnb.lottie.LottieDrawable;
import com.masterenglish.app.R;
import com.masterenglish.app.databinding.FragmentAiChallengeBinding;

import dagger.hilt.android.AndroidEntryPoint;

/** Role-play challenge with the mascot. Each reply is checked and mistakes go to the Weakness Clinic. */
@AndroidEntryPoint
public class AiChallengeFragment extends Fragment {

    private FragmentAiChallengeBinding binding;
    private AiChallengeViewModel viewModel;
    private ChatAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAiChallengeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(AiChallengeViewModel.class);
        adapter = new ChatAdapter();
        binding.chatList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.chatList.setAdapter(adapter);

        binding.closeButton.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        binding.startButton.setOnClickListener(v -> viewModel.start());
        binding.sendButton.setOnClickListener(v -> sendCurrentText());

        viewModel.messages().observe(getViewLifecycleOwner(), list -> {
            adapter.submit(list);
            binding.startButton.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
            if (!list.isEmpty()) {
                binding.chatList.scrollToPosition(list.size() - 1);
            }
        });
        viewModel.thinking().observe(getViewLifecycleOwner(), busy -> {
            binding.thinkingText.setVisibility(Boolean.TRUE.equals(busy) ? View.VISIBLE : View.GONE);
            binding.sendButton.setEnabled(!Boolean.TRUE.equals(busy));
            binding.mascot.setAnimation(Boolean.TRUE.equals(busy) ? R.raw.mascot_thinking : R.raw.mascot_wave);
            binding.mascot.setRepeatCount(LottieDrawable.INFINITE);
            binding.mascot.playAnimation();
        });
    }

    private void sendCurrentText() {
        if (binding.inputText.getText() == null) {
            return;
        }
        String text = binding.inputText.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        binding.inputText.setText("");
        viewModel.send(text);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
