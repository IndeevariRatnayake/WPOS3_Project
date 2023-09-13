package com.harshana.wposandroiposapp.Print;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.RemoteException;
import android.util.Log;

import com.harshana.wposandroiposapp.Base.Base;
import com.harshana.wposandroiposapp.Base.GlobalWait;
import com.harshana.wposandroiposapp.Base.HostIssuer;
import com.harshana.wposandroiposapp.Base.IssuerHostMap;
import com.harshana.wposandroiposapp.Base.QRTran;
import com.harshana.wposandroiposapp.Base.Services;
import com.harshana.wposandroiposapp.Base.SettlementData;
import com.harshana.wposandroiposapp.Base.Transaction;
import com.harshana.wposandroiposapp.Database.Card;
import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.Database.Issuer;
import com.harshana.wposandroiposapp.DevArea.GlobalData;
import com.harshana.wposandroiposapp.DevArea.TranStaticData;
import com.harshana.wposandroiposapp.MainActivity;
import com.harshana.wposandroiposapp.QRIntegration.QRDisplay;
import com.harshana.wposandroiposapp.QRIntegration.QRVerify;
import com.harshana.wposandroiposapp.Settings.SettingsInterpreter;
import com.harshana.wposandroiposapp.TransactionMode.Emv;
import com.harshana.wposandroiposapp.UI.BatchTrans.VoidActivity;
import com.harshana.wposandroiposapp.UI.OtherTrans.PreComp;
import com.harshana.wposandroiposapp.UI.Reports.AnyReceiptActivity;
import com.harshana.wposandroiposapp.UI.Reports.AnyReceiptQRActivity;
import com.harshana.wposandroiposapp.UI.Reports.ReportActivity;
import com.harshana.wposandroiposapp.UI.Utils.ForceReversals;
import com.harshana.wposandroiposapp.Utilities.Formatter;
import com.harshana.wposandroiposapp.Utilities.Utility;
import com.itextpdf.text.BaseColor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;

import wangpos.sdk4.libbasebinder.Printer;

public class Receipt extends Base {
    private static Receipt myInstance = null;
    private float lineSpace = 1.0f;

    public static Receipt getInstance() {
        if (myInstance == null)
            myInstance =  new Receipt();

        return myInstance;
    }

    public void putBlankLine() throws RemoteException {
        Services.printer.printStringExt(" ",0,0f,lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,false, false, false);
    }

    private static String logoName = "";

    public static void setLogoImageName(String logo) {
        logoName = logo;
    }

    public void printDiagnosticReportEMV() {
        //check whether the file is exist
        File pFile = appContext.getFilesDir();
        String path = pFile.getPath();
        path += "/Secured";

        File emvFileDir  =  new File(path);
        if (!emvFileDir.exists()) {
            showToast("No Transaction Data to Print",TOAST_TYPE_WARNING);
            errorTone();
            return;
        }

        //attempt to open the file
        File emvFile = new File(path, Emv.EMV_TAG_FILE);

        try {
            if(bankCard != null)
                bankCard.breakOffCommand();
            Services.printer.printInit();
            Services.printer.clearPrintDataCache();
            checkPrinterStatus();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        try {
            Services.printer.printString("EMV DIAG REPORT",25, Printer.Align.CENTER,true,false);
            Services.printer.printString("------------------------------------------------------",20, Printer.Align.CENTER,true,false);

            BufferedReader bufferedReader = new BufferedReader(new FileReader(emvFile));
            String line = "";

            while ( (line = bufferedReader.readLine()) != null) {
                //tokenize the line
                int start = 0 ;
                int index = line.indexOf("|");
                String tagName = line.substring(0,index);

                start = ++index;
                index = line.indexOf("|",start);
                String tagDesc  = line.substring(start,index);

                start = ++index;
                String tagData = line.substring(start);

                Services.printer.print2StringInLine(tagDesc + "[" + tagName + "]" ,tagData ,lineSpace,Printer.Font.MONOSPACE,16, Printer.Align.LEFT,true, false,false);

            }

            feedPaper(60);
            Services.printer.printFinish();
            checkPrinterStatus();
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
    }

    public void checkPrinterStatusRecpt() {
        int[] status = new int[1];
        int ret = -1;
        try {
            ret = Services.printer.getPrinterStatus(status);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        // 00 The printer is normal
        // 0x01：Parameter error
        // 0x06：Not executable
        // 0x8A：out of paper,
        // 0x8B：overheat
        if(ret == 0) {
            switch (status[0]) {
                case 00:
                    break;
                case 0x01:
                    showToast("Parameter Error", TOAST_TYPE_INFO);
                    break;
                case 0x06:
                    showToast("Not Executable", TOAST_TYPE_INFO);
                    break;
                case 0x8A:
                    do {
                        showToast("Out of Paper", TOAST_TYPE_INFO);
                        errorTone();
                        try {
                            Services.printer.getPrinterStatus(status);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    } while(status[0] == 0x8A);

                    break;
                case 0x8B:
                    showToast("Overheat", TOAST_TYPE_INFO);
                    break;
            }
        }
    }

    public void checkPrinterStatus() {
        int[] status = new int[1];
        int ret = -1;
        try {
            ret = Services.printer.getPrinterStatus(status);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        // 00 The printer is normal
        // 0x01：Parameter error
        // 0x06：Not executable
        // 0x8A：out of paper,
        // 0x8B：overheat
        if(ret == 0) {
            switch (status[0]) {
                case 00:
                    break;
                case 0x01:
                    showToast("Parameter Error", TOAST_TYPE_INFO);
                    break;
                case 0x06:
                    showToast("Not Executable", TOAST_TYPE_INFO);
                    break;
                case 0x8A:
                    showToast("Out of Paper", TOAST_TYPE_INFO);
                    break;
                case 0x8B:
                    showToast("Overheat", TOAST_TYPE_INFO);
                    break;
            }
        }
    }

    //this prints the detail report based on the selected issuer and merchant
    public void printDetailReport(int issuerNumber, int merchantNumber) {
        try {
            //select the required set of transactions
            String selectQuary = "SELECT * FROM TXN WHERE MerchantNumber = " + merchantNumber + " AND TransactionCode != " + TranStaticData.TranTypes.PRE_AUTH;

            Cursor report = transactionDatabase.readWithCustomQuary(selectQuary);
            if (report == null)
                return;

            if (report.getCount() == 0) {
                showToast("No Transactions Found for The Selected Merchant",TOAST_TYPE_INFO);
                return;
            }

            String currencySymbol = configDatabase.getCurrency(merchantNumber);

            if(bankCard != null)
                bankCard.breakOffCommand();

            Services.printer.printInit();
            Services.printer.clearPrintDataCache();
            checkPrinterStatus();

            printImage(logoName,350,130);

            MerchantTerminal merchantTerminal = loadMerchantDetails(merchantNumber);
            if (merchantTerminal.merchName != null)
                Services.printer.printStringExt(merchantTerminal.merchName, 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 17, Printer.Align.CENTER, true, false, false);

            if (merchantTerminal.addrLine1 != null)
                Services.printer.printStringExt(merchantTerminal.addrLine1,0,0f,lineSpace, Printer.Font.SANS_SERIF,17, Printer.Align.CENTER,true, false, false);

            if (merchantTerminal.addrLine2 != null)
                Services.printer.printStringExt(merchantTerminal.addrLine2,0,0f,lineSpace, Printer.Font.SANS_SERIF,17, Printer.Align.CENTER,true, false, false);

            HostIssuer host = IssuerHostMap.hosts[issuerNumber];

            int fontSize = 20;
            Services.printer.setPrintLineSpacing(0);

            //print terminal related data
            Services.printer.print2StringInLine("DATE/TIME   ", Utility.getCurrentDateReceipt() + " " + Utility.getTimeFormatted(Formatter.getCurrentTimeFormatted()),lineSpace, Printer.Font.SANS_SERIF,fontSize, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("MERCHANT ID ", merchantTerminal.merchantID,lineSpace, Printer.Font.SANS_SERIF,fontSize, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("TERMINAL ID ", merchantTerminal.terminalID,lineSpace, Printer.Font.SANS_SERIF,fontSize, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("BATCH NO    ", Formatter.formatForSixDigits(Integer.valueOf(merchantTerminal.batchNumber)),lineSpace, Printer.Font.SANS_SERIF,fontSize, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("ISSUER      ", host.hostName,lineSpace, Printer.Font.SANS_SERIF,fontSize, Printer.Align.LEFT,true, false, false);

            putBlankLine();
            Services.printer.printString("DETAIL REPORT",25, Printer.Align.CENTER,true,false);

            //Print the report header here
            Services.printer.print2StringInLine("CARD NAME","CARD NUMBER" ,lineSpace,Printer.Font.MONOSPACE,fontSize, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine("APPROVE CODE","INVOICE NUMBER" ,lineSpace,Printer.Font.MONOSPACE,fontSize, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine("TXN DATE ","TXN TIME" ,lineSpace,Printer.Font.MONOSPACE,fontSize, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine("TRANSACTION","AMOUNT" ,lineSpace,Printer.Font.MONOSPACE,fontSize, Printer.Align.LEFT,false, false,false);

            Services.printer.printString("------------------------------------------------------",fontSize, Printer.Align.CENTER,true,false);

            String issuerName, tranName, strAmount, transactionDate, approvalCode, cardNumber, transactionTime;
            int invoiceNumber, numberOfTransaction = 0, sale_count = 0, offline_sale_count = 0, void_sale_count = 0, void_offline_sale_count = 0, fontSize2, refund_count = 0;
            long amount, sale_total_amount = 0, offline_sale_total_amount = 0, void_sale_total_amount = 0, void_offline_sale_total_amount = 0, refund_total_amount = 0;
            boolean isBold;

            report.moveToFirst();
            do {
                currentTransaction = transactionDatabase.getTransaction(report);
                if (currentTransaction == null)
                    return;

                loadCardAndIssuerSettlement();

                issuerName = currentTransaction.issuerData.issuerLabel;
                transactionDate = currentTransaction.receiptDate;
                approvalCode = currentTransaction.approveCode;

                cardNumber = currentTransaction.PAN;
                invoiceNumber = currentTransaction.inInvoiceNumber;
                transactionTime = currentTransaction.Time;
                amount = currentTransaction.lnBaseTransactionAmount;

                tranName = TranStaticData.getTranName(currentTransaction.inTransactionCode);
                strAmount = Formatter.formatAmount(amount, currencySymbol);

                isBold = false;
                fontSize2 = 17;

                if (currentTransaction.isVoided == 1) {
                    tranName = "Void " + tranName;
                    strAmount = "-" + strAmount;
                    isBold = true;
                    fontSize2 = 18;
                }

                //print the record
                Services.printer.print2StringInLine(issuerName,Formatter.maskPan(cardNumber,"****NNNN****NNNN", '*') ,lineSpace,Printer.Font.SANS_SERIF,fontSize2, Printer.Align.LEFT,isBold, false,false);
                Services.printer.print2StringInLine(approvalCode,Formatter.formatForSixDigits(invoiceNumber) ,lineSpace,Printer.Font.SANS_SERIF,fontSize2, Printer.Align.LEFT,isBold, false,false);
                Services.printer.print2StringInLine(transactionDate,Utility.getTimeFormatted(transactionTime) ,lineSpace,Printer.Font.SANS_SERIF,fontSize2, Printer.Align.LEFT,isBold, false,false);
                Services.printer.print2StringInLine(tranName,strAmount ,lineSpace,Printer.Font.SANS_SERIF,fontSize2, Printer.Align.LEFT,isBold, false,false);

                putBlankLine();

                //calculate the sale amount
                if (currentTransaction.isVoided == 0) {
                    if (currentTransaction.inTransactionCode == TranStaticData.TranTypes.SALE) {
                        sale_total_amount += currentTransaction.lnBaseTransactionAmount;
                        sale_count++;
                    }
                    else if (currentTransaction.inTransactionCode == TranStaticData.TranTypes.PRE_COMP) {
                        offline_sale_total_amount += currentTransaction.lnBaseTransactionAmount;
                        offline_sale_count++;
                    }
                    else if (currentTransaction.inTransactionCode == TranStaticData.TranTypes.REFUND) {
                        refund_total_amount += currentTransaction.lnBaseTransactionAmount;
                        refund_count++;
                    }
                }
                else {
                    if (currentTransaction.inTransactionCode == TranStaticData.TranTypes.SALE) {
                        void_sale_total_amount += currentTransaction.lnBaseTransactionAmount;
                        void_sale_count++;
                    } else if (currentTransaction.inTransactionCode == TranStaticData.TranTypes.PRE_COMP) {
                        void_offline_sale_total_amount += currentTransaction.lnBaseTransactionAmount;
                        void_offline_sale_count++;
                    }
                }

                numberOfTransaction++;

                if (numberOfTransaction % 8 == 0) {
                    feedPaper(0);
                }
            }
            while (report.moveToNext());

            Services.printer.printString("Summary",25, Printer.Align.CENTER,true,false);

            Services.printer.print2StringInLine("            COUNT   ", "AMOUNT",lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("SALE     " +  Formatter.fillInFront(" ",Integer.toString(sale_count),6) + "  " , Formatter.formatAmount(sale_total_amount,currencySymbol),lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("PRE COMP " +  Formatter.fillInFront(" ",Integer.toString(offline_sale_count),6) + "  " , Formatter.formatAmount(offline_sale_total_amount,currencySymbol),lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("V SALE   " +  Formatter.fillInFront(" ",Integer.toString(void_sale_count),6) + "  " , Formatter.formatAmount(void_sale_total_amount,currencySymbol),lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("V PRECOMP" +  Formatter.fillInFront(" ",Integer.toString(void_offline_sale_count),6) + "  " , Formatter.formatAmount(void_offline_sale_total_amount,currencySymbol),lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("REFUND   " +  Formatter.fillInFront(" ",Integer.toString(refund_count),6)  + "  " , Formatter.formatAmount(refund_total_amount,currencySymbol),lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.printStringExt("------------------------------",0,0f,lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);

            //We need to feed the paper little bit after printing is done so  the user an see the paper well
            feedPaper(60);
            Services.printer.printFinish();
            checkPrinterStatus();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void printDetailReportQR() {
        try {
            //select the required set of transactions
            String selectQuary = "SELECT * FROM QRBatch";

            Cursor report = transactionDatabase.readWithCustomQuary(selectQuary);
            if (report == null)
                return;

            if (report.getCount() == 0) {
                showToast("No QR Transactions Found",TOAST_TYPE_INFO);
                return;
            }

            Services.printer.printInit();
            Services.printer.clearPrintDataCache();
            checkPrinterStatus();

            printImage(logoName,350,130);
            int fontSize1 = 20;

            Services.printer.setPrintLineSpacing(0);
            //print terminal related data
            Services.printer.print2StringInLine("DATE/TIME   ", ": " + Utility.getCurrentDateReceipt() + " " + Utility.getTimeFormatted(Formatter.getCurrentTimeFormatted()),lineSpace, Printer.Font.SANS_SERIF,fontSize1, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("MERCHANT NAME ", ": " + "Wpos QR-Suntech",lineSpace, Printer.Font.SANS_SERIF,fontSize1, Printer.Align.LEFT,true, false, false);

            putBlankLine();
            Services.printer.printString("DETAIL REPORT",25, Printer.Align.CENTER,true,false);

            //Print the report header here
            Services.printer.print2StringInLine("TXN DATE ","TXN TIME" ,lineSpace,Printer.Font.MONOSPACE,fontSize1, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine(" ","CARD NUMBER" ,lineSpace,Printer.Font.MONOSPACE,fontSize1, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine("REFLABLE","AMOUNT" ,lineSpace,Printer.Font.MONOSPACE,fontSize1, Printer.Align.LEFT,false, false,false);

            Services.printer.printString("------------------------------------------------------",fontSize1, Printer.Align.CENTER,true,false);

            String transactionDate;
            String transactionTime;
            String PAN;
            String refLable;
            long amount;

            long qrsale_total_amount = 0 ;

            int qrsale_count = 0;

            QRTran qrTran;

            report.moveToFirst();
            do {
                qrTran = transactionDatabase.getQRTranfromBatch(report);
                if (qrTran == null)
                    return;

                transactionDate = qrTran.Date;
                transactionDate = Utility.getDateFormatted(transactionDate.substring(2));
                refLable = Formatter.removeleadingZeros(qrTran.refQRLabel);
                transactionTime = qrTran.Time;
                amount = qrTran.tranQRAmount;
                PAN = qrTran.PAN;

                String strAmount = Formatter.formatAmount(amount, "LKR");

                boolean isBold = false;
                int fontSize = 17;

                //print the record
                Services.printer.print2StringInLine(transactionDate,transactionTime ,lineSpace,Printer.Font.SANS_SERIF,fontSize, Printer.Align.LEFT,isBold, false,false);
                Services.printer.print2StringInLine(" ",PAN ,lineSpace,Printer.Font.SANS_SERIF,fontSize, Printer.Align.LEFT,isBold, false,false);
                Services.printer.print2StringInLine(refLable,strAmount ,lineSpace,Printer.Font.SANS_SERIF,fontSize, Printer.Align.LEFT,isBold, false,false);

                putBlankLine();

                //calculate the sale amount
                qrsale_total_amount += qrTran.tranQRAmount;
                qrsale_count++;
            }
            while (report.moveToNext());

            Services.printer.printString("Summary",25, Printer.Align.CENTER,true,false);

            Services.printer.printStringExt("------------------------------",0,0f,lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.printStringExt("                COUNT   AMOUNT",0,0f,lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.printStringExt("[QR_SALE]       " +  Formatter.fillInFront(" ",Integer.toString(qrsale_count),6) + "    " + Formatter.formatAmount(qrsale_total_amount,"LKR"),0,0f,lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.printStringExt("------------------------------",0,0f,lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);

            //We need to feed the paper little bit after printing is done so  the user an see the paper well
            feedPaper(60);
            Services.printer.printFinish();
            checkPrinterStatus();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public  void printDetailReportForSettlement(int hostSelected, int baseMerchantSelected) {
        try {
            //select the required set of transactions
            String selectQuary  = "SELECT * FROM TXN WHERE MerchantNumber = " + baseMerchantSelected + " AND TransactionCode != " + TranStaticData.TranTypes.PRE_AUTH;
            Cursor report = transactionDatabase.readWithCustomQuary(selectQuary);
            if (report == null)
                return;

            if (report.getCount() == 0) {
                showToast("No Transactions Found for The Selected Merchant",TOAST_TYPE_INFO);
                return;
            }

            String currencySymbol = configDatabase.getCurrency(baseMerchantSelected);
            if(bankCard != null)
                bankCard.breakOffCommand();
            Services.printer.printInit();
            Services.printer.clearPrintDataCache();
            checkPrinterStatus();

            printImage(logoName,350,130);
            int fontSize = 20;
            Services.printer.setPrintLineSpacing(0);

            MerchantTerminal merchantTerminal = loadMerchantDetails(baseMerchantSelected);
            if (merchantTerminal.merchName != null)
                Services.printer.printStringExt(merchantTerminal.merchName, 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 17, Printer.Align.CENTER, true, false, false);

            if (merchantTerminal.addrLine1 != null)
                Services.printer.printStringExt(merchantTerminal.addrLine1,0,0f,lineSpace, Printer.Font.SANS_SERIF,17, Printer.Align.CENTER,true, false, false);

            if (merchantTerminal.addrLine2 != null)
                Services.printer.printStringExt(merchantTerminal.addrLine2,0,0f,lineSpace, Printer.Font.SANS_SERIF,17, Printer.Align.CENTER,true, false, false);

            //print terminal related data
            Services.printer.print2StringInLine("DATE/TIME   ", Utility.getCurrentDateReceipt() + " " + Utility.getTimeFormatted(Formatter.getCurrentTimeFormatted()),lineSpace, Printer.Font.SANS_SERIF,fontSize, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("MERCHANT ID ", merchantTerminal.merchantID,lineSpace, Printer.Font.SANS_SERIF,fontSize, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("TERMINAL ID ", merchantTerminal.terminalID,lineSpace, Printer.Font.SANS_SERIF,fontSize, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("BATCH NO    ", Formatter.formatForSixDigits(Integer.valueOf(merchantTerminal.batchNumber)),lineSpace, Printer.Font.SANS_SERIF,fontSize, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("HOST        ", IssuerHostMap.hosts[hostSelected].hostName,lineSpace, Printer.Font.SANS_SERIF,fontSize, Printer.Align.LEFT,true, false, false);

            putBlankLine();
            Services.printer.printString("DETAIL REPORT",25, Printer.Align.CENTER,true,false);

            //Print the report header here
            Services.printer.print2StringInLine("CARD NAME","CARD NUMBER" ,lineSpace,Printer.Font.MONOSPACE,fontSize, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine("APPROVE CODE","INVOICE NUMBER" ,lineSpace,Printer.Font.MONOSPACE,fontSize, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine("TXN DATE ","TXN TIME" ,lineSpace,Printer.Font.MONOSPACE,fontSize, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine("TRANSACTION","AMOUNT" ,lineSpace,Printer.Font.MONOSPACE,fontSize, Printer.Align.LEFT,false, false,false);

            Services.printer.printString("------------------------------------------------------",fontSize, Printer.Align.CENTER,true,false);

            String issuerName, transactionDate, approvalCode, cardNumber, transactionTime, tranName, strAmount;
            int invoiceNumber, numberOfTransaction = 0, sale_count = 0, offline_sale_count = 0, void_sale_count = 0, void_offline_sale_count = 0, refund_count = 0, fontSize2;
            long amount, sale_total_amount = 0, offline_sale_total_amount = 0, void_sale_total_amount = 0, void_offline_sale_total_amount = 0, refund_total_amount = 0;
            boolean isBold;

            report.moveToFirst();

            do {
                currentTransaction = transactionDatabase.getTransaction(report);
                if (currentTransaction == null)
                    return;

                loadCardAndIssuerSettlement();

                issuerName = currentTransaction.issuerData.issuerLabel;
                transactionDate = currentTransaction.receiptDate;
                approvalCode = currentTransaction.approveCode;

                cardNumber = currentTransaction.PAN;
                invoiceNumber = currentTransaction.inInvoiceNumber;
                transactionTime = currentTransaction.Time;
                amount = currentTransaction.lnBaseTransactionAmount;

                tranName = TranStaticData.getTranName(currentTransaction.inTransactionCode);
                strAmount = Formatter.formatAmount(amount, currencySymbol);

                fontSize2 = 17;
                isBold = false;

                if (currentTransaction.isVoided == 1) {
                    tranName = "Void " + tranName;
                    strAmount = "-" + strAmount;
                    isBold = true;
                    fontSize2 = 18;
                }

                //print the record
                Services.printer.print2StringInLine(issuerName,Formatter.maskPan(cardNumber,"****NNNN****NNNN", '*') ,lineSpace,Printer.Font.SANS_SERIF,fontSize2, Printer.Align.LEFT,isBold, false,false);
                Services.printer.print2StringInLine(approvalCode,Formatter.formatForSixDigits(invoiceNumber) ,lineSpace,Printer.Font.SANS_SERIF,fontSize2, Printer.Align.LEFT,isBold, false,false);
                Services.printer.print2StringInLine(transactionDate,Utility.getTimeFormatted(transactionTime) ,lineSpace,Printer.Font.SANS_SERIF,fontSize2, Printer.Align.LEFT,isBold, false,false);
                Services.printer.print2StringInLine(tranName,strAmount ,lineSpace,Printer.Font.SANS_SERIF,fontSize2, Printer.Align.LEFT,isBold, false,false);

                putBlankLine();

                //calculate the sale amount
                if (currentTransaction.isVoided == 0) {
                    if (currentTransaction.inTransactionCode == TranStaticData.TranTypes.SALE) {
                        sale_total_amount += currentTransaction.lnBaseTransactionAmount;
                        sale_count++;
                    } else if (currentTransaction.inTransactionCode == TranStaticData.TranTypes.PRE_COMP) {
                        offline_sale_total_amount += currentTransaction.lnBaseTransactionAmount;
                        offline_sale_count++;
                    } else if (currentTransaction.inTransactionCode == TranStaticData.TranTypes.REFUND) {
                        refund_total_amount += currentTransaction.lnBaseTransactionAmount;
                        refund_count++;
                    }
                }
                else {
                    if (currentTransaction.inTransactionCode == TranStaticData.TranTypes.SALE) {
                        void_sale_total_amount += currentTransaction.lnBaseTransactionAmount;
                        void_sale_count++;
                    } else if (currentTransaction.inTransactionCode == TranStaticData.TranTypes.PRE_COMP) {
                        void_offline_sale_total_amount += currentTransaction.lnBaseTransactionAmount;
                        void_offline_sale_count++;
                    }
                }

                numberOfTransaction++;

                if (numberOfTransaction % 8 == 0) {
                    feedPaper(0);
                }
            } while (report.moveToNext());

            Services.printer.printString("Summary",25, Printer.Align.CENTER,true,false);

            Services.printer.print2StringInLine("            COUNT  ","AMOUNT",lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("SALE     " +  Formatter.fillInFront(" ",Integer.toString(sale_count),6) + "  " , Formatter.formatAmount(sale_total_amount,currencySymbol),lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("PRE COMP " +  Formatter.fillInFront(" ",Integer.toString(offline_sale_count),6) + "  " , Formatter.formatAmount(offline_sale_total_amount,currencySymbol),lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("V SALE   " +  Formatter.fillInFront(" ",Integer.toString(void_sale_count),6) + "  " , Formatter.formatAmount(void_sale_total_amount,currencySymbol),lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("V PRECOMP" +  Formatter.fillInFront(" ",Integer.toString(void_offline_sale_count),6) + "  " , Formatter.formatAmount(void_offline_sale_total_amount,currencySymbol),lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("REFUND   " +  Formatter.fillInFront(" ",Integer.toString(refund_count),6)  + "  " , Formatter.formatAmount(refund_total_amount,currencySymbol),lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.printStringExt("------------------------------",0,0f,lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);

            GlobalData.SaleAmt = Formatter.formatAmount(sale_total_amount,currencySymbol);
            GlobalData.RefundAmt = Formatter.formatAmount(refund_total_amount,currencySymbol);
            //We need to feed the paper little bit after printing is done so  the user an see the paper well
            feedPaper(60);
            Services.printer.printFinish();
            checkPrinterStatus();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void printAnyReceipt(int transactionID, int isLast) {
        int count;
        Cursor issuerRec;
        String selectQuary = "SELECT * FROM TXN WHERE ID = " + transactionID;
        Cursor report = transactionDatabase.readWithCustomQuary(selectQuary);
        if (report == null) {
            return;
        }

        count = report.getCount();
        if (count == 0)    {
            showToast("No Transactions Found for The Selected Merchant",TOAST_TYPE_INFO);
            return;
        }

        report.moveToFirst();

        currentTransaction = transactionDatabase.getTransaction(report);
        if (currentTransaction == null) {
            return;
        }

        Cursor cardRec = null;

        //find the cardData record in the data base and the matching bin
        if ((cardRec = configDatabase.findCardRecord(currentTransaction.PAN)) == null)
            return;

        currentTransaction.cdtIndex = cardRec.getInt(cardRec.getColumnIndex("ID"));
        currentTransaction.cardData = new Card(cardRec);

        Cursor terminalRec = null;

        if((terminalRec = configDatabase.loadTerminal(currentTransaction.merchantNumber)) == null) {
            showToast("Error with terminal record loading",TOAST_TYPE_INFO);
            return;
        }

        currentTransaction.currencySymbol = configDatabase.getCurrency(currentTransaction.merchantNumber);

        GlobalData.addressLine1 = terminalRec.getString(terminalRec.getColumnIndex("RctHdr1"));
        GlobalData.addressLine2 = terminalRec.getString(terminalRec.getColumnIndex("RctHdr2"));
        GlobalData.addressLine3 = terminalRec.getString(terminalRec.getColumnIndex("RctHdr3"));
        GlobalData.merchantName = terminalRec.getString(terminalRec.getColumnIndex("MerchantName"));

        terminalRec.close();
        //load the issuerData record
        if ((issuerRec = configDatabase.loadIssuer(currentTransaction.issuerNumber)) == null) {
            showToast("Error with issuerData loading",TOAST_TYPE_INFO);
            return;
        }
        currentTransaction.issuerData = new Issuer(issuerRec);
        GlobalData.transactionName = TranStaticData.getTranName(currentTransaction.inTransactionCode);
        if(currentTransaction.isVoided == 1){
            GlobalData.transactionName = "VOID " + GlobalData.transactionName;
        }

        GlobalData.isDuplicate = 1;

        try {
            if(isLast == 1) {
                printReceipt(3);
            }
            else if(isLast == 2) {
                printReceipt(2);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void printAnyReceiptQR(int transactionID, int isLast) {
        int count;
        Cursor issuerRec;
        String selectQuary = "SELECT * FROM QRBatch WHERE ID = " + transactionID;
        Cursor report = transactionDatabase.readWithCustomQuary(selectQuary);
        if (report == null) {
            return;
        }

        count = report.getCount();
        if (count == 0)    {
            showToast("No Transactions Found for The Selected Merchant",TOAST_TYPE_INFO);
            return;
        }

        report.moveToFirst();

        QRTran qrTran = transactionDatabase.getQRTranfromBatch(report);
        if (qrTran == null) {
            return;
        }

        GlobalData.isDuplicate = 1;

        try {
            if(isLast == 1) {
                printReceiptQRBase(3, qrTran);
            }
            else if(isLast == 2) {
                printReceiptQRBase(2, qrTran);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    //this routine prints the summery report for the selected host and this will be the settlement report
    public  void printSummeryReport(int issuerNumber, int merchantNumber) {
        try {
            //select the required set of transactions
            String currencySymbol;
            String selectQuary = "SELECT * FROM TXN WHERE  MerchantNumber = " + merchantNumber + " AND TransactionCode != " + TranStaticData.TranTypes.PRE_AUTH;

            Cursor report = transactionDatabase.readWithCustomQuary(selectQuary);
            if (report == null)
                return;

            if (report.getCount() == 0) {
                showToast("No Transactions Found for The Selected Merchant",TOAST_TYPE_INFO);
                return;
            }

            MerchantTerminal merchantTerminal = loadMerchantDetails(merchantNumber);
            currencySymbol = configDatabase.getCurrency(merchantNumber);

            if(bankCard != null)
                bankCard.breakOffCommand();
            Services.printer.printInit();
            Services.printer.clearPrintDataCache();
            checkPrinterStatus();
            Services.printer.setPrintLineSpacing(0);

            printImage(logoName,350,130);

            //print the address line
            if (merchantTerminal.merchName != null)
                Services.printer.printStringExt(merchantTerminal.merchName, 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 17, Printer.Align.CENTER, true, false, false);

            if (merchantTerminal.addrLine1 != null)
                Services.printer.printStringExt(merchantTerminal.addrLine1,0,0f,lineSpace, Printer.Font.SANS_SERIF,17, Printer.Align.CENTER,true, false, false);

            if (merchantTerminal.addrLine2 != null)
                Services.printer.printStringExt(merchantTerminal.addrLine2,0,0f,lineSpace, Printer.Font.SANS_SERIF,17, Printer.Align.CENTER,true, false, false);

            //print the header of the detail report here
            Services.printer.print2StringInLine("DATE/TIME",Utility.getCurrentDateReceipt() + " " + Utility.getTimeFormatted(Formatter.getCurrentTimeFormatted()) ,lineSpace,Printer.Font.SANS_SERIF,20, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine("MERCHANT ID  ",merchantTerminal.merchantID ,lineSpace,Printer.Font.SANS_SERIF,20, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine("TERMINAL ID  ",merchantTerminal.terminalID ,lineSpace,Printer.Font.SANS_SERIF,20, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine("BATCH NO     ",merchantTerminal.batchNumber ,lineSpace,Printer.Font.SANS_SERIF,20, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine("ISSUER         ",IssuerHostMap.hosts[issuerNumber].hostName ,lineSpace,Printer.Font.SANS_SERIF,20, Printer.Align.LEFT,false, false,false);


            String reportName = "";
            boolean isSettlement = false;

            if (  (currentTransaction != null) && ( (currentTransaction.inTransactionCode == TranStaticData.TranTypes.SETTLE) ||
                    (currentTransaction.inTransactionCode == TranStaticData.TranTypes.CLOSE_BATCH_UPLOAD)))
            {
                reportName = "SETTLEMENT";
                isSettlement = true;
            }
            else
                reportName = "SUMMARY REPORT";

            Services.printer.printStringExt(reportName,0,0f,lineSpace, Printer.Font.SANS_SERIF,22, Printer.Align.CENTER,true, false, false);

            Services.printer.printString("------------------------------------------------------",20,Printer.Align.CENTER,false,false);

            Services.printer.printStringExt("                     COUNT                        TOTAL ",0,0f,lineSpace, Printer.Font.SANS_SERIF,20, Printer.Align.LEFT,true, false, false);
            int card_totals = 0 ;
            long total_amount = 0 ;
            for(int i=0; i < IssuerHostMap.hosts[issuerNumber].issuerList.size() ; i++) {
                int issuer = IssuerHostMap.hosts[issuerNumber].issuerList.get(i);
                int count = transactionDatabase.getTranCountIssuer(issuer,merchantNumber);
                long amount = transactionDatabase.getTranAmountIssuer(issuer,merchantNumber);

                Issuer Actissuer = new Issuer(configDatabase.loadIssuer(issuer));

                Services.printer.print2StringInLine(Formatter.fillInBack(" ", Actissuer.issuerLabel, 8) + Formatter.fillInFront(" ",String.valueOf(count),3), Formatter.formatAmount(amount,currencySymbol),lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
                card_totals = card_totals + count;
                total_amount = total_amount + amount;
            }

            Services.printer.print2StringInLine("VOID    " + Formatter.fillInFront(" ", String.valueOf(transactionDatabase.getVoidTranCountMerch(merchantNumber)),3), Formatter.formatAmount(transactionDatabase.getVoidTranAmountMerch(merchantNumber),currencySymbol),lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("TOTALS  " + Formatter.fillInFront(" ", String.valueOf(card_totals),3), Formatter.formatAmount(total_amount,currencySymbol),lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.printStringExt("--------------------------------------------------------------",0,0f,lineSpace, Printer.Font.SANS_SERIF,18, Printer.Align.LEFT,true, false, false);

            putBlankLine();

            if (isSettlement)
                Services.printer.printStringExt("SETTLEMENT SUCCESSFUL " ,0,0f,lineSpace, Printer.Font.SANS_SERIF,20, Printer.Align.CENTER,true, false, false);

            feedPaper(60);
            Services.printer.printFinish();
            checkPrinterStatus();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public  void printSummeryReportQR() {
        try {
            //select the required set of transactions
            String selectQuary = "SELECT * FROM QRBatch";

            Cursor report = transactionDatabase.readWithCustomQuary(selectQuary);
            if (report == null)
                return;

            if (report.getCount() == 0)
            {
                showToast("No Transactions Found for The Selected Merchant",TOAST_TYPE_INFO);
                return;
            }

            Services.printer.printInit();
            Services.printer.clearPrintDataCache();
            checkPrinterStatus();
            Services.printer.setPrintLineSpacing(0);
            printImage(logoName,350,130);

            //print the address line
            if (GlobalData.addressLine1 != null)
                Services.printer.printStringExt("WPOS QR-SUNTECH",0,0f,lineSpace, Printer.Font.SANS_SERIF,18, Printer.Align.CENTER,true, false, false);
            if (GlobalData.addressLine2 != null)
                Services.printer.printStringExt("COLOMBO",0,0f,lineSpace, Printer.Font.SANS_SERIF,18, Printer.Align.CENTER,true, false, false);

            //print the header of the detail report here
            Services.printer.print2StringInLine("DATE/TIME  ", ": " + Utility.getCurrentDateReceipt() + " " + Utility.getTimeFormatted(Formatter.getCurrentTimeFormatted()) ,lineSpace,Printer.Font.MONOSPACE,20, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine("MERCHANT NAME   ", ": Wpos QR-Suntech", lineSpace, Printer.Font.MONOSPACE, 20, Printer.Align.LEFT, true, false, false);

            String reportName = "SUMMARY REPORT";
            boolean isSettlement = false;

            Services.printer.printStringExt(reportName,0,0f,lineSpace, Printer.Font.SANS_SERIF,20, Printer.Align.CENTER,true, false, false);

            Services.printer.printString("------------------------------------------------------",20,Printer.Align.CENTER,false,false);

            int qr_sale_count = 0;
            long qr_sale_total_amount = 0 ;

            report.moveToFirst();

            QRTran qrTran;

            do
            {
                qrTran = transactionDatabase.getQRTranfromBatch(report);
                if (qrTran == null)
                    return;

                qr_sale_count++;
                qr_sale_total_amount += qrTran.tranQRAmount;
            } while (report.moveToNext());

            Services.printer.printStringExt("--------------------------------------------------------------",0,0f,lineSpace, Printer.Font.SANS_SERIF,18, Printer.Align.LEFT,true, false, false);
            Services.printer.printStringExt("                                             COUNT      TOTAL ",0,0f,lineSpace, Printer.Font.SANS_SERIF,20, Printer.Align.LEFT,true, false, false);

            Services.printer.printStringExt("[QR TOTALS] " + Formatter.fillInFront(" ", String.valueOf(qr_sale_count),6) + "  " + Formatter.formatAmount(qr_sale_total_amount,"LKR"),0,0f,lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.printStringExt("--------------------------------------------------------------",0,0f,lineSpace, Printer.Font.SANS_SERIF,18, Printer.Align.LEFT,true, false, false);

            putBlankLine();

            feedPaper(60);
            Services.printer.printFinish();
            checkPrinterStatus();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    //this routine prints the summery report for the settlement and all
    public  void printReceiptSummeryForSettlement(int hostSelected, int baseMerchantSelected) {
        try {
            //select the required set of transactions
            String currencySymbol;
            String selectQuary  = "SELECT * FROM TXN WHERE MerchantNumber = " + baseMerchantSelected + " AND TransactionCode != " + TranStaticData.TranTypes.PRE_AUTH;
            Cursor report = transactionDatabase.readWithCustomQuary(selectQuary);
            if (report == null)
                return;

            if (report.getCount() == 0) {
                showToast("No Transactions Found for The Selected Merchant",TOAST_TYPE_INFO);
                return;
            }

            MerchantTerminal merchantTerminal = loadMerchantDetails(baseMerchantSelected);
            currencySymbol = configDatabase.getCurrency(baseMerchantSelected);

            if(bankCard != null)
                bankCard.breakOffCommand();
            Services.printer.printInit();
            Services.printer.clearPrintDataCache();
            checkPrinterStatus();
            Services.printer.setPrintLineSpacing(0);

            printImage(logoName,350,130);

            //print the address line
            GlobalData.settlementData = new SettlementData();

            if (merchantTerminal.merchName != null)
                Services.printer.printStringExt(merchantTerminal.merchName, 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 17, Printer.Align.CENTER, true, false, false);

            if (merchantTerminal.addrLine1 != null)
                Services.printer.printStringExt(merchantTerminal.addrLine1,0,0f,lineSpace, Printer.Font.SANS_SERIF,17, Printer.Align.CENTER,true, false, false);

            if (merchantTerminal.addrLine2 != null)
                Services.printer.printStringExt(merchantTerminal.addrLine2,0,0f,lineSpace, Printer.Font.SANS_SERIF,17, Printer.Align.CENTER,true, false, false);

            //print the header of the detail report here
            GlobalData.settlementData.Date = Utility.getCurrentDateReceipt();
            GlobalData.settlementData.Time = Utility.getTimeFormatted(Formatter.getCurrentTimeFormatted());
            GlobalData.settlementData.MerchantID = merchantTerminal.merchantID;
            GlobalData.settlementData.TerminalID = merchantTerminal.terminalID;
            GlobalData.settlementData.HostID = hostSelected;
            GlobalData.settlementData.BatchNumber = merchantTerminal.batchNumber;

            Services.printer.print2StringInLine("DATE/TIME",GlobalData.settlementData.Date + " " + GlobalData.settlementData.Time ,lineSpace,Printer.Font.SANS_SERIF,20, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine("MERCHANT ID  ",merchantTerminal.merchantID ,lineSpace,Printer.Font.SANS_SERIF,20, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine("TERMINAL ID  ",merchantTerminal.terminalID ,lineSpace,Printer.Font.SANS_SERIF,20, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine("BATCH NO     ",merchantTerminal.batchNumber ,lineSpace,Printer.Font.SANS_SERIF,20, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine("HOST         ",IssuerHostMap.hosts[hostSelected].hostName ,lineSpace,Printer.Font.SANS_SERIF,20, Printer.Align.LEFT,false, false,false);

            String reportName = "";
            boolean isSettlement = false;

            if ((currentTransaction.inTransactionCode == TranStaticData.TranTypes.SETTLE) ||
                    (currentTransaction.inTransactionCode == TranStaticData.TranTypes.CLOSE_BATCH_UPLOAD)) {
                reportName = "SETTLEMENT";
                isSettlement = true;
            }
            else
                reportName = "SUMMARY REPORT";

            Services.printer.printStringExt(reportName,0,0f,lineSpace, Printer.Font.SANS_SERIF,20, Printer.Align.CENTER,true, false, false);

            Services.printer.printString("------------------------------------------------------",20,Printer.Align.CENTER,false,false);

            Services.printer.printStringExt("                     COUNT                        TOTAL ",0,0f,lineSpace, Printer.Font.SANS_SERIF,20, Printer.Align.LEFT,true, false, false);
            int card_totals = 0 ;
            long total_amount = 0 ;
            for(int i=0; i < IssuerHostMap.hosts[hostSelected].issuerList.size() ; i++) {
                int issuer = IssuerHostMap.hosts[hostSelected].issuerList.get(i);
                int count = transactionDatabase.getTranCountIssuer(issuer,baseMerchantSelected);
                long amount = transactionDatabase.getTranAmountIssuer(issuer,baseMerchantSelected);

                Issuer Actissuer = new Issuer(configDatabase.loadIssuer(issuer));
                if(i == 0) {
                    GlobalData.settlementData.VisaAmount = amount;
                    GlobalData.settlementData.VisaCount = count;
                }
                else if (i == 1) {
                    GlobalData.settlementData.MasterAmount = amount;
                    GlobalData.settlementData.MasterCount = count;
                }
                else {
                    GlobalData.settlementData.CupAmount = amount;
                    GlobalData.settlementData.CupCount = count;
                }

                Services.printer.print2StringInLine(Formatter.fillInBack(" ", Actissuer.issuerLabel, 8) + Formatter.fillInFront(" ",String.valueOf(count),3), Formatter.formatAmount(amount,currencySymbol),lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
                card_totals = card_totals + count;
                total_amount = total_amount + amount;
            }

            GlobalData.settlementData.SubTotal = total_amount;
            GlobalData.settlementData.CardTotals = card_totals;
            GlobalData.settlementData.CardTotals = card_totals;
            GlobalData.settlementData.VoidCount = transactionDatabase.getVoidTranCountMerch(baseMerchantSelected);
            GlobalData.settlementData.VoidAmount = transactionDatabase.getVoidTranAmountMerch(baseMerchantSelected);

            Services.printer.print2StringInLine("VOID    " + Formatter.fillInFront(" ", String.valueOf(transactionDatabase.getVoidTranCountMerch(baseMerchantSelected)),3), Formatter.formatAmount(transactionDatabase.getVoidTranAmountMerch(baseMerchantSelected),currencySymbol),lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("TOTALS  " + Formatter.fillInFront(" ", String.valueOf(card_totals),3), Formatter.formatAmount(total_amount,currencySymbol),lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.printStringExt("--------------------------------------------------------------",0,0f,lineSpace, Printer.Font.SANS_SERIF,18, Printer.Align.LEFT,true, false, false);

            putBlankLine();

            if (isSettlement)
                Services.printer.printStringExt("SETTLEMENT SUCCESSFUL " ,0,0f,lineSpace, Printer.Font.SANS_SERIF,20, Printer.Align.CENTER,true, false, false);

            feedPaper(60);
            Services.printer.printFinish();
            checkPrinterStatus();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public  void printLastReciptSettlement(int recID) {
        try {
            //select the required set of transactions
            String LastSettlementQuery = "SELECT * FROM LASTSETTLEMENT WHERE ID = " + recID;
            Cursor lastSettleBatch = transactionDatabase.readWithCustomQuary(LastSettlementQuery);

            if (lastSettleBatch == null)
                return;

            if (lastSettleBatch.getCount() == 0) {
                showToast("No Transactions Found for The Selected Merchant",TOAST_TYPE_INFO);
                return;
            }

            lastSettleBatch.moveToFirst();

            SettlementData lastSettlement = transactionDatabase.getLastSettlement(lastSettleBatch);
            if (lastSettlement == null)    {
                return;
            }

            Cursor terminalRec = null;

            if((terminalRec = configDatabase.loadTerminal(lastSettlement.merchantNumber)) == null) {
                showToast("Error with terminal record loading",TOAST_TYPE_INFO);
                return;
            }

            String addressLine1 = terminalRec.getString(terminalRec.getColumnIndex("RctHdr1"));
            String addressLine2 = terminalRec.getString(terminalRec.getColumnIndex("RctHdr2"));
            String merchName = terminalRec.getString(terminalRec.getColumnIndex("MerchantName"));
            String currencySymbol = configDatabase.getCurrency(lastSettlement.merchantNumber);

            terminalRec.close();

            if(bankCard != null)
                bankCard.breakOffCommand();
            Services.printer.printInit();
            Services.printer.clearPrintDataCache();
            checkPrinterStatus();
            Services.printer.setPrintLineSpacing(0);
            printImage(logoName,350,130);

            //print the address line

            Services.printer.printStringExt(merchName,0,0f,lineSpace, Printer.Font.SANS_SERIF,16, Printer.Align.CENTER,true, false, false);
            Services.printer.printStringExt(addressLine1,0,0f,lineSpace, Printer.Font.SANS_SERIF,17, Printer.Align.CENTER,true, false, false);
            Services.printer.printStringExt(addressLine2,0,0f,lineSpace, Printer.Font.SANS_SERIF,17, Printer.Align.CENTER,true, false, false);

            //print the header of the detail report here
            Services.printer.print2StringInLine("DATE/TIME",lastSettlement.Date + " " + lastSettlement.Time ,lineSpace,Printer.Font.MONOSPACE,20, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine("MERCHANT ID  ",lastSettlement.MerchantID ,lineSpace,Printer.Font.MONOSPACE,20, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine("TERMINAL ID  ",lastSettlement.TerminalID ,lineSpace,Printer.Font.MONOSPACE,20, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine("BATCH NO     ",lastSettlement.BatchNumber ,lineSpace,Printer.Font.MONOSPACE,20, Printer.Align.LEFT,false, false,false);
            Services.printer.print2StringInLine("HOST         ",IssuerHostMap.hosts[lastSettlement.HostID].hostName ,lineSpace,Printer.Font.MONOSPACE,20, Printer.Align.LEFT,false, false,false);

            String reportName = "SETTLEMENT";

            Services.printer.printStringExt(reportName,0,0f,lineSpace, Printer.Font.SANS_SERIF,20, Printer.Align.CENTER,true, false, false);

            Services.printer.printString("------------------------------------------------------",20,Printer.Align.CENTER,false,false);

            Services.printer.printStringExt("--------------------------------------------------------------",0,0f,lineSpace, Printer.Font.SANS_SERIF,18, Printer.Align.LEFT,true, false, false);

            Services.printer.printStringExt("                     COUNT                        TOTAL ",0,0f,lineSpace, Printer.Font.SANS_SERIF,20, Printer.Align.LEFT,true, false, false);

            for(int i=0; i < IssuerHostMap.hosts[lastSettlement.HostID].issuerList.size() ; i++) {
                int issuer = IssuerHostMap.hosts[lastSettlement.HostID].issuerList.get(i);
                Issuer Actissuer = new Issuer(configDatabase.loadIssuer(issuer));
                long amount = 0;
                int count = 0;

                if(i == 0) {
                    amount = lastSettlement.VisaAmount;
                    count = lastSettlement.VisaCount;
                }
                else if (i == 1) {
                    amount = lastSettlement.MasterAmount;
                    count = lastSettlement.MasterCount;
                }
                else {
                    amount = lastSettlement.CupAmount;
                    count = lastSettlement.CupCount;
                }

                Services.printer.print2StringInLine(Formatter.fillInBack(" ", Actissuer.issuerLabel, 8) + Formatter.fillInFront(" ",String.valueOf(count),3), Formatter.formatAmount(amount,currencySymbol),lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            }

            Services.printer.print2StringInLine("VOID    " + Formatter.fillInFront(" ", String.valueOf(lastSettlement.VoidCount),3), Formatter.formatAmount(lastSettlement.VoidAmount,currencySymbol),lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.print2StringInLine("TOTALS  " + Formatter.fillInFront(" ", String.valueOf(lastSettlement.CardTotals),3), Formatter.formatAmount(lastSettlement.SubTotal,currencySymbol),lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.printStringExt("--------------------------------------------------------------",0,0f,lineSpace, Printer.Font.SANS_SERIF,18, Printer.Align.LEFT,true, false, false);

            putBlankLine();

            Services.printer.printStringExt("SETTLEMENT SUCCESSFUL " ,0,0f,lineSpace, Printer.Font.SANS_SERIF,20, Printer.Align.CENTER,true, false, false);

            feedPaper(60);
            Services.printer.printFinish();
            checkPrinterStatus();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    boolean yesPressed = false;
    boolean pressed   = false;

    public void printReceipt(int isInVoid) throws  RemoteException {
        if (SettingsInterpreter.isPrintCustCopy())  {    //so we print both
            printReceipt(ReceiptType.MERCH_COPY);

            if ((GlobalData.transactionName.contains("VOID")) && (isInVoid == 1)){
                ((Activity) VoidActivity.context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("COPY--->", "02 ALERT RUN");

                        AlertDialog.Builder printAlert = new AlertDialog.Builder(VoidActivity.context);
                        printAlert.setTitle("Transaction Receipt");
                        printAlert.setMessage("Would you like to print the customer copy ?");
                        printAlert.setCancelable(false);
                        printAlert.setPositiveButton("Print", printDialogClickListener);
                        printAlert.setNegativeButton("No", printDialogClickListener);
                        printAlert.show();
                    }
                });

                pressed = false;
                yesPressed = false;
            }
            else if (isInVoid == 3) {
                ((Activity) ReportActivity.context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("COPY--->", "02 ALERT RUN");

                        AlertDialog.Builder printAlert = new AlertDialog.Builder(ReportActivity.context);
                        printAlert.setTitle("Transaction Receipt");
                        printAlert.setMessage("Would you like to print the customer copy ?");
                        printAlert.setCancelable(false);
                        printAlert.setPositiveButton("Print", printDialogClickListener);
                        printAlert.setNegativeButton("No", printDialogClickListener);
                        printAlert.show();
                    }
                });

                //===============

                pressed = false;
                yesPressed = false;
            }
            else if (isInVoid == 4) {
                ((Activity) PreComp.context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("COPY--->", "02 ALERT RUN");

                        AlertDialog.Builder printAlert = new AlertDialog.Builder(PreComp.context);
                        printAlert.setTitle("Transaction Receipt");
                        printAlert.setMessage("Would you like to print the customer copy ?");
                        printAlert.setCancelable(false);
                        printAlert.setPositiveButton("Print", printDialogClickListener);
                        printAlert.setNegativeButton("No", printDialogClickListener);
                        printAlert.show();
                    }
                });

                //===============

                pressed = false;
                yesPressed = false;
            }
            else if (isInVoid == 2) {
                ((Activity) AnyReceiptActivity.context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("COPY--->", "02 ALERT RUN");

                        AlertDialog.Builder printAlert = new AlertDialog.Builder(AnyReceiptActivity.context);
                        printAlert.setTitle("Transaction Receipt");
                        printAlert.setMessage("Would you like to print the customer copy ?");
                        printAlert.setCancelable(false);
                        printAlert.setPositiveButton("Print", printDialogClickListener);
                        printAlert.setNegativeButton("No", printDialogClickListener);
                        printAlert.show();
                    }
                });

                //===============

                pressed = false;
                yesPressed = false;
            }
            else {
                (mActivity).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("COPY", "02 ALERT RUN");
                        AlertDialog.Builder printAlert = new AlertDialog.Builder(mActivity);
                        printAlert.setTitle("Transaction Receipt");
                        printAlert.setMessage("Would you like to print the customer copy ?");
                        printAlert.setCancelable(false);
                        printAlert.setPositiveButton("Print", printDialogClickListener);
                        printAlert.setNegativeButton("No", printDialogClickListener);
                        printAlert.show();
                    }
                });
            }

            while (!pressed)
                sleepMe(500);

            if (yesPressed) {
                Log.d("COPY", "03");
                printReceipt(ReceiptType.CUST_COPY);
                if(GlobalData.isDuplicate == 1) {
                    GlobalData.isDuplicate = 0;
                }
            }

            pressed = false;
            yesPressed = false;
        }
        else {
            Log.d("COPY", "01");
            printReceipt(ReceiptType.MERCH_COPY);
            if(GlobalData.isDuplicate == 1) {
                GlobalData.isDuplicate = 0;
            }
        }
    }

    public void printReceiptRev() throws  RemoteException {
        if (SettingsInterpreter.isPrintCustCopy())  {    //so we print both
            printReceiptReversal(ReceiptType.MERCH_COPY);

            ((Activity) ForceReversals.context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("COPY--->", "02 ALERT RUN");

                    AlertDialog.Builder printAlert = new AlertDialog.Builder(ForceReversals.context);
                    printAlert.setTitle("Transaction Receipt");
                    printAlert.setMessage("Would you like to print the customer copy ?");
                    printAlert.setCancelable(false);
                    printAlert.setPositiveButton("Print", printDialogClickListener);
                    printAlert.setNegativeButton("No", printDialogClickListener);
                    printAlert.show();
                }
            });

            //===============

            pressed = false;
            yesPressed = false;

            while (!pressed)
                sleepMe(500);

            if (yesPressed) {
                Log.d("COPY", "03");
                printReceiptReversal(ReceiptType.CUST_COPY);
            }

            pressed = false;
            yesPressed = false;
        }
        else {
            Log.d("COPY", "01");
            printReceiptReversal(ReceiptType.MERCH_COPY);
        }
    }

    DialogInterface.OnClickListener printDialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case  DialogInterface.BUTTON_POSITIVE:
                    pressed = true;
                    yesPressed = true;
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    pressed = true;
                    break;
            }
        }
    };

    public void printReceipt(ReceiptType rcptType) throws RemoteException {
        Log.d("DDDDDDDDDD", "--------->printReceipt");

        int fontSize = 18;

        if(bankCard != null)
            bankCard.breakOffCommand();

        Services.printer.printInit();
        Services.printer.clearPrintDataCache();
        if(GlobalData.isDuplicate == 1) {
            checkPrinterStatus();
        }
        else {
            checkPrinterStatusRecpt();
        }
        Services.printer.setPrintLineSpacing(0);

        //print the logo
        printImage(logoName, 350, 90);

        Log.d("XXXXXXXXXXXXXXXX", "XXXXXPRINT");
        //print the address line
        if (GlobalData.merchantName != null)
            Services.printer.printStringExt(GlobalData.merchantName, 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 17, Printer.Align.CENTER, true, false, false);

        if (GlobalData.addressLine1 != null)
            Services.printer.printStringExt(GlobalData.addressLine1, 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 17, Printer.Align.CENTER, true, false, false);

        if (GlobalData.addressLine2 != null)
            Services.printer.printStringExt(GlobalData.addressLine2, 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 17, Printer.Align.CENTER, true, false, false);

        //if (GlobalData.addressLine3 != null)
         //   Services.printer.printStringExt(GlobalData.addressLine3, 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 17, Printer.Align.CENTER, true, false, false);

        if(GlobalData.isDuplicate == 1) {
            Services.printer.printStringExt("DUPLICATE", 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 30, Printer.Align.CENTER, true, false, false);
        }
        putBlankLine();

        //print terminal related data
        Services.printer.printStringExt("DATE/TIME         :   " + currentTransaction.receiptDate + " " + Utility.getTimeFormatted(currentTransaction.Time), 0, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);
        Services.printer.printStringExt("MERCHANT ID   :   " + currentTransaction.merchantID, 0, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);
        Services.printer.printStringExt("TERMINAL ID     :   " + currentTransaction.terminalID, 0, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);
        if(GlobalData.isDuplicate == 1) {
            Services.printer.printStringExt("BATCH NO           :   " + Formatter.formatForSixDigits(loadBatchDuplicate()), 0, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);
        }
        else {
            Services.printer.printStringExt("BATCH NO           :   " + Formatter.formatForSixDigits(currentTransaction.batchNumber), 0, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);
        }
        Services.printer.printStringExt("INVOICE NO        :   " + Formatter.formatForSixDigits(currentTransaction.inInvoiceNumber), 0, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);

        String tranAmount = Formatter.formatAmount(currentTransaction.lnBaseTransactionAmount, currentTransaction.currencySymbol);

        if (currentTransaction.origTransactionCode == TranStaticData.TranTypes.VOID) {
            GlobalData.transactionName = "VOID " + TranStaticData.getTranName(currentTransaction.inTransactionCode);
            tranAmount = "-" + tranAmount;
        }

        //putBlankLine();
        //print the transaction name
        Services.printer.printStringExt(GlobalData.transactionName,0,0f,lineSpace, Printer.Font.MONOSPACE,30, Printer.Align.CENTER,true, false, false);
        //putBlankLine();

        String tranModeChar = "";

        if (currentTransaction.isFallbackTransaction)
            tranModeChar = "F";
        else if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.EMV_CARD)
            tranModeChar = "C";
        else if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.NOT_USING_CHIP)
            tranModeChar = "S";
        else if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.CTLS_CARD)
            tranModeChar = "CTLS";
        else if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.MANUAL_KEY_IN)
            tranModeChar = "M";

        currentTransaction.PAN = removePanPadding(currentTransaction.PAN);
        Log.d("DDDDDDDDDD", "---------> TYPE : " + currentTransaction.getCardLabel());
        // putBlankLine();
        Services.printer.printStringExt("CARD NO        :   " + Formatter.maskPan(currentTransaction.PAN, "****NNNN****NNNN", '*') + " " + tranModeChar, 1, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);
        Services.printer.printStringExt("EXP DATE       :   " + "**/**", 1, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);
        Services.printer.printStringExt("CARD TYPE   :   " + currentTransaction.cardData.cardLabel, 1, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);
        Services.printer.printStringExt("APPR CODE   :   " + currentTransaction.approveCode, 1, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);
        Services.printer.printStringExt("RREF NO         :   " + currentTransaction.RRN, 1, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);

        String msg = "Amount                     ";

        if(currentTransaction.discount > 0) {
            putBlankLine();
            Services.printer.printStringExt("Promotion ["+ currentTransaction.promoName + "] Applied",0,0f,lineSpace, Printer.Font.SANS_SERIF,18, Printer.Align.CENTER,false, false, false);
            Services.printer.printStringExt("Discount [" + currentTransaction.discountPercentage +"%]   " + Formatter.formatAmount(currentTransaction.discount,currentTransaction.currencySymbol)   ,0,0f,lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            msg = "Discounted Amount ";
        }

        //get the currency code
        String currencySymbol = currentTransaction.currencySymbol;
        if (currencySymbol == null)
            currencySymbol = "LKR";

        putBlankLine();
        Services.printer.printStringExt(msg + Formatter.formatAmount(currentTransaction.lnBaseTransactionAmount, currencySymbol), 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 25, Printer.Align.LEFT, true, false, false);
        putBlankLine();

        int cvmResult = 0;

        /**
         * merchant copy print
         */
        if (rcptType == ReceiptType.MERCH_COPY) {
            if (currentTransaction.inTransactionCode != TranStaticData.TranTypes.PRE_COMP) {
                if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.EMV_CARD)
                    cvmResult = Emv.getCVMAnalyzedResult();
                else if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.CTLS_CARD)
                    cvmResult = Emv.getCVMAnalyzedResultCTLS();
            }

            if (currentTransaction.inTransactionCode == TranStaticData.TranTypes.PRE_COMP)
                cvmResult = Emv.SIGNATURE_REQUIRED;

            if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.NOT_USING_CHIP || currentTransaction.inChipStatus == Transaction.ChipStatusTypes.MANUAL_KEY_IN)
                cvmResult = Emv.SIGNATURE_REQUIRED;

            if (cvmResult == Emv.SIGNATURE_REQUIRED)
                Services.printer.printStringExt("SIGNATURE ......................................................"  ,1,0f,lineSpace, Printer.Font.SANS_SERIF,18, Printer.Align.LEFT,false, false, false);
            else if(cvmResult == Emv.NO_SIGNATURE_REQUIRED)
                Services.printer.printStringExt("NO SIGNATURE REQUIRED"  ,0,0f,lineSpace, Printer.Font.SANS_SERIF,18, Printer.Align.CENTER,false, false, false);
            else if (cvmResult == Emv.PIN_VERIFIED)
                Services.printer.printStringExt("PIN VERIFIED", 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 18, Printer.Align.CENTER, false, false, false);
            else
                Services.printer.printStringExt("SIGNATURE ......................................................"  ,1,0f,lineSpace, Printer.Font.SANS_SERIF,18, Printer.Align.LEFT,false, false, false);

            Services.printer.printStringExt("I AGREE TO PAY THE ABOVE TOTAL AMOUNT"  ,0,0f,lineSpace, Printer.Font.SANS_SERIF,15, Printer.Align.CENTER,true, false, false);
            Services.printer.printStringExt("ACCORDING TO THE CARD ISSUER AGREEMENT"  ,0,0f,lineSpace, Printer.Font.SANS_SERIF,15, Printer.Align.CENTER,true, false, false);
            Services.printer.printStringExt("THANK YOU FOR BANKING WITH US"  ,0,0f,lineSpace, Printer.Font.SANS_SERIF,15, Printer.Align.CENTER,true, false, false);
            Services.printer.printStringExt("***MERCHANT COPY***"  ,0,0f,lineSpace, Printer.Font.SANS_SERIF,14, Printer.Align.CENTER,true, false, false);
        } else {
            Services.printer.printStringExt("I AGREE TO PAY THE ABOVE TOTAL AMOUNT", 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 15, Printer.Align.CENTER, true, false, false);
            Services.printer.printStringExt("ACCORDING TO THE CARD ISSUER AGREEMENT", 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 15, Printer.Align.CENTER, true, false, false);
            Services.printer.printStringExt("THANK YOU FOR BANKING WITH US", 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 15, Printer.Align.CENTER, true, false, false);
            Services.printer.printStringExt("***CUSTOMER COPY***", 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 14, Printer.Align.CENTER, true, false, false);
        }

        feedPaper(60);
        Services.printer.printFinish();
        if(GlobalData.isDuplicate == 1) {
            checkPrinterStatus();
        }
        else {
            checkPrinterStatusRecpt();
        }
    }

    private int loadBatchDuplicate() {
        //by now the merchant id is available to process so we can load the relevant terminal record to proceed with the transaction
        Cursor terminalRec = null;
        int batchNum = 0;

        if((terminalRec = configDatabase.loadTerminal(currentTransaction.merchantNumber)) == null) {
            showToast("Error with terminal record loading",TOAST_TYPE_FAILED);
            return 0;
        }

        batchNum = terminalRec.getInt(terminalRec.getColumnIndex("BatchNumber"));

        return batchNum;
    }

    public void printReceiptReversal(ReceiptType rcptType) throws RemoteException {
        Log.d("DDDDDDDDDD", "--------->printReceipt");

        int fontSize = 18;

        if(bankCard != null)
            bankCard.breakOffCommand();

        Services.printer.printInit();
        Services.printer.clearPrintDataCache();
        if(GlobalData.isDuplicate == 1) {
            checkPrinterStatus();
        }
        else {
            checkPrinterStatusRecpt();
        }
        Services.printer.setPrintLineSpacing(0);

        //print the logo
        printImage(logoName, 350, 90);

        Log.d("XXXXXXXXXXXXXXXX", "XXXXXPRINT");
        //print the address line
        if (GlobalData.merchantName != null)
            Services.printer.printStringExt(GlobalData.merchantName, 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 17, Printer.Align.CENTER, true, false, false);

        if (GlobalData.addressLine1 != null)
            Services.printer.printStringExt(GlobalData.addressLine1, 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 17, Printer.Align.CENTER, true, false, false);

        if (GlobalData.addressLine2 != null)
            Services.printer.printStringExt(GlobalData.addressLine2, 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 17, Printer.Align.CENTER, true, false, false);

        //if (GlobalData.addressLine3 != null)
        //   Services.printer.printStringExt(GlobalData.addressLine3, 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 17, Printer.Align.CENTER, true, false, false);

        if(GlobalData.isDuplicate == 1) {
            Services.printer.printStringExt("DUPLICATE", 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 30, Printer.Align.CENTER, true, false, false);
        }
        putBlankLine();

        //print terminal related data
        Services.printer.printStringExt("DATE/TIME         :   " + Utility.getDateFormattedRev(currentTransaction.Date) + " " + Utility.getTimeFormatted(currentTransaction.Time), 0, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);
        Services.printer.printStringExt("MERCHANT ID   :   " + currentTransaction.merchantID, 0, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);
        Services.printer.printStringExt("TERMINAL ID     :   " + currentTransaction.terminalID, 0, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);
        Services.printer.printStringExt("BATCH NO           :   " + Formatter.formatForSixDigits(currentTransaction.batchNumber), 0, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);
        Services.printer.printStringExt("INVOICE NO        :   " + Formatter.formatForSixDigits(currentTransaction.inInvoiceNumber), 0, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);

        String tranAmount = Formatter.formatAmount(currentTransaction.lnBaseTransactionAmount, currentTransaction.currencySymbol);

        GlobalData.transactionName = "TRANSACTION REVERSED";
        tranAmount = "-" + tranAmount;

        //putBlankLine();
        //print the transaction name
        Services.printer.printStringExt(GlobalData.transactionName,0,0f,lineSpace, Printer.Font.MONOSPACE,30, Printer.Align.CENTER,true, false, false);
        //putBlankLine();

        String tranModeChar = "";

        if (currentTransaction.isFallbackTransaction)
            tranModeChar = "F";
        else if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.EMV_CARD)
            tranModeChar = "C";
        else if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.NOT_USING_CHIP)
            tranModeChar = "S";
        else if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.CTLS_CARD)
            tranModeChar = "CTLS";
        else if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.MANUAL_KEY_IN)
            tranModeChar = "M";

        currentTransaction.PAN = removePanPadding(currentTransaction.PAN);
        Log.d("DDDDDDDDDD", "---------> TYPE : " + currentTransaction.getCardLabel());
        // putBlankLine();
        Services.printer.printStringExt("CARD NO        :   " + Formatter.maskPan(currentTransaction.PAN, "****NNNN****NNNN", '*') + " " + tranModeChar, 1, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);
        Services.printer.printStringExt("EXP DATE       :   " + "**/**", 1, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);
        Services.printer.printStringExt("CARD TYPE   :   " + currentTransaction.cardData.cardLabel, 1, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);
        Services.printer.printStringExt("APPR CODE   :   " + currentTransaction.approveCode, 1, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);
        Services.printer.printStringExt("RREF NO         :   " + currentTransaction.RRN, 1, 0f, lineSpace, Printer.Font.SANS_SERIF, fontSize, Printer.Align.LEFT, false, false, false);

        String msg = "Amount                     ";

        if(currentTransaction.discount > 0) {
            putBlankLine();
            Services.printer.printStringExt("Promotion ["+ currentTransaction.promoName + "] Applied",0,0f,lineSpace, Printer.Font.SANS_SERIF,18, Printer.Align.CENTER,false, false, false);
            Services.printer.printStringExt("Discount [" + currentTransaction.discountPercentage +"%]   " + Formatter.formatAmount(currentTransaction.discount,currentTransaction.currencySymbol)   ,0,0f,lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            msg = "Discounted Amount ";
        }

        //get the currency code
        String currencySymbol = currentTransaction.currencySymbol;
        if (currencySymbol == null)
            currencySymbol = "LKR";

        putBlankLine();
        Services.printer.printStringExt(msg + Formatter.formatAmount(currentTransaction.lnBaseTransactionAmount, currencySymbol), 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 25, Printer.Align.LEFT, true, false, false);
        putBlankLine();

        int cvmResult = 0;

        /**
         * merchant copy print
         */
        if (rcptType == ReceiptType.MERCH_COPY) {
            Services.printer.printStringExt("NO SIGNATURE REQUIRED"  ,0,0f,lineSpace, Printer.Font.SANS_SERIF,18, Printer.Align.CENTER,false, false, false);

            Services.printer.printStringExt("***MERCHANT COPY***"  ,0,0f,lineSpace, Printer.Font.SANS_SERIF,14, Printer.Align.CENTER,true, false, false);
        } else {
            Services.printer.printStringExt("***CUSTOMER COPY***", 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 14, Printer.Align.CENTER, true, false, false);
        }

        feedPaper(60);
        Services.printer.printFinish();
        if(GlobalData.isDuplicate == 1) {
            checkPrinterStatus();
        }
        else {
            checkPrinterStatusRecpt();
        }
    }

    public void printReceiptQRBase(int isVerify, QRTran tran) throws  RemoteException {
        if (SettingsInterpreter.isPrintCustCopy())  {    //so we print both
            printReceiptQR(ReceiptType.MERCH_COPY, tran);

            if (isVerify == 1){
                ((Activity) QRVerify.context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("COPY--->", "02 ALERT RUN");

                        AlertDialog.Builder printAlert = new AlertDialog.Builder(QRVerify.context);
                        printAlert.setTitle("Transaction Receipt");
                        printAlert.setMessage("Would you like to print the customer copy ?");
                        printAlert.setCancelable(false);
                        printAlert.setPositiveButton("Print", printDialogClickListener);
                        printAlert.setNegativeButton("No", printDialogClickListener);
                        printAlert.show();
                    }
                });

                pressed = false;
                yesPressed = false;
            }
            else if (isVerify == 2){
                ((Activity) AnyReceiptQRActivity.context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("COPY--->", "02 ALERT RUN");

                        AlertDialog.Builder printAlert = new AlertDialog.Builder(AnyReceiptQRActivity.context);
                        printAlert.setTitle("Transaction Receipt");
                        printAlert.setMessage("Would you like to print the customer copy ?");
                        printAlert.setCancelable(false);
                        printAlert.setPositiveButton("Print", printDialogClickListener);
                        printAlert.setNegativeButton("No", printDialogClickListener);
                        printAlert.show();
                    }
                });

                pressed = false;
                yesPressed = false;
            }
            else if (isVerify == 3){
                ((Activity) ReportActivity.context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("COPY--->", "02 ALERT RUN");

                        AlertDialog.Builder printAlert = new AlertDialog.Builder(ReportActivity.context);
                        printAlert.setTitle("Transaction Receipt");
                        printAlert.setMessage("Would you like to print the customer copy ?");
                        printAlert.setCancelable(false);
                        printAlert.setPositiveButton("Print", printDialogClickListener);
                        printAlert.setNegativeButton("No", printDialogClickListener);
                        printAlert.show();
                    }
                });

                pressed = false;
                yesPressed = false;
            }
            else {
                ((Activity) QRDisplay.context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("COPY", "02 ALERT RUN");
                        AlertDialog.Builder printAlert = new AlertDialog.Builder(QRDisplay.context);
                        printAlert.setTitle("Transaction Receipt");
                        printAlert.setMessage("Would you like to print the customer copy ?");
                        printAlert.setCancelable(false);
                        printAlert.setPositiveButton("Print", printDialogClickListener);
                        printAlert.setNegativeButton("No", printDialogClickListener);
                        printAlert.show();
                    }
                });
            }

            while (!pressed)
                sleepMe(500);

            if (yesPressed) {
                Log.d("COPY", "03");
                printReceiptQR(ReceiptType.CUST_COPY, tran);
                if(GlobalData.isDuplicate == 1) {
                    GlobalData.isDuplicate = 0;
                }
            }

            pressed = false;
            yesPressed = false;
        }
        else {
            Log.d("COPY", "01");
            printReceiptQR(ReceiptType.MERCH_COPY,tran);
            if(GlobalData.isDuplicate == 1) {
                GlobalData.isDuplicate = 0;
            }
        }
    }

    public void printReceiptQR(ReceiptType rcptType, QRTran qrTran) throws RemoteException {
        int fontSize = 18;

        if(bankCard != null)
            bankCard.breakOffCommand();

        Services.printer.printInit();
        Services.printer.clearPrintDataCache();
        checkPrinterStatus();
        Services.printer.setPrintLineSpacing(0);
        //print the logo
        printImage(logoName, 350, 130);
        putBlankLine();
        if(GlobalData.isDuplicate == 1) {
            Services.printer.printStringExt("DUPLICATE", 0, 0f, lineSpace, Printer.Font.SANS_SERIF, 30, Printer.Align.CENTER, true, false, false);
        }
        putBlankLine();
        String date = Utility.getDateFormatted(qrTran.Date.substring(2));

        //print terminal related data
        Services.printer.printStringExt("DATE/TIME     : " + date + " " + qrTran.Time, 0, 0f, lineSpace, Printer.Font.MONOSPACE, fontSize, Printer.Align.LEFT, true, false, false);
        Services.printer.printStringExt("MERCHANT ID   : " + qrTran.mid, 0, 0f, lineSpace, Printer.Font.MONOSPACE, fontSize, Printer.Align.LEFT, true, false, false);
        Services.printer.printStringExt("MERCHANT NAME : " + qrTran.merchName, 0, 0f, lineSpace, Printer.Font.MONOSPACE, fontSize, Printer.Align.LEFT, true, false, false);
        Services.printer.printStringExt("TERMINAL ID   : " + qrTran.terminalID, 0, 0f, lineSpace, Printer.Font.MONOSPACE, fontSize, Printer.Align.LEFT, true, false, false);
        Services.printer.printStringExt("MCC           : " + qrTran.MCC, 0, 0f, lineSpace, Printer.Font.MONOSPACE, fontSize, Printer.Align.LEFT, true, false, false);
        if(qrTran.cusMobile.equals("null")) {
            qrTran.cusMobile = "N/A";
        }
        Services.printer.printStringExt("CUS MOBILE    : " + qrTran.cusMobile, 0, 0f, lineSpace, Printer.Font.MONOSPACE, fontSize, Printer.Align.LEFT, true, false, false);

        putBlankLine();
        //print the transaction name
        Services.printer.printStringExt("QR SALE", 0, 0f, lineSpace, Printer.Font.MONOSPACE, 30, Printer.Align.CENTER, true, false, false);

        Services.printer.printStringExt("PAY METHOD    : " + "QR", 0, 0f, lineSpace, Printer.Font.MONOSPACE, fontSize, Printer.Align.LEFT, true, false, false);
        Services.printer.printStringExt("QR TYPE       : " + qrTran.qrType, 0, 0f, lineSpace, Printer.Font.MONOSPACE, fontSize, Printer.Align.LEFT, true, false, false);
        Services.printer.printStringExt("CARD HOLDER   : " + qrTran.cardHolder, 0, 0f, lineSpace, Printer.Font.MONOSPACE, fontSize, Printer.Align.LEFT, true, false, false);
        Services.printer.printStringExt("PAN           : " + qrTran.PAN, 0, 0f, lineSpace, Printer.Font.MONOSPACE, fontSize, Printer.Align.LEFT, true, false, false);
        Services.printer.printStringExt("TRACE NO      : " + qrTran.trace, 0, 0f, lineSpace, Printer.Font.MONOSPACE, fontSize, Printer.Align.LEFT, true, false, false);
        Services.printer.printStringExt("TRANS REF     : " + Formatter.removeleadingZeros(qrTran.refQRLabel), 0, 0f, lineSpace, Printer.Font.MONOSPACE, fontSize, Printer.Align.LEFT, true, false, false);
        Services.printer.printStringExt("TX ORG        : " + qrTran.txOrg, 0, 0f, lineSpace, Printer.Font.MONOSPACE, fontSize, Printer.Align.LEFT, true, false, false);

        String msg = "Amount      ";

        putBlankLine();
        String currencySymbol = "";
        currencySymbol = "LKR";
        Services.printer.printStringExt(msg + Formatter.formatAmount(qrTran.tranQRAmount, currencySymbol), 0, 0f, lineSpace, Printer.Font.MONOSPACE, 25, Printer.Align.LEFT, true, false, false);

        putBlankLine();

        if (rcptType == ReceiptType.MERCH_COPY) {
            Services.printer.printStringExt("NO SIGNATURE REQUIRED", 0, 0f, lineSpace, Printer.Font.MONOSPACE, 18, Printer.Align.CENTER, false, false, false);

            Services.printer.printStringExt("I AGREE TO PAY THE ABOVE TOTAL AMOUNT", 0, 0f, lineSpace, Printer.Font.MONOSPACE, 15, Printer.Align.CENTER, true, false, false);
            Services.printer.printStringExt("ACCORDING TO THE CARD ISSUER AGREEMENT", 0, 0f, lineSpace, Printer.Font.MONOSPACE, 15, Printer.Align.CENTER, true, false, false);
            Services.printer.printStringExt("THANK YOU FOR BANKING WITH US", 0, 0f, lineSpace, Printer.Font.MONOSPACE, 15, Printer.Align.CENTER, true, false, false);
            putBlankLine();
            Services.printer.printStringExt("***MERCHANT COPY***", 0, 0f, lineSpace, Printer.Font.MONOSPACE, 14, Printer.Align.CENTER, true, false, false);
        } else {
            Services.printer.printStringExt("***CUSTOMER COPY***", 0, 0f, lineSpace, Printer.Font.MONOSPACE, 14, Printer.Align.CENTER, true, false, false);
        }

        feedPaper(60);
        Services.printer.printFinish();
        checkPrinterStatus();
    }

    private String removePanPadding(String PAN) {
        while (PAN.endsWith("F"))
            PAN = PAN.substring(0, PAN.length() - 1);

        return PAN;
    }

    private void feedPaper(int rows) {
        try {
            Services.printer.printPaper(rows);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void printImage(String imageName, int width, int height) {
        try {
            InputStream inputStream = appContext.getAssets().open(imageName);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            Services.printer.printImageBase(bitmap, width, height, Printer.Align.CENTER, 0);
            bitmap.recycle();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    public void printISOLogData(String data) {
        String[] lines = data.split("\n");

        try {
            if(bankCard != null)
                bankCard.breakOffCommand();
            Services.printer.printInit();
            Services.printer.clearPrintDataCache();

            String line = "";

            for (int i = 0 ; i < lines.length; i++) {
                line = lines[i];

                if (line != "")
                    line = "| " + line + " |";

                Services.printer.printStringExt(line,0,0f,lineSpace, Printer.Font.MONOSPACE,19, Printer.Align.LEFT,false, false, false);
            }

            Services.printer.printFinish();
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
    }

    public enum ReceiptType {
        MERCH_COPY,
        CUST_COPY
    }

    //prints the host parameters
    public void printHostParameters(int selected, int selectedMerch) {
        try {
            if(bankCard != null)
                bankCard.breakOffCommand();
            checkPrinterStatus();
            Services.printer.printInit();
            Services.printer.clearPrintDataCache();
            Services.printer.setPrintLineSpacing(0);

            int baseIssuer  = IssuerHostMap.hosts[selected].baseIssuer;

            String selectQuary = "SELECT * FROM IIT WHERE IssuerNumber = " + baseIssuer;

            Cursor rec = null;
            DBHelper configDB = DBHelper.getInstance(appContext);

            try {
                rec = configDB.readWithCustomQuary(selectQuary);
                if (rec == null || rec.getCount() == 0)
                    return;
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }

            rec.moveToFirst();

            String hostName = IssuerHostMap.hosts[selected].hostName;
            String ip = rec.getString(rec.getColumnIndex("IP"));
            String port = String.valueOf(rec.getInt(rec.getColumnIndex("Port")));
            String NII = rec.getString(rec.getColumnIndex("NII"));
            String secureNII = rec.getString(rec.getColumnIndex("SecureNII"));

            rec.close();

            //get the tid and mid
            selectQuary = "SELECT MerchantID,TerminalID FROM TMIF,MIT WHERE TMIF.IssuerNumber = " + baseIssuer +
                    " AND MIT.MerchantNumber = TMIF.MerchantNumber AND MIT.MerchantNumber = " + selectedMerch;

            try {
                rec = configDB.readWithCustomQuary(selectQuary);
                if (rec == null || rec.getCount() == 0)
                    return;
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }

            rec.moveToFirst();

            //get the first mid tid
            String tid = rec.getString(rec.getColumnIndex("TerminalID"));
            String mid = rec.getString(rec.getColumnIndex("MerchantID"));

            rec.close();

            Services.printer.printString("HOST PARAMETERS",25, Printer.Align.CENTER,true,false);
            Services.printer.printString("------------------------------------------------------",20, Printer.Align.CENTER,true,false);

            putBlankLine();

            //print terminal related data
            Services.printer.printStringExt("HOST        : " + hostName,0,0f,lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.printStringExt("TERMINAL ID : " + tid,0,0f,lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.printStringExt("MERCHANT ID : " + mid,0,0f,lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.printStringExt("IP          : " + ip,0,0f,lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.printStringExt("PORT        : " + port,0,0f,lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.printStringExt("NII         : " + NII,0,0f,lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);
            Services.printer.printStringExt("SECURE NII  : " + secureNII,0,0f,lineSpace, Printer.Font.MONOSPACE,20, Printer.Align.LEFT,true, false, false);

            Services.printer.printString("------------------------------------------------------",20, Printer.Align.CENTER,true,false);

            //We need to feed the paper little bit after printing is done so  the user an see the paper well
            feedPaper(60);
            Services.printer.printFinish();
            checkPrinterStatus();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}