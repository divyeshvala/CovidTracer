package com.example.ctssd.Utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import retrofit2.http.DELETE;

public class DatabaseHelper extends SQLiteOpenHelper
{
    private static final String DATABASE = "Database.db";
    private static final String TABLE1 = "table1";
    private static final String TABLE2 = "table2";

    public DatabaseHelper(Context context)
    {
        super(context, DATABASE, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        String table1 = "CREATE TABLE "+ TABLE1 + "(PHONE TEXT PRIMARY KEY, TIME TEXT)";
        String table2 = "CREATE TABLE "+ TABLE2 + "(id INTEGER PRIMARY KEY AUTOINCREMENT, count INTEGER)";
        db.execSQL(table1);
        db.execSQL(table2);
//        db.execSQL("CREATE TABLE table1 (PHONE TEXT PRIMARY KEY, TIME TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        //db.execSQL("DROP TABLE IF EXISTS table1");
        db.execSQL("DROP TABLE IF EXISTS "+TABLE1);
        db.execSQL("DROP TABLE IF EXISTS "+TABLE2);
        onCreate(db);
    }

    public boolean insertData(String phone, String time)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put("PHONE", phone);
        contentValues.put("TIME", time);
        long res = db.insertWithOnConflict("table1", null, contentValues,SQLiteDatabase.CONFLICT_REPLACE);

        if(res == -1)
            return false;
        return true;
    }

    public boolean insertDataTable2(int count)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put("count", count);
        long res = db.insertWithOnConflict("table2", null, contentValues,SQLiteDatabase.CONFLICT_REPLACE);

        if(res == -1)
            return false;
        return true;
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
