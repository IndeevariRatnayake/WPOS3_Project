package com.harshana.wposandroiposapp.UI.Users;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.harshana.wposandroiposapp.DevArea.GlobalData;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.Security.PasswordManager;
import com.harshana.wposandroiposapp.Settings.Preferences;

public class AdminLoginActivity extends AppCompatActivity implements View.OnClickListener {
    Context appContext;
    private Button adminLoginButton, existButton;
    private EditText adminPwEditTextText;
    private Preferences pref;
    private ProgressBar bar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appContext = this;
        setContentView(R.layout.activity_admin_login);

        pref = Preferences.getInstance(this);
        adminPwEditTextText = findViewById(R.id.adminPwEditTextText);
        adminLoginButton = findViewById(R.id.adminLoginButton);
        adminLoginButton.setOnClickListener(this);
        existButton = findViewById(R.id.existButton);
        existButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.adminLoginButton:
                String pw = adminPwEditTextText.getText().toString();

                if (pw.trim().isEmpty()) {
                    showAlert(this, "Please enter a valid password");
                } else if (pw.equalsIgnoreCase(PasswordManager.SUPERVISOR_PASSWORD)) {
                    try {
                        pref.saveSetting(GlobalData.USER_ROLE, GlobalData.ADMIN);
                        finish();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    showAlert(this, "Please enter a valid password");
                }
                break;

            case R.id.existButton:
                finish();
                break;
        }
    }

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