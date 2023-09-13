package com.harshana.wposandroiposapp.Settings;

import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.harshana.wposandroiposapp.ColorScheme.CLScheme;
import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.UI.Other.ActionBarLayout;
import com.harshana.wposandroiposapp.UI.Utils.ClearBatch;

import java.util.ArrayList;

public class TableConfigActivity extends AppCompatActivity {
    DBHelper configDatabase  = null;
    GridLayout gridContrl = null;
    Cursor curRecSet = null;
    Button btnPrev = null;
    Button btnNext = null;
    Button btnSeek = null;
    Button btnSave = null;
    Button btnExit = null;
    EditText  txtRecIndex = null;
    Spinner cmbTableList = null;
    String tableName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_config);

        gridContrl = findViewById(R.id.controlGrid);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable( new ColorDrawable(getResources().getColor(R.color.colorBlack)));

        //ActionBarLayout actionBarLayout = ActionBarLayout.getInstance(this,getResources().getString(R.string.app_name),getResources().getColor(R.color.colorBlack));
        //actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        //actionBar.setCustomView(actionBarLayout.createAndGetActionbarLayoutEx());
        configDatabase = DBHelper.getInstance(getApplicationContext());
        curRecSet = readTableAndPlaceLayout("IIT");

        tableName = "IIT";

        if (curRecSet != null)
            populateRecordOnControls(curRecSet);

        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnSeek = findViewById(R.id.btnSeek);
        txtRecIndex = findViewById(R.id.txtRecNumber);
        btnSave = findViewById(R.id.btnSave);
        btnExit = findViewById(R.id.btnExit);

        btnPrev.setOnClickListener(clickListener);
        btnNext.setOnClickListener(clickListener);
        btnSeek.setOnClickListener(clickListener);
        btnSave.setOnClickListener(clickListener);
        btnExit.setOnClickListener(clickListener);

        cmbTableList = findViewById(R.id.cmbTableList);

        btnPrev.requestFocusFromTouch();

        final ArrayList<String> tableList = new ArrayList<>();
        tableList.add("CDT");
        tableList.add("MIT");
        tableList.add("TMIF");
        tableList.add("IIT");
        tableList.add("IHT");
        tableList.add("PST");
        tableList.add("CST");

        ArrayAdapter<String> arrayAdapter =  new ArrayAdapter<>(this,R.layout.spinner_item,tableList);
        cmbTableList.setAdapter(arrayAdapter);

        cmbTableList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                tableName =  tableList.get(i);
                curRecSet = readTableAndPlaceLayout(tableName);

                if (curRecSet != null)
                    populateRecordOnControls(curRecSet);

                CLScheme.applyColorScheme(gridContrl);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        if (CLScheme.isThemeEnabled) {
            actionBar.setBackgroundDrawable( new ColorDrawable(CLScheme.getWindowColor()));
            Spannable text = new SpannableString(actionBar.getTitle());
            text.setSpan(new ForegroundColorSpan(Color.BLACK), 0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            actionBar.setTitle("");
        }

        LinearLayout linearLayout = findViewById(R.id.idBaseLayoutTableConfig);
        CLScheme.applyColorScheme(linearLayout);

        RelativeLayout relativeLayout  = findViewById(R.id.idSubLayoutTableConfig);
        CLScheme.applyColorScheme(relativeLayout);
        CLScheme.applyColorScheme(gridContrl);
    }

    protected Cursor readTableAndPlaceLayout(String tableName) {
        String quary = "SELECT * FROM " + tableName;

        Cursor tableData = configDatabase.readWithCustomQuary(quary);
        if (tableData == null || tableData.getCount() == 0)
            return null;

        //remove existing controllers if there is any
        gridContrl.removeAllViews();

        int colCount  = tableData.getColumnCount();
        tableData.moveToFirst();

        int dataType = 0;
        for (int i = 0; i < colCount; i++) {
            //determine the type of the column
            dataType = tableData.getType(i);

            //generate label and edit tex boxes for each coloumn
            TextView label =  new TextView(this);
            EditText editText =  new EditText(this);

            String colName = tableData.getColumnName(i);
            label.setText(colName);
            label.setTag(colName);
            label.setTextColor(Color.WHITE);
            label.setWidth(350);

            editText.setId(i);
            editText.setTag(colName);
            editText.setTextColor(Color.WHITE);

            gridContrl.addView(label);
            gridContrl.addView(editText);
        }

        return tableData;
    }

    protected void populateRecordOnControls(Cursor recSet) {
        //recSet.moveToFirst();
        int colCount  = recSet.getColumnCount();

        for (int colIndex = 0 ; colIndex < colCount; colIndex++) {
            String colName = recSet.getColumnName(colIndex);
            View control = null;
            if ( (control = getChildWithTag(colName)) == null)
                return;

            //select only edit text boxes
            if (control instanceof EditText) {
                EditText editBox = (EditText) control;
                String data = "";
                if (recSet.getType(colIndex) == Cursor.FIELD_TYPE_INTEGER)
                    data = String.valueOf(recSet.getInt(colIndex));
                else if (recSet.getType(colIndex) == Cursor.FIELD_TYPE_STRING)
                    data = recSet.getString(colIndex);

                editBox.setText(data);
            }
        }

        CLScheme.applyColorScheme(gridContrl);
    }

    View getChildWithTag(String tag) {
        int childCount = gridContrl.getChildCount();

        for (int  i = 0 ; i < childCount; i++) {
            View child = gridContrl.getChildAt(i);
            String tagChild = child.getTag().toString();

            if (tag.equals(tagChild) && child instanceof EditText)
                return child;
        }

        return null;
    }

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view == btnPrev) {
                if (curRecSet.moveToPrevious())
                    populateRecordOnControls(curRecSet);
                else {
                    curRecSet.moveToLast();
                    populateRecordOnControls(curRecSet);
                }
            }
            else if (view == btnNext) {
                if (curRecSet.moveToNext())
                    populateRecordOnControls(curRecSet);
                else {
                    curRecSet.moveToFirst();
                    populateRecordOnControls(curRecSet);
                }
            }
            else if (view == btnSeek) {
                int recIndex = Integer.valueOf(txtRecIndex.getText().toString());
                try {
                    curRecSet.moveToPosition(recIndex);
                    populateRecordOnControls(curRecSet);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            else if (view == btnSave) {
                if (saveCurRecord(tableName)) {
                    curRecSet = readTableAndPlaceLayout(tableName);
                    if (curRecSet != null)
                        populateRecordOnControls(curRecSet);
                }
            }
            else if (view == btnExit) {
                finish();
            }
        }
    };

    protected  boolean saveCurRecord(String tableName) {
        //validate all the fields if anything contains empty field
        int conCount = gridContrl.getChildCount();

        //generate the dynamic quary for update
        String updateQuary  = "UPDATE " + tableName + " SET ";

        //skip over the first field (primary key should not be update through quary)
        for ( int c = 2; c < conCount; c++) {
            View control = gridContrl.getChildAt(c);

            if (control instanceof  EditText) {
                String colName = control.getTag().toString();
                int colDataType = curRecSet.getType(curRecSet.getColumnIndex(colName));

                try{
                    if (colDataType == Cursor.FIELD_TYPE_INTEGER)
                        updateQuary += colName + " = " + Integer.valueOf(((EditText) control).getText().toString()) + ",";
                    else if (colDataType == Cursor.FIELD_TYPE_STRING) {
                        String str = ((EditText) control).getText().toString();
                        if(str.contains("'")) {
                            str = str.replace("'", "''");
                        }
                        updateQuary += colName + " = " + "'" + str + "'" + ",";
                    }
                    else if(tableName.equals("MIT") && colDataType == Cursor.FIELD_TYPE_NULL) {
                        String str = ((EditText) control).getText().toString();
                        if(str.contains("'")) {
                            str = str.replace("'", "''");
                        }
                        updateQuary += colName + " = " + "'" + str + "'" + ",";
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showToast("Invalid Data Type");
                    return false;
                }
            }
        }

        updateQuary = updateQuary.substring(0,updateQuary.length() - 1);

        View control = gridContrl.getChildAt(1) ; // this must always be the primary key of the table

        String primaryKeyColName = "";
        if (control instanceof EditText)
            primaryKeyColName = control.getTag().toString();

        String primaryData = ((EditText) control).getText().toString();
        //set the where condition
        updateQuary += " WHERE " + primaryKeyColName + " = " + primaryData;

        if ( !configDatabase.executeCustomQuary(updateQuary)) {
            showToast("Saving the current record failed");
            return false;
        }
        else {
            showToast("Successfully Saved");
            return true;
        }
    }

    @Override
    public void onBackPressed() {

    }

    Toast showToastMessage;
    void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(TableConfigActivity.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }
}