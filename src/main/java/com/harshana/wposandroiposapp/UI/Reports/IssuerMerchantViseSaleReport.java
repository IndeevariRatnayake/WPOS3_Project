package com.harshana.wposandroiposapp.UI.Reports;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.Database.DBHelperTransaction;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.UI.Fregments.IssuerMerchantSelectFragment;
import com.harshana.wposandroiposapp.UI.Other.ActionBarLayout;
import com.harshana.wposandroiposapp.UI.Utils.ClearBatch;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class IssuerMerchantViseSaleReport extends AppCompatActivity implements IssuerMerchantSelectFragment.OnFragmentInteractionListener
{
    BarChart barChart = null;
    DBHelperTransaction transactionDB = null;
    DBHelper configDB = null;

    Button btnGenerate = null;
    EditText txtNumDays = null;

    private  void hide()
    {
        InputMethodManager inputManager = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_issuer_merchant_report);

        //capturePassword(IssuerMerchantViseSaleReport.this);
        Fragment fragment = new IssuerMerchantSelectFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction  = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.placeHolder,fragment);
        fragmentTransaction.commit();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable( new ColorDrawable(getResources().getColor(R.color.colorBlack)));

        ActionBarLayout actionBarLayout = ActionBarLayout.getInstance(this,getResources().getString(R.string.app_name),getResources().getColor(R.color.colorBlack));
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(actionBarLayout.createAndGetActionbarLayoutEx());

        barChart = findViewById(R.id.barChart);
        barChart.getDescription().setEnabled(false);


        //barChart.setFitBars(true);

        transactionDB = DBHelperTransaction.getInstance(getApplicationContext());
        configDB = DBHelper.getInstance(getApplicationContext());

        txtNumDays = findViewById(R.id.txtNumDays);
        txtNumDays.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                barChart.setVisibility(View.INVISIBLE);
            }
        });

        txtNumDays.setOnFocusChangeListener(new View.OnFocusChangeListener()
        {
            @Override
            public void onFocusChange(View v, boolean hasFocus)
            {
                barChart.setVisibility(View.INVISIBLE);
            }
        });

        ((IssuerMerchantSelectFragment) fragment).setOnSelectionMadeListener(new IssuerMerchantSelectFragment.OnSelectionMade()
        {
            @Override
            public void onSelectionMadeNotify(int issuerSelected, int merchantSelected)
            {
                selecterIssuer = issuerSelected;
                selecterMerchant = merchantSelected;
            }
        });

        btnGenerate = findViewById(R.id.btnGenerate);
        btnGenerate.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                String days = txtNumDays.getText().toString();
                try
                {
                    numDays = Integer.valueOf(days);
                }catch (Exception ex)
                {
                    showToast("Please set a numerical value for number of days");
                }

                if (selecterIssuer < 0 || selecterMerchant < 0 || numDays == 0)
                {
                    showToast("Please check your inputs");
                    return;
                }

                hide();
                barChart.setVisibility(View.VISIBLE);
                values = loadData();
                setDataForGraphs();


                //populate and draw the graph
            }
        });
    }


    ArrayList<ValItem> values = null;
    int selecterIssuer = -1;
    int selecterMerchant = -1;

    int numDays = 0;

    String cardBarand = "";

    private ArrayList<ValItem> loadData()
    {
        //load the card brand
        String quary = "SELECT IssuerLable FROM IIT WHERE IssuerNumber = " + selecterIssuer;

        Cursor labelRec = configDB.readWithCustomQuary(quary);
        if (labelRec == null || labelRec.getCount() == 0)
            return null;

        labelRec.moveToFirst();

        //get the name for graph plotting
        cardBarand = labelRec.getString(labelRec.getColumnIndex("IssuerLable"));
        labelRec.close();

        //read the number of transactions performed using the selected card brand using a selected merchant
        //for selected number of days

        Cursor tranRecs ;

        //get the transaction count for each day for selected number of days
        Date today = new Date();
        Calendar calendar = Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("MMdd");

        String strDate = "";


        ArrayList<ValItem> values = new ArrayList<>();

        for (int i = 0; i < numDays; i++)
        {
            //read the record set which was done using a particular card brand
            strDate = dateFormat.format(calendar.getTime());
            calendar.add(Calendar.DATE, -1);
            quary = "SELECT SUM(BaseTransactionAmount) AS Amount FROM STAT WHERE IssuerNumber = " + selecterIssuer + " AND MerchantNumber = " + selecterMerchant + " AND TxnDate = " + "'" + strDate + "'";
            tranRecs = transactionDB.readWithCustomQuary(quary);

            if (tranRecs == null || tranRecs.getCount() == 0)
                continue;

            tranRecs.moveToFirst();

            //get the sum of amount
            long amountVal = tranRecs.getLong(tranRecs.getColumnIndex("Amount"));
            if (amountVal > 0)
            {   double am = (double)amountVal;
                am = am / 100;

                ValItem item = new ValItem(strDate,(long)am);
                values.add(item);
            }


            tranRecs.close();

        }
        return values;

    }

    Toast showToastMessage;
    void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(IssuerMerchantViseSaleReport.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    int index = 0 ;
    private void setDataForGraphs() {
        ArrayList<BarEntry> yValues = new ArrayList<>();
        barChart.clear();

        int listSize = values.size();
        int colors[]  = new int[listSize];

        int r,g,b,a;

        //get the max value within the list
        float maxVal = 0 ;

        for (ValItem item: values) {
            if (item.value > maxVal)
                maxVal = item.value;
        }

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        final ArrayList<String> xLabels = new ArrayList<>();

        index = 0;

        for (ValItem val: values) {
            yValues.add(new BarEntry(index,val.value));

            //setting up the color
            g = 200;
            b = 100;
            a = 255;
            //set the r value based on the y value

            float rValue = (val.value/maxVal) * 255.0f;
            r = (int)rValue;

            int color = Color.argb(a,r,g,b);
            colors[index++] = color;

            String date = val.date;

            date = date.substring(0,2) + "/" + date.substring(3);
            xLabels.add(date);
        }

        xAxis.setGranularityEnabled(true);

        try {
            //setting up the color template
            BarDataSet dataSet = new BarDataSet(yValues," Y");
            dataSet.setColors(colors);
            dataSet.setDrawValues(true);
            BarData data =  new BarData(dataSet);
            barChart.setData(data);
            barChart.invalidate();
            barChart.animateY(1000);
        } catch (Exception ex) {
            ex.printStackTrace();
            showToast("Failed drawing the chart");
        }
    }
}