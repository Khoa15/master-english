package com.doradodo.app.ui.entertainment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.doradodo.app.utils.AppExecutors;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class EntertainmentViewModelTest {

    /** Direct executors: the work and the hand-back both run right where they are submitted. */
    private static EntertainmentViewModel viewModel() {
        AppExecutors direct = new AppExecutors(Runnable::run, Runnable::run);
        return new EntertainmentViewModel(null, null, null, null, direct);
    }

    @Test
    public void asyncHandsTheResultBack() {
        List<Integer> got = new ArrayList<>();
        viewModel().async(() -> 42, got::add);
        assertEquals(1, got.size());
        assertEquals(42, (int) got.get(0));
    }

    @Test
    public void aFailingJobHandsBackNull() {
        List<String> got = new ArrayList<>();
        viewModel().<String>async(() -> {
            throw new IllegalStateException("boom");
        }, got::add);
        assertEquals(1, got.size());
        assertNull(got.get(0));
    }

    @Test
    public void asyncWithoutAResultJustRunsTheWork() {
        int[] ran = {0};
        viewModel().async(() -> ran[0]++);
        assertEquals(1, ran[0]);
    }
}
