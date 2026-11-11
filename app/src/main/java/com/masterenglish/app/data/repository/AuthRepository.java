package com.masterenglish.app.data.repository;

import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.masterenglish.app.R;
import com.masterenglish.app.data.local.prefs.AppPreferences;

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

/**
 * Firebase Authentication (Email/Password and Google) plus Guest Mode.
 * Firebase is optional: without app/google-services.json only Guest Mode is available.
 */
@Singleton
public class AuthRepository {

    public static final String NOT_CONFIGURED = "Firebase chưa được cấu hình. Dùng chế độ Học thử hoặc thêm google-services.json.";

    public interface Listener {
        void onSuccess(String userId);

        void onError(String message);
    }

    private final Context context;
    private final AppPreferences preferences;
    private final UserRepository userRepository;
    private final AiRepository aiRepository;

    @Inject
    public AuthRepository(@ApplicationContext Context context, AppPreferences preferences,
                          UserRepository userRepository, AiRepository aiRepository) {
        this.context = context;
        this.preferences = preferences;
        this.userRepository = userRepository;
        this.aiRepository = aiRepository;
    }

    public boolean isFirebaseConfigured() {
        return !FirebaseApp.getApps(context).isEmpty();
    }

    public GoogleSignInClient googleSignInClient() {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.google_web_client_id))
                .requestEmail()
                .build();
        return GoogleSignIn.getClient(context, options);
    }

    public void signInWithEmail(String email, String password, Listener listener) {
        if (!isFirebaseConfigured()) {
            listener.onError(NOT_CONFIGURED);
            return;
        }
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> handleResult(task.isSuccessful(), task.getResult(),
                        task.getException(), listener));
    }

    public void registerWithEmail(String email, String password, Listener listener) {
        if (!isFirebaseConfigured()) {
            listener.onError(NOT_CONFIGURED);
            return;
        }
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> handleResult(task.isSuccessful(), task.getResult(),
                        task.getException(), listener));
    }

    public void signInWithGoogle(String idToken, Listener listener) {
        if (!isFirebaseConfigured()) {
            listener.onError(NOT_CONFIGURED);
            return;
        }
        FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .addOnCompleteListener(task -> handleResult(task.isSuccessful(), task.getResult(),
                        task.getException(), listener));
    }

    /** Guest Mode: data stays in this device's SQLite database under a local id. */
    public void continueAsGuest(Listener listener) {
        String current = preferences.getCurrentUserId();
        String guestId = current != null && current.startsWith("guest-") ? current : "guest-" + UUID.randomUUID();
        finishLogin(guestId, null, "Khách", null, listener);
    }

    public void signOut() {
        if (isFirebaseConfigured()) {
            FirebaseAuth.getInstance().signOut();
        }
        preferences.clearSession();
    }

    private void handleResult(boolean success, AuthResult result, Exception error, Listener listener) {
        if (!success || result == null || result.getUser() == null) {
            listener.onError(error != null && error.getMessage() != null ? error.getMessage() : "Đăng nhập thất bại");
            return;
        }
        FirebaseUser user = result.getUser();
        String email = user.getEmail();
        String name = user.getDisplayName() != null ? user.getDisplayName()
                : email != null ? email.substring(0, email.indexOf('@') > 0 ? email.indexOf('@') : email.length())
                : "Learner";
        finishLogin(user.getUid(), email, name, user.getUid(), listener);
    }

    private void finishLogin(String userId, String email, String displayName, String googleId, Listener listener) {
        userRepository.ensureUser(userId, email, displayName, googleId, ignored -> {
            preferences.setCurrentUserId(userId);
            applyOnboardingAnswers(userId);
            preferences.setOnboardingDone(true);
            listener.onSuccess(userId);
        });
    }

    /** Copies the onboarding survey into the profile and the mascot's memory. */
    private void applyOnboardingAnswers(String userId) {
        String goal = preferences.getOnboardingGoal();
        String level = preferences.getOnboardingLevel();
        if (goal == null || level == null) {
            return;
        }
        userRepository.saveTargetMinutes(userId, preferences.getOnboardingMinutes());
        aiRepository.remember(userId, "learning_goal", goal, 1f);
        aiRepository.remember(userId, "start_level", level, 1f);
    }
}
