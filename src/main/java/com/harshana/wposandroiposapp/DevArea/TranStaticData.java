package com.harshana.wposandroiposapp.DevArea;

import com.harshana.wposandroiposapp.Base.TData;

/**
 * Created by harshana_m on 10/23/2018.
 */

public class TranStaticData extends TData
{
    private static TranStaticData myInstance = null;

    public static TranStaticData getMyInstance()
    {
        if (myInstance == null)
            return new TranStaticData();

        return myInstance;
    }

    public static class TranTypes
    {
        public static final int SALE = 1;
        public static final int REVERSAL = 2;
        public static final int BATCH_UPLOAD = 3;
        public static final int VOID = 4;
        public static final int SETTLE = 5;
        public static final int CLOSE_BATCH_UPLOAD = 6;
        public static final int KEYEXCHANGE = 7;
        public static final int REFUND = 8;
        public static final int DCC_REQUEST = 9;
        public static final int PRE_AUTH = 10;
        public static final int OFFLINE_SALE = 11;
        public static final int PRE_COMP = 12;
        public static final int CASH_BACK = 13;
    }


    public  void initTranData()
    {
        addTran(TranTypes.SALE,"0200","3020058020c00204","000000","Sale");
        addTran(TranTypes.OFFLINE_SALE,"0220","703C058004C00004","000000","Offline Sale");
        addTran(TranTypes.REVERSAL,"0400","7024058000C00000","004000","Reversal");
        addTran(TranTypes.BATCH_UPLOAD,"0320","3020058000C00004","004000","Batch Upload");
        addTran(TranTypes.VOID,"0200","703C058008C00004","024000","Void");
        addTran(TranTypes.SETTLE,"0500","2020010000C00012","920000","Settlement");
        addTran(TranTypes.CLOSE_BATCH_UPLOAD,"0500","2020010000C00012","960000","Close Batch");
        addTran(TranTypes.KEYEXCHANGE,"0800","2020010000C01004","010000","TLE Key Exchange");
        addTran(TranTypes.REFUND,"0200","3020058000C00004","200000","Refund");
        addTran(TranTypes.DCC_REQUEST,"0100","703C018000E18000","000000","DCC Request");
        addTran(TranTypes.PRE_AUTH,"0100","3020058020C00204","300000","Pre Auth");
        addTran(TranTypes.PRE_COMP,"0220","703C05800CC00004","000000","Pre Comp");
        addTran(TranTypes.CASH_BACK,"0200","3020058020c00204","170000","Cash Back");
    }
}