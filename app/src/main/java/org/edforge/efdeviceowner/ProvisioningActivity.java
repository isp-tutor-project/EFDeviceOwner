package org.edforge.efdeviceowner;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;

import org.edforge.efdeviceowner.net.CClient;
import org.edforge.util.IThreadComplete;

/**
 * Created by kevin on 11/1/2018.
 */

public class ProvisioningActivity extends Activity implements IThreadComplete {

    private static final String TAG = "ProvisioningActivity";

    private View masterContainer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_provisioning);
//        masterContainer = findViewById(R.id.container);




    }

    @Override
    public void notifyOfThreadComplete(CClient.ClientThread thread, String status) {

    }
}
