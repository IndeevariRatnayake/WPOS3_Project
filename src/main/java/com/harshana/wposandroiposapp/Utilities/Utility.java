package com.harshana.wposandroiposapp.Utilities;

import android.content.Context;
import android.content.res.AssetManager;

import com.harshana.wposandroiposapp.Base.Services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by harshana_m on 10/19/2018.
 */

public class Utility {
    public static String byte2HexStr(byte[] var0, int offset, int length ) {

        int var3 = 0;
        try {
            if (var0 == null) {
                return "";
            } else {
                String var1 = "";
                StringBuilder var2 = new StringBuilder();

                for (var3 = offset; var3 < (offset+length); ++var3) {
                    var1 = Integer.toHexString(var0[var3] & 255);
                    var2.append(var1.length() == 1 ? "0" + var1 : var1);
                }

                return var2.toString().toUpperCase().trim();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public static String bcd2Ascii(byte[] bcd) {
        if (bcd == null) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder(bcd.length << 1);
            byte[] var2 = bcd;
            int var3 = bcd.length;

            for(int var4 = 0; var4 < var3; ++var4) {
                byte ch = var2[var4];
                byte half = (byte)(ch >> 4);
                sb.append((char)(half + (half > 9 ? 55 : 48)));
                half = (byte)(ch & 15);
                sb.append((char)(half + (half > 9 ? 55 : 48)));
            }

            return sb.toString();
        }
    }

    public static String byte2HexStr(byte[] var0) {
        if (var0 == null) {
            return "";
        } else {
            String var1 = "";
            StringBuilder var2 = new StringBuilder();

            for (int var3 = 0; var3 < var0.length; ++var3) {
                var1 = Integer.toHexString(var0[var3] & 255);
                var2.append(var1.length() == 1 ? "0" + var1 : var1);
            }

            return var2.toString().toUpperCase().trim();
        }
    }

    public static byte[] hexStr2Byte(String hexString) {
        if (hexString == null || hexString.length() == 0 ) {
            return new byte[] {0};
        }
        String hexStrTrimed = hexString.replace(" ", "");
        {
            String hexStr = hexStrTrimed;
            int len = hexStrTrimed.length();
            if( (len % 2 ) == 1 ){
                hexStr = hexStrTrimed + "0";
                ++len;
            }
            char highChar, lowChar;
            int  high, low;
            byte result [] = new byte[len/2];
            for( int i=0; i< hexStr.length(); i++ ) {
                highChar = hexStr.charAt(i);
                lowChar = hexStr.charAt(i+1);
                high = CHAR2INT(highChar);
                low = CHAR2INT(lowChar);
                result[i/2] = (byte) (high*16+low);
                i++;
            }
            return  result;
        }
    }

    public static byte[] hexStr2Byte(String hexString, int beginIndex, int length ) {
        if (hexString == null || hexString.length() == 0 ) {
            return new byte[] {0};
        }
        {
            if( length > hexString.length() ){
                length = hexString.length();
            }
            String hexStr = hexString;
            int len = length;
            if( (len % 2 ) == 1 ){
                hexStr = hexString + "0";
                ++len;
            }
            byte result [] = new byte[len/2];
            String s;
            for( int i=beginIndex; i< len; i++ ) {
                s = hexStr.substring(i,i+2);
                int v = Integer.parseInt(s, 16);

                result[i/2] = (byte) v;
                i++;
            }
            return  result;
        }
    }

    public static byte HEX2DEC( int hex ){
        return (byte)((hex/10)*16+hex%10);
    }

    public static int DEC2INT( byte dec ){
        int high = ((0x007F & dec) >> 4);
        if( 0!= (0x0080&dec) ) {
            high += 8;
        }
        return (high)*10+(dec&0x0F) ;
    }

    public static int CHAR2INT(char c) {
        if(
                (c >= '0' && c <= '9' )
                        || (c == '=' )
        ) {
            return c - '0';
        } else if(c >= 'a' && c <= 'f'){
            return c - 'a' +10;
        } else if(c >= 'A' && c <= 'F'){
            return c - 'A' +10;
        } else {
            return 0;
        }
    }

    public static String forceFetchTagValue(String currentEmv55Data,String tag) {
        byte[] outData;
        int []outDataLen = new int[3];
        String data = Utility.getValueofTag(currentEmv55Data,tag);
        if (data == null) {
            int result = 0;
            outData = new byte[12];

            int iTag =  0;
            try {
                iTag = Integer.valueOf(tag,16);
                result = Services.emvCore.getTLV(iTag,outData,outDataLen);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            if (result == 0) {
               String value = Utility.byte2HexStr(outData,0,outDataLen[0]);
               return value;
            }
        }

        return null;
    }

    public static  String getValueofTag(String TLVString,String tag) {
        String data = "";

        if (TLVString == null || tag == null)
            return null;

        int index = TLVString.indexOf(tag);
        if (index > 0) {
            //found the tag
            //get the next byte for the length
            String tagLenString = TLVString.substring(index + tag.length(),index + tag.length() + 2);
            int inLen = Integer.valueOf(tagLenString);
            int index_to_data = index + tag.length() + tagLenString.length();

            //extract the data portion from the tlv
            data = TLVString.substring(index_to_data,index_to_data + (inLen * 2));
            return data;
        }

        return null;
    }

    public static String getCurrentTime() {
        SimpleDateFormat dt = new SimpleDateFormat("HH/mm/ss");
        String currentTime = dt.format(new Date());
        return currentTime;
    }

    public static String getTimeFormatted(String rawTime) {
        rawTime = rawTime.substring(0,2) + ":" + rawTime.substring(2,4) + ":" + rawTime.substring(4,6);
        return rawTime;
    }

    public static String getDateFormatted(String rawDate) {
        rawDate = rawDate.substring(0,2) + "/" +  rawDate.substring(2,4);
        return rawDate;
    }

    public static String getDateFormattedRev(String rawDate) {
        SimpleDateFormat dt = new SimpleDateFormat("yy");
        String currentTime = dt.format(new Date());
        rawDate = rawDate.substring(2,4) + "/" +  rawDate.substring(0,2) + "/" + currentTime;
        return rawDate;
    }

    public static String getCurrentDate() {
        SimpleDateFormat dt = new SimpleDateFormat("dd/MM/yy");
        String currentDate = dt.format(new Date());
        currentDate = currentDate.substring(0,currentDate.length() - 3);
        return currentDate;
    }

    public static String getCurrentDateReceipt() {
        SimpleDateFormat dt = new SimpleDateFormat("dd/MM/yy");
        String currentDate = dt.format(new Date());
        return currentDate;
    }

    public static String asciiToString(String asciiString) {
        int start = 0 ;
        int end = 2;

        String char_value = "";
        String str = "";

        while( end <= asciiString.length()) {
            char_value = asciiString.substring(start,end);
            int val = Integer.valueOf(char_value,16);
            str += Character.toString((char)val);
            start = end;
            end += 2;
        }

        return str;
    }

    public static String StringToAscii(String str) {
        String asciiString = null;

        for (int i= 0 ; i < str.length(); i++) {
            int val = str.charAt(i);
            String asciiVal = String.valueOf(val);
            asciiString += asciiVal;
        }

        return asciiString;
    }

    public static byte[] xorArrays(byte[] a, byte[] b) {
        byte[] xor = new byte[a.length];

        for (int i = 0; i < a.length; i++) {
            xor[i] = (byte) (a[i] ^ b[i]);
        }

        return xor;
    }

    public static String convertHexToBinary(String hex) {
        StringBuilder binStrBuilder = new StringBuilder();
        int c = 1;
        for (int i = 0; i < hex.length() - 1; i += 2) {

            String output = hex.substring(i, (i + 2));
            int decimal = Integer.parseInt(output, 16);

            String binStr = Integer.toBinaryString(decimal);
            int len = binStr.length();
            StringBuilder sbf = new StringBuilder();

            if (len < 8) {

                for (int k = 0; k < (8 - len); k++) {
                    sbf.append("0");
                }
                sbf.append(binStr);
            } else {
                sbf.append(binStr);
            }

            c++;
            binStrBuilder.append(sbf.toString());
        }

        return binStrBuilder.toString();
    }


    public static String constructTLV(int tag,byte[] data,int len)
    {
        String tagName = Integer.toHexString(tag);
        tagName = tagName.toUpperCase();

        String sLen = Integer.toHexString(len);
        sLen = Formatter.fillInFront("0",sLen,2);

        byte[] d = Arrays.copyOf(data,len);
        String converted = byte2HexStr(d);

        String tlv = tagName + sLen + converted;
        return tlv;
    }


    public static boolean isWithinBinRange(String pan,String lBound,String hBound)
    {
        int intLBound = Integer.valueOf(lBound);
        int intHBound  = Integer.valueOf(hBound);

        pan = pan.substring(0,6);
        int iPan = Integer.valueOf(pan);

        return (iPan >= intLBound && iPan < intHBound);
    }


    public static boolean isWithinDate(String startDate,String endDate)
    {
        Date dateStart;
        Date dateEnd;

        String tranDate = Formatter.getCurrentDateFormatted();
        try
        {
            dateStart =  new SimpleDateFormat("MMdd").parse(startDate);
            dateEnd = new SimpleDateFormat("MMdd").parse(endDate);

        }catch (Exception ex)
        {
            return false;
        }


        //reverse the dates if necessary
        if (dateStart.after(dateEnd))
        {
            Date temp = dateStart;
            dateStart  = dateEnd;
            dateEnd = temp;
        }


        Date txnDate  = null;
        try
        {
            txnDate = new SimpleDateFormat("MMdd").parse(tranDate);
        }catch (Exception ex){}


        if (txnDate.getTime() != dateStart.getTime())
        {
            if (txnDate.getTime() != dateEnd.getTime())
            {
                if (txnDate.before(dateStart) || txnDate.after(dateEnd)) //out of the range so we ignore
                    return false;

            }
        }

        return true;

    }

    public static boolean unzipFromAsset(Context context,String zipFileName,String targetLocation)
    {
        try
        {
            InputStream fileInputStream = null;

            AssetManager assetManager = context.getAssets();
            fileInputStream = assetManager.open(zipFileName);

            ZipInputStream zipInputStream =  new ZipInputStream(fileInputStream);
            ZipEntry zipEntry  = null;
            FileOutputStream out = null;

            while ((zipEntry = zipInputStream.getNextEntry()) != null)
            {
                if (zipEntry.isDirectory())
                    ;
                else
                {
                    String name[] = zipEntry.getName().split(File.separator);
                    String fileName = name[name.length - 1];

                    out = new FileOutputStream(targetLocation + File.separator + fileName);
                    copyFile(zipInputStream,out);
                }
            }



            if (zipInputStream != null)
                zipInputStream.close();

            if (out != null)
            {
                out.flush();
                out.close();
                out = null;

            }

        }catch (Exception ex)
        {
            ex.printStackTrace();
            return false;
        }



        return true;
    }


    private static void copyFile(InputStream in, OutputStream out) throws IOException
    {
       byte[] buffer = new byte[1024];
       int readbytes = 0 ;

       while ( (readbytes = in.read(buffer )) > 0)
           out.write(buffer,0,readbytes);

    }


    public static boolean copyFile(String fromPath,String toPath)
    {
        byte [] buffer =  new byte[1024];
        try
        {
            FileInputStream in = new FileInputStream(fromPath);
            FileOutputStream out =  new FileOutputStream(toPath);

            int readBytes = 0 ;
            while ((readBytes = in.read(buffer)) > 0)
                out.write(buffer,0,readBytes);

            if(in != null)
            {
                in.close();
                in = null;
            }

            if (out != null)
            {
                out.flush();
                out.close();
                out = null;
            }

        }catch (Exception ex)
        {
            ex.printStackTrace();
            return false;
        }

        return true;
    }


}
