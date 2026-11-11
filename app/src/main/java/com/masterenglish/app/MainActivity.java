package com.masterenglish.app;

import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.IdRes;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.repository.UserRepository;
import com.masterenglish.app.databinding.ActivityMainBinding;
import com.masterenglish.app.service.reminder.ReminderScheduler;
import com.masterenglish.app.utils.SoundManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Single Activity host: a NavHostFragment with a bottom bar for the three main tabs. */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Inject
    AppPreferences preferences;

    @Inject
    UserRepository userRepository;

    @Inject
    SoundManager soundManager;

    private ActivityMainBinding binding;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);
        if (host == null) {
            return;
        }
        NavController navController = host.getNavController();
        NavGraph graph = navController.getNavInflater().inflate(R.navigation.nav_graph);
        boolean signedIn = preferences.isOnboardingDone() && preferences.getCurrentUserId() != null;
        @IdRes int startDestination = signedIn ? R.id.roadmapFragment : R.id.onboardingFragment;
        graph.setStartDestination(startDestination);
        navController.setGraph(graph);

        NavigationUI.setupWithNavController(binding.bottomNav, navController);
        navController.addOnDestinationChangedListener((controller, destination, arguments) ->
                binding.bottomNav.setVisibility(isMainTab(destination.getId()) ? View.VISIBLE : View.GONE));

        if (signedIn) {
            observeSoundSetting();
            ReminderScheduler.schedule(this, preferences.getReminderHour(), preferences.getReminderMinute());
        }
    }

    /** Keeps the SoundPool switch in step with the learner's "Âm thanh" setting. */
    private void observeSoundSetting() {
        String userId = preferences.getCurrentUserId();
        if (userId == null) {
            return;
        }
        userRepository.observeSettings(userId).observe(this, settings -> {
            if (settings != null) {
                soundManager.setEnabled(settings.soundFxEnabled);
            }
        });
    }

    public void requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private static boolean isMainTab(int destinationId) {
        return destinationId == R.id.roadmapFragment
                || destinationId == R.id.reviewFragment
                || destinationId == R.id.profileFragment;
    }
}
