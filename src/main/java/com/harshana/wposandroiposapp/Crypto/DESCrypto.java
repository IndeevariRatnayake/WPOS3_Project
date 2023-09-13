package com.harshana.wposandroiposapp.Crypto;

import com.harshana.wposandroiposapp.Base.Keys;
import com.harshana.wposandroiposapp.Base.Services;
import com.harshana.wposandroiposapp.Utilities.Formatter;
import com.harshana.wposandroiposapp.Utilities.Utility;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


import wangpos.sdk4.libbasebinder.Core;


/**
 * Created by harshana_m on 12/21/2018.
 */

public class DESCrypto
{
    public static byte[] encrypt3Des(String clearData, String key) throws Exception
    {
        final byte[] keydata1 = Utility.hexStr2Byte(key);
        SecretKeySpec symKey1 = new SecretKeySpec(keydata1 ,"DESede");

        Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,symKey1);

        byte []encrypted =  cipher.doFinal(Utility.hexStr2Byte(clearData));

        return encrypted;
    }

    public static byte[] decrypt3Des(String encData, String key) throws Exception
    {
        final byte[] keydata1 = Utility.hexStr2Byte(key);
        SecretKeySpec symKey1 = new SecretKeySpec(keydata1 ,"DESede");

        Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,symKey1);

        byte []encrypted =  cipher.doFinal(Utility.hexStr2Byte(encData));

        return encrypted;
    }

    public static byte[] encryptDes(String clearData, String key) throws Exception
    {
        final byte[] keydata1 = Utility.hexStr2Byte(key);
        SecretKeySpec symKey1 = new SecretKeySpec(keydata1 ,"DES");

        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,symKey1);

        byte []encrypted =  cipher.doFinal(Utility.hexStr2Byte(clearData));

        return encrypted;
    }

    public static byte[] decryptDes(String encData, String key) throws Exception
    {
        final byte[] keydata1 = Utility.hexStr2Byte(key);
        SecretKeySpec symKey1 = new SecretKeySpec(keydata1 ,"DES");

        Cipher cipher = Cipher.getInstance("DES/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,symKey1);

        byte []encrypted =  cipher.doFinal(Utility.hexStr2Byte(encData));

        return encrypted;
    }

    public static byte[] encrypt3DesBytes(byte[] data, String key) throws Exception
    {
        final byte[] keydata1 = Utility.hexStr2Byte(key);
        SecretKeySpec symKey1 = new SecretKeySpec(keydata1 ,"DESede");

        Cipher cipher = Cipher.getInstance("DESede/ECB/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,symKey1);

        byte []encrypted =  cipher.doFinal(data);

        return encrypted;
    }

    public static byte[] encryptUsingMAKKeyHardware(byte[] data) throws Exception
    {
        byte[] encrypted = new byte[data.length];
        int retVal = -1;

        byte[] vector =  new byte[8]; // initial vector
        int [] outLen = new int[2];


        retVal  = Services.core.dataEnDecryptEx(
                Core.ALGORITHM_DES,
                0x00,
                Keys.getReleventPackageName(),
                Services.core.ENCRYPT_MODE_ECB,
                vector.length,
                vector,
                data.length,
                data,
                0x00,
                encrypted,
                outLen);


        String strEnc = null;

        if (retVal == 0)
            strEnc = Utility.byte2HexStr(encrypted,0,outLen[0]);

        if (strEnc != null)
            encrypted = Utility.hexStr2Byte(strEnc);

        return encrypted;

    }

    public static String dukptGetCurrentKCV() {
        String dummy = "0000000000000000";

        byte[] vector =  new byte[8]; // initial vector
        byte[] dukptEncryptedData = new byte[dummy.length() + 10];
        int [] outLen = new int[2];

        int retVal = 0 ;
        try {

            byte[]  toBeEncrypt = Utility.hexStr2Byte(dummy);

            retVal =  Services.core.dataEnDecryptForIPEK(
                    Core.ALGORITHM_3DES,
                    0,
                    Keys.getReleventPackageName(),
                    0x02,
                    vector.length,
                    vector,
                    toBeEncrypt.length,
                    toBeEncrypt,
                    0x00,
                    dukptEncryptedData,
                    outLen);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        String dukptEncrypted = "";
        if (retVal == 0) {
            int encryptedDatalen = outLen[0];
            dukptEncrypted = Utility.byte2HexStr(dukptEncryptedData,0,encryptedDatalen);
        }

        return dukptEncrypted;
    }

    public static byte[] dukptEncryptData(String clearData) throws Exception {
        byte[] vector =  new byte[8]; // initial vector
        byte[] dukptEncryptedData = new byte[clearData.length()];
        int [] outLen = new int[2];

        int retVal = 0 ;
        try {

            byte[]  toBeEncrypt = DESCrypto.prepareEncryptionDataWithProperPadding(clearData);

            retVal =  Services.core.dataEnDecryptForIPEK(
                    Core.ALGORITHM_3DES,
                    0,
                    Keys.getReleventPackageName(),
                    0x02,
                    vector.length,
                    vector,
                    toBeEncrypt.length,
                    toBeEncrypt,
                    0x00,
                    dukptEncryptedData,
                    outLen);

        }catch (Exception ex)
        {
            ex.printStackTrace();
        }

        String dukptEncrypted = "";
        if (retVal == 0)
        {
            int encryptedDatalen = outLen[0];
            if (encryptedDatalen == 0)
               throw new Exception("No Keys injected");
            dukptEncrypted = Utility.byte2HexStr(dukptEncryptedData,0,encryptedDatalen);
        }

        return Utility.hexStr2Byte(dukptEncrypted);

    }

    public static byte[] dukptDecryptData(String encryptedData)
    {
        byte[] vector =  new byte[8]; // initial vector
        byte[] dukptDecryptedData = new byte[encryptedData.length() * 2];
        int [] outLen = new int[2];

        int retVal = 0 ;
        try
        {

            byte[]  toBeDecrypted = Utility.hexStr2Byte(encryptedData);

            retVal =  Services.core.dataEnDecryptForIPEK(
                    Core.ALGORITHM_3DES,
                    1,
                    Keys.getReleventPackageName(),
                    0x02,
                    vector.length,
                    vector,
                    toBeDecrypted.length,
                    toBeDecrypted,
                    0x00,
                    dukptDecryptedData,
                    outLen);

        }catch (Exception ex)
        {
            ex.printStackTrace();
        }

        String dukptDecrypted = "";

        if (retVal == 0)
        {
            int encryptedDatalen = outLen[0];
            dukptDecrypted = Utility.byte2HexStr(dukptDecryptedData,0,encryptedDatalen);
        }

        return Utility.hexStr2Byte(dukptDecrypted);

    }

    public static byte[] prepareEncryptionDataWithProperPadding(String clearData) throws Exception
    {

        if (clearData.length() % 2 != 0)
            clearData += "0";

        byte[] data = Utility.hexStr2Byte(clearData);

        int numBytes = data.length;
        int numBytesToBePadded = 8  - (numBytes % 8);

        String padString = "";
        numBytesToBePadded *= 2;

        if (numBytesToBePadded > 0)
        {
            for (int i  = 0 ; i < numBytesToBePadded - 2; i++)
                padString += "0";

            String howManyPadded = Integer.toHexString(numBytesToBePadded / 2);
            howManyPadded = Formatter.fillInFront("0",howManyPadded,2);
            padString += howManyPadded;
            clearData += padString;
        }

        data = Utility.hexStr2Byte(clearData);


        return data;
    }

    public static byte[] encrypt3DesWithCBCTLEPadding(String clearData, String key) throws Exception
    {
        byte [] encrypted = null;
        final byte[] keydata1 = Utility.hexStr2Byte(key);
        SecretKeySpec symKey1 = new SecretKeySpec(keydata1 ,"DESede");

        Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
        byte[] x = new byte[8];
        IvParameterSpec iv = new IvParameterSpec(x);
        cipher.init(Cipher.ENCRYPT_MODE,symKey1,iv);

        if (clearData.length() % 2 != 0)
            clearData += "0";

        encrypted = Utility.hexStr2Byte(clearData);

        int numBytes = encrypted.length;
        int numBytesToBePadded = 8  - (numBytes % 8);

        String padString = "";
        numBytesToBePadded *= 2;

        if (numBytesToBePadded > 0)
        {
            for (int i  = 0 ; i < numBytesToBePadded - 2; i++)
                padString += "0";

            String howManyPadded = Integer.toHexString(numBytesToBePadded / 2);
            howManyPadded = Formatter.fillInFront("0",howManyPadded,2);
            padString += howManyPadded;
            clearData += padString;
        }

        encrypted = Utility.hexStr2Byte(clearData);
        encrypted = cipher.doFinal(encrypted);

        return encrypted;
    }



    public static byte[] decrypt3DesCBCTLENoPadding(String clearData, String key) throws Exception
    {

        final byte[] keydata1 = Utility.hexStr2Byte(key);
        SecretKeySpec symKey1 = new SecretKeySpec(keydata1 ,"DESede");

        Cipher cipher = Cipher.getInstance("DESede/CBC/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE,symKey1);

        byte []decrypted =  cipher.doFinal(Utility.hexStr2Byte(clearData));

        return decrypted;

    }



}
