package com.harshana.wposandroiposapp.UI.Settings;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.harshana.wposandroiposapp.DevArea.GlobalData;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.Settings.Preferences;

public class SetMerchantPwActivity extends AppCompatActivity implements View.OnClickListener {

    private Preferences pref;
    private EditText pwEditTextText, confirmPwEditTextText;
    private Button setMerchantPwButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_merchant_pw);
        pref = Preferences.getInstance(this);

        pwEditTextText = findViewById(R.id.pwEditTextText);
        confirmPwEditTextText = findViewById(R.id.confirmPwEditTextText);
        setMerchantPwButton = findViewById(R.id.setMerchantPwButton);
        setMerchantPwButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.setMerchantPwButton:

                String pw = pwEditTextText.getText().toString();
                String confirm = confirmPwEditTextText.getText().toString();

                if (pw == null) {
                    showAlert(this, "Please enter password");
                } else if (pw.isEmpty()) {
                    showAlert(this, "Please enter password");
                } else if (!pw.equals(confirm)) {
                    showAlert(this, "Password mismatch. Please check ");
                } else {
                    Log.e("PW", " : " + pw);
                    pref.saveSetting(GlobalData.MERCHANT, pw);
                    Toast.makeText(this, "Successfully set Merchant Password", Toast.LENGTH_SHORT).show();
                    finish();
                }

                break;
        }

    }

    /**
     * show message alert
     */
    public void showAlert(Context context, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setCancelable(false)
                .setTitle("Alert")
                .setMessage(message)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.show();
    }
}