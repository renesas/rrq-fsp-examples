package com.renesas.wifi.awsiot;

import com.amazonaws.regions.Regions;

public class UserConfig {

    /*
     * 1. Filter for Device Provisioning - removed

     * 2. Customer specific IoT endpoint
     *    AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com

     * 3. Cognito pool ID. For this app, pool needs to be unauthenticated pool with AWS IoT permissions.

     * 4. Name of the AWS IoT policy to attach to a newly created certificate

     * 5. Thing of AWS IoT - get from device by provisioning

     * 6. you must first create a bucket using the S3 console before running the app (https://console.aws.amazon.com/s3/).
     *    After creating a bucket, put it's name in the field below.
     */

    public static final String AP_NORTHEAST_2_SPECIFIC_ENDPOINT = "a1kzdt4nun8bnh-ats.iot.ap-northeast-2.amazonaws.com";  //2 Seoul
    public static final String US_WEST_2_SPECIFIC_ENDPOINT = "a1kzdt4nun8bnh-ats.iot.us-west-2.amazonaws.com";  //2 Oregon
    public static final String EU_WEST_2_SPECIFIC_ENDPOINT = "a2tv87mmdfuuy6-ats.iot.eu-west-2.amazonaws.com";  //2 London

    public static final String AP_NORTHEAST_2_AWS_IOT_POLICY_NAME = "DOORLOCK_POLICY";  //4
    public static final String US_WEST_2_AWS_IOT_POLICY_NAME = "USA_POLICY";  //4
    public static final String EU_WEST_2_AWS_IOT_POLICY_NAME = "MyPolicy";

    /*
     * 7. Region of AWS IoT
     */
    public static final Regions AP_NORTHEAST_2_MY_REGION = Regions.AP_NORTHEAST_2;
    public static final Regions US_WEST_2_MY_REGION = Regions.US_WEST_2;
    public static final Regions EU_WEST_2_MY_REGION = Regions.EU_WEST_2;

    /*
     * 8. Region of your Cognito identity pool ID.
     */
    public static final String AP_NORTHEAST_2_COGNITO_POOL_REGION = "ap-northeast-2";
    public static final String US_WEST_2_COGNITO_POOL_REGION = "us-west-2";
    public static final String EU_WEST_2_COGNITO_POOL_REGION = "eu-west-2";

    //[[for single Cognito pool id (Identify-fleet-provision-demo)
    public static final String COGNITO_POOL_ID = "ap-northeast-2:b414964c-595d-4db5-aa3a-babe9dc24f96";
    //]]

    /*
     * 9 Region of your bucket.
     */
    public static final String AP_NORTHEAST_2_BUCKET_REGION = "ap-northeast-2";
    public static final String US_WEST_2_BUCKET_REGION = "us-west-2";
    public static final String EU_WEST_2_BUCKET_REGION = "eu-west-2";  //add for Keith

    public static final String PST_BUCKET_NAME_1 = "bucket-pst-doorlock-1";  //6
    public static final String PST_BUCKET_NAME_2 = "bucket-pst-doorlock-2";  //6
    public static final String PST_BUCKET_NAME_3 = "bucket-pst-doorlock-3";  //6
    public static final String PST_BUCKET_NAME_4 = "bucket-pst-doorlock-4";  //6
    public static final String PST_BUCKET_NAME_5 = "bucket-pst-doorlock-5";  //6

    public static final String FAE_BUCKET_NAME_1 = "bucket-fae-doorlock-1";  //6
    public static final String FAE_BUCKET_NAME_2 = "bucket-fae-doorlock-2";  //6
    public static final String FAE_BUCKET_NAME_3 = "bucket-fae-doorlock-3";  //6
    public static final String FAE_BUCKET_NAME_4 = "bucket-fae-doorlock-4";  //6

    public static final String PAE_BUCKET_NAME_1 = "bucket-pae-doorlock-1";  //6

    public static final String NDT_BUCKET_NAME_1 = "bucket-ndt-doorlock-1";  //6
    public static final String NDT_BUCKET_NAME_2 = "bucket-ndt-doorlock-2";  //6
    public static final String NDT_BUCKET_NAME_3 = "bucket-ndt-doorlock-3";  //6

    public static final String USA_BUCKET_NAME_1  = "bucket-usa-1";  //6
    public static final String USA_BUCKET_NAME_2  = "bucket-usa-2";  //6
    public static final String USA_BUCKET_NAME_3  = "bucket-usa-3";  //6
    public static final String USA_BUCKET_NAME_4  = "bucket-usa-4";  //6
    public static final String USA_BUCKET_NAME_5  = "bucket-usa-5";  //6
    public static final String USA_BUCKET_NAME_6  = "bucket-usa-6";  //6
    public static final String USA_BUCKET_NAME_7  = "bucket-usa-7";  //6
    public static final String USA_BUCKET_NAME_8  = "bucket-usa-8";  //6
    public static final String USA_BUCKET_NAME_9  = "bucket-usa-9";  //6
    public static final String USA_BUCKET_NAME_10 = "bucket-usa-10";  //6
    public static final String USA_BUCKET_NAME_11 = "bucket-usa-11";  //6
    public static final String USA_BUCKET_NAME_12 = "bucket-usa-12";  //6
    public static final String USA_BUCKET_NAME_13 = "bucket-usa-13";  //6
    public static final String USA_BUCKET_NAME_14 = "bucket-usa-14";  //6
    public static final String USA_BUCKET_NAME_15 = "bucket-usa-15";  //6
    public static final String USA_BUCKET_NAME_16 = "bucket-usa-16";  //6
    public static final String USA_BUCKET_NAME_17 = "bucket-usa-17";  //6
    public static final String USA_BUCKET_NAME_18 = "bucket-usa-18";  //6
    public static final String USA_BUCKET_NAME_19 = "bucket-usa-19";  //6
    public static final String USA_BUCKET_NAME_20 = "bucket-usa-20";  //6
    public static final String USA_BUCKET_NAME_21 = "bucket-usa-21";  //6
    public static final String USA_BUCKET_NAME_22 = "bucket-usa-22";  //6
    public static final String USA_BUCKET_NAME_23 = "bucket-usa-23";  //6
    public static final String USA_BUCKET_NAME_24 = "bucket-usa-24";  //6
    public static final String USA_BUCKET_NAME_25 = "bucket-usa-25";  //6
    public static final String USA_BUCKET_NAME_26 = "bucket-usa-26";  //6
    public static final String USA_BUCKET_NAME_27 = "bucket-usa-27";  //6
    public static final String USA_BUCKET_NAME_28 = "bucket-usa-28";  //6
    public static final String USA_BUCKET_NAME_29 = "bucket-usa-29";  //6
    public static final String USA_BUCKET_NAME_30 = "bucket-usa-30";  //6

    public static final String EVB_BUCKET_NAME_1 = "bucket-evb-doorlock-1";  //6
    public static final String EVB_BUCKET_NAME_2 = "bucket-evb-doorlock-2";  //6
    public static final String EVB_BUCKET_NAME_3 = "bucket-evb-doorlock-3";  //6
    public static final String EVB_BUCKET_NAME_4 = "bucket-evb-doorlock-4";  //6
    public static final String EVB_BUCKET_NAME_5 = "bucket-evb-doorlock-5";  //6
    public static final String EVB_BUCKET_NAME_6 = "bucket-evb-doorlock-6";  //6
    public static final String EVB_BUCKET_NAME_7 = "bucket-evb-doorlock-7";  //6
    public static final String EVB_BUCKET_NAME_8 = "bucket-evb-doorlock-8";  //6
    public static final String EVB_BUCKET_NAME_9 = "bucket-evb-doorlock-9";  //6
    public static final String EVB_BUCKET_NAME_10 = "bucket-evb-doorlock-10";  //6
    public static final String EVB_BUCKET_NAME_11 = "bucket-evb-doorlock-11";  //6
    public static final String EVB_BUCKET_NAME_12 = "bucket-evb-doorlock-12";  //6
    public static final String EVB_BUCKET_NAME_13 = "bucket-evb-doorlock-13";  //6
    public static final String EVB_BUCKET_NAME_14 = "bucket-evb-doorlock-14";  //6
    public static final String EVB_BUCKET_NAME_15 = "bucket-evb-doorlock-15";  //6
    public static final String EVB_BUCKET_NAME_16 = "bucket-evb-doorlock-16";  //6
    public static final String EVB_BUCKET_NAME_17 = "bucket-evb-doorlock-17";  //6
    public static final String EVB_BUCKET_NAME_18 = "bucket-evb-doorlock-18";  //6
    public static final String EVB_BUCKET_NAME_19 = "bucket-evb-doorlock-19";  //6
    public static final String EVB_BUCKET_NAME_20 = "bucket-evb-doorlock-20";  //6
    public static final String EVB_BUCKET_NAME_21 = "bucket-evb-doorlock-21";  //6
    public static final String EVB_BUCKET_NAME_22 = "bucket-evb-doorlock-22";  //6
    public static final String EVB_BUCKET_NAME_23 = "bucket-evb-doorlock-23";  //6
    public static final String EVB_BUCKET_NAME_24 = "bucket-evb-doorlock-24";  //6
    public static final String EVB_BUCKET_NAME_25 = "bucket-evb-doorlock-25";  //6
    public static final String EVB_BUCKET_NAME_26 = "bucket-evb-doorlock-26";  //6
    public static final String EVB_BUCKET_NAME_27 = "bucket-evb-doorlock-27";  //6
    public static final String EVB_BUCKET_NAME_28 = "bucket-evb-doorlock-28";  //6
    public static final String EVB_BUCKET_NAME_29 = "bucket-evb-doorlock-29";  //6
    public static final String EVB_BUCKET_NAME_30 = "bucket-evb-doorlock-30";  //6

    public static final String DIALOG_BUCKET_NAME_1 = "";  //6
    public static final String DIALOG_BUCKET_NAME_2 = "";  //6
    public static final String DIALOG_BUCKET_NAME_3 = "";  //6
    public static final String DIALOG_BUCKET_NAME_4 = "";  //6
    public static final String DIALOG_BUCKET_NAME_5 = "";  //6
    public static final String DIALOG_BUCKET_NAME_6 = "";  //6
    public static final String DIALOG_BUCKET_NAME_7 = "";  //6
    public static final String DIALOG_BUCKET_NAME_8 = "";  //6
    public static final String DIALOG_BUCKET_NAME_9 = "";  //6
    public static final String DIALOG_BUCKET_NAME_10 = "";  //6
    public static final String DIALOG_BUCKET_NAME_11 = "";  //6
    public static final String DIALOG_BUCKET_NAME_12 = "";  //6
    public static final String DIALOG_BUCKET_NAME_13 = "";  //6
    public static final String DIALOG_BUCKET_NAME_14 = "";  //6
    public static final String DIALOG_BUCKET_NAME_15 = "";  //6
    public static final String DIALOG_BUCKET_NAME_16 = "";  //6
    public static final String DIALOG_BUCKET_NAME_17 = "";  //6
    public static final String DIALOG_BUCKET_NAME_18 = "";  //6
    public static final String DIALOG_BUCKET_NAME_19 = "";  //6
    public static final String DIALOG_BUCKET_NAME_20 = "";  //6

    public static final String DIALOG_DOORLOCK_BUCKET_NAME = "dialogdoorlock-log";

}
