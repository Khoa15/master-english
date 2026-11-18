package com.doradodo.app;

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

import com.doradodo.app.data.local.prefs.AppPreferences;
import com.doradodo.app.data.repository.UserRepository;
import com.doradodo.app.databinding.ActivityMainBinding;
import com.doradodo.app.service.reminder.ReminderScheduler;
import com.doradodo.app.ui.common.TabFragment;
import com.doradodo.app.utils.SoundManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Single Activity host: a NavHostFragment with a bottom bar for the two main tabs (Vocabulary, Profile). */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    @Inject
    AppPreferences preferences;

    @Inject
    UserRepository userRepository;

    @Inject
    SoundManager soundManager;

    @Inject
    com.doradodo.app.data.repository.LanguageRepository languages;

    @Inject
    com.doradodo.app.utils.AppExecutors executors;

    private ActivityMainBinding binding;

    private final ActivityResultLauncher<String> notificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Status bar matches the paper background of every screen, with dark icons.
        getWindow().setStatusBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.neutral_bg_ds));
        new androidx.core.view.WindowInsetsControllerCompat(getWindow(), binding.getRoot())
                .setAppearanceLightStatusBars(true);

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host);
        if (host == null) {
            return;
        }
        NavController navController = host.getNavController();
        NavGraph graph = navController.getNavInflater().inflate(R.navigation.nav_graph);
        boolean signedIn = preferences.isOnboardingDone() && preferences.getCurrentUserId() != null;
        @IdRes int startDestination = signedIn ? R.id.vocabularyFragment : R.id.onboardingFragment;
        graph.setStartDestination(startDestination);
        navController.setGraph(graph);

        // Keeps the selected item in sync with the destination; the click listener below takes over clicks.
        NavigationUI.setupWithNavController(binding.bottomNav, navController);
        // Tab switches are instant: NavigationUI's default listener adds ~200 ms fade/scale animations and
        // throws each tab's state away. Here there is no animation and each tab's state is saved and restored.
        final androidx.navigation.NavOptions tabOptions = new androidx.navigation.NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(graph.getStartDestinationId(), false, true)
                .build();
        binding.bottomNav.setOnItemSelectedListener(item -> {
            try {
                navController.navigate(item.getItemId(), null, tabOptions);
            } catch (IllegalArgumentException e) {
                return false;
            }
            return true;
        });
        // Frosted glass pill: blurs whatever scrolls behind the floating navigation.
        com.doradodo.app.ui.common.GlassNav.install(findViewById(R.id.content_root), binding.bottomNav);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            showTabFor(destination.getId());
            boolean mainTab = isMainTab(destination.getId());
            binding.bottomNav.setVisibility(mainTab ? View.VISIBLE : View.GONE);
        });

        if (signedIn) {
            // The app follows the learner's display language (kept in the database).
            executors.diskIO().execute(() -> {
                String lang = languages.applyOnboardingChoice();
                runOnUiThread(() -> com.doradodo.app.ui.common.LocaleSync.apply(lang));
            });
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

    // Vocabulary is a regular destination; Profile is the only fragment kept in the tab container.
    private static final int[] TAB_IDS = {R.id.profileFragment};
    private static final String[] TAB_TAGS = {"tab_profile"};

    /** Index of the tab fragment currently shown, or -1 while a non-tab screen is open. */
    private int visibleTab = -1;

    private static TabFragment createTab(int index) {
        return new com.doradodo.app.ui.profile.ProfileFragment();
    }

    /**
     * The four tab fragments are created once and only shown or hidden afterwards. A hidden tab is capped at
     * CREATED (no observers, no work) and releases its content; the shown one is RESUMED and rebuilds from cache.
     */
    private void showTabFor(int destinationId) {
        androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
        if (fm.isStateSaved()) {
            return;
        }
        int target = -1;
        for (int i = 0; i < TAB_IDS.length; i++) {
            if (TAB_IDS[i] == destinationId) {
                target = i;
            }
        }
        // visibleTab is lost when the activity is recreated (language change, rotation) while the FragmentManager
        // restores the tab fragments as they were. Trust the fragments, not the field: skip only when both agree.
        boolean inSync = target == visibleTab;
        for (int i = 0; i < TAB_IDS.length && inSync; i++) {
            androidx.fragment.app.Fragment f = fm.findFragmentByTag(TAB_TAGS[i]);
            if (f != null && f.isHidden() == (i == target)) {
                inSync = false;
            }
        }
        if (inSync) {
            return;
        }
        final int previous = visibleTab;
        final int shown = target;
        visibleTab = target;
        androidx.fragment.app.FragmentTransaction ft = fm.beginTransaction().setReorderingAllowed(true);
        for (int i = 0; i < TAB_IDS.length; i++) {
            androidx.fragment.app.Fragment f = fm.findFragmentByTag(TAB_TAGS[i]);
            if (f == null) {
                if (shown < 0) {
                    continue;
                }
                f = createTab(i);
                ft.add(R.id.tab_container, f, TAB_TAGS[i]);
            }
            if (i == shown) {
                ft.show(f).setMaxLifecycle(f, androidx.lifecycle.Lifecycle.State.RESUMED);
            } else {
                ft.hide(f).setMaxLifecycle(f, androidx.lifecycle.Lifecycle.State.CREATED);
            }
        }
        ft.runOnCommit(() -> {
            if (previous >= 0 && previous != shown) {
                androidx.fragment.app.Fragment f = fm.findFragmentByTag(TAB_TAGS[previous]);
                if (f instanceof TabFragment) {
                    ((TabFragment) f).onTabHidden();
                }
            }
            if (shown >= 0) {
                androidx.fragment.app.Fragment f = fm.findFragmentByTag(TAB_TAGS[shown]);
                if (f instanceof TabFragment) {
                    ((TabFragment) f).onTabShown();
                }
            }
        });
        ft.commit();
    }

    private static boolean isMainTab(int destinationId) {
        return destinationId == R.id.vocabularyFragment
                || destinationId == R.id.allPacksFragment
                || destinationId == R.id.entertainmentFragment
                || destinationId == R.id.profileFragment;
    }
}
