package com.masterenglish.app.ui.lesson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.masterenglish.app.data.local.Enums.ParentType;
import com.masterenglish.app.data.local.Enums.QuestionType;
import com.masterenglish.app.data.local.entity.LessonQuestionEntity;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class QuestionModelsTest {

    private static LessonQuestionEntity question(QuestionType type, String correct) {
        LessonQuestionEntity q = new LessonQuestionEntity();
        q.id = "q";
        q.parentType = ParentType.VOCABULARY;
        q.parentId = "vocab_001";
        q.questionType = type;
        q.prompt = "prompt";
        q.options = new JsonArray();
        q.correctAnswer = new JsonPrimitive(correct);
        return q;
    }

    @Test
    public void multipleChoiceComparesNormalisedText() {
        LessonQuestionEntity q = question(QuestionType.MULTIPLE_CHOICE, "B) Web developer");
        assertTrue(QuestionModels.isCorrect(q, "B) Web developer"));
        assertFalse(QuestionModels.isCorrect(q, "A) Teacher"));
    }

    @Test
    public void voiceInputAllowsSmallRecognitionErrors() {
        LessonQuestionEntity q = question(QuestionType.VOICE_INPUT, "I am a web developer.");
        assertTrue(QuestionModels.isCorrect(q, "i am a web developer"));
        assertTrue(QuestionModels.isCorrect(q, "I am a web develope"));
        assertFalse(QuestionModels.isCorrect(q, "I am a doctor"));
        assertFalse(QuestionModels.isCorrect(q, "   "));
    }

    @Test
    public void levenshteinMatchesKnownDistances() {
        assertEquals(0, QuestionModels.levenshtein("abc", "abc"));
        assertEquals(3, QuestionModels.levenshtein("kitten", "sitting"));
        assertEquals(3, QuestionModels.levenshtein("", "abc"));
    }

    @Test
    public void matchingNeedsEveryPairToBeCorrect() {
        LessonQuestionEntity q = question(QuestionType.MATCHING, "");
        JsonArray pairs = new JsonArray();
        com.google.gson.JsonObject a = new com.google.gson.JsonObject();
        a.addProperty("left", "job");
        a.addProperty("right", "Công việc");
        com.google.gson.JsonObject b = new com.google.gson.JsonObject();
        b.addProperty("left", "work");
        b.addProperty("right", "Làm việc");
        pairs.add(a);
        pairs.add(b);
        q.options = pairs;
        q.correctAnswer = pairs;

        Map<String, String> good = new HashMap<>();
        good.put("job", "Công việc");
        good.put("work", "Làm việc");
        assertTrue(QuestionModels.isCorrect(q, good));

        Map<String, String> swapped = new HashMap<>();
        swapped.put("job", "Làm việc");
        swapped.put("work", "Công việc");
        assertFalse(QuestionModels.isCorrect(q, swapped));
    }
}
