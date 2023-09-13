package com.harshana.wposandroiposapp.Settings;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {
    Context appContext;
    private static final String GLOBAL_SETTINGS_TAG  = "settings";
    private static Preferences myInstance = null;

    private Preferences(Context c)
    {
        appContext = c;
    }

    public static Preferences getInstance(Context c) {
        if (myInstance == null)
            myInstance = new Preferences(c);

        return myInstance;
    }

    public void saveSetting(String name,String value) {
        SharedPreferences settings = appContext.getSharedPreferences(GLOBAL_SETTINGS_TAG, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(name,value);
        editor.commit();
    }

    public String getSetting(String name) {
        SharedPreferences settings = appContext.getSharedPreferences(GLOBAL_SETTINGS_TAG, 0);
        String value = settings.getString(name,null);
        return value;
    }

    public boolean getBoolean(String name) {
        SharedPreferences settings = appContext.getSharedPreferences(GLOBAL_SETTINGS_TAG, 0);
        boolean value = settings.getBoolean(name,false);
        return value;
    }

    public void saveBoolean(String name,boolean value) {
        SharedPreferences settings = appContext.getSharedPreferences(GLOBAL_SETTINGS_TAG, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(name,value);
        editor.commit();
    }

    private static final String INITIAL_LOADING = "initial_loading";

    public boolean isInitialLoading() {
        SharedPreferences prefs = appContext.getSharedPreferences(GLOBAL_SETTINGS_TAG, 0);
        String value = prefs.getString(INITIAL_LOADING,"0");

        if (Integer.valueOf(value) == 0) //yes this is an initial loading
            return true;

        return false;
    }

    public void setInitialLoadingDone() {
        SharedPreferences settings = appContext.getSharedPreferences(GLOBAL_SETTINGS_TAG, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(INITIAL_LOADING,"1");
        editor.commit();
    }

    public void clear() {
        SharedPreferences preferences = appContext.getSharedPreferences(GLOBAL_SETTINGS_TAG,0);
        preferences.edit().clear();
        preferences.edit().commit();
    }
}