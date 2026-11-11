package com.masterenglish.app.ui.roadmap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.masterenglish.app.data.repository.RoadmapItem;
import com.masterenglish.app.databinding.BottomSheetLessonPreviewBinding;
import com.masterenglish.app.utils.SoundManager;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Summary of a node with the "BẮT ĐẦU" button. */
@AndroidEntryPoint
public class LessonPreviewSheet extends BottomSheetDialogFragment {

    private static final String ARG_NODE_ID = "nodeId";
    private static final String ARG_TITLE = "title";
    private static final String ARG_SUMMARY = "summary";
    private static final String ARG_TYPE = "type";

    public interface Listener {
        void onStartLesson(String nodeId);
    }

    @Inject
    SoundManager soundManager;

    private BottomSheetLessonPreviewBinding binding;

    public static LessonPreviewSheet newInstance(RoadmapItem item) {
        LessonPreviewSheet sheet = new LessonPreviewSheet();
        Bundle args = new Bundle();
        args.putString(ARG_NODE_ID, item.node.id);
        args.putString(ARG_TITLE, item.node.title);
        args.putString(ARG_SUMMARY, item.node.summary);
        args.putString(ARG_TYPE, item.node.nodeType.name());
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetLessonPreviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        binding.previewTitle.setText(args.getString(ARG_TITLE));
        binding.previewSummary.setText(args.getString(ARG_SUMMARY));
        binding.previewType.setText(labelFor(args.getString(ARG_TYPE)));
        binding.startButton.setOnClickListener(v -> {
            soundManager.play(SoundManager.Effect.POP);
            Fragment parent = getParentFragment();
            if (parent instanceof Listener) {
                ((Listener) parent).onStartLesson(args.getString(ARG_NODE_ID));
            }
            dismiss();
        });
    }

    private static String labelFor(String type) {
        if (type == null) {
            return "";
        }
        switch (type) {
            case "GRAMMAR":
                return "Ngữ pháp";
            case "LISTENING":
                return "Luyện nghe";
            case "READING":
                return "Luyện đọc";
            case "CHECKPOINT_BOSS":
                return "Boss giai đoạn";
            default:
                return "Từ vựng";
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
