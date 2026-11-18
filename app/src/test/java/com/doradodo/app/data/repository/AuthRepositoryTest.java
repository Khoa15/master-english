package com.doradodo.app.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthResult;
import com.doradodo.app.data.local.AppDatabase;
import com.doradodo.app.data.local.prefs.AppPreferences;
import com.doradodo.app.utils.AppExecutors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** A failed Firebase task must reach the listener as an error instead of throwing from getResult(). */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AuthRepositoryTest {

    private AppDatabase db;
    private AuthRepository auth;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).allowMainThreadQueries().build();
        AppExecutors executors = new AppExecutors();
        auth = new AuthRepository(context, new AppPreferences(context),
                new UserRepository(db.userDao(), db.badgeDao(), executors));
    }

    @After
    public void tearDown() {
        db.close();
    }

    private static final class Recorder implements AuthRepository.Listener {
        String userId;
        String error;

        @Override
        public void onSuccess(String userId) {
            this.userId = userId;
        }

        @Override
        public void onError(String message) {
            this.error = message;
        }
    }

    @Test
    public void failedSignInReportsTheFirebaseMessage() {
        Recorder recorder = new Recorder();

        auth.handleResult(Tasks.<AuthResult>forException(new Exception("Mật khẩu không đúng")), recorder);

        assertEquals("Mật khẩu không đúng", recorder.error);
        assertNull(recorder.userId);
    }

    @Test
    public void successWithoutAUserIsAnError() {
        Recorder recorder = new Recorder();

        auth.handleResult(Tasks.<AuthResult>forResult(null), recorder);

        assertEquals(AuthRepository.SIGN_IN_FAILED, recorder.error);
    }
}
