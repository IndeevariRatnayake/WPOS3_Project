package com.harshana.wposandroiposapp.UI.Utils;

import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.Database.DBHelperTransaction;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.UI.Fregments.HostMerchantSelectFragment;
import com.harshana.wposandroiposapp.UI.Other.ActionBarLayout;

public class ClearBatch extends AppCompatActivity implements HostMerchantSelectFragment.OnFragmentInteractionListener {
    DBHelperTransaction dbTransactions  = null;
    DBHelper dbHelper = null;

    Button btnClearBatch,btnClose;
    TextView txtInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clear_batch);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable( new ColorDrawable(getResources().getColor(R.color.colorBlack)));

        ActionBarLayout actionBarLayout = ActionBarLayout.getInstance(this,getResources().getString(R.string.app_name),getResources().getColor(R.color.colorBlack));
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(actionBarLayout.createAndGetActionbarLayoutEx());

        Typeface tp = Typeface.createFromAsset(getAssets(),"digital_font.ttf");

        //replace the merchant select fragment on the activity
        Fragment fragment = new HostMerchantSelectFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction  = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.placeHolder,fragment);
        fragmentTransaction.commit();

        ((HostMerchantSelectFragment) fragment).setOnSelectionMadeListener(selectionMade);

        dbTransactions = DBHelperTransaction.getInstance(this);
        dbHelper = DBHelper.getInstance(this);

        btnClearBatch = findViewById(R.id.btnClearBatch);
        btnClose = findViewById(R.id.btnClose);

        btnClearBatch.setOnClickListener(onClickListener);
        btnClose.setOnClickListener(onClickListener);
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnClearBatch) {
                if (selectedMerchant  == -1) {
                    showToast("Please select  a merchant to continue");
                    return;
                }

                if (checkBatchEmpty(selectedMerchant)) {
                    showToast("Batch is empty");
                    return;
                }

                //clear the reversal
                if (clearBatch(selectedMerchant)) {
                    showToast("Batch cleared");
                }
                else {
                    showToast("Clearing batch Failed");
                }
            }
            else if (v == btnClose) {
                finish();
            }
        }
    };

    Toast showToastMessage;
    void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(ClearBatch.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }

    int selectedHost = -1;
    int selectedMerchant = -1;

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    HostMerchantSelectFragment.OnSelectionMade selectionMade = new HostMerchantSelectFragment.OnSelectionMade() {
        @Override
        public void onSelectionMadeNotify(int hostSelected, int merchantSelected) {
            selectedHost = hostSelected;
            selectedMerchant = merchantSelected;
        }
    };

    private boolean checkBatchEmpty(int merchantNumber) {
        String searchQuary = "SELECT * FROM TXN WHERE MerchantNumber = " + merchantNumber;
        boolean retVal = false;

        try {
            Cursor batchRecord = dbTransactions.readWithCustomQuary(searchQuary);
            if (batchRecord == null || batchRecord.getCount() == 0)
                retVal =  true;
            else
                retVal = false;

            batchRecord.close();

            return retVal;

        } catch (Exception ex) {
            return false;
        }
    }

    private boolean clearBatch(int merchanNumber) {
        String deleteQuary = "DELETE FROM TXN WHERE MerchantNumber = " + merchanNumber;

        int retResult = 0 ;
        try {
            dbTransactions.executeCustomQuary(deleteQuary);
            dbHelper.setMustSettleFlagState(merchanNumber,false);
            dbHelper.setClearBatchFlag(merchanNumber,false);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void onBackPressed()
    {

    }
}
