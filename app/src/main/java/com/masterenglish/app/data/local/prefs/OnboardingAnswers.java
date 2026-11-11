package com.masterenglish.app.data.local.prefs;

import java.util.ArrayList;
import java.util.List;

/**
 * What the learner picked in the onboarding flow. Values are the labels shown on screen, so the
 * mascot's memory can read them directly. Stored as JSON by {@link AppPreferences}.
 */
public class OnboardingAnswers {

    public String name = "";
    public String role = "Student";
    public String ageRange = "25 - 34";
    public String goal = "Advance my career";
    public int dailyMinutes = 20;
    public List<String> skills = new ArrayList<>(List.of("Vocabulary", "Listening", "Speaking"));
    public String job = "Software and IT";
}
