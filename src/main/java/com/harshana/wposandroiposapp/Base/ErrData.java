package com.harshana.wposandroiposapp.Base;


import java.util.ArrayList;
import java.util.List;

/**
 * Created by harshana_m on 10/25/2018.
 */

public class ErrData
{
    private static List<Error> list ;
    public ErrData()
    {
        list = new ArrayList<>();
    }

    class Error
    {
        int errCode;
        String description;

        Error(int err,String desc)
        {
            errCode = err;
            description = desc;
        }
    }

    protected void addErrorCodeWithDesc(int errCode, String desc) {
        list.add(new Error(errCode,desc));
    }

    public static String getErrorDesc(int errCode) {
        for ( Error er : list) {
            if (er.errCode == errCode)
                return  er.description;
        }

        return null;
    }
}
