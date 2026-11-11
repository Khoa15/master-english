package com.masterenglish.app.data.local.seed;

import com.google.gson.JsonElement;

import java.util.List;

/**
 * Gson DTOs that mirror the four master JSON files in assets/data/.
 * Field names intentionally match the JSON keys.
 */
public final class SeedModels {

    private SeedModels() {
    }

    public static class VocabularySeed {
        public String id;
        public String term;
        public String ipa;
        public String part_of_speech;
        public String cefr_level;
        public String domain;
        public String definition_vi;
        public String example_en;
        public String example_vi;
        public List<String> synonyms;
        public List<String> antonyms;
    }

    public static class GrammarSeed {
        public String id;
        public String topic;
        public String cefr_level;
        public String domain;
        public String explanation_vi;
        public String formula;
        public String usage_context;
        public JsonElement examples;
        public JsonElement common_mistakes;
    }

    public static class ReadingSeed {
        public String id;
        public String title;
        public String cefr_level;
        public String domain;
        public String passage;
        public List<QuestionSeed> questions;
        public List<String> key_vocabulary;
    }

    public static class ListeningSeed {
        public String id;
        public String title;
        public String cefr_level;
        public String domain;
        public String context;
        public List<TranscriptLine> transcript;
        public List<QuestionSeed> questions;
    }

    public static class QuestionSeed {
        public String question_id;
        public String question;
        public List<String> options;
        public String answer;
        public String explanation_vi;
    }

    public static class TranscriptLine {
        public String speaker;
        public String text;
    }
}
