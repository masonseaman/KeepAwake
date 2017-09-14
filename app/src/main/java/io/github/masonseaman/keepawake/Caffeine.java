package io.github.masonseaman.keepawake;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by mason on 6/8/17.
 */

public class Caffeine implements Runnable {
    private float mg;
    private DateTime start;
    private DateTime calculatedStart;
    private float updatedValue;
    private CaffeineDatabaseHelper dbHelper;
    private static ReentrantLock lock;
    private boolean firstRun = false;
    private Context context;

    public Caffeine(float mg, DateTime start, Context context, ReentrantLock lock) {
        Log.d("error", "caffeine started");
        this.mg = mg;
        updatedValue = mg;
        this.start = start;
        this.calculatedStart = start;
        this.lock = lock;
        this.context = context;

//        //create database entry
        dbHelper = new CaffeineDatabaseHelper(context);
//        dbHelper.addCaffeine(start, mg);
    }

    @Override
    public void run() {

        if (!firstRun) {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            firstRun = true;
        }

        try {
            while (updatedValue >= 5) {
                Log.d("error", "starting loop\n\n\n\n");

                DateTime now = new DateTime();
                Long timeDiff = new Interval(calculatedStart, now).toDurationMillis();

                updatedValue = (float) (updatedValue * Math.pow((.5), (timeDiff / 18000000.0)));

                lock.lock();
                dbHelper.updateCaffeine(start, updatedValue);
                lock.unlock();

                calculatedStart = now;
                Log.d("refresh", Integer.toString(dbHelper.getRefreshRate()));
                Thread.sleep(dbHelper.getRefreshRate());
            }
            dbHelper.deleteRow(start);

        } catch (InterruptedException e) {
            run();
        }
    }
}