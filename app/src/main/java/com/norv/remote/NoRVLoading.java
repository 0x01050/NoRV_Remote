package com.norv.remote;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kloadingspin.KLoadingSpin;

import java.util.ArrayList;

public class NoRVLoading extends AppCompatActivity {

    KLoadingSpin connectSpin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.norv_loading);

        connectSpin = findViewById(R.id.norv_loading_spin);
        connectSpin.startAnimation();
        connectSpin.setIsVisible(true);
        connectSpin.setVisibility(View.VISIBLE);


        WifiManager wifiManager = (WifiManager) NoRVLoading.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if(!wifiManager.isWifiEnabled()) {
            gotoConnectScreen();
            return;
        }

        NoRVApi.getInstance().checkRouterLive(new NoRVApi.RouterListener() {
            @Override
            public void onSuccess(ArrayList<NoRVApi.RouterModel> routers) {
                NoRVApi.getInstance().checkRouterInternet(new NoRVApi.RouterListener() {
                    @Override
                    public void onSuccess(ArrayList<NoRVApi.RouterModel> routers) {
                        NoRVApi.getInstance().getStatus(new NoRVApi.StatusListener() {
                            @Override
                            public void onSuccess(String status, String ignorable, String runningTime, String breaksNumber) {
                                switch (status) {
                                    case NoRVConst.LOADED:
                                        gotoConfirmScreen();
                                        break;
                                    case NoRVConst.STARTED:
                                        gotoRTMPScreen();
                                        break;
                                    case NoRVConst.PAUSED:
                                        gotoPauseScreen();
                                        break;
                                    default:
                                        gotoHomeScreen();
                                        break;
                                }
                            }

                            @Override
                            public void onFailure(String errorMsg) {
                                Log.e("NoRV Get Status", errorMsg);
                                gotoHomeScreen();
                            }
                        });
                    }

                    @Override
                    public void onFailure(String errorMsg) {
                        Log.e("NoRV Router Internet", errorMsg);
                        gotoInternetScreen();
                    }
                });
            }

            @Override
            public void onFailure(String errorMsg) {
                Log.e("NoRV Router Live", errorMsg);
                gotoConnectScreen();
            }
        });
    }

    @Override
    protected void onResume() {
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

    private void gotoConnectScreen() {
        Intent connectIntent = new Intent(NoRVLoading.this, NoRVConnect.class);
        startActivity(connectIntent);
        finish();
    }

    private void gotoInternetScreen() {
        Intent connectIntent = new Intent(NoRVLoading.this, NoRVInternet.class);
        startActivity(connectIntent);
        finish();
    }

    private void gotoHomeScreen() {
        Intent activityIntent = new Intent(NoRVLoading.this, NoRVActivity.class);
        startActivity(activityIntent);
        finish();
    }

    private void gotoConfirmScreen() {
        Intent confirmIntent = new Intent(NoRVLoading.this, NoRVConfirm.class);
        startActivity(confirmIntent);
        finish();
    }

    private void gotoRTMPScreen() {
        startService(new Intent(NoRVLoading.this, NoRVRTMP.class));
        finish();
    }

    private void gotoPauseScreen() {
        Intent pauseIntent = new Intent(NoRVLoading.this, NoRVPause.class);
        startActivity(pauseIntent);
        finish();
    }
}