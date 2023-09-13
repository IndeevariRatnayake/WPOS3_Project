package com.harshana.wposandroiposapp;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.SystemClock;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.harshana.wposandroiposapp.Base.Base;
import com.harshana.wposandroiposapp.Base.Services;
import com.harshana.wposandroiposapp.Base.WaitTimer;
import com.harshana.wposandroiposapp.ColorScheme.CLScheme;
import com.harshana.wposandroiposapp.DevArea.GlobalData;
import com.harshana.wposandroiposapp.DevArea.TranStaticData;
import com.harshana.wposandroiposapp.ECR.ECR;
import com.harshana.wposandroiposapp.ECR.UsbService;
import com.harshana.wposandroiposapp.QRIntegration.QRDisplay;
import com.harshana.wposandroiposapp.QRIntegration.QRVerify;
import com.harshana.wposandroiposapp.Settings.Preferences;
import com.harshana.wposandroiposapp.Settings.SettingsActivity;
import com.harshana.wposandroiposapp.Settings.SettingsInterpreter;
import com.harshana.wposandroiposapp.UI.BatchTrans.SettlementActivity;
import com.harshana.wposandroiposapp.UI.BatchTrans.VoidActivity;
import com.harshana.wposandroiposapp.UI.Other.Last4Activity;
import com.harshana.wposandroiposapp.UI.OtherTrans.PreComp;
import com.harshana.wposandroiposapp.UI.Reports.ReportActivity;
import com.harshana.wposandroiposapp.UI.Settings.MerchantLoginActivity;
import com.harshana.wposandroiposapp.UI.Settings.SettingsHomeActivity;
import com.harshana.wposandroiposapp.UI.TranFlow.InputAmount;
import com.harshana.wposandroiposapp.UI.TranFlow.ManualKeyEntry;
import com.harshana.wposandroiposapp.UI.Users.AdminLoginActivity;
import com.harshana.wposandroiposapp.UI.Utils.ClearBatch;
import com.harshana.wposandroiposapp.UI.Utils.ClearReversals;
import com.harshana.wposandroiposapp.UI.Utils.ForceReversals;
import com.harshana.wposandroiposapp.Utilities.Sounds;

import java.util.Set;

import pl.droidsonroids.gif.GifImageView;

import static maes.tech.intentanim.CustomIntent.customType;

public class MainActivity extends AppCompatActivity {
    //defines the basic global variables
    public static Base applicationBase;
    public static Context myContext;
    public static Handler mainMessageHandler;

    public static final int OPER_SHOW_TOAST = 1;
    public static final int OPER_SHOW_TOAST_X = 2;
    public static final int OPER_START_ACTIVITY = 4;
    public static final int OPER_UPDATE_STATUS_TEXT = 5;
    public static final int ACTIVITY_START_AMOUNT_INPUT = 1;
    public static final int ACTIVITY_LAST_FOUR = 3;
    public static final int OPER_CARD_INPUT_SCREEN = 10;
    public static final int OPER_QR_SCREEN = 15;

    public static final int REQUEST_START_AMOUNT_INPUT = 1;
    public static final int REQUEST_START_MANUAL_KEY_IN = 2;
    public static final int REQUEST_START_AMOUNT_INPUT_QR = 3;

    public static TextView txtStatus;
    static TextView txtStatusExt;
    DrawerLayout drawerLayout;
    private ProgressDialog progress = null;

    GifImageView gifPlayer;

    ScrollView scrollViewCards = null;
    ActionBarDrawerToggle toggle;
    public static ECR ecr;
    public static boolean isOnPause = false;

    View.OnTouchListener mainActivityTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                //applicationBase.launchCardReaderThread();
            }
            return false;
        }
    };

    View.OnTouchListener touchListener = mainActivityTouchListener;
    private TextView userRoleTextView, logOutTextView;
    private LinearLayout saleLinearLayout, preCompLinearLayout, voidLinearLayout, settlementLinearLayout, clearBatchLinearLayout,
            clearReversalLinearLayout, setupLinearLayout, preAuthLinearLayout, reportLinearLayout, settingsLinearLayout,
            adminLinearLayout, loginAccessLinearLayout, qrLinearLayout, qrVerifyLinearLayout, forceReversalLinearLayout;
    private boolean cardInputTimerOff = false;
    private boolean isManual = false;
    private long mLastClickTime = 0;

    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String role = Preferences.getInstance(MainActivity.this).getSetting(GlobalData.USER_ROLE);

            if (role == null) {
                role = "";
            }
            if (applicationBase.verifyTerminalAvailabilityForOperation()) {
                return;
            }

            // Preventing multiple clicks, using threshold of 1 second
            if (SystemClock.elapsedRealtime() - mLastClickTime < 1000) {
                return;
            }
            mLastClickTime = SystemClock.elapsedRealtime();

            if (v == saleLinearLayout) {
                //GlobalData.transactionCode = TranStaticData.TranTypes.SALE;
                boolean merchenable = true;
                Intent amountInput = new Intent(MainActivity.this, InputAmount.class);
                amountInput.putExtra("merchenable",merchenable);
                startActivityForResult(amountInput, REQUEST_START_AMOUNT_INPUT);
            }
            else if (v == voidLinearLayout) {
                if (role.equalsIgnoreCase(GlobalData.ADMIN)) {
                    Intent voidActivity = new Intent(MainActivity.this, VoidActivity.class);
                    startActivity(voidActivity);
                } else {
                    Intent callVoid = new Intent(MainActivity.this, MerchantLoginActivity.class);
                    callVoid.putExtra("type", "void");
                    startActivity(callVoid);
                }
            }
            else if (v == settlementLinearLayout) {
                if (role.equalsIgnoreCase(GlobalData.ADMIN)) {
                    Intent startSettleActivity = new Intent(MainActivity.this, SettlementActivity.class);
                    startActivity(startSettleActivity);
                } else {
                    Intent callVoid = new Intent(MainActivity.this, MerchantLoginActivity.class);
                    callVoid.putExtra("type", "settlement");
                    startActivity(callVoid);
                }
            }
            else if (v == logOutTextView) {
                Preferences.getInstance(MainActivity.this).saveSetting(GlobalData.USER_ROLE, "");
                checkUserRole();
            }
            else if (v == qrLinearLayout) {
                if(applicationBase.checkForQRTran()) {
                    showToast("Pending QR Tran Available");
                }
                else {
                    boolean merchenable = false;
                    Intent amountInputQR = new Intent(MainActivity.this, InputAmount.class);
                    amountInputQR.putExtra("merchenable",merchenable);
                    startActivityForResult(amountInputQR, REQUEST_START_AMOUNT_INPUT_QR);
                }
            }
            else if (v == qrVerifyLinearLayout) {
                if(applicationBase.checkForQRTran()) {
                    Intent qrActivity = new Intent(MainActivity.this, QRVerify.class);
                    startActivity(qrActivity);
                }
                else {
                    showToast("No Pending QR Tran");
                }
            }
            else if (v == adminLinearLayout) {
                Intent adminActivity = new Intent(MainActivity.this, AdminLoginActivity.class);
                startActivity(adminActivity);
            }
            else if (v == setupLinearLayout) {
                Intent settingsActivity = new Intent(MainActivity.this, SettingsHomeActivity.class);
                startActivity(settingsActivity);
            }
            else if (v == reportLinearLayout) {
                Intent settingsActivity = new Intent(MainActivity.this, ReportActivity.class);
                startActivity(settingsActivity);
            }
            else if (v == settingsLinearLayout) {
                Intent settingsActivity = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(settingsActivity);
            }
            else if (v == forceReversalLinearLayout) {
                if (role.equalsIgnoreCase(GlobalData.ADMIN)) {
                    Intent settingsActivity = new Intent(MainActivity.this, ForceReversals.class);
                    startActivity(settingsActivity);
                } else {
                    Intent callVoid = new Intent(MainActivity.this, MerchantLoginActivity.class);
                    callVoid.putExtra("type", "forceReversal");
                    startActivity(callVoid);
                }
            }
            else if (v == clearReversalLinearLayout) {
                Intent startClearReversals = new Intent(MainActivity.this, ClearReversals.class);
                startActivity(startClearReversals);
            }
            else if (v == clearBatchLinearLayout) {
                if (role.equalsIgnoreCase(GlobalData.ADMIN)) {
                    Intent startClearBatch = new Intent(MainActivity.this, ClearBatch.class);
                    startActivity(startClearBatch);
                } else {
                    Intent callVoid = new Intent(MainActivity.this, MerchantLoginActivity.class);
                    callVoid.putExtra("type", "clearBatch");
                    startActivity(callVoid);
                }
            }
            else if (v == preAuthLinearLayout) {
                GlobalData.transactionCode = TranStaticData.TranTypes.PRE_AUTH;
                boolean merchenable = true;
                Intent amountInput = new Intent(MainActivity.this, InputAmount.class);
                amountInput.putExtra("merchenable",merchenable);
                startActivityForResult(amountInput, REQUEST_START_AMOUNT_INPUT);
            }
            else if (v == preCompLinearLayout) {
                Intent preComp = new Intent(MainActivity.this, PreComp.class);
                startActivity(preComp);
            }
        }
    };

    Toast showToastMessage;
    void showToast(String toastMessage) {
        if (showToastMessage != null) {
            showToastMessage.setText(toastMessage);
            showToastMessage.show();
        } else {
            showToastMessage = Toast.makeText(MainActivity.this,toastMessage,Toast.LENGTH_SHORT);
            showToastMessage.show();
        }
    }

    Toast showToastMessagehnd;
    void showToasthnd(String toastMessage) {
        if (showToastMessagehnd != null) {
            showToastMessagehnd.setText(toastMessage);
        } else {
            showToastMessagehnd = Toast.makeText(MainActivity.this,toastMessage,Toast.LENGTH_SHORT);
        }
    }

    View.OnTouchListener cardInputTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if(SettingsInterpreter.isManualKeyIn()) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if(!isManual) {
                        isManual = true;
                        cardInputTimerOff = true;
                        Sounds sounds = Sounds.getInstance();
                        sounds.playCustSound(R.raw.tran_detect);
                        //start the manual key entry screen
                        applicationBase.setCardThreadStop(true);
                        applicationBase.setInTransaction(true);
                        try {
                            if (applicationBase.bankCard != null)
                                applicationBase.bankCard.breakOffCommand();
                        } catch (Exception e) {
                            isManual = false;
                            e.printStackTrace();
                        }
                        Intent startManualKeyIntent = new Intent(MainActivity.this, ManualKeyEntry.class);
                        startActivityForResult(startManualKeyIntent, REQUEST_START_MANUAL_KEY_IN);
                    }
                }
            }
            return false;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            SpannableString spannableString = new SpannableString(menu.getItem(i).getTitle().toString());
            int end = spannableString.length();
            spannableString.setSpan(new RelativeSizeSpan(0.8f), 0, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            item.setTitle(spannableString);

            Menu subMenu = null;

            //process all the sub menus
            MenuItem subMenuItem = null;

            subMenu = item.getSubMenu();

            do {
                for (int j = 0; j < subMenu.size(); j++) {
                    subMenuItem = subMenu.getItem(j);
                    spannableString = new SpannableString(subMenu.getItem(j).getTitle().toString());
                    end = spannableString.length();
                    spannableString.setSpan(new RelativeSizeSpan(0.8f), 0, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    subMenuItem.setTitle(spannableString);
                }
            } while ((subMenu = subMenuItem.getSubMenu()) != null);
        }
        return true;
    }

    @Override
    protected void onPause() {
        // WHEN THE SCREEN IS ABOUT TO TURN OFF
        if (ScreenReceiver.wasScreenOn) {
            // THIS IS THE CASE WHEN ONPAUSE() IS CALLED BY THE SYSTEM DUE TO A SCREEN STATE CHANGE
            System.out.println("SCREEN TURNED OFF");
            try {
                if(applicationBase.bankCard != null)
                    applicationBase.bankCard.breakOffCommand();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            // THIS IS WHEN ONPAUSE() IS CALLED WHEN THE SCREEN STATE HAS NOT CHANGED
        }
        super.onPause();
        isOnPause = true;
        applicationBase.setCardThreadStop(true);

        if (SettingsInterpreter.isECREnabled()) {
            unregisterReceiver(mUsbReceiver);
            unbindService(usbConnection);
        }
    }

    @Override
    protected void onStart() {
        isOnPause = false;
        super.onStart();
    }

    @Override
    protected void onResume() {
        // ONLY WHEN SCREEN TURNS ON
        if (!ScreenReceiver.wasScreenOn) {
            // THIS IS WHEN ONRESUME() IS CALLED DUE TO A SCREEN STATE CHANGE
            System.out.println("SCREEN TURNED ON");
            applicationBase.launchCardReaderThread();
        } else {
            // THIS IS WHEN ONRESUME() IS CALLED WHEN THE SCREEN STATE HAS NOT CHANGED
        }
        super.onResume();

        applicationBase.setCardThreadStop(false);

        isOnPause = false;

        if (SettingsInterpreter.isECREnabled()) {
            setFilters();  // Start listening notifications from UsbService
            startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
        }

        boolean val = Preferences.getInstance(this).getBoolean("is_pre_auth_enable");
        boolean val2 = Preferences.getInstance(this).getBoolean("is_force_rev_enable");
        Log.d("llllllll val", " : " + val);
        if (val) {
            preAuthLinearLayout.setVisibility(View.VISIBLE);
            preCompLinearLayout.setVisibility(View.VISIBLE);
        } else {
            preAuthLinearLayout.setVisibility(View.INVISIBLE);
            preCompLinearLayout.setVisibility(View.INVISIBLE);
        }

        if(SettingsInterpreter.isForceReversalEnabled()) {
            forceReversalLinearLayout.setVisibility(View.VISIBLE);
        } else {
            forceReversalLinearLayout.setVisibility(View.INVISIBLE);
        }

        checkUserRole();
    }

    /**
     * check logged user role and its access
     */
    private void checkUserRole() {
        String role = Preferences.getInstance(this).getSetting(GlobalData.USER_ROLE);
        Log.d("PPPPPPPPPPPPP USER ROLE", " : " + role);
        if (role == null || role.trim().isEmpty()) {
            ///Default user
            userRoleTextView.setVisibility(View.INVISIBLE);
            logOutTextView.setVisibility(View.INVISIBLE);
            loginAccessLinearLayout.setVisibility(View.GONE);
            defaultUser();
        } else if (role.trim().equalsIgnoreCase(GlobalData.ADMIN)) {
            ///Admin user
            userRoleTextView.setVisibility(View.VISIBLE);
            logOutTextView.setVisibility(View.VISIBLE);
            loginAccessLinearLayout.setVisibility(View.VISIBLE);
            userRoleTextView.setText("Admin Access");
            adminUser();
        } else if (role.trim().equalsIgnoreCase(GlobalData.MERCHANT)) {
            ///Merchant user
            userRoleTextView.setVisibility(View.VISIBLE);
            logOutTextView.setVisibility(View.VISIBLE);
            loginAccessLinearLayout.setVisibility(View.VISIBLE);
            userRoleTextView.setText("Merchant Access");
            merchantUser();
        }
    }

    /**
     * Default User display
     */
    private void defaultUser() {
        setScreenMode(1);
        saleLinearLayout.setVisibility(View.VISIBLE);
        voidLinearLayout.setVisibility(View.VISIBLE);
        settlementLinearLayout.setVisibility(View.VISIBLE);
        checkPreAuth();
        qrLinearLayout.setVisibility(View.VISIBLE);
        qrVerifyLinearLayout.setVisibility(View.VISIBLE);
        reportLinearLayout.setVisibility(View.VISIBLE);
        setupLinearLayout.setVisibility(View.INVISIBLE);
        settingsLinearLayout.setVisibility(View.INVISIBLE);
        clearBatchLinearLayout.setVisibility(View.INVISIBLE);
        clearReversalLinearLayout.setVisibility(View.INVISIBLE);
    }

    /**
     * Merchant User display
     */
    private void merchantUser() {
        setScreenMode(1);
        saleLinearLayout.setVisibility(View.VISIBLE);
        voidLinearLayout.setVisibility(View.VISIBLE);
        settlementLinearLayout.setVisibility(View.VISIBLE);
        checkPreAuth();
        qrLinearLayout.setVisibility(View.VISIBLE);
        qrVerifyLinearLayout.setVisibility(View.VISIBLE);
        reportLinearLayout.setVisibility(View.VISIBLE);
        setupLinearLayout.setVisibility(View.INVISIBLE);
        settingsLinearLayout.setVisibility(View.INVISIBLE);
        adminLinearLayout.setVisibility(View.INVISIBLE);
        clearBatchLinearLayout.setVisibility(View.INVISIBLE);
        clearReversalLinearLayout.setVisibility(View.INVISIBLE);
    }

    /**
     * check pre auth enable disable status
     */
    private void checkPreAuth() {
        boolean val = Preferences.getInstance(this).getBoolean("is_pre_auth_enable");
        Log.d("llllllll val", " : " + val);
        if (val) {
            preAuthLinearLayout.setVisibility(View.VISIBLE);
            preCompLinearLayout.setVisibility(View.VISIBLE);
        } else {
            preAuthLinearLayout.setVisibility(View.INVISIBLE);
            preCompLinearLayout.setVisibility(View.INVISIBLE);
        }

        if (SettingsInterpreter.isForceReversalEnabled()) {
            forceReversalLinearLayout.setVisibility(View.VISIBLE);
        } else {
            forceReversalLinearLayout.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * admin User display
     */
    private void adminUser() {
        setScreenMode(0);
        saleLinearLayout.setVisibility(View.VISIBLE);
        voidLinearLayout.setVisibility(View.VISIBLE);
        settlementLinearLayout.setVisibility(View.VISIBLE);
        preCompLinearLayout.setVisibility(View.VISIBLE);
        preAuthLinearLayout.setVisibility(View.VISIBLE);
        clearBatchLinearLayout.setVisibility(View.VISIBLE);
        qrLinearLayout.setVisibility(View.VISIBLE);
        qrVerifyLinearLayout.setVisibility(View.VISIBLE);
        reportLinearLayout.setVisibility(View.VISIBLE);
        setupLinearLayout.setVisibility(View.VISIBLE);
        settingsLinearLayout.setVisibility(View.VISIBLE);
        clearReversalLinearLayout.setVisibility(View.VISIBLE);
        if(SettingsInterpreter.isForceReversalEnabled())
            forceReversalLinearLayout.setVisibility(View.VISIBLE);
        else
            forceReversalLinearLayout.setVisibility(View.INVISIBLE);
    }

    public void setScreenMode(int mode) {
        KeyControlUtil.setStatusbarMode(mode);
        if (mode == 1) {
            KeyControlUtil.setPropForControlBackKey(true);
            KeyControlUtil.setPropForControlHomeKey(true);
            KeyControlUtil.setPropForControlMenuKey(true);
        }
        else {
            KeyControlUtil.setPropForControlBackKey(false);
            KeyControlUtil.setPropForControlHomeKey(false);
            KeyControlUtil.setPropForControlMenuKey(false);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myContext = MainActivity.this;
        setScreenMode(1);
        mHandler = new MyHandler();

        initMainMessageHandler();

        applicationBase = new Base(getApplicationContext(),MainActivity.this);

        txtStatus = findViewById(R.id.txtStatus);
        txtStatusExt = findViewById(R.id.txtStatusExt);

        userRoleTextView = findViewById(R.id.userRoleTextView);
        logOutTextView = findViewById(R.id.logOutTextView);
        logOutTextView.setOnClickListener(clickListener);
        loginAccessLinearLayout = findViewById(R.id.loginAccessLinearLayout);
        preCompLinearLayout = findViewById(R.id.preCompLinearLayout);
        preCompLinearLayout.setOnClickListener(clickListener);
        qrLinearLayout = findViewById(R.id.qrLinearLayout);
        qrLinearLayout.setOnClickListener(clickListener);
        qrVerifyLinearLayout = findViewById(R.id.qrVerifyLinearLayout);
        qrVerifyLinearLayout.setOnClickListener(clickListener);
        saleLinearLayout = findViewById(R.id.saleLinearLayout);
        saleLinearLayout.setOnClickListener(clickListener);
        voidLinearLayout = findViewById(R.id.voidLinearLayout);
        voidLinearLayout.setOnClickListener(clickListener);
        settlementLinearLayout = findViewById(R.id.settlementLinearLayout);
        settlementLinearLayout.setOnClickListener(clickListener);
        clearBatchLinearLayout = findViewById(R.id.clearBatchLinearLayout);
        clearBatchLinearLayout.setOnClickListener(clickListener);
        clearReversalLinearLayout = findViewById(R.id.clearReversalLinearLayout);
        clearReversalLinearLayout.setOnClickListener(clickListener);
        setupLinearLayout = findViewById(R.id.setupLinearLayout);
        setupLinearLayout.setOnClickListener(clickListener);
        preAuthLinearLayout = findViewById(R.id.preAuthLinearLayout);
        preAuthLinearLayout.setOnClickListener(clickListener);
        reportLinearLayout = findViewById(R.id.reportLinearLayout);
        reportLinearLayout.setOnClickListener(clickListener);
        settingsLinearLayout = findViewById(R.id.settingsLinearLayout);
        settingsLinearLayout.setOnClickListener(clickListener);
        adminLinearLayout = findViewById(R.id.adminLinearLayout);
        adminLinearLayout.setOnClickListener(clickListener);
        forceReversalLinearLayout = findViewById(R.id.forceReversalLinearLayout);
        forceReversalLinearLayout.setOnClickListener(clickListener);

        CLScheme.setCurrentColorScheme(CLScheme.SCHEME_NTB);

        if (!SettingsInterpreter.isThemeEnabled())
            CLScheme.isThemeEnabled = false;

        drawerLayout = findViewById(R.id.drawerlayout);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        gifPlayer = findViewById(R.id.gifPlayer);

        scrollViewCards = findViewById(R.id.scrollGrid);

        initProgressDialog();
        new MyTask(progress).execute();

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        BroadcastReceiver broadcastReceiver = new ScreenReceiver();
        registerReceiver(broadcastReceiver, intentFilter);

        //Indeevari
        //Preferences.getInstance(MainActivity.this).saveSetting(GlobalData.USER_ROLE, GlobalData.MERCHANT);
    }

    void initMainMessageHandler() {
        mainMessageHandler = new Handler() {
            String msgToDisplay;

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                //this will display a toast on the main window
                if (msg.what == OPER_START_ACTIVITY) {
                    if (msg.arg1 == ACTIVITY_START_AMOUNT_INPUT) {
                        boolean merchenable = true;
                        Intent amountIntent = new Intent(MainActivity.this, InputAmount.class);
                        amountIntent.putExtra("merchenable",merchenable);
                        startActivity(amountIntent);
                        customType(MainActivity.this, "left-to-right");
                    } else if (msg.arg1 == ACTIVITY_LAST_FOUR) {
                        Intent merchantIntent = new Intent(MainActivity.this, Last4Activity.class);
                        startActivity(merchantIntent);
                    }
                } else if (msg.what == OPER_SHOW_TOAST) {
                    msgToDisplay = msg.getData().getString("tst_msg");
                    showToasthnd(msgToDisplay);
                    TextView view =  showToastMessagehnd.getView().findViewById(android.R.id.message);

                    if (msg.arg1 == Base.TOAST_TYPE_FAILED) {
                        view.setTextColor(Color.RED);
                        view.setTextSize(20.0f);
                    } else if (msg.arg1 == Base.TOAST_TYPE_WARNING) {
                        view.setTextColor(Color.YELLOW);
                        view.setTextSize(20.0f);
                    } else if (msg.arg1 == Base.TOAST_TYPE_INFO) {
                        view.setTextColor(Color.BLUE);
                        view.setTextSize(20.0f);
                    } else if (msg.arg1 == Base.TOAST_TYPE_SUCCESS) {
                        view.setTextColor(Color.GREEN);
                        view.setTextSize(20.0f);
                    }
                    showToastMessagehnd.show();
                } else if (msg.what == OPER_SHOW_TOAST_X) {
                    msgToDisplay = msg.getData().getString("tst_msg");
                    showToast(msgToDisplay);
                } else if (msg.what == OPER_UPDATE_STATUS_TEXT) {
                    msgToDisplay = msg.getData().getString("status_text");
                    txtStatus.setText(msgToDisplay);
                    txtStatus.setTextColor(getResources().getColor(R.color.colorWhite));
                    txtStatus.setBackgroundColor(getResources().getColor(R.color.bg));

                    if((msgToDisplay.equals("Receiving...")) || (msgToDisplay.equals("Please Remove the Card"))){
                        terminalInProcessDialog(msgToDisplay);
                    } else {
                        if(progress != null)
                            progress.dismiss();
                    }
                    /*if((msgToDisplay.equals("Terminal Ready")) || (msgToDisplay.equals("Touch To Wake Up"))) {
                        if(progress != null)
                            progress.dismiss();
                    } else if((msgToDisplay.equals("Receiving..."))){
                        terminalInProcessDialog(msgToDisplay);
                    }*/
                } else if (msg.what == OPER_CARD_INPUT_SCREEN) {
                    displayCardInputScreen();
                } else if (msg.what == OPER_QR_SCREEN) {
                    Intent qrActivity = new Intent(MainActivity.this, QRDisplay.class);
                    startActivity(qrActivity);
                }
            }
        };
    }

    private void terminalInProcessDialog(String msg) {
        if(progress.isShowing()) {
            progress.setTitle(msg);
        }
        else {
            progress = new ProgressDialog(this);
            progress.setTitle(msg);
            progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
            progress.show();
        }
    }

    private void initProgressDialog() {
        progress = new ProgressDialog(this);
        progress.setTitle("Loading");
        progress.setMessage("Wait while loading...");
        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
    }

    private void setTouchListener() {
        View mainView = findViewById(R.id.id_main_layout);
        mainView.setOnTouchListener(touchListener);
    }

    public void displayCardInputScreen() {
        //load the required animation and play the audible clip to notify for requesting card input
        final View mainView = findViewById(R.id.id_main_layout);
        cardInputTimerOff = false;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus.setText("Please Insert or Tap Card");
                txtStatus.setTextColor(getResources().getColor(R.color.colorBlack));
                //txtStatus.setBackgroundColor(getResources().getColor(R.color.colorWhite));
                scrollViewCards.setVisibility(View.INVISIBLE);
                txtStatus.setPadding(0,0,0,120);
                gifPlayer.setImageResource(R.drawable.tp);
                gifPlayer.setVisibility(View.VISIBLE);
                mainView.setOnTouchListener(cardInputTouchListener);
            }
        });

        final WaitTimer timer = new WaitTimer(10);
        timer.setOnTimeOutListener(new WaitTimer.OnTimeOutListener() {
            @Override
            public void onTimeOut() {
                GlobalData.globalTransactionAmount = 0;
                GlobalData.transactionCode = -1;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtStatus.setTextColor(getResources().getColor(R.color.colorWhite));
                        txtStatus.setBackgroundColor(getResources().getColor(R.color.bg));
                        txtStatus.setPadding(0,0,0,0);
                        scrollViewCards.setVisibility(View.VISIBLE);
                        gifPlayer.setVisibility(View.GONE);
                        txtStatus.setText("Terminal Ready");
                    }
                });

                timer.stopTimer();
                mainView.setOnTouchListener(mainActivityTouchListener);
            }
        });

        timer.setOnTimerTickListener(new WaitTimer.OnTimerTickListener() {
            @Override
            public void onTimerTick(int tick) {
                if (cardInputTimerOff || applicationBase.verifyTerminalAvailabilityForOperation()) {
                    //entered in a transaction
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtStatus.setTextColor(getResources().getColor(R.color.colorWhite));
                            txtStatus.setBackgroundColor(getResources().getColor(R.color.bg));
                            txtStatus.setPadding(0,0,0,0);
                            scrollViewCards.setVisibility(View.VISIBLE);
                            gifPlayer.setVisibility(View.GONE);
                            txtStatus.setText("Terminal Ready");
                            mainView.setOnTouchListener(mainActivityTouchListener);
                        }
                    });
                    timer.stopTimer();
                }
            }
        });

        timer.start();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        if (applicationBase.verifyTerminalAvailabilityForOperation()) {
            showToast("Application is Busy,Please wait");
            return;
        }

        if (SettingsInterpreter.isECREnabled()) {
            unregisterReceiver(mUsbReceiver);
            unbindService(usbConnection);
        }

        AlertDialog.Builder alert =  new AlertDialog.Builder(MainActivity.this);
        alert.setTitle("Confirm Your Action");
        alert.setMessage("Do you want to exit from the application?");
        alert.setPositiveButton("Yes",dialogClickListener);
        alert.setNegativeButton("No",dialogClickListener);
        alert.setCancelable(false);
        alert.show();
    }

    DialogInterface.OnClickListener dialogClickListener =  new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    //clicked yes
                    System.exit(0);
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    break;
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_START_AMOUNT_INPUT) {
            if (resultCode == RESULT_OK)
                displayCardInputScreen();
            else if (resultCode == RESULT_CANCELED)
                applicationBase.setInTransaction(false);

        } else if (requestCode == REQUEST_START_MANUAL_KEY_IN) {
            if (resultCode == RESULT_CANCELED) {
                applicationBase.setInTransaction(false);
            }
            isManual = false;
        } else if (requestCode == REQUEST_START_AMOUNT_INPUT_QR) {
            if (resultCode == RESULT_OK) {
                Intent qrActivity = new Intent(MainActivity.this, QRDisplay.class);
                startActivity(qrActivity);
            }
        }
    }

    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.changeBaudRate(115200);
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };


    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    public UsbService usbService;
    public  MyHandler mHandler;

    /*
     * This handler will btextView1e passed to UsbService. Data received from serial port is displayed through this handler
     */
    static String ecrBuffer = "";
    private static class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int retVal = 0 ;
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    try {
                        String data = (String) msg.obj;
                        retVal = ecr.performECRFunc(data);
                        if (retVal == 1)
                            data = "";
                        break;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                case UsbService.CTS_CHANGE:
                    break;
                case UsbService.DSR_CHANGE:
                    break;
                case UsbService.SYNC_READ:
                    ecrBuffer = (String) msg.obj;
                    retVal = ecr.performECRFunc(ecrBuffer);
                    if (retVal == 1)
                        ecrBuffer = "";
                    break;
            }
        }
    }

    public class MyTask extends AsyncTask<Void, Void, Void> {
        ProgressDialog myprogress;
        public MyTask(ProgressDialog progress) {
            this.myprogress = progress;
        }

        public void onPreExecute() {
            myprogress.show();
        }

        public Void doInBackground(Void... unused) {
            applicationBase.initialize();

            applicationBase.setOnInitializationFinished(new Base.onInitializationFinished() {
                @Override
                public void onInitFinished() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if((myprogress != null) && myprogress.isShowing())
                                    myprogress.dismiss();
                            } catch (final IllegalArgumentException e) {
                                e.printStackTrace();
                                // Handle or log or ignore
                            } catch (final Exception e) {
                                e.printStackTrace();
                                // Handle or log or ignore
                            } finally {
                                myprogress = null;
                            }

                        }
                    });

                    ecr = new ECR(usbService);

                    applicationBase.setOnCtlsCardResultListener(new Base.onCTLSCardResult() {
                        @Override
                        public void onCltsStartUI(String msg) {
                            displayCardInputScreen();
                        }
                    });
                }
            });

            applicationBase.setOnTransEndState(new Base.onTranStartEndState() {
                @Override
                public void onTranStarted() {
                }

                @Override
                public void onTranEnd() {
                }
            });

            setTouchListener();
            return null;
        }

        public void onPostExecute(Void unused) {
            //myprogress.dismiss();
        }
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }
}