package com.doradodo.app.ui.entertainment;

import androidx.lifecycle.ViewModel;

import com.doradodo.app.data.repository.EntertainmentRepository;
import com.doradodo.app.utils.AppExecutors;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/** Runs the Entertainment repository's database work off the main thread and hands results back on it. */
@HiltViewModel
public class EntertainmentViewModel extends ViewModel {

    private final EntertainmentRepository repository;
    private final AppExecutors executors;

    @Inject
    public EntertainmentViewModel(EntertainmentRepository repository, AppExecutors executors) {
        this.repository = repository;
        this.executors = executors;
    }

    public EntertainmentRepository repo() {
        return repository;
    }

    /** Runs {@code work} on a disk thread; {@code done} gets its result on the main thread. */
    public <T> void async(Callable<T> work, Consumer<T> done) {
        executors.diskIO().execute(() -> {
            T result;
            try {
                result = work.call();
            } catch (Exception e) {
                result = null;
            }
            final T value = result;
            executors.mainThread().execute(() -> done.accept(value));
        });
    }

    public void async(Runnable work) {
        executors.diskIO().execute(work);
    }
}
