package com.harshana.wposandroiposapp.Crypto;

import java.security.MessageDigest;

/**
 * Created by harshana_m on 12/27/2018.
 */

public class MAC
{
    public static byte[] makeSHA1ForPacket(String packet)
    {

        byte[] result = null;
        packet = packet.substring(14,packet.length() - 18);         //skip the tpdu
        try
        {
            MessageDigest crypt =  MessageDigest.getInstance("SHA1");
            crypt.reset();
            result = crypt.digest(packet.getBytes());

        }catch ( Exception ex)
        {
            ex.printStackTrace();
            return null;
        }

        return result;
    }

    public static byte[] makeSHA1(String packet)
    {

        byte[] result = null;
        try
        {
            MessageDigest crypt =  MessageDigest.getInstance("SHA1");
            crypt.reset();
            result = crypt.digest(packet.getBytes());

        }catch ( Exception ex)
        {
            ex.printStackTrace();
            return null;
        }

        return result;
    }
}
