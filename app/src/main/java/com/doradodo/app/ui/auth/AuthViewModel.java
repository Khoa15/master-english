package com.doradodo.app.ui.auth;

import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.doradodo.app.R;
import com.doradodo.app.data.repository.AuthRepository;
import com.doradodo.app.data.repository.LanguageRepository;
import com.doradodo.app.utils.AppExecutors;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

@HiltViewModel
public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final LanguageRepository languages;
    private final AppExecutors executors;
    private final MutableLiveData<String> error = new MutableLiveData<>();
    /** An error the app itself wrote (a string resource), as opposed to a message from the server. */
    private final MutableLiveData<Integer> errorRes = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> signedInUser = new MutableLiveData<>();

    @Inject
    public AuthViewModel(AuthRepository authRepository, LanguageRepository languages, AppExecutors executors) {
        this.authRepository = authRepository;
        this.languages = languages;
        this.executors = executors;
    }

    public LiveData<Integer> errorRes() {
        return errorRes;
    }

    public LiveData<String> error() {
        return error;
    }

    public LiveData<Boolean> loading() {
        return loading;
    }

    public LiveData<String> signedInUser() {
        return signedInUser;
    }

    public boolean firebaseReady() {
        return authRepository.isFirebaseConfigured();
    }

    public com.google.android.gms.auth.api.signin.GoogleSignInClient googleClient() {
        return authRepository.googleSignInClient();
    }

    public void login(String email, String password, boolean register) {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            errorRes.setValue(R.string.auth_invalid_email);
            return;
        }
        if (password.length() < 6) {
            errorRes.setValue(R.string.auth_short_password);
            return;
        }
        loading.setValue(true);
        AuthRepository.Listener listener = listener();
        if (register) {
            authRepository.registerWithEmail(email, password, listener);
        } else {
            authRepository.signInWithEmail(email, password, listener);
        }
    }

    public void loginWithGoogle(String idToken) {
        loading.setValue(true);
        authRepository.signInWithGoogle(idToken, listener());
    }

    public void continueAsGuest() {
        loading.setValue(true);
        authRepository.continueAsGuest(listener());
    }

    private AuthRepository.Listener listener() {
        return new AuthRepository.Listener() {
            @Override
            public void onSuccess(String userId) {
                // The language picked on the first onboarding screen becomes the learner's display language.
                executors.diskIO().execute(() -> {
                    languages.applyOnboardingChoice();
                    loading.postValue(false);
                    signedInUser.postValue(userId);
                });
            }

            @Override
            public void onError(String message) {
                if (AuthRepository.NOT_CONFIGURED.equals(message)) {
                    loading.postValue(false);
                    errorRes.postValue(R.string.auth_not_configured);
                    return;
                }
                if (AuthRepository.SIGN_IN_FAILED.equals(message)) {
                    loading.postValue(false);
                    errorRes.postValue(R.string.auth_failed);
                    return;
                }
                loading.postValue(false);
                error.postValue(message);
            }
        };
    }
}
