package com.harshana.wposandroiposapp.UI.Utils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;

public class DialogUtils {
    private static DialogUtils instance = null;

    public static DialogUtils getInstance() {
        if (instance == null)
            instance = new DialogUtils();

        return instance;
    }

    private DialogUtils() {

    }

    public interface onAlertBoxButtonActions {
        void OnPositivePressAction();
        void OnNegativePressAction();
    }

    //generate an alert dialog box
    public void alertDialogBox (Context appContext, String title, String message, String positiveText, String negativeText,
                                       final onAlertBoxButtonActions buttonPressActions) {

        AlertDialog dialog = new AlertDialog.Builder(appContext)
                                .setTitle(title)
                                .setCancelable(false)
                                .setMessage(message)
                                .setPositiveButton(positiveText, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        buttonPressActions.OnPositivePressAction();
                                    }
                                })
                                .setNegativeButton(negativeText, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        buttonPressActions.OnNegativePressAction();
                                    }
                                }).show();
    }
}