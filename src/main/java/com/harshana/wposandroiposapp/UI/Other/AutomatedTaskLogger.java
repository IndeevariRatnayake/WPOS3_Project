package com.harshana.wposandroiposapp.UI.Other;

import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.harshana.wposandroiposapp.R;
import com.harshana.wposandroiposapp.Utilities.AutomatedLogQueue;


public class AutomatedTaskLogger extends AppCompatActivity
{
    TextView txtLog,txtTaskTitle,txtTaskDesc;

    AutomatedLogQueue automatedLogQueue = null;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_automated_task_logger);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable( new ColorDrawable(getResources().getColor(R.color.colorBlack)));

        ActionBarLayout actionBarLayout = ActionBarLayout.getInstance(this,getResources().getString(R.string.app_name),getResources().getColor(R.color.colorBlack));
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(actionBarLayout.createAndGetActionbarLayoutEx());

        Typeface tp = Typeface.createFromAsset(getAssets(),"digital_font.ttf");

        txtLog  = findViewById(R.id.logData);
        txtLog.setTypeface(tp);

        txtTaskTitle = findViewById(R.id.taskTitle);
        txtTaskDesc  = findViewById(R.id.taskDesc);

        automatedLogQueue = AutomatedLogQueue.getInstance();
        fetchLogAndDisplay();

        String taskName = getIntent().getStringExtra("TASK_NAME");
        String taskDesc = getIntent().getStringExtra("DESC");

        txtTaskTitle.setText(taskName);
        txtTaskDesc.setText(taskDesc);
    }



    String msg;
    private void fetchLogAndDisplay()
    {
        Thread displayThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while (true)
                {
                    msg =  automatedLogQueue.getMessageFromAutomatedLogQueue();
                    if (msg != null)
                    {
                        if (msg.equals("CLOSE_LOGGER"))
                        {
                            Log.d("LOGGER","harshana close called ");
                            automatedLogQueue.clearQueue();
                            finish();
                            break;
                        }


                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                msg += "\n\n";
                                txtLog.append(msg);
                            }
                        });

                    }

                    try
                    {
                        Thread.sleep(500);
                    }catch (Exception ex) {}
                }

            }
        });

        displayThread.start();
    }

}
