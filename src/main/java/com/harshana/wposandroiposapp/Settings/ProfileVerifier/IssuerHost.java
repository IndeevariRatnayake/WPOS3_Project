package com.harshana.wposandroiposapp.Settings.ProfileVerifier;

import java.util.ArrayList;
import java.util.List;

public class IssuerHost
{
    public int recId;
    public String name;
    public List<Integer> issrList;
    public int baseIssuer;

    public void setIssuer(String _name,String issuerList,int _baseIssuer) throws Exception
    {
        name = _name;
        String issuers[] = issuerList.split(",");
        issrList = new ArrayList<>();

        for (String s : issuers)
            issrList.add(Integer.valueOf(s));

        baseIssuer = _baseIssuer;
    }


}
