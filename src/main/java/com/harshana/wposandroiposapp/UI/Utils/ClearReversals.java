package com.harshana.wposandroiposapp.UI.Utils;

import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.harshana.wposandroiposapp.Database.DBHelperTransaction;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.UI.Fregments.HostMerchantSelectFragment;
import com.harshana.wposandroiposapp.UI.Other.ActionBarLayout;
import com.harshana.wposandroiposapp.Utilities.Formatter;

public class ClearReversals extends AppCompatActivity implements HostMerchantSelectFragment.OnFragmentInteractionListener {
    DBHelperTransaction dbTransactions  = null;

    Button btnClearReversal,btnClose;
    TextView txtInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clear_reversals);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable( new ColorDrawable(getResources().getColor(R.color.colorBlack)));

        ActionBarLayout actionBarLayout = ActionBarLayout.getInstance(this,getResources().getString(R.string.app_name),getResources().getColor(R.color.colorBlack));
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(actionBarLayout.createAndGetActionbarLayoutEx());

        Typeface tp = Typeface.createFromAsset(getAssets(),"digital_font.ttf");

        //replace the merchant select fragment on the activity
        Fragment fragment = new HostMerchantSelectFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction  = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.placeHolder,fragment);
        fragmentTransaction.commit();

        ((HostMerchantSelectFragment) fragment).setOnSelectionMadeListener(selectionMade);

        dbTransactions = DBHelperTransaction.getInstance(this);

        btnClearReversal = findViewById(R.id.btnClearReversal);
        btnClose = findViewById(R.id.btnClose);

        btnClearReversal.setOnClickListener(onClickListener);
        btnClose.setOnClickListener(onClickListener);

        txtInfo = findViewById(R.id.txtInfo);
        txtInfo.setTypeface(tp);

    }

    View.OnClickListener onClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            if (v == btnClearReversal)
            {
                if (selectedMerchant  == -1)
                {
                    showToast("Please select  a merchant to continue");
                    return;
                }

                //clear the reversal
                if (clearReversal(selectedMerchant))
                {
                    showToast("Reversal cleared");
                    txtInfo.setText("REVERSAL TRANSACTION SUMMERY");
                }
                else {
                    showToast("Clear Reversal Failed");
                }
            }
            else if (v == btnClose) {
                finish();
            }
        }
    };

    Toast showToastMessage;
    void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(ClearReversals.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }
    int selectedHost = -1;
    int selectedMerchant = -1;


    @Override
    public void onFragmentInteraction(Uri uri)
    {

    }

    HostMerchantSelectFragment.OnSelectionMade selectionMade = new HostMerchantSelectFragment.OnSelectionMade()
    {
        @Override
        public void onSelectionMadeNotify(int hostSelected, int merchantSelected)
        {
            selectedHost = hostSelected;
            selectedMerchant = merchantSelected;

            if (!checkReversal(selectedMerchant))
            {
                showToast("There is no reversal to clear");
                return;
            }

        }
    };

    private boolean checkReversal(int merchantNumber)
    {
        String searchQuary = "SELECT * FROM RVSL WHERE MerchantNumber = " + merchantNumber;


        try
        {
            Cursor rvslRecord = dbTransactions.readWithCustomQuary(searchQuary);
            if (rvslRecord == null || rvslRecord.getCount() == 0)
               {
                   txtInfo.setText("");
                   return false;
               }

            rvslRecord.moveToFirst();

            //there is a reversal. so we populate on the view
            String invoiceNumber = rvslRecord.getString(rvslRecord.getColumnIndex("InvoiceNumber"));
            long amount = rvslRecord.getLong(rvslRecord.getColumnIndex("BaseTransactionAmount"));
            String strAmount = Formatter.formatAmount(amount,"Rs");
            String pan = rvslRecord.getString(rvslRecord.getColumnIndex("PAN"));
            pan = Formatter.maskPan(pan,"****NNNN****NNNN",'*');

            String msg = "Invoice Number      :  " + invoiceNumber + "\n\n" +
                         "Amount                :  " + strAmount + "\n\n" +
                         "PAN                     :  " + pan ;

            txtInfo.setText(msg);
            rvslRecord.close();
            return true;
        }catch (Exception ex){

            return false;
        }
    }

    private boolean  clearReversal(int merchanNumber)
    {
        String deleteQuary = "DELETE FROM RVSL WHERE MerchantNumber = " + merchanNumber;

        int retResult = 0 ;
        try
        {
            dbTransactions.executeCustomQuary(deleteQuary);
            return true;
        }catch (Exception ex)
        {
            return false;
        }
    }

    @Override
    public void onBackPressed()
    {

    }
}
