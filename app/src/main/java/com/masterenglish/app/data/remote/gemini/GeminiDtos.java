package com.masterenglish.app.data.remote.gemini;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/** Request and response bodies for the Gemini generateContent endpoint. */
public final class GeminiDtos {

    private GeminiDtos() {
    }

    public static final class Request {
        @SerializedName("system_instruction")
        public Content systemInstruction;
        public List<Content> contents = new ArrayList<>();
        @SerializedName("generationConfig")
        public GenerationConfig generationConfig;
    }

    public static final class Content {
        public String role;
        public List<Part> parts = new ArrayList<>();

        public static Content of(String role, String text) {
            Content content = new Content();
            content.role = role;
            Part part = new Part();
            part.text = text;
            content.parts.add(part);
            return content;
        }
    }

    public static final class Part {
        public String text;
    }

    public static final class GenerationConfig {
        public double temperature = 0.7;
        @SerializedName("responseMimeType")
        public String responseMimeType = "application/json";
        @SerializedName("maxOutputTokens")
        public int maxOutputTokens = 512;
    }

    public static final class Response {
        public List<Candidate> candidates;

        /** Concatenated text of the first candidate, or null when Gemini returned nothing. */
        public String firstText() {
            if (candidates == null || candidates.isEmpty() || candidates.get(0).content == null) {
                return null;
            }
            StringBuilder builder = new StringBuilder();
            for (Part part : candidates.get(0).content.parts) {
                if (part.text != null) {
                    builder.append(part.text);
                }
            }
            return builder.length() == 0 ? null : builder.toString();
        }
    }

    public static final class Candidate {
        public Content content;
    }
}
