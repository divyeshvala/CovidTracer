package com.example.ctssd.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.ctssd.model.Contact;
import java.util.ArrayList;
import java.util.List;

/**
 * To read and write the data of people the user has contacted.
 * It contains Phone, RiskIndex, Time, Location of the person this user
 * came into contact with.
 */
public class ContactDao extends SQLiteOpenHelper implements BaseDao<Contact> {

    private static final String TABLE = "table1";
    private static final String DATABASE = "Database.db";

    public ContactDao(Context context) {
        super(context, DATABASE, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        String query = "CREATE TABLE "+ TABLE + " (PHONE TEXT PRIMARY KEY, TIME TEXT, RISK INTEGER, LOCATION TEXT)";
        sqLiteDatabase.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS "+TABLE);
        onCreate(sqLiteDatabase);
    }

    @Override
    public void save(Contact object) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues contentValues = new ContentValues();
        contentValues.put("PHONE", object.getPhone());
        contentValues.put("TIME", object.getTime());
        contentValues.put("RISK", object.getRisk());
        contentValues.put("LOCATION", object.getLocation());
        db.insertWithOnConflict(TABLE, null, contentValues,SQLiteDatabase.CONFLICT_REPLACE);
    }

    public Contact findById(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from "+TABLE+" where PHONE="+id, null);
        if(cursor!=null)
            return new Contact(cursor.getString(0), cursor.getString(1), cursor.getInt(2),
                    cursor.getString(3));

        return null;
    }

    @Override
    public List<Contact> getAll() {
        List<Contact> contacts = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.rawQuery("select * from "+TABLE, null);

        while(cursor!=null && cursor.moveToNext()) {
            contacts.add(new Contact(cursor.getString(0), cursor.getString(1), cursor.getInt(2),
                    cursor.getString(3)));
        }
        return contacts;
    }

    public void deleteAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from "+ TABLE);
    }

    public int getCount() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from "+TABLE, null);
        if(cursor!=null)
            return cursor.getCount();
        return 0;
    }
}
