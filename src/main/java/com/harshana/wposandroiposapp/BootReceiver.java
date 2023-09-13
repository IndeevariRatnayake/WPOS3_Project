package com.harshana.wposandroiposapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


public class BootReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d("ysh", "onReceive: ");
        if(intent.getAction().equals("android.intent.action.BOOT_COMPLETED")){// boot  
            Log.d("ysh", "BOOT_COMPLETED: ");
            Intent intent2 = new Intent(context, MainActivity.class);
            //☆ 如果在广播里面开启Activity 要设置一个任务栈环境
            intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent2);
        }
    }
}
