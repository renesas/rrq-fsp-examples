package com.renesas.wifi.awsiot;


import android.content.Context;
import android.content.SharedPreferences;

import com.amazonaws.regions.Regions;
import com.renesas.wifi.activity.MainActivity;
import com.renesas.wifi.buildOption;
import com.renesas.wifi.util.MyLog;
import com.renesas.wifi.util.StaticDataSave;


public class AWSConfig {

    private static Context appContext;
    private static AWSConfig awsConfigInstance;
    static UserConfig userConfig;

    public static String ThingPreString;
    public static String CUSTOMER_SPECIFIC_ENDPOINT;
    public static String AWS_IOT_POLICY_NAME;
    public static Regions MY_REGION;
    public static String COGNITO_POOL_REGION;
    public static String BUCKET_REGION;

    public static AWSConfig getInstance() {
        if (awsConfigInstance == null)
            awsConfigInstance = new AWSConfig();
        return awsConfigInstance;
    }

    public static void setConfig() {
        appContext = MainActivity.mContext;
        if (appContext != null) {
            userConfig = new UserConfig();
            StaticDataSave.saveData = appContext.getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
            StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);

            if (buildOption.region.equals("ap-northeast-2")) {
                StaticDataSave.region = StaticDataSave.saveData.getString(StaticDataSave.regionKey, "ap-northeast-2");
            } else if (buildOption.region.equals("eu-west-2")) {
                StaticDataSave.region = StaticDataSave.saveData.getString(StaticDataSave.regionKey, "eu-west-2");  //modify for Keith
            } else if (buildOption.region.equals("us-west-2")) {
                StaticDataSave.region = StaticDataSave.saveData.getString(StaticDataSave.regionKey, "us-west-2");
            }

            SharedPreferences.Editor editor = StaticDataSave.saveData.edit();

            if (StaticDataSave.thingName != null) {

                if (StaticDataSave.region != null) {
                    if (StaticDataSave.region.contains("ap-northeast-2")) {
                        CUSTOMER_SPECIFIC_ENDPOINT = userConfig.AP_NORTHEAST_2_SPECIFIC_ENDPOINT;
                        AWS_IOT_POLICY_NAME = userConfig.AP_NORTHEAST_2_AWS_IOT_POLICY_NAME;
                        MY_REGION = userConfig.AP_NORTHEAST_2_MY_REGION;
                        COGNITO_POOL_REGION = userConfig.AP_NORTHEAST_2_COGNITO_POOL_REGION;
                        BUCKET_REGION = userConfig.AP_NORTHEAST_2_BUCKET_REGION;
                    } else if (StaticDataSave.region.contains("us-west-2")) {
                        CUSTOMER_SPECIFIC_ENDPOINT = userConfig.US_WEST_2_SPECIFIC_ENDPOINT;
                        AWS_IOT_POLICY_NAME = userConfig.US_WEST_2_AWS_IOT_POLICY_NAME;
                        MY_REGION = userConfig.US_WEST_2_MY_REGION;
                        COGNITO_POOL_REGION = userConfig.US_WEST_2_COGNITO_POOL_REGION;
                        BUCKET_REGION = userConfig.US_WEST_2_BUCKET_REGION;
                    }
                    else if (StaticDataSave.region.contains("eu-west-2")) {
                        CUSTOMER_SPECIFIC_ENDPOINT = userConfig.EU_WEST_2_SPECIFIC_ENDPOINT;
                        AWS_IOT_POLICY_NAME = userConfig.EU_WEST_2_AWS_IOT_POLICY_NAME;
                        MY_REGION = userConfig.EU_WEST_2_MY_REGION;
                        COGNITO_POOL_REGION = userConfig.EU_WEST_2_COGNITO_POOL_REGION;
                        BUCKET_REGION = userConfig.EU_WEST_2_BUCKET_REGION;
                    }
                }

                //[[for single cognito pool id
                StaticDataSave.cognitoPoolId = userConfig.COGNITO_POOL_ID;
                //]]

                if (StaticDataSave.thingName.equals("PST-DOORLOCK-1")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_1;
                } else if (StaticDataSave.thingName.equals("PST-DOORLOCK-2")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_2;
                } else if (StaticDataSave.thingName.equals("PST-DOORLOCK-3")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_3;
                } else if (StaticDataSave.thingName.equals("PST-DOORLOCK-4")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_4;
                } else if (StaticDataSave.thingName.equals("PST-DOORLOCK-5")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("FAE-DOORLOCK-1")) {
                    StaticDataSave.bucketName = userConfig.FAE_BUCKET_NAME_1;
                } else if (StaticDataSave.thingName.equals("FAE-DOORLOCK-2")) {
                    StaticDataSave.bucketName = userConfig.FAE_BUCKET_NAME_2;
                } else if (StaticDataSave.thingName.equals("FAE-DOORLOCK-3")) {
                    StaticDataSave.bucketName = userConfig.FAE_BUCKET_NAME_3;
                } else if (StaticDataSave.thingName.equals("FAE-DOORLOCK-4")) {
                    StaticDataSave.bucketName = userConfig.FAE_BUCKET_NAME_4;
                } else if (StaticDataSave.thingName.equals("PAE-DOORLOCK-1")) {
                    StaticDataSave.bucketName = userConfig.PAE_BUCKET_NAME_1;
                } else if (StaticDataSave.thingName.equals("NDT-DOORLOCK-1")) {
                    StaticDataSave.bucketName = userConfig.NDT_BUCKET_NAME_1;
                } else if (StaticDataSave.thingName.equals("NDT-DOORLOCK-2")) {
                    StaticDataSave.bucketName = userConfig.NDT_BUCKET_NAME_2;
                } else if (StaticDataSave.thingName.equals("NDT-DOORLOCK-3")) {
                    StaticDataSave.bucketName = userConfig.NDT_BUCKET_NAME_3;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-1")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_1;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-2")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_2;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-3")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_3;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-4")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_4;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-5")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-6")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_6;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-7")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_7;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-8")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_8;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-9")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_9;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-10")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_10;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-11")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_11;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-12")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_12;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-13")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_13;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-14")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_14;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-15")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_15;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-16")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_16;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-17")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_17;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-18")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_18;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-19")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_19;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-20")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_20;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-21")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_21;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-22")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_22;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-23")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_23;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-24")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_24;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-25")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_25;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-26")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_26;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-27")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_27;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-28")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_28;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-29")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_29;
                } else if (StaticDataSave.thingName.equals("EVB-DOORLOCK-30")) {
                    StaticDataSave.bucketName = userConfig.EVB_BUCKET_NAME_30;
                } else if (StaticDataSave.thingName.equals("DIALOG-DOORLOCK-1")) {
                     StaticDataSave.bucketName = userConfig.DIALOG_BUCKET_NAME_1;
                } else if (StaticDataSave.thingName.equals("DIALOG-DOORLOCK-2")) {
                    StaticDataSave.bucketName = userConfig.DIALOG_BUCKET_NAME_2;
                } else if (StaticDataSave.thingName.equals("DIALOG-DOORLOCK-3")) {
                    StaticDataSave.bucketName = userConfig.DIALOG_BUCKET_NAME_3;
                } else if (StaticDataSave.thingName.equals("DIALOG-DOORLOCK-4")) {
                    StaticDataSave.bucketName = userConfig.DIALOG_BUCKET_NAME_4;
                } else if (StaticDataSave.thingName.equals("DIALOG-DOORLOCK-5")) {
                    StaticDataSave.bucketName = userConfig.DIALOG_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-DOORLOCK-6")) {
                    StaticDataSave.bucketName = userConfig.DIALOG_BUCKET_NAME_6;
                } else if (StaticDataSave.thingName.equals("DIALOG-DOORLOCK-7")) {
                    StaticDataSave.bucketName = userConfig.DIALOG_BUCKET_NAME_7;
                } else if (StaticDataSave.thingName.equals("DIALOG-DOORLOCK-8")) {
                    StaticDataSave.bucketName = userConfig.DIALOG_BUCKET_NAME_8;
                } else if (StaticDataSave.thingName.equals("DIALOG-DOORLOCK-9")) {
                    StaticDataSave.bucketName = userConfig.DIALOG_BUCKET_NAME_9;
                } else if (StaticDataSave.thingName.equals("DIALOG-DOORLOCK-10")) {
                    StaticDataSave.bucketName = userConfig.DIALOG_BUCKET_NAME_10;
                } else if (StaticDataSave.thingName.equals("DIALOG-DOORLOCK-11")) {
                    StaticDataSave.bucketName = userConfig.DIALOG_BUCKET_NAME_11;
                } else if (StaticDataSave.thingName.equals("DIALOG-DOORLOCK-12")) {
                    StaticDataSave.bucketName = userConfig.DIALOG_BUCKET_NAME_12;
                } else if (StaticDataSave.thingName.equals("DIALOG-DOORLOCK-13")) {
                    StaticDataSave.bucketName = userConfig.DIALOG_BUCKET_NAME_13;
                } else if (StaticDataSave.thingName.equals("DIALOG-DOORLOCK-14")) {
                    StaticDataSave.bucketName = userConfig.DIALOG_BUCKET_NAME_14;
                } else if (StaticDataSave.thingName.equals("DIALOG-DOORLOCK-15")) {
                    StaticDataSave.bucketName = userConfig.DIALOG_BUCKET_NAME_15;
                } else if (StaticDataSave.thingName.equals("DIALOG-DOORLOCK-16")) {
                    StaticDataSave.bucketName = userConfig.DIALOG_BUCKET_NAME_16;
                } else if (StaticDataSave.thingName.equals("DIALOG-DOORLOCK-17")) {
                    StaticDataSave.bucketName = userConfig.DIALOG_BUCKET_NAME_17;
                } else if (StaticDataSave.thingName.equals("DIALOG-DOORLOCK-18")) {
                    StaticDataSave.bucketName = userConfig.DIALOG_BUCKET_NAME_18;
                } else if (StaticDataSave.thingName.equals("DIALOG-DOORLOCK-19")) {
                    StaticDataSave.bucketName = userConfig.DIALOG_BUCKET_NAME_19;
                } else if (StaticDataSave.thingName.equals("DIALOG-DOORLOCK-20")) {
                    StaticDataSave.bucketName = userConfig.DIALOG_BUCKET_NAME_20;
                } else if (StaticDataSave.thingName.equals("THING-USA-1")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_1;
                } else if (StaticDataSave.thingName.equals("THING-USA-2")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_2;
                } else if (StaticDataSave.thingName.equals("THING-USA-3")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_3;
                } else if (StaticDataSave.thingName.equals("THING-USA-4")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_4;
                } else if (StaticDataSave.thingName.equals("THING-USA-5")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("THING-USA-6")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_6;
                } else if (StaticDataSave.thingName.equals("THING-USA-7")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_7;
                } else if (StaticDataSave.thingName.equals("THING-USA-8")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_8;
                } else if (StaticDataSave.thingName.equals("THING-USA-9")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_9;
                } else if (StaticDataSave.thingName.equals("THING-USA-10")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_10;
                } else if (StaticDataSave.thingName.equals("THING-USA-11")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_11;
                } else if (StaticDataSave.thingName.equals("THING-USA-12")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_12;
                } else if (StaticDataSave.thingName.equals("THING-USA-13")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_13;
                } else if (StaticDataSave.thingName.equals("THING-USA-14")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_14;
                } else if (StaticDataSave.thingName.equals("THING-USA-15")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_15;
                } else if (StaticDataSave.thingName.equals("THING-USA-16")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_16;
                } else if (StaticDataSave.thingName.equals("THING-USA-17")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_17;
                } else if (StaticDataSave.thingName.equals("THING-USA-18")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_18;
                } else if (StaticDataSave.thingName.equals("THING-USA-19")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_19;
                } else if (StaticDataSave.thingName.equals("THING-USA-20")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_20;
                } else if (StaticDataSave.thingName.equals("THING-USA-21")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_21;
                } else if (StaticDataSave.thingName.equals("THING-USA-22")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_22;
                } else if (StaticDataSave.thingName.equals("THING-USA-23")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_23;
                } else if (StaticDataSave.thingName.equals("THING-USA-24")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_24;
                } else if (StaticDataSave.thingName.equals("THING-USA-25")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_25;
                } else if (StaticDataSave.thingName.equals("THING-USA-26")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_26;
                } else if (StaticDataSave.thingName.equals("THING-USA-27")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_27;
                } else if (StaticDataSave.thingName.equals("THING-USA-28")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_28;
                } else if (StaticDataSave.thingName.equals("THING-USA-29")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_29;
                } else if (StaticDataSave.thingName.equals("THING-USA-30")) {
                    StaticDataSave.bucketName = userConfig.USA_BUCKET_NAME_30;
                }
                else if (StaticDataSave.thingName.equals("IOT-SENSOR-1")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-2")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-3")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-4")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-5")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-6")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-7")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-8")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-9")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-10")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-11")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-12")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-13")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-14")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-15")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-16")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-17")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-18")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-19")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-20")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-21")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-22")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-23")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-24")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-25")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-26")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-27")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-28")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-29")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-30")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-31")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-32")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-33")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-34")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-35")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-36")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-37")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-38")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-39")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-40")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-41")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-42")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-43")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-44")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-45")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-46")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-47")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-48")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-49")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("IOT-SENSOR-50")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                }
                else if (StaticDataSave.thingName.equals("DIALOG-IOT-1")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-2")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-3")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-4")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-5")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-6")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-7")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-8")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-9")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-10")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-11")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-12")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-13")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-14")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-15")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-16")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-17")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-18")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-19")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-20")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-21")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-22")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-23")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-24")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-25")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-26")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-27")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-28")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-29")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-30")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-31")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-32")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-33")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-34")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-35")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-36")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-37")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-38")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-39")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                } else if (StaticDataSave.thingName.equals("DIALOG-IOT-40")) {
                    StaticDataSave.bucketName = userConfig.PST_BUCKET_NAME_5;
                }
                else if (StaticDataSave.thingName.equals("DialogDoorLock")) {
                    StaticDataSave.bucketName = userConfig.DIALOG_DOORLOCK_BUCKET_NAME;
                }

                MyLog.i("StaticDataSave.cognitoPoolId = "+StaticDataSave.cognitoPoolId);
                MyLog.i("StaticDataSave.bucketName = "+StaticDataSave.bucketName);
                editor.putString(StaticDataSave.cognitoPoolIdKey, StaticDataSave.cognitoPoolId);
                editor.putString(StaticDataSave.bucketNameKey, StaticDataSave.bucketName);
                editor.commit();
            }
        }

    }


}
