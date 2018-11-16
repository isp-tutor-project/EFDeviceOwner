package org.edforge.efdeviceowner.net;


import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.edforge.util.TCONST;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import static java.lang.Integer.parseInt;
import static org.edforge.util.TCONST.NET_STATUS;


/**
 * Created by kevin on 10/19/2018.
 */

public class CServer {

    private Context                 mContext;
    private ServerSocket            serverSocket;

    private LocalBroadcastManager   bManager;
    private EFNetManager            mNetManager;

    Thread serverThread;

    DataInputStream inStream;
    DataOutputStream outStream;

    CCommandProcessor  mProcessor;

    public static ByteOrder order;

    public static InetAddress SERVERIP ;
    public static final int   CONNECTIONS = 5;
    public static final int   SERVERPORT  = 12007;

    public int serverState = TCONST.START_STATE;

    private Thread      mClientThread;
    private CEF_Command mCommand = new CEF_Command();
    private String ip;

    static final String TAG="CServer";


    public CServer(Context context) {

        mContext    = context;
        mNetManager = EFNetManager.getInstance();

        try {

            SERVERIP = Inet4Address.getByAddress(mNetManager.getIpAddressAsByteArr());

        } catch (UnknownHostException e) {

            e.printStackTrace();
        }

        // Capture the local broadcast manager
        bManager = LocalBroadcastManager.getInstance(mContext);

        mProcessor = new CCommandProcessor(mContext);

        serverThread = new Thread(new ServerThread());
        serverThread.start();

    }


    public void onStop() {

        try {
            if((serverSocket != null) && serverSocket.isBound()) {

                if(mClientThread != null)
                    mClientThread.interrupt();

                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void broadcast(String Action, String Msg) {

        Intent msg = new Intent(Action);
        msg.putExtra(TCONST.NAME_FIELD, Msg);

        bManager.sendBroadcast(msg);
    }


    class ServerThread implements Runnable {

        public void run() {
            Socket socket = null;
            try {

                serverSocket = new ServerSocket(SERVERPORT, CONNECTIONS, SERVERIP);

            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {

                try {

                    broadcast(NET_STATUS, "Waiting for connection:");

                    socket = serverSocket.accept();

                    broadcast(NET_STATUS, "connected");

                    CommunicationThread commThread = new CommunicationThread(socket);

                    mClientThread = new Thread(commThread);
                    mClientThread.start();

                } catch (IOException e) {

                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    class CommunicationThread implements Runnable {

        private Socket clientSocket;
        private byte[] bytes = new byte[4];

        private int cmdSize;


        public CommunicationThread(Socket _clientSocket) {

            clientSocket = _clientSocket;

            try {

                inStream  = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream( )));
                outStream = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream( )));

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public byte[] getData(int size) {

            byte[] bytes = null;
            int    bytesAvail;

            try {
                bytes = new byte[size];

                bytesAvail = inStream.read(bytes, 0,size);

                if(bytesAvail < 0) {
                    bytes = null;

                    Log.i(TAG, "End of Stream:");
                    Thread.currentThread().interrupt();
                }
            } catch (IOException e) {

                Log.i(TAG, "Connection Closed:");
                Thread.currentThread().interrupt();
            }

            return bytes;
        }


        public void runProtocol() {

            byte[]      bytes = null;
            int         bytesAvail;
            ByteBuffer wrapped;
            String ack;

            switch(serverState) {

                // The start state is used to resolve the byte order difference between the client
                // and the server
                //
                case TCONST.START_STATE:

                    try {

                        outStream.writeBytes("STATE" + TCONST.START_STATE);
                        outStream.flush();

                        bytes = getData(4);
                        if (bytes != null) {

                            wrapped = ByteBuffer.wrap(bytes); // big-endian by default
                            int num = wrapped.getInt(); // 1

                            ByteOrder byteOrder = wrapped.order();

                            if (num != 5231) {
                                if (byteOrder == ByteOrder.BIG_ENDIAN) {

                                    CServer.order = ByteOrder.LITTLE_ENDIAN;
                                    wrapped = ByteBuffer.wrap(bytes).order(CServer.order);

                                    num = wrapped.getInt(); // 1
                                } else {

                                    CServer.order = ByteOrder.BIG_ENDIAN;
                                    wrapped = ByteBuffer.wrap(bytes).order(CServer.order);

                                    num = wrapped.getInt(); // 1
                                }
                            }
                            Log.i(TAG, "num: " + num);

                            if(num == 5231) {
                                Log.i(TAG, "SIGNATURE VALID:");
                                serverState = TCONST.COMMAND_WAIT;
                            }
                            else {
                                broadcast(NET_STATUS, "Invalid Signature");

                                Log.e(TAG, "ERROR: SIG FAIL Connection Closed:");
                                Thread.currentThread().interrupt();
                            }
                        }

                    }catch(IOException e) {

                        Log.i(TAG, "Connection Closed:");
                        Thread.currentThread().interrupt();
                    }
                    break;

                case TCONST.COMMAND_WAIT:

                    try {
                        outStream.writeBytes("STATE" + TCONST.COMMAND_WAIT);
                        outStream.flush();

                        bytes = getData(4);

                        if (bytes != null) {

                            wrapped = ByteBuffer.wrap(bytes).order(CServer.order);
                            cmdSize = wrapped.getInt(); // 1

                            Log.i(TAG, "Loading Command:" + cmdSize);
                            serverState = TCONST.COMMAND_PACKET;
                        }

                    }catch (IOException e) {

                        Log.i(TAG, "Connection Closed:");
                        Thread.currentThread().interrupt();
                    }

                    break;

                case TCONST.COMMAND_PACKET:

                    try {
                        outStream.writeBytes("STATE" + TCONST.COMMAND_PACKET);
                        outStream.flush();

                        bytes = getData(cmdSize);

                        if(bytes != null) {

                            String json = new String(bytes, Charset.forName("UTF-8"));

                            Log.i(TAG, "Command Received: " + json);

                            mCommand = new CEF_Command();

                            try {
                                mCommand.loadJSON(new JSONObject(json), null);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            switch(mCommand.command) {

                                case TCONST.PULL:
                                    broadcast(NET_STATUS, "Processing: " + mCommand.command + " : " + mCommand.from);
                                    break;

                                case TCONST.PUSH:
                                case TCONST.INSTALL:
                                    broadcast(NET_STATUS, "Processing: " + mCommand.command + " : " + mCommand.to );
                                    break;


                            }

                            serverState = TCONST.PROCESS_COMMAND;
                        }
                    } catch (IOException e) {

                        Log.i(TAG, "Connection Closed:");
                        Thread.currentThread().interrupt();
                    }
                    break;

                case TCONST.PROCESS_COMMAND:

                    try {
                        outStream.writeBytes("STATE" + TCONST.PROCESS_COMMAND);
                        outStream.flush();

                        // Will transition to either
                        //
                        //        COMMAND_SENDSTART  or COMMAND_RECVSTART
                        //
                        serverState = mProcessor.process(mCommand);

                        bytes = getData(3);
                        ack   = new String(bytes, Charset.forName("UTF-8"));
                        Log.i(TAG, "PROCESSING ACK Received: " + ack);


                    } catch (IOException e) {

                        Log.i(TAG, "Connection Closed:");
                        Thread.currentThread().interrupt();
                    }
                    break;


                case TCONST.COMMAND_RECVSTART:

                    serverState = mProcessor.prepareRecvData();
                    break;


                case TCONST.COMMAND_RECVDATA:

                    try {
                        outStream.writeBytes("STATE" + TCONST.COMMAND_RECVDATA);
                        outStream.flush();

                        serverState = mProcessor.recvData(inStream, outStream);

                    } catch (IOException e) {

                        Log.i(TAG, "Connection Closed:");
                        Thread.currentThread().interrupt();
                    }
                    break;

                case TCONST.COMMAND_RECVACK:

                    try {
                        outStream.writeBytes("STATE" + TCONST.COMMAND_RECVACK);
                        outStream.flush();

                        bytes = getData(3);
                        ack   = new String(bytes, Charset.forName("UTF-8"));
                        //Log.i(TAG, "RECV ACK Received: " + ack);

                        serverState = TCONST.COMMAND_RECVDATA;

                    } catch (IOException e) {

                        Log.i(TAG, "Connection Closed:");
                        Thread.currentThread().interrupt();
                    }
                    break;


                case TCONST.COMMAND_SENDSTART:

                    long fileSize = mProcessor.prepareSendData();

                    try {
                        outStream.writeBytes(Long.toString(fileSize));
                        outStream.flush();

                    } catch (IOException e) {

                        Log.i(TAG, "Connection Closed:");
                        Thread.currentThread().interrupt();
                    }

                    bytes = getData(2);

                    ack = new String(bytes, Charset.forName("UTF-8"));
                    //Log.i(TAG, "SEND START Acknowledgment: " + ack);

                    serverState = TCONST.COMMAND_SENDDATA;
                    break;


                case TCONST.COMMAND_SENDDATA:

                    serverState =  mProcessor.sendData(outStream);

                    break;


                case TCONST.COMMAND_SENDACK:

                    bytes = getData(3);
                    ack   = new String(bytes, Charset.forName("UTF-8"));
                    //Log.i(TAG, "SEND ACK Received: " + ack);

                    serverState = TCONST.COMMAND_SENDDATA;
                    break;

            }
        }


        public void run() {

            while (!Thread.currentThread().isInterrupted()) {

                Log.i(TAG, "Server State: " + serverState);
                runProtocol();
            }

            serverState = TCONST.START_STATE;
        }
    }

}
