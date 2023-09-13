package com.harshana.wposandroiposapp.UI.Other;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.harshana.wposandroiposapp.R;

public class ActionBarLayout
{
    Context context = null;
    String title = "";
    int backgroundColor = 0;

    private static ActionBarLayout  instance = null;

    private ActionBarLayout(Context appContext, String actionBarTitle,int actionBarBackgrounColor)
    {
        context = appContext;
        title = actionBarTitle;
        backgroundColor = actionBarBackgrounColor;
    }


    public static ActionBarLayout getInstance(Context context,String actionBarTitle,int actionBarBackgroundColor)
    {
        if (instance == null)
            instance = new ActionBarLayout(context,actionBarTitle,actionBarBackgroundColor);
        return instance;
    }

    public RelativeLayout createAndGetActionbarLayoutEx()
    {
        RelativeLayout relativeLayout = new RelativeLayout(context);
        relativeLayout.setBackgroundColor(backgroundColor);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);

        relativeLayout.setLayoutParams(params);


        TextView textView = new TextView(context);
        textView.setTextSize(25.0f);
        textView.setTextColor(context.getResources().getColor(R.color.colorPrimaryDark));
        textView.setText(title);
        textView.setBackgroundColor(backgroundColor);
        relativeLayout.addView(textView);

        RelativeLayout.LayoutParams lp =  new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_HORIZONTAL);
        lp.addRule(RelativeLayout.CENTER_VERTICAL);

        textView.setLayoutParams(lp);

        textView.setGravity(Gravity.CENTER);

        return relativeLayout;
    }


    public LinearLayout createAndGetActionbarLayout()
    {
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setBackgroundColor(backgroundColor);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);


        params.weight = 5.0f;
        params.gravity = Gravity.CENTER;

        linearLayout.setLayoutParams(params);


        TextView textView = new TextView(context);
        textView.setTextSize(25.0f);
        textView.setTextColor(context.getResources().getColor(android.R.color.white));
        textView.setText(title);
        textView.setBackgroundColor(backgroundColor);
        linearLayout.addView(textView);

        textView.setGravity(Gravity.CENTER);

        return linearLayout;
    }

}
