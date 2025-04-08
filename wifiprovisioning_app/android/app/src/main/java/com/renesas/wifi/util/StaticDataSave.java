package com.renesas.wifi.util;


import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;


public class StaticDataSave {

    public static SharedPreferences saveData = null;

    public static final String mSharedPreferencesName = "com.renesas.wifi.data";

    /*
     * Provisioning *
     */

    /*
     * StaticDataSave.mode = 1 : Soft-AP mode
     * StaticDataSave.mode = 2 : Concurrent mode
     * StaticDataSave.mode = 10 : General AWS IoT (unused)
     * StaticDataSave.mode = 11 : AT-CMD AWS IoT (unused)
     * StaticDataSave.mode = 12 : General AWS IoT
     * StaticDataSave.mode = 13 : AT-CMD AWS IoT
     * StaticDataSave.mode = 14 : Concurrent - General AWS IoT
     * StaticDataSave.mode = 15 : Concurrent - AT-CMD AWS IoT
     *
     * StaticDataSave.mode = 20 : General Azure IoT
     * StaticDataSave.mode = 21 : AT-CMD Azure IoT
     * StaticDataSave.mode = 22 : Concurrent - General Azure IoT
     * StaticDataSave.mode = 23 : Concurrent - AT-CMD Azure IoT
     */

    public static String device = "";
    public static int mode = -1;
    public static final String modeKey = "modeKey";

    public static int socketType = -1;  // 0 : TCP, 1 : TLS

    public static final int SOCKET_PORT_FOR_TCP = 9999;
    public static final int SOCKET_PORT_FOR_TCP1 = 80;
    public static final int SOCKET_PORT_FOR_TLS = 9900;
    public static final int SOCKET_PORT_FOR_TLS1 = 443;

    public static String deviceSSID = "";
    public static String deviceSecurity = "WPA2";
    public static String devicePassword = "1234567890";

    public static String serverURL = "https://www.renesas.com";

    public static final String networkSSIDKey = "networkSSIDKey";
    public static String networkSSID = null;

    public static boolean networkSecurity = false;

    public static int networkSecurityNum = -1;
    public static int isHidden = -1;

    public static Integer enterpriseAuthType = -1;
    public static Integer enterpriseAuthProtocol = -1;
    public static String enterpriseID = "";
    public static String enterprisePassword = "";

    public static String networkPassword = null;
    public static int setSSIDPWResult = -1;
    public static int apConnectionResult = -1;
    public static int rebootResult = -1;

    public static String thingName = null;
    public static final String thingNameKey = "thingNameKey";

    public static String mDeviceName = null;
    public static String mDeviceAddress = null;

    //[[add in v2.4.16
    public static BluetoothDevice mBleDevice = null;
    //]]

    /*
     * AWS IoT *
     */

    public static String region = "ap-northeast-2";
    public static final String regionKey = "regionKey";

    public static final String userNameKey = "userNameKey";
    public static String userName = null;

    public static final String cognitoPoolIdKey = "cognitoPoolIdKey";
    public static String cognitoPoolId = null;

    public static final String bucketNameKey = "bucketNameKey";
    public static String bucketName = null;

    public  static final String doorStateFlagKey = "doorStateFlagKey";
    public static String doorStateFlag = null;  //true : open, false :close

    public  static final String windowStateFlagKey = "windowStateFlagKey";
    public static String windowStateFlag = null;  //true : open, false :close

    public static final String existOTAupdateFlagKey = "existOTAupdateFlagKey";
    public static boolean existOTAupdateFlag = false;

    public static final String OTAupdateProgressFlagKey = "OTAupdateProgressFlagKey";
    public static boolean OTAupdateProgressFlag = false;

    public static final String readyFlagKey = "readyFlagKey";
    public static boolean readyFlag = false;

    public static String macAddress = null;
    public static String softApSSID = null;

    public static int storagePermissionResult = 0;
    public static int locationPermissionResult = 0;

    public static String serverIP = null;
    public static String serverPort = null;
    public static String serverProductID = null;
    public  static final String serverIPKey = "serverIP";
    public  static final String serverPortKey = "serverPort";
    public  static final String serverProductIDKey = "serverPorductID";

    public static String pingAddress = "8.8.8.8";
    public static String svrAddress = "192.168.0.1";
    public static int svrPort = 10195;
    public static String svrUrl = "www.google.com";

    public static int wakeupNum = -1;

    /*
     * Azure IoT *
     */
    public static String azureConString = null;
    public static final String azureConStringKey = "azureConStringKey";

}
