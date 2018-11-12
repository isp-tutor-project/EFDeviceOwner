package org.edforge.efdeviceowner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.edforge.util.TCONST;

import static org.edforge.util.TCONST.PLUG_CONNECT;
import static org.edforge.util.TCONST.PLUG_DISCONNECT;

/**
 * Created by kevin on 11/6/2018.
 */

public class PlugStatusReceiver extends BroadcastReceiver {

    private static final String TAG               = "PlugStatusReceiver";
    public static final String PLUG_PREFS         = "plug_prefs";
    public static final String PLUG_CONNECT_STATE = "plug_conn";

    private SharedPreferences mSharedPrefs;
    private LocalBroadcastManager bManager;

//    public PlugStatusReceiver() {
//    }

    @Override
    public void onReceive(Context context, Intent intent) {

        // Capture the local broadcast manager
        bManager = LocalBroadcastManager.getInstance(context);

        Log.i(TAG, "BATTERY:" + intent.getAction());

        mSharedPrefs = context.getSharedPreferences(PLUG_PREFS, Context.MODE_PRIVATE);
        
        if(intent.getAction() == PLUG_CONNECT) {
            showToast(context, "Plug Connected");
            markPlugConnected(true);
        }

        if(intent.getAction() == PLUG_DISCONNECT) {
            showToast(context, "Plug Disconnected");
            markPlugConnected(false);
            broadcast(TCONST.EFHOME_STARTER_INTENT);
        }
    }

    public void broadcast(String Action) {

        Intent msg = new Intent(Action);
        bManager.sendBroadcast(msg);
    }

    void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    private boolean isPlugConnected() {

        return mSharedPrefs.getBoolean(PLUG_CONNECT_STATE, false);
    }
    private void markPlugConnected(boolean status) {

        mSharedPrefs.edit().putBoolean(PLUG_CONNECT_STATE, status).commit();
    }


}
