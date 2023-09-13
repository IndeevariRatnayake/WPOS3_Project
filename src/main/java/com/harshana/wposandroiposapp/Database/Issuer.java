package com.harshana.wposandroiposapp.Database;


import android.database.Cursor;

/**
 * Created by harshana_m on 11/9/2018.
 */

public class Issuer
{
    public int issuerNumber;
    public String issuerAbbr;
    public String issuerLabel;
    public String maskCustCopy;
    public String maskMerchantCopy;
    public String IP;
    public int port;
    public String NII;
    public String secureNII;
    public String TPDU;


    int colIndex  = 0;

    public Issuer(Cursor rec)
    {
        try
        {
            colIndex = rec.getColumnIndex("IssuerNumber");
            issuerNumber = rec.getInt(colIndex);

            colIndex = rec.getColumnIndex("IssuerAbbrev");
            issuerAbbr = rec.getString(colIndex);

            colIndex = rec.getColumnIndex("IssuerLable");
            issuerLabel = rec.getString(colIndex);

            colIndex = rec.getColumnIndex("MaskCustomerCopy");
            maskCustCopy = rec.getString(colIndex);

            colIndex = rec.getColumnIndex("MaskMerchantCopy");
            maskMerchantCopy = rec.getString(colIndex);

            IP = rec.getString(rec.getColumnIndex("IP"));
            port = rec.getInt(rec.getColumnIndex("Port"));

            NII = rec.getString(rec.getColumnIndex("NII"));
            secureNII = rec.getString(rec.getColumnIndex("SecureNII"));
            TPDU = rec.getString(rec.getColumnIndex("TPDU"));


        }catch ( Exception e)
        {
            e.printStackTrace();
        }


    }
}


