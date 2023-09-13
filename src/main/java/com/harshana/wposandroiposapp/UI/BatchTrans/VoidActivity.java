package com.harshana.wposandroiposapp.UI.BatchTrans;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.harshana.wposandroiposapp.Database.DBHelperTransaction;
import com.harshana.wposandroiposapp.DevArea.TranStaticData;
import com.harshana.wposandroiposapp.MainActivity;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.UI.Fregments.HostMerchantSelectFragment;
import com.harshana.wposandroiposapp.Utilities.Formatter;
import com.tooltip.Tooltip;

import static com.harshana.wposandroiposapp.Base.Base.VoidResults.VOID_COMM_FALIURE;
import static com.harshana.wposandroiposapp.Base.Base.VoidResults.VOID_FAILED;
import static com.harshana.wposandroiposapp.Base.Base.VoidResults.VOID_REVERSED;
import static com.harshana.wposandroiposapp.Base.Base.VoidResults.VOID_SUCCESS;

public class VoidActivity extends AppCompatActivity implements HostMerchantSelectFragment.OnFragmentInteractionListener {
    Button btnCheck = null;
    Button btnCancel = null;
    Button btnConfirm = null;

    EditText txtInvoiceNumber = null;
    TextView txtCardNumber = null;
    TextView txtAmount = null;

    public static Context context;

    DBHelperTransaction tranDatabase = null;
    int selectedIssuer = -1;
    int selectedMerchant = -1;
    boolean okToConfirm = false;
    int tranID = -1;
    boolean isVoidProcessing = false;
    ProgressDialog progressDialog;

    HostMerchantSelectFragment.OnSelectionMade selectionMade = new HostMerchantSelectFragment.OnSelectionMade() {
        @Override
        public void onSelectionMadeNotify(int issuerSelected, int merchantSelected) {
            selectedIssuer = issuerSelected;
            selectedMerchant = merchantSelected;
        }
    };

    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    finish();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        }
    };

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isVoidProcessing) {
                showToast("Void is being processed, Please wait ");
                return;
            }
            if (v == btnCheck) {
                //search and display transaction details
                if (selectedMerchant <= 0) {
                    showToast("Please select a merchant to proceed");
                    okToConfirm = false;
                    clearFields();
                    return;
                } else if (txtInvoiceNumber.getText().toString() == null || txtInvoiceNumber.getText().toString() == "") {
                    showToast("Please enter a valid invoice number to continue");
                    return;
                }

                int invoiceNumber = 0 ;
                try {
                    invoiceNumber = Integer.parseInt(txtInvoiceNumber.getText().toString());
                } catch (Exception ex) {
                    showToast("Please enter a valid invoice number to continue");
                    return;
                }
                String quary = "SELECT * FROM TXN  WHERE TXN.MerchantNumber = " + selectedMerchant + "  AND TXN.InvoiceNumber = " + invoiceNumber;
                //we have a valid merchant number so we query the db
                Cursor txn = tranDatabase.readWithCustomQuary(quary);

                if (txn == null || txn.getCount() == 0) {
                    showToast("Transaction not found");
                    okToConfirm = false;
                    clearFields();
                    return;
                }

                txn.moveToFirst();
                //we have a valid transaction so we display it
                int tranCode = txn.getInt(txn.getColumnIndex("TransactionCode"));
                if(tranCode == TranStaticData.TranTypes.PRE_AUTH) {
                    showToast("Void disabled for Pre Auth");
                    okToConfirm = false;
                    clearFields();
                    return;
                }
                String cardNumber = txn.getString(txn.getColumnIndex("PAN"));
                cardNumber = Formatter.maskPan(cardNumber,"****NNNN****NNNN",'*');

                long amount = txn.getLong(txn.getColumnIndex("BaseTransactionAmount"));
                String strAmount = Formatter.formatAmount(amount,"Rs");

                tranID = txn.getInt(txn.getColumnIndex("ID"));

                int isVoided = txn.getInt(txn.getColumnIndex("Voided"));
                if (isVoided == 1)
                    txtCardNumber.setText("Card No : " + cardNumber + " [VOIDED]");
                else
                    txtCardNumber.setText("Card No : " + cardNumber);

                txtAmount.setText("Amount  : " + strAmount);
                okToConfirm = isVoided != 1;
                txn.close();
            }
            else if (v == btnCancel) {
                AlertDialog.Builder alert =  new AlertDialog.Builder(VoidActivity.this);
                alert.setTitle("Confirm Your Action");
                alert.setMessage("Are you sure you want to exit void operation?");
                alert.setPositiveButton("Yes",dialogClickListener);
                alert.setNegativeButton("No",dialogClickListener);
                alert.setCancelable(false);
                alert.show();
            }
            else if (v == btnConfirm) {
                if (!okToConfirm) {
                    showToast("Please check a transaction before proceed");
                    return;
                }

                //we need to perform the void operation
                VoidThread  voidThread =  new VoidThread(tranID);
                voidThread.setOnVoidFinished(new VoidThread.onVoidFinished() {
                    @Override
                    public void onVoidFinished(final int status) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (status == VOID_SUCCESS)
                                    showToast("Void Transaction Successful");
                                else if (status == VOID_COMM_FALIURE)
                                    showToast("Communication Failure ");
                                else if (status == VOID_FAILED)
                                    showToast("Void Request Declined");
                                else if (status == VOID_REVERSED)
                                    showToast("Void Reversed");
                            }
                        });

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                            }
                        });

                        okToConfirm = false;
                        isVoidProcessing = false;
                    }
                });

                isVoidProcessing = true;
                voidThread.start();
                isVoidProcessing = true;
                progressDialog = new ProgressDialog(VoidActivity.this);
                progressDialog.setTitle("Void");
                progressDialog.setMessage("Processing Online...");
                progressDialog.setCancelable(false);
                progressDialog.show();
            }
        }
    };

    @Override
    public void onBackPressed() {

    }

    void displayToolTip(View v) {
        Tooltip tip = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_void);

        context = this;

        //replace the merchant select fragment on the activity
        Fragment fragment = new HostMerchantSelectFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.placeHolder, fragment);
        fragmentTransaction.commit();

        ((HostMerchantSelectFragment) fragment).setOnSelectionMadeListener(selectionMade);

        btnCheck = findViewById(R.id.btnCheck);
        btnCancel = findViewById(R.id.btnCancel);
        btnConfirm = findViewById(R.id.btnConfirm);

        txtInvoiceNumber = findViewById(R.id.txtInvoiceNumber);

        txtAmount = findViewById(R.id.txtTranAmount);
        txtCardNumber = findViewById(R.id.txtCardNumber);

        Typeface tp = Typeface.createFromAsset(getAssets(), "digital_font.ttf");
        txtInvoiceNumber.setTypeface(tp);
        txtAmount.setTypeface(tp);
        txtCardNumber.setTypeface(tp);


        btnConfirm.setOnClickListener(clickListener);
        btnCancel.setOnClickListener(clickListener);
        btnCheck.setOnClickListener(clickListener);

        tranDatabase = DBHelperTransaction.getInstance(getApplicationContext());

        displayToolTip(txtInvoiceNumber);

        txtInvoiceNumber.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if(i== EditorInfo.IME_ACTION_DONE){
                    //search and display transaction details
                    if (selectedMerchant <= 0) {
                        showToast("Please select a merchant to proceed");
                        okToConfirm = false;
                        clearFields();
                        return false;
                    } else if (txtInvoiceNumber.getText().toString() == null || txtInvoiceNumber.getText().toString() == "") {
                        showToast("Please enter a valid invoice number to continue");
                        okToConfirm = false;
                        clearFields();
                        return false;
                    }

                    int invoiceNumber = 0 ;
                    try {
                        invoiceNumber = Integer.valueOf(txtInvoiceNumber.getText().toString());
                    } catch (Exception ex) {
                        showToast("Please enter a valid invoice number to continue");
                        okToConfirm = false;
                        clearFields();
                        return false;
                    }
                    String quary = "SELECT * FROM TXN  WHERE TXN.MerchantNumber = " + selectedMerchant + "  AND TXN.InvoiceNumber = " + invoiceNumber;
                    //we have a valid merchant number so we query the db
                    Cursor txn = tranDatabase.readWithCustomQuary(quary);

                    if (txn == null || txn.getCount() == 0) {
                        showToast("Transaction not found");
                        okToConfirm = false;
                        clearFields();
                        return false;
                    }

                    txn.moveToFirst();
                    //we have a valid transaction so we display it
                    int tranCode = txn.getInt(txn.getColumnIndex("TransactionCode"));
                    if(tranCode == TranStaticData.TranTypes.PRE_AUTH) {
                        showToast("Void disabled for Pre Auth");
                        okToConfirm = false;
                        clearFields();
                        return false;
                    }

                    String cardNumber = txn.getString(txn.getColumnIndex("PAN"));
                    cardNumber = Formatter.maskPan(cardNumber,"****NNNN****NNNN",'*');

                    long amount = txn.getLong(txn.getColumnIndex("BaseTransactionAmount"));
                    String strAmount = Formatter.formatAmount(amount,"Rs");

                    tranID = txn.getInt(txn.getColumnIndex("ID"));

                    int isVoided = txn.getInt(txn.getColumnIndex("Voided"));
                    if (isVoided == 1)
                        txtCardNumber.setText("Card No : " + cardNumber + " [VOIDED]");
                    else
                        txtCardNumber.setText("Card No : " + cardNumber);

                    txtAmount.setText("Amount  : " + strAmount);
                    okToConfirm = isVoided != 1;
                    txn.close();
                }
                return false;
            }
        });
    }

    private void clearFields() {
        txtCardNumber.setText("Card No :");
        txtAmount.setText("Amount  :");
    }

    Toast showToastMessage;
    void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(VoidActivity.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}

class VoidThread extends Thread {
    int tranId = 0;

    public VoidThread(int id)
    {
        tranId = id;
    }

    @Override
    public void run() {
        int status = MainActivity.applicationBase.performVoid(tranId);
        if (listener != null)
            listener.onVoidFinished(status);
    }

    private onVoidFinished listener = null;

    public interface  onVoidFinished {
        void onVoidFinished(int status);
    }

    public void setOnVoidFinished(onVoidFinished l)
    {
        listener = l;
    }
}