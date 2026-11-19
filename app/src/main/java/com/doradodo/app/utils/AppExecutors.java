package com.doradodo.app.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Shared thread pools. Room and network work never run on the main thread. */
@Singleton
public class AppExecutors {

    private final Executor diskIO;
    private final Executor mainThread;

    @Inject
    public AppExecutors() {
        this(Executors.newFixedThreadPool(3), new MainThreadExecutor());
    }

    /** Runs work on the given executors; tests pass direct ones so results arrive without threads. */
    public AppExecutors(Executor diskIO, Executor mainThread) {
        this.diskIO = diskIO;
        this.mainThread = mainThread;
    }

    public Executor diskIO() {
        return diskIO;
    }

    public Executor mainThread() {
        return mainThread;
    }

    private static final class MainThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            handler.post(command);
        }
    }
}
