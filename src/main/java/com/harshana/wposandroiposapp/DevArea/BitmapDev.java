package com.harshana.wposandroiposapp.DevArea;

import com.harshana.wposandroiposapp.Base.Transaction;
import com.harshana.wposandroiposapp.Packet.Bitmap;
import com.harshana.wposandroiposapp.Settings.SettingsInterpreter;
/**
 * Created by harshana_m on 10/25/2018.
 */

public class  BitmapDev extends Bitmap implements Bitmap.OnSetOverrideFieldSettingsListener {

    public BitmapDev(String bitmap){super(bitmap);}

    //set  as required in below method
    @Override
    public void onSetOverrideFieldSettings(Transaction tran) {
        if (tran.isTLEEncrypting) // if tle encrypting we skip modifying the bitmap
            return;

        if (SettingsInterpreter.isTLEEnabled()) {  }

        resetField(2);
        resetField(14);

        //set common fields here
        setField(4);
        setField(11);
        setField(22);
        setField(41);
        setField(42);
        setField(35);

        if (tran.encryptedPINBlock != null)
            setField(52);

        if  (   (tran.inChipStatus == Transaction.ChipStatusTypes.EMV_CARD) ||
                (tran.inChipStatus == Transaction.ChipStatusTypes.CTLS_CARD))
            setField(55);


        if (tran.inTransactionCode == TranStaticData.TranTypes.REVERSAL) {
            resetField(35);
            setField(2);
            setField(14);           //set for expiration dates
            setField(62);
            resetField(55);
            resetField(37);
            resetField(57);
            resetField(64);
        }
        else if ((tran.inTransactionCode == TranStaticData.TranTypes.SETTLE) || (tran.inTransactionCode == TranStaticData.TranTypes.CLOSE_BATCH_UPLOAD)) {
            resetField(4);
            setField(60);               //set for batch number
        }
        else if (tran.origTransactionCode == TranStaticData.TranTypes.BATCH_UPLOAD) {
            setField(2);
            setField(12);
            setField(13);
            setField(14);
            resetField(35);
            resetField(23);
            setField(37);
            setField(38);
            setField(39);
            setField(60);
        }
        else if (tran.inTransactionCode == TranStaticData.TranTypes.KEYEXCHANGE) {
            resetField(4);
            resetField(2);
            setField(63);
            setField(64);
        }
        else if (tran.inTransactionCode == TranStaticData.TranTypes.DCC_REQUEST)
            resetField(35);
        else if (tran.origTransactionCode == TranStaticData.TranTypes.VOID) {
            setField(2);
            resetField(35);
            setField(14);
            setField(37);
            resetField(55);
        }

        //overriding the standards settings
        String cardLabel = "";
        if ((cardLabel = tran.getCardLabel()) != null) {
            if (cardLabel.equals("CUP"))
                resetField(23);
            else if ( cardLabel.equals("AMEX")) {
                setField(23);

                if (tran.origTransactionCode == TranStaticData.TranTypes.VOID) {
                    setField(38);
                    setField(55);
                }
            }
            else if (cardLabel.equals("JCB")) {
                setField(60);
            }
        }

        if (tran.inChipStatus == Transaction.ChipStatusTypes.MANUAL_KEY_IN) {
            setField(2);
            setField(14);
        }

        if (tran.inTransactionCode == TranStaticData.TranTypes.PRE_COMP) {
            setField(2);
            setField(14);
            setField(22);
            resetField(35);
            resetField(55);
        }
    }
}