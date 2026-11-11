package com.masterenglish.app.ui.review;

import com.masterenglish.app.service.ai.AiTutorService;

import java.util.ArrayList;
import java.util.List;

/** One line of the challenge conversation. */
public final class ChatItem {

    public final boolean fromUser;
    public final String text;
    public final String correction;
    public final List<AiTutorService.Mistake> mistakes;

    private ChatItem(boolean fromUser, String text, String correction, List<AiTutorService.Mistake> mistakes) {
        this.fromUser = fromUser;
        this.text = text;
        this.correction = correction;
        this.mistakes = mistakes;
    }

    public static ChatItem fromUser(String text) {
        return new ChatItem(true, text, "", new ArrayList<>());
    }

    public static ChatItem fromMascot(AiTutorService.Evaluation evaluation) {
        return new ChatItem(false, evaluation.reply, evaluation.correction, evaluation.mistakes);
    }
}
