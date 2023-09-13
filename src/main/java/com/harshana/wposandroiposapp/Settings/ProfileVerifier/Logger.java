package com.harshana.wposandroiposapp.Settings.ProfileVerifier;

import android.content.Context;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class Logger
{
    private Context context;
    private static final String LOGFILE = "profile_verifier_logger.txt";
    private File logFile  = null;
    BufferedWriter bufferedWriter = null;

    private static Logger instance = null;

    public static Logger getInstance(Context context)
    {
        instance = instance;
        if (instance == null)
            instance = new Logger(context);

        return instance;
    }

    private Logger(Context _context)
    {
        //initialize the log file
        context = _context;
    }

    public void init()
    {
        String path = context.getFilesDir().toString();

        try
        {
            logFile = new File(path + "/" + LOGFILE);
            bufferedWriter = new BufferedWriter(new FileWriter(logFile));
        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public void ex(Exception ex)
    {
        writeLine(ex.getMessage());
    }

    public void writeLine(String line)
    {
        try
        {
            line += "\r\n";
            bufferedWriter.write(line);
        }catch (Exception   ex)
        {
            ex.printStackTrace();
        }

    }
    public enum StatusTag
    {
        SUCCESS,
        FAILED,
        WARNING,
        AMBIGUOUS
    }

    public void log(String data,StatusTag tag)
    {
        String tagS = "";

        if (tag == StatusTag.SUCCESS)
            tagS = "SUCCESS";
        else if (tag == StatusTag.FAILED)
            tagS  = "FAILED";
        else if (tag == StatusTag.WARNING)
            tagS = "WARNING";
        else if (tag == StatusTag.AMBIGUOUS)
            tagS = "AMBIGUOUS";

        if (tag == StatusTag.FAILED)
            writeLine("-----------------------------------------------------------------------------------------------------");
        data  = tagS + " == " + data ;
        writeLine(data);

        if (tag == StatusTag.FAILED)
            writeLine("-----------------------------------------------------------------------------------------------------");


    }

    public void deInit()
    {
        try
        {
            bufferedWriter.flush();
            bufferedWriter.close();
        }catch (Exception ex) {}

    }
}
