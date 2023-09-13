package com.harshana.wposandroiposapp.Settings.ProfileVerifier;


import java.util.HashMap;
import java.util.Map;

public class RuleSet
{
    public Map<Integer,Boolean> ruleStore;

    public static final int RULE_ISSUER_HOST_LOADING = 1;
    public static final int RULE_CHECKING_DUPLICATED_ISSUERS = 2;
    public static final int RULE_CHECKING_BASE_ISSUER_SETTING = 3;
    public static final int RULE_CHECKING_REPEATED_ISSUERS_IN_OTHER_HOSTS = 4;
    public static final int RULE_CHECKING_BASE_ISSUER_MERCHANT_COUNT_WITH_LIST = 5;
    public static final int RULE_CHECKING_BASE_MERCHANT_DUPL_TID_MID = 6;
    public static final int RULE_CHECKING_BASE_ISSUER_DATA_WITH_OTHER_MERCH = 7;
    public static final int RULE_CHECKING_VALID_ISSUERS = 8;


    public RuleSet()
    {
        ruleStore = new HashMap<>();

        ruleStore.put(RULE_ISSUER_HOST_LOADING,false);
        ruleStore.put(RULE_CHECKING_DUPLICATED_ISSUERS,false);
        ruleStore.put(RULE_CHECKING_BASE_ISSUER_SETTING,false);
        ruleStore.put(RULE_CHECKING_REPEATED_ISSUERS_IN_OTHER_HOSTS,false);
        ruleStore.put(RULE_CHECKING_BASE_ISSUER_MERCHANT_COUNT_WITH_LIST,false);
        ruleStore.put(RULE_CHECKING_BASE_MERCHANT_DUPL_TID_MID,false);
        ruleStore.put(RULE_CHECKING_BASE_ISSUER_DATA_WITH_OTHER_MERCH,false);
        ruleStore.put(RULE_CHECKING_VALID_ISSUERS,false);
    }

    public void setRuleState(int Rule)
    {
        ruleStore.put(Rule,true);
    }
}
