package com.example.ctssd.Utils;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Base64;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.ctssd.Activities.Main2Activity;
import com.example.ctssd.Activities.MainActivity;
import com.example.ctssd.R;
import com.example.ctssd.Services.Alarm;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Utilities
{
    private static final String TAG = "Utilities";
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseHelper myDb;
    private ArrayList<UserObject> list = new ArrayList<>();

    private final String key = "aesEncryptionKey";
    private final String initVector = "encryptionIntVec";

    public boolean isTwentyFourHoursOver(Context context)
    {
        myDb = new DatabaseHelper(context);
        SharedPreferences settings = context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        int lastDay = settings.getInt("day", 0);
        int lastMonth = settings.getInt("month", 0);
        int lastYear = settings.getInt("year", 0);
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        Log.i("Utils", "Last date :"+lastDay+"-"+lastMonth+"-"+lastYear);
        Log.i("Utils", "Current date :"+currentDay+"-"+currentMonth+"-"+currentYear);

        if(lastDay==0 || (lastDay == currentDay && lastMonth==currentMonth && lastYear==currentYear))
        {
            Log.i(TAG, "lastday==currentday");
            if(lastDay==0) {
                inserLocalDataZeroes();
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
        Log.i(TAG, "Inside TwentyFourHoursWork");
        SharedPreferences settings = context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        int lastDay = settings.getInt("day", 0);
        int lastMonth = settings.getInt("month", 0);
        int lastYear = settings.getInt("year", 0);
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        Log.i(TAG, "LAST DATE : "+lastDay+", "+lastMonth+", "+lastYear);

        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("day", currentDay);
        editor.putInt("month", currentMonth);
        editor.putInt("year", currentYear);
        editor.apply();

        float BTOnTime = settings.getFloat("totalBTOnTime", 0);
        int preMaxRFC = settings.getInt("maxRiskFromContacts", 0);

        SharedPreferences.Editor ed = settings.edit();
        // update number of days since installation.
        ed.putInt("totalDays", settings.getInt("totalDays", 1)+1);
        ed.putFloat("totalBTOnTime", 0);
        ed.putInt("crowdNo", 0);
        ed.putInt("crowdInstances", 0);
        ed.putInt("contactsTodayPenalty", 0);
        ed.putInt("maxRiskFromContacts", 0);
        ed.putInt("bluetoothPenalty", 0);
        ed.putInt("myRiskIndex", 0);
        ed.putInt("fromContactsRiskMax", 0);
        ed.putInt("fromContactsToday", 0);
        ed.putInt("fromBluetoothOffTime", 0);
        ed.putInt("fromCrowdInstances", 0);
        ed.putInt("bluetoothPenalty",0);
        ed.apply();

        myDb = new DatabaseHelper(context);
        Cursor cursor = myDb.getAllData();
        int countTable1 = 0;
        if(cursor!=null)
        {
            countTable1 = cursor.getCount()  + settings.getInt("contactsTodayPenalty", 0);
            SharedPreferences.Editor edit = settings.edit();
            edit.putInt("contactsTodayPenalty", 0);
            edit.apply();
        }
        while(cursor!=null && cursor.moveToNext())
        {
            myDb.insertDataTable3(lastDay, lastMonth, lastYear, cursor.getString(0), cursor.getString(1), cursor.getString(3));
        }
        Objects.requireNonNull(cursor).close();
        myDb.deleteAllRecords();  //delete all records from table1
        Log.i(TAG, "Data stored now in table3 : "+countTable1);

        myDb.insertDataTable2(countTable1, preRiskIndex, BTOnTime);
        Cursor cursor1 = myDb.getAllDataTable2();
        int countTable2 = 0;
        if(cursor1!=null)
        {
            countTable2 = cursor1.getCount();
            cursor1.close();
        }

        if(countTable2>13)
        {
            myDb.deleteFirstIRecordsTable2(countTable2-13);
        }
        Log.i(TAG, "Records in table2 :"+countTable2);

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
        Log.i(TAG, "15 day old dates :"+day+", "+month+", "+year);
        myDb.delete15DaysOldRecordsTable3(day, month, year);
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
        SharedPreferences settings = context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        String deviceId = settings.getString("myDeviceId", "NA");

        myDb = new DatabaseHelper(context);

        Cursor cursor = myDb.getAllDataTable3();

        DatabaseReference databaseReference = database.getReference("Users"+"/"+ deviceId);
        while(cursor!=null && cursor.moveToNext())
        {
            databaseReference.child(String.valueOf(cursor.getInt(3)))
                    .child(String.valueOf(cursor.getInt(2)))
                    .child(String.valueOf(cursor.getInt(1)))
                    .child(cursor.getString(4)).child("Time").setValue(cursor.getString(5));
            databaseReference.child(String.valueOf(cursor.getInt(3)))
                    .child(String.valueOf(cursor.getInt(2)))
                    .child(String.valueOf(cursor.getInt(1)))
                    .child(cursor.getString(4)).child("Location").setValue(cursor.getString(5));
        }
        Log.i(TAG, "Data uploaded to cloud");
        delete15DayOldCloud();
    }

    private void delete15DayOldCloud()
    {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -15);
        Date dateBefore18Days = cal.getTime();
        cal.setTime(dateBefore18Days);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        database.getReference("Users"+"/"+Main2Activity.myPhoneNumber+"/"+year+"/"+month+"/"+day).removeValue();
    }

    public String encryptAES128(String value)
    {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            byte[] encrypted = cipher.doFinal(value.getBytes());
            return Base64.encodeToString(encrypted, android.util.Base64.DEFAULT);
        }catch (Exception e){
            e.printStackTrace();
        }
        return "null";
    }

    public String decryptAES128(String text)
    {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            byte[] original = cipher.doFinal(Base64.decode(text, Base64.DEFAULT));
            return new String(original);
        } catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return null;
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
        Intent notificationIntent = new Intent(context, Main2Activity.class);
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
        Intent activityIntent = new Intent(context, Main2Activity.class);
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
        Intent activityIntent = new Intent(context, Main2Activity.class);
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
        float totalBTOnTime = settings.getFloat("totalBTOnTime", 0);

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

        if(totalHours-totalBTOnTime>0)
            return (totalHours - totalBTOnTime);
        return 0;
    }

    private void inserLocalDataZeroes()
    {
        myDb.insertDataTable2(0, 0, 0);
        myDb.insertDataTable2(0, 0, 0);
        myDb.insertDataTable2(0, 0, 0);
        myDb.insertDataTable2(0, 0, 0);
        myDb.insertDataTable2(0, 0, 0);
        myDb.insertDataTable2(0, 0, 0);
        myDb.insertDataTable2(0, 0, 0);
        myDb.insertDataTable2(0, 0, 0);
        myDb.insertDataTable2(0, 0, 0);
        myDb.insertDataTable2(0, 0, 0);
        myDb.insertDataTable2(0, 0, 0);
        myDb.insertDataTable2(0, 0, 0);
        myDb.insertDataTable2(0, 0, 0);
//        myDb.insertDataTable2(3, 4);
//        myDb.insertDataTable2(20, 3);
//        myDb.insertDataTable2(12, 5);
//        myDb.insertDataTable2(14, 8);
//        myDb.insertDataTable2(5, 4);
//        myDb.insertDataTable2(8, 9);
//        myDb.insertDataTable2(19, 7);
//        myDb.insertDataTable2(25, 14);
//        myDb.insertDataTable2(7, 15);
//        myDb.insertDataTable2(1, 20);
//        myDb.insertDataTable2(8, 24);
//        myDb.insertDataTable2(2, 21);
//        myDb.insertDataTable2(16, 13);
    }
}
