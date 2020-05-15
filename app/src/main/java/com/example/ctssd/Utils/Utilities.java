package com.example.ctssd.Utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;
import com.example.ctssd.Activities.Main2Activity;
import com.example.ctssd.Services.BackgroundService;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Utilities
{
    private static final String TAG = "Utilities";
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseHelper myDb;
    private ArrayList<UserObject> list = new ArrayList<>();

    public void uploadDataToCloud(Context context)
    {
        myDb = new DatabaseHelper(context);
        SharedPreferences settings = context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        int lastDay = settings.getInt("day", 0);
        int lastMonth = settings.getInt("month", 0);
        int lastYear = settings.getInt("year", 0);
        int currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        int currentMonth = Calendar.getInstance().get(Calendar.MONTH);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("day", currentDay);
        editor.putInt("month", currentMonth);
        editor.putInt("year", currentYear);
        editor.apply();

        if(lastDay==0 || lastDay == currentDay )
        {
            Log.i("Utilities", "lastday==currentday");
            if(lastDay==0)
                inserLocalDataZeroes();
            return;
        }
        // update preFromContactsToday
        SharedPreferences.Editor ed = settings.edit();
        ed.putInt("preFromContactsToday", settings.getInt("preFromContactsToday", 0)+settings.getInt("todaysFromContactsToday", 0));

        // update number of days since installation.
        ed.putInt("totalDays", settings.getInt("totalDays", 1)+1);
        ed.apply();

        // Upload data to database
        list = getLocalData(context);
        DatabaseReference databaseReference = database.getReference("Users"+"/"+ Main2Activity.myPhoneNumber+"/"+
                lastYear+"/"+lastMonth+"/"+lastDay);
        for (UserObject object : list)
        {
            databaseReference.child(object.getPhone()).setValue(object.getTime());
        }
        Log.i("Utilities", "Data uploaded to cloud");

        int countTable1 = list.size();
        myDb.insertDataTable2(countTable1);
        Cursor cursor = myDb.getAllDataTable2();
        int countTable2 = cursor.getCount();
        cursor.close();
        if(countTable2>13)
        {
            myDb.deleteFirstIRecordsTable2(countTable2-13);
        }
        Log.i("Records after deleting ", myDb.getAllDataTable2().getCount()+"");
        delete18DayOldCloud();   // Delete 18 days old data from cloud
    }

    private void delete18DayOldCloud()
    {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -18);
        Date dateBefore18Days = cal.getTime();
        cal.setTime(dateBefore18Days);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        database.getReference("Users"+"/"+Main2Activity.myMacAdd+"/"+year+"/"+month+"/"+day).removeValue();
    }

    private ArrayList<UserObject> getLocalData(Context context)
    {
        myDb = new DatabaseHelper(context);
        Cursor cursor = myDb.getAllData();

        if(cursor==null || cursor.getCount()==0)
        {
            return list;
        }

        while(cursor.moveToNext())
        {
            list.add(new UserObject(cursor.getString(0), cursor.getString(1)));
        }
        cursor.close();
        myDb.deleteAllRecords();
        return list;
    }

    public static void showMessage(Context context, String title, String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
    }

    public float getTotalBluetoothOffTime(Context context)
    {
        float currentBluetoothTime = getCurrentBTOnTime();
        SharedPreferences settings = context.getSharedPreferences("MySharedPref", context.MODE_PRIVATE);
        float prevBTOnTime = settings.getFloat("bluetoothTime", 0);
        float totalBTOnTime =  prevBTOnTime+ currentBluetoothTime;

        Calendar endTime = Calendar.getInstance();
        Calendar c1 = Calendar.getInstance();
        c1.set(Calendar.MONTH, settings.getInt("startingMonth", c1.get(Calendar.MONTH)));
        c1.set(Calendar.DAY_OF_MONTH, settings.getInt("startingDay", c1.get(Calendar.DAY_OF_MONTH)));
        c1.set(Calendar.YEAR, settings.getInt("startingYear", c1.get(Calendar.YEAR)));
        c1.set(Calendar.HOUR_OF_DAY, settings.getInt("startingHour", c1.get(Calendar.HOUR_OF_DAY)));
        c1.set(Calendar.MINUTE, settings.getInt("startingMinute", c1.get(Calendar.MINUTE)));

        float totalHours;
        if(endTime.get(Calendar.DAY_OF_MONTH)==c1.get(Calendar.DAY_OF_MONTH) && endTime.get(Calendar.MONTH)==c1.get(Calendar.MONTH))
        {
            totalHours = endTime.get(Calendar.HOUR_OF_DAY)-c1.get(Calendar.HOUR_OF_DAY);
            Log.i(TAG, "Same day. totalHours :"+totalHours);
        }
        else
        {
            int startDayHours = 23 - c1.get(Calendar.HOUR_OF_DAY);
            int endDayHours = endTime.get(Calendar.HOUR_OF_DAY)-6;
            c1.set(Calendar.HOUR_OF_DAY, 23);
            endTime.set(Calendar.HOUR_OF_DAY, 1);
            int totaldays = (int) TimeUnit.DAYS.convert(endTime.getTime().getTime()-c1.getTime().getTime(), TimeUnit.MILLISECONDS);
            totalHours = startDayHours + endDayHours + totaldays*17;
        }

        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat("bluetoothTime", totalBTOnTime);
        editor.apply();
        BackgroundService.startTime = Calendar.getInstance();

        Log.i(TAG, "TotalBtOnTime :"+totalBTOnTime);

        if(totalHours-totalBTOnTime>0)
            return (totalHours - totalBTOnTime);
        return 0;
    }

    private float getCurrentBTOnTime()
    {
        Calendar startTime = BackgroundService.startTime;
        Calendar endTime = Calendar.getInstance();

        if(startTime==null)
            return 0;

        float currentBluetoothTime=0;

        if(startTime.get(Calendar.HOUR_OF_DAY)<6)
        {
            currentBluetoothTime = 0;
            if(endTime.get(Calendar.HOUR_OF_DAY)-6 >= 0)
            {
                currentBluetoothTime += endTime.get(Calendar.HOUR_OF_DAY)-6;
                currentBluetoothTime += endTime.get(Calendar.MINUTE)/60.0 ;
            }
            Log.i(TAG, "CurrentBTTime1 :"+currentBluetoothTime);
        }
        else if(endTime.get(Calendar.HOUR_OF_DAY)==23)
        {
            currentBluetoothTime = 0;
            if( 11-startTime.get(Calendar.HOUR_OF_DAY) > 0)
            {
                currentBluetoothTime += 23-startTime.get(Calendar.HOUR_OF_DAY)-1;
                currentBluetoothTime += (60-startTime.get(Calendar.MINUTE))/60.0 ;
            }
            Log.i(TAG, "CurrentBTTime1 :"+currentBluetoothTime);
        }
        else
        {
            currentBluetoothTime = (float) ( endTime.getTime().getTime()-startTime.getTime().getTime())/3600000;
            Log.i(TAG, "CurrentBTTime3 :"+currentBluetoothTime);
        }
        return currentBluetoothTime;
    }

    private void inserLocalDataZeroes()
    {
        myDb.insertDataTable2(0);
        myDb.insertDataTable2(0);
        myDb.insertDataTable2(0);
        myDb.insertDataTable2(0);
        myDb.insertDataTable2(0);
        myDb.insertDataTable2(0);
        myDb.insertDataTable2(0);
        myDb.insertDataTable2(0);
        myDb.insertDataTable2(0);
        myDb.insertDataTable2(0);
        myDb.insertDataTable2(0);
        myDb.insertDataTable2(0);
        myDb.insertDataTable2(0);
    }
}
