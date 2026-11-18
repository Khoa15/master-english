package com.doradodo.app.data.remote;

import com.doradodo.app.data.remote.dto.FeedbackRequest;
import com.doradodo.app.data.remote.dto.SyncAck;
import com.doradodo.app.data.remote.dto.SyncPayload;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * Backend REST API backing the MySQL server (base URL from BuildConfig.API_BASE_URL).
 * Every path starts with its API version ("v1/..."). A breaking change gets a new "v2/..." path next to the
 * old one; paths are never edited in place, because installed apps keep calling the version they shipped with.
 * Requests carry the learner's Firebase ID token (see AuthInterceptor).
 */
public interface ApiService {

    @POST("v1/sync/progress")
    Call<SyncAck> pushProgress(@Body SyncPayload payload);

    @GET("v1/sync/progress")
    Call<SyncPayload> pullProgress(@Query("userId") String userId);

    @POST("v1/feedback")
    Call<SyncAck> sendFeedback(@Body FeedbackRequest request);
}
