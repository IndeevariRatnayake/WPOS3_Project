package com.harshana.wposandroiposapp.DevArea;


import android.util.Log;

import com.harshana.wposandroiposapp.Base.Base;
import com.harshana.wposandroiposapp.Base.IssuerHostMap;
import com.harshana.wposandroiposapp.Base.Transaction;
import com.harshana.wposandroiposapp.Packet.DataPacket;
import com.harshana.wposandroiposapp.Settings.SettingsInterpreter;
import com.harshana.wposandroiposapp.TLE.TLE;
import com.harshana.wposandroiposapp.TLE.TLEKeyDownload;
import com.harshana.wposandroiposapp.Utilities.Formatter;
import com.harshana.wposandroiposapp.Utilities.Utility;


/**
 * Created by harshana_m on 10/25/2018.
 */

public class PacketDev extends DataPacket implements DataPacket.OnPacketLoadListener {
    public PacketDev(Transaction tran){super(tran);}

    //load the mti of the transaction
    @Override
    public String loadFiled_00_Data(Transaction tran) {
        String data = tran.MTI;
        return data;
    }

    public static int field2OrigLength = 0 ;

    @Override
    public String loadFiled_02_Data(Transaction tran) {
        String data =  tran.PAN;
        if (data != null && SettingsInterpreter.isTLEEnabled())
            field2OrigLength = data.length();

        return data;
    }

    @Override
    public String loadFiled_03_Data(Transaction tran) {
        String data = tran.procCode;
        return data;
    }

    //This is the amount field contains the transactions authorized amount
    @Override
    public String loadFiled_04_Data(Transaction tran) {
        String data =  Long.toString(tran.lnBaseTransactionAmount);
        return data;
    }

    @Override
    public String loadFiled_05_Data(Transaction tran)
    {
        return null;
    }

    @Override
    public String loadFiled_06_Data(Transaction tran)
    {
        return null;
    }

    @Override
    public String loadFiled_07_Data(Transaction tran)
    {
        return null;
    }

    @Override
    public String loadFiled_08_Data(Transaction tran)
    {
        return null;
    }

    @Override
    public String loadFiled_09_Data(Transaction tran)
    {
        return null;
    }

    @Override
    public String loadFiled_10_Data(Transaction tran)
    {
        return null;
    }

    @Override
    public String loadFiled_11_Data(Transaction tran) {
        String data  = Formatter.formatForSixDigits(tran.traceNumber);
        return data;
    }

    @Override
    public String loadFiled_12_Data(Transaction tran)
    {
        //get the terminal time
        String data = "";
        if (    (tran.origTransactionCode == TranStaticData.TranTypes.VOID) ||
                (tran.origTransactionCode == TranStaticData.TranTypes.BATCH_UPLOAD) ||
                (tran.origTransactionCode == TranStaticData.TranTypes.SETTLE))
            data = tran.Time;
        else
            data =  Formatter.getCurrentTimeFormatted();

        return data;
    }

    @Override
    public String loadFiled_13_Data(Transaction tran)
    {
        String data = "";
        if ((tran.origTransactionCode == TranStaticData.TranTypes.VOID) ||
                (tran.origTransactionCode == TranStaticData.TranTypes.BATCH_UPLOAD) ||
                (tran.origTransactionCode == TranStaticData.TranTypes.SETTLE))
            data = tran.Date;
        else
            data = Formatter.getCurrentDateFormatted();

        return data;
    }

    @Override
    public String loadFiled_14_Data(Transaction tran)
    {
        String data   = tran.expDate;
        return data;
    }

    @Override
    public String loadFiled_15_Data(Transaction tran)
    {
        return null;
    }

    @Override
    public String loadFiled_16_Data(Transaction tran)
    {
        return null;
    }

    @Override
    public String loadFiled_17_Data(Transaction tran)
    {
        return null;
    }

    @Override
    public String loadFiled_18_Data(Transaction tran)
    {
        return null;
    }

    @Override
    public String loadFiled_19_Data(Transaction tran)
    {
        return null;
    }

    @Override
    public String loadFiled_20_Data(Transaction tran)
    {
        return null;
    }

    @Override
    public String loadFiled_21_Data(Transaction tran)
    {
        return null;
    }


    private String getPosEntryMode(Transaction tran)
    {
        String panEntry = "";
        String pinStatus = "";

        if ( tran.getCardLabel() == null)
            return null;
        int mode  = tran.inChipStatus;
        if (mode == Transaction.ChipStatusTypes.MANUAL_KEY_IN)
            panEntry = "01";
        else if (mode == Transaction.ChipStatusTypes.NOT_USING_CHIP && tran.isFallbackTransaction)
            panEntry = "80";
        else if (mode == Transaction.ChipStatusTypes.NOT_USING_CHIP)
            panEntry = "02";
        else if(mode == Transaction.ChipStatusTypes.EMV_CARD)
            panEntry = "05";
        else if (mode == Transaction.ChipStatusTypes.CTLS_CARD)
            panEntry = "07";
        else
            panEntry = "00";

        if (tran.encryptedPINBlock == null)
            pinStatus = "2";
        else
            pinStatus = "1";

        return "0" + panEntry + pinStatus;
    }

    @Override
    public String loadFiled_22_Data(Transaction tran)
    {
        String data = getPosEntryMode(tran);
        return data;
    }

    final static String PAN_SEQUNECE_NUMBER = "5F34";
    @Override
    public String loadFiled_23_Data(Transaction tran) {
        String data = Utility.getValueofTag(tran.emv55Data,PAN_SEQUNECE_NUMBER);
        //adjust for the coding size
        if ((data != null) && (data.length()  < 4))
            data = "00" + data;

        return data;
    }

    @Override
    public String loadFiled_24_Data(Transaction tran) {
        String data = "";

        if (tran.inTransactionCode == TranStaticData.TranTypes.KEYEXCHANGE)
            data = tran.secureNII;
        else if (tran.inTransactionCode == TranStaticData.TranTypes.DCC_REQUEST)         //load the dcc nii
            data = "430";
        else {
            if (SettingsInterpreter.isTLEEnabled())
                data = tran.secureNII;
            else
                data = tran.NII;
        }

        data =  "0" + data;
        return data;
    }

    @Override
    public String loadFiled_25_Data(Transaction tran) {
        String data = "00";
        return data;
    }

    @Override
    public String loadFiled_26_Data(Transaction tran) {
        return null;
    }

    @Override
    public String loadFiled_27_Data(Transaction tran) {
        return null;
    }

    @Override
    public String loadFiled_28_Data(Transaction tran) {
        return null;
    }

    @Override
    public String loadFiled_29_Data(Transaction tran) {
        return null;
    }

    @Override
    public String loadFiled_30_Data(Transaction tran) {
        return null;
    }

    @Override
    public String loadFiled_31_Data(Transaction tran) {
        return null;
    }

    @Override
    public String loadFiled_32_Data(Transaction tran) {
        return null;
    }

    @Override
    public String loadFiled_33_Data(Transaction tran) {
        return null;
    }

    @Override
    public String loadFiled_34_Data(Transaction tran) {
        return null;
    }

    @Override
    public String loadFiled_35_Data(Transaction tran) {
        String data = tran.track2;
        if (data != null)
            data = TLE.setupField35ForTLE(data);

        return data;
    }

    @Override
    public String loadFiled_36_Data(Transaction tran) {
        return null;
    }

    @Override
    public String loadFiled_37_Data(Transaction tran) {
        String data = tran.RRN;
        return data;
    }

    @Override
    public String loadFiled_38_Data(Transaction tran) {
        String data = tran.approveCode;
        return data;
    }

    @Override
    public String loadFiled_39_Data(Transaction tran) {
        String data = tran.responseCode;
        return data;
    }

    @Override
    public String loadFiled_40_Data(Transaction tran) {
        return null;
    }

    @Override
    public String loadFiled_41_Data(Transaction tran) {
        String data = tran.terminalID;
        return data;
    }

    @Override
    public String loadFiled_42_Data(Transaction tran) {
        String data = tran.merchantID;
        return data;
    }

    @Override
    public String loadFiled_43_Data(Transaction tran) {
        String data = "";
        return data;
    }

    @Override
    public String loadFiled_44_Data(Transaction tran) {
        return null;
    }

    @Override
    public String loadFiled_45_Data(Transaction tran) {
        return null;
    }

    @Override
    public String loadFiled_46_Data(Transaction tran) {
        return null;
    }

    @Override
    public String loadFiled_47_Data(Transaction tran) {
        return null;
    }

    @Override
    public String loadFiled_48_Data(Transaction tran) {
        String data = "";
        data = "HBNDCC";
        return data;
    }

    @Override
    public String loadFiled_49_Data(Transaction tran) {
        String data  = "";
        return data;
    }

    @Override
    public String loadFiled_50_Data(Transaction tran) {
        return null;
    }

    @Override
    public String loadFiled_51_Data(Transaction tran) {
        return null;
    }

    @Override
    public String loadFiled_52_Data(Transaction tran) {
        String data = tran.encryptedPINBlock;
        return data;
    }

    @Override
    public String loadFiled_53_Data(Transaction tran) {
        return null;
    }

    @Override
    public String loadFiled_54_Data(Transaction tran) {
        return null;
    }

    @Override
    public String loadFiled_55_Data(Transaction tran) {
        String data = tran.emv55Data;
        return data;
    }

    @Override
    public String loadFiled_56_Data(Transaction tran) {
        return null;
    }


    @Override
    public String loadFiled_57_Data(Transaction tran)
    {
        String data = "";
        if (SettingsInterpreter.isTLEEnabled())
            data = TLE.getTransactionEncryptedData();
        return data;
    }

    @Override
    public String loadFiled_58_Data(Transaction tran)
    {
        return null;
    }

    @Override
    public String loadFiled_59_Data(Transaction tran)
    {
        return null;
    }

    @Override
    public String loadFiled_60_Data(Transaction tran) {
        String data = "";

        if ((tran.inTransactionCode == TranStaticData.TranTypes.SETTLE) || (tran.inTransactionCode == TranStaticData.TranTypes.CLOSE_BATCH_UPLOAD) ||
                (tran.origTransactionCode == TranStaticData.TranTypes.BATCH_UPLOAD))
            data = Formatter.formatForSixDigits(tran.batchNumber);
        else if(tran.getCardLabel().equals("JCB")) {
            data = Formatter.formatForSixDigits(tran.batchNumber);
        }

        if (SettingsInterpreter.setField60ForCombank && tran.origTransactionCode == TranStaticData.TranTypes.BATCH_UPLOAD)
            data = tran.origTransactionMTI + Formatter.formatForSixDigits(tran.traceNumber) + Formatter.formatForSixDigits( tran.inInvoiceNumber) + "000000";

        return data;
    }

    @Override
    public String loadFiled_61_Data(Transaction tran) {
        return null;
    }

    @Override
    public String loadFiled_62_Data(Transaction tran) {
        String data =  "" ;
        if (tran.inTransactionCode == TranStaticData.TranTypes.KEYEXCHANGE)
            data  = TLEKeyDownload.getTLEHeader();
        else
            data = Formatter.formatForSixDigits(tran.inInvoiceNumber);
        return data;
    }

    @Override
    public String loadFiled_63_Data(Transaction tran) {
        if(tran.inTransactionCode == TranStaticData.TranTypes.KEYEXCHANGE) {
            return Base.getTIDList(tran.issuerNumber);
        }
        else {
            int totalSaleAmount = Base.getAmountTotal(TranStaticData.TranTypes.SALE, tran.userSelectedIssuer, tran.userSelectedMerchantNumber) + Base.getAmountTotal(TranStaticData.TranTypes.PRE_COMP, tran.userSelectedIssuer, tran.userSelectedMerchantNumber);
            int totalSaleCount = Base.getAmountCount(TranStaticData.TranTypes.SALE, tran.userSelectedIssuer, tran.userSelectedMerchantNumber) + Base.getAmountCount(TranStaticData.TranTypes.PRE_COMP, tran.userSelectedIssuer, tran.userSelectedMerchantNumber);

            String sTotalSaleAmount = Formatter.getAmountFormattedForSettlement(totalSaleAmount);
            String sSaleCount = Formatter.getCountFormattedForSettlement(totalSaleCount);

            int totalRefundAmount = Base.getAmountTotal(TranStaticData.TranTypes.REFUND, tran.userSelectedIssuer, tran.userSelectedMerchantNumber);
            int totalRefundCount = Base.getAmountCount(TranStaticData.TranTypes.REFUND, tran.userSelectedIssuer, tran.userSelectedMerchantNumber);

            String sTotalRefundAmount = Formatter.getAmountFormattedForSettlement(totalRefundAmount);
            String sRefundCount = Formatter.getCountFormattedForSettlement(totalRefundCount);

            return sSaleCount + sTotalSaleAmount + sRefundCount + sTotalRefundAmount;
        }
    }

    @Override
    public String loadFiled_64_Data(Transaction tran) {
        String data = "";
        if (tran.inTransactionCode == TranStaticData.TranTypes.KEYEXCHANGE)
            data = TLEKeyDownload.getMac();
        else if (SettingsInterpreter.isTLEEnabled())
            data = TLE.getTransactionMAC();
        return data;
    }
}