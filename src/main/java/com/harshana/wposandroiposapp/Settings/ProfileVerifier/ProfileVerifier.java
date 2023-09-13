package com.harshana.wposandroiposapp.Settings.ProfileVerifier;

import android.content.Context;
import android.database.Cursor;

import com.harshana.wposandroiposapp.Settings.DBHelperSync;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProfileVerifier
{
    private DBHelperSync helperSync;
    private Context appContext;
    private Logger logger;
    private List<IssuerHost> issuerHostsList;
    private RuleSet rules;

    private static ProfileVerifier instance = null;

    public static ProfileVerifier getInstance(Context c)
    {
        if (instance == null)
            instance = new ProfileVerifier(c);

        return instance;
    }

    private ProfileVerifier(Context _context)
    {
        appContext = _context;
        init();
    }

    boolean init()
    {
        //load the issuer hosts
        helperSync = DBHelperSync.getInstance(appContext);
        issuerHostsList = new ArrayList<>();
        rules = new RuleSet();

        logger = Logger.getInstance(appContext);
        return true;
    }

    private void verify()
    {
        logger = Logger.getInstance(appContext);
        logger.init();

        logger.writeLine("**************************************************");
        logger.writeLine("*********** PROFILE VERIFIER LOG TRACE ***********");
        logger.writeLine("**************************************************");
        logger.writeLine(" ");

        if (issuerHostsList != null)
            issuerHostsList.clear();
        if ( !loadIssuers())
        {
            logger.writeLine("proper issuer host loading was problamatic , process terminated");
            return;
        }

        //issue host loading fine
        rules.setRuleState(RuleSet.RULE_ISSUER_HOST_LOADING);
        logger.log("loading issuer host ", Logger.StatusTag.SUCCESS);


        if (!checkForValidIssuers())
            return;


        rules.setRuleState(RuleSet.RULE_CHECKING_VALID_ISSUERS);
        logger.log("checking for validity of issuers", Logger.StatusTag.SUCCESS);


        //check for the duplicated issuers in the issue list
        if (!isIssuersDuplicated())
            return;


        //no duplicated issuers
        rules.setRuleState(RuleSet.RULE_CHECKING_DUPLICATED_ISSUERS);
        logger.log("checking duplicated issuers for all hosts", Logger.StatusTag.SUCCESS);

        //check if the base issuer is set correct way
        if (!isBaseisSetCorrect())
            return;

        //base issuers are set correctly
        rules.setRuleState(RuleSet.RULE_CHECKING_BASE_ISSUER_SETTING);
        logger.log("checking if base issuer is correctly set", Logger.StatusTag.SUCCESS);

        if (!checkIssuersRepeatedInOtherHosts())
            return;

        //no repeated issuers
        rules.setRuleState(RuleSet.RULE_CHECKING_REPEATED_ISSUERS_IN_OTHER_HOSTS);
        logger.log("checking for duplicated issuers in unrelated hosts", Logger.StatusTag.SUCCESS);


        if (!checkBaseoIssuerMerchantsCounts())
            return;

        rules.setRuleState(RuleSet.RULE_CHECKING_BASE_ISSUER_MERCHANT_COUNT_WITH_LIST);
        logger.log("checking for base issuer merchant count and sub merchant counts", Logger.StatusTag.SUCCESS);

        //base issuer merchant counts are ok

        if (!checkBaseMerchantDuplicatedTIDOrMID())
            return;


        rules.setRuleState(RuleSet.RULE_CHECKING_BASE_MERCHANT_DUPL_TID_MID);
        logger.log("checking for base issuer merchant duplicated tid mid", Logger.StatusTag.SUCCESS);


        if (!checkBaseMerchantDataWithsubMerchantData())
            return;

        rules.setRuleState(RuleSet. RULE_CHECKING_BASE_ISSUER_DATA_WITH_OTHER_MERCH);
        logger.log("checking other merchant data with base merchants ", Logger.StatusTag.SUCCESS);

    }

    private boolean checkForValidIssuers()
    {
        //load the existing issuers
        String quary = "SELECT * FROM IIT";
        Cursor iit = null;

        try
        {
            iit = helperSync.readWithCustomQuary(quary);
        }catch (Exception ex)
        {
            ex.printStackTrace();
        }

        if (iit == null)
        {
            logger.writeLine("Critical error loading iit");
            return false;
        }

        if (iit.getCount() == 0)
        {
            logger.log("There are no valid records for issuers" , Logger.StatusTag.FAILED);
            return false;
        }

        List<Integer> iits  = new ArrayList<>();

        while (iit.moveToNext())
            iits.add(iit.getInt(iit.getColumnIndex("IssuerNumber")));


        for (IssuerHost issuerHost : issuerHostsList)
        {
            for (Integer i : issuerHost.issrList)
            {
                if (!iits.contains(i))
                {
                    iit.close();
                    logger.log("invalid issuer is defined within a host , host is " + issuerHost.name + " Invalid issuer id is " + i, Logger.StatusTag.FAILED);
                    logger.writeLine("[resolution] define only the issuers which has been defined in the iit table");
                    return false;
                }
            }
        }


        iit.close();
        return true;
    }

    public void performVerification()
    {
       verify();


       //checking for what percentage the profile is correct
        int size = rules.ruleStore.size();
        int stepCount = 0 ;

        //reset all
        for (Map.Entry<Integer,Boolean> entry : rules.ruleStore.entrySet())
        {
            if (entry.getValue() == true)
                stepCount++;
        }



        //calculate the percentage
        float perc = ((float)stepCount / size) * 100;

        for (Map.Entry<Integer,Boolean> entry : rules.ruleStore.entrySet())
        {
            rules.ruleStore.put(entry.getKey(),false);
        }

        logger.writeLine(" ");
        logger.writeLine(" ");
        logger.writeLine("########################################################################");
        logger.writeLine("Profile is " + (int)perc + "% Completed");
        logger.writeLine("########################################################################");


        logger.deInit();
    }

    private boolean checkBaseMerchantDuplicatedTIDOrMID()
    {
        for (IssuerHost issuerHost : issuerHostsList)
        {
            int baseIsser = issuerHost.baseIssuer;

            //load the mid and tid
            String quary = "SELECT TerminalID,MerchantID FROM TMIF,MIT WHERE TMIF.IssuerNumber = " + baseIsser + " AND  TMIF.MerchantNumber = MIT.MerchantNumber";

            Cursor baseMerchData = null;
            try
            {
                baseMerchData = helperSync.readWithCustomQuary(quary);
            }catch (Exception ex)
            {
                ex.printStackTrace();
                return false;
            }

            //check for duplicated data
            //check if the tid is duplicated
            List<String> tids = new ArrayList<>();
            List<String> mids = new ArrayList<>();

            while (baseMerchData.moveToNext())
            {
                tids.add(baseMerchData.getString(baseMerchData.getColumnIndex("TerminalID")));
                mids.add(baseMerchData.getString(baseMerchData.getColumnIndex("MerchantID")));
            }


            for (int i = 0 ; i < tids.size(); i++)
            {
                for (int j = 0 ; j < tids.size(); j++)
                {
                    if (i == j) continue;

                    if (tids.get(i).equals(tids.get(j)))
                    {
                        logger.log("Duplicated data found for host " + issuerHost.name + " in base issuer  " + baseIsser + " TID data " + "id = " + i + " and id = " + j, Logger.StatusTag.FAILED);
                        logger.writeLine("[resolution] for a base issuer there can not be duplicated TIDs");
                        baseMerchData.close();
                        return false;
                    }
                }
            }

            for (int i = 0 ; i < mids.size(); i++)
            {
                for (int j = 0 ; j < mids.size(); j++)
                {
                    if (i == j) continue;

                    if (mids.get(i).equals(mids.get(j)))
                    {
                        logger.log("Duplicated data found for host " + issuerHost.name + " in base issuer  " + baseIsser + " MID data " + "id = " + i + " and id = " + j, Logger.StatusTag.FAILED);
                        logger.writeLine("[resolution] for a base issuer there can not be duplicated Mids");
                        baseMerchData.close();
                        return false;
                    }
                }
            }





            baseMerchData.close();
        }

        return true;
    }

    private boolean checkBaseMerchantDataWithsubMerchantData()
    {
        for(IssuerHost issuerHost : issuerHostsList)
        {
            int baseIssuer = issuerHost.baseIssuer;
            String quary = "SELECT * FROM TMIF,MIT WHERE IssuerNumber = " + baseIssuer + " AND TMIF.MerchantNumber = MIT.MerchantNumber";

            Cursor baseMerchantData = null;

            try
            {
                baseMerchantData = helperSync.readWithCustomQuary(quary);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                return false;
            }

            //load the next sub merchant data and compare whether they have the same data
            for (Integer i : issuerHost.issrList)
            {
                if (i == baseIssuer)
                    continue;

                String subQuary = "SELECT * FROM TMIF,MIT WHERE IssuerNumber = " + i + " AND TMIF.MerchantNumber = MIT.MerchantNumber";
                Cursor subMerchantData = null;

                try
                {
                    subMerchantData = helperSync.readWithCustomQuary(subQuary);
                }catch (Exception ex)
                {
                    ex.printStackTrace();
                    return false;
                }

                //compare the records
               int recId = 0 ;
                if ( (recId = compareRecordCursors(baseMerchantData,subMerchantData)) > 0)
                {
                    logger.log("for host " + issuerHost.name + " sub merchant data mismatch between base issuer " + baseIssuer + " sub issuer " + i + " source rec id " + recId, Logger.StatusTag.FAILED);
                    logger.writeLine("[resolution] the bound issuer merchants too should have the same data as base issuer merchant data");
                    return false;
                }

                subMerchantData.close();

            }
        }
        return true;
    }

    private int compareRecordCursors(Cursor left, Cursor right)
    {
        if (left.getCount() != right.getCount())
            return -1;

        int colCount = left.getColumnCount();

        boolean changeDetected = false;

        while (left.moveToNext())
        {
            right.moveToNext();

            String leftData = "";
            String rightData= "";

            for (int i = 1; i < colCount; i++)
            {
                if (    left.getColumnName(i).equals("HostId") ||
                        left.getColumnName(i).equals("IssuerNumber") ||
                        left.getColumnName(i).equals("HostId") ||
                        left.getColumnName(i).equals("MerchantNumber") ||
                        left.getColumnName(i).equals("MerchantName") ||
                        left.getColumnName(i).equals("MerchantPassword") ||
                        left.getColumnName(i).equals("AutoSettTime") ||
                        left.getColumnName(i).equals("AutoSettDate") ||
                        left.getColumnName(i).equals("InvNumber") ||
                        left.getColumnName(i).equals("STAN") ||
                        left.getColumnName(i).equals("MustSettleFlag") ||
                        left.getColumnName(i).equals("BatchNumber")

                )
                    continue;
                else
                {
                    leftData  = left.getString(i);
                    rightData = right.getString(i);
                }


                if (!leftData.equals(rightData))
                {
                    logger.log("Data miss matching occurs in column " + left.getColumnName(i), Logger.StatusTag.FAILED);
                    return Integer.valueOf(left.getInt(0));
                }

            }

            right.moveToNext();
        }


        return 0;

    }

    private boolean checkBaseoIssuerMerchantsCounts()
    {
        for (IssuerHost issuerHost : issuerHostsList)
        {
            int baseIssuer = issuerHost.baseIssuer;

            //load the merchants for the base issuer
            String quary = "SELECT IssuerNumber FROM TMIF,MIT WHERE IssuerNumber = " + baseIssuer + " AND TMIF.MerchantNumber = MIT.MerchantNumber";
            Cursor issuers = null;

            try
            {
                issuers = helperSync.readWithCustomQuary(quary);

            }catch (Exception ex)
            {
                ex.printStackTrace();
                return false;
            }

            if (issuers == null || issuers.getCount()  == 0)
            {
                issuers.close();
                logger.log("For the host " + issuerHost.name + " for base issuer , No merchants has been set", Logger.StatusTag.FAILED);
                return false;
            }


            issuers.close();
            int baseMerchnantCount = issuers.getCount();

            //check if other issuers have the same count
            for (Integer i : issuerHost.issrList)
            {
                if (i == baseIssuer)
                    continue;

                //load the other merchants
                String subQuary = "SELECT IssuerNumber FROM TMIF,MIT WHERE IssuerNumber = " + i + " AND TMIF.MerchantNumber = MIT.MerchantNumber";
                Cursor subMerchs = null;

                try
                {
                    subMerchs = helperSync.readWithCustomQuary(subQuary);
                }catch (Exception ex)
                {
                    ex.printStackTrace();
                    return false;
                }

                if (subMerchs == null)
                {
                    logger.log("The sub issuer = " + i + " of host " + issuerHost.name  + " has no merchants assigned", Logger.StatusTag.FAILED);
                    return false;
                }

                if (subMerchs.getCount() != baseMerchnantCount)
                {
                    subMerchs.close();
                    logger.log("The issuer " + i + " should have equal number of merchants assigned as base issuer of the host " + issuerHost.name + " base issuer has " + baseMerchnantCount + " but sub issuer has " + subMerchs.getCount() , Logger.StatusTag.FAILED);
                    logger.writeLine("[resolution] if you have n number of merchants for the base issuer you need to define same number of merchants for other bound issuers too");
                    return false;
                }
            }
        }

        return true;
    }


    private boolean checkIssuersRepeatedInOtherHosts()
    {
        int count = issuerHostsList.size();

        if (count == 1)
            return true;

        for (int ind = 0 ; ind < count; ind++)
        {
            IssuerHost issuerHost = issuerHostsList.get(ind);
            IssuerHost nextHost;

            if (ind + 1 < count)
                nextHost = issuerHostsList.get(ind + 1);
            else
                return true;

            //compare with the next list
            for (Integer i : issuerHost.issrList)
            {
                for (Integer x : nextHost.issrList)
                {
                    if (i == x)
                    {
                        logger.log("The issuer " + i + "in host " + issuerHost.name + " is repeated in host " + nextHost.name , Logger.StatusTag.FAILED);
                        logger.writeLine("an issuer can not be bind to two hosts. remove invalid bindings");
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean isBaseisSetCorrect()
    {
        for (IssuerHost issuerHost : issuerHostsList)
        {
            if (!isContainedIn(issuerHost.issrList,issuerHost.baseIssuer))
            {
                logger.log("The base issuer should be one of the issuer in the defined list " + "source --> Host Name " + issuerHost.name , Logger.StatusTag.FAILED);
                logger.writeLine("[resolution] define of the issuer as the base issuer from the issuer list");
                return false;
            }
        }

        return true;
    }

    private boolean isIssuersDuplicated ()
    {
        for (IssuerHost issuerHost : issuerHostsList)
        {
            for  (Integer i : issuerHost.issrList)
            {
                if (isDuplicated(issuerHost.issrList,i))
                {
                    logger.log("A host can not have duplicated issuers " + "source --> Host Name = "  + issuerHost.name  , Logger.StatusTag.FAILED);
                    logger.writeLine("[resolution] check for the host's issuer list and remove duplicates");
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isContainedIn(List<Integer> intList, int comp)
    {
        for (Integer i : intList)
        {
            if (i == comp)
                return true;
        }

        return false;
    }


    private boolean isDuplicated(List<Integer> intList, int comp)
    {
        int duplCount = 0 ;

        for (Integer i : intList)
        {
            if (i == comp)
                duplCount++;

            if (duplCount == 2)
                return true;
        }

        return false;
    }

    private boolean loadIssuers()
    {
        String quary = "SELECT * FROM IHT";

        Cursor issuers = null;
        try {
            issuers = helperSync.readWithCustomQuary(quary);
            if (issuers == null || issuers.getCount() == 0)
            {
                logger.log("Issuer host table has no records", Logger.StatusTag.FAILED);
                return false;
            }
        }catch (Exception ex)
        {
            ex.printStackTrace();
            logger.ex(ex);
            return false;
        }


        while (issuers.moveToNext())
        {
            String name = issuers.getString(issuers.getColumnIndex("HostName"));
            String list = issuers.getString(issuers.getColumnIndex("IssuerList"));
            int  bIssuer = issuers.getInt(issuers.getColumnIndex("BaseIssuer"));

            IssuerHost is = new IssuerHost();
            try
            {
                is.setIssuer(name,list,bIssuer);
                issuerHostsList.add(is);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                logger.ex(ex);
                return false;
            }

        }

        //there are valid records
        issuers.close();

        return true;
    }
}
