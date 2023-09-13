package com.harshana.wposandroiposapp.Base;

import android.util.Log;

import com.harshana.wposandroiposapp.Communication.CommEngine;
import com.harshana.wposandroiposapp.DevArea.GlobalData;
import com.harshana.wposandroiposapp.DevArea.TranStaticData;
import com.harshana.wposandroiposapp.Print.Receipt;
import com.harshana.wposandroiposapp.Settings.Preferences;
import com.harshana.wposandroiposapp.Settings.SettingsInterpreter;
import com.harshana.wposandroiposapp.TLE.TLE;
import com.harshana.wposandroiposapp.Utilities.Sounds;

public class ReversalHandler extends Base {
    public int pushpPendingReversal() {
        byte[] recievedPacket =  null;

        if (!SettingsInterpreter.isReversalEnabled)
            return -1;

        updateStatusText("Checking Reversals");
        if (!loadTerminal()) {
            Log.d("SSSSSSSS", "01");
            showToastX("Couldn't load the terminal data", TOAST_TYPE_FAILED);
            return REVERSAL_FAILED;
        }

        Log.d("SSSSSSSS", "02");
        Log.d("SSSSSSSS Merchant", " : " + currentTransaction.merchantNumber);
        int checkRevResult = transactionDatabase.checkReversal(currentTransaction.merchantNumber);
        Log.d("checkRevResult", " : " + checkRevResult);

        if (checkRevResult == -1) {
            showToastX("DB Retrieving  Reversal Data Error", TOAST_TYPE_FAILED);
            return REVERSAL_FAILED;
        } else if (checkRevResult == 1) {
            //there is a reversal and it should be sent prior to this transaction
            //back up the current transaction
            if(SettingsInterpreter.isForceReversalEnabled()) {
                showToastX("Please clear the reversal",TOAST_TYPE_INFO);
                GlobalData.isForceRev = true;
                return REVERSAL_FAILED;
            }
            Transaction originalTran  = currentTransaction;
            Transaction revTran = transactionDatabase.getReveralTransaction(currentTransaction.merchantNumber);

            TData tranData  = TranStaticData.getTran(TranStaticData.TranTypes.REVERSAL);

            revTran.MTI = tranData.MTI;
            revTran.procCode = tranData.ProcCode;
            revTran.bitmap = originalTran.bitmap;

            if ((revTran.bitmap == null) || (revTran.bitmap.equals("")))
                revTran.bitmap = tranData.Bitmap;

            revTran.inTransactionCode = TranStaticData.TranTypes.REVERSAL;
            //currentTransaction = new Transaction();
            currentTransaction = revTran;

            int origInvNumber = revTran.inInvoiceNumber;

            //load the terminal and issuer information
            loadCardAndIssuer();
            loadTerminal();

            currentTransaction.inInvoiceNumber = origInvNumber;
            //Testing
            //currentTransaction.PAN = "4135410022448063";
            //currentTransaction.lnBaseTransactionAmount = 1000;
            currentTransaction.traceNumber = revTran.traceNumber;

            CommEngine comm = CommEngine.getInstance(currentTransaction.issuerData.IP,currentTransaction.issuerData.port);
            comm.setRectimeout(SettingsInterpreter.getReversalTimeout());

            TLE.updateKSN();

            setFieldsAndLoadPacket();
            packet.setPacketTPDU(currentTransaction.TPDU);
            byte[] rawPacket = packet.getRawDataPacket();
            recievedPacket = sendAndRecieve(comm, rawPacket);
            Log.d("isReversal FFF", "02 : " + isReversal);

            printISOLogs();

            if (isReversal) {
                //no response to the reversal, simply abort both transaction
                isReversal = false;
                showToastX("No response for the reversal",TOAST_TYPE_INFO);
                comm.disconnect();
                return REVERSAL_FAILED;
            }
            else if (!isReversal && recievedPacket != null) {
                //we have a reversal response
                String revRespCode = getUnpackPacketAndHostResponse(recievedPacket);
                if (revRespCode.equals("00")) {
                    //we need to clear the reversal entry we just entered
                    if (!transactionDatabase.removeReversal(currentTransaction.merchantNumber)) {
                        showToast("Removing reversal failed!,Critical",TOAST_TYPE_FAILED);
                    }

                    //restore the original transaction for the current transaction object
                    configDatabase.saveInvoiceNumber(currentTransaction);
                    currentTransaction = originalTran;
                    currentTransaction.inInvoiceNumber = origInvNumber;
                    comm.disconnect();
                    return REVERSAL_SUCCESS;
                    //fall through to below original transaction processing code
                }
                else {
                    showToastX("Host Declined the Reversal",TOAST_TYPE_FAILED);
                    comm.disconnect();
                    return REVERSAL_FAILED;
                    //if the reversal is not approved we still abort processing transaction for this merchant
                }
            }
            else if ( recievedPacket == null) {
                //communication failure
                showToastX("Reversal Failed - No response",TOAST_TYPE_INFO);
                comm.disconnect();
                return REVERSAL_FAILED;
            }
        }
        return 0;
    }
}