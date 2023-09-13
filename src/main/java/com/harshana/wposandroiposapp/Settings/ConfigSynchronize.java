package com.harshana.wposandroiposapp.Settings;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.harshana.wposandroiposapp.Cup.CUPData;
import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.Utilities.GPSTracker;
import com.harshana.wposandroiposapp.Utilities.Utility;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class ConfigSynchronize {
    final static int JOB_UPDATE_SETTINGS = 1;
    final static int JOB_PUSH_TRANSACTIONS = 2;

    private static final int RESOLUTION_TIMER = 20;  //resulution timer is set to 5  seconds
    static String TRAN_PUSH_LINK = "http://192.168.8.200:8400/api/v1/transactions?physical-terminal-id=";
    static String ACK_PUSH_LINK = "http://192.168.8.200:8200/api/v1/settings/update-terminal-ack";
    static String CUST_DET_PUSH_LINK = "http://192.168.8.200:8200/api/v1/promotion/send-card-promotion-email";

    static String macKey = "0393e944ee8108bb66fc9fa4f99f9c862481e9e0519e18232ba61b0767eee8c6";

    private final static String FILE_NAME = "configurations.txt";
    private static final String Mit_constraints[] =
            {
                    "InvNumber", "STAN", "BatchNumber", "MustSettleFlag"
            };
    
    //Sampath IT
    static String SETT_DOWNLOAD_LINK = "/device-management-service/api/v1/settings/settings";
    //AWS
    //static String SETT_DOWNLOAD_LINK = "http://18.221.115.191:8762/device-management-service/api/v1/settings/settings";

    private final static char TAG_OPEN = '{';
    private final static char TAG_CLOSE = '}';
    private static int TRANSACTION_UPLOAD_INTERVAL = 10;
    private static int POLL_INTERVAL = 5;             //seconds
    private static int CONNECTION_TIME_OUT = 10;
    private int lapsedTimeForBackEndDataPush = 0;
    String versionProcessing = "", qrmidpro = "";

    Context appContext;
    Activity mActivity;
    ProgressDialog progressDialog = null;
    List<JSONObject> bkDataSet;
    boolean status = false;

    private static ConfigSynchronize myInstance = null;
    private int lapsedTimeForConfigSync = 0;

    Handler handler;
    String configFilePath = "";
    private static final String configFileName = "LibConfig.conf";

    List<Settings> settingsList = null;

    String reason = "";

    public void init() throws Exception
    {
        if (onGetTerminalBusyFlag == null)
            throw new Exception("Terminal busy status import func is not registered");

        settingsList = new ArrayList<>();
        initSettingsList();

        //create the configuration file
        File configFilePath = appContext.getFilesDir();

        this.configFilePath = configFilePath.getPath();
        this.configFilePath += "/Lib";

        File file =  new File(this.configFilePath);
        if (!file.exists()) {
            file.mkdirs();

            //save the initial configurations
            if (!saveConfigurations())
                throw new Exception("Saving configurations failed");
        }
        else
        {
           if (! loadConfigurations())
               throw new Exception("Loading configurations failed");
        }

        handler = new Handler(Looper.getMainLooper());
        initNodes();
        startProcess();
    }

    void initSettingsList()
    {
        settingsList.add(new Settings("poll_interval",String.valueOf(POLL_INTERVAL)));
        settingsList.add(new Settings("transaction_upload_interval",String.valueOf(TRANSACTION_UPLOAD_INTERVAL)));
        settingsList.add(new Settings("connection_time_out",String.valueOf(CONNECTION_TIME_OUT)));
    }

    Map<String,String> decodedMap ;

    private void SleepMe(int seconds)
    {
        try
        {
            Thread.sleep(seconds * 1000);
        }catch (Exception ex){}
    }

    private void popMessage(final String message)
    {
        if (handler == null)
            handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable()
        {
            @Override
            public void run()
            {
                Toast.makeText(appContext,message,Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean loadConfigurations() {
        File file = new File(configFilePath,configFileName);
        if (!file.exists())
            return false;

        //read the current configurations
        try {
            BufferedReader fileReader = new BufferedReader(new FileReader(file));
            String readSettings = "";

            settingsList.clear();
            while ( (readSettings = fileReader.readLine()) != null)
            {
                int indexOfDelim = readSettings.indexOf(':');
                String key = readSettings.substring(0,indexOfDelim);
                String value = readSettings.substring(indexOfDelim + 1 );

                settingsList.add(new Settings(key,value));
            }
        }catch (Exception ex)
        {
            ex.printStackTrace();
            return false;
        }


        //load the settings in to the program variables
        for (Settings set : settingsList)
        {
            if (set.name.compareTo("poll_interval") == 0)
                POLL_INTERVAL = Integer.valueOf(set.value);
            else if (set.name.compareTo("transaction_upload_interval") == 0)
                TRANSACTION_UPLOAD_INTERVAL = Integer.valueOf(set.value);
            else if (set.name.compareTo("connection_time_out") == 0)
                CONNECTION_TIME_OUT = Integer.valueOf(set.value);
            else if (set.name.compareTo("settings_download_link") == 0)
                SETT_DOWNLOAD_LINK = (set.value);
            else if (set.name.compareTo("tran_push_link") == 0)
                TRAN_PUSH_LINK = (set.value);
            else if (set.name.compareTo("ack_push_link") == 0)
                ACK_PUSH_LINK = (set.value);
            else if (set.name.compareTo("cust_det_push_link") == 0)
                CUST_DET_PUSH_LINK = (set.value);
        }

        return true;
    }

    private boolean isStopped = false;
    private Map<String, String> dataMap;
    private onParsingConfigs onStatusUpdate = null;
    private onGetBackEndData onGetBackEndData = null;
    private onGetTerminalBusyFlag onGetTerminalBusyFlag = null;
    private List<NodeInfo> nodes;

    private ConfigSynchronize(Context context, Activity activity) {
        dataMap = new HashMap<>();
        appContext = context;
        mActivity = activity;
    }

    public static ConfigSynchronize getInstance(Context c, Activity activity) {
        if (myInstance == null)
            myInstance = new ConfigSynchronize(c, activity);

        return myInstance;
    }

    private boolean saveConfigurations() {
        //open the file and commit the initial configurations
        File file = new File(configFilePath, configFileName);

        String settingString = "";
        try {
            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(file));
            for (Settings settings : settingsList) {
                settingString = settings.name + ":" + settings.value + "\n";
                fileWriter.write(settingString, 0, settingString.length());
            }

            fileWriter.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
        return true;

    }

    public Context setContext(Context newContext) {
        Context tempContext = appContext;
        appContext = newContext;
        return tempContext;
    }

    public int pushTransactionsManual() {
        TYPE_DATA_BE type = TYPE_DATA_BE.TYPE_TRANSACTION;
        bkDataSet = onGetBackEndData.getData(type);

        int numTrans = 0;
        if (bkDataSet != null) {

            final CountDownLatch countDownLatch = new CountDownLatch(1);

            JSONArray jsonArray = new JSONArray();
            for (JSONObject json : bkDataSet) {
                jsonArray.put(json);
                numTrans++;
            }


            final JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("", jsonArray);
            } catch (Exception ex) {
                ex.printStackTrace();
            }


            Thread pushThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    status = pushDataToBackend(jsonObject);
                    countDownLatch.countDown();

                }
            });

            pushThread.start();

            try {
                countDownLatch.await();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return numTrans;
    }

    private void callOnStatusUpdate(final String status, final int percentage)
    {
        if (onStatusUpdate == null)
        {
            handler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    if (progressDialog != null)
                        progressDialog.setMessage(status + " " + percentage + "%");
                }
            });
        }
    }

    //this routine push the acknowledgement to the back end for any task whih has been performed
    //by the terminal based on the  back end request
    void pushAcknowledgeState(int task,boolean isSuccess,String reason)
    {
        try
        {
            URL uploadLink =  new URL(ACK_PUSH_LINK);

            HttpURLConnection uploadConnection = (HttpURLConnection)uploadLink.openConnection();
            //uploadConnection.setRequestMethod("POST");
            uploadConnection.setRequestMethod("PUT");
            uploadConnection.setRequestProperty("Content-Type","application/json; utf-8");
            uploadConnection.setRequestProperty("Accept","application/json");
            /*uploadConnection.setRequestProperty("terminalId",getFirstTID());
            uploadConnection.setRequestProperty("versionNumber",getSettingsVersion());*/
            if (isSuccess)
                uploadConnection.setRequestProperty("success","1");
            else
                uploadConnection.setRequestProperty("success","0");

            uploadConnection.setDoOutput(true);

            JSONObject jsonObject = new JSONObject();
            //jsonObject.put(String.valueOf(task),isSuccess);

            //jsonObject.put("terminalId",getFirstTID());
            jsonObject.put("terminalId","tid0001");
            jsonObject.put("versionNumber",getSettingsVersion());

            String jsonString = jsonObject.toString();

            OutputStream outputStream =  uploadConnection.getOutputStream();
            byte[] outBytes = jsonObject.toString().getBytes();
            outputStream.write(outBytes);

            if (outputStream != null)
            {
                outputStream.flush();
                outputStream.close();
            }

            //read the response of the update
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(uploadConnection.getInputStream(),"utf-8"));
            StringBuilder response = new StringBuilder();

            String res;

            while ( (res = bufferedReader.readLine()) != null)
                response.append(res);

        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }


    public static boolean puchCustDataToBackend(final JSONObject jsonObject)
    {
        Thread pushData = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    String link = CUST_DET_PUSH_LINK;
                    URL uploadLink =  new URL(link);
                    String jsonString = jsonObject.toString();

                    //jsonString = jsonString.substring(4,jsonString.length() - 1);

                    //generate the mac hash
                    //tring mac = generateMacHash(jsonString);
                    //mac  = mac.toUpperCase();

                    HttpURLConnection uploadConnection = (HttpURLConnection)uploadLink.openConnection();
                    uploadConnection.setRequestMethod("POST");
                    uploadConnection.setRequestProperty("Content-Type","application/json; utf-8");
                    uploadConnection.setRequestProperty("Accept","application/json");
                    //uploadConnection.setRequestProperty("mac-hash",mac);


                    uploadConnection.setDoOutput(true);
                    OutputStream outputStream = uploadConnection.getOutputStream();

                    byte[] output = jsonString.getBytes("utf-8");
                    outputStream.write(output,0,output.length);

                    //read the response
                    BufferedReader br = new BufferedReader( new InputStreamReader(uploadConnection.getInputStream(), "utf-8"));
                    StringBuilder response = new StringBuilder();
                    String resp = null;

                    while ((resp = br.readLine()) != null)
                        response.append(resp.trim());

                    String r = response.toString();

                    outputStream.close();



                }catch (Exception ex)
                {
                    ex.printStackTrace();
                    //pushAcknowledgeState(JOB_PUSH_TRANSACTIONS,false,ex.getMessage());

                }
            }
        });

        pushData.start();

        return true;

    }

    boolean pushDataToBackend(JSONObject jsonObject)
    {
        try
        {
            String link = TRAN_PUSH_LINK + getFirstTID();
            URL uploadLink =  new URL(link);
            String jsonString = jsonObject.toString();

            jsonString = jsonString.substring(4,jsonString.length() - 1);

            //generate the mac hash
            String mac = generateMacHash(jsonString);
            mac  = mac.toUpperCase();

            HttpURLConnection uploadConnection = (HttpURLConnection)uploadLink.openConnection();
            uploadConnection.setRequestMethod("POST");
            uploadConnection.setRequestProperty("Content-Type","application/json; utf-8");
            uploadConnection.setRequestProperty("Accept","application/json");
            uploadConnection.setRequestProperty("mac-hash",mac);


            uploadConnection.setDoOutput(true);
            OutputStream outputStream = uploadConnection.getOutputStream();

            byte[] output = jsonString.getBytes("utf-8");
            outputStream.write(output,0,output.length);

            //read the response
            BufferedReader br = new BufferedReader( new InputStreamReader(uploadConnection.getInputStream(), "utf-8"));
            StringBuilder response = new StringBuilder();
            String resp = null;

            while ((resp = br.readLine()) != null)
                response.append(resp.trim());

            String r = response.toString();

            outputStream.close();
            return true;


        }catch (Exception ex)
        {
            ex.printStackTrace();
            pushAcknowledgeState(JOB_PUSH_TRANSACTIONS,false,ex.getMessage());
            return false;
        }
    }


    private String getFirstTID()
    {
        DBHelper dbHelper = DBHelper.getInstance(appContext);
        String quary = "SELECT TerminalID FROM TMIF LIMIT 1";

        String firstTID = "";

        try
        {
            Cursor tidRec = dbHelper.readWithCustomQuary(quary);
            if (tidRec  == null || tidRec.getCount() == 0)
                return null;

            tidRec.moveToFirst();
            firstTID = tidRec.getString(tidRec.getColumnIndex("TerminalID"));


            //reformat the link
            tidRec.close();
        }catch (Exception ex)
        {
            ex.printStackTrace();
            return null;
        }

        return firstTID;
    }


    DBHelperSync confgDbSync = DBHelperSync.getInstance(appContext);
    DBHelper configDB = DBHelper.getInstance(appContext);

    private String getSettingsVersion() {
        String quary = "SELECT SettVersion from PST";
        String version = "";

        try {
            Cursor rec =  configDB.readWithCustomQuary(quary);
            if (rec == null || rec.getCount() == 0)
                return null;

            if (rec.getCount() == 1)
                rec.moveToFirst();
            else
                rec.moveToLast();

            version = rec.getString(rec.getColumnIndex("SettVersion"));
            rec.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return version;
    }

    private void writeSettingsVersion(String version) {
        String updateQuary = "UPDATE PST SET AppVersion= 'V1', SettVersion = '" + version + "' WHERE ID = 1";

        boolean result = false;
        try {
            result = confgDbSync.executeCustomQuary(updateQuary);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void writeQRMID(String mid) {
        String updateQuary = "UPDATE PST SET qr_mid= '" + mid + "' WHERE ID = 1";

        boolean result = false;
        try {
            result = confgDbSync.executeCustomQuary(updateQuary);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void downloadSettingsManual() {

        try {
            if (handler == null)
                init();
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        final CountDownLatch countDownLatch = new CountDownLatch(1);

        Thread configThread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean status = false;
                popMessage("Start Polling..");
                status = startPoll();

                SleepMe(2);

                if (status) {
                    Log.e("------------------->", "URL01");
                    popMessage("Start Decoding...");
                    decodedMap = parseConfigContentInvi();
                } else
                    pushAcknowledgeState(JOB_UPDATE_SETTINGS, false, reason);


                if (status && decodedMap != null) {
                    popMessage("Start Extracting...");
                    status = extractAndCommitData(decodedMap);

                    //copy back to the original database
                    DBHelperSync dbInstance = DBHelperSync.getInstance(appContext);
                    DBHelper dbOrigInstance = DBHelper.getInstance(appContext);

                    if (status) {
                        String dbPath = dbInstance.getDbPath();
                        String dbOrigPath = dbOrigInstance.getDbPath();

                        status = Utility.copyFile(dbPath, dbOrigPath);
                    }


                    if (status)
                        pushAcknowledgeState(JOB_UPDATE_SETTINGS, status, "ok");
                    else
                        pushAcknowledgeState(JOB_UPDATE_SETTINGS, status, "Database Sync Failed");

                    if (status)
                        popMessage("Extraction and Syncing Successful");
                    else
                        popMessage("Extraction and Syncing Failed");

                    countDownLatch.countDown();

                }


            }
        });

        configThread.start();

        try {
            countDownLatch.await();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private Location getCurrentKnownLocation(Context context) {
        GPSTracker gpsTracker = GPSTracker.getInstance(context);
        Location location = gpsTracker.getLocation();

        return location;
    }

    public void stopProcess() {
        isStopped = true;
    }

    public void onStatusUpdate(onParsingConfigs _func) {
        onStatusUpdate = _func;
    }

    private String getTerminalSerialNo() {
        return Build.SERIAL;
    }

    private void startProcess() {
        progressDialog = new ProgressDialog(mActivity);
        progressDialog.setTitle("Config Synchronize");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        isStopped = false;


        Thread configThread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean status = false;

                while (!isStopped) {
                    SleepMe(RESOLUTION_TIMER);

                    lapsedTimeForConfigSync += RESOLUTION_TIMER;
                    lapsedTimeForBackEndDataPush += RESOLUTION_TIMER;

                    if (onGetTerminalBusyFlag.getTerminalBusyStatus()) // if the terminal is busy back off
                        continue;

                    if (lapsedTimeForConfigSync >= POLL_INTERVAL) {
                        lapsedTimeForConfigSync = 0;

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.show();
                            }
                        });


                        popMessage("Start Polling..");
                        status = startPoll();
                        Log.e("------------------->", "URL02 : " + status);
                        if (status) {

                            popMessage("Start Decoding...");
                            decodedMap = parseConfigContentInvi();
                        }


                        //SleepMe(2);

                        if (status && decodedMap != null) {
                            //delete existing data on the tables
                            wipeDatabaseTables();
                            popMessage("Start Extracting...");
                            status = extractAndCommitData(decodedMap);

                            //copy back to the original database
                            DBHelperSync dbInstance = DBHelperSync.getInstance(appContext);
                            DBHelper dbOrigInstance = DBHelper.getInstance(appContext);

                            if (status) {
                                String dbPath = dbInstance.getDbPath();
                                String dbOrigPath = dbOrigInstance.getDbPath();

                                status = Utility.copyFile(dbPath, dbOrigPath);
                            }

                            //write the current successful settings version in the database
                            if (status)
                                pushAcknowledgeState(JOB_UPDATE_SETTINGS, status, "ok");


                            if (status)
                                popMessage("Extraction and Syncing Successful");
                            else
                                popMessage("Extraction and Syncing Failed");

                        }
                        progressDialog.dismiss();
                    }


                    if ((lapsedTimeForBackEndDataPush >= TRANSACTION_UPLOAD_INTERVAL) && (onGetBackEndData != null)) {
                        lapsedTimeForBackEndDataPush = 0;

                        TYPE_DATA_BE type = TYPE_DATA_BE.TYPE_TRANSACTION;
                        bkDataSet = onGetBackEndData.getData(type);

                        if (bkDataSet != null) {
                            JSONArray jsonArray = new JSONArray();
                            for (JSONObject json : bkDataSet)
                                jsonArray.put(json);

                            JSONObject jsonObject = new JSONObject();
                            try {
                                jsonObject.put("Transactions", jsonArray);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }

                            pushDataToBackend(jsonObject);
                        }
                    }
                }
            }
        });

        configThread.start();

    }

    boolean  startPoll() {
        try {
            String link = reformatSettDownloadRequest("http://" + configDB.loadProfileIPPort() + SETT_DOWNLOAD_LINK);
            URL downloadLink = new URL(link);

            Log.e("Push Pull Download link", "LLLLLLLLL : " + link);
            HttpURLConnection downloadConnection = (HttpURLConnection) downloadLink.openConnection();
            downloadConnection.setRequestMethod("GET");
            downloadConnection.setConnectTimeout(20 * 1000);
            downloadConnection.setReadTimeout(20 * 1000);
//            downloadConnection.setConnectTimeout(CONNECTION_TIME_OUT * 1000);
//            downloadConnection.setReadTimeout(CONNECTION_TIME_OUT * 1000);

            callOnStatusUpdate("Connecting..", 0);
            downloadConnection.connect();

            //connection is successful and start reading the file
            int respCode = downloadConnection.getResponseCode();
            Log.e("Response code", " : " + respCode);
            Log.e("HTTP_OK", " : " + HttpURLConnection.HTTP_OK);
            if (respCode != HttpURLConnection.HTTP_OK)
                return false;

            String macHash = downloadConnection.getHeaderField("machash");

            File path = appContext.getFilesDir();
            File configFile = new File(path, FILE_NAME);

            if (configFile.exists())
                configFile.delete();

            int totalDownloaded = 0;

            InputStream inputStream = downloadConnection.getInputStream();
            FileOutputStream fileOutputStream = new FileOutputStream(configFile);
            byte[] readBuffer = new byte[100];

            int readLen = 0;
            int totalLength = downloadConnection.getContentLength();
            Log.e("inputStream", " : " + inputStream);
            Log.e("totalLength", " : " + totalLength);

//            while ( (readLen = inputStream.read(readBuffer)) != 53)
            while ((readLen = inputStream.read(readBuffer)) != -1) {
                Log.e("CHECK", " : " + readLen);
                fileOutputStream.write(readBuffer, 0, readLen);

                totalDownloaded += readLen;
                float intermediate = ((float) totalDownloaded / (float) totalLength);
                intermediate = (intermediate * 100);

                if (intermediate > 90.0f)
                    callOnStatusUpdate("Download Finishing...", (int) intermediate);
                else
                    callOnStatusUpdate("Downloading...", (int) intermediate);

                try {
                    Thread.sleep(100);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            Log.e("HHHHHHHHHH", "01");
            fileOutputStream.close();
            inputStream.close();

            //verifying the mac
            //open the settings file downloaded
            BufferedReader bufferedReader = new BufferedReader(new FileReader(configFile));
            String line = "";

            StringBuilder builder = new StringBuilder();
            while ((line = bufferedReader.readLine()) != null)
                builder.append(line);

            Log.e("HHHHHHHHHH", "builder : " + builder);
            String data = builder.toString();
            Log.e("HHHHHHHHHH", "data : " + data);
            String generatedMac = generateMacHash(data);

            if (macHash != null && !generatedMac.equals(macHash))
            {
                Log.e("Response code", " FLASE02 ");
                reason = "Mac failed at terminal";
                return false;
            }


        }catch (Exception ex)
        {
            ex.printStackTrace();
            reason = ex.getMessage();
            return false;
        }

        return true;
    }

    private String generateMacHash(String data) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        sha256_HMAC.init(new SecretKeySpec(macKey.getBytes(), "HmacSHA256"));
        byte[] result = sha256_HMAC.doFinal(data.getBytes());
        return Utility.byte2HexStr(result);
    }

    private String reformatSettDownloadRequest(String rawReq) {
        String terminalSerialNo = getTerminalSerialNo();

        terminalSerialNo = Build.SERIAL;
        String settVersion = getSettingsVersion();


        if (settingsList == null || settVersion == null || settVersion.equals("NO") || settVersion.equals(""))
            settVersion = "V0";
        else {
            //request the next higher version of the settings
            int versionNumber = Integer.valueOf(settVersion.substring(1));
            versionNumber++;
            settVersion = "V" + String.valueOf(versionNumber);
        }

        rawReq = rawReq + "/" + terminalSerialNo + "/" + settVersion;

        return rawReq;
    }

    public Map<String, String> parseConfigContent() {
        Log.e("parseConfigContent", "------->01");
        String configContent = "";


        //open the file
        File filePath = appContext.getFilesDir();
        String path = filePath.getPath();

        File configFile = new File(path, FILE_NAME);

        configContent = "";
        try
        {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(configFile));
            String line = "";

            while ( (line = bufferedReader.readLine()) != null)
                configContent += line;

            bufferedReader.close();
        }catch (Exception ex)
        {
            ex.printStackTrace();
            return null;
        }


        String rec = "";

        configContent = configContent.replaceAll("\\s+","");

        //check whether the settings tag exist
        int index = 0;
        int tag_rec_count = 0 ;


        dataMap.clear();
        if (true) //index is found
        {
            int tagOpenCount = 0;
            int tagCloseCount = 0;

            callOnStatusUpdate("Parsing started", 0);

            List<Character> tagDataStack = new Stack<>();
            Map<Integer, Integer> tagDataMapOpen = new HashMap<>();
            Map<Integer, Integer> tagDataMapClose = new HashMap<>();

            char x = 0;
            String belongingTag = "";

            StringBuffer stringBuffer = new StringBuffer(configContent.substring(index));

            Log.e("configContent {{ ", " : " + configContent.toString());
//            Log.e("stringBuffer {{ ", " : " + stringBuffer.toString());


            int totalLength = stringBuffer.length();

            String tagName = "";
            for (int c = 0; c < totalLength; c++) {
                char cc = stringBuffer.charAt(c);


                if (c % 20 == 0) {
                    float percentage = (float) ((float) c / (totalLength - 1)) * 100.0f;
                    callOnStatusUpdate("Parsing Config [" + tagName + "]", (int) percentage);

                    try {
                        Thread.sleep(10);
                    } catch (Exception ex) {
                    }

                }

                if (cc == TAG_OPEN) {
                    //get the tag belonging tag name
                    if (tagDataStack.size() > 0)
                        x = ((Stack<Character>) tagDataStack).peek();

                    if (x == TAG_OPEN || x == TAG_CLOSE) {
                        int prevTagOpenPos = 0;
                        if (x == TAG_OPEN) {
                            prevTagOpenPos = tagDataMapOpen.get(tagOpenCount);
                            prevTagOpenPos++;
                        } else if (x == TAG_CLOSE) {
                            prevTagOpenPos = tagDataMapClose.get(tagCloseCount);
                            prevTagOpenPos++;
                        }

                        if ((c - prevTagOpenPos) > 3) {
                            belongingTag = stringBuffer.substring(prevTagOpenPos, c);

                            int start = 0;
                            int end = 0;
                            int count = 0;

                            for (int i = 0; i < belongingTag.length(); i++) {
                                char ccc = belongingTag.charAt(i);
                                if (ccc == '"') {
                                    if (++count == 1)
                                        start = i;
                                    else if (count == 2)
                                        end = i;
                                }

                                if (end > 00)
                                    break;
                            }

                            if (end > 0)
                                belongingTag = belongingTag.substring(start + 1,end);
                        }
                    }

                    ((Stack<Character>) tagDataStack).push(TAG_OPEN);
                    tagOpenCount++;
                    tagDataMapOpen.put(tagOpenCount,c);
                }
                else if (cc == TAG_CLOSE)
                {
                    x =  ((Stack<Character>) tagDataStack).peek();
                    if (x == TAG_OPEN)  //a record must be extracted
                    {
                        //get the positions
                        int start = tagDataMapOpen.get(tagOpenCount);
                        start++;                                      //by pass the opening tag

                        int end = c;
                        rec = stringBuffer.substring(start,end);

                        tagName =  belongingTag + "_" + String.valueOf(tag_rec_count++);
                        dataMap.put(tagName,rec);
                    }

                    ((Stack<Character>) tagDataStack).push(TAG_CLOSE);
                    tagCloseCount++;
                    tagDataMapClose.put(tagCloseCount,c);
                }
            }
        }
        return dataMap;
    }

    public Map<String, String> parseConfigContentInvi() {
        Log.e("parseConfigContent", "------->01");
        String configContent = "";

        //open the file
        File filePath = appContext.getFilesDir();
        String path = filePath.getPath();

        File configFile = new File(path, FILE_NAME);

        configContent = "";
        try
        {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(configFile));
            String line = "";

            while ( (line = bufferedReader.readLine()) != null)
                configContent += line;

            bufferedReader.close();
        }catch (Exception ex)
        {
            ex.printStackTrace();
            return null;
        }

        //check whether the settings tag exist
        dataMap.clear();

        JSONObject jsonObject = null,jsonObjectSet = null;
        try {
            jsonObject = new JSONObject(configContent);
            String settings = getValueFromRecInvi("settings", jsonObject);

            jsonObjectSet = new JSONObject(settings);
            Iterator<?> keys = jsonObjectSet.keys();
            while(keys.hasNext() ) {
                String key = (String)keys.next();
                if(key.equalsIgnoreCase("qrmid") || key.equalsIgnoreCase("version")) {
                    dataMap.put(key + "_1", "\"" + key + "\":" + getValueFromRecInvi(key, jsonObjectSet));
                }
                else {
                    JSONArray xx = jsonObjectSet.getJSONArray(key);
                    int length = xx.length();
                    for (int i = 0; i < length; i++) {
                        String rec = xx.getString(i).replace("{", "");
                        rec = rec.replace("}", "");
                        String tagName = key + "_" + (i + 1);
                        dataMap.put(tagName, rec);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return dataMap;
    }

    private boolean extractAndCommitData(Map<String,String> map) {
        if (map.size() == 0)
            return false;

        if (nodes == null)
            initNodes();

        if (handler == null)
            handler = new Handler(Looper.getMainLooper());

        handler.post(new Runnable()
        {
            @Override
            public void run()
            {
                if (progressDialog != null)
                    progressDialog.setMessage("Extracting Data");
            }
        });

        int count = 0 ;
        int totalSize =  map.size();

        //get each item in  the map
        for ( Map.Entry<String, String> entry : map.entrySet() )
        {
            try
            {
                Thread.sleep(100);
            }catch (Exception ex){}

            String key = entry.getKey();
            int perc = (int) (((float)++count / (float)totalSize) * 100);
            callOnStatusUpdate("Extracting..",perc);

            //reformat the key since it has already been altered
            int index = key.indexOf("_");
            if (index > 0)
                key = key.substring(0,index);

            String value  = entry.getValue();
            key = key.toUpperCase();

            //find what type of data is has to be updated
            for (NodeInfo node : nodes) {
                if (key.compareTo(node.name) == 0 || key.compareTo(node.nameAlias) == 0) {
                    if (NodeInfo.TYPE_TABLE == node.type) {
                        //a table must be updated
                        updateTable(node.name, value, false);
                        /**
                         * update CDT for CUP with Exel sheet
                         */

                        break;
                    } else if (NodeInfo.TYPE_PROMO == node.type) {
                        //a promotion must be updated
                    } else if (NodeInfo.TYPE_VERSION == node.type) {
                        /*String keyTag = "VERSION";
                        int i = value.indexOf(keyTag);
                        if (i > 0) {
                            i += keyTag.length() + 2;
                            versionProcessing = value.substring(i, value.length());
                            writeSettingsVersion(versionProcessing);
                        }*/
                    } else if (NodeInfo.TYPE_QRMID == node.type) {
                        String keyTag = "qrmid";
                        int i = value.indexOf(keyTag);
                        if (i > 0) {
                            i += keyTag.length() + 2;
                            qrmidpro = value.substring(i, value.length());
                            writeQRMID(qrmidpro);
                        }
                    } else if (NodeInfo.TYPE_AID == node.type) {
                        //aid has to be updated for the particular issuer
                        int x = 0;

                    } else if (NodeInfo.TYPE_CAPK == node.type) {
                        //capk has to be updated
                        int x = 0;
                    }

                }
            }
        }
        updateCUPBinRange();
        return true;
    }

    private void wipeDatabaseTables() {
        DBHelperSync syncDB = DBHelperSync.getInstance(appContext);

        String quary = "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name  NOT LIKE 'android_%'";
        try {
            Cursor tables = syncDB.readWithCustomQuary(quary);
            if (tables == null || tables.getCount() == 0)
                return;

            //tables.moveToFirst();
            while (tables.moveToNext()) {
                String tableName = tables.getString(tables.getColumnIndex("name"));

                if(tableName.equals("PST") || tableName.equals("TMIF") || tableName.equals("MIT") || tableName.equals("CST")) {
                    continue;
                }

                String delquary = "DELETE FROM " + tableName;

                Log.e("DELETE TABLE NAME : ", " " + tableName);

                if (!tableName.equalsIgnoreCase("TLE")
                        && !tableName.equalsIgnoreCase("TFI")) {//
                    syncDB.executeCustomQuary(delquary);
                }
            }

            tables.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }


    /**
     * update CUP bin range  from Excel sheet
     */
    private void updateCUPBinRange() {
        try {

            Log.e("EXCEL", "Start.......");
            InputStream is;
            AssetManager assetManager = appContext.getAssets();
            is = assetManager.open("cup_data.xml");


            XmlPullParserFactory parserFactory;
            parserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = parserFactory.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(is, null);


            ArrayList<CUPData> cupList = new ArrayList<>();
            int eventType = parser.getEventType();
            CUPData currentPlayer = null;

            while (eventType != XmlPullParser.END_DOCUMENT) {
                String eltName = null;

                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        eltName = parser.getName();

                        Log.e("XML", " : " + eltName);


                        if ("cup".equals(eltName)) {
                            currentPlayer = new CUPData();
                            cupList.add(currentPlayer);
                        } else if (currentPlayer != null) {
                            if ("low".equals(eltName)) {
                                currentPlayer.setLow(parser.nextText());
                            } else if ("high".equals(eltName)) {
                                currentPlayer.setHigh(parser.nextText());
                            } else if ("id".equals(eltName)) {
                                currentPlayer.setId(parser.nextText());
                            }
                        }
                        break;
                }

                eventType = parser.next();
            }

            Log.e("CUP SIZE=======", " : " + cupList.size());

            for (int i = 0; i < cupList.size(); i++) {

                String id = cupList.get(i).getId();
                String query = "";
                if (id.equalsIgnoreCase("1") || id.equalsIgnoreCase("7")) {
                    query = "\"ID\":" + cupList.get(i).getId() +
                            ",\"PANLow\":\"" + cupList.get(i).getLow() +
                            "\",\"PANHigh\":\"" + cupList.get(i).getHigh() +
                            "\",\"CardAbbre\":\"VI\",\"CardLable\":\"VISA\",\"TrackRequired\":null,\"TxnBitmap\":null,\"FloorLimit\":0,\"HostIndex\":0,\"HostGroup\":0,\"MinPANDigit\":0,\"MaxPANDigit\":0," +
                            "\"IssuerNumber\":1,\"CheckLuhn\":false,\"ExpDateRequired\":false,\"ManualEntry\":false,\"ChkSvcCode\":true";

                } else if (id.equalsIgnoreCase("2") || id.equalsIgnoreCase("3")) {
                    query = "\"ID\":" + cupList.get(i).getId() +
                            ",\"PANLow\":\"" + cupList.get(i).getLow() +
                            "\",\"PANHigh\":\"" + cupList.get(i).getHigh() +
                            "\",\"CardAbbre\":\"MA\",\"CardLable\":\"MASTER\",\"TrackRequired\":null,\"TxnBitmap\":null,\"FloorLimit\":0,\"HostIndex\":0,\"HostGroup\":0,\"MinPANDigit\":0,\"MaxPANDigit\":0," +
                            "\"IssuerNumber\":2,\"CheckLuhn\":false,\"ExpDateRequired\":false,\"ManualEntry\":false,\"ChkSvcCode\":true";

                } else if (id.equalsIgnoreCase("4") || id.equalsIgnoreCase("5")
                        || id.equalsIgnoreCase("6")) {
                    query = "\"ID\":" + cupList.get(i).getId() +
                            ",\"PANLow\":\"" + cupList.get(i).getLow() +
                            "\",\"PANHigh\":\"" + cupList.get(i).getHigh() +
                            "\",\"CardAbbre\":\"AM\",\"CardLable\":\"AMEX\",\"TrackRequired\":null,\"TxnBitmap\":null,\"FloorLimit\":0,\"HostIndex\":0,\"HostGroup\":0,\"MinPANDigit\":0,\"MaxPANDigit\":0," +
                            "\"IssuerNumber\":3,\"CheckLuhn\":false,\"ExpDateRequired\":false,\"ManualEntry\":false,\"ChkSvcCode\":true";

                } else {
                    query = " \"ID\":" + cupList.get(i).getId() +
                            ",\"PANLow\":\"" + cupList.get(i).getLow() +
                            "\",\"PANHigh\":\"" + cupList.get(i).getHigh() + "\",\"CardAbbre\":\"CU\",\"CardLable\":\"CUP\",\"TrackRequired\":null,\"TxnBitmap\":null,\"FloorLimit\":0,\"HostIndex\":0,\"HostGroup\":0,\"MinPANDigit\":0,\"MaxPANDigit\":0,\"IssuerNumber\":4,\"CheckLuhn\":false," +
                            "\"ExpDateRequired\":false,\"ManualEntry\":false,\"ChkSvcCode\":true";
                }

                updateTable("CDT", query, true);
            }

            Log.e("EXCEL", "Complete.......");
        } catch (Exception e) {
            Log.e("EXCEL", "Exception.......");
            e.printStackTrace();
        }
    }

    //this is the method that has to be done using the first part of the alogright
    private boolean updateTable(String tableName, String record, boolean isLocal) {
        DBHelperSync syncDb = DBHelperSync.getInstance(appContext);
        JSONObject jsonObject = null;

        Log.e(" INSERT TABLE NAME", " : " + tableName);
        Log.e(" INSERT TABLE RECORD", " : " + record);

        try {
            String json = "{" + record + "}";
            jsonObject = new JSONObject(json);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }


        if (!isLocal) {
            if (tableName.equalsIgnoreCase("CDT")) {
                Log.e("-------", "-----COntain Table : " + tableName);
                return false;
            }
        }

        //get the column list of the table
        String quary = "SELECT * FROM " + tableName + " LIMIT 1"; //get the first record of the table
        String commitQuary = "";

        try {
            Cursor topRec = syncDb.readWithCustomQuary(quary);

            //get the primary key field
            String primaryColName = topRec.getColumnName(0);
            String primaryValue = getValueFromRec(primaryColName, jsonObject);

            Log.e(" INSERT TABLE tableName", " : " + tableName);
            Log.e("primary1", ":" + primaryColName);
            Log.e("primaryValue", " : " + primaryValue);

            boolean isExist = isItExistingRecord(tableName, primaryColName, primaryValue);

            //if its an existing record we need to perform an update quary

            //get the column list
            int columnCount = topRec.getColumnCount();

            String colName = "";
            String value = "";

            if (isExist) {
                commitQuary = "UPDATE " + tableName + " SET ";

                for (int i = 1; i < columnCount; i++) {
                    colName = topRec.getColumnName(i);

                    if (tableName.compareTo("MIT") == 0)  //enforce the non updating columns
                    {
                        boolean colExistInList = false;

                        for (int ii = 0; ii < Mit_constraints.length; ii++) {
                            if (colName.compareTo(Mit_constraints[ii]) == 0) {
                                colExistInList = true;
                                break;
                            }
                        }

                        if (colExistInList)
                            continue;

                    }

                    value = getValueFromRec(colName, jsonObject);

                    if(tableName.equals("TMIF")) {
                        if ((value == null) || (value.equals("NULL"))) {//store default values
                            value = "0";
                        }
                    }
                    else if(tableName.equals("MIT")) {
                        if ((value == null) || (value.equals("NULL"))) {//store default values
                            value = "NULL";
                        }
                    }
                    else {
                        if ((value == null) || (value.equals("NULL"))) {//store default values
                            value = " ";
                        }
                    }

                    if(value.contains("'")) {
                        value = value.replace("'", "''");
                    }

                    //value = removeTrailingColons(value);
                    if (colName.equals("IssuerList") || colName.equals("MerchantName") || colName.equals("RctHdr1") || colName.equals("RctHdr2") || colName.equals("RctHdr3") || colName.equals("MerchantID") )
                        value = "'" + value + "'";
                    else
                        value = value.replace('"','\'');

                    if (value.equals("FALSE"))
                        value = "0";
                    else if (value.equals("TRUE"))
                        value = "1";

                    commitQuary +=  colName + " = " + value + ", ";
                }

                commitQuary = commitQuary.substring(0, commitQuary.length() - 2);
                commitQuary += " ";
                commitQuary += " WHERE " + primaryColName + " = " + primaryValue + ", ";

                commitQuary = commitQuary.substring(0, commitQuary.length() - 2);
                boolean status = syncDb.executeCustomQuary(commitQuary);
                topRec.close();
                return status;
            } else {
                ContentValues content = new ContentValues();

                //if the record doesn't exist we perform the insertion
                try {
                    for (int i = 0; i < columnCount; i++) {
                        colName = topRec.getColumnName(i);

                        value = getValueFromRec(colName, jsonObject);

                        if (value == null) //store default values
                        {
                            value = " ";
                        }

                        value = removeTrailingColons(value);
                        value = value.replace('"', '\'');

                        if (value.equals("FALSE"))
                            value = "0";
                        else if (value.equals("TRUE"))
                            value = "1";

                        content.put(colName, value);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return false;
                }

                topRec.close();
                return syncDb.insertRecords(tableName, content);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private boolean isItExistingRecord(String tableName, String colName, String value) throws Exception {
        String quary = "SELECT * FROM " + tableName + " WHERE " + colName + " =" + value;

        DBHelperSync syncDB = DBHelperSync.getInstance(appContext);
        try {
            Cursor rec = syncDB.readWithCustomQuary(quary);
            int count = rec.getCount();
            rec.close();

            if (count > 0)
                return true;

            return false;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new Exception("No Error executing quary");
        }
    }

    private void initNodes() {
        nodes = new ArrayList<>();

        nodes.add(new NodeInfo("MIT", "MERCHANTINFORMATION", NodeInfo.TYPE_TABLE));
        nodes.add(new NodeInfo("TMIF", "TERMINALMERCHANTINFORMATION", NodeInfo.TYPE_TABLE));
        nodes.add(new NodeInfo("IHT", "HOST", NodeInfo.TYPE_TABLE));
        nodes.add(new NodeInfo("IIT", "ISSUERINFORMATION", NodeInfo.TYPE_TABLE));
        nodes.add(new NodeInfo("CDT", "CARDDATA", NodeInfo.TYPE_TABLE));
        nodes.add(new NodeInfo("TMIF", "TERMINALMERCHANTINFO", NodeInfo.TYPE_TABLE));
        nodes.add(new NodeInfo("BINPROMO", "BINRANGEPROMOTION", NodeInfo.TYPE_TABLE));
        nodes.add(new NodeInfo("PST", "GENERALINFO", NodeInfo.TYPE_TABLE));
        nodes.add(new NodeInfo("PROMOTION", "", NodeInfo.TYPE_PROMO));
        nodes.add(new NodeInfo("VERSION", "", NodeInfo.TYPE_VERSION));
        nodes.add(new NodeInfo("QRMID", "", NodeInfo.TYPE_QRMID));
        nodes.add(new NodeInfo("CAPK", "", NodeInfo.TYPE_CAPK));
        nodes.add(new NodeInfo("AID", "", NodeInfo.TYPE_AID));
    }

    private String removeTrailingColons(String value) {
        int count = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '"')
                count++;
        }

        if (count == 2)
            value = value.substring(1, value.length() - 1);

        return value;
    }

    private String getValueFromRecInvi(String tagName, JSONObject record) {
        try {
            String val = record.getString(tagName);
            return val;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getValueFromRec(String tagName, JSONObject record) {
        try {
            String val = record.getString(tagName);
            val = val.toUpperCase();
            return val;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setonGetBackEndData(onGetBackEndData func) {
        onGetBackEndData = func;
    }

    public void setOnGetTerminalBusyFlag(onGetTerminalBusyFlag func) {
        onGetTerminalBusyFlag = func;
    }

    public enum TYPE_DATA_BE {
        TYPE_TRANSACTION
    }

    public interface onParsingConfigs {
        void onParsing(String status, int Percentage);
    }

    public interface onGetBackEndData {
        List<JSONObject> getData(TYPE_DATA_BE type);
    }

    public interface onGetTerminalBusyFlag {
        boolean getTerminalBusyStatus();
    }

}


class NodeInfo {
    public static final int TYPE_TABLE = 1;
    public static final int TYPE_PROMO = 2;
    public static final int TYPE_VERSION = 3;
    public static final int TYPE_CAPK = 4;
    public static final int TYPE_AID = 5;
    public static final int TYPE_QRMID = 6;


    String name;
    String nameAlias;
    int type;

    public NodeInfo(String _name, String _nameAlias, int _type) {
        name = _name;
        nameAlias = _nameAlias;
        type = _type;
    }

}

class Settings
{
    String name;
    String value;

    public Settings(String _name,String _value)
    {
        name = _name;
        value = _value;
    }
}
