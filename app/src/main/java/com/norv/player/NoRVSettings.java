package com.norv.player;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

public class NoRVSettings extends AppCompatActivity {

    private static final int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST = 1998;
    SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.norv_settings);
        settingsFragment = new SettingsFragment(this);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, settingsFragment)
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        PreferenceManager.setDefaultValues(this, R.xml.settings_preferences, false);
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            settingsFragment.UpdatePermission(Settings.canDrawOverlays(this), true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private static final String PERMISSION_KEY = "permission_overlay";
        private static final EditTextPreference.OnBindEditTextListener numberOnly = new EditTextPreference.OnBindEditTextListener() {
            @Override
            public void onBindEditText(@NonNull EditText editText) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            }
        };
        NoRVSettings parent;

        SettingsFragment(NoRVSettings parent) {
            this.parent = parent;
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.settings_preferences, rootKey);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                UpdatePermission(true, false);
            } else {
                UpdatePermission(Settings.canDrawOverlays(parent), true);
                SwitchPreferenceCompat overlayPermissionPreference = findPreference(PERMISSION_KEY);
                if(overlayPermissionPreference != null) {
                    overlayPermissionPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object value) {
                            askPermission();
                            return false;
                        }
                    });
                }
            }

            EditTextPreference streamingWidthPreference = getPreferenceManager().findPreference("streaming_width");
            if(streamingWidthPreference != null)
                streamingWidthPreference.setOnBindEditTextListener(numberOnly);
            EditTextPreference streamingHeightPreference = getPreferenceManager().findPreference("streaming_height");
            if(streamingHeightPreference != null)
                streamingHeightPreference.setOnBindEditTextListener(numberOnly);
        }

        public void UpdatePermission(boolean checked, boolean enabled) {
            SwitchPreferenceCompat overlayPermissionPreference = findPreference(PERMISSION_KEY);
            if(overlayPermissionPreference != null) {
                overlayPermissionPreference.setChecked(checked);
                overlayPermissionPreference.setEnabled(enabled);
            }
        }

        private void askPermission() {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + parent.getPackageName()));
            parent.startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST);
        }
    }
}