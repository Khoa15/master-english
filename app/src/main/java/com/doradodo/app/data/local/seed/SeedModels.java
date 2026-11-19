package com.doradodo.app.data.local.seed;

import java.util.List;
import java.util.Map;

/** Gson DTOs that mirror assets/data/vocabulary.<lang>.json (built by tools/build_vocabulary.py). */
public final class SeedModels {

    private SeedModels() {
    }

    public static class VocabularyFile {
        /** The language of every word and pack of the file (BCP-47). */
        public String lang;
        public List<LevelSeed> levels;
        public List<PackSeed> packs;
        public List<WordSeed> words;
    }

    public static class LevelSeed {
        public String system;
        public String code;
        public int position;
    }

    public static class PackSeed {
        public String id;
        public String title;
        public String level_system;
        public String level_code;
        public int position;
        /** Title per language code; {@link #title} is the English one. May be missing in older assets. */
        public Map<String, String> titles;
        /** Ids of the words in this pack, in the order they are listed. */
        public List<String> words;
    }

    public static class WordSeed {
        public String id;
        public String term;
        public String ipa;
        public String level_system;
        public String level_code;
        public int rank;
        public List<SenseSeed> senses;
    }

    public static class SenseSeed {
        public String pos;
        public String definition;
        public String definition_vi;
        public String example;
        public String example_vi;
        /** Short meaning per language code (BCP-47). Older assets only have {@link #definition_vi}. */
        public Map<String, String> translations;
        /** Any number of examples. Older assets only have {@link #example} / {@link #example_vi}. */
        public List<ExampleSeed> examples;
    }

    public static class ExampleSeed {
        public String text;
        /** Translation per language code. */
        public Map<String, String> translations;
    }
}
