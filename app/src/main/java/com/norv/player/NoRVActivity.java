package com.norv.player;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class NoRVActivity extends AppCompatActivity {
    private static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST = 1998;
    private Menu optionsMenu = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.norv_activity);

//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//            startService(new Intent(MainActivity.this, FloatingViewService.class));
//        } else if (Settings.canDrawOverlays(this)) {
//            startService(new Intent(MainActivity.this, FloatingViewService.class));
//        } else {
//            askPermission();
//            return;
//        }
//        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            getMenuInflater().inflate(R.menu.norv_menu, menu);
            optionsMenu = menu;
        }
        return true;
}
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                askPermission();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST && optionsMenu != null && Settings.canDrawOverlays(this)) {
            optionsMenu.removeItem(R.id.search);
        }
    }

    private void askPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST);
    }


}
