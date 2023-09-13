package com.harshana.wposandroiposapp.Base;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;

import com.harshana.wposandroiposapp.Communication.CommEngine;
import com.harshana.wposandroiposapp.Cup.BinLoader;
import com.harshana.wposandroiposapp.Database.Card;
import com.harshana.wposandroiposapp.Database.DBHelper;
import com.harshana.wposandroiposapp.Database.DBHelperTransaction;
import com.harshana.wposandroiposapp.Database.Issuer;
import com.harshana.wposandroiposapp.DevArea.BitmapDev;
import com.harshana.wposandroiposapp.DevArea.ErrorInfoDev;
import com.harshana.wposandroiposapp.DevArea.GlobalData;
import com.harshana.wposandroiposapp.DevArea.PacketDev;
import com.harshana.wposandroiposapp.DevArea.TranStaticData;
import com.harshana.wposandroiposapp.DevArea.UnpackedPacket;
import com.harshana.wposandroiposapp.ECR.ECR;
import com.harshana.wposandroiposapp.MainActivity;
import com.harshana.wposandroiposapp.Packet.DataPacket;
import com.harshana.wposandroiposapp.Packet.ISO8583u;
import com.harshana.wposandroiposapp.Print.Receipt;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.Settings.ConfigSynchronize;
import com.harshana.wposandroiposapp.Settings.DBHelperSync;
import com.harshana.wposandroiposapp.Settings.Preferences;
import com.harshana.wposandroiposapp.Settings.SettingsInterpreter;
import com.harshana.wposandroiposapp.TLE.TLE;
import com.harshana.wposandroiposapp.TLV.TLV;
import com.harshana.wposandroiposapp.TLV.TLVList;
import com.harshana.wposandroiposapp.TransactionMode.Emv;
import com.harshana.wposandroiposapp.TransactionMode.Mag;
import com.harshana.wposandroiposapp.UI.Other.AutomatedTaskLogger;
import com.harshana.wposandroiposapp.Utilities.AssetUtils;
import com.harshana.wposandroiposapp.Utilities.AutomatedLogQueue;
import com.harshana.wposandroiposapp.Utilities.BytesUtil;
import com.harshana.wposandroiposapp.Utilities.Formatter;
import com.harshana.wposandroiposapp.Utilities.Scheduler;
import com.harshana.wposandroiposapp.Utilities.Sounds;
import com.harshana.wposandroiposapp.Utilities.StringUtils;
import com.harshana.wposandroiposapp.Utilities.Utility;

import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import sdk4.wangpos.libemvbinder.CAPK;
import sdk4.wangpos.libemvbinder.EmvAppList;
import sdk4.wangpos.libemvbinder.EmvCore;
import sdk4.wangpos.libemvbinder.EmvParam;
import wangpos.sdk4.emv.adp.AmexAID;
import wangpos.sdk4.emv.jspeedy.JSpeedyAID;
import wangpos.sdk4.libbasebinder.BankCard;
import wangpos.sdk4.libbasebinder.Core;
import wangpos.sdk4.libbasebinder.Printer;
import wangpos.sdk4.libkeymanagerbinder.Key;

import static com.harshana.wposandroiposapp.MainActivity.applicationBase;

public class Base {
    private static final int SEARCH_CARD_TIMEOUT = 5;
    private static final int CARD_READ_THREAD_SLEEP_LATENCY = 1000;

    //private int secondsForCardThreadsleep = 0;
    private static final String packageName = "com.harshana.wposandroiposapp";
    protected static Context appContext;
    private boolean terminalBusyFlag = false;

    public static final int GENERIC_ERROR_TRAN_MIDDLE  = 1;
    public static final int SUCCESS = 0;
    public static final int TRY_AGAIN = 2;
    private static final int ISSUER_CUP = 3;

    public boolean isMag = false;

    public void setTerminalBusyFlag(boolean state) {
        terminalBusyFlag = state;
    }
    public boolean isTerminalBusy() {
        return terminalBusyFlag;
    }

    public final static int PUSH_REVERSAL = 4;
    public static final String ASSETS_EMV_PARAM = "EmvParam";
    public static Handler mainHandler;
    protected static DBHelper configDatabase = null;
    protected static DBHelperTransaction transactionDatabase = null;
    //contains the visa test keys
    private static String[] CAPK_DATA = {
        // amex
        /*index C9*/  "9F0605A0000000259F2201C9DF05083230323431323331DF060101DF070101DF0281B0B362DB5733C15B8797B8ECEE55CB1A371F760E0BEDD3715BB270424FD4EA26062C38C3F4AAA3732A83D36EA8E9602F6683EECC6BAFF63DD2D49014BDE4D6D603CD744206B05B4BAD0C64C63AB3976B5C8CAAF8539549F5921C0B700D5B0F83C4E7E946068BAAAB5463544DB18C63801118F2182EFCC8A1E85E53C2A7AE839A5C6A3CABE73762B70D170AB64AFC6CA482944902611FB0061E09A67ACB77E493D998A0CCF93D81A4F6C0DC6B7DF22E62DBDF040103DF03148E8DFF443D78CD91DE88821D70C98F0638E51E49",
        /*index CA*/  "9F0605A0000000259F2201CADF05083230323431323331DF060101DF070101DF0281F8C23ECBD7119F479C2EE546C123A585D697A7D10B55C2D28BEF0D299C01DC65420A03FE5227ECDECB8025FBC86EEBC1935298C1753AB849936749719591758C315FA150400789BB14FADD6EAE2AD617DA38163199D1BAD5D3F8F6A7A20AEF420ADFE2404D30B219359C6A4952565CCCA6F11EC5BE564B49B0EA5BF5B3DC8C5C6401208D0029C3957A8C5922CBDE39D3A564C6DEBB6BD2AEF91FC27BB3D3892BEB9646DCE2E1EF8581EFFA712158AAEC541C0BBB4B3E279D7DA54E45A0ACC3570E712C9F7CDF985CFAFD382AE13A3B214A9E8E1E71AB1EA707895112ABC3A97D0FCB0AE2EE5C85492B6CFD54885CDD6337E895CC70FB3255E3DF040103DF03146BDA32B1AA171444C7E8F88075A74FBFE845765F",

        //visa
        /*index 92*/     "9F0605A0000000039F220192DF05083230323431323331DF060101DF070101DF0281B0996AF56F569187D09293C14810450ED8EE3357397B18A2458EFAA92DA3B6DF6514EC060195318FD43BE9B8F0CC669E3F844057CBDDF8BDA191BB64473BC8DC9A730DB8F6B4EDE3924186FFD9B8C7735789C23A36BA0B8AF65372EB57EA5D89E7D14E9C7B6B557460F10885DA16AC923F15AF3758F0F03EBD3C5C2C949CBA306DB44E6A2C076C5F67E281D7EF56785DC4D75945E491F01918800A9E2DC66F60080566CE0DAF8D17EAD46AD8E30A247C9FDF040103DF0314429C954A3859CEF91295F663C963E582ED6EB253",
        /*index 94*/     "9F0605A0000000039F220194DF05083230323431323331DF060101DF070101DF0281F8ACD2B12302EE644F3F835ABD1FC7A6F62CCE48FFEC622AA8EF062BEF6FB8BA8BC68BBF6AB5870EED579BC3973E121303D34841A796D6DCBC41DBF9E52C4609795C0CCF7EE86FA1D5CB041071ED2C51D2202F63F1156C58A92D38BC60BDF424E1776E2BC9648078A03B36FB554375FC53D57C73F5160EA59F3AFC5398EC7B67758D65C9BFF7828B6B82D4BE124A416AB7301914311EA462C19F771F31B3B57336000DFF732D3B83DE07052D730354D297BEC72871DCCF0E193F171ABA27EE464C6A97690943D59BDABB2A27EB71CEEBDAFA1176046478FD62FEC452D5CA393296530AA3F41927ADFE434A2DF2AE3054F8840657A26E0FC617DF040103DF0314C4A3C43CCF87327D136B804160E47D43B60E6E0F",
        /*index 95*/     "9F0605A0000000039F220195DF05083230323431323331DF060101DF070101DF028190BE9E1FA5E9A803852999C4AB432DB28600DCD9DAB76DFAAA47355A0FE37B1508AC6BF38860D3C6C2E5B12A3CAAF2A7005A7241EBAA7771112C74CF9A0634652FBCA0E5980C54A64761EA101A114E0F0B5572ADD57D010B7C9C887E104CA4EE1272DA66D997B9A90B5A6D624AB6C57E73C8F919000EB5F684898EF8C3DBEFB330C62660BED88EA78E909AFF05F6DA627BDF040103DF0314EE1511CEC71020A9B90443B37B1D5F6E703030F6",

        //master
        /* index F1*/    "9F0605A0000000049F2201F1DF05083230323431323331DF060101DF070101DF0281B0A0DCF4BDE19C3546B4B6F0414D174DDE294AABBB828C5A834D73AAE27C99B0B053A90278007239B6459FF0BBCD7B4B9C6C50AC02CE91368DA1BD21AAEADBC65347337D89B68F5C99A09D05BE02DD1F8C5BA20E2F13FB2A27C41D3F85CAD5CF6668E75851EC66EDBF98851FD4E42C44C1D59F5984703B27D5B9F21B8FA0D93279FBBF69E090642909C9EA27F898959541AA6757F5F624104F6E1D3A9532F2A6E51515AEAD1B43B3D7835088A2FAFA7BE7DF040103DF0314D8E68DA167AB5A85D8C3D55ECB9B0517A1A5B4BB",
        /* index FE*/    "9F0605A0000000049F2201EFDF05083230323431323331DF060101DF070101DF0281F8A191CB87473F29349B5D60A88B3EAEE0973AA6F1A082F358D849FDDFF9C091F899EDA9792CAF09EF28F5D22404B88A2293EEBBC1949C43BEA4D60CFD879A1539544E09E0F09F60F065B2BF2A13ECC705F3D468B9D33AE77AD9D3F19CA40F23DCF5EB7C04DC8F69EBA565B1EBCB4686CD274785530FF6F6E9EE43AA43FDB02CE00DAEC15C7B8FD6A9B394BABA419D3F6DC85E16569BE8E76989688EFEA2DF22FF7D35C043338DEAA982A02B866DE5328519EBBCD6F03CDD686673847F84DB651AB86C28CF1462562C577B853564A290C8556D818531268D25CC98A4CC6A0BDFFFDA2DCCA3A94C998559E307FDDF915006D9A987B07DDAEB3BDF040103DF031421766EBB0EE122AFB65D7845B73DB46BAB65427A",

        //cup
        /* index 0b */   "9F0605A0000003339F22010BDF05083230323431323331DF060101DF070101DF0281F8CF9FDF46B356378E9AF311B0F981B21A1F22F250FB11F55C958709E3C7241918293483289EAE688A094C02C344E2999F315A72841F489E24B1BA0056CFAB3B479D0E826452375DCDBB67E97EC2AA66F4601D774FEAEF775ACCC621BFEB65FB0053FC5F392AA5E1D4C41A4DE9FFDFDF1327C4BB874F1F63A599EE3902FE95E729FD78D4234DC7E6CF1ABABAA3F6DB29B7F05D1D901D2E76A606A8CBFFFFECBD918FA2D278BDB43B0434F5D45134BE1C2781D157D501FF43E5F1C470967CD57CE53B64D82974C8275937C5D8502A1252A8A5D6088A259B694F98648D9AF2CB0EFD9D943C69F896D49FA39702162ACB5AF29B90BADE005BC157DF040103DF0314BD331F9996A490B33C13441066A09AD3FEB5F66C",
        /* index 09 */   "9F0605A0000003339F220109DF05083230323431323331DF060101DF070101DF0281B0EB374DFC5A96B71D2863875EDA2EAFB96B1B439D3ECE0B1826A2672EEEFA7990286776F8BD989A15141A75C384DFC14FEF9243AAB32707659BE9E4797A247C2F0B6D99372F384AF62FE23BC54BCDC57A9ACD1D5585C303F201EF4E8B806AFB809DB1A3DB1CD112AC884F164A67B99C7D6E5A8A6DF1D3CAE6D7ED3D5BE725B2DE4ADE23FA679BF4EB15A93D8A6E29C7FFA1A70DE2E54F593D908A3BF9EBBD760BBFDC8DB8B54497E6C5BE0E4A4DAC29E5DF040103DF0314A075306EAB0045BAF72CDD33B3B678779DE1F527",
        /* index 08 */   "9F0605A0000003339F220108DF05083230323431323331DF060101DF070101DF028190B61645EDFD5498FB246444037A0FA18C0F101EBD8EFA54573CE6E6A7FBF63ED21D66340852B0211CF5EEF6A1CD989F66AF21A8EB19DBD8DBC3706D135363A0D683D046304F5A836BC1BC632821AFE7A2F75DA3C50AC74C545A754562204137169663CFCC0B06E67E2109EBA41BC67FF20CC8AC80D7B6EE1A95465B3B2657533EA56D92D539E5064360EA4850FED2D1BFDF040103DF0314EE23B616C95C02652AD18860E48787C079E8E85A",
        /* index 0a */   "9F0605A0000003339F22010ADF05083230323431323331DF060101DF070101DF028180B2AB1B6E9AC55A75ADFD5BBC34490E53C4C3381F34E60E7FAC21CC2B26DD34462B64A6FAE2495ED1DD383B8138BEA100FF9B7A111817E7B9869A9742B19E5C9DAC56F8B8827F11B05A08ECCF9E8D5E85B0F7CFA644EFF3E9B796688F38E006DEB21E101C01028903A06023AC5AAB8635F8E307A53AC742BDCE6A283F585F48EFDF040103DF0314C88BE6B2417C4F941C9371EA35A377158767E4E3",

        //JCB
        /* index 7A */   "9F0605A0000000659F22017ADF05083230323431323331DF060101DF070101DF0281F881CCF2D6E5CD28E4E12105B505AA161D9830DEFE2ABD8FFFA500839E345276CD3CCDF61B0FDE18AE48E1EA1A5CD7DA7A119BEFAE316C1F91D74BB77CD5C4E2EB91C3C356057D78561D1C661313D9540837CCDF9369C18E417E964C268B7FE60A387464C31A11358F303C18FB7C182BB3BD04148E0973A9FA8A128DA7B8F4E475C29A5CC5F2A289114FE7A3B34E1FECDABC8F8524A9C2230C778038B916106FF91EB77DBBF5AC973F3F2A3507590F5BF77CF94F39AF6F9D971B9207516A08F109B16DF1D1B4E673905EBC7B78561902B2C4C39CA864F4F422FAE9FE59CB112F82FFABBC9ACCB246EC46F0020BEDBF98EE768C206A0F13B5B3DF040103DF0314D9EA344CDB0D03764476DDE3F7DD1FCD63835662",
        /* index 11 */   "9F0605A0000000659F220111DF05083230323431323331DF060101DF070101DF0281B0A2583AA40746E3A63C22478F576D1EFC5FB046135A6FC739E82B55035F71B09BEB566EDB9968DD649B94B6DEDC033899884E908C27BE1CD291E5436F762553297763DAA3B890D778C0F01E3344CECDFB3BA70D7E055B8C760D0179A403D6B55F2B3B083912B183ADB7927441BED3395A199EEFE0DEBD1F5FC3264033DA856F4A8B93916885BD42F9C1F456AAB8CFA83AC574833EB5E87BB9D4C006A4B5346BD9E17E139AB6552D9C58BC041195336485DF040103DF0314D9FD62C9DD4E6DE7741E9A17FB1FF2C5DB948BCB",
        /* index 13 */   "9F0605A0000000659F220113DF05083230323431323331DF060101DF070101DF0281F8A3270868367E6E29349FC2743EE545AC53BD3029782488997650108524FD051E3B6EACA6A9A6C1441D28889A5F46413C8F62F3645AAEB30A1521EEF41FD4F3445BFA1AB29F9AC1A74D9A16B93293296CB09162B149BAC22F88AD8F322D684D6B49A12413FC1B6AC70EDEDB18EC1585519A89B50B3D03E14063C2CA58B7C2BA7FB22799A33BCDE6AFCBEB4A7D64911D08D18C47F9BD14A9FAD8805A15DE5A38945A97919B7AB88EFA11A88C0CD92C6EE7DC352AB0746ABF13585913C8A4E04464B77909C6BD94341A8976C4769EA6C0D30A60F4EE8FA19E767B170DF4FA80312DBA61DB645D5D1560873E2674E1F620083F30180BD96CA589DF040103DF031454CFAE617150DFA09D3F901C9123524523EBEDF3"
    };
    private static String[] AID_DATA = {
                    //Amex
                    "9F0606A00000002501DF0101009F090200019F0802008CDF1105DC50FC9800DF1205DE00FC9800DF130500100000009F1B0400000000DF150400000000DF160100DF170199DF14029F37DF1801019F7B06000000000100DF1906000000100000DF2006000002000000DF21060000007500009F3303E0F8C89F40056000F0F0019F1A020144DF8121050000000000DF812205F45084800CDF812005F45084800C9F1D10102C0000000000000000DF81230400100000DF81240400200000DF81250400200000DF812604005000009F3501229F6604A2004001",

                    //master
                    "9F0607A00000000410109F01009F090200029F1501129F16009F1A0201449F1C009F1D082C008000000000009F1E04112233449F3303E0B8C89F3501229F40056000F0F0019F4E009F6D0200019F7E00DF6000DF6200DF6300DF810800DF810900DF810A00DF810C0102DF810D00DF81170100DF811801B8DF81190108DF811A039F6A04DF811B0120DF811C020000DF811D0100DF811E0110DF811F0108DF812005F45084800CDF8121050000000000DF812205F45084800CDF812306000000000000DF812406999999999999DF812506999999999999DF812606999999999999DF812C01009F1B06000000000000DF2106000002500000DF19060000000000009F3501229F6604A2004001DF13050000000000DF1205FC50B8F800DF1105FC50B8A000DF20069999999999999F7B06000000000100",
                    "9F0607A00000001010309F01009F090200029F1501129F16009F1A0201449F1C009F1D082C008000000000009F1E04112233449F3303E0B8C89F3501229F40056000F0F0019F4E009F6D0200019F7E00DF6000DF6200DF6300DF810800DF810900DF810A00DF810C0102DF810D00DF81170100DF811801B8DF81190108DF811A039F6A04DF811B0120DF811C020000DF811D0100DF811E0110DF811F0108DF812005F45084800CDF8121050000000000DF812205F45084800CDF812306000000000000DF812406999999999999DF812506999999999999DF812606999999999999DF812C01009F1B06000000000000DF2106000002500000DF19060000000000009F3501229F6604A2004001DF13050000000000DF1205FC50B8F800DF1105FC50B8A000DF20069999999999999F7B06000000000100",

                    //visa
                    "9F0607A0000000031010DF0101009F0802008CDF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF150435303030DF160100DF170100DF14029F37DF1801019F7B060000000001009F090200969F3303E0B8C89F4005D000F0A0019F1A020144DF1906000000000001DF2006999999999999DF21060000025000009F3501229F6604A2004001",
                    "9F0607A0000000032010DF0101009F0802008CDF1105DC4000A800DF1205DC4004F800DF130500100000009F1B0400000000DF150435303030DF160100DF170100DF14029F37DF1801019F7B060000000001009F090200969F3303E0B8C89F4005D000F0A0019F1A020144DF1906000000000001DF2006999999999999DF21060000025000009F3501229F6604A2004001",

                    //cup
                    "9F0608A000000333010101DF0101009F090200309F0802008CDF1105584000A800DF1205584024F830DF130500100000009F1B0400000000DF150400000000DF160199DF170199DF14029F37DF1801019F7B06000000000100DF1906000000000000DF2006999999999999DF21060000025000019F3303E0F8C89F4005D000F0A0019F1A0201449F3501229F660426800001",
                    "9F0608A000000333010102DF0101009F090200309F0802008CDF1105584000A800DF1205584024F830DF130500100000009F1B0400000000DF150400000000DF160199DF170199DF14029F37DF1801019F7B06000000000100DF1906000000000000DF2006999999999999DF21060000025000019F3303E0F8C89F4005D000F0A0019F1A0201449F3501229F660426800001",
                    "9F0608A000000333010101DF0101009F090200309F0802008CDF1105584000A800DF1205584024F830DF130500100000009F1B0400000000DF150400000000DF160199DF170199DF14029F37DF1801019F7B06000000000100DF1906000000000000DF2006999999999999DF21060000025000019F3303E0F8C89F4005D000F0A0019F1A0201449F3501229F660426800001",
                    "9F0608A000000333010103DF0101009F090200309F0802008CDF1105584000A800DF1205584024F830DF130500100000009F1B0400000000DF150400000000DF160199DF170199DF14029F37DF1801019F7B06000000000100DF1906000000000000DF2006999999999999DF21060000025000019F3303E0F8C89F4005D000F0A0019F1A0201449F3501229F660426800001",
                    "9F0608A000000333010106DF0101009F090200309F0802008CDF1105584000A800DF1205584024F830DF130500100000009F1B0400000000DF150400000000DF160199DF170199DF14029F37DF1801019F7B06000000000100DF1906000000000000DF2006999999999999DF21060000025000019F3303E0F8C89F4005D000F0A0019F1A0201449F3501229F660426800001",

                    //JCB
                    "9F0607A0000000651010DF0101009F090200029F08020021DF1105FC60242800DF1205FC60ACF800DF130500100000009F1B0400000000DF150400000000DF160100DF170100DF14029F37DF1801019F7B06000000000100DF1906000000000000DF2006000002000000DF21060000007500009F3303E0F8C89F40056000F0F0019F1A020144DF8121050000000000DF812205F45084800CDF812005F45084800C9F1D10102C0000000000000000DF81230400100000DF81240400200000DF81250400200000DF812604005000009F350122"

            };
    private static int PATH_AMEX = 7;
    //    public static TextToSpeechUtil textToSpeechUtil ;
    private static int PAYPASS_MSD = 5;
    protected static  Activity mActivity;
    Scheduler scheduler;
    private boolean shouldCardThreadStop = false;

    public Base() {
    }

    public Base(Context applicationContext, Activity activity) {
        mActivity = activity;
        appContext = applicationContext;
    }

    //in here the routine is responsible only to transfer the new transaction which have not yet been uploaded
    //to the back end

    private static final String LAST_TRAN_ID_SETTING = "TRAN_ID";
    boolean checkforDuplicates = true;

    List<JSONObject> getTransactions()
    {
        DBHelperTransaction tranDB = DBHelperTransaction.getInstance(appContext);

        String quary = "SELECT * FROM TXN";
        List<JSONObject> tranList = null;

        Preferences pref = Preferences.getInstance(appContext);

        try {
            Cursor trans = tranDB.readWithCustomQuary(quary);
            if (trans.getCount() == 0)
                return null;


            String tranID = "";
            if (checkforDuplicates) {
                tranID = pref.getSetting(LAST_TRAN_ID_SETTING);
                int fromID = Integer.valueOf(tranID);

                //get the last record from the selected record set
                trans.moveToLast();

                int selId = trans.getInt(0);
                if (selId <= fromID) // there is no new transactions
                {
                    trans.close();
                    return null;
                }

                trans.moveToFirst();
                {
                    //move the cursor up until the from id
                    do {
                        if (fromID == trans.getInt(0))
                            break;
                    } while ((trans.moveToNext()));
                }
            }

            int colCount = trans.getColumnCount();
            tranList = new ArrayList<>();
            JSONObject jsonObject;

            while (trans.moveToNext())
            {
                jsonObject = new JSONObject();

                for (int i = 0; i < colCount ; i++) {
                    int type = trans.getType(i);
                    String colName = trans.getColumnName(i);

                    colName = findAndReformatTheColName(colName);
                    if (colName != null)
                    {
                        if (type == 1)
                        {
                            String strAmount = "";

                            //convert the amount to actual currency to cater a back end requirement
                            if (colName.equals("baseTransactionAmount"))
                            {
                                long amount = Long.valueOf(trans.getString(i));
                                if (amount < 100)
                                    strAmount = "0." + String.valueOf(amount);
                                else if (amount % 100 == 0)
                                {
                                    amount = amount / 100;
                                    strAmount = String.valueOf(amount);
                                    strAmount += ".00";
                                }
                                else
                                {
                                    int remainder = (int) (amount % 100);
                                    amount = amount / 100;
                                    strAmount = String.valueOf(amount) + "." + String.valueOf(remainder);
                                }

                                //convert the string to float
                                float val = Float.valueOf(strAmount);
                                jsonObject.put(colName,val);
                            }
                            else
                                jsonObject.put(colName, trans.getInt(i));
                        }

                        else if (type == 3 || type == 0)
                        {
                            if (trans.getString(i) == null)
                                jsonObject.put(colName, "");
                            else
                                jsonObject.put(colName, trans.getString(i));

                        }

                    }

                }

                String pan = (String)jsonObject.get("cardNumber");
                jsonObject.put("pan",pan);
                jsonObject.put("physicalTerminalId",getFirstTID());

                tranList.add(jsonObject);
            }

            //save the last tran id
            trans.moveToPrevious();
            if (checkforDuplicates) {
                tranID = trans.getString(0);
                pref.saveSetting(LAST_TRAN_ID_SETTING, tranID);
            }


            trans.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return tranList;
    }

    final static int TAG_BACK_END = 0;
    final static int TAG_TERMINAL = 1;

    String tags[][]  =
            {
                    {"approveCode" 				   ,"ApproveCode"}               ,
                    {"baseTransactionAmount"       ,"BaseTransactionAmount"}     ,
                    {"cardNumber"                  ,"PAN"}                       ,
                    {"cdtIndex"                    ,"CdtIndex"}                  ,
                    {"chipStatus"                  ,"ChipStatus"}                ,
                    {"creditOrDebit"               ,"Credit/Debit"}              ,
                    {"discount"                    ,"Discount"}                  ,
                    {"emvField55"                  ,"emvField55"}                ,
                    {"expDate"                     ,"expDate"}                   ,
                    {"extData"                     ,"ExtData"}                   ,
                    {"fuelCharge"                  ,"FuelCharge"}                ,
                    {"host"                        ,"Host"}                      ,
                    {"invoiceNumber"               ,"InvoiceNumber"}             ,
                    {"issuerNumber"                ,"IssuerNumber"}              ,
                    {"merchantId"                  ,"MerchantID"}                ,
                    {"merchantName"                ,"MerchantName"}              ,
                    {"merchantNumber"              ,"MerchantNumber"}            ,
                    {"mti"                         ,"MTI"}                       ,
                    {"nii"                         ,"NII"}                       ,
                    {"pan"                         ,"PAN"}                       ,
                    {"physicalTerminalId"          ,""}                          ,
                    {"procCode"                    ,"ProcCode"}                  ,
                    {"responseCode"                ,"ResponoseCode"}             ,
                    {"rrn"                         ,"RRN"}                       ,
                    {"secureNii"                   ,"SecureNII"}                 ,
                    {"serviceCrg"                  ,"ServiceCrg"}                ,
                    {"svcCode"                     ,"svcCode"}                   ,
                    {"terminalId"                  ,"TerminalID"}                ,
                    {"tipAmount"                   ,"TipAmount"}                 ,
                    {"totalAmount"                 ,"TotalAmount"}               ,
                    {"tpdu"                        ,"TPDU"}                      ,
                    {"traceNumber"                 ,"TraceNumber"}               ,
                    {"track2"                      ,"track2"}                    ,
                    {"transactionCode"             ,"TransactionCode"}           ,
                    {"transactionId"               ,"ID"}                        ,
                    {"txnDate"                     ,"TxnDate"}                   ,
                    {"txnTime"                     ,"TxnTime"}                   ,
                    {"voided"                      ,"Voided"}                    ,

            };

    private String findAndReformatTheColName(String colName)
    {
        //find the string in the array
        String tagName = "";
        int arrLen = tags.length;

        for ( int i = 0 ; i < arrLen; i++)
        {
            //check in the back end tags
            tagName = tags[i][TAG_BACK_END];
            if (tagName.equals(colName))
               return tagName;

        }

        //search in the db level
        for (int i = 0 ; i < arrLen; i++)
        {
            tagName = tags[i][TAG_TERMINAL];
            if (tagName.equals(colName))
                return tags[i][TAG_BACK_END];
        }

        return null;
    }


    public static ReversalHandler reversalHandler = null;
    private onInitializationFinished listener = null;

    public static void setAID() throws Exception {
        int result = -1;
        Log.d("CALLLLLLLLLLLLLLLLL", "AIDDDDD");
        Services.emvCore.delAllAID();

        Log.d("AID", "AID Importing started");
        for (int i = 0; i < AID_DATA.length; i++) {

            TLVList tlvList = TLVList.fromBinary(AID_DATA[i]);

            try {
                EmvAppList emvAppList = new EmvAppList(appContext);
                String AID = tlvList.getTLV("9F06").getValue();

                if(AID.equalsIgnoreCase("A00000002501")) {
                    AmexAID amexAID = new AmexAID();
                    amexAID.setAid(AID);
                    amexAID.setAppVersionNum(tlvList.getTLV("9F09").getValue());
                    //amexAID.setCvmReqLimit();
                    amexAID.setFloorLimit(Long.parseLong(tlvList.getTLV("9F1B").getValue()));
                    amexAID.setTACDefault(tlvList.getTLV("DF11").getValue());
                    amexAID.setTACDenial(tlvList.getTLV("DF13").getValue());
                    amexAID.setTACOnline(tlvList.getTLV("DF12").getValue());
                    //amexAID.setTermTransQuali();
                    result = Services.emvCore.addAID_AMEX(amexAID);
                }
                else if (AID.equalsIgnoreCase("A0000000651010")) {
                    JSpeedyAID jSpeedyAID = new JSpeedyAID();
                    //jSpeedyAID.setAcquirerID();
                    jSpeedyAID.setAddTermCap(tlvList.getTLV("9F40").getValue());
                    jSpeedyAID.setAid(tlvList.getTLV("9F06").getValue());
                    jSpeedyAID.setAppVersion(tlvList.getTLV("9F08").getValue());
                    jSpeedyAID.setClCVMLimit(Long.parseLong(tlvList.getTLV("DF21").getValue()));
                    jSpeedyAID.setClEMVFloorLimit(Long.parseLong(tlvList.getTLV("9F1B").getValue()));
                    jSpeedyAID.setClFloorLimit(Long.parseLong(tlvList.getTLV("DF19").getValue()));
                    jSpeedyAID.setClTransLimit(Long.parseLong(tlvList.getTLV("DF20").getValue()));
                    //jSpeedyAID.setCombinationOpt();
                    jSpeedyAID.setCountryCode(tlvList.getTLV("9F1A").getValue());
                    jSpeedyAID.setMaxTargetPercentage(tlvList.getTLV("DF16").getValue().getBytes()[0]);
                    //jSpeedyAID.setMerchantCode();
                    //jSpeedyAID.setMerchantName();
                    //jSpeedyAID.setRemovalTimeout();
                    //jSpeedyAID.setStaticTermInterPro();
                    jSpeedyAID.setTacDecline(tlvList.getTLV("DF13").getValue());
                    jSpeedyAID.setTacDefault(tlvList.getTLV("DF11").getValue());
                    jSpeedyAID.setTacOnline(tlvList.getTLV("DF12").getValue());
                    jSpeedyAID.setTargetPercentage(tlvList.getTLV("DF17").getValue().getBytes()[0]);
                    jSpeedyAID.setTermCap(tlvList.getTLV("9F33").getValue());
                    jSpeedyAID.setTerminalType(tlvList.getTLV("9F35").getValue());
                    jSpeedyAID.setThresholdValue(Long.parseLong(tlvList.getTLV("DF15").getValue()));
                    jSpeedyAID.setTransType("02");
                    result = Services.emvCore.addAID_JCB(jSpeedyAID);
                    Log.d("PAY val 09 : ", " : " + result);
                }
                else {
                    result = emvAppList.setAID(tlvList.getTLV("9F06").getValue());//aid

                    //set for cup testing
                    emvAppList.setTransCurrCode("144");


                    if (tlvList.getTLV("DF01") != null) {
                        emvAppList.setSelFlag(tlvList.getTLV("DF01").getValue());
                    }


                    if (tlvList.getTLV("9F09") != null) {
                        emvAppList.setVersion(tlvList.getTLV("9F09").getValue());
                    }
                    if (tlvList.getTLV("9F1D") != null) {
                        emvAppList.setRiskManData(tlvList.getTLV("9F1D").getValue());
                    }
                    String val = "";
                    if (tlvList.getTLV("DF11") != null) {
                        emvAppList.setTACDefault(tlvList.getTLV("DF11").getValue());//TAC－default
                        val = tlvList.getTLV("DF11").getValue();

                    }
                    if (tlvList.getTLV("DF12") != null) {
                        emvAppList.setTACOnline(tlvList.getTLV("DF12").getValue());//TAC－online
                        val = tlvList.getTLV("DF12").getValue();
                    }
                    if (tlvList.getTLV("DF13") != null) {
                        emvAppList.setTACDenial(tlvList.getTLV("DF13").getValue());//TAC－default
                        val = tlvList.getTLV("DF13").getValue();
                    }

                    if (tlvList.getTLV("DF8121") != null) {
                        emvAppList.setPPassTACDenial(tlvList.getTLV("DF8121").getValue());//TAC－default
                        val = tlvList.getTLV("DF8121").getValue();
                    }
                    if (tlvList.getTLV("DF8122") != null) {
                        emvAppList.setPPassTACOnline(tlvList.getTLV("DF8122").getValue());//TAC－online
                        val = tlvList.getTLV("DF8122").getValue();
                    }
                    if (tlvList.getTLV("DF8120") != null) {
                        emvAppList.setPPassTACDefault(tlvList.getTLV("DF8120").getValue());//TAC－default
                        val = tlvList.getTLV("DF8120").getValue();
                    }


                    if (tlvList.getTLV("9F1B") != null) {

                        long floorLimit = Long.parseLong(tlvList.getTLV("9F1B").getValue());
                        Log.d("floorLimit : ", "9F1B : " + floorLimit);
                        emvAppList.setFloorLimit(floorLimit);
                    }

                    //emvAppList.setForceOnline("1");
                    emvAppList.setFloorLimitCheck(1);
                    emvAppList.setCL_bFloorLimitCheck("1");
                    emvAppList.setCL_bAmount0Check("1");
                    emvAppList.setCL_bAmount0Option("1");
                    emvAppList.setCL_bCVMLimitCheck("1");
                    emvAppList.setCL_bTransLimitCheck("1");

                    emvAppList.setPPassTermFLmtFlg("1");
                    emvAppList.setPPassRdClssTxnLmtFlg("1");
                    emvAppList.setPPassRdCVMLmtFlg("1");
                    emvAppList.setPPassRdClssFLmtFlg("1");
                    emvAppList.setPPassRdClssTxnLmtONdeviceFlg("1");
                    emvAppList.setPPassRdClssTxnLmtNoONdeviceFlg("1");


                    if (tlvList.getTLV("DF15") != null) {
                        emvAppList.setThreshold(Long.parseLong(tlvList.getTLV("DF15").getValue()));
                    }
                    if (tlvList.getTLV("DF16") != null) {
                        emvAppList.setMaxTargetPer(Integer.parseInt(tlvList.getTLV("DF16").getValue()));
                    }
                    if (tlvList.getTLV("DF17") != null) {
                        emvAppList.setTargetPer(Integer.parseInt(tlvList.getTLV("DF17").getValue()));
                    }
                    if (tlvList.getTLV("DF14") != null) {
                        emvAppList.setDDOL(tlvList.getTLV("DF14").getValue());
                    }
                    if (tlvList.getTLV("DF18") != null) {
                        emvAppList.setBOnlinePin(Integer.parseInt(tlvList.getTLV("DF18").getValue()));
                    }
                    if (tlvList.getTLV("9F7B") != null) {
                        Log.d("PAY CONTACTLESS : ", "9F7B : " + Long.parseLong(tlvList.getTLV("9F7B").getValue()));
                        emvAppList.setEC_TermLimit(Long.parseLong(tlvList.getTLV("9F7B").getValue()));
                    } else {
                        Log.d("PAY CONTACTLESS : ", "9F7B : " + "NULL");

                    }
                    if (tlvList.getTLV("9F7B") != null) {
                        emvAppList.setEC_bTermLimitCheck(1);
                    }
                    if (tlvList.getTLV("DF19") != null) {
                        Log.d("PAY FLOOR LIMIT : ", "DF19 : " + Long.parseLong(tlvList.getTLV("DF19").getValue()));
                        emvAppList.setCL_FloorLimit(Long.parseLong(tlvList.getTLV("DF19").getValue()));
                        val = tlvList.getTLV("DF19").getValue();
                        Log.d("PAY val 01 : ", " : " + val);
                    }

                    if (tlvList.getTLV("9F33") != null)
                        result = emvAppList.setTermCapab(BytesUtil.byte2Int(Utility.hexStr2Byte(tlvList.getTLV("9F33").getValue())));
                    Log.d("PAY val 02 : ", " : " + result);

                    if (tlvList.getTLV("9F40") != null) {
                        String valu = (tlvList.getTLV("9F40").getValue());
                        result = emvAppList.setExTermCapab((tlvList.getTLV("9F40").getValue()));
                        Log.d("PAY val 03 : ", " : " + result);
                    }

                    if (tlvList.getTLV("9F1A") != null) {
                        result = emvAppList.setCountryCode(tlvList.getTLV("9F1A").getValue());
                        Log.d("PAY val 04 : ", " : " + result);
                    }

                    if (tlvList.getTLV("DF8125") != null) {//payvawe非接交易限额
                        long l = Long.parseLong(tlvList.getTLV("DF8125").getValue());
                        Log.d("setRdClss: ", "DF8125 : " + l);
                        emvAppList.setRdClssTxnLmtONdevice(Long.parseLong(tlvList.getTLV("DF8125").getValue()));
                        val = tlvList.getTLV("DF19").getValue();
                        Log.d("PAY val 05 : ", " : " + val);
                    }

                    if (tlvList.getTLV("DF8124") != null) {//payvawe非接交易限额
                        Log.d("setRdClssTxnLmtNoON: ", "DF8124 : " + Long.parseLong(tlvList.getTLV("DF8124").getValue()));
                        emvAppList.setRdClssTxnLmtNoONdevice(Long.parseLong(tlvList.getTLV("DF8124").getValue()));
                    }

                    if (tlvList.getTLV("DF20") != null) {
                        Log.d("PAY CONTACTLESS : ", "DF20 : " + Long.parseLong(tlvList.getTLV("DF20").getValue()));
                        emvAppList.setCL_TransLimit(Long.parseLong(tlvList.getTLV("DF20").getValue()));
                        val = tlvList.getTLV("DF20").getValue();
                        Log.d("PAY val 06 : ", " : " + val);
                    } else {
                        Log.d("PAYPASSTEST", "DF20 : " + "NULLLLL--->");
                    }

                    if (tlvList.getTLV("DF21") != null) {
                        Log.d("PAY DF21 : ", "DF21 : " + Long.parseLong(tlvList.getTLV("DF21").getValue()));
                        emvAppList.setCL_CVMLimit(2500000);
//                    emvAppList.setCL_CVMLimit(Long.parseLong(tlvList.getTLV("DF21").getValue()));
                        val = tlvList.getTLV("DF21").getValue();
                        Log.d("PAY val 07 : ", " : " + val);
                    }

                    if (tlvList.getTLV("DF811A") != null) {
                        Log.d("PAYPASSTEST", "UDOL :  " + tlvList.getTLV("DF811A").getValue());
                        emvAppList.setUDOL(StringUtils.string2BCD(tlvList.getTLV("DF811A").getValue()));
                    }
                    if (tlvList.getTLV("9F6D") != null) {
                        emvAppList.setMagAvn(StringUtils.string2BCD(tlvList.getTLV("9F6D").getValue()));
                    }
                    if (tlvList.getTLV("DF811A") != null) {
                        int len = tlvList.getTLV("DF811A").getLength();
                        emvAppList.setUDOLLen(new byte[]{(byte) ((len >> 8) & 0xFF), (byte) (len & 0xFF)});
                    }
                    if (tlvList.getTLV("DF811E") != null) {
                        emvAppList.setUcMagStrCVMCapWithCVM(StringUtils.string2BCD(tlvList.getTLV("DF811E").getValue()));
                    }
                    if (tlvList.getTLV("DF812C") != null) {
                        emvAppList.setUcMagStrCVMCapNoCVM(StringUtils.string2BCD(tlvList.getTLV("DF812C").getValue()));
                    }
                    if (tlvList.getTLV("DF811B") != null) {
                        emvAppList.setUcKernelConfig(new byte[]{0x30});
                    }
                    if (tlvList.getTLV("DF8118") != null) {
                        emvAppList.setUcCVMCap(StringUtils.string2BCD(tlvList.getTLV("DF8118").getValue()));
                    }
                    if (tlvList.getTLV("DF8119") != null) {
                        emvAppList.setUcCVMCapNoCVM(StringUtils.string2BCD(tlvList.getTLV("DF8119").getValue()));
                    }
                    if (tlvList.getTLV("DF8117") != null) {
                        Log.d("PAYPASSTEST", "DF8117 = " + tlvList.getTLV("DF8117").getValue());
                        emvAppList.setUcCardDataInputCap(StringUtils.string2BCD(tlvList.getTLV("DF8117").getValue()));
                    }
                    if (tlvList.getTLV("DF811F") != null) {
                        Log.d("PAYPASSTEST", "DF811F = " + tlvList.getTLV("DF811F").getValue());
                        emvAppList.setSecurityCap(StringUtils.string2BCD(tlvList.getTLV("DF811F").getValue()));
                    }
                    if (tlvList.getTLV("DF811F") != null) {
                        Log.d("PAYPASSTEST", "DF811F = " + tlvList.getTLV("DF811F").getValue());
                        emvAppList.setSecurityCap(StringUtils.string2BCD(tlvList.getTLV("DF811F").getValue()));
                    }

                    if (tlvList.getTLV("9F35") != null)
                        result = emvAppList.setTerminalType(BytesUtil.byte2Int(Utility.hexStr2Byte(tlvList.getTLV("9F35").getValue())));
                    Log.d("PAY val 08 : ", " : " + result);

                    emvAppList.setTermInfoEnableFlag(1);
                    emvAppList.setUcMagSupportFlg(new byte[]{0x01});

                    String extratedTag;
                    if (tlvList.getTLV("9F66") != null) {
                        extratedTag = tlvList.getTLV("9F66").getValue();
                        emvAppList.setTermTransQuali(extratedTag);   //A2004001   without online pin  A6004001 with online pin     //26800001 cup contactless
                    }

                    if (AID.equalsIgnoreCase("A0000000041010") || AID.equalsIgnoreCase("A0000000101030")) {
                        byte[] mcdata = emvAppList.toMCByteArray();
                        result = Services.emvCore.addAID_MC(emvAppList.toByteArray(), mcdata);
                    } else {
                        result = Services.emvCore.addAID(emvAppList.toByteArray());
                    }
                }
                Log.d("PAY val 09 : ", " : " + result);
                result = -1;

            } catch (RemoteException ex) {
                ex.printStackTrace();
                Log.d("haraparam", "error while setting aid");
            }
        }
    }

    public static Transaction loadCardAndIssuerToTransaction(Transaction currentTransaction, String PAN) {
        Cursor cardRec = null;
        Cursor issuerRec = null;

        Log.d("CARD LABEL 02 PAN", " : " + PAN);
        //find the cardData record in the data base and the matching bin
        if ((cardRec = configDatabase.findCardRecord(PAN)) == null)
            return null;

        currentTransaction.cdtIndex = cardRec.getInt(cardRec.getColumnIndex("ID"));
        currentTransaction.cardData = new Card(cardRec);
        Log.d("VVVVVVVV CAR 01", " : " + currentTransaction.cardData.cardLabel);
        currentTransaction.issuerNumber = currentTransaction.cardData.issuerNumber;
        //load the issuerData record
        if ((issuerRec = configDatabase.loadIssuer(currentTransaction.cardData.issuerNumber)) == null)
            return null;

        currentTransaction.issuerData = new Issuer(issuerRec);
        GlobalData.selectedIssuer = currentTransaction.issuerData.issuerNumber;

        cardRec.close();
        issuerRec.close();

        return currentTransaction;
    }

    public static String getTIDList(int host) {
        //get the total  number of transactions
        return configDatabase.getFieldsForTIDRequest(host);
    }

    public static int getAmountTotal(int tranCode, int issuerNumber, int merchantNumber) {
        //get the total  number of transactions
        String totalQuary = "";
        totalQuary = IssuerHostMap.genQuaryForSettlementTotalAmount(tranCode, issuerNumber, merchantNumber);

        Cursor totalRec = transactionDatabase.readWithCustomQuary(totalQuary);

        if (totalRec.getCount() == 0)
            return -1;

        totalRec.moveToFirst();

        int amountTotal = totalRec.getInt(totalRec.getColumnIndex("Total"));

        totalRec.close();
        return amountTotal;

    }

    public static int getAmountCount(int tranCode, int issuerNumber, int merchantNumber) {
        String countQuary = "";
        countQuary = IssuerHostMap.genQuaryForSettlementTotalCount(tranCode, issuerNumber, merchantNumber);

        Cursor countRec = transactionDatabase.readWithCustomQuary(countQuary);
        if (countRec.getCount() == 0)
            return -1;

        countRec.moveToFirst();
        int tranCount = countRec.getInt(countRec.getColumnIndex("tCount"));

        countRec.close();
        return tranCount;
    }

    public void setOnInitializationFinished(onInitializationFinished func) {
        listener = func;
    }

    public static  BankCard bankCard;

    public static Transaction currentTransaction;

    private boolean isInTransaction = false;

    public void setInTransaction(boolean status)
    {
        isInTransaction = status;
    }

    public boolean isInTransaction() {
        return isInTransaction;
    }

    public void errorTone() {
        Sounds s = Sounds.getInstance();
        s.playCustSound(R.raw.error);
    }

    public void setOnTransEndState(onTranStartEndState func)
    {
        tranState = func;
    }

    private onTranStartEndState tranState = null;


    public interface onTranStartEndState
    {
        void onTranStarted();
        void onTranEnd();
    }

    private void invokeCallbackOnTransStarted()
    {
        if (tranState != null)
            tranState.onTranStarted();
    }

    private void invokeCallbackOnTranFinished()
    {
        if (tranState != null)
            tranState.onTranEnd();
    }

    public interface onCTLSCardResult {
         void onCltsStartUI(String msg);
    }

    private onCTLSCardResult ctlsUIListener = null;

    public void setOnCtlsCardResultListener(onCTLSCardResult listener) {
        ctlsUIListener = listener;
    }

    public void callOnCLTLSListener(String msg) {
        if (ctlsUIListener != null)
            ctlsUIListener.onCltsStartUI(msg);
    }

    int CtlsCardResult = 0 ;
    //this thread infinitely check for the card inputs
    public boolean isCardReaderSleeping = false;

    public boolean getIsCardReaderSleeping() {
        return isCardReaderSleeping;
    }

    public void launchCardReaderThread() {
        if (getIsCardReaderSleeping()) {
            CardReadingThread cardReadingThread = new CardReadingThread();
            cardReadingThread.start();
        }
    }

    class CardReadingThread extends Thread {
        int idleCount = 0 ;

        @Override
        public void run() {
            byte[] outData =  new byte[512];
            int[] outDataLen = new int[1];

            isCardReaderSleeping = false;
            while (true) {
                sleepMe(CARD_READ_THREAD_SLEEP_LATENCY);

                if (GlobalData.isManualKeyIn) {
                    GlobalData.isManualKeyIn = false;

                    //initiate a manual key in transaction
                    currentTransaction  = new Transaction();
                    currentTransaction .lnBaseTransactionAmount = GlobalData.globalTransactionAmount;
                    currentTransaction.expDate = GlobalData.manualKeyExpDate;
                    currentTransaction.PAN = GlobalData.manualKeyPan;
                    currentTransaction.inChipStatus = Transaction.ChipStatusTypes.MANUAL_KEY_IN;

                    initTransaction();
                    Mag magTran = new Mag();

                    setInTransaction(true);
                    magTran.processTransaction();
                    setInTransaction(false);
                    applicationBase.setInTransaction(false);
                }

                if (shouldCardThreadStop || MainActivity.isOnPause)
                    continue;

                bankCard =  new BankCard(appContext);

                //issue a blocking call on searching for a card
                try {
                    int readCardResult;

                    if (GlobalData.globalTransactionAmount <= 0) {
                        readCardResult = bankCard.readCard(BankCard.CARD_TYPE_NORMAL, BankCard.CARD_MODE_ICC | BankCard.CARD_MODE_MAG, SEARCH_CARD_TIMEOUT, outData, outDataLen, getPinPackageName());
                    }
                    else {
                        readCardResult = bankCard.readCard(BankCard.CARD_TYPE_NORMAL, BankCard.CARD_MODE_PICC | BankCard.CARD_MODE_ICC |
                                    BankCard.CARD_MODE_NFC, SEARCH_CARD_TIMEOUT, outData, outDataLen, getPinPackageName());
                    }

                    if (readCardResult == 0) {//successful detection of the card
                        if (MainActivity.isOnPause)
                            continue;

                        switch (outData[0]) {
                            //handles the mag stripe and spawn a new transaction
                            case 0x00:
                                idleCount = 0;
                                int len1 = outData[1];
                                int len2 = outData[2];
                                int len3 = outData[3];

                                if (len1 == 0 && len2 == 0 && len3 == 0) {
                                    showToast("Swipe card error",TOAST_TYPE_FAILED);
                                    errorTone();
                                    break;
                                }
                                else if (len2 == 0) {
                                    showToast("No track 2 data found",TOAST_TYPE_INFO);
                                    errorTone();
                                    break;
                                }

                                byte[] track2Data = Arrays.copyOfRange(outData,len1 + 4,len1 + len2 + 4);

                                if (len2 > 0) {
                                    Sounds sounds = Sounds.getInstance();
                                    sounds.playCustSound(R.raw.tran_detect);

                                    //parse the track 2 data in to the transaction object
                                    currentTransaction = new Transaction();
                                    currentTransaction.track2 = new String(track2Data);

                                    String [] splitted =  currentTransaction.track2.split("=");
                                    String serviceCode = splitted[1];
                                    String cardNumber = splitted[0];

                                    String expDate = serviceCode.substring(0,4);
                                    serviceCode = serviceCode.substring(4);

                                    currentTransaction.scvCode = serviceCode;
                                    currentTransaction.PAN = cardNumber;
                                    currentTransaction.expDate = expDate;
                                    currentTransaction.inChipStatus = Transaction.ChipStatusTypes.NOT_USING_CHIP;

                                    initTransaction();
                                    Mag magTran = new Mag();
                                    setInTransaction(true);

                                    int tranResult = -1;

                                    tranResult = magTran.processTransaction();

                                    if ((tranResult == GENERIC_ERROR_TRAN_MIDDLE) && (SettingsInterpreter.isECREnabled()) && (MainActivity.ecr.isECRInitiated))
                                        MainActivity.ecr.pushTransactionDetails(GENERIC_ERROR_TRAN_MIDDLE,null,null);

                                    setInTransaction(false);
                                    MainActivity.ecr.isECRInitiated = false;
                                }
                                break;

                            case 0x01:
                                idleCount = 0;
                                showToast("Card reading failed",TOAST_TYPE_FAILED);
                                break;

                            //handles the chip card detection and spawning a new transaction
                            case 0x05:
                                idleCount = 0;
                                Sounds sounds = Sounds.getInstance();
                                sounds.playCustSound(R.raw.tran_detect);

                                currentTransaction = new Transaction();
                                currentTransaction.inChipStatus = Transaction.ChipStatusTypes.EMV_CARD;
                                currentTransaction.inTransactionCode = TranStaticData.TranTypes.SALE;

                                initTransaction();
                                Emv emvTran =  new Emv();
                                setInTransaction(true);

                                int tranResult = -1;

                                //setAnimationImageResource(R.drawable.processing);
                                tranResult = emvTran.processTransaction(mActivity);
                                //setAnimationImageResource(R.drawable.welcome_anim_new);

                                if ( (tranResult == GENERIC_ERROR_TRAN_MIDDLE) && (SettingsInterpreter.isECREnabled()) && (MainActivity.ecr.isECRInitiated))
                                        MainActivity.ecr.pushTransactionDetails(GENERIC_ERROR_TRAN_MIDDLE,null,null);
                                    MainActivity.ecr.isECRInitiated = false;

                                setInTransaction(false);
                                break;

                            case 0x07:
                                idleCount = 0;
                                if (GlobalData.globalTransactionAmount <= 0)
                                    break;

                                sounds = Sounds.getInstance();
                                sounds.playCustSound(R.raw.tran_detect);

                                currentTransaction = new Transaction();
                                currentTransaction.inChipStatus = Transaction.ChipStatusTypes.CTLS_CARD;
                                currentTransaction.inTransactionCode = TranStaticData.TranTypes.SALE;
                                initTransaction();
                                Emv ctlsTran =  new Emv();
                                setInTransaction(true);
                                //invoke the callbacks
                                invokeCallbackOnTransStarted();
                                CtlsCardResult = ctlsTran.processTransaction(mActivity);
                                invokeCallbackOnTranFinished();
                                setInTransaction(false);

                                if (CtlsCardResult == TRY_AGAIN) {
                                    CtlsCardResult = 0;
                                    Thread cardUIThread = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            GlobalData.globalTransactionAmount = currentTransaction.lnBaseTransactionAmount;
                                            callOnCLTLSListener("Tap");
                                        }
                                    });
                                    cardUIThread.start();
                                }

                                break;

                            case 0x03:
                                // showToast("Search Card time out");
                                break;

                            case 0x04:
                                // showToast("Search Card time out");
                                break;

                            default:
                                showToast("Invalid Attempt, Please try again",TOAST_TYPE_FAILED);
                                errorTone();
                                MainActivity.ecr.isECRInitiated = false;
                                setInTransaction(false);
                                break;
                        }
                    }
                    else {
                        showToast("Read Card result error",TOAST_TYPE_FAILED);
                        errorTone();
                        MainActivity.ecr.isECRInitiated = false;
                        setInTransaction(false);
                        break;
                    }
                }catch (Exception ex) {
                    ex.printStackTrace();
                    MainActivity.ecr.isECRInitiated = false;
                    setInTransaction(false);
                }
            }
        }
    }

    public boolean isCardLabel(String cardLabel) {
        cardLabel = cardLabel.toUpperCase();
        String tranCardLabel = currentTransaction.cardData.cardLabel;
        tranCardLabel = tranCardLabel.toUpperCase();

        return cardLabel.equals(tranCardLabel);
    }

    private void scheduleAutomatedTasks() {
        scheduler.startRunning();

        //scheduled a task to perform the auto settlement
        //schedule  a auto settlement job for each host
        if (SettingsInterpreter.isTerminalAutoSettlementEnabled())      //check whether the terminal auto settlement is enabled
        {
            for (int hostIndex = 0; hostIndex < IssuerHostMap.numHostEntries; hostIndex++) {
                String autoSettlementTime = IssuerHostMap.hosts[hostIndex].autoSettlmentTime;
                boolean isAutoSettlementEnabled = (IssuerHostMap.hosts[hostIndex].isAutoSettlmentEnabled == 1) ? true : false;

                if (isAutoSettlementEnabled) {
                    final int finalHostIndex = hostIndex;
                    scheduler.scheduleATask(new Scheduler.ScheduledTask() {
                        @Override
                        public boolean execute() {
                            //bring up the automated logger screen
                            if (isInTransaction || isTerminalBusy())
                                return false;

                            startAutomatedTaskLogger("Automated Settlement", "Please do not shutdown or Restart the terminal until the process finished");
                            performAutoSettlement(finalHostIndex);
                            closeAutomatedTaskLogger();
                            return false;
                        }
                    }, autoSettlementTime, Scheduler.RescheduleType.DAILY);
                }

            }

        }


        //schedule a task to update the cpu usage on the home view
        scheduler.scheduleATask(new Scheduler.ScheduledTask() {
            @Override
            public boolean execute() {
                return false;
            }
        }, null, Scheduler.RescheduleType.RUN_AT_EACH_LOOP);

    }

    public static final int TOAST_TYPE_WARNING = 1;
    public static final int TOAST_TYPE_SUCCESS = 2;
    public static final int TOAST_TYPE_FAILED = 3;
    public static final int TOAST_TYPE_INFO = 4;

    private String getFirstTID() {
        DBHelper dbHelper = DBHelper.getInstance(appContext);
        String quary = "SELECT TerminalID FROM TMIF LIMIT 1";

        String firstTID = "";

        try {
            Cursor tidRec = dbHelper.readWithCustomQuary(quary);
            if (tidRec == null || tidRec.getCount() == 0)
                return null;

            tidRec.moveToFirst();
            firstTID = tidRec.getString(tidRec.getColumnIndex("TerminalID"));

            //reformat the link
            tidRec.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return firstTID;
    }

    public void updateStatusText(String text) {
        Message msg = new Message();
        msg.what = MainActivity.OPER_UPDATE_STATUS_TEXT;
        msg.getData().putString("status_text",text);
        mainHandler.sendMessage(msg);
    }

    public void playSound(int r) {
        Sounds sounds = Sounds.getInstance();
        sounds.playCustSound(r);
    }

    //this invoke the input amount screen ,this will be invoked async
    public void invokeAmountInputScreen() {
        Message msg = new Message();
        msg.what = MainActivity.OPER_START_ACTIVITY;
        msg.arg1 = MainActivity.ACTIVITY_START_AMOUNT_INPUT;
        GlobalWait.setWaiting();
        mainHandler.sendMessage(msg);
    }

    protected void requestConfirmLastFour() {
        Message msg = new Message();
        msg.what = MainActivity.OPER_START_ACTIVITY;
        msg.arg1 = MainActivity.ACTIVITY_LAST_FOUR;
        GlobalWait.setWaiting();
        mainHandler.sendMessage(msg);
    }

    public boolean getResultFromInputAmountScreen() {
        while (true) {
            if (!GlobalWait.isWaiting())
                break;
            sleepMe(400);
        }

        return !GlobalWait.isLastOperCancelled();
    }

    protected boolean loadTerminal() {
        //by now the merchant id is available to process so we can load the relevant terminal record to proceed with the transaction
        Cursor terminalRec = null;

        if((terminalRec = configDatabase.loadTerminal(currentTransaction.merchantNumber)) == null) {
            showToast("Error with terminal record loading",TOAST_TYPE_FAILED);
            return false;
        }

        currentTransaction.terminalID = terminalRec.getString(terminalRec.getColumnIndex("TerminalID"));
        currentTransaction.merchantID = terminalRec.getString(terminalRec.getColumnIndex("MerchantID"));
        currentTransaction.currencySymbol = configDatabase.getCurrency(currentTransaction.merchantNumber);

        Log.d("Merchant ID--->", " : " + currentTransaction.merchantID);
        //since we have moved the nii and secure nii to the iit we get it from there
        Cursor issuerRec = null;
        if (currentTransaction.actualIssuerNumber  == 0)
            currentTransaction.actualIssuerNumber = currentTransaction.issuerNumber;
        if ((issuerRec = configDatabase.loadIssuer(currentTransaction.actualIssuerNumber)) == null) {
            showToast("Error with loading network parameters", TOAST_TYPE_FAILED);
            return false;
        }

        Issuer actualIssuer = new Issuer(issuerRec);
        currentTransaction.NII = actualIssuer.NII;
        currentTransaction.secureNII = actualIssuer.secureNII;
        currentTransaction.TPDU = actualIssuer.TPDU;

        if (currentTransaction.cardData != null) {
            String cardLabel = currentTransaction.cardData.cardLabel;
            if (cardLabel.equalsIgnoreCase("cup"))
                currentTransaction.NII = "080";
        }

        currentTransaction.inInvoiceNumber = terminalRec.getInt(terminalRec.getColumnIndex("InvNumber"));
        currentTransaction.batchNumber = terminalRec.getInt(terminalRec.getColumnIndex("BatchNumber"));

        GlobalData.addressLine1 = terminalRec.getString(terminalRec.getColumnIndex("RctHdr1"));
        GlobalData.addressLine2 = terminalRec.getString(terminalRec.getColumnIndex("RctHdr2"));
        GlobalData.addressLine3 = terminalRec.getString(terminalRec.getColumnIndex("RctHdr3"));
        GlobalData.merchantName = terminalRec.getString(terminalRec.getColumnIndex("MerchantName"));

        GlobalData.autoSettleDate = terminalRec.getString(terminalRec.getColumnIndex("AutoSettDate"));
        GlobalData.autoSettleTime = terminalRec.getString(terminalRec.getColumnIndex("AutoSettTime"));

        issuerRec.close();
        terminalRec.close();
        return true;
    }

    //setting  certificate authority public keys
    private void setCAPK() {

        try {
            Services.emvCore.delAllCAPK();
        } catch (RemoteException ex) {
            ex.printStackTrace();
        }
        for (int i = 0; i < CAPK_DATA.length; i++) {
            TLVList tlvList = TLVList.fromBinary(CAPK_DATA[i]);

            try {
                CAPK capk = new CAPK(appContext);
                capk.setRID(tlvList.getTLV("9F06").getValue());// rid
                capk.setKeyID(tlvList.getTLV("9F22").getValue());//认证中心公钥索引

                if (tlvList.getTLV("DF05") != null) {
                    capk.setExpDate(tlvList.getTLV("DF05").getValue());//认证中心公钥有效期
                    Log.d("acpkData", "ExpDate--->" + capk.getExpDate() + "\ntag-->" + tlvList.getTLV("DF05").getValue());
                }
                if (tlvList.getTLV("DF06") != null) {
                    capk.setHashInd(tlvList.getTLV("DF06").getValue());//认证中心公钥哈什算法标识
                    Log.d("acpkData", "HashInd--->" + capk.getHashInd() + "\ntag-->" + tlvList.getTLV("DF06").getValue());
                }
                if (tlvList.getTLV("DF07") != null) {
                    capk.setArithInd(tlvList.getTLV("DF07").getValue());//认证中心公钥算法标识
                    Log.d("acpkData", "ArithInd--->" + capk.getArithInd() + "\ntag-->" + tlvList.getTLV("DF07").getValue());
                }
                if (tlvList.getTLV("DF02") != null) {
                    TLV key = tlvList.getTLV("DF02");
                    String valueu = key.getValue();

                    capk.setModul(tlvList.getTLV("DF02").getValue());//认证中心公钥模
                    Log.d("acpkData", "tModul--->" + capk.getModul() + "\ntag-->" + tlvList.getTLV("DF02").getValue());
                }
                if (tlvList.getTLV("DF04") != null) {
                    capk.setExponent(tlvList.getTLV("DF04").getValue());//认证中心公钥指数
                    Log.d("acpkData", "Exponent--->" + capk.getExponent() + "\ntag-->" + tlvList.getTLV("DF04").getValue());
                }
                if (tlvList.getTLV("DF03") != null) {
                    capk.setCheckSum(tlvList.getTLV("DF03").getValue().substring(0, 40));//认证中心公钥校验值
                    Log.d("acpkData", tlvList.getTLV("DF03").getLength() + "----" + tlvList.getTLV("DF03").getTLLength() + "CheckSum--->" + capk.getCheckSum() + "\ntag-->" + tlvList.getTLV("DF03").getValue() + "\ndataSize" + capk.toByteArray().length);
                }

                Log.d("addCapk", tlvList.toString() + "\n" + "capkSize-->" + capk.toByteArray().length + "\n" + capk.print());
                int result = Services.emvCore.addCAPK(capk.toByteArray());
                result = 0;


            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void setEmvParam() {
        Log.d("mEmvCore", Services.emvCore + "setEmvParam" + Services.emvCore);
        String[] filesArray = AssetUtils.getFilesArrayFromAssets(appContext, ASSETS_EMV_PARAM);

        for (int i = 0; i < filesArray.length; i++) {
            byte[] bytes = AssetUtils.getFromAssets(appContext, filesArray[i]);
            EmvParam emvParam = new EmvParam(appContext);

            try {
                emvParam.setTransType(02);
                emvParam.setTransType9C("00");
                emvParam.setMerchId("000000000000001");
                emvParam.setTermId("11111111");
                emvParam.setTransCurrCode("0144");
                emvParam.setSupportPSESel(1);

            } catch (RemoteException e) {
                e.printStackTrace();
            }

            try {
                Services.emvCore.setParam(emvParam.toByteArray());
                byte[] out = new byte[1024];
                int[] length = {1024};
                Services.emvCore.getParam(out, length);
                emvParam.parseByteArray(emvParam.toByteArray());
                Log.d("zys", "emvParam.print() = " + emvParam.print());
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void showToast(String message, int type) {
        Message msg = new Message();
        msg.what = MainActivity.OPER_SHOW_TOAST;
        msg.arg1 = type;
        msg.getData().putString("tst_msg", message);
        mainHandler.sendMessage(msg);
    }

    public void showToastX(String message, int type) {
        Message msg = new Message();
        msg.what = MainActivity.OPER_SHOW_TOAST_X;
        msg.arg1 = type;
        msg.getData().putString("tst_msg", message);
        mainHandler.sendMessage(msg);
    }

    public void startBusyAnimation(String message) {
        updateStatusText(message);
        setTerminalBusyFlag(true);
    }

    //initialize the application data here
    public void initialize() {
        mainHandler = MainActivity.mainMessageHandler;

        //startBusyAnimation("Preparing init DB...");
        DBHelperSync dbHelperSync = DBHelperSync.getInstance(appContext);
        try {
            dbHelperSync.prepareInitialDatabase();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //stopBusyAnimation();

        IssuerHostMap.init();

        Thread initThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //initialization is finished
                Preferences preferences = Preferences.getInstance(appContext);

                //startBusyAnimation("Initializing...");
                ConnectToService();

                configDatabase = new DBHelper(appContext);
                transactionDatabase = new DBHelperTransaction(appContext);

                //stopBusyAnimation();
                playSound(R.raw.welcome);

                TranStaticData tranData = new TranStaticData();
                tranData.initTranData();

                ErrorInfoDev errorInfoDev = ErrorInfoDev.getMyInstance();
                errorInfoDev.initErrorData();

                Receipt.setLogoImageName("sampath_logo.png");

                //initialize the cup bin
                BinLoader binLoader = BinLoader.getInstance(appContext);
                /*binLoader.setOnStatusUpdate(new BinLoader.onStatusUpdate() {
                    @Override
                    public void OnStatusUpdate(String status) {
                        updateStatusText(status);
                    }
                });*/

                Preferences pref = Preferences.getInstance(appContext);
                String CUP_BIN_LOAD_STNGS_TAG = "CUP_BIN_LOAD";

                if (null == pref.getSetting(CUP_BIN_LOAD_STNGS_TAG) || pref.getSetting(CUP_BIN_LOAD_STNGS_TAG).equals("0")) //this is an initial loading
                {
                    if (!binLoader.loadBinInToDB(ISSUER_CUP)) {
                        pref.saveSetting(CUP_BIN_LOAD_STNGS_TAG, "0");
                    } else {
                        //set as loading successful
                        pref.saveSetting(CUP_BIN_LOAD_STNGS_TAG, "1");
                    }
                }


                scheduler = Scheduler.getInstance(appContext, 10); //scheduler fire for each 10 seconds

                //all the automated scheduled tasks should be included in the method
                scheduleAutomatedTasks();

                //secondsForCardThreadsleep = SettingsInterpreter.getCardReaderIdleTimeOut();

                CardReadingThread cardReadingThread = new CardReadingThread();
                cardReadingThread.start();

                if (listener != null)
                    listener.onInitFinished();

                SettingsInterpreter.setOnDevModeChangedListener(new SettingsInterpreter.onSettingsChange() {
                    @Override
                    public void OnSettingsChange(String key, boolean state) {
                        if (key.equals("enable_dev_mode"))
                            GlobalData.inDebugMode = state;
                        else if (key.equals("enable_pp_module")) {
                            ConfigSynchronize configSynchronizer = null;
                            configSynchronizer = ConfigSynchronize.getInstance(appContext, mActivity);
                            if (state) {
                                try {
                                    configSynchronizer.init();

                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            } else {
                                configSynchronizer.stopProcess();
                            }

                        } else if (key.equals("performance_monitor")) {
                            if (state)
                                scheduler.startRunning();
                            else
                                scheduler.stopRunning();

                        }
                    }
                });

                //GlobalData.inDebugMode = SettingsInterpreter.isDevModeOn();
                preferences.setInitialLoadingDone();
            }
        });

        initThread.start();

        reversalHandler = new ReversalHandler();

        ConfigSynchronize configSynchronize = ConfigSynchronize.getInstance(appContext, mActivity);
        configSynchronize.setOnGetTerminalBusyFlag(new ConfigSynchronize.onGetTerminalBusyFlag() {
            @Override
            public boolean getTerminalBusyStatus() {
                return isInTransaction();
            }
        });


        //function registration to push the data to the back end
        configSynchronize.setonGetBackEndData(new ConfigSynchronize.onGetBackEndData() {
            @Override
            public List<JSONObject> getData(ConfigSynchronize.TYPE_DATA_BE type) {
                //push all the transaction to the back end
                if (type == ConfigSynchronize.TYPE_DATA_BE.TYPE_TRANSACTION) {
                    //get the transaction list from the db of the application
                    return getTransactions();
                }
                return null;
            }
        });


        if (SettingsInterpreter.isPushPullModuleEnabled()) {
            try {
                configSynchronize.init();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    public void ConnectToService() {
        BankCard bankCard = new BankCard(appContext);
        try {
            bankCard.breakOffCommand();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //startBusyAnimation("Setting Core");

        //initialize basic services here ..
        Services.keys = new Key(appContext);
        Services.emvCore = new EmvCore(appContext);
        Services.core = new Core(appContext);
        Services.printer = new Printer(appContext);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date(System.currentTimeMillis());
        String str = simpleDateFormat.format(date);// 1971 < year < 2099

        try {
            Services.core.setDateTime(str.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //importing the keys in to secure area of the terminal
        //startBusyAnimation("Initializing keys..");
        Keys.init();
        //updateStatusText("Setting Keys...");
        Keys.setRequiredKeys();

        try {
            //updateStatusText("Setting CAPKs");
            setCAPK();

            //updateStatusText("Setting AIDs");

            setAID();
            //updateStatusText("Setting EMV Parameters");
            setEmvParam();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void stopBusyAnimation() {
        setTerminalBusyFlag(false);
        updateStatusText("Terminal Ready");
    }

    private boolean isDCCRequest() {
        return currentTransaction.inTransactionCode == TranStaticData.TranTypes.DCC_REQUEST;
    }


    protected boolean isReversal ;

    public void sleepMe(int milliSeconds) {
        try {
            Thread.sleep(milliSeconds);
        } catch (Exception ex) {
        }
    }

    private boolean isKeyExchangeTran() {
        return currentTransaction.inTransactionCode == TranStaticData.TranTypes.KEYEXCHANGE;
    }

    //method to increment the ksn accordingly for hardware dukpt encryption
    private void doKSNIncrementForBatchTrans()
    {
        String ksn = "";

        if (!SettingsInterpreter.isTLEEnabled())
            return;

        if (currentTransaction.origTransactionCode == TranStaticData.TranTypes.VOID ||
                currentTransaction.origTransactionCode == TranStaticData.TranTypes.BATCH_UPLOAD ||
                currentTransaction.inTransactionCode == TranStaticData.TranTypes.SETTLE ||
                currentTransaction.inTransactionCode == TranStaticData.TranTypes.CLOSE_BATCH_UPLOAD)
            TLE.updateKSN();
    }

    //check whether the particular merchant have pending settlement
    protected boolean isPendingSettle(int merchNumber) {
        String pendingSettleQuary = "SELECT * FROM MIT WHERE MerchantNumber = " + merchNumber;
        boolean isPending = false;
        try {
            Cursor pendingSettle = configDatabase.readWithCustomQuary(pendingSettleQuary);
            if (pendingSettle != null) {
                pendingSettle.moveToFirst();
                isPending = pendingSettle.getInt(pendingSettle.getColumnIndex("MustSettleFlag")) == 1;
                pendingSettle.close();
            }
        } catch (Exception ex) {
        }

        return isPending;
    }

    protected boolean isPendingBatchClear(int merchNumber) {
        String pendingSettleQuary = "SELECT * FROM MIT WHERE MerchantNumber = " + merchNumber;
        boolean isPending = false;
        try {
            Cursor pendingSettle = configDatabase.readWithCustomQuary(pendingSettleQuary);
            if (pendingSettle != null) {
                pendingSettle.moveToFirst();
                isPending = pendingSettle.getInt(pendingSettle.getColumnIndex("ClearBatchFlag")) == 1;
                pendingSettle.close();
            }
        } catch (Exception ex) {
        }

        return isPending;
    }

    public void selectMerchant() {
        currentTransaction.merchantNumber = GlobalData.selectedMerchant;
        GlobalData.selectedIssuer = 0;
        GlobalData.selectedMerchant = 0;
    }

    public final static int TERMINATE_TRANSACTION = 4;
    public final static int REVERSAL_FAILED = 5;
    public final static int REVERSAL_SUCCESS = 6;
    public final static int COMM_FALIURE = 3;

    //This routine is responsible for communication send and receive of the communication
    //this is a generic function which is used to perform the transaction
    public byte[] sendAndRecieve(CommEngine engine, byte[] rawPacket) {
        Socket socket = engine.getSocket();
        byte[] recieved = null;
        Log.d("isReversal FFF", "03 : " + isReversal);
        isReversal = false;
        if (socket != null && !socket.isConnected())
            socket = null;

        if (socket != null && socket.isClosed())
            socket = null;

        updateStatusText("Connecting..");

        if (socket == null) {
            if (!engine.connect())
                return null;

            socket = engine.getSocket();
        }

        if (socket.isConnected()) {
            doKSNIncrementForBatchTrans();

            //in here we need to print the clear packet if requested in the settings
            if (SettingsInterpreter.isIsoPrintEnabled()) {
                Emv.writeISOLogOnDiskFile(rawPacket, Emv.MODE.SEND);
            }
            String clearPacket = Utility.byte2HexStr(rawPacket).substring(4);
            //here we set the encrypted packet if the tle is enabled here
            if (SettingsInterpreter.isTLEEnabled() && (!isKeyExchangeTran()) && (!isDCCRequest())) {
                TLE tle = TLE.getInstance();
                rawPacket = tle.encryptPacket(rawPacket);
                if (rawPacket == null) {
                    sleepMe(3000);
                    return null;
                }

                String encHexPacket = Utility.byte2HexStr(rawPacket).substring(4);
            }

            engine.setSendData(rawPacket);
            startBusyAnimation("Sending...");
            if ( 0 == engine.sendDataBuffer())
               return null;

            incAndSaveTraceNumber(currentTransaction.merchantNumber,currentTransaction.traceNumber);
            startBusyAnimation("Receiving...");
            recieved = engine.recieveDataBuffer();
            stopBusyAnimation();
            Log.d("******* recieved", " : " + recieved);

            if (recieved == null || recieved.length == 0) {
                if (GlobalData.isReversalEnabled) {
                    Log.d("isReversal FFF", "04 : " + isReversal);
                    isReversal = true;
                }

                showToast("Receiving Failed, Response Timed Out",TOAST_TYPE_FAILED);
                Emv.writeISOLogOnDiskFile(null, Emv.MODE.RECIEVE);
                return null;
            }

            String clearPacketDec = "";

            //if tle is enabled first we decrypt the packet before passing
            if (SettingsInterpreter.isTLEEnabled() && (!isKeyExchangeTran()) && (!isDCCRequest())) {
                Log.d("PRINT", "01");
                if (SettingsInterpreter.isIsoPrintEnabledTLE()) {
                    Log.d("PRINT", "02");
                    engine.startPrint(recieved, CommEngine.commTypePrint.RECIEVE);
                }

                TLE tle = TLE.getInstance();
                recieved = tle.decryptPacket(recieved);
                clearPacketDec = Utility.byte2HexStr(recieved);
            }

            if (recieved != null) {
                if (SettingsInterpreter.isIsoPrintEnabled())
                    Emv.writeISOLogOnDiskFile(recieved, Emv.MODE.RECIEVE);
            }
            else
                Emv.writeISOLogOnDiskFile(null, Emv.MODE.RECIEVE);

            return recieved;
        }
        return null;
    }

    public void setCardThreadStop(boolean state) {
        shouldCardThreadStop = state;
    }

    protected int getEMVTransInfo() throws RemoteException {
        Log.v("getEMVTransInfo", "get emv transaction information");
        byte[] outData = new byte[100];
        int[] outDataLen = new int[1];


        //get the card number here
        currentTransaction.PAN = Utility.byte2HexStr(Arrays.copyOf(outData, outDataLen[0]));

        Services.emvCore.getTLV(0x5F34, outData, outDataLen);
        currentTransaction.cardSerialNo = String.valueOf(outData[0]);

        int path = Services.emvCore.getPath();
        if (Services.emvCore.getPath() == PAYPASS_MSD) {
            //getting the contact less mag track2 info
            int rest = Services.emvCore.getMagTrackData_MC(0x2, outData, outDataLen);

            String tagTLV57 = null;
            if (rest == 0) {
                tagTLV57 = BytesUtil.fromBytes(Arrays.copyOf(outData, outDataLen[0]));
                if (currentTransaction.PAN.isEmpty()) {
                    currentTransaction.PAN = tagTLV57.split("D")[0];
                    int track2Len = tagTLV57.indexOf("F");

                    if (track2Len > 0)
                        currentTransaction.track2 = tagTLV57.substring(0, track2Len);
                    else
                        currentTransaction.track2 = tagTLV57;

                }
            }

            //get the track1 data
            rest = Services.emvCore.getMagTrackData_MC(0x01, outData, outDataLen);

            if (rest == 0) {
                tagTLV57 = BytesUtil.fromBytes(Arrays.copyOf(outData, outDataLen[0]));

                int track1Len = tagTLV57.indexOf("F");
                if (track1Len > 0)
                    currentTransaction.track1 = tagTLV57.substring(0, track1Len);
                else
                    currentTransaction.track1 = tagTLV57;

            }
        } else {
            String tagTLV57 = "";
            int result = Services.emvCore.getTLV(0x57, outData, outDataLen);

            if (result == 0) {
                tagTLV57 = BytesUtil.bytes2HexString(Arrays.copyOf(outData, outDataLen[0]));
                Log.d("track information", tagTLV57 + "\n" + BytesUtil.bytes2HexString(Arrays.copyOf(outData, outDataLen[0])));

                if (currentTransaction.PAN == null || currentTransaction.PAN == "")
                    currentTransaction.PAN = tagTLV57.split("D")[0];

                int track2Len = tagTLV57.indexOf("generatePDFReceipt");
                if (track2Len >= 0)
                    currentTransaction.track2 = tagTLV57.substring(0, track2Len);
                else
                    currentTransaction.track2 = tagTLV57;
            }


            Services.emvCore.getTLV(0x5F24, outData, outDataLen);
            if (outDataLen[0] != 0)
                currentTransaction.expDate = BytesUtil.bytes2HexString(Arrays.copyOf(outData, outDataLen[0])).substring(0, 4);
            else {
                if (!tagTLV57.isEmpty()) {
                    String expDate = tagTLV57.split("D")[1]; //get the data after D
                    expDate = expDate.substring(0, 4);
                    currentTransaction.expDate = expDate;
                }

            }
            Log.d("getEMVTransInfo", "result :" + result);
            if (result != 0)
                return -1;
        }

        Log.d("getEMVTransInfo", "return 0");
        return 0;
    }

    private String prepareScript(String script) {
        String strInternalLength = Integer.toHexString(script.length() / 2);  //byte length from hex string
        if (strInternalLength.length() == 1)
            strInternalLength = "0" + strInternalLength;

        script = strInternalLength + script;
        return script;
    }

    public void setEMVDataAfterOnline(byte[] recievedPacket, String responseCode, byte[] resultBytes, int[] ints) {

        if (/*currentTransaction.inChipStatus == Transaction.ChipStatusTypes.CTLS_CARD ||*/
                currentTransaction.inChipStatus == Transaction.ChipStatusTypes.NOT_USING_CHIP ||
                        currentTransaction.inChipStatus == Transaction.ChipStatusTypes.MANUAL_KEY_IN
        )
            return;

        int bufferPos = 1;

        if (responseCode == null)
            responseCode = "Z3";

        //copy the response code
        byte[] respBytes = responseCode.getBytes();
        System.arraycopy(respBytes,0,resultBytes,bufferPos,respBytes.length);
        bufferPos += respBytes.length;


        String apprCode = "";

        //set the appr code
        if (recievedPacket != null)
            apprCode = getUnpackPacketAndHostApprovalCode(recievedPacket);
        else
            apprCode = null;

        if (apprCode == null)
            resultBytes[++bufferPos] = 0x00;
        else
        {
            //set the approval code length
            respBytes = BytesUtil.hexString2Bytes(apprCode);
            resultBytes[bufferPos++] = (byte) respBytes.length;
            System.arraycopy(respBytes,0,resultBytes,bufferPos,respBytes.length);
            bufferPos += respBytes.length;
        }

        if (recievedPacket == null)
            currentTransaction.emv55Data = null;

        if (currentTransaction.emv55Data == null) //no 55 data was sent from the host
        {
            resultBytes[bufferPos++] = 0x00;
            ints[0] = bufferPos + 1;
            return ;
        }

        //copy the emv elate things
        String strEmvData = "";
        if (currentTransaction.emv55Data != null)
            strEmvData  = currentTransaction.emv55Data;

        Log.d("LLLLLLLL", "QQQ");
        String tag91 = Emv.extractDataForTag(strEmvData,0x91);
        String tag71 = Emv.extractDataForTag(strEmvData,0x71);
        String tag72 = Emv.extractDataForTag(strEmvData,0x72);


        if (tag91 == null)
            resultBytes[bufferPos++] = 0x00;
        else
        {
            byte [] data =  Utility.hexStr2Byte(tag91);
            resultBytes[bufferPos++] =  (byte) data.length;
            System.arraycopy(data, 0, resultBytes, bufferPos, data.length);
            bufferPos += data.length;
        }


        byte scriptLen = 0;
        byte[] dataTag71 = null;
        byte[] dataTag72 = null;

        if (tag71 == null)
           scriptLen = 0x00;
        else
        {
            tag71 = prepareScript(tag71);
            tag71 = "71" + tag71;
            dataTag71 =  Utility.hexStr2Byte(tag71);
            scriptLen = (byte) dataTag71.length;
        }


        if (tag72 == null)
            scriptLen += 0x00;
        else
        {
            tag72 = prepareScript(tag72);
            tag72 = "72" + tag72;
            dataTag72 =  Utility.hexStr2Byte(tag72);
            scriptLen = (byte) dataTag72.length;
        }


        //assign the total script len
        resultBytes[bufferPos++] = scriptLen;

        if (scriptLen > 0)
        {
            if (tag71 != null)
            {
                System.arraycopy(dataTag71, 0, resultBytes, bufferPos, dataTag71.length);
                bufferPos += dataTag71.length;
            }
            if (tag72 != null) {
                System.arraycopy(dataTag72, 0, resultBytes, bufferPos, dataTag72.length);
                bufferPos += dataTag72.length;
            }
        }

        ints[0] = bufferPos + 1;
    }

    public int sendTransactionOnline(byte[] resultBytes, int[] ints) {
        byte[] recievedPacket;
        if (currentTransaction.inTransactionCode == TranStaticData.TranTypes.OFFLINE_SALE || currentTransaction.inTransactionCode == TranStaticData.TranTypes.PRE_COMP)
            return 0;

        Log.d("TRANS", "02");
        //check the pending settlements here
        if (isPendingSettle(currentTransaction.merchantNumber)) {
            showToast("Please settle the merchant batch before proceeding",TOAST_TYPE_FAILED);
            sleepMe(2000);
            return TERMINATE_TRANSACTION;
        }

        if (isPendingBatchClear(currentTransaction.merchantNumber)) {
            showToast("Please Clear the batch before proceeding",TOAST_TYPE_FAILED);
            sleepMe(2000);
            return TERMINATE_TRANSACTION;
        }

        Log.d("TRANS", "03");
        if (!loadTerminal())
            return TERMINATE_TRANSACTION;

        Log.d("TRANS", "04");
        incInvoiceNumber();

        currentTransaction.Date = Formatter.getCurrentDateFormatted();
        currentTransaction.receiptDate = Utility.getCurrentDateReceipt();
        currentTransaction.Time = Formatter.getCurrentTimeFormatted();

        CommEngine comm = CommEngine.getInstance(currentTransaction.issuerData.IP,currentTransaction.issuerData.port);
        comm.setRectimeout(SettingsInterpreter.getConnectTimeout());

        if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.EMV_CARD)
            startBusyAnimation("EMV Online Processing..");
        else if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.NOT_USING_CHIP)
            startBusyAnimation("Mag Online Processing..");

        setFieldsAndLoadPacket();
        Log.d("***** secureNII", " : " + currentTransaction.secureNII);
        Log.d("***** NII", " : " + currentTransaction.NII);
        if (SettingsInterpreter.isTLEEnabled())
            currentTransaction.TPDU = Formatter.replaceNII(currentTransaction.TPDU, currentTransaction.secureNII);
        else
            currentTransaction.TPDU = Formatter.replaceNII(currentTransaction.TPDU, currentTransaction.NII);

        packet.setPacketTPDU(currentTransaction.TPDU);
        byte[] rawPacket = packet.getRawDataPacket();

        recievedPacket = sendAndRecieve(comm, rawPacket);
        printISOLogs();
        Log.d("PRINT AAAA", "AAA");
        //A reversal must be generated for this transaction
        if (SettingsInterpreter.isReversalEnabled && isReversal) {
            //save the current transaction in the reversal table
            if (false == transactionDatabase.writeReversal(currentTransaction)) {
                showToast("Reversal Committing failed, Aborting Transaction", TOAST_TYPE_FAILED);
                comm.disconnect();
            }

            setEMVDataAfterOnline(null, null, resultBytes, ints);
            //comm.disconnect();
            GlobalData.globalResult = PUSH_REVERSAL;
            return 3;
        }

        Log.d("KKKKKKKK recievedPacket", " : " + recievedPacket);

        if (recievedPacket == null) {
            showToast("Communication Failure",TOAST_TYPE_FAILED);

            setEMVDataAfterOnline(null,null,resultBytes,ints);
            GlobalData.globalResult = COMM_FALIURE;
            return COMM_FALIURE;
        }

        ISO8583u response =  new ISO8583u();

        if (response.unpack(recievedPacket, 0)) {
            String responseCode = getHostResponse(response);
            Log.d("KKKK responseCode ", " : " + responseCode);
            //set the response code
            currentTransaction.responseCode = responseCode;

            //push the result after the ecr transaction is finished
            if (SettingsInterpreter.isECREnabled() && MainActivity.ecr.isECRInitiated) {
                if (currentTransaction.inChipStatus == Transaction.ChipStatusTypes.NOT_USING_CHIP)
                    MainActivity.ecr.pushTransactionDetails(SUCCESS, currentTransaction, responseCode);
                else if (!responseCode.equals("00"))
                    MainActivity.ecr.pushTransactionDetails(SUCCESS, currentTransaction, responseCode);
            }

            unpackFieldsAndStoreInTransaction(response);
            setEMVDataAfterOnline(recievedPacket, responseCode, resultBytes, ints);

            //by pass the host code 96 for temporary
            if (responseCode.equals("00"))
                comm.disconnect();
            else {
                try {
                    showToast(ErrData.getErrorDesc(Integer.valueOf(responseCode)),TOAST_TYPE_INFO);
                } catch (Exception ex) {
                    //the error code may come in hex format so we try with converted hex to dec
                    try {
                        int errCode = Integer.valueOf(responseCode,16);
                        String errorDesc = ErrData.getErrorDesc(errCode);

                        if (errorDesc != null)
                            showToast(ErrData.getErrorDesc(errCode),TOAST_TYPE_INFO);
                        else
                            showToast("Error Code is " + responseCode,TOAST_TYPE_INFO);

                    } catch (Exception exx) {
                        exx.printStackTrace();
                        showToast("Error code is " + responseCode,TOAST_TYPE_INFO);
                    }
                }

                comm.disconnect();

                GlobalData.globalResult = TERMINATE_TRANSACTION;
                return TERMINATE_TRANSACTION;
            }
        }
        else {
            showToast("Packet Error",TOAST_TYPE_FAILED);
            comm.disconnect();
            return TERMINATE_TRANSACTION;
        }

        return 0;
    }

    protected  boolean loadCardAndIssuer() {
        Cursor cardRec = null;
        Cursor issuerRec = null;

        //find the cardData record in the data base and the matching bin
        Log.d("PAN------->", " : " + currentTransaction.PAN);
        if ((cardRec = configDatabase.findCardRecord(currentTransaction.PAN)) == null) {
            showToastX("Card record not found",TOAST_TYPE_FAILED);
            return false;
        }

        currentTransaction.cdtIndex = cardRec.getInt(cardRec.getColumnIndex("ID"));
        currentTransaction.cardData = new Card(cardRec);
        Log.d("VVVVVVVV CAR 02", " : " + currentTransaction.cardData.cardLabel);

        if((currentTransaction.inTransactionCode == TranStaticData.TranTypes.PRE_AUTH) && (currentTransaction.cardData.cardLabel.equals("CUP"))) {
            cardRec.close();
            showToastX("Pre Auth blocked for CUP", TOAST_TYPE_FAILED);
            return false;
        }

        if(currentTransaction.inTransactionCode != TranStaticData.TranTypes.REVERSAL) {
            if (currentTransaction.cardData.expDataRequired) {
                if (!validateExpDate(currentTransaction.expDate)) {
                    cardRec.close();
                    showToastX("Expired Card", TOAST_TYPE_FAILED);
                    return false;
                }
            }
        }

        /*if ((currentTransaction.inChipStatus == Transaction.ChipStatusTypes.MANUAL_KEY_IN) && (!currentTransaction.cardData.manualEntry)) {
            cardRec.close();
            showToastX("Manual Key In Disabled for Card", TOAST_TYPE_FAILED);
            return false;
        } */

        currentTransaction.issuerNumber  = currentTransaction.cardData.issuerNumber;
        //load the issuerData record
        Log.d("Invi_Issuer", "issuerNumber2 :" + currentTransaction.issuerNumber);
        if ((issuerRec = configDatabase.loadIssuer(currentTransaction.issuerNumber)) == null) {
            cardRec.close();
            showToastX("Error with issuerData loading",TOAST_TYPE_FAILED);
            return false;
        }

        currentTransaction.issuerData = new Issuer(issuerRec);
        currentTransaction.actualIssuerNumber = currentTransaction.issuerNumber;
        currentTransaction.issuerNumber = IssuerHostMap.getBaseIssuer(currentTransaction.cardData.issuerNumber);

        GlobalData.selectedIssuer = currentTransaction.issuerNumber;
        cardRec.close();
        issuerRec.close();
        return true;
    }

    protected  boolean loadCardAndIssuerSettlement() {
        Cursor cardRec = null;
        Cursor issuerRec = null;

        //find the cardData record in the data base and the matching bin
        Log.d("PAN------->", " : " + currentTransaction.PAN);
        if ((cardRec = configDatabase.findCardRecord(currentTransaction.PAN)) == null) {
            showToastX("Card record not found",TOAST_TYPE_FAILED);
            return false;
        }

        currentTransaction.cdtIndex = cardRec.getInt(cardRec.getColumnIndex("ID"));
        currentTransaction.cardData = new Card(cardRec);
        Log.d("VVVVVVVV CAR 02", " : " + currentTransaction.cardData.cardLabel);

        currentTransaction.issuerNumber  = currentTransaction.cardData.issuerNumber;
        //load the issuerData record
        Log.d("Invi_Issuer", "issuerNumber3 :" + currentTransaction.issuerNumber);
        if ((issuerRec = configDatabase.loadIssuer(currentTransaction.issuerNumber)) == null) {
            cardRec.close();
            showToastX("Error with issuerData loading",TOAST_TYPE_FAILED);
            return false;
        }

        currentTransaction.issuerData = new Issuer(issuerRec);
        currentTransaction.actualIssuerNumber = currentTransaction.issuerNumber;
        currentTransaction.issuerNumber = IssuerHostMap.getBaseIssuer(currentTransaction.cardData.issuerNumber);

        GlobalData.selectedIssuer = currentTransaction.issuerNumber;
        cardRec.close();
        issuerRec.close();
        return true;
    }

    private boolean validateExpDate(String expDate){
        String date = expDate.substring(2, 4) + "/" + expDate.substring(0, 2);

        try {
            Date expdate = new SimpleDateFormat("MM/yy").parse(date);
            Calendar c = Calendar.getInstance();
            c.setTime(expdate);
            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
            expdate = c.getTime();
            if (expdate.after(new Date())) {
                return true;
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    public int pushForceReversal(int merchant) {
        byte[] recievedPacket =  null;

        Log.d("IIIIII Merchant", " : " + merchant);

        Transaction revTran = transactionDatabase.getReveralTransaction(merchant);

        TData tranData  = TranStaticData.getTran(TranStaticData.TranTypes.REVERSAL);
        revTran.MTI = tranData.MTI;
        revTran.procCode = tranData.ProcCode;
        revTran.bitmap = tranData.Bitmap;
        revTran.inTransactionCode = TranStaticData.TranTypes.REVERSAL;

        currentTransaction = new Transaction();
        currentTransaction = revTran;

        int origInvNumber = revTran.inInvoiceNumber;

        //load the terminal and issuer information
        loadCardAndIssuer();
        loadTerminal();

        currentTransaction.inInvoiceNumber = origInvNumber;
        currentTransaction.traceNumber = revTran.traceNumber;

        CommEngine comm = CommEngine.getInstance(currentTransaction.issuerData.IP,currentTransaction.issuerData.port);
        comm.setRectimeout(SettingsInterpreter.getReversalTimeout());

        TLE.updateKSN();

        setFieldsAndLoadPacket();
        packet.setPacketTPDU(currentTransaction.TPDU);
        byte[] rawPacket = packet.getRawDataPacket();
        recievedPacket = sendAndRecieve(comm, rawPacket);
        Log.d("isReversal FFF", "02 : " + isReversal);

        printISOLogs();

        if (isReversal) {
            //no response to the reversal, simply abort both transaction
            isReversal = false;
            showToastX("No response for the reversal",TOAST_TYPE_INFO);
            comm.disconnect();
            return VoidResults.VOID_FAILED;
        }
        else if (!isReversal && recievedPacket != null) {
            //we have a reversal response
            String revRespCode = getUnpackPacketAndHostResponse(recievedPacket);
            if (revRespCode.equals("00")) {
                try {
                    Receipt rcpt = Receipt.getInstance();
                    rcpt.printReceiptRev();
                } catch (Exception e) {
                    e.printStackTrace();
                    return VoidResults.VOID_FAILED;
                }

                //we need to clear the reversal entry we just entered
                if (!transactionDatabase.removeReversal(currentTransaction.merchantNumber)) {
                    showToast("Removing reversal failed!,Critical",TOAST_TYPE_FAILED);
                    return VoidResults.VOID_FAILED;
                }

                configDatabase.saveInvoiceNumber(currentTransaction);
                comm.disconnect();
                return VoidResults.VOID_SUCCESS;
            }
            else {
                showToastX("Host Declined the Reversal",TOAST_TYPE_FAILED);
                comm.disconnect();
                return VoidResults.VOID_FAILED;
                //if the reversal is not approved we still abort processing transaction for this merchant
            }
        }
        else if (recievedPacket == null) {
            //communication failure
            showToastX("Reversal Failed - No response",TOAST_TYPE_INFO);
            comm.disconnect();
            return VoidResults.VOID_FAILED;
        }
        return VoidResults.VOID_FAILED;
    }

    public int performVoid(int transactionID) {
        byte[] receivedPacket;
        String voidTranQuary = "SELECT * FROM TXN WHERE ID = " + transactionID;

        Cursor tran = transactionDatabase.readWithCustomQuary(voidTranQuary);
        tran.moveToFirst();
        Transaction voidTran = transactionDatabase.getTransaction(tran);
        tran.close();

        if (voidTran == null)
            return VoidResults.VOID_LOAD_FAILED;

        TData curTranData = TranStaticData.getTran(TranStaticData.TranTypes.VOID);
        voidTran.MTI = curTranData.MTI;
        voidTran.bitmap = curTranData.Bitmap;
        voidTran.origTransactionCode = TranStaticData.TranTypes.VOID;
        voidTran.procCode = curTranData.ProcCode;

        currentTransaction = voidTran;

        if (!loadCardAndIssuerSettlement())
            return VoidResults.VOID_LOAD_FAILED;

        int originalInvoice = currentTransaction.inInvoiceNumber;

        if (!loadTerminal())
            return VoidResults.VOID_LOAD_FAILED;

        try {
            bankCard.breakOffCommand();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if((voidTran.inTransactionCode == TranStaticData.TranTypes.PRE_COMP)){
            currentTransaction.inInvoiceNumber = originalInvoice;
            setFieldsAndLoadPacket();
            packet.setPacketTPDU(currentTransaction.TPDU);

            showToast("Void Approved", TOAST_TYPE_SUCCESS);

            //update the transaction as a void transaction
            String updateQuary = "UPDATE TXN SET Voided = 1 WHERE ID = " + transactionID;
            transactionDatabase.executeCustomQuary(updateQuary);

            try {
                Log.d("COPY HOME", "---01AAAA");
                Receipt voidReceipt = Receipt.getInstance();
                voidReceipt.printReceipt(1);

                Log.d("COPY HOME", "---01");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return VoidResults.VOID_SUCCESS;
        }
        else {
            //push if there is any reversal is pending
            if (REVERSAL_FAILED == reversalHandler.pushpPendingReversal()) {
                if(!SettingsInterpreter.isForceReversalEnabled())
                    showToast("Reversal Failed", TOAST_TYPE_FAILED);
                errorTone();
                return VoidResults.VOID_REVERSAL_FAILED;
            }

            currentTransaction.inInvoiceNumber = originalInvoice;

            CommEngine comm = CommEngine.getInstance(currentTransaction.issuerData.IP, currentTransaction.issuerData.port);
            comm.setRectimeout(10);
            comm.printCommIso();

            setFieldsAndLoadPacket();
            packet.setPacketTPDU(currentTransaction.TPDU);
            byte[] rawPacket = packet.getRawDataPacket();
            receivedPacket = sendAndRecieve(comm, rawPacket);
            Log.d("PRINT AAA", "BBBBBB");

            if (isReversal) {
                //no response for the void request so we need to send the reversal request
                isReversal = false;
                Log.d("isReversal FFF", "05 : " + isReversal);

                //write the reversal transaction for future use
                if (!transactionDatabase.writeReversal(currentTransaction)) {
                    showToast("Writing Reversal for Void Transaction Failed, Aborting!", TOAST_TYPE_FAILED);
                    errorTone();
                    comm.disconnect();
                    return VoidResults.VOID_LOAD_FAILED;
                }

                if (REVERSAL_FAILED == reversalHandler.pushpPendingReversal()) {
                    if(!SettingsInterpreter.isForceReversalEnabled())
                        showToast("Void Reversal Failed", TOAST_TYPE_FAILED);
                    errorTone();
                    return VoidResults.VOID_REVERSAL_FAILED;
                }
            }

            comm.disconnect();

            if (receivedPacket == null) {
                showToast("Communication Failed", TOAST_TYPE_FAILED);
                errorTone();
                return VoidResults.VOID_COMM_FALIURE;
            } else {
                printISOLogs();
                ISO8583u resp = new ISO8583u();
                if (resp.unpack(receivedPacket, 0)) {
                    String response = getHostResponse(resp);
                    if (response.equals("00")) {
                        showToast("Void Approved", TOAST_TYPE_SUCCESS);

                        //update the transaction as a void transaction
                        String updateQuary = "UPDATE TXN SET Voided = 1 WHERE ID = " + transactionID;
                        transactionDatabase.executeCustomQuary(updateQuary);
                        try {
                            Log.d("COPY HOME", "---01AAAA");
                            Receipt voidReceipt = Receipt.getInstance();
                            voidReceipt.printReceipt(1);

                            Log.d("COPY HOME", "---01");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        return VoidResults.VOID_SUCCESS;
                    } else {
                        int err = -1;
                        String strResp = "";

                        try {
                            err = Integer.valueOf(response);
                            strResp = ErrorInfoDev.getErrorDesc(err);
                        } catch (Exception ex) {
                            err = Integer.valueOf(response, 16);
                            strResp = ErrorInfoDev.getErrorDesc(err);
                            ex.printStackTrace();
                        }

                        showToast(strResp, TOAST_TYPE_INFO);
                    }
                }
            }
        }
        return VoidResults.VOID_FAILED;
    }

    public boolean checkForQRTran() {
        String query = "SELECT * FROM QR";
        Cursor qr = transactionDatabase.readWithCustomQuary(query);
        if (qr.getCount() == 0)
            return false;

        return true;
    }

    protected void incInvoiceNumber() {
        currentTransaction.inInvoiceNumber++;            //increment the invoice number

        if (currentTransaction.inInvoiceNumber >= 999999)
            currentTransaction.inInvoiceNumber = 0;
    }

    protected int getTraceNumber(int merchNumber )
    {
        String getTrace = "SELECT * FROM MIT WHERE MerchantNumber = " + merchNumber;

        Cursor c = configDatabase.readWithCustomQuary(getTrace);
        if ( c == null || c.getCount() == 0)
            return -1;

        c.moveToFirst();

        int traceNumber = c.getInt(c.getColumnIndex("STAN"));
        c.close();
        return traceNumber;
    }

    private void initTransaction() {
        //search within the registered transactions
        int tran = GlobalData.transactionCode;

        if (tran < 0) tran  = TranStaticData.TranTypes.SALE;        //make the default transaction sale
        TData tData = TranStaticData.getTran(tran);

        currentTransaction.MTI = tData.MTI;
        currentTransaction.procCode = tData.ProcCode;
        currentTransaction.bitmap = tData.Bitmap;
        currentTransaction.inTransactionCode = tran;
        GlobalData.transactionName = tData.tranName;

        GlobalData.transactionCode = -1;
    }

    protected PacketDev packet;

    public void setFieldsAndLoadPacket() {
        //all the fields are set here , at this point the callbacks are called so that the
        //developer is able to customize each and every field loading
        BitmapDev bitmap = new BitmapDev(currentTransaction.bitmap);
        bitmap.setOnSetOverrideFieldSettingsListener(bitmap);
        bitmap.OverrideFieldSettings(currentTransaction);     //in this the call back is called so that developer can set

        packet =  new PacketDev(currentTransaction);
        packet.setOnPacketLoadListener(packet);

        //get the trace number here
        if(currentTransaction.inTransactionCode != TranStaticData.TranTypes.REVERSAL) {
            int trace = getTraceNumber(currentTransaction.merchantNumber);
            currentTransaction.traceNumber = trace;
        }

        try
        {
            packet.loadPacketData(bitmap.getFieldList());
        }
        catch (DataPacket.InvalidData ex)
        {
            //log the error
            Log.d("data",ex.getMessage());
            return;
        }
    }

    protected boolean incAndSaveTraceNumber(int merchant,int traceNumber)
    {
        traceNumber++;
        if (traceNumber > 999999)
            traceNumber  = 0 ;

        String updateTrace = "UPDATE MIT SET STAN = " + traceNumber + " WHERE MerchantNumber = " + merchant;

        return false != configDatabase.executeCustomQuary(updateTrace);

    }

    protected String getHostResponse(ISO8583u resp)
    {
        String response = null;
        if (resp.unpackValidField[39])
            response = resp.getUnpack(39);

        return response;
    }


    //this routine is set to get and extract the response code from the packet
    public String getUnpackPacketAndHostResponse(byte[] packet)
    {
        String respCode = null;
        ISO8583u resp = new ISO8583u();
        if (resp.unpack(packet,0))
            respCode = getHostResponse(resp);

        return respCode;

    }

    public String getUnpackPacketAndHostApprovalCode(byte[] packet)
    {
        String approvalCode = null;
        ISO8583u resp = new ISO8583u();
        if (resp.unpack(packet,0))
        {
            if (resp.unpackValidField[38])
               approvalCode = resp.getUnpack(38);
        }

        return approvalCode;
    }

    protected void unpackFieldsAndStoreInTransaction(ISO8583u response)
    {
        if (response.unpackValidField[37])
            currentTransaction.RRN = response.getUnpack(37);

        if (response.unpackValidField[38])
            currentTransaction.approveCode = response.getUnpack(38);

        if (response.unpackValidField[39])
            currentTransaction.responseCode = response.getUnpack(39);

        if (response.unpackValidField[55])
            currentTransaction.emv55Data = response.getUnpack(55);
        else
            currentTransaction.emv55Data = null;

        if (response.unpackValidField[62])
            UnpackedPacket.Field62 = response.getUnpack(62);

    }

    public static class VoidResults {
        public static int VOID_REVERSED = 1;
        public static int VOID_FAILED = 2;
        public static int VOID_REVERSAL_FAILED = 3;
        public static int VOID_SUCCESS = 4;
        public static int VOID_COMM_FALIURE = 5;
        public static int VOID_LOAD_FAILED = 6;
    }

    private boolean pushPendingPreComps(CommEngine comm,int merchantNumber) {
        byte [] receivedPacket = null;

        //check whether there are pending pre comps which has to be pushed to the issuer host
        String selectPreComp = "SELECT * FROM TXN WHERE MerchantNumber = " + merchantNumber + " AND TransactionCode = " + TranStaticData.TranTypes.PRE_COMP;
        Cursor preCompRecs = null;

        Transaction settleBackup = new Transaction();

        try {
            preCompRecs = transactionDatabase.readWithCustomQuary(selectPreComp);
        } catch (Exception ex) {
            showToast("Retrieving pre comps failed",TOAST_TYPE_FAILED);
            callSettlementChangeListenerCallback("Aborting Settlement!");
            return  false;
        }

        if (preCompRecs != null && preCompRecs.getCount() == 0)
            return true;

        //this code pushes the pre comps to the back end
        if (preCompRecs != null && preCompRecs.getCount()  > 0) {
            //back up the settle tran

            settleBackup = currentTransaction;

            //load the pre comps
            while (preCompRecs.moveToNext())
            {
                Transaction preCompTransaction = transactionDatabase.getTransaction(preCompRecs);

                TData preCompTranSpec = TranStaticData.getTran(TranStaticData.TranTypes.PRE_COMP);
                preCompTransaction.MTI = preCompTranSpec.MTI;
                preCompTransaction.bitmap = preCompTranSpec.Bitmap;
                preCompTransaction.inTransactionCode = TranStaticData.TranTypes.PRE_COMP;
                preCompTransaction.procCode = preCompTranSpec.ProcCode;

                currentTransaction = preCompTransaction;
                loadCardAndIssuer();
                loadTerminal();
                //currentTransaction.issuerData = settleBackup.issuerData;

                setFieldsAndLoadPacket();
                if (SettingsInterpreter.isTLEEnabled())
                    currentTransaction.TPDU = Formatter.replaceNII(currentTransaction.TPDU,currentTransaction.secureNII);
                else
                    currentTransaction.TPDU = Formatter.replaceNII(currentTransaction.TPDU,currentTransaction.NII);

              /* currentTransaction.TPDU = settleBackup.TPDU;
               currentTransaction.NII = settleBackup.NII;
               currentTransaction.secureNII = settleBackup.secureNII;*/

                packet.setPacketTPDU(currentTransaction.TPDU);
                byte[] preCompPacket = packet.getRawDataPacket();
                String hexPacket = Utility.byte2HexStr(preCompPacket);
                receivedPacket = sendAndRecieve(comm, preCompPacket);
                Log.d("PRINT AAA", "02CCCCC");

                if (isReversal) {
                    showToast("No host Response",TOAST_TYPE_INFO);
                    return false;
                }
                else if (receivedPacket == null) {
                    showToast("Communication Failure",TOAST_TYPE_FAILED);
                    return false;
                }

                String hostResp = getUnpackPacketAndHostResponse(receivedPacket);

                if (hostResp != null)
                {
                    if (!hostResp.equals("00"))
                        return false;
                }
            }
        }

        currentTransaction = settleBackup;
        return true;
    }

    public boolean verifyTerminalAvailabilityForOperation() {
        if (isInTransaction() || isTerminalBusy()) {
            return true;
        }
        return false;
    }


    protected  boolean loadIssuer(int issuerNumber) {
        Cursor issuerRec;

        //load the issuerData record
        Log.d("Invi_Issuer", "issuerNumber1 :" + issuerNumber);
        if ((issuerRec = configDatabase.loadIssuer(issuerNumber)) == null) {
            showToast("Error with issuerData loading",TOAST_TYPE_FAILED);
            return false;
        }
        currentTransaction.issuerData = new Issuer(issuerRec);
        currentTransaction.issuerNumber = currentTransaction.issuerData.issuerNumber;
        issuerRec.close();

        return true;

    }

    final static int EMPTY_BATCH_RECORDS = 1;
    final static int BATCH_LOAD_ERROR = 2;
    final static int BATCH_UPLOAD_FAILED = 3;
    final static int BATCH_COMM_FALIURE =  4;

    public interface OnSettlementStateChange {
        void OnSettlementStateChanged(String status);
    }

    private OnSettlementStateChange settlementStateChangeListener = null;

    public void setOnSettlementStateChangeListener(OnSettlementStateChange li) {
        settlementStateChangeListener  = li;
    }

    private void callSettlementChangeListenerCallback(String status) {
        if (settlementStateChangeListener != null)
            settlementStateChangeListener.OnSettlementStateChanged(status);
    }

    public int checkSettleBatch(int hostSelected, int baseSelectedMerchant) {
        Cursor tranBatch = null;
        String selectQuery = "SELECT * FROM TXN WHERE MerchantNumber = " +  baseSelectedMerchant +
                " AND Voided = 0 AND TransactionCode != " + TranStaticData.TranTypes.PRE_AUTH;

        tranBatch = transactionDatabase.readWithCustomQuary(selectQuery);

        //upload each and every transaction one by one as a batch upload transaction
        if (tranBatch.getCount() == 0) {
            return EMPTY_BATCH_RECORDS;
        }
        else {
            return 0;
        }
    }

    public int performSettlement(int hostSelected, int baseSelectedMerchant) {
        Cursor tranBatch = null, offlineBatch = null;
        byte[] receivedPacket = null;
        int batchTranCount = 0 ;
        String hostResp;

        String selectQuery = "SELECT * FROM TXN WHERE MerchantNumber = " +  baseSelectedMerchant +
                " AND Voided = 0 AND TransactionCode != " + TranStaticData.TranTypes.PRE_AUTH;

        try {
            bankCard.breakOffCommand();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (isPendingBatchClear(baseSelectedMerchant)) {
            showToast("Please Clear the batch before proceeding",TOAST_TYPE_FAILED);
            sleepMe(2000);
            return TERMINATE_TRANSACTION;
        }

        tranBatch = transactionDatabase.readWithCustomQuary(selectQuery);

        callSettlementChangeListenerCallback("Checking Batch");
        //upload each and every transaction one by one as a batch upload transaction
        if (tranBatch.getCount() == 0) {
            callSettlementChangeListenerCallback("Batch Empty");
            return EMPTY_BATCH_RECORDS;
        }

        CommEngine comm;

        //check whether there is any pending reversals before proceeding
        int isReversalExist = 0 ;

        isReversalExist = transactionDatabase.checkReversal(baseSelectedMerchant);

        callSettlementChangeListenerCallback("Checking Pending Reversals");
        if (isReversalExist == 1) {
            currentTransaction = new Transaction();
            currentTransaction.issuerNumber = IssuerHostMap.hosts[hostSelected].baseIssuer;
            currentTransaction.merchantNumber = baseSelectedMerchant;

            callSettlementChangeListenerCallback("Pushing pending reversal");
            //yes there is a reversal we need to push it
            if (REVERSAL_FAILED == reversalHandler.pushpPendingReversal()) {
                if(!SettingsInterpreter.isForceReversalEnabled())
                    showToast("Reversal Failed", TOAST_TYPE_FAILED);
                callSettlementChangeListenerCallback("Aborting Settlement!");
                return  -1;
            }
        }

        TData curTranData = TranStaticData.getTran(TranStaticData.TranTypes.OFFLINE_SALE);
        byte[] rawPacket;
        comm = CommEngine.getInstance(currentTransaction.issuerData.IP,currentTransaction.issuerData.port);
        comm.disconnect();

        String offlineQuary = "SELECT * FROM TXN WHERE MerchantNumber = " +  baseSelectedMerchant +
                " AND Voided = 0 AND TransactionCode = " + TranStaticData.TranTypes.PRE_COMP;

        offlineBatch = transactionDatabase.readWithCustomQuary(offlineQuary);
        if(offlineBatch.getCount() != 0){
            setMustSettleFlagState(baseSelectedMerchant,true);

            while (offlineBatch.moveToNext())   {

                Transaction offlineTran = transactionDatabase.getTransaction(offlineBatch);
                if (offlineTran == null)  {
                    GlobalData.transactionCode = -1;
                    showToast("Error retriving transactions aborting!", TOAST_TYPE_FAILED);
                    return BATCH_LOAD_ERROR;
                }

                offlineTran.MTI = curTranData.MTI;
                offlineTran.bitmap = curTranData.Bitmap;
                offlineTran.procCode = curTranData.ProcCode;

                currentTransaction = offlineTran;

                if (!loadCardAndIssuerSettlement()) {
                    GlobalData.transactionCode = -1;
                    return BATCH_LOAD_ERROR;
                }

                //we must back up and restore the original invoice number instead of last invoice number
                int invoiceNumber = currentTransaction.inInvoiceNumber;

                if (!loadTerminal()) {
                    GlobalData.transactionCode = -1;
                    return BATCH_LOAD_ERROR;
                }

                currentTransaction.inInvoiceNumber = invoiceNumber;

                setFieldsAndLoadPacket();
                packet.setPacketTPDU(currentTransaction.TPDU);
                rawPacket = packet.getRawDataPacket();
                receivedPacket  = sendAndRecieve(comm,rawPacket);

                if(receivedPacket == null){
                    GlobalData.transactionCode = -1;
                    showToast("Communication Failure",TOAST_TYPE_FAILED);
                    comm.disconnect();
                    return BATCH_UPLOAD_FAILED;
                }

                hostResp = getUnpackPacketAndHostResponse(receivedPacket);
                if (hostResp != null) {
                    if (!hostResp.equals("00")) {
                        GlobalData.transactionCode = -1;
                        showToast("Offline Failed",TOAST_TYPE_FAILED);
                        comm.disconnect();
                        return BATCH_UPLOAD_FAILED;
                    }
                    else{
                        String refNo = getUnpackPacketAndRRN(receivedPacket);
                        String rrnUpdateTxn = "UPDATE TXN SET RRN = '" + refNo + "' WHERE TraceNumber = " + currentTransaction.traceNumber;
                        transactionDatabase.executeCustomQuary(rrnUpdateTxn);
                    }
                }
            }
        }

        offlineBatch.close();
        Transaction settleTran = new Transaction();

        curTranData = TranStaticData.getTran(TranStaticData.TranTypes.SETTLE);
        settleTran.MTI = curTranData.MTI;
        settleTran.bitmap = curTranData.Bitmap;
        settleTran.inTransactionCode = TranStaticData.TranTypes.SETTLE;
        settleTran.procCode = curTranData.ProcCode;

        currentTransaction = settleTran;
        currentTransaction.merchantNumber = baseSelectedMerchant;

        int issuerNumber = IssuerHostMap.hosts[hostSelected].baseIssuer;
        //set the priority for the base issuer for loading settlement transaction
        if (!loadIssuer(issuerNumber))
            return BATCH_LOAD_ERROR;

        comm = CommEngine.getInstance(currentTransaction.issuerData.IP,currentTransaction.issuerData.port);

        //we must back up and restore the original invoice number instead of last invoice number
        if (!loadTerminal())
            return BATCH_LOAD_ERROR;

        currentTransaction.userSelectedIssuer = hostSelected;
        currentTransaction.userSelectedMerchantNumber = baseSelectedMerchant;

        if (SettingsInterpreter.isTLEEnabled())
            currentTransaction.TPDU = Formatter.replaceNII(currentTransaction.TPDU,currentTransaction.secureNII);
        else
            currentTransaction.TPDU = Formatter.replaceNII(currentTransaction.TPDU,currentTransaction.NII);

        //her we push the pre comps by this there should be communication setup for this process to work
        callSettlementChangeListenerCallback("Settlement Uploading Advices..");
        /*if (!pushPendingPreComps(comm,baseSelectedMerchant)) {
            callSettlementChangeListenerCallback("Uploading Advices Failed!");
            errorTone();
            comm.disconnect();
            return -1;
        }*/

        setFieldsAndLoadPacket();

        packet.setPacketTPDU(currentTransaction.TPDU);
        rawPacket = packet.getRawDataPacket();
        receivedPacket = sendAndRecieve(comm, rawPacket);
        Log.d("PRIN AAAAT", "02 DDDDD");

        callSettlementChangeListenerCallback("Processing Settlement..");

        if (isReversal) {
            showToast("No host Response",TOAST_TYPE_INFO);
            errorTone();
            comm.disconnect();
            setMustSettleFlagState(baseSelectedMerchant,true);
            return -1;
        }
        else if (receivedPacket == null) {
            showToast("Communication Failure",TOAST_TYPE_FAILED);
            errorTone();
            comm.disconnect();
            return -1;
        }

        hostResp = getUnpackPacketAndHostResponse(receivedPacket);

        if (hostResp != null) {
            if (hostResp.equals("00")) {
                performPostSettlementTask(hostSelected,baseSelectedMerchant);
                comm.disconnect();
                return 1;
            }
            else if (!hostResp.equals("95")) {
                showToast("Unknown Error",TOAST_TYPE_INFO);
                comm.disconnect();
                errorTone();
                setMustSettleFlagState(baseSelectedMerchant,true);
                return 1;
            }
        }

        callSettlementChangeListenerCallback("Uploading Batch...");

        tranBatch = transactionDatabase.readWithCustomQuary(selectQuery);
        //response code is 95 so we need to upload the batch
        while (tranBatch.moveToNext()) {
            Transaction uploadingTran = transactionDatabase.getTransaction(tranBatch);
            if (uploadingTran == null) {
                showToast("Error retrieving bath transactions aborting!",TOAST_TYPE_FAILED);
                return BATCH_LOAD_ERROR;
            }

            uploadingTran.origTransactionMTI = uploadingTran.MTI;
            uploadingTran.origTraceNumber = uploadingTran.traceNumber;
            curTranData = TranStaticData.getTran(TranStaticData.TranTypes.BATCH_UPLOAD);
            uploadingTran.MTI = curTranData.MTI;
            uploadingTran.bitmap = curTranData.Bitmap;
            uploadingTran.origTransactionCode = TranStaticData.TranTypes.BATCH_UPLOAD;

            //modify the original transactions processing code for bath upload
            String procCode = uploadingTran.procCode.substring(0,uploadingTran.procCode.length() - 1);
            procCode = procCode + "1";

            uploadingTran.procCode = procCode;
            currentTransaction = uploadingTran;

            if (!loadCardAndIssuerSettlement())
                return BATCH_LOAD_ERROR;

            //we must back up and restore the original invoice number instead of last invoice number
            int invoiceNumber = currentTransaction.inInvoiceNumber;

            if (!loadTerminal())
                return BATCH_LOAD_ERROR;

            currentTransaction.inInvoiceNumber = invoiceNumber;

            setFieldsAndLoadPacket();
            packet.setPacketTPDU(currentTransaction.TPDU);
            rawPacket = packet.getRawDataPacket();
            String hexPacket = Utility.byte2HexStr(rawPacket);
            receivedPacket = sendAndRecieve(comm, rawPacket);
            Log.d("PRINT EEE ", "02 EEEEE");

            batchTranCount++;
            callSettlementChangeListenerCallback("Uploading.. [" + batchTranCount + "]");


            if (isReversal)
            {
                showToast("No host response",TOAST_TYPE_INFO);
                errorTone();
                comm.disconnect();
                setMustSettleFlagState(baseSelectedMerchant,true);
                return BATCH_UPLOAD_FAILED;
            }

            hostResp = getUnpackPacketAndHostResponse(receivedPacket);
            if (hostResp != null)
            {
                if (!hostResp.equals("00"))
                {
                    showToast("Batch Upload Failed",TOAST_TYPE_FAILED);
                    comm.disconnect();
                    setMustSettleFlagState(baseSelectedMerchant,true);
                    return BATCH_UPLOAD_FAILED;
                }
            }
        }

        curTranData = TranStaticData.getTran(TranStaticData.TranTypes.CLOSE_BATCH_UPLOAD);
        settleTran.MTI = curTranData.MTI;
        settleTran.bitmap = curTranData.Bitmap;
        settleTran.inTransactionCode = TranStaticData.TranTypes.CLOSE_BATCH_UPLOAD;
        settleTran.procCode = curTranData.ProcCode;

        currentTransaction = settleTran;
        currentTransaction.merchantNumber = baseSelectedMerchant;

        if (false == loadIssuer(issuerNumber))
            return BATCH_LOAD_ERROR;

        if (false == loadTerminal())
            return BATCH_LOAD_ERROR;

        callSettlementChangeListenerCallback("Closing Batch Upload..");
        setFieldsAndLoadPacket();
        packet.setPacketTPDU(currentTransaction.TPDU);
        rawPacket = packet.getRawDataPacket();
        receivedPacket = sendAndRecieve(comm, rawPacket);
        Log.d("PRINT CCCC", "02SSDSDD");

        //batch upload close transaction
        if (isReversal)
        {
            showToast("No host response",TOAST_TYPE_INFO);
            errorTone();
            comm.disconnect();
            setMustSettleFlagState(baseSelectedMerchant,true);
            return -1;
        }
        else if (receivedPacket == null) {
            showToast("Communication Failure",TOAST_TYPE_FAILED);
            errorTone();
            comm.disconnect();
            setMustSettleFlagState(baseSelectedMerchant,true);
            return -1;
        }

        //analyze the response
        hostResp = getUnpackPacketAndHostResponse(receivedPacket);

        if (hostResp != null)
        {
            if (!hostResp.equals("00"))
            {
                callSettlementChangeListenerCallback("Failed..");
                showToast("Settlement Failed",TOAST_TYPE_FAILED);
                comm.disconnect();
                errorTone();
                setMustSettleFlagState(baseSelectedMerchant,true);
                return -1;
            }
            else
            {
                performPostSettlementTask(hostSelected,baseSelectedMerchant);
                comm.disconnect();
                return 1;
            }

        }
        return -1;
    }

    private String getUnpackPacketAndRRN(byte[] packet) {
        String rrn = null;
        ISO8583u resp = new ISO8583u();
        if (resp.unpack(packet,0))
            rrn = getRRN(resp);

        return rrn;
    }

    protected String getRRN(ISO8583u resp) {
        String response = null;
        if (resp.unpackValidField[37])
            response = resp.getUnpack(37);

        return response;
    }

    public void startAutomatedTaskLogger(String taskName,String desc)
    {
        Intent startAutomatedLogger = new Intent(mActivity, AutomatedTaskLogger.class);
        startAutomatedLogger.putExtra("TASK_NAME",taskName);
        startAutomatedLogger.putExtra("DESC",desc);

        (mActivity).startActivity(startAutomatedLogger);
    }

    public void closeAutomatedTaskLogger()
    {
        AutomatedLogQueue automatedLogQueue = AutomatedLogQueue.getInstance();
        automatedLogQueue.addMessageToAutomatedLogQueue("CLOSE_LOGGER");
    }

    public void performAutoSettlement(int host) {
        final AutomatedLogQueue automatedLogQueue = AutomatedLogQueue.getInstance();

        setOnSettlementStateChangeListener(new OnSettlementStateChange() {
            @Override
            public void OnSettlementStateChanged(String status) {
                automatedLogQueue.addMessageToAutomatedLogQueue(status);
            }
        });

        int baseIssuer = IssuerHostMap.hosts[host].baseIssuer;

        sleepMe(3000);
        setTerminalBusyFlag(true);
        setInTransaction(true);

        automatedLogQueue.addMessageToAutomatedLogQueue("Starting Auto Settlement\n");

        try {
            BankCard bankCard = new BankCard(appContext);
            bankCard.breakOffCommand();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        String msg = "\n\nSettling " + IssuerHostMap.hosts[host].hostName + "\n" ;
        automatedLogQueue.addMessageToAutomatedLogQueue(msg);
        GlobalData.isAutoSettling = true;

        if (SettingsInterpreter.isSingleMerchantEnabled())
        {
            int primaryMerchant = getFirstMerchantOfIssuer(baseIssuer);
            performSettlement(host,primaryMerchant);
        }
        else
        {
            //multi merchant auto settlement
            List<Integer> merchants = getAllMerchantsOfIssuer(baseIssuer);
            if (merchants != null)
            {
                for (Integer merchant : merchants)
                {
                    callSettlementChangeListenerCallback(IssuerHostMap.hosts[host].hostName + " [" + merchant + "]");
                    performSettlement(host,merchant);
                    sleepMe(2000);
                }
            }
        }

        GlobalData.isAutoSettling = false;
        sleepMe(3000);
        setInTransaction(false);
        setTerminalBusyFlag(false);
    }

    public interface onInitializationFinished {
        void onInitFinished();
    }

    protected boolean setMustSettleFlagState(int marchNumber,boolean state)
    {
        String mustSettleQuary  = "";
        boolean retResult = false;
        if (state)
            mustSettleQuary  = "UPDATE MIT SET MustSettleFlag = 1 WHERE MerchantNumber = " + marchNumber;
        else
            mustSettleQuary  = "UPDATE MIT SET MustSettleFlag = 0 WHERE MerchantNumber = " + marchNumber;

        try
        {
            retResult = configDatabase.executeCustomQuary(mustSettleQuary);
            return retResult != false;
        }catch (Exception ex)
        {
            ex.printStackTrace();
            return false;
        }
    }

    protected boolean setClearBatchFlag(int marchNumber,boolean state)
    {
        String mustSettleQuary  = "";
        boolean retResult = false;
        if (state)
            mustSettleQuary  = "UPDATE MIT SET ClearBatchFlag = 1 WHERE MerchantNumber = " + marchNumber;
        else
            mustSettleQuary  = "UPDATE MIT SET ClearBatchFlag = 0 WHERE MerchantNumber = " + marchNumber;

        try
        {
            retResult = configDatabase.executeCustomQuary(mustSettleQuary);
            return retResult != false;
        }catch (Exception ex)
        {
            ex.printStackTrace();
            return false;
        }
    }

    public boolean incrementTheBatchNumber(int selectedHost,int baseIssuerMerchantSelected) {
        String get  = "SELECT  * FROM MIT WHERE MerchantNumber = " + baseIssuerMerchantSelected;

        try {
            Cursor result = configDatabase.readWithCustomQuary(get);
            if (result == null || result.getCount() == 0)
                return false;

            result.moveToFirst();

            //get the batch number
            int batchNumber = result.getInt(result.getColumnIndex("BatchNumber"));
            batchNumber++;

            if (batchNumber > 999999)
                batchNumber = 0;

            //need to update batch number of all the merchants
            String put = "UPDATE MIT SET BatchNumber = " + batchNumber + " WHERE MerchantNumber = " + baseIssuerMerchantSelected;
            configDatabase.executeCustomQuary(put);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }


    private void performPostSettlementTask(int selectedHost,int merchantNumber) {
        String deleteSettlementQuary  = "DELETE FROM TXN WHERE TransactionCode != " + TranStaticData.TranTypes.PRE_AUTH + " AND MerchantNumber = " + merchantNumber;
        try {
            Receipt summery = Receipt.getInstance();
            if (!GlobalData.isAutoSettling)
                summery.printReceiptSummeryForSettlement(selectedHost, merchantNumber);

            callSettlementChangeListenerCallback("Success..");
            //increment the  batch number here
            if(!incrementTheBatchNumber(selectedHost, merchantNumber)) {
                setClearBatchFlag(merchantNumber,true);
                showToast("Batch Clear Failed, Please Clear Batch", TOAST_TYPE_FAILED);
                return;
            }
            if(!setMustSettleFlagState(merchantNumber, false)) {
                setClearBatchFlag(merchantNumber,true);
                showToast("Batch Clear Failed, Please Clear Batch", TOAST_TYPE_FAILED);
                return;
            }
            if(!transactionDatabase.writeLastSettlement(GlobalData.settlementData, merchantNumber)) {
                setClearBatchFlag(merchantNumber,true);
                showToast("Batch Clear Failed, Please Clear Batch", TOAST_TYPE_FAILED);
                return;
            }

            //wipe out the relevant records for this settlement on the database
            transactionDatabase.executeCustomQuary(deleteSettlementQuary);

            checkPendingPreAuthorization(merchantNumber);

            showToast("Settlement Successful", TOAST_TYPE_SUCCESS);
        } catch (Exception e) {
            e.printStackTrace();
            setClearBatchFlag(merchantNumber,true);
            showToast("Batch Clear Failed, Please Clear Batch", TOAST_TYPE_FAILED);
        }
    }

    private void checkPendingPreAuthorization(int merchantNumber) {
        SimpleDateFormat dt = new SimpleDateFormat("yyMMdd");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -20);
        Date todate1 = cal.getTime();
        String fromdate = dt.format(todate1);

        String deletePreAuthQuary = "DELETE FROM TXN WHERE MerchantNumber = " + merchantNumber + " AND TransactionCode = " + TranStaticData.TranTypes.PRE_AUTH + " AND substr(TxnReceiptDate,7)||substr(TxnReceiptDate,4,2)||substr(TxnReceiptDate,1,2) < '" + fromdate + "'";

        try {
            transactionDatabase.executeCustomQuary(deletePreAuthQuary);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public class MerchantTerminal {
        public String addrLine1;
        public String addrLine2;
        public String addrLine3;

        public String merchantID;
        public String terminalID;
        public String batchNumber;
        public String issuerName;
        public String merchName;
    }

    protected MerchantTerminal loadMerchantDetails(int merchNumber) {
        String merchanSelect = "SELECT * FROM MIT,TMIF,IIT WHERE TMIF.MerchantNumber = MIT.MerchantNumber AND IIT.IssuerNumber = TMIF.IssuerNumber AND MIT.MerchantNumber = " + merchNumber;

        Cursor merchRec =  configDatabase.readWithCustomQuary(merchanSelect);

        MerchantTerminal mt  =  new MerchantTerminal();

        if (merchRec != null && merchRec.getCount() > 0) {
            merchRec.moveToFirst();
            mt.addrLine1 = merchRec.getString(merchRec.getColumnIndex("RctHdr1"));
            mt.addrLine2 = merchRec.getString(merchRec.getColumnIndex("RctHdr2"));
            mt.addrLine3 = merchRec.getString(merchRec.getColumnIndex("RctHdr3"));

            mt.merchantID = merchRec.getString(merchRec.getColumnIndex("MerchantID"));
            mt.terminalID = merchRec.getString(merchRec.getColumnIndex("TerminalID"));
            mt.issuerName = merchRec.getString(merchRec.getColumnIndex("IssuerLable"));
            mt.merchName = merchRec.getString(merchRec.getColumnIndex("MerchantName"));
            int batchNumber = merchRec.getInt(merchRec.getColumnIndex("BatchNumber"));
            GlobalData.merchantName = merchRec.getString(merchRec.getColumnIndex("MerchantName"));

            mt.batchNumber = Formatter.formatForSixDigits(batchNumber);
            merchRec.close();

            return mt;
        }
        return null;
    }

    public static int getFirstMerchantOfHost(int host) {
        int baseIssuer = IssuerHostMap.hosts[host].baseIssuer;
        return getFirstMerchantOfIssuer(baseIssuer);
    }

    public static int getFirstMerchantOfIssuer(int issuerNumber) {
        String merchantQuary = "select  mit.MerchantName,mit.MerchantNumber from tmif,mit where tmif.IssuerNumber = " + issuerNumber + " and  tmif.MerchantNumber = mit.MerchantNumber";
        Cursor merchRecords =  configDatabase.readWithCustomQuary(merchantQuary);

        if (merchRecords == null || merchRecords.getCount() == 0)
            return -1;

        merchRecords.moveToFirst();
        //get the first records number
        int merchNumber  = merchRecords.getInt(merchRecords.getColumnIndex("MerchantNumber"));
        return merchNumber;
    }

    public static List<Integer> getAllMerchantsOfIssuer(int issuerNumber) {
        String selectQuary = "SELECT MIT.MerchantNumber FROM TMIF,MIT WHERE IssuerNumber = " + issuerNumber +
                " AND TMIF.MerchantNumber = MIT.MerchantNumber";

        Cursor merchantRecs = null;
        try {
            merchantRecs = configDatabase.readWithCustomQuary(selectQuary);
            if (merchantRecs == null || merchantRecs.getCount() == 0)
                return null;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        List<Integer> merchants = new ArrayList<>();

        while (merchantRecs.moveToNext()) {
            int merchNumber = merchantRecs.getInt(merchantRecs.getColumnIndex("MerchantNumber"));
            merchants.add(merchNumber);
        }
        return merchants;
    }

    public static String getPinPackageName()
    {
        return packageName;
    }

    public void printDiagnosisReport() {
        if (!SettingsInterpreter.isEMVDiagnosisEnabled())
            return ;

        Thread diagPrintThread = new  Thread(new Runnable() {
            @Override
            public void run() {
                setTerminalBusyFlag(true);
                Receipt receipt = Receipt.getInstance();
                receipt.printDiagnosticReportEMV();
                setTerminalBusyFlag(false);
                setCardThreadStop(false);
            }
        });

        setCardThreadStop(true);
        diagPrintThread.start();
    }

    public String getCurrentContextPackageName() {
        //search through the name of the host for the selected issuer
        for (HostIssuer issuer : IssuerHostMap.hosts) {
            for (int issuerNum : issuer.issuerList) {
                if (issuerNum == currentTransaction.issuerNumber)       //found the selected issuer
                    return issuer.hostName;
            }
        }
        return null;
    }

    public String getCurrencySymbol(String currencyCode) {
        String quary = "SELECT CurrencySymbol FROM CST WHERE CurrencyCode = " + currencyCode;
        String currencySymbol = "";
        try {
            Cursor curSym = configDatabase.readWithCustomQuary(quary);
            if (curSym != null &&  curSym.getCount() == 0)
                return null;

            curSym.moveToFirst();
            currencySymbol  = curSym.getString(curSym.getColumnIndex("CurrencySymbol"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return currencySymbol;
    }

    public void setPackageName() {
        try {
           String packageName = getCurrentContextPackageName();
           Services.keys.setPackageName(packageName);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public int initiateECR() {
        int ecrResult = 0;

        if (MainActivity.ecr.isECRInitiated && SettingsInterpreter.isECREnabled()) {
            ecrResult = MainActivity.ecr.pushBinWaitForSale(currentTransaction);
            if (ecrResult == ECR.NO_ECR_RESPONSE) {
                showToast("No ECR Response..",TOAST_TYPE_INFO);
                playSound(R.raw.error);
                return GENERIC_ERROR_TRAN_MIDDLE;
            }
        }
        return SUCCESS;
    }

    public void printISOLogs() {
        if (SettingsInterpreter.isIsoPrintEnabled()) {
            Receipt rcpt =  Receipt.getInstance();
            String data = Emv.getISOLogData(Emv.MODE.SEND);
            if (data != null)
                rcpt.printISOLogData(data);

            data = Emv.getISOLogData(Emv.MODE.RECIEVE);
            if (data != null)
                rcpt.printISOLogData(data);
        }
    }
}