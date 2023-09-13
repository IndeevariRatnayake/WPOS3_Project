package com.harshana.wposandroiposapp.UI.Reports;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.Toast;

import com.harshana.wposandroiposapp.Database.DBHelperTransaction;
import com.harshana.wposandroiposapp.Print.Receipt;
import com.harshana.wposandroiposapp.R;

public class QRReportActivity extends AppCompatActivity {
    DBHelperTransaction transactionDB;
    Button btnCancel;
    Button btnPrint;
    RadioGroup radioGroup;
    RadioButton radioDetailReport;
    RadioButton radioButtonSummery;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrreport);

        transactionDB = DBHelperTransaction.getInstance(this);

        btnCancel = findViewById(R.id.btnCancelQR);
        btnPrint = findViewById(R.id.btnPrintQR);

        btnCancel.setOnClickListener(clickListener);
        btnPrint.setOnClickListener(clickListener);

        radioGroup = findViewById(R.id.radioGroupQR);
        radioDetailReport = findViewById(R.id.rdDetailReportQR);
        radioButtonSummery = findViewById(R.id.rdSummeryReportQR);
    }

    @Override
    public void onBackPressed()
    {

    }


    boolean isPrinting = false;

    private int DETAIL_REPORT = 1;
    private int SUMMERY_REPORT = 2;

    int reportType = 0;
    View.OnClickListener clickListener =  new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnCancel) {
                if (isPrinting) {
                    showToast("Please wait until the printing is finished");
                    return;
                }

                finish();
            }
            else if (v == btnPrint) {
                if (isPrinting) {
                    showToast("Please wait until the printing is finished");
                    return;
                }

                String message  = null;
                reportType = DETAIL_REPORT;  //default report type
                int selectedID = radioGroup.getCheckedRadioButtonId();
                RadioButton radioButton = null;

                radioButton = findViewById(selectedID);

                if (radioButton == radioDetailReport)
                    reportType = DETAIL_REPORT;
                else if (radioButton == radioButtonSummery)
                    reportType = SUMMERY_REPORT;

                if (reportType == DETAIL_REPORT)
                    message = "Detail Report";
                else if (reportType == SUMMERY_REPORT)
                    message = "Summery Report";

                progressDialog = new ProgressDialog(QRReportActivity.this);
                progressDialog.setTitle(message);
                progressDialog.setMessage("Printing...");
                progressDialog.setCancelable(false);
                progressDialog.setCanceledOnTouchOutside(false);
                progressDialog.show();

                //run the detail report printing activity in a separate thread
                Thread detPrintThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Receipt rcpt = Receipt.getInstance();
                        if (reportType == DETAIL_REPORT)
                            rcpt.printDetailReportQR();
                        else if (reportType == SUMMERY_REPORT)
                            rcpt.printSummeryReportQR();

                        finishDetailReport();
                    }
                });
                isPrinting = true;
                detPrintThread.start();
            }
        }
    };

    ProgressDialog progressDialog;

    void finishDetailReport() {
        isPrinting = false;
        progressDialog.dismiss();
    }

    Toast showToastMessage;
    void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(QRReportActivity.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }
}