package com.harshana.wposandroiposapp.TLE;


import android.database.Cursor;
import android.os.RemoteException;
import android.util.Log;

import com.harshana.wposandroiposapp.Base.Base;
import com.harshana.wposandroiposapp.Base.ErrData;
import com.harshana.wposandroiposapp.Base.HostIssuer;
import com.harshana.wposandroiposapp.Base.IssuerHostMap;
import com.harshana.wposandroiposapp.Base.Keys;
import com.harshana.wposandroiposapp.Base.Services;
import com.harshana.wposandroiposapp.Base.TData;
import com.harshana.wposandroiposapp.Base.Transaction;
import com.harshana.wposandroiposapp.Communication.CommEngine;
import com.harshana.wposandroiposapp.Crypto.DESCrypto;
import com.harshana.wposandroiposapp.Crypto.MAC;
import com.harshana.wposandroiposapp.DevArea.GlobalData;
import com.harshana.wposandroiposapp.DevArea.TranStaticData;
import com.harshana.wposandroiposapp.DevArea.UnpackedPacket;
import com.harshana.wposandroiposapp.Packet.ISO8583u;
import com.harshana.wposandroiposapp.Settings.SettingsInterpreter;
import com.harshana.wposandroiposapp.Utilities.APDU;
import com.harshana.wposandroiposapp.Utilities.Formatter;
import com.harshana.wposandroiposapp.Utilities.Utility;

import java.util.Arrays;
import java.util.List;

import wangpos.sdk4.libbasebinder.BankCard;
/**
 * Created by harshana_m on 12/18/2018.
 */

public class TLEKeyDownload extends Base {
    private static String APDU_SELECT_APP  = "00A40400";
    private static String TLE_AID = "0102030405060708090001";
    private static String APDU_GET_SERIAL = "0C010000";
    private static String APDU_GET_PIN_VERIF_MODE = "0C020000";
    private static String APDU_GET_PIN_VERFICATION = "0C020100";
    private static String APDU_GET_ENC_METHOD = "0C030000";
    private static String APDU_GET_COUNTER = "0C030100";
    private static String APDU_GET_MAC = "0C060000";
    private static String APDU_3DES_DEC = "0C040000";
    private static String APDU_RSA_DEC = "0C050000";

    private static String cardSerialNumber = "";
    private static String cardKeyDownloadCounter = "";
    private static String tlePINBlock = "";
    private static String pinVerfivicationMode = "";
    private static String mac = "";
    String pinEnetered = null;
    BankCard tleBankCard = null;

    void forceRemoveCard() {
        int counter = 0;
        int sleepTime  = 500;
        startBusyAnimation("Please Remove the Card");
        try {
            while ( (bankCard != null) && (( bankCard.iccDetect()) == 1)) {//contact card detected
                if (counter != 0 && counter % 10 == 0) {
                    Services.core.buzzerEx(100);
                    Services.core.buzzerEx(100);

                    if (sleepTime >= 200)
                        sleepTime -= 50;
                }
                sleepMe(sleepTime);
                counter++;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        stopBusyAnimation();
    }

    private OnTLEKeyProcessResult func = null;

    public void setOnTLEKeyProcessResult(OnTLEKeyProcessResult lis)
    {
        func  = lis;
    }
    public interface OnTLEKeyProcessResult {
        void onTleKeyProcessResult(String result, int resultCode);
    }

    private void callCallBack(String msg,int code) {
        if (func != null)
            func.onTleKeyProcessResult(msg,code);
    }

    byte[]  exchangeApdu(byte[] sendData) {
        if (tleBankCard == null)
            return null;

        int sendDataLen = sendData.length;
        byte[] outData = new byte[128];
        int[] outDataLen = new int[1];

        int result  = -1;
        try {
           result =  tleBankCard.sendAPDU(BankCard.CARD_MODE_ICC,sendData,sendDataLen,outData,outDataLen);
        } catch (Exception ex) {}

        if (result < 0)
            return null;

        int outLen = outDataLen[0];

        byte[] resp = Arrays.copyOfRange(outData,0,outLen);
        return resp;
    }

    public void performTLEKeyExchange(String pin,int selHost) {
        byte[] recievedPacket;
        byte[] apduCommandBytes = null;
        byte[] apduResult = null;

        CommEngine comm;

        Thread tleIniThread = new Thread(new Runnable() {
            @Override
            public void run() {
                tleBankCard =  new BankCard(appContext);
            }
        });

        tleIniThread.start();
        try {
            tleIniThread.join();
        } catch (Exception ex){}

        //initialization done
        TData tData = TranStaticData.getTran(TranStaticData.TranTypes.KEYEXCHANGE);
        currentTransaction = new Transaction();

        currentTransaction.MTI = tData.MTI;
        currentTransaction.procCode = tData.ProcCode;
        currentTransaction.bitmap = tData.Bitmap;
        currentTransaction.inChipStatus = Transaction.ChipStatusTypes.CHIP_CARD;
        currentTransaction.inTransactionCode = TranStaticData.TranTypes.KEYEXCHANGE;
        GlobalData.transactionName = tData.tranName;

        pinEnetered = pin;

        //TLE key exchange is performed using the first tid , Usually the first
        //TID is configured as a visa tid so we copy a dummy PAN to simulate this as a
        //visa transaction but we do not perform any payment related processing
        currentTransaction.PAN = "4532130200601587";

        //check whether a chip card is already inserted before proceeding
        callCallBack("Preparing..",1);

        try {
            currentTransaction.issuerNumber = IssuerHostMap.hosts[selHost].baseIssuer;
            currentTransaction.actualIssuerNumber = currentTransaction.issuerNumber;
            loadIssuer(currentTransaction.issuerNumber);

            //get the first merchant
            String merchantQuary = "SELECT MerchantNumber FROM TMIF WHERE IssuerNumber = " + currentTransaction.issuerNumber + " LIMIT 1";

            Cursor merchRec = configDatabase.readWithCustomQuary(merchantQuary);
            if (merchRec == null || merchRec.getCount() == 0)
                return ;

            merchRec.moveToFirst();

            currentTransaction.merchantNumber = merchRec.getInt(merchRec.getColumnIndex("MerchantNumber"));              //TLE key is always downloaded using the first tid

            if (!loadTerminal())
                return;

            currentTransaction.Date = Formatter.getCurrentDateFormatted();
            currentTransaction.Time = Formatter.getCurrentTimeFormatted();

            comm = CommEngine.getInstance(currentTransaction.issuerData.IP,currentTransaction.issuerData.port);
            comm.setRectimeout(SettingsInterpreter.getReversalTimeout());
            comm.disconnect();

            callCallBack("Extracting Data...",1);
            //power up success so we try to get the serial number

            int cardResult;

            byte[] outData = new byte[128];
            int [] len = new int[1];

            cardResult = tleBankCard.readCard(BankCard.CARD_TYPE_NORMAL, BankCard.CARD_MODE_MAG | BankCard.CARD_MODE_ICC | BankCard.CARD_MODE_PICC, 60, outData, len, "com.harshana.wposandroiposapp.TLE");

            cardResult = tleBankCard.iccDetect();
            cardResult = tleBankCard.piccDetect();

            //open the card reader
           // cardResult =  tleBankCard.openCloseCardReader(BankCard.CARD_MODE_ICC,0x01);

            //select the application
            apduCommandBytes = APDU.ContructAPDU(APDU_SELECT_APP,TLE_AID);
            apduResult = exchangeApdu(apduCommandBytes);
            String result = Utility.byte2HexStr(apduResult);

            if (result.endsWith("9000")) {
                callCallBack("Extracting Serial",1);
                //get the serial number
                apduCommandBytes = APDU.ContructAPDU(APDU_GET_SERIAL,null);
                apduResult = exchangeApdu(apduCommandBytes);
                result = Utility.byte2HexStr(apduResult);

                if (result.endsWith("9000")) {
                    //get the serial number here
                    cardSerialNumber = result.substring(0,result.length() - 4);

                    callCallBack("Getting PIN V-Mode",1);
                    //get the pin verification mode
                    apduCommandBytes = APDU.ContructAPDU(APDU_GET_PIN_VERIF_MODE,null);
                    apduResult = exchangeApdu(apduCommandBytes);
                    result = Utility.byte2HexStr(apduResult);

                    if (result.endsWith("9000")) {
                        pinVerfivicationMode = result.substring(0,result.length() - 4);

                        callCallBack("Generating PIN Block",1);
                        apduCommandBytes = APDU.ContructAPDU(APDU_GET_PIN_VERFICATION,pin);
                        apduResult = exchangeApdu(apduCommandBytes);
                        result = Utility.byte2HexStr(apduResult);

                        //get the verified pin block here
                        if (result.endsWith("9000")) {
                            tlePINBlock = result.substring(0,result.length() - 4);

                            if (pinVerfivicationMode.equals("01")) //pin verification is online
                                currentTransaction.encryptedPINBlock = tlePINBlock;

                            apduCommandBytes = APDU.ContructAPDU(APDU_GET_COUNTER,null);
                            apduResult = exchangeApdu(apduCommandBytes);
                            result = Utility.byte2HexStr(apduResult);

                            //extract the used counter here
                            if (result.endsWith("9000")) {
                                cardKeyDownloadCounter = result.substring(0,result.length() - 4);

                                setFieldsAndLoadPacket();
                                currentTransaction.TPDU = Formatter.replaceNII(currentTransaction.TPDU,currentTransaction.secureNII);
                                packet.setPacketTPDU(currentTransaction.TPDU);

                                callCallBack("Generating MAC",1);
                                byte[] rawPacket = packet.getRawDataPacketWithMac();
                                String hexPacket = Utility.byte2HexStr(rawPacket);
                                byte[] SHAresult = MAC.makeSHA1ForPacket(hexPacket);
                                String shaDigest = Utility.byte2HexStr(SHAresult) + "80000000";
                                String apdu = APDU_GET_MAC + 18 + shaDigest;

                                apduCommandBytes = Utility.hexStr2Byte(apdu);
                                apduResult = exchangeApdu(apduCommandBytes);
                                result = Utility.byte2HexStr(apduResult);

                                if (result.endsWith("9000"))
                                    mac = result.substring(0,result.length() - 4);
                            }

                            callCallBack("Pin Verification is Through",1);
                        }
                        else {
                            //pin verification failed
                            callCallBack("Pin Verification Failed",1);
                            errorTone();
                            return;
                        }
                    }
                    else {
                        showToast("Getting PIN verification mode failed, Aborting!",TOAST_TYPE_FAILED);
                        errorTone();
                        return;
                    }
                }
            }
            else {
                showToast("APDU response error",TOAST_TYPE_FAILED);
                return;
            }
        } catch (RemoteException ex) {
            ex.printStackTrace();
            return;
        }

        callCallBack("Start Sending",1);
        setFieldsAndLoadPacket();
        Log.e("PRINT TPDU", "02 ERE : "+currentTransaction.TPDU);
        Log.e("PRINT secureNII", "02 ERE : "+currentTransaction.secureNII);
        currentTransaction.TPDU = Formatter.replaceNII(currentTransaction.TPDU,currentTransaction.secureNII);
        Log.e("PRINT TPDU 2", "02 ERE : "+currentTransaction.TPDU);
        packet.setPacketTPDU(currentTransaction.TPDU);
        byte[] rawPacket = packet.getRawDataPacketWithMac();

        String packet = Utility.byte2HexStr(rawPacket);
        Log.e("PRINT ERER", "02 ERE : "+packet);
        recievedPacket = sendAndRecieve(comm,rawPacket);
        packet = Utility.byte2HexStr(recievedPacket);
        Log.e("PRINT ERER", "03 ERE : "+packet);

        //A reversal must be generated for this transaction
        if (isReversal) {
            showToast("No Server Response",TOAST_TYPE_INFO);
            errorTone();
            return;
        }
        else if (recievedPacket == null) {
            showToast("Communication Failure",TOAST_TYPE_INFO);
            errorTone();
            return;
        }

        ISO8583u response =  new ISO8583u();
        String test = Utility.byte2HexStr(recievedPacket);

        if (response.unpack(recievedPacket,0)) {
            String responseCode = getHostResponse(response);
            if (responseCode != null &&  responseCode.equals("00")) {
                unpackFieldsAndStoreInTransaction(response);
                //showToast("Receive Success");
                callCallBack("Server Returned Success",1);
                comm.disconnect();

                callCallBack("Saving Keys",1);

                //extract and save the returned keys
                String key = saveExtractedEncryptedKeys();
                if (key == null)
                    return;

                callCallBack("Generating Keys",1);
                //Dukpt dkpt =  Dukpt.getInstance();
                //dkpt.inInitKeyDukpt(key);
                callCallBack("Key Download Successful",0);
            }
            else if (responseCode == null) {
                showToast("No Response Code is Set in Server Response,Aborting Process..",TOAST_TYPE_FAILED);
                errorTone();
                comm.disconnect();
            }
            else {
                try {
                    showToast(ErrData.getErrorDesc(Integer.valueOf(responseCode)),TOAST_TYPE_INFO);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                //host declined or other error
                comm.disconnect();
                callCallBack("Server Declined [" + responseCode + "]",1);
                forceRemoveCard();
                return;
            }
        }
        else {
            showToast("Packet Error",TOAST_TYPE_FAILED);
            errorTone();
            comm.disconnect();
            forceRemoveCard();
            return;
        }

        forceRemoveCard();
        callCallBack("Ready",1);
    }

    String  setOddParityKey(String hexKey) {
        int i=0;
        byte b = 0;

        byte[] keyBytes = Utility.hexStr2Byte(hexKey);

        for (i = 0; i < keyBytes.length; i++) {
            b = keyBytes[i];
            keyBytes[i] =(byte) ((b & (byte)0xFE) | ((((b >> 1) ^ (b >> 2) ^ (b >> 3) ^ (b >> 4) ^ (b >> 5) ^ (b >> 6) ^ (b >> 7)) ^ (byte)0x01) & (byte)0x01));
        }

        String key = Utility.byte2HexStr(keyBytes);
        return key;
    }

    public static final int VISA_MASTER_TMK_INDEX = 3;
    public static final int VISA_MASTER_MAC_INDEX = 4;
    public static final int AMEX_TMK_INDEX = 5;
    public static final int AMEX_MAC_INDEX = 6;
    public static final int JCB_TMK_INDEX = 7;
    public static final int JCB_MAC_INDEX = 8;

    private String  saveExtractedEncryptedKeys() {
        String tleData = UnpackedPacket.Field62;
        if (tleData == null) {
            showToast("No TLE Key Data, Aborting",TOAST_TYPE_INFO);
            return null;
        }
        try {
            String test =  Utility.asciiToString(tleData);
        } catch (Exception ex) {
            showToast("No TLE Key Data, Aborting",TOAST_TYPE_INFO);
            return null;
        }

        tleData = tleData.substring(40);

        int start = 0;
        int indexDelim = tleData.indexOf("01");
        start = indexDelim + 2;

        callCallBack("Getting Key ID",1);
        indexDelim = tleData.indexOf("01",start);
        String keyID = tleData.substring(start,indexDelim);
        start = indexDelim + 2;

        callCallBack("Parsing TMK",1);
        indexDelim = tleData.indexOf("01",start);
        String tmk = tleData.substring(start,indexDelim);
        start = indexDelim + 2;

        callCallBack("Parsing TMK check value",1);
        indexDelim = tleData.indexOf("01",start);
        String tmkCheck = tleData.substring(start,indexDelim);
        start = indexDelim + 2;

        callCallBack("Parsing MAC",1);
        indexDelim = tleData.indexOf("01",start);
        String mac = tleData.substring(start,indexDelim);
        start = indexDelim + 2;

        callCallBack("Parsing MAC check value",1);
        indexDelim = tleData.indexOf("01",start);
        if (indexDelim == -1)
            indexDelim = tleData.length();
        String macCheck = tleData.substring(start,indexDelim);
        start = indexDelim + 2;

        callCallBack("Verifying KCVs",1);

        if ( (!checkKey(tmk,tmkCheck)) || (!checkKey(mac,macCheck))) {
            showToast("Key check failed",TOAST_TYPE_FAILED);
            errorTone();
            return null;
        }

        tmk = decryptKeys(tmk);
        mac = decryptKeys(mac);

        callCallBack("Injecting Secure Area",1);

        tmk = setOddParityKey(tmk);
        mac = setOddParityKey(mac);

        //store the key ID for visa in 32
        keyID  = Utility.asciiToString(keyID);
        byte[] data = Utility.hexStr2Byte(keyID);

        String dummy = "0000000000000000";
        int host = getHostGroupRefKeyDownload();

        if (host == 0) {//for visa master
            TLEKeyGeneration.inStoreSecureAria(data,TLE.VISA_MASTER_KEY_ID_INDEX);  //key id

            //reset the initial counter
            data = Utility.hexStr2Byte(dummy);
            TLEKeyGeneration.inStoreSecureAria(data,TLE.VISA_MASTER_EF_INDEX);      //e/f
            TLEKeyGeneration.inStoreSecureAria(data,TLE.VISA_MASTER_COUNTER_INDEX);  //counter
            data = Utility.hexStr2Byte(tmk);

            //pass the host name as the package name
            if (TLE.isHardwareDUKPTEnabled) {
                Keys.setKeyForDUKPT(tmk);
                Keys.setMacKey(mac);
            }
        }
        else if (host == 1) {
            TLEKeyGeneration.inStoreSecureAria(data,TLE.AMEX_KEY_ID_INDEX);  //key id

            data = Utility.hexStr2Byte(dummy);
            TLEKeyGeneration.inStoreSecureAria(data,TLE.AMEX_EF_INDEX);
            TLEKeyGeneration.inStoreSecureAria(data,TLE.AMEX_COUNTER_INDEX);
            data = Utility.hexStr2Byte(tmk);

            //pass the host name as the package name
            if (TLE.isHardwareDUKPTEnabled) {
                Keys.setKeyForDUKPT(tmk);
                Keys.setMacKey(mac);
            }
        }
        else if (host == 2) {
            TLEKeyGeneration.inStoreSecureAria(data,TLE.JCB_KEY_ID_INDEX);  //key id

            data = Utility.hexStr2Byte(dummy);
            TLEKeyGeneration.inStoreSecureAria(data,TLE.JCB_EF_INDEX);
            TLEKeyGeneration.inStoreSecureAria(data,TLE.JCB_COUNTER_INDEX);
            data = Utility.hexStr2Byte(tmk);

            //pass the host name as the package name
            if (TLE.isHardwareDUKPTEnabled) {
                Keys.setKeyForDUKPT(tmk);
                Keys.setMacKey(mac);
            }
        }

        return tmk;
    }

    public static int getHostGroupRefKeyDownload() {
        int numHosts = IssuerHostMap.numHostEntries;
        int curIssuer  = currentTransaction.issuerNumber;

        for (int host = 0 ; host < numHosts; host++) {
            HostIssuer hostSel = IssuerHostMap.hosts[host];
            List<Integer> issuers = hostSel.issuerList;
            if (issuers.contains(curIssuer))
                return host;
        }

        return -1;
    }

    private String decryptKeys(String key) {
        byte apduCommandBytes[] = null;
        byte apduResult[] = null;
        String result;
        try {
            //insertReader.powerUp();
            apduCommandBytes = APDU.ContructAPDU(APDU_SELECT_APP,TLE_AID);
            apduResult = exchangeApdu(apduCommandBytes);
            result = Utility.byte2HexStr(apduResult);

            if (pinVerfivicationMode.equals("02")) {
                apduCommandBytes = APDU.ContructAPDU(APDU_GET_PIN_VERFICATION,pinEnetered);
                apduResult = exchangeApdu(apduCommandBytes);
                result = Utility.byte2HexStr(apduResult);
            }

            apduCommandBytes = APDU.ContructAPDU(APDU_GET_ENC_METHOD,null);
            apduResult = exchangeApdu(apduCommandBytes);
            result = Utility.byte2HexStr(apduResult);

            if (result.endsWith("9000")) {
                encryptionAlgo = result.substring(0,result.length() - 4);

                key = Utility.asciiToString(key);
                if (encryptionAlgo.equals("01"))
                    apduCommandBytes = APDU.ContructAPDU(APDU_3DES_DEC,key);
                else if (encryptionAlgo.equals("02"))
                    apduCommandBytes = APDU.ContructAPDU(APDU_RSA_DEC,key);

                apduResult = exchangeApdu(apduCommandBytes);
            }
            //insertReader.powerDown();
        } catch (Exception ex) {
            return null;
        }

        String clearKey = Utility.byte2HexStr(apduResult);
        clearKey = clearKey.substring(0,clearKey.length() - 4);

        return  clearKey;
    }
    //keys should be checked against the sent values from the host

    String encryptionAlgo = null;

    private boolean checkKey(String key,String keyCheck) {
        byte apduCommandBytes[] = null;
        byte apduResult[] = null;
        String result;

        String decryptedKey = "";
        String dummyText = "0000000000000000";
        String strCheckValue="";

        try {
            //insertReader.powerUp();
            apduCommandBytes = APDU.ContructAPDU(APDU_SELECT_APP,TLE_AID);
            apduResult = exchangeApdu(apduCommandBytes);
            result = Utility.byte2HexStr(apduResult);

            if (result.endsWith("9000")) {
                if (pinVerfivicationMode.equals("02")) {
                    apduCommandBytes = APDU.ContructAPDU(APDU_GET_PIN_VERFICATION,pinEnetered);
                    apduResult = exchangeApdu(apduCommandBytes);
                    result = Utility.byte2HexStr(apduResult);
                }

                apduCommandBytes = APDU.ContructAPDU(APDU_GET_ENC_METHOD,null);
                apduResult = exchangeApdu(apduCommandBytes);
                result = Utility.byte2HexStr(apduResult);

                if (result.endsWith("9000")) {
                    encryptionAlgo = result.substring(0,result.length() - 4);

                    if (pinVerfivicationMode.equals("02")) {
                        apduCommandBytes = APDU.ContructAPDU(APDU_GET_PIN_VERFICATION,pinEnetered);
                        apduResult = exchangeApdu(apduCommandBytes);
                        result = Utility.byte2HexStr(apduResult);
                    }
                    key = Utility.asciiToString(key);

                    if (encryptionAlgo.equals("01"))
                        apduCommandBytes = APDU.ContructAPDU(APDU_3DES_DEC,key);
                    else if (encryptionAlgo.equals("02"))
                        apduCommandBytes = APDU.ContructAPDU(APDU_RSA_DEC,key);

                    apduResult = exchangeApdu(apduCommandBytes);

                    //insertReader.powerDown();
                    result = Utility.byte2HexStr(apduResult);

                    if (result.endsWith("9000")) {
                        //we have the decrypted key
                        decryptedKey = result.substring(0,result.length() - 4);
                        byte[] checkByteValues = DESCrypto.encrypt3Des(dummyText,decryptedKey);
                        strCheckValue = Utility.byte2HexStr(checkByteValues);
                        keyCheck = Utility.asciiToString(keyCheck);

                        return strCheckValue.startsWith(keyCheck);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

        return false;
    }

    public static String getMac() {
        String m  = mac;
        mac = "";
        return m;
    }

    public static String getTLEHeader() {
        String tleHeader = "";
        String pinBlock = "";
        String eAlgo = "";
        String uKId = "";
        String keySize = "";
        String macAlgo  = "";
        String dMTR = "";
        String aid = "";
        String onlineOffiline = "";
        String counter = "";
        String serialLength = "";
        String crdSerial = "";

        int issuerNumber = 1;

        /*try
        {
            IDeviceInfo deviceInfo = services.deviceService.getDeviceInfo();
            crdSerial = deviceInfo.getSerialNo();
        }catch (RemoteException ex)
        {
            ex.printStackTrace();
            return null;
        }*/

        //load the data from the TLE table
        String quary = "SELECT * FROM TLE WHERE ID = " + issuerNumber;
        Cursor tleData = configDatabase.readWithCustomQuary(quary);

        if (tleData == null && tleData.getCount() == 0 )
            return null;

        tleData.moveToFirst();

        eAlgo = tleData.getString(tleData.getColumnIndex("EAlgo"));
        uKId  = tleData.getString(tleData.getColumnIndex("UKID"));
        keySize = tleData.getString(tleData.getColumnIndex("KSize"));
        macAlgo = tleData.getString(tleData.getColumnIndex("MAlgo"));
        dMTR = tleData.getString(tleData.getColumnIndex("DMTR"));
        aid = tleData.getString(tleData.getColumnIndex("AID"));
        onlineOffiline = pinVerfivicationMode;

        counter = cardKeyDownloadCounter;
        crdSerial =  cardSerialNumber;
        crdSerial = Formatter.fillInBack("0",crdSerial,16);
        int serialLen = crdSerial.length() ;
        String sSerialLenHex =  Integer.toHexString(serialLen);
        tleHeader = eAlgo + uKId + keySize + macAlgo + dMTR + aid + onlineOffiline + counter + sSerialLenHex + crdSerial;

        return tleHeader;
    }
}