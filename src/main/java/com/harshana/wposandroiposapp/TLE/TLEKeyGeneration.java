package com.harshana.wposandroiposapp.TLE;


import android.database.Cursor;

import com.harshana.wposandroiposapp.Base.Base;
import com.harshana.wposandroiposapp.Base.HostIssuer;
import com.harshana.wposandroiposapp.Base.IssuerHostMap;
import com.harshana.wposandroiposapp.Base.Keys;
import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.DevArea.GlobalData;
import com.harshana.wposandroiposapp.Utilities.Formatter;
import com.harshana.wposandroiposapp.Utilities.Utility;

import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;



public class TLEKeyGeneration extends Base
{

    public static String kcvForUniqueKey = "";
    public static String szTLETXN_HEAD = "";


    private static String loadSmartTID()
    {
        DBHelper configDatabase = DBHelper.getInstance(appContext);
        String tid = null;
        int issuerNumber = 0 ;

        int host = inGetHostGroupRef();
        issuerNumber = IssuerHostMap.hosts[host].baseIssuer;

        //quarry to select the first TID for the selected issuer
        String selectQuary = "SELECT TerminalID FROM TMIF WHERE IssuerNumber = " +  issuerNumber + " LIMIT 1";
        try
        {
            Cursor result  = configDatabase.readWithCustomQuary(selectQuary);

            if (result == null || result.getCount() == 0)
                return null;

            result.moveToFirst();
            tid = result.getString(result.getColumnIndex("TerminalID"));

        }catch (Exception ex)
        {
            ex.printStackTrace();
            return null;
        }

        return tid;

    }

    public static byte[] unKsnCreate()
    {
        int count = 0;
        String szCount = "",szEFgyn = "",szSmartTid = "",szKeyid="",szKsn= "";
        byte[] unKsn = new byte[8];

        //harshana write a function to load the first tid which is used in key download
        szSmartTid = "70303391";
        szSmartTid = loadSmartTID();

        szCount = String.format("%06X",count);
        szEFgyn = TLEKeyGeneration.szTleEFGyn();
        String str1 = szEFgyn.substring(0,1);
        szCount = szCount.substring(1);
        szCount = str1 + szCount;

        szKeyid = szGetKeyIdGyn();
        szKeyid = szKeyid.substring(0,6);
        szKsn = szKeyid + szSmartTid + szCount;
        szKsn = szKsn.substring(4);
        //SVC_DSP_2_HEX(szKsn+4,(char*)unKsn,16);
        unKsn = Utility.hexStr2Byte(szKsn);
        return unKsn;
    }

    public static String szGetKeyIdGyn()
    {
        String szKEYIDGyn="";
        int inKeyIndex=0;
        short shIndex = 0;
        byte[] unKEYIDGyn = new byte[16];
        int inslotIndex = inGetHostGroupRef();


        switch(inslotIndex)
        {
            case 0:
                shIndex=32;
                break;
            case 1:
                shIndex=35;
                break;
            case 2:
                shIndex=38;
                break;
            default:
                break;
        }


        unKEYIDGyn =  unDataFromSecureAria(shIndex);
        szKEYIDGyn = Utility.byte2HexStr(unKEYIDGyn,0,6);

        return szKEYIDGyn;
    }

    public static int inGetHostGroupRef()
    {

        int numHosts = IssuerHostMap.numHostEntries;
        int curIssuer  = currentTransaction.issuerNumber;

        for (int host = 0 ; host < numHosts; host++)
        {
            HostIssuer hostSel = IssuerHostMap.hosts[host];
            List<Integer> issuers = hostSel.issuerList;
            if (issuers.contains(curIssuer))
                return host;
        }

        return -1;
    }

    public static int inIncTxnCount()
    {
        long count=0L;
        int inhost = 0;
        String szCount="";
        short shIndex = 0;
        byte[] unEFgyn = new byte[1];


        inhost = inGetHostGroupRef();

        switch(inhost)
        {
            case 0:
                shIndex = 31;
                count =inGetCounterGyn(shIndex);
                if(count==0xFFFFF)
                {
                    count = 0;
                    unEFgyn[0] = 0x0F;
                    shIndex--;
                    inSetEFGyn(unEFgyn,shIndex);
                }
                else
                    count = count + 1;

                szCount = String.format("%06X", count);
                inSetCountGyn(szCount,shIndex);
                break;

            case 1:
                shIndex = 34;
                count =inGetCounterGyn(shIndex);
                if(count==0xFFFFF) {
                    count = 0;
                    unEFgyn[0] = 0x0F;
                    shIndex--;
                    inSetEFGyn(unEFgyn,shIndex);
                }
                else
                    count = count + 1;
                //memset(szCount,0,sizeof(szCount));
                // sprintf(szCount,"%06X",count);
                //szCount[6]='\0';
                szCount = String.format("%06X", count);
                inSetCountGyn(szCount,shIndex);
                break;
        }
        return 0;
    }

    public static int inGetCounterGyn(short shIndex) {
        int inCount = 0;
        byte[] unTemp = new byte[16];
        byte[] unCount = new byte[3];
        String szCount="";
        unTemp = unDataFromSecureAria(shIndex);
        szCount = Utility.byte2HexStr(unTemp,0,3);
        inCount = Integer.valueOf(szCount,16);
        return inCount;
    }

    public static int inSetEFGyn(byte[] unEFGyn,short shIndex) {
        inStoreSecureAria(unEFGyn,shIndex);
        return 0;
    }

    public static int inSetCountGyn(String szCountGyn,short shIndex) {
        byte[] unCount = new byte[3];

        unCount = Utility.hexStr2Byte(szCountGyn,0,6);
        inStoreSecureAria(unCount,shIndex);
        return 0;
    }

    public static long lnGetHostTxnCount() {
        long count=0L;
        int inhost = 0;
        short shIndex=0;
        inhost = inGetHostGroupRef();

        switch(inhost) {
            case 0:
                shIndex=31;
                count =inGetCounterGyn(shIndex);
                break;

            case 1:
                shIndex=34;
                count =inGetCounterGyn(shIndex);
                break;

            case 2:
                break;
        }
        return count;
    }

    public static String szTleEFGyn() {
        int inhost = 0;
        short shIndex=0;
        String szEF = "";
        inhost = inGetHostGroupRef();
        switch(inhost) {
            case 0:
                shIndex=30;
                szEF=szGetEFGyn(shIndex);
                break;

            case 1:
                shIndex=33;
                szEF=szGetEFGyn(shIndex);
                break;

            case 2:
                shIndex=36;
                szEF=szGetEFGyn(shIndex);
                break;
        }
        return szEF;
    }

    public static String szGetEFGyn(short shIndex) {
        String szEFGyn = "";

        byte[] unEFGyn = new byte[16];
        unEFGyn = unDataFromSecureAria(shIndex);
        szEFGyn = Utility.byte2HexStr(unEFGyn,0,1);
        szEFGyn = szEFGyn.substring(0,1);

        if (szEFGyn.equals("0"))
            szEFGyn = "E";
        else
            szEFGyn = "generatePDFReceipt";
        return szEFGyn;
    }

    public static String vdSetHostCountWhenBitCoutTen(long inCnt) {
        String szCount="";
        short shIndex=0;
        int inhost = 0;

        inhost = inGetHostGroupRef();

        switch(inhost) {
            case 0:
                shIndex = 31;
                szCount = String.format("%06X", inCnt);
                inSetCountGyn(szCount,shIndex);
                break;

            case 1:
                shIndex = 34;
                szCount = String.format("%06X", inCnt);
                inSetCountGyn(szCount,shIndex);
                break;

            case 2:
                shIndex = 37;
                szCount = String.format("%06X", inCnt);
                inSetCountGyn(szCount,shIndex);
                break;
        }
        return  szCount;
    }

    //harshana
    public  static int inGETFUTUREKEYSLOTINDEX(int inHost) {
        int index = 0;

        if(inHost == 0)
            index = 40;
        else if(inHost == 1)
            index = 60;
        else if(inHost == 2)
            index = 80;
        return index;
    }

    public static byte[] unTripleDesEncpt(byte[] unClearData, byte[] unkey) throws Exception {
        byte [] unEncryptedData = new byte[16];

        SecretKeySpec symKey1 = new SecretKeySpec(unkey ,"DESede");
        Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,symKey1);
        unEncryptedData =  cipher.doFinal(unClearData);
        return unEncryptedData;
    }

    public static byte[] unDesEncpt(byte[] unClearData, byte[] unkey) throws Exception {
        byte [] unEncryptedData;

        SecretKeySpec symKey1 = new SecretKeySpec(unkey ,"DES");
        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,symKey1);
        unEncryptedData =  cipher.doFinal(unClearData);
        return unEncryptedData;
    }

    public static int inStoreSecureAria(byte[] unData,int inSlot) {
        String keyData = Utility.byte2HexStr(unData);
        Keys.setKeyInSlot(inSlot,keyData);
        return 0;
    }

    public static byte[] unDataFromSecureAria(int inSlot) {
        String keyHex = Keys.getKeyFromSlot(inSlot);
        byte[] unData = Utility.hexStr2Byte(keyHex);
        return unData;
    }

    public static String getCounter() {
        String szSmartTid = "", szKeyId = "",szSerialNo="",szTxnCount="",szEFgyn = "",szCount = "";
        long inTxnCount = 0;

        if((szGetTHUKID()=="03") || (szGetTHUKID()=="04")) {
            inTxnCount = lnGetHostTxnCount();
            //harshana load the first tid
            szSmartTid = loadSmartTID();
            szKeyId = szGetKeyIdGyn();
            szTxnCount = szKeyId.substring(0,6);
            szTxnCount = szTxnCount + szSmartTid;
            szCount = String.format("%05X",inTxnCount);
            szEFgyn = szTleEFGyn();
            szTxnCount = szTxnCount + szEFgyn + szCount;
        }
        return szTxnCount;
    }

    public  static  String szTLE_Header(){
        String szSmartTid = "", szKeyId = "",szSerialNo="",szTxnCount="",szEFgyn = "",szCount = "";
        long inTxnCount = 0;

        if((szGetTHUKID()=="03") || (szGetTHUKID()=="04")) {
            inTxnCount = lnGetHostTxnCount() - 1;
            szSmartTid = loadSmartTID();
            szKeyId = szGetKeyIdGyn();
            szTxnCount = szKeyId.substring(0,6);
            szTxnCount = szTxnCount + szSmartTid;
            szCount = String.format("%05X",inTxnCount);
            szEFgyn = szTleEFGyn();
            szTxnCount = szTxnCount + szEFgyn + szCount;
        }

        //override and load the hardware generated ksn
        szTxnCount = GlobalData.ksn;

        szTLETXN_HEAD = "";
        szTLETXN_HEAD = "0002";
        szKeyId = szGetKeyIdGyn();
        szTLETXN_HEAD = szTLETXN_HEAD + szKeyId;
        szTLETXN_HEAD = szTLETXN_HEAD + "0001";
        szTLETXN_HEAD = szTLETXN_HEAD + "01";
        szTLETXN_HEAD = szTLETXN_HEAD + "03";
        szTLETXN_HEAD = szTLETXN_HEAD + "04";
        szTLETXN_HEAD = szTLETXN_HEAD + szTxnCount;
        szTLETXN_HEAD = szTLETXN_HEAD + "01";
        szTLETXN_HEAD = szTLETXN_HEAD + "01";

        if(szGetTHAID() == "0002") {
            szTLETXN_HEAD = szTLETXN_HEAD +"08";
            String trail = Formatter.fillInBack("0", szSmartTid,16 );
            szTLETXN_HEAD = szTLETXN_HEAD + trail;
        }

        //this is the kcv which is generated for the key check value at the back end
        String kcv = TLE.KCV;

        //szTLETXN_HEAD = szTLETXN_HEAD + kcvForUniqueKey.substring(0,6);
        szTLETXN_HEAD = szTLETXN_HEAD + kcv.substring(0,6);
        return szTLETXN_HEAD;
    }

    public static String szGetTHUKID() {
        return "03";
    }

    public static String szGetTHAID() {
        return "0002";
    }
}