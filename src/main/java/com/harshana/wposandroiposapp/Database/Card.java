package com.harshana.wposandroiposapp.Database;



import android.database.Cursor;
import android.util.Log;

/**
 * Created by harshana_m on 10/25/2018.
 */

//Contains the selected cardData details
public class Card
{
    public String panLow;
    public String panHigh;
    public String cardAbbr;
    public String cardLabel;
    public String trackRequired;
    public String transacctionBitmap;
    public long floorLimit;
    public int hostIndex;
    public int hostGroup;
    public int minPanDigits;
    public int maxPanDigits;
    public int issuerNumber;
    public boolean checkLuhn;
    public boolean expDataRequired;
    public boolean manualEntry;
    public boolean chkSvcCode;

    int colIndex  = 0;

    //initialize the cardData record
    public Card(Cursor rec)
    {
        colIndex = rec.getColumnIndex("PANLow");
        panLow = rec.getString(colIndex);

        colIndex = rec.getColumnIndexOrThrow("PANHigh");
        panHigh = rec.getString(colIndex);

        colIndex = rec.getColumnIndex("CardAbbre");
        cardAbbr = rec.getString(colIndex);

        colIndex = rec.getColumnIndex("CardLable");
        cardLabel = rec.getString(colIndex);
        Log.d("CARD LABEL 01"," : "+cardLabel);

        colIndex = rec.getColumnIndex("TrackRequired");
        trackRequired = rec.getString(colIndex);

        colIndex = rec.getColumnIndex("TxnBitmap");
        transacctionBitmap = rec.getString(colIndex);

        colIndex = rec.getColumnIndex("FloorLimit");
        floorLimit = rec.getInt(colIndex);

        colIndex = rec.getColumnIndex("MinPANDigit");
        minPanDigits = rec.getInt(colIndex);

        colIndex = rec.getColumnIndex("MaxPANDigit");
        maxPanDigits = rec.getInt(colIndex);

        colIndex = rec.getColumnIndex("IssuerNumber");
        issuerNumber = rec.getInt(colIndex);

        colIndex = rec.getColumnIndex("CheckLuhn");
        checkLuhn = rec.getInt(colIndex) > 0;

        colIndex = rec.getColumnIndex("ExpDateRequired");
        expDataRequired = rec.getInt(colIndex) > 0;

        colIndex = rec.getColumnIndex("ManualEntry");
        manualEntry = rec.getInt(colIndex) > 0;

        colIndex = rec.getColumnIndex("ChkSvcCode");
        chkSvcCode = rec.getInt(colIndex) > 0;

    }
}
