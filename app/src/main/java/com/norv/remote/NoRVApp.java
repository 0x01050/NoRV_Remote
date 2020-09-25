package com.norv.remote;

import android.app.Application;

public class NoRVApp extends Application {
    public static NoRVApp instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Thread.setDefaultUncaughtExceptionHandler(new NoRVHandler(getApplicationContext()));
    }
    public static NoRVApp getInstance() {
        return instance;
    }
}