package com.harshana.wposandroiposapp.UI.Reports;

import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.harshana.wposandroiposapp.Base.HostIssuer;
import com.harshana.wposandroiposapp.Base.IssuerHostMap;
import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.Print.Receipt;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.UI.Fregments.HostMerchantSelectFragment;
import com.harshana.wposandroiposapp.UI.Other.ActionBarLayout;

import java.util.ArrayList;

public class HostReport extends AppCompatActivity implements HostMerchantSelectFragment.OnFragmentInteractionListener{
    TextView txtInfo ;
    Button btnPrint;
    Button btnClose;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_report);

        Fragment fragment = new HostMerchantSelectFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction  = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.placeHolder,fragment);
        fragmentTransaction.commit();

        ((HostMerchantSelectFragment) fragment).setOnSelectionMadeListener(selectionMade);

        txtInfo = findViewById(R.id.info);
        btnPrint = findViewById(R.id.btnPrint);
        btnClose = findViewById(R.id.btnClose);

        btnPrint.setOnClickListener(onClickListener);
        btnClose.setOnClickListener(onClickListener);

        Typeface tp = Typeface.createFromAsset(getAssets(),"digital_font.ttf");
        txtInfo.setTypeface(tp);

        loadAndPopulateHostParameters(0,1);
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    public void onFragmentInteraction(Uri uri) {
    }

    int selectedHost =  - 1;
    int selectedMerchant = -1;

    HostMerchantSelectFragment.OnSelectionMade selectionMade = new HostMerchantSelectFragment.OnSelectionMade() {
        @Override
        public void onSelectionMadeNotify(int hostSelected, int merchantSelected) {
            selectedHost = hostSelected;
            selectedMerchant = merchantSelected;

            loadAndPopulateHostParameters(selectedHost,selectedMerchant);
        }
    };

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnPrint) {
                startPrint();
                try {
                    Receipt rcpt = Receipt.getInstance();
                    if (selectedHost > -1)
                        rcpt.printHostParameters(selectedHost, selectedMerchant);
                    stopPrint();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if (v == btnClose)
                finish();
        }
    };

    private void startPrint() {
        btnPrint.setClickable(false);
        btnPrint.setEnabled(false);
        btnClose.setClickable(false);
        btnClose.setEnabled(false);
    }

    private void stopPrint() {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnPrint.setClickable(true);
                    btnPrint.setEnabled(true);
                    btnClose.setClickable(true);
                    btnClose.setEnabled(true);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAndPopulateHostParameters(int selected, int selectedMerchant) {
        Log.e("llll selected "," : "+selected);
        //get the base issuer
        int baseIssuer  = IssuerHostMap.hosts[selected].baseIssuer;
        Log.e("llll33333 selected "," : "+baseIssuer);
        String selectQuary = "SELECT * FROM IIT WHERE IssuerNumber = " + baseIssuer;

        Cursor rec = null;
        DBHelper configDB = DBHelper.getInstance(this);

        try {
            rec = configDB.readWithCustomQuary(selectQuary);
            if (rec == null || rec.getCount() == 0)
                return;
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        rec.moveToFirst();

        String hostName = IssuerHostMap.hosts[selected].hostName;
        String ip = rec.getString(rec.getColumnIndex("IP"));
        String port = String.valueOf(rec.getInt(rec.getColumnIndex("Port")));
        String NII = rec.getString(rec.getColumnIndex("NII"));
        String secureNII = rec.getString(rec.getColumnIndex("SecureNII"));

        rec.close();

        //get the tid and mid
        selectQuary = "SELECT MerchantID,TerminalID FROM TMIF,MIT WHERE TMIF.IssuerNumber = " + baseIssuer +
                " AND MIT.MerchantNumber = TMIF.MerchantNumber AND MIT.MerchantNumber = " + selectedMerchant;

        try {
            rec = configDB.readWithCustomQuary(selectQuary);
            if (rec == null || rec.getCount() == 0)
                return;
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        rec.moveToFirst();

        //get the first mid tid
        String tid = rec.getString(rec.getColumnIndex("TerminalID"));
        String mid = rec.getString(rec.getColumnIndex("MerchantID"));

        String info = "Host           :   " + hostName + "\n\n"+
                      "Terminal ID  :   " + tid + "\n\n"+
                      "Merchant ID :   " + mid + "\n\n" +
                      "IP                :   " + ip + "\n\n" +
                      "Port           :   " + port + "\n\n" +
                      "NII               :   " + NII + "\n\n" +
                      "Secure NII    :   " + secureNII;


        txtInfo.setText(info);
        rec.close();
    }
}