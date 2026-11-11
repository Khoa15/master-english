package com.masterenglish.app.data.remote.gemini;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

/** Google Gemini REST API. The key travels in a header, never in the URL. */
public interface GeminiApi {

    @POST("v1beta/models/{model}:generateContent")
    Call<GeminiDtos.Response> generateContent(
            @Path("model") String model,
            @Header("x-goog-api-key") String apiKey,
            @Body GeminiDtos.Request body);
}
