// package com.doradodo.app.ui.entertainment;
//
// import android.content.Context;
// import android.os.Bundle;
// import android.view.LayoutInflater;
// import android.view.View;
// import android.view.ViewGroup;
//
// import androidx.annotation.NonNull;
// import androidx.annotation.Nullable;
//
// import com.doradodo.app.R;
// import com.doradodo.app.data.repository.GameWordRepository;
// import com.doradodo.app.ui.ds.Ui;
// import com.google.gson.Gson;
//
// import java.io.IOException;
// import java.io.InputStreamReader;
// import java.nio.charset.StandardCharsets;
// import java.util.ArrayList;
// import java.util.Arrays;
// import java.util.Collections;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
//
// import dagger.hilt.android.AndroidEntryPoint;
//
// /**
//  * "Ngữ cảnh": a situation is shown and the learner picks the word that fits it.
//  * The learning data (the words and their choices) is in assets/data/contexts.en.json. The situation and its
//  * explanation are in assets/data/context_texts.&lt;display language&gt;.json, so the screen follows the language
//  * the app is shown in; a missing text falls back to English.
//  */
// @AndroidEntryPoint
// public class ContextFragment extends QuizFragment {
//
//     private static final String LEARNING_ASSET = "data/contexts.en.json";
//     private static final String TEXT_ASSET = "data/context_texts.%s.json";
//     private static final String DEFAULT_LANG = "en";
//     private static final int QUESTIONS = 8;
//
//     /** Set before the questions load in the background, so the loader never needs the fragment. */
//     private Context appContext;
//
//     @Nullable
//     @Override
//     public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
//                              @Nullable Bundle savedInstanceState) {
//         appContext = requireContext().getApplicationContext();
//         return super.onCreateView(inflater, container, savedInstanceState);
//     }
//
//     @Override
//     protected Config config() {
//         Config c = new Config();
//         c.title = getString(R.string.ent_context_title);
//         c.scoreLabelRes = R.string.ent_label_correct;
//         c.heroLabel = getString(R.string.ent_context_situation);
//         c.optionsLabel = getString(R.string.ent_context_pick);
//         c.correctMsg = getString(R.string.ent_correct);
//         c.finishedPrompt = getString(R.string.ent_done);
//         c.restartLabel = getString(R.string.ent_play_again);
//         c.bigPrompt = false;
//         c.progressColor = Ui.GREEN;
//         c.heroBg = Ui.GREEN_TINT;
//         c.heroBorder = GameUi.GREEN_BORDER;
//         c.heroInk = GameUi.GREEN_EDGE;
//         c.points = 10;
//         c.emptyTitle = getString(R.string.ent_context_empty);
//         c.emptyMessage = getString(R.string.ent_context_empty_msg);
//         return c;
//     }
//
//     @Override
//     protected Q[] buildQuestions(GameWordRepository repo) {
//         List<Item> items = readJson(LEARNING_ASSET, ContextFile.class).items;
//         Map<String, Text> english = textsFor(DEFAULT_LANG);
//         String lang = repo.displayLanguage();
//         Map<String, Text> shown = DEFAULT_LANG.equals(lang) ? english : textsFor(lang);
//
//         Collections.shuffle(items);
//         List<Q> out = new ArrayList<>();
//         for (Item item : items) {
//             if (out.size() >= QUESTIONS) {
//                 break;
//             }
//             Text text = shown.containsKey(item.id) ? shown.get(item.id) : english.get(item.id);
//             if (text == null || text.scenario == null) {
//                 continue;
//             }
//             List<String> options = new ArrayList<>(Arrays.asList(item.options));
//             Collections.shuffle(options);
//             int ans = options.indexOf(item.answer);
//             if (ans >= 0 && options.size() >= 2) {
//                 out.add(new Q(text.scenario, options.toArray(new String[0]), ans, text.explain));
//             }
//         }
//         return out.toArray(new Q[0]);
//     }
//
//     /** The situations and explanations of one display language; empty when that language has no file. */
//     private Map<String, Text> textsFor(String lang) {
//         Map<String, Text> map = new HashMap<>();
//         TextFile file = readJson(String.format(TEXT_ASSET, lang), TextFile.class);
//         for (Text t : file.items) {
//             if (t.id != null) {
//                 map.put(t.id, t);
//             }
//         }
//         return map;
//     }
//
//     /** Reads a bundled JSON file; a missing or unreadable file gives an empty one, so the screen shows its empty state. */
//     private <T> T readJson(String asset, Class<T> type) {
//         try (InputStreamReader reader = new InputStreamReader(appContext.getAssets().open(asset), StandardCharsets.UTF_8)) {
//             T parsed = new Gson().fromJson(reader, type);
//             if (parsed != null) {
//                 return parsed;
//             }
//         } catch (IOException | RuntimeException e) {
//             // Fall through to the empty value below.
//         }
//         try {
//             return type.getDeclaredConstructor().newInstance();
//         } catch (ReflectiveOperationException e) {
//             throw new IllegalStateException(e);
//         }
//     }
//
//     static final class ContextFile {
//         List<Item> items = new ArrayList<>();
//     }
//
//     static final class Item {
//         String id;
//         String term;
//         String[] options = new String[0];
//         String answer;
//     }
//
//     static final class TextFile {
//         List<Text> items = new ArrayList<>();
//     }
//
//     static final class Text {
//         String id;
//         String scenario;
//         String explain;
//     }
// }
//