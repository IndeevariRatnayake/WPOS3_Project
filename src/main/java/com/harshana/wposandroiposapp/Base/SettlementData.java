package com.harshana.wposandroiposapp.Base;

public class SettlementData {
    public String Date,Time,BatchNumber,MerchantID,TerminalID;
    public int HostID,merchantNumber;
    public int VisaCount = 0, MasterCount = 0, CupCount = 0, CardTotals = 0, VoidCount = 0;
    public long VisaAmount, MasterAmount, CupAmount, SubTotal, VoidAmount;
}
