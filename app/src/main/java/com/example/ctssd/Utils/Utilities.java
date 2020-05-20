package com.example.ctssd.Utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;
import com.example.ctssd.Services.BackgroundService;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class Utilities
{
    private static final String TAG = "Utilities";
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseHelper myDb;
    private ArrayList<UserObject> list = new ArrayList<>();

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

        if(lastDay==0 || (lastDay == currentDay && lastMonth==currentMonth && lastYear==currentYear))
        {
            Log.i(TAG, "lastday==currentday");
            if(lastDay==0)
                inserLocalDataZeroes();
            return false;
        }
        return true;
    }

    public void TwentyFourHoursWork(Context context)
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
        editor.putInt("month", currentMonth+1);   // TODO: remove +1 if needed.
        editor.putInt("year", currentYear);
        editor.apply();

        SharedPreferences.Editor ed = settings.edit();
        // update number of days since installation.
        ed.putInt("totalDays", settings.getInt("totalDays", 1)+1);
        ed.apply();

        myDb = new DatabaseHelper(context);
        Cursor cursor = myDb.getAllData();
        int countTable1 = 0;
        if(cursor!=null)
        {
            countTable1 = cursor.getCount();
        }
        while(cursor!=null && cursor.moveToNext())
        {
            myDb.insertDataTable3(lastDay, lastMonth, lastYear, cursor.getString(0));
        }
        Objects.requireNonNull(cursor).close();
        myDb.deleteAllRecords();  //delete all records from table1
        Log.i(TAG, "Data stored now in table3 : "+countTable1);

        myDb.insertDataTable2(countTable1);
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

        delete15DayOldDataFromTable3();   // Delete 15 days old data from table3
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
        SharedPreferences settings = context.getSharedPreferences("MySharedPref", Context.MODE_PRIVATE);
        float totalBTOnTime = settings.getFloat("totalBTOnTime", 0);

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
            Log.i(TAG, "getTotalBluetoothOffTime: Same day. totalHours :"+totalHours);
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
        editor.putFloat("totalBTOnTime", totalBTOnTime);
        editor.apply();

        Log.i(TAG, "TotalBtOnTime :"+totalBTOnTime);

        if(totalHours-totalBTOnTime>0)
            return (totalHours - totalBTOnTime);
        return 0;
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
