package com.renesas.wifi;

import com.amazonaws.regions.Regions;

public class buildOption {

    public final static int FC9000 = 1;
    public final static int DA16200 = 2;
    public final static int SMARTLOCK_BOARD = 3;
    public final static int DEVICE = SMARTLOCK_BOARD;

    public static final String WIFI_SCAN_FILTER = "";
    public static final String WIFI_SCAN_FILTER1 = "DA16200";   //for default
    public static final String WIFI_SCAN_FILTER2 = "DOORLOCK";  //for awsiot
    public static final String WIFI_SCAN_FILTER3 = "DoorLock";  //for SDS
    public static final String WIFI_SCAN_FILTER4 = "THING";  //for awsiot USA
    public static final String WIFI_SCAN_FILTER5 = "SENSOR";  //for sensor

    public static final String ServerURL ="https://www.renesas.com";
    public final static boolean USE_DPM_SET = false;
    public final static boolean AT_COMMAND = true;

    /*
     * awsiot
     */
    public final static String region = "ap-northeast-2";
    public static final Regions REGION = Regions.AP_NORTHEAST_2;

    public final static boolean DEBUG_MODE = true;  // display MQTT topic/message of Publish/Subscribe
    public final static boolean TEST_MODE = true;  //display count number of publish message
    public final static boolean STATIC_THING_NAME = false;
    public final static boolean CONFIRM_DOOR_OPEN = false;
    public final static boolean USE_MONITOR_SERVICE = false;

    // for test
    public static Boolean AUTO_TEST = false;
    public static final int AUTO_TEST_MIN_TIME = 1; // min
    public static final int AUTO_TEST_MAX_TIME = 2; // min
    public static final int AUTO_TEST_FIRST_START_TIME = 1; // min

    //for sensing
    public static Boolean PTIM_SENSING = false;  // true : PTIM sensing, false : MCU sensing

    /*
     * Multi-Provisioning
     */
    public static final String HOTSPOT_SSID = "TestHotspot";

}
