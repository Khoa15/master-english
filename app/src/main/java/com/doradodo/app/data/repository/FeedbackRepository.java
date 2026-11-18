package com.doradodo.app.data.repository;

import android.os.Build;

import com.doradodo.app.BuildConfig;
import com.doradodo.app.data.local.prefs.AppPreferences;
import com.doradodo.app.data.remote.ApiService;
import com.doradodo.app.data.remote.dto.FeedbackRequest;
import com.doradodo.app.data.remote.dto.SyncAck;
import com.doradodo.app.utils.AppExecutors;
import com.doradodo.app.utils.Callback;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

/** Sends learner feedback to the backend. The result is reported on the main thread. */
@Singleton
public class FeedbackRepository {

    private final ApiService api;
    private final AppPreferences preferences;
    private final AppExecutors executors;

    @Inject
    public FeedbackRepository(ApiService api, AppPreferences preferences, AppExecutors executors) {
        this.api = api;
        this.preferences = preferences;
        this.executors = executors;
    }

    public static final int OK = 0;
    public static final int FAILED = 1;
    /** Guest, signed out, or the server rejected the token. */
    public static final int SIGN_IN_REQUIRED = 2;
    public static final int RATE_LIMITED = 3;

    /** {@code onDone} receives one of the result codes above, on the main thread. */
    public void send(int rating, String type, String message, Callback<Integer> onDone) {
        String userId = preferences.getCurrentUserId();
        if (userId == null || userId.startsWith("guest-")) {
            // Feedback is for signed-in accounts; do not even call the server.
            executors.mainThread().execute(() -> onDone.onResult(SIGN_IN_REQUIRED));
            return;
        }
        FeedbackRequest request = new FeedbackRequest();
        request.rating = rating;
        request.type = type;
        request.message = message;
        request.appVersion = BuildConfig.VERSION_NAME;
        request.sdkInt = Build.VERSION.SDK_INT;
        request.locale = Locale.getDefault().toLanguageTag();
        request.sentAt = System.currentTimeMillis();
        executors.diskIO().execute(() -> {
            int result;
            try {
                Response<SyncAck> response = api.sendFeedback(request).execute();
                int code = response.code();
                result = response.isSuccessful() ? OK
                        : code == 401 ? SIGN_IN_REQUIRED
                        : code == 429 ? RATE_LIMITED : FAILED;
            } catch (Exception e) {
                result = FAILED;
            }
            int finalResult = result;
            executors.mainThread().execute(() -> onDone.onResult(finalResult));
        });
    }
}
