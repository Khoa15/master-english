package com.masterenglish.app.data.remote.agent;

import com.google.gson.annotations.SerializedName;

/**
 * Request and response bodies for the AI Agent behind the API Gateway.
 * Adjust the field names here if the gateway contract differs.
 */
public final class AgentDtos {

    private AgentDtos() {
    }

    public static final class Request {
        @SerializedName("system_prompt")
        public final String systemPrompt;

        @SerializedName("user_message")
        public final String userMessage;

        public Request(String systemPrompt, String userMessage) {
            this.systemPrompt = systemPrompt;
            this.userMessage = userMessage;
        }
    }

    public static final class Response {
        @SerializedName("text")
        public String text;
    }
}
