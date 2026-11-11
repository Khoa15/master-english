package com.masterenglish.app.ui.review;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.masterenglish.app.R;
import com.masterenglish.app.data.local.entity.GrammarEntity;
import com.masterenglish.app.data.local.entity.ListeningEntity;
import com.masterenglish.app.data.local.entity.ReadingEntity;
import com.masterenglish.app.data.local.entity.VocabularyEntity;
import com.masterenglish.app.databinding.FragmentReviewBinding;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/** Review tab: Weakness Clinic, SRS entry, and four skill tabs. */
@AndroidEntryPoint
public class ReviewFragment extends Fragment {

    private FragmentReviewBinding binding;
    private ReviewViewModel viewModel;
    private ReviewAdapter adapter;
    private WeaknessAdapter weaknessAdapter;

    private List<VocabularyEntity> vocabulary = new ArrayList<>();
    private List<GrammarEntity> grammar = new ArrayList<>();
    private List<ListeningEntity> listening = new ArrayList<>();
    private List<ReadingEntity> reading = new ArrayList<>();
    private int selectedTab;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentReviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ReviewViewModel.class);

        weaknessAdapter = new WeaknessAdapter();
        binding.weaknessList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.weaknessList.setAdapter(weaknessAdapter);
        binding.weaknessList.setNestedScrollingEnabled(false);

        adapter = new ReviewAdapter(this::openRow);
        binding.skillList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.skillList.setAdapter(adapter);
        binding.skillList.setNestedScrollingEnabled(false);

        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.review_tab_vocab));
        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.review_tab_grammar));
        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.review_tab_listening));
        binding.tabs.addTab(binding.tabs.newTab().setText(R.string.review_tab_reading));
        binding.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab.getPosition();
                renderTab();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        binding.flashcardButton.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_review_to_flashcards));
        binding.aiCard.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_review_to_ai));

        viewModel.weaknesses().observe(getViewLifecycleOwner(), items -> {
            weaknessAdapter.submitList(items);
            binding.clinicEmpty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
        });
        viewModel.dueCount().observe(getViewLifecycleOwner(), count ->
                binding.dueText.setText(getString(R.string.review_due_format, count == null ? 0 : count)));
        viewModel.vocabulary().observe(getViewLifecycleOwner(), items -> {
            vocabulary = items;
            renderTab();
        });
        viewModel.grammar().observe(getViewLifecycleOwner(), items -> {
            grammar = items;
            renderTab();
        });
        viewModel.listening().observe(getViewLifecycleOwner(), items -> {
            listening = items;
            renderTab();
        });
        viewModel.reading().observe(getViewLifecycleOwner(), items -> {
            reading = items;
            renderTab();
        });
    }

    private void renderTab() {
        if (binding == null) {
            return;
        }
        List<ReviewRow> rows = new ArrayList<>();
        switch (selectedTab) {
            case 1:
                for (GrammarEntity g : grammar) {
                    rows.add(new ReviewRow(g.id, ReviewRow.Kind.GRAMMAR, g.topicName,
                            g.cefrLevel + " · " + (g.domain == null ? "" : g.domain), g.ruleExplanationMarkdown
                            + (g.formula == null ? "" : "\n\nCông thức: " + g.formula)));
                }
                break;
            case 2:
                for (ListeningEntity l : listening) {
                    rows.add(new ReviewRow(l.id, ReviewRow.Kind.LISTENING, l.title, l.cefrLevel, l.context));
                }
                break;
            case 3:
                for (ReadingEntity r : reading) {
                    rows.add(new ReviewRow(r.id, ReviewRow.Kind.READING, r.title, r.cefrLevel, null));
                }
                break;
            default:
                for (VocabularyEntity v : ReviewViewModel.sorted(vocabulary)) {
                    rows.add(new ReviewRow(v.id, ReviewRow.Kind.VOCABULARY, v.word,
                            (v.phonetic == null ? "" : v.phonetic + "  ") + v.meaningVi,
                            v.exampleSentence == null ? "" : v.exampleSentence
                                    + (v.exampleTranslation == null ? "" : "\n" + v.exampleTranslation)));
                }
                break;
        }
        adapter.submit(rows);
    }

    private void openRow(ReviewRow row) {
        Bundle args = new Bundle();
        if (row.kind == ReviewRow.Kind.LISTENING) {
            args.putString("listeningId", row.id);
            NavHostFragment.findNavController(this).navigate(R.id.action_review_to_listening, args);
        } else if (row.kind == ReviewRow.Kind.READING) {
            args.putString("readingId", row.id);
            NavHostFragment.findNavController(this).navigate(R.id.action_review_to_reading, args);
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
