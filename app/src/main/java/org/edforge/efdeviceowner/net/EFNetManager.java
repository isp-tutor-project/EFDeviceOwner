package org.edforge.efdeviceowner.net;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.edforge.util.TCONST;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import static android.net.ConnectivityManager.CONNECTIVITY_ACTION;
import static java.lang.Integer.parseInt;
import static org.edforge.util.TCONST.NET_SWITCH_SUCESS;


public class EFNetManager {

    private static final EFNetManager ourInstance = new EFNetManager();

    private       Context                   mContext;
    private       LocalBroadcastManager     bManager;
    private       StateReceiver             bReceiver;
    private       EFWifiManager             mWIFImanager;

    public static final String ETHERNET = "Ethernet";
    public static final String WIFI     = "WiFi";

    public static final String ETHERNET_PFX = "eth";        // Starts with
    public static final String WIFI_PFX     = "wlan";       // Starts with

    public String mNetInterface;
    public String mIPaddress;
    public String mMACbyteArr;
    public String mMACid;

    private static final String TAG = "EFNetManager";


    public static EFNetManager getInstance() {
        return ourInstance;
    }

    public static EFNetManager getInstance(Context context) {

        ourInstance.mContext = context;
        ourInstance.init();

        return ourInstance;
    }

    private EFNetManager() {
    }

    public void onDestroy() {

        mContext.unregisterReceiver(bReceiver);
    }


    private void init() {

        mWIFImanager = new EFWifiManager(mContext);

        // Capture the local broadcast manager
        bManager = LocalBroadcastManager.getInstance(mContext);

        IntentFilter filter = new IntentFilter(CONNECTIVITY_ACTION);
        filter.addAction(CONNECTIVITY_ACTION);

        ourInstance.bReceiver = new StateReceiver();

        mContext.registerReceiver(ourInstance.bReceiver, filter);
    }


    public  String getMAC(NetworkInterface nif) {

        String mac = "";

        try {
            byte[] mMacByteArr = nif.getHardwareAddress();

            if (mMacByteArr != null) {

                StringBuilder res1 = new StringBuilder();
                for (byte b : mMacByteArr) {
                    String hex = Integer.toHexString(b & 0xFF);
                    if (hex.length() == 1)
                        hex = "0".concat(hex);
                    res1.append(hex.concat(":"));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                mMACid = res1.toString();
            }
        } catch (Exception ex) {
        }

        return mac;
    }


    public void reqSwitchNetwork(String target) {

        if(getInterfaceType().equals(ETHERNET)) {
            broadcast(NET_SWITCH_SUCESS,"Connected to: Ethernet");
        }
        else {
            mWIFImanager.reqSwitchNetwork(target);
        }
    }



    public void scanNetInterface() {

        try {
            Enumeration<NetworkInterface> enumNetworkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (enumNetworkInterfaces.hasMoreElements()) {

                NetworkInterface networkInterface        = enumNetworkInterfaces.nextElement();
                Enumeration <InetAddress>enumInetAddress = networkInterface.getInetAddresses();

                while (enumInetAddress.hasMoreElements()) {

                    InetAddress inetAddress = enumInetAddress.nextElement();

                    if (inetAddress.isSiteLocalAddress()) {

                        getMAC(networkInterface);

                        mNetInterface = networkInterface.getName();
                        mIPaddress    = inetAddress.getHostAddress();
                    }
                }
            }

        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public String getInterfaceType() {

        String result = "Unknown";

        if(mNetInterface.startsWith(ETHERNET_PFX)) result = ETHERNET;
        if(mNetInterface.startsWith(WIFI_PFX))     result = "WiFi";

        return result;
    }


    public String getNetInterface() {

        scanNetInterface();

        return mNetInterface;
    }


    public String getIpAsString() {

        scanNetInterface();

        return mIPaddress;
    }


    public String getMacAsString() {

        return mMACid;
    }


    public byte[] getIpAddressAsByteArr() {

        String ip = getIpAsString();

        String[] ipArray = ip.split("\\.");
        byte[]   ipAddr  = new byte[4];

        for(int i1 = 0 ; i1 < ipArray.length ; i1++ ) {

            ipAddr[i1] = (byte)parseInt(ipArray[i1]);
        }

        return ipAddr;
    }


    class StateReceiver extends BroadcastReceiver {

        public void onReceive (Context context, Intent intent) {

            Log.d("Loader", "Broadcast recieved: ");

            switch(intent.getAction()) {


            }
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
