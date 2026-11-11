package com.masterenglish.app.ui.lesson;

import android.Manifest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.airbnb.lottie.LottieDrawable;
import com.masterenglish.app.R;
import com.masterenglish.app.data.local.entity.LessonQuestionEntity;
import com.masterenglish.app.data.repository.LessonReward;
import com.masterenglish.app.ui.lesson.questions.VoiceView;
import com.masterenglish.app.databinding.FragmentLessonBinding;
import com.masterenglish.app.utils.AnimationUtils;
import com.masterenglish.app.utils.Haptics;
import com.masterenglish.app.utils.SoundManager;
import com.masterenglish.app.utils.TextUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import nl.dionsegijn.konfetti.core.Party;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.Position;
import nl.dionsegijn.konfetti.core.emitter.Emitter;

/**
 * Runs one lesson: question by question, with immediate feedback (sound, haptics,
 * mascot reaction), then a result screen with confetti and rewards.
 */
@AndroidEntryPoint
public class LessonFragment extends Fragment {

    @Inject
    SoundManager soundManager;

    private FragmentLessonBinding binding;
    private LessonViewModel viewModel;
    private QuestionView current;
    private LessonQuestionEntity currentQuestion;
    private List<LessonQuestionEntity> questions;
    private int index;
    private boolean checked;

    private final ActivityResultLauncher<String> micPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted && current instanceof VoiceView) {
                    ((VoiceView) current).startListening();
                } else if (!granted && binding != null) {
                    Toast.makeText(requireContext(), R.string.voice_no_permission, Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLessonBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(LessonViewModel.class);
        String nodeId = requireArguments().getString("nodeId", "");

        binding.closeButton.setOnClickListener(v -> confirmExit());
        binding.actionButton.setOnClickListener(v -> {
            if (checked) {
                advance();
            } else {
                check();
            }
        });
        binding.resultButton.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        confirmExit();
                    }
                });

        viewModel.questions().observe(getViewLifecycleOwner(), this::onQuestionsLoaded);
        viewModel.reward().observe(getViewLifecycleOwner(), this::showResult);
        viewModel.load(nodeId);
    }

    private void onQuestionsLoaded(List<LessonQuestionEntity> loaded) {
        if (loaded == null) {
            return;
        }
        questions = loaded;
        if (loaded.isEmpty()) {
            binding.emptyText.setVisibility(View.VISIBLE);
            binding.actionButton.setText(R.string.lesson_finish);
            binding.actionButton.setEnabled(true);
            binding.actionButton.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
            return;
        }
        showQuestion(0);
    }

    private void showQuestion(int position) {
        index = position;
        checked = false;
        currentQuestion = questions.get(position);
        binding.questionContainer.removeAllViews();
        current = QuestionViews.create(getLayoutInflater(), binding.questionContainer, currentQuestion,
                this::onAnswerChanged, () -> micPermission.launch(Manifest.permission.RECORD_AUDIO));
        binding.questionContainer.addView(current.getView());
        binding.feedbackCard.setVisibility(View.GONE);
        binding.actionButton.setText(R.string.lesson_check);
        binding.actionButton.setEnabled(false);
        AnimationUtils.tweenProgress(binding.progressBar, Math.round(100f * position / questions.size()));
        playMascot(R.raw.mascot_wave, false);
    }

    /** Reorder and matching widgets report their initial state from their constructor, before {@code current} is set. */
    private void onAnswerChanged() {
        if (!checked && binding != null && current != null) {
            binding.actionButton.setEnabled(current.hasAnswer());
        }
    }

    private void check() {
        Object answer = current.getAnswer();
        boolean correct = QuestionModels.isCorrect(currentQuestion, answer);
        viewModel.recordAnswer(currentQuestion, correct);
        checked = true;
        current.lock();

        String explanation = currentQuestion.explanation == null ? "" : currentQuestion.explanation;
        binding.feedbackTitle.setText(correct ? R.string.lesson_correct : R.string.lesson_wrong);
        binding.feedbackTitle.setTextColor(requireContext().getColor(correct ? R.color.brand_green_dark : R.color.danger_red));
        binding.feedbackBody.setText(explanation);
        binding.feedbackCard.setVisibility(View.VISIBLE);
        AnimationUtils.springPop(binding.feedbackCard);

        if (correct) {
            soundManager.play(SoundManager.Effect.DING);
            playMascot(R.raw.mascot_celebrate, false);
        } else {
            soundManager.play(SoundManager.Effect.BUZZ_ERROR);
            Haptics.lightBuzz(requireContext());
            AnimationUtils.shake(binding.questionContainer);
            playMascot(R.raw.mascot_thinking, true);
            current.reveal(QuestionModels.correctAnswer(currentQuestion));
        }
        binding.actionButton.setText(R.string.lesson_continue);
        binding.actionButton.setEnabled(true);
    }

    private void advance() {
        if (index + 1 < questions.size()) {
            showQuestion(index + 1);
            return;
        }
        binding.actionButton.setEnabled(false);
        binding.questionContainer.setVisibility(View.GONE);
        binding.feedbackCard.setVisibility(View.GONE);
        viewModel.finish();
    }

    private void showResult(LessonReward reward) {
        if (binding == null || reward == null) {
            return;
        }
        binding.actionButton.setVisibility(View.GONE);
        binding.progressBar.setProgress(100);
        binding.resultContainer.setVisibility(View.VISIBLE);
        binding.resultTitle.setText(R.string.lesson_result_title);
        binding.resultStats.setText(getString(R.string.lesson_result_stats,
                viewModel.correctCount(), questions.size(), reward.experience, reward.gems));
        binding.resultButton.setVisibility(View.VISIBLE);
        playMascot(R.raw.mascot_celebrate, false);
        soundManager.play(reward.firstCompletion ? SoundManager.Effect.VICTORY : SoundManager.Effect.LEVEL_UP);
        celebrate();
        if (!reward.newBadges.isEmpty()) {
            Toast.makeText(requireContext(), "Mở khóa huy hiệu: " + TextUtils.joinWords(reward.newBadges),
                    Toast.LENGTH_LONG).show();
        }
    }

    /** A burst of paper confetti from the top of the screen. */
    private void celebrate() {
        Party party = new PartyFactory(new Emitter(2L, TimeUnit.SECONDS).max(160))
                .angle(270)
                .spread(120)
                .setSpeedBetween(4f, 12f)
                .position(new Position.Relative(0.5, 0.0))
                .colors(Arrays.asList(0xFF58CC02, 0xFFFFC800, 0xFF1CB0F6, 0xFFCE82FF, 0xFFFF4B4B))
                .timeToLive(3000L)
                .build();
        binding.konfettiView.start(party);
    }

    private void playMascot(int rawRes, boolean once) {
        binding.mascot.setAnimation(rawRes);
        binding.mascot.setRepeatCount(once ? 0 : com.airbnb.lottie.LottieDrawable.INFINITE);
        binding.mascot.playAnimation();
    }

    private void confirmExit() {
        if (binding == null) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.lesson_exit_title)
                .setMessage(R.string.lesson_exit_message)
                .setPositiveButton(R.string.lesson_exit_confirm,
                        (dialog, which) -> NavHostFragment.findNavController(this).popBackStack())
                .setNegativeButton(R.string.lesson_cancel, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        if (current != null) {
            current.release();
            current = null;
        }
        binding = null;
        super.onDestroyView();
    }
}
