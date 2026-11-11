package com.masterenglish.app.ui.profile;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.masterenglish.app.R;
import com.masterenglish.app.data.local.entity.BadgeEntity;
import com.masterenglish.app.data.local.entity.DailyStudyLogEntity;
import com.masterenglish.app.data.local.entity.UserBadgeEntity;
import com.masterenglish.app.data.local.entity.UserEntity;
import com.masterenglish.app.data.local.entity.UserSettingsEntity;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.repository.SkillProgress;
import com.masterenglish.app.databinding.FragmentProfileBinding;
import com.masterenglish.app.ui.common.SkeletonView;
import com.masterenglish.app.ui.common.TabFragment;
import com.masterenglish.app.service.reminder.NotificationHelper;
import com.masterenglish.app.service.reminder.ReminderScheduler;
import com.masterenglish.app.service.sync.SyncScheduler;
import com.masterenglish.app.utils.Levels;
import com.masterenglish.app.utils.SoundManager;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Profile tab: stats, badges and the week's study chart. Settings live on their own page. */
@AndroidEntryPoint
public class ProfileFragment extends TabFragment {



    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private BadgeAdapter badgeAdapter;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return withSkeleton(binding.getRoot(), SkeletonView.Variant.PROFILE);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.skillListening.setOnClickListener(v -> openSkill(SkillProgress.LISTENING));
        binding.skillReading.setOnClickListener(v -> openSkill(SkillProgress.READING));
        binding.skillSpeaking.setOnClickListener(v -> openSkill(SkillProgress.SPEAKING));
        binding.skillWriting.setOnClickListener(v -> openSkill(SkillProgress.WRITING));
        binding.skillGrammar.setOnClickListener(v -> openSkill(SkillProgress.GRAMMAR));
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        viewModel.skills().observe(getViewLifecycleOwner(), this::bindSkillChips);

        badgeAdapter = new BadgeAdapter();
        binding.badgeList.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        binding.badgeList.setAdapter(badgeAdapter);
        binding.badgeList.setNestedScrollingEnabled(false);


        binding.guestNote.setVisibility(viewModel.isGuest() ? View.VISIBLE : View.GONE);
        binding.settingsRow.setOnClickListener(v -> NavHostFragment.findNavController(this)
                .navigate(R.id.action_profile_to_settings));

        viewModel.user().observe(getViewLifecycleOwner(), this::bindUser);
        viewModel.studyLog().observe(getViewLifecycleOwner(), this::bindChart);
        viewModel.badges().observe(getViewLifecycleOwner(), badges -> refreshBadges());
        viewModel.unlocked().observe(getViewLifecycleOwner(), unlocked -> refreshBadges());
        setupChart();
    }

    private void bindUser(UserEntity user) {
        if (user == null || binding == null) {
            return;
        }
        hideSkeleton();
        binding.nameText.setText(user.displayName);
        binding.emailText.setText(user.isGuest() ? getString(R.string.profile_guest_name) : user.email);
        binding.tierText.setText(Levels.tierName(user.experiencePoints));
        binding.xpText.setText(getString(R.string.profile_xp_format, user.experiencePoints,
                Levels.tierName(user.experiencePoints)));
        binding.xpBar.setProgress(Levels.progressPercent(user.experiencePoints));
        binding.streakText.setText(String.valueOf(user.streakCount));
        binding.gemText.setText(String.valueOf(user.gemCount));
    }


    @Override
    public void onTabHidden() {
        if (binding == null) {
            return;
        }
        badgeAdapter.submit(new ArrayList<>(), new HashSet<>());
        binding.weeklyChart.clear();
        binding.cefrScale.removeAllViews();
        binding.skillRows.removeAllViews();
        binding.grammarErrors.removeAllViews();
        binding.strongSkills.removeAllViews();
        binding.weakSkills.removeAllViews();
        showSkeleton();
    }

    @Override
    public void onTabShown() {
        if (binding == null) {
            return;
        }
        Map<String, SkillProgress.Summary> skills = viewModel.skills().getValue();
        if (skills != null) {
            bindSkillChips(skills);
        }
        bindChart(viewModel.studyLog().getValue());
        refreshBadges();
        UserEntity user = viewModel.user().getValue();
        if (user != null) {
            bindUser(user);
        }
    }

    private void refreshBadges() {
        if (binding == null) {
            return;
        }
        List<BadgeEntity> catalogue = viewModel.badges().getValue() == null
                ? new ArrayList<>() : new ArrayList<>(viewModel.badges().getValue());
        List<UserBadgeEntity> unlocked = viewModel.unlocked().getValue();
        Set<String> unlockedIds = new HashSet<>();
        if (unlocked != null) {
            for (UserBadgeEntity row : unlocked) {
                unlockedIds.add(row.badgeId);
            }
        }
        // Unlocked badges first (stable, so catalogue order is kept within each group).
        catalogue.sort(Comparator.comparing((BadgeEntity badge) -> !unlockedIds.contains(badge.id)));
        badgeAdapter.submit(catalogue, unlockedIds);
        int unlockedCount = 0;
        for (BadgeEntity badge : catalogue) {
            if (unlockedIds.contains(badge.id)) {
                unlockedCount++;
            }
        }
        binding.badgeCount.setText(getString(R.string.profile_badges_count, unlockedCount, catalogue.size()));
    }

    private void setupChart() {
        binding.weeklyChart.getDescription().setEnabled(false);
        binding.weeklyChart.getLegend().setEnabled(false);
        binding.weeklyChart.setDrawGridBackground(false);
        binding.weeklyChart.setScaleEnabled(false);
        binding.weeklyChart.getAxisRight().setEnabled(false);
        XAxis x = binding.weeklyChart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setGranularity(1f);
        binding.weeklyChart.getAxisLeft().setAxisMinimum(0f);
    }

    /** Minutes per day for the last seven days, oldest first, including days with no study. */
    private void bindChart(List<DailyStudyLogEntity> log) {
        if (binding == null) {
            return;
        }
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        DateTimeFormatter label = DateTimeFormatter.ofPattern("dd/MM", Locale.ROOT);
        for (int i = 0; i < 7; i++) {
            LocalDate day = today.minusDays(6 - i);
            int minutes = 0;
            if (log != null) {
                for (DailyStudyLogEntity row : log) {
                    if (row.dateKey.equals(day.toString())) {
                        minutes = row.minutes;
                    }
                }
            }
            entries.add(new BarEntry(i, minutes));
            labels.add(day.format(label));
        }
        BarDataSet dataSet = new BarDataSet(entries, getString(R.string.profile_study_days));
        dataSet.setColor(ContextCompat.getColor(requireContext(), R.color.brand_green));
        dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.neutral_text));
        dataSet.setValueTextSize(11f);
        BarData data = new BarData(dataSet);
        data.setBarWidth(0.55f);
        binding.weeklyChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        binding.weeklyChart.setData(data);
        binding.weeklyChart.invalidate();
    }


    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    /** Each skill chip shows its name and, once there are lessons for it, the CEFR level. */
    private void bindSkillChips(Map<String, SkillProgress.Summary> skills) {
        bindSkillChip(binding.skillListening, R.string.onboard_skill_listening, skills.get(SkillProgress.LISTENING));
        bindSkillChip(binding.skillReading, R.string.onboard_skill_reading, skills.get(SkillProgress.READING));
        bindSkillChip(binding.skillSpeaking, R.string.onboard_skill_speaking, skills.get(SkillProgress.SPEAKING));
        bindSkillChip(binding.skillWriting, R.string.onboard_skill_writing, skills.get(SkillProgress.WRITING));
        bindSkillChip(binding.skillGrammar, R.string.onboard_skill_grammar, skills.get(SkillProgress.GRAMMAR));
        bindProfileSections(skills);
    }

    private static final String[] CEFR_LEVELS = {"A1", "A2", "B1", "B2", "C1", "C2"};
    private static final String[] SKILL_IDS = {SkillProgress.LISTENING, SkillProgress.READING, SkillProgress.SPEAKING,
            SkillProgress.WRITING, SkillProgress.GRAMMAR};
    private static final int[] SKILL_NAME_RES = {R.string.onboard_skill_listening, R.string.onboard_skill_reading,
            R.string.onboard_skill_speaking, R.string.onboard_skill_writing, R.string.onboard_skill_grammar};

    /** Fills the CEFR scale, the skill rows, grammar errors and the strongest and weakest skills. */
    private void bindProfileSections(Map<String, SkillProgress.Summary> skills) {
        // Current level: the highest CEFR level any skill is working in.
        int current = 0;
        for (SkillProgress.Summary summary : skills.values()) {
            if (summary == null || summary.level == null) {
                continue;
            }
            for (int i = 0; i < CEFR_LEVELS.length; i++) {
                if (CEFR_LEVELS[i].equals(summary.level)) {
                    current = Math.max(current, i);
                }
            }
        }

        binding.cefrScale.removeAllViews();
        for (int i = 0; i < CEFR_LEVELS.length; i++) {
            TextView step = new TextView(requireContext());
            step.setText(CEFR_LEVELS[i]);
            step.setGravity(android.view.Gravity.CENTER);
            step.setTextSize(13);
            step.setTypeface(null, android.graphics.Typeface.BOLD);
            int bg = i < current ? 0xFFDDF0E6 : i == current ? 0xFFE0A030 : 0xFFFFFFFF;
            int ink = i < current ? 0xFF1F5C41 : i == current ? 0xFF1B1A17 : 0xFF9C978A;
            step.setBackgroundColor(bg);
            step.setTextColor(ink);
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(0,
                    dp(40), 1f);
            params.setMarginEnd(dp(4));
            binding.cefrScale.addView(step, params);
        }

        binding.skillRows.removeAllViews();
        for (int i = 0; i < SKILL_IDS.length; i++) {
            SkillProgress.Summary summary = skills.get(SKILL_IDS[i]);
            android.widget.LinearLayout row = new android.widget.LinearLayout(requireContext());
            row.setOrientation(android.widget.LinearLayout.VERTICAL);
            row.setPadding(0, dp(10), 0, dp(10));
            android.widget.LinearLayout head = new android.widget.LinearLayout(requireContext());
            head.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            TextView name = new TextView(requireContext());
            name.setText(getString(SKILL_NAME_RES[i]));
            name.setTextSize(15);
            name.setTextColor(0xFF1B1A17);
            name.setTypeface(null, android.graphics.Typeface.BOLD);
            android.widget.LinearLayout.LayoutParams nameParams = new android.widget.LinearLayout.LayoutParams(0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            head.addView(name, nameParams);
            TextView level = new TextView(requireContext());
            level.setText(summary == null || summary.level == null ? "—" : summary.level);
            level.setTextSize(13);
            level.setTypeface(null, android.graphics.Typeface.BOLD);
            level.setTextColor(0xFF1B1A17);
            level.setBackgroundResource(R.drawable.bg_ds_pill);
            level.setPadding(dp(10), dp(3), dp(10), dp(3));
            head.addView(level);
            row.addView(head);
            android.widget.ProgressBar bar = new android.widget.ProgressBar(requireContext(), null,
                    android.R.attr.progressBarStyleHorizontal);
            bar.setMax(100);
            bar.setProgress(summary == null ? 0 : summary.percent());
            bar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF2E7D5B));
            android.widget.LinearLayout.LayoutParams barParams = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, dp(6));
            barParams.topMargin = dp(8);
            row.addView(bar, barParams);
            String skillId = SKILL_IDS[i];
            row.setOnClickListener(v -> openSkill(skillId));
            binding.skillRows.addView(row);
        }

        // Grammar errors: each recurring tag with a bar scaled to the most frequent one.
        binding.grammarErrors.removeAllViews();
        SkillProgress.Summary grammar = skills.get(SkillProgress.GRAMMAR);
        if (grammar == null || grammar.recent.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("—");
            empty.setTextColor(0xFF6B6860);
            binding.grammarErrors.addView(empty);
        } else {
            int max = 1;
            for (SkillProgress.Mistake m : grammar.recent) {
                max = Math.max(max, m.count);
            }
            for (SkillProgress.Mistake m : grammar.recent) {
                android.widget.LinearLayout row = new android.widget.LinearLayout(requireContext());
                row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.setPadding(0, dp(4), 0, dp(4));
                TextView tag = new TextView(requireContext());
                tag.setText(m.tag);
                tag.setTextSize(13);
                tag.setTextColor(0xFF1B1A17);
                row.addView(tag, new android.widget.LinearLayout.LayoutParams(dp(130),
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT));
                android.widget.ProgressBar bar = new android.widget.ProgressBar(requireContext(), null,
                        android.R.attr.progressBarStyleHorizontal);
                bar.setMax(max);
                bar.setProgress(m.count);
                bar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFFC2412D));
                row.addView(bar, new android.widget.LinearLayout.LayoutParams(0, dp(8), 1f));
                TextView count = new TextView(requireContext());
                count.setText(String.valueOf(m.count));
                count.setTextSize(13);
                count.setTypeface(null, android.graphics.Typeface.BOLD);
                count.setTextColor(0xFF1B1A17);
                count.setPadding(dp(8), 0, 0, 0);
                row.addView(count);
                binding.grammarErrors.addView(row);
            }
        }

        // Strongest and weakest skills, ordered by completion.
        List<SkillProgress.Summary> ranked = new java.util.ArrayList<>();
        for (String id : SKILL_IDS) {
            SkillProgress.Summary summary = skills.get(id);
            if (summary != null && summary.total > 0) {
                ranked.add(summary);
            }
        }
        ranked.sort((a, b) -> Integer.compare(b.percent(), a.percent()));
        fillRanking(binding.strongSkills, R.string.profile_strong_title, ranked, true);
        fillRanking(binding.weakSkills, R.string.profile_weak_title, ranked, false);
    }

    private void fillRanking(android.widget.LinearLayout container, int titleRes,
                             List<SkillProgress.Summary> ranked, boolean strongest) {
        container.removeAllViews();
        TextView title = new TextView(requireContext());
        title.setText(titleRes);
        title.setTextSize(13);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(strongest ? 0xFF1F5C41 : 0xFFC2412D);
        container.addView(title);
        if (ranked.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("—");
            empty.setTextColor(0xFF6B6860);
            container.addView(empty);
            return;
        }
        int count = Math.min(3, ranked.size());
        for (int i = 0; i < count; i++) {
            SkillProgress.Summary summary = strongest ? ranked.get(i) : ranked.get(ranked.size() - 1 - i);
            TextView line = new TextView(requireContext());
            line.setText(skillName(summary.skillId) + "  " + summary.percent() + "%");
            line.setTextSize(13);
            line.setTextColor(0xFF1B1A17);
            line.setPadding(0, dp(6), 0, 0);
            container.addView(line);
        }
    }

    private String skillName(String skillId) {
        for (int i = 0; i < SKILL_IDS.length; i++) {
            if (SKILL_IDS[i].equals(skillId)) {
                return getString(SKILL_NAME_RES[i]);
            }
        }
        return skillId;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void bindSkillChip(MaterialButton chip, int nameRes, @Nullable SkillProgress.Summary summary) {
        String name = getString(nameRes);
        chip.setText(summary == null || summary.level == null
                ? name
                : getString(R.string.skill_chip, name, summary.level));
    }

    private void openSkill(String skillId) {
        android.os.Bundle args = new android.os.Bundle();
        args.putString("skillId", skillId);
        androidx.navigation.fragment.NavHostFragment.findNavController(this)
                .navigate(R.id.action_profile_to_skill, args);
    }
}
