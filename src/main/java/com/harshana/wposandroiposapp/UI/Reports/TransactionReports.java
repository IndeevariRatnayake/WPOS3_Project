package com.harshana.wposandroiposapp.UI.Reports;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.harshana.wposandroiposapp.Base.Base;
import com.harshana.wposandroiposapp.Base.Transaction;
import com.harshana.wposandroiposapp.Database.DBHelperTransaction;
import com.harshana.wposandroiposapp.Print.Receipt;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.UI.Fregments.HostMerchantSelectFragment;
import com.harshana.wposandroiposapp.Utilities.Formatter;

public class TransactionReports extends AppCompatActivity implements HostMerchantSelectFragment.OnFragmentInteractionListener {
    private TableLayout tableLayout;
    DBHelperTransaction transactionDB;

    private static float TEXT_SIZE  = 11f;

    Button btnCancel;
    Button btnPrint;

    RadioGroup radioGroup;
    RadioButton radioDetailReport;
    RadioButton radioButtonSummery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_report);

        Fragment fragment = new HostMerchantSelectFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction  = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.placeHolder,fragment);
        fragmentTransaction.commit();

        tableLayout = findViewById(R.id.tableLayout);
        transactionDB = DBHelperTransaction.getInstance(getApplicationContext());

        generateHeader();

        ((HostMerchantSelectFragment) fragment).setOnSelectionMadeListener(new HostMerchantSelectFragment.OnSelectionMade() {
            @Override
            public void onSelectionMadeNotify(int issuerSelected, int merchantSelected) {
                resetTableLayout();
                generateHeader();
                populateData(issuerSelected,merchantSelected);

                selectedIssuer = issuerSelected;
                getSelectedMerchant = merchantSelected;
            }
        });

        btnCancel = findViewById(R.id.btnCancel);
        btnPrint = findViewById(R.id.btnPrint);

        btnCancel.setOnClickListener(clickListener);
        btnPrint.setOnClickListener(clickListener);

        radioGroup = findViewById(R.id.radioGroup);
        radioDetailReport = findViewById(R.id.rdDetailReport);
        radioButtonSummery = findViewById(R.id.rdSummeryReport);
    }

    int selectedIssuer = -1;
    int getSelectedMerchant = -1;

    @Override
    public void onBackPressed() {

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

                if (getSelectedMerchant < 0) {
                    showToast("Please select a merchant to proceed");
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

                progressDialog = new ProgressDialog(TransactionReports.this);
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
                            rcpt.printDetailReport(selectedIssuer,getSelectedMerchant);
                        else if (reportType == SUMMERY_REPORT)
                            rcpt.printSummeryReport(selectedIssuer,getSelectedMerchant);

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

    void resetTableLayout()
    {
       tableLayout.removeAllViews();
    }

    Toast showToastMessage;
    public void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(TransactionReports.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }

    void populateData(int issuerNumber, int merchantNumber) {
        String selectQuary = "SELECT * FROM TXN WHERE MerchantNumber = " + merchantNumber;
        int rowColor = Color.BLACK;

        //loads the data in to the cursor
        Cursor reportData  = transactionDB.readWithCustomQuary(selectQuary);
        if (reportData == null || reportData.getCount() == 0) {
            showToast("No data to populate");
            return;
        }

        //reportData.moveToFirst();
        TableRow row = null;

        //add each row in to the table layout based on the order of the header
        while (reportData.moveToNext()) {
            row = new TableRow(this);

            Transaction currentTransaction = transactionDB.getTransaction(reportData); // get the current tran

            if (currentTransaction.isVoided == 1)
                rowColor = Color.RED;
            else
                rowColor = Color.BLUE;

            currentTransaction = Base.loadCardAndIssuerToTransaction(currentTransaction,currentTransaction.PAN);

            if (currentTransaction == null) {
                showToast("There was issue loading transaction related data");
                return;
            }
            //add the card label
            TextView txtCardName = new TextView(this);
            txtCardName.setText(currentTransaction.cardData.cardLabel);
            txtCardName.setPadding(5,5,5,0);
            txtCardName.setTextSize(TEXT_SIZE);
            txtCardName.setTextColor(rowColor);
            row.addView(txtCardName);

            String amount = Formatter.formatAmount(currentTransaction.lnBaseTransactionAmount,"Rs");
            //fill the extra chars for future alignments
            amount  = Formatter.fillInBack(" ",amount,12);
            TextView txtAmount = new TextView(this);
            txtAmount.setText(amount);
            txtAmount.setPadding(5,5,5,0);
            txtAmount.setTextSize(TEXT_SIZE);
            txtAmount.setTextColor(rowColor);
            row.addView(txtAmount);

            String invoice = String.valueOf(currentTransaction.inInvoiceNumber);
            invoice = Formatter.fillInBack(" ",invoice,6);

            TextView txtInvoiceNumber = new TextView(this);
            txtInvoiceNumber.setText(invoice);
            txtInvoiceNumber.setPadding(5,5,5,0);
            txtInvoiceNumber.setTextSize(TEXT_SIZE);
            txtInvoiceNumber.setTextColor(rowColor);
            row.addView(txtInvoiceNumber);

            String pan = Formatter.maskPan(currentTransaction.PAN,"****NNNN****NNNN",'*');
            pan = Formatter.fillInBack(" ",pan,19);

            TextView txtCardNumber = new TextView(this);
            txtCardNumber.setText(pan);
            txtCardNumber.setPadding(5,5,5,0);
            txtCardNumber.setTextSize(TEXT_SIZE);
            txtCardNumber.setTextColor(rowColor);
            row.addView(txtCardNumber);

            TextView txtTranDate = new TextView(this);
            txtTranDate.setText(currentTransaction.Date);
            txtTranDate.setPadding(5,5,5,0);
            txtTranDate.setTextSize(TEXT_SIZE);
            txtTranDate.setTextColor(rowColor);
            row.addView(txtTranDate);

            TextView txtTime = new TextView(this);
            txtTime.setText(currentTransaction.Time.substring(0,currentTransaction.Time.length() - 2));
            txtTime.setPadding(5,5,5,0);
            txtTime.setTextSize(TEXT_SIZE);
            txtTime.setTextColor(rowColor);
            row.addView(txtTime);

            tableLayout.addView(row,new TableLayout.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,TableRow.LayoutParams.WRAP_CONTENT));
        }
    }

    //generates the columns and add to view
    void generateHeader() {
        //generate the columns
        TableRow headRow =  new TableRow(this);

        TextView txtCardName = new TextView(this);
        txtCardName.setText("CARD");
        txtCardName.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        txtCardName.setPadding(5,5,5,0);
        txtCardName.setTextSize(TEXT_SIZE);
        headRow.addView(txtCardName);

        TextView txtAmount = new TextView(this);
        txtAmount.setText("AMOUNT");
        txtAmount.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        txtAmount.setPadding(5,5,5,0);
        txtAmount.setTextSize(TEXT_SIZE);
        headRow.addView(txtAmount);

        TextView txtInvoiceNumber = new TextView(this);
        txtInvoiceNumber.setText("INV");
        txtInvoiceNumber.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        txtInvoiceNumber.setPadding(5,5,5,0);
        txtInvoiceNumber.setTextSize(TEXT_SIZE);
        headRow.addView(txtInvoiceNumber);

        TextView txtCardNumber = new TextView(this);
        txtCardNumber.setText("CARD NUM");
        txtCardNumber.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        txtCardNumber.setPadding(5,5,5,0);
        txtCardNumber.setTextSize(TEXT_SIZE);
        headRow.addView(txtCardNumber);

        TextView txtTranDate = new TextView(this);
        txtTranDate.setText("DATE");
        txtTranDate.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        txtTranDate.setPadding(5,5,5,0);
        txtTranDate.setTextSize(TEXT_SIZE);
        headRow.addView(txtTranDate);

        TextView txtTime = new TextView(this);
        txtTime.setText("TIME");
        txtTime.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        txtTime.setPadding(5,5,5,0);
        txtTime.setTextSize(TEXT_SIZE);
        headRow.addView(txtTime);

        tableLayout.addView(headRow,new TableLayout.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,TableRow.LayoutParams.WRAP_CONTENT));
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}