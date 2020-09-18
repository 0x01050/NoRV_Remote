package com.norv.player;

import android.app.Application;
import android.content.Context;

public class NoRVApp extends Application {
    public static NoRVApp instance;
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Thread.setDefaultUncaughtExceptionHandler(new NoRVHandler(getApplicationContext()));
    }
    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }
    public static NoRVApp getInstance() {
        return instance;
    }
}