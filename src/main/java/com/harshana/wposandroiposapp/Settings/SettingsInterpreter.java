package com.harshana.wposandroiposapp.Settings;


import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.harshana.wposandroiposapp.Base.Base;


/**
 * Created by harshana_m on 11/23/2018.
 */

public class SettingsInterpreter extends Base {
    static SharedPreferences sharedPreferences = null;

    public static boolean setField60ForCombank = true;
    public static boolean isReversalEnabled = true;
    public static boolean isSingleMerchantEnabled = true;

    public SettingsInterpreter()
    {  }

    public static boolean isSingleMerchantEnabled() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        return sharedPreferences.getBoolean("is_single_merchant",true);
    }

    public static boolean isForceReversalEnabled() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        return sharedPreferences.getBoolean("is_force_rev_enable",false);
    }

    public static boolean isAutoReversalEnabled() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        return sharedPreferences.getBoolean("is_auto_reversal",false);
    }

    public static boolean isTerminalAutoSettlementEnabled() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        return sharedPreferences.getBoolean("is_terminal_aut_settle_enabled",false);
    }

    /*public static int getCardReaderIdleTimeOut() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        String timeout =  sharedPreferences.getString("card_reader_idle_timeout","5");
        return Integer.valueOf(timeout);
    }*/
    public static boolean isIsoPrintEnabled() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        return sharedPreferences.getBoolean("iso_print",false);
    }

    public static boolean isUAT() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        return sharedPreferences.getBoolean("is_uat",false);
    }

    public static boolean isIsoPrintEnabledTLE() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        return sharedPreferences.getBoolean("iso_print_enc", false);
    }

    public static int getConnectTimeout() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        String timeout =  sharedPreferences.getString("connect_timeout","12");
        return Integer.valueOf(timeout);
    }

    public static int getReversalTimeout() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        String revTimeout =  sharedPreferences.getString("reversal_timeout","12");
        return Integer.valueOf(revTimeout);
    }

    public static boolean isTLEEnabled() {
        //check whether the specific host is supporting the tle
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        return sharedPreferences.getBoolean("tle_enable",true);
    }

    public static boolean isThemeEnabled() {
        sharedPreferences  = PreferenceManager.getDefaultSharedPreferences(appContext);
        return sharedPreferences.getBoolean("theme_enabled",false);
    }

    public static boolean isBatteryMonitorOn() {
        sharedPreferences  = PreferenceManager.getDefaultSharedPreferences(appContext);
        return sharedPreferences.getBoolean("battery_monitor",true);
    }

    public static boolean isSpeechOn() {
        sharedPreferences  = PreferenceManager.getDefaultSharedPreferences(appContext);
        return sharedPreferences.getBoolean("speech_enable",true);
    }

    public static boolean isDCCEnabled() {
        sharedPreferences  = PreferenceManager.getDefaultSharedPreferences(appContext);
        return sharedPreferences.getBoolean("dcc_enabled",true);
    }

    public static boolean isManualKeyIn() {
        sharedPreferences  = PreferenceManager.getDefaultSharedPreferences(appContext);
        return sharedPreferences.getBoolean("manual_key_in",false);
    }

    public static boolean isEMVDiagnosisEnabled() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        return sharedPreferences.getBoolean("emv_diag_enable",false);
    }

    public static boolean isECREnabled() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        return sharedPreferences.getBoolean("ecr_enable",false);
    }

    public static boolean isPushPullModuleEnabled() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        return sharedPreferences.getBoolean("enable_pp_module",false);
    }

    private static onSettingsChange onDevModeChangedListener;

    public interface onSettingsChange {
         void OnSettingsChange(String key,boolean state);
    }

    public static void setOnDevModeChangedListener(onSettingsChange _onDevModeChangedListener) {
        onDevModeChangedListener = _onDevModeChangedListener;
    }

    public static void callSettingsChangedFunc(String key,boolean status) {
        onDevModeChangedListener.OnSettingsChange(key,status);
    }

    public static boolean isPrintCustCopy() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        return sharedPreferences.getBoolean("enable_cust_copy",true);
    }
}