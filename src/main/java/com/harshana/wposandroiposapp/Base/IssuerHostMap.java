package com.harshana.wposandroiposapp.Base;

import android.database.Cursor;
import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.DevArea.TranStaticData;

import java.util.ArrayList;
import java.util.List;

import static com.harshana.wposandroiposapp.Base.Base.appContext;


/**
 * Created by harshana_m on 1/22/2019.
 */

public class IssuerHostMap
{
    public static int numHostEntries = 0 ;
    public static HostIssuer hosts[];
    private static DBHelper configDatabase = null;

    public static boolean init()
    {
        configDatabase = DBHelper.getInstance(appContext);
        String quary = "SELECT * FROM IHT";
        try
        {
            Cursor issuerCombinations = configDatabase.readWithCustomQuary(quary);

            if ((issuerCombinations == null) || (issuerCombinations.getCount() == 0))
                return false;

            numHostEntries = issuerCombinations.getCount();
            hosts =  new HostIssuer[numHostEntries];

            int index = 0 ;
            while (issuerCombinations.moveToNext())
            {
                HostIssuer hostIssuer = new HostIssuer();

                hostIssuer.hostName = issuerCombinations.getString(issuerCombinations.getColumnIndex("HostName"));
                hostIssuer.issuerList = IssuerHostMap.getIntegerList(issuerCombinations.getString(issuerCombinations.getColumnIndex("IssuerList")));
                hostIssuer.baseIssuer = issuerCombinations.getInt(issuerCombinations.getColumnIndex("BaseIssuer"));
                hostIssuer.isTLEEnabled = issuerCombinations.getInt(issuerCombinations.getColumnIndex("TLEEnabled"));
                hostIssuer.isAutoSettlmentEnabled = issuerCombinations.getInt(issuerCombinations.getColumnIndex("AutoSettlementEnabled"));
                hostIssuer.autoSettlmentTime = issuerCombinations.getString(issuerCombinations.getColumnIndex("AutoSettlementTime"));

                hosts[index++] = hostIssuer;

            }

            issuerCombinations.close();
        }catch (Exception ex)
        {
            ex.printStackTrace();
            return false;
        }

        return true;
    }


    //To get the base issuer when sub issuer is given
    public static int getBaseIssuer(int issuer)
    {
        for (HostIssuer  host : hosts)
        {
            for (int compIssuer : host.issuerList)
            {
                if (compIssuer == issuer)
                    return host.baseIssuer;
            }
        }

        return -1;
    }

    public static int getBaseIssuer(String hostName)
    {
        for (HostIssuer host : hosts)
        {
            if (host.hostName.equals(hostName))
                return host.baseIssuer;
        }

        return -1;
    }
    public static String genQuarytoSelectMerchants(List<Integer> list)
    {
        int size = list.size();
        int issuer =  -1;

        String quary = "SELECT MIT.MerchantName,MIT.MerchantNumber,TMIF.IssuerNumber " +
                "FROM MIT,TMIF WHERE ";

        String gen = "";
        for (int i = 0; i < size; i++)
        {
            issuer = list.get(i);
            gen += "TMIF.IssuerNumber = " + issuer + " OR ";
        }

        //remove the last or
        gen = gen.substring(0,gen.length() - 4);

        quary = quary + gen + " AND MIT.MerchantNumber = TMIF.MerchantNumber";
        return quary + gen;

    }

    public static List<Integer> getIntegerList(String data)
    {
        int offset = 0 ;
        int start = 0 ;

        List<Integer> ints =  new ArrayList<>();

        while (offset < data.length())
        {
            offset = data.indexOf(",",start);
            int number;

            if (offset  < 0)
                offset = data.length();

            number = Integer.valueOf(data.substring(start,offset));

            ints.add(number);
            offset++;
            start = offset;
        }

        return ints;

    }

    public static String  getQuaryForPostSettlementBatchWipe(int selectedHost,int baseHostMerchantNumber)
    {
        String Quary = "DELETE FROM TXN WHERE ";

        //IssuerNumber = " + issuerNumber + " AND MerchantNumber = " + merchantNumber ;
        HostIssuer host = hosts[selectedHost];
        int listSize = host.issuerList.size();
        List<Integer> merchantList   =  new ArrayList<>();

        //retrieve the TID of the selected merchant's
        String selTidQuary = "SELECT TerminalID FROM TMIF WHERE IssuerNumber = " + host.baseIssuer  + " AND MerchantNumber = " + baseHostMerchantNumber;
        Cursor tidRec = null;

        tidRec =  configDatabase.readWithCustomQuary(selTidQuary);
        if (tidRec == null || tidRec.getCount() == 0)
            return null;

        tidRec.moveToFirst();

        String tid = tidRec.getString(tidRec.getColumnIndex("TerminalID"));
        tidRec.close();
        //now we have the base host selected tid so we search for other bounds hosts for the same tid

        //collect the merchant list of each
        for ( int i = 0 ; i < listSize; i++)
        {
            int issuer = host.issuerList.get(i);
            String quary = "SELECT MerchantNumber FROM TMIF WHERE IssuerNumber = " + issuer + " AND TerminalID = " + "'" + tid + "'";

            Cursor merchantRec = configDatabase.readWithCustomQuary(quary);
            int merchantNumber;

            if (merchantRec != null)
                merchantRec.moveToFirst();

            if (merchantRec != null && merchantRec.getCount() > 0)
                merchantNumber = merchantRec.getInt(merchantRec.getColumnIndex("MerchantNumber"));
            else
                merchantNumber = -1;

            merchantList.add(merchantNumber);
            merchantRec.close();
        }

        //now we have the merchant list so we generate the tran select quary
        for ( int i = 0 ; i < listSize; i++)
        {
            int merchantNumber = merchantList.get(i);
            if (merchantNumber != -1)
                Quary += " (IssuerNumber = " + host.issuerList.get(i) + " AND MerchantNumber = " + merchantNumber + ") OR";
        }

        Quary = Quary.substring(0, Quary.length() - 2); // remove last added or
        return  Quary;

    }


    public static String genQuaryForSettlementTotalCount(int inTranscationCode,int selectedHost,int baseHostMerchantNumber)
    {
        String Quary = "SELECT COUNT(*) As tCount FROM TXN WHERE (Voided == 0) AND (TransactionCode = " + inTranscationCode + ") AND (";

        //IssuerNumber = " + issuerNumber + " AND MerchantNumber = " + merchantNumber ;
        HostIssuer host = hosts[selectedHost];
        int listSize = host.issuerList.size();
        List<Integer> merchantList =  new ArrayList<>();

        //retrieve the TID of the selected merchant's
        String selTidQuary = "SELECT TerminalID FROM TMIF WHERE IssuerNumber = " + host.baseIssuer  + " AND MerchantNumber = " + baseHostMerchantNumber;
        Cursor tidRec = null;

        tidRec =  configDatabase.readWithCustomQuary(selTidQuary);
        if (tidRec == null)
            return null;

        tidRec.moveToFirst();

        String tid = tidRec.getString(tidRec.getColumnIndex("TerminalID"));
        tidRec.close();

        //now we have the base host selected tid so we search for other bounds hosts for the same tid
        //collect the merchant list of each
        for ( int i = 0 ; i < listSize; i++)
        {
            int issuer = host.issuerList.get(i);
            String quary = "SELECT MerchantNumber FROM TMIF WHERE IssuerNumber = " + issuer + " AND TerminalID = " + "'" + tid + "'";

            Cursor merchantRec = configDatabase.readWithCustomQuary(quary);
            int merchantNumber;

            if (merchantRec != null)
                merchantRec.moveToFirst();

            if (merchantRec != null && merchantRec.getCount() > 0)
                merchantNumber = merchantRec.getInt(merchantRec.getColumnIndex("MerchantNumber"));
            else
                merchantNumber = -1;

            merchantList.add(merchantNumber);
            merchantRec.close();
        }

        //now we have the merchant list so we generate the tran select quary
        for ( int i = 0 ; i < listSize; i++)
        {
            int merchantNumber = merchantList.get(i);
            if (merchantNumber != -1)
                Quary += " (IssuerNumber = " + host.issuerList.get(i) + " AND MerchantNumber = " + merchantNumber + ") OR";
        }

        Quary = Quary.substring(0, Quary.length() - 2); // remove last added or
        Quary +=  ")";
        return  Quary;

    }


    public static String genQuaryForSettlementTotalAmount(int inTranscationCode,int selectedHost,int baseHostMerchantNumber)
    {
        String Quary = "SELECT SUM(BaseTransactionAmount) As Total FROM TXN WHERE (Voided == 0) AND (TransactionCode = " + inTranscationCode + ") AND (";

        //IssuerNumber = " + issuerNumber + " AND MerchantNumber = " + merchantNumber ;
        HostIssuer host = hosts[selectedHost];
        int listSize = host.issuerList.size();
        List<Integer> merchantList   =  new ArrayList<>();

        //retrieve the TID of the selected merchant's
        String selTidQuary = "SELECT TerminalID FROM TMIF WHERE IssuerNumber = " + host.baseIssuer  + " AND MerchantNumber = " + baseHostMerchantNumber;
        Cursor tidRec = null;

        tidRec =  configDatabase.readWithCustomQuary(selTidQuary);
        if (tidRec == null)
            return null;

        tidRec.moveToFirst();

        String tid = tidRec.getString(tidRec.getColumnIndex("TerminalID"));
        tidRec.close();

        //now we have the base host selected tid so we search for other bounds hosts for the same tid
        //collect the merchant list of each
        for ( int i = 0 ; i < listSize; i++)
        {
            int issuer = host.issuerList.get(i);
            String quary = "SELECT MerchantNumber FROM TMIF WHERE IssuerNumber = " + issuer + " AND TerminalID = " + "'" + tid + "'";

            Cursor merchantRec = configDatabase.readWithCustomQuary(quary);
            int merchantNumber;

            if (merchantRec != null)
                merchantRec.moveToFirst();

            if (merchantRec != null && merchantRec.getCount() > 0)
                merchantNumber = merchantRec.getInt(merchantRec.getColumnIndex("MerchantNumber"));
            else
                merchantNumber = -1;

            merchantList.add(merchantNumber);
            merchantRec.close();
        }

        //now we have the merchant list so we generate the tran select quary
        for ( int i = 0 ; i < listSize; i++)
        {
            int merchantNumber = merchantList.get(i);
            if (merchantNumber != -1)
                Quary += " (IssuerNumber = " + host.issuerList.get(i) + " AND MerchantNumber = " + merchantNumber + ") OR";
        }

        Quary = Quary.substring(0, Quary.length() - 2); // remove last added or
        Quary = Quary + ")";
        return  Quary;

    }


    public static String genQuaryForSettlementTransactions(int selectedHost,int baseHostMerchantNumber)
    {
        String Quary = "SELECT * FROM TXN WHERE (Voided = 0)  AND ";

        //IssuerNumber = " + issuerNumber + " AND MerchantNumber = " + merchantNumber ;
        HostIssuer host = hosts[selectedHost];
        int listSize = host.issuerList.size();
        List<Integer> merchantList   =  new ArrayList<>();

        //retrieve the TID of the selected merchant's
        String selTidQuary = "SELECT TerminalID FROM TMIF WHERE IssuerNumber = " + host.baseIssuer  + " AND MerchantNumber = " + baseHostMerchantNumber;
        Cursor tidRec = null;

        tidRec =  configDatabase.readWithCustomQuary(selTidQuary);
        if (tidRec == null)
            return null;

        tidRec.moveToFirst();

        String tid = tidRec.getString(tidRec.getColumnIndex("TerminalID"));
        tidRec.close();

        //now we have the base host selected tid so we search for other bounds hosts for the same tid
        //collect the merchant list of each
        for ( int i = 0 ; i < listSize; i++)
        {
            int issuer = host.issuerList.get(i);
            String quary = "SELECT MerchantNumber FROM TMIF WHERE IssuerNumber = " + issuer + " AND TerminalID = " + "'" + tid + "'";

            Cursor merchantRec = configDatabase.readWithCustomQuary(quary);
            int merchantNumber;

            if (merchantRec != null)
                merchantRec.moveToFirst();

            if (merchantRec != null && merchantRec.getCount() > 0)
                merchantNumber = merchantRec.getInt(merchantRec.getColumnIndex("MerchantNumber"));
            else
                merchantNumber = -1;

            merchantList.add(merchantNumber);
            merchantRec.close();
        }

        //now we have the merchant list so we generate the tran select quary
        for ( int i = 0 ; i < listSize; i++)
        {
            int merchantNumber = merchantList.get(i);
            if (merchantNumber != -1)
                Quary += " (IssuerNumber = " + host.issuerList.get(i) + " AND MerchantNumber = " + merchantNumber + ") OR";
        }

        Quary = Quary.substring(0, Quary.length() - 2); // remove last added or

        //pre authorizations are not considered for the settlement
        Quary += " AND TransactionCode != " + TranStaticData.TranTypes.PRE_AUTH;
        return  Quary;

    }

    public static List<Integer> getRelatedMerchantList(int selectedHost,int baseHostMerchantNumber)
    {
        //IssuerNumber = " + issuerNumber + " AND MerchantNumber = " + merchantNumber ;
        HostIssuer host = hosts[selectedHost];
        int listSize = host.issuerList.size();
        List<Integer> merchantList   =  new ArrayList<>();

        //retrieve the TID of the selected merchant's
        String selTidQuary = "SELECT TerminalID FROM TMIF WHERE IssuerNumber = " + host.baseIssuer  + " AND MerchantNumber = " + baseHostMerchantNumber;
        Cursor tidRec = null;

        tidRec =  configDatabase.readWithCustomQuary(selTidQuary);
        if (tidRec == null)
            return null;

        tidRec.moveToFirst();

        String tid = tidRec.getString(tidRec.getColumnIndex("TerminalID"));

        //now we have the base host selected tid so we search for other bounds hosts for the same tid
        //collect the merchant list of each
        for ( int i = 0 ; i < listSize; i++)
        {
            int issuer = host.issuerList.get(i);
            String quary = "SELECT MerchantNumber FROM TMIF WHERE IssuerNumber = " + issuer + " AND TerminalID = " + "'" + tid + "'";

            Cursor merchantRec = configDatabase.readWithCustomQuary(quary);
            int merchantNumber;

            if (merchantRec != null)
                merchantRec.moveToFirst();

            if (merchantRec != null && merchantRec.getCount() > 0)
                merchantNumber = merchantRec.getInt(merchantRec.getColumnIndex("MerchantNumber"));
            else
                merchantNumber = -1;

            merchantList.add(merchantNumber);
        }

        return merchantList;
    }


    public static String genQueryForBatchIncrementAfterSettle(int selectedHost, int baseHostMerchantNumber, int batchNumber)
    {
        String Quary = "UPDATE MIT SET BatchNumber = " + batchNumber + " WHERE ";

        //IssuerNumber = " + issuerNumber + " AND MerchantNumber = " + merchantNumber ;
        HostIssuer host = hosts[selectedHost];
        int listSize = host.issuerList.size();
        List<Integer> merchantList   =  new ArrayList<>();

        //retrieve the TID of the selected merchant's
        String selTidQuary = "SELECT TerminalID FROM TMIF WHERE IssuerNumber = " + host.baseIssuer  + " AND MerchantNumber = " + baseHostMerchantNumber;
        Cursor tidRec = null;

        tidRec =  configDatabase.readWithCustomQuary(selTidQuary);
        if (tidRec == null)
            return null;

        tidRec.moveToFirst();

        String tid = tidRec.getString(tidRec.getColumnIndex("TerminalID"));

        //now we have the base host selected tid so we search for other bounds hosts for the same tid
        //collect the merchant list of each
        for ( int i = 0 ; i < listSize; i++)
        {
            int issuer = host.issuerList.get(i);
            String quary = "SELECT MerchantNumber FROM TMIF WHERE IssuerNumber = " + issuer + " AND TerminalID = " + "'" + tid + "'";

            Cursor merchantRec = configDatabase.readWithCustomQuary(quary);
            int merchantNumber;

            if (merchantRec != null)
                merchantRec.moveToFirst();

            if (merchantRec != null && merchantRec.getCount() > 0)
                merchantNumber = merchantRec.getInt(merchantRec.getColumnIndex("MerchantNumber"));
            else
                merchantNumber = -1;

            merchantList.add(merchantNumber);
        }

        //now we have the merchant list so we generate the tran select quary
        for ( int i = 0 ; i < listSize; i++)
        {
            int merchantNumber = merchantList.get(i);
            if (merchantNumber != -1)
                Quary += " ( MerchantNumber = " + merchantNumber + ") OR";
        }

        Quary = Quary.substring(0, Quary.length() - 2); // remove last added or
        return  Quary;

    }

}

