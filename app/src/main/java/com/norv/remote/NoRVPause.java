package com.norv.remote;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kloadingspin.KLoadingSpin;

public class NoRVPause extends AppCompatActivity {
    private KLoadingSpin resumeSpin;
    private TextView runningTimeLabel;
    private TextView breaksNumberLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.norv_pause);

        hideSystemUI();

        final int DELAY = 1000;
        ColorDrawable baseColor = new ColorDrawable(0xff00ff00);
        ColorDrawable targetColor = new ColorDrawable(0xffff0000);
        AnimationDrawable layoutBackground = new AnimationDrawable();
        layoutBackground.addFrame(baseColor, DELAY);
        layoutBackground.addFrame(targetColor, DELAY);
        findViewById(R.id.pause_background).setBackground(layoutBackground);
        layoutBackground.start();

        initCamera();

        findViewById(R.id.pause_resume_deposition).setOnClickListener(view -> NoRVPause.this.resumeDeposition());

        resumeSpin = findViewById(R.id.norv_pause_resume_spin);

        runningTimeLabel = findViewById(R.id.pause_total_time);
        breaksNumberLabel = findViewById(R.id.pause_breaks_number);
    }

    private void initCamera()
    {
        WebView cameraView = findViewById(R.id.pause_camera);
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

    private void resumeDeposition() {
        resumeSpin.startAnimation();
        resumeSpin.setIsVisible(true);
        resumeSpin.setVisibility(View.VISIBLE);
        NoRVApi.getInstance().controlDeposition("resumeDeposition", null, new NoRVApi.ControlListener() {
            @Override
            public void onSuccess(String respMsg) {
            }

            @Override
            public void onFailure(String errorMsg) {
                resumeSpin.stopAnimation();
                resumeSpin.setVisibility(View.INVISIBLE);
                Toast.makeText(NoRVPause.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
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
                        case NoRVConst.STARTED:
                            gotoRTMPScreen();
                            break;
                        case NoRVConst.PAUSED:
                            runOnUiThread(() -> {
                                runningTimeLabel.setText(runningTime);
                                breaksNumberLabel.setText(breaksNumber);
                            });
                        default:
                            handler.postDelayed(checkStatus, NoRVConst.CheckStatusInterval);
                            break;
                    }
                    if ("True".equals(ignorable)) {
                        findViewById(R.id.pause_resume_deposition).setEnabled(false);
                    } else {
                        findViewById(R.id.pause_resume_deposition).setEnabled(true);
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
        Intent activityIntent = new Intent(NoRVPause.this, NoRVActivity.class);
        startActivity(activityIntent);
        finish();
    }

    private void gotoConfirmScreen() {
        Intent confirmIntent = new Intent(NoRVPause.this, NoRVConfirm.class);
        startActivity(confirmIntent);
        finish();
    }

    private void gotoRTMPScreen() {
        startService(new Intent(NoRVPause.this, NoRVRTMP.class));
        finish();
    }

}