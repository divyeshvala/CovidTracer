package com.example.ctssd.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.ctssd.model.DailyStat;
import java.util.ArrayList;
import java.util.List;

public class DailyStatDao extends SQLiteOpenHelper implements BaseDao<DailyStat> {

    private static final String TABLE = "contacts";
    private static final String DATABASE = "Database.db";

    public DailyStatDao(Context context) {
        super(context, DATABASE, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String query = "CREATE TABLE "+ TABLE + "(ID INTEGER PRIMARY KEY AUTOINCREMENT, COUNT INTEGER, RISK INTEGER, BTON FLOAT)";
        sqLiteDatabase.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS "+TABLE);
        onCreate(sqLiteDatabase);
    }

    @Override
    public void save(DailyStat object) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put("COUNT", object.getContactsCount());
        contentValues.put("RISK", object.getRisk());
        contentValues.put("BTON", object.getBluetoothOnTime());
        db.insertWithOnConflict("table2", null, contentValues,SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Override
    public List<DailyStat> getAll() {
        List<DailyStat> dailyStats = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery("select * from "+TABLE, null);

        while(cursor!=null && cursor.moveToNext()) {
            dailyStats.add(new DailyStat(cursor.getInt(0), cursor.getInt(1), cursor.getFloat(2)));
        }
        return dailyStats;
    }
}
