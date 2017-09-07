package io.github.masonseaman.keepawake;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.joda.time.DateTime;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by mason on 6/10/17.
 */

public class CaffeineDatabaseHelper extends SQLiteOpenHelper {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "CaffeineDatabase.db";
    public static final String TABLE_NAME = "caffeine";
    public static final String START_TIME = "startTime";
    public static final String CAFFEINE_AMOUNT = "caffeineAmount";

    private static final String DATABASE_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    START_TIME + " TEXT, " +
                    CAFFEINE_AMOUNT + " REAL);";


    public CaffeineDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        onCreate(db);
    }

    public boolean addCaffeine(DateTime startTime, float caffeine) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues(2);
        cv.put(START_TIME, startTime.toString());
        cv.put(CAFFEINE_AMOUNT, caffeine);
        db.insert(TABLE_NAME, null, cv);
        db.close();
        return true;
    }

    public float getTotal() {
        SQLiteDatabase db = this.getReadableDatabase();
        String total = "SELECT sum(" + CAFFEINE_AMOUNT + ") as 'total' " +
                "FROM " + TABLE_NAME + ";";
        //Log.d("database", total);
        Cursor res = db.rawQuery(total, null);
        res.moveToFirst();
        float x = res.getFloat(res.getColumnIndex("total"));
        Log.d("database", "getting total of " + x);
        res.close();
        db.close();
        return x;
    }

    public void deleteRow(DateTime time){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, "where 'startTime'=" + time.toString(), null);
        db.close();
    }

    public HashMap<String,Float> getAndClearCaffeineAmounts(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM caffeine", null);
        res.moveToFirst();
        HashMap<String,Float> hm = new HashMap<String, Float>();
        while(res!=null){
            String s = res.getString(res.getColumnIndex(START_TIME));
            float f = Float.parseFloat(res.getString(res.getColumnIndex(CAFFEINE_AMOUNT)));
            hm.put(s,f);
        }
        db.close();
        return hm;
    }

    public void deleteCaffeine(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME,null,null);
        db.close();
    }

    public void updateCaffeine(DateTime startTime, float caffeine) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues(2);
        cv.put(START_TIME, startTime.toString());
        cv.put(CAFFEINE_AMOUNT, caffeine);
        String where = "'" + START_TIME + "' = '" + startTime + "';";
        //db.update(TABLE_NAME, cv, where, null);
        db.execSQL("UPDATE " + TABLE_NAME + " SET " + CAFFEINE_AMOUNT + "='" + caffeine + "' WHERE " + START_TIME + "='" + startTime.toString()+"'");
        Log.d("database", startTime + " " + caffeine);
        db.close();
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public int getRefreshRate(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM refresh;",null);
        res.moveToFirst();
        int x = res.getInt(res.getColumnIndex("refreshRate"));
        res.close();
        db.close();
        return x;
    }

    public void updateRefreshRate(int x){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE refresh SET refreshRate = '" + x + "'");
        db.close();

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("database", DATABASE_CREATE);
        db.execSQL(DATABASE_CREATE);
        db.execSQL("CREATE TABLE refresh (refreshRate REAL)");
        ContentValues cv = new ContentValues();
        cv.put("refreshRate", 900000);
        db.insert("refresh", null, cv);
        Log.d("database", "inserted");
    }
}