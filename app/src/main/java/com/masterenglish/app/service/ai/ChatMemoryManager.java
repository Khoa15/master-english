package com.masterenglish.app.service.ai;

import com.masterenglish.app.data.local.entity.AiPersonaMemoryEntity;
import com.masterenglish.app.data.local.entity.UserEntity;
import com.masterenglish.app.data.local.entity.UserSettingsEntity;
import com.masterenglish.app.data.local.entity.UserWeaknessEntity;
import com.masterenglish.app.data.repository.AiRepository;
import com.masterenglish.app.data.repository.ProgressRepository;
import com.masterenglish.app.data.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Long-term memory for the mascot. Builds the system prompt from the learner's
 * profile, streak, most frequent mistakes and stored persona memories.
 * Must run on a background thread (reads Room synchronously).
 */
@Singleton
public class ChatMemoryManager {

    private static final String ROLE_TEMPLATE =
            "User Profile: Name: %s, Current Streak: %d, Frequent Mistakes: {%s}. "
                    + "Your role: A witty, encouraging English mascot tutor. "
                    + "Challenge the user specifically on their weak grammar and vocabulary. "
                    + "If they make a mistake, gently point it out, explain the rule, and ask them to correct it.";

    private static final String OUTPUT_CONTRACT =
            " Always answer with ONE JSON object and nothing else: "
                    + "{\"reply\": \"<your message in English, max 3 sentences>\", "
                    + "\"correction\": \"<corrected version of the learner's last sentence, or empty>\", "
                    + "\"mistakes\": [{\"tag\": \"<snake_case error tag>\", "
                    + "\"skill\": \"GRAMMAR or VOCABULARY or LISTENING or READING\", "
                    + "\"explanation\": \"<short rule in Vietnamese>\"}]}."
                    + " The mistakes array is empty when the learner made no error.";

    private final UserRepository userRepository;
    private final AiRepository aiRepository;
    private final ProgressRepository progressRepository;

    @Inject
    public ChatMemoryManager(UserRepository userRepository, AiRepository aiRepository,
                             ProgressRepository progressRepository) {
        this.userRepository = userRepository;
        this.aiRepository = aiRepository;
        this.progressRepository = progressRepository;
    }

    public String buildSystemPrompt(String userId) {
        UserEntity user = userRepository.getUserNow(userId);
        UserSettingsEntity settings = userRepository.getSettingsNow(userId);
        String name = user == null ? "Learner" : user.displayName;
        int streak = user == null ? 0 : user.streakCount;

        List<String> tags = new ArrayList<>();
        for (UserWeaknessEntity weakness : progressRepository.getTopWeaknessesNow(userId, 3)) {
            tags.add(weakness.errorTag);
        }
        String mistakes = tags.isEmpty() ? "none yet" : String.join(", ", tags);

        StringBuilder prompt = new StringBuilder(String.format(ROLE_TEMPLATE, name, streak, mistakes));
        String difficulty = settings == null ? "ADAPTIVE" : settings.aiChallengeDifficulty;
        prompt.append(" Challenge difficulty: ").append(difficulty).append('.');
        List<AiPersonaMemoryEntity> memories = aiRepository.getMemoriesNow(userId);
        if (!memories.isEmpty()) {
            prompt.append(" Known about the learner:");
            for (AiPersonaMemoryEntity memory : memories) {
                prompt.append(' ').append(memory.memoryKey).append('=').append(memory.memoryValue).append(';');
            }
        }
        prompt.append(OUTPUT_CONTRACT);
        return prompt.toString();
    }

    /** A short list of weak tags, used for the offline daily tip and challenge topic. */
    public List<String> weakTags(String userId, int limit) {
        List<String> tags = new ArrayList<>();
        for (UserWeaknessEntity weakness : progressRepository.getTopWeaknessesNow(userId, limit)) {
            tags.add(weakness.errorTag);
        }
        return tags;
    }
}
