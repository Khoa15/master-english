package com.doradodo.app.utils;

/** Result callback delivered on the main thread. */
public interface Callback<T> {

    void onResult(T value);
}
