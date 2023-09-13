package com.harshana.wposandroiposapp.ColorScheme;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Created by harshana_m on 1/14/2019.
 */

public class CLScheme
{
    public static final int SCHEME_DEFAULT = -1;
    public static final int SCHEME_BOC = 0;
    public static final int SCHEME_NTB = 1;
    public static final int SCHEME_SAMPTH = 2;
    public static final int SCHEME_COMMERCIAL = 3;


    //setting index
    public static int WINDOW_BG_CL = 0;
    public static int BUTTON_BG_CL = 1;
    public static int BUTTON_FG_CL = 2;
    public static int TEXTV_BG_CL = 3;
    public static int TEXTV_FG_CL = 4;
    public static int EDITT_BG_CL = 5;
    public static int EDITT_FG_CL = 6;

    public static boolean isThemeEnabled  = true;

    public static int currentScheme = SCHEME_BOC;

    public static int getCurrentColorScheme()
    {
        return currentScheme;
    }

    public static void setCurrentColorScheme(int scheme)
    {
        currentScheme = scheme;
    }

    public static int[][]clSettings = {
            //window back ground color                  // button bg color // button text color      //text box background                      //textbox foreground        //edit text background                                 //edit text fg
            {Color.YELLOW,                              Color.BLACK,        Color.WHITE,            Color.YELLOW,                               Color.BLACK,              Color.parseColor("#CCCC00") ,                  Color.BLACK},          //BOC scheme
            {Color.parseColor("#0080FF"),     Color.BLACK,        Color.BLACK,          Color.parseColor("#0080FF"),       Color.BLACK,              Color.parseColor("#004C99") ,                  Color.BLACK}           //NTB scheme

    };

    public static int getWindowColor()
    {
        return clSettings[currentScheme][WINDOW_BG_CL];
    }

    public static int [] getSchemeSettings(int scheme_code)
    {
        try
        {
            return clSettings[scheme_code];
        }catch (Exception ex)
        {
            return null;
        }
    }



    public static void applyColorScheme(View baseView)
    {
        Drawable curDarable = null;

        if (!isThemeEnabled)
            return;


        int windowColor = 0;
        int buttonBGColor = 0;
        int buttonFGColor = 0;
        int textvBGColor = 0 ;
        int textvFGColor = 0 ;
        int edittBGColor = 0;
        int edittFGColor = 0 ;

        int scheme = CLScheme.getCurrentColorScheme();


        int[] settings = CLScheme.getSchemeSettings(scheme);

        if (settings == null)
            return;

        windowColor = settings[WINDOW_BG_CL];
        buttonBGColor = settings[BUTTON_BG_CL];
        buttonFGColor = settings[BUTTON_FG_CL];
        textvBGColor = settings[TEXTV_BG_CL];
        textvFGColor = settings[TEXTV_FG_CL];
        edittBGColor  = settings[EDITT_BG_CL];
        edittFGColor  = settings[EDITT_FG_CL];

        //apply the settings for all the controls within the window
        View v = baseView;
        RelativeLayout relLayout = null;
        ConstraintLayout consLayout = null;
        GridLayout gridLayout = null;
        LinearLayout linearLayout = null;


        if (v instanceof RelativeLayout)
            relLayout = (RelativeLayout)v;
        else if (v instanceof  ConstraintLayout)
            consLayout = (ConstraintLayout)v;
        else if (v instanceof  GridLayout)
            gridLayout = (GridLayout)v;
        else if (v instanceof LinearLayout)
            linearLayout = (LinearLayout)v;

        int childCount = 0 ;

        if(relLayout != null)
        {
            childCount = relLayout.getChildCount();
            relLayout.setBackgroundColor(windowColor);
        }
        else if (consLayout != null)
        {
            childCount = consLayout.getChildCount();
            consLayout.setBackgroundColor(windowColor);
        }
        else if (gridLayout != null)
        {
            childCount = gridLayout.getChildCount();
            gridLayout.setBackgroundColor(windowColor);
        }
        else if (linearLayout != null)
        {
            childCount = linearLayout.getChildCount();
            linearLayout.setBackgroundColor(windowColor);
        }


        View child = null;

        for (int i = 0 ; i < childCount; i++)
        {
            if (relLayout != null)
                child = relLayout.getChildAt(i);
            else if (consLayout != null)
                child = consLayout.getChildAt(i);
            else if (gridLayout != null)
                child = gridLayout.getChildAt(i);
            else if (linearLayout != null)
                child = linearLayout.getChildAt(i);

            if (child instanceof Button)
            {
                //child.setBackgroundColor(buttonBGColor);
                //((Button) child).setTextColor(buttonFGColor);
            }
            else if (child instanceof EditText)
            {
                ((EditText) child).setTextColor(edittFGColor);
                (child).getBackground().setColorFilter(edittBGColor, PorterDuff.Mode.SRC_ATOP);
            }
            else if (child instanceof TextView)
            {
                child.setBackgroundColor(textvBGColor);
                ((TextView)child).setTextColor(textvFGColor);
            }
            else if (child instanceof ListView)
            {
                child.setBackgroundColor(windowColor);
                ((ListView)child).setSelector(android.R.color.white);
            }
            else if (child instanceof ScrollView)
            {
                child.setBackgroundColor(windowColor);
            }
        }

    }
}
