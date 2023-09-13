package com.harshana.wposandroiposapp.QRIntegration;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.harshana.wposandroiposapp.Base.QRTran;
import com.harshana.wposandroiposapp.Base.WaitTimer;
import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.Database.DBHelperTransaction;
import com.harshana.wposandroiposapp.DevArea.GlobalData;
import com.harshana.wposandroiposapp.MainActivity;
import com.harshana.wposandroiposapp.Print.Receipt;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.Settings.Preferences;
import com.harshana.wposandroiposapp.Settings.SettingsInterpreter;
import com.harshana.wposandroiposapp.UI.Other.ActionBarLayout;
import com.harshana.wposandroiposapp.Utilities.Formatter;
import com.harshana.wposandroiposapp.Utilities.Utility;

import net.glxn.qrgen.android.QRCode;

import org.json.JSONObject;

import static com.harshana.wposandroiposapp.QRIntegration.QRCoreLogic.MCC;
import static com.harshana.wposandroiposapp.QRIntegration.QRCoreLogic.MERC_NAME;
import static com.harshana.wposandroiposapp.QRIntegration.QRCoreLogic.MID;
import static com.harshana.wposandroiposapp.QRIntegration.QRCoreLogic.REF_LABEL;
import static com.harshana.wposandroiposapp.QRIntegration.QRCoreLogic.TX_AMT;
import static com.harshana.wposandroiposapp.QRIntegration.QRCoreLogic.TX_CURRENCY;
import static com.harshana.wposandroiposapp.QRIntegration.QRCoreLogic.TX_ORG;

public class QRDisplay extends AppCompatActivity {
    QRCoreLogic qrCoreLogic = null;
    ImageView qrImageView = null;
    TextView txtStatus = null;
    Button btnManualPoll = null;
    final static String STATUS_COMPLETE = "01";
    final static String STATUS_FAILED = "05";
    final static String STATUS_INCOMPLETE = "09";
    Button btnCancel = null;
    Preferences preferences = null;
    String generatedQRCode = "";
    Bitmap lastQRBitMap = null;
    int waitSeconds = 0;
    String merchantName = "";
    long tranAmount = 0;
    String refLabel = "";
    public static Context context;

    DBHelperTransaction dbHelperTransaction;
    DBHelper dbHelper;

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnCancel)
                finish();
            else if (v == btnManualPoll) {
                Thread manualPollThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        poll();
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
        }
    };

    String mid;

    ProgressDialog progressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_q_r_display);

        context = this;

        preferences = Preferences.getInstance(getApplicationContext());
        dbHelperTransaction = DBHelperTransaction.getInstance(this);
        dbHelper = DBHelper.getInstance(this);

        qrCoreLogic = QRCoreLogic.getInstance();
        qrCoreLogic.initQRCode();
        qrCoreLogic.prepareSSLContextForCustCert();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable( new ColorDrawable(getResources().getColor(R.color.colorBlack)));

        ActionBarLayout actionBarLayout = ActionBarLayout.getInstance(this, getResources().getString(R.string.app_name), getResources().getColor(R.color.colorBlack));
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(actionBarLayout.createAndGetActionbarLayoutEx());

        qrImageView = findViewById(R.id.imgQRCode);
        txtStatus = findViewById(R.id.txtStatus);
        btnManualPoll = findViewById(R.id.btnManualPoll);
        btnCancel = findViewById(R.id.btnCancel);
        btnManualPoll.setEnabled(false);

        btnCancel.setOnClickListener(clickListener);
        btnManualPoll.setOnClickListener(clickListener);

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

        //start the QR process
        String jsonString = buildGenDynamicQRXMLMessage(true);

        qrCoreLogic.setOnRecieveQRCode(new QRCoreLogic.onRecieveQRCode() {
            @Override
            public void OnRecieveQRCode(final String qrCode) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (qrCode == null) {
                            updateStatusText("Failed fetching QR!");
                            dismissProgressDialog();
                            return;
                        }

                        QRTran qrTran1 = new QRTran();
                        qrTran1.QRCode = qrCode;
                        qrTran1.refQRLabel = refLabel;
                        qrTran1.tranQRAmount = tranAmount;

                        dbHelperTransaction.writeQRTran(qrTran1);

                        Bitmap myBitmap = QRCode.from(qrCode).withSize(400, 400).bitmap();
                        qrImageView.setImageBitmap(myBitmap);
                        updateStatusText("Please Scan");
                        dismissProgressDialog();
                        generatedQRCode = qrCode;

                        //set the last qr bit map to printed later
                        qrImageView.setDrawingCacheEnabled(true);
                        lastQRBitMap = qrImageView.getDrawingCache();
                        startAutoTransactionPollingIn(40);
                    }
                });
            }
        });

        showProgressDialog("Processing...");

        //String generateQRLink = "https://qrpos.sampath.lk/webservicesRest/api/lankaqr/v1/generateqr?ServiceName=GenerateQR&TokenID=0";
        String generateQRLink = "https://qrpos.sampath.lk/webservicesRest/api/lankaqr/v2/generateqr";
        //String generateQRLink = "https://qrpos.sampath.lk/webservicesRest/api/lankaqr/v1/generateqr";
        //String generateQRLink = "https://192.168.133.179:443/webservicesRest/api/lankaqr/v1/generateqr?ServiceName=GenerateQR&TokenID=0";
        qrCoreLogic.fetchQRCodeRESTV2(generateQRLink, jsonString, "LQR_suntechit", "nPOKV4w4Y29Q3fQG3bXaW3NN2IuBp49pxXvg9FlvQuZ2uXtIBQiCjTvpNpVqIJPDsTQWud0SGFPD8PjWcYo4oy0YMpUNW6OXeH+JltXu3UynjNRHfHMS6uzP9m7gg2vx");
    }

    private void dismissProgressDialog() {
        if (progressDialog != null)
            progressDialog.dismiss();
    }

    public boolean startAutoTransactionPollingIn(int seconds) {
        btnManualPoll.setEnabled(false);

        waitSeconds = seconds;
        final WaitTimer waitTimerToAutoPoll = new WaitTimer(seconds);
        waitTimerToAutoPoll.start();

        waitTimerToAutoPoll.setOnTimeOutListener(new WaitTimer.OnTimeOutListener() {
            @Override
            public void onTimeOut() {
                final WaitTimer pollTimer = new WaitTimer(60);
                updateStatusText("Checking Transaction Status...");
                pollTimer.setOnTimerTickListener(new WaitTimer.OnTimerTickListener() {
                    @Override
                    public void onTimerTick(int tick) {
                        if (tick % 20 == 0) {
                            int status = poll();
                            if (SettingsInterpreter.isECREnabled() && MainActivity.ecr.isECRInitiated) {
                                MainActivity.ecr.pushQRDetails(0, merchantName, tranAmount, refLabel, status);
                                MainActivity.ecr.isECRInitiated = false;
                            }
                            if (status == -1) {
                                updateStatusText("Failed to receive");
                                pollTimer.stopTimer();
                            } else if (status == 9) {
                                updateStatusText("Retry");
                                pollTimer.stopTimer();
                            }
                            else if (status == 1) {
                                updateStatusText("Completed");
                                pollTimer.stopTimer();
                            }
                        }
                    }
                });

                pollTimer.setOnTimeOutListener(new WaitTimer.OnTimeOutListener() {
                    @Override
                    public void onTimeOut() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnManualPoll.setEnabled(true);
                            }
                        });
                    }
                });

                pollTimer.start();
            }
        });

        waitTimerToAutoPoll.setOnTimerTickListener(new WaitTimer.OnTimerTickListener() {
            @Override
            public void onTimerTick(int tick) {
                updateStatusText(String.valueOf(tick));
            }
        });

        return true;
    }

    private void decodeResponse(String response) {
        qrTran = new QRTran();

        try {
            JSONObject jsonObject = new JSONObject(response);
            qrTran.status = jsonObject.getString("TX_STATUS");
            if(qrTran.status.equals(STATUS_COMPLETE)){
                qrTran.mid = jsonObject.getString("MID");
                qrTran.merchName = jsonObject.getString("MERC_NAME");
                qrTran.terminalID = jsonObject.getString("TerminlaId");
                qrTran.MCC = jsonObject.getString("MCC");
                qrTran.cusMobile = jsonObject.getString("MOBILE_NUMBER");
                qrTran.cardHolder = jsonObject.getString("CRD_HLDR");
                qrTran.PAN = jsonObject.getString("FRM_CRD");
                qrTran.trace = jsonObject.getString("TRACE");
                //qrTran.refQRLabel = jsonObject.getString("REF_LABEL");
                qrTran.txOrg = jsonObject.getString("TX_ORG");
                if(jsonObject.getInt("QR_TYPE") == 0) {
                    qrTran.qrType = "DYNAMIC";
                }
                else {
                    qrTran.qrType = "STATIC";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public QRTran qrTran;
    private int poll() {
        String statusXML = buildGenQRTransciontStatusMessage(generatedQRCode);

        Log.e("===>","======================= ++++++generatedQRCode " + generatedQRCode);
        String resp = qrCoreLogic.validateQR(generatedQRCode);
        decodeResponse(resp);
        String status = qrTran.status;

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
                        qrTran.tranQRAmount = tranAmount;
                        qrTran.refQRLabel = refLabel;
                        qrTran.Date = Formatter.getCurrentDate();
                        qrTran.Time = Utility.getTimeFormatted(Formatter.getCurrentTimeFormatted());

                        dbHelperTransaction.writeQRTrantoBatch(qrTran);
                        receipt.printReceiptQR(Receipt.ReceiptType.MERCH_COPY, qrTran);
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

    @Override
    public void onBackPressed() {
        return;
    }

    private void showProgressDialog(String mesage) {
        progressDialog = new ProgressDialog(QRDisplay.this);
        progressDialog.setCancelable(false);
        progressDialog.setTitle("QR Code ");
        progressDialog.setMessage(mesage);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    private void deleteQRTable () {
        String query = "DELETE FROM QR";

        dbHelperTransaction.executeCustomQuary(query);
    }

    private void updateStatusText(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus.setText(text);
            }
        });
    }

    public String getMIDforQR() {
        String mid;
        mid = dbHelper.loadQRMID();
        //mid = "1627800000000000030050060000";
        return mid;
    }

    public String getRefLabelForQR() {
        //we simply generate increment the transaction number
        long ref = 0;
        String strRef = preferences.getSetting("qr_ref_label");
        if (strRef == null)
            ref = 0;
        else
            ref = Long.valueOf(strRef);

        ref++;

        strRef = String.valueOf(ref);
        strRef = Formatter.fillInFront("0", strRef, 20);
        preferences.saveSetting("qr_ref_label", String.valueOf(ref));
        return strRef;
    }

    public String buildGenDynamicQRXMLMessage(boolean isJson) {
        String msg = "";

        try {
            if (isJson) {
                msg = qrCoreLogic.buildGenQRJsonV2(
                        QRCoreLogic.QRType.DYNAMIC,
                        QRCoreLogic.TXType.POS_PAYMENT,
                        QRCoreLogic.PaymentType.PEER_TO_MERCHANT,
                        QRCoreLogic.BillDataType.BILL_DATA_NOT_PRESENT,
                        QRCoreLogic.FeeIndicator.MOBILE,
                        QRCoreLogic.LangOptions.ALTERNATE_NOT_REQUIRED,
                        "LK",
                        "Colombo",
                        "0000", "SMB_POS",
                        "123165464",
                        "0710000000",
                        new QRCoreLogic.onRequireUserData() {
                            @Override
                            public String OnRequireUserData(int id) {
                                String result = null;

                                switch (id) {
                                    case MID:
                                        //get the mid for the transaction and return,
                                        result = getMIDforQR();
                                        mid = result;
                                        break;

                                    case MCC:
                                        //get the merchant category code
                                        result = "4814";
                                        break;

                                    case MERC_NAME:
                                        //merchant name
                                        result = "Wpos QR-Suntech";
                                        merchantName = result;
                                        break;

                                    case TX_AMT:
                                        tranAmount = GlobalData.globalTransactionAmount;
                                        result = String.valueOf(tranAmount);
                                        GlobalData.globalTransactionAmount = 0;
                                        break;

                                    case TX_ORG:
                                        result = "SMB_POS";
                                        break;

                                    case TX_CURRENCY:
                                        result = "144";
                                        break;

                                    case REF_LABEL:
                                        result = getRefLabelForQR();
                                        refLabel = result;
                                        break;

                                }
                                return result;
                            }
                        }
                );
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return msg;
    }

    public String buildGenQRTransciontStatusMessage(String generatedQR) {
        return qrCoreLogic.buildStatusMessage(generatedQR);
    }
}