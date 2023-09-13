package com.harshana.wposandroiposapp.Utilities;



import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by harshana_m on 11/7/2018.
 */

public class Formatter {
    public static String removeDecimalPlace(String value) {
        if(!value.contains(".")) {
            return value;
        }
        int decimalIndex = value.indexOf(".");

        if (decimalIndex > 0) {
            //re arrange the amount to be converted
            String before = value.substring(0, decimalIndex);
            String after = value.substring(decimalIndex + 1);

            value = before + after;
        }
        return value;
    }

    public static String removeleadingZeros(String val) {
        int ind = 0;
        for (int i = 0; i < val.length(); i++) {
            char p = val.charAt(i);
            if (p != '0') {
                ind = i;
                break;
            }
        }
        val = val.substring(ind, val.length());
        return val;
    }

    private static final DecimalFormat df = new DecimalFormat("0.00");
    public static String formatAmount(long amount,String curSymbol) {
        String amt = Long.toString(amount);
        int len = amt.length();

        String front = "",back = "";

        if (len > 2) {
            back  = amt.substring(len - 2, len);
            front = amt.substring(0,len - 2);

            amt = front + "." + back;
        } else if ( len == 2) {
            amt = "0." + amt;
        }
        else {
            amt = "0.0" + amt;
        }

        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        decimalFormat.setGroupingUsed(true);
        decimalFormat.setGroupingSize(3);
        double damount = Double.parseDouble(amt);
        amt = decimalFormat.format(damount);

        amt = curSymbol + " " + amt;
        return amt;
    }

    public static String formatAmountUI(long amount) {
        String amt = Long.toString(amount);
        int len = amt.length();

        String front = "",back = "";

        if (len > 2) {
            back  = amt.substring(len - 2, len);
            front = amt.substring(0,len - 2);

            amt = front + "." + back;
        } else if ( len == 2) {
            amt = "0." + amt;
        }
        else {
            amt = "0.0" + amt;
        }

        DecimalFormat decimalFormat = new DecimalFormat("0.00");
        decimalFormat.setGroupingUsed(true);
        decimalFormat.setGroupingSize(3);
        double damount = Double.parseDouble(amt);
        amt = decimalFormat.format(damount);

        return amt;
    }

    public static String formatAmount(long amount) {
        String amt = Long.toString(amount);
        int len = amt.length();

        String front = "",back = "";

        if (len > 2) {
            back  = amt.substring(len - 2, len);
            front = amt.substring(0,len - 2);

            amt = front + "." + back;
        } else if ( len == 2) {
            amt = "0." + amt;
        }
        else {
            amt = "0.0" + amt;
        }

        /*DecimalFormat decimalFormat = new DecimalFormat("0.00");
        decimalFormat.setGroupingUsed(true);
        decimalFormat.setGroupingSize(3);
        double damount = Double.parseDouble(amt);
        amt = decimalFormat.format(damount);*/

        return amt;
    }

    public static String maskPan(String pan,String pattern,char maskChar) {
        String formatted = "";
        char toAdd = 0;

        for (int i = 0; i < pan.length(); i++) {
            if (i < pattern.length() && pattern.charAt(i) == maskChar)
                toAdd = maskChar;
            else
                toAdd = pan.charAt(i);

            formatted += toAdd;

            if (i >= pan.length())
                break;
        }

        return formatted;
    }


    public static String formatForSixDigits(int invoiceNumber)
    {
        String inv = Integer.toString(invoiceNumber);
        int padLen = 6 - inv.length();
        String front = "";

        for (int i = 0 ; i < padLen; i++)
            front += "0";

        return front + inv;
    }

    public static String getCurrentTimeFormatted()
    {
        SimpleDateFormat dt = new SimpleDateFormat("HHmmss");
        String currentTime = dt.format(new Date());
        return currentTime;
    }

    public static String getCurrentDateFormatted()
    {
        SimpleDateFormat dt = new SimpleDateFormat("yyMMdd");
        String currentDate = dt.format(new Date());
        currentDate = currentDate.substring(2);
        return currentDate;
    }

    public static String getCurrentDate()
    {
        SimpleDateFormat dt = new SimpleDateFormat("yyMMdd");
        return dt.format(new Date());
    }

    public static String getCountFormattedForSettlement(int count)
    {
        String strCount = Integer.toString(count);
        String pad = "";

        int lenGap = 3 - strCount.length();
        for (int i = 0 ; i < lenGap; i++)
            pad += "0";

        return pad + strCount;

    }

    public static String getAmountFormattedForSettlement(int amount)
    {
        String strAmount = Integer.toString(amount);
        String pad = "";

        int lenGap = 12 - strAmount.length();
        for (int i = 0 ; i < lenGap; i++)
            pad += "0";

        return pad + strAmount;
    }


    public static String fillInBack(String c, String data , int expecteLen)
    {
        int fillLen = expecteLen - data.length();
        String filler = "";

        for (int i = 0; i < fillLen; i++)
            filler += c;

        return data +filler;
    }

    public static String fillInFront(String c, String data , int expecteLen)
    {
        int fillLen = expecteLen - data.length();
        String filler = "";

        for (int i = 0; i < fillLen; i++)
            filler += c;

        return filler + data ;
    }

    public static String fillInFrontFixed(String c,String data, int frontFillCount)
    {
        String filler = "";
        for (int i = 0 ; i < frontFillCount;  i++)
            filler += c;

        return filler + data;
    }


    public static String replaceNII(String tpdu,String newNII)
    {
        String start = tpdu.substring(0,3);
        String last = tpdu.substring(tpdu.length() - 4);

        return (start + newNII + last);
    }

    public static String filleBothEnds(String c, String data,int expectedTotalLength)
    {
        if (data.length() <= expectedTotalLength )
            return data;

        int frontAndBackFillCount =  (expectedTotalLength - data.length()) / 2;

        String dummy = "";
        for (int i = 0 ; i < frontAndBackFillCount; i++)
            dummy += c;

        data  = dummy + data + dummy;

        return data;
    }

}
