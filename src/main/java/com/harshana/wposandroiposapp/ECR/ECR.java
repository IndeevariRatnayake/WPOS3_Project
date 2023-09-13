package com.harshana.wposandroiposapp.ECR;
import android.os.Message;

import com.harshana.wposandroiposapp.Base.Base;
import com.harshana.wposandroiposapp.Base.Transaction;
import com.harshana.wposandroiposapp.MainActivity;
import com.harshana.wposandroiposapp.Utilities.Formatter;

public class ECR {
    public  boolean isECRInitiated = false;

    UsbService usbService = null;
    byte [] buffer = null;

    String dummy = "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";

    public static  long saleAmount = 0;

    public ECR(UsbService service) {
        usbService = service;
        buffer =  new byte[200];
    }

    String cmdInterpreted = "";
    public  int  performECRFunc(final String command) {
        isECRInitiated = true;
        cmdInterpreted  = convertStringToUTF8(command);

        if (command.contains("BINN#")) {
            //again filter the commands here so we can use only one thread code for all
            Message msg = new Message();
            msg.what = MainActivity.OPER_CARD_INPUT_SCREEN;
            MainActivity.mainMessageHandler.sendMessage(msg);
            return 1;
        }
        else if ( command.contains("SALK")) { //Sale
            if (command.length() == 17) {
                Thread ecr = new Thread(new Runnable() {
                    @Override
                    public void run() {
                         //initialize the sale amount
                         String strSaleAmount = command.substring(4,16);
                         strSaleAmount = strSaleAmount.trim();

                         //setting the sale amount thread safe manner
                         synchronized (ECR.class) {
                             saleAmount = Long.valueOf(strSaleAmount);
                         }
                    }
                });

                ecr.start();
                return 1;
            }

        }
        else if (command.contains("BINO")) { //Void

        }
        else if (command.contains("QRSA")) { //QR
            if (command.length() == 17) {
                String strSaleAmount = command.substring(4,16);
                strSaleAmount = strSaleAmount.trim();
                saleAmount = Long.valueOf(strSaleAmount);

                Message msg = new Message();
                msg.what = MainActivity.OPER_QR_SCREEN;
                MainActivity.mainMessageHandler.sendMessage(msg);
                return 1;
            }
        }

        return 0;
    }

    public static String convertStringToUTF8(String s) {
        String out = null;
        try {
            out = new String(s.getBytes("UTF-8"), "ISO-8859-1");
        } catch (java.io.UnsupportedEncodingException e) {
            return null;
        }
        return out;
    }

    public static final int WAIT_LATENCY = 10;
    private static final int LOOP_LATENCY = 500;

    public static final int NO_ECR_RESPONSE = -1;
    public static final String GENERIC_ERROR = "1A";

    public int pushTransactionDetails(int error_state,Transaction tran,String responseCode) {
        //zero out the memory content
        System.arraycopy(dummy.getBytes(),0,buffer,0,200);

        //evaluate the error state first
        if (error_state == Base.GENERIC_ERROR_TRAN_MIDDLE) {//Generic error the 1A should be pushed to the client application
            //simply send the generic error
            setResponseCode(GENERIC_ERROR);
        }
        else if (error_state == Base.SUCCESS) {
            if (!responseCode.equals("00")) {
                //set the response code to the host returned value and send
                setResponseCode(responseCode);
            }
            else {//indicates the success state so we extract the transaction details and push it
                //set the invoice number in the buffer
                String data = Formatter.formatForSixDigits(tran.inInvoiceNumber);
                System.arraycopy(data.getBytes(),0,buffer,0,6);

                //set the terminal id
                data = Formatter.fillInFront("0",tran.terminalID,8);
                System.arraycopy(data.getBytes(),0,buffer,12,8);

                //set the merchant id
                data = Formatter.fillInFront("0",tran.merchantID,15);
                System.arraycopy(data.getBytes(),0,buffer,20,15);

                //set the approval code
                if (tran.approveCode != null)
                    data = Formatter.fillInFront("0",tran.approveCode,6);
                else
                    data = "000000";
                System.arraycopy(data.getBytes(),0,buffer,41,6);

                //set the response code
                setResponseCode(responseCode);

                //set the rrn
                if (tran.RRN !=null)
                    data = Formatter.fillInFront("0",tran.RRN,12);
                else
                    data = "000000000000";

                System.arraycopy(data.getBytes(),0,buffer,49,12);

                //set the card bin first 6
                data = tran.PAN.substring(0,6);
                System.arraycopy(data.getBytes(),0,buffer,61,6);

                //set the card bin last 4
                data = tran.PAN.substring(tran.PAN.length() - 4);
                System.arraycopy(data.getBytes(),0,buffer,67,4);

                //set the card label
                data = tran.issuerData.issuerLabel;
                data = Formatter.fillInFront(" ",data,12);
                System.arraycopy(data.getBytes(),0,buffer,71,12);
            }
        }

        usbService.write(buffer);
        return 1;
    }

    public int pushQRDetails(int error_state,String merchName, long tranAmt, String refLable,int responseCode) {
        //zero out the memory content
        System.arraycopy(dummy.getBytes(),0,buffer,0,200);

        //evaluate the error state first
        if (error_state == Base.GENERIC_ERROR_TRAN_MIDDLE) {//Generic error the 1A should be pushed to the client application
            //simply send the generic error
            setResponseCode(GENERIC_ERROR);
        }
        else if (error_state == Base.SUCCESS) {
            if ((responseCode == -1) || (responseCode == 9)) {
                //set the response code to the host returned value and send
                setResponseCode("1A");
            }
            else {//indicates the success state so we extract the transaction details and push it
                String data = "QR";
                System.arraycopy(data.getBytes(),0,buffer,0,2);

                //set the merchantName in the buffer
                data = Formatter.fillInFront(" ",merchName,20);
                System.arraycopy(data.getBytes(),0,buffer,2,20);

                //set the refLable
                data = Formatter.fillInFront(" ",refLable,20);
                System.arraycopy(data.getBytes(),0,buffer,22,20);

                setResponseCode("00");
            }
        }

        usbService.write(buffer);
        return 1;
    }

    private void setResponseCode(String respCode) {
        System.arraycopy(respCode.getBytes(),0,buffer,47,2);
    }

    public int pushBinWaitForSale(Transaction tran) {
        String cardBinFirst = tran.PAN.substring(0,6);
        String cardBinLast = tran.PAN.substring(tran.PAN.length() - 4);

        System.arraycopy(dummy.getBytes(),0,buffer,0,200);
        System.arraycopy(cardBinFirst.getBytes(),0,buffer,61,6);
        System.arraycopy(cardBinLast.getBytes(),0,buffer,67,4);

        if (usbService == null)
            return 1;

        usbService.write(buffer);

        int retValue = 0;
        int milliSecCounter  = 0 ;

        //wait for sale command to be arrived
        while (true) {
            try {
                Thread.sleep(LOOP_LATENCY);
            }catch (Exception ex){}

            milliSecCounter += LOOP_LATENCY;

            if (milliSecCounter >= (WAIT_LATENCY * 1000)) {
                retValue =  NO_ECR_RESPONSE;
                break;
            }

            synchronized (ECR.class) {
                if (saleAmount > 0) {//sale amount is set
                    tran.lnBaseTransactionAmount = saleAmount;
                    retValue = 0;
                    break;
                }
            }
        }
        return retValue;
    }
}