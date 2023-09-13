package com.harshana.wposandroiposapp.Base;

/**
 * Created by harshana_m on 10/29/2018.
 */

public class WaitTimer extends Thread {
    int timeOut = 0 ;
    int timerCount = 0 ;
    private static int THREAD_SLEEP = 50;
    public boolean callTimeoutListener = true;

    public WaitTimer(int timeout)
    {
        timeOut = timeout;
    }

    public void stopTimer() {
        callTimeoutListener = false;
        timerCount = (timeOut * 1000);
    }

    public void run() {
        while (true) {
            timerCount += THREAD_SLEEP;
            if (timerCount >= (timeOut * 1000))
                break;

            try { Thread.sleep(THREAD_SLEEP);} catch (Exception ex){}

            float pers = ((float)timerCount / (float)(timeOut * 1000)) * 100;

            if (listenerTick != null)
                listenerTick.onTimerTick((int)pers);
        }

        if ((callTimeoutListener) && (listener != null))
            listener.onTimeOut();
    }

    private OnTimeOutListener listener = null;

    public interface OnTimeOutListener {
        void onTimeOut();
    }

    public void setOnTimeOutListener(OnTimeOutListener setListener)
    {
        listener = setListener;
    }

    private OnTimerTickListener listenerTick = null;

    public void setOnTimerTickListener (OnTimerTickListener l)
    {
        listenerTick = l;
    }

    public interface OnTimerTickListener {
        void onTimerTick(int tick);
    }
}