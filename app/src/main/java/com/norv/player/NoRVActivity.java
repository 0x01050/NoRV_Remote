package com.norv.player;

import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class NoRVActivity extends AppCompatActivity {
    private static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST = 1998;
    private Menu optionsMenu = null;

    private EditText witnessName = null;
    private Spinner witnessType = null;
    private Spinner timezone = null;
    private EditText caseName = null;
    private Spinner counselFor = null;
    private EditText addressDeposition = null;
    private boolean keyboardOpened = false;

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
        addressDeposition = findViewById(R.id.activity_address);

        Button loadDeposition = findViewById(R.id.activity_load_deposition);
        loadDeposition.setOnClickListener(v -> {
            if(!Settings.canDrawOverlays(NoRVActivity.this)) {
                Toast.makeText(NoRVActivity.this, "You need System Alert Window Permission to do this", Toast.LENGTH_LONG).show();
                return;
            }

            Intent confirmIntent = new Intent(NoRVActivity.this, NoRVConfirm.class);

            if(TextUtils.isEmpty(witnessName.getText())) {
                witnessName.requestFocus();
                return;
            }
            confirmIntent.putExtra("Witness", witnessName.getText().toString());

            if("Select".equals(witnessType.getSelectedItem().toString())) {
                witnessType.requestFocus();
                return;
            }
            confirmIntent.putExtra("Template", witnessType.getSelectedItem().toString());

            if("Select".equals(timezone.getSelectedItem().toString())) {
                timezone.performClick();
                return;
            }
            confirmIntent.putExtra("TimeZone", timezone.getSelectedItem().toString());

            if(TextUtils.isEmpty(caseName.getText())) {
                caseName.requestFocus();
                return;
            }
            confirmIntent.putExtra("CaseName", caseName.getText().toString());

            if("Select".equals(counselFor.getSelectedItem().toString())) {
                counselFor.requestFocus();
                return;
            }
            confirmIntent.putExtra("Counsel", counselFor.getSelectedItem().toString());

            if(TextUtils.isEmpty(addressDeposition.getText())) {
                addressDeposition.requestFocus();
                return;
            }
            confirmIntent.putExtra("Address", addressDeposition.getText().toString());

            startActivity(confirmIntent);
            finish();
        });

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

    @Override
    protected void onResume() {
        hideSystemUI();
        super.onResume();
    }

    private void hideSystemUI() {
        try
        {
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
                this.getSupportActionBar().hide();
            } else {
                this.getSupportActionBar().show();
            }
        }
        catch (Exception e){}
    }
}
