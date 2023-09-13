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

public class AnyReceiptQRActivity extends AppCompatActivity {
    DBHelper db;
    DBHelperTransaction dbTransaction;

    Button btnCancel;
    Button btnPrint;
    EditText refLable;
    public static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_any_receipt_qractivity);
        context = this;

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        refLable = findViewById(R.id.etRefNumber);
        btnPrint = findViewById(R.id.btnConfirm_anyTranQR);
        btnCancel = findViewById(R.id.btnCancel_anyTranQR);

        db = DBHelper.getInstance(getApplicationContext());
        dbTransaction = DBHelperTransaction.getInstance(getApplicationContext());

        btnPrint.setOnClickListener(clickListener);
        btnCancel.setOnClickListener(clickListener);
    }

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
                            rcpt.printAnyReceiptQR(selectedTranID, isAny);
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

    int ref = 0;
    boolean okToConfirm = false;

    //load a specific  transaction from the database
    int loadAnyReceiptTranQR(int refNum) {
        int id;
        String quary = "SELECT * FROM QRBatch WHERE REFQRLable = " + refNum + "'";
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
            showToastMessage = Toast.makeText(AnyReceiptQRActivity.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }

    private void loadAnyTran() {
        if (refLable.getText().length() == 0) {
            showToast("Please Enter a Valid Reference Number");
            okToConfirm = false;
            return;
        }
        ref =  Integer.valueOf(refLable.getText().toString());

        selectedTranID = loadAnyReceiptTranQR(ref);
        if ( selectedTranID == -1)      {
            okToConfirm = false;
            showToast("There is no Transaction for the Reference Number " + ref);
            return;
        }
    }
}
