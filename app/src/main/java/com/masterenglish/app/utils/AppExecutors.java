package com.masterenglish.app.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/** Shared thread pools. Room and network work never run on the main thread. */
@Singleton
public class AppExecutors {

    private final Executor diskIO = Executors.newFixedThreadPool(3);
    private final Executor mainThread = new MainThreadExecutor();

    @Inject
    public AppExecutors() {
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
