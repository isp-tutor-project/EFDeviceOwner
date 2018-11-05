package org.edforge.efdeviceowner.net;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.AttributeSet;
import android.util.Log;

import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;

/**
 * Created by kevin on 10/31/2018.
 */

public class EFNetManager {

    private static final String TAG                 = "WifiManager";
    private final Context mContext;

    private StateReceiver  bReceiver;

    public EFNetManager(Context context) {

        mContext = context;

        IntentFilter filter = new IntentFilter(CONNECTIVITY_ACTION);
        filter.addAction(CONNECTIVITY_ACTION);

        bReceiver = new StateReceiver();

        context.registerReceiver(bReceiver, filter);
    }


    public void onDestroy() {

        mContext.unregisterReceiver(bReceiver);
    }


    class StateReceiver extends BroadcastReceiver {

        public void onReceive (Context context, Intent intent) {

            Log.d("Loader", "Broadcast recieved: ");

            switch(intent.getAction()) {


            }
        }
    }

}
