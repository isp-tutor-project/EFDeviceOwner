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
import android.content.pm.PackageInstaller;
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
import android.view.WindowManager;
import android.widget.FrameLayout;

import org.edforge.efdeviceowner.net.CCommandProcessor;
import org.edforge.efdeviceowner.net.EFNetManager;
import org.edforge.util.TCONST;

import java.util.ArrayList;

import static org.edforge.efdeviceowner.PlugStatusReceiver.PLUG_CONNECT_STATE;
import static org.edforge.efdeviceowner.PlugStatusReceiver.PLUG_PREFS;
import static org.edforge.util.TCONST.ANDROID_BREAK_OUT;
import static org.edforge.util.TCONST.ASUSDEF_LAUNCHER;
import static org.edforge.util.TCONST.EFHOME_LAUNCH_INTENT;
import static org.edforge.util.TCONST.EFHOME_PACKAGE;
import static org.edforge.util.TCONST.EFHOME_STARTER_INTENT;
import static org.edforge.util.TCONST.EFHOST_PACKAGE;
import static org.edforge.util.TCONST.EFOWNER_LAUNCHER;
import static org.edforge.util.TCONST.EFOWNER_PACKAGE;
import static org.edforge.util.TCONST.REBOOT_DEVICE;
import static org.edforge.util.TCONST.SYSTEM_STATUS;
import static org.edforge.util.TCONST.USER_MODE;
import static org.edforge.util.TCONST.WIPE_DEVICE;

// Note: To switch from non-test build to test build you need to do the following
//
// 1. Force install a test build using  adb install -r -t EFdeviceOwner.apk
// 2. Remove the current device-woner - adb shell dpm remove-active-admin "org.edforge.efdeviceowner/org.edforge.efdeviceowner.DeviceOwnerReceiver"
//
// You may then install debug signed deviceowner apks.
//


// ADB (on all versions of Android):
// adb shell dpm set-device-owner "org.edforge.efdeviceowner/.DeviceOwnerReceiver"
// adb shell dpm remove-active-admin "org.edforge.efdeviceowner/org.edforge.efdeviceowner.DeviceOwnerReceiver"
// adb install -r -t EFdeviceOwner.apk
// adb install -r -t app-release.apk
// adb uninstall org.edforge.efdeviceowner
//
// See: https://stackoverflow.com/questions/2442713/view-the-tasks-activity-stack
// adb shell dumpsys activity activities | sed -En -e '/Stack #/p' -e '/Running activities/,/Run #0/p'

// Default tablet launcher | com.asus.launcher/com.android.launcher3.Launcher

// Kill an app from adb
//adb shell pm clear com.my.app.package

// switching to TCPIP adb
// adb tcpip 5555
// adb connect 192.168.2.23
//
// switching back to usb debugging
// adb usb

public class OwnerActivity extends Activity implements IEdForgeLauncher {

    private MasterContainer masterContainer;

    private LocalBroadcastManager   bManager;
    private homeReceiver            bReceiver;

    private LayoutInflater mInflater;

    private BreakOutView        breakOutView;
    private DeviceOwnerView     deviceOwnerView;
    private SlaveModeView       slaveModeView;
    private DefInstructionView  defConditionView;
    private View                mCurrView = null;

    static public String        VERSION_EDFORGE;
    static public ArrayList     VERSION_SPEC;


    private String mPackage;
    private ComponentName       mAdminComponentName;
    private ActivityManager     mActivityManager;
    private EFNetManager        mNetManager;
    private DevicePolicyManager mDevicePolicyManager;
    private LauncherManager     mLauncherManager;
    private PostProvisioning    mProvisioningManager;
    private CCommandProcessor   mCommandProcessor;
    private ComponentName       mDeviceAdmin;
    private SharedPreferences   mSharedPrefs;
    private PackageManager      mPackageManager;
    private boolean             mInLockTask = true;
    private boolean             mHomeAvail  = false;

    private boolean isInBreakOut = false;

    private static final String Battery_PLUGGED_ANY = Integer.toString(
            BatteryManager.BATTERY_PLUGGED_AC |
                    BatteryManager.BATTERY_PLUGGED_USB |
                    BatteryManager.BATTERY_PLUGGED_WIRELESS);

    private static final String DONT_STAY_ON = "0";

    public final static String ASSET_FOLDER = Environment.getExternalStorageDirectory() + TCONST.EDFORGE_FOLDER;
    public final static String UPDATE_FOLDER = Environment.getExternalStorageDirectory() + TCONST.EDFORGE_UPDATE_FOLDER;
    public final static String LOG_PATH = Environment.getExternalStorageDirectory() + TCONST.EDFORGE_LOG_FOLDER;
    public final static String DATA_PATH = Environment.getExternalStorageDirectory() + TCONST.EDFORGE_DATA_FOLDER;
    public final static String XFER_PATH = Environment.getExternalStorageDirectory() + TCONST.EDFORGE_DATA_TRANSFER;

    public final static String[] efPaths = {ASSET_FOLDER, UPDATE_FOLDER, LOG_PATH, DATA_PATH, XFER_PATH};
    public final static String[] taskLockPkgs = {EFOWNER_PACKAGE, EFHOME_PACKAGE, EFHOST_PACKAGE};

    private static final String TAG = "OwnerActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        FrameLayout.LayoutParams params;

        // Note = we don't want the system to try and recreate any of our views- always pass null
        //
        super.onCreate(null);

        mSharedPrefs         = getSharedPreferences(PLUG_PREFS, Context.MODE_PRIVATE);
        mProvisioningManager = new PostProvisioning(this);
        mActivityManager     = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        mNetManager          = EFNetManager.getInstance(this);
        mAdminComponentName  = DeviceOwnerReceiver.getComponentName(this);
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mLauncherManager     = new LauncherManager(this);
        mCommandProcessor    = new CCommandProcessor(this);
        mPackage             = getApplicationContext().getPackageName();
        mPackageManager      = getPackageManager();

        mSharedPrefs.edit().putBoolean(PLUG_CONNECT_STATE, true).commit();

        if (mDevicePolicyManager.isDeviceOwnerApp(mPackage)) {

            mProvisioningManager.performPostProvisioningOperations(null);
            mCommandProcessor.validatePaths(efPaths);
        }

        // Capture the local broadcast manager
        bManager = LocalBroadcastManager.getInstance(this);
        VERSION_EDFORGE   = BuildConfig.VERSION_NAME;

        IntentFilter filter = new IntentFilter(TCONST.LAUNCH_SETTINGS);
        filter.addAction(TCONST.SLAVE_MODE);
        filter.addAction(TCONST.COND_MODE);
        filter.addAction(TCONST.SET_HOME_APP);
        filter.addAction(TCONST.REQ_REBOOT_DEVICE);
        filter.addAction(TCONST.REQ_WIPE_DEVICE);
        filter.addAction(TCONST.REQ_ANDROID_BREAKOUT);
        filter.addAction(TCONST.REBOOT_DEVICE);
        filter.addAction(TCONST.WIPE_DEVICE);
        filter.addAction(TCONST.GO_HOME);
        filter.addAction(TCONST.ANDROID_BREAK_OUT);
        filter.addAction(USER_MODE);
        filter.addAction(EFHOME_STARTER_INTENT);

        bReceiver = new homeReceiver();
        bManager.registerReceiver(bReceiver, filter);

//        Intent it = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
//        it.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, new ComponentName(this, YourAdminReceiver.class));
//        startActivityForResult(it, 0);

        setContentView(R.layout.activity_master_container);
        masterContainer = (MasterContainer) findViewById(R.id.master_container);

        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        breakOutView = (BreakOutView) mInflater.inflate(R.layout.breakout_view, null);
        params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        breakOutView.setLayoutParams(params);
        breakOutView.setMode(ANDROID_BREAK_OUT);
        breakOutView.setCallback(this);

        deviceOwnerView = (DeviceOwnerView) mInflater.inflate(R.layout.device_owner_view, null);
        params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        deviceOwnerView.setLayoutParams(params);
        deviceOwnerView.setCallback(this);

        slaveModeView = (SlaveModeView) mInflater.inflate(R.layout.slave_mode_view, null);
        params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        slaveModeView.setLayoutParams(params);

        defConditionView = (DefInstructionView) mInflater.inflate(R.layout.default_instruction_view, null);
        params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        defConditionView.setLayoutParams(params);

        if (mDevicePolicyManager.isDeviceOwnerApp(mPackage)) {

            // This app is set up as the device owner. Show the main features.
            broadcast(SYSTEM_STATUS, "EdForge Owns Device");
            setDefaultCosuPolicies(true);

        } else {

            broadcast(SYSTEM_STATUS, "Google Owns Device");
        }
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);


    }

    private boolean testPackageInstalled(String uri) {

        try {
            mPackageManager.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
        }

//        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
//
//        dpm.clearDeviceOwnerApp(context.getPackageName());

        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bManager.unregisterReceiver(bReceiver);
        mNetManager.onDestroy();
    }

    @Override
    protected void onStart() {

        super.onStart();
    }

    private boolean isPlugConnected() {

        return mSharedPrefs.getBoolean(PLUG_CONNECT_STATE, false);
    }

    /**
     *
     */
    @Override
    protected void onResume() {

        super.onResume();

        mHomeAvail = testPackageInstalled(EFHOME_PACKAGE);

        setFullScreen();

        // Note this may be called several times for a single restart so we can't reset
        // isInBreakOut = false; here or it will go to user_mode if unplugged.
        //
        if(isPlugConnected() || isInBreakOut || !mHomeAvail) {
            switchView(deviceOwnerView);
        }
        else {
            broadcast(USER_MODE);
        }
    }

    private void setFullScreen() {

        ((View) masterContainer).setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
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
        if (active)
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


    private void enterLockTask() {

        if (mDevicePolicyManager.isDeviceOwnerApp(mPackage)) {

            // start lock task mode if it's not already active
            //
            mLauncherManager.setPreferredLauncher(EFOWNER_LAUNCHER);

            try {
                if (!mInLockTask) {
                    startLockTask();
                    mInLockTask = true;
                }
            } catch (IllegalStateException e) {
                // no lock task present, ignore
            }
        }
    }


    private void exitLockTask() {

        if (mDevicePolicyManager.isDeviceOwnerApp(mPackage)) {

            // stop lock task mode if it's active
            //
            try {
                if(mInLockTask) {
                    stopLockTask();
                    mInLockTask = false;
                }
            } catch (IllegalStateException e) {
                // no lock task present, ignore
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case 100:
                isInBreakOut = true;
                Log.d(TAG, "BreakOut Selected");
                break;

            default:
                break;
        }
    }

    protected void wipeData() {

        DevicePolicyManager manager =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        mDeviceAdmin = DeviceOwnerReceiver.getComponentName(this);
        manager.clearUserRestriction(mDeviceAdmin, UserManager.DISALLOW_FACTORY_RESET);
//        manager.removeActiveAdmin(mDeviceAdmin)

        manager.wipeData(0);
    }


    protected void rebootDevice() {
        DevicePolicyManager manager =
                (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        mDeviceAdmin = DeviceOwnerReceiver.getComponentName(this);

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

        if (mCurrView != null)
            masterContainer.removeView(mCurrView);

        masterContainer.addAndShow(target);
        mCurrView = target;
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


    public Intent getIntent(String source) {

        Intent launch = new Intent(source);

        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        launch.addCategory(Intent.CATEGORY_DEFAULT);
        return launch;
    }


    class homeReceiver extends BroadcastReceiver {

        PackageInstaller.SessionInfo session;

        public void onReceive (Context context, Intent intent) {

            Log.d("homeReceiver", "Broadcast received: ");

            switch(intent.getAction()) {

                case TCONST.GO_HOME:

//                    startLockTask();

                    // Allow the screen to sleep when not in a session
                    //
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    switchView(deviceOwnerView);
                    break;

//                case TCONST.SETUP_MODE:
//                    // Disable screen sleep while in a session
//                    //
//                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//
//                    switchView(slaveModeView);
//                    slaveModeView.startClient();
//                    break;
//
                case TCONST.SLAVE_MODE:

//                    stopLockTask();

                    // Disable screen sleep while in a session
                    //
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    switchView(slaveModeView);
                    slaveModeView.startServer();
                    break;

                case TCONST.COND_MODE:

                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    defConditionView.configView();
                    switchView(defConditionView);
                    break;


                case USER_MODE:
                case EFHOME_STARTER_INTENT:
                    mHomeAvail = testPackageInstalled(EFHOME_PACKAGE);

                    if(mHomeAvail) {
                        isInBreakOut = false;
                        startActivityForResult(getIntent(EFHOME_LAUNCH_INTENT), 100);
                    }
                    break;

                case TCONST.LAUNCH_SETTINGS:
                    startActivityForResult(new Intent(android.provider.Settings.ACTION_SETTINGS), 200);
                    break;

                case TCONST.REQ_ANDROID_BREAKOUT:

                    breakOutView.setMode(ANDROID_BREAK_OUT);
                    switchView(breakOutView);
                    break;

                case TCONST.REQ_REBOOT_DEVICE:
                    breakOutView.setMode(REBOOT_DEVICE);
                    switchView(breakOutView);
                    break;

                case TCONST.REQ_WIPE_DEVICE:
                    breakOutView.setMode(WIPE_DEVICE);
                    switchView(breakOutView);
                    break;

                case TCONST.ANDROID_BREAK_OUT:

                    exitLockTask();
//                    mLauncherManager.clearPreferredLauncher();
                    mLauncherManager.setPreferredLauncher(ASUSDEF_LAUNCHER);
                    startActivity(Util.getHomeLaunchIntent());
//                    rebootDevice();
                    finish();
                    break;

                case TCONST.REBOOT_DEVICE:
                    exitLockTask();
                    rebootDevice();
                    break;

                case WIPE_DEVICE:
                    exitLockTask();
                    wipeData();
                    break;
            }
        }
    }


    /**
     * TODO: TEST
     */
    @Override
    public void onBackPressed() {

        setFullScreen();
    }


}



