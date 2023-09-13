package com.harshana.wposandroiposapp.UI.Settings;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.Database.DBHelperTransaction;
import com.harshana.wposandroiposapp.DevTools.PushPullTest;
import com.harshana.wposandroiposapp.KeyOperations.PINKeyInject;
import com.harshana.wposandroiposapp.KeyOperations.TLEKeyDownload;
import com.harshana.wposandroiposapp.MainActivity;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.Settings.TableConfigActivity;
import com.harshana.wposandroiposapp.Base.Keys;

public class SettingsHomeActivity extends AppCompatActivity implements View.OnClickListener {


    private Button tleButton, eraseKeyButton, pinButton, pushTestButton, editTableButton, setMerchantPwButton, clearPendingQR;
    DBHelperTransaction dbHelperTransaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_home);

        dbHelperTransaction = DBHelperTransaction.getInstance(this);

        tleButton = findViewById(R.id.tleButton);
        tleButton.setOnClickListener(this);
        pinButton = findViewById(R.id.pinButton);
        pinButton.setOnClickListener(this);
        pushTestButton = findViewById(R.id.pushTestButton);
        pushTestButton.setOnClickListener(this);
        eraseKeyButton = findViewById(R.id.eraseKeyButton);
        eraseKeyButton.setOnClickListener(this);
        editTableButton = findViewById(R.id.editTableButton);
        editTableButton.setOnClickListener(this);
        setMerchantPwButton = findViewById(R.id.setMerchantPwButton);
        setMerchantPwButton.setOnClickListener(this);
        clearPendingQR = findViewById(R.id.pinQR);
        clearPendingQR.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tleButton:
                Intent tleKeydownload = new Intent(this, TLEKeyDownload.class);
                startActivity(tleKeydownload);
                break;
            case R.id.pinButton:
                Intent pinKeyInjection = new Intent(this, PINKeyInject.class);
                startActivity(pinKeyInjection);
                break;
            case R.id.pushTestButton:
                Intent intentPushPullTest = new Intent(this, PushPullTest.class);
                startActivity(intentPushPullTest);
                break;
            case R.id.eraseKeyButton:
                AlertDialog.Builder alertKey =  new AlertDialog.Builder(SettingsHomeActivity.this);
                alertKey.setTitle("Confirm Your Action");
                alertKey.setMessage("Do you want to clear all keys?");
                alertKey.setPositiveButton("Yes",dialogClickListenerKey);
                alertKey.setNegativeButton("No",dialogClickListenerKey);
                alertKey.setCancelable(false);
                alertKey.show();
                break;
            case R.id.editTableButton:
                Intent intentTableEdit = new Intent(this, TableConfigActivity.class);
                startActivity(intentTableEdit);
                break;
            case R.id.setMerchantPwButton:
                Intent merchantPw = new Intent(this, SetMerchantPwActivity.class);
                startActivity(merchantPw);
                break;
            case R.id.pinQR:
                AlertDialog.Builder alert =  new AlertDialog.Builder(SettingsHomeActivity.this);
                alert.setTitle("Confirm Your Action");
                alert.setMessage("Do you want to clear pending QR?");
                alert.setPositiveButton("Yes",dialogClickListener);
                alert.setNegativeButton("No",dialogClickListener);
                alert.setCancelable(false);
                alert.show();
                break;
        }
    }

    DialogInterface.OnClickListener dialogClickListener =  new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    dbHelperTransaction.executeCustomQuary("DELETE FROM QR");
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        }
    };

    DialogInterface.OnClickListener dialogClickListenerKey =  new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    Keys.erase();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        }
    };

}