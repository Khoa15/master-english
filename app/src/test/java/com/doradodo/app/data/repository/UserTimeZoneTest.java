package com.doradodo.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.doradodo.app.data.local.prefs.AppPreferences;

import java.time.ZoneId;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class UserTimeZoneTest {

    private UserTimeZone timeZone;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        timeZone = new UserTimeZone(new AppPreferences(context));
    }

    @Test
    public void followsTheDeviceUntilTheLearnerChoosesAZone() {
        assertTrue(timeZone.isAutomatic());
        assertEquals(ZoneId.systemDefault(), timeZone.zone());
    }

    @Test
    public void aChosenZoneWins() {
        timeZone.set("Asia/Ho_Chi_Minh");
        assertFalse(timeZone.isAutomatic());
        assertEquals(ZoneId.of("Asia/Ho_Chi_Minh"), timeZone.zone());
    }

    @Test
    public void goingBackToAutomaticFollowsTheDeviceAgain() {
        timeZone.set("Europe/Paris");
        timeZone.useAutomatic();
        assertTrue(timeZone.isAutomatic());
        assertEquals(ZoneId.systemDefault(), timeZone.zone());
    }

    @Test
    public void anUnknownZoneIdIsIgnored() {
        timeZone.set("Asia/Ho_Chi_Minh");
        timeZone.set("Not/AZone");
        assertEquals(ZoneId.of("Asia/Ho_Chi_Minh"), timeZone.zone());
    }

    @Test
    public void theChoiceSurvivesANewInstance() {
        timeZone.set("America/New_York");
        Context context = ApplicationProvider.getApplicationContext();
        assertEquals(ZoneId.of("America/New_York"), new UserTimeZone(new AppPreferences(context)).zone());
    }
}
