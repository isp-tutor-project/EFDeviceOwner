/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.edforge.efdeviceowner;

import android.annotation.TargetApi;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Build;
import android.os.PersistableBundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE;
import static android.app.admin.DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED;
import static org.edforge.efdeviceowner.DeviceOwnerReceiver.getComponentName;
import static org.edforge.util.TCONST.ASUSDEF_LAUNCHER;
import static org.edforge.util.TCONST.EFOWNER_LAUNCHER;

/**
 * Task executed after provisioning is done indicated by either the
 * {@link DevicePolicyManager#ACTION_PROVISIONING_SUCCESSFUL} activity intent or the
 * {@link android.app.admin.DeviceAdminReceiver#onProfileProvisioningComplete(Context, Intent)}
 * broadcast.
 *
 * <p>Operations performed:
 * <ul>
 *     <li>self-grant all run-time permissions</li>
 *     <li>enable the launcher activity</li>
 *     <li>start waiting for first account ready broadcast</li>
 * </ul>
 */
public class PostProvisioning {

    private static final String TAG                 = "PostProvisioningTask";
    private static final String POST_PROV_PREFS     = "post_prov_prefs";
    private static final String KEY_POST_PROV_DONE  = "key_post_prov_done";

    private final Context mContext;
    private final DevicePolicyManager mDevicePolicyManager;
    private final SharedPreferences mSharedPrefs;
    private final LauncherManager  mLauncherManager;



    public PostProvisioning(Context context) {
        mContext             = context;
        mDevicePolicyManager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        mSharedPrefs         = context.getSharedPreferences(POST_PROV_PREFS, Context.MODE_PRIVATE);
        mLauncherManager     = new LauncherManager(context);
    }

    public boolean performPostProvisioningOperations(Intent intent) {

        if (isPostProvisioningDone()) {
            return false;
        }
        markPostProvisioningDone();

        autoGrantRequestedPermissionsToSelf();

        mLauncherManager.setPreferredLauncher(EFOWNER_LAUNCHER);
        mLauncherManager.clearPreferredLauncherByID(ASUSDEF_LAUNCHER);
        return true;
    }

    public Intent getPostProvisioningLaunchIntent(Intent intent) {
        // Enable the profile after provisioning is complete.
        Intent launch = null;

        // Retreive the admin extras bundle, which we can use to determine the original context for
        // TestDPCs launch.
        PersistableBundle extras = intent.getParcelableExtra(EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE);

        String packageName    = mContext.getPackageName();
        boolean isDeviceOwner = mDevicePolicyManager.isDeviceOwnerApp(packageName);

        // Drop out quickly if we're neither profile or device owner.
        if (!isDeviceOwner) {
                 return null;
        }

        launch = new Intent(mContext, OwnerActivity.class);
        launch.putExtra(EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE, extras);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return launch;
    }


    private boolean isPostProvisioningDone() {
        return mSharedPrefs.getBoolean(KEY_POST_PROV_DONE, false);
    }
    private void markPostProvisioningDone() {
        mSharedPrefs.edit().putBoolean(KEY_POST_PROV_DONE, true).commit();
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void autoGrantRequestedPermissionsToSelf() {

        String        packageName        = mContext.getPackageName();
        ComponentName adminComponentName = getComponentName(mContext);

        List<String> permissions = getRuntimePermissions(mContext.getPackageManager(), packageName);

        for (String permission : permissions) {

            boolean success = mDevicePolicyManager.setPermissionGrantState(adminComponentName,
                    packageName, permission, PERMISSION_GRANT_STATE_GRANTED);

            Log.d(TAG, "Auto-granting " + permission + ", success: " + success);

            if (!success) {
                Log.e(TAG, "Failed to auto grant permission to self: " + permission);
            }
        }
    }

    public void autoGrantRequestedPermissionsToPackage(String packageName) {

        ComponentName adminComponentName = getComponentName(mContext);

        List<String> permissions = getRuntimePermissions(mContext.getPackageManager(), packageName);

        for (String permission : permissions) {

            boolean success = mDevicePolicyManager.setPermissionGrantState(adminComponentName,
                    packageName, permission, PERMISSION_GRANT_STATE_GRANTED);

            if (success) {
                Log.d(TAG, "Auto-granting: " + permission + ", success");
            }
            else {
                Log.e(TAG, "Failed to auto grant permission to self: " + permission);
            }
        }
    }


    private List<String> getRuntimePermissions(PackageManager packageManager, String packageName) {
        List<String> permissions = new ArrayList<>();
        PackageInfo packageInfo;
        try {
            packageInfo =
                    packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not retrieve info about the package: " + packageName, e);
            return permissions;
        }

        if (packageInfo != null && packageInfo.requestedPermissions != null) {
            for (String requestedPerm : packageInfo.requestedPermissions) {
                if (isRuntimePermission(packageManager, requestedPerm)) {
                    permissions.add(requestedPerm);
                }
            }
        }
        return permissions;
    }

    private boolean isRuntimePermission(PackageManager packageManager, String permission) {
        try {
            PermissionInfo pInfo = packageManager.getPermissionInfo(permission, 0);
            if (pInfo != null) {
                if ((pInfo.protectionLevel & PermissionInfo.PROTECTION_MASK_BASE)
                        == PermissionInfo.PROTECTION_DANGEROUS) {
                    return true;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.i(TAG, "Could not retrieve info about the permission: " + permission);
        }
        return false;
    }
}
