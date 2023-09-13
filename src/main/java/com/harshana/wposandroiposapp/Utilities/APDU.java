package com.harshana.wposandroiposapp.Utilities;

/**
 * Created by harshana_m on 12/19/2018.
 */

public class APDU
{
    public static byte[] ContructAPDU(String command,String data)
    {
        String sLen =  "";

        if (data == null)
            sLen = "00";
        else
        {
            int len =  data.length();
            len /= 2;                   //actual byte length of the command
            sLen = Integer.toHexString(len);     //convert the value to hex
        }

        if (sLen.length()  == 1)
            sLen = "0" + sLen;

        if (data == null)
            data = "";

        //construct the command
        String apdu = command + sLen + data;
        return Utility.hexStr2Byte(apdu);
    }



}
