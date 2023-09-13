package com.harshana.wposandroiposapp.Settings;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.UI.Other.ActionBarLayout;
import com.harshana.wposandroiposapp.UI.Utils.ClearBatch;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable( new ColorDrawable(getResources().getColor(R.color.colorBlack)));

        ActionBarLayout actionBarLayout = ActionBarLayout.getInstance(this,getResources().getString(R.string.app_name),getResources().getColor(R.color.colorBlack));
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(actionBarLayout.createAndGetActionbarLayoutEx());

        Fragment fragment =  new SettingsFragment();
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

        if (savedInstanceState == null) {//running for the first time
            fragmentTransaction.add(R.id.settingsLayout,fragment,"settings_page");
            fragmentTransaction.commit();
        } else {
            fragment = getFragmentManager().findFragmentByTag("settings_page");
        }

        onSharedPreferenceChangeListener =  new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                showToast( "Setting Changed");
                try {
                    Log.e("MENU KEY", " : " + key);
                    if (key.equalsIgnoreCase("is_pre_auth_enable")) {
                        boolean val = Preferences.getInstance(SettingsActivity.this).getBoolean("is_pre_auth_enable");
                        Log.e("VVVVVV ", "  :" + val);
                        Preferences.getInstance(SettingsActivity.this).saveBoolean(key, !val);
                    } else {
                        SettingsInterpreter.callSettingsChangedFunc(key, sharedPreferences.getBoolean(key, true));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
    }
    private SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = null;

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    Toast showToastMessage;
    void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(SettingsActivity.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }
}