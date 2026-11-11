package com.masterenglish.app.utils;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Pure string helpers shared by seeding, answer checking and error tagging. */
public final class TextUtils {

    private TextUtils() {
    }

    /** "Present Perfect for Experience" -> "present_perfect_for_experience". */
    public static String slug(String text) {
        String ascii = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return ascii.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    /** Lower-cases, strips punctuation and collapses whitespace so answers compare fairly. */
    public static String normalizeAnswer(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}' ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static List<String> tokenize(String sentence) {
        List<String> tokens = new ArrayList<>();
        for (String part : sentence.trim().split("\\s+")) {
            if (!part.isEmpty()) {
                tokens.add(part);
            }
        }
        return tokens;
    }

    public static String joinWords(List<String> words) {
        return String.join(" ", words);
    }

    /** Returns text with the first whole-word occurrence of {@code word} replaced by a blank. */
    public static String blankOut(String sentence, String word, String blank) {
        String pattern = "(?i)\\b" + java.util.regex.Pattern.quote(word) + "\\b";
        return sentence.replaceFirst(pattern, blank);
    }

    public static boolean containsWord(String sentence, String word) {
        return sentence.toLowerCase(Locale.ROOT).matches(
                ".*\\b" + java.util.regex.Pattern.quote(word.toLowerCase(Locale.ROOT)) + "\\b.*");
    }

    public static String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1);
    }

    public static List<String> asList(String... values) {
        return new ArrayList<>(Arrays.asList(values));
    }
}
