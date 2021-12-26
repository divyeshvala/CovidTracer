package com.example.ctssd.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.example.ctssd.activities.SplashActivity;

import java.util.Calendar;

public class Alarm extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Intent intent2 = new Intent(context, SplashActivity.class);
        intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent2);
    }

    public void setAlarm(Context context){

        // get a Calendar object with current time
        Calendar cal = Calendar.getInstance();
        // add 24 hours +- something to current time. So it becomes tomorrows 7AM.
        int hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int timeLapseInMinutes = (24 - (hr-7))*60 - Calendar.getInstance().get(Calendar.MINUTE);
        cal.add(Calendar.MINUTE, timeLapseInMinutes);
        Intent intent = new Intent(context, Alarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 192837, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get the AlarmManager service
        AlarmManager am = (AlarmManager) context.getSystemService(context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), sender);
    }

    public void cancelAlarm(Context context)
    {
        Intent intent = new Intent(context, Alarm.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}
