package com.harshana.wposandroiposapp.UI.Reports;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.Database.DBHelperTransaction;
import com.harshana.wposandroiposapp.Print.Receipt;
import com.harshana.wposandroiposapp.UI.Fregments.HostMerchantSelectFragment;
import com.harshana.wposandroiposapp.R;

public class AnyReceiptActivity extends AppCompatActivity implements HostMerchantSelectFragment.OnFragmentInteractionListener{
    DBHelper db;
    DBHelperTransaction dbTransaction;

    Button btnCancel;
    Button btnPrint;
    EditText txtInvoice;
    public static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_any_receipt);
        context = this;

        //replace the merchant select fragment on the activity
        Fragment fragment = new HostMerchantSelectFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction  = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.placeHolder,fragment);
        fragmentTransaction.commit();

        ((HostMerchantSelectFragment) fragment).setOnSelectionMadeListener(selectionMade);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        txtInvoice = findViewById(R.id.etInvoiceNumber);
        btnPrint = findViewById(R.id.btnConfirm_anyTran);
        btnCancel = findViewById(R.id.btnCancel_anyTran);

        db = DBHelper.getInstance(getApplicationContext());
        dbTransaction = DBHelperTransaction.getInstance(getApplicationContext());

        btnPrint.setOnClickListener(clickListener);
        btnCancel.setOnClickListener(clickListener);
    }

    HostMerchantSelectFragment.OnSelectionMade selectionMade = new HostMerchantSelectFragment.OnSelectionMade() {
        @Override
        public void onSelectionMadeNotify(int hostSelected, int merchantSelected) {
            selectedHost = hostSelected;
            selectedMerchant = merchantSelected;
        }
    };

    int selectedTranID = 0 ;
    int isAny=2;
    View.OnClickListener clickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View view) {
            if (view == btnPrint) {
                btnPrint.setClickable(false);
                btnPrint.setEnabled(false);
                btnCancel.setClickable(false);
                btnCancel.setEnabled(false);
                loadAnyTran();

                Thread detPrintThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Receipt rcpt = Receipt.getInstance();
                            rcpt.printAnyReceipt(selectedTranID, isAny);
                            finishAnyReceipt();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                isPrinting = true;
                detPrintThread.start();
            }
            else if (view == btnCancel) {
                finish();
            }
        }
    };

    boolean isPrinting = false;

    void finishAnyReceipt() {
        isPrinting = false;
        finish();
    }

    int invoice = 0;
    boolean okToConfirm = false;
    int selectedHost =  - 1;
    int selectedMerchant = -1;

    //load a specific  transaction from the database
    int loadAnyReceiptTran(int issuerId,int merchNum,int invoiceNum) {
        int id;
        String tidQuery = "SELECT * FROM TMIF WHERE MerchantNumber = " + merchNum;
        Cursor tidCur = db.readWithCustomQuary(tidQuery);
        if(tidCur.getCount() == 0) {
            return -1;
        }

        tidCur.moveToFirst();
        String tid = tidCur.getString(tidCur.getColumnIndex("TerminalID"));

        String quary = "SELECT * FROM TXN WHERE Host = " + issuerId + " AND InvoiceNumber = " + invoiceNum + " AND TerminalID = '" + tid + "'";
        Cursor anyTran  = dbTransaction.readWithCustomQuary(quary);
        if (anyTran.getCount() == 0 ) {
            return -1;
        }

        anyTran.moveToFirst();
        id = anyTran.getInt(anyTran.getColumnIndex("ID"));
        return id;
    }

    Toast showToastMessage;
    void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(AnyReceiptActivity.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }

    private void loadAnyTran() {
        if (txtInvoice.getText().length() == 0) {
            showToast("Please Enter a Valid Invoice Number");
            okToConfirm = false;
            return;
        }
        invoice =  Integer.valueOf(txtInvoice.getText().toString());

        if (selectedHost == -1 || selectedMerchant == -1) {
            okToConfirm = false;
            showToast("Please Select a Issuer and a Merchant to Proceed");
            return;
        }
        selectedTranID = loadAnyReceiptTran(selectedHost,selectedMerchant,invoice);
        if ( selectedTranID == -1)      {
            okToConfirm = false;
            showToast("There is no Transaction for the Invoice Number " + invoice);
            return;
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
