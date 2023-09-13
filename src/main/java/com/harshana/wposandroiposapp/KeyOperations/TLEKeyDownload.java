package com.harshana.wposandroiposapp.KeyOperations;

import android.content.DialogInterface;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.harshana.wposandroiposapp.Base.HostIssuer;
import com.harshana.wposandroiposapp.Base.IssuerHostMap;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.UI.Other.ActionBarLayout;
import com.harshana.wposandroiposapp.UI.Utils.ClearBatch;

import java.util.ArrayList;

//this is the tle key downloading
//routine which is used to download the tle key
public class TLEKeyDownload extends AppCompatActivity {
    ListView lvIssuers = null;
    Button btnKeyDownload = null;
    EditText pinText = null;
    Button btnClose = null;
    TextView txtStatus = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tle__key_download);

        lvIssuers = findViewById(R.id.lvIssuersKeyDownload);
        loadIssuers();

        pinText = findViewById(R.id.txtPIN);
        btnKeyDownload = findViewById(R.id.btnKeyDownload);
        btnClose = findViewById(R.id.btnClose);
        btnClose.setOnClickListener(clickListener);

        btnKeyDownload.setOnClickListener(clickListener);
        lvIssuers.setOnItemClickListener(itemClickListener);

        txtStatus = findViewById(R.id.txtStatus);

        Typeface tp = Typeface.createFromAsset(getAssets(),"digital_font.ttf");
        txtStatus.setTypeface(tp);
        pinText.setTypeface(tp);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable( new ColorDrawable(getResources().getColor(R.color.colorBlack)));

        ActionBarLayout actionBarLayout = ActionBarLayout.getInstance(this,getResources().getString(R.string.app_name),getResources().getColor(R.color.colorBlack));
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(actionBarLayout.createAndGetActionbarLayoutEx());
    }

    ArrayList issuerList;
    ArrayAdapter issuerAdapter;

    int selected_host = -1;

    String pin = "";
    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnKeyDownload) {
                pin = pinText.getText().toString();

                if (pin == null || pin.length() == 0) {
                    showToast("Please provide the pin to proceed");
                    return;
                }
                else if (selected_host < 0) {
                    showToast("Please make a selection before proceed");
                    return;
                }

                //perform the key download process in a new thread
                Thread tleThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        com.harshana.wposandroiposapp.TLE.TLEKeyDownload tleKeyDownload = new com.harshana.wposandroiposapp.TLE.TLEKeyDownload();
                        tleKeyDownload.setOnTLEKeyProcessResult(new com.harshana.wposandroiposapp.TLE.TLEKeyDownload.OnTLEKeyProcessResult() {
                            @Override
                            public void onTleKeyProcessResult(final String result,final int code) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        txtStatus.setText(result);
                                        try {
                                            Thread.sleep(100);
                                        } catch (Exception ex){}
                                    }
                                });
                            }
                        });
                        tleKeyDownload.performTLEKeyExchange(pin,selected_host);
                    }
                });

                tleThread.start();
            }
            else if (v == btnClose) {
                AlertDialog.Builder alert =  new AlertDialog.Builder(TLEKeyDownload.this);
                alert.setTitle("Confirm Your Action");
                alert.setMessage("Do you really want to exit from key download?");
                alert.setPositiveButton("Yes",dialogClickListener);
                alert.setNegativeButton("No",dialogClickListener);
                alert.setCancelable(false);
                alert.show();
            }
        }

        DialogInterface.OnClickListener dialogClickListener =  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        //clicked yes
                        finish();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };
    };

    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selected_host = position;
        }
    };

    Toast showToastMessage;
    void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(TLEKeyDownload.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }

    void loadIssuers() {
        issuerList =  new ArrayList<>();
        issuerAdapter =  new ArrayAdapter(this,android.R.layout.simple_expandable_list_item_1,issuerList);
        lvIssuers.setAdapter(issuerAdapter);

        for (int hostIndex = 0; hostIndex < IssuerHostMap.numHostEntries; hostIndex++)
        {
            HostIssuer hostIssuer =  IssuerHostMap.hosts[hostIndex];
            issuerList.add(hostIssuer.hostName);
        }

        issuerAdapter.notifyDataSetChanged();
    }



}
