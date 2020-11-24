package com.norv.remote;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.kloadingspin.KLoadingSpin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NoRVConnect extends AppCompatActivity {

    TextView connectButton;
    KLoadingSpin connectSpin;
    int animTick = -1;
    AlertDialog currentAlert = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.norv_connect);

        connectSpin = findViewById(R.id.norv_connect_spin);

        connectButton = findViewById(R.id.connect_button);
        connectButton.setOnClickListener(view -> {
            if (!permissionsGranted()) {
                if(animTick >= 0)
                    stopAnimation();
                return;
            }

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            String password = sharedPreferences.getString(NoRVConst.Router_PWD_Key, "");
            if (password == null || password.isEmpty()) {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setTitle("NoRV Remote");
                alert.setMessage("Input password for NoRV-Center Router");

                LinearLayout inputLayout = new LinearLayout(NoRVConnect.this);
                inputLayout.setOrientation(LinearLayout.VERTICAL);

                final EditText input = new EditText(NoRVConnect.this);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                inputLayout.addView(input);
                inputLayout.setPadding(25, 0, 25, 0);
                alert.setView(inputLayout);

                alert.setPositiveButton("Ok", (dialog, whichButton) -> {
                    currentAlert = null;
                    if (input.getText().toString().isEmpty()) {
                        stopAnimation();
                        return;
                    }
                    ConnectWifi(input.getText().toString());
                });

                alert.setNegativeButton("Cancel", (dialog, whichButton) -> {
                    currentAlert = null;
                    stopAnimation();
                });

                currentAlert = alert.show();
            } else {
                ConnectWifi(password);
            }
        });

        if (!permissionsGranted()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, NoRVConst.LOCATION_PERMISSION_GRANT);
        } else {
            startAnimation();
            new Handler(Looper.getMainLooper()).postDelayed(() -> connectButton.performClick(), 1000);
        }
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

    private Boolean permissionsGranted() {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NoRVConst.LOCATION_PERMISSION_GRANT) {
            for (int i = 0; i < permissions.length && i < grantResults.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) && grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    connectButton.performClick();
                }
            }
        }
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

    private void ConnectWifi(String password) {

        if(animTick < 0) {
            startAnimation();
        }

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + NoRVConst.Router_SSID + "\"";
        conf.preSharedKey = "\"" + password + "\"";

        WifiManager wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.addNetwork(conf);
        if(!wifiManager.isWifiEnabled() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent internetIntent = new Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY);
            internetIntent.putExtra("password", password);
            startActivityForResult(internetIntent, NoRVConst.INTERNET_PERMISSION_GRANT);
            return;
        }
        if(!wifiManager.isWifiEnabled())
            wifiManager.setWifiEnabled(true);
        ConnectToNetworkWEP(password);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NoRVConst.INTERNET_PERMISSION_GRANT) {
            WifiManager wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if(wifiManager.isWifiEnabled()) {
                String password = data.getStringExtra("password");
                ConnectToNetworkWEP(password);
            }
        }
    }

    public void ConnectToNetworkWEP(String password) {
        try {
            WifiManager wifiManager = (WifiManager) this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo cNet = wifiManager.getConnectionInfo();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
                boolean connected = false;
                for( WifiConfiguration i : list ) {
                    if(i.SSID != null && i.SSID.equals("\"" + NoRVConst.Router_SSID + "\"")) {
                        Log.e("NoRV Router Now", i.SSID);
                        if(cNet != null && cNet.getNetworkId() != -1)
                        {
                            wifiManager.disconnect();
                            if(cNet.getSSID() != null)
                                Log.e("NoRV Router Prev", cNet.getSSID());
                            wifiManager.removeNetwork(cNet.getNetworkId());
                        }
                        wifiManager.enableNetwork(i.networkId, true);
                        wifiManager.reconnect();
                        connected = true;
                        break;
                    }
                }
                if(connected) {
                    //WiFi Connection success, return true
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(NoRVConst.Router_PWD_Key, password);
                    editor.apply();
                    return;
                }
            }
        } catch (Exception ex) {
            System.out.println(Arrays.toString(ex.getStackTrace()));
        }

        stopAnimation();
    }

    private void gotoHomeScreen() {
        Intent activityIntent = new Intent(NoRVConnect.this, NoRVActivity.class);
        startActivity(activityIntent);
        finish();
    }

    final Handler handler = new Handler(Looper.getMainLooper());
    boolean isRunning = true;
    final Runnable checkRouterLive = new Runnable() {
        @Override
        public void run() {
            if(animTick >= 0) {
                animTick ++;
                if(animTick > NoRVConst.Animation_Max_Limit) {
                    stopAnimation();

                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.remove(NoRVConst.Router_PWD_Key);
                    editor.apply();
                }
            }
            WifiManager wifiManager = (WifiManager) NoRVConnect.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if(!wifiManager.isWifiEnabled()) {
                Log.e("NoRV Router Live", "Wifi is disabled");
                handler.postDelayed(checkRouterLive, NoRVConst.CheckRouterLiveInterval);
                return;
            }

            NoRVApi.getInstance().checkRouterLive(new NoRVApi.RouterListener() {
                @Override
                public void onSuccess(ArrayList<NoRVApi.RouterModel> routers) {
                    gotoHomeScreen();
                }

                @Override
                public void onFailure(String errorMsg) {
                    if(!isRunning)
                        return;
                    Log.e("NoRV Router Live", errorMsg);
                    handler.postDelayed(checkRouterLive, NoRVConst.CheckRouterLiveInterval);
                }
            });
        }
    };

    @Override
    protected void onPause() {
        isRunning = false;
        handler.removeCallbacks(checkRouterLive);

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
}
