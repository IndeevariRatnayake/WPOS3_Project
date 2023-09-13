package com.harshana.wposandroiposapp.UI.OtherTrans;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.harshana.wposandroiposapp.Base.Base;
import com.harshana.wposandroiposapp.Base.GlobalWait;
import com.harshana.wposandroiposapp.Base.TData;
import com.harshana.wposandroiposapp.Base.Transaction;
import com.harshana.wposandroiposapp.Database.DBHelperTransaction;
import com.harshana.wposandroiposapp.DevArea.GlobalData;
import com.harshana.wposandroiposapp.DevArea.TranStaticData;
import com.harshana.wposandroiposapp.Print.Receipt;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.UI.BatchTrans.VoidActivity;
import com.harshana.wposandroiposapp.UI.Fregments.HostMerchantSelectFragment;
import com.harshana.wposandroiposapp.UI.Other.ActionBarLayout;
import com.harshana.wposandroiposapp.Utilities.Formatter;
import com.harshana.wposandroiposapp.Utilities.Sounds;

public class PreComp extends AppCompatActivity implements HostMerchantSelectFragment.OnFragmentInteractionListener {
    int selectedIssuer = -1;
    int selectedMerchant = -1;

    EditText txtInvoice;
    TextView txtDesc,txtDesc2;
    Button btnCancel,btnPreComp;
    public static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_comp);

        context = this;

        Fragment fragment = new HostMerchantSelectFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction  = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.placeHolder,fragment);
        fragmentTransaction.commit();

        ((HostMerchantSelectFragment) fragment).setOnSelectionMadeListener(new HostMerchantSelectFragment.OnSelectionMade() {
            @Override
            public void onSelectionMadeNotify(int issuerSelected, int merchantSelected) {
                selectedIssuer = issuerSelected;
                selectedMerchant = merchantSelected;

                checkPreComp();
            }
        });

        txtInvoice = findViewById(R.id.txtInvoiceNumber);
        Typeface tp = Typeface.createFromAsset(getAssets(),"digital_font.ttf");
        txtInvoice.setTypeface(tp);

        txtDesc = findViewById(R.id.txtDesc);
        txtDesc2 = findViewById(R.id.txtdesc2);
        txtDesc2.setTypeface(tp);
        txtDesc.setTypeface(tp);

        btnCancel = findViewById(R.id.btnCancel);
        btnPreComp = findViewById(R.id.btnPreComp);

        btnCancel.setOnClickListener(onClickListener);
        btnPreComp.setOnClickListener(onClickListener);

        txtInvoice.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if(i== EditorInfo.IME_ACTION_DONE){
                    //search and display transaction details
                    if (selectedMerchant <= 0) {
                        showToast("Please select a merchant to proceed");
                        return false;
                    }

                    checkPreComp();
                }
                return false;
            }
        });
    }

    private boolean checkPreComp() {
        boolean ret = false;

        //perform validations
        String invoice = txtInvoice.getText().toString();

        if (invoice == null || invoice.equals("")) {
            setStatus("No inputs given, Check inputs");
            return false;
        }

        if (invoice.length() > 0)
            ret = searchPreAuth(selectedMerchant,Integer.valueOf(invoice));

        btnPreComp.setEnabled(ret);

        if (!ret)
            txtDesc2.setText("");

        return ret;
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnPreComp) {
                if(checkPreComp()) {
                    getPreCommAmount();
                }
            }
            else if (v == btnCancel)
                finish();
        }
    };

    long selectedTranId = 0;
    long preAuthAmount = 0 ;
    long preCompAmount = 0;

    private boolean searchPreAuth(int selectedMerchant,int invoiceNumber) {
        String preAuthQuary =  "SELECT * FROM TXN WHERE MerchantNumber = "  +
                     selectedMerchant + " AND TransactionCode = " +
                     TranStaticData.TranTypes.PRE_AUTH + " AND  InvoiceNumber = " + invoiceNumber;

        DBHelperTransaction dbTransaction = DBHelperTransaction.getInstance(this);
        try {
            Cursor preAuthRec = dbTransaction.readWithCustomQuary(preAuthQuary);

            if (preAuthRec == null || preAuthRec.getCount() == 0) {
                setStatus("No Pre auth transaction found");
                return false;
            }

            preAuthRec.moveToFirst();

            //populate the transaction details
            String invNumber = preAuthRec.getString(preAuthRec.getColumnIndex("InvoiceNumber"));
            String cardPan = preAuthRec.getString(preAuthRec.getColumnIndex("PAN"));
            cardPan = Formatter.maskPan(cardPan,"****NNNN****NNNN", '*');
            preAuthAmount = preAuthRec.getLong(preAuthRec.getColumnIndex("BaseTransactionAmount"));

            String msg = "Invoice No [" + invNumber + "] " + " PAN [" + cardPan + "]";
            txtDesc.setText("Amount [" + Formatter.formatAmount(preAuthAmount,"Rs") + "]");
            txtDesc2.setText(msg);

            selectedTranId = preAuthRec.getLong(preAuthRec.getColumnIndex("ID"));

            preAuthRec.close();
        } catch (Exception ex) {
            return false;
        }

        return true;
    }

    public void setStatus(String status) {
        txtDesc.setText(status);
    }

    private void getPreCommAmount() {
        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(12);

        final AlertDialog.Builder inputBox = new AlertDialog.Builder(this);
        final EditText txtAmount = new EditText(this);
        txtAmount.setText( Formatter.formatAmount(preAuthAmount));
        txtAmount.setInputType(InputType.TYPE_CLASS_NUMBER + InputType.TYPE_NUMBER_FLAG_DECIMAL);
        txtAmount.setFilters(FilterArray);
        inputBox.setView(txtAmount);
        inputBox.setTitle("Pre Comp Amount");
        inputBox.setMessage("Please input an amount to pre comp");

        inputBox.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        inputBox.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        final AlertDialog alert  = inputBox.create();
        alert.show();

        alert.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String amount = txtAmount.getText().toString();
                if (amount  == null || amount.equals("")) {
                    showToast("Enter a valid amount");
                    return;
                }

                if(!amount.contains(".")) {
                    showToast("Invalid amount format");
                    return;
                }
                double damtmax = preAuthAmount*0.0115;
                double damtmin = preAuthAmount*0.0085;
                double amt = Double.parseDouble(amount);

                if((amt > damtmax) || (amt < damtmin)) {
                    showToast("Enter a valid amount");
                    return;
                }
                try {
                    preCompAmount  = Long.valueOf(Formatter.removeDecimalPlace(amount));

                    //perform the pre comp transaction here
                    PreCompTran preCompTran = new PreCompTran(PreComp.this);
                    preCompTran.performPreComp(selectedTranId,preCompAmount);
                    alert.dismiss();

                    displayProgress("Please wait , Processing...");

                    preCompTran.setOnTransStatusUpdate(new PreCompTran.onTranStatusUpdate() {
                        @Override
                        public void OnTransStatusUpdate(PreCompTran.TransStatus status) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dismissProgress();
                                }
                            });

                            if (status == PreCompTran.TransStatus.SUCCESS) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        selectedMerchant = -1;
                                        selectedTranId = -1;
                                        txtInvoice.setText("");
                                        btnPreComp.setEnabled(false);
                                    }
                                });
                            }
                        }
                    });
                } catch (Exception ex) {
                    showToast("Entered amount is invalid");
                    return;
                }
            }
        });
    }

    ProgressDialog progressDialog = null;

    private void displayProgress(String message) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Pre Comp ");
        progressDialog.setMessage(message);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private void dismissProgress() {
       if (progressDialog != null)
           progressDialog.dismiss();

    }

    Toast showToastMessage;
    void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(PreComp.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}

class PreCompTran extends Base {
    Context c;
    public PreCompTran(Context cc) {
        c = cc;
        appContext = c;
    }

    public boolean performPreComp(final long tranID, final long tranAmount) {
        //fetch the transaction from the database
        String quary = "SELECT * FROM TXN WHERE ID = " + tranID;
        boolean tranFinished = false;

        final DBHelperTransaction transactions = DBHelperTransaction.getInstance(c);
        Cursor tran = null;
        try {
            tran = transactions.readWithCustomQuary(quary);

            if (tran == null || tran.getCount() == 0) {
                Log.d("PRE_COMP", "Failed1");
                callInTransStatusUpdate(TransStatus.FAILED);
                return false;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            Log.d("PRE_COMP", "Failed2");
            callInTransStatusUpdate(TransStatus.FAILED);
            return false;
        }

        Log.d("PRE_COMP", "1");

        tran.moveToFirst();
        Transaction preCompTran = transactions.getTransaction(tran);

        TData curTranData = TranStaticData.getTran(TranStaticData.TranTypes.PRE_COMP);
        preCompTran.MTI = curTranData.MTI;
        preCompTran.bitmap = curTranData.Bitmap;
        preCompTran.inTransactionCode = TranStaticData.TranTypes.PRE_COMP;
        preCompTran.procCode = curTranData.ProcCode;
        preCompTran.lnBaseTransactionAmount = tranAmount;
        GlobalData.transactionName = curTranData.tranName;

        currentTransaction = preCompTran;
        loadCardAndIssuer();
        int originalInvoice = currentTransaction.inInvoiceNumber;
        loadTerminal();
        currentTransaction.inInvoiceNumber = originalInvoice;

        setFieldsAndLoadPacket();
        packet.setPacketTPDU(currentTransaction.TPDU);

        Thread printThread = new Thread(new Runnable() {
           @Override
           public void run() {
               try {
                   //update the db as a pre comp tran
                   String updateQuary = "UPDATE TXN SET BaseTransactionAmount = " +
                           tranAmount + ", TransactionCode = " +
                           TranStaticData.TranTypes.PRE_COMP + " WHERE ID = " + tranID;

                   Log.d("PRE_COMP", updateQuary);
                   transactions.executeCustomQuary(updateQuary);

                   Receipt rcpt = Receipt.getInstance();
                   Log.d("PRE_COMP", "2");
                   rcpt.printReceipt(4);
                   Log.d("PRE_COMP", "3");
               } catch (Exception ex ) {
                   ex.printStackTrace();
                   Log.d("PRE_COMP", "Failed5");
                   callInTransStatusUpdate(TransStatus.FAILED);
               }

               Log.d("PRE_COMP", "Success");
               callInTransStatusUpdate(TransStatus.SUCCESS);
           }
        });

        printThread.start();
        return true;
    }

    public interface onTranStatusUpdate {
        void OnTransStatusUpdate(TransStatus status);
    }

    private onTranStatusUpdate listener = null;

    public void setOnTransStatusUpdate(onTranStatusUpdate func) {
        listener = func;
    }

    private void callInTransStatusUpdate(TransStatus status) {
        if (listener != null)
            listener.OnTransStatusUpdate(status);
    }

    public enum TransStatus {
        SUCCESS,
        FAILED;
    }
}