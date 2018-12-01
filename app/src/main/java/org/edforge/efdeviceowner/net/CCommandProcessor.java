package org.edforge.efdeviceowner.net;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.edforge.efdeviceowner.OwnerActivity;
import org.edforge.efdeviceowner.PackageUpdater;
import org.edforge.efdeviceowner.PostProvisioning;
import org.edforge.efdeviceowner.Zip;
import org.edforge.util.TCONST;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.edforge.util.TCONST.INSTALLATION_COMPLETE;
import static org.edforge.util.TCONST.INSTALLATION_PENDING;
import static org.edforge.util.TCONST.NET_STATUS;

/**
 * Created by kevin on 10/21/2018.
 */

public class CCommandProcessor {

    private Context mContext;
    private PackageUpdater mPackageUpdater;
    private PostProvisioning mPostProvisioning;

    private CEF_Command mCommand;
    private LocalBroadcastManager bManager;

    private Zip mZip;
    private String mOutPath;
    private String mTmpPath;
    private File mZipFile;
    private File mTmpFile;
    private long mZipSize;

    InputStream inputStream = null;
    OutputStream outputStream = null;

    byte[] buffer = new byte[1024];
    int read;

    long bytesAvail = 0;
    long bytesRecvd = 0;


    static final String TAG = "CCommandProcessor";

    public CCommandProcessor(Context context) {

        mContext = context;
        mPackageUpdater = new PackageUpdater(mContext);
        mPostProvisioning = new PostProvisioning(mContext);

        // Capture the local broadcast manager
        bManager = LocalBroadcastManager.getInstance(mContext);
    }


    public int process(CEF_Command comm) {

        int result = TCONST.COMMAND_WAIT;

        mCommand = comm;

        switch (mCommand.command) {

            case TCONST.PULL:
                result = TCONST.COMMAND_SENDSTART;
                break;

            case TCONST.PUSH:
            case TCONST.INSTALL:
                result = TCONST.COMMAND_RECVSTART;
                break;

            case TCONST.CLEAN:

                File Clean_Path = new File(Environment.getExternalStorageDirectory() + mCommand.to);

                cleanDirectory(Clean_Path);
                broadcast(NET_STATUS, "Folder Cleared");

                result = TCONST.COMMAND_WAIT;
                break;
        }

        return result;
    }


    public static void removeDirectory(File dir) {

        if (dir.isDirectory()) {

            File[] files = dir.listFiles();

            if (files != null && files.length > 0) {

                for (File aFile : files) {
                    removeDirectory(aFile);
                }
            }
            dir.delete();

        } else {
            dir.delete();
        }
    }

    public static void cleanDirectory(File dir) {

        if (dir.isDirectory()) {

            File[] files = dir.listFiles();

            if (files != null && files.length > 0) {

                for (File aFile : files) {
                    removeDirectory(aFile);
                }
            }
        }
    }

    private String cleanTempPath() {

        String tempPath = OwnerActivity.XFER_PATH + TCONST.EDFORGEZIPTEMP;

        File tempFile = new File(tempPath);

        if (tempFile.exists())
            tempFile.delete();

        return tempPath;
    }

    public String joinPath(String base, String leaf) {

        String result = base;

        if (leaf.matches("/^[\\w\\$]/")) {

            result += File.separator + leaf;
        } else {
            result += leaf;
        }

        return result;
    }


    public String validatePath(String path) {

        String sdcard = "";
        String[] pathArr = path.split("/");

        for (String folder : pathArr) {

            if ((folder != null) && (!folder.isEmpty())) {

                sdcard += File.separator + folder;

                File folderInstance = new File(sdcard);

                if (!folderInstance.exists()) {
                    Log.i(TAG, "Making: " + sdcard);
                    folderInstance.mkdir();
                }
            }
        }

        return path;
    }


    public void validatePaths(String[] paths) {

        for (String folder : paths) {

            validatePath(folder);
        }
    }


    public int prepareRecvData() {

        int result = TCONST.COMMAND_WAIT;

        try {
            if (mCommand.extract) {
                mTmpPath = cleanTempPath();
            } else {
                mTmpPath = joinPath(Environment.getExternalStorageDirectory().getAbsolutePath(), mCommand.to);
            }

            outputStream = new FileOutputStream(mTmpPath);
            bytesAvail = mCommand.size;
            bytesRecvd = 0;

            result = TCONST.COMMAND_RECVDATA;

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }


    public int recvData(DataInputStream socketInStream, DataOutputStream socketOutStream) {

        int result = TCONST.COMMAND_WAIT;

        try {

            if ((bytesAvail > 0 && (read = socketInStream.read(buffer)) != -1)) {


                outputStream.write(buffer, 0, ((read > bytesAvail) ? (int) bytesAvail : read));
                bytesAvail -= read;
                bytesRecvd += read;

                Log.i(TAG, "Bytes Remain/Recvd: " + bytesAvail + "  ::  " + bytesRecvd);

                result = TCONST.COMMAND_RECVACK;
            }

            if (bytesAvail <= 0) {

                outputStream.flush();
                outputStream.close();

                if (mCommand.extract) {

                    mOutPath = joinPath(Environment.getExternalStorageDirectory().getAbsolutePath(), mCommand.to);

                    Log.i(TAG, "Extracting Data");
                    mZip = new Zip(mContext);
                    mZip.open(mTmpPath);
                    broadcast(INSTALLATION_PENDING);
                    mZip.extractAll(mTmpPath, mOutPath);
                    broadcast(INSTALLATION_COMPLETE);
                }

                if (mCommand.app_package != null && mCommand.command.equals(TCONST.INSTALL)) {

                    Log.i(TAG, "Installing Package");
                    broadcast(INSTALLATION_PENDING);

                    mPackageUpdater.updatePackage(mCommand, mTmpPath);
                    Log.i(TAG, "Package Installed");
                }
                result = TCONST.COMMAND_WAIT;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }


    public long prepareSendData() {

        long result = 0L;

        if (mCommand.compress) {

            try {
                mTmpPath = cleanTempPath();
                mZip = new Zip(mContext);

                mZip.compressAssets(Environment.getExternalStorageDirectory() + mCommand.from, mTmpPath, mCommand.recurse);

                mZipFile = new File(mTmpPath);

                inputStream = new FileInputStream(mTmpPath);

                mZipSize = result = mZipFile.length();

            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            Log.e(TAG, "Unsupported Command: " + mCommand.command + " without compress!");
        }

        return result;
    }


    public int sendData(DataOutputStream socketStream) {

        int result = TCONST.COMMAND_WAIT;

        if (mCommand.compress) {

            try {
                if (mZipSize > 0) {

                    read = inputStream.read(buffer);

                    mZipSize -= read;

                    socketStream.write(buffer, 0, read);
                    socketStream.flush();

                    Log.i(TAG, "Sent Bytes: " + read);
                    result = TCONST.COMMAND_SENDACK;
                } else {
                    inputStream.close();
                    socketStream.flush();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "Unsupported");
        }

        return result;
    }


    protected void moveFile(String inputPath, String outputPath) {

        copyFile(inputPath, outputPath);

        new File(inputPath).delete();
    }


    protected void copyFile(String inputPath, String outputPath) {

        OutputStream out = null;
        InputStream in = null;

        byte[] buffer = new byte[1024];
        int read;

        try {

//            System.out.println("File copy:" + inputPath + " -to- " + outputPath);

            try {
                in = new FileInputStream(inputPath);
                out = new FileOutputStream(outputPath);

                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }

                in.close();
                out.flush();
                out.close();

            } catch (java.io.FileNotFoundException e) {
                System.out.println("INFO: Skipping missing file: " + inputPath + " - reason: " + e);

            } catch (IOException e) {
                System.out.println("ERROR: Failed to copy asset file: " + inputPath + " - reason: " + e);

            } finally {

                in = null;
                out = null;
            }
        } catch (Exception e) {
            System.out.println("INFO: File Copy Failed: " + inputPath + " - reason: " + e);
        }
    }

    public void broadcast(String Action, String Msg) {

        Intent msg = new Intent(Action);
        msg.putExtra(TCONST.NAME_FIELD, Msg);

        bManager.sendBroadcast(msg);
    }


    public void broadcast(String Action) {

        Intent msg = new Intent(Action);
        bManager.sendBroadcast(msg);
    }
}
