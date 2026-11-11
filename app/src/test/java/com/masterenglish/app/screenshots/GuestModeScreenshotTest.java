package com.masterenglish.app.screenshots;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Looper;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.tabs.TabLayout;
import com.masterenglish.app.MainActivity;
import com.masterenglish.app.R;
import com.masterenglish.app.ui.roadmap.LessonPreviewSheet;

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
import java.util.function.BooleanSupplier;

/**
 * Drives the Guest Mode journey through the real UI and saves a PNG of each screen.
 * Output: screenshots/guest-mode/*.png at the repository root.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, qualifiers = "w412dp-h915dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class GuestModeScreenshotTest {

    private static final File OUT = new File("../screenshots/guest-mode");

    @Test
    public void guestModeJourney() throws Exception {
        OUT.mkdirs();
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();
        settle();

        shot(activity, "01-onboarding-intro");
        click(activity, R.id.nextButton);
        settle();
        shot(activity, "02-onboarding-goal");
        click(activity, R.id.nextButton);
        settle();
        shot(activity, "03-onboarding-level");
        click(activity, R.id.nextButton);
        settle();
        shot(activity, "04-auth");

        click(activity, R.id.guestButton);
        waitUntil(() -> activity.findViewById(R.id.roadmapList) != null);
        RecyclerView roadmap = activity.findViewById(R.id.roadmapList);
        waitUntil(() -> roadmap.getAdapter() != null && roadmap.getAdapter().getItemCount() > 0);
        settle();
        shot(activity, "05-roadmap");

        roadmap.post(() -> { });
        settle();
        RecyclerView.ViewHolder first = roadmap.findViewHolderForAdapterPosition(0);
        assertNotNull("first roadmap node not laid out", first);
        first.itemView.performClick();
        settle();
        BottomSheetDialogFragment sheet = findSheet(activity);
        assertNotNull("lesson preview sheet not shown", sheet);
        View sheetDecor = sheet.getDialog().getWindow().getDecorView();
        shotView(activity, sheetDecor, "06-lesson-preview");
        sheetDecor.findViewById(R.id.startButton).performClick();
        settle();

        waitUntil(() -> activity.findViewById(R.id.questionContainer) != null
                && ((android.view.ViewGroup) activity.findViewById(R.id.questionContainer)).getChildCount() > 0);
        settle();
        shot(activity, "07-lesson-question");

        View options = activity.findViewById(R.id.optionsContainer);
        if (options instanceof android.view.ViewGroup && ((android.view.ViewGroup) options).getChildCount() > 0) {
            ((android.view.ViewGroup) options).getChildAt(0).performClick();
            click(activity, R.id.actionButton);
            settle();
            shot(activity, "08-lesson-feedback");
        }

        Navigation.findNavController(activity, R.id.nav_host).popBackStack();
        settle();
        BottomNavigationView bottom = activity.findViewById(R.id.bottom_nav);
        bottom.setSelectedItemId(R.id.reviewFragment);
        settle();
        shot(activity, "09-review-vocabulary");

        TabLayout tabs = findTabs(activity);
        if (tabs != null) {
            tabs.selectTab(tabs.getTabAt(1));
            settle();
            shot(activity, "10-review-grammar");
            tabs.selectTab(tabs.getTabAt(2));
            settle();
            shot(activity, "11-review-listening");
        }

        Navigation.findNavController(activity, R.id.nav_host).navigate(R.id.aiChallengeFragment);
        settle();
        shot(activity, "12-ai-challenge");
        Navigation.findNavController(activity, R.id.nav_host).popBackStack();
        settle();

        bottom.setSelectedItemId(R.id.profileFragment);
        settle();
        shot(activity, "13-profile");
    }

    private static TabLayout findTabs(AppCompatActivity activity) {
        return activity.findViewById(R.id.tabs);
    }

    private static BottomSheetDialogFragment findSheet(AppCompatActivity activity) {
        Fragment host = activity.getSupportFragmentManager().findFragmentById(R.id.nav_host);
        if (host == null) {
            return null;
        }
        for (Fragment fragment : host.getChildFragmentManager().getFragments()) {
            if (fragment instanceof LessonPreviewSheet) {
                return (BottomSheetDialogFragment) fragment;
            }
        }
        for (Fragment top : host.getChildFragmentManager().getFragments()) {
            for (Fragment nested : top.getChildFragmentManager().getFragments()) {
                if (nested instanceof LessonPreviewSheet) {
                    return (BottomSheetDialogFragment) nested;
                }
            }
        }
        return null;
    }

    private static void click(AppCompatActivity activity, int id) {
        View view = activity.findViewById(id);
        assertNotNull("missing view " + id, view);
        view.performClick();
    }

    private static void settle() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(40);
        }
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
        shotView(activity, activity.getWindow().getDecorView(), name);
    }

    private static void shotView(AppCompatActivity activity, View root, String name) throws Exception {
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
