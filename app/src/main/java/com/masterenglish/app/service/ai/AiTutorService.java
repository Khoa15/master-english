package com.masterenglish.app.service.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masterenglish.app.data.local.Enums.Sender;
import com.masterenglish.app.data.local.Enums.SkillType;
import com.masterenglish.app.data.local.entity.AiChatMessageEntity;
import com.masterenglish.app.data.repository.AiRepository;
import com.masterenglish.app.data.repository.ProgressRepository;
import com.masterenglish.app.utils.AppExecutors;
import com.masterenglish.app.utils.Callback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Drives the AI mascot. Two modes:
 * <ul>
 *   <li>Challenge engine: role-play prompts aimed at the learner's weak tags.</li>
 *   <li>Direct error catching: each reply is checked, tagged and written to user_weaknesses.</li>
 * </ul>
 * Without a Gemini key the service answers offline with a helpful notice instead.
 */
@Singleton
public class AiTutorService {

    private static final String CHALLENGE_REQUEST = "Start a short role-play challenge in a work setting (IT team, "
            + "interview or meeting). Target my weak points: %s. Ask me one question and wait for my answer.";
    private static final String NO_KEY_REPLY = "Mascot đang ngủ vì chưa có Gemini API key. "
            + "Thêm key trong Hồ sơ → Cài đặt để bật trò chuyện AI.";
    private static final String OFFLINE_REPLY = "Mình chưa kết nối được tới Gemini. Bạn thử lại sau nhé!";

    /** Result of one AI turn. */
    public static final class Evaluation {
        public final String reply;
        public final String correction;
        public final List<Mistake> mistakes;

        public Evaluation(String reply, String correction, List<Mistake> mistakes) {
            this.reply = reply;
            this.correction = correction == null ? "" : correction;
            this.mistakes = mistakes;
        }
    }

    public static final class Mistake {
        public final String tag;
        public final SkillType skill;
        public final String explanation;

        public Mistake(String tag, SkillType skill, String explanation) {
            this.tag = tag;
            this.skill = skill;
            this.explanation = explanation;
        }
    }

    private final ChatMemoryManager memoryManager;
    private final GeminiClient gemini;
    private final AiRepository aiRepository;
    private final ProgressRepository progressRepository;
    private final AppExecutors executors;

    @Inject
    public AiTutorService(ChatMemoryManager memoryManager, GeminiClient gemini, AiRepository aiRepository,
                          ProgressRepository progressRepository, AppExecutors executors) {
        this.memoryManager = memoryManager;
        this.gemini = gemini;
        this.aiRepository = aiRepository;
        this.progressRepository = progressRepository;
        this.executors = executors;
    }

    /** Opens a role-play challenge built from the learner's weak tags. */
    public void startChallenge(String userId, Callback<Evaluation> callback) {
        executors.diskIO().execute(() -> {
            String topic = joinOrDefault(memoryManager.weakTags(userId, 3), "general vocabulary");
            Evaluation result = runTurn(userId, String.format(CHALLENGE_REQUEST, topic), null);
            postToMain(callback, result);
        });
    }

    /** Sends the learner's message, checks it and records any mistakes. */
    public void sendMessage(String userId, String text, Callback<Evaluation> callback) {
        executors.diskIO().execute(() -> {
            Evaluation result = runTurn(userId, text, text);
            postToMain(callback, result);
        });
    }

    /** Offline daily tip based on the learner's weakest tag. Safe to call on any thread. */
    public void dailyTip(String userId, Callback<String> callback) {
        executors.diskIO().execute(() -> {
            List<String> tags = memoryManager.weakTags(userId, 1);
            String tip = tags.isEmpty()
                    ? "Hôm nay hãy hoàn thành một bài học để mở khóa điểm yếu đầu tiên của bạn!"
                    : "Hôm nay hãy ôn lại chủ đề: " + tags.get(0).replace('_', ' ')
                    + ". Mình sẽ đặt thử thách về nó ngay khi bạn sẵn sàng!";
            postToMain(callback, tip);
        });
    }

    private Evaluation runTurn(String userId, String userText, String learnerSentence) {
        if (!gemini.hasApiKey()) {
            return new Evaluation(NO_KEY_REPLY, "", new ArrayList<>());
        }
        String sessionId = aiRepository.ensureSessionNow(userId);
        if (learnerSentence != null) {
            aiRepository.saveMessageNow(message(sessionId, Sender.USER, learnerSentence, null));
        }
        try {
            String raw = gemini.generate(memoryManager.buildSystemPrompt(userId), userText);
            Evaluation evaluation = parse(raw);
            for (Mistake mistake : evaluation.mistakes) {
                progressRepository.recordMistake(userId, WeaknessDetector.normalizeTag(mistake.tag), mistake.skill);
            }
            aiRepository.saveMessageNow(message(sessionId, Sender.AI_MASCOT, evaluation.reply,
                    evaluation.correction));
            return evaluation;
        } catch (IOException e) {
            return new Evaluation(OFFLINE_REPLY, "", new ArrayList<>());
        }
    }

    /** Parses the JSON contract; falls back to plain text if the model ignored it. */
    static Evaluation parse(String raw) {
        String cleaned = raw.trim().replaceAll("^```(json)?", "").replaceAll("```$", "").trim();
        try {
            JsonElement element = JsonParser.parseString(cleaned);
            if (!element.isJsonObject()) {
                return new Evaluation(raw, "", new ArrayList<>());
            }
            JsonObject object = element.getAsJsonObject();
            String reply = stringOr(object, "reply", cleaned);
            String correction = stringOr(object, "correction", "");
            List<Mistake> mistakes = new ArrayList<>();
            if (object.has("mistakes") && object.get("mistakes").isJsonArray()) {
                JsonArray array = object.getAsJsonArray("mistakes");
                for (JsonElement item : array) {
                    if (!item.isJsonObject()) {
                        continue;
                    }
                    JsonObject m = item.getAsJsonObject();
                    String tag = stringOr(m, "tag", "");
                    if (!tag.isEmpty()) {
                        mistakes.add(new Mistake(tag,
                                WeaknessDetector.skillFrom(stringOr(m, "skill", null), SkillType.GRAMMAR),
                                stringOr(m, "explanation", "")));
                    }
                }
            }
            return new Evaluation(reply, correction, mistakes);
        } catch (RuntimeException e) {
            return new Evaluation(raw, "", new ArrayList<>());
        }
    }

    private static String stringOr(JsonObject object, String key, String fallback) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsString();
    }

    private static AiChatMessageEntity message(String sessionId, Sender sender, String text, String correction) {
        AiChatMessageEntity entity = new AiChatMessageEntity();
        entity.id = UUID.randomUUID().toString();
        entity.sessionId = sessionId;
        entity.sender = sender;
        entity.messageText = text;
        entity.correctionSuggestions = correction;
        entity.createdAt = System.currentTimeMillis();
        return entity;
    }

    private static String joinOrDefault(List<String> items, String fallback) {
        if (items.isEmpty()) {
            return fallback;
        }
        return String.join(", ", items).replace('_', ' ');
    }

    private <T> void postToMain(Callback<T> callback, T value) {
        executors.mainThread().execute(() -> callback.onResult(value));
    }
}
