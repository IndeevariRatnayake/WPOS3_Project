package com.harshana.wposandroiposapp;

import java.lang.reflect.Method;

public class KeyControlUtil {
    static Class WangPosManagerClass;
    static Object WangPosManager;

    public static void initWpos() {
        try {
            WangPosManagerClass = Class.forName("android.os.WangPosManager");
            WangPosManager = WangPosManagerClass.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
    * 0 is open
    * 1 is close
    * */
    public static void setADBMode(int type) {
        initWpos();

        try {
            Method method = WangPosManagerClass.getMethod("setAdbMode", int.class);
            method.invoke(WangPosManager, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * power button  on-off
     *
     * @param type true - on ;false - off
     */
    public static void setPropForControlPowerKey(boolean type) {
        initWpos();
        try {
            Method method = WangPosManagerClass.getMethod("setPropForControlPowerKey", boolean.class);
            method.invoke(WangPosManager, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /**
     * set SystemUi's statusbar available(This systemui can't be a third party app)
     *
     * @param mode if the mode is 0, statusbar available
     *   if the mode is 1, statusbar unavailable
     */
    public static void setStatusbarMode(int mode) {
        initWpos();
        try{
            Method method = WangPosManagerClass.getMethod("setStatusbarMode", int.class);
            method.invoke(WangPosManager, mode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the back key is available or unavailable
     *
     * @param type if type is true, back key is unavailable
     *   else back key is available.
     */
    public static  void setPropForControlBackKey(boolean type) {
        initWpos();
        try{
            Method method = WangPosManagerClass.getMethod("setPropForControlBackKey", boolean.class);
            method.invoke(WangPosManager, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Set the home key is available or unavailable
     *
     * @param type if type is true, home key is unavailable
     *   else home key is available.
     */
    public static void setPropForControlHomeKey(boolean type) {
        initWpos();
        try{
            Method method = WangPosManagerClass.getMethod("setPropForControlHomeKey", boolean.class);
            method.invoke(WangPosManager, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Set the menu key is available or unavailable
     *
     * @param type if type is true, menu key is unavailable
     *   else menu key is available.
     */
    public static void setPropForControlMenuKey(boolean type) {
        initWpos();
        try{
            Method method = WangPosManagerClass.getMethod("setPropForControlMenuKey", boolean.class);
            method.invoke(WangPosManager, type);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
