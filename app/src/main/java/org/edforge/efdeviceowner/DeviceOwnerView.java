package org.edforge.efdeviceowner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.edforge.util.TCONST;

import static org.edforge.util.TCONST.LAUNCH_SETTINGS;
import static org.edforge.util.TCONST.REQ_ANDROID_BREAKOUT;
import static org.edforge.util.TCONST.REQ_REBOOT_DEVICE;
import static org.edforge.util.TCONST.REQ_WIPE_DEVICE;
import static org.edforge.util.TCONST.SETUP_MODE;
import static org.edforge.util.TCONST.SLAVE_MODE;
import static org.edforge.util.TCONST.SYSTEM_STATUS;
import static org.edforge.util.TCONST.USER_MODE;

/**
 * Created by kevin on 11/4/2018.
 */

public class DeviceOwnerView extends FrameLayout {

    private Context           mContext;
    private IEdForgeLauncher  mCallback;

    private TextView        Sstatus;
    private TextView        Sid;
    private Button          SuserMode;
    private Button          SslaveMode;
    private Button          SsetupMode;
    private Button          Swipebutton;
    private Button          SsettingsButton;
    private Button          SEFhomeButton;
    private Button          SASUShomeButton;
    private Button          SbreakOutButton;
    private Button          SbootButton;

    private LocalBroadcastManager   bManager;
    private ownerViewReceiver       bReceiver;


    public DeviceOwnerView(Context context) {
        super(context);
        init(context, null);
    }

    public DeviceOwnerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public DeviceOwnerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    // Function to find the index of an element in a primitive array in Java
    public static int find(int[] a, int target)
    {
        for (int i = 0; i < a.length; i++)
            if (a[i] == target)
                return i;

        return -1;
    }
    public void init(Context context, AttributeSet attrs) {

        mContext = context;

        // Capture the local broadcast manager
        bManager = LocalBroadcastManager.getInstance(mContext);

        IntentFilter filter = new IntentFilter(SYSTEM_STATUS);

        bReceiver = new ownerViewReceiver();
        bManager.registerReceiver(bReceiver, filter);
    }


    @Override
    protected void onFinishInflate() {

        super.onFinishInflate();

        Sstatus = (TextView) findViewById(R.id.Sstatus);
        Sid     = (TextView) findViewById(R.id.Sid);

        Sid.setText(OwnerActivity.VERSION_EDFORGE);

        SbreakOutButton = (Button) findViewById(R.id.SbreakButton);
        SbreakOutButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                broadcast(REQ_ANDROID_BREAKOUT);
            }
        });

        SsetupMode = (Button) findViewById(R.id.SsetupMode);
        SsetupMode.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                broadcast(SETUP_MODE);
            }
        });

        SslaveMode = (Button) findViewById(R.id.SslaveMode);
        SslaveMode.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                broadcast(SLAVE_MODE);
            }
        });

        SuserMode = (Button) findViewById(R.id.SuserMode);
        SuserMode.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                broadcast(USER_MODE);
            }
        });

        SsettingsButton = (Button) findViewById(R.id.SsettingsButton);
        SsettingsButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                broadcast(LAUNCH_SETTINGS);
            }
        });

//        SEFhomeButton = (Button) findViewById(R.id.SEFhomeButton);
//        SEFhomeButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                broadcast(SET_HOME_APP, EFHOME_LAUNCHER);
//            }
//        });
//
//        SASUShomeButton = (Button) findViewById(R.id.SASUShomeButton);
//        SASUShomeButton.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                broadcast(SET_HOME_APP, ASUSDEF_LAUNCHER);
//            }
//        });

        SbootButton = (Button) findViewById(R.id.SbootButton);
        SbootButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                broadcast(REQ_REBOOT_DEVICE);
            }
        });


        Swipebutton = (Button) findViewById(R.id.SwipeButton);
        Swipebutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                broadcast(REQ_WIPE_DEVICE);
            }
        });

    }


    public void setCallback(IEdForgeLauncher _callback) {

        mCallback = _callback;

    }


    public void broadcast(String Action) {

        Intent msg = new Intent(Action);
        bManager.sendBroadcast(msg);
    }
    public void broadcast(String Action, int Msg) {

        Intent msg = new Intent(Action);
        msg.putExtra(TCONST.INT_FIELD, Msg);

        bManager.sendBroadcast(msg);
    }
    public void broadcast(String Action, String Msg) {

        Intent msg = new Intent(Action);
        msg.putExtra(TCONST.NAME_FIELD, Msg);

        bManager.sendBroadcast(msg);
    }


    class ownerViewReceiver extends BroadcastReceiver {

        public void onReceive (Context context, Intent intent) {

            Log.d("nameView", "Broadcast recieved: ");

            switch(intent.getAction()) {

                case SYSTEM_STATUS:
                    String status = intent.getStringExtra(TCONST.NAME_FIELD);

                    Sstatus.setText(status);
                    break;


            }
        }
    }
}
