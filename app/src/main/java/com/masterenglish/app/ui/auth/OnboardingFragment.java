package com.masterenglish.app.ui.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.masterenglish.app.MainActivity;
import com.masterenglish.app.R;
import com.masterenglish.app.data.local.prefs.OnboardingAnswers;
import com.masterenglish.app.databinding.FragmentOnboardingBinding;
import com.masterenglish.app.ui.lesson.questions.SimpleTextWatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Eight screens: a welcome, six survey questions with a progress bar, then a summary of the plan
 * before sign-in. Each screen is its own layout; the pager only moves between them.
 */
@AndroidEntryPoint
public class OnboardingFragment extends Fragment {

    private static final int PAGE_WELCOME = 0;
    private static final int PAGE_SUMMARY = 7;
    private static final int PAGE_COUNT = 8;
    private static final int QUESTION_COUNT = 6;
    private static final int MAX_SKILLS = 3;

    private static final int[] PAGE_LAYOUTS = {
            R.layout.layout_onboarding_welcome,
            R.layout.layout_onboarding_who,
            R.layout.layout_onboarding_age,
            R.layout.layout_onboarding_goal,
            R.layout.layout_onboarding_time,
            R.layout.layout_onboarding_skills,
            R.layout.layout_onboarding_job,
            R.layout.layout_onboarding_ready
    };

    private static final int[] ROLE_BUTTONS = {R.id.roleStudent, R.id.roleProfessional, R.id.roleTeacher, R.id.roleJobSeeker};
    private static final int[] ROLE_LABELS = {R.string.onboard_role_student, R.string.onboard_role_professional,
            R.string.onboard_role_teacher, R.string.onboard_role_job_seeker};

    private static final int[] AGE_BUTTONS = {R.id.ageUnder18, R.id.age18to24, R.id.age25to34, R.id.age35to44,
            R.id.age45to54, R.id.age55plus};
    private static final int[] AGE_LABELS = {R.string.onboard_age_under_18, R.string.onboard_age_18_24,
            R.string.onboard_age_25_34, R.string.onboard_age_35_44, R.string.onboard_age_45_54,
            R.string.onboard_age_55_plus};

    private static final int[] GOAL_BUTTONS = {R.id.goalExam, R.id.goalCareer, R.id.goalAbroad, R.id.goalOverseas,
            R.id.goalTravel, R.id.goalChat};
    private static final int[] GOAL_LABELS = {R.string.onboard_goal_exam, R.string.onboard_goal_career,
            R.string.onboard_goal_abroad, R.string.onboard_goal_overseas, R.string.onboard_goal_travel,
            R.string.onboard_goal_chat};

    private static final int[] MINUTE_BUTTONS = {R.id.min10, R.id.min20, R.id.min30, R.id.min45};
    private static final int[] MINUTE_VALUES = {10, 20, 30, 45};

    private static final int[] SKILL_BUTTONS = {R.id.skillGrammar, R.id.skillVocabulary, R.id.skillListening,
            R.id.skillReading, R.id.skillWriting, R.id.skillSpeaking};
    private static final int[] SKILL_LABELS = {R.string.onboard_skill_grammar, R.string.onboard_skill_vocabulary,
            R.string.onboard_skill_listening, R.string.onboard_skill_reading, R.string.onboard_skill_writing,
            R.string.onboard_skill_speaking};

    private static final int[] JOB_BUTTONS = {R.id.jobSoftware, R.id.jobHealthcare, R.id.jobBusiness,
            R.id.jobEducation, R.id.jobCreative, R.id.jobFinance, R.id.jobNotWorking};
    private static final int[] JOB_LABELS = {R.string.onboard_job_software, R.string.onboard_job_healthcare,
            R.string.onboard_job_business, R.string.onboard_job_education, R.string.onboard_job_creative,
            R.string.onboard_job_finance, R.string.onboard_job_not_working};

    private FragmentOnboardingBinding binding;
    private OnboardingViewModel viewModel;
    private OnBackPressedCallback backCallback;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(OnboardingViewModel.class);
        binding.pager.setAdapter(new PagesAdapter());
        binding.pager.setUserInputEnabled(false);
        binding.pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                showPage(position);
            }
        });
        binding.nextButton.setOnClickListener(v -> goForward());
        binding.backButton.setOnClickListener(v -> goBack());

        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                goBack();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);
        showPage(binding.pager.getCurrentItem());
    }

    /** Updates the header, the button label and the back gesture for the visible screen. */
    private void showPage(int position) {
        boolean questionPage = position >= 1 && position < PAGE_SUMMARY;
        binding.header.setVisibility(questionPage ? View.VISIBLE : View.INVISIBLE);
        if (questionPage) {
            binding.progress.setProgressCompat(position * 100 / QUESTION_COUNT, true);
            binding.progressLabel.setText(getString(R.string.onboard_step_format, position, QUESTION_COUNT));
        }
        binding.nextButton.setText(nextLabel(position));
        backCallback.setEnabled(position > 0);
        if (position == PAGE_SUMMARY) {
            // The summary shows answers given on earlier screens, so refresh it each time it opens.
            binding.pager.getAdapter().notifyItemChanged(PAGE_SUMMARY);
        }
    }

    private static int nextLabel(int position) {
        if (position == PAGE_WELCOME) {
            return R.string.onboard_get_started;
        }
        return position == PAGE_SUMMARY ? R.string.onboard_start_learning : R.string.onboard_continue;
    }

    private void goForward() {
        int current = binding.pager.getCurrentItem();
        if (current < PAGE_SUMMARY) {
            binding.pager.setCurrentItem(current + 1, true);
        } else {
            finishOnboarding();
        }
    }

    private void goBack() {
        int current = binding.pager.getCurrentItem();
        if (current > 0) {
            binding.pager.setCurrentItem(current - 1, true);
        }
    }

    private void finishOnboarding() {
        viewModel.saveAnswers();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).requestNotificationPermissionIfNeeded();
        }
        NavHostFragment.findNavController(this).navigate(R.id.action_onboarding_to_auth);
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    /** Pages are inflated by hand because each one is a different layout with its own controls. */
    private final class PagesAdapter extends RecyclerView.Adapter<PageHolder> {

        @NonNull
        @Override
        public PageHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View page = LayoutInflater.from(parent.getContext()).inflate(PAGE_LAYOUTS[viewType], parent, false);
            page.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            bindPage(page, viewType);
            return new PageHolder(page);
        }

        @Override
        public void onBindViewHolder(@NonNull PageHolder holder, int position) {
            if (position == PAGE_SUMMARY) {
                bindSummary(holder.itemView);
            }
        }

        @Override
        public int getItemCount() {
            return PAGE_COUNT;
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }
    }

    /** Wires each screen's controls once, when it is created. Listeners stay with the view. */
    private void bindPage(View page, int position) {
        switch (position) {
            case 1:
                bindWho(page);
                break;
            case 2:
                bindChoice(page.findViewById(R.id.ageGroup), AGE_BUTTONS, AGE_LABELS,
                        viewModel.answers().ageRange, value -> viewModel.answers().ageRange = value);
                break;
            case 3:
                bindChoice(page.findViewById(R.id.goalGroup), GOAL_BUTTONS, GOAL_LABELS,
                        viewModel.answers().goal, value -> viewModel.answers().goal = value);
                break;
            case 4:
                bindMinutes(page.findViewById(R.id.minutesGroup));
                break;
            case 5:
                bindSkills(page.findViewById(R.id.skillsGroup));
                break;
            case 6:
                bindChoice(page.findViewById(R.id.jobGroup), JOB_BUTTONS, JOB_LABELS,
                        viewModel.answers().job, value -> viewModel.answers().job = value);
                break;
            default:
                break;
        }
    }

    private void bindWho(View page) {
        OnboardingAnswers answers = viewModel.answers();
        TextInputEditText nameInput = page.findViewById(R.id.nameInput);
        nameInput.setText(answers.name);
        nameInput.addTextChangedListener(new SimpleTextWatcher(
                () -> answers.name = nameInput.getText() == null ? "" : nameInput.getText().toString()));
        bindChoice(page.findViewById(R.id.roleGroup), ROLE_BUTTONS, ROLE_LABELS, answers.role,
                value -> answers.role = value);
    }

    /** Single choice: checks the button whose label matches the saved answer, then stores what is picked. */
    private void bindChoice(MaterialButtonToggleGroup group, int[] buttonIds, int[] labelIds, String selected,
                            Consumer<String> store) {
        for (int i = 0; i < buttonIds.length; i++) {
            if (getString(labelIds[i]).equals(selected)) {
                group.check(buttonIds[i]);
            }
        }
        group.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            for (int i = 0; i < buttonIds.length; i++) {
                if (buttonIds[i] == checkedId) {
                    store.accept(getString(labelIds[i]));
                }
            }
        });
    }

    private void bindMinutes(MaterialButtonToggleGroup group) {
        OnboardingAnswers answers = viewModel.answers();
        for (int i = 0; i < MINUTE_BUTTONS.length; i++) {
            if (MINUTE_VALUES[i] == answers.dailyMinutes) {
                group.check(MINUTE_BUTTONS[i]);
            }
        }
        group.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            for (int i = 0; i < MINUTE_BUTTONS.length; i++) {
                if (MINUTE_BUTTONS[i] == checkedId) {
                    answers.dailyMinutes = MINUTE_VALUES[i];
                }
            }
        });
    }

    /** Multiple choice with a limit: a fourth skill is refused by unchecking it again. */
    private void bindSkills(MaterialButtonToggleGroup group) {
        OnboardingAnswers answers = viewModel.answers();
        for (int i = 0; i < SKILL_BUTTONS.length; i++) {
            if (answers.skills.contains(getString(SKILL_LABELS[i]))) {
                group.check(SKILL_BUTTONS[i]);
            }
        }
        group.addOnButtonCheckedListener((g, checkedId, isChecked) -> {
            if (isChecked && g.getCheckedButtonIds().size() > MAX_SKILLS) {
                g.uncheck(checkedId);
                return;
            }
            answers.skills = checkedLabels(g, SKILL_BUTTONS, SKILL_LABELS);
        });
    }

    private List<String> checkedLabels(MaterialButtonToggleGroup group, int[] buttonIds, int[] labelIds) {
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < buttonIds.length; i++) {
            if (group.getCheckedButtonIds().contains(buttonIds[i])) {
                labels.add(getString(labelIds[i]));
            }
        }
        return labels;
    }

    private void bindSummary(View page) {
        OnboardingAnswers answers = viewModel.answers();
        setText(page, R.id.readyWho, answers.role);
        setText(page, R.id.readyAge, answers.ageRange);
        setText(page, R.id.readyGoal, answers.goal);
        setText(page, R.id.readyTime, getString(R.string.onboard_minutes_format, answers.dailyMinutes));
        setText(page, R.id.readyFocus, String.join(", ", answers.skills));
        setText(page, R.id.readyJob, answers.job);
    }

    private static void setText(View page, int viewId, String value) {
        ((TextView) page.findViewById(viewId)).setText(value);
    }

    private static final class PageHolder extends RecyclerView.ViewHolder {
        PageHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
