package com.doradodo.app.data.remote;

import android.content.Context;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Adds the signed-in learner's Firebase ID token as {@code Authorization: Bearer}. Runs on OkHttp's
 * background thread, so waiting for the token is fine. Guests have no token: the server answers 401.
 * Firebase refreshes an expired token by itself when {@code getIdToken(false)} is asked for it.
 */
public final class AuthInterceptor implements Interceptor {

    private final Context context;

    public AuthInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String token = currentToken();
        if (token != null) {
            request = request.newBuilder().header("Authorization", "Bearer " + token).build();
        }
        return chain.proceed(request);
    }

    private String currentToken() {
        if (FirebaseApp.getApps(context).isEmpty()) {
            return null;
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return null;
        }
        try {
            GetTokenResult result = Tasks.await(user.getIdToken(false), 10, TimeUnit.SECONDS);
            return result.getToken();
        } catch (Exception e) {
            return null;
        }
    }
}
