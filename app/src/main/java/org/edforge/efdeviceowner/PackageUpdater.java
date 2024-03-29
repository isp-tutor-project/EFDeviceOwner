package org.edforge.efdeviceowner;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;

import org.edforge.efdeviceowner.net.CEF_Command;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static org.edforge.util.TCONST.ACTION_INSTALL_COMPLETE;
import static org.edforge.util.TCONST.EDFORGE_INSTALLED_PACKAGE;
import static org.edforge.util.TCONST.EFOWNER_PACKAGE;
import static org.edforge.util.TCONST.KEY_POST_PROV_DONE;
import static org.edforge.util.TCONST.POST_PROV_PREFS;

/**
 * Created by kevin on 10/20/2018.
 */


// E/PackageInstaller: Commit of session 855829700 failed: Failed to parse /data/app/vmdl855829700.tmp/EF_Install_Session: AndroidManifest.xml

public class PackageUpdater {

    private Context     mContext;
    private CEF_Command mCommand;
    private int         mReqCode = 1;

    private SharedPreferences mSharedPrefs;


    public PackageUpdater(Context context) {

        mContext = context;
    }

    public boolean updatePackage(CEF_Command command, String apkPath) {

        mCommand        = command;
        mReqCode        = (int)System.currentTimeMillis() % 100000;
        mSharedPrefs    = mContext.getSharedPreferences(POST_PROV_PREFS, Context.MODE_PRIVATE);

        try {
            PackageInstaller               pi = mContext.getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams sp = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            sp.setAppPackageName(mCommand.app_package);

//            if(mCommand.app_package.equals(EFOWNER_PACKAGE)) {
//                mSharedPrefs.edit().putBoolean(KEY_POST_PROV_DONE, false).commit();
//            }

            int sessId = pi.createSession(sp);

            PackageInstaller.Session session = pi.openSession(sessId);

            // .. write updated APK file to out

            long sizeBytes  = 0;
            final File file = new File(apkPath);
            if (file.isFile()) {
                sizeBytes = file.length();
            }

            InputStream in = null;
            OutputStream out = null;

            in = new FileInputStream(apkPath);
            out = session.openWrite("EF_Install_Session", 0, sizeBytes);

            int total     = 0;
            byte[] buffer = new byte[65536];
            int c;

            while ((c = in.read(buffer)) != -1) {
                total += c;
                out.write(buffer, 0, c);
            }
            session.fsync(out);
            in.close();
            out.close();

            System.out.println("InstallApkViaPackageInstaller - Success: streamed apk " + total + " bytes");

            session.commit(getIntentSender(mReqCode++));
            session.close();

//            mContext.sendBroadcast(getInstalledIntent());

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return true;
    }


    private IntentSender getIntentSender(int reqcode) {

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                mContext,
                reqcode,
                getInstalledIntent(),
                0);

        return pendingIntent.getIntentSender();
    }

    private Intent getInstalledIntent() {

        Intent intent = new Intent(ACTION_INSTALL_COMPLETE);
        intent.putExtra(EDFORGE_INSTALLED_PACKAGE, mCommand.app_package);

        return intent;
    }
}
