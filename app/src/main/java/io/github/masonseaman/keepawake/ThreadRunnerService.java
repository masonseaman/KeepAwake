package io.github.masonseaman.keepawake;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.util.Log;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadRunnerService extends Service {

    public static ReentrantLock lock = new ReentrantLock();
    private ArrayList<Thread> threadIds = new ArrayList();

    public ThreadRunnerService() {
        super();
    }

    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message inputMessage) {
            Log.d("handler","handler run");
            float update = inputMessage.getData().getFloat("value");
            if(update>0) update = 0;
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("error", "thread service started");
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        CaffeineDatabaseHelper dbHelper = new CaffeineDatabaseHelper(this);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        Notification notification = new Notification.Builder(this)
                .setContentTitle("Keep Awake")
                .setContentText("Tracking Caffeine in Background")
                .setSmallIcon(R.drawable.coffee)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1,notification);

        Bundle b = intent.getExtras();

        if(b.containsKey("reboot")){
            //only start the threads from the database if they are not alive
            if(threadIds.size()==0 || !threadIds.get(0).isAlive()) {
                Log.d("restarted", "not alive!");
                startThreadsFromDatabase();
            }
            else {
                Log.d("restarted", "already alive!");
                makeThreadsLoop();
            }

        }

        else if(b.containsKey("changed")){
            makeThreadsLoop();
        }

        else {
            float caffeineAmount = b.getFloat("caffeine");
            DateTime startTime = DateTime.parse(b.getString("startTime"));
            Log.d("error", caffeineAmount + "caffeine");

            //create database entry
            if(dbHelper.addCaffeine(startTime, caffeineAmount)) {
                //create thread and add id to threadid list
                Thread newCaffeine = new Thread(new Caffeine(caffeineAmount, startTime, this, lock));
                threadIds.add(newCaffeine);

                //start thread
                newCaffeine.start();
            }

            return START_NOT_STICKY;
        }
        return START_NOT_STICKY;
    }

    public void makeThreadsLoop(){
        for(Thread id:threadIds){
            id.interrupt();
        }
    }

    public int startThreadsFromDatabase(){
        Log.d("reboot","rebooted");

        CaffeineDatabaseHelper dbHelper = new CaffeineDatabaseHelper(this);

        threadIds.clear();

        HashMap<String, Float> hm = dbHelper.getAndClearCaffeineAmounts();

        Log.d("reboot", hm.toString());

        for(String key : hm.keySet()){
            Log.d("restarted", "caffeine val " + hm.get(key).toString());
            Log.d("restarted", "date " + DateTime.parse(key));
            Thread newCaffeine = new Thread(new Caffeine(hm.get(key), DateTime.parse(key),getApplicationContext(),lock));
            threadIds.add(newCaffeine);
            newCaffeine.start();
        }

        return START_NOT_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
