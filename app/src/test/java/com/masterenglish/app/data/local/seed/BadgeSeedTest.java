package com.masterenglish.app.data.local.seed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.masterenglish.app.data.local.AppDatabase;
import com.masterenglish.app.utils.AppExecutors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Badges must exist even when content was already seeded by an older build (regression: empty Profile badges). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class BadgeSeedTest {

    private AppDatabase db;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).allowMainThreadQueries().build();
        new LocalDataSeeder(context, db).seedIfEmpty();
    }

    @After
    public void tearDown() {
        db.close();
    }

    @Test
    public void fullSeedInsertsEveryBadge() {
        assertEquals(BadgeCatalogue.all().size(), db.badgeDao().getBadges().size());
        assertFalse(db.badgeDao().getBadges().isEmpty());
    }

    @Test
    public void badgesAreRestoredWhenContentAlreadyExists() {
        db.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM badges");
        assertEquals(0, db.badgeDao().getBadges().size());

        // Content is already present, so only the badge catalogue should be written.
        new LocalDataSeeder(ApplicationProvider.getApplicationContext(), db).seedIfEmpty();

        assertEquals(BadgeCatalogue.all().size(), db.badgeDao().getBadges().size());
    }
}
