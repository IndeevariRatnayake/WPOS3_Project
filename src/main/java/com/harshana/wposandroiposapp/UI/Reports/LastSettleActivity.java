package com.harshana.wposandroiposapp.UI.Reports;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.harshana.wposandroiposapp.Base.HostIssuer;
import com.harshana.wposandroiposapp.Base.IssuerHostMap;
import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.Database.DBHelperTransaction;
import com.harshana.wposandroiposapp.Print.Receipt;
import com.harshana.wposandroiposapp.UI.Fregments.HostMerchantSelectFragment;
import com.harshana.wposandroiposapp.UI.Fregments.IssuerItem;
import com.harshana.wposandroiposapp.UI.Fregments.MerchantItem;
import com.harshana.wposandroiposapp.UI.Other.ActionBarLayout;
import com.harshana.wposandroiposapp.UI.TemporyAdapter;
import com.harshana.wposandroiposapp.UI.TemporyAdapterMerchant;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.UI.Utils.ClearBatch;

import java.util.ArrayList;

public class LastSettleActivity extends AppCompatActivity implements HostMerchantSelectFragment.OnFragmentInteractionListener {
    DBHelper db;
    DBHelperTransaction dbTransaction;

    Button btnCancel;
    Button btnPrint;
    Fragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_last_settle);

        //replace the merchant select fragment on the activity
        fragment = new HostMerchantSelectFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction  = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.placeHolder,fragment);
        fragmentTransaction.commit();

        ((HostMerchantSelectFragment) fragment).setOnSelectionMadeListener(selectionMade);

        Typeface tp = Typeface.createFromAsset(getAssets(),"digital_font.ttf");

        db = DBHelper.getInstance(getApplicationContext());
        dbTransaction = DBHelperTransaction.getInstance(getApplicationContext());

        btnPrint = findViewById(R.id.btnLastSettle);
        btnCancel = findViewById(R.id.btnLastCancel);

        btnPrint.setOnClickListener(clickListener);
        btnCancel.setOnClickListener(clickListener);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable( new ColorDrawable(getResources().getColor(R.color.colorBlack)));

        ActionBarLayout actionBarLayout = ActionBarLayout.getInstance(this,getResources().getString(R.string.app_name),getResources().getColor(R.color.colorBlack));
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(actionBarLayout.createAndGetActionbarLayoutEx());
    }

    boolean isPrinting = false;
    int selectedTranID = 0 ;
    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view == btnPrint) {
                btnPrint.setEnabled(false);
                btnPrint.setClickable(false);
                btnCancel.setClickable(false);
                btnCancel.setEnabled(false);
                loadLastSettle();

                Receipt summery = Receipt.getInstance();
                summery.printLastReciptSettlement(selectedTranID);
                finishLastSettle();
            }
            else if (view == btnCancel) {
                finish();
            }
        }
    };

    void finishLastSettle() {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnCancel.setEnabled(true);
                    btnCancel.setClickable(true);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void finishDetailReport() {
        isPrinting = false;
    }

    boolean okToConfirm = false;

    //load a specific  transaction from the database
    int loadLastSettleID(int issuerId,int merchNum) {
        int id;
        String lastSettleQuery = "SELECT * FROM LASTSETTLEMENT WHERE MerchantNumber = " + merchNum;
        Cursor lastStlCur = dbTransaction.readWithCustomQuary(lastSettleQuery);
        if(lastStlCur.getCount() == 0) {
            return -1;
        }

        lastStlCur.moveToFirst();
        id = lastStlCur.getInt(lastStlCur.getColumnIndex("ID"));
        return id;
    }

    int selectedHost =  - 1;
    int selectedMerchant = -1;
    HostMerchantSelectFragment.OnSelectionMade selectionMade = new HostMerchantSelectFragment.OnSelectionMade() {
        @Override
        public void onSelectionMadeNotify(int hostSelected, int merchantSelected) {
            selectedHost = hostSelected;
            selectedMerchant = merchantSelected;
        }
    };

    @Override
    public void onFragmentInteraction(Uri uri) {
    }

    private void loadLastSettle() {
        if (selectedHost == -1 || selectedMerchant == -1) {
            okToConfirm = false;
            showToast("Please Select a Issuer and a Merchant to Proceed");
            return;
        }
        selectedTranID = loadLastSettleID(selectedHost,selectedMerchant);
        if ( selectedTranID == -1)      {
            okToConfirm = false;
            showToast("There is no Transaction for selected merchant");
            return;
        }
    }

    Toast showToastMessage;
    void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(LastSettleActivity.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }
}