package com.masterenglish.app.screenshots;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.masterenglish.app.MainActivity;
import com.masterenglish.app.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.function.BooleanSupplier;

/**
 * Captures every screen of the app for the design audit against the Roadmap canvas.
 * Output: screenshots/audit/*.png and screenshots/audit/report.txt (one line per screen).
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w390dp-h844dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class DesignAuditScreenshotTest {

    private static final File OUT = new File("../screenshots/audit");

    @Test
    public void captureAllScreens() throws Exception {
        OUT.mkdirs();
        PrintWriter report = new PrintWriter(new File(OUT, "report.txt"), "UTF-8");
        try {
            MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
            settle();
            runOnboardingToGuest(activity);
            report.println("OK onboarding+guest reached");

            NavController nav = Navigation.findNavController(activity, R.id.nav_host);
            // Main tabs: the bottom bar switches them.
            capture(report, activity, "roadmap", () -> nav.navigate(R.id.roadmapFragment));
            capture(report, activity, "practice", () -> nav.navigate(R.id.reviewFragment));
            capture(report, activity, "tutor", () -> nav.navigate(R.id.tutorFragment));
            capture(report, activity, "profile", () -> nav.navigate(R.id.profileFragment));
            capture(report, activity, "vocabulary", () -> nav.navigate(R.id.vocabularyFragment));
            capture(report, activity, "vocab-topic-A1", () -> nav.navigate(R.id.vocabTopicFragment, args("topicId", "A1")));
            capture(report, activity, "vocab-practice", () -> nav.navigate(R.id.vocabPracticeFragment, args("topicId", "all")));
            capture(report, activity, "vocab-complete", () -> nav.navigate(R.id.vocabCompleteFragment, args("topicId", "all")));
            capture(report, activity, "speaking-practice", () -> nav.navigate(R.id.speakingPracticeFragment, args("topicId", "job-interview")));
            capture(report, activity, "speaking-summary", () -> nav.navigate(R.id.speakingSummaryFragment, args("topicId", "job-interview")));
            capture(report, activity, "skill-detail", () -> nav.navigate(R.id.skillDetailFragment, args("skillId", "listening")));
            capture(report, activity, "ai-challenge", () -> nav.navigate(R.id.aiChallengeFragment));
            capture(report, activity, "flashcards", () -> nav.navigate(R.id.flashcardFragment));
            capture(report, activity, "lesson", () -> nav.navigate(R.id.lessonFragment, args("nodeId", "")));
        } finally {
            report.close();
        }
    }

    /** Pick a language, step through the survey, then continue as a guest. */
    private static void runOnboardingToGuest(MainActivity activity) throws InterruptedException {
        View english = activity.findViewById(R.id.langEnglish);
        if (english == null) {
            return;
        }
        english.performClick();
        settle();
        for (int i = 0; i < 10; i++) {
            View next = activity.findViewById(R.id.nextButton);
            if (next == null || activity.findViewById(R.id.guestButton) != null) {
                break;
            }
            next.performClick();
            settle();
        }
        View guest = activity.findViewById(R.id.guestButton);
        if (guest != null) {
            guest.performClick();
        }
        waitUntil(() -> activity.findViewById(R.id.roadmapList) != null);
        settle();
    }

    private interface Step {
        void run();
    }

    private static void capture(PrintWriter report, AppCompatActivity activity, String name, Step step) {
        report.println("START " + name);
        report.flush();
        try {
            step.run();
            settle();
            waitIdle();
            shot(activity, name);
            report.println("OK " + name);
        } catch (Throwable t) {
            report.println("FAIL " + name + ": " + t);
        }
        report.flush();
    }

    private static Bundle args(String key, String value) {
        Bundle bundle = new Bundle();
        bundle.putString(key, value);
        return bundle;
    }

    private static void waitIdle() throws InterruptedException {
        // Bounded: a repeating animation would keep idle() running forever.
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(300));
    }

    private static void settle() throws InterruptedException {
        // Bounded: a repeating animation would keep idle() running forever.
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(300));
    }

    private static void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 20_000;
        while (!condition.getAsBoolean()) {
            assertTrue("timed out waiting for UI state", System.currentTimeMillis() < deadline);
            ShadowLooper.idleMainLooper();
            Thread.sleep(20);
        }
        settle();
    }

    private static void shot(AppCompatActivity activity, String name) throws Exception {
        View root = activity.getWindow().getDecorView();
        int width = activity.getResources().getDisplayMetrics().widthPixels;
        int height = activity.getResources().getDisplayMetrics().heightPixels;
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        OUT.mkdirs();
        try (FileOutputStream out = new FileOutputStream(new File(OUT, name + ".png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        }
        bitmap.recycle();
    }
}
