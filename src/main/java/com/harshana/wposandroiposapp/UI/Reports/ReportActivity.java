package com.harshana.wposandroiposapp.UI.Reports;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.harshana.wposandroiposapp.Database.DBHelperTransaction;
import com.harshana.wposandroiposapp.DevArea.GlobalData;
import com.harshana.wposandroiposapp.MainActivity;
import com.harshana.wposandroiposapp.Print.Receipt;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.Settings.Preferences;

public class ReportActivity extends AppCompatActivity implements View.OnClickListener {

    private Button hostParameterButton, transactionReportButton, qrReportButton, lastReceiptButton, lastReceiptButtonQR, anyReceiptButton, anyReceiptButtonQR, lastSettlementButton, cardBrandButton,
            bankWiseButton, diagnosticButton, existButton;
    protected static DBHelperTransaction transactionDatabase = null;

    public static Context context;
    private LinearLayout btnArea;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        context = this;
        transactionDatabase = DBHelperTransaction.getInstance(getApplicationContext());

        hostParameterButton = findViewById(R.id.hostParameterButton);
        hostParameterButton.setOnClickListener(this);
        transactionReportButton = findViewById(R.id.transactionReportButton);
        transactionReportButton.setOnClickListener(this);
        qrReportButton = findViewById(R.id.qrReportButton);
        qrReportButton.setOnClickListener(this);
        lastReceiptButton = findViewById(R.id.lastReceipt);
        lastReceiptButton.setOnClickListener(this);
        anyReceiptButton = findViewById(R.id.anyReceipt);
        anyReceiptButton.setOnClickListener(this);
        lastReceiptButtonQR = findViewById(R.id.lastReceiptQR);
        lastReceiptButtonQR.setOnClickListener(this);
        anyReceiptButtonQR = findViewById(R.id.anyReceiptQR);
        anyReceiptButtonQR.setOnClickListener(this);
        lastSettlementButton = findViewById(R.id.lastSettlement);
        lastSettlementButton.setOnClickListener(this);
        cardBrandButton = findViewById(R.id.cardBrandButton);
        cardBrandButton.setOnClickListener(this);
        bankWiseButton = findViewById(R.id.bankWiseButton);
        bankWiseButton.setOnClickListener(this);
        diagnosticButton = findViewById(R.id.diagnosticButton);
        diagnosticButton.setOnClickListener(this);
        existButton = findViewById(R.id.existButton);
        existButton.setOnClickListener(this);
        btnArea = findViewById(R.id.layoutbutton);

        String role = Preferences.getInstance(this).getSetting(GlobalData.USER_ROLE);

        cardBrandButton.setVisibility(View.GONE);
        diagnosticButton.setVisibility(View.GONE);
        bankWiseButton.setVisibility(View.GONE);
        if (role.equalsIgnoreCase(GlobalData.ADMIN)) {
            cardBrandButton.setVisibility(View.VISIBLE);
            bankWiseButton.setVisibility(View.VISIBLE);
            diagnosticButton.setVisibility(View.VISIBLE);
        }
    }

    boolean isPrinting = false;
    int selectedTranID = 0 ;
    int selectedTranIDQR = 0 ;
    int isLast = 1;
    private long mLastClickTime = 0;

    @Override
    public void onClick(View view) {
        // Preventing multiple clicks, using threshold of 1 second
        if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
            return;
        }
        mLastClickTime = SystemClock.elapsedRealtime();

        switch (view.getId()) {

            case R.id.hostParameterButton:
                Intent intentHostParam = new Intent(this, HostReport.class);
                startActivity(intentHostParam);
                break;

            case R.id.transactionReportButton:
                Intent transReport = new Intent(this, TransactionReports.class);
                startActivity(transReport);
                break;

            case R.id.qrReportButton:
                Intent qrReport = new Intent(this, QRReportActivity.class);
                startActivity(qrReport);
                break;

            case R.id.lastReceipt:
                selectedTranID = loadLastReceiptTran();

                if(selectedTranID == -1) {
                    showToast("No transactions found");
                    break;
                }
                startLastReceipt();

                Thread detPrintThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Receipt rcpt = Receipt.getInstance();
                            rcpt.printAnyReceipt(selectedTranID, isLast);
                            finishLastReceipt();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                isPrinting = true;
                detPrintThread.start();
                break;

            case R.id.lastReceiptQR:
                selectedTranIDQR = loadLastReceiptTranQR();

                if(selectedTranIDQR == -1) {
                    showToast("No transactions found");
                    break;
                }
                startLastReceipt();

                Thread detPrintThreadQR = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Receipt rcpt = Receipt.getInstance();
                            rcpt.printAnyReceiptQR(selectedTranIDQR, isLast);
                            finishLastReceipt();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                isPrinting = true;
                detPrintThreadQR.start();
                break;

            case R.id.anyReceipt:
                try {
                    Intent anyReceipt = new Intent(this, AnyReceiptActivity.class);
                    startActivity(anyReceipt);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case R.id.anyReceiptQR:
                try {
                    Intent anyReceipt = new Intent(this, AnyReceiptQRActivity.class);
                    startActivity(anyReceipt);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case R.id.lastSettlement:
                Intent lastSettlement = new Intent(this, LastSettleActivity.class);
                startActivity(lastSettlement);
                break;

            case R.id.cardBrandButton:
                Intent cardBrand = new Intent(this, IssuerMerchantViseSaleReport.class);
                startActivity(cardBrand);
                break;

            case R.id.bankWiseButton:
                Intent bankWise = new Intent(this, BankViseSaleReport.class);
                startActivity(bankWise);
                break;

            case R.id.diagnosticButton:
                MainActivity.applicationBase.printDiagnosisReport();
                break;
            case R.id.existButton:
                finish();
                break;
        }
    }

    Toast showToastMessage;
    void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(ReportActivity.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }

    void finishLastReceipt() {
        isPrinting = false;
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    enableDisableView(btnArea, true);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void startLastReceipt() {
        enableDisableView(btnArea, false);
    }

    public static void enableDisableView(View view, boolean enabled) {
        view.setEnabled(enabled);
        if ( view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)view;

            for ( int idx = 0 ; idx < group.getChildCount() ; idx++ ) {
                enableDisableView(group.getChildAt(idx), enabled);
            }
        }
    }

    int loadLastReceiptTran() {
        int id;
        String quary = "SELECT * FROM TXN ORDER BY ID DESC LIMIT 1";
        Cursor anyTran  = transactionDatabase.readWithCustomQuary(quary);

        if (anyTran.getCount() == 0 ) {
            return -1;
        }

        anyTran.moveToFirst();
        id = anyTran.getInt(anyTran.getColumnIndex("ID"));
        return id;
    }

    int loadLastReceiptTranQR() {
        int id;
        String quary = "SELECT * FROM QRBatch ORDER BY ID DESC LIMIT 1";
        Cursor anyTran  = transactionDatabase.readWithCustomQuary(quary);

        if (anyTran.getCount() == 0 ) {
            return -1;
        }

        anyTran.moveToFirst();
        id = anyTran.getInt(anyTran.getColumnIndex("ID"));
        return id;
    }
}