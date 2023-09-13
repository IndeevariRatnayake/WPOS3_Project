package com.harshana.wposandroiposapp.Database;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.harshana.wposandroiposapp.Base.HostIssuer;
import com.harshana.wposandroiposapp.Base.IssuerHostMap;
import com.harshana.wposandroiposapp.Base.QRTran;
import com.harshana.wposandroiposapp.Base.SettlementData;
import com.harshana.wposandroiposapp.Base.Transaction;
import com.harshana.wposandroiposapp.DevArea.TranStaticData;

import java.text.SimpleDateFormat;
import java.time.Period;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Created by harshana_m on 11/8/2018.
 */

public class DBHelperTransaction extends SQLiteOpenHelper {
    private static final String DBName = "DBTransactions.db";

    SQLiteDatabase db;
    Context appContext;
    String dbPath = "";
    private static DBHelperTransaction instance = null;

    public DBHelperTransaction(Context context) {
        super(context, DBName, null, 2);
        dbPath = context.getDatabasePath(DBName).getPath();
        appContext = context;
        db = this.getWritableDatabase();

        instance = this;
    }

    public static DBHelperTransaction getInstance(Context context) {
        if (instance == null)
            instance = new DBHelperTransaction(context);

        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public Cursor getRerversal(int merchNumber) {
        String quary = "SELECT * FROM RVSL WHERE MerchantNumber = " + merchNumber;

        Cursor rvsl = db.rawQuery(quary,null);
        if (rvsl.getCount() == 0)
            return null;

        return rvsl;
    }

    public int getTranCountIssuer(int issuer, int merchantNumber) {
        String quary = "SELECT * FROM TXN WHERE Voided = 0 AND ActIssuerNumber = " + issuer + " AND MerchantNumber = " + merchantNumber + " AND TransactionCode != " + TranStaticData.TranTypes.PRE_AUTH;

        try {
            Cursor cnt = db.rawQuery(quary, null);

            return cnt.getCount();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public long getTranAmountIssuer(int issuer, int merchantNumber) {
        String quary = "SELECT SUM(BaseTransactionAmount) FROM TXN WHERE Voided = 0 AND ActIssuerNumber = " + issuer + " AND MerchantNumber = " + merchantNumber + " AND TransactionCode != " + TranStaticData.TranTypes.PRE_AUTH;

        try {
            Cursor amt = db.rawQuery(quary, null);
            if (amt.getCount() == 0)
                return 0;
            amt.moveToFirst();

            return amt.getLong(amt.getColumnIndex("SUM(BaseTransactionAmount)"));
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int getVoidTranCountMerch(int merch) {
        String quary = "SELECT * FROM TXN WHERE Voided = 1 AND MerchantNumber = " + merch + " AND TransactionCode != " + TranStaticData.TranTypes.PRE_AUTH ;

        try {
            Cursor cnt = db.rawQuery(quary, null);

            return cnt.getCount();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public long getVoidTranAmountMerch(int merch) {
        String quary = "SELECT SUM(BaseTransactionAmount) FROM TXN WHERE Voided = 1 AND MerchantNumber = " + merch + " AND TransactionCode != " + TranStaticData.TranTypes.PRE_AUTH;

        try {
            Cursor amt = db.rawQuery(quary, null);
            if (amt.getCount() == 0)
                return 0;
            amt.moveToFirst();

            return amt.getLong(amt.getColumnIndex("SUM(BaseTransactionAmount)"));
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public long getTranAmountSALE(int merch) {
        String query = "SELECT SUM(BaseTransactionAmount) FROM TXN WHERE MerchantNumber = " + merch + " AND Voided = 0 AND TransactionCode = " + TranStaticData.TranTypes.SALE;

        try {
            Cursor amt = db.rawQuery(query, null);
            if (amt.getCount() == 0)
                return 0;
            amt.moveToFirst();

            return amt.getLong(amt.getColumnIndex("SUM(BaseTransactionAmount)"));
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public boolean writeReversal(Transaction tran) {
        ContentValues values =  new ContentValues();
        values.put("InvoiceNumber",tran.inInvoiceNumber);
        values.put("TraceNumber",tran.traceNumber);
        values.put("TotalAmount",tran.lnTotalAmount);
        values.put("TxnDate",tran.Date);
        values.put("TxnTime",tran.Time);
        values.put("Host",0);
        values.put("MerchantNumber",tran.merchantNumber);
        values.put("ServiceCrg",tran.serviceCharge);
        values.put("TipAmount",tran.tipAmount);
        values.put("FuelCharge",tran.fuelCharge);
        values.put("[Credit/Debit]",tran.tranCreditOrDebit);
        values.put("ApproveCode",tran.approveCode);
        values.put("RRN",tran.RRN);
        values.put("Discount",tran.discount);
        values.put("MTI",tran.MTI);
        values.put("ProcCode",tran.procCode);
        values.put("TransactionCode",tran.inTransactionCode);
        values.put("ChipStatus",tran.inChipStatus);
        values.put("BaseTransactionAmount",tran.lnBaseTransactionAmount);
        values.put("PAN",tran.PAN);
        values.put("CardSNumber",tran.cardSerialNo);
        values.put("track2",tran.track2);
        values.put("svcCode",tran.scvCode);
        values.put("expDate",tran.expDate);
        values.put("TerminalID",tran.terminalID);
        values.put("MerchantID",tran.merchantID);
        values.put("MerchantName",tran.merchantName);
        values.put("NII",tran.NII);
        values.put("SecureNII",tran.secureNII);
        values.put("TPDU",tran.TPDU);
        values.put("emvField55",tran.emv55Data);
        values.put("ResponoseCode",tran.responseCode);
        values.put("CdtIndex",tran.cdtIndex);
        values.put("IssuerNumber",tran.issuerNumber);
        long result =  0;
        try
        {
            result =  db.insert("RVSL",null,values);
            if (result == -1)
                return false;

        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return true;
    }

    public boolean writeLastSettlement(SettlementData data, int merchantNumber) {
        String deleteQuery = "DELETE FROM LASTSETTLEMENT WHERE MerchantNumber = " + merchantNumber;

        ContentValues values =  new ContentValues();
        values.put("Date",data.Date);
        values.put("Time",data.Time);
        values.put("BatchNumner",data.BatchNumber);
        values.put("Host",data.HostID);
        values.put("VisaCount",data.VisaCount);
        values.put("VisaTotal",data.VisaAmount);
        values.put("MasterCount",data.MasterCount);
        values.put("MasterTotal",data.MasterAmount);
        values.put("CupCount",data.CupCount);
        values.put("CupTotal",data.CupAmount);
        values.put("CardTotals",data.CardTotals);
        values.put("SubTotal",data.SubTotal);
        values.put("VoidCount",data.VoidCount);
        values.put("VoidTotal",data.VoidAmount);
        values.put("MerchantID",data.MerchantID);
        values.put("TerminalID",data.TerminalID);
        values.put("MerchantNumber", merchantNumber);

        long result = 0;

        try {
            db.execSQL(deleteQuery);
            result = db.insert("LASTSETTLEMENT",null,values);

            if (result == -1 )
                return  false;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return true;
    }

    public SettlementData getLastSettlement(Cursor result) {
        SettlementData settlementData =  new SettlementData();

        try {
            settlementData.Date = result.getString(result.getColumnIndex("Date"));
            settlementData.Time = result.getString(result.getColumnIndex("Time"));
            settlementData.BatchNumber = result.getString(result.getColumnIndex("BatchNumner"));
            settlementData.HostID = result.getInt(result.getColumnIndex("Host"));
            settlementData.VisaCount = result.getInt(result.getColumnIndex("VisaCount"));
            settlementData.VisaAmount = result.getLong(result.getColumnIndex("VisaTotal"));
            settlementData.MasterCount = result.getInt(result.getColumnIndex("MasterCount"));
            settlementData.MasterAmount = result.getLong(result.getColumnIndex("MasterTotal"));
            settlementData.CupCount = result.getInt(result.getColumnIndex("CupCount"));
            settlementData.CupAmount = result.getLong(result.getColumnIndex("CupTotal"));
            settlementData.CardTotals = result.getInt(result.getColumnIndex("CardTotals"));
            settlementData.SubTotal = result.getLong(result.getColumnIndex("SubTotal"));
            settlementData.VoidCount = result.getInt(result.getColumnIndex("VoidCount"));
            settlementData.VoidAmount = result.getLong(result.getColumnIndex("VoidTotal"));
            settlementData.MerchantID = result.getString(result.getColumnIndex("MerchantID"));
            settlementData.TerminalID = result.getString(result.getColumnIndex("TerminalID"));
            settlementData.merchantNumber = result.getInt(result.getColumnIndex("MerchantNumber"));
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return  settlementData;
    }

    public static int getHostGroupRef(int issuerNumber) {

        int numHosts = IssuerHostMap.numHostEntries;
        int curIssuer  = issuerNumber;

        for (int host = 0 ; host < numHosts; host++) {
            HostIssuer hostSel = IssuerHostMap.hosts[host];
            List<Integer> issuers = hostSel.issuerList;
            if (issuers.contains(curIssuer))
                return host;
        }

        return -1;
    }

    public boolean writeTransaction(Transaction tran) {
        ContentValues values =  new ContentValues();
        values.put("InvoiceNumber",tran.inInvoiceNumber);
        values.put("TraceNumber",tran.traceNumber);
        values.put("TotalAmount",tran.lnTotalAmount);
        values.put("TxnDate",tran.Date);
        values.put("TxnReceiptDate",tran.receiptDate);
        values.put("TxnTime",tran.Time);
        values.put("Host",getHostGroupRef(tran.issuerNumber));
        values.put("MerchantNumber",tran.merchantNumber);
        values.put("ServiceCrg",tran.serviceCharge);
        values.put("TipAmount",tran.tipAmount);
        values.put("FuelCharge",tran.fuelCharge);
        values.put("[Credit/Debit]",tran.tranCreditOrDebit);
        values.put("ApproveCode",tran.approveCode);
        values.put("RRN",tran.RRN);
        values.put("Discount",tran.discount);
        values.put("MTI",tran.MTI);
        values.put("ProcCode",tran.procCode);
        values.put("TransactionCode",tran.inTransactionCode);
        values.put("ChipStatus",tran.inChipStatus);
        values.put("BaseTransactionAmount",tran.lnBaseTransactionAmount);
        values.put("PAN",tran.PAN);
        values.put("CardSNumber",tran.cardSerialNo);
        values.put("track2",tran.track2);
        values.put("svcCode",tran.scvCode);
        values.put("expDate",tran.expDate);
        values.put("TerminalID",tran.terminalID);
        values.put("MerchantID",tran.merchantID);
        values.put("MerchantName",tran.merchantName);
        values.put("NII",tran.NII);
        values.put("SecureNII",tran.secureNII);
        values.put("TPDU",tran.TPDU);
        values.put("emvField55",tran.emv55Data);
        values.put("ResponoseCode",tran.responseCode);
        values.put("CdtIndex",tran.cdtIndex);
        values.put("IssuerNumber",tran.issuerNumber);
        values.put("ActIssuerNumber",tran.actualIssuerNumber);
        values.put("Voided",tran.isVoided);
        values.put("ExtData",tran.ExtData);

        long result = 0;

        try
        {
            result = db.insert("TXN",null,values);
            if (result == -1 )
                return  false;

        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return true;
    }

    public Transaction getTransaction(Cursor result) {
        Transaction transaction =  new Transaction();

        try {
            transaction.MTI = result.getString(result.getColumnIndex("MTI"));
            transaction.inInvoiceNumber = result.getInt(result.getColumnIndex("InvoiceNumber"));
            transaction.traceNumber = result.getInt(result.getColumnIndex("TraceNumber"));
            transaction.lnTotalAmount = result.getInt(result.getColumnIndex("TotalAmount"));
            transaction.Date = result.getString(result.getColumnIndex("TxnDate"));
            transaction.receiptDate = result.getString(result.getColumnIndex("TxnReceiptDate"));
            transaction.Time = result.getString(result.getColumnIndex("TxnTime"));
            transaction.merchantNumber = result.getInt(result.getColumnIndex("MerchantNumber"));
            transaction.serviceCharge = result.getInt(result.getColumnIndex("ServiceCrg"));
            transaction.tipAmount = result.getInt(result.getColumnIndex("TipAmount"));
            transaction.fuelCharge = result.getInt(result.getColumnIndex("FuelCharge"));
            transaction.tranCreditOrDebit = result.getString(result.getColumnIndex("Credit/Debit"));
            transaction.approveCode = result.getString(result.getColumnIndex("ApproveCode"));
            transaction.RRN = result.getString(result.getColumnIndex("RRN"));
            transaction.discount = result.getInt(result.getColumnIndex("Discount"));
            transaction.inChipStatus = result.getInt(result.getColumnIndex("ChipStatus"));
            transaction.lnBaseTransactionAmount = result.getInt(result.getColumnIndex("BaseTransactionAmount"));
            transaction.PAN = result.getString(result.getColumnIndex("PAN"));
            transaction.cardSerialNo = result.getString(result.getColumnIndex("CardSNumber"));
            transaction.track2 = result.getString(result.getColumnIndex("track2"));
            transaction.scvCode = result.getString(result.getColumnIndex("svcCode"));
            transaction.expDate = result.getString(result.getColumnIndex("expDate"));
            transaction.terminalID = result.getString(result.getColumnIndex("TerminalID"));
            transaction.merchantID = result.getString(result.getColumnIndex("MerchantID"));
            transaction.merchantName = result.getString(result.getColumnIndex("MerchantName"));
            transaction.NII = result.getString(result.getColumnIndex("NII"));
            transaction.TPDU = result.getString(result.getColumnIndex("TPDU"));
            transaction.emv55Data = result.getString(result.getColumnIndex("emvField55"));
            transaction.responseCode = result.getString(result.getColumnIndex("ResponoseCode"));
            transaction.cdtIndex = result.getInt(result.getColumnIndex("CdtIndex"));
            transaction.issuerNumber = result.getInt(result.getColumnIndex("IssuerNumber"));
            transaction.actualIssuerNumber = result.getInt(result.getColumnIndex("ActIssuerNumber"));
            transaction.procCode = result.getString(result.getColumnIndex("ProcCode"));
            transaction.inTransactionCode  = result.getInt(result.getColumnIndex("TransactionCode"));
            transaction.isVoided = result.getInt(result.getColumnIndex("Voided"));
            transaction.ExtData = result.getString(result.getColumnIndex("ExtData"));

        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return  transaction;
    }

    public Transaction getReveralTransaction(int merchNum) {
        Transaction transaction =  new Transaction();

        String quary = "SELECT * FROM RVSL WHERE MerchantNumber = " + merchNum;
        Cursor result = null;
        try {
            result = db.rawQuery(quary,null);
            result.moveToFirst();
            transaction.inInvoiceNumber = result.getInt(result.getColumnIndex("InvoiceNumber"));
            transaction.traceNumber = result.getInt(result.getColumnIndex("TraceNumber"));
            transaction.lnTotalAmount = result.getInt(result.getColumnIndex("TotalAmount"));
            transaction.Date = result.getString(result.getColumnIndex("TxnDate"));
            transaction.Time = result.getString(result.getColumnIndex("TxnTime"));
            transaction.merchantNumber = result.getInt(result.getColumnIndex("MerchantNumber"));
            transaction.serviceCharge = result.getInt(result.getColumnIndex("ServiceCrg"));
            transaction.tipAmount = result.getInt(result.getColumnIndex("TipAmount"));
            transaction.fuelCharge = result.getInt(result.getColumnIndex("FuelCharge"));
            transaction.tranCreditOrDebit = result.getString(result.getColumnIndex("Credit/Debit"));
            transaction.approveCode = result.getString(result.getColumnIndex("ApproveCode"));
            transaction.RRN = result.getString(result.getColumnIndex("RRN"));
            transaction.discount = result.getInt(result.getColumnIndex("Discount"));
            transaction.inChipStatus = result.getInt(result.getColumnIndex("ChipStatus"));
            transaction.lnBaseTransactionAmount = result.getInt(result.getColumnIndex("BaseTransactionAmount"));
            transaction.PAN = result.getString(result.getColumnIndex("PAN"));
            transaction.cardSerialNo = result.getString(result.getColumnIndex("CardSNumber"));
            transaction.track2 = result.getString(result.getColumnIndex("track2"));
            transaction.scvCode = result.getString(result.getColumnIndex("svcCode"));
            transaction.expDate = result.getString(result.getColumnIndex("expDate"));
            transaction.terminalID = result.getString(result.getColumnIndex("TerminalID"));
            transaction.merchantID = result.getString(result.getColumnIndex("MerchantID"));
            transaction.merchantName = result.getString(result.getColumnIndex("MerchantName"));
            transaction.NII = result.getString(result.getColumnIndex("NII"));
            transaction.TPDU = result.getString(result.getColumnIndex("TPDU"));
            transaction.emv55Data = result.getString(result.getColumnIndex("emvField55"));
            transaction.responseCode = result.getString(result.getColumnIndex("ResponoseCode"));
            transaction.cdtIndex = result.getInt(result.getColumnIndex("CdtIndex"));
            transaction.issuerNumber = result.getInt(result.getColumnIndex("IssuerNumber"));

            result.close();
        }
        catch (Exception ex) {
            result.close();
            ex.printStackTrace();
        }

        return  transaction;
    }

    public boolean removeReversal(int merchNum) {
        String quary = "DELETE FROM RVSL WHERE MerchantNumber = " + merchNum;

        try {
            db.execSQL(quary);
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public int checkReversal (int merchNum) {
        String quary = "SELECT * FROM RVSL WHERE MerchantNumber = " + merchNum;

        try {
            Cursor rev = db.rawQuery(quary,null);
            if (rev.getCount() > 0)
                return 1;
            return 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            return -1;
        }
    }

    public boolean writeQRTran(QRTran tran) {
        ContentValues values =  new ContentValues();
        values.put("QRCode",tran.QRCode);
        values.put("TranQRAmount",tran.tranQRAmount);
        values.put("REFQRLable",tran.refQRLabel);

        long result = 0;

        try {
            result = db.insert("QR",null,values);
            if (result == -1 )
                return  false;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return true;
    }

    public QRTran getQRTran(Cursor result) {
        QRTran transaction =  new QRTran();

        try {
            transaction.QRCode = result.getString(result.getColumnIndex("QRCode"));
            transaction.tranQRAmount = result.getInt(result.getColumnIndex("TranQRAmount"));
            transaction.refQRLabel = result.getString(result.getColumnIndex("REFQRLable"));
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return  transaction;
    }

    public void deleteQRTranBatch(QRTran qrTran) {
        String query = "SELECT * FROM QRBatch WHERE ID = 1";
        String delQuery = "DELETE FROM QRBatch";

        try {
            Cursor firstTran = db.rawQuery(query, null);
            if (firstTran.getCount() == 0)
                return;

            firstTran.moveToFirst();
            String fDate = firstTran.getString(firstTran.getColumnIndex("Date"));
            Date fday = new SimpleDateFormat("yyMMdd").parse(fDate);

            Date lday = new SimpleDateFormat("yyMMdd").parse(qrTran.Date);

            long diffInMillies = Math.abs(lday.getTime() - fday.getTime());
            long diff = TimeUnit.DAYS.convert(diffInMillies, TimeUnit.MILLISECONDS);

            if (diff > 2) {
                db.execSQL(delQuery);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public boolean writeQRTrantoBatch(QRTran tran) {
        deleteQRTranBatch(tran);

        ContentValues values =  new ContentValues();
        values.put("REFQRLable",tran.refQRLabel);
        values.put("TranQRAmount",tran.tranQRAmount);
        values.put("Date",tran.Date);
        values.put("Time",tran.Time);
        values.put("CardHolder",tran.cardHolder);
        values.put("Mobile",tran.cusMobile);
        values.put("MCC",tran.MCC);
        values.put("MerchName",tran.merchName);
        values.put("MerchID",tran.mid);
        values.put("PAN",tran.PAN);
        values.put("QRType",tran.qrType);
        values.put("Status",tran.status);
        values.put("TerminalID",tran.terminalID);
        values.put("Trace",tran.trace);
        values.put("TxOrg",tran.txOrg);

        long result = 0;

        try {
            result = db.insert("QRBatch",null,values);
            if (result == -1 )
                return  false;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return true;
    }

    public QRTran getQRTranfromBatch(Cursor result) {
        QRTran transaction =  new QRTran();

        try {
            transaction.refQRLabel = result.getString(result.getColumnIndex("REFQRLable"));
            transaction.tranQRAmount = result.getInt(result.getColumnIndex("TranQRAmount"));
            transaction.Date = result.getString(result.getColumnIndex("Date"));
            transaction.Time = result.getString(result.getColumnIndex("Time"));
            transaction.cardHolder = result.getString(result.getColumnIndex("CardHolder"));
            transaction.cusMobile = result.getString(result.getColumnIndex("Mobile"));
            transaction.MCC = result.getString(result.getColumnIndex("MCC"));
            transaction.merchName = result.getString(result.getColumnIndex("MerchName"));
            transaction.mid = result.getString(result.getColumnIndex("MerchID"));
            transaction.PAN = result.getString(result.getColumnIndex("PAN"));
            transaction.qrType = result.getString(result.getColumnIndex("QRType"));
            transaction.status = result.getString(result.getColumnIndex("Status"));
            transaction.terminalID = result.getString(result.getColumnIndex("TerminalID"));
            transaction.trace = result.getString(result.getColumnIndex("Trace"));
            transaction.txOrg = result.getString(result.getColumnIndex("TxOrg"));
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return  transaction;
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

    public void executeCustomQuary(String quary) {
        try {
            db.execSQL(quary);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}