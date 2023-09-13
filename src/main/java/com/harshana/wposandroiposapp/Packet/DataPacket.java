package com.harshana.wposandroiposapp.Packet;



import com.harshana.wposandroiposapp.Base.Transaction;
import com.harshana.wposandroiposapp.Utilities.Formatter;
import com.harshana.wposandroiposapp.Utilities.Utility;

import java.util.HashMap;
import java.util.List;
import java.util.Map;





public class DataPacket
{
    Attrib fieldAtributes[];

    Transaction curTransaction;
    Map<Integer, String> data8583 ;
    OnPacketLoadListener packetLoadListener = null;
    String callbackResult ;



    public DataPacket(Transaction transaction)
    {
        curTransaction = transaction;
        data8583 =  new HashMap<Integer, String>();
    }

    enum AttribFormat
    {
        ALPHA_NEUMERIC,
        OTHERS
    }

    enum LenType
    {
        FIXED,
        VARIABLE
    }

    class Attrib
    {
        int id;
        int length;
        AttribFormat format;
        LenType lenType;


        public Attrib(int i, int l,AttribFormat f,LenType ml)
        {
            id = i;
            length = l;
            format = f;
            lenType = ml;
        }
    }


    private String tpdu;

    public void setPacketTPDU(String packetTpdu)
    {
        tpdu = packetTpdu;
    }


    public void setOnPacketLoadListener(OnPacketLoadListener listener)
    {
        packetLoadListener = listener;
    }


    String invokeCallbackFunction(int id)
    {
        String result = null;


        if (id == 2)  result = packetLoadListener.loadFiled_02_Data(curTransaction);
        if (id == 3)  result = packetLoadListener.loadFiled_03_Data(curTransaction);
        if (id == 4)  result = packetLoadListener.loadFiled_04_Data(curTransaction);
        if (id == 5)  result = packetLoadListener.loadFiled_05_Data(curTransaction);
        if (id == 6)  result = packetLoadListener.loadFiled_06_Data(curTransaction);
        if (id == 7)  result = packetLoadListener.loadFiled_07_Data(curTransaction);
        if (id == 8)  result = packetLoadListener.loadFiled_08_Data(curTransaction);
        if (id == 9)  result = packetLoadListener.loadFiled_09_Data(curTransaction);
        if (id == 10)  result = packetLoadListener.loadFiled_10_Data(curTransaction);
        if (id == 11)  result = packetLoadListener.loadFiled_11_Data(curTransaction);
        if (id == 12)  result = packetLoadListener.loadFiled_12_Data(curTransaction);
        if (id == 13)  result = packetLoadListener.loadFiled_13_Data(curTransaction);
        if (id == 14)  result = packetLoadListener.loadFiled_14_Data(curTransaction);
        if (id == 15)  result = packetLoadListener.loadFiled_15_Data(curTransaction);
        if (id == 16)  result = packetLoadListener.loadFiled_16_Data(curTransaction);
        if (id == 17)  result = packetLoadListener.loadFiled_17_Data(curTransaction);
        if (id == 18)  result = packetLoadListener.loadFiled_18_Data(curTransaction);
        if (id == 19)  result = packetLoadListener.loadFiled_19_Data(curTransaction);
        if (id == 20)  result = packetLoadListener.loadFiled_20_Data(curTransaction);
        if (id == 21)  result = packetLoadListener.loadFiled_21_Data(curTransaction);
        if (id == 22)  result = packetLoadListener.loadFiled_22_Data(curTransaction);
        if (id == 23)  result = packetLoadListener.loadFiled_23_Data(curTransaction);
        if (id == 24)  result = packetLoadListener.loadFiled_24_Data(curTransaction);
        if (id == 25)  result = packetLoadListener.loadFiled_25_Data(curTransaction);
        if (id == 26)  result = packetLoadListener.loadFiled_26_Data(curTransaction);
        if (id == 27)  result = packetLoadListener.loadFiled_27_Data(curTransaction);
        if (id == 28)  result = packetLoadListener.loadFiled_28_Data(curTransaction);
        if (id == 29)  result = packetLoadListener.loadFiled_29_Data(curTransaction);
        if (id == 30)  result = packetLoadListener.loadFiled_30_Data(curTransaction);
        if (id == 31)  result = packetLoadListener.loadFiled_31_Data(curTransaction);
        if (id == 32)  result = packetLoadListener.loadFiled_32_Data(curTransaction);
        if (id == 33)  result = packetLoadListener.loadFiled_33_Data(curTransaction);
        if (id == 34)  result = packetLoadListener.loadFiled_34_Data(curTransaction);
        if (id == 35)  result = packetLoadListener.loadFiled_35_Data(curTransaction);
        if (id == 36)  result = packetLoadListener.loadFiled_36_Data(curTransaction);
        if (id == 37)  result = packetLoadListener.loadFiled_37_Data(curTransaction);
        if (id == 38)  result = packetLoadListener.loadFiled_38_Data(curTransaction);
        if (id == 39)  result = packetLoadListener.loadFiled_39_Data(curTransaction);
        if (id == 40)  result = packetLoadListener.loadFiled_40_Data(curTransaction);
        if (id == 41)  result = packetLoadListener.loadFiled_41_Data(curTransaction);
        if (id == 42)  result = packetLoadListener.loadFiled_42_Data(curTransaction);
        if (id == 43)  result = packetLoadListener.loadFiled_43_Data(curTransaction);
        if (id == 44)  result = packetLoadListener.loadFiled_44_Data(curTransaction);
        if (id == 45)  result = packetLoadListener.loadFiled_45_Data(curTransaction);
        if (id == 46)  result = packetLoadListener.loadFiled_46_Data(curTransaction);
        if (id == 47)  result = packetLoadListener.loadFiled_47_Data(curTransaction);
        if (id == 48)  result = packetLoadListener.loadFiled_48_Data(curTransaction);
        if (id == 49)  result = packetLoadListener.loadFiled_49_Data(curTransaction);
        if (id == 50)  result = packetLoadListener.loadFiled_50_Data(curTransaction);
        if (id == 51)  result = packetLoadListener.loadFiled_51_Data(curTransaction);
        if (id == 52)  result = packetLoadListener.loadFiled_52_Data(curTransaction);
        if (id == 53)  result = packetLoadListener.loadFiled_53_Data(curTransaction);
        if (id == 54)  result = packetLoadListener.loadFiled_54_Data(curTransaction);
        if (id == 55)  result = packetLoadListener.loadFiled_55_Data(curTransaction);
        if (id == 56)  result = packetLoadListener.loadFiled_56_Data(curTransaction);
        if (id == 57)  result = packetLoadListener.loadFiled_57_Data(curTransaction);
        if (id == 58)  result = packetLoadListener.loadFiled_58_Data(curTransaction);
        if (id == 59)  result = packetLoadListener.loadFiled_59_Data(curTransaction);
        if (id == 60)  result = packetLoadListener.loadFiled_60_Data(curTransaction);
        if (id == 61)  result = packetLoadListener.loadFiled_61_Data(curTransaction);
        if (id == 62)  result = packetLoadListener.loadFiled_62_Data(curTransaction);
        if (id == 63)  result = packetLoadListener.loadFiled_63_Data(curTransaction);
        if (id == 64)  result = packetLoadListener.loadFiled_64_Data(curTransaction);

        return result;
    }


    public  String padZerosInfront(String data, int numZeros)
    {
        String s = "";

        for (int i  = 0 ; i < numZeros; i++)
        {
            s += "0";
        }

        return s + data;
    }

    public void loadPacketData(List<Integer> fieldList) throws InvalidData
    {

        if (packetLoadListener == null)
            return;

        callbackResult = packetLoadListener.loadFiled_00_Data(curTransaction);
        data8583.put(0,callbackResult);   //set the data in actual packet


        for(int fieldIndex : fieldList)
        {
            //do not call for field 55 loader
            if (fieldIndex == 55)
                if (curTransaction.inChipStatus == Transaction.ChipStatusTypes.NOT_USING_CHIP)
                    continue;


            callbackResult = invokeCallbackFunction(fieldIndex);

            if (callbackResult != null)
            {
                if (fieldIndex == 4)
                {
                    if (callbackResult.length() < 12)
                    {
                        int lenToBePadded = 12 - callbackResult.length();
                        callbackResult = padZerosInfront(callbackResult,lenToBePadded);
                    }
                }

                data8583.put(fieldIndex,callbackResult);   //set the data in actual packet
            }
        }

    }

    public interface OnPacketLoadListener
    {
        String loadFiled_00_Data(Transaction tran) ;
        // String loadFiled_01_Data(Transaction tran) ;
        String loadFiled_02_Data(Transaction tran) ;
        String loadFiled_03_Data(Transaction tran) ;
        String loadFiled_04_Data(Transaction tran) ;
        String loadFiled_05_Data(Transaction tran) ;
        String loadFiled_06_Data(Transaction tran) ;
        String loadFiled_07_Data(Transaction tran) ;
        String loadFiled_08_Data(Transaction tran) ;
        String loadFiled_09_Data(Transaction tran) ;
        String loadFiled_10_Data(Transaction tran) ;
        String loadFiled_11_Data(Transaction tran) ;
        String loadFiled_12_Data(Transaction tran) ;
        String loadFiled_13_Data(Transaction tran) ;
        String loadFiled_14_Data(Transaction tran) ;
        String loadFiled_15_Data(Transaction tran) ;
        String loadFiled_16_Data(Transaction tran) ;
        String loadFiled_17_Data(Transaction tran) ;
        String loadFiled_18_Data(Transaction tran) ;
        String loadFiled_19_Data(Transaction tran) ;
        String loadFiled_20_Data(Transaction tran) ;
        String loadFiled_21_Data(Transaction tran) ;
        String loadFiled_22_Data(Transaction tran) ;
        String loadFiled_23_Data(Transaction tran) ;
        String loadFiled_24_Data(Transaction tran) ;
        String loadFiled_25_Data(Transaction tran) ;
        String loadFiled_26_Data(Transaction tran) ;
        String loadFiled_27_Data(Transaction tran) ;
        String loadFiled_28_Data(Transaction tran) ;
        String loadFiled_29_Data(Transaction tran) ;
        String loadFiled_30_Data(Transaction tran) ;
        String loadFiled_31_Data(Transaction tran) ;
        String loadFiled_32_Data(Transaction tran) ;
        String loadFiled_33_Data(Transaction tran) ;
        String loadFiled_34_Data(Transaction tran) ;
        String loadFiled_35_Data(Transaction tran) ;
        String loadFiled_36_Data(Transaction tran) ;
        String loadFiled_37_Data(Transaction tran) ;
        String loadFiled_38_Data(Transaction tran) ;
        String loadFiled_39_Data(Transaction tran) ;
        String loadFiled_40_Data(Transaction tran) ;
        String loadFiled_41_Data(Transaction tran) ;
        String loadFiled_42_Data(Transaction tran) ;
        String loadFiled_43_Data(Transaction tran) ;
        String loadFiled_44_Data(Transaction tran) ;
        String loadFiled_45_Data(Transaction tran) ;
        String loadFiled_46_Data(Transaction tran) ;
        String loadFiled_47_Data(Transaction tran) ;
        String loadFiled_48_Data(Transaction tran) ;
        String loadFiled_49_Data(Transaction tran) ;
        String loadFiled_50_Data(Transaction tran) ;
        String loadFiled_51_Data(Transaction tran) ;
        String loadFiled_52_Data(Transaction tran) ;
        String loadFiled_53_Data(Transaction tran) ;
        String loadFiled_54_Data(Transaction tran) ;
        String loadFiled_55_Data(Transaction tran) ;
        String loadFiled_56_Data(Transaction tran) ;
        String loadFiled_57_Data(Transaction tran) ;
        String loadFiled_58_Data(Transaction tran) ;
        String loadFiled_59_Data(Transaction tran) ;
        String loadFiled_60_Data(Transaction tran) ;
        String loadFiled_61_Data(Transaction tran) ;
        String loadFiled_62_Data(Transaction tran) ;
        String loadFiled_63_Data(Transaction tran) ;
        String loadFiled_64_Data(Transaction tran) ;

    }



    //custom exception to be thrown
    public class InvalidData extends  Exception
    {
        int fieldID;
        String cause;

        InvalidData(int field,String msg)
        {
            fieldID = field;
            cause = msg;
        }
        @Override
        public String getMessage()
        {
            return cause + " Field : " + fieldID;

        }
    }


    //get the row packet
    public byte[] getRawDataPacket()
    {
        /*
        ISO8583u iso8583u = new ISO8583u();
        iso8583u.setHeader("6000110000");
        byte[] packet =  iso8583u.makePacket( data8583, ISO8583.PACKET_TYPE.PACKET_TYPE_HEXLEN_BUF );
        String rawHexPacket = Utility.byte2HexStr(packet);

        String packetSize = rawHexPacket.substring(0,4);
        String data = rawHexPacket.substring(14,rawHexPacket.length());
        rawHexPacket = packetSize + tpdu + data;
        packet = Utility.hexStr2Byte(rawHexPacket);
        */
        return getRawDataPacketWithoutMac();
    }

    public byte[] getRawDataPacketWithMac()
    {
        ISO8583u iso8583u = new ISO8583u();
        iso8583u.setHeader("6000110000");
        byte[] packet =  iso8583u.makePacket( data8583, ISO8583.PACKET_TYPE.PACKET_TYPE_HEXLEN_BUF );
        String rawHexPacket = Utility.byte2HexStr(packet);

        String packetSize = rawHexPacket.substring(0,4);
        String data = rawHexPacket.substring(14);
        rawHexPacket = packetSize + tpdu + data;
        packet = Utility.hexStr2Byte(rawHexPacket);

        return packet;
    }

    public  byte[] getRawDataPacketWithMap(Map<Integer,String> mm)
    {
        ISO8583u iso8583u = new ISO8583u();
        iso8583u.setHeader("6000110000");
        data8583 = mm;
        byte[] packet =  iso8583u.makePacket( data8583, ISO8583.PACKET_TYPE.PACKET_TYPE_HEXLEN_BUF );
        String rawHexPacket = Utility.byte2HexStr(packet);


        String packetSize = rawHexPacket.substring(0,4);
        String data = rawHexPacket.substring(14);
        rawHexPacket = packetSize + "6000110000" + data;
        packet = Utility.hexStr2Byte(rawHexPacket);

        return packet;
    }

    public byte[] getRawDataPacketWithoutMac()
    {
        ISO8583u iso8583u = new ISO8583u();
        iso8583u.setHeader("6000110000");
        byte[] packet =  iso8583u.makePacket( data8583, ISO8583.PACKET_TYPE.PACKET_TYPE_HEXLEN_BUF );
        String rawHexPacket = Utility.byte2HexStr(packet);


        if (!iso8583u.unpack(packet))
            return null;

        String packetSize = rawHexPacket.substring(0,4);

        if(iso8583u.unpackValidField[64]) //there is valid 64 content so we do not strip it
        {
            String data = rawHexPacket.substring(14);
            rawHexPacket = packetSize + tpdu + data;
            packet = Utility.hexStr2Byte(rawHexPacket);
            return packet;
        }

        //recalculate the size of the packet without the mac
        int sizeWithoutMac = Integer.parseInt(packetSize,16);
        sizeWithoutMac -= 9; //remove the mac from the packet

        packetSize = Integer.toHexString(sizeWithoutMac);
        packetSize = Formatter.fillInFront("0",packetSize,4);
        String data = rawHexPacket.substring(14,rawHexPacket.length() - 18);
        rawHexPacket = packetSize + tpdu + data;
        packet = Utility.hexStr2Byte(rawHexPacket);

        return packet;
    }
}

