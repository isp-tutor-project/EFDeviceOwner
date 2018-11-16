package org.edforge.efdeviceowner;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import static android.content.Intent.ACTION_PACKAGE_ADDED;
import static android.content.Intent.ACTION_PACKAGE_CHANGED;
import static android.content.Intent.ACTION_PACKAGE_REMOVED;
import static android.content.Intent.ACTION_PACKAGE_REPLACED;
import static android.content.Intent.EXTRA_UID;
import static org.edforge.util.TCONST.ACTION_INSTALL_COMPLETE;
import static org.edforge.util.TCONST.EFOWNER_PACKAGE;
import static org.edforge.util.TCONST.INSTALLATION_COMPLETE;


/**
 * Created by kevin on 11/11/2018.
 */

public class PackageUpdateReceiver extends BroadcastReceiver {

    private static final String TAG = "PackageUpdateReceiver";
    private LocalBroadcastManager bManager;

    PackageInstaller.SessionInfo session;

    @Override
    public void onReceive(Context context, Intent intent) {

        PackageManager mPackageManager;
        PostProvisioning mProvisioningManager;
        int uid;
        String packageName;

                // Capture the local broadcast manager
        bManager = LocalBroadcastManager.getInstance(context);

        Log.d("PackageUpdateReceiver", "Broadcast recieved: ");

        switch(intent.getAction()) {

            case ACTION_PACKAGE_ADDED:

                Log.i(TAG, "PackageUpdateReceiver: ADD COMPLETE");

                mPackageManager = context.getPackageManager();
                mProvisioningManager = new PostProvisioning(context);

                uid = intent.getIntExtra(EXTRA_UID, 0);
                packageName = mPackageManager.getNameForUid(uid);

                Log.i(TAG, "PackageUpdateReceiver: TARGET PACKAGE: " + packageName);

                mProvisioningManager.autoGrantRequestedPermissionsToPackage(packageName);

                broadcast(INSTALLATION_COMPLETE);
                break;

            case ACTION_PACKAGE_REMOVED:

                Log.i(TAG, "PackageUpdateReceiver: REMOVE COMPLETE");
                break;

            case ACTION_PACKAGE_CHANGED:

                Log.i(TAG, "PackageUpdateReceiver: CHANGE COMPLETE");
                break;

            case ACTION_PACKAGE_REPLACED:

                Log.i(TAG, "PackageUpdateReceiver: ADD COMPLETE");

                mPackageManager = context.getPackageManager();
                mProvisioningManager = new PostProvisioning(context);

                uid = intent.getIntExtra(EXTRA_UID, 0);
                packageName = mPackageManager.getNameForUid(uid);

                if(packageName.equals(EFOWNER_PACKAGE)) {

                    context.startActivity(Util.getOwnerLaunchIntent());
                }
                break;

            case ACTION_INSTALL_COMPLETE:

                Log.i(TAG, "PackageUpdateReceiver: UPDATE COMPLETE - Setting Permissions");
//                session = intent.getParcelableExtra(EXTRA_SESSION);
//                mProvisioningManager.autoGrantRequestedPermissionsToPackage(session.getAppPackageName());
                break;
        }
    }

    public void broadcast(String Action) {

        Intent msg = new Intent(Action);
        bManager.sendBroadcast(msg);
    }

}
