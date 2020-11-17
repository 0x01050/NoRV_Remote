package com.norv.remote;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kloadingspin.KLoadingSpin;

import java.util.ArrayList;

public class NoRVInternet extends AppCompatActivity {

    TextView connectButton;
    KLoadingSpin connectSpin;
    int animTick = -1;

    AlertDialog currentAlert = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.norv_internet);

        connectSpin = findViewById(R.id.norv_internet_spin);

        connectButton = findViewById(R.id.internet_button);
        connectButton.setOnClickListener(view -> {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String password = sharedPreferences.getString(NoRVConst.Router_PWD_Key, "");
            if (password == null || password.isEmpty()) {

                AlertDialog.Builder alert = new AlertDialog.Builder(NoRVInternet.this);
                alert.setTitle("NoRV Remote");
                alert.setMessage("Input password for NoRV-Center Router");

                LinearLayout inputLayout = new LinearLayout(NoRVInternet.this);
                inputLayout.setOrientation(LinearLayout.VERTICAL);

                final EditText input = new EditText(NoRVInternet.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                inputLayout.addView(input);
                inputLayout.setPadding(25, 0, 25, 0);
                alert.setView(inputLayout);

                alert.setPositiveButton("Ok", (dialog, whichButton) -> {
                    currentAlert = null;
                    if (input.getText().toString().isEmpty())
                        return;
                    ConnectInternet(input.getText().toString());
                });

                alert.setNegativeButton("Cancel", (dialog, whichButton) -> currentAlert = null);

                currentAlert = alert.show();
            } else {
                ConnectInternet(password);
            }
        });
    }

    private void startAnimation() {
        connectSpin.startAnimation();
        connectSpin.setIsVisible(true);
        connectSpin.setVisibility(View.VISIBLE);
        animTick = 0;
    }

    private void stopAnimation() {
        connectSpin.stopAnimation();
        connectSpin.setVisibility(View.INVISIBLE);
        animTick = -1;
    }

    private void ConnectInternet(String password) {
        startAnimation();

        NoRVApi.getInstance().loginRouter(password, new NoRVApi.RouterListener() {
            @Override
            public void onSuccess(ArrayList<NoRVApi.RouterModel> routers) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(NoRVConst.Router_PWD_Key, password);
                editor.apply();

                NoRVApi.getInstance().scanRouters(new NoRVApi.RouterListener() {
                    @Override
                    public void onSuccess(ArrayList<NoRVApi.RouterModel> routers) {
                        AlertDialog.Builder alert = new AlertDialog.Builder( NoRVInternet.this);
                        alert.setTitle("NoRV Remote");
                        alert.setMessage("Select Wifi router and Input password for it");

//                        ArrayList<String> routerSSIDs = new ArrayList<>();
//                        for(NoRVApi.RouterModel router : routers) {
//                            routerSSIDs.add(router.ssid);
//                        }

                        LinearLayout inputLayout = new LinearLayout(NoRVInternet.this);
                        inputLayout.setOrientation(LinearLayout.VERTICAL);

                        final Spinner ssidInput = new Spinner(NoRVInternet.this);
                        ssidInput.setAdapter(new ArrayAdapter<>(NoRVInternet.this, android.R.layout.simple_spinner_dropdown_item, routers));
                        final EditText pwdInput = new EditText(NoRVInternet.this);
                        pwdInput.setHint("Password");
                        pwdInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                        inputLayout.addView(ssidInput);
                        inputLayout.addView(pwdInput);
                        inputLayout.setPadding(25, 0, 25, 0);
                        alert.setView(inputLayout);

                        alert.setPositiveButton("Ok", (dialog, whichButton) -> {
                            currentAlert = null;
                            NoRVApi.RouterModel router = (NoRVApi.RouterModel) ssidInput.getSelectedItem();
                            if(router == null)
                                return;
                            if (pwdInput.getText().toString().isEmpty())
                                return;
                            NoRVApi.getInstance().joinRouter(router.ssid, router.mac, router.channel, router.caps, pwdInput.getText().toString(), new NoRVApi.RouterListener() {
                                @Override
                                public void onSuccess(ArrayList<NoRVApi.RouterModel> routers) {
                                    Log.e("NoRV Internet Join", "Success");
                                }

                                @Override
                                public void onFailure(String errorMsg) {
                                    Log.e("NoRV Internet Join", errorMsg);
                                    stopAnimation();
                                }
                            });
                        });

                        alert.setNegativeButton("Cancel", (dialog, whichButton) -> {
                            currentAlert = null;
                            stopAnimation();
                        });

                        currentAlert = alert.show();
                    }

                    @Override
                    public void onFailure(String errorMsg) {
                        Log.e("NoRV Internet Scan", errorMsg);
                        stopAnimation();
                    }
                });
            }

            @Override
            public void onFailure(String errorMsg) {
                Log.e("NoRV Internet Login", errorMsg);
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove(NoRVConst.Router_PWD_Key);
                editor.apply();
                stopAnimation();
            }
        });
    }


    final Handler handler = new Handler(Looper.getMainLooper());
    boolean isRunning = true;
    final Runnable checkRouterLive = new Runnable() {
        @Override
        public void run() {
            WifiManager wifiManager = (WifiManager) NoRVInternet.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
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
        if(animTick >= 0) {
            animTick ++;
            if(animTick > 30) {
                stopAnimation();
            }
        }
        NoRVApi.getInstance().checkRouterInternet(new NoRVApi.RouterListener() {
            @Override
            public void onSuccess(ArrayList<NoRVApi.RouterModel> routers) {
                gotoHomeScreen();
            }

            @Override
            public void onFailure(String errorMsg) {
                if (!isRunning)
                    return;
                Log.e("NoRV Internet", errorMsg);
                handler.postDelayed(checkRouterLive, NoRVConst.CheckRouterLiveInterval);
            }
        });
    };

    private void gotoHomeScreen() {
        Intent activityIntent = new Intent(NoRVInternet.this, NoRVActivity.class);
        startActivity(activityIntent);
        finish();
    }

    private void gotoConnectScreen() {
        Intent connectIntent = new Intent(NoRVInternet.this, NoRVConnect.class);
        startActivity(connectIntent);
        finish();
    }

    @Override
    protected void onPause() {
        isRunning = false;
        handler.removeCallbacks(checkRouterLive);
        handler.removeCallbacks(checkRouterInternet);

        if(currentAlert != null)
            currentAlert.dismiss();
        super.onPause();
    }

    @Override
    protected void onResume() {
        isRunning = true;
        handler.postDelayed(checkRouterLive, NoRVConst.CheckRouterLiveInterval);

        currentAlert = null;

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
}
