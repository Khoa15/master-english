package com.masterenglish.app.data.local.seed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import com.masterenglish.app.data.local.Enums.NodeType;
import com.masterenglish.app.data.local.Enums.QuestionType;
import com.masterenglish.app.data.local.entity.LessonQuestionEntity;
import com.masterenglish.app.data.local.entity.RoadmapNodeEntity;
import com.masterenglish.app.data.local.seed.SeedModels.GrammarSeed;
import com.masterenglish.app.data.local.seed.SeedModels.ListeningSeed;
import com.masterenglish.app.data.local.seed.SeedModels.ReadingSeed;
import com.masterenglish.app.data.local.seed.SeedModels.VocabularySeed;
import com.masterenglish.app.utils.TextUtils;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Runs the real master JSON through the builder and checks the invariants the app relies on. */
public class ContentBuilderTest {

    private static ContentBuilder.Bundle bundle;

    @BeforeClass
    public static void build() throws IOException {
        Gson gson = new Gson();
        List<VocabularySeed> vocabulary = read(gson, "vocabulary.json", new TypeToken<List<VocabularySeed>>() { });
        List<GrammarSeed> grammar = read(gson, "grammar.json", new TypeToken<List<GrammarSeed>>() { });
        List<ReadingSeed> reading = read(gson, "reading.json", new TypeToken<List<ReadingSeed>>() { });
        List<ListeningSeed> listening = read(gson, "listening.json", new TypeToken<List<ListeningSeed>>() { });
        Map<String, Integer> durations = new HashMap<>();
        for (ListeningSeed item : listening) {
            durations.put(item.id, 30);
        }
        bundle = ContentBuilder.build(vocabulary, grammar, reading, listening, durations);
    }

    private static <T> T read(Gson gson, String name, TypeToken<T> type) throws IOException {
        File file = new File("src/main/assets/data/" + name);
        assertTrue("missing " + file.getAbsolutePath(), file.exists());
        try (Reader reader = new FileReader(file, java.nio.charset.StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, type.getType());
        }
    }

    @Test
    public void everySourceRowBecomesAContentRow() {
        assertEquals(120, bundle.vocabulary.size());
        assertEquals(36, bundle.grammar.size());
        assertEquals(18, bundle.reading.size());
        assertEquals(18, bundle.listening.size());
    }

    @Test
    public void roadmapHasOneStagePerLevelAndOneBossPerStage() {
        assertEquals(ContentBuilder.LEVELS.length, bundle.stages.size());
        int bosses = 0;
        for (RoadmapNodeEntity node : bundle.nodes) {
            if (node.nodeType == NodeType.CHECKPOINT_BOSS) {
                bosses++;
            }
        }
        assertEquals(ContentBuilder.LEVELS.length, bosses);
    }

    @Test
    public void nodeIdsAndPositionsAreUniqueAndContiguous() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < bundle.nodes.size(); i++) {
            RoadmapNodeEntity node = bundle.nodes.get(i);
            assertTrue("duplicate node id " + node.id, ids.add(node.id));
            assertEquals(i, node.positionIndex);
        }
    }

    @Test
    public void everyNonBossNodeOwnsItsContent() {
        Set<String> nodeIds = new HashSet<>();
        for (RoadmapNodeEntity node : bundle.nodes) {
            nodeIds.add(node.id);
        }
        for (com.masterenglish.app.data.local.entity.VocabularyEntity v : bundle.vocabulary) {
            assertTrue(nodeIds.contains(v.nodeId));
        }
        for (com.masterenglish.app.data.local.entity.GrammarEntity g : bundle.grammar) {
            assertTrue(nodeIds.contains(g.nodeId));
        }
        for (com.masterenglish.app.data.local.entity.ReadingEntity r : bundle.reading) {
            assertTrue(nodeIds.contains(r.nodeId));
        }
        for (com.masterenglish.app.data.local.entity.ListeningEntity l : bundle.listening) {
            assertTrue(nodeIds.contains(l.nodeId));
        }
    }

    @Test
    public void questionIdsAreUnique() {
        Set<String> ids = new HashSet<>();
        for (LessonQuestionEntity q : bundle.questions) {
            assertTrue("duplicate question id " + q.id, ids.add(q.id));
        }
    }

    @Test
    public void allFiveQuestionTypesAreGenerated() {
        Set<QuestionType> types = new HashSet<>();
        for (LessonQuestionEntity q : bundle.questions) {
            types.add(q.questionType);
        }
        assertEquals(new HashSet<>(java.util.Arrays.asList(QuestionType.values())), types);
    }

    @Test
    public void multipleChoiceAnswersAreAmongTheOptions() {
        for (LessonQuestionEntity q : bundle.questions) {
            if (q.questionType != QuestionType.MULTIPLE_CHOICE) {
                continue;
            }
            List<String> options = new ArrayList<>();
            for (com.google.gson.JsonElement e : q.options.getAsJsonArray()) {
                options.add(e.getAsString());
            }
            assertTrue("answer not in options for " + q.id,
                    options.contains(q.correctAnswer.getAsString()));
            assertEquals("duplicate options for " + q.id, options.size(), new HashSet<>(options).size());
        }
    }

    @Test
    public void reorderQuestionsCanBeRebuiltIntoTheSentence() {
        int checked = 0;
        for (LessonQuestionEntity q : bundle.questions) {
            if (q.questionType != QuestionType.REORDER) {
                continue;
            }
            List<String> tokens = new ArrayList<>();
            for (com.google.gson.JsonElement e : q.options.getAsJsonArray()) {
                tokens.add(e.getAsString());
            }
            List<String> target = TextUtils.tokenize(q.correctAnswer.getAsString());
            List<String> a = new ArrayList<>(tokens);
            List<String> b = new ArrayList<>(target);
            Collections.sort(a);
            Collections.sort(b);
            assertEquals("tokens differ for " + q.id, b, a);
            assertFalse("bank already solved for " + q.id, tokens.equals(target));
            checked++;
        }
        assertTrue(checked > 50);
    }

    @Test
    public void matchingQuestionsPairEachTermWithItsMeaning() {
        for (LessonQuestionEntity q : bundle.questions) {
            if (q.questionType != QuestionType.MATCHING) {
                continue;
            }
            JsonArray pairs = q.options.getAsJsonArray();
            assertTrue(pairs.size() >= 2 && pairs.size() <= 4);
            assertEquals(pairs.toString(), q.correctAnswer.toString());
        }
    }

    @Test
    public void listeningAudioPathsPointAtBundledFiles() {
        for (com.masterenglish.app.data.local.entity.ListeningEntity l : bundle.listening) {
            assertTrue(l.audioUrl, l.audioUrl.matches("raw/listen_\\d{3}"));
            assertNotNull(l.transcript);
        }
    }

    @Test
    public void nodeTitlesAreNeverBlank() {
        for (RoadmapNodeEntity node : bundle.nodes) {
            assertFalse(node.title.trim().isEmpty());
            assertFalse(node.iconResName.isEmpty());
        }
    }
}
