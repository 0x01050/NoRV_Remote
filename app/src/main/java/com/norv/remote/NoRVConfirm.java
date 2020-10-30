package com.norv.remote;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.crowdfire.cfalertdialog.CFAlertDialog;
import com.example.kloadingspin.KLoadingSpin;

public class NoRVConfirm extends AppCompatActivity {

    private KLoadingSpin startSpin = null;
    private KLoadingSpin cancelSpin = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.norv_confirm);

        hideSystemUI();

        initCamera();

        findViewById(R.id.confirm_cancel_deposition).setOnClickListener(view -> new CFAlertDialog.Builder(NoRVConfirm.this)
            .setDialogStyle(CFAlertDialog.CFAlertStyle.BOTTOM_SHEET)
            .setTextGravity(Gravity.CENTER_HORIZONTAL)
            .setTitle("Cancel Deposition?")
            .setMessage("Looks like you are going to cancel the deposition. Click Yes to cancel.")
            .setCancelable(false)
            .addButton("YES", -1, -1, CFAlertDialog.CFAlertActionStyle.POSITIVE, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, (dialog, which) -> {
                dialog.dismiss();
                NoRVConfirm.this.cancelDeposition();
            })
            .addButton("NO", -1, -1, CFAlertDialog.CFAlertActionStyle.NEGATIVE, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, (dialog, which) -> dialog.dismiss())
            .show()
        );
        findViewById(R.id.confirm_start_deposition).setOnClickListener(view -> new CFAlertDialog.Builder(NoRVConfirm.this)
            .setDialogStyle(CFAlertDialog.CFAlertStyle.BOTTOM_SHEET)
            .setTextGravity(Gravity.CENTER_HORIZONTAL)
            .setTitle("Start Deposition?")
            .setMessage("Looks like you are going to start the deposition. Click Yes to start.")
            .setCancelable(false)
            .addButton("YES", -1, -1, CFAlertDialog.CFAlertActionStyle.POSITIVE, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, (dialog, which) -> {
                dialog.dismiss();
                NoRVConfirm.this.startDeposition();
            })
            .addButton("NO", -1, -1, CFAlertDialog.CFAlertActionStyle.NEGATIVE, CFAlertDialog.CFAlertActionAlignment.JUSTIFIED, (dialog, which) -> dialog.dismiss())
            .show()
        );

        findViewById(R.id.confirm_dialog_accept).setOnClickListener(view -> {
            NoRVConfirm.this.dismissConfirm();
            NoRVConfirm.this.runDeposition();
        });
        findViewById(R.id.confirm_dialog_cancel).setOnClickListener(view -> NoRVConfirm.this.dismissConfirm());

        startSpin = findViewById(R.id.norv_confirm_start_spin);
        cancelSpin = findViewById(R.id.norv_confirm_cancel_spin);
    }

    private void initCamera()
    {
        WebView cameraView = findViewById(R.id.confirm_camera);
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

    private void cancelDeposition()
    {
        cancelSpin.startAnimation();
        cancelSpin.setIsVisible(true);
        cancelSpin.setVisibility(View.VISIBLE);
        NoRVApi.getInstance().controlDeposition("cancelDeposition", null, new NoRVApi.ControlListener() {
            @Override
            public void onSuccess(String respMsg) {
            }

            @Override
            public void onFailure(String errorMsg) {
                cancelSpin.stopAnimation();
                cancelSpin.setVisibility(View.INVISIBLE);
                Toast.makeText(NoRVConfirm.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startDeposition()
    {
        findViewById(R.id.confirm_dialog_container).setVisibility(View.VISIBLE);
    }

    private void dismissConfirm()
    {
        findViewById(R.id.confirm_dialog_container).setVisibility(View.INVISIBLE);
    }

    private void runDeposition()
    {
        startSpin.startAnimation();
        startSpin.setIsVisible(true);
        startSpin.setVisibility(View.VISIBLE);
        NoRVApi.getInstance().controlDeposition("startDeposition", null, new NoRVApi.ControlListener() {
            @Override
            public void onSuccess(String respMsg) {
            }

            @Override
            public void onFailure(String errorMsg) {
                startSpin.stopAnimation();
                startSpin.setVisibility(View.INVISIBLE);
                Toast.makeText(NoRVConfirm.this, errorMsg, Toast.LENGTH_SHORT).show();
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
                        case NoRVConst.STARTED:
                            gotoRTMPScreen();
                            break;
                        case NoRVConst.PAUSED:
                            gotoPauseScreen();
                            break;
                        default:
                            handler.postDelayed(checkStatus, NoRVConst.CheckStatusInterval);
                            break;
                    }
                    if ("True".equals(ignorable)) {
                        findViewById(R.id.confirm_start_deposition).setEnabled(false);
                        findViewById(R.id.confirm_cancel_deposition).setEnabled(false);
                    } else {
                        findViewById(R.id.confirm_start_deposition).setEnabled(true);
                        findViewById(R.id.confirm_cancel_deposition).setEnabled(true);
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
        Intent activityIntent = new Intent(NoRVConfirm.this, NoRVActivity.class);
        startActivity(activityIntent);
        finish();
    }

    private void gotoRTMPScreen() {
        startService(new Intent(NoRVConfirm.this, NoRVRTMP.class));
        finish();
    }

    private void gotoPauseScreen() {
        Intent pauseIntent = new Intent(NoRVConfirm.this, NoRVPause.class);
        startActivity(pauseIntent);
        finish();
    }
}