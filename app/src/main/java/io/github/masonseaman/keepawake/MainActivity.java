package io.github.masonseaman.keepawake;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceManager;
//import android.support.design.widget.FloatingActionButton;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;

import java.text.DecimalFormat;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity {

    private static TextView mgRemaining;

    private CaffeineDatabaseHelper dbHelper;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dbHelper = new CaffeineDatabaseHelper(this);

        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(sharedPref.getBoolean("run", true)){
            new MaterialDialog.Builder(MainActivity.this)
                    .title("Disclaimer")
                    .content("This app calculates caffeine in body based on a half life of 5 hours. " +
                            "This is an estimate and should only be used for entertainment.")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.isCancelled();
                        }
                    })
                    .positiveText("Ok")
                    .show();
            sharedPref.edit().putBoolean("run",false).commit();
        }

        setContentView(R.layout.activity_main);
        JodaTimeAndroid.init(this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mgRemaining = (TextView) findViewById(R.id.mg);

        Thread t = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            DecimalFormat df = new DecimalFormat();
                            df.setMaximumFractionDigits(2);
                            mgRemaining.setText(df.format(dbHelper.getTotal()));
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.start();

        final FloatingActionsMenu fab = (FloatingActionsMenu) findViewById(R.id.menu);

        final FloatingActionButton coffee = (FloatingActionButton) findViewById(R.id.coffee);

        final FloatingActionButton custom = (FloatingActionButton) findViewById(R.id.custom);

        coffee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //call startService with coffee defaults
                startService(v, 150);
            }
        });

        custom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                View x = getLayoutInflater().inflate(R.layout.sample_my_view,null,false);

                //make dialog with other options
                boolean wrapInScrollView = true;
                new MaterialDialog.Builder(MainActivity.this)
                        .title("Input custom amount")
                        .customView(R.layout.sample_my_view,wrapInScrollView)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                EditText input = (EditText)dialog.findViewById(R.id.custom_mg);
                                if(TextUtils.isEmpty(input.getText().toString())||Float.parseFloat(input.getText().toString())>500) {
                                    Snackbar.make(v, "Invalid amount, must be between 0 and 500", Snackbar.LENGTH_SHORT).show();
                                }
                                else {
                                    float inputNum = Float.parseFloat(input.getText().toString());
                                    startService(v, inputNum);
                                }
                                dialog.isCancelled();
                            }
                        })
                        .positiveText("Submit")
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                materialDialog.isCancelled();
                            }
                        })
                        .negativeText("Cancel")
                        .show();
            }
        });

        fab.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                coffee.setVisibility(coffee.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
                custom.setVisibility(custom.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
            }
        });
    }

    public void startService(View view, float caffeine) {
        Intent serviceIntent = new Intent(getApplicationContext(), ThreadRunnerService.class);
        serviceIntent.putExtra("caffeine", caffeine);
        serviceIntent.putExtra("startTime", new DateTime().toString());
        startService(serviceIntent);
        Log.d("error", "fab pressed");
        Snackbar.make(view, "Caffeine added!", Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onPause() {
        super.onPause();

    }


    @Override
    public void onStop() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        sp.edit().putString("end_time", new DateTime().toString()).commit();
        Log.d("stop", "stopped");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void onResume() {
        super.onResume();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.delete_database) {
            new MaterialDialog.Builder(MainActivity.this)
                    .title("Are you sure you want to delete all caffeine?")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                            dbHelper.deleteCaffeine();
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                            materialDialog.dismiss();
                        }
                    })
                    .positiveText("Delete all caffeine")
                    .negativeText("Cancel")
                    .show();
        }
        if (id == R.id.change_refresh_rate){
            new MaterialDialog.Builder(MainActivity.this)
                    .title("Enter refresh rate in minutes between 1 and 720")
                    .customView(R.layout.refresh_rate_change,true)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                            EditText input = (EditText)materialDialog.findViewById(R.id.refresh_rate);
                            int x = -1;
                            try{
                                x = Integer.parseInt(input.getText().toString());
                            }catch (Exception e){

                            }
                            if(TextUtils.isEmpty(input.getText().toString())||x>720||x<=0){

                            }
                            else {
                                if(dbHelper.getTotal()>0) {
                                    Intent intent = new Intent(MainActivity.this, ThreadRunnerService.class);
                                    intent.putExtra("changed", true);
                                    CaffeineDatabaseHelper dbHelper = new CaffeineDatabaseHelper(MainActivity.this);
                                    dbHelper.updateRefreshRate(x*60000);
                                    startService(intent);
                                }
                                else{
                                    CaffeineDatabaseHelper dbHelper = new CaffeineDatabaseHelper(MainActivity.this);
                                    dbHelper.updateRefreshRate(x*60000);
                                }

                            }
                            materialDialog.dismiss();
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                            materialDialog.dismiss();
                        }
                    })
                    .positiveText("Submit refresh rate")
                    .negativeText("Cancel")
                    .show();
        }
        if(id == R.id.menu_refresh){
            if(dbHelper.getTotal()!=0) {
                Intent intent = new Intent(MainActivity.this, ThreadRunnerService.class);
                intent.putExtra("changed", true);
                startService(intent);
                Log.d("refresh rate", Integer.toString(dbHelper.getRefreshRate()));
            }
            Snackbar.make(this.findViewById(R.id.menu_refresh), "Refreshing...", Snackbar.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }
}
