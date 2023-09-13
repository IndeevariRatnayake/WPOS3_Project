package com.harshana.wposandroiposapp.Base;


import com.harshana.wposandroiposapp.Database.Card;
import com.harshana.wposandroiposapp.Database.Issuer;

/**
 * Created by harshana_m on 10/19/2018.
 */

public class Transaction {
    public String MTI = "";
    public String procCode = "";
    public String bitmap = "";

    //common fields
    public int inTransactionCode;
    public int inChipStatus;

    public boolean isFallbackTransaction = false;

    public static class ChipStatusTypes {
        public final static int EMV_CARD = 1;
        public final static int NOT_USING_CHIP = 2;
        public final static int CTLS_CARD = 3;
        public final static int CHIP_CARD = 4;
        public final static int MANUAL_KEY_IN = 5;
    }

    public int cdtIndex;
    public short shTranType;
    public int inInvoiceNumber;
    public int traceNumber;

    public String promoName = "";

    public long lnBaseTransactionAmount = 0;
    public long lnTotalAmount;
    public long serviceCharge;
    public long tipAmount;
    public long fuelCharge;
    public long discount;
    public float discountPercentage;

    public String tranCreditOrDebit = "";
    public String PAN = "";
    public int inCardType;
    public String Date = "",Time = "";
    public String receiptDate = "";
    public String cardSerialNo = "";

    public String track1 = "";
    public String track2 = "";
    public String scvCode = "";
    public String expDate = "";

    public String terminalID = "";
    public String merchantID = "";
    public String currencySymbol = "";
    public int merchantNumber;
    public String merchantName = "";
    public String NII = "";
    public String secureNII = "";
    public String TPDU = "";
    public String emv55Data = "";
    public String responseCode = "";
    public String approveCode = "";
    public String RRN = "";

    public int batchNumber;

    public int actualIssuerNumber;
    public int issuerNumber;
    public int isVoided = 0;

    public static class RejectReasons {
        public final static int USER_CANCELLED  = 1;
        public final static int HOST_DECLINED = 2;
        public final static int HOST_DECLINED_REVERSAL = 7;
        public final static int BUILD_ERROR = 3;
        public final static int REVERSAL_DB_ERROR = 4;
        public final static int NO_HOST_RESPONSE = 5;
        public final static int CURRENT_TRAN_REVERSE_SUCCESS  = 6;
        public final static int COMM_FALIURE = 8;
        public final static int TRANSACTION_DB_ERROR = 9;
        public final static int HOST_AND_CHIP_APPROVED_ONLINE = 10;
        public final static int HOST_ONLINE_CHIP_APPROVED_OFFLINE = 11;
        public final static int ERROR = 12;
        public final static int NO_HOST_RESPONSE_REVERSAL = 13;
        public final static int DECLINED_BY_CHIP = 14;
    }

    public int transactionRejectReason = 0;

    public void setRejectReason(int reason)
    {
        transactionRejectReason = reason;
    }
    public int getRejectReson()
    {
        return transactionRejectReason;
    }

    public Card cardData =  null;
    public Issuer issuerData = null;

    public String getCardLabel() {
        if (cardData == null)
            return null;
        else
            return cardData.cardLabel;
    }

    public int userSelectedIssuer = 0 ;
    public int userSelectedMerchantNumber = 0 ;
    public String encryptedPINBlock = null;
    public int origTransactionCode;
    public String origTransactionMTI;
    public int origTraceNumber;

    public boolean isTLEEncrypting = false;
    public String ExtData = null;
    public String emvResult = "";
    public boolean isManualEntry = false;

}
