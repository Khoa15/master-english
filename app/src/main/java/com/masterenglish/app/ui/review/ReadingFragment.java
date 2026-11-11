package com.masterenglish.app.ui.review;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.chip.Chip;
import com.masterenglish.app.R;
import com.masterenglish.app.data.local.entity.ReadingEntity;
import com.masterenglish.app.data.local.entity.VocabularyEntity;
import com.masterenglish.app.databinding.FragmentReadingBinding;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Reading practice. Every word of the passage is tappable: a balloon shows its meaning
 * from the vocabulary table, anchored just above the tapped word.
 */
@AndroidEntryPoint
public class ReadingFragment extends Fragment {

    private static final Pattern WORD = Pattern.compile("[A-Za-z][A-Za-z']*");

    private FragmentReadingBinding binding;
    private ReadingViewModel viewModel;
    private PopupWindow balloon;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReadingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ReadingViewModel.class);
        binding.closeButton.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        binding.passage.setMovementMethod(LinkMovementMethod.getInstance());

        viewModel.passage().observe(getViewLifecycleOwner(), this::bindPassage);
        viewModel.questions().observe(getViewLifecycleOwner(), list -> {
            if (list != null && !list.isEmpty()) {
                QuizSection.render(getLayoutInflater(), binding.quizContainer, list,
                        viewModel.progress(), viewModel.userId());
            }
        });
        viewModel.load(requireArguments().getString("readingId", ""));
    }

    private void bindPassage(ReadingEntity reading) {
        if (reading == null || binding == null) {
            return;
        }
        binding.title.setText(reading.title);
        binding.wordCount.setText(getString(R.string.reading_meta_format, reading.wordCount, reading.cefrLevel));
        binding.passage.setText(tappable(reading.passageText));

        binding.keyVocabGroup.removeAllViews();
        for (String word : reading.keyVocabulary) {
            Chip chip = new Chip(requireContext());
            chip.setText(word);
            chip.setOnClickListener(v -> lookUp(word, chip));
            binding.keyVocabGroup.addView(chip);
        }
    }

    /** Wraps each English word in a ClickableSpan that knows where it sits in the text. */
    private SpannableStringBuilder tappable(String passage) {
        SpannableStringBuilder builder = new SpannableStringBuilder(passage);
        Matcher matcher = WORD.matcher(passage);
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String word = matcher.group();
            builder.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    showBalloonFor(word, (TextView) widget, start);
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.setUnderlineText(false);
                    ds.setColor(requireContext().getColor(R.color.neutral_text));
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }

    private void showBalloonFor(String word, TextView anchor, int offset) {
        android.text.Layout layout = anchor.getLayout();
        if (layout == null) {
            return;
        }
        int line = layout.getLineForOffset(offset);
        float x = layout.getPrimaryHorizontal(offset) + anchor.getTotalPaddingLeft();
        float y = layout.getLineTop(line) + anchor.getTotalPaddingTop();
        float lineHeight = layout.getLineBottom(line) - layout.getLineTop(line);
        lookUp(word, anchor, x, y, lineHeight);
    }

    private void lookUp(String word, View anchor) {
        lookUp(word, anchor, 0, 0, 0);
    }

    private void lookUp(String word, View anchor, float x, float y, float lineHeight) {
        String key = word.toLowerCase(Locale.ROOT).replace("'", "");
        viewModel.lookUp(key, entry -> {
            String text = entry == null
                    ? word + ": " + getString(R.string.reading_not_in_dict)
                    : entry.word + (entry.phonetic == null ? "" : "  " + entry.phonetic) + "\n" + entry.meaningVi;
            if (binding != null) {
                showBalloon(anchor, text, x, y, lineHeight);
            }
        });
    }

    private void showBalloon(View anchor, String text, float x, float y, float lineHeight) {
        dismissBalloon();
        TextView content = new TextView(requireContext());
        content.setText(text);
        content.setTextSize(15f);
        content.setTypeface(Typeface.DEFAULT);
        content.setTextColor(requireContext().getColor(R.color.neutral_text));
        content.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_bubble));
        content.setPadding(28, 18, 28, 18);
        content.setMaxWidth((int) (anchor.getWidth() * 0.8f));

        balloon = new PopupWindow(content, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        balloon.setOutsideTouchable(true);
        balloon.setElevation(12f);
        content.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        int left = (int) (location[0] + x);
        int top = (int) (location[1] + y - content.getMeasuredHeight() - lineHeight / 2f);
        balloon.showAtLocation(anchor, Gravity.NO_GRAVITY, Math.max(0, left), Math.max(0, top));
    }

    private void dismissBalloon() {
        if (balloon != null && balloon.isShowing()) {
            balloon.dismiss();
        }
        balloon = null;
    }

    @Override
    public void onPause() {
        dismissBalloon();
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        dismissBalloon();
        binding = null;
        super.onDestroyView();
    }
}
