package com.harshana.wposandroiposapp.UI.TranFlow;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.harshana.wposandroiposapp.Base.GlobalWait;
import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.DevArea.GlobalData;
import com.harshana.wposandroiposapp.ECR.ECR;
import com.harshana.wposandroiposapp.MainActivity;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.Settings.SettingsInterpreter;
import com.harshana.wposandroiposapp.UI.Other.ActionBarLayout;
import com.harshana.wposandroiposapp.Utilities.Formatter;

import java.text.DecimalFormat;

public class InputAmount extends AppCompatActivity {
    DBHelper dbHelper;

    Button btn1,btn2,btn3,btn4,btn5,btn6,btn7,btn8,btn9,btn0,btn00;
    ImageView btnClear;
    Button btnProceed;
    Button btnCancel;

    TextView txtBase,txtCurrency;
    Boolean merchenable = false;

    public static final int REQUEST_START_MERCH = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_amount);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            merchenable = extras.getBoolean("merchenable");
        }
        dbHelper = DBHelper.getInstance(this);

        if (SettingsInterpreter.isECREnabled() && MainActivity.ecr.isECRInitiated) {
            GlobalData.globalTransactionAmount = ECR.saleAmount;
            ECR.saleAmount = 0;
            GlobalWait.setLastOperCancelled(false);
            GlobalWait.resetWaiting();
            setResult(RESULT_OK);
            finish();
        }

        btn1 = findViewById(R.id.btn_num1);
        btn2 = findViewById(R.id.btn_num2);
        btn3 = findViewById(R.id.btn_num3);
        btn4 = findViewById(R.id.btn_num4);
        btn5 = findViewById(R.id.btn_num5);
        btn6 = findViewById(R.id.btn_num6);
        btn7 = findViewById(R.id.btn_num7);
        btn8 = findViewById(R.id.btn_num8);
        btn9 = findViewById(R.id.btn_num9);
        btn0 = findViewById(R.id.btn_num0);
        btn00 = findViewById(R.id.btn_num00);
        btnClear = findViewById(R.id.btn_num_clear);
        btnProceed = findViewById(R.id.btnProceedAmountScreen);
        btnCancel = findViewById(R.id.btnCancelAmountScreen);

        btn0.setOnClickListener(numClickListener);
        btn1.setOnClickListener(numClickListener);
        btn2.setOnClickListener(numClickListener);
        btn3.setOnClickListener(numClickListener);
        btn4.setOnClickListener(numClickListener);
        btn5.setOnClickListener(numClickListener);
        btn6.setOnClickListener(numClickListener);
        btn7.setOnClickListener(numClickListener);
        btn8.setOnClickListener(numClickListener);
        btn9.setOnClickListener(numClickListener);
        btn00.setOnClickListener(numClickListener);
        btnClear.setOnClickListener(numClickListener);
        btnProceed.setOnClickListener(numClickListener);
        btnCancel.setOnClickListener(numClickListener);

        txtBase = findViewById(R.id.txtBase);
        txtCurrency = findViewById(R.id.txtCurrency);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable( new ColorDrawable(getResources().getColor(R.color.colorBlack)));

        ActionBarLayout actionBarLayout = ActionBarLayout.getInstance(this,getResources().getString(R.string.app_name),getResources().getColor(R.color.colorBlack));
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(actionBarLayout.createAndGetActionbarLayoutEx());

        Typeface tp = Typeface.createFromAsset(getAssets(),"digital_font.ttf");
        txtBase.setTypeface(tp);
        txtCurrency.setTypeface(tp);
    }

    String inputAmount = "";

    View.OnClickListener numClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnProceed) {
                if (inputAmount.length() == 0) {
                    showToast("Invalid Amount");
                    return;
                }
                else if (Long.valueOf(inputAmount) == 0) {
                    showToast("0 amount not accepted");
                    return;
                }

                if (Long.valueOf(inputAmount) > dbHelper.loadMaxAmount()) {
                    showToast("Amount limit exceeded");
                    return;
                }

                GlobalData.globalTransactionAmount = Long.valueOf(inputAmount);
                if (SettingsInterpreter.isSingleMerchantEnabled() || (SettingsInterpreter.isECREnabled() && MainActivity.ecr.isECRInitiated)) {
                    GlobalWait.setLastOperCancelled(false);
                    GlobalWait.resetWaiting();
                    setResult(RESULT_OK);
                    finish();
                }
                else if(merchenable){
                    Intent issuerMerchantIntent = new Intent(InputAmount.this, MerchantSelectActivity.class);
                    startActivityForResult(issuerMerchantIntent, REQUEST_START_MERCH);
                }
                else {
                    GlobalWait.setLastOperCancelled(false);
                    GlobalWait.resetWaiting();
                    setResult(RESULT_OK);
                    finish();
                }
            }
            else if (v == btnCancel) {
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE:
                                //clicked yes
                                GlobalWait.setLastOperCancelled(true);
                                GlobalWait.resetWaiting();
                                setResult(RESULT_CANCELED);
                                finish();
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                };

                AlertDialog.Builder dialog = new AlertDialog.Builder(InputAmount.this);
                dialog.setTitle("Amount Input");
                dialog.setMessage("Are you sure you want to exit from amount Input?");
                dialog.setPositiveButton("Yes",dialogClickListener);
                dialog.setNegativeButton("No",dialogClickListener);
                dialog.setCancelable(false);
                dialog.show();
            }
            else if (v instanceof  Button) {
                String buttonName = ((Button)v).getText().toString();
                //append the button index to the text
                if(buttonName.equals("00")){
                    if (inputAmount.length() < 11) {
                        inputAmount += buttonName.substring(buttonName.length() - 2,2);
                        updateAmountOnView(inputAmount);
                    }
                }
                else{
                    if (inputAmount.length() < 12) {
                        inputAmount += buttonName.substring(buttonName.length() - 1,1);
                        updateAmountOnView(inputAmount);
                    }
                }
            }
            else if (v == btnClear) {
                //we must simulate the back space here
                if (inputAmount.length() > 0) {
                    inputAmount = inputAmount.substring(0,inputAmount.length()-1);
                    updateAmountOnView(inputAmount);
                }
            }
        }
    };

    void updateAmountOnView(String amount) {
        //check whether the entered amount is greater than the maximum amount allowed
        if (amount.length() == 0) {
            inputAmount = "";
            txtBase.setText("0.00");
            return;
        }

        long lnAmount = Long.valueOf(amount);

        if(lnAmount == 0) {
            inputAmount = "";
            txtBase.setText("0.00");
            return;
        }

        String amt = Formatter.formatAmountUI(lnAmount);

        txtBase.setText(amt);
    }

    Toast showToastMessage;
    void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(InputAmount.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        GlobalWait.setLastOperCancelled(true);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(showToastMessage != null) {
            showToastMessage.cancel();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_START_MERCH) {
            if (resultCode == RESULT_OK) {
                GlobalWait.setLastOperCancelled(false);
                GlobalWait.resetWaiting();
                setResult(RESULT_OK);
                finish();
            }
            else if (resultCode == RESULT_CANCELED) {
                GlobalData.globalTransactionAmount = 0;
                GlobalWait.setLastOperCancelled(true);
                GlobalWait.resetWaiting();
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }
}