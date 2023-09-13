package com.harshana.wposandroiposapp.KeyOperations;


import android.content.DialogInterface;
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
import android.widget.Toast;

import com.harshana.wposandroiposapp.Base.HostIssuer;
import com.harshana.wposandroiposapp.Base.IssuerHostMap;
import com.harshana.wposandroiposapp.Base.Services;
import com.harshana.wposandroiposapp.Crypto.DESCrypto;
import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.R;

import com.harshana.wposandroiposapp.UI.Other.ActionBarLayout;
import com.harshana.wposandroiposapp.Utilities.Sounds;
import com.harshana.wposandroiposapp.Utilities.Utility;

import java.util.ArrayList;
import wangpos.sdk4.libkeymanagerbinder.Key;

import static com.harshana.wposandroiposapp.Base.Base.bankCard;

//this module employ to inject both parts of the injecting
public class PINKeyInject extends AppCompatActivity
{
    Button btnComp1;
    Button btnComp2;

    EditText txtComp1;
    EditText txtComp2;

    ListView lvHosts;
    Button btnInject;

    DBHelper configDatabase = null;
    Button btnExit ;

    private static Key key = Services.keys;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_key_inject);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable( new ColorDrawable(getResources().getColor(R.color.colorBlack)));

        ActionBarLayout actionBarLayout = ActionBarLayout.getInstance(this,getResources().getString(R.string.app_name),getResources().getColor(R.color.colorBlack));
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(actionBarLayout.createAndGetActionbarLayoutEx());


        btnComp1 = findViewById(R.id.cmp1Done);
        btnComp2 = findViewById(R.id.cmp2Done);

        txtComp1 = findViewById(R.id.cmp1);
        txtComp2 = findViewById(R.id.cmp2);

        lvHosts = findViewById(R.id.lvHosts);
        btnInject = findViewById(R.id.btnInject);

        btnExit = findViewById(R.id.btnExit);

        configDatabase = DBHelper.getInstance(getApplicationContext());
        loadHosts();

        btnComp1.setOnClickListener(clickListener);
        btnComp2.setOnClickListener(clickListener);
        btnInject.setOnClickListener(clickListener);
        btnExit.setOnClickListener(clickListener);

        lvHosts.setOnItemClickListener(itemClickListener);
    }

    String comp1 = "";
    String comp2 = "";


    ArrayList hostList;
    ArrayAdapter hostAdapter;


    //loading the existing hosts
    void loadHosts()
    {
        hostList =  new ArrayList<>();
        hostAdapter =  new ArrayAdapter(this,android.R.layout.simple_expandable_list_item_1, hostList);
        lvHosts.setAdapter(hostAdapter);

        for(HostIssuer host : IssuerHostMap.hosts)
            hostList.add(host.hostName);


        hostAdapter.notifyDataSetChanged();
    }

    private void showToast(String msg)
    {
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
    }

    boolean comp1Ok = false;
    boolean comp2Ok = false;

    String keyComponent1 = "";
    String keyComponent2 = "";

    byte[] keyBytes1 = null;
    byte[] keyBytes2 = null;

    byte[] keyBytes = null;

    DialogInterface.OnClickListener dialogClickListener  = new DialogInterface.OnClickListener()
    {
        @Override
        public void onClick(DialogInterface dialog, int which)
        {
            if (which == DialogInterface.BUTTON_POSITIVE)
                finish();

        }
    };

    View.OnClickListener clickListener =  new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            if (isInjecting)
            {
                showToast("Please wait while the injecting is finished");
                return;
            }
            else if (v == btnExit)
            {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(PINKeyInject.this);
                alertDialog.setCancelable(false);
                alertDialog.setMessage("Do you really want to exit from pin key injection operation?");
                alertDialog.setTitle("Confirm Your Action");
                alertDialog.setPositiveButton("Yes",dialogClickListener);
                alertDialog.setNegativeButton("No",dialogClickListener);
                alertDialog.show();
            }
            else if (v == btnComp1) {
                comp1 = txtComp1.getText().toString();
                if (comp1.length() != 32) {
                    showToast("Please key in a valid key comp1");
                    return;
                }

                btnComp1.setEnabled(false);
                txtComp1.setEnabled(false);
                comp1Ok = true;
            }
            else if (v == btnComp2) {
                comp2 = txtComp2.getText().toString();
                if (comp2.length() != 32) {
                    showToast("Please key in a valid key comp1");
                    return;
                }

                btnComp2.setEnabled(false);
                txtComp2.setEnabled(false);
                comp2Ok = true;
            }
            else if (v == btnInject) {
                //validate the inputs
                if (packageName == "") {
                    showToast("Please select a host to inject the PIN key");
                    return;
                }

                if ( !comp1Ok || !comp2Ok || packageName == "") {
                    showToast("Please re check the inputs ");
                    return;
                }

                //if all the inputs are correct then we inject
                keyComponent1 = comp1;
                keyComponent2 = comp2;

                keyBytes1 = Utility.hexStr2Byte(keyComponent1);
                keyBytes2 = Utility.hexStr2Byte(keyComponent2);

                //xor the  components
                keyBytes = Utility.xorArrays(keyBytes1,keyBytes2);
                String masterKey = Utility.byte2HexStr(keyBytes);

                keyBytes = Utility.hexStr2Byte(masterKey);

                //String terminalWorkingKey = "D7BD5EDC0C29E2CBB66665AE583C9D16"; //must be decrypted with the user given master key
                //String terminalWorkingKey = "EF07F2262AEE7ECCAE4895D8B5F2379E"; //Sent by Anupa
                String terminalWorkingKey = "DCF225D6FA8745D2DCF225D6FA8745D2";

                try {
                    keyBytes =  DESCrypto.decrypt3Des(terminalWorkingKey,masterKey);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                //overridden for testing
                //terminalWorkingKey = "174463711EC54E9A1DACA50C5C951D42";
                //keyBytes = Utility.hexStr2Byte(terminalWorkingKey);

                String clearTPK = Utility.byte2HexStr(keyBytes);

                Thread pinkeyeSetThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        byte [] certData = new byte[8];
                        byte[] checkVal  = new byte[1];
                        pinInjectResult = -1;

                        isInjecting = true;
                        //now we have the key which should be injected to the secure storage
                        try {
                            bankCard.breakOffCommand();
                            key.erasePED();
                            pinInjectResult = key.updateKeyWithAlgorithm(
                                    Key.KEY_REQUEST_PEK,
                                    0x00,
                                    Key.KEY_PROTECT_ZERO,
                                    certData,
                                    keyBytes,
                                    false,
                                    0x00,
                                    checkVal,
                                    packageName,
                                    1);

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }

                        isInjecting = false;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Sounds clip = Sounds.getInstance();
                                if (pinInjectResult == 0) {
                                    showToast("Injecting PIN key Successful");
                                }
                                else {
                                    showToast("Key injection failed");
                                }
                                btnInject.setText("Inject PIN Key");
                            }
                        });
                    }
                });

                btnInject.setText("Injecting...");
                pinkeyeSetThread.start();
            }
        }
    };

    boolean isInjecting = false;
    int pinInjectResult = -1;
    String packageName = "";

    AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            packageName = lvHosts.getItemAtPosition(position).toString();
        }
    };
}