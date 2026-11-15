package com.masterenglish.app.ui.profile;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.masterenglish.app.R;
import com.masterenglish.app.data.local.entity.DailyStudyLogEntity;
import com.masterenglish.app.data.local.entity.UserEntity;
import com.masterenglish.app.service.reminder.ReminderScheduler;
import com.masterenglish.app.ui.common.TabFragment;
import com.masterenglish.app.ui.ds.Ui;
import com.masterenglish.app.ui.vocab.VocabViewModel;
import com.masterenglish.app.utils.CefrLevel;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;

/** Profile tab: identity, stats, streak calendar, personal info, links and preferences. */
@AndroidEntryPoint
public class ProfileFragment extends TabFragment {

    private Ui.Screen screen;
    private ProfileViewModel viewModel;
    private VocabViewModel vocabViewModel;
    private LinearLayout identity;
    private LinearLayout stats;
    private LinearLayout calendar;
    private LinearLayout personal;
    private LinearLayout preferences;
    private UserEntity user;
    private int learned;
    private Set<Integer> activeDays = new HashSet<>();
    private YearMonth month = YearMonth.now();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        screen = new Ui.Screen(c, 110);
        LinearLayout header = Ui.row(c);
        header.addView(Ui.text(c, "Profile", 26, 700, Ui.INK), Ui.weight(1f));
        ImageView gear = Ui.icon(c, R.drawable.ic_ds_gear, 22, Ui.INK);
        gear.setPadding(Ui.dp(c, 9), Ui.dp(c, 9), Ui.dp(c, 9), Ui.dp(c, 9));
        gear.setBackground(Ui.shape(c, Ui.CARD, 12, Ui.LINE, 1));
        gear.setContentDescription("Settings");
        gear.setOnClickListener(v -> navigate(R.id.action_profile_to_settings));
        header.addView(gear, new LinearLayout.LayoutParams(Ui.dp(c, 40), Ui.dp(c, 40)));
        screen.top.addView(header, Ui.matchWidth());
        screen.top.setPadding(Ui.dp(c, 20), Ui.dp(c, 24), Ui.dp(c, 20), Ui.dp(c, 4));

        LinearLayout body = screen.body;
        identity = Ui.card(c, 16, 20);
        body.addView(identity, Ui.matchWidth());
        stats = Ui.row(c);
        stats.setBaselineAligned(false);
        Ui.add(body, stats, 12);
        calendar = Ui.card(c, 16, 18);
        Ui.add(body, calendar, 12);
        Ui.add(body, section(c, "Personal info"), 22);
        personal = Ui.card(c, 4, 18);
        Ui.add(body, personal, 8);
        Ui.add(body, section(c, "Insights"), 22);
        Ui.add(body, link(c, "Analytics", "Stats & progress", R.id.action_profile_to_analytics), 8);
        Ui.add(body, link(c, "Subscription", "Free plan", R.id.action_profile_to_subscription), 8);
        Ui.add(body, link(c, "Feedback", "Send us a note", R.id.action_profile_to_feedback), 8);
        Ui.add(body, section(c, "Preferences"), 22);
        preferences = Ui.card(c, 4, 18);
        Ui.add(body, preferences, 8);
        TextView signOut = Ui.text(c, "Sign out", 15, 700, Ui.RED);
        signOut.setGravity(Gravity.CENTER);
        signOut.setBackground(Ui.shape(c, Ui.CARD, 16, Ui.LINE, 1));
        signOut.setMinHeight(Ui.dp(c, 52));
        signOut.setClickable(true);
        signOut.setOnClickListener(v -> {
            viewModel.signOut();
            navigate(R.id.action_profile_to_auth);
        });
        Ui.add(body, signOut, 22);
        return screen.root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        vocabViewModel = new ViewModelProvider(this).get(VocabViewModel.class);
        viewModel.user().observe(getViewLifecycleOwner(), u -> {
            user = u;
            bindIdentity();
            bindStats();
        });
        viewModel.month().observe(getViewLifecycleOwner(), m -> month = m);
        viewModel.monthLog().observe(getViewLifecycleOwner(), log -> {
            activeDays = new HashSet<>();
            if (log != null) {
                for (DailyStudyLogEntity e : log) {
                    if (e.minutes > 0) {
                        activeDays.add(LocalDate.parse(e.dateKey).getDayOfMonth());
                    }
                }
            }
            bindCalendar();
        });
        vocabViewModel.library().observe(getViewLifecycleOwner(), l -> {
            learned = l.learnedCount;
            bindStats();
        });
        bindPersonal();
        bindPreferences();
    }

    @Override
    public void onTabShown() {
        if (viewModel != null) {
            vocabViewModel.load();
            bindPersonal();
            bindPreferences();
        }
    }

    @Override
    public void onTabHidden() {
    }

    private void navigate(int action) {
        try {
            NavHostFragment.findNavController(requireActivity().getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host)).navigate(action);
        } catch (IllegalArgumentException | IllegalStateException ignored) {
            // Still on a tab placeholder or double tap.
        }
    }

    // ----- Sections ---------------------------------------------------------

    private TextView section(Context c, String title) {
        return Ui.text(c, title, 18, 700, Ui.INK);
    }

    private void bindIdentity() {
        if (user == null) {
            return;
        }
        Context c = requireContext();
        identity.removeAllViews();
        LinearLayout row = Ui.row(c);
        String name = user.displayName == null || user.displayName.isEmpty() ? "Learner" : user.displayName;
        TextView avatar = Ui.text(c, name.substring(0, 1).toUpperCase(Locale.getDefault()), 24, 700, Ui.GREEN_INK);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(Ui.shape(c, Ui.GREEN_TINT, 999));
        row.addView(avatar, new LinearLayout.LayoutParams(Ui.dp(c, 56), Ui.dp(c, 56)));
        LinearLayout names = Ui.column(c);
        names.addView(Ui.text(c, name, 18, 700, Ui.INK));
        LocalDate joined = java.time.Instant.ofEpochMilli(user.createdAt).atZone(ZoneId.systemDefault()).toLocalDate();
        Ui.add(names, Ui.text(c, "Joined " + joined.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                + " " + joined.getYear(), 12, 400, Ui.MUTED), 3);
        TextView level = Ui.pill(c, "Level " + CefrLevel.of(user.experiencePoints), Ui.AMBER_TINT, Ui.AMBER_INK, 12);
        LinearLayout.LayoutParams lp = Ui.wrap();
        lp.topMargin = Ui.dp(c, 8);
        names.addView(level, lp);
        LinearLayout.LayoutParams np = Ui.weight(1f);
        np.leftMargin = Ui.dp(c, 14);
        row.addView(names, np);
        identity.addView(row, Ui.matchWidth());
    }

    private void bindStats() {
        if (user == null) {
            return;
        }
        Context c = requireContext();
        stats.removeAllViews();
        statCard(c, String.format(Locale.US, "%,d", learned), "Words learned", Ui.GREEN_INK, 0);
        statCard(c, String.valueOf(user.streakCount), "Day streak", Ui.AMBER_INK, 10);
        statCard(c, String.format(Locale.US, "%,d", user.experiencePoints), "XP", Ui.BLUE_INK, 10);
    }

    private void statCard(Context c, String value, String label, int color, float left) {
        LinearLayout box = Ui.column(c);
        box.setBackground(Ui.shape(c, Ui.CARD, 16, Ui.LINE, 1));
        box.setPadding(Ui.dp(c, 12), Ui.dp(c, 14), Ui.dp(c, 12), Ui.dp(c, 14));
        box.addView(Ui.text(c, value, 20, 700, color));
        Ui.add(box, Ui.text(c, label, 11, 600, Ui.MUTED), 4);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        p.leftMargin = Ui.dp(c, left);
        stats.addView(box, p);
    }

    private void bindCalendar() {
        Context c = requireContext();
        calendar.removeAllViews();
        LinearLayout head = Ui.row(c);
        head.addView(navArrow(c, R.drawable.ic_ds_back, "Previous month", -1));
        TextView title = Ui.text(c, month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear(),
                15, 700, Ui.INK);
        title.setGravity(Gravity.CENTER);
        head.addView(title, Ui.weight(1f));
        head.addView(navArrow(c, R.drawable.ic_ds_chevron, "Next month", 1));
        calendar.addView(head, Ui.matchWidth());

        LinearLayout weekdays = Ui.row(c);
        for (String d : new String[]{"S", "M", "T", "W", "T", "F", "S"}) {
            TextView t = Ui.text(c, d, 11, 600, Ui.MUTED);
            t.setGravity(Gravity.CENTER);
            weekdays.addView(t, new LinearLayout.LayoutParams(0, Ui.dp(c, 24), 1f));
        }
        Ui.add(calendar, weekdays, 8);

        int offset = month.atDay(1).getDayOfWeek().getValue() % 7;
        int days = month.lengthOfMonth();
        LocalDate today = LocalDate.now();
        int cells = ((offset + days + 6) / 7) * 7;
        LinearLayout line = null;
        for (int i = 0; i < cells; i++) {
            if (i % 7 == 0) {
                line = Ui.row(c);
                Ui.add(calendar, line, 2);
            }
            int day = i - offset + 1;
            TextView cell = Ui.text(c, day >= 1 && day <= days ? String.valueOf(day) : "", 13, 600, Ui.INK);
            cell.setGravity(Gravity.CENTER);
            if (day >= 1 && day <= days) {
                if (activeDays.contains(day)) {
                    cell.setBackground(Ui.shape(c, Ui.GREEN, 999));
                    cell.setTextColor(0xFFFFFFFF);
                } else if (month.atDay(day).equals(today)) {
                    cell.setBackground(Ui.shape(c, 0, 999, Ui.GREEN, 1.5f));
                }
            }
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, Ui.dp(c, 36), 1f);
            line.addView(cell, p);
        }
        LinearLayout legend = Ui.row(c);
        View dot = new View(c);
        dot.setBackground(Ui.shape(c, Ui.GREEN, 999));
        legend.addView(dot, new LinearLayout.LayoutParams(Ui.dp(c, 10), Ui.dp(c, 10)));
        LinearLayout.LayoutParams lp = Ui.wrap();
        lp.leftMargin = Ui.dp(c, 6);
        legend.addView(Ui.text(c, "Streak", 12, 600, Ui.MUTED), lp);
        Ui.add(calendar, legend, 10);
    }

    private ImageView navArrow(Context c, int res, String description, int delta) {
        ImageView v = Ui.icon(c, res, 20, Ui.INK);
        v.setPadding(Ui.dp(c, 12), Ui.dp(c, 12), Ui.dp(c, 12), Ui.dp(c, 12));
        v.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(c, 44), Ui.dp(c, 44)));
        v.setContentDescription(description);
        v.setOnClickListener(x -> viewModel.shiftMonth(delta));
        return v;
    }

    private void bindPersonal() {
        Context c = requireContext();
        personal.removeAllViews();
        addPicker(personal, "Display language", ProfileViewModel.LANGUAGE, "English", new String[]{"English", "Tiếng Việt"});
        addPicker(personal, "Country", ProfileViewModel.COUNTRY, "Vietnam",
                new String[]{"Vietnam", "United States", "United Kingdom", "Australia", "Canada", "Singapore", "Other"});
        addPicker(personal, "Gender", ProfileViewModel.GENDER, "Prefer not to say",
                new String[]{"Female", "Male", "Non-binary", "Prefer not to say"});
        View birthday = infoRow(c, "Date of birth", viewModel.info(ProfileViewModel.BIRTHDAY, "Not set"), false);
        birthday.setOnClickListener(v -> {
            LocalDate d = LocalDate.of(2000, 1, 1);
            new DatePickerDialog(c, (picker, y, m, day) ->
                    {
                        viewModel.setInfo(ProfileViewModel.BIRTHDAY, LocalDate.of(y, m + 1, day)
                                .format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)));
                        bindPersonal();
                    }, d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth()).show();
        });
        personal.addView(birthday, Ui.matchWidth());
    }

    private void addPicker(LinearLayout parent, String label, String key, String fallback, String[] options) {
        View row = infoRow(requireContext(), label, viewModel.info(key, fallback), true);
        row.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(requireContext(), v);
            for (int i = 0; i < options.length; i++) {
                menu.getMenu().add(0, i, i, options[i]);
            }
            menu.setOnMenuItemClickListener(item -> {
                viewModel.setInfo(key, options[item.getItemId()]);
                bindPersonal();
                return true;
            });
            menu.show();
        });
        parent.addView(row, Ui.matchWidth());
    }

    private void bindPreferences() {
        Context c = requireContext();
        preferences.removeAllViews();
        View goal = infoRow(c, "Daily goal", viewModel.dailyGoal() + " words", true);
        goal.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(c, v);
            int[] goals = {10, 20, 30, 50};
            for (int i = 0; i < goals.length; i++) {
                menu.getMenu().add(0, goals[i], i, goals[i] + " words");
            }
            menu.setOnMenuItemClickListener(item -> {
                viewModel.setDailyGoal(item.getItemId());
                bindPreferences();
                return true;
            });
            menu.show();
        });
        preferences.addView(goal, Ui.matchWidth());
        int hour = viewModel.preferences().getReminderHour();
        int minute = viewModel.preferences().getReminderMinute();
        View reminder = infoRow(c, "Reminders", viewModel.preferences().isReminderEnabled()
                ? formatTime(hour, minute) : "Off", true);
        reminder.setOnClickListener(v -> new TimePickerDialog(c, (picker, h, m) -> {
            viewModel.preferences().saveReminder(true, h, m);
            ReminderScheduler.schedule(c, h, m);
            bindPreferences();
        }, hour, minute, false).show());
        preferences.addView(reminder, Ui.matchWidth());
    }

    private static String formatTime(int hour, int minute) {
        int h12 = hour % 12 == 0 ? 12 : hour % 12;
        return String.format(Locale.US, "%d:%02d %s", h12, minute, hour < 12 ? "AM" : "PM");
    }

    private View infoRow(Context c, String label, String value, boolean divider) {
        LinearLayout row = Ui.row(c);
        row.setMinimumHeight(Ui.dp(c, 52));
        row.setPadding(Ui.dp(c, 12), Ui.dp(c, 4), Ui.dp(c, 8), Ui.dp(c, 4));
        row.setClickable(true);
        row.addView(Ui.text(c, label, 15, 600, Ui.INK), Ui.weight(1f));
        row.addView(Ui.text(c, value, 14, 400, Ui.MUTED), Ui.wrap());
        ImageView arrow = Ui.icon(c, R.drawable.ic_ds_chevron, 18, Ui.MUTED);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(Ui.dp(c, 18), Ui.dp(c, 18));
        ap.leftMargin = Ui.dp(c, 4);
        row.addView(arrow, ap);
        return row;
    }

    private View link(Context c, String title, String subtitle, int action) {
        LinearLayout row = Ui.row(c);
        row.setBackground(Ui.shape(c, Ui.CARD, 16, Ui.LINE, 1));
        row.setPadding(Ui.dp(c, 16), Ui.dp(c, 14), Ui.dp(c, 12), Ui.dp(c, 14));
        row.setClickable(true);
        LinearLayout names = Ui.column(c);
        names.addView(Ui.text(c, title, 15, 600, Ui.INK));
        Ui.add(names, Ui.text(c, subtitle, 12, 400, Ui.MUTED), 2);
        row.addView(names, Ui.weight(1f));
        row.addView(Ui.icon(c, R.drawable.ic_ds_chevron, 18, Ui.MUTED));
        row.setOnClickListener(v -> navigate(action));
        return row;
    }
}
