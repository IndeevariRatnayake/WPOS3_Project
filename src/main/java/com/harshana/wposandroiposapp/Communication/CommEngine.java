package com.harshana.wposandroiposapp.Communication;


import android.util.Log;

import com.harshana.wposandroiposapp.Base.Services;
import com.harshana.wposandroiposapp.Settings.SettingsInterpreter;
import com.harshana.wposandroiposapp.Utilities.Utility;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import wangpos.sdk4.libbasebinder.Printer;


/**
 * Created by harshana_m on 10/29/2018.
 */

public class CommEngine extends Thread
{
    private Socket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    private String ip;
    private int port;
    private byte[] sendData;
    private byte[] recvData;

    private int timeOut = 0;
    private static boolean printComm = false;
    private static CommEngine myInstance = null;

    private static int connectTimeOut = 0 ;

    private boolean extendedMode = false;

    public static CommEngine getInstance(String ip,int port)
    {
        if (myInstance == null)
            myInstance =  new CommEngine(ip,port);

        myInstance.setIPnPort(ip,port);
        printIso = SettingsInterpreter.isIsoPrintEnabled();

        //set the connect time out
        connectTimeOut = SettingsInterpreter.getConnectTimeout();
        return myInstance;
    }

    private void setIPnPort(String ip,int port)
    {
        this.ip = ip;
        this.port = port;
    }
    public CommEngine(String connectIP,int connectPort)
    {
        ip = connectIP;
        port = connectPort;
    }

    public void setSendData(byte[] data)
    {
        sendData = data;
    }
    public void setRectimeout(int rectimeout)
    {
        timeOut = rectimeout;
    }

    public boolean connect() {
        try {
            socket = new Socket();

            Log.d("PPPPP IP", " : " + ip);
            Log.d("PPPPP port", " : " + port);

            //=========================
            //  ip = "192.168.131.185";
            //=========================

            socket.connect(new InetSocketAddress(ip, port), connectTimeOut * 1000);
            return socket != null;

        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }

    }

    public enum commTypePrint
    {
        SEND,
        RECIEVE
    }


    class isoPrintThread extends  Thread
    {
        byte[] data;
        commTypePrint type;
        boolean printDone = false;


        public isoPrintThread(byte[] d,commTypePrint t)
        {
            data = d;
            type = t;
        }

        @Override
        public void run()
        {
            print(data,type);

            while (!printDone)
            {
            }

            printDone = false;
        }

        public void print(byte[] data , commTypePrint type)
        {

            String dataStr = Utility.byte2HexStr(data);
            dataStr  = dataStr.substring(4);

            int start = 0 ;
            int remLineLen = 27;
            int len = 0;
            String prLine = "";

            String formatted = "";

            //put space between each byte
            while (true)
            {
                String sb = dataStr.substring(start,start + 2);
                formatted  = formatted + sb + " ";
                start += 2;

                if (start >= dataStr.length())
                    break;
            }


            try
            {

                Services.printer.printInit();

                if (type == commTypePrint.SEND)
                    Services.printer.printString( "| SEND PACKET",20, Printer.Align.CENTER,true,false);
                else
                    Services.printer.printString( "| RECEIVE PACKET",20, Printer.Align.CENTER,true,false);


            }catch (Exception ex)
            {
                ex.printStackTrace();
            }


            len = formatted.length();
            start = 0 ;

            while (true)
            {
                if ((start + remLineLen) < len)
                    prLine = formatted.substring(start, start + remLineLen);
                else
                {
                    prLine = formatted.substring(start, start + (len - start));
                    break;
                }
                start += remLineLen;

                prLine = "| " + prLine + " |";

                int prResult = 0 ;
                try
                {
                    prResult = Services.printer.printStringExt( prLine,
                            0,
                            0,
                            1.0f,
                            Printer.Font.MONOSPACE,
                            20,
                            Printer.Align.CENTER,
                            false,
                            false,
                            false);

                }
                catch (Exception ex ){
                    ex.printStackTrace();
                }
            }
            try
            {
                Services.printer.printFinish();
            }catch (Exception Ex)
            {
                Ex.printStackTrace();
            }

        }
    }

    static boolean printIso = false;

    public void startPrint(byte[] data,commTypePrint type)
    {
        isoPrintThread iso = new isoPrintThread(data,type);
        iso.start();
        try{ iso.join();}catch (InterruptedException ex){}
    }

    public int sendDataBuffer() {
        int dlen = 0 ;
        try {
            outputStream =  socket.getOutputStream();
            outputStream.write(sendData);
            outputStream.flush();

            dlen = sendData.length;
        } catch (IOException ex) {
            connect();
            try {
                outputStream =  socket.getOutputStream();
                outputStream.write(sendData);
                outputStream.flush();
            } catch (Exception exx) {
                exx.printStackTrace();
            }
        }
        return dlen;
    }

    public Socket getSocket()
    {
        return socket;
    }

    public byte[] recieveDataBuffer() {
        return recieveDataBuffer(2024,timeOut);
    }

    byte[] recData;
    public byte[] recieveDataBuffer(int expectedLength,int timeRecTimeout) {
        Log.d("******* recieved return", " 01A: START" );
        try {
            socket.setSoTimeout(timeRecTimeout * 10000);
            inputStream = socket.getInputStream();
            recData = new byte[expectedLength];
            int readBytesLen = inputStream.read(recData);
            Log.d("******* recieved return", " 01A: readBytesLen : "+readBytesLen );
            if (readBytesLen > 0) {
                byte[] bufNew = new byte[readBytesLen];
                System.arraycopy(recData, 0, bufNew, 0, readBytesLen);
                //if (printIso)
                // startPrint(bufNew,commTypePrint.RECIEVE);
                Log.d("******* recieved return", " 01A: " + bufNew);
                return bufNew;
            }
            else if (readBytesLen == 0) {
                Log.d("******* recieved return", " 01: null");
                return null;
            }
        } catch (Exception ex) {
            Log.d("******* recieved return", " 02: null");
            ex.printStackTrace();
        }
        Log.d("******* recieved return", " 03: null");
        return null;
    }

    public void disconnect() {
        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }

            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }

            if (socket != null)
                socket.close();
            socket = null;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public enum CommState {
        IDLE,
        CONNECTED,
        CONNECT_FAILED,
        SENDING_FAILED,
        RECIVING_FAILED,
        RECIEVED
    }

    private CommState commState = CommState.IDLE;

    public CommState getCommState()
    {
        return commState;
    }

    public interface OnTransAndReceivedComplete {
        void TransAndRecieveCompled();
    }

    private OnTransAndReceivedComplete trns = null;
    public void setOnTranAndRecievedComplete(OnTransAndReceivedComplete l)
    {
        trns = l;
    }
    public void ExtendedModeRun() {
        if (connect()) {
            callCallback("Connected!");
            commState = CommState.CONNECTED;
        }
        else {
            callCallback("Connect Failed..");
            commState = CommState.CONNECT_FAILED;
            return;
        }

        while (sendData != null) {
            callCallback("Sending...");

            if (sendDataBuffer() > 0)
                ;  //callCallback("Sending Success..");
            else {
                callCallback("Sending Failed!");
                disconnect();
                return;
            }

            if (timeOut < 5)
                timeOut = 10;

            callCallback("Recieving...");

            if ((recvData = recieveDataBuffer(2024,timeOut)) != null) {
                callCallback("Recived..");
                commState = CommState.RECIEVED;

                if (trns != null)
                    trns.TransAndRecieveCompled();
            } else {
                callCallback("Recieve Failed!");
                commState = CommState.RECIVING_FAILED;
                disconnect();
                return;
            }
        }
        disconnect();
    }

    public void NormalModeRun() {
        callCallback("Connecting...");

        if (connect()) {
            callCallback("Connected!");
            commState = CommState.CONNECTED;
        } else {
            callCallback("Connect Failed..");
            commState = CommState.CONNECT_FAILED;
            return;
        }

        callCallback("Sending...");

        if (sendDataBuffer() > 0)
            ;  //callCallback("Sending Success..");
        else {
            callCallback("Sending Failed!");
            disconnect();
            return;
        }

        if (timeOut < 5)
            timeOut = 10;

        callCallback("Recieving...");

        if ((recvData = recieveDataBuffer(2024,timeOut)) != null) {
            callCallback("Recived..");
            commState = CommState.RECIEVED;
        } else {
            callCallback("Recieve Failed!");
            commState = CommState.RECIVING_FAILED;
            disconnect();
            return;
        }
    }

    @Override
    public void run() {
        if (extendedMode)
            ExtendedModeRun();
        else
            NormalModeRun();
    }

    void callCallback(String str) {
        if (listener != null)
            listener.OnCommunicationStatus(str);
    }

    private OnCommStatusCheckListener listener = null;

    public void setOnCommstatusCheckListener(OnCommStatusCheckListener newListener) {
        listener = newListener;
    }

    public interface  OnCommStatusCheckListener {
        void OnCommunicationStatus(String status);
    }

    public void printCommIso() {
    }
}