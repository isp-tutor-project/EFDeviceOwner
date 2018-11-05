package org.edforge.util;

import org.edforge.efdeviceowner.net.CClient;

/**
 * Created by kevin on 11/1/2018.
 */

public interface IThreadComplete {

    void notifyOfThreadComplete(CClient.ClientThread thread, String status);
}
