package com.norv.remote;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kloadingspin.KLoadingSpin;

public class NoRVEnd extends AppCompatActivity {
    private KLoadingSpin stopSpin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.norv_end);

        hideSystemUI();

        initCamera();

        findViewById(R.id.accept_conclude_deposition).setOnClickListener(view -> NoRVEnd.this.acceptEndDeposition());
        findViewById(R.id.cancel_conclude_deposition).setOnClickListener(view -> NoRVEnd.this.cancelEndDeposition());

        stopSpin = findViewById(R.id.norv_end_stop_spin);
    }

    private void initCamera()
    {
        WebView cameraView = findViewById(R.id.end_camera);
        WebSettings settings = cameraView.getSettings();
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAppCacheEnabled(true);
        settings.setSafeBrowsingEnabled(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        cameraView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        CookieManager.getInstance().setAcceptThirdPartyCookies(cameraView, true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(true);
        settings.setBlockNetworkImage(false);
        cameraView.loadUrl("file:///android_asset/camera.html");
    }

    private void acceptEndDeposition() {
        stopSpin.startAnimation();
        stopSpin.setIsVisible(true);
        stopSpin.setVisibility(View.VISIBLE);
        NoRVApi.getInstance().controlDeposition("stopDeposition", null, new NoRVApi.ControlListener() {
            @Override
            public void onSuccess(String respMsg) {
            }

            @Override
            public void onFailure(String errorMsg) {
                stopSpin.stopAnimation();
                stopSpin.setVisibility(View.INVISIBLE);
                Toast.makeText(NoRVEnd.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cancelEndDeposition() {
        startService(new Intent(NoRVEnd.this, NoRVRTMP.class));
        finish();
    }

    @Override
    public void onBackPressed()
    {
    }

    final Handler handler = new Handler(Looper.getMainLooper());
    final Runnable checkStatus = new Runnable() {
        @Override
        public void run() {
            NoRVApi.getInstance().getStatus(new NoRVApi.StatusListener() {
                @Override
                public void onSuccess(String status, String ignorable, String runningTime, String breaksNumber) {
                    switch (status) {
                        case NoRVConst.STOPPED:
                            gotoHomeScreen();
                            break;
                        case NoRVConst.LOADED:
                            gotoConfirmScreen();
                            break;
                        case NoRVConst.PAUSED:
                            gotoPauseScreen();
                            break;
                        default:
                            handler.postDelayed(checkStatus, NoRVConst.CheckStatusInterval);
                            break;
                    }
                    if ("True".equals(ignorable)) {
                        findViewById(R.id.accept_conclude_deposition).setEnabled(false);
                        findViewById(R.id.cancel_conclude_deposition).setEnabled(false);
                    } else {
                        findViewById(R.id.accept_conclude_deposition).setEnabled(true);
                        findViewById(R.id.cancel_conclude_deposition).setEnabled(true);
                    }
                }

                @Override
                public void onFailure(String errorMsg) {
                    Log.e("NoRV Get Status", errorMsg);
                    handler.postDelayed(checkStatus, NoRVConst.CheckStatusInterval);
                }
            });
        }
    };

    @Override
    protected void onPause() {
        handler.removeCallbacks(checkStatus);
        super.onPause();
    }

    @Override
    protected void onResume() {
        handler.postDelayed(checkStatus, NoRVConst.CheckStatusInterval);
        hideSystemUI();
        super.onResume();
    }

    private void hideSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
        ActionBar actionBar = this.getSupportActionBar();
        if(actionBar != null)
            actionBar.hide();
    }

    private void gotoHomeScreen() {
        Intent activityIntent = new Intent(NoRVEnd.this, NoRVActivity.class);
        startActivity(activityIntent);
        finish();
    }

    private void gotoConfirmScreen() {
        Intent confirmIntent = new Intent(NoRVEnd.this, NoRVConfirm.class);
        startActivity(confirmIntent);
        finish();
    }

    private void gotoPauseScreen() {
        Intent pauseIntent = new Intent(NoRVEnd.this, NoRVPause.class);
        startActivity(pauseIntent);
        finish();
    }
}
