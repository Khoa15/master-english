package com.doradodo.app.ui.profile;

import android.app.AlertDialog;
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

import com.doradodo.app.R;
import com.doradodo.app.utils.ZoneOptions;
import com.doradodo.app.utils.ZoneLabel;
import com.doradodo.app.data.repository.UserTimeZone;
import com.doradodo.app.data.local.entity.DailyStudyLogEntity;
import com.doradodo.app.data.local.entity.UserEntity;
import com.doradodo.app.i18n.AppLanguages;
import com.doradodo.app.service.reminder.ReminderScheduler;
import com.doradodo.app.ui.common.TabFragment;
import com.doradodo.app.ui.ds.Ui;
import com.doradodo.app.ui.vocab.VocabViewModel;
import com.doradodo.app.utils.CefrLevel;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.Instant;
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
        header.addView(Ui.text(c, c.getString(R.string.prof_title), 26, 700, Ui.INK), Ui.weight(1f));
        ImageView gear = Ui.icon(c, R.drawable.ic_ds_gear, 22, Ui.INK);
        gear.setPadding(Ui.dp(c, 9), Ui.dp(c, 9), Ui.dp(c, 9), Ui.dp(c, 9));
        gear.setBackground(Ui.shape(c, Ui.CARD, 12, Ui.LINE, 1));
        gear.setContentDescription(c.getString(R.string.prof_settings));
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
        Ui.add(body, section(c, c.getString(R.string.prof_personal)), 22);
        personal = Ui.card(c, 4, 18);
        Ui.add(body, personal, 8);
        Ui.add(body, section(c, c.getString(R.string.prof_insights)), 22);
        Ui.add(body, link(c, c.getString(R.string.prof_analytics), c.getString(R.string.prof_analytics_sub), R.id.action_profile_to_analytics), 8);
        Ui.add(body, link(c, c.getString(R.string.prof_subscription), c.getString(R.string.prof_free_plan), R.id.action_profile_to_subscription), 8);
        Ui.add(body, link(c, c.getString(R.string.prof_feedback), c.getString(R.string.prof_feedback_sub), R.id.action_profile_to_feedback), 8);
        Ui.add(body, section(c, c.getString(R.string.prof_preferences)), 22);
        preferences = Ui.card(c, 4, 18);
        Ui.add(body, preferences, 8);
        TextView signOut = Ui.text(c, c.getString(R.string.profile_logout), 15, 700, Ui.RED);
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
        String name = user.displayName == null || user.displayName.isEmpty() ? c.getString(R.string.prof_learner) : user.displayName;
        TextView avatar = Ui.text(c, name.substring(0, 1).toUpperCase(Locale.getDefault()), 24, 700, Ui.GREEN_INK);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(Ui.shape(c, Ui.GREEN_TINT, 999));
        row.addView(avatar, new LinearLayout.LayoutParams(Ui.dp(c, 56), Ui.dp(c, 56)));
        LinearLayout names = Ui.column(c);
        names.addView(Ui.text(c, name, 18, 700, Ui.INK));
        LocalDate joined = java.time.Instant.ofEpochMilli(user.createdAt).atZone(ZoneId.systemDefault()).toLocalDate();
        Ui.add(names, Ui.text(c, c.getString(R.string.prof_joined, java.time.format.DateTimeFormatter.ofPattern("LLL yyyy",
                Locale.getDefault()).format(joined)), 12, 400, Ui.MUTED), 3);
        TextView level = Ui.pill(c, c.getString(R.string.prof_level, CefrLevel.of(user.experiencePoints)), Ui.AMBER_TINT, Ui.AMBER_INK, 12);
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
        statCard(c, String.format(Locale.getDefault(), "%,d", learned), c.getString(R.string.prof_words_learned), Ui.GREEN_INK, 0);
        statCard(c, String.valueOf(user.streakCount), c.getString(R.string.prof_day_streak), Ui.AMBER_INK, 10);
        statCard(c, String.format(Locale.getDefault(), "%,d", user.experiencePoints), c.getString(R.string.prof_xp), Ui.BLUE_INK, 10);
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
        head.addView(navArrow(c, R.drawable.ic_ds_back, c.getString(R.string.prof_prev_month), -1));
        TextView title = Ui.text(c, java.time.format.DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()).format(month),
                15, 700, Ui.INK);
        title.setGravity(Gravity.CENTER);
        head.addView(title, Ui.weight(1f));
        head.addView(navArrow(c, R.drawable.ic_ds_chevron, c.getString(R.string.prof_next_month), 1));
        calendar.addView(head, Ui.matchWidth());

        LinearLayout weekdays = Ui.row(c);
        for (java.time.DayOfWeek dow : new java.time.DayOfWeek[]{java.time.DayOfWeek.SUNDAY, java.time.DayOfWeek.MONDAY,
                java.time.DayOfWeek.TUESDAY, java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY,
                java.time.DayOfWeek.FRIDAY, java.time.DayOfWeek.SATURDAY}) {
            String d = dow.getDisplayName(TextStyle.NARROW, Locale.getDefault());
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
        legend.addView(Ui.text(c, c.getString(R.string.prof_streak), 12, 600, Ui.MUTED), lp);
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
        bindLanguages();
        // The stored value is the English name, so it survives a change of language; only the label is translated.
        addPicker(personal, c.getString(R.string.prof_country), ProfileViewModel.COUNTRY, defaultCountry(),
                new String[]{"Vietnam", "United States", "United Kingdom", "Australia", "Canada", "Singapore", "Other"},
                new int[]{R.string.country_vietnam, R.string.country_us, R.string.country_uk, R.string.country_au,
                        R.string.country_ca, R.string.country_sg, R.string.country_other});
        addPicker(personal, c.getString(R.string.prof_gender), ProfileViewModel.GENDER, "Prefer not to say",
                new String[]{"Female", "Male", "Non-binary", "Prefer not to say"},
                new int[]{R.string.gender_female, R.string.gender_male, R.string.gender_nonbinary, R.string.gender_none});
        View birthday = infoRow(c, c.getString(R.string.prof_birth), birthdayLabel(c), false);
        birthday.setOnClickListener(v -> {
            LocalDate d = LocalDate.of(2000, 1, 1);
            new DatePickerDialog(c, (picker, y, m, day) ->
                    {
                        viewModel.setInfo(ProfileViewModel.BIRTHDAY, LocalDate.of(y, m + 1, day)
                                .toString());
                        bindPersonal();
                    }, d.getYear(), d.getMonthValue() - 1, d.getDayOfMonth()).show();
        });
        personal.addView(birthday, Ui.matchWidth());
    }

    /** Display language and learning language, both stored in user_settings. */
    private void bindLanguages() {
        viewModel.loadLanguages(state -> {
            if (personal == null || !isAdded()) {
                return;
            }
            Context c = requireContext();
            View display = infoRow(c, c.getString(R.string.prof_display_lang), AppLanguages.of(c).nameOf(state.display), true);
            display.setOnClickListener(v -> pickLanguage(v, AppLanguages.of(c).codes(), lang ->
                    viewModel.setDisplayLanguage(lang, () -> {
                        bindPersonal();
                        com.doradodo.app.ui.common.LocaleSync.apply(lang);
                    })));
            View learning = infoRow(c, c.getString(R.string.prof_learning_lang), AppLanguages.of(c).nameOf(state.learning), true);
            learning.setOnClickListener(v -> pickLanguage(v, state.learnable, lang ->
                    viewModel.setLearningLanguage(lang, this::bindPersonal)));
            // This runs after a database read and bindPersonal can run twice: drop rows an earlier run added.
            for (int i = personal.getChildCount() - 1; i >= 0; i--) {
                if ("language".equals(personal.getChildAt(i).getTag())) {
                    personal.removeViewAt(i);
                }
            }
            display.setTag("language");
            learning.setTag("language");
            // Keep the language rows above the other personal rows.
            personal.addView(display, 0, Ui.matchWidth());
            personal.addView(learning, 1, Ui.matchWidth());
        });
    }

    private void pickLanguage(View anchor, List<String> codes, java.util.function.Consumer<String> onPick) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        for (int i = 0; i < codes.size(); i++) {
            menu.getMenu().add(0, i, i, AppLanguages.of(requireContext()).nameOf(codes.get(i)));
        }
        menu.setOnMenuItemClickListener(item -> {
            onPick.accept(codes.get(item.getItemId()));
            return true;
        });
        menu.show();
    }

    /** The birth date is stored as an ISO date; an older value written as text is shown as it is. */
    private String birthdayLabel(Context c) {
        String stored = viewModel.info(ProfileViewModel.BIRTHDAY, "");
        if (stored.isEmpty()) {
            return c.getString(R.string.prof_not_set);
        }
        try {
            return LocalDate.parse(stored).format(java.time.format.DateTimeFormatter
                    .ofLocalizedDate(java.time.format.FormatStyle.MEDIUM).withLocale(Locale.getDefault()));
        } catch (java.time.format.DateTimeParseException e) {
            return stored;
        }
    }

    /** {@code values} are what is stored; {@code labels} are the string resources shown for them. */
    /** Until the learner picks one, the country is the device's region, not a fixed country. */
    private static String defaultCountry() {
        switch (Locale.getDefault().getCountry()) {
            case "VN":
                return "Vietnam";
            case "US":
                return "United States";
            case "GB":
                return "United Kingdom";
            case "AU":
                return "Australia";
            case "CA":
                return "Canada";
            case "SG":
                return "Singapore";
            default:
                return "Other";
        }
    }

    private void addPicker(LinearLayout parent, String label, String key, String fallback, String[] values, int[] labels) {
        String current = viewModel.info(key, fallback);
        int shown = labels[0];
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(current)) {
                shown = labels[i];
            }
        }
        View row = infoRow(requireContext(), label, getString(shown), true);
        row.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(requireContext(), v);
            for (int i = 0; i < values.length; i++) {
                menu.getMenu().add(0, i, i, getString(labels[i]));
            }
            menu.setOnMenuItemClickListener(item -> {
                viewModel.setInfo(key, values[item.getItemId()]);
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
        View goal = infoRow(c, c.getString(R.string.prof_daily_goal), Ui.plural(c, R.plurals.plural_words, viewModel.dailyGoal()), true);
        goal.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(c, v);
            int[] goals = {10, 20, 30, 50};
            for (int i = 0; i < goals.length; i++) {
                menu.getMenu().add(0, goals[i], i, Ui.plural(c, R.plurals.plural_words, goals[i]));
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
        View reminder = infoRow(c, c.getString(R.string.prof_reminders), viewModel.preferences().isReminderEnabled()
                ? formatTime(c, hour, minute) : c.getString(R.string.prof_off), true);
        reminder.setOnClickListener(v -> new TimePickerDialog(c, (picker, h, m) -> {
            viewModel.preferences().saveReminder(true, h, m);
            ReminderScheduler.schedule(c, h, m);
            bindPreferences();
        }, hour, minute, false).show());
        preferences.addView(reminder, Ui.matchWidth());
        preferences.addView(timeZoneRow(c), Ui.matchWidth());
    }

    /** The time zone dates are shown in: "UTC+7 · Ho Chi Minh (Auto)"; tap to pick another or follow the device. */
    private View timeZoneRow(Context c) {
        UserTimeZone userZone = viewModel.timeZone();
        Instant now = Instant.now();
        ZoneId zone = userZone.zone();
        String city = zone.getId().substring(zone.getId().lastIndexOf('/') + 1).replace('_', ' ');
        String value = ZoneLabel.format(zone, now) + " · " + city
                + (userZone.isAutomatic() ? " (" + c.getString(R.string.prof_tz_auto_tag) + ")" : "");
        View row = infoRow(c, c.getString(R.string.prof_time_zone), value, true);
        row.setOnClickListener(v -> {
            List<String> ids = ZoneOptions.build(now);
            String[] labels = new String[ids.size() + 1];
            labels[0] = c.getString(R.string.prof_tz_auto);
            for (int i = 0; i < ids.size(); i++) {
                labels[i + 1] = ZoneLabel.format(ZoneId.of(ids.get(i)), now) + " · " + ids.get(i).replace('_', ' ');
            }
            new AlertDialog.Builder(c).setTitle(R.string.prof_tz_pick).setItems(labels, (dialog, which) -> {
                if (which == 0) {
                    userZone.useAutomatic();
                } else {
                    userZone.set(ids.get(which - 1));
                }
                bindPreferences();
            }).show();
        });
        return row;
    }

    private static String formatTime(Context c, int hour, int minute) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, hour);
        cal.set(java.util.Calendar.MINUTE, minute);
        return android.text.format.DateFormat.getTimeFormat(c).format(cal.getTime());
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
