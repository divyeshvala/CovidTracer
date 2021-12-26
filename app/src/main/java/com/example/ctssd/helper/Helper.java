package com.example.ctssd.helper;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.example.ctssd.activities.MainActivity;
import com.example.ctssd.R;
import com.example.ctssd.dao.ContactDao;
import com.example.ctssd.dao.ContactHistoryDao;
import com.example.ctssd.dao.DailyStatDao;
import com.example.ctssd.model.Contact;
import com.example.ctssd.model.DailyStat;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Helper
{
    private static final String TAG = "Helper";
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private ContactDao contactDao;
    private ContactHistoryDao contactHistoryDao;
    private DailyStatDao dailyStatDao;

    public boolean isTwentyFourHoursOver(Context context)
    {
        contactDao = new ContactDao(context);
        dailyStatDao = new DailyStatDao(context);
        contactHistoryDao = new ContactHistoryDao(context);

        SharedPreferences settings = context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        int lastDay = settings.getInt("day", 0);
        int lastMonth = settings.getInt("month", 0);
        int lastYear = settings.getInt("year", 0);
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        if(lastDay==0 || (lastDay == currentDay && lastMonth==currentMonth && lastYear==currentYear))
        {
            if(lastDay==0) {
                insertDummyStats();
                SharedPreferences.Editor editor = settings.edit();
                editor.putInt("day", currentDay);
                editor.putInt("month", currentMonth);
                editor.putInt("year", currentYear);
                editor.apply();
            }
            return false;
        }
        return true;
    }

    public void TwentyFourHoursWork(Context context, int preRiskIndex)
    {
        SharedPreferences settings = context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("day", currentDay);
        editor.putInt("month", currentMonth);
        editor.putInt("year", currentYear);
        editor.apply();

        float BTOffTime = settings.getFloat("totalBTOnTime", 0);

        SharedPreferences.Editor ed = settings.edit();
        // update number of days since installation.
        ed.putInt("totalDays", settings.getInt("totalDays", 1)+1);
        ed.putFloat("totalBTOffTime", 0);
        ed.putInt("crowdNo", 0);
        ed.putInt("crowdInstances", 0);
        ed.putInt("contactsTodayPenalty", 0);
        ed.putInt("maxRiskFromContacts", 0);
        ed.putInt("myRiskIndex", 0);
        ed.putInt("fromContactsRiskMax", 0);
        ed.putInt("fromContactsToday", 0);
        ed.putInt("fromBluetoothOffTime", 0);
        ed.putInt("fromCrowdInstances", 0);
        ed.apply();

        int contactsCount = contactDao.getCount() + settings.getInt("contactsTodayPenalty", 0);
        SharedPreferences.Editor edit = settings.edit();
        edit.putInt("contactsTodayPenalty", 0);
        edit.apply();

        List<Contact> contacts = contactDao.getAll();
        for(Contact contact : contacts)
        {
            contactHistoryDao.save(contact);
        }
        contactDao.deleteAll();  //delete all records from contacts table

        dailyStatDao.save(new DailyStat(contactsCount, preRiskIndex, BTOffTime));

        int dailyStatCount = dailyStatDao.getCount();
        if(dailyStatCount>13)
        {
            dailyStatDao.deleteFirstNRecords(dailyStatCount-13);
        }

        delete15DayOldDataFromTable3();   // Delete 15 days old data from table

        System.exit(0);

    }

    private void delete15DayOldDataFromTable3()
    {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -15);
        Date dateBefore18Days = cal.getTime();
        cal.setTime(dateBefore18Days);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        contactHistoryDao.delete15DaysOldRecords(day, month, year);
    }

    public boolean isInternetAvailable(Context context)
    {
        boolean connected = false;
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            //we are connected to a network
            connected = true;
        }
        Log.i(TAG, "Internet :"+connected);
        return connected;
    }

    public void uploadDataToCloud(Context context)
    {
//        SharedPreferences settings = context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
//        String deviceId = settings.getString("myDeviceId", "NA");
//
//        myDb = new DatabaseHelper(context);
//
//        Cursor cursor = myDb.getAllDataTable3();
//
//        DatabaseReference databaseReference = database.getReference("Users"+"/"+ deviceId);
//        while(cursor!=null && cursor.moveToNext())
//        {
//            databaseReference.child(String.valueOf(cursor.getInt(3)))
//                    .child(String.valueOf(cursor.getInt(2)))
//                    .child(String.valueOf(cursor.getInt(1)))
//                    .child(cursor.getString(4)).child("Time").setValue(cursor.getString(5));
//            databaseReference.child(String.valueOf(cursor.getInt(3)))
//                    .child(String.valueOf(cursor.getInt(2)))
//                    .child(String.valueOf(cursor.getInt(1)))
//                    .child(cursor.getString(4)).child("Location").setValue(cursor.getString(6));
//        }
//        Log.i(TAG, "Data uploaded to cloud");
        delete15DayOldCloud();
    }

    private void delete15DayOldCloud()
    {
//        Calendar cal = Calendar.getInstance();
//        cal.add(Calendar.DATE, -15);
//        Date dateBefore18Days = cal.getTime();
//        cal.setTime(dateBefore18Days);
//        int year = cal.get(Calendar.YEAR);
//        int month = cal.get(Calendar.MONTH);
//        int day = cal.get(Calendar.DAY_OF_MONTH);
//        database.getReference("Users"+"/"+ MainActivity.myDeviceId+"/"+year+"/"+month+"/"+day).removeValue();
    }

    public static void showMessage(Context context, String title, String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
    }

    private void createANotificationChannel(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "statsUpdate",
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public void sendStatsUpdateNotification(Context context, int riskFactor, int contactsToday)
    {
        createANotificationChannel(context);
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                112, notificationIntent, 0);

        RemoteViews contentView = new RemoteViews(context.getPackageName(), R.layout.stats_notif_layout);
        contentView.setTextViewText(R.id.id_notif_riskFactor, String.valueOf(riskFactor)+"%");
        contentView.setTextViewText(R.id.id_notif_contactsToday, String.valueOf(contactsToday));

        Notification notification = new NotificationCompat.Builder(context, "statsUpdate")
                //.setContentTitle("Covid Tracer")
                .setSmallIcon(R.drawable.ic_transfer_within_a_station_black_24dp)
                .setTicker("Stats")
                .setContentIntent(pendingIntent)
                .setContent(contentView)
                //.setStyle(new NotificationCompat.BigTextStyle().bigText("App is running in background.\n"))
                //.setSmallIcon(R.drawable.icon_state)
                //.setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(112, notification);
    }

    public void sendTempNotification(Context context, String title, String text, int notifId, String channelId)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            CharSequence name = "temp";
            String description = "temp notification";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        Intent activityIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                0, activityIntent, 0);

        Notification notification = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_bluetooth_black_24dp)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setColor(Color.BLUE)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notifId, notification);
    }

    public void sendNotifToTurnOnBT(Context context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            CharSequence name = "Bluetooth";
            String description = "For turning on bluetooth";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("bluetoothC", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        Intent activityIntent = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                0, activityIntent, 0);

        Notification notification = new NotificationCompat.Builder(context, "bluetoothC")
                .setSmallIcon(R.drawable.ic_bluetooth_black_24dp)
                .setContentTitle("Your Bluetooth is off.")
                .setContentText("Please turn on bluetooth for your own safety.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setColor(Color.BLUE)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1245, notification);
    }

    public float getTotalBluetoothOffTime(Context context)
    {
        SharedPreferences settings = context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        float totalBTOffTime = settings.getFloat("totalBTOffTime", 0);

        Calendar endTime = Calendar.getInstance();
        Calendar c1 = Calendar.getInstance();
        c1.set(Calendar.MONTH, settings.getInt("startingMonth", c1.get(Calendar.MONTH)));
        c1.set(Calendar.DAY_OF_MONTH, settings.getInt("startingDay", c1.get(Calendar.DAY_OF_MONTH)));
        c1.set(Calendar.YEAR, settings.getInt("startingYear", c1.get(Calendar.YEAR)));

        float totalHours=0;
        if(endTime.get(Calendar.DAY_OF_MONTH)==c1.get(Calendar.DAY_OF_MONTH) && endTime.get(Calendar.MONTH)==c1.get(Calendar.MONTH))
        {
            totalHours = endTime.get(Calendar.HOUR_OF_DAY)-settings.getInt("startingHour", c1.get(Calendar.HOUR_OF_DAY));
            Log.i(TAG, "getTotalBluetoothOffTime: Same day. totalHours :"+totalHours);
        }
        else
        {
            int temp = endTime.get(Calendar.HOUR_OF_DAY)-7;
            if(temp>0)
                totalHours = temp;
        }

        if(totalHours-totalBTOffTime>0)
            return (totalHours - totalBTOffTime);
        return 0;
    }


    private void insertDummyStats()
    {
        for(int i=0; i<13; i++)
        {
            dailyStatDao.save(new DailyStat(0,0,0));
        }
    }
}
