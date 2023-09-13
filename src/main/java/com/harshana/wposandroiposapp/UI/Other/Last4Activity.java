package com.harshana.wposandroiposapp.UI.Other;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.harshana.wposandroiposapp.Base.Base;
import com.harshana.wposandroiposapp.Base.GlobalWait;
import com.harshana.wposandroiposapp.R;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class Last4Activity extends AppCompatActivity implements View.OnClickListener{
    EditText last4Digits;
    Button btnConfirmLast,btnCloseLast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_last4);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        last4Digits = findViewById(R.id.etLast4Digits);
        //btnConfirmLast = findViewById(R.id.btnConfirm_Last4);
        btnCloseLast = findViewById(R.id.btnCancel_Last4);

        last4Digits.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Last4Activity.this.INPUT_METHOD_SERVICE);
        //imm.showSoftInput(last4Digits, InputMethodManager.SHOW_IMPLICIT);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

        btnCloseLast.setOnClickListener(this);
        //btnConfirmLast.setOnClickListener(this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.colorBlack)));

        ActionBarLayout actionBarLayout = ActionBarLayout.getInstance(this, getResources().getString(R.string.app_name), getResources().getColor(R.color.colorBlack));
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(actionBarLayout.createAndGetActionbarLayoutEx());

        last4Digits.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if(i== EditorInfo.IME_ACTION_DONE){
                    //Toast.makeText(getApplicationContext(),"Done pressed",Toast.LENGTH_SHORT).show();
                    String lastd =  last4Digits.getText().toString();
                    String lastCard = Base.currentTransaction.PAN.substring((Base.currentTransaction.PAN.length()-4), (Base.currentTransaction.PAN.length()));

                    if ((lastd.equals("")) || (lastd.length() != 4)) {
                        showToast("Please Enter Correct Info");
                        return false;
                    }

                    if(!lastd.equals(lastCard)) {
                        showToast("Invalid Last 4 digits");
                        GlobalWait.setLastOperCancelled(true);
                        GlobalWait.resetWaiting();
                        setResult(RESULT_CANCELED);
                        finish();
                    } else {

                        GlobalWait.setLastOperCancelled(false);
                        GlobalWait.resetWaiting();
                        setResult(RESULT_OK);
                        finish();
                    }
                }
                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    public void onClick(View v) {
        /*if (v == btnConfirmLast) {
            String lastd =  last4Digits.getText().toString();
            String lastCard = Base.currentTransaction.PAN.substring((Base.currentTransaction.PAN.length()-4), (Base.currentTransaction.PAN.length()));

            if ((lastd.equals("")) || (lastd.length() != 4)) {
                showToast("Please Enter Correct Info");
                return;
            }

            if(!lastd.equals(lastCard)) {
                showToast("Invalid Last 4 digits");
                GlobalWait.setLastOperCancelled(true);
                GlobalWait.resetWaiting();
                setResult(RESULT_CANCELED);
                finish();
            } else {

                GlobalWait.setLastOperCancelled(false);
                GlobalWait.resetWaiting();
                setResult(RESULT_OK);
                finish();
            }
        }*/
        if (v == btnCloseLast)   {
            GlobalWait.setLastOperCancelled(true);
            GlobalWait.resetWaiting();
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    Toast showToastMessage;
    void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(Last4Activity.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }
}