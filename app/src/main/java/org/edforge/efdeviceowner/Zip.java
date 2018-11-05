package org.edforge.efdeviceowner;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import org.edforge.util.TCONST;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;


public class Zip {

    private ZipFile _zipFile;
    private ZipOutputStream zipOutputStream = null;
    protected StringBuffer masterErr;

    protected String quality;

    static String[] ZIPIGNORE = {};


    private Context mContext;

    private LocalBroadcastManager bManager;

    public Zip(Context _context) {

        init(_context);
    }

    public Zip(ZipFile zipFile, Context _context) {

        _zipFile = zipFile;
        init(_context);
    }


    public Zip(String pathToZipFile, Context _context) throws IOException {

        _zipFile = new ZipFile(pathToZipFile);
        init(_context);
    }


    public void init(Context _context) {

        mContext = _context;

        masterErr = new StringBuffer();

        String ErrText = "FILE OBJECT" + ",\t" + "FOLDER" + "\r\n";
        masterErr.append(ErrText);

        // Capture the local broadcast manager
        //
        bManager = LocalBroadcastManager.getInstance(mContext);
    }


    public void open(String pathToZipFile) throws IOException {

        _zipFile = new ZipFile(pathToZipFile);
    }

    public void close() throws IOException {

        _zipFile.close();
    }


    public void broadcast(String Action, String Msg) {

        // Let the persona know where to look
        Intent msg = new Intent(Action);
        msg.putExtra(TCONST.TEXT_FIELD, Msg);

        bManager.sendBroadcast(msg);
    }


    public void broadcast(String Action, int Msg) {

        // Let the persona know where to look
        Intent msg = new Intent(Action);
        msg.putExtra(TCONST.INT_FIELD, Msg);

        bManager.sendBroadcast(msg);
    }


    public void extractAll(String extractName, String extractPath) throws IOException {

        File targetDir = new File(extractPath);
        int  fileCnt   = 0;

        if(!targetDir.exists() && !targetDir.mkdirs()){
            throw new IOException("Unable to create directory");
        }

        if(!targetDir.isDirectory()){
            throw new IOException("Unable to extract to a non-directory");
        }

        Enumeration<? extends ZipEntry> zipEntries = _zipFile.entries();

        broadcast(TCONST.START_PROGRESSIVE_UPDATE, new Integer(_zipFile.size()).toString());
        broadcast(TCONST.PROGRESS_TITLE, TCONST.ASSET_UPDATE_MSG + extractName + TCONST.PLEASE_WAIT);

        while(zipEntries.hasMoreElements()){

            ZipEntry zipEntry = zipEntries.nextElement();

            String path = extractPath + zipEntry.getName();

            broadcast(TCONST.UPDATE_PROGRESS, new Integer(++fileCnt).toString());

            if(zipEntry.isDirectory()){

				File newDir = new File(path);

				if(!newDir.exists() && !newDir.mkdirs()){
					throw new IOException("Unable to extract the zip entry " + path);
				}
            }
            else {

                broadcast(TCONST.PROGRESS_MSG2, path);

                BufferedInputStream inputStream = new BufferedInputStream(_zipFile.getInputStream(zipEntry));

                File outputFile = new File(path);
                File outputDir = new File(outputFile.getParent());

                if(!outputDir.exists() && !outputDir.mkdirs()){
                    throw new IOException("unable to make directory for entry " + path);
                }

                if(!outputFile.exists() && !outputFile.createNewFile()){
                    throw new IOException("Unable to create directory for " + path);
                }

                BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));

                try {
                    int bytes;
                    int total = 0;

                    while((bytes = inputStream.read()) != -1) {

                        outputStream.write(bytes);
                        total += bytes;
                    }
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
                finally{
                    outputStream.close();
                    inputStream.close();
                }
            }
        }
    }


    public void compressAssets(String inputPath, String outputPath, Boolean recurse) {

        try {
            zipOutputStream = new ZipOutputStream(new FileOutputStream(outputPath));

            File inputFile = new File(inputPath);

            String[] path = inputPath.split("/");

            if (inputFile.isFile()) {
                String zippath =  path[path.length-2] + "/" +  path[path.length-1];
                processFileAssets(inputPath,  zippath);
            }
            else if (inputFile.isDirectory()) {
                processFolderAssets(inputPath, path[path.length-1], recurse);
            }

            // Note: Don't write logs to zip in production - some Android implementations
            // will fail in extraction.
            //
            //writeMasterLogs(LOG_FOLDER);

            zipOutputStream.close();

        } catch (FileNotFoundException ex) {

            Logger.getLogger(Zip.class.getName()).log(Level.SEVERE, null, ex);

        } catch (IOException ex) {

            Logger.getLogger(Zip.class.getName()).log(Level.SEVERE, null, ex);

        } finally {

            try {
                zipOutputStream.close();
            } catch (IOException ex) {
                Logger.getLogger(Zip.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }


    public String[] listFolder(String path, boolean quiet) {

        File folder = new File(path);
        String[]  names = new String[0];
        int       i1 = 0;

        File[] listOfFiles = folder.listFiles();

        if(listOfFiles != null) {

            names = new String[listOfFiles.length];

            if(!quiet)
                System.out.println("Listing folder: " + path);

            for (File fileObj : listOfFiles) {

                names[i1++] = fileObj.getName();

                if(!quiet) {
                    if (fileObj.isFile()) {
                        System.out.println("File " + fileObj.getName());
                    } else if (fileObj.isDirectory()) {
                        System.out.println("Folder " + fileObj.getName());
                    }
                }
            }
        }

        return names;
    }


    protected int processFolderAssets(String inputPath, String outputPath, Boolean recurse ) {

        int      fileCount  = 0;
        String DstPath    = outputPath;
        String[] files      = null;
        String[] folderList = null;
        boolean  skip;

        StringBuffer folderLst = new StringBuffer();
        StringBuffer folderErr = new StringBuffer();

        try {

            if(outputPath.length() > 0) {

                DstPath = outputPath + "/";

                ZipEntry folderZipEntry = new ZipEntry(DstPath);
                zipOutputStream.putNextEntry(folderZipEntry);
            }
            folderList = listFolder(inputPath, false);

            // Paths are relative so don't put '/' path sep on files in base folder
            //
            if (inputPath.length() > 0)
                inputPath += "/";

            for (String objectname : folderList) {

                // Process the .rtignore
                //
                skip = false;

                for(String test : ZIPIGNORE) {
                    if(objectname.equals(test)) {
                        skip = true;
                        System.out.println("Skipping: " + objectname);
                        break;
                    }
                }
                if(skip)
                    continue;

                System.out.println("Processing: " + objectname);

                String srcPath = inputPath + objectname;

                File element = new File(srcPath);

                if(element.isDirectory()) {

                    String outPath = DstPath + objectname;

                    if (recurse) {

                        int subCount = processFolderAssets(srcPath, outPath, true);

                        if(subCount == 0) {
//                                outputFile.delete();
                        }
                        else {
                            fileCount += subCount;
                        }
                    }
                }
                else if (element.isFile()) {

                    fileCount++;

                    // Just do an unhashed copy of the file
                    //
                    String outPath  = outputPath + "/" + objectname;

                    processFileAssets(srcPath, outPath);
                }
                else {
                    System.out.println("Skipping: " + srcPath);

                    String logText = objectname + ",\t" + outputPath + "\r\n";
                    masterErr.append(logText);
                    folderErr.append(logText);
                }
            }
            if(outputPath.length() > 0) {
                zipOutputStream.closeEntry();
            }

        } catch (IOException ex) {

            Logger.getLogger(Zip.class.getName()).log(Level.SEVERE, null, ex);
        }
        return fileCount;
    }


    protected void processFileAssets(String inputPath, String fileName) {

        try {
            // Track the source files actual modified time/date
            // We can then use this on the device to exclude unchanged files.
            //
            long sourceFileTime = new File(inputPath).lastModified();

            ZipEntry zipEntry = new ZipEntry(fileName);

            zipOutputStream.putNextEntry(zipEntry);

            FileInputStream fileInputStream = new FileInputStream(inputPath);
            byte[] buf = new byte[1024];
            int bytesRead;

            // Read the input file by chucks of 1024 bytes
            // and write the read bytes to the zip stream
            while ((bytesRead = fileInputStream.read(buf)) > 0) {
                zipOutputStream.write(buf, 0, bytesRead);
            }

            // close ZipEntry to store the stream to the file
            zipOutputStream.closeEntry();

            // Update the last changed time to match source
            //
            zipEntry.setTime(sourceFileTime);

        } catch (IOException ex) {

            Logger.getLogger(Zip.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
