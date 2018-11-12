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

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.PersistableBundle;
import android.widget.Toast;

import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE;

/**
 * Handles events related to device owner.
 */
public class DeviceOwnerReceiver extends DeviceAdminReceiver {

    private PostProvisioning mProvisioner;

    /**
     * Called on the new profile when device owner provisioning has completed. Device owner
     * provisioning is the process of setting up the device so that its main profile is managed by
     * the mobile device management (MDM) application set up as the device owner.
     */
    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {

        mProvisioner = new PostProvisioning(context);

        if (!mProvisioner.performPostProvisioningOperations(intent)) {
            showToast(context, "Post Provisioning Failed = EdForge");
            return;
        }

        // Enable the profile
        DevicePolicyManager manager = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName mDeviceAdmin = getComponentName(context);

        manager.setProfileName(mDeviceAdmin, context.getString(R.string.profile_name));
        manager.setProfileEnabled(mDeviceAdmin);

        Intent launch = new Intent(context, OwnerActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(launch);
    }

    void showToast(Context context, String msg) {
        String status = "Admin State Change:" + msg;
        Toast.makeText(context, status, Toast.LENGTH_SHORT).show();
    }

    /**
     *  This is called even when owner is set via adb
     */
    @Override
    public void onEnabled(Context context, Intent intent) {

        super.onEnabled(context, intent);
        showToast(context, "Admin Enabled");
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {

        super.onDisableRequested(context, intent);
        return "Admin Will Be Disabled";
    }

    @Override
    public void onDisabled(Context context, Intent intent) {

        super.onDisabled(context, intent);
        showToast(context, "Admin is Disabled");
    }

    @Override
    public void onPasswordChanged(Context context, Intent intent) {

        super.onPasswordChanged(context, intent);
        showToast(context, "Password Changed");
    }

    /**
     * @return A newly instantiated {@link android.content.ComponentName} for this
     * DeviceAdminReceiver.
     */
    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context.getApplicationContext(), DeviceOwnerReceiver.class);
    }

}


