package com.harshana.wposandroiposapp.Packet;


import com.harshana.wposandroiposapp.Base.Transaction;
import com.harshana.wposandroiposapp.Utilities.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by harshana_m on 10/23/2018.
 */


class BitmapException extends Exception
{
    @Override
    public String getMessage()
    {
        super.getMessage();
        return "Parsing Error";
    }
}



public class Bitmap
{

    String map;

    OnSetOverrideFieldSettingsListener overrideFieldListener = null;


    public Bitmap (String bitmap)
    {
        map = bitmap;
    }

    public static List<Integer> getFieldList(String bitmapx) throws BitmapException
    {
        if (bitmapx.length() != 16) throw new BitmapException();

        List<Integer> fieldList  =  new ArrayList<>();
        byte [] bitmap = Utility.hexStr2Byte(bitmapx);

        for (int byteIndex = 7 ; byteIndex >= 0; byteIndex--)
        {
            byte curByte = bitmap[byteIndex];
            for (int bitIndex = 0; bitIndex < 8; bitIndex++)
            {
                if ((curByte & (1 << bitIndex)) == (1 << bitIndex))
                {
                    int index = bitIndex  + ( 8 * ( 7 - byteIndex));
                    index = 64 - index;
                    fieldList.add(index);
                }
            }
        }

        return fieldList;
    }

    public List<Integer>getFieldList()
    {
        List<Integer> lst = null;

        try
        {
            lst = getFieldList(map);
            return lst;

        }catch (Exception e)
        {
            e.printStackTrace();
        }

        return lst;
    }

    protected void setField(int fieldNumber)
    {
        //extract the chcaracter position
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
    }

    protected void resetField(int fieldNumber)
    {
        //extract the char position
        int pos = (64 - fieldNumber) / 8 + 1 ; // which byte from the left

        int beginIndex =  map.length() - (pos * 2);
        String header = map.substring(0,beginIndex);
        String exByte = map.substring(beginIndex ,beginIndex + 2 ) ;
        String trailer = map.substring(beginIndex + 2);

        int bitShift =  fieldNumber % 8 ;

        int value = Integer.valueOf(exByte,16);
        bitShift = 8 - bitShift;
        bitShift = 1 << bitShift;
        bitShift = ~bitShift;

        value = (value & bitShift);

        if (fieldNumber == 64)
        {
            value = Integer.valueOf(exByte);
            value = value & (~1);
        }

        exByte = Integer.toHexString(value);

        if (exByte.length() == 1)
            exByte = "0" + exByte;

        map = header + exByte + trailer;

    }



    protected boolean isSet(int fieldNumber)
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

        return (bitShift ==  (value & bitShift));

    }

    public interface OnSetOverrideFieldSettingsListener
    {
        void onSetOverrideFieldSettings(Transaction tran);
    }

    public void setOnSetOverrideFieldSettingsListener(OnSetOverrideFieldSettingsListener listener)
    {
        overrideFieldListener = listener;
    }

    public void OverrideFieldSettings(Transaction tran)
    {
        if (overrideFieldListener == null)
            return;

        overrideFieldListener.onSetOverrideFieldSettings(tran);
    }


}


