package com.doradodo.app.ui.vocab;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.doradodo.app.data.local.entity.WordRow;
import com.doradodo.app.data.local.prefs.AppPreferences;
import com.doradodo.app.ui.vocab.PracticeViewModel.Stage;
import com.doradodo.app.utils.AppExecutors;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PracticeViewModelTest {

    private Context context;
    private PracticeViewModel vm;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        vm = new PracticeViewModel(context, new AppPreferences(context), null, null, null,
                new AppExecutors(Runnable::run, Runnable::run));
    }

    private static VocabViewModel.Word word(String term, String example) {
        WordRow row = new WordRow();
        row.id = term;
        row.term = term;
        row.level = "A1";
        row.definition = "meaning of " + term;
        row.example = example;
        return new VocabViewModel.Word(row, VocabViewModel.Status.NEW, false, 0, 0);
    }

    @Test
    public void splitExampleCutsAroundTheWordIgnoringCase() {
        String[] parts = PracticeViewModel.splitExample(word("apple", "An Apple a day."));
        assertArrayEquals(new String[]{"An ", "Apple", " a day."}, parts);
    }

    @Test
    public void splitExampleIsNullWithoutAnExampleOrWhenTheWordIsMissing() {
        assertNull(PracticeViewModel.splitExample(word("apple", null)));
        assertNull(PracticeViewModel.splitExample(word("apple", "A pear a day.")));
    }

    @Test
    public void accuracyIsMinusOneUntilAStageIsGraded() {
        assertEquals(-1, PracticeViewModel.accuracy(context, Stage.CHOICE));
    }

    @Test
    public void recordCountsRightAndWrongAnswersPerStage() {
        vm.record(Stage.CHOICE, "a", true);
        vm.record(Stage.CHOICE, "b", false);
        vm.record(Stage.CHOICE, "c", true);
        vm.record(Stage.CHOICE, "d", true);
        assertEquals(75, PracticeViewModel.accuracy(context, Stage.CHOICE));
    }

    @Test
    public void theFlashcardIsNotGraded() {
        vm.record(Stage.FLASHCARD, "a", false);
        assertEquals(-1, PracticeViewModel.accuracy(context, Stage.FLASHCARD));
    }

    @Test
    public void meaningsAlwaysContainTheRightAnswer() {
        VocabViewModel.Word w = word("apple", null);
        assertEquals(Collections.singletonList(w.meaning), vm.meanings(w, 4));
    }

    @Test
    public void finishBeforeARoundStartedPublishesNothing() {
        vm.finish();
        assertNull(vm.result().getValue());
    }
}
