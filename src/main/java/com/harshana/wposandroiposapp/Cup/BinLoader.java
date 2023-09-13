package com.harshana.wposandroiposapp.Cup;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.Utilities.Formatter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class BinLoader
{
    private static String binFilePath = "";
    private static final String DIR_NAME = "CupBin";
    public static BinLoader instance = null;
    private final static int COMPARING_BIN_LENGTH = 10;

    Context context = null;

    private BinLoader(Context c)
    {
        context = c;
        File binDir = context.getFilesDir();
        binDir = new File (binDir.getPath() + "/" + DIR_NAME);

        if (!binDir.isDirectory())
        {
            binFilePath = null;
            return;
        }

        binFilePath = binDir.getPath();
    }

    public static BinLoader getInstance(Context c)
    {
        if (instance == null)
            instance = new BinLoader(c);

        return instance;
    }



    public interface onStatusUpdate
    {
        void OnStatusUpdate(String status);
    }

    private onStatusUpdate  onStatusUpdate = null;

    public void setOnStatusUpdate( onStatusUpdate listener)
    {
        onStatusUpdate = listener;
    }

    private void callStatusUpdateListener(String status)
    {
        onStatusUpdate.OnStatusUpdate(status);
    }

    private static final int OFFSET_BIN_LENGTH = 111;

    public boolean loadBinInToDB(int referringIssuerNumber)
    {

        Log.d("CUB BIN __---","_____________________");
        long totalCount = 0 ;
        long currentIndex = 0 ;
        String fileName = "";

        if (binFilePath == null)
            return false;

       File dir = new File(binFilePath);
        Log.d("CUB binFilePath"," : "+binFilePath);

        //delete the existing scheme CDT records
        String delQuary = "DELETE FROM CDT WHERE IssuerNumber = " + referringIssuerNumber;
        DBHelper configDB = DBHelper.getInstance(context);

       try
       {
           if ( false == configDB.executeCustomQuary(delQuary))
               return false;
       }catch ( Exception ex) {ex.printStackTrace();}

       try
       {
           for (File f : dir.listFiles())
           {
               BufferedReader bufferedReader = new BufferedReader(new FileReader(f));

               while (bufferedReader.readLine() != null)
                   totalCount++;

               bufferedReader.close();

               fileName = f.getName();

               //re open the file for processing
               bufferedReader = new BufferedReader((new FileReader(f)));
               String line = "";
               int countIndex = 0 ;
               totalCount -= 2;

               while ( (line = bufferedReader.readLine()) != null)
               {
                    if (currentIndex++ == 0 || currentIndex == totalCount ) continue;

                   String strBinLen = "";
                   int iBinLen = 0 ;
                   String bin  = "";
                   String panLow = "";
                   String panHigh = "";
                   try
                    {
                        strBinLen = line.substring(OFFSET_BIN_LENGTH,OFFSET_BIN_LENGTH + 2);
                        iBinLen = Integer.valueOf(strBinLen);

                        //extract the bin
                        bin = line.substring(OFFSET_BIN_LENGTH + 2,OFFSET_BIN_LENGTH + 2 + iBinLen);

                        //construct the pan low and pan high from the bin
                        panLow = Formatter.fillInBack("0",bin,COMPARING_BIN_LENGTH);
                        panHigh = Formatter.fillInBack("9",bin,COMPARING_BIN_LENGTH);

                    }catch ( Exception ex)
                    {
                        ex.printStackTrace();
                        continue;
                    }

                   //records deleted
                   ContentValues values =  new ContentValues();
                   values.put("PANLow",panLow);
                   values.put("PANHigh",panHigh);
                   values.put("CardAbbre","CU");
                   values.put("CardLable","CUP");
                   values.put("TrackRequired","TRACK");
                   values.put("FloorLimit",0);
                   values.put("HostIndex",0);
                   values.put("HostGroup",0);
                   values.put("MinPANDigit",16);
                   values.put("MaxPANDigit",19);
                   values.put("IssuerNumber",referringIssuerNumber);
                   values.put("CheckLuhn",0);
                   values.put("ExpDateRequired",0);
                   values.put("ManualEntry",0);
                   values.put("ChkSvcCode",0);



                   float percentage = (float) (currentIndex - 2) / (float)totalCount * 100.0f;
                   String updateString = "[" + currentIndex + "] CUP BIN [ " + fileName + " --   " + (int)percentage + "% ]";
                   callStatusUpdateListener(updateString);

                  if (false == configDB.insertRecordInTable("CDT",values))
                      return false;
               }

               totalCount = 0;
               currentIndex = 0 ;
               countIndex = 0 ;
               fileName = "";

               bufferedReader.close();
           }
       }catch (Exception ex)
       {
           ex.printStackTrace();
           return false;
       }

       return true;
    }

}
