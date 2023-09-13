package com.harshana.wposandroiposapp.DevTools;


//import android.support.v7.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.harshana.wposandroiposapp.MainActivity;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.Settings.ConfigSynchronize;
import com.harshana.wposandroiposapp.Settings.ProfileVerifier.ProfileVerifier;


public class PushPullTest extends AppCompatActivity
{
    Button btnDownloadSettings = null;
    Button btnPushTransactions = null;
    Button btnRunVerifier = null;

    TextView txtStatus  = null;

    ConfigSynchronize configSynchronize;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_push_pull_test);

        //capturePassword(PushPullTest.this);

        btnDownloadSettings = findViewById(R.id.btnDownloadSettings);
        btnPushTransactions = findViewById(R.id.btnPushTrans);
        btnRunVerifier = findViewById(R.id.btnRunVerifier);

        txtStatus = findViewById(R.id.txtStatus);
        configSynchronize = ConfigSynchronize.getInstance(getApplicationContext(), (Activity)MainActivity.myContext);

        btnDownloadSettings.setOnClickListener(clickListener);
        btnPushTransactions.setOnClickListener(clickListener);
        btnRunVerifier.setOnClickListener(clickListener);
    }

    View.OnClickListener clickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            if (v == btnDownloadSettings)
            {
                txtStatus.setText("Downloading started...");
                btnDownloadSettings.setEnabled(false);
                configSynchronize.downloadSettingsManual();
                btnDownloadSettings.setEnabled(true);
                txtStatus.setText("Done");
            }
            else if (v == btnPushTransactions)
            {
                txtStatus.setText("Started Pushing..");
                btnPushTransactions.setEnabled(false);
                int numTrans  = configSynchronize.pushTransactionsManual();
                btnPushTransactions.setEnabled(true);
                if (numTrans > 0)
                    txtStatus.setText(String.valueOf(numTrans) + " Transactions Pushed");
                else
                    txtStatus.setText("Nothing to push");
            }
            else if (v == btnRunVerifier)
            {
                ProfileVerifier verifier = ProfileVerifier.getInstance(getApplicationContext());
                verifier.performVerification();
                Toast.makeText(PushPullTest.this,"Please check the generated log",Toast.LENGTH_SHORT).show();
            }
        }
    };
}
