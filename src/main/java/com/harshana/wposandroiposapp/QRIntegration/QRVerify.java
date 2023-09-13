package com.harshana.wposandroiposapp.QRIntegration;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.harshana.wposandroiposapp.Base.QRTran;
import com.harshana.wposandroiposapp.Database.DBHelperTransaction;
import com.harshana.wposandroiposapp.Print.Receipt;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.Settings.SettingsInterpreter;
import com.harshana.wposandroiposapp.UI.Other.ActionBarLayout;
import com.harshana.wposandroiposapp.Utilities.Formatter;
import com.harshana.wposandroiposapp.Utilities.Utility;

import net.glxn.qrgen.android.QRCode;

import org.json.JSONObject;

public class QRVerify extends AppCompatActivity {
    QRCoreLogic qrCoreLogic = null;
    ImageView qrImageView = null;
    TextView txtStatus = null;
    Button btnConfirm = null, btnClear = null;
    final static String STATUS_COMPLETE = "01";
    final static String STATUS_FAILED = "05";
    final static String STATUS_INCOMPLETE = "09";
    Button btnCancel = null;
    public static Context context;
    String merchantName = "WPOS QR";

    boolean isAvailable = false;

    ProgressDialog progressDialog = null;
    DBHelperTransaction dbHelperTransaction;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrverify);

        context = this;

        qrCoreLogic = QRCoreLogic.getInstance();
        qrCoreLogic.initQRCode();
        qrCoreLogic.prepareSSLContextForCustCert();

        dbHelperTransaction = DBHelperTransaction.getInstance(this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable( new ColorDrawable(getResources().getColor(R.color.colorBlack)));

        ActionBarLayout actionBarLayout = ActionBarLayout.getInstance(this, getResources().getString(R.string.app_name), getResources().getColor(R.color.colorBlack));
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(actionBarLayout.createAndGetActionbarLayoutEx());

        txtStatus = findViewById(R.id.txtStatusQRVerify);
        btnConfirm = findViewById(R.id.btnConfirmQRVerify);
        btnCancel = findViewById(R.id.btnCancelQRVerify);
        btnClear = findViewById(R.id.btnClearQRVerify);
        qrImageView = findViewById(R.id.imgQRCodeQRVerify);

        btnCancel.setOnClickListener(clickListener);
        btnConfirm.setOnClickListener(clickListener);
        btnClear.setOnClickListener(clickListener);

        isAvailable = checkForQRTran();

        Bitmap myBitmap = QRCode.from(qrTran.QRCode).withSize(400, 400).bitmap();
        qrImageView.setImageBitmap(myBitmap);
        updateStatusText("Last QR");
        dismissProgressDialog();

        qrCoreLogic.setOnStatusUpdate(new QRCoreLogic.onStatusUpdate() {
            @Override
            public void OnStatusUpdate(QRCoreLogic.Status status) {
                String statMessage = null;
                switch (status) {
                    case CONNECTING:
                        statMessage = "Connecting...";
                        break;

                    case CONNECTION_FAILED:
                        statMessage = "Connection Failed";
                        dismissProgressDialog();
                        break;

                    case RECIVING:
                        statMessage = "Receiving...";
                        break;

                    case RECIEVE_FAILED:
                        statMessage = "Receiving Failed";
                        dismissProgressDialog();
                        break;

                    case FAILED:
                        statMessage = "Request Failed";
                        dismissProgressDialog();
                        break;

                    case SUCCESS:
                        statMessage = "Success";
                        dismissProgressDialog();
                        break;
                }

                updateStatusText(statMessage);
            }
        });
    }

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnCancel)
                finish();
            else if (v == btnConfirm) {
                btnConfirm.setEnabled(false);
                Thread manualPollThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int status = poll();
                        if (status == -1) {
                            updateStatusText("Failed to receive");
                            enableConfirm(true);
                        } else if (status == 9) {
                            updateStatusText("Transaction incomplete");
                            enableConfirm(true);
                        }
                        else if (status == 1) {
                            updateStatusText("Completed");
                        }
                    }
                }
                );
                manualPollThread.start();
                try {
                    manualPollThread.join();
                }
                catch (Exception ex) {
                }
            }
            else if (v == btnClear) {
                deleteQRTable();
                finish();
            }
        }
    };

    QRTran qrTran;

    private boolean checkForQRTran() {
        String query = "SELECT * FROM QR";
        Cursor qr = dbHelperTransaction.readWithCustomQuary(query);
        if (qr.getCount() == 0)
            return false;

        qr.moveToFirst();

        qrTran = dbHelperTransaction.getQRTran(qr);

        return true;
    }

    private void decodeResponse(String response) {
        qrTranBatch = new QRTran();

        try {
            JSONObject jsonObject = new JSONObject(response);
            qrTranBatch.status = jsonObject.getString("TX_STATUS");
            if(qrTranBatch.status.equals(STATUS_COMPLETE)){
                qrTranBatch.mid = jsonObject.getString("MID");
                qrTranBatch.merchName = jsonObject.getString("MERC_NAME");
                qrTranBatch.terminalID = jsonObject.getString("TerminlaId");
                qrTranBatch.MCC = jsonObject.getString("MCC");
                qrTranBatch.cusMobile = jsonObject.getString("MOBILE_NUMBER");
                qrTranBatch.cardHolder = jsonObject.getString("CRD_HLDR");
                qrTranBatch.PAN = jsonObject.getString("FRM_CRD");
                qrTranBatch.trace = jsonObject.getString("TRACE");
                //qrTranBatch.refQRLabel = jsonObject.getString("REF_LABEL");
                qrTranBatch.txOrg = jsonObject.getString("TX_ORG");
                if(jsonObject.getInt("QR_TYPE") == 0) {
                    qrTranBatch.qrType = "DYNAMIC";
                }
                else {
                    qrTranBatch.qrType = "STATIC";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public QRTran qrTranBatch;
    private int poll() {
        String statusXML = qrCoreLogic.buildStatusMessage(qrTran.QRCode);

        Log.e("===>","======================= ++++++generatedQRCode " + qrTran.QRCode);
        String resp = qrCoreLogic.validateQR(qrTran.QRCode);
        decodeResponse(resp);
        String status = qrTranBatch.status;
        System.out.println("======================= ++++++status " + status);

        if (status == null) //receiving failed network failure
            return -1;
        else if (status.equals("05") || status.equals(STATUS_FAILED))
            return -1;
        else if (status.equals(STATUS_INCOMPLETE)) {
            updateStatusText("Transaction incomplete");
            return 9;
        } else if (status.equals(STATUS_COMPLETE)) {
            //print the receipt
            updateStatusText(("Transaction Complete"));
            final Receipt receipt = Receipt.getInstance();

            Thread printThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        qrTranBatch.tranQRAmount = qrTran.tranQRAmount;
                        qrTranBatch.refQRLabel = qrTran.refQRLabel;
                        qrTranBatch.Date = Formatter.getCurrentDate();
                        qrTranBatch.Time = Utility.getTimeFormatted(Formatter.getCurrentTimeFormatted());

                        dbHelperTransaction.writeQRTrantoBatch(qrTranBatch);
                        receipt.printReceiptQRBase(1, qrTranBatch);
                        finish();
                    } catch (Exception ex) {
                    }
                }
            });

            printThread.start();
            deleteQRTable();
            return 1;
        }

        return -1;

    }

    private void dismissProgressDialog() {
        if (progressDialog != null)
            progressDialog.dismiss();
    }

    private void updateStatusText(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus.setText(text);
            }
        });

    }

    private void enableConfirm(final boolean val) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnConfirm.setEnabled(val);
            }
        });

    }

    private void deleteQRTable () {
        String query = "DELETE FROM QR";

        dbHelperTransaction.executeCustomQuary(query);
    }
}