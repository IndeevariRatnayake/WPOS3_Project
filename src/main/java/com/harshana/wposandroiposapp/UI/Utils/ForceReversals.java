package com.harshana.wposandroiposapp.UI.Utils;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.harshana.wposandroiposapp.Database.DBHelperTransaction;
import com.harshana.wposandroiposapp.MainActivity;
import com.harshana.wposandroiposapp.UI.Fregments.HostMerchantSelectFragment;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.UI.Other.ActionBarLayout;
import com.harshana.wposandroiposapp.Utilities.Formatter;

import static com.harshana.wposandroiposapp.Base.Base.VoidResults.VOID_COMM_FALIURE;
import static com.harshana.wposandroiposapp.Base.Base.VoidResults.VOID_FAILED;
import static com.harshana.wposandroiposapp.Base.Base.VoidResults.VOID_SUCCESS;

public class ForceReversals extends AppCompatActivity implements HostMerchantSelectFragment.OnFragmentInteractionListener {
    DBHelperTransaction dbTransactions  = null;
    Button sendReversal,btnClose;
    TextView txtInfo;
    boolean isReversalProcessing = false;
    ProgressDialog progressDialog;
    public static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_force_reversals);

        context = this;

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

        sendReversal = findViewById(R.id.btnSendReversal);
        btnClose = findViewById(R.id.btnRevClose);

        sendReversal.setOnClickListener(onClickListener);
        btnClose.setOnClickListener(onClickListener);

        txtInfo = findViewById(R.id.txtInfo);
        txtInfo.setTypeface(tp);
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isReversalProcessing) {
                showToast("Reversal is being processed, Please wait ");
                return;
            }

            if (v == sendReversal) {
                if (selectedMerchant  == -1) {
                    showToast("Please select  a merchant to continue");
                    return;
                }

                String searchQuary = "SELECT * FROM RVSL WHERE MerchantNumber = " + selectedMerchant;

                try {
                    Cursor rvslRecord = dbTransactions.readWithCustomQuary(searchQuary);
                    if (rvslRecord == null || rvslRecord.getCount() == 0) {
                        showToast("No Reversal Found");
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                sendForceReversal(selectedMerchant);
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
            showToastMessage = Toast.makeText(ForceReversals.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }
    int selectedHost = -1;
    int selectedMerchant = -1;

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    HostMerchantSelectFragment.OnSelectionMade selectionMade = new HostMerchantSelectFragment.OnSelectionMade() {
        @Override
        public void onSelectionMadeNotify(int hostSelected, int merchantSelected) {
            selectedHost = hostSelected;
            selectedMerchant = merchantSelected;

            if (!checkReversal(selectedMerchant)) {
                showToast("There is no reversal to clear");
                return;
            }
        }
    };

    private boolean checkReversal(int merchantNumber) {
        String searchQuary = "SELECT * FROM RVSL WHERE MerchantNumber = " + merchantNumber;

        try {
            Cursor rvslRecord = dbTransactions.readWithCustomQuary(searchQuary);
            if (rvslRecord == null || rvslRecord.getCount() == 0) {
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
        } catch (Exception ex){

            return false;
        }
    }

    private void sendForceReversal(int merchanNumber) {
        //we need to perform the void operation
        ForceRevThread revThread =  new ForceRevThread(merchanNumber);
        revThread.setOnRevFinished(new ForceRevThread.onRevFinished() {
            @Override
            public void onRevFinished(final int status) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (status == VOID_SUCCESS) {
                            showToast("Reversal Successful");
                            txtInfo.setText("No Reversal Found");
                        }
                        else if (status == VOID_COMM_FALIURE)
                            showToast("Communication Failure ");
                        else if (status == VOID_FAILED)
                            showToast("Reversal Request Declined");
                    }
                });

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                });

                isReversalProcessing = false;
            }
        });

        isReversalProcessing = true;
        revThread.start();
        isReversalProcessing = true;
        progressDialog = new ProgressDialog(ForceReversals.this);
        progressDialog.setTitle("Reversal");
        progressDialog.setMessage("Processing Online...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    @Override
    public void onBackPressed()
    {

    }
}

class ForceRevThread extends Thread {
    int merchNum = 0;

    public ForceRevThread(int id) {
        merchNum = id;
    }

    @Override
    public void run() {
        int status = MainActivity.applicationBase.pushForceReversal(merchNum);
        if (listener != null)
            listener.onRevFinished(status);
    }

    private onRevFinished listener = null;

    public interface  onRevFinished {
        void onRevFinished(int status);
    }

    public void setOnRevFinished(onRevFinished l) {
        listener = l;
    }
}