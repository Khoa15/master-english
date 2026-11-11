package com.masterenglish.app.data.remote;

import com.masterenglish.app.data.remote.dto.SyncAck;
import com.masterenglish.app.data.remote.dto.SyncPayload;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/** Backend REST API backing the MySQL server (base URL from BuildConfig.API_BASE_URL). */
public interface ApiService {

    @POST("sync/progress")
    Call<SyncAck> pushProgress(@Body SyncPayload payload);

    @GET("sync/progress")
    Call<SyncPayload> pullProgress(@Query("userId") String userId);
}
