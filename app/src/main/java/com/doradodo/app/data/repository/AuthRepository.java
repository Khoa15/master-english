package com.doradodo.app.data.repository;

import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.doradodo.app.R;
import com.doradodo.app.data.local.prefs.AppPreferences;
import com.doradodo.app.data.local.prefs.OnboardingAnswers;

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

    /** Not texts but codes: the screen shows them in the language of the app (see the auth_* strings). */
    public static final String NOT_CONFIGURED = "auth:not_configured";
    public static final String SIGN_IN_FAILED = "auth:sign_in_failed";

    public interface Listener {
        void onSuccess(String userId);

        void onError(String message);
    }

    private final Context context;
    private final AppPreferences preferences;
    private final UserRepository userRepository;

    @Inject
    public AuthRepository(@ApplicationContext Context context, AppPreferences preferences,
                          UserRepository userRepository) {
        this.context = context;
        this.preferences = preferences;
        this.userRepository = userRepository;
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
                .addOnCompleteListener(task -> handleResult(task, listener));
    }

    public void registerWithEmail(String email, String password, Listener listener) {
        if (!isFirebaseConfigured()) {
            listener.onError(NOT_CONFIGURED);
            return;
        }
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> handleResult(task, listener));
    }

    public void signInWithGoogle(String idToken, Listener listener) {
        if (!isFirebaseConfigured()) {
            listener.onError(NOT_CONFIGURED);
            return;
        }
        FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .addOnCompleteListener(task -> handleResult(task, listener));
    }

    /** Guest Mode: data stays in this device's SQLite database under a local id. */
    public void continueAsGuest(Listener listener) {
        String current = preferences.getCurrentUserId();
        String guestId = current != null && current.startsWith("guest-") ? current : "guest-" + UUID.randomUUID();
        // No stored name: the profile shows the localized "Learner" until the user sets one.
        finishLogin(guestId, null, "", null, listener);
    }

    public void signOut() {
        if (isFirebaseConfigured()) {
            FirebaseAuth.getInstance().signOut();
        }
        preferences.clearSession();
    }

    /**
     * Reads the outcome of a Firebase sign-in. {@code Task.getResult()} throws on a failed task, so the
     * result is only read after {@code isSuccessful()} has been checked.
     */
    void handleResult(Task<AuthResult> task, Listener listener) {
        AuthResult result = task.isSuccessful() ? task.getResult() : null;
        if (result == null || result.getUser() == null) {
            Exception error = task.getException();
            listener.onError(error != null && error.getMessage() != null ? error.getMessage() : SIGN_IN_FAILED);
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

    /** Copies the onboarding answers into the profile and the mascot's memory. */
    private void applyOnboardingAnswers(String userId) {
        OnboardingAnswers answers = preferences.getOnboardingAnswers();
        if (answers == null) {
            return;
        }
        userRepository.saveTargetMinutes(userId, answers.dailyMinutes);
    }
}
