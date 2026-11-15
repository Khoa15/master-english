package com.masterenglish.app.data.local.seed;

import java.util.List;

/** Gson DTOs that mirror assets/data/vocabulary.json (built by tools/build_vocabulary.py). */
public final class SeedModels {

    private SeedModels() {
    }

    public static class VocabularyFile {
        public List<PackSeed> packs;
        public List<WordSeed> words;
    }

    public static class PackSeed {
        public String id;
        public String title;
        public String cefr_level;
        public int position;
        /** Ids of the words in this pack, in the order they are listed. */
        public List<String> words;
    }

    public static class WordSeed {
        public String id;
        public String term;
        public String ipa;
        public String cefr_level;
        public int rank;
        public List<SenseSeed> senses;
    }

    public static class SenseSeed {
        public String pos;
        public String definition;
    }
}
