package com.harshana.wposandroiposapp.UI;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.harshana.wposandroiposapp.Base.HostIssuer;
import com.harshana.wposandroiposapp.Base.IssuerHostMap;
import com.harshana.wposandroiposapp.Base.Services;
import com.harshana.wposandroiposapp.DevArea.GlobalData;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.Utilities.BytesUtil;
import com.harshana.wposandroiposapp.Utilities.Utility;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import wangpos.sdk4.base.ICallbackListener;
import wangpos.sdk4.libbasebinder.Core;

import static com.harshana.wposandroiposapp.Base.Base.currentTransaction;

public class KeyPadDialog {
    Core mCore;
    Button btnb1, btnb2, btnb3, btnb4, btnb5, btnb6, btnb7, btnb8, btnb9, btnb0,
            btncancel, btnconfirm, btnclean;
    View view;
    Dialog dialog;
    static TextView title = null;
    Handler mHandler = null;
    static KeyPadDialog keypad;
    private ICallbackListener callback;
    private static OnPinPadListener tmponPinPadListener;

    public static abstract class OnPinPadListener {
        protected  abstract void onSuccess();
        protected  abstract void onSuccess(String pin);
        protected abstract void onBypass();
        protected void onError(){}
        protected void onError(String errorMsg){
            onError();
        }
        protected void onError(int errorCode,String errorMsg){
            onError(errorMsg);
        }
    }

    public static KeyPadDialog getInstance() {
        if(keypad == null ) {
            keypad = new KeyPadDialog();
        }
        return keypad;
    }

    public KeyPadDialog() {
    }

    public void showDialog(final Activity context, final OnPinPadListener onPinPadListener) {
        tmponPinPadListener = onPinPadListener;
        mHandler = new EventHandler();
        mCore = Services.core;

        dialog = new Dialog(context, R.style.my_dialog);
        view = LayoutInflater.from(context).inflate(R.layout.layout_pin,null);
        btnb1 = view.findViewById(R.id.button1);
        btnb2 = view.findViewById(R.id.button2);
        btnb3 = view.findViewById(R.id.button3);
        btnb4 = view.findViewById(R.id.button4);
        btnb5 = view.findViewById(R.id.button5);
        btnb6 = view.findViewById(R.id.button6);
        btnb7 = view.findViewById(R.id.button7);
        btnb8 = view.findViewById(R.id.button8);
        btnb9 = view.findViewById(R.id.button9);
        btnb0 = view.findViewById(R.id.button0);
        btncancel = view.findViewById(R.id.buttoncan);
        btnconfirm = view.findViewById(R.id.buttonconfirm);
        btnclean = view.findViewById(R.id.buttonclean);
        Window dialogWindow = dialog.getWindow();
        dialogWindow.setGravity( Gravity.BOTTOM);

        dialogWindow.setWindowAnimations(R.style.dialogstyle);
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.x = 0;
        lp.y = 0;
        view.measure(0, 0);
        lp.height = view.getMeasuredHeight();
        lp.alpha = 9f;
        dialogWindow.setAttributes(lp);
        dialog.setContentView(view);
        if (!context.isFinishing() || !context.isDestroyed())
            dialog.show();
        Log.v("button",btnb0.getY()+"---"+btnb0.getX()+"----"+btnb0.getPivotX()+"----"+btnb0.getPivotX());
        callback = new ICallbackListener.Stub(){
            @Override
            public int emvCoreCallback(int command, byte[] data, byte[] result, int[] resultlen) {
                Log.d("dialog emvCoreCallback"," command:"+command+"\tdata"+data[0]+mHandler);
                if (command != Core.CALLBACK_PIN)
                    return -1;
                if (data[0] == Core.PIN_CMD_PREPARE) {
                    Log.d("PINPad", "pin pad init data len is " + data.length);

                    Message msg = new Message();
                    msg.what = 1;
                    Bundle bd = new Bundle();
                    bd.putByteArray("data", data);
                    msg.setData(bd);
                    Log.i("KeyPadDialog", "PIN_CMD_PREPARE: "+new String(data)+"---"+data[1]);
                    if (mHandler != null)
                        mHandler.sendMessage(msg);

                    try {
                        mCore.generatePINPrepareData(result, btnb1, btnb2, btnb3, btnb4, btnb5,
                                btnb6, btnb7, btnb8, btnb9, btnb0, btncancel,
                                btnconfirm, btnclean,  context);
                        if (btnclean != null) {
                            resultlen[0] = 113;
                        }else {
                            resultlen[0] = 105;
                        }
                    } catch (Exception e) {
                        Log.d("PINPad", "mReceiver RemoteException " + e.toString());
                    }
                } else if (data[0] == Core.PIN_CMD_UPDATE) {
                    result[0] = 0;
                    resultlen[0] = 1;

                    Message msg = new Message();
                    msg.what = 2;
                    Bundle bd = new Bundle();
                    bd.putByteArray("data", data);
                    Log.i("KeyPadDialog", "PIN_CMD_UPDATE: "+new String(data));
                    msg.setData(bd);
                    if (mHandler != null)
                        mHandler.sendMessage(msg);
                } else if (data[0] == Core.PIN_CMD_QUIT) {
                    Log.i("KeyPadDialog", "emvCoreCallback: "+ Core.PIN_CMD_QUIT +"---"+mHandler);
                    result[0] = 0;
                    resultlen[0] = 1;

                    Message msg = new Message();
                    msg.what = 3;
                    Bundle bd = new Bundle();
                    bd.putByteArray("data", data);
                    Log.i("KeyPadDialog", "PIN_CMD_QUIT: "+data[1]+"--->"+ Utility.byte2HexStr(data));
                    msg.setData(bd);
                    if(data[1]==0) {
                        String pin =  BytesUtil.bytes2HexString(Arrays.copyOfRange(data,4,4+data[3]));
                        Log.i("KeyPadDialog", "pin data len: "+ Arrays.copyOfRange(data,4,4+data[3]).length+"pin data"+pin);
                        GlobalData.pin = pin;
                    }
                    if (mHandler != null)
                        mHandler.sendMessage(msg);
                }
                return 0;
            }
        };

        ((TextView)view.findViewById(R.id.textView)).setText("");

        new PINThread().start();
    }

    public int showDialog(final Activity context, final int command, final byte[] data, final byte[] result, final int[] resultlen, final OnPinPadListener onPinPadListener) {
        tmponPinPadListener = onPinPadListener;
        mHandler = new EventHandler();
        mCore = Services.core;

        if(data[0]!= 0x01 && dialog != null && dialog.isShowing()) {
            Log.e("PINPad", "isShowing");
            if (command != Core.CALLBACK_PIN) {
                //onPinPadListener.onSuccess();
                return -1;
            }
            if (data[0] == Core.PIN_CMD_PREPARE) {
                Log.e("PINPad", "pin pad init data len is " + data.length);

                Message msg = new Message();
                msg.what = 1;
                Bundle bd = new Bundle();
                bd.putByteArray("data", data);
                msg.setData(bd);
                String dt = Utility.byte2HexStr(data);

                if (mHandler != null)
                    mHandler.sendMessage(msg);

                try {
                    mCore.generatePINPrepareData(result, btnb1, btnb2, btnb3, btnb4, btnb5,
                            btnb6, btnb7, btnb8, btnb9, btnb0, btncancel,
                            btnconfirm, btnclean,  context);
                    if (btnclean != null) {
                        resultlen[0] = 113;
                    }else {
                        resultlen[0] = 105;
                    }
                } catch (Exception e) {
                    Log.e("PINPad", "mReceiver RemoteException " + e.toString());
                }
            } else if (data[0] == Core.PIN_CMD_UPDATE) {
                result[0] = 0;
                resultlen[0] = 1;

                Message msg = new Message();
                msg.what = 2;
                Bundle bd = new Bundle();
                bd.putByteArray("data", data);
                msg.setData(bd);
                if (mHandler != null)
                    mHandler.sendMessage(msg);
                Log.d("Invi_PIN", "Update");
                onPinPadListener.onSuccess();
            } else if (data[0] == Core.PIN_CMD_QUIT) {
                result[0] = 0;
                resultlen[0] = 1;

                Message msg = new Message();
                msg.what = 3;
                Bundle bd = new Bundle();
                bd.putByteArray("data", data);
                msg.setData(bd);

                if (mHandler != null)
                    mHandler.sendMessage(msg);
            }
        }
        else {
            view = LayoutInflater.from(context).inflate(R.layout.layout_pin, null);
            dialog = new Dialog(context, R.style.my_dialog);
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    if (command != Core.CALLBACK_PIN) {
                        //onPinPadListener.onSuccess();
                        return;
                    }
                    if (data[0] == Core.PIN_CMD_PREPARE) {
                        Log.e("PINPad", "pin pad init data len is " + data.length);

                        Message msg = new Message();
                        msg.what = 1;
                        Bundle bd = new Bundle();
                        bd.putByteArray("data", data);
                        msg.setData(bd);

                        if (data[1] == 0x01)
                            title.setText("Online PIN");
                        else if (data[1] == 0x02) {
                            String titleText = null;
                            titleText = "Offline PIN";

                            if (data[2] == 0x01) // try flag
                            {
                                int remainingAttempts = data[3];
                                if (remainingAttempts > 1)
                                    titleText += " " + remainingAttempts;
                                else
                                    titleText = "Last Attempt";
                            }

                            title.setText(titleText);
                        }


                        String dsr = Utility.byte2HexStr(data);
                        if (mHandler != null)
                            mHandler.sendMessage(msg);

                        try {
                            mCore.generatePINPrepareData(result, btnb1, btnb2, btnb3, btnb4, btnb5,
                                    btnb6, btnb7, btnb8, btnb9, btnb0, btncancel,
                                    btnconfirm, btnclean, context);
                            if (btnclean != null) {
                                resultlen[0] = 113;
                            }else {
                                resultlen[0] = 105;
                            }
                            Log.d("Invi_PIN", "Prepare");
                            onPinPadListener.onSuccess();
                        } catch (Exception e) {
                            Log.e("PINPad", "mReceiver RemoteException " + e.toString());
                        }
                    }
                    else if (data[0] == Core.PIN_CMD_UPDATE) {
                        result[0] = 0;
                        resultlen[0] = 1;

                        Message msg = new Message();
                        msg.what = 2;
                        Bundle bd = new Bundle();
                        bd.putByteArray("data", data);
                        msg.setData(bd);
                        //onPinPadListener.onSuccess();
                        if (mHandler != null)
                            mHandler.sendMessage(msg);

                    }
                    else if (data[0] == Core.PIN_CMD_QUIT) {
                        result[0] = 0;
                        resultlen[0] = 1;

                        Message msg = new Message();
                        msg.what = 3;
                        Bundle bd = new Bundle();
                        bd.putByteArray("data", data);
                        msg.setData(bd);
                        //onPinPadListener.onSuccess();
                        if (mHandler != null)
                            mHandler.sendMessage(msg);
                    }
                }
            });

            btnb1 = view.findViewById(R.id.button1);
            btnb2 = view.findViewById(R.id.button2);
            btnb3 = view.findViewById(R.id.button3);
            btnb4 = view.findViewById(R.id.button4);
            btnb5 = view.findViewById(R.id.button5);
            btnb6 = view.findViewById(R.id.button6);
            btnb7 = view.findViewById(R.id.button7);
            btnb8 = view.findViewById(R.id.button8);
            btnb9 = view.findViewById(R.id.button9);
            btnb0 = view.findViewById(R.id.button0);
            btncancel = view.findViewById(R.id.buttoncan);
            btnconfirm = view.findViewById(R.id.buttonconfirm);
            btnclean = view.findViewById(R.id.buttonclean);

            title = view.findViewById(R.id.msg_title);

            Window dialogWindow = dialog.getWindow();
            dialogWindow.setGravity(Gravity.BOTTOM);
            Log.v("button", btnb0.getY() + "---" + btnb0.getX() + "----" + btnb0.getPivotX() + "----" + btnb0.getPivotX());
            dialogWindow.setWindowAnimations(R.style.dialogstyle);
            WindowManager.LayoutParams lp = dialogWindow.getAttributes();
            lp.x = 0;
            lp.y = 0;
            view.measure(0, 0);
            lp.height = view.getMeasuredHeight();
            lp.alpha = 9f;
            dialogWindow.setAttributes(lp);
            dialog.setContentView(view);
            Log.i("", "showDialog: context.isDestroyed()"+context.isDestroyed()+"context.isFinishing()"+context.isFinishing());
            if (!context.isFinishing() || !context.isDestroyed()) {
                try {
                    dialog.show();
                } catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }

            Log.v("button show", "-----");

            ((TextView) view.findViewById(R.id.textView)).setText("");

            /*btncancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });*/

            Log.d("Invi_PIN", "onshowdialog finished");
        }
        return 0;
    }

    public  class PINThread extends Thread {
        @Override
        public void run () {
            Log.d("Invi_PIN", "PINThread");
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            byte[] formatdata = new byte[8];
            String pan = currentTransaction.PAN;            //WeiPassGlobal.getTransactionInfo().getCardNo();

            int ret = -1;

            //search through the name of the host for the selected issuer
            String packageName  = "";
            for (HostIssuer issuer : IssuerHostMap.hosts) {
                for (int issuerNum : issuer.issuerList) {
                    if (issuerNum == currentTransaction.issuerNumber) {      //found the selected issuer
                        packageName =  issuer.hostName;
                        break;
                    }
                }
            }
            try {
                ret = mCore.startPinInput(60, packageName, 1, 4, 12, 0x00, formatdata, pan.length(), pan.getBytes(StandardCharsets.UTF_8), callback);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (ret != RspCode.OK) {
                // do some error notify
                //finish();
            }
        }
    }

    class RspCode {
        static final String TAG = "RspCode";
        public static final int OK = 0;
        public static final int ERROR = -1;

        public RspCode() {
        }
    }

    class EventHandler extends Handler {
        private String strpin;
        public EventHandler() {
        }

        public EventHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage (Message msg) {
            super.handleMessage(msg);
            Bundle bd = null;
            byte[] data = null;
            Log.i("EventHandler", "handleMessage: "+msg.what);
            switch (msg.what) {
                case 1:
                    // PIN input process start, secure chip generated random key sequence need display.
                    bd = msg.getData();
                    data = bd.getByteArray("data");
                    String displaynumber = null;
                    displaynumber = "" + (data[4] - 0x30);
                    btnb1.setText(displaynumber);

                    displaynumber = "" + (data[5] - 0x30);
                    btnb2.setText(displaynumber);

                    displaynumber = "" + (data[6] - 0x30);
                    btnb3.setText(displaynumber);

                    displaynumber = "" + (data[7] - 0x30);
                    btnb4.setText(displaynumber);

                    displaynumber = "" + (data[8] - 0x30);
                    btnb5.setText(displaynumber);

                    displaynumber = "" + (data[9] - 0x30);
                    btnb6.setText(displaynumber);

                    displaynumber = "" + (data[10] - 0x30);
                    btnb7.setText(displaynumber);

                    displaynumber = "" + (data[11] - 0x30);
                    btnb8.setText(displaynumber);

                    displaynumber = "" + (data[12] - 0x30);
                    btnb9.setText(displaynumber);

                    displaynumber = "" + (data[13] - 0x30);
                    btnb0.setText(displaynumber);
                    view.setVisibility(View.VISIBLE);
                    break;
                case 2:
                    // User input, need show corresponding amount of stars *
                    bd = msg.getData();
                    data = bd.getByteArray("data");
                    int count = data[1];
                    String stars = "";
                    for (int i = 0; i < count; i++) {
                        stars += "*";
                    }
                    ((TextView)view.findViewById(R.id.textView)).setText(stars);
                    break;
                case 3:
                    Log.d("EventHandler1", "handleMessage: "+msg.what);
                    dialog.cancel();
                    // Input PIN process is finished.
//                    RestoreKeyPad();
                    bd = msg.getData();
                    data = bd.getByteArray("data");
                    if(data != null) {
                        Log.d("EventHandler1", "data: " + data);
                    }
                    Log.d("EventHandler1", "data[1]: "+data[1]);

                    //Success
                    if (data[1] == Core.PIN_QUIT_SUCCESS) {
                        Log.d("EventHandler1", "data[2]: "+data[2]);
                        //No PIN upload
                        if (data[2] == Core.PIN_QUIT_NOUPLOAD) {
                            ((TextView)view.findViewById(R.id.textView)).setText("No PIN inputed");
                            Log.d("EventHandler1", "No pin");
                            tmponPinPadListener.onSuccess();
                        }
                        //Plain PIN
                        //only for test mode
                        else if (data[2] == Core.PIN_QUIT_PAINUPLOAD) {
                            int pinlen = data[3];
                            Log.d("PINPad", "Pain pinlen is " + pinlen);
                            byte[] PINData = new byte[pinlen];
                            PINData[0] = data[1];
                            java.lang.System.arraycopy(data, 4, PINData, 0, pinlen);
                            strpin = Utility.byte2HexStr(PINData);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    tmponPinPadListener.onSuccess(strpin);
                                }
                            }).start();

                            ((TextView)view.findViewById(R.id.textView)).setText(strpin);
                        }
                        //Encrypt PIN
                        else if (data[2] == Core.PIN_QUIT_PINBLOCKUPLOAD) {
                            int pinlen = data[3];
                            Log.d("PINPad", "Encrypt pinlen is " + pinlen);
                            byte[] PINData = new byte[pinlen];
                            PINData[0] = data[1];
                            java.lang.System.arraycopy(data, 4, PINData, 0, pinlen);

                            strpin = Utility.byte2HexStr(PINData);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    tmponPinPadListener.onSuccess(strpin);
                                }
                            }).start();
                            ((TextView)view.findViewById(R.id.textView)).setText(strpin);
                        }
                    }
                    //cancel
                    else if (data[1] == Core.PIN_QUIT_CANCEL) {
                        Log.d("Invi_PIN"," error :cancel no pin");
                        tmponPinPadListener.onError(0,"cancel no pin");
                        ((TextView)view.findViewById(R.id.textView)).setText("User calceled");
                    }
                    //bypass
                    else if (data[1] == Core.PIN_QUIT_BYPASS) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                tmponPinPadListener.onBypass();
                            }
                        }).start();

                        ((TextView)view.findViewById(R.id.textView)).setText("bypass");
                    }
                    //error
                    else if (data[1] == Core.PIN_QUIT_ERROR) {
                        Log.d("Invi_PIN"," error :pin error");
                        tmponPinPadListener.onError(Core.PIN_QUIT_ERROR,"pin  error");
                        ((TextView)view.findViewById(R.id.textView)).setText("Error");
                    }
                    //timeout
                    else if (data[1] == Core.PIN_QUIT_TIMEOUT) {
                        Log.d("Invi_PIN"," error :pin timeout");
                        tmponPinPadListener.onError(Core.PIN_QUIT_TIMEOUT,"pin Timeout");
                        ((TextView)view.findViewById(R.id.textView)).setText("Timeout");
                    }
                    //no PAN
                    else if (data[1] == Core.PIN_QUIT_ERRORPAN) {
                        Log.d("Invi_PIN"," error :no card no");
                        tmponPinPadListener.onError(Core.PIN_QUIT_ERRORPAN,"no Card NO");
                        ((TextView)view.findViewById(R.id.textView)).setText("No PAN");
                    }
                    //others
                    else {
                        Log.d("Invi_PIN"," error :other " + data[1]);
                        tmponPinPadListener.onError(-1,"Other Error");
                        ((TextView)view.findViewById(R.id.textView)).setText("Other Error");
                    }
                    break;
                default:
                    break;
            }
        }
    }
}