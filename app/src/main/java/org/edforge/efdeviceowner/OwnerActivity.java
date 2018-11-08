//*********************************************************************************
//
//    Copyright(c) Kevin Willows All Rights Reserved
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
//*********************************************************************************

package org.edforge.efdeviceowner;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.edforge.efdeviceowner.net.CCommandProcessor;
import org.edforge.util.TCONST;

import java.util.List;

import static android.app.ActivityManager.LOCK_TASK_MODE_LOCKED;
import static android.app.ActivityManager.LOCK_TASK_MODE_NONE;
import static android.app.ActivityManager.LOCK_TASK_MODE_PINNED;
import static org.edforge.util.TCONST.BREAK_OUT;
import static org.edforge.util.TCONST.ASUSDEF_LAUNCHER;
import static org.edforge.util.TCONST.EFHOME_LAUNCHER;
import static org.edforge.util.TCONST.EFHOME_LAUNCH_INTENT;
import static org.edforge.util.TCONST.EFHOME_PACKAGE;
import static org.edforge.util.TCONST.EFHOST_PACKAGE;
import static org.edforge.util.TCONST.EFOWNER_LAUNCHER;
import static org.edforge.util.TCONST.EFOWNER_PACKAGE;
import static org.edforge.util.TCONST.FINISH_APP;
import static org.edforge.util.TCONST.LAUNCH_HOME;
import static org.edforge.util.TCONST.REBOOT_DEVICE;
import static org.edforge.util.TCONST.SYSTEM_STATUS;
import static org.edforge.util.TCONST.WIPE_DEVICE;

// ADB (on all versions of Android):
// adb shell dpm set-device-owner "org.edforge.efdeviceowner/.DeviceOwnerReceiver"
// adb shell dpm remove-active-admin "org.edforge.efdeviceowner/org.edforge.efdeviceowner.DeviceOwnerReceiver"
// adb install -r -t EFdeviceOwner.apk

// Default tablet launcher | com.asus.launcher/com.android.launcher3.Launcher

public class OwnerActivity extends Activity implements IEdForgeLauncher {

    private MasterContainer masterContainer;

    private LocalBroadcastManager   bManager;
    private homeReceiver            bReceiver;

    private LayoutInflater          mInflater;

    private BreakOutView        breakOutView;
    private DeviceOwnerView     deviceOwnerView;
    private SlaveModeView       slaveModeView;
    private View                mCurrView = null;


    private String              mPackage;
    private ComponentName       mAdminComponentName;
    private ActivityManager     mActivityManager;
    private DevicePolicyManager mDevicePolicyManager;
    private LauncherManager     mLauncherManager;
    private PostProvisioning    mProvisioningManager;
    private CCommandProcessor   mCommandProcessor;
    private ComponentName       mDeviceAdmin;

    private static final String Battery_PLUGGED_ANY = Integer.toString(
                    BatteryManager.BATTERY_PLUGGED_AC |
                        BatteryManager.BATTERY_PLUGGED_USB |
                        BatteryManager.BATTERY_PLUGGED_WIRELESS);

    private static final String DONT_STAY_ON = "0";

    public final static String  ASSET_FOLDER   = Environment.getExternalStorageDirectory() + TCONST.EDFORGE_FOLDER;
    public final static String  UPDATE_FOLDER  = Environment.getExternalStorageDirectory() + TCONST.EDFORGE_UPDATE_FOLDER;
    public final static String  LOG_PATH       = Environment.getExternalStorageDirectory() + TCONST.EDFORGE_LOG_FOLDER;
    public final static String  DATA_PATH      = Environment.getExternalStorageDirectory() + TCONST.EDFORGE_DATA_FOLDER;
    public final static String  XFER_PATH      = Environment.getExternalStorageDirectory() + TCONST.EDFORGE_DATA_TRANSFER;

    public final static String[] efPaths = {ASSET_FOLDER,UPDATE_FOLDER,LOG_PATH,DATA_PATH,XFER_PATH};
    public final static String[] taskLockPkgs = {EFOWNER_PACKAGE,EFHOME_PACKAGE,EFHOST_PACKAGE};

    private static final String TAG = "OwnerActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        FrameLayout.LayoutParams params;

        super.onCreate(savedInstanceState);

        mProvisioningManager = new PostProvisioning(this);
        mActivityManager     = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        mAdminComponentName  = DeviceOwnerReceiver.getComponentName(this);
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mLauncherManager     = new LauncherManager(this);
        mCommandProcessor    = new CCommandProcessor(this);
        mPackage             = getApplicationContext().getPackageName();


        if (mDevicePolicyManager.isDeviceOwnerApp(mPackage)) {

            mProvisioningManager.performPostProvisioningOperations(null);
            mCommandProcessor.validatePaths(efPaths);
        }

        // Capture the local broadcast manager
        bManager = LocalBroadcastManager.getInstance(this);

        IntentFilter filter = new IntentFilter(TCONST.LAUNCH_SETTINGS);
        filter.addAction(TCONST.SLAVE_MODE);
        filter.addAction(TCONST.SET_HOME_APP);
        filter.addAction(TCONST.REQ_REBOOT_DEVICE);
        filter.addAction(TCONST.REQ_WIPE_DEVICE);
        filter.addAction(TCONST.REQ_ANDROID_BREAKOUT);
        filter.addAction(TCONST.REBOOT_DEVICE);
        filter.addAction(TCONST.WIPE_DEVICE);
        filter.addAction(TCONST.GO_HOME);
        filter.addAction(TCONST.BREAK_OUT);
        filter.addAction(TCONST.SETUP_MODE);
        filter.addAction(TCONST.USER_MODE);

        bReceiver = new homeReceiver();
        bManager.registerReceiver(bReceiver, filter);

//        Intent it = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
//        it.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(this, YourAdminReceiver.class));
//        startActivityForResult(it, 0);

        setContentView(R.layout.activity_master_container);
        masterContainer = (MasterContainer)findViewById(R.id.master_container);

        mInflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        breakOutView = (BreakOutView) mInflater.inflate(R.layout.breakout_view, null );
        params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        breakOutView.setLayoutParams(params);
        breakOutView.setMode(BREAK_OUT);
        breakOutView.setCallback(this);

        deviceOwnerView = (DeviceOwnerView) mInflater.inflate(R.layout.device_owner_view, null );
        params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        deviceOwnerView.setLayoutParams(params);
        deviceOwnerView.setCallback(this);

        slaveModeView = (SlaveModeView) mInflater.inflate(R.layout.slave_mode_view, null );
        params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        slaveModeView.setLayoutParams(params);

        if (mDevicePolicyManager.isDeviceOwnerApp(mPackage)) {

            // This app is set up as the device owner. Show the main features.
            broadcast(SYSTEM_STATUS,"EdForge Owns Device");
            setDefaultCosuPolicies(true);

        } else {

            broadcast(SYSTEM_STATUS,"Google Owns Device");
        }

//        Intent launchIntent = getIntent();
//        Log.i(TAG, "DEVICEOWNER LAUNCH:" + launchIntent.getAction());
//        Log.i(TAG, "mProvisioningManager: " + (mProvisioningManager == null? "NULL": "NOTNULL"));

        switchView(deviceOwnerView);
    }


    private void setDefaultCosuPolicies(boolean active) {

        // set user restrictions
//        setUserRestriction(UserManager.DISALLOW_SAFE_BOOT, active);
        setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, active);
        setUserRestriction(UserManager.DISALLOW_ADD_USER, active);
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, active);
//        setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, active);

        // disable keyguard and status bar
        mDevicePolicyManager.setKeyguardDisabled(mAdminComponentName, active);
        mDevicePolicyManager.setStatusBarDisabled(mAdminComponentName, active);

        // enable STAY_ON_WHILE_PLUGGED_IN
//        enableStayOnWhilePluggedIn(active);

        // set this Activity as a lock task package
        //
        if(active)
            mDevicePolicyManager.setLockTaskPackages(mAdminComponentName, taskLockPkgs);

    }

    private void setUserRestriction(String restriction, boolean disallow) {

        if (disallow) {
            mDevicePolicyManager.addUserRestriction(mAdminComponentName,
                    restriction);
        } else {
            mDevicePolicyManager.clearUserRestriction(mAdminComponentName,
                    restriction);
        }
    }

    private void enableStayOnWhilePluggedIn(boolean enabled) {

        if (enabled) {
            mDevicePolicyManager.setGlobalSetting(
                    mAdminComponentName,
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    Battery_PLUGGED_ANY);
        } else {
            mDevicePolicyManager.setGlobalSetting(
                    mAdminComponentName,
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN, DONT_STAY_ON);
        }
    }



    private void setFullScreen() {

        if (mDevicePolicyManager.isDeviceOwnerApp(mPackage)) {

            if(isAppOnForeground(this))
                                enterLockTask();
        }

        ((View) masterContainer).setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }



    private boolean isAppOnForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    private void enterLockTask() {

        if (mDevicePolicyManager.isDeviceOwnerApp(mPackage)) {

            // start lock task mode if it's not already active
            //
            mLauncherManager.setPreferredLauncher(EFOWNER_LAUNCHER);
            startLockTask();

            switch (mActivityManager.getLockTaskModeState()) {

                case LOCK_TASK_MODE_NONE:
                case LOCK_TASK_MODE_PINNED:
                    break;
            }
        }
    }


    private void exitLockTask() {

        if (mDevicePolicyManager.isDeviceOwnerApp(mPackage)) {

            // stop lock task mode if it's active
            //
            try {
                stopLockTask();
            } catch (IllegalStateException e) {
                // no lock task present, ignore
            }

            switch (mActivityManager.getLockTaskModeState()) {
                case LOCK_TASK_MODE_LOCKED:
                case LOCK_TASK_MODE_PINNED:
                    break;
            }
        }
    }


    @Override
    protected void onStart() {

        super.onStart();
    }


    /**
     *
     */
    @Override
    protected void onResume() {

        super.onResume();

        SharedPreferences prefs = getPreferences(MODE_PRIVATE);

        String restoredText = prefs.getString("text", null);

        if (restoredText != null) {
        }
        setFullScreen();

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Check which request we're responding to
        if (requestCode == 0) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.

                // Do something with the contact here (bigger example below)
            }
        }
    }

    protected void wipeData() {

        DevicePolicyManager manager =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        mDeviceAdmin = DeviceOwnerReceiver.getComponentName(this );
        manager.clearUserRestriction(mDeviceAdmin, UserManager.DISALLOW_FACTORY_RESET);
//        manager.removeActiveAdmin(mDeviceAdmin)

        manager.wipeData(0);
    }


    protected void rebootDevice() {
        DevicePolicyManager manager =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        mDeviceAdmin = DeviceOwnerReceiver.getComponentName(this );

        manager.reboot(mDeviceAdmin);
    }


    @Override
    public void gotoHome() {

    }

    @Override
    public void startBreakOut() {

    }

    @Override
    public void nextStep(String stepID) {

    }


    public void switchView(View target) {

        if(mCurrView != null)
            masterContainer.removeView(mCurrView);

        masterContainer.addAndShow(target);
        mCurrView = target;
    }


    public void broadcast(String Action, String Msg) {

        Intent msg = new Intent(Action);
        msg.putExtra(TCONST.NAME_FIELD, Msg);

        bManager.sendBroadcast(msg);
    }


    public Intent getIntent(String source) {

        Intent launch = new Intent(source);

        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launch.addCategory(Intent.CATEGORY_HOME);
        launch.addCategory(Intent.CATEGORY_DEFAULT);
        return launch;
    }


    class homeReceiver extends BroadcastReceiver {

        public void onReceive (Context context, Intent intent) {

            Log.d("homeReceiver", "Broadcast recieved: ");

            switch(intent.getAction()) {

                case TCONST.GO_HOME:
                    switchView(deviceOwnerView);
                    break;

                case TCONST.SETUP_MODE:
                    switchView(slaveModeView);
                    slaveModeView.startClient();
                    break;

                case TCONST.SLAVE_MODE:
                    switchView(slaveModeView);
                    slaveModeView.startServer();
                    break;

                case TCONST.USER_MODE:
                    mLauncherManager.setPreferredLauncher(EFHOME_LAUNCHER);
                    startActivity(getIntent(EFHOME_LAUNCH_INTENT));
                    finish();
                    break;

                case TCONST.LAUNCH_SETTINGS:
                    startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 0);
                    break;

                case TCONST.SET_HOME_APP:
                    String ID = intent.getStringExtra(TCONST.NAME_FIELD);

                    mLauncherManager.setPreferredLauncher(ID);
                    break;

                case TCONST.REQ_ANDROID_BREAKOUT:

                    exitLockTask();
                    mLauncherManager.setPreferredLauncher(ASUSDEF_LAUNCHER);

                    Intent startMain = new Intent(Intent.ACTION_MAIN);
                    startMain.addCategory(Intent.CATEGORY_HOME);

                    startActivity(startMain);
                    //                    rebootDevice();
                    finish();
                    break;

                case TCONST.REQ_REBOOT_DEVICE:
                    breakOutView.setMode(REBOOT_DEVICE);
                    switchView(breakOutView);
                    break;

                case TCONST.REQ_WIPE_DEVICE:
                    breakOutView.setMode(WIPE_DEVICE);
                    switchView(breakOutView);
                    break;

                case TCONST.REBOOT_DEVICE:
                    exitLockTask();
                    rebootDevice();
                    break;

                case WIPE_DEVICE:
                    exitLockTask();
                    wipeData();
                    break;

                case FINISH_APP:
                    exitLockTask();
                    finish();
                    break;

            }
        }
    }
}



