package com.harshana.wposandroiposapp.QRIntegration;

import android.os.Build;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.harshana.wposandroiposapp.Base.QRTran;
import com.harshana.wposandroiposapp.Utilities.XMlHelper.ChildTag;
import com.harshana.wposandroiposapp.Utilities.XMlHelper.ParentTag;
import com.harshana.wposandroiposapp.Utilities.XMlHelper.XMLHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import static com.harshana.wposandroiposapp.QRIntegration.QRDisplay.STATUS_COMPLETE;

public class QRCoreLogic {
    //general request message ids
    public static final int MESSAGE_ID = 1;
    public static final int QR_TYPE = 2;
    public static final int TX_TYPE = 3;
    public static final int PAYMENT_TYPE = 4;
    public static final int MID = 5;
    public static final int MCC = 6;
    public static final int MERC_NAME = 7;
    public static final int FEE_INDICATOR = 8;
    public static final int FEE_PERCENTAGE = 9;
    public static final int FEE_AMT = 10;
    public static final int COUNTRY_CODE = 11;
    public static final int MERC_CITY = 12;
    public static final int POSTAL_CODE = 13;
    public static final int BILL_DATA = 14;
    public static final int BILL_NUMBER = 15;
    public static final int STORE_LABEL = 16;
    public static final int LOYALTY_NUMBER = 17;
    public static final int REF_LABEL = 18;
    public static final int CUST_LABEL = 19;
    public static final int TRMNL_LABEL = 20;
    public static final int PURPOSE = 21;
    public static final int CONSUMER_DATA = 22;
    public static final int OPT_MERC = 23;
    public static final int OPT_MERC_NAME = 24;
    public static final int OPT_MERC_CITY = 25;
    public static final int TX_AMT = 26;
    public static final int TX_CURRENCY = 27;
    public static final int PAYMENT_TYPEx = 28;
    public static final int TX_ORG = 29;
    public static final int TX_STATUS = 30;
    public static final int MOBILE_NUMBER = 31;

    //response message ids
    private static int NAME1 = 0;
    private static int VALUE1 = 1;
    private static int NAME2 = 2;
    private static int VALUE2 = 3;
    private static int NAME3 = 4;
    private static int VALUE3 = 5;

    String values[][] = {
            {"name1",               "value1",        "name2",                    "value2",         "name3",                  "value3"},
            {null,                   null,            null,                     null,               null,                   null},          //message_od
            {"dynamic",              "0",             "static",                 "1",                null,                   null},          //qr_type
            {"pos payments",          "0",            "bill payments",          "1",                "fund transfer",        "3"},           //tx_type
            {"peer to peer",         "0",             "peer to merchant",       "1",                null,                   null},          //payment_type
            {null,                   null,            null,                     null,               null,                   null},          //mid
            {null,                   null,            null,                     null,               null,                   null},          //mcc
            {null,                   null,            null,                     null,               null,                   null},          //merc_name
            {"mobile pr tip",        "0",            "fee fixed 1",             "1",               "fee fixed 2",           "2"},        //fee_indicator
            {null,                   null,            null,                     null,               null,                   null},          //fee_percentage
            {null,                   null,            null,                     null,               null,                   null},          //fee_amount
            {null,                   null,            null,                     null,               null,                   null},          //country_code
            {null,                   null,            null,                     null,               null,                   null},          //merc_city
            {null,                   null,            null,                     null,               null,                   null},          //postal_code
            {"no additional data",      "0",            "additional data",      "1",                null,                   null},          //bill_data
            {null,                   null,            null,                     null,               null,                   null},          //bill_number
            {null,                   null,            null,                     null,               null,                   null},          //store_label
            {null,                   null,            null,                     null,               null,                   null},          //loyalty_number
            {null,                   null,            null,                     null,               null,                   null},          //ref_label
            {null,                   null,            null,                     null,               null,                   null},          //cust_label
            {null,                   null,            null,                     null,               null,                   null},          //trmnl_label
            {null,                   null,            null,                     null,               null,                   null},          //purpose
            {"mob cust",             "M",            "email cust",              "E",               "addr cust",             "A"},          //consumer_data
            {"no alt lang",           "0",            "alt lang",              "1",               null,                     null},          //consumer_data
            {null,                   null,            null,                     null,               null,                   null},          //opt_merc_name
            {null,                   null,            null,                     null,               null,                   null},          //opt_merc_city
            {null,                   null,            null,                     null,               null,                   null},          //tx_amnt
            {null,                   null,            null,                     null,               null,                   null},          //tc_currency
            {null,                   null,            null,                     null,               null,                   null},          //payment_type
            {null,                   null,            null,                     null,               null,                   null},          //tx_org
            {"complete",             "01",            "fail",                     "05",               "incomplete",           "09"},          //purpose
    };

    int tryCounter = 0;

    private boolean isValidationCorrect (int id,String data) {
        //find the defined tag
        for (TagDef tag : tagDefnitions) {
            if (tag.id == id) {
                if (data.length() == tag.length)
                    return true;
            }
        }

        return false;
    }

    public String getValueFromPossibleValues(int id,String name) {
        for (int i = 0; i < 6; i++) {
            if (values[id][i].equals(name))
                return values[id][i + 1];  //return the corresponding value
        }
        return null;
    }

    List<TagDef> tagDefnitions ;
    private static QRCoreLogic instance = null;
    public static QRCoreLogic getInstance() {
        if (instance == null)
            instance = new QRCoreLogic();

        return instance;
    }
    private QRCoreLogic()
    {
        tagDefnitions = new ArrayList<>();
    }

    public enum TagType {
        STRING,
        NEUMERIC,
        ALPHANUMARIC
    }
    boolean connected = false;

    public void addTagDefinition(int _id,String _tagName,TagType _type, int _length,String _desc) {
        TagDef td = new TagDef(_id,_tagName,_type,_length,_desc);
        tagDefnitions.add(td);
    }

    public String getTagName(int id) {
        for (TagDef t : tagDefnitions) {
            if (t.id == id)
                return t.tagName;
        }

        return null;
    }

    public static enum QRType {
        STATIC,
        DYNAMIC
    }

    public static enum TXType {
        POS_PAYMENT,
        BILL_PAYMENT,
        FUND_TRANSFER
    }

    public static enum PaymentType {
        PEER_TO_PEER,
        PEER_TO_MERCHANT
    }

    public static enum BillDataType {
        BILL_DATA_PRESENT,
        BILL_DATA_NOT_PRESENT,
    }

    public static enum LangOptions {
        ALTERNATE_REQUIRED,
        ALTERNATE_NOT_REQUIRED
    }

    public static enum FeeIndicator {
        MOBILE,
        FIXED,
        PERCENTAGE
    }

    boolean useCustCert = false;
    SSLContext sslContext = null;

    public void initQRCode() {
        //here we sets up all the things
        addTagDefinition(MESSAGE_ID, "MESSAGE_ID", QRCoreLogic.TagType.STRING, 12, "specifies the message id");
        addTagDefinition(QR_TYPE, "QR_TYPE", QRCoreLogic.TagType.NEUMERIC, 1, "defines the qr type required");
        addTagDefinition(TX_TYPE, "TX_TYPE", QRCoreLogic.TagType.ALPHANUMARIC, 1, "defines the transaction type");
        addTagDefinition(PAYMENT_TYPE, "PAYMENT_TYPE", QRCoreLogic.TagType.NEUMERIC, 1, "payment type");
        addTagDefinition(MID, "MID", QRCoreLogic.TagType.NEUMERIC, 15, "unique id provided for the merchant");
        addTagDefinition(MCC, "MCC", QRCoreLogic.TagType.NEUMERIC, 4, "merchant category code");
        addTagDefinition(MERC_NAME, "MERC_NAME", QRCoreLogic.TagType.ALPHANUMARIC, 25, "name of the merchant");
        addTagDefinition(FEE_INDICATOR, "FEE_INDICATE", QRCoreLogic.TagType.NEUMERIC, 2, "fee types");
        addTagDefinition(FEE_PERCENTAGE, "FEE_PERCENTAGE", QRCoreLogic.TagType.NEUMERIC, 5, "the percentage conv fee");
        addTagDefinition(FEE_AMT, "FEE_AMT", QRCoreLogic.TagType.NEUMERIC, 13, "fixed amount");
        addTagDefinition(COUNTRY_CODE, "COUNTRY_CODE", QRCoreLogic.TagType.NEUMERIC, 2, "country code");
        addTagDefinition(MERC_CITY, "MERC_CITY", QRCoreLogic.TagType.ALPHANUMARIC, 15, "city of the merchant");
        addTagDefinition(POSTAL_CODE, "POSTAL_CODE", QRCoreLogic.TagType.ALPHANUMARIC, 10, "postal code");
        addTagDefinition(BILL_DATA, "BILL_DATA", QRCoreLogic.TagType.NEUMERIC, 1, "bill data");
        addTagDefinition(BILL_NUMBER, "BILL_NUMBER", QRCoreLogic.TagType.ALPHANUMARIC, 25, "defines the invoice number ");
        addTagDefinition(STORE_LABEL, "STORE_LABEL", QRCoreLogic.TagType.ALPHANUMARIC, 25, "A distinctive value associated to a store. This value could be provided by the merchant or could be an indication of the mobile application to prompt the consumer to input a store label’");
        addTagDefinition(LOYALTY_NUMBER, "LOYALTY_NUMBER", QRCoreLogic.TagType.ALPHANUMARIC, 25, "A loyalty card number. This number could be provided by the merchant or could be an indication of the mobile application to prompt the consumer to input their loyalty number. ");
        addTagDefinition(REF_LABEL, "REF_LABEL", QRCoreLogic.TagType.ALPHANUMARIC, 25, "Any value as defined by the merchant or acquirer in order to identify the transaction. This value could be provided by the merchant or could be an indication for the mobile app to prompt the consumer to input a transaction");
        addTagDefinition(CUST_LABEL, "CUST_LABEL", QRCoreLogic.TagType.ALPHANUMARIC, 25, "any value identify specific customer");
        addTagDefinition(TRMNL_LABEL, "TRMNL_LABEL", QRCoreLogic.TagType.ALPHANUMARIC, 25, "desctinctive value for the terminal");
        addTagDefinition(PURPOSE, "PURPOSE", QRCoreLogic.TagType.ALPHANUMARIC, 25, "any value to define the purpose of the tran");
        addTagDefinition(CONSUMER_DATA, "CONSUMER_DATA", QRCoreLogic.TagType.ALPHANUMARIC, 3, "Contains indications that the mobile application is to provide the requested information in order to complete the transaction. The information requested should be provided by the mobile application in the authorization without unnecessarily prompting the consumer. Can be one of the following.");
        addTagDefinition(OPT_MERC, "OPT_MERC", QRCoreLogic.TagType.NEUMERIC, 1, "specify the need for an alternative lang");
        addTagDefinition(OPT_MERC_NAME, "OPT_MERC_NAME", QRCoreLogic.TagType.STRING, 25, "merchant name");
        addTagDefinition(OPT_MERC_CITY, "OPT_MERC_CITY", QRCoreLogic.TagType.STRING, 15, "specify merchant city in alternative lang");
        addTagDefinition(TX_AMT, "TX_AMT", QRCoreLogic.TagType.NEUMERIC, 12, "transaction amount without the dec point");
        addTagDefinition(TX_CURRENCY, "TX_CURRENCY", QRCoreLogic.TagType.NEUMERIC, 3, "transaction currency");
        addTagDefinition(PAYMENT_TYPEx, "PAYMENT_TYPEx", QRCoreLogic.TagType.NEUMERIC, 1, "payemnt type");
        addTagDefinition(TX_ORG, "TX_ORG", QRCoreLogic.TagType.ALPHANUMARIC, 20, "transaction origin");
        addTagDefinition(TX_STATUS, "TX_STATUS", QRCoreLogic.TagType.NEUMERIC, 1, "tran  status");
        addTagDefinition(MOBILE_NUMBER, "MOBILE_NUMBER", TagType.STRING, 10, "Merchant mobile no");
    }

    public String buildStatusMessage(String generatedQRCode) {
        ParentTag trxMessageTagParent = new ParentTag("TRX_MESSAGE");
        ParentTag messageParent = new ParentTag("MESSAGE");

        trxMessageTagParent.insertChild(messageParent);

        //ChildTag childTag = new ChildTag(getTagName(MESSAGE_ID),"emvco_acknowledge_qr_ack");
        ChildTag childTag = new ChildTag(getTagName(MESSAGE_ID), "emvco_validate_qr");

        messageParent.insertChild(childTag);

        childTag = new ChildTag("QR_CODE", generatedQRCode);
        messageParent.insertChild(childTag);

//        childTag = new ChildTag(getTagName((TX_STATUS)),getValueFromPossibleValues(TX_STATUS,"incomplete"));
//        messageParent.insertChild(childTag);

        XMLHelper xmlHelper = XMLHelper.getInstance();
        xmlHelper.addParentTag(trxMessageTagParent);

        String xml = "";
        try {
            xml = xmlHelper.generateXML();
        } catch (Exception ex) {
            return null;
        }

        return xml;
    }

    private String getDateTimeFormatted() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = dateFormat.format(new Date());

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        String strTime = timeFormat.format(new Date());

        String dt = strDate + "T" + strTime + ".511Z";
        return dt;
    }

    private JSONArray getDeviceData() {
        JSONArray jsonArray = new JSONArray();

        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("PropertyName", "AndroidVersion");
            jsonObject.put("PropertyValue", "5.1");

            jsonArray.put(jsonObject);

            jsonObject = new JSONObject();
            jsonObject.put("PropertyName", "Immi");
            jsonObject.put("PropertyValue", "867250030132883");

            jsonArray.put(jsonObject);

        } catch (Exception ex) {
        }


        return jsonArray;
    }

    public interface onStatusUpdate
    {
        void OnStatusUpdate(Status status);
    }

    private onStatusUpdate listener = null;

    public void setOnStatusUpdate(onStatusUpdate onStatusUpdate)
    {
        listener = onStatusUpdate;
    }

    public String buildGenQRJsonV2(QRType qrType,
                                 TXType txType,
                                 PaymentType paymentType,
                                 BillDataType billDataType,
                                 FeeIndicator feeIndicator,
                                 LangOptions langOptions,
                                 String countryCode,
                                 String mercCity,
                                 String postalCode,
                                 String txOrg,
                                 String billerId,
                                 String mobileNo,
                                 onRequireUserData dataCollectionInterface) throws Exception {

        if (qrType == null)
            throw new Exception("QRtype can not be null");

        if (txType == null && paymentType == null)
            throw new Exception("Either txType or PaymentType should be provided to continue");

        if (countryCode == null || mercCity == null || postalCode == null || txOrg == null || billerId == null || mobileNo == null)
            throw new Exception("country code , merch city , posalCode, txOrg, Mobileno and billerId all should be provided to continue");

        if (dataCollectionInterface == null)
            throw new Exception("Data acquiring listener can not be null");

        StringBuilder str = new StringBuilder();


        //set the child tags
        String strQRType = "";
        String strTXType = "";
        String strPaymentType = "";


        if (qrType == QRType.DYNAMIC)
            strQRType = getValueFromPossibleValues(QR_TYPE, "dynamic");
        else if (qrType == QRType.STATIC)
            strQRType = getValueFromPossibleValues(QR_TYPE, "static");

        if (strQRType.equals("0")) {
            if (txType == TXType.POS_PAYMENT)
                strTXType = getValueFromPossibleValues(TX_TYPE, "pos payments");
            else if (txType == TXType.BILL_PAYMENT)
                strTXType = getValueFromPossibleValues(TX_TYPE, "bills payments");
            else if (txType == TXType.FUND_TRANSFER)
                strTXType = getValueFromPossibleValues(TX_TYPE, "fund transfer");
        }


        JSONObject jsonObject = new JSONObject();
        JSONObject jsonObject2 = new JSONObject();
        JSONObject jsonObject3 = new JSONObject();
        JSONObject jsonObject4 = new JSONObject();
        String dtTime = getDateTimeFormatted();
        jsonObject.put("RequestTime", dtTime);
        jsonObject.put("hostname", "192.168.129.65");
        jsonObject.put("port", "1220");
        jsonObject.put("RequestID", "CCARD_123456780");

        String strBillDataType = null;
        String refLabel = null;

        if (billDataType == BillDataType.BILL_DATA_NOT_PRESENT)
            strBillDataType = getValueFromPossibleValues(BILL_DATA, "no additional data");
        else if (billDataType == BillDataType.BILL_DATA_PRESENT)
            strBillDataType = getValueFromPossibleValues(BILL_DATA, "additional data");

        refLabel = dataCollectionInterface.OnRequireUserData(REF_LABEL);

        String mid = null;

        if ((strQRType.equals("0") && strTXType.equals("0")) || (strQRType.equals("1") && billerId == null)) {
            mid = dataCollectionInterface.OnRequireUserData(MID);
            if (mid == null /*||  !isValidationCorrect(MID,mid)*/)
                throw new Exception("MID is not valid");

            jsonObject4.put(getTagName(MID), mid);
        }

        String txnAmount = null;
        if (strQRType.equals("0")) {
            txnAmount = dataCollectionInterface.OnRequireUserData(TX_AMT);

            Log.e("txnAmount========", " : " + txnAmount);
            txnAmount = txnAmount.substring(0, txnAmount.length() - 2) + "." + txnAmount.substring(txnAmount.length() - 2, txnAmount.length());
            jsonObject4.put(getTagName(TX_AMT), txnAmount);
        }

        if (strQRType.equals("0")) {
            if (paymentType == PaymentType.PEER_TO_PEER)
                strPaymentType = getValueFromPossibleValues(PAYMENT_TYPE, "peer to peer");
            else if (paymentType == PaymentType.PEER_TO_MERCHANT)
                strPaymentType = getValueFromPossibleValues(PAYMENT_TYPE, "peer to merchant");

            jsonObject4.put(getTagName(PAYMENT_TYPE), strPaymentType);
        }

        String merchName = null;

        if (mid != null) {
            merchName = dataCollectionInterface.OnRequireUserData(MERC_NAME);
            jsonObject4.put(getTagName(MERC_NAME), merchName.toUpperCase());
        }
        jsonObject4.put(getTagName(MERC_CITY), mercCity.toUpperCase());
        jsonObject4.put(getTagName(OPT_MERC), "0");

        String txStatus = null;
        txStatus = getValueFromPossibleValues(TX_STATUS, "incomplete");
        jsonObject4.put(getTagName(TX_STATUS), txStatus);

        jsonObject4.put(getTagName(REF_LABEL), refLabel);
        jsonObject4.put(getTagName(TX_TYPE), strTXType);
        jsonObject4.put(getTagName(QR_TYPE), strQRType);
        jsonObject4.put("MESSAGE_ID", "emvco_generate_qr");
        String txnCurrency = null;
        if (txnAmount != null) {
            txnCurrency = dataCollectionInterface.OnRequireUserData(TX_CURRENCY);
            jsonObject4.put(getTagName(TX_CURRENCY), txnCurrency);
        }

        if (postalCode != null) //optional
        {
            jsonObject4.put(getTagName(POSTAL_CODE), postalCode);
        }

        jsonObject4.put(getTagName(COUNTRY_CODE), countryCode.toUpperCase());
        jsonObject4.put("RequestTime", dtTime);
        jsonObject4.put(getTagName(BILL_DATA), strBillDataType);

        String mcc = null;
        mcc = dataCollectionInterface.OnRequireUserData(MCC);
        if (mcc == null || !isValidationCorrect(MCC, mcc))
            throw new Exception("MCC is not valid");

        jsonObject4.put(getTagName(MCC), mcc);
        jsonObject4.put(getTagName(TX_ORG), txOrg);

        jsonObject3.put("MESSAGE", jsonObject4);
        jsonObject2.put("TRX_MESSAGE", jsonObject3);
        jsonObject.put("xmlmsg", jsonObject2);
        JSONArray deviceData = getDeviceData();


        jsonObject.put("DeviceData", deviceData);

        String jsonString = jsonObject.toString();
        return jsonString;
    }

    private void callOnStatusUpdate(Status  status)
    {
        if (listener != null)
            listener.OnStatusUpdate(status);
    }

    Socket socket = null;

    private boolean connect(String ip,int port) {
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }

            socket = new Socket();
            callOnStatusUpdate(Status.CONNECTING);
            /*SocketAddress socketAddress=new InetSocketAddress("qrpos.sampath.lk", 1220);
            socket.bind(socketAddress);
            socket.connect(socketAddress,10 * 1000);*/
            socket.connect(new InetSocketAddress("qrpos.sampath.lk", 22), 10 * 1000);
            return true;
        } catch (Exception ex) {
            System.out.println("============== ex " + ex.toString());
            callOnStatusUpdate(Status.CONNECTION_FAILED);
            return false;
        }
    }

    public void prepareSSLContextForCustCert() {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            InputStream inputStream = getClass().getResourceAsStream("/assets/qrpos.crt");

            Certificate ca = null;
            try {
                ca = certificateFactory.generateCertificate(inputStream);
                System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            //create a key store containing our ca
            String keystoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keystoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            //create a trust manager which trust the ca in our key store
            String tmfAlgo = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(tmfAlgo);
            trustManagerFactory.init(keyStore);

            //create SSL context which uses our trust manager
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            //sslContext.init(null,new X509TrustManager[]{new NullX509TrustManager()},null);
            useCustCert = true;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String validateQR(String qrcode) {
        String url = "https://qrpos.sampath.lk/webservicesRest/api/lankaqr/v2/validateqr";
        String auth = "TFFSX3N1bnRlY2hpdDo3MytmOW50NG9NRy90WDNzWWdwZ1RWM2dURC8yamlGbHB1NXpUM1FkSDg2UUZqNDFHa2pudStBU210dEFpV28rdXh4U2pMbW8za28xT3BIaW9jQ2Y4d25RYXVLU1NtQUd1ZVRlSmR3SkZxNWpFSnBrVkd3c3E1KzdYeW9xa1BnWQ==";
        String auth2 = "c2l0czpnLzdUbWIxYXBxUkx0UzJSazhGV3dIazZ3NVR1ZFJBaWVrekNBYWU3ZkJxQ0VTR1VMeVBGUTNOWEF5NWp0REZ4bklKeUlSdWw3WEVVOE43ak1KalZ6dmVsUi9QZkUxSldqWW5nV3dCdm1MY1NkSjVJOGMvT2lHSTZWdW9MWDlxag==";

        Date now = new Date(); // java.util.Date, NOT java.sql.Date or java.sql.Timestamp!
        String format1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH).format(now);
        String response = "";

        try {
            URL downloadLink = new URL(url);
            HttpsURLConnection urlConnection = (HttpsURLConnection) downloadLink.openConnection();
            Log.e("________>>>>>", "useCustCert : " + useCustCert);

            if (useCustCert)
                urlConnection.setSSLSocketFactory(sslContext.getSocketFactory());

            urlConnection.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String s, SSLSession sslSession) {
                    return true;
                }
            });

            System.out.println("**********************************************");
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("ServiceName", "ValidateQR_V2");
            urlConnection.setRequestProperty("TokenID", "0");
            urlConnection.setRequestProperty("Authorization", "Basic " + auth2);
            urlConnection.setConnectTimeout(10 * 6000);
            urlConnection.setReadTimeout(10 * 6000);
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setChunkedStreamingMode(0);

            JSONObject jsobBody = new JSONObject();
            JSONObject jsobBody2 = new JSONObject();
            JSONObject jsobBody3 = new JSONObject();
            JSONObject jsobBody4 = new JSONObject();

            jsobBody.put("RequestTime", format1.toString());
            jsobBody.put("hostname", "192.168.129.65");
            jsobBody.put("port", "1220");
            jsobBody.put("RequestID", "CCARD_123456780");
            //Indeevari
            jsobBody4.put("MESSAGE_ID", "emvco_validate_qr");
            jsobBody4.put("QR_CODE", qrcode);
            jsobBody3.put("MESSAGE", jsobBody4);
            jsobBody2.put("TRX_MESSAGE", jsobBody3);

            jsobBody.put("xmlmsg", jsobBody2);

            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    out, "UTF-8"));
            writer.write(jsobBody.toString());
            writer.flush();

            urlConnection.connect();
            System.out.println("********************************************** sample object " + jsobBody.toString());
            InputStream inputStream = null;


            int respCode = urlConnection.getResponseCode();

            System.out.println("********************************************** respCode " + respCode);

            if (respCode != 200) {
                return "";
            }

            inputStream = urlConnection.getInputStream();

            //now read the response
            int readLen = 0;
            int totalLength = urlConnection.getContentLength();

            byte[] readBuffer = new byte[1024];

            //read the response
            StringBuilder str = new StringBuilder();
            while ((readLen = inputStream.read(readBuffer)) != -1)
                str.append(new String(readBuffer));

            response = str.toString();
            System.out.println("********************************************** response " + response);

            //------------
            inputStream.close();
            urlConnection.disconnect();
            //----------
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }

        return response;
    }

    public void fetchQRCodeRESTV2(final String url, final String jsobBody, final String username, final String password) {
        Thread netThread = new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                try {
                    Log.e("QR URL", " : " + url);
                    URL downloadLink = new URL(url);
                    HttpsURLConnection urlConnection = (HttpsURLConnection) downloadLink.openConnection();


                    if (useCustCert)
                        urlConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                    String auth = "";
//                    String auth =  username + ":" + password;
//                    auth = Base64.encodeToString(auth.getBytes("utf-8"),Base64.DEFAULT);
                    System.out.println("auth ============== " + auth);
                    urlConnection.setHostnameVerifier(new HostnameVerifier() {
                        @Override
                        public boolean verify(String s, SSLSession sslSession) {
                            return true;
                        }
                    });
                    //auth = "TFFSX3N1bnRlY2hpdDpuUE9LVjR3NFkyOVEzZlFHM2JYYVczTk4ySXVCcDQ5cHhYdmc5Rmx2UXVaMnVYdElCUWlDalR2cE5wVnFJSlBEc1RRV3VkMFNHRlBEOFBqV2NZbzRveTBZTXBVTlc2T1hlSCtKbHRYdTNVeW5qTlJIZkhNUzZ1elA5bTdnZzJ2eA==";
                    //auth = "sits:KHTeQgY9EdXuspo3s8Y0F4Gbcy13aygbQeYcVFIpe5UJ8ToAXCflvF2cbB7UOpfORHDxSjkacmj3bqyk9cCM77oohuvtE0anJ4v6qaFBinXAJldP14Sgr8rngvKYCDbI";
                    auth = "sits:e086IUtqljYKkUqW1+WFzyzA45lWNiQs7NPuBQehdDvmyVTlnAluEcnQttZJ+rAlF5rt6xAIrmA8itY5hTHW/BGHi/6PaHaW7yHUyQQiEgayadcMuunPsU13IKGcVYqB";
                    auth = Base64.encodeToString(auth.getBytes("utf-8"),Base64.DEFAULT);

                    auth = "c2l0czplMDg2SVV0cWxqWUtrVXFXMStXRnp5ekE0NWxXTmlRczdOUHVCUWVoZER2bXlWVGxuQWx1RWNuUXR0WkorckFsRjVydDZ4QUlybUE4aXRZNWhUSFcvQkdIaS82UGFIYVc3eUhVeVFRaUVnYXlhZGNNdXVuUHNVMTNJS0djVllxQgo=";
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "application/json");
                    urlConnection.setRequestProperty("ServiceName", "GenerateQR_V2");
                    urlConnection.setRequestProperty("TokenID", "0");
                    urlConnection.setRequestProperty("Authorization", "Basic " + auth);
                    //urlConnection.setRequestProperty("Host", "uatweb.sampath.lk");
                    //urlConnection.setRequestProperty("Authorization", "sits " + auth);
                    urlConnection.setConnectTimeout(10 * 1000);
                    urlConnection.setReadTimeout(10 * 2000);
                    urlConnection.setDoOutput(true);
                    urlConnection.setDoInput(true);
                    urlConnection.setChunkedStreamingMode(0);

//                    getDeviceData
                    Date now = new Date(); // java.util.Date, NOT java.sql.Date or java.sql.Timestamp!
                    String format1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH).format(now);

                    OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                    writer.write(jsobBody.toString());
                    writer.flush();

                    //urlConnection.connect();
                    System.out.println("========================================= sample object " + jsobBody.toString());
                    InputStream inputStream = null;
                    int respCode = urlConnection.getResponseCode();

                    System.out.println("respCode ========================================= " + respCode);

                    if (respCode != 200) {
                        String res = urlConnection.getResponseMessage();
                        qrListener.OnRecieveQRCode(null);
                        return;
                    }

                    System.out.println("========================================= 3");
                    inputStream = urlConnection.getInputStream();

                    //now read the response
                    int readLen = 0;
                    int totalLength = urlConnection.getContentLength();

                    byte[] readBuffer = new byte[1024];

                    //read the response
                    StringBuilder str = new StringBuilder();
                    while ((readLen = inputStream.read(readBuffer)) != -1)
                        str.append(new String(readBuffer));

                    String response = str.toString();
                    System.out.println("========================================= response " + response);
                    String qrCode = "";
                    String subCode = "";

                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        subCode = jsonObject.getString("SubCode");
                        if (subCode.equals("0"))
                            qrCode = jsonObject.getString("QR_CODE");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    //------------
                    inputStream.close();
                    urlConnection.disconnect();
                    //----------

                    if (!subCode.equals("0") || qrCode.isEmpty())
                        qrListener.OnRecieveQRCode(null);
                    else
                        qrListener.OnRecieveQRCode(qrCode);

//                    inputStream.close();
//                    //outputStream.close();
//
//                    urlConnection.disconnect();
                } catch (Exception ex) {
                    qrListener.OnRecieveQRCode(null);
                    ex.printStackTrace();
                }

            }
        });

        netThread.start();

    }

    public enum Status {
        CONNECTING,
        RECIVING,
        RECIEVE_FAILED,
        CONNECTION_FAILED,
        SUCCESS,
        FAILED,
        QR_PENDING,
        QR_STATUS_RETRIEVED
    }

    public interface onRequireUserData {
        String OnRequireUserData(int id);
    }

    public static class TagDef {
        String tagName;
        TagType type;
        int length;
        String desc;
        int id;

        public TagDef(int _id, String _tagName, TagType _type, int _length, String _desc) {
            tagName = _tagName;
            type = _type;
            length = _length;
            desc = _desc;
            id  = _id;
        }
    }

    public class NullHostNameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            Log.i("RestUtilImpl", "Approving certificate for " + hostname);
            return true;
        }

    }

    public interface onRecieveQRCode
    {
        void OnRecieveQRCode(String qrCode);
    }

    private onRecieveQRCode qrListener = null;

    public void setOnRecieveQRCode(onRecieveQRCode onRecieveQRCode)
    {
        qrListener = onRecieveQRCode;
    }
}
