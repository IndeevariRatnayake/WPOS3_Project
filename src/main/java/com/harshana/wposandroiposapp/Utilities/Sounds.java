package com.harshana.wposandroiposapp.Utilities;


import android.media.MediaPlayer;

import com.harshana.wposandroiposapp.Base.Base;
import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.Settings.SettingsInterpreter;


public class Sounds extends Base {
    public static Sounds myInstance = null;

    private Sounds()
    {
        initSounds();
    }

    MediaPlayer player = null;

    public static Sounds getInstance() {
        if(myInstance == null)
            myInstance =  new Sounds();

        return myInstance;
    }

    void initSounds()
    {
        player = MediaPlayer.create(appContext,R.raw.swinghint);
    }

    public void playSound() {
        player.start();
        try {Thread.sleep(1000);} catch (Exception ex){}
    }

    public void playCustSound(int id) {
        if (player != null)
            player.release();

        if (SettingsInterpreter.isSpeechOn()) {
            player = MediaPlayer.create(appContext,id);
            player.start();
        }
    }
}
