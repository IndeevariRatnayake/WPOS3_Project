package com.harshana.wposandroiposapp.TLE;


import android.database.Cursor;
import android.util.Base64;
import android.util.Log;

import com.harshana.wposandroiposapp.Base.Base;
import com.harshana.wposandroiposapp.Base.ErrData;
import com.harshana.wposandroiposapp.Base.Keys;
import com.harshana.wposandroiposapp.Crypto.DESCrypto;
import com.harshana.wposandroiposapp.Crypto.MAC;
import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.DevArea.GlobalData;
import com.harshana.wposandroiposapp.DevArea.PacketDev;
import com.harshana.wposandroiposapp.DevArea.TranStaticData;
import com.harshana.wposandroiposapp.Packet.ISO8583u;
import com.harshana.wposandroiposapp.Print.Receipt;
import com.harshana.wposandroiposapp.Settings.SettingsInterpreter;
import com.harshana.wposandroiposapp.Utilities.Formatter;
import com.harshana.wposandroiposapp.Utilities.Utility;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


/**
 * Created by harshana_m on 12/27/2018.
 */

public class TLE extends Base
{
    protected static final int TYPE_BCD = 1;
    protected static final int TYPE_ASC = 2;

    public static final int VISA_MASTER_SESSION_KEY_INDEX = 29;
    public static final int VISA_MASTER_EF_INDEX = 30;
    public static final int VISA_MASTER_COUNTER_INDEX = 31;
    public static final int VISA_MASTER_KEY_ID_INDEX = 32;

    public static final int AMEX_EF_INDEX = 33;
    public static final int AMEX_COUNTER_INDEX = 34;
    public static final int AMEX_KEY_ID_INDEX = 35;

    public static final int JCB_EF_INDEX = 36;
    public static final int JCB_COUNTER_INDEX = 37;
    public static final int JCB_KEY_ID_INDEX = 38;

    public static String KCV = "";

    public static boolean isHardwareDUKPTEnabled = true;

    private static TLE myInstance = null;

    String transactionRandomNumber = "";
    String sessionKey = null;

    public static TLE getInstance() {
        if (myInstance == null)
            myInstance = new TLE();

        return myInstance;
    }

    private static String base64EncodedString = "";
    private static String transactionMAC = "";

    public static String getTransactionMAC()
    {
        return transactionMAC;
    }
    static int tleHeaderLength = 0 ;


    public static void updateKSN() {
        if (SettingsInterpreter.isTLEEnabled() && isHardwareDUKPTEnabled) {
            String ksn = "";
            Keys.increaseKSNforDukpt();
            ksn  = Keys.getKSNforDukpt();
            GlobalData.ksn = ksn.substring(4);
            Log.e("KKKKKKK", "____________________");
        }
    }

    public static String getTransactionEncryptedData() {
        tleHeaderLength = 0;
        String header = TLEKeyGeneration.szTLE_Header();
        tleHeaderLength  = header.length();
        return  header + base64EncodedString;
    }

    private String getMacKey() {
        int host = TLEKeyDownload.getHostGroupRefKeyDownload();
        String macKey = null;

        if (host == 0) //for visa master
            macKey = Keys.getKeyFromSlot(TLEKeyDownload.VISA_MASTER_MAC_INDEX);
        else if (host  == 1)
            macKey = Keys.getKeyFromSlot(TLEKeyDownload.AMEX_MAC_INDEX);
        else if (host  == 2)
            macKey = Keys.getKeyFromSlot(TLEKeyDownload.JCB_MAC_INDEX);

        return macKey;
    }

    //load the encrypting fields from the database
    private List<Integer> getEncryptingFieldList(int issuerNumber) {
        DBHelper configDatabase = DBHelper.getInstance(appContext);
        Log.e("ISSUU+ER", "issuerNumber : " + issuerNumber);
        String fieldName  = "";
        String selectQuarry = "SELECT * FROM TFI WHERE IssuerNumber = " + issuerNumber;
        List<Integer> fieldList = new ArrayList<>();
        int[] fieldsToCheck = {2,3,4,11,12,13,14,22,23,24,25,35,37,38,39,41,42,45,48,52,54,55,60,61,62,63};

        try {
            Cursor fieldResult = configDatabase.readWithCustomQuary(selectQuarry);

            Log.e("ISSUU+ER", "fieldResult : " + fieldResult);
            Log.e("ISSUU+ER", "fieldResult sizw : " + fieldResult.getCount());

            if (fieldResult == null || fieldResult.getCount() == 0)
                return null;

            fieldResult.moveToFirst();      //get the first selected record

            /*for (int index = 2; index < 64; index++) {
                try {
                    fieldName = "Field" + index;
                    Log.e("GGG fieldName"," : "+fieldName);
                    int value = fieldResult.getInt(fieldResult.getColumnIndex(fieldName));

                    if (value == 1)         //the field should be encrypted
                        fieldList.add(index);

                }catch (Exception ex)
                {
                    ex.printStackTrace();
                    continue;
                }
            }*/

            for(int i = 0; i < fieldsToCheck.length; i++) {
                try {
                    fieldName = "Field" + fieldsToCheck[i];
                    int value = fieldResult.getInt(fieldResult.getColumnIndex(fieldName));

                    if (value == 1)
                        fieldList.add(fieldsToCheck[i]);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    continue;
                }
            }

            fieldResult.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        Log.e("ISSUU+ER", "fieldList sizeeee : " + fieldList.size());
        return fieldList;
    }

    //the function to decrypt the packets sensitive data. this method employs the hardware encryption and decryption
    //facility . for transaction encryption hardware enabled dukpt is employed
    public byte[] decryptPacket(byte[] dataPacket)
    {
        ISO8583u encryptedPacket =  new ISO8583u();

        if (!encryptedPacket.unpack(dataPacket))
            return null;                            //unpack error


        updateStatusText("Decryption Started..");
        String encHexPacket = Utility.byte2HexStr(dataPacket);
        String bitmap = encHexPacket.substring(18, 18 + 16);

        //get the list of fields which are already in the packet
        List<Integer> fieldList =  getFieldList(bitmap);

        if (fieldList == null)
            return null;                 //getting field list

        Map<Integer,String> encFieldDataMap = new HashMap<>();

        for (Integer field : fieldList) {
            if (encryptedPacket.unpackValidField[field]) {
                String data = encryptedPacket.getUnpack(field);
                encFieldDataMap.put(field,data);

                String error = "";
                if (field == 39) {
                    //if its a TLE error we cease processing and return
                    try {
                        int resp = Integer.valueOf(data,16);
                        if (resp >= 0xC1) {
                            error = ErrData.getErrorDesc(resp);
                            showToast(error,TOAST_TYPE_INFO);
                            return null;
                        }
                    } catch (Exception ex) {
                        showToast("Unknown resp code back end " + error,TOAST_TYPE_INFO);
                        return null;
                    }
                }
            }
        }

        // we have got all the fields by now
        //check whether the packet contained the encrypted data and the mac
        if (!encFieldDataMap.containsKey(57))
        {
            errorTone();
            showToast("No Encrypted Data Found, Aborting",TOAST_TYPE_WARNING);
            return null;
        }
        else if (!encFieldDataMap.containsKey(64))
        {
            errorTone();
            showToast("No Mac Found in the Response Message",TOAST_TYPE_WARNING);
            return null;
        }

        String field57Data = encFieldDataMap.get(57);
        field57Data = field57Data.substring(tleHeaderLength);
        String mac = encryptedPacket.getUnpack(64);

        //here we have encrypted data found we start decrypting the data
        String Base64String = field57Data;
        byte[] Base64Decoded = Base64.decode(Base64String,Base64.DEFAULT);
        String hexDecoded = Utility.byte2HexStr(Base64Decoded);

        byte[] dukptDecrypted = DESCrypto.dukptDecryptData(hexDecoded);
        String decrHexString = Utility.byte2HexStr(dukptDecrypted);
        String decryptedString = decrHexString;

        //now we have the plain encrypted fields, start extracting the fields
        //parsing the fields
        int field;
        int length;
        String data;
        String asciiOrNot = "0";

        Map<Integer,String> clearPacketMap =  new HashMap<>();
        String randomNumber  = decryptedString.substring(0,6);

        if (!randomNumber.equals(transactionRandomNumber))
        {
            showToast("ICV Failure",TOAST_TYPE_FAILED);
            errorTone();
            return null;
        }

        decryptedString = decryptedString.substring(6);

        //extract if there is any padding
        String sNumPadded = decryptedString.substring(decryptedString.length() - 1);
        int numPadded  = Integer.valueOf(sNumPadded,16);

        numPadded *= 2;
        //numPadded++; // commented for sampath new TLE

        int offset = 0;

        if (numPadded < decryptedString.length())
            decryptedString = decryptedString.substring(0 , decryptedString.length() - numPadded);
        else
            offset = decryptedString.length();


        try
        {
            //remove padding if there is any
            while (offset < decryptedString.length())
            {
                //extracting  the field number
                field = Integer.valueOf(decryptedString.substring(offset,offset + 3));
                offset += 3;

                //extracting the ascii converted or not
                asciiOrNot = decryptedString.substring(offset,offset + 1);
                offset++;

                //extracting the length of the field
                length = Integer.valueOf(decryptedString.substring(offset,offset + 3));
                offset += 3;


                //extracting the data field
                data = decryptedString.substring(offset,offset + length);

                //please keep an eye on below code. which is used to fix the wrong length interpretation of odd length field
                if (length % 2 != 0)
                    data  = "0" + data;

                if (asciiOrNot.equals("1"))    // yes ascii encoded and decode to a utf-8 string
                    data = Utility.asciiToString(data);

                offset += length;

                //add to the new encrypted map
                clearPacketMap.put(field,data);
            }
        }catch (Exception ex)
        {
            ex.printStackTrace();
        }


        //add the non encrypted fields in the original packet too in the clear packet map
        for (Map.Entry<Integer,String> item: encFieldDataMap.entrySet())
        {
            int fieldx = item.getKey();
            String datax  = item.getValue();

            if (fieldx == 57 || fieldx == 64)
                continue;

            clearPacketMap.put(fieldx,datax);
        }

        //make a new packet from the constructed fields
        //set the mti from the received packet
        String mti = encHexPacket.substring(14,14 + 4);
        clearPacketMap.put(0,mti);
        byte[] clearPacketBuffer = packet.getRawDataPacketWithMap(clearPacketMap);
        String clearPacketHex  = Utility.byte2HexStr(clearPacketBuffer);

        //skipped the mac failure check for new tle of sampath
        clearPacketHex = clearPacketHex.substring(0 , clearPacketHex.length() - 18);
        String currentTranMac = generateMacForPacket(clearPacketHex);


        if (!mac.equals(currentTranMac))
            showToast("Mac Failed at Terminal",TOAST_TYPE_FAILED);//calculate the mac for the received packet using sha - 1

        updateStatusText("Decrypted..");
        updateStatusText("Terminal Ready");
        return clearPacketBuffer;
    }


    String generateMacForPacket(String packet)
    {
        String mac = "";
        String forMacVerification = packet.substring(14);

        try
        {
            byte[] macBytes  = MAC.makeSHA1(forMacVerification);
            mac = Utility.byte2HexStr(macBytes);

            mac = mac + "80" + "000000";
            byte[] initVec = new byte[8];
            byte[] macByteArr = Utility.hexStr2Byte(mac);

            //xor the first byte of the init vector
            byte[] selected = Arrays.copyOfRange(macByteArr,0,8);
            byte[] result = Utility.xorArrays(initVec,selected);

            String XorRestult = Utility.byte2HexStr(result);
            result = DESCrypto.encryptUsingMAKKeyHardware(result);

            String encResult = Utility.byte2HexStr(result);

            selected = Arrays.copyOfRange(macByteArr,8,16);
            result = Utility.xorArrays(result,selected);
            result =  DESCrypto.encryptUsingMAKKeyHardware(result);

            selected = Arrays.copyOfRange(macByteArr,16,24);
            result = Utility.xorArrays(result,selected);
            result = DESCrypto.encryptUsingMAKKeyHardware(result);

            mac = Utility.byte2HexStr(result);

        }catch ( Exception ex)
        {
            ex.printStackTrace();
            return null;
        }

        return mac;
    }

    public static List<Integer> getFieldList(String bitmapx) {
        List<Integer> fieldList  =  new ArrayList<>();
        byte [] bitmap = Utility.hexStr2Byte(bitmapx);

        for (int byteIndex = 7 ; byteIndex >= 0; byteIndex--) {
            byte curByte = bitmap[byteIndex];
            for (int bitIndex = 0; bitIndex < 8; bitIndex++) {
                if ((curByte & (1 << bitIndex)) == (1 << bitIndex)) {
                    int index = bitIndex  + ( 8 * ( 7 - byteIndex));
                    index = 64 - index;
                    fieldList.add(index);
                }
            }
        }

        return fieldList;
    }

    protected static String resetField(int fieldNumber, String map) {
        //extract the char position
        int pos = (64 - fieldNumber) / 8 + 1 ; // which byte from the left

        int beginIndex =  map.length() - (pos * 2);
        String header = map.substring(0,beginIndex);
        String exByte = map.substring(beginIndex ,beginIndex + 2 ) ;
        String trailer = map.substring(beginIndex + 2);

        int value = Integer.valueOf(exByte,16);

        int bitShift =  fieldNumber % 8 ;
        if (bitShift != 0)
        {
            bitShift = 8 - bitShift;
            bitShift = 1 << bitShift;
            bitShift = ~bitShift;
        }
        else
        {
            bitShift = 1;
            bitShift = ~bitShift;
        }


        value = (value & bitShift);
        exByte = Integer.toHexString(value);

        if (exByte.length() == 1)
            exByte = "0" + exByte;

        map = header + exByte + trailer;
        return map;

    }


    protected static String setField(int fieldNumber,String map)
    {
        //extract the character position
        int pos = (64 - fieldNumber) / 8 + 1 ; // which byte from the left

        int beginIndex =  map.length() - (pos * 2);
        String header = map.substring(0,beginIndex);
        String exByte = map.substring(beginIndex ,beginIndex + 2 ) ;
        String trailer = map.substring(beginIndex + 2);

        int bitShift =  fieldNumber % 8 ;
        int value = Integer.valueOf(exByte,16);
        bitShift = 8 - bitShift;
        bitShift = 1 << bitShift;

        value = (value | bitShift);

        if (fieldNumber == 64)          //put this since the above logic breaks for this field number
        {
            //get the ex byte and set the last bit of it
            int val = Integer.valueOf(exByte,16);
            val  = (val | 1);

            //convert back to string
            exByte = Integer.toHexString(val);
        }
        else
            exByte = Integer.toHexString(value);

        if (exByte.length() == 1)
            exByte = "0" + exByte;

        map = header + exByte + trailer;
        return map;
    }


    private int MAX  = 999999;
    private int MIN  = 900000;

    private String getRandomForTLE()
    {
        Random rnd =  new Random();
        int rand  = rnd.nextInt(MAX - MIN) + MIN;

        String sRand = String.valueOf(rand);
        return sRand;
    }

    public  byte[] encryptPacket(byte [] dataPacket) {
        try {
            Log.e("EncryptPac", "____________________");
            ISO8583u originalPacket = new ISO8583u();
            sessionKey = null;

            transactionRandomNumber = "";
            if (!originalPacket.unpack(dataPacket))
                return null;
            Log.e("EncryptPac", "1");

            TLE.updateKSN();
            Log.e("EncryptPac", "2");

            updateStatusText("Encryption Started");

            //we have a valid packet unpacked
            //get the bit map from the original packet
            String origHexPacket = Utility.byte2HexStr(dataPacket);
            String bitmap = origHexPacket.substring(18, 18 + 16);
            Log.e("EncryptPac", "3");

            transactionMAC = generateMacForPacket(origHexPacket);
            Log.e("EncryptPac", "4");

            //get the list of fields which have been set in the packet
            List<Integer> origPacketFields = getFieldList(bitmap);

            if (origPacketFields == null)
                return null;
            Log.e("EncryptPac", "5");

            //get the list of fields which have been configured for encryption
            Log.e("HHHHHHH issuerNumber", " : " + currentTransaction.issuerData.issuerNumber);
            List<Integer> shouldEncryptFields = getEncryptingFieldList(currentTransaction.issuerData.issuerNumber);

            int field, length;
            String sField, sLength, asciiOrNot;
            String fieldData;
            String fieldSetStringforTLE = "";
            String fieldForTLE = "";

            for (Integer fieldIndex : origPacketFields) {
                //omit the fields that is not permitted for encryption
                Log.e("HHHHHHH ", "shouldEncryptFields :  " + shouldEncryptFields);
                Log.e("HHHHHHH ", "fieldIndex :  " + fieldIndex);
                if (!shouldEncryptFields.contains(fieldIndex))
                    continue;

                Log.e("HHHHHHH ", "000000000000000");
                field = fieldIndex;
                sField = Formatter.fillInFront("0", String.valueOf(field), 3); //get the formatted field number

                fieldData = originalPacket.getUnpack(field);
                byte data[] = originalPacket.getFieldCust(field, fieldData);

                //get the data as its in the packet
                String dataInPacket = Utility.byte2HexStr(data);
                fieldData = dataInPacket;

                asciiOrNot = "0";
                int fieldType = originalPacket.fieldType(field);

                if (fieldType == TYPE_ASC)
                    asciiOrNot = "1";

                if ((fieldIndex == 22) ||
                        (fieldIndex == 23) ||
                        (fieldIndex == 24)
                )
                    dataInPacket = dataInPacket.substring(1); //strip off the first char

                //modified since all type of transaction modes require to remove the last nibble of the data
                //if  (fieldIndex == 35 /*&& currentTransaction.inChipStatus == Transaction.ChipStatusTypes.EMV_CARD*/)
                //dataInPacket = dataInPacket.substring(0,dataInPacket.length() - 1);

                if (fieldIndex == 35)
                    dataInPacket = setupField35ForTLE(dataInPacket);

                //get the field length and formatted
                length = dataInPacket.length();

                if (fieldIndex == 2 && PacketDev.field2OrigLength > 0)
                    length = PacketDev.field2OrigLength;

                sLength = Formatter.fillInFront("0", String.valueOf(length), 3);

                fieldData = dataInPacket;
                fieldForTLE = sField + asciiOrNot + sLength + fieldData;

                //concat for the final string
                fieldSetStringforTLE += fieldForTLE;

                //reset the field in the new bitmap
                bitmap = resetField(field, bitmap);
            }

            Log.e("EncryptPac", "6");
            transactionRandomNumber = getRandomForTLE();
            fieldSetStringforTLE = transactionRandomNumber + fieldSetStringforTLE;

            Log.e("EncryptPac", "7");

            //perform the encryption using 3 des
            byte[] encryptedData = null;
            byte[] dukptEncryptedData = null;

            try {
                dukptEncryptedData = DESCrypto.dukptEncryptData(fieldSetStringforTLE);
            } catch (Exception ex) {
                updateStatusText("No keys injected");
                ex.printStackTrace();
                return null;
            }
            Log.e("EncryptPac", "8");

            KCV = DESCrypto.dukptGetCurrentKCV();
            Log.e("EncryptPac", "9");
            encryptedData = dukptEncryptedData;

            //encode the string as a base64 string
            base64EncodedString = Base64.encodeToString(encryptedData, Base64.DEFAULT);

            byte[] decoded = Base64.decode(base64EncodedString, Base64.DEFAULT);

            //set the field 57 and 64
            bitmap = setField(57, bitmap);
            bitmap = setField(64, bitmap);

            currentTransaction.isTLEEncrypting = true;

            //generate the  new packet
            currentTransaction.bitmap = bitmap;
            Log.e("EncryptPac", "10");
            setFieldsAndLoadPacket();
            currentTransaction.TPDU = Formatter.replaceNII(currentTransaction.TPDU, currentTransaction.secureNII);
            packet.setPacketTPDU(currentTransaction.TPDU);
            byte[] rawPacket = packet.getRawDataPacket();

            currentTransaction.isTLEEncrypting = false;
            updateStatusText("Encrypted..");

            return rawPacket;
        } catch (Exception e) {
            e.printStackTrace();
            /*showToast(e.toString(),TOAST_TYPE_INFO);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);

            showToastX(sw.toString(),TOAST_TYPE_INFO);*/
            showToast("Encryption Failed", TOAST_TYPE_INFO);
            return null;
        }
    }

    boolean isPrinting = false;

    void finishReceipt() {
        isPrinting = false;
    }

    public static String setupField35ForTLE(String track2Data) {
        final int trackLen = 37;
          String retData = "";

        //currentTransaction.track2 = "5413330089020045D2512201019460027F";
        track2Data = currentTransaction.track2;
        retData = track2Data;

        //remove F if there is any
        while (track2Data.endsWith("F"))
            track2Data = track2Data.substring(0,track2Data.length() - 1);

        int curTrackLen = track2Data.length();

        if (curTrackLen == trackLen)
            retData = track2Data;
        else if (curTrackLen > trackLen)
        {
            int diff = curTrackLen - trackLen;
            track2Data = track2Data.substring(0,curTrackLen - diff);
            retData = track2Data;
        }


        retData = track2Data;
       /* else if (curTrackLen == 34)
        {
            track2Data = track2Data.substring(0,33);
            retData = track2Data;
        }
        else if ( curTrackLen < trackLen)
        {
            //if the length is odd we add one
            if (!cardLabel.equals("MASTER"))
            {
                if (curTrackLen % 2 != 0)
                    track2Data += "0";
            }

            retData = track2Data;
        }*/

        return  retData;
    }

}
