package com.harshana.wposandroiposapp.Utilities;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * Created by harshana_m on 11/26/2018.
 */

@SuppressLint("AppCompatCustomView")
public class TypeWriter extends TextView
{
    private CharSequence mText = "";
    private int mIndex = 0 ;
    private int mDelay = 150;
    private int stayCount = 0;

    private Handler mHandler = new Handler();

    public TypeWriter(Context context)
    {
        super(context);
    }

    public TypeWriter(Context context, AttributeSet attr)
    {
        super(context,attr);
    }



    private Runnable charAdder  = new Runnable()
    {
        @Override
        public void run()
        {


            setText(mText.subSequence(0 , mIndex++));
            mHandler.postDelayed(charAdder,mDelay);

            if (mIndex >= mText.length())
            {
                mIndex = mText.length();
                stayCount++;
            }

            if (stayCount > 30)
            {
                stayCount = 0;
                mIndex = 0;
            }

        }
    };

    private boolean shouldIStop = false;

    public void setStopAnimationEndOfString()
    {
        shouldIStop = true;
    }

    public void animateText(CharSequence text)
    {
        mText = text;
        mIndex = 0;
        setText("");

        mHandler.removeCallbacks(charAdder);
        mHandler.postDelayed(charAdder,mDelay);
    }

    public void setAnimDelay(int de)
    {
        mDelay = de;
    }
}
