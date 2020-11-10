package com.norv.remote;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class NoRVLoading extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.norv_loading);
    }

    final Handler handler = new Handler(Looper.getMainLooper());
    final Runnable checkRouterLive = () -> NoRVApi.getInstance().checkRouterLive(new NoRVApi.RouterListener() {
        @Override
        public void onSuccess(ArrayList<NoRVApi.RouterModel> routers) {
            handler.postDelayed(checkRouterInternet, NoRVConst.CheckRouterInternetInterval);
        }

        @Override
        public void onFailure(String errorMsg) {
            Log.e("NoRV Router Live", errorMsg);
            gotoConnect();
        }
    });
    final Runnable checkRouterInternet = () -> {
    };

    @Override
    protected void onPause() {
        handler.removeCallbacks(checkRouterLive);
        handler.removeCallbacks(checkRouterInternet);
        super.onPause();
    }

    @Override
    protected void onResume() {
        handler.postDelayed(checkRouterLive, NoRVConst.CheckRouterLiveInterval);
        hideSystemUI();
        super.onResume();
    }

    private void hideSystemUI() {
        ActionBar actionBar = this.getSupportActionBar();
        if(Settings.canDrawOverlays(this)) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
            if(actionBar != null)
                actionBar.hide();
        } else if(actionBar != null) {
            actionBar.show();
        }
    }

    private void gotoConnect() {
        Intent connectIntent = new Intent(NoRVLoading.this, NoRVConnect.class);
        startActivity(connectIntent);
        finish();
    }
}