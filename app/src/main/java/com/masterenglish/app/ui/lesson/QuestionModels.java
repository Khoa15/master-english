package com.masterenglish.app.ui.lesson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.masterenglish.app.data.local.Enums.QuestionType;
import com.masterenglish.app.data.local.entity.LessonQuestionEntity;
import com.masterenglish.app.utils.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Reads question JSON and decides whether an answer is correct. */
public final class QuestionModels {

    private QuestionModels() {
    }

    /** One pair of a MATCHING question. */
    public static final class MatchPair {
        public final String left;
        public final String right;

        public MatchPair(String left, String right) {
            this.left = left;
            this.right = right;
        }
    }

    public static List<String> stringOptions(LessonQuestionEntity question) {
        List<String> values = new ArrayList<>();
        if (question.options != null && question.options.isJsonArray()) {
            for (JsonElement element : question.options.getAsJsonArray()) {
                values.add(element.getAsString());
            }
        }
        return values;
    }

    public static List<MatchPair> matchPairs(LessonQuestionEntity question) {
        List<MatchPair> pairs = new ArrayList<>();
        JsonArray source = question.questionType == QuestionType.MATCHING && question.options != null
                && question.options.isJsonArray() ? question.options.getAsJsonArray() : new JsonArray();
        for (JsonElement element : source) {
            JsonObject pair = element.getAsJsonObject();
            pairs.add(new MatchPair(pair.get("left").getAsString(), pair.get("right").getAsString()));
        }
        return pairs;
    }

    /** The correct answer in the same shape the view produces: String, or Map for MATCHING. */
    public static Object correctAnswer(LessonQuestionEntity question) {
        if (question.questionType == QuestionType.MATCHING) {
            Map<String, String> map = new HashMap<>();
            for (MatchPair pair : matchPairs(question)) {
                map.put(pair.left, pair.right);
            }
            return map;
        }
        return question.correctAnswer == null ? "" : question.correctAnswer.getAsString();
    }

    public static boolean isCorrect(LessonQuestionEntity question, Object answer) {
        switch (question.questionType) {
            case MATCHING:
                return matchingCorrect(question, answer);
            case VOICE_INPUT:
                return voiceCorrect(question, String.valueOf(answer));
            case REORDER:
            case FILL_BLANK:
            case MULTIPLE_CHOICE:
            default:
                return TextUtils.normalizeAnswer(String.valueOf(answer))
                        .equals(TextUtils.normalizeAnswer(String.valueOf(correctAnswer(question))));
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean matchingCorrect(LessonQuestionEntity question, Object answer) {
        if (!(answer instanceof Map)) {
            return false;
        }
        Map<String, String> given = (Map<String, String>) answer;
        List<MatchPair> pairs = matchPairs(question);
        if (given.size() != pairs.size()) {
            return false;
        }
        for (MatchPair pair : pairs) {
            if (!pair.right.equals(given.get(pair.left))) {
                return false;
            }
        }
        return true;
    }

    /** Speech recognition is imperfect: accept small edits (about one wrong letter per 10 characters). */
    private static boolean voiceCorrect(LessonQuestionEntity question, String spoken) {
        String expected = TextUtils.normalizeAnswer(String.valueOf(correctAnswer(question)));
        String actual = TextUtils.normalizeAnswer(spoken);
        if (actual.isEmpty()) {
            return false;
        }
        int allowed = Math.max(1, expected.length() / 10);
        return levenshtein(expected, actual) <= allowed;
    }

    static int levenshtein(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }
}
