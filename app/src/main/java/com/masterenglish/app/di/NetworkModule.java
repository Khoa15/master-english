package com.masterenglish.app.di;

import com.google.gson.Gson;
import com.masterenglish.app.BuildConfig;
import com.masterenglish.app.data.remote.ApiService;
import com.masterenglish.app.data.remote.agent.AgentApi;

import java.util.concurrent.TimeUnit;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import javax.inject.Singleton;

@Module
@InstallIn(SingletonComponent.class)
public abstract class NetworkModule {

    /** Placeholder only: the AI Agent URL is per-user and supplied at call time through @Url. */
    private static final String AGENT_PLACEHOLDER_BASE_URL = "https://agent-gateway.invalid/";

    @Provides
    @Singleton
    static Gson provideGson() {
        return new Gson();
    }

    @Provides
    @Singleton
    static OkHttpClient provideOkHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        // Never log bodies in release builds: they can contain the API key or learner text.
        logging.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BASIC : HttpLoggingInterceptor.Level.NONE);
        return new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build();
    }

    @Provides
    @Singleton
    static ApiService provideApiService(OkHttpClient client, Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ApiService.class);
    }

    @Provides
    @Singleton
    static AgentApi provideAgentApi(OkHttpClient client, Gson gson) {
        return new Retrofit.Builder()
                .baseUrl(AGENT_PLACEHOLDER_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(AgentApi.class);
    }
}
