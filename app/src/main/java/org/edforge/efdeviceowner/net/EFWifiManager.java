package org.edforge.efdeviceowner.net;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.edforge.util.TCONST;

import java.util.List;

import static java.lang.Thread.sleep;
import static org.edforge.util.TCONST.NET_SWITCH_FAILED;
import static org.edforge.util.TCONST.NET_SWITCH_SUCESS;

/**
 * Created by kevin on 10/31/2018.
 */

public class EFWifiManager {

    private final Context mContext;

    private String mSSID = "\"EdForge\"";

    private String mNetworkSSID = "EdForge";
    private String mNetworkPass = "";
    private String mSecTyp      = TCONST.OPEN;

    private LocalBroadcastManager bManager;
    private WifiManager       mWifiManager;
    private WifiConfiguration mConf;

    private Thread workerThread;

    private static final String TAG = "WifiManager";



    public EFWifiManager(Context context) {

        mContext     = context;

        // Capture the local broadcast manager
        bManager     = LocalBroadcastManager.getInstance(mContext);
        mWifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);

        mConf = new WifiConfiguration();
        mConf.SSID = "\"" + mNetworkSSID + "\"";   // Please note the quotes. String should contain ssid in quotes

        switch(mSecTyp) {
            case TCONST.WEP:
                mConf.wepKeys[0] = "\"" + mNetworkPass + "\"";
                mConf.wepTxKeyIndex = 0;
                mConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                mConf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                break;

            case TCONST.WPA:
                mConf.preSharedKey = "\""+ mNetworkPass +"\"";
                break;

            case TCONST.OPEN:
                mConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                break;
        }

        mWifiManager.addNetwork(mConf);
    }

    private String SSID(String target) {

        return "\"" + target + "\"";
    }

    public void reqSwitchNetwork(String target) {

        WifiInfo info = mWifiManager.getConnectionInfo();

        if (!info.getSSID().equals(SSID(target))) {
            Log.i(TAG, info.getSSID());

            workerThread = new Thread(new netSwitch(target));
            workerThread.start();
        }
        else {
            broadcast(NET_SWITCH_SUCESS,"Connected to: EdForge");
        }
    }


    public class netSwitch implements Runnable {

        Integer i1;
        String  target;
        WifiConfiguration mConf;

        public netSwitch(String _target) {
            target = _target;
        }

        public void switchNetwork() {

            List<WifiConfiguration> list = mWifiManager.getConfiguredNetworks();

            for (WifiConfiguration i : list) {

                if (i.SSID != null && i.SSID.equals(SSID(target))) {

                    mWifiManager.disconnect();
                    mWifiManager.enableNetwork(i.networkId, true);
                    mWifiManager.reconnect();
                    break;
                }
            }
        }


        private Boolean testNetwork(String target) {

            WifiInfo info = mWifiManager.getConnectionInfo();

            return info.getSSID().equals(SSID(target));
        }


        @Override
        public void run() {

            Boolean result = false;

            switchNetwork();

            for(i1 = 0 ; i1 < 20 ; i1++) {
                try {

                    sleep(300);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (testNetwork(target)) {
                    broadcast(NET_SWITCH_SUCESS,"Connected to: EdForge");
                    result = true;
                    break;
                }
            }

            if(!result)
                broadcast(NET_SWITCH_FAILED, "Wifi Network Not Found");
        }
    }

    public void broadcast(String Action) {

        Intent msg = new Intent(Action);
        bManager.sendBroadcast(msg);
    }
    public void broadcast(String Action, String Msg) {

        Intent msg = new Intent(Action);
        msg.putExtra(TCONST.NAME_FIELD, Msg);

        bManager.sendBroadcast(msg);
    }



}
