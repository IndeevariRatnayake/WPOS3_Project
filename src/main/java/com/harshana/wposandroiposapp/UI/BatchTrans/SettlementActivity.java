package com.harshana.wposandroiposapp.UI.BatchTrans;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.harshana.wposandroiposapp.Base.Base;
import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.Database.DBHelperTransaction;
import com.harshana.wposandroiposapp.DevArea.GlobalData;
import com.harshana.wposandroiposapp.DevArea.TranStaticData;
import com.harshana.wposandroiposapp.MainActivity;
import com.harshana.wposandroiposapp.Print.Receipt;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.UI.Fregments.HostMerchantSelectFragment;
import com.harshana.wposandroiposapp.UI.Utils.DialogUtils;
import com.harshana.wposandroiposapp.Utilities.Formatter;

public class SettlementActivity extends AppCompatActivity  implements HostMerchantSelectFragment.OnFragmentInteractionListener {
    TextView txtStatus  = null;
    Button btnCancel = null;
    Button btnSettle = null;
    ScrollView frame = null;
    DBHelper configDatabase = null;
    DBHelperTransaction transactionDatabase = null;

    DialogUtils dialogUtils = null;
    ProgressDialog progressDialog = null;
    Fragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settlement);

        //replace the merchant select fragment on the activity
        fragment = new HostMerchantSelectFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction  = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.placeHolder,fragment);
        fragmentTransaction.commit();

        ((HostMerchantSelectFragment) fragment).setOnSelectionMadeListener(selectionMade);

        Typeface tp = Typeface.createFromAsset(getAssets(),"digital_font.ttf");

        //initialize the controls
        txtStatus = findViewById(R.id.txtTranStatus);
        txtStatus.setTypeface(tp);

        btnCancel = findViewById(R.id.btnCancel);
        btnSettle = findViewById(R.id.btnSettle);
        frame = findViewById(R.id.listscroll);

        btnCancel.setOnClickListener(clickListener);
        btnSettle.setOnClickListener(clickListener);

        configDatabase = DBHelper.getInstance(getApplicationContext());
        transactionDatabase = DBHelperTransaction.getInstance(getApplicationContext());

        MainActivity.applicationBase.setOnSettlementStateChangeListener(new Base.OnSettlementStateChange() {
            @Override
            public void OnSettlementStateChanged(final String status) {
                //set to run on UI thread since this is called back from a different thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run()
                    {
                        txtStatus.setText("Status : " + status);
                    }
                });
            }
        });

        dialogUtils = DialogUtils.getInstance();
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

    public static void enableDisableViewGroup(ViewGroup viewGroup, boolean enabled) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = viewGroup.getChildAt(i);
            view.setEnabled(enabled);
            if (view instanceof ViewGroup) {
                enableDisableViewGroup((ViewGroup) view, enabled);
            }
        }
    }

    boolean isSettlementRunning = false;

    Toast showToastMessage;
    void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(SettlementActivity.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }

    private void startSettlementProcess() {
        btnCancel.setEnabled(false);
        btnSettle.setEnabled(false);
        isSettlementRunning = true;
        SettlementThread settlementThread =  new SettlementThread(selectedHost,selectedMerchant);
        settlementThread.setOnSettlmentFinished(new SettlementThread.OnSettlementFinished() {
            @Override
            public void settlementProcessFinished() {
                isSettlementRunning = false;
                if (progressDialog != null)
                    progressDialog.dismiss();

                finishSettlement();
            }
        });

        //start the settlement thread
        progressDialog  =  new ProgressDialog(SettlementActivity.this);
        progressDialog.setTitle("Settlement");
        progressDialog.setMessage("Settlement is Processing...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        settlementThread.start();
    }

    View.OnClickListener clickListener =  new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnSettle) {
                if (isSettlementRunning) {
                    showToast("Settlement is being processed, Please wait");
                    return;
                } else if (selectedMerchant < 0) {
                    showToast("Please select a merchant to proceed settlement");
                    return;
                }

                if(MainActivity.applicationBase.checkSettleBatch(selectedHost,selectedMerchant) == 1) {
                    showToast("Empty Batch");
                    return;
                }

                startSettlement();

                dialogUtils.alertDialogBox(
                        SettlementActivity.this,
                        "Detail Report",
                        "Do you like to print Detail Report ?",
                        "Yes",
                        "No, Settle",
                        new DialogUtils.onAlertBoxButtonActions() {
                            @Override
                            public void OnPositivePressAction() {
                                Receipt rcpt = Receipt.getInstance();
                                rcpt.printDetailReportForSettlement(selectedHost,selectedMerchant);
                                showDialogConfirmation();
                            }

                            @Override
                            public void OnNegativePressAction() {
                                String currencySymbol = configDatabase.getCurrency(selectedMerchant);
                                GlobalData.SaleAmt = Formatter.formatAmount(transactionDatabase.getTranAmountSALE(selectedMerchant),currencySymbol);
                                GlobalData.RefundAmt = "LKR 0.00";
                                showDialogConfirmation();
                            }
                        }
                );
            } else if (v == btnCancel) {
                dialogUtils.alertDialogBox(
                        SettlementActivity.this,
                        "Settlement",
                        "Do you want to Exit Settlement ?",
                        "Yes",
                        "No",
                        new DialogUtils.onAlertBoxButtonActions() {
                            @Override
                            public void OnPositivePressAction() {
                                finish();
                            }

                            @Override
                            public void OnNegativePressAction() {

                            }
                        }
                );
            }
        }
    };

    void startSettlement() {
        btnCancel.setEnabled(false);
        btnCancel.setClickable(false);
        btnSettle.setEnabled(false);
        btnSettle.setClickable(false);
        enableDisableViewGroup((ViewGroup)fragment.getView(), false);
    }

    void finishSettlement() {
        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txtStatus.setText("Ready");
                    btnCancel.setEnabled(true);
                    btnCancel.setClickable(true);
                    btnSettle.setEnabled(true);
                    btnSettle.setClickable(true);
                    enableDisableViewGroup((ViewGroup)fragment.getView(), true);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void showDialogConfirmation() {
        dialogUtils.alertDialogBox(
                SettlementActivity.this,
                "Settlement",
                "Sale Amount " + GlobalData.SaleAmt + "\n" +
                        "Refund Amount " + GlobalData.RefundAmt + "\n" +
                        "\n" +
                        "Do you Confirm ?",
                "Yes",
                "No",
                new DialogUtils.onAlertBoxButtonActions() {
                    @Override
                    public void OnPositivePressAction() {
                        startSettlementProcess();
                    }

                    @Override
                    public void OnNegativePressAction() {
                        finishSettlement();
                    }
                }
        );
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
    }
}

class SettlementThread extends Thread {
    int selectedHost =  -1;
    int selectedMerchant  = -1;

    public SettlementThread(int iss,int merchBase) {
        selectedHost = iss;
        selectedMerchant = merchBase;
    }

    @Override
    public void run() {
        MainActivity.applicationBase.performSettlement(selectedHost,selectedMerchant);
        callSettlmentFinishedCallback();
    }

    public interface OnSettlementFinished {
        void settlementProcessFinished();
    }

    private OnSettlementFinished listener = null;

    public void setOnSettlmentFinished(OnSettlementFinished func)
    {
        listener = func;
    }

    private void callSettlmentFinishedCallback() {
        if (listener != null)
            listener.settlementProcessFinished();
    }
}