package org.edforge.efdeviceowner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.edforge.efdeviceowner.net.CClient;
import org.edforge.efdeviceowner.net.CServer;
import org.edforge.efdeviceowner.net.EFNetManager;
import org.edforge.efdeviceowner.net.EFWifiManager;
import org.edforge.util.IThreadComplete;
import org.edforge.util.TCONST;

import static org.edforge.util.TCONST.COMMAND_MODE;
import static org.edforge.util.TCONST.EDFORGE_NETID;
import static org.edforge.util.TCONST.GO_HOME;
import static org.edforge.util.TCONST.INSTALLATION_COMPLETE;
import static org.edforge.util.TCONST.INSTALLATION_PENDING;
import static org.edforge.util.TCONST.NET_STATUS;
import static org.edforge.util.TCONST.NET_SWITCH_FAILED;
import static org.edforge.util.TCONST.NET_SWITCH_SUCESS;
import static org.edforge.util.TCONST.SETUP_MODE;
import static org.edforge.util.TCONST.SYSTEM_STATUS;

/**
 * Created by kevin on 11/4/2018.
 */

public class SlaveModeView extends FrameLayout implements IThreadComplete {

    private Context mContext;
    private IEdForgeLauncher  mCallback;

    private EFNetManager    mNetManager;
    private CClient         mClient;
    private CServer         mServer;
    private String          mMode;
    private int             mPending = 0;

    private TextView        Spending;
    private TextView        Sstatus;
    private TextView        SslaveStatus;
    private Button          bHome;

    private boolean               isInitialized = false;
    private LocalBroadcastManager bManager;
    private slaveViewReceiver     bReceiver;


    public SlaveModeView(Context context) {
        super(context);
        init(context, null);
    }

    public SlaveModeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public SlaveModeView(Context context, AttributeSet attrs, int defStyleAttr) {
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

        if(!isInitialized) {

            isInitialized = true;

            // Capture the local broadcast manager
            bManager = LocalBroadcastManager.getInstance(mContext);
            mNetManager = EFNetManager.getInstance();

            IntentFilter filter = new IntentFilter(SYSTEM_STATUS);
            filter.addAction(NET_SWITCH_SUCESS);
            filter.addAction(NET_SWITCH_FAILED);
            filter.addAction(NET_STATUS);
            filter.addAction(INSTALLATION_PENDING);
            filter.addAction(INSTALLATION_COMPLETE);

            bReceiver = new slaveViewReceiver();
            bManager.registerReceiver(bReceiver, filter);
        }
    }


    @Override
    protected void onFinishInflate() {

        super.onFinishInflate();

        Spending     = (TextView) findViewById(R.id.Spending);
        Sstatus      = (TextView) findViewById(R.id.Sstatus);
        SslaveStatus = (TextView) findViewById(R.id.SslaveStatus);
        SslaveStatus.setMovementMethod(new ScrollingMovementMethod());

        bHome = (Button)   findViewById(R.id.bHome);
        bHome.setOnClickListener(clickListener);
    }


    public void startClient() {

        mMode = SETUP_MODE;
        SslaveStatus.setText("Setup Status:");
        mNetManager.scanNetInterface();

        mNetManager.reqSwitchNetwork(EDFORGE_NETID);
    }

    public void startServer() {

        mMode = COMMAND_MODE;
        SslaveStatus.setText("Setup Status:");
        mNetManager.scanNetInterface();

        mNetManager.reqSwitchNetwork(EDFORGE_NETID);
    }




    @Override
    public void notifyOfThreadComplete(CClient.ClientThread thread, String status) {
    }


    public void broadcast(String Action) {

        Intent msg = new Intent(Action);
        bManager.sendBroadcast(msg);
    }


    OnClickListener clickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {

            int vid = v.getId();

            if (vid == R.id.bHome) {

                if(mPending > 0) {
                    Toast.makeText(mContext, "Installations Pending: PLEASE WAIT.", Toast.LENGTH_SHORT).show();
                    Spending.setTextColor( 0xffDD0000);
                }
                else {
                    if (mServer != null)
                        mServer.onStop();
//
//                if(mClient != null)
//                    mClient.onStop();

                    broadcast(GO_HOME);
                }
            }
        }
    };


    class slaveViewReceiver extends BroadcastReceiver {

        private String status;

        public void onReceive (Context context, Intent intent) {

            Log.d("nameView", "Broadcast recieved: ");

            switch(intent.getAction()) {

                case SYSTEM_STATUS:
                    status = intent.getStringExtra(TCONST.NAME_FIELD);

                    Sstatus.setText(status);
                    break;

                case NET_SWITCH_FAILED:
                    status = intent.getStringExtra(TCONST.NAME_FIELD);
                    SslaveStatus.append("\n"+status);
                    break;

                case NET_SWITCH_SUCESS:
                    status = intent.getStringExtra(TCONST.NAME_FIELD);
                    SslaveStatus.append("\n"+status);

                    switch(mMode) {
                        case SETUP_MODE:
                            mClient = new CClient(mContext, SlaveModeView.this);
                            break;
                        case COMMAND_MODE:
                            mServer = new CServer(mContext);
                            break;
                    }
                    break;

                case NET_STATUS:
                    status = intent.getStringExtra(TCONST.NAME_FIELD);
                    SslaveStatus.append("\n"+status);
                    break;

                case INSTALLATION_PENDING:
                    mPending++;
                    Spending.setText("Pending Installations: " + mPending);
                    break;

                case INSTALLATION_COMPLETE:
                    mPending--;
                    Spending.setText("Pending Installations: " + mPending);
                    break;
            }
        }
    }
}
