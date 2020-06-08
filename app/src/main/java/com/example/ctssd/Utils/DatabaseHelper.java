package com.example.ctssd.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.Objects;

public class DatabaseHelper extends SQLiteOpenHelper
{
    private static final String DATABASE = "Database.db";
    private static final String TABLE0 = "table0";
    private static final String TABLE1 = "table1";
    private static final String TABLE2 = "table2";
    private static final String TABLE3 = "table3";
    private static final String TABLE4 = "table4";

    public DatabaseHelper(Context context)
    {
        super(context, DATABASE, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        String table0 = "CREATE TABLE "+ TABLE0 + "(PHONE TEXT PRIMARY KEY, TIME TEXT, RISK INTEGER)";
        String table1 = "CREATE TABLE "+ TABLE1 + "(PHONE TEXT PRIMARY KEY, TIME TEXT, RISK INTEGER, LOCATION TEXT)";
        String table2 = "CREATE TABLE "+ TABLE2 + "(id INTEGER PRIMARY KEY AUTOINCREMENT, count INTEGER, RISK INTEGER, BTON FLOAT)";
        String table4 = "CREATE TABLE "+ TABLE4 + "(id INTEGER PRIMARY KEY AUTOINCREMENT, DATE TEXT, TIME TEXT, LOC TEXT)";
        String table3 = "CREATE TABLE "+ TABLE3 + "(id INTEGER PRIMARY KEY AUTOINCREMENT, DAY INTEGER, MONTH INTEGER, YEAR INTEGER, PHONE TEXT, TIME TEXT)";

        db.execSQL(table0);
        db.execSQL(table1);
        db.execSQL(table2);
        db.execSQL(table3);
        db.execSQL(table4);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE0);
        db.execSQL("DROP TABLE IF EXISTS "+TABLE1);
        db.execSQL("DROP TABLE IF EXISTS "+TABLE2);
        db.execSQL("DROP TABLE IF EXISTS "+TABLE3);
        db.execSQL("DROP TABLE IF EXISTS "+TABLE4);
        onCreate(db);
    }

    public void insertDataTable0(String phone, String time, int riskIndex)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put("PHONE", phone);
        contentValues.put("TIME", time);
        contentValues.put("RISK", riskIndex);
        db.insertWithOnConflict("table0", null, contentValues,SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void deleteAllRecordsTable0()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from table0");
    }


    public void insertData(String phone, String time, int riskIndex, String location)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put("PHONE", phone);
        contentValues.put("TIME", time);
        contentValues.put("RISK", riskIndex);
        contentValues.put("LOCATION", location);
        db.insertWithOnConflict("table1", null, contentValues,SQLiteDatabase.CONFLICT_REPLACE);
    }

    public int getRiskOfContact(String phone)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery("select * from table1", null);

        while(cursor!=null && cursor.moveToNext())
        {
            if(cursor.getString(0).equals(phone))
            {
                int i = cursor.getInt(2);
                cursor.close();
                return i;
            }
        }
        Objects.requireNonNull(cursor).close();
        return -1;
    }

    public void insertDataTable2(int count, int risk, float BTonTime)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put("count", count);
        contentValues.put("risk", risk);
        contentValues.put("BTON", BTonTime);
        db.insertWithOnConflict("table2", null, contentValues,SQLiteDatabase.CONFLICT_REPLACE);

    }

    public void insertDataTable3(int day, int month, int year, String phone, String time)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put("DAY", day);
        contentValues.put("MONTH", month);
        contentValues.put("YEAR", year);
        contentValues.put("PHONE", phone);
        contentValues.put("TIME", time);
        db.insert("table3", null, contentValues);
    }

    public void insertDataTable4(String date, String time, String location)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put("DATE", date);
        contentValues.put("TIME", time);
        contentValues.put("LOC", location);
        db.insert("table4", null, contentValues);
    }

    public void deleteAllRecords()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from table1");
    }

    public void deleteFirstIRecordsTable2(int i)
    {
        // delete 14 days old data
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from table2 where id in (select id from table2 order by id LIMIT "+i+");");
    }

    public void delete15DaysOldRecordsTable3(int day, int month, int year)
    {
        // delete 15 days old data
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from table3 where DAY="+day+" and MONTH="+month+" and YEAR="+year +";");
    }

    public Cursor getAllData()
    {
        SQLiteDatabase db = this.getWritableDatabase();

        return db.rawQuery("select * from table1", null);
    }

    public Cursor getAllDataTable2()
    {
        SQLiteDatabase db = this.getWritableDatabase();

        return db.rawQuery("select * from table2", null);

    }

    public Cursor getAllDataTable3()
    {
        SQLiteDatabase db = this.getWritableDatabase();

        return db.rawQuery("select * from table3", null);

    }

    /*
    public boolean updateData(String id, String name, String surname, String marks)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put("ID", id);
        contentValues.put("NAME", name);

        db.update("table1",contentValues, "id = ?", new String[] { id });

        return true;
    }

    public int deleteData(String id)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        return db.delete("table1", "id = ?", new String[] { id });
    }
     */
}
