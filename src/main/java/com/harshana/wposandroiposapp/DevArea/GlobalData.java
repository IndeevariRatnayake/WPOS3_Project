package com.harshana.wposandroiposapp.DevArea;



import com.harshana.wposandroiposapp.Base.SettlementData;

import java.util.List;

public class GlobalData
{
    public static int transactionCode = -1;
    public static String transactionName = "";
    public static String addressLine1 = "";
    public static String addressLine2 = "";
    public static String addressLine3 = "";
    public static String merchantName = "";
    public static String autoSettleDate = "";
    public static String autoSettleTime = "";

    public static String SaleAmt = "";
    public static String RefundAmt = "";

    public static SettlementData settlementData;
    private static List<String> appList = null;
    public static void setAppList(List<String> apps)
    {
        appList = apps;
    }

    public static List<String> getAppList()
    {
        return appList;
    }
    public static int applicationIndex =  -1;

    public static int isDuplicate = 0;
    public static boolean isForceRev = false;

    public static void setSelectedApplicationIndex(int index)
    {
        applicationIndex = index;
    }
    public static int getSelectedApplicationIndex()
    {
        return applicationIndex;
    }

    public static boolean isFallback = false;
    public static boolean bECRFuncShouldStop = false;
    public static long lnFallBackStartedTime = 0 ;
    public static long lnFallbackAmount = 0 ;
    public static boolean isFallbackFromCTLS  = false;

    public static boolean isForiegnCurrencySelected = false;
    public static String cardCurrency = "";
    public static int DCCSelectedCurrencyType = -1;
    public static long globalTransactionAmount = 0;

    public static int selectedIssuer = 0;
    public static int selectedMerchant = 0 ;
    public static String pin = "";

    public static String ksn ="";
    public static boolean inDebugMode ;
    public static boolean isReversalEnabled = true;
    public static int globalResult = 0;
    public static boolean isManualKeyIn = false;
    public static String manualKeyExpDate = "";
    public static String manualKeyPan = "";


    public static boolean isAutoSettling = false;
    public static final String USER_ROLE = "USER_ROLE";
    public static final String ADMIN = "ADMIN";
    public static final String MERCHANT = "MERCHANT";


}
