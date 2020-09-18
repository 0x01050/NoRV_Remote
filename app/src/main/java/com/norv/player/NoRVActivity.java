package com.norv.player;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class NoRVActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        Log.e("NoRV", "Activity started");
        super.onCreate(savedInstanceState);
//        try
//        {
//            this.getSupportActionBar().hide();
//        }
//        catch (NullPointerException e){}
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

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.norv_menu, menu);
        return true;
}
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
            switch (item.getItemId())
            {
                case R.id.search:
                    Intent settingsIntent = new Intent(NoRVActivity.this, NoRVSettings.class);
                    startActivity(settingsIntent);
                    return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }



}
