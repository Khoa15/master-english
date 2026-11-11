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

import com.masterenglish.app.R;
import com.masterenglish.app.data.local.entity.VocabularyEntity;
import com.masterenglish.app.databinding.FragmentFlashcardBinding;
import com.masterenglish.app.utils.AnimationUtils;
import com.masterenglish.app.utils.SoundManager;
import com.masterenglish.app.utils.SrsAlgorithm;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Flip a card to see the meaning, then grade it. The grade sets the next review date. */
@AndroidEntryPoint
public class FlashcardFragment extends Fragment {

    @Inject
    SoundManager soundManager;

    private FragmentFlashcardBinding binding;
    private FlashcardViewModel viewModel;
    private boolean showingBack;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFlashcardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(FlashcardViewModel.class);
        binding.closeButton.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        binding.flashCard.setOnClickListener(v -> flip());
        binding.againButton.setOnClickListener(v -> grade(SrsAlgorithm.Button.AGAIN));
        binding.hardButton.setOnClickListener(v -> grade(SrsAlgorithm.Button.HARD));
        binding.goodButton.setOnClickListener(v -> grade(SrsAlgorithm.Button.GOOD));
        binding.easyButton.setOnClickListener(v -> grade(SrsAlgorithm.Button.EASY));
        viewModel.deck().observe(getViewLifecycleOwner(), list -> render(list));
        viewModel.load();
    }

    private void render(List<VocabularyEntity> deck) {
        if (binding == null) {
            return;
        }
        if (deck == null || deck.isEmpty()) {
            binding.doneText.setText(R.string.flash_done);
            binding.doneText.setVisibility(View.VISIBLE);
            binding.cardGroup.setVisibility(View.GONE);
            return;
        }
        int done = viewModel.position();
        binding.progressText.setText(getString(R.string.review_due_format, deck.size() - done));
        binding.progressBar.setMax(deck.size());
        binding.progressBar.setProgress(done);
        if (viewModel.finished()) {
            binding.doneText.setText(R.string.flash_done);
            binding.doneText.setVisibility(View.VISIBLE);
            binding.cardGroup.setVisibility(View.GONE);
            return;
        }
        showFront(viewModel.current());
    }

    private void showFront(VocabularyEntity card) {
        showingBack = false;
        binding.cardWord.setText(card.word);
        binding.cardPhonetic.setText(card.phonetic == null ? "" : card.phonetic
                + (card.partOfSpeech == null ? "" : "  ·  " + card.partOfSpeech));
        binding.cardBack.setText(card.meaningVi + (card.exampleSentence == null ? "" : "\n\n"
                + card.exampleSentence + (card.exampleTranslation == null ? "" : "\n" + card.exampleTranslation)));
        binding.cardBack.setVisibility(View.GONE);
        binding.gradeRow.setVisibility(View.GONE);
        binding.showAnswerHint.setVisibility(View.VISIBLE);
        AnimationUtils.springPop(binding.flashCard);
    }

    private void flip() {
        if (showingBack || viewModel.current() == null) {
            return;
        }
        AnimationUtils.flip(binding.flashCard, () -> {
            showingBack = true;
            binding.cardBack.setVisibility(View.VISIBLE);
            binding.showAnswerHint.setVisibility(View.GONE);
            binding.gradeRow.setVisibility(View.VISIBLE);
        });
    }

    private void grade(SrsAlgorithm.Button button) {
        if (!showingBack) {
            return;
        }
        soundManager.play(SoundManager.Effect.POP);
        viewModel.grade(button);
        render(viewModel.deck().getValue());
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
