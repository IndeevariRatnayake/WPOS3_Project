package com.harshana.wposandroiposapp.UI.TranFlow;

import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.DevArea.GlobalData;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.UI.Fregments.HostMerchantSelectFragment;

public class MerchantSelectActivity extends AppCompatActivity  implements HostMerchantSelectFragment.OnFragmentInteractionListener {
    DBHelper configDb;

    Button btnProceed = null;
    Button btnCancel  = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merchant_select);

        //replace the merchant select fragment on the activity
        Fragment fragment = new HostMerchantSelectFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction  = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.placeHolder,fragment);
        fragmentTransaction.commit();

        ((HostMerchantSelectFragment) fragment).setOnSelectionMadeListener(selectionMade);

        configDb = DBHelper.getInstance(getApplicationContext());

        btnProceed = findViewById(R.id.btnProceed);
        btnCancel = findViewById(R.id.btnCancel);

        btnProceed.setOnClickListener(clickListener);
        btnCancel.setOnClickListener(clickListener);
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

    //this is the method for callback of the on click listener for the buttons
    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == btnCancel) {
                AlertDialog.Builder alert =  new AlertDialog.Builder(MerchantSelectActivity.this);
                alert.setTitle("Confirm Your Action");
                alert.setMessage("Do you really want to cancel selection ?");
                alert.setPositiveButton("Yes",dialogClickListener);
                alert.setNegativeButton("No",dialogClickListener);
                alert.setCancelable(false);
                alert.show();
            }
            else if (v == btnProceed) {
                if (selectedMerchant < 0) {
                    showToast("Please select a merchant to proceed");
                    return;
                }
                GlobalData.selectedMerchant = selectedMerchant;
                //GlobalWait.setLastOperCancelled(false);
                //GlobalWait.resetWaiting();
                setResult(RESULT_OK);
                finish();
            }
        }

        DialogInterface.OnClickListener dialogClickListener =  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        //GlobalWait.setLastOperCancelled(true);
                        //GlobalWait.resetWaiting();
                        setResult(RESULT_CANCELED);
                        finish();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };
    };

    Toast showToastMessage;
    void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(MerchantSelectActivity.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
    }
}