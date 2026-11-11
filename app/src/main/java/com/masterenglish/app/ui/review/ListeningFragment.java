package com.masterenglish.app.ui.review;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.masterenglish.app.R;
import com.masterenglish.app.data.local.entity.ListeningEntity;
import com.masterenglish.app.data.local.entity.LessonQuestionEntity;
import com.masterenglish.app.databinding.FragmentListeningBinding;
import com.masterenglish.app.data.local.seed.SeedModels;
import com.masterenglish.app.utils.AudioResources;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Listening practice: audio with speed control, a karaoke transcript that highlights the
 * current line, and questions. Line timing is estimated from each line's length.
 */
@AndroidEntryPoint
public class ListeningFragment extends Fragment {

    private static final long TICK_MS = 120L;
    private static final float[] SPEEDS = {0.75f, 1.0f, 1.25f};

    private FragmentListeningBinding binding;
    private ListeningViewModel viewModel;
    private MediaPlayer player;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = this::tick;
    private final List<TextView> transcriptViews = new ArrayList<>();
    private final List<Float> lineStarts = new ArrayList<>();
    private float speed = 1f;
    private boolean prepared;
    private int highlighted = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentListeningBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ListeningViewModel.class);
        binding.closeButton.setOnClickListener(v -> NavHostFragment.findNavController(this).popBackStack());
        binding.playButton.setOnClickListener(v -> togglePlay());
        binding.transcriptToggle.setOnClickListener(v -> toggleTranscript());
        binding.speedGroup.check(R.id.speed100);
        binding.speedGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            speed = checkedId == R.id.speed075 ? SPEEDS[0] : checkedId == R.id.speed125 ? SPEEDS[2] : SPEEDS[1];
            applySpeed();
        });

        viewModel.lesson().observe(getViewLifecycleOwner(), this::bindLesson);
        viewModel.questions().observe(getViewLifecycleOwner(), list -> {
            if (list != null && !list.isEmpty()) {
                QuizSection.render(getLayoutInflater(), binding.quizContainer, list,
                        viewModel.progress(), viewModel.userId());
            }
        });
        viewModel.load(requireArguments().getString("listeningId", ""));
    }

    private void bindLesson(ListeningEntity lesson) {
        if (lesson == null || binding == null) {
            return;
        }
        binding.title.setText(lesson.title);
        binding.context.setText(lesson.context == null ? "" : lesson.context);
        renderTranscript(parseTranscript(lesson));
        Uri uri = AudioResources.resolve(requireContext(), lesson.audioUrl);
        if (uri == null) {
            binding.playButton.setEnabled(false);
            binding.status.setText(R.string.voice_no_permission);
            return;
        }
        prepare(uri, lesson);
    }

    private void prepare(Uri uri, ListeningEntity lesson) {
        releasePlayer();
        player = new MediaPlayer();
        try {
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build());
            player.setDataSource(requireContext(), uri);
            player.setOnPreparedListener(mp -> {
                prepared = true;
                binding.seekBar.setMax(mp.getDuration());
                binding.totalTime.setText(format(mp.getDuration()));
                if (lesson.durationSeconds == 0) {
                    viewModel.saveDuration(lesson.id, Math.round(mp.getDuration() / 1000f));
                }
                applySpeed();
                binding.playButton.setEnabled(true);
            });
            player.setOnCompletionListener(mp -> {
                setPlayingUi(false);
                binding.seekBar.setProgress(binding.seekBar.getMax());
                highlight(transcriptViews.size() - 1);
            });
            player.prepareAsync();
        } catch (IOException | IllegalStateException e) {
            binding.status.setText(R.string.voice_no_permission);
        }
    }

    private void togglePlay() {
        if (!prepared || player == null) {
            return;
        }
        if (player.isPlaying()) {
            player.pause();
            setPlayingUi(false);
        } else {
            player.start();
            applySpeed();
            setPlayingUi(true);
            handler.post(ticker);
        }
    }

    private void applySpeed() {
        if (player != null && prepared) {
            try {
                player.setPlaybackParams(new PlaybackParams().setSpeed(speed).setPitch(1f));
            } catch (IllegalStateException | IllegalArgumentException ignored) {
                // The player rejects speed changes in some states; the next start re-applies it.
            }
        }
    }

    private void setPlayingUi(boolean playing) {
        binding.playButton.setText(playing ? R.string.listen_pause : R.string.listen_play);
        binding.playButton.setIcon(ContextCompat.getDrawable(requireContext(), playing ? R.drawable.ic_pause : R.drawable.ic_play));
    }

    private void tick() {
        if (player == null || binding == null) {
            return;
        }
        int position = player.getCurrentPosition();
        binding.seekBar.setProgress(position);
        binding.currentTime.setText(format(position));
        float fraction = player.getDuration() <= 0 ? 0f : position / (float) player.getDuration();
        int line = 0;
        for (int i = 0; i < lineStarts.size(); i++) {
            if (fraction >= lineStarts.get(i)) {
                line = i;
            }
        }
        highlight(line);
        if (player.isPlaying()) {
            handler.postDelayed(ticker, TICK_MS);
        }
    }

    private void highlight(int index) {
        if (index == highlighted) {
            return;
        }
        highlighted = index;
        for (int i = 0; i < transcriptViews.size(); i++) {
            TextView line = transcriptViews.get(i);
            boolean active = i == index;
            line.setBackgroundResource(active ? R.drawable.bg_chip_selected : 0);
            line.setAlpha(active ? 1f : 0.55f);
        }
    }

    private void toggleTranscript() {
        boolean visible = binding.transcriptContainer.getVisibility() == View.VISIBLE;
        binding.transcriptContainer.setVisibility(visible ? View.GONE : View.VISIBLE);
        binding.transcriptToggle.setText(visible ? R.string.listen_show_transcript : R.string.listen_hide_transcript);
    }

    /** Parses the stored JSON transcript into speaker/text lines. */
    private List<SeedModels.TranscriptLine> parseTranscript(ListeningEntity lesson) {
        Gson gson = new Gson();
        List<SeedModels.TranscriptLine> lines = gson.fromJson(lesson.transcript,
                new TypeToken<List<SeedModels.TranscriptLine>>() { }.getType());
        return lines == null ? new ArrayList<>() : lines;
    }

    private void renderTranscript(List<SeedModels.TranscriptLine> lines) {
        binding.transcriptContainer.removeAllViews();
        transcriptViews.clear();
        lineStarts.clear();
        int totalChars = 0;
        for (SeedModels.TranscriptLine line : lines) {
            totalChars += line.text.length();
        }
        int cumulative = 0;
        for (SeedModels.TranscriptLine line : lines) {
            lineStarts.add(totalChars == 0 ? 0f : cumulative / (float) totalChars);
            cumulative += line.text.length();
            TextView view = new TextView(requireContext());
            view.setText(line.speaker + ": " + line.text);
            view.setTextSize(16f);
            view.setTextColor(requireContext().getColor(R.color.neutral_text));
            view.setPadding(20, 14, 20, 14);
            view.setAlpha(0.55f);
            binding.transcriptContainer.addView(view);
            transcriptViews.add(view);
        }
        highlighted = -1;
        highlight(0);
    }

    private static String format(int millis) {
        int seconds = millis / 1000;
        return String.format(java.util.Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    private void releasePlayer() {
        handler.removeCallbacks(ticker);
        if (player != null) {
            player.release();
            player = null;
        }
        prepared = false;
    }

    @Override
    public void onPause() {
        if (player != null && player.isPlaying()) {
            player.pause();
            setPlayingUi(false);
        }
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        releasePlayer();
        binding = null;
        super.onDestroyView();
    }
}
