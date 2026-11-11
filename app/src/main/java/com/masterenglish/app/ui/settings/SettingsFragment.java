package com.masterenglish.app.ui.settings;

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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.masterenglish.app.R;
import com.masterenglish.app.data.local.entity.UserSettingsEntity;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.databinding.FragmentSettingsBinding;
import com.masterenglish.app.service.reminder.NotificationHelper;
import com.masterenglish.app.service.reminder.ReminderScheduler;
import com.masterenglish.app.service.sync.SyncScheduler;
import com.masterenglish.app.utils.SoundManager;

import java.util.Arrays;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Settings page opened from Profile: reminders, learning goals, AI Agent connection, sync and sign-out. */
@AndroidEntryPoint
public class SettingsFragment extends Fragment {

    private static final String[] DIFFICULTIES = {"EASY", "MEDIUM", "HARD", "ADAPTIVE"};
    private static final int[] TARGET_MINUTES = {5, 10, 15};

    @Inject
    AppPreferences preferences;

    @Inject
    SoundManager soundManager;

    private FragmentSettingsBinding binding;
    private SettingsViewModel viewModel;
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
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        binding.backButton.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

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

        binding.syncButton.setOnClickListener(v -> {
            SyncScheduler.syncNow(requireContext());
            Toast.makeText(requireContext(), R.string.profile_sync_started, Toast.LENGTH_SHORT).show();
        });
        binding.logoutButton.setOnClickListener(v -> {
            viewModel.signOut();
            Toast.makeText(requireContext(), R.string.profile_sign_out_done, Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(this).navigate(R.id.action_settings_to_auth);
        });

        viewModel.settings().observe(getViewLifecycleOwner(), this::bindSettings);
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
