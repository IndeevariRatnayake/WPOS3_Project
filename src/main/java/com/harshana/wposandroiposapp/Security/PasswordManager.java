package com.harshana.wposandroiposapp.Security;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.widget.EditText;

public class PasswordManager
{
    private static PasswordManager instance = null;

    private PasswordManager(){}

    public static PasswordManager getInstance()
    {
        if (instance == null)
            instance = new PasswordManager();

        return instance;
    }
    public final static String SUPERVISOR_PASSWORD = "sits810912";


    public  static void capturePassword(final Context context)
    {

        final EditText txtPassword = new EditText(context);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setView(txtPassword);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {

            }
        })
                .setTitle("Password Required")
                .setMessage("Please enter supervisor password")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {

                    }
                })
                .setCancelable(false);


       final AlertDialog alertDialog = builder.create();
       alertDialog.show();

       alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
       {
           @Override
           public void onClick(View v)
           {
               //check for the correct password
               String password =  txtPassword.getText().toString();

               if (password == null || password =="")
               {
                   showAlert(context,"Please enter a valid password");
                   return;
               }

               if (password.equals(SUPERVISOR_PASSWORD))
               {
                   alertDialog.dismiss();
               }
               else
               {
                   showAlert(context,"Wrong Password");
                   txtPassword.setText("");

               }
           }
       });

       alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener()
       {
           @Override
           public void onClick(View v)
           {
               alertDialog.dismiss();
               ((Activity)context).finish();
           }
       });


    }


    public static void showAlert(Context context,String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setCancelable(false)
                .setTitle("Alert")
                .setMessage(message)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });

        builder.show();
    }

}
