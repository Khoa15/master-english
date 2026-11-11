package com.masterenglish.app.service.ai;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.masterenglish.app.data.local.Enums.SkillType;

import org.junit.Test;

public class AiTutorParseTest {

    @Test
    public void parsesTheJsonContract() {
        String raw = "{\"reply\":\"Nice try!\",\"correction\":\"I am a developer.\","
                + "\"mistakes\":[{\"tag\":\"Past Simple\",\"skill\":\"grammar\",\"explanation\":\"Dùng was/were\"}]}";
        AiTutorService.Evaluation e = AiTutorService.parse(raw);
        assertEquals("Nice try!", e.reply);
        assertEquals("I am a developer.", e.correction);
        assertEquals(1, e.mistakes.size());
        assertEquals("Past Simple", e.mistakes.get(0).tag);
        assertEquals(SkillType.GRAMMAR, e.mistakes.get(0).skill);
    }

    @Test
    public void stripsMarkdownFencesAroundJson() {
        String raw = "```json\n{\"reply\":\"Hi\",\"correction\":\"\",\"mistakes\":[]}\n```";
        AiTutorService.Evaluation e = AiTutorService.parse(raw);
        assertEquals("Hi", e.reply);
        assertTrue(e.mistakes.isEmpty());
    }

    @Test
    public void fallsBackToPlainTextWhenModelIgnoresTheContract() {
        AiTutorService.Evaluation e = AiTutorService.parse("Just chatting, no JSON here.");
        assertEquals("Just chatting, no JSON here.", e.reply);
        assertTrue(e.mistakes.isEmpty());
    }

    @Test
    public void unknownSkillFallsBackToGrammar() {
        String raw = "{\"reply\":\"x\",\"mistakes\":[{\"tag\":\"t\",\"skill\":\"WHAT\",\"explanation\":\"\"}]}";
        assertEquals(SkillType.GRAMMAR, AiTutorService.parse(raw).mistakes.get(0).skill);
    }
}
