package com.harshana.wposandroiposapp.Utilities;

import java.util.ArrayDeque;
import java.util.Queue;

public class AutomatedLogQueue
{
    private Queue<String> automatedLogQueue = null;
    private AutomatedLogQueue()
    {
        automatedLogQueue = new ArrayDeque<>();
    }

    private static AutomatedLogQueue instance = null;

    public static AutomatedLogQueue getInstance()
    {
        if (instance == null)
            instance = new AutomatedLogQueue();

        return instance;
    }

    public void addMessageToAutomatedLogQueue(String msg)
    {
        synchronized (this)
        {
            automatedLogQueue.add(msg);
        }
    }

    public String getMessageFromAutomatedLogQueue()
    {
        synchronized (this)
        {
            if (automatedLogQueue.size() > 0)
            {
                String msg = automatedLogQueue.remove();
                return msg;
            }
        }

        return null;
    }

    public void clearQueue()
    {
        synchronized (this)
        {
            automatedLogQueue.clear();
        }
    }
}
