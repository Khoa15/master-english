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
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.masterenglish.app.R;
import com.masterenglish.app.data.local.entity.BadgeEntity;
import com.masterenglish.app.data.local.entity.DailyStudyLogEntity;
import com.masterenglish.app.data.local.entity.UserBadgeEntity;
import com.masterenglish.app.data.local.entity.UserEntity;
import com.masterenglish.app.data.local.entity.UserSettingsEntity;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.databinding.FragmentProfileBinding;
import com.masterenglish.app.service.reminder.NotificationHelper;
import com.masterenglish.app.service.reminder.ReminderScheduler;
import com.masterenglish.app.service.sync.SyncScheduler;
import com.masterenglish.app.utils.Levels;
import com.masterenglish.app.utils.SoundManager;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Profile tab: stats, badges, the week's study chart, and settings. */
@AndroidEntryPoint
public class ProfileFragment extends Fragment {

    private static final String[] DIFFICULTIES = {"EASY", "MEDIUM", "HARD", "ADAPTIVE"};
    private static final int[] TARGET_MINUTES = {5, 10, 15};

    @Inject
    AppPreferences preferences;

    @Inject
    SoundManager soundManager;

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private BadgeAdapter badgeAdapter;
    private UserSettingsEntity currentSettings;
    private boolean bindingSettings;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    turnReminderOn();
                } else {
                    binding.notifSwitch.setChecked(false);
                    Toast.makeText(requireContext(), R.string.profile_notification_denied, Toast.LENGTH_LONG).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

        badgeAdapter = new BadgeAdapter();
        binding.badgeList.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        binding.badgeList.setAdapter(badgeAdapter);
        binding.badgeList.setNestedScrollingEnabled(false);

        binding.difficultySpinner.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, Arrays.asList(DIFFICULTIES)));
        binding.difficultySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!bindingSettings && currentSettings != null) {
                    currentSettings.aiChallengeDifficulty = DIFFICULTIES[position];
                    save();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        binding.soundSwitch.setOnCheckedChangeListener(this::onSoundToggled);
        binding.notifSwitch.setOnCheckedChangeListener(this::onNotificationsToggled);
        binding.reminderTimeButton.setOnClickListener(v -> pickReminderTime());
        binding.minutesGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || bindingSettings || currentSettings == null) {
                return;
            }
            currentSettings.targetLearningMinutes = minutesFor(checkedId);
            save();
        });
        binding.saveKeyButton.setOnClickListener(v -> {
            viewModel.saveGeminiKey(binding.geminiKeyInput.getText() == null ? ""
                    : binding.geminiKeyInput.getText().toString());
            Toast.makeText(requireContext(), R.string.profile_saved_toast, Toast.LENGTH_SHORT).show();
        });
        binding.syncButton.setOnClickListener(v -> {
            SyncScheduler.syncNow(requireContext());
            Toast.makeText(requireContext(), R.string.profile_sync_started, Toast.LENGTH_SHORT).show();
        });
        binding.logoutButton.setOnClickListener(v -> {
            viewModel.signOut();
            Toast.makeText(requireContext(), R.string.profile_sign_out_done, Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).navigate(R.id.action_profile_to_auth);
        });

        binding.geminiKeyInput.setText(viewModel.geminiKey());
        binding.guestNote.setVisibility(viewModel.isGuest() ? View.VISIBLE : View.GONE);

        viewModel.user().observe(getViewLifecycleOwner(), this::bindUser);
        viewModel.settings().observe(getViewLifecycleOwner(), this::bindSettings);
        viewModel.studyLog().observe(getViewLifecycleOwner(), this::bindChart);
        viewModel.badges().observe(getViewLifecycleOwner(), badges -> refreshBadges());
        viewModel.unlocked().observe(getViewLifecycleOwner(), unlocked -> refreshBadges());
        setupChart();
    }

    private void bindUser(UserEntity user) {
        if (user == null || binding == null) {
            return;
        }
        binding.nameText.setText(user.displayName);
        binding.emailText.setText(user.isGuest() ? getString(R.string.profile_guest_name) : user.email);
        binding.tierText.setText(Levels.tierName(user.experiencePoints));
        binding.xpText.setText(getString(R.string.profile_xp_format, user.experiencePoints,
                Levels.tierName(user.experiencePoints)));
        binding.xpBar.setProgress(Levels.progressPercent(user.experiencePoints));
        binding.streakText.setText(String.valueOf(user.streakCount));
        binding.gemText.setText(String.valueOf(user.gemCount));
    }

    private void bindSettings(UserSettingsEntity settings) {
        if (settings == null || binding == null) {
            return;
        }
        currentSettings = settings;
        bindingSettings = true;
        binding.soundSwitch.setChecked(settings.soundFxEnabled);
        binding.notifSwitch.setChecked(settings.notificationsEnabled);
        binding.difficultySpinner.setSelection(Arrays.asList(DIFFICULTIES).indexOf(settings.aiChallengeDifficulty));
        binding.minutesGroup.check(checkedIdFor(settings.targetLearningMinutes));
        binding.reminderTimeButton.setText(getString(R.string.profile_reminder_time,
                settings.dailyReminderTime.substring(0, 5)));
        bindingSettings = false;
    }

    private void refreshBadges() {
        if (binding == null) {
            return;
        }
        List<BadgeEntity> catalogue = viewModel.badges().getValue();
        List<UserBadgeEntity> unlocked = viewModel.unlocked().getValue();
        Set<String> unlockedIds = new HashSet<>();
        if (unlocked != null) {
            for (UserBadgeEntity row : unlocked) {
                unlockedIds.add(row.badgeId);
            }
        }
        badgeAdapter.submit(catalogue == null ? new ArrayList<>() : catalogue, unlockedIds);
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

    private void onSoundToggled(CompoundButton button, boolean checked) {
        soundManager.setEnabled(checked);
        if (!bindingSettings && currentSettings != null) {
            currentSettings.soundFxEnabled = checked;
            save();
        }
    }

    private void onNotificationsToggled(CompoundButton button, boolean checked) {
        if (bindingSettings || currentSettings == null) {
            return;
        }
        currentSettings.notificationsEnabled = checked;
        save();
        if (!checked) {
            preferences.saveReminder(false, preferences.getReminderHour(), preferences.getReminderMinute());
            ReminderScheduler.cancel(requireContext());
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !NotificationHelper.hasNotificationPermission(requireContext())) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        turnReminderOn();
    }

    private void turnReminderOn() {
        preferences.saveReminder(true, preferences.getReminderHour(), preferences.getReminderMinute());
        ReminderScheduler.schedule(requireContext(), preferences.getReminderHour(), preferences.getReminderMinute());
    }

    private void pickReminderTime() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(preferences.getReminderHour())
                .setMinute(preferences.getReminderMinute())
                .setTitleText(R.string.profile_reminder_time_title)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            int hour = picker.getHour();
            int minute = picker.getMinute();
            if (currentSettings != null) {
                currentSettings.dailyReminderTime = String.format(Locale.ROOT, "%02d:%02d:00", hour, minute);
                save();
            }
            preferences.saveReminder(preferences.isReminderEnabled(), hour, minute);
            if (preferences.isReminderEnabled()) {
                ReminderScheduler.schedule(requireContext(), hour, minute);
            }
            binding.reminderTimeButton.setText(getString(R.string.profile_reminder_time,
                    String.format(Locale.ROOT, "%02d:%02d", hour, minute)));
        });
        picker.show(getChildFragmentManager(), "reminder_time");
    }

    private void save() {
        if (currentSettings != null) {
            viewModel.saveSettings(currentSettings);
        }
    }

    private static int minutesFor(int checkedId) {
        if (checkedId == R.id.profileMin5) {
            return TARGET_MINUTES[0];
        }
        return checkedId == R.id.profileMin15 ? TARGET_MINUTES[2] : TARGET_MINUTES[1];
    }

    private static int checkedIdFor(int minutes) {
        if (minutes <= TARGET_MINUTES[0]) {
            return R.id.profileMin5;
        }
        return minutes >= TARGET_MINUTES[2] ? R.id.profileMin15 : R.id.profileMin10;
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
