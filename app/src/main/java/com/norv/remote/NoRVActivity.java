package com.norv.remote;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.kloadingspin.KLoadingSpin;
import com.loopj.android.http.RequestParams;

public class NoRVActivity extends AppCompatActivity {
    private static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST = 1998;
    private Menu optionsMenu = null;

    private EditText witnessName = null;
    private Spinner witnessType = null;
    private Spinner timezone = null;
    private EditText caseName = null;
    private Spinner counselFor = null;

    private EditText addressStreet = null;
    private EditText addressCity = null;
    private EditText addressState = null;
    private EditText addressZip = null;


    private boolean keyboardOpened = false;

    private KLoadingSpin loadingSpin = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.norv_activity);

        hideSystemUI();

        ConstraintLayout rootView = findViewById(R.id.norv_activity_layout);
        ScrollView scroll = findViewById(R.id.norv_activity_container);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect visibleRect = new Rect();
            rootView.getWindowVisibleDisplayFrame(visibleRect);
            int heightDiff = rootView.getRootView().getHeight() - visibleRect.height();
            rootView.setMaxHeight(visibleRect.height());
            if (heightDiff > 100) {
                if(!keyboardOpened) {
                    View focusView = NoRVActivity.this.getCurrentFocus();
                    if(focusView != null && focusView.getId() != R.id.activity_witnessName) {
                        scroll.post(() -> {
                            scroll.fullScroll(View.FOCUS_DOWN);
                            focusView.requestFocus();
                        });
                    }
                }
                keyboardOpened = true;
            } else {
                keyboardOpened = false;
            }
        });

        witnessName = findViewById(R.id.activity_witnessName);
        witnessType = findViewById(R.id.activity_witnessType);
        witnessType.setFocusable(true);
        timezone = findViewById(R.id.activity_timezone);
        caseName = findViewById(R.id.activity_casename);
        counselFor = findViewById(R.id.activity_counsel);

        addressStreet = findViewById(R.id.activity_address_street);
        addressCity = findViewById(R.id.activity_address_city);
        addressState = findViewById(R.id.activity_address_state);
        addressZip = findViewById(R.id.activity_address_zip);

        Button loadDeposition = findViewById(R.id.activity_load_deposition);
        loadDeposition.setOnClickListener(v -> {
            if(!Settings.canDrawOverlays(NoRVActivity.this)) {
                Toast.makeText(NoRVActivity.this, "You need System Alert Window Permission to do this", Toast.LENGTH_LONG).show();
                return;
            }


            RequestParams params = new RequestParams();
            if(TextUtils.isEmpty(witnessName.getText())) {
                witnessName.requestFocus();
                return;
            }
            params.add("Witness", witnessName.getText().toString());

            if("Select".equals(witnessType.getSelectedItem().toString())) {
                witnessType.requestFocus();
                return;
            }
            params.add("Template", witnessType.getSelectedItem().toString());

            if("Select".equals(timezone.getSelectedItem().toString())) {
                timezone.performClick();
                return;
            }
            params.add("TimeZone", timezone.getSelectedItem().toString());

            if(TextUtils.isEmpty(caseName.getText())) {
                caseName.requestFocus();
                return;
            }
            params.add("CaseName", ParseCaseName(caseName.getText().toString()));

            if("Select".equals(counselFor.getSelectedItem().toString())) {
                counselFor.requestFocus();
                return;
            }
            params.add("Counsel", counselFor.getSelectedItem().toString());

            String address = "";
            if(TextUtils.isEmpty(addressStreet.getText())) {
                addressStreet.requestFocus();
                return;
            }
            address += addressStreet.getText().toString();
            if(TextUtils.isEmpty(addressCity.getText())) {
                addressCity.requestFocus();
                return;
            }
            address += ", " + addressCity.getText().toString();
            if(TextUtils.isEmpty(addressState.getText())) {
                addressState.requestFocus();
                return;
            }
            address += ", " + addressState.getText().toString();
            if(TextUtils.isEmpty(addressZip.getText())) {
                addressZip.requestFocus();
                return;
            }
            address += ", " + addressZip.getText().toString();
            params.add("Address", address);

            hideKeyboard();
            hideSystemUI();
            loadingSpin.startAnimation();
            loadingSpin.setIsVisible(true);
            loadingSpin.setVisibility(View.VISIBLE);
            NoRVApi.getInstance().controlDeposition("loadDeposition", params, new NoRVApi.ControlListener() {
                @Override
                public void onSuccess(String respMsg) {
                }

                @Override
                public void onFailure(String errorMsg) {
                    loadingSpin.stopAnimation();
                    loadingSpin.setVisibility(View.INVISIBLE);
                    Toast.makeText(NoRVActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
        });

        loadingSpin = findViewById(R.id.norv_activity_load_spin);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(!Settings.canDrawOverlays(this)) {
            getMenuInflater().inflate(R.menu.norv_menu, menu);
            optionsMenu = menu;
        }
        return true;
}
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.search) {
            askPermission();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST && optionsMenu != null && Settings.canDrawOverlays(this)) {
            optionsMenu.removeItem(R.id.search);
            hideSystemUI();
        }
    }

    private void askPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST);
    }

    final Handler handler = new Handler(Looper.getMainLooper());
    final Runnable checkStatus = new Runnable() {
        @Override
        public void run() {
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
                            handler.postDelayed(checkStatus, NoRVConst.CheckStatusInterval);
                            break;
                    }
                    if ("True".equals(ignorable)) {
                        findViewById(R.id.activity_load_deposition).setEnabled(false);
                    } else {
                        findViewById(R.id.activity_load_deposition).setEnabled(true);
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

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(this);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void gotoConfirmScreen() {
        Intent confirmIntent = new Intent(NoRVActivity.this, NoRVConfirm.class);
        startActivity(confirmIntent);
        finish();
    }

    private void gotoRTMPScreen() {
        startService(new Intent(NoRVActivity.this, NoRVRTMP.class));
        finish();
    }

    private void gotoPauseScreen() {
        Intent pauseIntent = new Intent(NoRVActivity.this, NoRVPause.class);
        startActivity(pauseIntent);
        finish();
    }

    private static String ParseCaseName(String CaseName) {
        CaseName = ReplaceWithPattern(CaseName, " VS ");
        CaseName = ReplaceWithPattern(CaseName, " versus ");
        return CaseName;
    }
    private static String ReplaceWithPattern(String CaseName, String Pattern) {
        int index = containsIgnoreCase(CaseName, Pattern);
        if(index >= 0) {
            int length = Pattern.length();
            String find = CaseName.substring(index, index + length - 1);
            CaseName = CaseName.replace(find, find + ".");
        }
        return CaseName;
    }
    private static int containsIgnoreCase(String src, String what) {
        final int length = what.length();
        if (length == 0)
            return -1;
        final char firstLo = Character.toLowerCase(what.charAt(0));
        final char firstUp = Character.toUpperCase(what.charAt(0));

        for (int i = src.length() - length; i >= 0; i--) {
            final char ch = src.charAt(i);
            if (ch != firstLo && ch != firstUp)
                continue;
            if (src.regionMatches(true, i, what, 0, length))
                return i;
        }
        return -1;
    }
}
