package org.edforge.efdeviceowner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import org.edforge.util.TCONST;

import static org.edforge.util.TCONST.PLUG_CONNECT;
import static org.edforge.util.TCONST.PLUG_DISCONNECT;

/**
 * Created by kevin on 11/6/2018.
 */

public class PlugStatusReceiver extends BroadcastReceiver {

    private static final String TAG = "PlugStatusReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i(TAG, "BATTERY:" + intent.getAction());

//        Toast.makeText(context, intent.getAction(), Toast.LENGTH_SHORT).show();

        if(intent.getAction() == PLUG_CONNECT) {
            Intent launch = new Intent(TCONST.EFOWNER_LAUNCH_INTENT);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(launch);
        }

        if(intent.getAction() == PLUG_DISCONNECT) {
            Intent launch = new Intent(TCONST.EFHOME_LAUNCH_INTENT);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(launch);
        }
    }
}
