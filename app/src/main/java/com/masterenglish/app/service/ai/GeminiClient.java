package com.masterenglish.app.service.ai;

import com.masterenglish.app.BuildConfig;
import com.masterenglish.app.data.local.prefs.AppPreferences;
import com.masterenglish.app.data.remote.gemini.GeminiApi;
import com.masterenglish.app.data.remote.gemini.GeminiDtos;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import retrofit2.Response;

/** Blocking Gemini calls. Callers must be on a background thread. */
@Singleton
public class GeminiClient {

    private final GeminiApi api;
    private final AppPreferences preferences;

    @Inject
    public GeminiClient(GeminiApi api, AppPreferences preferences) {
        this.api = api;
        this.preferences = preferences;
    }

    /** A key typed in Settings wins over the one in local.properties. */
    public String apiKey() {
        String override = preferences.getGeminiApiKey();
        if (override != null && !override.isEmpty()) {
            return override;
        }
        return BuildConfig.GEMINI_API_KEY;
    }

    public boolean hasApiKey() {
        return !apiKey().isEmpty();
    }

    public String generate(String systemPrompt, String userMessage) throws IOException {
        String key = apiKey();
        if (key.isEmpty()) {
            throw new IOException("NO_API_KEY");
        }
        GeminiDtos.Request body = new GeminiDtos.Request();
        body.systemInstruction = GeminiDtos.Content.of(null, systemPrompt);
        body.contents.add(GeminiDtos.Content.of("user", userMessage));
        body.generationConfig = new GeminiDtos.GenerationConfig();

        Response<GeminiDtos.Response> response = api.generateContent(BuildConfig.GEMINI_MODEL, key, body).execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("Gemini HTTP " + response.code());
        }
        String text = response.body().firstText();
        if (text == null) {
            throw new IOException("Gemini returned no text");
        }
        return text;
    }
}
