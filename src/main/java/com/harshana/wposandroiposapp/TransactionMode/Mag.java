package com.harshana.wposandroiposapp.TransactionMode;

import android.os.SystemClock;
import android.util.Log;

import com.harshana.wposandroiposapp.Base.Base;
import com.harshana.wposandroiposapp.Base.Transaction;
import com.harshana.wposandroiposapp.DevArea.GlobalData;
import com.harshana.wposandroiposapp.MainActivity;
import com.harshana.wposandroiposapp.Print.Receipt;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.Settings.SettingsInterpreter;
import com.harshana.wposandroiposapp.UI.KeyPadDialog;

import java.util.concurrent.CountDownLatch;

import wangpos.sdk4.libbasebinder.Core;


public class Mag extends Base {
    String pinblock = "";
    private boolean pinError = false;
    public int  processTransaction() {
        //we have the pan here. if this is a ecr transaction then we must push the
        //extracted masked pan to the client application for necessary promotion
        //calculations.
        int ecrRet = 0;

        GlobalData.globalResult = 0;

        if (  (ecrRet = initiateECR()) != SUCCESS) {
            GlobalData.globalTransactionAmount = 0;
            return ecrRet;
        }

        int fallbackTime = 7;

        //check whether this is a fall back transaction
        if (GlobalData.isFallback) {
            long currentMiliForFallback = SystemClock.elapsedRealtime();
            long gap = currentMiliForFallback - GlobalData.lnFallBackStartedTime;

            if ( gap <= (fallbackTime * 1000)) {
                currentTransaction.isFallbackTransaction = true;
                currentTransaction.lnBaseTransactionAmount = GlobalData.lnFallbackAmount;
                GlobalData.lnFallbackAmount = 0;
            } else
                GlobalData.isFallback = false;
        }

        //load the card and the issuer
        if (!loadCardAndIssuer()) {
            showToast("Loading card and issuer data failed",TOAST_TYPE_FAILED);
            errorTone();
            GlobalData.globalTransactionAmount = 0;
            return GENERIC_ERROR_TRAN_MIDDLE;
        }

        //set the appropriate package name
        setPackageName();

        //force the user to use the chip instead of mad if the card contains a chip
        if (!currentTransaction.isFallbackTransaction && currentTransaction.inChipStatus == Transaction.ChipStatusTypes.NOT_USING_CHIP) {
            if (chkSVCCode() && currentTransaction.cardData.chkSvcCode) {
                showToast("Please, Insert the card",TOAST_TYPE_WARNING);
                GlobalData.globalTransactionAmount = 0;
                return GENERIC_ERROR_TRAN_MIDDLE;
            }
        }

        if(currentTransaction.inChipStatus != Transaction.ChipStatusTypes.MANUAL_KEY_IN) {
            requestConfirmLastFour();
            if (!getResultFromInputAmountScreen())
                return GENERIC_ERROR_TRAN_MIDDLE;
        }

        if (!currentTransaction.isFallbackTransaction) {
            currentTransaction.lnBaseTransactionAmount = GlobalData.globalTransactionAmount;
            GlobalData.globalTransactionAmount = 0 ;
        }

        //Acquire data from the user to proceed the transaction
        //get the amount from the user
        //currentTransaction.lnBaseTransactionAmount = GlobalData.globalTransactionAmount;
        if ((!currentTransaction.isFallbackTransaction) && (currentTransaction.lnBaseTransactionAmount == 0)) {
            invokeAmountInputScreen();
            if (!getResultFromInputAmountScreen())
                return GENERIC_ERROR_TRAN_MIDDLE;

            currentTransaction.lnBaseTransactionAmount = GlobalData.globalTransactionAmount;
            GlobalData.globalTransactionAmount = 0 ;
        }

        if (currentTransaction.getCardLabel().equals("CUP")) {
            final CountDownLatch countDownLatch  = new CountDownLatch(1);{
                (mActivity).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        KeyPadDialog.getInstance().showDialog(mActivity, new KeyPadDialog.OnPinPadListener() {
                            @Override
                            protected void onSuccess() {
                                countDownLatch.countDown();
                            }

                            @Override
                            protected void onSuccess(String pin) {
                                pinblock = pin;
                                countDownLatch.countDown();
                            }

                            @Override
                            protected void onError(int errorCode, String errorMsg) {
                                super.onError(errorCode, errorMsg);
                                pinError = true;
                                countDownLatch.countDown();
                            }

                            @Override
                            protected void onBypass()
                            {
                                countDownLatch.countDown();
                            }
                        });
                    }
                });

                try {
                    Log.d("Invi_PIN", "WaitingMag");
                    countDownLatch.await();
                    Log.d("Invi_PIN", "WaitingMag2");
                } catch (InterruptedException ex) {  }

                if(pinError) {
                    Log.d("Invi_PIN", "onBypass1");
                    showToast("Reading card and issuer failed", TOAST_TYPE_FAILED);
                    errorTone();
                    pinError = false;
                    return GENERIC_ERROR_TRAN_MIDDLE;
                }
            }

            if (pinblock != "")
                currentTransaction.encryptedPINBlock = pinblock;
        }

        if (SettingsInterpreter.isSingleMerchantEnabled() || (SettingsInterpreter.isECREnabled() && MainActivity.ecr.isECRInitiated)) {
            GlobalData.selectedMerchant = Base.getFirstMerchantOfIssuer(GlobalData.selectedIssuer);
        }
        selectMerchant();

        if (!loadTerminal()) {
            showToast("Loading terminal data failed, aborting...",TOAST_TYPE_FAILED);
            errorTone();
            return GENERIC_ERROR_TRAN_MIDDLE;
        }

        Log.d("TRANS","01");

        if (REVERSAL_FAILED == reversalHandler.pushpPendingReversal()) {
            if(!SettingsInterpreter.isForceReversalEnabled())
                showToast("Reversal Failed", TOAST_TYPE_FAILED);
            errorTone();
            return 0;
        }

        //user has confirmed the transaction so we proceed
        if (0 == sendTransactionOnline(null,null)) {
            //push the result after the ecr transaction is finished
            if( SettingsInterpreter.isECREnabled() && MainActivity.ecr.isECRInitiated )
                MainActivity.ecr.pushTransactionDetails(SUCCESS,currentTransaction,"00");

            if (!transactionDatabase.writeTransaction(currentTransaction)) {
                //must send a reversal here
                return TERMINATE_TRANSACTION;
            }

            configDatabase.saveInvoiceNumber(currentTransaction);
            showToast("Transaction Approved",TOAST_TYPE_SUCCESS);

            //play approved sound clip
            //playSound(R.raw.transaction_approved);
            //print the receipt here
            startBusyAnimation("Printing...");
            Receipt rcpt =  Receipt.getInstance();

            //printISOLogs();

            try {
                rcpt.printReceipt(0);
            } catch (Exception ex) {}

            stopBusyAnimation();
        }
        else {
            if (isReversal) {
                isReversal = false;

                if (REVERSAL_FAILED == reversalHandler.pushpPendingReversal()) {
                    if(!SettingsInterpreter.isForceReversalEnabled())
                        showToast("Auto Reversal Failed", TOAST_TYPE_FAILED);
                    errorTone();
                    return 0;
                }
            }
        }

        stopBusyAnimation();
        return SUCCESS;
    }



    protected boolean chkSVCCode()
    {
        return currentTransaction.scvCode.startsWith("2") || currentTransaction.scvCode.startsWith("6");
    }
}
