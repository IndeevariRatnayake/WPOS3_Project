package com.harshana.wposandroiposapp.UI.Settings;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.harshana.wposandroiposapp.DevArea.GlobalData;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.Settings.Preferences;
import com.harshana.wposandroiposapp.UI.BatchTrans.SettlementActivity;
import com.harshana.wposandroiposapp.UI.BatchTrans.VoidActivity;
import com.harshana.wposandroiposapp.UI.Utils.ClearBatch;
import com.harshana.wposandroiposapp.UI.Utils.ForceReversals;

public class MerchantLoginActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView typeTextView;
    private String type;
    private EditText pwEditTextText;
    private Button setMerchantPwButton,backButton;
    private Preferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchant_login);

        type = getIntent().getExtras().getString("type");

        pref = Preferences.getInstance(this);
        setMerchantPwButton = findViewById(R.id.setMerchantPwButton);
        setMerchantPwButton.setOnClickListener(this);
        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(this);
        pwEditTextText = findViewById(R.id.pwEditTextText);
        typeTextView = findViewById(R.id.typeTextView);

        if (type.equalsIgnoreCase("void")) {
            typeTextView.setText("Void");
        }
        if (type.equalsIgnoreCase("settlement")) {
            typeTextView.setText("Settlement");
        }
        if (type.equalsIgnoreCase("clearBatch")) {
            typeTextView.setText("Clear Batch");
        }
        if (type.equalsIgnoreCase("forceReversal")) {
            typeTextView.setText("Force Reversal");
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.setMerchantPwButton:
                String pw = pwEditTextText.getText().toString();
                Log.e("Merchant pw ", " : " + pref.getSetting(GlobalData.MERCHANT));
                Log.e("Merchant pwe ", " : " + pw);

                if (pref.getSetting(GlobalData.MERCHANT) == null) {
                    showToast("Please set merchant password. Contact admin for more details");
                } else if (pw.trim().isEmpty()) {
                    showToast("Please enter Merchant password");
                } else if (pw.equalsIgnoreCase(pref.getSetting(GlobalData.MERCHANT))) {

                    if (type.equalsIgnoreCase("settlement")) {
                        Intent startSettleActivity = new Intent(this, SettlementActivity.class);
                        startActivity(startSettleActivity);
                        finish();
                    }
                    if (type.equalsIgnoreCase("void")) {
                        Intent voidActivity = new Intent(this, VoidActivity.class);
                        startActivity(voidActivity);
                        finish();
                    }
                    if (type.equalsIgnoreCase("clearBatch")) {
                        Intent startClearBatch = new Intent(this, ClearBatch.class);
                        startActivity(startClearBatch);
                        finish();
                    }
                    if (type.equalsIgnoreCase("forceReversal")) {
                        Intent forceRev = new Intent(this, ForceReversals.class);
                        startActivity(forceRev);
                        finish();
                    }
                } else {
                    showToast("Merchant password incorrect");
                }
                break;

            case R.id.backButton:
                finish();
                break;
        }
    }

    Toast showToastMessage;
    void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(MerchantLoginActivity.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }
}