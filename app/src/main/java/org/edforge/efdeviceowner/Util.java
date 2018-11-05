/*
 * Copyright (C) 2016 The Android Open Source Project
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
import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v4.os.BuildCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import org.edforge.efdeviceowner.cosu.EnableCosuActivity;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Common utility functions.
 */
public class Util {
    private static final String TAG = "Util";

    private static final int DEFAULT_BUFFER_SIZE = 4096;

    private static final String BROADCAST_ACTION_FRP_CONFIG_CHANGED = "com.google.android.gms.auth.FRP_CONFIG_CHANGED";
    private static final String GMSCORE_PACKAGE = "com.google.android.gms";
    private static final String PERSISTENT_DEVICE_OWNER_STATE = "persistentDeviceOwnerState";

    /**
     * Format a friendly datetime for the current locale according to device policy documentation.
     * If the timestamp doesn't represent a real date, it will be interpreted as {@code null}.
     *
     * @return A {@link CharSequence} such as "12:35 PM today" or "June 15, 2033", or {@code null}
     * in the case that {@param timestampMs} equals zero.
     */
    public static CharSequence formatTimestamp(long timestampMs) {
        if (timestampMs == 0) {
            // DevicePolicyManager documentation describes this timestamp as having no effect,
            // so show nothing for this case as the policy has not been set.
            return null;
        }

        return DateUtils.formatSameDayTime(timestampMs, System.currentTimeMillis(),
                DateUtils.FORMAT_SHOW_WEEKDAY, DateUtils.FORMAT_SHOW_TIME);
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    public static boolean isDeviceOwner(Context context) {
        final DevicePolicyManager dpm = getDevicePolicyManager(context);
        return dpm.isDeviceOwnerApp(context.getPackageName());
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    public static boolean isProfileOwner(Context context) {
        final DevicePolicyManager dpm = getDevicePolicyManager(context);
        return dpm.isProfileOwnerApp(context.getPackageName());
    }

    public static boolean isAtLeastM() {
        return Build.VERSION.SDK_INT >= VERSION_CODES.M;
    }

    public static boolean isCosuLaunch(PersistableBundle extras) {
        return extras != null && (extras.get(EnableCosuActivity.BUNDLE_KEY_COSU_CONFIG) != null);
    }

    /** @return Intent for the default home activity */
    public static Intent getHomeIntent() {
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory(Intent.CATEGORY_HOME);
        return intent;
    }

    /** @return IntentFilter for the default home activity */
    public static IntentFilter getHomeIntentFilter() {
        final IntentFilter filter = new IntentFilter(Intent.ACTION_MAIN);
        filter.addCategory(Intent.CATEGORY_HOME);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        return filter;
    }

    private static DevicePolicyManager getDevicePolicyManager(Context context) {
        return (DevicePolicyManager)context.getSystemService(Service.DEVICE_POLICY_SERVICE);
    }

}
