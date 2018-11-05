/*
 * Copyright (C) 2015 The Android Open Source Project
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

package org.edforge.efdeviceowner.cosu;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.edforge.efdeviceowner.DeviceOwnerReceiver;
import org.edforge.efdeviceowner.R;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * This activity is started after provisioning is complete in {@link DeviceOwnerReceiver} for
 * COSU devices. It loads a config file and downloads, install, hides and enables apps according
 * to the data in the config file.
 */
public class EnableCosuActivity extends Activity {
    public static final String BUNDLE_KEY_COSU_CONFIG = "cosu-demo-config-location";

    private static final String MODE_CUSTOM = "custom";
    private static final String MODE_DEFAULT = "default";
    private static final String MODE_SINGLE = "single";

    private DownloadManager mDownloadManager;

    private TextView mStatusText;

    private Long mConfigDownloadId;
    private CosuConfig mConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // verify device owner status
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        if (!dpm.isDeviceOwnerApp(getPackageName())) {
            Log.e(CosuUtils.TAG, "TestDPC is not the device owner, cannot set up COSU device.");
            finish();
            return;
        }

        // read the admin bundle
        PersistableBundle persistableBundle = getIntent().getParcelableExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE);
        if (persistableBundle == null) {
            Log.e(CosuUtils.TAG, "No admin extra bundle");
            finish();
            return;
        }

        mDownloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        if (CosuUtils.DEBUG) Log.d(CosuUtils.TAG, "Downloading config file");

//        mStatusText.setText(getString(R.string.setup_cosu_status_download));
    }

    /**
     * Start the actual COSU mode and drop the user into different screens depending on the value
     * of mode.
     * default: Launch the home screen with a default launcher
     * custom: Launch KioskModeActivity (custom launcher) with only the kiosk apps present
     * single: Launch the first of the kiosk apps
     */
    private void startCosuMode() {
        Intent launchIntent = null;
        String mode = mConfig.getMode();
        String[] kioskApps = mConfig.getKioskApps();

        finish();
    }

}
