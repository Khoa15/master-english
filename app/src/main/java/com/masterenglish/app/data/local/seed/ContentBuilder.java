package com.masterenglish.app.data.local.seed;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.masterenglish.app.data.local.Enums.BadgeCategory;
import com.masterenglish.app.data.local.Enums.NodeType;
import com.masterenglish.app.data.local.Enums.ParentType;
import com.masterenglish.app.data.local.Enums.QuestionType;
import com.masterenglish.app.data.local.entity.BadgeEntity;
import com.masterenglish.app.data.local.entity.GrammarEntity;
import com.masterenglish.app.data.local.entity.LessonQuestionEntity;
import com.masterenglish.app.data.local.entity.ListeningEntity;
import com.masterenglish.app.data.local.entity.ReadingEntity;
import com.masterenglish.app.data.local.entity.RoadmapNodeEntity;
import com.masterenglish.app.data.local.entity.RoadmapStageEntity;
import com.masterenglish.app.data.local.entity.VocabularyEntity;
import com.masterenglish.app.data.local.seed.SeedModels.GrammarSeed;
import com.masterenglish.app.data.local.seed.SeedModels.ListeningSeed;
import com.masterenglish.app.data.local.seed.SeedModels.QuestionSeed;
import com.masterenglish.app.data.local.seed.SeedModels.ReadingSeed;
import com.masterenglish.app.data.local.seed.SeedModels.VocabularySeed;
import com.masterenglish.app.utils.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Turns the four master JSON files into the roadmap and the lesson questions.
 *
 * <p>Layout: one stage per CEFR level (A1..C2). Each stage interleaves vocabulary
 * groups (5 words), grammar topics, listening and reading units, then ends with a
 * CHECKPOINT_BOSS node. Output is deterministic: the same JSON always yields the
 * same roadmap and question set, which keeps server sync stable.
 */
public final class ContentBuilder {

    static final String[] LEVELS = {"A1", "A2", "B1", "B2", "C1", "C2"};

    private static final String[] STAGE_DESCRIPTIONS = {
            "Giới thiệu bản thân và công việc IT",
            "Giao tiếp hằng ngày ở nơi làm việc",
            "Mô tả quy trình và kinh nghiệm làm việc",
            "Phỏng vấn và thảo luận kỹ thuật",
            "Viết tài liệu và phân tích học thuật",
            "Thảo luận kiến trúc hệ thống ở cấp chuyên gia"
    };

    private static final int VOCAB_PER_NODE = 5;
    private static final int MATCHING_PAIRS = 4;

    /** Container for every row produced by one build. */
    public static final class Bundle {
        public final List<RoadmapStageEntity> stages = new ArrayList<>();
        public final List<RoadmapNodeEntity> nodes = new ArrayList<>();
        public final List<VocabularyEntity> vocabulary = new ArrayList<>();
        public final List<GrammarEntity> grammar = new ArrayList<>();
        public final List<ReadingEntity> reading = new ArrayList<>();
        public final List<ListeningEntity> listening = new ArrayList<>();
        public final List<LessonQuestionEntity> questions = new ArrayList<>();
        public final List<BadgeEntity> badges = new ArrayList<>();
    }

    private final List<VocabularySeed> vocabularySeeds;
    private final List<GrammarSeed> grammarSeeds;
    private final List<ReadingSeed> readingSeeds;
    private final List<ListeningSeed> listeningSeeds;
    private final Map<String, Integer> listeningDurations;
    private final Gson gson = new Gson();
    private final Bundle out = new Bundle();

    private final List<String> meaningPool = new ArrayList<>();
    private final List<String> termPool = new ArrayList<>();
    private final List<String> incorrectSentencePool = new ArrayList<>();
    private int positionCounter;

    private ContentBuilder(List<VocabularySeed> vocabularySeeds, List<GrammarSeed> grammarSeeds,
                           List<ReadingSeed> readingSeeds, List<ListeningSeed> listeningSeeds,
                           Map<String, Integer> listeningDurations) {
        this.vocabularySeeds = vocabularySeeds;
        this.grammarSeeds = grammarSeeds;
        this.readingSeeds = readingSeeds;
        this.listeningSeeds = listeningSeeds;
        this.listeningDurations = listeningDurations;
    }

    /**
     * @param listeningDurations duration in seconds per listening id, measured from the bundled audio
     */
    public static Bundle build(List<VocabularySeed> vocabulary, List<GrammarSeed> grammar,
                               List<ReadingSeed> reading, List<ListeningSeed> listening,
                               Map<String, Integer> listeningDurations) {
        ContentBuilder builder = new ContentBuilder(vocabulary, grammar, reading, listening, listeningDurations);
        return builder.run();
    }

    private Bundle run() {
        for (VocabularySeed v : vocabularySeeds) {
            meaningPool.add(v.definition_vi);
            termPool.add(v.term);
        }
        for (GrammarSeed g : grammarSeeds) {
            String incorrect = stringField(g.common_mistakes, "incorrect");
            if (incorrect != null) {
                incorrectSentencePool.add(incorrect);
            }
        }
        for (int s = 0; s < LEVELS.length; s++) {
            buildStage(s, LEVELS[s]);
        }
        buildBadges();
        return out;
    }

    // ----- Stages and nodes ----------------------------------------------

    private void buildStage(int index, String level) {
        int stageId = index + 1;
        RoadmapStageEntity stage = new RoadmapStageEntity();
        stage.id = stageId;
        stage.stageNumber = stageId;
        stage.title = "Giai đoạn " + stageId + " · " + level;
        stage.description = STAGE_DESCRIPTIONS[index];
        stage.minExpRequired = index * 400;
        out.stages.add(stage);

        List<RoadmapNodeEntity> vocabNodes = buildVocabularyNodes(stageId, level);
        List<RoadmapNodeEntity> grammarNodes = buildGrammarNodes(stageId, level);
        List<RoadmapNodeEntity> listeningNodes = buildListeningNodes(stageId, level);
        List<RoadmapNodeEntity> readingNodes = buildReadingNodes(stageId, level);

        List<RoadmapNodeEntity> ordered = interleave(vocabNodes, grammarNodes, listeningNodes, readingNodes);
        RoadmapNodeEntity boss = newNode(stageId, level + "_B01", "Boss " + level,
                "Thử thách tổng hợp: câu hỏi lấy từ mọi bài trong giai đoạn " + level,
                NodeType.CHECKPOINT_BOSS, "ic_skill_boss");
        ordered.add(boss);
        for (RoadmapNodeEntity node : ordered) {
            node.positionIndex = positionCounter++;
            out.nodes.add(node);
        }
    }

    private List<RoadmapNodeEntity> buildVocabularyNodes(int stageId, String level) {
        List<VocabularySeed> levelVocab = filterByLevel(vocabularySeeds, level, v -> v.cefr_level);
        List<RoadmapNodeEntity> nodes = new ArrayList<>();
        for (int start = 0, group = 1; start < levelVocab.size(); start += VOCAB_PER_NODE, group++) {
            List<VocabularySeed> words = levelVocab.subList(start, Math.min(levelVocab.size(), start + VOCAB_PER_NODE));
            List<String> terms = new ArrayList<>();
            for (VocabularySeed v : words) {
                terms.add(v.term);
            }
            String nodeId = level + "_V" + pad(group);
            RoadmapNodeEntity node = newNode(stageId, nodeId, "Từ vựng " + level + " · " + group,
                    "Học " + words.size() + " từ: " + String.join(", ", terms),
                    NodeType.VOCABULARY, "ic_skill_vocab");
            nodes.add(node);
            for (VocabularySeed v : words) {
                out.vocabulary.add(toVocabulary(v, nodeId));
            }
            out.questions.addAll(vocabularyQuestions(words, nodeId));
        }
        return nodes;
    }

    private List<RoadmapNodeEntity> buildGrammarNodes(int stageId, String level) {
        List<RoadmapNodeEntity> nodes = new ArrayList<>();
        int index = 0;
        for (GrammarSeed g : filterByLevel(grammarSeeds, level, item -> item.cefr_level)) {
            String nodeId = level + "_G" + pad(++index);
            nodes.add(newNode(stageId, nodeId, g.topic, safe(g.explanation_vi),
                    NodeType.GRAMMAR, "ic_skill_grammar"));
            out.grammar.add(toGrammar(g, nodeId));
            out.questions.addAll(grammarQuestions(g, nodeId));
        }
        return nodes;
    }

    private List<RoadmapNodeEntity> buildListeningNodes(int stageId, String level) {
        List<RoadmapNodeEntity> nodes = new ArrayList<>();
        int index = 0;
        for (ListeningSeed l : filterByLevel(listeningSeeds, level, item -> item.cefr_level)) {
            String nodeId = level + "_L" + pad(++index);
            nodes.add(newNode(stageId, nodeId, l.title, safe(l.context),
                    NodeType.LISTENING, "ic_skill_listening"));
            out.listening.add(toListening(l, nodeId));
            out.questions.addAll(seedQuestions(l.questions, ParentType.LISTENING, l.id,
                    audioPathFor(l.id), "comprehension_" + TextUtils.slug(l.title)));
        }
        return nodes;
    }

    private List<RoadmapNodeEntity> buildReadingNodes(int stageId, String level) {
        List<RoadmapNodeEntity> nodes = new ArrayList<>();
        int index = 0;
        for (ReadingSeed r : filterByLevel(readingSeeds, level, item -> item.cefr_level)) {
            String nodeId = level + "_R" + pad(++index);
            nodes.add(newNode(stageId, nodeId, r.title,
                    "Đọc đoạn văn và trả lời " + size(r.questions) + " câu hỏi",
                    NodeType.READING, "ic_skill_reading"));
            out.reading.add(toReading(r, nodeId));
            out.questions.addAll(seedQuestions(r.questions, ParentType.READING, r.id, null,
                    "comprehension_" + TextUtils.slug(r.title)));
        }
        return nodes;
    }

    @SafeVarargs
    private static List<RoadmapNodeEntity> interleave(List<RoadmapNodeEntity>... lanes) {
        List<RoadmapNodeEntity> result = new ArrayList<>();
        boolean added = true;
        int cursor = 0;
        while (added) {
            added = false;
            for (List<RoadmapNodeEntity> lane : lanes) {
                if (cursor < lane.size()) {
                    result.add(lane.get(cursor));
                    added = true;
                }
            }
            cursor++;
        }
        return result;
    }

    private static RoadmapNodeEntity newNode(int stageId, String id, String title, String summary,
                                             NodeType type, String icon) {
        RoadmapNodeEntity node = new RoadmapNodeEntity();
        node.id = id;
        node.stageId = stageId;
        node.title = title;
        node.summary = summary;
        node.nodeType = type;
        node.iconResName = icon;
        return node;
    }

    // ----- Content rows --------------------------------------------------

    private static VocabularyEntity toVocabulary(VocabularySeed v, String nodeId) {
        VocabularyEntity e = new VocabularyEntity();
        e.id = v.id;
        e.nodeId = nodeId;
        e.word = v.term;
        e.phonetic = v.ipa;
        e.partOfSpeech = v.part_of_speech;
        e.meaningVi = v.definition_vi;
        e.exampleSentence = v.example_en;
        e.exampleTranslation = v.example_vi;
        e.cefrLevel = v.cefr_level;
        e.domain = v.domain;
        e.synonyms = v.synonyms == null ? new ArrayList<>() : v.synonyms;
        e.antonyms = v.antonyms == null ? new ArrayList<>() : v.antonyms;
        return e;
    }

    private GrammarEntity toGrammar(GrammarSeed g, String nodeId) {
        GrammarEntity e = new GrammarEntity();
        e.id = g.id;
        e.nodeId = nodeId;
        e.topicName = g.topic;
        String usage = safe(g.usage_context);
        e.ruleExplanationMarkdown = safe(g.explanation_vi) + (usage.isEmpty() ? "" : "\n\n" + usage);
        e.formula = g.formula;
        e.commonMistakes = g.common_mistakes;
        e.examples = g.examples;
        e.cefrLevel = g.cefr_level;
        e.domain = g.domain;
        return e;
    }

    private ListeningEntity toListening(ListeningSeed l, String nodeId) {
        ListeningEntity e = new ListeningEntity();
        e.id = l.id;
        e.nodeId = nodeId;
        e.title = l.title;
        e.context = l.context;
        e.audioUrl = audioPathFor(l.id);
        e.transcript = gson.toJsonTree(l.transcript == null ? new ArrayList<>() : l.transcript);
        Integer seconds = listeningDurations.get(l.id);
        e.durationSeconds = seconds == null ? 0 : seconds;
        e.cefrLevel = l.cefr_level;
        e.domain = l.domain;
        return e;
    }

    private ReadingEntity toReading(ReadingSeed r, String nodeId) {
        ReadingEntity e = new ReadingEntity();
        e.id = r.id;
        e.nodeId = nodeId;
        e.title = r.title;
        e.passageText = safe(r.passage);
        e.wordCount = TextUtils.tokenize(e.passageText).size();
        e.keyVocabulary = r.key_vocabulary == null ? new ArrayList<>() : r.key_vocabulary;
        e.cefrLevel = r.cefr_level;
        e.domain = r.domain;
        return e;
    }

    // ----- Questions -----------------------------------------------------

    private List<LessonQuestionEntity> vocabularyQuestions(List<VocabularySeed> words, String nodeId) {
        List<LessonQuestionEntity> questions = new ArrayList<>();
        for (VocabularySeed v : words) {
            Random rnd = new Random(v.id.hashCode());
            String tag = "vocab_" + TextUtils.slug(v.term);
            String meaning = v.definition_vi;

            List<String> choices = new ArrayList<>();
            choices.add(meaning);
            choices.addAll(pick(meaningPool, setOf(meaning), 3, rnd));
            Collections.shuffle(choices, rnd);
            questions.add(question(ParentType.VOCABULARY, v.id, "mc", QuestionType.MULTIPLE_CHOICE,
                    "Nghĩa của từ \"" + v.term + "\" là gì?", null,
                    stringArray(choices), new JsonPrimitive(meaning),
                    v.term + " (" + safe(v.ipa) + ") nghĩa là: " + meaning, 10, tag));

            String example = safe(v.example_en);
            if (example.isEmpty()) {
                continue;
            }
            if (TextUtils.containsWord(example, v.term)) {
                List<String> bank = new ArrayList<>();
                bank.add(v.term);
                bank.addAll(pick(termPool, setOf(v.term), 2, rnd));
                Collections.shuffle(bank, rnd);
                questions.add(question(ParentType.VOCABULARY, v.id, "fill", QuestionType.FILL_BLANK,
                        TextUtils.blankOut(example, v.term, "_____") + "\nGợi ý: " + meaning,
                        null, stringArray(bank), new JsonPrimitive(v.term),
                        "Đáp án là \"" + v.term + "\": " + example, 10, tag));
            }

            List<String> tokens = TextUtils.tokenize(example);
            if (tokens.size() >= 3) {
                List<String> shuffled = new ArrayList<>(tokens);
                Collections.shuffle(shuffled, rnd);
                if (shuffled.equals(tokens)) {
                    Collections.reverse(shuffled);
                }
                questions.add(question(ParentType.VOCABULARY, v.id, "reorder", QuestionType.REORDER,
                        "Sắp xếp các từ thành câu đúng.\nNghĩa: " + safe(v.example_vi), null,
                        stringArray(shuffled), new JsonPrimitive(TextUtils.joinWords(tokens)),
                        example, 15, tag));
            }

            questions.add(question(ParentType.VOCABULARY, v.id, "voice", QuestionType.VOICE_INPUT,
                    "Đọc to câu sau bằng tiếng Anh", null,
                    new JsonArray(), new JsonPrimitive(example),
                    safe(v.example_vi), 15, tag));
        }

        if (words.size() >= 2) {
            int pairs = Math.min(MATCHING_PAIRS, words.size());
            JsonArray pairArray = new JsonArray();
            for (VocabularySeed v : words.subList(0, pairs)) {
                JsonObject pair = new JsonObject();
                pair.addProperty("left", v.term);
                pair.addProperty("right", v.definition_vi);
                pairArray.add(pair);
            }
            questions.add(question(ParentType.VOCABULARY, nodeId, "matching", QuestionType.MATCHING,
                    "Nối mỗi từ với nghĩa tiếng Việt đúng", null,
                    pairArray, pairArray.deepCopy(), "Ghép đúng từ và nghĩa để ghi nhớ.", 20,
                    "vocab_matching"));
        }
        return questions;
    }

    private List<LessonQuestionEntity> grammarQuestions(GrammarSeed g, String nodeId) {
        List<LessonQuestionEntity> questions = new ArrayList<>();
        String tag = "grammar_" + TextUtils.slug(g.topic);
        String correct = stringField(g.common_mistakes, "correct");
        String incorrect = stringField(g.common_mistakes, "incorrect");
        String reason = stringField(g.common_mistakes, "reason_vi");
        Random rnd = new Random(g.id.hashCode());

        if (correct != null && incorrect != null) {
            List<String> choices = new ArrayList<>();
            choices.add(correct);
            choices.add(incorrect);
            choices.addAll(pick(incorrectSentencePool, setOf(correct, incorrect), 2, rnd));
            Collections.shuffle(choices, rnd);
            questions.add(question(ParentType.GRAMMAR, g.id, "mc", QuestionType.MULTIPLE_CHOICE,
                    "Câu nào đúng ngữ pháp? (" + g.topic + ")", null,
                    stringArray(choices), new JsonPrimitive(correct),
                    safe(reason) + " Câu đúng: " + correct, 10, tag));
        }

        String example = firstExampleEn(g.examples);
        if (example != null) {
            List<String> tokens = TextUtils.tokenize(example);
            if (tokens.size() >= 3) {
                List<String> shuffled = new ArrayList<>(tokens);
                Collections.shuffle(shuffled, rnd);
                if (shuffled.equals(tokens)) {
                    Collections.reverse(shuffled);
                }
                questions.add(question(ParentType.GRAMMAR, g.id, "reorder", QuestionType.REORDER,
                        "Sắp xếp để tạo câu đúng theo mẫu: " + safe(g.formula), null,
                        stringArray(shuffled), new JsonPrimitive(TextUtils.joinWords(tokens)),
                        "Câu mẫu: " + example, 15, tag));
            }
        }
        return questions;
    }

    /** Multiple-choice questions shipped with reading/listening: the answer letter is mapped to its option text. */
    private List<LessonQuestionEntity> seedQuestions(List<QuestionSeed> seeds, ParentType parentType,
                                                     String parentId, String mediaUrl, String tag) {
        List<LessonQuestionEntity> questions = new ArrayList<>();
        if (seeds == null) {
            return questions;
        }
        for (QuestionSeed q : seeds) {
            List<String> options = q.options == null ? new ArrayList<>() : q.options;
            String answerText = answerText(options, q.answer);
            questions.add(question(parentType, parentId, q.question_id, QuestionType.MULTIPLE_CHOICE,
                    q.question, mediaUrl, stringArray(options), new JsonPrimitive(answerText),
                    safe(q.explanation_vi), 10, tag));
        }
        return questions;
    }

    private static String answerText(List<String> options, String letter) {
        if (letter == null) {
            return "";
        }
        for (String option : options) {
            if (option.trim().startsWith(letter.trim() + ")")) {
                return option;
            }
        }
        return letter;
    }

    private static LessonQuestionEntity question(ParentType parentType, String parentId, String suffix,
                                                 QuestionType type, String prompt, String mediaUrl,
                                                 JsonArray options, JsonElement correct,
                                                 String explanation, int points, String skillTag) {
        LessonQuestionEntity q = new LessonQuestionEntity();
        q.id = "q_" + parentId + "_" + TextUtils.slug(suffix);
        q.parentType = parentType;
        q.parentId = parentId;
        q.questionType = type;
        q.prompt = prompt;
        q.mediaUrl = mediaUrl;
        q.options = options;
        q.correctAnswer = correct;
        q.explanation = explanation;
        q.points = points;
        q.skillTag = skillTag;
        return q;
    }

    // ----- Gamification catalogue ----------------------------------------

    private void buildBadges() {
        out.badges.add(badge("badge_streak_30", "Chiến thần 30 ngày Streak",
                "Duy trì chuỗi học 30 ngày liên tiếp.", "ic_badge_streak",
                "STREAK", "{\"type\":\"STREAK\",\"days\":30}"));
        out.badges.add(badge("badge_vocab_master", "Bậc thầy từ vựng",
                "Ghi nhớ 50 từ vựng với SM-2 (đã ôn từ 3 lần trở lên).", "ic_badge_vocab",
                "VOCAB_MASTER", "{\"type\":\"VOCAB_MASTERED\",\"count\":50}"));
        out.badges.add(badge("badge_mistake_hunter", "Thợ săn lỗi sai",
                "Sửa đúng 10 lỗi mà AI Mascot đã chỉ ra.", "ic_badge_mistake",
                "AI_CHALLENGER", "{\"type\":\"CORRECTED_MISTAKES\",\"count\":10}"));
        out.badges.add(badge("badge_grammar_pro", "Chuyên gia ngữ pháp",
                "Hoàn thành 10 bài học ngữ pháp.", "ic_badge_grammar",
                "GRAMMAR_PRO", "{\"type\":\"GRAMMAR_LESSONS\",\"count\":10}"));
    }

    private static BadgeEntity badge(String id, String title, String description, String icon,
                                     String category, String criteriaJson) {
        BadgeEntity b = new BadgeEntity();
        b.id = id;
        b.title = title;
        b.description = description;
        b.iconUrl = icon;
        b.category = BadgeCategory.valueOf(category);
        b.criteria = JsonParser.parseString(criteriaJson);
        return b;
    }

    // ----- Helpers -------------------------------------------------------

    private interface LevelOf<T> {
        String level(T item);
    }

    private static <T> List<T> filterByLevel(List<T> items, String level, LevelOf<T> levelOf) {
        List<T> result = new ArrayList<>();
        if (items == null) {
            return result;
        }
        for (T item : items) {
            if (level.equalsIgnoreCase(levelOf.level(item))) {
                result.add(item);
            }
        }
        return result;
    }

    private static Set<String> setOf(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private static List<String> pick(List<String> pool, Set<String> exclude, int count, Random rnd) {
        List<String> copy = new ArrayList<>(pool);
        Collections.shuffle(copy, rnd);
        List<String> picked = new ArrayList<>();
        Set<String> seen = new HashSet<>(exclude);
        for (String candidate : copy) {
            if (picked.size() >= count) {
                break;
            }
            if (candidate != null && seen.add(candidate)) {
                picked.add(candidate);
            }
        }
        return picked;
    }

    private static JsonArray stringArray(List<String> values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            array.add(value);
        }
        return array;
    }

    private static String firstExampleEn(JsonElement examples) {
        if (examples == null || !examples.isJsonArray() || examples.getAsJsonArray().size() == 0) {
            return null;
        }
        return stringField(examples.getAsJsonArray().get(0), "en");
    }

    private static String stringField(JsonElement element, String key) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonElement value = element.getAsJsonObject().get(key);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    /** list_001 -> "raw/listen_001", the audio file produced by tools/generate_assets.py. */
    public static String audioPathFor(String listeningId) {
        return "raw/listen_" + listeningId.substring(listeningId.lastIndexOf('_') + 1);
    }

    private static String pad(int value) {
        return String.format(Locale.ROOT, "%02d", value);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static int size(List<?> list) {
        return list == null ? 0 : list.size();
    }
}
