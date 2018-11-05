package org.edforge.efdeviceowner.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.IllegalBlockingModeException;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import org.edforge.util.IThreadComplete;
import org.edforge.util.TCONST;
import org.json.JSONException;
import org.json.JSONObject;

import static java.lang.Integer.parseInt;
import static org.edforge.efdeviceowner.net.CServer.getIpAsString;
import static org.edforge.efdeviceowner.net.CServer.getMacAsString;
import static org.edforge.util.TCONST.NET_STATUS;

/**
 * Created by kevin on 10/31/2018.
 */

public class CClient {

    private Context                 mContext;
    private Socket                  clientSocket;
    private LocalBroadcastManager   bManager;

    Thread clientThread;

    DataInputStream inStream;
    DataOutputStream outStream;

    CCommandProcessor  mProcessor;

    public static ByteOrder order;

    public static InetAddress   SERVERIP ;
    public static final String  EdForge = "10.0.0.254";
    public static final int     CONNECTIONS = 5;
    public static final int     SERVERPORT = 12007;

    public int clientState = TCONST.START_STATE;

    private CEF_Command mCommand = new CEF_Command();
    private String ip;

    static final String TAG="CClient";


    public CClient(Context context, IThreadComplete owner) {

        mContext = context;

        try {

            SERVERIP = Inet4Address.getByName(EdForge);

        } catch (UnknownHostException e) {

            e.printStackTrace();
        }

        // Capture the local broadcast manager
        bManager = LocalBroadcastManager.getInstance(mContext);

        mProcessor = new CCommandProcessor(mContext);

        clientThread = new Thread(new ClientThread(owner));
        clientThread.start();

    }

    public void onStop() {

        try {
            if(clientSocket != null && clientSocket.isConnected())
                                                    clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcast(String Action, String Msg) {

        Intent msg = new Intent(Action);
        msg.putExtra(TCONST.NAME_FIELD, Msg);

        bManager.sendBroadcast(msg);
    }


    public class ClientThread implements Runnable {

        public ClientThread(IThreadComplete listener) {

            addListener(listener);
        }

        private final Set<IThreadComplete> listeners
                = new CopyOnWriteArraySet<IThreadComplete>();

        public final void addListener(final IThreadComplete listener) {
            listeners.add(listener);
        }

        public final void removeListener(final IThreadComplete listener) {
            listeners.remove(listener);
        }

        private final void notifyListeners(String status) {
            for (IThreadComplete listener : listeners) {
                listener.notifyOfThreadComplete(this, status);
            }
        }
        
        public void run() {

            String result = "Addr Advertisement: OK";

            try {

                clientSocket = new Socket();
                clientSocket.connect(new InetSocketAddress(SERVERIP, SERVERPORT), 5000);

                if((clientSocket != null) && (clientSocket.isConnected())) {

                    CommunicationThread commThread = new CommunicationThread(clientSocket);
                    new Thread(commThread).start();
                }

            } catch (IOException e) {
                e.printStackTrace();
                result = "Addr Advertisement: " + e.getMessage();
            } catch (IllegalBlockingModeException e) {
                e.printStackTrace();
                result = "Addr Advertisement: " + e.getMessage();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                result = "Addr Advertisement: " + e.getMessage();
            } finally {

                // notify Listeners of result
                //
                broadcast(NET_STATUS, result);
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


        public void run() {

            while (!Thread.currentThread().isInterrupted()) {

                Log.i(TAG, "Server State: " + clientState);

                try {
                    String data = getIpAsString();
                    data += "|" + getMacAsString();

                    Log.i(TAG, "Client Msg: " + data);

                    outStream.writeBytes(data);
                    outStream.flush();

                    clientSocket.close();
                    return;
                } catch (IOException e) {

                    e.printStackTrace();
                }
            }

            clientState = TCONST.START_STATE;
        }
    }
}
