package com.masterenglish.app.service.ai;

import com.masterenglish.app.BuildConfig;
import com.masterenglish.app.data.remote.agent.AgentApi;
import com.masterenglish.app.data.remote.agent.AgentDtos;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import okhttp3.HttpUrl;
import retrofit2.Response;

/**
 * Blocking calls to the AI Agent through the API Gateway. Callers must be on a background thread.
 * The gateway URL and token are developer config from BuildConfig (agent.gateway.url / agent.gateway.token
 * in local.properties, or AGENT_GATEWAY_URL / AGENT_GATEWAY_TOKEN in the environment).
 */
@Singleton
public class AgentClient {

    public static final String NOT_CONFIGURED = "AGENT_NOT_CONFIGURED";

    private final AgentApi api;

    @Inject
    public AgentClient(AgentApi api) {
        this.api = api;
    }

    public boolean isConfigured() {
        return !BuildConfig.AGENT_GATEWAY_URL.isEmpty() && !BuildConfig.AGENT_GATEWAY_TOKEN.isEmpty();
    }

    public String generate(String systemPrompt, String userMessage) throws IOException {
        String endpoint = BuildConfig.AGENT_GATEWAY_URL;
        String token = BuildConfig.AGENT_GATEWAY_TOKEN;
        if (endpoint.isEmpty() || token.isEmpty()) {
            throw new IOException(NOT_CONFIGURED);
        }
        // The token is sent in a header, so refuse any endpoint that is not HTTPS.
        HttpUrl parsed = HttpUrl.parse(endpoint);
        if (parsed == null || !"https".equals(parsed.scheme())) {
            throw new IOException("Gateway URL must use HTTPS");
        }

        AgentDtos.Request body = new AgentDtos.Request(systemPrompt, userMessage);
        Response<AgentDtos.Response> response = api.chat(endpoint, "Bearer " + token, body).execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("Agent gateway HTTP " + response.code());
        }
        String text = response.body().text;
        if (text == null || text.trim().isEmpty()) {
            throw new IOException("Agent returned no text");
        }
        return text;
    }
}
