package com.harshana.wposandroiposapp.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.harshana.wposandroiposapp.Base.Transaction;

/**
 * Created by harshana_m on 11/8/2018.
 */

public class DBHelper extends SQLiteOpenHelper {
    private static final String DBName = "DBConfig.db";

    SQLiteDatabase db;
    Context appContext;
    String dbPath = "";
    private static DBHelper instace = null;

    public String getDBName()
    {
        return DBName;
    }

    public String getDbPath()
    {
        return dbPath;
    }

    public DBHelper(Context context) {
        super(context, DBName, null, 2);
        dbPath = context.getDatabasePath(DBName).getPath();
        appContext = context;
        db = this.getWritableDatabase();

        instace = this;
    }

    public static DBHelper getInstance(Context context) {
        if (instace == null)
            instace = new DBHelper(context);
        return instace;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public boolean setClearBatchFlag(int marchNumber,boolean state)
    {
        String mustSettleQuary  = "";
        boolean retResult = false;
        if (state)
            mustSettleQuary  = "UPDATE MIT SET ClearBatchFlag = 1 WHERE MerchantNumber = " + marchNumber;
        else
            mustSettleQuary  = "UPDATE MIT SET ClearBatchFlag = 0 WHERE MerchantNumber = " + marchNumber;

        try
        {
            retResult = executeCustomQuary(mustSettleQuary);
            return retResult != false;
        }catch (Exception ex)
        {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean setMustSettleFlagState(int marchNumber,boolean state) {
        String mustSettleQuary  = "";
        boolean retResult = false;
        if (state)
            mustSettleQuary  = "UPDATE MIT SET MustSettleFlag = 1 WHERE MerchantNumber = " + marchNumber;
        else
            mustSettleQuary  = "UPDATE MIT SET MustSettleFlag = 0 WHERE MerchantNumber = " + marchNumber;

        try
        {
            retResult = executeCustomQuary(mustSettleQuary);
            return retResult != false;
        }catch (Exception ex)
        {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean insertRecordInTable(String tableName,ContentValues values) {
        try {
            db.insert(tableName,null,values);
            return true;
        } catch ( Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean saveInvoiceNumber(Transaction tran) {
        String invQuary = "UPDATE MIT SET InvNumber = " + tran.inInvoiceNumber + " WHERE MerchantNumber = " + tran.merchantNumber;

        try {
            db.execSQL(invQuary);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String getFieldsForTIDRequest(int host){
        String str63 = "";
        String GS = "~", RS = "#";

        String allTIDQuery = "SELECT * FROM TMIF WHERE TerminalID != '00000000' AND TerminalID != '0' AND HostId = " + host + " GROUP BY TerminalID ORDER BY ID";
        Cursor tidRec = db.rawQuery(allTIDQuery,null);

        if (tidRec.getCount() == 0)
            return null;

        while (tidRec.moveToNext()) {
            if(!tidRec.isFirst()){
                str63 += RS;
            }
            int merchantNo = tidRec.getInt(tidRec.getColumnIndex("MerchantNumber"));
            Cursor curMerch = getMID(merchantNo);
            String TID = tidRec.getString(tidRec.getColumnIndex("TerminalID"));
            String merchantID = curMerch.getString(curMerch.getColumnIndex("MerchantID"));

            str63 += TID + GS + merchantID;
            curMerch.close();
        }
        tidRec.close();
        return str63;
    }

    public Cursor getMID(int merchantID) {
        String merchQuery = "SELECT * FROM MIT WHERE MerchantNumber = " + merchantID;
        Cursor midRec = db.rawQuery(merchQuery,null);

        if (midRec.getCount() == 0)
            return null;

        midRec.moveToFirst();
        return midRec;
    }

    public Cursor loadTerminal(int merchantNum) {
        String quary = "select * " + "from TMIF,MIT " + "WHERE TMIF.MerchantNumber = MIT.MerchantNumber AND TMIF.MerchantNumber = " + merchantNum;
        Cursor terminalRec = null;

        try {
            terminalRec = db.rawQuery(quary,null);
            if (terminalRec.getCount() == 0)
                return null;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        terminalRec.moveToFirst();
        return terminalRec;
    }

    public String loadQRMID() {
        String query = "SELECT * FROM PST";
        Cursor qrRec = db.rawQuery(query, null);

        if(qrRec.getCount() == 0)
            return null;

        qrRec.moveToFirst();

        return qrRec.getString(qrRec.getColumnIndex("qr_mid"));
    }

    public String getCurrency(int merchantId) {
        String currency;
        String query = "SELECT * FROM CST WHERE ID = " + merchantId;

        Cursor curQuery = db.rawQuery(query,null);

        if (curQuery.getCount() == 0)
            return null;

        curQuery.moveToFirst();

        try {
            currency = curQuery.getString(curQuery.getColumnIndex("CurrencySymbol"));
        } catch (Exception e) {
            e.printStackTrace();
            curQuery.close();
            return null;
        }
        curQuery.close();
        return currency;
    }

    public String loadProfileIPPort() {
        String ip,port;
        String query = "SELECT * FROM PST";
        Cursor qrRec = db.rawQuery(query, null);

        if(qrRec.getCount() == 0)
            return null;

        qrRec.moveToFirst();

        ip = qrRec.getString(qrRec.getColumnIndex("profile_IP"));
        port = qrRec.getString(qrRec.getColumnIndex("profile_PORT"));

        return ip + ":" + port;
    }

    public long loadMaxAmount() {
        long defaultAmt = 99999999;
        String query = "SELECT * FROM PST";
        Cursor qrRec = db.rawQuery(query, null);

        if(qrRec.getCount() == 0)
            return defaultAmt;

        qrRec.moveToFirst();

        return qrRec.getLong(qrRec.getColumnIndex("max_amt"));
    }

    public Cursor loadIssuer(int issuerID) {
        String queary = "SELECT * FROM IIT WHERE IssuerNumber = " + issuerID;
        Cursor issuerRec = db.rawQuery(queary,null);

        if (issuerRec.getCount() == 0)
            return null;

        issuerRec.moveToFirst();
        return issuerRec;
    }

    public Cursor readWithCustomQuary(String quary) {
        try {
            Cursor result = db.rawQuery(quary,null);
            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public Cursor findCardRecord(String pan) {
        String cardLable;
        String comp = pan.substring(0,6);
        String queary = "SELECT * FROM CDT WHERE substr(PANLow,1,6) <= '" + comp + "' AND substr(PANHigh,1,6) >= '" + comp + "'";
        Cursor cardRecs = null;
        Log.d("PAN COMP", " : " + comp);

        try {
            cardRecs =  db.rawQuery(queary,null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (cardRecs.getCount() == 0)
            return null;
        else if (cardRecs.getCount() > 1) {
            while (cardRecs.moveToNext()) {
                cardLable = cardRecs.getString(cardRecs.getColumnIndex("CardLable"));
                if (cardLable.equals("CUP")) {
                    return cardRecs;
                }
            }
            cardRecs.moveToFirst();
            return cardRecs;
        }
        else {
            cardRecs.moveToFirst();
            return cardRecs;
        }
    }

    public boolean executeCustomQuary(String quary) {
        try {
            db.execSQL(quary);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}