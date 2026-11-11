package com.masterenglish.app.data.remote.agent;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Url;

/**
 * AI Agent endpoint behind the API Gateway. The gateway URL comes from encrypted storage at call time,
 * so it is passed per request with {@link Url} rather than fixed in a base URL.
 */
public interface AgentApi {

    @POST
    Call<AgentDtos.Response> chat(
            @Url String endpoint,
            @Header("Authorization") String authorization,
            @Body AgentDtos.Request body);
}
