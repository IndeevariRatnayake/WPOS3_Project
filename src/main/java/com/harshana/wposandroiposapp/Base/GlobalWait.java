package com.harshana.wposandroiposapp.Base;

import com.harshana.wposandroiposapp.DevArea.GlobalData;

public class GlobalWait {
    private static boolean waiting = false;
    private static boolean lastOperCancelled = false;

    public static void  setWaiting() {
        waiting = true;
    }

    public static boolean isWaiting() {
        return waiting;
    }

    private static GlobalWait myInstance = null;

    public static GlobalWait getInstance() {
        if (myInstance == null)
            myInstance = new GlobalWait();
        return  myInstance;
    }

    public static void resetWaiting()
    {
        waiting = false;
    }

    public static void setLastOperCancelled(boolean status) {
        lastOperCancelled = status;
        if(lastOperCancelled) {
            GlobalData.transactionCode = -1;
        }
    }

    public static boolean isLastOperCancelled() {
        return lastOperCancelled;
    }
}