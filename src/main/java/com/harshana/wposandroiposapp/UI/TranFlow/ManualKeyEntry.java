package com.harshana.wposandroiposapp.UI.TranFlow;

import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.harshana.wposandroiposapp.Base.Base;
import com.harshana.wposandroiposapp.Base.GlobalWait;
import com.harshana.wposandroiposapp.DevArea.GlobalData;
import com.harshana.wposandroiposapp.MainActivity;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.UI.Other.ActionBarLayout;
import com.harshana.wposandroiposapp.UI.Other.Last4Activity;
import com.harshana.wposandroiposapp.Utilities.Formatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import wangpos.sdk4.libbasebinder.BankCard;

public class ManualKeyEntry extends AppCompatActivity {
    EditText txtPan = null;
    NumberPicker dtYearPicker = null;
    NumberPicker dtMonthPicker = null;
    Button btnCancel = null;
    Button btnProceed = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_key_entry);

        txtPan = findViewById(R.id.txtPan);
        dtYearPicker = findViewById(R.id.dtYearPicker);
        dtMonthPicker = findViewById(R.id.dtMonthPicker);
        btnCancel = findViewById(R.id.btnCancel);
        btnProceed = findViewById(R.id.btnProceed);

        Typeface tp = Typeface.createFromAsset(getAssets(), "digital_font.ttf");
        txtPan.setTypeface(tp);

        dtYearPicker.setMinValue(23);
        dtYearPicker.setMaxValue(99);

        dtMonthPicker.setMinValue(1);
        dtMonthPicker.setMaxValue(12);

        Calendar calendar = Calendar.getInstance();
        dtYearPicker.setValue(calendar.get(Calendar.YEAR));

        btnProceed.setOnClickListener(clickListener);
        btnCancel.setOnClickListener(clickListener);
    }

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (btnCancel == v) {
                GlobalData.globalTransactionAmount = 0;
                GlobalWait.setLastOperCancelled(true);
                setResult(RESULT_CANCELED);
                finish();
            } else if (btnProceed == v) {
                //validate the data
                btnProceed.setEnabled(false);
                btnProceed.setClickable(false);
                int year = dtYearPicker.getValue();
                String strYear = Formatter.fillInFront("0", String.valueOf(year), 2);

                int month = dtMonthPicker.getValue();
                String strMonth = Formatter.fillInFront("0", String.valueOf(month), 2);

                String pan = txtPan.getText().toString();
                if (pan == null || pan.equals("")) {
                    showToast("Please enter a valid PAN");
                    btnProceed.setEnabled(true);
                    btnProceed.setClickable(true);
                    return;
                } else if (pan.length() < 8) {
                    showToast("A PAN should be at least 8 numbers");
                    btnProceed.setEnabled(true);
                    btnProceed.setClickable(true);
                    return;
                }

                GlobalData.manualKeyExpDate = strYear + strMonth;
                if (!validateExpDate(GlobalData.manualKeyExpDate)) {
                    showToast("Expired Card");
                    btnProceed.setEnabled(true);
                    btnProceed.setClickable(true);
                    return;
                }

                //generate a new transaction and proceed
                GlobalData.isManualKeyIn = true;
                GlobalData.manualKeyPan = pan;
                try {
                    BankCard bankCard = new BankCard(ManualKeyEntry.this);
                    bankCard.breakOffCommand();
                } catch (Exception ex) {}
                setResult(RESULT_OK);
                finish();
            }
        }
    };

    private boolean validateExpDate(String expDate){
        String date = expDate.substring(2, 4) + "/" + expDate.substring(0, 2);

        try {
            Date expdate = new SimpleDateFormat("MM/yy").parse(date);
            Calendar c = Calendar.getInstance();
            c.setTime(expdate);
            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
            expdate = c.getTime();
            if (expdate.after(new Date())) {
                return true;
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        return;
    }

    Toast showToastMessage;
    void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(ManualKeyEntry.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }
}