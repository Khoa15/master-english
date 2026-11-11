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
import androidx.navigation.fragment.NavHostFragment;

import com.masterenglish.app.R;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.local.prefs.OnboardingAnswers;
import com.masterenglish.app.databinding.FragmentTutorBinding;
import com.masterenglish.app.databinding.ItemTopicCardBinding;
import com.masterenglish.app.ui.common.SkeletonView;
import com.masterenglish.app.ui.common.TabFragment;
import com.masterenglish.app.ui.redesign.TopicCardAdapter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Tutor tab: the learner's onboarding answers, speaking topics and a custom speaking practice. */
@AndroidEntryPoint
public class TutorFragment extends TabFragment {

    @Inject
    AppPreferences preferences;

    private FragmentTutorBinding binding;

    /** Speaking topics as shown on the Tutor artboard: id, title, level, minutes, success line, recommended. */
    private static final String[][] TOPICS = {
            {"job-interview", "Job interview", "B1", "15", "82% success · 4 sessions", "1"},
            {"travel", "Travel", "A2", "10", "76% success · 3 sessions", "1"},
            {"sport", "Sport", "B1", "10", "48% success · 2 sessions", "1"},
            {"news", "News", "B2", "15", "35% success · 2 sessions", "1"},
            {"daily-life", "Daily life", "A2", "8", "90% success · 5 sessions", "0"},
            {"introduce-myself", "Introduce myself", "A1", "5", "Not tried yet", "0"},
            {"health", "Health", "B1", "10", "52% success · 1 session", "0"},
            {"shopping", "Shopping", "A2", "8", "Not tried yet", "0"},
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentTutorBinding.inflate(inflater, container, false);
        return withSkeleton(binding.getRoot(), SkeletonView.Variant.GRID);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // The custom card opens its own page with the "Create new" button and saved situations.
        binding.customCard.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_tutor_to_custom));
    }

    @Override
    public void onTabHidden() {
        if (binding == null) {
            return;
        }
        binding.topicGrid.removeAllViews();
        binding.interestChips.removeAllViews();
        showSkeleton();
    }

    @Override
    public void onTabShown() {
        if (binding == null) {
            return;
        }
        OnboardingAnswers answers = preferences.getOnboardingAnswers();
        if (answers != null) {
            bindInterestChips(answers);
        }
        // Recommended topics come first, as in the design.
        List<TopicCardAdapter.Card> cards = new ArrayList<>();
        for (String[] t : TOPICS) {
            boolean recommended = "1".equals(t[5]);
            cards.add(new TopicCardAdapter.Card(t[0], t[1], t[2] + " · " + t[3] + " min", t[4], recommended, false));
        }
        cards.sort((a, b) -> Boolean.compare(b.badge, a.badge));
        bindTopics(cards);
        hideSkeleton();
    }

    /** Two cards per row, added to the page so the whole Tutor screen scrolls as one. */
    private void bindTopics(List<TopicCardAdapter.Card> cards) {
        binding.topicGrid.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int margin = dp(5);
        for (int i = 0; i < cards.size(); i += 2) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = i; j < Math.min(i + 2, cards.size()); j++) {
                TopicCardAdapter.Card card = cards.get(j);
                ItemTopicCardBinding item = ItemTopicCardBinding.inflate(inflater, row, false);
                item.topicTitle.setText(card.title);
                item.topicMeta.setText(card.meta);
                item.topicStat.setText(card.stat);
                item.topicBadge.setVisibility(card.badge ? View.VISIBLE : View.GONE);
                item.topicAction.setVisibility(View.GONE);
                item.card.setOnClickListener(v -> openTopic(card.id));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                params.setMargins(margin, margin, margin, margin);
                row.addView(item.getRoot(), params);
            }
            if (cards.size() - i < 2) {
                // Keep a lone last card at half width.
                View filler = new View(requireContext());
                row.addView(filler, new LinearLayout.LayoutParams(0, 0, 1f));
            }
            binding.topicGrid.addView(row);
        }
    }

    private void openTopic(String topicId) {
        Bundle args = new Bundle();
        args.putString("topicId", topicId);
        NavHostFragment.findNavController(this).navigate(R.id.action_tutor_to_speaking, args);
    }

    /** Shows the learner's job and goal from onboarding as pill chips. */
    private void bindInterestChips(OnboardingAnswers answers) {
        binding.interestChips.removeAllViews();
        List<String> labels = new ArrayList<>();
        if (answers.job != null && !answers.job.isEmpty()) {
            labels.add(answers.job);
        }
        if (answers.goal != null && !answers.goal.isEmpty()) {
            labels.add(answers.goal);
        }
        int pad = dp(12);
        for (String label : labels) {
            TextView chip = new TextView(requireContext());
            chip.setText(label);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            chip.setTextColor(requireContext().getColor(R.color.ds_body));
            chip.setBackgroundResource(R.drawable.bg_ds_pill);
            chip.setPadding(pad, dp(6), pad, dp(6));
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(dp(8));
            binding.interestChips.addView(chip, params);
        }
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
