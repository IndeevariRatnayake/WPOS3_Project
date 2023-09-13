package com.harshana.wposandroiposapp.Utilities;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class Scheduler
{

    int delay = 0 ;
    Thread shedulerThread = null;
    static  Context context;

    private static Date startedTime = null;
    private static float startedPercentage = 0.0f;

    private static Scheduler myInstance = null;

    public static Scheduler getInstance(Context c, int delay)
    {
        if (myInstance == null)
            myInstance = new Scheduler(c,delay);

        return myInstance;
    }


    //param represent how many times should be polled the battery level per minitue
    private Scheduler(Context c, int latencyInSeconds)
    {
        tasks = new ArrayList<>();
        context  = c;
        delay = latencyInSeconds;
    }



    public void stopRunning()
    {
        isStoppedRunning = true;
    }
    private boolean isStoppedRunning = false;

    class Task
    {
        public ScheduledTask task;
        public Date dateTime;
        public RescheduleType rescheduleType;

        public Task(ScheduledTask _task,Date _dateTime,RescheduleType _rescheduleType)
        {
            task = _task;
            dateTime  = _dateTime;
            rescheduleType = _rescheduleType;
        }
    }

    private List<Task> tasks;

    public interface ScheduledTask
    {
        boolean execute();
    }

    public enum RescheduleType
    {
        RUN_AT_EACH_LOOP,
        HOURLY,
        DAILY
    }
    public void scheduleATask(ScheduledTask task,String time,RescheduleType rescheduleType)
    {
        if (task == null)
            return;

        SimpleDateFormat sdf = new SimpleDateFormat("HHmm");
        try
        {
            Task tempTask = null;
            Date curSetDate = null;

            if (time != null)
            {
                Date dt = sdf.parse(time);
                curSetDate =  Calendar.getInstance().getTime();
                curSetDate.setHours(dt.getHours());
                curSetDate.setMinutes(dt.getMinutes());
            }

            tempTask = new Task(task,curSetDate,rescheduleType);
            tasks.add(tempTask);

        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void makeDelay(long delay)
    {
        try
        {
            Thread.sleep(delay);
        }catch (Exception ex) {}
    }

    long startTime =  0;
    public void startRunning()
    {
        isStoppedRunning = false;
        //start monitoring in a new thread

        shedulerThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                float percentage ;
                long consumedTime = 0 ;

                while(!isStoppedRunning)
                {
                    if ((delay * 1000) > consumedTime)
                    {
                        long newDelay = (delay * 1000) - consumedTime;
                        makeDelay(newDelay);
                    }

                    startTime = System.currentTimeMillis();

                    //run the scheduled tasks
                    if (tasks.size() > 0)
                    {
                        for (Task currentTask : tasks)
                        {
                            try
                            {
                                if (currentTask.dateTime == null && currentTask.rescheduleType == RescheduleType.RUN_AT_EACH_LOOP)
                                {
                                    currentTask.task.execute();
                                }
                                else if (currentTask.dateTime != null && (System.currentTimeMillis() >= currentTask.dateTime.getTime()))
                                {
                                    //re schedule the task
                                    if (currentTask.rescheduleType == RescheduleType.HOURLY) {
                                        long curSetTime = currentTask.dateTime.getTime();
                                        curSetTime += (60 * 60 * 1000);
                                        currentTask.dateTime.setTime(curSetTime);
                                    } else if (currentTask.rescheduleType == RescheduleType.DAILY) {
                                        //add 1 day to the former scheduled date
                                        long curSetTime = currentTask.dateTime.getTime();
                                        curSetTime += (24 * 60 * 60 * 1000);
                                        currentTask.dateTime.setTime(curSetTime);
                                    }
                                    currentTask.task.execute();
                                }
                            }catch (Exception ex)
                            {
                                ex.printStackTrace();
                            }
                        }
                    }

                    //all the tasks have been finished
                    consumedTime = System.currentTimeMillis() - startTime;

                    //the below was commented for later implementation with enhancement
                }
            }
        });

        shedulerThread.start();
    }




}
