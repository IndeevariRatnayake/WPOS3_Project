package com.harshana.wposandroiposapp.Base;

import android.os.Environment;
import android.os.RemoteException;
import android.util.Log;

import com.harshana.wposandroiposapp.Crypto.DESCrypto;
import com.harshana.wposandroiposapp.Settings.SettingsInterpreter;
import com.harshana.wposandroiposapp.TLE.TLEKeyGeneration;
import com.harshana.wposandroiposapp.Utilities.Formatter;
import com.harshana.wposandroiposapp.Utilities.Utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import wangpos.sdk4.libbasebinder.RspCode;
import wangpos.sdk4.libkeymanagerbinder.Key;

public class Keys {
    private static Key key = Services.keys;

    private static byte [] certData = new byte[8];
    private static String masterKey = "204619C81FD50DC70B7A251A0B981AF4";
    private static String dataEncryptionKey = "204619C81FD50DC70B7A251A0B981AF4";
    private static String pinEncryptionKey =  "204619C81FD50DC70B7A251A0B981AF4";

    private static byte[] checkVal  = new byte[1];

    private static String dpt = "DUKPT";

    public static void increaseKSNforDukpt() {
        int retVal = -1;

        String packageName = getReleventPackageName();
        try {
            Log.d("::::::::::::packageName", " : " + packageName);
//            retVal = key.IncreaseKSN("SAMPATH");
            retVal = key.IncreaseKSN(packageName);
            Log.d(":::::::::::::retVal", " : " + retVal);
        } catch (Exception ex ) {
            ex.printStackTrace();
        }
    }

    public static String getKSNforDukpt() {
        byte[] ksnData = new byte[16];
        int[] len = new int[1];

        String packageName = getReleventPackageName();

        int retval = -1;
        try {
            Log.d("packageName....", " : " + packageName);
            retval = key.GetKSN(packageName, ksnData, len);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Log.d(dpt, "retrieving  the ksn " + String.valueOf(retval));

        return Utility.byte2HexStr(ksnData, 0, len[0]);
    }

    public static String getReleventPackageName() {
        int host = TLEKeyGeneration.inGetHostGroupRef();
        String packageName = IssuerHostMap.hosts[host].hostName;

        return packageName;
    }

    public static String getReleventPackageNameForPIN() {
        String packageName = getReleventPackageName();
        packageName += ".Pin";
        return packageName;
    }

    public static final int MAC_KEY_INDEX = 5;

    public static void setMacKey(String mackey) {
        int retVal = -1;
        String packageName = getReleventPackageName();

        try {
            retVal = key.updateKeyEx(
                    Key.KEY_REQUEST_DEK,
                    Key.KEY_PROTECT_ZERO,
                    certData,
                    Utility.hexStr2Byte(mackey),
                    false,
                    0x00,
                    checkVal,
                    packageName,
                    MAC_KEY_INDEX
                    );
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static  void  setKeyForDUKPT(String keyToBeSetup) {
        int retVal = -1;
        String packageName = getReleventPackageName();

        try {
            retVal = key.updateKeyEx(
                    Key.KEY_REQUEST_IPEK,
                    Key.KEY_PROTECT_ZERO,
                    certData,
                    Utility.hexStr2Byte(keyToBeSetup),
                    false,
                    0x00,
                    checkVal,
                    packageName,
                    1
                    );

            String counter = TLEKeyGeneration.getCounter();
            Log.d("::::::::::counter", " : " + counter);
            byte[] ksnData = Utility.hexStr2Byte(counter);

            //inject initial ksn
            retVal = key.InjectIKSN(packageName,ksnData.length,ksnData);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void setRequiredKeys() {
        int ret = -1;
        byte[] keyBytes = Utility.hexStr2Byte(masterKey);

        try {
            ret = key.updateKeyEx(
                    Key.KEY_REQUEST_TMK,
                    Key.KEY_PROTECT_ZERO,
                    certData,
                    keyBytes,
                    true,
                    0x00,
                    checkVal, Base.getPinPackageName(),
                    1);


            keyBytes = Utility.hexStr2Byte(dataEncryptionKey);
            ret = key.updateKeyEx(
                    Key.KEY_REQUEST_DEK,
                    Key.KEY_PROTECT_TMK,
                    certData,
                    keyBytes,
                    false,
                    0x00,
                    checkVal,
                    Base.getPinPackageName(),
                    1);

            keyBytes = Utility.hexStr2Byte(pinEncryptionKey);
            ret = key.updateKeyWithAlgorithm(
                    Key.KEY_REQUEST_PEK,0x00,
                    Key.KEY_PROTECT_TMK,
                    certData,
                    keyBytes,
                    false,
                    0x00,
                    checkVal,
                     Base.getPinPackageName(),
                    1);

            //For CUP Key by Indeevari
            String masterKey = "";
            String terminalWorkingKey = "";
            if(SettingsInterpreter.isUAT()) {
                masterKey = "2323232323232323";
                terminalWorkingKey = "B2F76F8FCC995F56B2F76F8FCC995F56";
            }
            else {
                masterKey = "EA1A23016D769732";
                terminalWorkingKey = "6AEF14190132BBC06AEF14190132BBC0";
            }

            keyBytes =  DESCrypto.decryptDes(terminalWorkingKey,masterKey);
            ret = key.updateKeyWithAlgorithm(
                    Key.KEY_REQUEST_PEK,
                    0x00,
                    Key.KEY_PROTECT_ZERO,
                    certData,
                    keyBytes,
                    false,
                    0x00,
                    checkVal,
                    "SAMPATH",
                    1);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Log.d("PIN KEY", " : " + ret);
    }

    private static String keySet[] = new String[100];

    public static void  setKeyInSlot(int index,String key) {
        keySet[index] = key;
        writeKeyInSecFile(index,key);
    }

    //the code below will emulate a secure area but its not really a secured area
    static BufferedReader reader ;
    static BufferedWriter writer;

    //this is the part that initialize the secure file in the file system
    static File secKeyFile = null;
    private static final String SEC_FILE = "tleData.dat";

    public static int erase() {
        try {
            return key.erasePED();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return RspCode.ERROR;
    }

    public static void init() {
        path = Environment.getExternalStorageDirectory() + "/Download/";
        //read all the keys and store in the object
        if (secKeyFile == null)
            secKeyFile =  new File(path,SEC_FILE);

        try {
            //read up all the keys within the file
            if (reader == null)
                reader =  new BufferedReader(new FileReader(secKeyFile));

            String line = "";

            int index =  -1;
            String key = "";
            int offset = 0 ;

            while ((line = reader.readLine()) != null) {
                //tokenize and store in the array
                //get the index
                offset = line.indexOf("_");
                offset++;
                String sIndex = line.substring(offset,offset + 3);
                index = Integer.valueOf(sIndex);

                //get the key
                offset = line.indexOf("=");
                offset++;
                key = line.substring(offset);

                //store it
                keySet[index] = key;
            }

            reader.close();
            reader = null;
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
    }

    static String path = "";

    public static boolean writeKeyInSecFile(int index,String key) {
        try {
            File pathDirs =  new File(path);
            if (!pathDirs.exists())
                pathDirs.mkdirs();

            secKeyFile = new File(path,SEC_FILE);

            if (writer == null)
                writer = new BufferedWriter(new FileWriter(secKeyFile));

            //update the key array
            keySet[index] = key;
            String line = "";

            //write the entire array in to the file
            for (int i = 0; i < 100; i++) {
                if (keySet[i] == null)
                    continue;

                String sIndex = String.valueOf(i);
                sIndex = Formatter.fillInFront("0",sIndex,3);
                line = "KEY_" + sIndex + "=" + keySet[i] + "\n";
                writer.write(line);
            }
            writer.flush();
            writer.close();
            writer = null;
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public static String getKeyFromSlot(int index) {
        return keySet[index];
    }
}