package com.harshana.wposandroiposapp.TransactionMode;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import com.harshana.wposandroiposapp.Base.Base;
import com.harshana.wposandroiposapp.Base.GlobalWait;
import com.harshana.wposandroiposapp.Base.IssuerHostMap;
import com.harshana.wposandroiposapp.Base.Services;
import com.harshana.wposandroiposapp.Base.Transaction;
import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.DevArea.GlobalData;
import com.harshana.wposandroiposapp.DevArea.TranStaticData;
import com.harshana.wposandroiposapp.MainActivity;
import com.harshana.wposandroiposapp.Print.Receipt;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.Settings.SettingsInterpreter;
import com.harshana.wposandroiposapp.UI.KeyPadDialog;
import com.harshana.wposandroiposapp.Utilities.BytesUtil;
import com.harshana.wposandroiposapp.Utilities.Formatter;
import com.harshana.wposandroiposapp.Utilities.Utility;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import sdk4.wangpos.libemvbinder.EmvParam;
import wangpos.sdk4.emv.ICallbackListener;
import wangpos.sdk4.libbasebinder.Core;

import static com.harshana.wposandroiposapp.DevArea.EmvDev.AMEX_TagList;
import static com.harshana.wposandroiposapp.DevArea.EmvDev.AMEX_TagList_CTLS;
import static com.harshana.wposandroiposapp.DevArea.EmvDev.CUP_TagList;
import static com.harshana.wposandroiposapp.DevArea.EmvDev.CUP_TagList_CTLS;
import static com.harshana.wposandroiposapp.DevArea.EmvDev.JCB_TagList;
import static com.harshana.wposandroiposapp.DevArea.EmvDev.MASTER_TagList;
import static com.harshana.wposandroiposapp.DevArea.EmvDev.MASTER_TagList_CTLS;
import static com.harshana.wposandroiposapp.DevArea.EmvDev.VISA_TagList;
import static com.harshana.wposandroiposapp.DevArea.EmvDev.VISA_TagList_CTLS;
import static wangpos.sdk4.libbasebinder.Core.CALLBACK_APPREF;

public class Emv extends Base {
    public static final int PATH_PBOC  = 0x00;     //应用路径：标准PBOC
    public static final int PATH_QPBOC = 0x01;     //应用路径：qPBOC
    public static final int PATH_JCB   = 0x02;     //应用路径：MSD
    public static final int PATH_ECash = 0x03;     //应用路径：电子现金
    public static final int PATH_MAG   = 0x05;
    public static final int PATH_CHIP  = 0x06;
    public static final int PATH_AMEX  = 0x07;

    //emvCore.appSel  or emvCore.procTrans
    public static final int EMV_SUCCESS  = 0x00;
    public static final int ERR_EMVRSP  = -1;
    public static final int APPLICATION_BLOCK  = -2;
    public static final int CARD_BLOCK  = -3;
    public static final int ERR_USERCANCEL  = -4;
    public static final int ERR_EMVDATA  = -6;
    public static final int ERR_EMVDENIAL  = -8;
    public static final int ERR_ICCCMD  = -20;
    public static final int EMV_CARD_BLOCK  = -21;

    final String TAG = "HARA";
    Thread callBackThread;

    public static byte[] long2Bytes(long num) {
        byte[] byteNum = new byte[8];

        for (int ix = 0; ix < 8; ++ix) {
            int offset = 64 - (ix + 1) * 8;
            byteNum[ix] = (byte) ((num >> offset) & 0xff);
        }
        return byteNum;
    }

    public static final String EMV_TAG_FILE = "emvtag.dat";

    public static  String appName = "";
    public static final int SIGNATURE_REQUIRED = 1;
    public static final int PIN_VERIFIED = 2;
    public static final int NO_SIGNATURE_REQUIRED = 3;
    public static final int MOBILE_CVM_APPLIED = 4;
    private boolean pinError = false;

    private static byte TTQ_SIGNATURE_SUPPORT    = 0x20;           //byte 1
    private static byte TTQ_ONLINE_PIN_SUPPORT   = 0X40;           //byte 1
    private static byte TTQ_CVM_REQUIRED         = 0x40;           //byte 2
    private static byte TTQ_CDCVM_SUPPORT        = 0X40;           //byte 3

    private static byte CTQ_CDCVM_SUPPORT = (byte)0x80;
    private static byte CTQ_ONLINE_PIN_REQUIRED  = (byte)0x80;     //byte 1
    private static byte CTQ_SIGNATURE_REQUIRED = 0x40;
    private static byte AIP_EXP_MOBILE = 0x40;
    private static byte CTQ_CARD_CVM_REQUIRED =  0x40;
    private  Activity mActivity;

    public static int getCVMAnalyzedResultCTLS() {
        Integer[] TTQ = new Integer[3];
        Integer[] CTQ = new Integer[2];

        String TTQs = null;
        String CTQs = null;

        byte [] out =  new byte[20];
        int [] len =  new int[2];

        int result = 0 ;

        String strOutData = "";{
            byte [] outData = new  byte[100];
            int [] lenout = new int[2];

            try {
                int res = Services.emvCore.getTLV(0x9f34,outData,lenout);
                if (res == 0)
                   strOutData = Utility.byte2HexStr(outData,0,lenout[0]);
            } catch (Exception ex) {
                ex.printStackTrace();
                return -1;
            }
        }

        try {
            result = Services.emvCore.getTLV(0x9F66,out,len);
            if (result == 0 && len[0] > 0)
                TTQs = Utility.byte2HexStr(out,0,len[0]);

            result = Services.emvCore.getTLV(0x9F6C,out,len);
            if (result == 0 && len[0] > 0)
                CTQs = Utility.byte2HexStr(out,0,len[0]);
        } catch (RemoteException ex) {
            ex.printStackTrace();
            return -1;
        }

        try {
            if (TTQs != null) {
                TTQ[0] = Integer.valueOf(Integer.valueOf(TTQs.substring(0, 2), 16));
                TTQ[1] = Integer.valueOf(Integer.valueOf(TTQs.substring(2, 4), 16));
                TTQ[2] = Integer.valueOf(Integer.valueOf(TTQs.substring(4, 6), 16));
            } else
                TTQ = null;

            if (CTQs != null) {
                CTQ[0] = Integer.valueOf(Integer.valueOf(CTQs.substring(0, 2), 16));
                CTQ[1] = Integer.valueOf(Integer.valueOf(CTQs.substring(2, 4), 16));
            } else
                CTQ = null;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

        boolean isTerminalMobileCVMSupport = false;
        boolean isCVMRequired = false;

        boolean isCDCVMSupport = false;

        if (TTQ != null) {
            isTerminalMobileCVMSupport      =    ((TTQ[2] & TTQ_CDCVM_SUPPORT) > 0);
            isCVMRequired                   =    (((TTQ[1] & TTQ_CVM_REQUIRED) > 0));
        }

        if (CTQ != null) {
            isCDCVMSupport                  =    ((CTQ[1] & CTQ_CDCVM_SUPPORT) > 0);
        }

        if(currentTransaction.encryptedPINBlock != null)
            return PIN_VERIFIED;

        if (isCVMRequired)
            if (isTerminalMobileCVMSupport)
                if (isCDCVMSupport)
                    return MOBILE_CVM_APPLIED;

        if (strOutData.startsWith("5E") || strOutData.startsWith("1E") || strOutData.startsWith("03"))  {
            isCVMRequired = true;
        }

        if (isCVMRequired)
            return SIGNATURE_REQUIRED;
        else
            return NO_SIGNATURE_REQUIRED;
    }

    public static final String ISO_LOG_PATH_SEND = "ISOLogSnd.txt";

    private static final byte SEE_PHONE = 20;
    public static final String ISO_LOG_PATH_RECV = "ISOLogRec.txt";
    private static final int NUM_TAGS = 60;
    String emvTags[][] =
            {
                    {"4F		", "AID                    "},
                    {"57		", "TRACK2_EQ_DATA         "},
                    {"5A		", "APPL_PAN               "},
                    {"5F24	", "EXPIRY_DATE            "},
                    {"5F2A	", "TRANS_CURCY_CODE       "},
                    {"5F30	", "SERVICE_CODE           "},
                    {"5F34	", "APPL_PAN_SEQNUM        "},
                    {"81		", "AMOUNT_AUTH            "},
                    {"82	", "APPL_INTCHG_PROF       "},
                    {"86		", "ISSUER_SCRIPT_CMD      "},
                    {"89		", "AUTH_CODE              "},
                    {"8A		", "AUTH_RESP_CODE         "},
                    {"8C		", "CDOL1                  "},
                    {"8D		", "CDOL2                  "},
                    {"8E	", "CVM_LIST               "},
                    {"8F		", "CA_PK_INDEX(ICC)       "},
                    {"91		", "ISS_AUTH_DATA          "},
                    {"94		", "AFL                    "},
                    {"93		", "SGN_SAD                "},
                    {"95	", "TVR                    "},
                    {"97		", "TDOL                   "},
                    {"9A		", "TRANS_DATE             "},
                    {"9C		", "TRANS_TYPE             "},
                    {"9B	", "TSI                    "},
                    {"9F02	", "AMT_AUTH_NUM           "},
                    {"9F03	", "OTHER_AMT              "},
                    {"9F07	", "APPL_USE_CNTRL         "},
                    {"9F08	", "APP_VER_NUM            "},
                    {"9F09	", "TERM_VER_NUM           "},
                    {"9F0D	", "IAC_DEFAULT            "},
                    {"9F0E	", "IAC_DENIAL             "},
                    {"9F0F	", "IAC_ONLINE             "},
                    {"9F10	", "ISSUER_APP_DATA        "},
                    {"9F12	", "PREFERRED_NAME         "},
                    {"9F15	", "MERCHANT_CAT_CODE      "},
                    {"9F18	", "ISSUER_SCRIPT_ID       "},
                    {"9F1A	", "TERM_COUNTY_CODE       "},
                    {"9F1B	", "TERM_FLOOR_LIMIT       "},
                    {"9F1D	", "TERM_RISKMGMT_DATA     "},
                    {"9F1E	", "IFD_SER_NUM            "},
                    {"9F23	", "UC_OFFLINE_LMT         "},
                    {"9F26	", "AC9                    "},
                    {"9F27	", "CRYPT_INFO_DATA        "},
                    {"9F2A	", "TRANS_CURCY_CODE       "},
                    {"9F33	", "TERM_CAP               "},
                    {"9F34	", "CVM_RESULTS            "},
                    {"9F36	", "APP_TXN_COUNTER        "},
                    {"9F37	", "UNPREDICT_NUMBER       "},
                    {"9F38	", "PDOL                   "},
                    {"9F39	", "POS_ENT_MODE           "},
                    {"9F3A	", "AMT_REF_CURR           "},
                    {"9F3C	", "TRANS_REF_CURR         "},
                    {"9F45	", "DATA_AUTH_CODE         "},
                    {"9F49	", "DDOL                   "},
                    {"9F4A	", "SDA_TAGLIST            "},
                    {"9F4B	", "DYNAMIC_APPL_DATA      "},
                    {"9F4C	", "ICC_DYNAMIC_NUM        "},
                    {"9F53	", "TRAN_CURR_CODE         "},
                    {"9F5B	", "ISS_SCRIPT_RES         "},
                    {"71		", "ISUER_SCRPT_TEMPL_71   "},
                    {"72		", "ISUER_SCRPT_TEMPL_72   "},
            };

    //This is the callback function which required by all the program
    public ICallbackListener iCallBackListener = new ICallbackListener.Stub() {
        @Override
        public int emvCoreCallback(final int i, final byte[] bytes, final byte[] resultBytes, final int[] ints) {
            callBackThread = Thread.currentThread();
            final CountDownLatch countDownLatch =  new CountDownLatch(1);
            switch (i) {
                case Core.CALLBACK_PIN:
                    (mActivity).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            KeyPadDialog.getInstance().showDialog(mActivity, i, bytes, resultBytes, ints, new KeyPadDialog.OnPinPadListener() {
                                @Override
                                protected void onSuccess(String pin) {
                                    Log.d("Invi_PIN", "onSuccessPIN");
                                    currentTransaction.encryptedPINBlock = pin;
                                    countDownLatch.countDown();
                                }

                                @Override
                                protected void onBypass() {
                                    Log.d("Invi_PIN", "onBypass");
                                    countDownLatch.countDown();
                                }

                                @Override
                                protected void onSuccess() {
                                    Log.d("Invi_PIN", "onSuccess");
                                    countDownLatch.countDown();
                                }

                                @Override
                                protected void onError() {
                                    Log.d("Invi_PIN", "onError");
                                    currentTransaction.encryptedPINBlock = "";
                                    pinError = true;
                                    countDownLatch.countDown();
                                }
                            });
                        }
                    });

                    try {
                        Log.d("Invi_PIN", "waiting");
                        countDownLatch.await();
                        Log.d("Invi_PIN", "waiting ended");
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }

                    if(pinError) {
                        Log.d("Invi_PIN", "onBypass1");
                        showToast("Pin Entry Failed", TOAST_TYPE_FAILED);
                        errorTone();
                        pinError = false;
                        forceRemoveCard();
                        return GENERIC_ERROR_TRAN_MIDDLE;
                    }
                    break;

                case CALLBACK_APPREF:
                    break;

                case Core.CALLBACK_AMOUNT:
                    resultBytes[0] = 0;

                    byte[] tmp = long2Bytes(currentTransaction.lnBaseTransactionAmount);
                    Log.i(TAG, "emvCoreCallback: tmp:" + BytesUtil.bytes2HexString(tmp));
                    byte[] am = new byte[8];
                    am[0] = tmp[7];
                    am[1] = tmp[6];
                    am[2] = tmp[5];
                    am[3] = tmp[4];
                    am[4] = tmp[3];
                    am[5] = tmp[2];
                    am[6] = tmp[1];
                    am[7] = tmp[0];

                    System.arraycopy(am, 0, resultBytes, 1, 4);
                    ints[0] = 9;
                    countDownLatch.countDown();
                    break;

                case Core.CALLBACK_PINRESULT:
                    countDownLatch.countDown();
                    break;

                case Core.CALLBACK_ONLINE:
                    startBusyAnimation("Preparing EMV Data...");
                    processTransactionEMV(resultBytes,ints);
                    stopBusyAnimation();

                    startBusyAnimation("Sending Reversal...");
                    if (REVERSAL_FAILED == reversalHandler.pushpPendingReversal()) {
                        if(!SettingsInterpreter.isForceReversalEnabled())
                            showToast("Reversal Failed", TOAST_TYPE_FAILED);
                        errorTone();
                        forceRemoveCard();
                        return GENERIC_ERROR_TRAN_MIDDLE;
                    }
                    stopBusyAnimation();

                    //in this call a merchant should be selected by the user to proceed;
                    startBusyAnimation("Going Online..");
                    byte result = (byte) sendTransactionOnline(resultBytes, ints);

                    stopBusyAnimation();

                    resultBytes[0] = result;
                    countDownLatch.countDown();
                    break;

                case 0x0b18:
                    int x = 0;
                    break;

                case Core.CALLBACK_NOTIFY:
                    (mActivity).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            appName = "";

                            String resultDate = BytesUtil.bytes2HexString(bytes);
                            String appList = resultDate.substring(6);
                            final String[] appData = appList.split("A");

                            for (int i = 0; i < appData.length; i++)
                                appData[i] = BytesUtil.fromUtf8(BytesUtil.hexString2Bytes(appData[i]));


                            AlertDialog.Builder dialog =  new AlertDialog.Builder(mActivity);

                            dialog.setTitle("Select an Application");
                            dialog.setSingleChoiceItems(appData, 0, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    resultBytes[0] = (byte) which;
                                    ints[0]=1;
                                    countDownLatch.countDown();
                                    dialog.dismiss();
                                    appName = appData[which];
                                }
                            }).create().show();
                        }
                    });
                    break;
            }
            return 0;
        }
    };


    //constants which defines the transaction results
    public static final String  ARC_OFFLINEAPPROVED       =    "Y1";   //脱机成功
    public static final String  ARC_OFFhLINEDECLINED       =    "Z1";   //脱机拒绝
    public static final String  ARC_REFERRALAPPROVED      =    "Y2";   //
    public static final String  ARC_REFERRALDECLINED      =    "Z2";   //
    public static final String  ARC_ONLINEFAILOFFLINEAPPROVED ="Y3";   //联机失败 脱机成功
    public static final String  ARC_ONLINEFAILOFFLINEDECLINED ="Z3";   //联机失败 脱机拒绝


    void getEMVTransactionResult()
    {
        byte []outBytes = new byte[2];
        int []outLen = new int[1];

        int result;
        try
        {
           result =  Services.emvCore.getTLV(0x8A,outBytes,outLen);
        }catch (Exception ex)
        {
           ex.printStackTrace();
           return;
        }

        if (result != 0) return;

        String _8A_str = BytesUtil.fromBytes(outBytes);
        if (outBytes[0] > 0)
            currentTransaction.emvResult = _8A_str;

    }

    private static int saveScriptResult() throws RemoteException {
        Log.v("saveScriptResult","保存脚本");
        byte[] outData = new byte[256];
        int[]  outDataLen = new int[1];
        int result = Services.emvCore.getScriptResult(outData,outDataLen);
        Log.d("emvCore", "getScriptResult: "+result);
        if(result == 0) {
            Services.emvCore.setTLV(0xDF31,outData);
            //保存脚本上送报文  需要改

        }
        return result;
    }

    private static final int CTLS_SWITCH_INTERFACE = 1;

    private int analyseCTLSResult() {
        byte[] outData =  new byte[10];
        int [] outDataLen = new int[2];
        String strResult = "";

        int result = -1;

        if (currentTransaction.inChipStatus != Transaction.ChipStatusTypes.CTLS_CARD)
            return -1;

        try {
            result = Services.emvCore.getTLV(0x9F6C,outData,outDataLen);
        } catch (RemoteException ex) {
            ex.printStackTrace();
            return -1;
        }

        if (result == 0 || outDataLen[0] > 0) {
            strResult = Utility.byte2HexStr(outData,0,outDataLen[0]);

            if(strResult.startsWith("10"))
                return CTLS_SWITCH_INTERFACE;
        }

        return -1;
    }

    public static String TSI = "";
    public static String TVR = "";

    public static int getCVMAnalyzedResult() {
        if (currentTransaction.inChipStatus != Transaction.ChipStatusTypes.EMV_CARD)
            return 0;

        String cvmResult = "";

        //get the applied cvm result
        try {
            byte [] data =  new byte[10];
            int [] len = new int[1];

            int result = Services.emvCore.getTLV(0x9F34,data,len);

            if (result != 0)
                return 0;

            cvmResult = Utility.byte2HexStr(data,0,len[0]);
        }catch (RemoteException ex)
        {
            ex.printStackTrace();
        }

        if (cvmResult != null)
        {
            if((currentTransaction.encryptedPINBlock != null) && (currentTransaction.getCardLabel().equals("CUP"))) {
                return PIN_VERIFIED;
            }

            if (    cvmResult.startsWith("5E") ||   //signature required
                    cvmResult.startsWith("1E") ||   //signature required
                    cvmResult.startsWith("03"))     //signature + pin
                return SIGNATURE_REQUIRED;
            else if (
                    cvmResult.startsWith("41") ||
                            cvmResult.startsWith("42") ||
                            cvmResult.startsWith("20") ||
                            cvmResult.startsWith("44")
            )
                return PIN_VERIFIED;
        }

        return 0;
    }


    public static String CVR = "";
    public static String appID = "";
    public static String appLabel = "";

    private static int ___8drrd3148796d_Xaf() {
        boolean thisOne = false;
        int thisOneCountDown = 1;
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        for(StackTraceElement element : elements) {
            String methodName = element.getMethodName();
            int lineNum = element.getLineNumber();
            if (thisOne && (thisOneCountDown == 0)) {
                return lineNum;
            } else if (thisOne) {
                thisOneCountDown--;
            }
            if (methodName.equals("___8drrd3148796d_Xaf")) {
                thisOne = true;
            }
        }
        return -1;
    }

    public static String extractDataForTag(String coreTlvMessage, int tag) {
        String sTag = Integer.toHexString(tag);
        sTag = sTag.toUpperCase();

        int index = 0;
        int dataLen = 0;

        if ((index = coreTlvMessage.indexOf(sTag)) >= 0) //tag is found
        {
            //so we extract the tag
            int startPos = index + sTag.length();
            String sTagLen = coreTlvMessage.substring(startPos, startPos + 2);
            dataLen = Integer.valueOf(sTagLen, 16);

            dataLen *= 2;
            if (dataLen > 254)
                return null;
            //extract the data portion relevant to the tag
            startPos += 2;
            String data = "";
            try {
                Log.d("MMMMMMMM coreTlvMessage", " : " + coreTlvMessage);
                data = coreTlvMessage.substring(startPos, startPos + dataLen);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            Log.d("MMMMMMMM Return", " : " + data);
            return data;
        }

        return null;
    }

    public static void writeISOLogOnDiskFile(byte[] packet, MODE mde) {
        File filePath = appContext.getFilesDir();
        String path = filePath.getPath();
        Log.d("LOG FILE PATH", " : " + path);
        path += "/Secured";

        File dir = new File(path);

        if (!dir.exists())
            dir.mkdirs();

        File IsoLogFile = null;

        Log.d("LOG FILE packet", " : " + packet);
        if (mde == MODE.SEND)
            IsoLogFile = new File(path, ISO_LOG_PATH_SEND);
        else if (mde == MODE.RECIEVE)
            IsoLogFile = new File(path, ISO_LOG_PATH_RECV);

        if (packet == null) {
            IsoLogFile.delete();
            return;
        }

        String commData = Utility.byte2HexStr(packet);
        Log.d("LOG FILE commData", " : " + commData);

        //strip off the first two byte[packet length]
        commData = commData.substring(4);

        try {
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(IsoLogFile));

            String title = "";
            if (mde == MODE.SEND)
                title = "\n------------ SEND ------------\n";
            else
                title = "\n\n----------- RECEIVE ----------\n";

            fileWriter.write(title);

            int extractLen = 20;
            int start = 0;
            int offset = extractLen;

            while (offset <= commData.length()) {
                String line = commData.substring(start, offset);

                String formatted = "";
                //formatting the extracted line
                for (int i = 0; i < line.length(); i += 2)
                    formatted += line.substring(i, i + 2) + " ";

                //append end of the line
                formatted += "\n";

                start = offset;
                offset += extractLen;

                Log.d("FORMATTED", " : " + formatted);

                fileWriter.write(formatted);
            }

            if(start < commData.length()) {
                String line = commData.substring(start, commData.length());
                String formatted = "";
                //formatting the extracted line
                for (int i = 0; i < line.length(); i += 2)
                    formatted += line.substring(i, i + 2) + " ";

                //append end of the line
                formatted += "\n";
                fileWriter.write(formatted);
            }

            fileWriter.flush();
            fileWriter.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String getISOLogData(MODE mde) {
        String data = "";
        File filePath = appContext.getFilesDir();
        String path = filePath.getPath();
        path += "/Secured";

        File isoLogFile = null;

        if (mde == MODE.SEND)
            isoLogFile = new File(path, ISO_LOG_PATH_SEND);
        else if (mde == MODE.RECIEVE)
            isoLogFile = new File(path, ISO_LOG_PATH_RECV);

        try {
            BufferedReader fileReader = new BufferedReader(new FileReader(isoLogFile));
            String line = "";
            while ((line = fileReader.readLine()) != null)
                data += (line + "\n");

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return data;

    }

    private  void analyseEMVResult() throws RemoteException {
        byte[] outData = new byte[10];
        int[] outDateLen = new int[1];
        char ucMask = 0x80;
        int result = Services.emvCore.getTLV(0x9B,outData,outDateLen);

        String tag9B = Utility.byte2HexStr(outData);

        TSI = tag9B;

        if (result == 0) {
            for (int i = 8; i >= 3; i--) {
                if ((outData[0] & ucMask) > 0) {
                    switch (i) {
                        case 8:
                            Log.v("analyseEMVResult", "offline data authentication has been carried out");
                            break;

                        case 7:
                            Log.v("analyseEMVResult", "Cardholder certification has been carried out");
                            break;

                        case 6:
                            Log.v("analyseEMVResult", "Card risk management has been carried out");
                            break;

                        case 5:
                            Log.v("analyseEMVResult", "Issuing card certification has been carried out");
                            break;

                        case 4:
                            Log.v("analyseEMVResult", "Terminal risk management has been carried out");
                            break;

                        case 3:
                            Log.v("analyseEMVResult", "Script processing has been performed");
                            break;

                        default:
                            break;
                    }
                    Log.v("analyseEMVResult", "\n");
                }
                ucMask >>= 1;
            }
        }

        outData = new byte[10];
        outDateLen=new int[1];

        result = Services.emvCore.getTLV(0x95,outData,outDateLen);
        String tag95 = Utility.byte2HexStr(outData);
        TVR = tag95;

        ucMask = 0x80;

        if (result ==0) {
            for (int i = 8; i >= 3; i--) {
                if ((outData[0] & ucMask) > 0) {
                    switch (i) {
                        case 8:
                            Log.v("analyseEMVResult", "Offline data authentication not performed");
                            break;

                        case 7:
                            Log.v("analyseEMVResult", "Offline static data authentication failed");
                            break;

                        case 6:
                            Log.v("analyseEMVResult",  "IC Card data missing");
                            break;

                        case 5:
                            Log.v("analyseEMVResult", "The card appears in the terminal exception file");
                            break;

                        case 4:
                            Log.v("analyseEMVResult", "Offline dynamic data authentication failed");
                            break;

                        case 3:
                            Log.v("analyseEMVResult",  "Composite dynamic data authentication / application password generation failed");
                            break;

                        default:
                            break;
                    }
                    Log.v("analyseEMVResult",  "\n");
                }
                //右移一位
                ucMask >>= 1;
            }
        }
        ucMask = 0x80;

        if (result == 0) {
            for (int i = 8; i >= 4; i--) {
                if ((outData[1] & ucMask) > 0) {
                    switch (i) {
                        case 8:
                            Log.v("analyseEMVResult",  "IC Card and terminal application versions are inconsistent");
                            break;

                        case 7:
                            Log.v("analyseEMVResult",  "App has expired");
                            break;

                        case 6:
                            Log.v("analyseEMVResult", "App has not yet taken effect");
                            break;

                        case 5:
                            Log.v("analyseEMVResult", "Card products do not allow the requested service");
                            break;

                        case 4:
                            Log.v("analyseEMVResult",  "New card");
                            break;

                        default:
                            break;

                    }
                    Log.v("analyseEMVResult",  "\n");
                }
                //右移一位
                ucMask >>= 1;
            }
        }
        ucMask = 0x80;

        if (result == 0) {
            for (int i = 8; i >= 3; i--) {
                if ((outData[2] & ucMask) > 0) {
                    switch (i) {
                        case 8:
                            Log.v("analyseEMVResult",  "Cardholder verification was unsuccessful");
                            break;

                        case 7:
                            Log.v("analyseEMVResult",  "Unknown CVM");
                            break;

                        case 6:
                            Log.v("analyseEMVResult",  "PIN Retry count exceeded");
                            break;

                        case 5:
                            Log.v("analyseEMVResult",  "Request input PIN, But no PIN pad or PIN pad failure");
                            break;

                        case 4:
                            Log.v("analyseEMVResult",  "Ask for PIN, PIN pad, but no PIN");
                            break;

                        case 3:
                            Log.v("analyseEMVResult",  "Enter online PIN");
                            break;

                        default:
                            break;

                    }
                    Log.v("analyseEMVResult",  "\n");
                }
                //右移一位
                ucMask >>= 1;
            }
        }
        ucMask = 0x80;

        if (result == 0) {
            for (int i = 8; i >= 3; i--) {
                if ((outData[3] & ucMask) > 0) {
                    switch (i) {
                        case 8:
                            Log.v("analyseEMVResult",  "Transaction exceeds minimum");
                            break;

                        case 7:
                            Log.v("analyseEMVResult",  "Exceeding the limit of continuous offline transactions");
                            break;

                        case 6:
                            Log.v("analyseEMVResult",  "Exceeded the continuous offline transaction limit");
                            break;

                        case 5:
                            Log.v("analyseEMVResult",  "Transactions are randomly selected for online processing");
                            break;

                        case 4:
                            Log.v("analyseEMVResult",  "Merchants request online transactions");
                            break;

                        default:
                            break;

                    }
                    Log.v("analyseEMVResult",  "\n");
                }
                //右移一位
                ucMask >>= 1;
            }
        }
        ucMask = 0x80;
        if (result == 0) {
            for (int i = 8; i >= 5; i--) {
                if ((outData[4] & ucMask) > 0) {
                    switch (i) {
                        case 8:
                            Log.v("analyseEMVResult",  "Use default TDOL");
                            break;

                        case 7:
                            Log.v("analyseEMVResult",  "Issuer authentication failed");
                            break;

                        case 6:
                            Log.v("analyseEMVResult",  "Script processing failed before the last time the GENERATE AC command was generated");
                            break;

                        case 5:
                            Log.v("analyseEMVResult",  "Script processing failed after the last application password (AC) command was generated");
                            break;

                        default:
                            break;

                    }
                    Log.v("analyseEMVResult",  "\n");
                }
                //右移一位
                ucMask >>= 1;
            }
        }

        //get the issuer authentication data CVR
        result = Services.emvCore.getTLV(0x9F10,outData,outDateLen);

        if (result == 0 )
            CVR = Utility.byte2HexStr(outData, 0, outDateLen[0]);

        return;
    }

    int checkKernalOutcome() {
        try {
            byte[] outData = new byte[200];
            int[] lens = new int[2];

            int res = Services.emvCore.getOutCome(outData, lens);
            if (res == 0) {
                String val = Utility.byte2HexStr(outData, 0, lens[0]);
                Log.d("BBBBBBBBB", "01");
                val = extractDataForTag(val, 0xDF8116);
                Log.d("B01", " : " + val);
                if (val.startsWith("20"))
                    return SEE_PHONE;
            }


        } catch (Exception ex) {
        }

        return -1;
    }

    public void processTransactionEMV(byte[] callBackData, int[] callBackLen) {
        if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.EMV_CARD ||
                currentTransaction.inChipStatus == Transaction.ChipStatusTypes.CTLS_CARD) {

            byte[] outData = new byte[6];
            int[] outDataLen = new int[1];

            try
            {
                //set the terminal tag transaction type here
                byte tranType = 0x00;
                if (currentTransaction.inTransactionCode == TranStaticData.TranTypes.SALE)
                    tranType = 0x00;

                Services.emvCore.setTLV(0x9C,new byte[]{tranType});

                //read in the field 55 data
                outData = new byte[512];
                outDataLen  = new int[1];

                //it is possible to get the different types of tlv lists for each
                //individual issuer for now all available emv tags have been loaded

                //here we must load tlv value for each and every issuer  cuz some
                //issuers have different tags have been defined each issuer

                int []tagList = null;

                if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.EMV_CARD)
                {
                    if (isCardLabel("visa"))
                        tagList = VISA_TagList;
                    else if (isCardLabel("master"))
                        tagList = MASTER_TagList;
                    else if (isCardLabel("amex"))
                        tagList = AMEX_TagList;
                    else if (isCardLabel("cup"))
                        tagList = CUP_TagList;
                    else if (isCardLabel("jcb"))
                        tagList = JCB_TagList;
                    else
                        tagList = VISA_TagList;
                }
                else if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.CTLS_CARD)
                {
                    if (isCardLabel("visa"))
                        tagList = VISA_TagList_CTLS;
                    else if (isCardLabel("master"))
                        tagList = MASTER_TagList_CTLS;
                    else if (isCardLabel("amex"))
                        tagList = AMEX_TagList_CTLS;
                    else if (isCardLabel("cup"))
                        tagList = CUP_TagList_CTLS;
                }

                //transaction optimized
                currentTransaction.emv55Data =  build55DataFromCoreTLV(tagList);

                //check code
                if (currentTransaction.getCardLabel().equals("CUP") && currentTransaction.inChipStatus == Transaction.ChipStatusTypes.CTLS_CARD) {
                    String data = Utility.getValueofTag(currentTransaction.emv55Data,"5F34");
                    if (data == null) {
                        int result = 0;
                        outData = new byte[12];
                        result = Services.emvCore.getTLV(0x5F34,outData,outDataLen);
                        if (result == 0) {
                            int iLen  = outDataLen[0];
                            String len = String.valueOf(iLen);
                            len = Formatter.fillInFront("0",len,2);

                            String value = Utility.byte2HexStr(outData, 0, iLen);
                            String TLV = "5F34" + len + value;
                            currentTransaction.emv55Data = TLV + currentTransaction.emv55Data;
                        }
                    }
                }

                //set the application ID
                Log.d("BBBBBBBBB", "02");
                appID = extractDataForTag(currentTransaction.emv55Data, 0x84);
                appLabel = getTagData(0x50);  //specific tag retrieving from the emv heap
                if (appLabel != null && !appLabel.isEmpty())
                    appLabel = Utility.asciiToString(appLabel);

                //write the diagnosis report
                if (SettingsInterpreter.isEMVDiagnosisEnabled())
                {
                    byte data[] = new byte[512];
                    int lenData[] = new int[2];

                    try {
                        int result = Services.emvCore.getCoreTLVMessage(data, lenData);
                    } catch (RemoteException ex) {
                        ex.printStackTrace();
                    }

                    //get the length of the data
                    String coreTLVString = Utility.byte2HexStr(data);
                    String sLen = coreTLVString.substring(0, 8);
                    int len = Integer.valueOf(sLen, 16);

                    //strip off the length data from the original data
                    coreTLVString = coreTLVString.substring(8);
                    coreTLVString = coreTLVString.substring(0,len * 2);

                    writeChipTranTagsOnFile(coreTLVString);
                }
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
        }
    }

    private String extractTagAndData(String coreTlvMessage,int tag) {
        String sTag = Integer.toHexString(tag);
        sTag = sTag.toUpperCase();
        int index = 0 ;
        int dataLen = 0 ;
        int startPos = 0 ;

        while (true) {
            if ((index = coreTlvMessage.indexOf(sTag,startPos)) > 0) {//tag is found
                //so we extract the tag
                startPos = index + sTag.length();
                String sTagLen = coreTlvMessage.substring(startPos,startPos + 2);
                dataLen = Integer.valueOf(sTagLen,16);

                if (dataLen == 0)
                    continue;

                if((sTag.equals("82") && dataLen != 2) || (sTag.equals("9A") && dataLen != 3)) {
                    continue;
                }

                dataLen *= 2;
                if (dataLen > 20)
                    return null;
                //extract the data portion relevant to the tag
                startPos += 2;
                String data = coreTlvMessage.substring(startPos, startPos + dataLen);

                if(sTag.equals("9F1A") && data.equals("0000")) {
                    data = "0144";
                }
                sTag = sTag + sTagLen + data;
                return sTag;
            } else
                return null;
        }
    }

    private String getTagData(int tag) {
        byte[] data = new byte[30];
        int[] dataLen = new int[2];
        int result = 0;
        try {
            result = Services.emvCore.getTLV(tag, data, dataLen);

        } catch (RemoteException ex) {
            ex.printStackTrace();
        }

        if (result != 0)
            return null;

        return Utility.byte2HexStr(data, 0, dataLen[0]);
    }

    private String build55DataFromCoreTLV(int[] tagList) {

        byte data[] = new byte[512];
        int lenData[] = new int[2];

        try {
            int result = Services.emvCore.getCoreTLVMessage(data, lenData);
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }

        //get the length of the data
        String hexData = Utility.byte2HexStr(data);
        String sLen = hexData.substring(0, 8);
        int len = Integer.valueOf(sLen, 16);

        //strip off the length data from the original data
        hexData = hexData.substring(8);
        hexData = hexData.substring(0, len * 2);


        //find each tag from the retrieved tag String message
        String exTagData = null;
        String data55 = "";

        for (int tag : tagList) {
            String hexTag = Integer.toHexString(tag);
            try {
                int result = Services.emvCore.getTLV(tag, data, lenData);
                if (result == 0) {
                    int tagLen = lenData[0];
                    if (tagLen == 0) {
                        exTagData = null;
                    }
                    else {
                        exTagData = Utility.constructTLV(tag, data, tagLen);
                    }
                }
                else {
                    exTagData = null;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            if (exTagData == null) {
                //the tag is not found . so we request it from the emvcore
                exTagData = extractTagAndData(hexData, tag);
                if(exTagData == null) {
                    continue;
                }
            }

            data55 += exTagData;
        }

        return data55;
    }

    //this is the main routine which is called to process thr transaction after detecting particular card type
    public int processTransaction(Activity activity) {
        mActivity = activity;
        //get the amount from the user for chip trans
        if ((currentTransaction.inChipStatus == Transaction.ChipStatusTypes.EMV_CARD) && (!MainActivity.ecr.isECRInitiated) && (GlobalData.globalTransactionAmount == 0)) {
            invokeAmountInputScreen();
            if (!getResultFromInputAmountScreen())
                return GENERIC_ERROR_TRAN_MIDDLE;
        }
        selectMerchant();

        GlobalData.globalResult = 0;
        currentTransaction.lnBaseTransactionAmount = GlobalData.globalTransactionAmount;
        GlobalData.globalTransactionAmount = 0;

        Log.d("DDDDDDDDDDDDD", "DDDDDDDDDDDDDDDD");
        Log.d("terminalID", " : " + currentTransaction.terminalID);
        Log.d("merchantID", " : " + currentTransaction.merchantID);
        Log.d("merchantNumber", " : " + currentTransaction.merchantNumber);

        int baseIssuer = IssuerHostMap.hosts[0].baseIssuer;
        String selectQuary = "SELECT MerchantName,MerchantID,TerminalID FROM TMIF,MIT WHERE TMIF.IssuerNumber = " + baseIssuer +
                " AND MIT.MerchantNumber = TMIF.MerchantNumber";
        Cursor rec = null;
        String tid = "";
        String mid = "";
        String mName = "";

        try {
            rec = DBHelper.getInstance(appContext).readWithCustomQuary(selectQuary);
            rec.moveToFirst();
            tid = rec.getString(rec.getColumnIndex("TerminalID"));
            mid = rec.getString(rec.getColumnIndex("MerchantID"));
            mName = rec.getString(rec.getColumnIndex("MerchantName"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Log.d("tid", " : " + tid);
        Log.d("mid", " : " + mid);
        Log.d("mName", " : " + mName);
        Log.d("DDDDDDDDDDDDD", "DDDDDDDDDDDDDDDD");

        try {
            if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.EMV_CARD) {
                Log.d("TRNAS CARD TYPE", " EMV_CARD");

                Services.emvCore.transInit();

                EmvParam emvParam = new EmvParam(appContext);
                emvParam.setTransType(0x02);
                emvParam.setTransType9C("00");
                emvParam.setMerchId(mid);
                emvParam.setTermId(tid);
                emvParam.setMerchName(mName);
                emvParam.setTransCurrCode("144");
                emvParam.setSupportPSESel(1);
                Services.emvCore.setParam(emvParam.toByteArray());
            } else if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.CTLS_CARD) {
                Log.d("TRNAS CARD TYPE", " CTLS_CARD");
                byte[] outData = new byte[1024];
                int[] status = new int[2];

                Services.emvCore.transInit();

                EmvParam emvParam = new EmvParam(appContext);
//              emvParam.setTransType(0x02);
                emvParam.setTransType9C("00");
                emvParam.setMerchId(mid);
                emvParam.setTermId(tid);
                emvParam.setMerchName(mName);
                emvParam.setForceOnline(1);
                emvParam.setTerminalType(0x22);
                emvParam.setTransCurrCode("144");
                emvParam.setTransType(0x00);

                Services.emvCore.setParam(emvParam.toByteArray());

                int result = Services.emvCore.qPBOCPreProcess(iCallBackListener);

                if (result != 0) {
                    showToast("CTLS Pre Processing failed,Aborting...", TOAST_TYPE_FAILED);
                    errorTone();
                    return GENERIC_ERROR_TRAN_MIDDLE;
                }
            }

            int result = -1;
            int slot = 0;

            if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.EMV_CARD)
                slot = 0x01;
            else if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.CTLS_CARD)
                slot = 0x02;
            else
                return GENERIC_ERROR_TRAN_MIDDLE;

            String currencyCode = "";

            result = Services.emvCore.appSel(slot, 1, iCallBackListener);
            Log.d("-------", "-----------");
            Log.d("result111111", " : " + result);
            Log.d("-------", "-----------");

            if (result == EMV_SUCCESS) {
                if(Services.emvCore.getPath() == 2) { //JCB
                    try {
                        //This interface only works for american express ;
                        //function: Set up app networking status
                        ConnectivityManager connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                        if (connectivityManager == null || connectivityManager.getActiveNetworkInfo() == null) {
                            Services.emvCore.setAppOnlineStatus_AMEX(false);
                            Log.d(TAG, "WIFI is disconnect");
                        } else {
                            Services.emvCore.setAppOnlineStatus_AMEX(true);
                            Log.d(TAG, "WIFI is connected");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Log.d(TAG, "please add the android.permission.ACCESS_NETWORK_STATE");
                    }
                    int res;
                    res = Services.emvCore.procCLTrans_JCB(iCallBackListener);
                    Log.d("TransProcess", "procCLTrans_JCB: " + res);

                    return 0;// JCB tran
                }
                else if(Services.emvCore.getPath() == 7) { //AMEX
                    try {
                        //This interface only works for american express ;
                        //function: Set up app networking status
                        ConnectivityManager connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                        if (connectivityManager == null || connectivityManager.getActiveNetworkInfo() == null) {
                            Services.emvCore.setAppOnlineStatus_AMEX(false);
                            Log.d(TAG, "WIFI is disconnect");
                        } else {
                            Services.emvCore.setAppOnlineStatus_AMEX(true);
                            Log.d(TAG, "WIFI is connected");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        Log.d(TAG, "please add the android.permission.ACCESS_NETWORK_STATE");
                    }
                    Log.d(TAG, "procCLTrans_AMEX start: ");

                    int amexCallbackResult = Services.emvCore.procCLTrans_AMEX(iCallBackListener);

                    Log.d(TAG, "procCLTrans_AMEX result: " + amexCallbackResult);

                    //read the track 2 and parse pan based on the kernal
                    getEMVTransInfo();
                    return GENERIC_ERROR_TRAN_MIDDLE;
                }
            }

            if (SettingsInterpreter.isEMVDiagnosisEnabled())
            {
                byte data[] = new byte[512];
                int lenData[] = new int[2];

                try {
                    int res = Services.emvCore.getCoreTLVMessage(data, lenData);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }

                //get the length of the data
                String coreTLVString = Utility.byte2HexStr(data);
                String sLen = coreTLVString.substring(0, 8);
                int len = Integer.valueOf(sLen, 16);

                //strip off the length data from the original data
                coreTLVString = coreTLVString.substring(8);
                coreTLVString = coreTLVString.substring(0,len * 2);

                writeChipTranTagsOnFile(coreTLVString);
            }

            if (result == ERR_EMVRSP) {//fall back scenario
                showToast("Sorry, Try card swipe", TOAST_TYPE_WARNING);
                GlobalData.isFallback = true;
                GlobalData.lnFallBackStartedTime = SystemClock.elapsedRealtime();
                GlobalData.lnFallbackAmount = currentTransaction.lnBaseTransactionAmount;
                forceRemoveCard();
                return GENERIC_ERROR_TRAN_MIDDLE;
            }
            else if (result == APPLICATION_BLOCK) {//get processing option failed scenario
                showToast("Sorry, Not Accepted", TOAST_TYPE_WARNING);
                forceRemoveCard();
                return GENERIC_ERROR_TRAN_MIDDLE;
            }
            else if (result == 3) {// ctls transaction limit reached
                showToast("Transaction Limit,Please Insert Card", TOAST_TYPE_INFO);
                return GENERIC_ERROR_TRAN_MIDDLE;
            }
            else if (result == ERR_USERCANCEL) {//ctls try again
                showToast("Transaction Aborted,Try Again", TOAST_TYPE_INFO);
                return GENERIC_ERROR_TRAN_MIDDLE;
            }
            else if (result == -34) {//cdcvm for cup
                byte[] outData = new byte[100];
                int[] len = new int[2];

                result = Services.emvCore.getOutCome(outData, len);

                if (result == 0) {
                    String msgIdentifier = Utility.byte2HexStr(outData, 0, len[0]);
                    Log.d("LLLLLLLL", "03");
                    msgIdentifier = extractDataForTag(msgIdentifier, 0xDF8116);

                    if (msgIdentifier != null) {
                        msgIdentifier = msgIdentifier.substring(0, 2); //get the first byte of the tag
                        byte bMsgId = Byte.valueOf(msgIdentifier);

                        if (bMsgId == SEE_PHONE) {
                            GlobalData.globalTransactionAmount = currentTransaction.lnBaseTransactionAmount;
                            updateStatusText("See the Phone");
                            showToast("See the Phone", TOAST_TYPE_INFO);

                            sleepMe(4000);

                            updateStatusText("Try Again");
                            showToast("Try Again", TOAST_TYPE_INFO);
                            sleepMe(1000);

                            return TRY_AGAIN;
                        }
                    }
                }
            }
            else if (result == ERR_EMVDENIAL) {
                if(!GlobalData.isForceRev) {
                    showToast("Declined - ERR_EMVDENIAL", TOAST_TYPE_WARNING);
                }
                else {
                    GlobalData.isForceRev = false;
                }
                forceRemoveCard();
                return GENERIC_ERROR_TRAN_MIDDLE;
            }
            else if (result != 0)  {//if other wise not success
                showToast("App selection failed", TOAST_TYPE_WARNING);
                forceRemoveCard();
                return GENERIC_ERROR_TRAN_MIDDLE;
            }

            if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.EMV_CARD) {
                currencyCode = "0144";
                result = Services.emvCore.setTLV(0x5f2a, Utility.hexStr2Byte(currencyCode));
            }

            //read the application data by initiating the start of the emv process
            startBusyAnimation("Reading Application Data...");
            result = Services.emvCore.readAppData(iCallBackListener);
            stopBusyAnimation();
            Log.d("***********", "*************");
            Log.d("emvCore result ", " : " + result);
            Log.d("***********", "*************");

            if (result == 1002)        //paypass transaction limit
            {
                byte[] out = new byte[20];
                int[] len = new int[2];
                int res = Services.emvCore.getSelectedAID(out, len);

                if (res == 0) {
                    String aid = Utility.byte2HexStr(out, 0, len[0]);
                    if (aid.contains("A0000000041010") ||
                            aid.contains("A0000000101030")) {
                        showToast("Try contact interface", TOAST_TYPE_INFO);
                        return 0;
                    }
                }
            }

            if (result != 0) {
                if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.CTLS_CARD) {
                    byte[] outData = new byte[100];
                    int[] len = new int[2];

                    result = Services.emvCore.getOutCome(outData, len);
                    Log.d("AAAAAAAAAAAA", "AAAAAAAAA");
                    Log.d("result", ": " + result);
                    Log.d("AAAAAAAAAAAA", "AAAAAAAAA");

                    if (result == 0) {
                        String msgIdentifier = Utility.byte2HexStr(outData, 0, len[0]);
                        Log.d("LLLLLLLL", "01");
                        msgIdentifier = extractDataForTag(msgIdentifier, 0xDF8116);

                        if (msgIdentifier != null) {
                            msgIdentifier = msgIdentifier.substring(0, 2); //get the first byte of the tag
                            byte bMsgId = Byte.valueOf(msgIdentifier);

                            if (bMsgId == SEE_PHONE) {
                                GlobalData.globalTransactionAmount = currentTransaction.lnBaseTransactionAmount;
                                updateStatusText("See the Phone");
                                showToast("See the Phone", TOAST_TYPE_INFO);

                                sleepMe(4000);

                                updateStatusText("Try Again");
                                showToast("Try Again", TOAST_TYPE_INFO);
                                sleepMe(1000);

                                return TRY_AGAIN;
                            }
                        }
                    }
                }

                showToast("Error Reading Application Data", TOAST_TYPE_FAILED);
                errorTone();
                forceRemoveCard();
                return GENERIC_ERROR_TRAN_MIDDLE;
            }

            /*if (SettingsInterpreter.isEMVDiagnosisEnabled())
            {
                byte data[] = new byte[512];
                int lenData[] = new int[2];

                try {
                    int res = Services.emvCore.getCoreTLVMessage(data, lenData);
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }

                //get the length of the data
                String coreTLVString = Utility.byte2HexStr(data);
                String sLen = coreTLVString.substring(0, 8);
                int len = Integer.valueOf(sLen, 16);

                //strip off the length data from the original data
                coreTLVString = coreTLVString.substring(8);
                coreTLVString = coreTLVString.substring(0,len * 2);

                writeChipTranTagsOnFile(coreTLVString);
            }*/

            //perform the card offline data authentication
            updateStatusText("Authenticating the Card");
            if (0 != Services.emvCore.cardAuth()) {
                showToast("Error Card Authentication", TOAST_TYPE_FAILED);
                errorTone();
                forceRemoveCard();
                return GENERIC_ERROR_TRAN_MIDDLE;
            }

            updateStatusText("Reading EMV Data");
            //read the basic card related information from the card
            if (0 != getEMVTransInfo()) {
                showToast("Getting track information failed", TOAST_TYPE_FAILED);
                errorTone();
                forceRemoveCard();
                return GENERIC_ERROR_TRAN_MIDDLE;
            }

            if (MainActivity.ecr.isECRInitiated) {
                int ecrRet = 0;

                if ((ecrRet = initiateECR()) != SUCCESS) {
                    forceRemoveCard();
                    return GENERIC_ERROR_TRAN_MIDDLE;
                }

                invokeAmountInputScreen();
                if (!getResultFromInputAmountScreen()) {
                    forceRemoveCard();
                    return GENERIC_ERROR_TRAN_MIDDLE;
                }

                currentTransaction.lnBaseTransactionAmount = GlobalData.globalTransactionAmount;
                GlobalData.globalTransactionAmount = 0;
            }

            //currentTransaction.PAN = "5413330089704317";
            if (currentTransaction.PAN == null || currentTransaction.PAN.length() == 0) {
                showToast("PAN reading error ", TOAST_TYPE_FAILED);
                errorTone();
                forceRemoveCard();
                return GENERIC_ERROR_TRAN_MIDDLE;
            }

            updateStatusText("Loading card data");
            Log.d("Loading///////", "01 : ");
            //here we search for the bin block
            if (!loadCardAndIssuer()) {
                Log.d("Loading///////", "02");
                showToast("Reading card and issuer failed", TOAST_TYPE_FAILED);
                errorTone();
                forceRemoveCard();
                return GENERIC_ERROR_TRAN_MIDDLE;
            }

            if (SettingsInterpreter.isSingleMerchantEnabled() || (SettingsInterpreter.isECREnabled() && MainActivity.ecr.isECRInitiated)) {
                GlobalData.selectedMerchant = Base.getFirstMerchantOfIssuer(GlobalData.selectedIssuer);
                selectMerchant();
            }

            //change the processing code for amex ntb processing code
            if (currentTransaction.getCardLabel().equals("AMEX"))
                currentTransaction.procCode = "004000";

            //set the appropriate package name
            setPackageName();

            //getting the trans path
            int path = Services.emvCore.getPath();

            Services.core.configEMVPINSetting(getCurrentContextPackageName(),
                    60,
                    4,
                    12,
                    0x00,
                    false,
                    true,
                    false,
                    true, true, true
            );

            if (path == PATH_QPBOC || path == PATH_MAG || path == PATH_CHIP) {
                result = Services.emvCore.procQPBOCTrans(iCallBackListener);
                Log.d("KKKKKKK", "-------------RR01 : "+result);
            }
            /*else if(path == PATH_JCB) {
                result = Services.emvCore.procCLTrans_JCB(iCallBackListener);
                Log.d("KKKKKKK", "-------------RR01 : "+result);
            }*/
            else {
                result = Services.emvCore.procTrans(iCallBackListener);
                Log.d("KKKKKKK", "-------------RR02 : "+result);
            }

            Log.d("Invi_PIN", "Continue");
            //Auto reversal
            if ((GlobalData.globalResult == PUSH_REVERSAL) && (SettingsInterpreter.isAutoReversalEnabled())) {
                if (isReversal && !currentTransaction.getCardLabel().equals("AMEX")) {
                    isReversal = false;
                    if (REVERSAL_FAILED == reversalHandler.pushpPendingReversal())
                        if(!SettingsInterpreter.isForceReversalEnabled())
                            showToast("Auto Reversal Failed", TOAST_TYPE_FAILED);
                }

                GlobalData.globalResult = 0;
                forceRemoveCard();

                return TERMINATE_TRANSACTION;
            }

            if (checkKernalOutcome() == SEE_PHONE) {
                GlobalData.globalTransactionAmount = currentTransaction.lnBaseTransactionAmount;
                updateStatusText("See the Phone");
                showToast("See the Phone", TOAST_TYPE_INFO);

                sleepMe(4000);

                updateStatusText("Try Again");
                showToast("Try Again", TOAST_TYPE_INFO);
                sleepMe(1000);

                return TRY_AGAIN;
            }

            Log.d("KKKKKKK", "-------------XXXXX");
            if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.CTLS_CARD &&
                    currentTransaction.getCardLabel().equals("MASTER")) {
                Log.d("KKKKKKK", "-------------XXXXX 02");
                Log.d("KKKKKKK", "-------------result : " + currentTransaction.responseCode);
                Log.d("KKKKKKK", "-------------responseCode : " + result);
                //handling the card declining
                if (result == -24 || currentTransaction.responseCode.equals("65")) {
                    //card declining
                    showToast("Try contact interface", TOAST_TYPE_INFO);
                    return 0;
                }
            }

            Log.d("KKKKKKK", "-------------XXXXX-01");
            //we don't process emv related data if it is an offline  typ0e transaction
            if (currentTransaction.inTransactionCode == TranStaticData.TranTypes.PRE_COMP ||
                    currentTransaction.inTransactionCode == TranStaticData.TranTypes.OFFLINE_SALE) {
                Log.d("KKKKKK", "-------------XXXXX-03");
                Receipt receipt = Receipt.getInstance();
                receipt.printReceipt(0);
                forceRemoveCard();
                return SUCCESS;
            }

            Log.d("KKKKKK", "-------------XXXXX-04");
            //printISOLogs();
            Log.d("KKKKKK", "-------------XXXXX-05");
            if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.EMV_CARD){
                Log.d("KKKKKK", "-------------XXXXX-06");
                analyseEMVResult();
            }
            Log.d("KKKKKK", "-------------XXXXX-07 : "+result);

            int ctlsResult = analyseCTLSResult();
            Log.d("KKKKKK", "-------------XXXXX-08 : "+result);
            Log.d("KKKKKK", "-------------XXXXX-08A : "+GlobalData.globalResult);
            if (GlobalData.globalResult == COMM_FALIURE) {
                Log.d("KKKKKK", "-------------XXXXX-09");
                showToast("Communication Failure", TOAST_TYPE_FAILED);
            }

            if (GlobalData.globalResult == TERMINATE_TRANSACTION) {
                Log.d("KKKKKK", "-------------XXXXX-10");
            }
            else if (result == EMV_SUCCESS && GlobalData.globalResult == 0) {
                Log.d("KKKKKK", "-------------XXXXX-11");
                updateStatusText("Analysing EMV Result");

                //here the TSI and TVR is available for analysing
                getEMVTransactionResult();

                //currentTransaction.emvResult contains the tag 8a authorization response code
                saveScriptResult();

                //push the result after the ecr transaction is finished
                if (SettingsInterpreter.isECREnabled() &&
                        MainActivity.ecr.isECRInitiated)
                    MainActivity.ecr.pushTransactionDetails(SUCCESS, currentTransaction, "00");

                if (!transactionDatabase.writeTransaction(currentTransaction)) {
                    //must send a reversal here
                    return TERMINATE_TRANSACTION;
                }

                configDatabase.saveInvoiceNumber(currentTransaction);
                Log.d("FFFFFFFFFFFFFFF", " ----->Transaction Approved");
                showToast("Transaction Approved", TOAST_TYPE_SUCCESS);

                //play approved sound clip
                //playSound(R.raw.transaction_approved);
                //print the receipt here
                startBusyAnimation("Printing...");
                Receipt rcpt = Receipt.getInstance();

                rcpt.printReceipt(0);
                stopBusyAnimation();
            }
            else if (result == ERR_EMVDENIAL && ctlsResult == CTLS_SWITCH_INTERFACE) { //transaction offline declined
                Log.d("KKKKKK", "-------------XXXXX-12");
                showToast("Transaction Declined, Try Inserting Card", TOAST_TYPE_WARNING);
            }
            else if (result == ERR_EMVDENIAL) {
                Log.d("KKKKKK", "-------------XXXXX-13");
                if(GlobalData.globalResult != COMM_FALIURE) {
                    if(!GlobalData.isForceRev) {
                        showToast("Declined - ERR_EMVDENIAL", TOAST_TYPE_WARNING);
                    }
                    else {
                        GlobalData.isForceRev = false;
                    }
                }
            }
            else if (result == ERR_EMVDATA) {
                Log.d("KKKKKK", "-------------XXXXX-14");
                showToast("Declined - ERR_EMVDATA", TOAST_TYPE_WARNING);
            }
            else if (result == ERR_ICCCMD) {
                Log.d("KKKKKK", "-------------XXXXX-14");
                showToast("Declined by Chip", TOAST_TYPE_WARNING);
                if(currentTransaction.inInvoiceNumber != 0) {
                    if (!transactionDatabase.writeReversal(currentTransaction)) {
                        showToast("Reversal Committing failed, Aborting Transaction", TOAST_TYPE_FAILED);
                    }
                }
            }
            else {
                showToast("Invalid Attempt, Please try again", TOAST_TYPE_WARNING);
                Log.d("KKKKKK", "-------------XXXXX-15 " + result);
                if (SettingsInterpreter.isECREnabled() &&
                        MainActivity.ecr.isECRInitiated)
                    MainActivity.ecr.pushTransactionDetails(GENERIC_ERROR_TRAN_MIDDLE, null, null);
            }

            GlobalData.globalResult = 0;
            forceRemoveCard();

        } catch (Exception ex) {
            ex.printStackTrace();
            forceRemoveCard();
        }

        return SUCCESS;
    }

    public Map<Integer, String> extractAndBuildMapFromTags(String coreTLV, int[] tagList) {
        String TAG = "EMV55";
        byte[] tlv;

        Map<Integer, String> tagMap = new HashMap<>();

        byte[] outData = new byte[15];
        int[] len = new int[2];
        try
        {
            //load emv TLV data here
            for (int tag : tagList)
            {
                Log.d("BBBBBBBBB", "04");
                String data = extractDataForTag(coreTLV,tag);
                String sTag = Integer.toHexString(tag);

                if (data == null)
                {
                    //we need to quary from the card for the tlv
                   int result =  Services.emvCore.getTLV(tag,outData,len);
                   if (result == 0)
                   {
                       //extract the data portion from the out data
                       outData  = Arrays.copyOfRange(outData,0,len[0]);
                       data = Utility.byte2HexStr(outData);
                   }
                }

                if (null != data && data.length() > 0)
                    tagMap.put(tag, data);
                else
                    tagMap.put(tag, "XX");

            }
        } catch (Exception ex) {
        }

        return tagMap;
    }

    void forceRemoveCard() {
        int counter = 0;
        int sleepTime = 500;
        startBusyAnimation("Please Remove the Card");

        try {
            while ((bankCard != null) && ((bankCard.iccDetect()) == 1)) {//contact card detected
                if (/*counter != 0 && */counter % 10 == 0) {
                    Services.core.buzzerEx(100);
                    Services.core.buzzerEx(100);

                    if (sleepTime >= 200)
                        sleepTime -= 50;
                }
                sleepMe(sleepTime);
                counter++;

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        stopBusyAnimation();
    }

    public enum MODE {
        SEND,
        RECIEVE;
    }

    public void writeChipTranTagsOnFile( String coreTlvString)
    {
        int TAG_NAME = 0;
        int TAG_DESC = 1;

        //construct the tag array
        String tagName;

        int inTagList[] = new int[NUM_TAGS];

        //convert the tag list to set of integers
        for (int iTag = 0 ; iTag < NUM_TAGS; iTag++)
        {
            tagName  = emvTags[iTag][TAG_NAME].trim();
            int tag = Integer.valueOf(tagName,16);
            inTagList[iTag]  = tag;
        }


        //by now we have the tlv string start writing on the file
        File currentDir = appContext.getFilesDir();
        String path = currentDir.getPath();
        path += "/Secured";

        File emvFileDir =  new File(path);

        if (!emvFileDir.exists())
            emvFileDir.mkdirs();       //create the directory structure

        File emvFile = new File(path,EMV_TAG_FILE);



        try
        {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(emvFile));

            String tagDesc = "";
            String data = "";
            String writeLine = "";

            Map<Integer,String> tlvData = extractAndBuildMapFromTags(coreTlvString,inTagList);

            int inTag = 0;
            String sTag = "";

            for(Map.Entry<Integer,String> tlvx : tlvData.entrySet())
            {
                inTag = tlvx.getKey();
                tagName = Integer.toHexString(inTag);
                if ((tagName.length() & 0x01) == 0x01)
                    tagName = "0" + tagName;

                data = tlvx.getValue();

                //search through the array
                for (int i = 0 ; i < NUM_TAGS; i++)
                {
                    sTag = emvTags[i][TAG_NAME].trim();
                    int IntTag = Integer.valueOf(sTag,16);
                    int IntTagName = Integer.valueOf(tagName,16);

                    if ( IntTag == IntTagName )
                    {
                        tagDesc = emvTags[i][TAG_DESC].trim();
                        break;
                    }
                }

                if (data.equals("XX"))
                    data = "NULL";

                writeLine = tagName + "|" + tagDesc + "|" + data + "\n";
                bufferedWriter.write(writeLine);
            }

            bufferedWriter.flush();
            bufferedWriter.close();

        }catch (Exception ex)
        {
            ex.printStackTrace();
        }


    }
}
