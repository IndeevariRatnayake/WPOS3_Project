package com.harshana.wposandroiposapp.UI.Reports;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.Database.DBHelperTransaction;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.UI.Fregments.CustomAdapterIssuer;
import com.harshana.wposandroiposapp.UI.Fregments.IssuerItem;
import com.harshana.wposandroiposapp.UI.Other.ActionBarLayout;
import com.harshana.wposandroiposapp.UI.Utils.ClearBatch;
import com.harshana.wposandroiposapp.Utilities.Formatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class BankViseSaleReport extends AppCompatActivity
{

    DBHelperTransaction transactionDB = null;
    DBHelper configDB = null;

    ListView lvIssuer = null;

    TextView txtStartDate = null;
    TextView txtEndDate = null;
    Button btnGenerate = null;

    PieChart pieChart = null;


    int year,month,date;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank_wise_sale_report);

        //capturePassword(BankViseSaleReport.this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable( new ColorDrawable(getResources().getColor(R.color.colorBlack)));

        ActionBarLayout actionBarLayout = ActionBarLayout.getInstance(this,getResources().getString(R.string.app_name),getResources().getColor(R.color.colorBlack));
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(actionBarLayout.createAndGetActionbarLayoutEx());

        transactionDB = DBHelperTransaction.getInstance(getApplicationContext());
        configDB = DBHelper.getInstance(getApplicationContext());

        lvIssuer = findViewById(R.id.lvIssuer);
        lvIssuer.setOnItemClickListener(itemClickListener);

        loadIssuers();

        txtStartDate = findViewById(R.id.txtStartDate);
        txtEndDate = findViewById(R.id.txtEndDate);
        btnGenerate = findViewById(R.id.btnGenerate);

        Calendar calender = Calendar.getInstance();

        year = calender.get(Calendar.YEAR);
        month = calender.get(Calendar.MONTH);
        date = calender.get(Calendar.DAY_OF_MONTH);

        txtStartDate.setOnClickListener(clickListener);
        txtEndDate.setOnClickListener(clickListener);

        loadBinData();
        btnGenerate.setOnClickListener(clickListener);

    }

    String selIssuerName = "";
    int selIssuerNumber =  -1;

    AdapterView.OnItemClickListener itemClickListener  = new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
           IssuerItem issuer = issuerList.get(position);

           selIssuerName = issuer.issuerName;
           selIssuerNumber = issuer.issuerNumber;
        }
    };


    View.OnClickListener clickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            if (v == txtStartDate)
            {
                whichDate = 1;
                showDialog(999);
            }
            else if (v == txtEndDate)
            {
                whichDate = 2;
                showDialog(999);
            }
            else if (v == btnGenerate)
            {
                drawGraph();
            }
        }
    };

    private void drawGraph()
    {
        //validate the user inputs
        if (selIssuerNumber <= 0 )
        {
            showToast("Please select a card brand to proceed");
            return;
        }
        else if (isStartDateSet == false)
        {
            showToast("Please set start date of the date range");
            return;
        }
        else if (isEndDateSet == false)
        {
            showToast("Please set end date of the date range");
            return;
        }

        loadData();


    }

    boolean isStartDateSet = false;
    boolean isEndDateSet = false;
    int whichDate = 0;


    String strStartDate = "";
    String strEndDate = "";

    DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener()
    {
        @Override
        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth)
        {
            String date = (month + 1) + "/" + dayOfMonth;

            if (whichDate == 1)
            {
                txtStartDate.setText("Start Date : " + date);
                isStartDateSet = true;
                strStartDate = Formatter.fillInFront("0",String.valueOf(month + 1),2) + Formatter.fillInFront("0",String.valueOf(dayOfMonth),2);
            }
            else if (whichDate == 2)
            {
                txtEndDate.setText("End Date   : " + date);
                strEndDate = Formatter.fillInFront("0",String.valueOf(month + 1),2) + Formatter.fillInFront("0",String.valueOf(dayOfMonth),2);
                isEndDateSet = true;
            }
        }
    };

    @Override
    protected Dialog onCreateDialog(int id)
    {
        if (id == 999)
            return new DatePickerDialog(this,dateSetListener,year,month,date);
        return null;

    }

    Toast showToastMessage;
    void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(BankViseSaleReport.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }

    ArrayList<IssuerItem> issuerList = null;
    CustomAdapterIssuer issuerAdapter = null;

    //load the issuers in to the list
    void loadIssuers() {
        String quary  = "SELECT * FROM IIT";

        Cursor issuerRec = configDB.readWithCustomQuary(quary);
        if (issuerRec == null || issuerRec.getCount() == 0)
            return;


        issuerList = new ArrayList();
        issuerAdapter  =  new CustomAdapterIssuer(this,android.R.layout.simple_expandable_list_item_1, issuerList);
        lvIssuer.setAdapter(issuerAdapter);

        String issuerName = "";
        int issuerNumber = 0;

        while (issuerRec.moveToNext())
        {
            //get the issuer nme
            issuerName = issuerRec.getString(issuerRec.getColumnIndex("IssuerLable"));
            issuerNumber  = issuerRec.getInt(issuerRec.getColumnIndex("IssuerNumber"));

            IssuerItem issuer =  new IssuerItem(issuerNumber,issuerName);
            issuerList.add(issuer);
        }

        issuerAdapter.notifyDataSetChanged();
        issuerRec.close();

    }



    class BinObject
    {
        public int binLow ;
        public int binHi ;
        public String bank = "";
    }

    List<BinObject> binBlockList = null;


    //loading the data in to the array list
    void loadBinData()
    {
        binBlockList =  new ArrayList();

        //load all the records from the bin blocks
        String quary  = "SELECT BinLo,BinHi,NAME as Bank FROM BINLIST,BANKS WHERE BINLIST.BankID = BANKS.ID";

        /*"SELECT * FROM BINLIST";*/

        Cursor bins =  configDB.readWithCustomQuary(quary);

        if (bins == null || bins.getCount() == 0)
        {
            showToast("No Data found in bin list ");
            return;
        }

        BinObject bin = null;
        while (bins.moveToNext())
        {
            bin =  new BinObject();
            bin.binLow = Integer.valueOf(bins.getString(bins.getColumnIndex("BinLo")));
            bin.binHi = Integer.valueOf(bins.getString(bins.getColumnIndex("BinHi")));
            bin.bank = bins.getString(bins.getColumnIndex("Bank"));

            binBlockList.add(bin);
        }

        bins.close();

    }


    int totalNumTrans = 0 ;
    long totAmount = 0;

    void loadData()
    {
        String quary = "SELECT * FROM STAT WHERE IssuerNumber = " + selIssuerNumber;

        Cursor tranRecs  =  transactionDB.readWithCustomQuary(quary);

        if (tranRecs == null || tranRecs.getCount() == 0)
        {
            showToast("No data to display");
            return;
        }


        //filter in the data which lies in the selected date ranges
        Date dateStart;
        Date dateEnd;
        try
        {
            dateStart =  new SimpleDateFormat("MMdd").parse(strStartDate);
            dateEnd = (new SimpleDateFormat("MMdd").parse(strEndDate));

        }catch (Exception ex)
        {
            return;
        }

        //check the greater date which has been entered by the user
        if (dateStart.after(dateEnd))
        {
            showToast("Start Date and End Date Is taken reversed for the date filtering");
            Date temp = dateStart;
            dateStart  = dateEnd;
            dateEnd = temp;
        }


        //now we filter all the data that we have gathered
        Date txnDate = null;
        //search the bin list
        repData = new ArrayList<>();



       while (tranRecs.moveToNext())
       {
           totalNumTrans++;
           totAmount += tranRecs.getLong(tranRecs.getColumnIndex("BaseTransactionAmount"));

           String strTranDate  = tranRecs.getString(tranRecs.getColumnIndex("TxnDate"));
           try
           {
               txnDate = new SimpleDateFormat("MMdd").parse(strTranDate);
           }catch (Exception ex){}


           if (txnDate.getTime() != dateStart.getTime())
           {
               if (txnDate.getTime() != dateEnd.getTime())
               {
                   if (txnDate.before(dateStart) || txnDate.after(dateEnd)) //out of the range so we ignore
                       continue;

               }
           }

           //if the tran lies within the date range then we check in the available bin
           //check whether the particular card in a particular bin
           String bin = tranRecs.getString(tranRecs.getColumnIndex("Bin"));
           int intBin = Integer.valueOf(bin);



           for(BinObject b : binBlockList)
           {
               if((intBin >= b.binLow) && (intBin <= b.binHi))
               {
                   String bankName = b.bank;

                   //check whether the bank already has an entry
                   if (repData.size() == 0)
                   {
                       ReportData reportData =  new ReportData();
                       reportData.bankName = bankName;
                       reportData.numTrans = 1;
                       reportData.amountAccum = tranRecs.getLong(tranRecs.getColumnIndex("BaseTransactionAmount"));
                       repData.add(reportData);
                   }
                   else
                   {
                       boolean foundRepData = false;

                       for (ReportData r: repData)
                       {
                           if (r.bankName.equals(bankName))
                           {
                               r.amountAccum += tranRecs.getLong(tranRecs.getColumnIndex("BaseTransactionAmount"));
                               r.numTrans++;
                               foundRepData = true;
                               break;
                           }
                       }

                       //if the repdata is not found then we insert an entry
                       if (!foundRepData)
                       {
                           ReportData reportData =  new ReportData();
                           reportData.bankName = bankName;
                           reportData.amountAccum += tranRecs.getLong(tranRecs.getColumnIndex("BaseTransactionAmount"));
                           reportData.numTrans++;
                           repData.add(reportData);
                       }
                   }
                   break;

               }
           }

       }

       tranRecs.close();
       //data loading is  finished
        plotTheGraph();

    }

    private void plotTheGraph()
    {

        pieChart = findViewById(R.id.pieChart);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5,10,5,5);
        pieChart.setDragDecelerationFrictionCoef(0.99f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(64f);

        ArrayList<PieEntry> yValues = new ArrayList<>();

        for (ReportData r : repData)
        {
            float val = ((float)r.amountAccum / totAmount) * 100f;
            yValues.add(new PieEntry(val,r.bankName));
        }



        pieChart.animateY(1000, Easing.EaseInOutCubic);
        PieDataSet pieDataSet =  new PieDataSet(yValues,"Banks");
        pieDataSet.setSliceSpace(3f);
        pieDataSet.setSelectionShift(5f);
        pieDataSet.setColors(ColorTemplate.JOYFUL_COLORS);

        PieData pieData = new PieData(pieDataSet);
        pieData.setValueTextSize(10f);
        pieData.setValueTextColor(Color.YELLOW);


        pieChart.setData(pieData);
        pieChart.invalidate();




    }


    class ReportData
    {
        public String bankName = "";
        public int numTrans = 0;
        public long amountAccum = 0;
    }

    List<ReportData> repData = null;


}
