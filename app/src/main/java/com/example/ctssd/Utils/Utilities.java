package com.example.ctssd.Utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;
import com.example.ctssd.Activities.Main2Activity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class Utilities
{
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

        if(lastDay==0 || lastDay == currentDay)
        {
            Log.i("Utilities", "lastday==currentday");
            if(lastDay==0)
                inserLocalDataZeroes();
            return;
        }

        // Upload data to database
        list = getLocalData(context);
        DatabaseReference databaseReference = database.getReference("Users"+"/"+ Main2Activity.myMacAdd+"/"+
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

        while( cursor.moveToNext())
        {
            list.add(new UserObject(cursor.getString(0), cursor.getString(1)));
        }
        cursor.close();
        myDb.deleteAllRecords();
        return list;
    }

    public static boolean isPhoneNoValid(String phone)
    {
        if(phone.length()<10)
            return false;
        for(int i=1; i<phone.length(); i++)
        {
            if((int) phone.charAt(i)> '9')
            {
                return false;
            }
        }
        return true;
    }

    public static void showMessage(Context context, String title, String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.show();
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
