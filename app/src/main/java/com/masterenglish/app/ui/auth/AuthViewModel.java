package com.masterenglish.app.ui.auth;

import android.util.Patterns;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.masterenglish.app.data.repository.AuthRepository;

import dagger.hilt.android.lifecycle.HiltViewModel;

import javax.inject.Inject;

@HiltViewModel
public class AuthViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> signedInUser = new MutableLiveData<>();

    @Inject
    public AuthViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
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
            error.setValue("Email không hợp lệ");
            return;
        }
        if (password.length() < 6) {
            error.setValue("Mật khẩu cần ít nhất 6 ký tự");
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
                loading.postValue(false);
                signedInUser.postValue(userId);
            }

            @Override
            public void onError(String message) {
                loading.postValue(false);
                error.postValue(message);
            }
        };
    }
}
