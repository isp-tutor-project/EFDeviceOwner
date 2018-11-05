package org.edforge.efdeviceowner;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import org.edforge.util.TCONST;

import java.util.HashMap;

/**
 * Created by kevin on 10/31/2018.
 */

public class LauncherManager {

    private static final String TAG                 = "LauncherManager";

    private final Context mContext;
    private DevicePolicyManager mDevicePolicyManager;

    private static final String PREFS_DEVICE_OWNER  = "DeviceOwnerFragment";
    private static final String PREF_LAUNCHER       = "launcher";

    private HashMap launcherMap = new HashMap();

    public LauncherManager(Context context) {

        mContext = context;

        mDevicePolicyManager =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

        launcherMap.put(TCONST.ASUSDEF_LAUNCHER, new ComponentName("com.asus.launcher","com.android.launcher3.Launcher"));
        launcherMap.put(TCONST.EFOWNER_LAUNCHER, new ComponentName("org.edforge.efdeviceowner","org.edforge.efdeviceowner.OwnerActivity"));
        launcherMap.put(TCONST.EFHOME_LAUNCHER, new ComponentName("org.edforge.efhomescreen","org.edforge.efhomescreen.HomeActivity"));
    }

    /**
     * Loads the package name from SharedPreferences.
     *
     * @return The package name of the launcher currently set as preferred, or null if there is no
     * preferred launcher.
     */
    private String loadPersistentPreferredLauncher() {
        return mContext.getSharedPreferences(PREFS_DEVICE_OWNER, Context.MODE_PRIVATE)
                                                    .getString(PREF_LAUNCHER, null);
    }

    /**
     * Saves the package name into SharedPreferences.
     *
     * @param packageName The package name to be saved. Pass null to remove the preferred launcher.
     */
    private void savePersistentPreferredLauncher(String packageName) {

        SharedPreferences.Editor editor = mContext.getSharedPreferences(PREFS_DEVICE_OWNER,
                                                                        Context.MODE_PRIVATE).edit();

        if (packageName == null) {

            editor.remove(PREF_LAUNCHER);
        } else {

            editor.putString(PREF_LAUNCHER, packageName);
        }
        editor.apply();
    }

    public void addLauncher(String ID, String lPackage, String lClass) {

        launcherMap.put(ID, new ComponentName(lPackage,lClass));
    }

    /**
     * Sets the selected launcher as preferred.
     */
    public void setPreferredLauncher(String launcherID) {

        if(loadPersistentPreferredLauncher() != null)
                                    clearPreferredLauncher();

        ComponentName componentName = (ComponentName) launcherMap.get(launcherID);

        IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_HOME);
        filter.addCategory(Intent.CATEGORY_DEFAULT);

        mDevicePolicyManager.addPersistentPreferredActivity(
                DeviceOwnerReceiver.getComponentName(mContext), filter, componentName);

        savePersistentPreferredLauncher(componentName.getPackageName());
    }


    /**
     * Clears the launcher currently set as preferred.
     *
     */
    public void clearPreferredLauncher() {

        mDevicePolicyManager.clearPackagePersistentPreferredActivities(
                                DeviceOwnerReceiver.getComponentName(mContext),
                                loadPersistentPreferredLauncher());

        savePersistentPreferredLauncher(null);
    }

    /**
     * Clears the launcher currently set as preferred.
     *
     */
    public void clearPreferredLauncherByID(String launcherID) {

        ComponentName componentName = (ComponentName) launcherMap.get(launcherID);

        mDevicePolicyManager.clearPackagePersistentPreferredActivities(
                DeviceOwnerReceiver.getComponentName(mContext),
                componentName.getPackageName());
    }




}
