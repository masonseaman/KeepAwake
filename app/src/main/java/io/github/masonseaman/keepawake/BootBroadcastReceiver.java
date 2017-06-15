package io.github.masonseaman.keepawake;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import org.joda.time.DateTime;

/**
 * Created by mason on 6/13/17.
 */

public class BootBroadcastReceiver extends BroadcastReceiver {
    @Override

    public void onReceive(Context context, Intent intent) {
        Intent newIntent = new Intent(context,ThreadRunnerService.class);
        newIntent.putExtra("reboot", true);
        context.startService(newIntent);
    }
}
