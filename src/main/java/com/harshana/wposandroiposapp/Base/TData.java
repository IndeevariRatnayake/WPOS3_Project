package com.harshana.wposandroiposapp.Base;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by harshana_m on 10/23/2018.
 */

public class TData
{
    public int TranID;
    public String MTI;
    public String Bitmap;
    public String ProcCode;
    public String tranName;

    static List<TData> Trans;


    protected TData()
    {
        Trans = new ArrayList<>();
    }

    protected TData(int id,String mti,String bitmap,String procCode,String name)
    {
        TranID = id;
        MTI = mti;
        Bitmap = bitmap;
        ProcCode  = procCode;
        tranName = name;
    }

    public  void addTran(int id, String mti, String bitmap, String procCode,String name)
    {
        TData t;
        t  = new TData(id,mti,bitmap,procCode,name);
        Trans.add(t);
    }


    String getBitmap(int tranID)
    {
        for (TData t: Trans)
        {
            if(t.TranID == tranID)
                return t.Bitmap;
        }

        return null;
    }

    public static TData getTran(int id)
    {
        for (TData t : Trans)
        {
            if (t.TranID == id)
                return t;
        }
        return null;
    }

    public static String getTranName(int tranCode)
    {
        for (TData t: Trans)
        {
            if (t.TranID == tranCode)
                return t.tranName;

        }

        return null;
    }

}
