package org.edforge.efdeviceowner;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by kevin on 10/20/2018.
 */

public class PackageUpdater {

    private Context mContext;

    private String ACTION_INSTALL_COMPLETE = "ACTION_INSTALL_COMPLETE";

    public PackageUpdater(Context context) {

        mContext = context;
    }

    public boolean updatePackage(String apkPath) {

        try {
            PackageInstaller pi = mContext.getPackageManager().getPackageInstaller();
            int sessId = pi.createSession(new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL));

            PackageInstaller.Session session = pi.openSession(sessId);

            // .. write updated APK file to out


            long sizeBytes = 0;
            final File file = new File(apkPath);
            if (file.isFile()) {
                sizeBytes = file.length();
            }

            InputStream in = null;
            OutputStream out = null;

            in = new FileInputStream(apkPath);
            out = session.openWrite("EF_Install_Session", 0, sizeBytes);

            int total = 0;
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

            int arbNum = 314159;

            session.commit(createIntentSender(mContext, arbNum));
            session.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return true;
    }


    private IntentSender createIntentSender(Context context, int sessionId) {

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                new Intent(ACTION_INSTALL_COMPLETE),
                0);

        return pendingIntent.getIntentSender();
    }
}
