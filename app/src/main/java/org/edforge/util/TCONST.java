//*********************************************************************************
//
//    Copyright(c) 2016-2017  Kevin Willows All Rights Reserved
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
//*********************************************************************************

package org.edforge.util;

// global tutor constants

import android.content.Intent;

public class TCONST {

    public static final String EDFORGE_NETID          = "EdForge";

    public static final String EDFORGE_FOLDER         = "/EdForge/";
    public static final String EDFORGE_UPDATE_FOLDER  = "/EdForge_UPDATE/";
    public static final String EDFORGE_LOG_FOLDER     = "/EdForge_LOG/";
    public static final String EDFORGE_DATA_FOLDER    = "/EdForge_DATA/";
    public static final String EDFORGE_DATA_TRANSFER  = "/EdForge_XFER/";


    // data sources
    public static final String ASSETS          = "ASSETS";
    public static final String RESOURCES       = "RESOURCE";
    public static final String EXTERN          = "EXTERN";
    public static final String DEFINED         = "DEFINED";

    // WIFI_PFX constants
    public static final String WEP         = "WEP";
    public static final String WPA         = "WPA";
    public static final String OPEN        = "OPEN";

    public static final String START_PROGRESSIVE_UPDATE   = "START_PROGRESSIVE_UPDATE";
    public static final String START_INDETERMINATE_UPDATE = "START_INDETERMINATE_UPDATE";
    public static final String UPDATE_PROGRESS            = "UPDATE_PROGRESS";
    public static final String PROGRESS_TITLE             = "PROGRESS_TITLE";
    public static final String PROGRESS_MSG1              = "PROGRESS_MSG1";
    public static final String PROGRESS_MSG2              = "PROGRESS_MSG2";
    public static final String ASSET_UPDATE_MSG           = "Installing Assets: ";


    public static final String PLEASE_WAIT                = " - Please Wait.";


    // Server States
    public static final int START_STATE         = 0;
    public static final int COMMAND_WAIT        = 1;
    public static final int COMMAND_PACKET      = 2;
    public static final int PROCESS_COMMAND     = 3;

    public static final int COMMAND_SENDSTART   = 4;
    public static final int COMMAND_SENDDATA    = 5;
    public static final int COMMAND_SENDACK     = 6;

    public static final int COMMAND_RECVSTART   = 7;
    public static final int COMMAND_RECVDATA    = 8;
    public static final int COMMAND_RECVACK     = 9;


    public static final String EDFORGE_INSTALLED_PACKAGE = "EDFORGE_INSTALLED_PACKAGE";
    public static final String ACTION_INSTALL_COMPLETE   = "ACTION_INSTALL_COMPLETE";


    public static final String PULL         = "PULL";
    public static final String PUSH         = "PUSH";
    public static final String INSTALL      = "INSTALL";
    public static final String CLEAN        = "CLEAN";

    public static final String EDFORGEZIPTEMP = "efztempfile.zip";

    public static final String GO_HOME              = "GO_HOME";
    public static final String SYSTEM_STATUS        = "SYSTEM_STATUS";
    public static final String SLAVE_MODE           = "SLAVE_MODE";
    public static final String USER_MODE            = "USER_MODE";

    public static final String LAUNCH_SETTINGS      = "LAUNCH_SETTINGS";
    public static final String SET_HOME_APP         = "SET_HOME_APP";

    public static final String REQ_REBOOT_DEVICE    = "REQ_REBOOT_DEVICE";
    public static final String REQ_WIPE_DEVICE      = "REQ_WIPE_DEVICE";
    public static final String REQ_ANDROID_BREAKOUT = "REQ_ANDROID_BREAKOUT";

    public static final String REASON               = "REASON";
    public static final String ANDROID_BREAK_OUT    = "ANDROID_BREAK_OUT";
    public static final String REBOOT_DEVICE        = "REBOOT_DEVICE";
    public static final String WIPE_DEVICE          = "WIPE_DEVICE";

    public static final String NAME_FIELD           = "NAME_FIELD";
    public static final String INT_FIELD            = "INT_FIELD";
    public static final String TEXT_FIELD           = "TEXT_FIELD";

    public static final String NET_SWITCH_SUCESS    = "NET_SWITCH_SUCESS";
    public static final String NET_SWITCH_FAILED    = "NET_SWITCH_FAILED";
    public static final String NET_STATUS           = "NET_STATUS";
    public static final String SETUP_MODE           = "SETUP_MODE";
    public static final String COMMAND_MODE         = "COMMAND_MODE";

    public static final String EFOWNER_LAUNCH_INTENT  = "org.edforge.efdeviceowner.EF_DEVICE_OWNER";
    public static final String EFHOME_LAUNCH_INTENT   = "org.edforge.efhomescreen.EF_HOME_SCREEN";
    public static final String EFHOST_LAUNCH_INTENT   = "org.edforge.androidhost.EF_ANDROID_HOST";
    public static final String ASUS_LAUNCH_INTENT     = "com.asus.launcher/com.android.launcher3.Launcher";
    public static final String LAUNCH_HOME            = Intent.ACTION_MAIN;

    public static final String EFOWNER_PACKAGE = "org.edforge.efdeviceowner";
    public static final String EFHOME_PACKAGE  = "org.edforge.efhomescreen";
    public static final String EFHOST_PACKAGE  = "org.edforge.androidhost";

    public static final String ASUSDEF_LAUNCHER     = "default";
    public static final String EFOWNER_LAUNCHER     = "efOwner";
    public static final String EFHOME_LAUNCHER      = "efHome";

    public static final String PLUG_CONNECT     = "android.intent.action.ACTION_POWER_CONNECTED";
    public static final String PLUG_DISCONNECT  = "android.intent.action.ACTION_POWER_DISCONNECTED";

    public static final String EFOWNER_STARTER_INTENT   = "org.edforge.efdeviceowner.EF_DEVICE_OWNER";
    public static final String EFHOST_FINISHER_INTENT   = "org.edforge.androidhost.EFHOST_FINISHER_INTENT";
    public static final String EFHOME_FINISHER_INTENT   = "org.edforge.efhomescreen.EFHOME_FINISHER_INTENT";
    public static final String EFHOME_STARTER_INTENT    = "org.edforge.efhomescreen.EFHOME_STARTER_INTENT";

    public static final String INSTALLATION_PENDING    = "org.edforge.INSTALLATION_PENDING";
    public static final String INSTALLATION_COMPLETE   = "org.edforge.INSTALLATION_COMPLETE";

    public static final String POST_PROV_PREFS     = "post_prov_prefs";
    public static final String KEY_POST_PROV_DONE  = "key_post_prov_done";


}



