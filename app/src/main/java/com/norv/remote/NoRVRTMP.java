package com.norv.remote;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.kloadingspin.KLoadingSpin;

import java.util.ArrayList;

public class NoRVRTMP extends Service {
    private WindowManager mWindowManager;
    private View mFloatingView;

    final private static int LEFTMOST = -100000;
    final private static int RIGHTMOST = 100000;

    private KLoadingSpin pauseSpin;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("InflateParams")
    @Override
    public void onCreate() {
        super.onCreate();

        try {
            mFloatingView = LayoutInflater.from(this).inflate(R.layout.norv_rtmp, null);

            final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP;
            params.x = LEFTMOST;
            mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            mWindowManager.addView(mFloatingView, params);

            mFloatingView.findViewById(R.id.rtmp_close_button).setOnClickListener(view -> {
                throw new NullPointerException();
            });

            mFloatingView.findViewById(R.id.rtmp_left_button).setOnClickListener(view -> {
                params.x = LEFTMOST;
                mWindowManager.updateViewLayout(mFloatingView, params);
                mFloatingView.findViewById(R.id.rtmp_left_button).setVisibility(View.INVISIBLE);
                mFloatingView.findViewById(R.id.rtmp_right_button).setVisibility(View.VISIBLE);
            });
            mFloatingView.findViewById(R.id.rtmp_right_button).setOnClickListener(view -> {
                params.x = RIGHTMOST;
                mWindowManager.updateViewLayout(mFloatingView, params);
                mFloatingView.findViewById(R.id.rtmp_left_button).setVisibility(View.VISIBLE);
                mFloatingView.findViewById(R.id.rtmp_right_button).setVisibility(View.INVISIBLE);
            });

            initCamera();

            mFloatingView.findViewById(R.id.rtmp_pause_deposition).setOnClickListener(view -> NoRVRTMP.this.pauseDeposition());
            mFloatingView.findViewById(R.id.rtmp_end_deposition).setOnClickListener(view -> NoRVRTMP.this.endDeposition());

            pauseSpin = mFloatingView.findViewById(R.id.norv_rtmp_pause_spin);
            pauseSpin.startAnimation();
            pauseSpin.stopAnimation();

            isRunning = true;
            handler.postDelayed(checkStatus, NoRVConst.CheckStatusInterval);
            handler.postDelayed(checkRouterLive, NoRVConst.CheckRouterLiveInterval);
        }
        catch (Exception e) {
            stopSelf();
        }
    }

    private void initCamera()
    {
        WebView cameraView = mFloatingView.findViewById(R.id.service_camera);
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
        cameraView.setWebChromeClient(new WebChromeClient()
        {
            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("CameraView", consoleMessage.message());
                return super.onConsoleMessage(consoleMessage);
            }
        });
        cameraView.loadUrl("file:///android_asset/camera.html");
    }

    private void endCamera()
    {
        WebView cameraView = mFloatingView.findViewById(R.id.service_camera);
        cameraView.loadUrl("javascript:closeSocket();");
        cameraView.destroy();
    }

    final Handler handler = new Handler(Looper.getMainLooper());
    boolean isRunning = true;
    final Runnable checkStatus = new Runnable() {
        @Override
        public void run() {
            NoRVApi.getInstance().getStatus(new NoRVApi.StatusListener() {
                @Override
                public void onSuccess(String status, String ignorable, String runningTime, String breaksNumber) {
                    if(!isRunning)
                        return;
                    new Handler(Looper.getMainLooper()).post(() -> {
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
                        if (mFloatingView != null) {
                            if ("True".equals(ignorable)) {
                                mFloatingView.findViewById(R.id.rtmp_pause_deposition).setEnabled(false);
                                mFloatingView.findViewById(R.id.rtmp_end_deposition).setEnabled(false);
                            } else {
                                mFloatingView.findViewById(R.id.rtmp_pause_deposition).setEnabled(true);
                                mFloatingView.findViewById(R.id.rtmp_end_deposition).setEnabled(true);
                            }
                        }
                    });
                }

                @Override
                public void onFailure(String errorMsg) {
                    if(!isRunning)
                        return;
                    Log.e("NoRV Get Status", errorMsg);
                    handler.postDelayed(checkStatus, NoRVConst.CheckStatusInterval);
                }
            });
        }
    };

    final Runnable checkRouterLive = new Runnable() {
        @Override
        public void run() {
            WifiManager wifiManager = (WifiManager) NoRVRTMP.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if(!wifiManager.isWifiEnabled()) {
                Log.e("NoRV Router Live", "Wifi is disabled");
                gotoConnectScreen();
                return;
            }
            NoRVApi.getInstance().checkRouterLive(new NoRVApi.RouterListener() {
                @Override
                public void onSuccess(ArrayList<NoRVApi.RouterModel> routers) {
                    if(!isRunning)
                        return;
                    handler.postDelayed(checkRouterInternet, NoRVConst.CheckRouterInternetInterval);
                }

                @Override
                public void onFailure(String errorMsg) {
                    Log.e("NoRV Router Live", errorMsg);
                    gotoConnectScreen();
                }
            });
        }
    };
    final Runnable checkRouterInternet = () -> {
        if(!isRunning)
            return;
        handler.postDelayed(checkRouterLive, NoRVConst.CheckRouterLiveInterval);
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingView != null) {
            mWindowManager.removeView(mFloatingView);
        }
        isRunning = false;
        handler.removeCallbacks(checkStatus);
        handler.removeCallbacks(checkRouterLive);
        handler.removeCallbacks(checkRouterInternet);
    }

    private void pauseDeposition() {
        pauseSpin.startAnimation();
        pauseSpin.setIsVisible(true);
        pauseSpin.setVisibility(View.VISIBLE);
        NoRVApi.getInstance().controlDeposition("pauseDeposition", null, new NoRVApi.ControlListener() {
            @Override
            public void onSuccess(String respMsg) {
            }

            @Override
            public void onFailure(String errorMsg) {
                pauseSpin.stopAnimation();
                pauseSpin.setVisibility(View.INVISIBLE);
                Toast.makeText(NoRVRTMP.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void endDeposition() {
        endCamera();
        Intent endIntent = new Intent(NoRVRTMP.this, NoRVEnd.class);
        endIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(endIntent);
        stopSelf();
    }

    private void gotoHomeScreen() {
        endCamera();
        Intent activityIntent = new Intent(NoRVRTMP.this, NoRVActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(activityIntent);
        stopSelf();
    }

    private void gotoConfirmScreen() {
        endCamera();
        Intent confirmIntent = new Intent(NoRVRTMP.this, NoRVConfirm.class);
        confirmIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(confirmIntent);
        stopSelf();
    }

    private void gotoPauseScreen() {
        endCamera();
        Intent pauseIntent = new Intent(NoRVRTMP.this, NoRVPause.class);
        pauseIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(pauseIntent);
        stopSelf();
    }

    private void gotoConnectScreen() {
        endCamera();
        Intent connectIntent = new Intent(NoRVRTMP.this, NoRVConnect.class);
        connectIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(connectIntent);
        stopSelf();
    }
}