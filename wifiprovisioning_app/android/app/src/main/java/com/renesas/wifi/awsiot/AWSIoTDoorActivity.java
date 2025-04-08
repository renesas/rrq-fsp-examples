/**
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * <p>
 * http://aws.amazon.com/apache2.0
 * <p>
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package com.renesas.wifi.awsiot;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.PorterDuff;
import android.graphics.drawable.AnimationDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttLastWillAndTestament;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iotdata.AWSIotDataClient;
import com.amazonaws.services.iotdata.model.GetThingShadowRequest;
import com.amazonaws.services.iotdata.model.GetThingShadowResult;
import com.amazonaws.services.iotdata.model.UpdateThingShadowRequest;
import com.amazonaws.services.iotdata.model.UpdateThingShadowResult;
import com.renesas.wifi.BuildInformation;
import com.renesas.wifi.DA16200.activity.SelectDeviceActivity;
import com.renesas.wifi.activity.MainActivity;
import com.renesas.wifi.R;
import com.renesas.wifi.awsiot.log.LogActivity;
import com.renesas.wifi.awsiot.log.S3Util;
import com.renesas.wifi.awsiot.setting.door.DoorSettingActivity;
import com.renesas.wifi.awsiot.shadow.DoorMetaData;
import com.renesas.wifi.awsiot.shadow.DoorlockStatus;
import com.renesas.wifi.buildOption;
import com.renesas.wifi.util.CustomToast;
import com.renesas.wifi.util.MyLog;
import com.renesas.wifi.util.NetworkUtil;
import com.renesas.wifi.util.StaticDataSave;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static android.net.wifi.SupplicantState.COMPLETED;
import static com.renesas.wifi.awsiot.setting.door.DoorFunctionSetFragment.badge_notification;

public class AWSIoTDoorActivity extends Activity {

    public static Regions MY_REGION;
    SubScribeAsyncTask subScribeAsyncTask;

    NotificationManager notificationManager;

    // MQTT Message
    private static final String APP_CONNECT_MESSAGE = "connected";
    private static final String DEVICE_CONNECT_RESPONSE_MESSAGE = "yes";

    private static final String APP_CONTROL_OPEN_MESSAGE = "doorOpen";
    private static final String ATCMD_APP_CONTROL_OPEN_MESSAGE = "0 app_door open";
    private static final String DEVICE_CONTROL_OPEN_RESPONSE_MESSAGE = "opened";

    private static final String APP_CONTROL_CLOSE_MESSAGE = "doorClose";
    private static final String ATCMD_APP_CONTROL_CLOSE_MESSAGE = "0 app_door close";
    private static final String DEVICE_CONTROL_CLOSE_RESPONSE_MESSAGE = "closed";

    private static final String APP_UPDATE_SENSOR_MESSAGE = "updateSensor";
    private static final String ATCMD_APP_UPDATE_SENSOR_MESSAGE = "8 app_shadow update";
    private static final String DEVICE_UPDATE_SENSOR_RESPONSE_MESSAGE = "updated";

    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = "iot_keystore";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "default";

    private View decorView;
    private int uiOptions;

    public static Context mContext;

    private Handler mHandler_device_connect_noResponse;
    private Runnable mRunnable_device_connect_noResponse;

    private Handler mHandler_mqtt_connect_noResponse;
    private Runnable mRunnable_mqtt_connect_noResponse;

    private Handler mHandler_update_sensor_noResponse;
    private Runnable mRunnable_update_sensor_noResponse;

    private Handler mHandler_open_noResponse;
    private Runnable mRunnable_open_noResponse;

    private Handler mHandler_close_noResponse;
    private Runnable mRunnable_close_noResponse;

    private CustomToast customToast = null;
    private Handler mHandler_toast;
    private S3Util S3util;

    private NetworkChangeReceiver mNetworkReceiver = null;

    private LinearLayout ll_debug;
    private TextView tvSubscribeTopic;
    private TextView tvLastSubscribeMessage;
    private TextView tvPublishTopic;
    private TextView tvPublishMessage;
    private ImageView iv_deviceNetworkState;

    public static AWSIotClient mIotAndroidClient;
    public static AWSIotMqttManager mqttManager;
    public static CognitoCachingCredentialsProvider credentialsProvider;
    public static String clientId;
    public static String keystorePath;
    public static String keystoreName;
    public static String keystorePassword;
    public static KeyStore clientKeyStore;
    public static String certificateId;
    public static AWSIotDataClient iotDataClient;

    private TextView textMainKeyName;
    private RelativeLayout layerMainKey;
    private LinearLayout layerNoConnect;
    private LinearLayout layerConnecting;

    private ProgressBar progressingConnecting;
    private ProgressBar progressingOpenClose;

    private MaterialDialog openConfirmDialog = null;
    private ProgressDialog waitDialog;
    private ProgressDialog updateDialog;
    private MaterialDialog terminateDialog = null;
    private MaterialDialog noInternetDialog = null;
    private MaterialDialog checkGPSDialog = null;
    private MaterialDialog noResponseDialog = null;
    private MaterialDialog batteryAlertDialog = null;
    private MaterialDialog temperatureAlertDialog = null;
    private MaterialDialog deviceConnectFailDialog = null;
    public ProgressDialog updatingDialog = null;
    public AlertDialog updateSuccessDialog = null;
    public AlertDialog updateFailDialog = null;

    private ImageView imgConnStat;
    private ImageView imgDisconnStat;
    private ImageView imgRegisterDoorLock;
    private ImageView imgDoorLock;
    private ImageView imgDoorOpen;
    private TextView tv_doorState;
    private TextView batteryText;
    private TextView temperatureText;
    private TextView updatedTimeText;
    private ImageView iv_update_sensor;

    private ImageView iv_notify;
    private TextView tv_notify;
    private ImageView iv_addUser;
    private TextView tv_addUser;
    private ImageView iv_setting;
    private TextView tv_setting;
    private TextView door_badge_setting;

    private final static int BUTTON_CLICK_TIME = 100;
    private final static int GET_SHADOW_TIME = 2000;
    private final static int CONNECT_RECHECK_TIME = 30000;
    private final static int UPDATE_SENSOR_TIME = 60000;
    private final static int SUBCRIBE_TIMEOUT_TIME = 60000; //msec
    private final static int COMMAND_TIMEOUT_TIME = 15000; //msec
    private final static int PROVISIONING_TIMEOUT_TIME = 60000; //msec

    static public int statusBarHeight;

    public static boolean isMqttConnected = false;
    public static boolean isDeviceConnected = false;
    public static boolean messageReceived = false;
    public static boolean fromSetting = false;
    private static boolean isAyncTaskCompleted = true;

    private WifiManager wifiManager;

    public GetShadowTask getStatusShadowTask;
    public GetSensorShadowTask GetSensorShadowTask;

    public UpdateShadowTask updateShadowTask;

    public int publishCount = 0;

    public static AWSConfig awsConfig;
    public static Activity activity;

    //public static int batteryMax_MCU = 3213;  // 6 Volt
    public static int batteryMax_MCU;  // 5.5 Volt
    public static int batteryMin_MCU;  // 4 Volt
    //public static int batteryMin_MCU = 1957;  // 3.5 Volt
    public static int unitBattery_MCU = 0;

    //public static int batteryMax_PTIM = 3213;  // 6 Volt
    public static int batteryMax_PTIM = 2940;  // 5.5 Volt
    //public static int batteryMax_PTIM = 2687;  // 5 Volt
    public static int batteryMin_PTIM = 2240;  // 4 Volt
    //public static int batteryMin_PTIM = 1957;  // 3.5 Volt
    public static int unitBattery_PTIM = 0;

    boolean isDoorStateUpdated = false;
    boolean isTemperatureUpdated = false;
    boolean isBatteryUpdated = false;
    public boolean isOtaUpdated = false;

    long pastDoorStateTimestamp = 0;
    long pastTemperatureTimestamp = 0;
    long pastBatteryTimestamp = 0;
    long pastOtaTimestamp = 0;

    public static AWSIoTDoorActivity instanceDoor;

    public static AWSIoTDoorActivity getInstanceMain() {
        return instanceDoor;
    }

    static boolean toggleOpen = false;

    private final Handler handler = new Handler();

    public Handler DoorHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            AWSEVENT event = AWSEVENT.values()[msg.what];
            switch (event) {

                case E_DEVICE_OPEN_NO_RESPONSE_REMOVE_CALLBACK:
                    MyLog.i("=== E_DEVICE_OPEN_NO_RESPONSE_REMOVE_CALLBACK ===");
                    if (noResponseDialog != null) {
                        if (noResponseDialog.isShowing()) {
                            noResponseDialog.dismiss();
                            noResponseDialog = null;
                        }
                    }
                    if (mHandler_open_noResponse != null) {
                        mHandler_open_noResponse.removeCallbacksAndMessages(null);
                    }
                    break;

                case E_DEVICE_CLOSE_NO_RESPONSE_REMOVE_CALLBACK:
                    MyLog.i("=== E_DEVICE_CLOSE_NO_RESPONSE_REMOVE_CALLBACK ===");
                    if (noResponseDialog != null) {
                        if (noResponseDialog.isShowing()) {
                            noResponseDialog.dismiss();
                            noResponseDialog = null;
                        }
                    }
                    if (mHandler_close_noResponse != null) {
                        mHandler_close_noResponse.removeCallbacksAndMessages(null);
                    }
                    break;

                case E_DEVICE_CONNECT_CHECK:
                    MyLog.i("=== E_DEVICE_CONNECT_CHECK ===");
                    Device_connectCheck();
                    MyLog.i("isDeviceConnected = " + isDeviceConnected);
                    if (isDeviceConnected == false) {
                        postEvent(AWSEVENT.E_DEVICE_CONNECT_CHECK, CONNECT_RECHECK_TIME);
                    }
                    break;

                case E_DEVICE_CONNECT_REMOVE_CALLBACK:
                    MyLog.i("=== E_DEVICE_CONNECT_REMOVE_CALLBACK ===");
                    MyLog.i("isMqttConnected = " + isMqttConnected);
                    if (isMqttConnected) {
                        if (waitDialog != null) {
                            waitDialog.dismiss();
                        }
                    }

                    if (mHandler_device_connect_noResponse != null) {
                        mHandler_device_connect_noResponse.removeCallbacksAndMessages(null);
                    }
                    break;

                case E_MQTT_REMOVE_CALLBACK:
                    MyLog.i("=== E_MQTT_REMOVE_CALLBACK ===");
                    if (mHandler_mqtt_connect_noResponse != null) {
                        mHandler_mqtt_connect_noResponse.removeCallbacksAndMessages(null);
                    }
                    break;

                case E_UPDATE_SENSOR_NO_RESPONSE_REMOVE_CALLBACK:
                    MyLog.i("=== E_UPDATE_SENSOR_NO_RESPONSE_REMOVE_CALLBACK ===");
                    if (updateDialog != null) {
                        if (updateDialog.isShowing()) {
                            updateDialog.dismiss();
                            updateDialog = null;
                        }
                    }
                    if (mHandler_update_sensor_noResponse != null) {
                        mHandler_update_sensor_noResponse.removeCallbacksAndMessages(null);
                    }
                    break;

                case E_WIFI_SET_CHECK:
                    MyLog.i("=== E_WIFI_SET_CHECK ===");
                    break;

                case E_GET_SHADOW:
                    if (StaticDataSave.thingName != null) {
                        if (isAyncTaskCompleted) {
                            MyLog.i("=== E_GET_SHADOW ===");
                            removeEvent(AWSEVENT.E_GET_SHADOW);
                            getShadows();
                            MyLog.i("fromSetting = " + fromSetting);
                            postEvent(AWSEVENT.E_GET_SHADOW, GET_SHADOW_TIME);
                        } else {
                            try {
                                Thread.sleep(1000);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            sendEvent(AWSEVENT.E_GET_SHADOW);
                        }
                    }
                    break;

                case E_UPDATE_SENSOR:
                    if (StaticDataSave.thingName != null) {
                        MyLog.i("=== E_UPDATE_SENSOR ===");
                        removeEvent(AWSEVENT.E_UPDATE_SENSOR);
                        postEvent(AWSEVENT.E_UPDATE_SENSOR, UPDATE_SENSOR_TIME);
                    }
                    break;

                case E_AUTO_TEST: {
                    Random r = new Random();
                    int i1 = r.nextInt(buildOption.AUTO_TEST_MAX_TIME - buildOption.AUTO_TEST_MIN_TIME) + buildOption.AUTO_TEST_MIN_TIME;
                    if (StaticDataSave.thingName != null) {

                        MyLog.i("=== E_AUTO_TEST == " + toggleOpen + " Time  = " + 1);
                        if (toggleOpen == false) {
                            clickImgDoorLocked();
                            toggleOpen = true;
                        } else {
                            clickImgDoorOpened();
                            toggleOpen = false;
                        }
                        postEvent(AWSEVENT.E_AUTO_TEST, 1000 * 60 * i1);
                    }
                }
                break;

                case E_SHOW_BATTERY_ALERT:
                    MyLog.i("=== E_SHOW_BATTERY_ALARM ===");
                    if (batteryAlertDialog == null) {
                        batteryAlertDialog = new MaterialDialog.Builder(mContext)
                                .theme(Theme.LIGHT)
                                .title("Low Battery Warning")
                                .titleColor(mContext.getResources().getColor(R.color.blue3))
                                .titleGravity(GravityEnum.CENTER)
                                .content("The remaining battery capacity of the doorlock is under 10%." + "\n" + "Please change battery of doorlock.")
                                .positiveText("OK")
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        dialog.dismiss();
                                    }
                                })
                                .build();
                        batteryAlertDialog.getWindow().setGravity(Gravity.CENTER);
                        batteryAlertDialog.show();
                        batteryAlertDialog.setCanceledOnTouchOutside(false);
                        batteryAlertDialog.setCancelable(false);
                    }
                    break;

                case E_SHOW_TEMPERATURE_ALERT:
                    MyLog.i("=== E_SHOW_TEMPERATURE_ALARM ===");
                    if (temperatureAlertDialog == null) {
                        temperatureAlertDialog = new MaterialDialog.Builder(mContext)
                                .theme(Theme.LIGHT)
                                .title("High Temperature Warning")
                                .titleColor(mContext.getResources().getColor(R.color.blue3))
                                .titleGravity(GravityEnum.CENTER)
                                .content("A high temperature has been detected from the doorlock sensor." + "\n" + "Please check it.")
                                .positiveText("OK")
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        dialog.dismiss();
                                    }
                                })
                                .build();
                        temperatureAlertDialog.getWindow().setGravity(Gravity.CENTER);
                        temperatureAlertDialog.show();
                        temperatureAlertDialog.setCanceledOnTouchOutside(false);
                        temperatureAlertDialog.setCancelable(false);
                        break;
                    }

                case E_DEVICE_CONNECT_TIMEOUT:
                    MyLog.i("=== E_DEVICE_CONNECT_TIMEOUT ===");

                    if (waitDialog != null) {
                        waitDialog.dismiss();
                        waitDialog = null;
                    }

                    removeEvent(AWSEVENT.E_DEVICE_CONNECT_TIMEOUT);

                    removeEvent(AWSEVENT.E_GET_SHADOW);
                    removeEvent(AWSEVENT.E_UPDATE_SENSOR);

                    sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);
                    sendEvent(AWSEVENT.E_MQTT_REMOVE_CALLBACK);

                    if (deviceConnectFailDialog == null) {
                        deviceConnectFailDialog = new MaterialDialog.Builder(mContext)
                                .theme(Theme.LIGHT)
                                .title("Device connection failure")
                                .titleColor(mContext.getResources().getColor(R.color.blue3))
                                .titleGravity(GravityEnum.CENTER)
                                .content("Cannot connect with the device.\n" +
                                        "Please check the device status and try again.")
                                .positiveText("OK")
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        dialog.dismiss();
                                        Intent main = new Intent(mContext, MainActivity.class);
                                        main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(main);
                                        finishAffinity();
                                    }
                                })
                                .build();
                        deviceConnectFailDialog.getWindow().setGravity(Gravity.CENTER);
                        deviceConnectFailDialog.show();
                        deviceConnectFailDialog.setCanceledOnTouchOutside(false);
                        deviceConnectFailDialog.setCancelable(false);
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    public void sendEvent(AWSEVENT _Event) {
        int m;
        m = _Event.ordinal();
        Message msg = DoorHandler.obtainMessage(m);
        DoorHandler.sendEmptyMessage(msg.what);
    }

    public void sendEvent(AWSEVENT _Event, int _arg1) {
        int m;
        m = _Event.ordinal();
        Message msg = DoorHandler.obtainMessage(m);
        msg.arg1 = _arg1;
        DoorHandler.sendEmptyMessage(msg.what);
    }

    private void postEvent(AWSEVENT _Event, int _time) {
        int m;
        m = _Event.ordinal();
        Message msg = DoorHandler.obtainMessage(m);
        DoorHandler.sendEmptyMessageDelayed(msg.what, _time);
    }

    public void removeEvent(AWSEVENT _Event) {
        int m;
        m = _Event.ordinal();
        Message msg = DoorHandler.obtainMessage(m);
        DoorHandler.removeMessages(msg.what);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.awsiot_activity_door);

        //[[add in v2.4.16
        if (buildOption.AUTO_TEST == true) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        //]]

        getBuildInformation();

        awsConfig = new AWSConfig();
        awsConfig.setConfig();

        mNetworkReceiver = new NetworkChangeReceiver();
        IntentFilter network_filter = new IntentFilter();
        network_filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, network_filter);

        mContext = this;
        instanceDoor = this;
        activity = AWSIoTDoorActivity.this;

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        S3util = new S3Util();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        customToast = new CustomToast(getApplicationContext());
        mHandler_toast = new Handler();

        mHandler_device_connect_noResponse = new Handler();
        mRunnable_device_connect_noResponse = new Runnable() {
            @Override
            public void run() {
                MyLog.i("mRunnable_device_connect_noResponse :: messageReceived = " + messageReceived);
                if (messageReceived == false) {
                    isDeviceConnected = false;
                    if (iv_deviceNetworkState != null) {
                        iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_red);
                        iv_deviceNetworkState.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                        iv_deviceNetworkState.clearAnimation();
                    }
                    layerNoConnect.setVisibility(View.VISIBLE);

                    if (noInternetDialog != null) {
                        noInternetDialog.dismiss();
                        noInternetDialog = null;
                    }
                    layerMainKey.setVisibility(View.GONE);
                    layerConnecting.setVisibility(View.GONE);

                    progressingOpenClose.setVisibility(View.INVISIBLE);

                    if (waitDialog != null) {
                        waitDialog.dismiss();
                    }
                    removeEvent(AWSEVENT.E_DEVICE_CONNECT_TIMEOUT);
                }
            }
        };

        mHandler_mqtt_connect_noResponse = new Handler();
        mRunnable_mqtt_connect_noResponse = new Runnable() {
            @Override
            public void run() {
                MyLog.i("mRunnable_mqtt_connect_noResponse :: messageReceived = " + messageReceived);
                layerNoConnect.setVisibility(View.GONE);

                layerMainKey.setVisibility(View.VISIBLE);

                if (StaticDataSave.readyFlag == true && noInternetDialog == null) {
                    noInternetDialog = new MaterialDialog.Builder(mContext)
                            .theme(Theme.LIGHT)
                            .title("Notice")
                            .titleColor(mContext.getResources().getColor(R.color.blue3))
                            .titleGravity(GravityEnum.CENTER)
                            .content("No internet connection")
                            .contentColor(mContext.getResources().getColor(R.color.black))
                            .contentGravity(GravityEnum.CENTER)
                            .showListener(new DialogInterface.OnShowListener() {
                                @Override
                                public void onShow(DialogInterface dialogInterface) {
                                    final MaterialDialog dialog = (MaterialDialog) dialogInterface;
                                }

                            }).build();

                    noInternetDialog.getWindow().setGravity(Gravity.CENTER);
                    noInternetDialog.show();
                    noInternetDialog.setCanceledOnTouchOutside(false);
                    noInternetDialog.setCancelable(false);
                }

                layerConnecting.setVisibility(View.GONE);

                progressingOpenClose.setVisibility(View.INVISIBLE);

                if (waitDialog != null) {
                    waitDialog.dismiss();
                }
                removeEvent(AWSEVENT.E_DEVICE_CONNECT_TIMEOUT);
            }
        };

        mHandler_update_sensor_noResponse = new Handler();
        mRunnable_update_sensor_noResponse = new Runnable() {
            @Override
            public void run() {
                MyLog.i("mRunnable_update_sensor :: messageReceived = " + messageReceived);

                if (iv_deviceNetworkState != null) {
                    iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_red);
                    iv_deviceNetworkState.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                    iv_deviceNetworkState.clearAnimation();
                }

                if (updateDialog != null) {
                    if (updateDialog.isShowing()) {
                        updateDialog.dismiss();
                        updateDialog = null;
                    }
                }

                if (noResponseDialog == null) {
                    noResponseDialog = new MaterialDialog.Builder(mContext)
                            .theme(Theme.LIGHT)
                            .title("Error")
                            .titleColor(mContext.getResources().getColor(R.color.blue3))
                            .titleGravity(GravityEnum.CENTER)
                            .content("The device does not respond.\n" +
                                    "Would you like to try again?")
                            .contentColor(mContext.getResources().getColor(R.color.black))
                            .negativeText("CANCEL")
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    dialog.dismiss();
                                    noResponseDialog = null;
                                    sendEvent(AWSEVENT.E_UPDATE_SENSOR_NO_RESPONSE_REMOVE_CALLBACK);
                                    sendEvent(AWSEVENT.E_GET_SHADOW);
                                    sendEvent(AWSEVENT.E_UPDATE_SENSOR);
                                }
                            })
                            .positiveText("OK")
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    sendEvent(AWSEVENT.E_UPDATE_SENSOR_NO_RESPONSE_REMOVE_CALLBACK);
                                    dialog.dismiss();
                                    noResponseDialog = null;
                                    updateSensorRequest();
                                }
                            })
                            .showListener(new DialogInterface.OnShowListener() {
                                @Override
                                public void onShow(DialogInterface dialogInterface) {
                                    final MaterialDialog dialog = (MaterialDialog) dialogInterface;
                                }

                            }).build();
                    noResponseDialog.getWindow().setGravity(Gravity.CENTER);
                    noResponseDialog.show();
                    noResponseDialog.setCanceledOnTouchOutside(false);
                    noResponseDialog.setCancelable(false);
                }
            }
        };

        mHandler_open_noResponse = new Handler();
        mRunnable_open_noResponse = new Runnable() {
            @Override
            public void run() {

                if (iv_deviceNetworkState != null) {
                    iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_red);
                    iv_deviceNetworkState.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                    iv_deviceNetworkState.clearAnimation();
                }

                if (noResponseDialog == null) {
                    noResponseDialog = new MaterialDialog.Builder(mContext)
                            .theme(Theme.LIGHT)
                            .title("Error")
                            .titleColor(mContext.getResources().getColor(R.color.blue3))
                            .titleGravity(GravityEnum.CENTER)
                            .content("The door lock does not respond.\n" +
                                    "Would you like to try again?")
                            .contentColor(mContext.getResources().getColor(R.color.black))
                            .negativeText("CANCEL")
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    dialog.dismiss();
                                    noResponseDialog = null;
                                    sendEvent(AWSEVENT.E_DEVICE_OPEN_NO_RESPONSE_REMOVE_CALLBACK);
                                    sendEvent(AWSEVENT.E_GET_SHADOW);
                                    sendEvent(AWSEVENT.E_UPDATE_SENSOR);
                                }
                            })
                            .positiveText("OK")
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    sendEvent(AWSEVENT.E_DEVICE_OPEN_NO_RESPONSE_REMOVE_CALLBACK);
                                    dialog.dismiss();
                                    noResponseDialog = null;
                                    clickImgDoorLocked();
                                }
                            })
                            .showListener(new DialogInterface.OnShowListener() {
                                @Override
                                public void onShow(DialogInterface dialogInterface) {
                                    final MaterialDialog dialog = (MaterialDialog) dialogInterface;
                                }

                            }).build();
                    noResponseDialog.getWindow().setGravity(Gravity.CENTER);
                    noResponseDialog.show();
                    noResponseDialog.setCanceledOnTouchOutside(false);
                    noResponseDialog.setCancelable(false);
                }
                progressingOpenClose.setVisibility(View.INVISIBLE);
            }
        };

        mHandler_close_noResponse = new Handler();
        mRunnable_close_noResponse = new Runnable() {
            @Override
            public void run() {

                if (iv_deviceNetworkState != null) {
                    iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_red);
                    iv_deviceNetworkState.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                    iv_deviceNetworkState.clearAnimation();
                }

                if (noResponseDialog == null) {
                    noResponseDialog = new MaterialDialog.Builder(mContext)
                            .theme(Theme.LIGHT)
                            .title("Error")
                            .titleColor(mContext.getResources().getColor(R.color.blue3))
                            .titleGravity(GravityEnum.CENTER)
                            .content("The door lock does not respond.\n" +
                                    "Would you like to try again?")
                            .contentColor(mContext.getResources().getColor(R.color.black))
                            .negativeText("CANCEL")
                            .onNegative(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    dialog.dismiss();
                                    noResponseDialog = null;
                                    sendEvent(AWSEVENT.E_DEVICE_CLOSE_NO_RESPONSE_REMOVE_CALLBACK);
                                    sendEvent(AWSEVENT.E_GET_SHADOW);
                                    sendEvent(AWSEVENT.E_UPDATE_SENSOR);
                                }
                            })
                            .positiveText("OK")
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    sendEvent(AWSEVENT.E_DEVICE_CLOSE_NO_RESPONSE_REMOVE_CALLBACK);
                                    dialog.dismiss();
                                    noResponseDialog = null;
                                    clickImgDoorOpened();
                                }
                            })
                            .showListener(new DialogInterface.OnShowListener() {
                                @Override
                                public void onShow(DialogInterface dialogInterface) {
                                    final MaterialDialog dialog = (MaterialDialog) dialogInterface;
                                }

                            }).build();
                    noResponseDialog.getWindow().setGravity(Gravity.CENTER);
                    noResponseDialog.show();
                    noResponseDialog.setCanceledOnTouchOutside(false);
                    noResponseDialog.setCancelable(false);
                }
                progressingOpenClose.setVisibility(View.INVISIBLE);
            }
        };

        initMainResource();

        if (iv_update_sensor != null) {
            iv_update_sensor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Thread.sleep(BUTTON_CLICK_TIME);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (StaticDataSave.thingName != null) {
                        if (progressingOpenClose.getVisibility() != View.VISIBLE) {
                            MyLog.i("isMqttConnected = " + isMqttConnected + ", isDeviceConnected = " + isDeviceConnected);
                            if (isMqttConnected && isDeviceConnected) {
                                removeEvent(AWSEVENT.E_GET_SHADOW);
                                removeEvent(AWSEVENT.E_UPDATE_SENSOR);
                                updateSensorRequest();
                            } else if (isMqttConnected == false) {
                                mHandler_toast.post(new ToastRunnable("The network connection is uncertain"));
                            } else if (isDeviceConnected == false) {
                                mHandler_toast.post(new ToastRunnable("It is not connected to the device"));
                            }
                        } else {
                            mHandler_toast.post(new ToastRunnable("Another job is being processed"));
                        }
                    }
                }
            });
        }

        if (imgDoorLock != null) {
            imgDoorLock.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        Thread.sleep(BUTTON_CLICK_TIME);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    clickImgDoorLocked();
                }
            });
        }

        if (imgDoorOpen != null) {
            imgDoorOpen.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        Thread.sleep(BUTTON_CLICK_TIME);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    clickImgDoorOpened();
                }
            });
        }

        iv_notify.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    iv_notify.setScaleX(0.8f);
                    iv_notify.setScaleY(0.8f);
                    iv_notify.setColorFilter(getResources().getColor(R.color.blue3), PorterDuff.Mode.MULTIPLY);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    try {
                        Thread.sleep(BUTTON_CLICK_TIME);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    iv_notify.setScaleX(1.0f);
                    iv_notify.setScaleY(1.0f);
                    iv_notify.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.MULTIPLY);
                }
                return false;
            }
        });

        iv_notify.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AWSIoTDoorActivity.this, LogActivity.class);
                startActivity(intent);
                MyLog.i("=== click Notify Button === ");
            }
        });

        iv_addUser.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    iv_addUser.setScaleX(0.8f);
                    iv_addUser.setScaleY(0.8f);
                    iv_addUser.setColorFilter(getResources().getColor(R.color.blue3), PorterDuff.Mode.MULTIPLY);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    try {
                        Thread.sleep(BUTTON_CLICK_TIME);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    iv_addUser.setScaleX(1.0f);
                    iv_addUser.setScaleY(1.0f);
                    iv_addUser.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.MULTIPLY);
                }
                return false;
            }
        });

        iv_addUser.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {

                MyLog.i("=== click addUser Button === ");

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        iv_setting.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    iv_setting.setScaleX(0.8f);
                    iv_setting.setScaleY(0.8f);
                    iv_setting.setColorFilter(getResources().getColor(R.color.blue3), PorterDuff.Mode.MULTIPLY);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    try {
                        Thread.sleep(BUTTON_CLICK_TIME);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    iv_setting.setScaleX(1.0f);
                    iv_setting.setScaleY(1.0f);
                    iv_setting.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.MULTIPLY);
                }
                return false;
            }
        });

        iv_setting.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent main = new Intent(AWSIoTDoorActivity.this, DoorSettingActivity.class);
                startActivity(main);
                MyLog.i("=== click Setting Button === ");
            }
        });
    }

    public void getBuildInformation() {
        MyLog.i(" ===========================");
        MyLog.i(" Building date  = " + BuildInformation.RELEASE_DATE);
        MyLog.i(" App version  = " + BuildInformation.RELEASE_VERSION);
        MyLog.i(" Constructor  = " + BuildInformation.RELEASE_CONSTRUCTOR);
        MyLog.i(" ===========================");
    }

    public void initMqtt() {

        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not guarantee uniqueness.

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.bindProcessToNetwork(null);

        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);
        StaticDataSave.cognitoPoolId = StaticDataSave.saveData.getString(StaticDataSave.cognitoPoolIdKey, null);

        StaticDataSave.region = buildOption.region;
        SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
        editor.putString(StaticDataSave.regionKey, StaticDataSave.region);
        editor.commit();
        MyLog.i("StaticDataSave.region = " + StaticDataSave.region);
        if (StaticDataSave.region != null) {
            if (StaticDataSave.region.equals("ap-northeast-2")) {
                MY_REGION = Regions.AP_NORTHEAST_2;
            } else if (StaticDataSave.region.equals("us-west-2")) {
                MY_REGION = Regions.US_WEST_2;
            } else if (StaticDataSave.region.equals("eu-west-2")) {
                MY_REGION = Regions.EU_WEST_2;
            }
        }
        MyLog.i("StaticDataSave.cognitoPoolId = " + StaticDataSave.cognitoPoolId);
        if (StaticDataSave.thingName != null) {
            clientId = StaticDataSave.thingName + "-" + UUID.randomUUID().toString();
            MyLog.i("clientId = " + clientId);
        }

        // Initialize the AWS Cognito credentials provider

        if (StaticDataSave.cognitoPoolId != null) {
            MyLog.i("StaticDataSave.cognitoPoolId = " + StaticDataSave.cognitoPoolId);
            credentialsProvider = new CognitoCachingCredentialsProvider(
                    getApplicationContext(), // context
                    StaticDataSave.cognitoPoolId,
                    MY_REGION // Region
            );
        }


        Region region = Region.getRegion(MY_REGION);

        // MQTT Client

        mqttManager = new AWSIotMqttManager(clientId, awsConfig.CUSTOMER_SPECIFIC_ENDPOINT);
        // Set keepalive to 10 seconds.  Will recognize disconnects more quickly but will also send
        // MQTT pings every 10 seconds.
        mqttManager.setKeepAlive(10);

        // Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
        // AWS IoT will publish this message to alert other clients.
        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
                "Android client lost connection", AWSIotMqttQos.QOS0);
        mqttManager.setMqttLastWillAndTestament(lwt);
        mqttManager.setAutoReconnect(true);


        // IoT Client (for creation of certificate if needed)
        mIotAndroidClient = new AWSIotClient(credentialsProvider);
        mIotAndroidClient.setRegion(region);

        connectMqtt();
    }

    public void connectMqtt() {
        MyLog.i("clientId = " + clientId);

        try {
            mqttManager.connect(getCredentialsProvider(), new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status, final Throwable throwable) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (status == AWSIotMqttClientStatus.Connecting) {
                                MyLog.i("AWSIotMqttClientStatus :: Connecting");
                                isMqttConnected = false;
                                Animation startAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink_animation);
                                imgConnStat.setVisibility(View.VISIBLE);
                                imgConnStat.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                                imgConnStat.startAnimation(startAnimation);
                                imgDisconnStat.clearAnimation();
                                imgDisconnStat.setVisibility(View.INVISIBLE);
                                mHandler_mqtt_connect_noResponse.postDelayed(mRunnable_mqtt_connect_noResponse, SUBCRIBE_TIMEOUT_TIME);
                            } else if (status == AWSIotMqttClientStatus.Connected) {
                                MyLog.i("AWSIotMqttClientStatus :: Connected");
                                mHandler_toast.post(new ToastRunnable("Connected to AWS server"));
                                isMqttConnected = true;

                                if (isDeviceConnected) {
                                    if (waitDialog != null) {
                                        waitDialog.dismiss();
                                    }
                                    removeEvent(AWSEVENT.E_DEVICE_CONNECT_TIMEOUT);
                                }

                                subScribeAsyncTask = new SubScribeAsyncTask();
                                subScribeAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                                imgConnStat.setVisibility(View.VISIBLE);
                                imgConnStat.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                                imgConnStat.clearAnimation();
                                imgDisconnStat.clearAnimation();
                                imgDisconnStat.setVisibility(View.INVISIBLE);
                                sendEvent(AWSEVENT.E_MQTT_REMOVE_CALLBACK);
                                sendEvent(AWSEVENT.E_DEVICE_CONNECT_CHECK);
                                sendEvent(AWSEVENT.E_GET_SHADOW);
                                sendEvent(AWSEVENT.E_UPDATE_SENSOR);

                            } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                MyLog.i("AWSIotMqttClientStatus :: Reconnecting");
                                if (throwable != null) {
                                    MyLog.e("Connection error." + throwable);
                                }
                                isMqttConnected = false;
                                Animation startAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink_animation);

                                removeEvent(AWSEVENT.E_GET_SHADOW);
                                removeEvent(AWSEVENT.E_UPDATE_SENSOR);

                                try {
                                    if (subScribeAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                                        subScribeAsyncTask.cancel(true);
                                    }
                                } catch (Exception e) {
                                }

                                imgConnStat.setVisibility(View.VISIBLE);
                                imgConnStat.setColorFilter(Color.YELLOW, PorterDuff.Mode.MULTIPLY);
                                imgConnStat.startAnimation(startAnimation);
                                imgDisconnStat.clearAnimation();
                                imgDisconnStat.setVisibility(View.INVISIBLE);
                            } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                MyLog.i("AWSIotMqttClientStatus :: ConnectionLost");
                                if (throwable != null) {
                                    MyLog.e("Connection error. >>> " + throwable);
                                }
                                mHandler_toast.post(new ToastRunnable("Disconnected to AWS server"));
                                isMqttConnected = false;
                                imgConnStat.clearAnimation();
                                imgConnStat.setVisibility(View.INVISIBLE);
                                imgDisconnStat.setVisibility(View.VISIBLE);
                                imgDisconnStat.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                                imgDisconnStat.clearAnimation();

                                removeEvent(AWSEVENT.E_GET_SHADOW);
                                removeEvent(AWSEVENT.E_UPDATE_SENSOR);

                                try {
                                    if (subScribeAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                                        subScribeAsyncTask.cancel(true);
                                    }
                                } catch (Exception e) {
                                }

                            } else {
                                MyLog.i("AWSIotMqttClientStatus :: ERROR");
                                mHandler_toast.post(new ToastRunnable("Error occurred while connecting to the AWS server."));
                                isMqttConnected = false;

                                imgConnStat.clearAnimation();
                                imgConnStat.setVisibility(View.INVISIBLE);
                                imgDisconnStat.setVisibility(View.VISIBLE);
                                imgDisconnStat.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                                imgDisconnStat.clearAnimation();
                                sendEvent(AWSEVENT.E_MQTT_REMOVE_CALLBACK);

                                removeEvent(AWSEVENT.E_GET_SHADOW);
                                removeEvent(AWSEVENT.E_UPDATE_SENSOR);

                                try {
                                    if (subScribeAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                                        subScribeAsyncTask.cancel(true);
                                    }
                                } catch (Exception e) {

                                }
                            }
                        }
                    });
                }
            });
        } catch (final Exception e) {
            MyLog.e("Connection error." + e);
            mHandler_toast.post(new ToastRunnable("Error occurred while connecting to the AWS server."));
            isMqttConnected = false;
            Animation startAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink_animation);
            imgConnStat.setVisibility(View.INVISIBLE);
            imgConnStat.clearAnimation();
            imgDisconnStat.setVisibility(View.VISIBLE);
            imgDisconnStat.startAnimation(startAnimation);
            imgDisconnStat.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);

            removeEvent(AWSEVENT.E_GET_SHADOW);
            removeEvent(AWSEVENT.E_UPDATE_SENSOR);

            try {
                if (subScribeAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                    subScribeAsyncTask.cancel(true);
                }
            } catch (Exception e1) {
            }
        }
    }

    public void disconnectMqtt() {
        try {
            mqttManager.disconnect();
        } catch (Exception e) {
            MyLog.e("Disconnect error. >>> " + e);
        }
    }

    private void Device_connectCheck() {
        MyLog.i("=== Device_connectCheck() ===");
        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);
        final String pubTopic = StaticDataSave.thingName + "/" + "AppControl";
        final String msg = APP_CONNECT_MESSAGE;
        tvPublishTopic.setText(pubTopic);
        tvPublishMessage.setText(msg);

        try {
            mqttManager.publishString(msg, pubTopic, AWSIotMqttQos.QOS0);
            MyLog.i("=================================");
            MyLog.i("Message published:");
            MyLog.i("   pubTopic: " + pubTopic);
            MyLog.i(" Message: " + msg);
            MyLog.i("=================================");
            if (iv_deviceNetworkState != null) {
                iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_yellow);
                iv_deviceNetworkState.setColorFilter(Color.YELLOW, PorterDuff.Mode.MULTIPLY);
                Animation startAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink_animation);
                iv_deviceNetworkState.startAnimation(startAnimation);
            }
            mHandler_device_connect_noResponse.postDelayed(mRunnable_device_connect_noResponse, SUBCRIBE_TIMEOUT_TIME);
            MyLog.i("mHandler_device_connect_noResponse.postDelayed(mRunnable_device_connect_noResponse, SUBCRIBE_TIMEOUT_TIME) 1");
        } catch (Exception e) {
            MyLog.e("Publish error. >>> " + e);
        }
    }

    private void doorOpen() {
        SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);
        final String topic = StaticDataSave.thingName + "/" + "AppControl";
        String msg = "";
        publishCount++;

        if (buildOption.TEST_MODE == true) {
            msg = APP_CONTROL_OPEN_MESSAGE + " <" + publishCount + "> " + sfd.format(new Date());
        } else {
            msg = APP_CONTROL_OPEN_MESSAGE;
        }

        tvPublishTopic.setText(topic);
        tvPublishMessage.setText(msg);

        try {
            mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
            MyLog.i("=================================");
            MyLog.i("Message published:");
            MyLog.i("   pubTopic: " + topic);
            MyLog.i(" Message: " + msg);
            MyLog.i("=================================");

            mHandler_device_connect_noResponse.postDelayed(mRunnable_device_connect_noResponse, SUBCRIBE_TIMEOUT_TIME);
            MyLog.i("mHandler_device_connect_noResponse.postDelayed(mRunnable_device_connect_noResponse, SUBCRIBE_TIMEOUT_TIME) 2");

        } catch (Exception e) {
            MyLog.e("Publish error. >>> " + e);
        }

    }

    private void doorClose() {

        SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);
        final String topic = StaticDataSave.thingName + "/" + "AppControl";
        String msg = "";
        publishCount++;

        if (buildOption.TEST_MODE == true) {
            msg = APP_CONTROL_CLOSE_MESSAGE + " <" + publishCount + "> " + sfd.format(new Date());
        } else {
            msg = APP_CONTROL_CLOSE_MESSAGE;
        }

        tvPublishTopic.setText(topic);
        tvPublishMessage.setText(msg);

        try {
            mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
            MyLog.i("=================================");
            MyLog.i("Message published:");
            MyLog.i("   pubTopic: " + topic);
            MyLog.i(" Message: " + msg);
            MyLog.i("=================================");

            mHandler_device_connect_noResponse.postDelayed(mRunnable_device_connect_noResponse, SUBCRIBE_TIMEOUT_TIME);
            MyLog.i("mHandler_device_connect_noResponse.postDelayed(mRunnable_device_connect_noResponse, SUBCRIBE_TIMEOUT_TIME) 3");
        } catch (Exception e) {
            MyLog.e("Publish error. >>> " + e);
        }
    }

    private void updateSensorRequest() {
        if (updateDialog == null) {
            updateDialog = new ProgressDialog(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
            updateDialog.setMessage("Reading from doorlock sensor ...");
            updateDialog.show();

            int displayWidth = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowMetrics windowMetrics = getWindowManager().getCurrentWindowMetrics();
                Insets insets = windowMetrics.getWindowInsets()
                        .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
                displayWidth = windowMetrics.getBounds().width() - insets.left - insets.right;
            } else {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                displayWidth = displayMetrics.widthPixels;
            }

            WindowManager.LayoutParams params = updateDialog.getWindow().getAttributes();
            int dialogWindowWidth = (int) (displayWidth * 0.8f);
            params.width = dialogWindowWidth;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            updateDialog.getWindow().setAttributes((WindowManager.LayoutParams) params);
            updateDialog.setCanceledOnTouchOutside(false);
            updateDialog.setCancelable(false);
        }

        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);
        final String topic = StaticDataSave.thingName + "/" + "AppControl";
        final String msg = APP_UPDATE_SENSOR_MESSAGE;
        tvPublishTopic.setText(topic);
        tvPublishMessage.setText(msg);

        try {
            mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
            MyLog.i("=================================");
            MyLog.i("Message published:");
            MyLog.i("   pubTopic: " + topic);
            MyLog.i(" Message: " + msg);
            MyLog.i("=================================");

            mHandler_update_sensor_noResponse.postDelayed(mRunnable_update_sensor_noResponse, SUBCRIBE_TIMEOUT_TIME);

        } catch (Exception e) {
            MyLog.e("Publish error. >>> " + e);
        }

    }

    private void sensorStatusUpdated(String statusState) {

        Gson gson = new Gson();
        DoorlockStatus ds = gson.fromJson(statusState, DoorlockStatus.class);

        Gson gson1 = new Gson();
        DoorMetaData dm = gson1.fromJson(statusState, DoorMetaData.class);

        if (dm.metadata.reported.doorState != null) {

            long curTimestamp = dm.metadata.reported.doorState.timestamp;
            Date dt = new Date(curTimestamp * 1000);
            SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
            updatedTimeText.setText(sfd.format(dt));

            if (pastDoorStateTimestamp != curTimestamp) {
                isDoorStateUpdated = true;
            } else {
                isDoorStateUpdated = false;
            }

            MyLog.i("isShadowUpdated = " + String.valueOf(isDoorStateUpdated));

            if (isDoorStateUpdated) {
                if (ds.state.reported.doorState != null) {
                    MyLog.i(String.format("doorState : %s", ds.state.reported.doorState));
                }

                MyLog.i(String.format("doorStateChange :  %d", ds.state.reported.doorStateChange));
                MyLog.i(String.format("DoorOpenMode :  %s", ds.state.reported.DoorOpenMode));
                MyLog.i(String.format("Battery :  %f", ds.state.reported.battery));
                MyLog.i(String.format("Temperature :  %f", ds.state.reported.temperature));
                MyLog.i(String.format("Doorbell :  %s", ds.state.reported.doorBell));

                if (ds.state.reported.doorState != null && ds.state.reported.openMethod != null) {
                    if (ds.state.reported.doorState.equals("true")) {  //opened
                        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                        tv_doorState.setText(R.string.door_opened);
                        StaticDataSave.doorStateFlag = StaticDataSave.saveData.getString(StaticDataSave.doorStateFlagKey, null);
                        if (StaticDataSave.doorStateFlag != null) {
                            if (StaticDataSave.doorStateFlag.equals("false")) {
                                if (progressingOpenClose != null) {
                                    progressingOpenClose.setVisibility(View.INVISIBLE);
                                }
                                imgDoorLock.setVisibility(View.INVISIBLE);
                                imgDoorOpen.setVisibility(View.VISIBLE);
                            }
                        }
                        StaticDataSave.doorStateFlag = "true";
                        SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                        editor.putString(StaticDataSave.doorStateFlagKey, StaticDataSave.doorStateFlag);
                        editor.commit();

                    } else if (ds.state.reported.doorState.equals("false")) {  //closed
                        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                        tv_doorState.setText(R.string.door_closed);
                        StaticDataSave.doorStateFlag = StaticDataSave.saveData.getString(StaticDataSave.doorStateFlagKey, null);
                        if (StaticDataSave.doorStateFlag != null) {
                            if (StaticDataSave.doorStateFlag.equals("true")) {
                                if (progressingOpenClose != null) {
                                    progressingOpenClose.setVisibility(View.INVISIBLE);
                                }
                                imgDoorOpen.setVisibility(View.INVISIBLE);
                                imgDoorLock.setVisibility(View.VISIBLE);
                            }
                        }
                        StaticDataSave.doorStateFlag = "false";
                        SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                        editor.putString(StaticDataSave.doorStateFlagKey, StaticDataSave.doorStateFlag);
                        editor.commit();
                    }
                }
            }
            pastDoorStateTimestamp = curTimestamp;
            isDoorStateUpdated = false;
        }

        if (dm.metadata.reported.temperature != null) {
            long curTemperatureTimestamp = dm.metadata.reported.temperature.timestamp;
            Date dt2 = new Date(curTemperatureTimestamp * 1000);
            SimpleDateFormat sfd2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
            updatedTimeText.setText(sfd2.format(dt2));

            if (pastTemperatureTimestamp != curTemperatureTimestamp) {
                isTemperatureUpdated = true;
            } else {
                isTemperatureUpdated = false;
            }
            MyLog.i("isTemperatureUpdated = " + String.valueOf(isTemperatureUpdated));

            temperatureText.setText("- -" + " \u00b0" + "C");
            if (buildOption.DEVICE == buildOption.SMARTLOCK_BOARD) {
                updateTemperaureUI(ds.state.reported.temperature);
            } else {
                if (isTemperatureUpdated) {
                    updateTemperaureUI(ds.state.reported.temperature);
                }
            }
            pastTemperatureTimestamp = curTemperatureTimestamp;
            isTemperatureUpdated = false;
        }

        if (dm.metadata.reported.battery != null) {
            long curBatteryTimestamp = dm.metadata.reported.battery.timestamp;
            Date dt3 = new Date(curBatteryTimestamp * 1000);
            SimpleDateFormat sfd3 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
            updatedTimeText.setText(sfd3.format(dt3));

            if (pastBatteryTimestamp != curBatteryTimestamp) {
                isBatteryUpdated = true;
            } else {
                isBatteryUpdated = false;
            }
            MyLog.i("isBatteryUpdated = " + String.valueOf(isBatteryUpdated));

            batteryText.setText("- -" + " \u0025");
            if (buildOption.DEVICE == buildOption.SMARTLOCK_BOARD) {
                updateBatteryUI(ds.state.reported.battery);
            } else {
                if (isBatteryUpdated) {
                    updateBatteryUI(ds.state.reported.battery);
                }
            }

            pastBatteryTimestamp = curBatteryTimestamp;
            isBatteryUpdated = false;
        }

        if (dm.metadata.reported.OTAupdate != null) {
            long curOtaTimestamp = dm.metadata.reported.OTAupdate.timestamp;
            Date dt4 = new Date(curOtaTimestamp * 1000);
            SimpleDateFormat sfd4 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
            updatedTimeText.setText(sfd4.format(dt4));

            if (pastOtaTimestamp != curOtaTimestamp) {
                isOtaUpdated = true;
            } else {
                isOtaUpdated = false;
            }
            MyLog.i("isOtaUpdated = " + String.valueOf(isOtaUpdated));

            if (isOtaUpdated) {
                //OTA update check
                StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                MyLog.i("ds.state.reported.OTAupdate = " + ds.state.reported.OTAupdate);
                if (ds.state.reported.OTAupdate == 0) {  //none
                    StaticDataSave.existOTAupdateFlag = false;
                    StaticDataSave.OTAupdateProgressFlag = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            door_badge_setting.setVisibility(View.INVISIBLE);
                        }
                    });

                } else if (ds.state.reported.OTAupdate == 1) {  // exist update
                    StaticDataSave.existOTAupdateFlag = true;
                    StaticDataSave.OTAupdateProgressFlag = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            door_badge_setting.setVisibility(View.VISIBLE);
                        }
                    });
                } else if (ds.state.reported.OTAupdate == 2) {  // update progressing
                    StaticDataSave.existOTAupdateFlag = false;
                    StaticDataSave.OTAupdateProgressFlag = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            door_badge_setting.setVisibility(View.INVISIBLE);
                        }
                    });
                }
                MyLog.i("StaticDataSave.existOTAupdateFlag = " + StaticDataSave.existOTAupdateFlag);
                MyLog.i("StaticDataSave.OTAupdateProgressFlag = " + StaticDataSave.OTAupdateProgressFlag);

                editor.putBoolean(StaticDataSave.existOTAupdateFlagKey, StaticDataSave.existOTAupdateFlag);
                editor.putBoolean(StaticDataSave.OTAupdateProgressFlagKey, StaticDataSave.OTAupdateProgressFlag);
                editor.commit();
            }

            pastOtaTimestamp = curOtaTimestamp;
            isOtaUpdated = false;
        }
        //[[add in v2.4.16
        sendEvent(AWSEVENT.E_GET_SHADOW);
        //]]
    }

    public int calBattery(float shadowBattery) {
        int retBattery = 0;
        int intShadowBattery = Math.round(shadowBattery);

        if (buildOption.PTIM_SENSING == true) {
            unitBattery_PTIM = (batteryMax_PTIM - batteryMin_PTIM) / 10;
            if ((intShadowBattery > (batteryMin_PTIM + (unitBattery_PTIM * 9))) && (intShadowBattery < 10000)) {
                retBattery = 100;
            } else if ((intShadowBattery > (batteryMin_PTIM + (unitBattery_PTIM * 8))) && (intShadowBattery <= (batteryMin_PTIM + (unitBattery_PTIM * 9)))) {
                retBattery = 90;
            } else if ((intShadowBattery > (batteryMin_PTIM + (unitBattery_PTIM * 7))) && (intShadowBattery <= (batteryMin_PTIM + (unitBattery_PTIM * 8)))) {
                retBattery = 80;
            } else if ((intShadowBattery > (batteryMin_PTIM + (unitBattery_PTIM * 6))) && (intShadowBattery <= (batteryMin_PTIM + (unitBattery_PTIM * 7)))) {
                retBattery = 70;
            } else if ((intShadowBattery > (batteryMin_PTIM + (unitBattery_PTIM * 5))) && (intShadowBattery <= (batteryMin_PTIM + (unitBattery_PTIM * 6)))) {
                retBattery = 60;
            } else if ((intShadowBattery > (batteryMin_PTIM + (unitBattery_PTIM * 4))) && (intShadowBattery <= (batteryMin_PTIM + (unitBattery_PTIM * 5)))) {
                retBattery = 50;
            } else if ((intShadowBattery > (batteryMin_PTIM + (unitBattery_PTIM * 3))) && (intShadowBattery <= (batteryMin_PTIM + (unitBattery_PTIM * 4)))) {
                retBattery = 40;
            } else if ((intShadowBattery > (batteryMin_PTIM + (unitBattery_PTIM * 2))) && (intShadowBattery <= (batteryMin_PTIM + (unitBattery_PTIM * 3)))) {
                retBattery = 30;
            } else if ((intShadowBattery > (batteryMin_PTIM + (unitBattery_PTIM * 1))) && (intShadowBattery <= (batteryMin_PTIM + (unitBattery_PTIM * 2)))) {
                retBattery = 20;
            } else if ((intShadowBattery > batteryMin_PTIM) && (intShadowBattery <= (batteryMin_PTIM + (unitBattery_PTIM * 1)))) {
                retBattery = 10;
            } else if (intShadowBattery >= 10000) {
                retBattery = 200;
            } else {
                retBattery = 0;
            }
        } else {

            if (buildOption.DEVICE == buildOption.DA16200) {
                batteryMax_MCU = 4050;
                batteryMin_MCU = 2900;
            } else if (buildOption.DEVICE == buildOption.FC9000) {
                batteryMax_MCU = 2200;  // 5.5 Volt
                batteryMin_MCU = 1500;  // 4 Volt
            }

            unitBattery_MCU = (batteryMax_MCU - batteryMin_MCU) / 10;
            if ((intShadowBattery > (batteryMin_MCU + (unitBattery_MCU * 9))) && (intShadowBattery < 10000)) {
                retBattery = 100;
            } else if ((intShadowBattery > (batteryMin_MCU + (unitBattery_MCU * 8))) && (intShadowBattery <= (batteryMin_MCU + (unitBattery_MCU * 9)))) {
                retBattery = 90;
            } else if ((intShadowBattery > (batteryMin_MCU + (unitBattery_MCU * 7))) && (intShadowBattery <= (batteryMin_MCU + (unitBattery_MCU * 8)))) {
                retBattery = 80;
            } else if ((intShadowBattery > (batteryMin_MCU + (unitBattery_MCU * 6))) && (intShadowBattery <= (batteryMin_MCU + (unitBattery_MCU * 7)))) {
                retBattery = 70;
            } else if ((intShadowBattery > (batteryMin_MCU + (unitBattery_MCU * 5))) && (intShadowBattery <= (batteryMin_MCU + (unitBattery_MCU * 6)))) {
                retBattery = 60;
            } else if ((intShadowBattery > (batteryMin_MCU + (unitBattery_MCU * 4))) && (intShadowBattery <= (batteryMin_MCU + (unitBattery_MCU * 5)))) {
                retBattery = 50;
            } else if ((intShadowBattery > (batteryMin_MCU + (unitBattery_MCU * 3))) && (intShadowBattery <= (batteryMin_MCU + (unitBattery_MCU * 4)))) {
                retBattery = 40;
            } else if ((intShadowBattery > (batteryMin_MCU + (unitBattery_MCU * 2))) && (intShadowBattery <= (batteryMin_MCU + (unitBattery_MCU * 3)))) {
                retBattery = 30;
            } else if ((intShadowBattery > (batteryMin_MCU + (unitBattery_MCU * 1))) && (intShadowBattery <= (batteryMin_MCU + (unitBattery_MCU * 2)))) {
                retBattery = 20;
            } else if ((intShadowBattery > batteryMin_MCU) && (intShadowBattery <= (batteryMin_MCU + (unitBattery_MCU * 1)))) {
                retBattery = 10;
            } else if (intShadowBattery >= 10000) {
                retBattery = 200;
            } else {
                retBattery = 0;
            }
        }
        return retBattery;
    }

    private void shadowUpdated(String statusState) {

        Gson gson = new Gson();
        DoorlockStatus ds = gson.fromJson(statusState, DoorlockStatus.class);

        Gson gson1 = new Gson();
        DoorMetaData dm = gson1.fromJson(statusState, DoorMetaData.class);

        if (dm.metadata.reported.doorState != null) {
            Long curTimestamp = dm.metadata.reported.doorState.timestamp;
            if (curTimestamp != null) {
                Date dt = new Date(curTimestamp * 1000);
                SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
                updatedTimeText.setText(sfd.format(dt));

                if (pastDoorStateTimestamp != curTimestamp) {
                    isDoorStateUpdated = true;
                } else {
                    isDoorStateUpdated = false;
                }
            }

            MyLog.i("isShadowUpdated = " + String.valueOf(isDoorStateUpdated));

            if (isDoorStateUpdated) {

                if (ds.state.reported.doorState != null) {
                    MyLog.i(String.format("doorState : %s", ds.state.reported.doorState));
                }

                MyLog.i(String.format("doorStateChange :  %d", ds.state.reported.doorStateChange));
                MyLog.i(String.format("DoorOpenMode :  %s", ds.state.reported.DoorOpenMode));
                MyLog.i(String.format("Battery :  %f", ds.state.reported.battery));
                MyLog.i(String.format("Temperature :  %f", ds.state.reported.temperature));
                MyLog.i(String.format("Doorbell :  %s", ds.state.reported.doorBell));

                if (ds.state.reported.doorState != null && ds.state.reported.openMethod != null) {
                    if (ds.state.reported.doorState.equals("true")) {  //opened
                        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                        tv_doorState.setText(R.string.door_opened);
                        StaticDataSave.doorStateFlag = StaticDataSave.saveData.getString(StaticDataSave.doorStateFlagKey, null);
                        if (StaticDataSave.doorStateFlag != null) {
                            if (StaticDataSave.doorStateFlag.equals("false")) {
                                if (progressingOpenClose != null) {
                                    progressingOpenClose.setVisibility(View.INVISIBLE);
                                }
                                imgDoorLock.setVisibility(View.INVISIBLE);
                                imgDoorOpen.setVisibility(View.VISIBLE);
                            }
                        }
                        StaticDataSave.doorStateFlag = "true";
                        SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                        editor.putString(StaticDataSave.doorStateFlagKey, StaticDataSave.doorStateFlag);
                        editor.commit();

                    } else if (ds.state.reported.doorState.equals("false")) {  //closed
                        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                        tv_doorState.setText(R.string.door_closed);
                        StaticDataSave.doorStateFlag = StaticDataSave.saveData.getString(StaticDataSave.doorStateFlagKey, null);
                        if (StaticDataSave.doorStateFlag != null) {
                            if (StaticDataSave.doorStateFlag.equals("true")) {
                                if (progressingOpenClose != null) {
                                    progressingOpenClose.setVisibility(View.INVISIBLE);
                                }
                                imgDoorOpen.setVisibility(View.INVISIBLE);
                                imgDoorLock.setVisibility(View.VISIBLE);
                            }
                        }
                        StaticDataSave.doorStateFlag = "false";
                        SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                        editor.putString(StaticDataSave.doorStateFlagKey, StaticDataSave.doorStateFlag);
                        editor.commit();
                    }
                }
            }
            pastDoorStateTimestamp = curTimestamp;
            isDoorStateUpdated = false;
        }

        if (dm.metadata.reported.temperature != null) {
            Long curTemperatureTimestamp = dm.metadata.reported.temperature.timestamp;
            if (curTemperatureTimestamp != null) {
                Date dt2 = new Date(curTemperatureTimestamp * 1000);
                SimpleDateFormat sfd2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
                updatedTimeText.setText(sfd2.format(dt2));

                if (pastTemperatureTimestamp != curTemperatureTimestamp) {
                    isTemperatureUpdated = true;
                } else {
                    isTemperatureUpdated = false;
                }
            }

            MyLog.i("isTemperatureUpdated = " + String.valueOf(isTemperatureUpdated));

            if (isTemperatureUpdated) {
                updateTemperaureUI(ds.state.reported.temperature);
            }
            pastTemperatureTimestamp = curTemperatureTimestamp;
            isTemperatureUpdated = false;
        }

        if (dm.metadata.reported.battery != null) {
            Long curBatteryTimestamp = dm.metadata.reported.battery.timestamp;
            if (curBatteryTimestamp != null) {
                Date dt3 = new Date(curBatteryTimestamp * 1000);
                SimpleDateFormat sfd3 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
                updatedTimeText.setText(sfd3.format(dt3));

                if (pastBatteryTimestamp != curBatteryTimestamp) {
                    isBatteryUpdated = true;
                } else {
                    isBatteryUpdated = false;
                }
            }

            MyLog.i("isBatteryUpdated = " + String.valueOf(isBatteryUpdated));

            if (isBatteryUpdated) {
                updateBatteryUI(ds.state.reported.battery);
            }

            pastBatteryTimestamp = curBatteryTimestamp;
            isBatteryUpdated = false;
        }

        if (dm.metadata.reported.OTAupdate != null) {
            Long curOtaTimestamp = dm.metadata.reported.OTAupdate.timestamp;
            if (curOtaTimestamp != null) {
                Date dt4 = new Date(curOtaTimestamp * 1000);
                SimpleDateFormat sfd4 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
                updatedTimeText.setText(sfd4.format(dt4));

                if (pastOtaTimestamp != curOtaTimestamp) {
                    isOtaUpdated = true;
                } else {
                    isOtaUpdated = false;
                }
            }

            MyLog.i("isOtaUpdated = " + String.valueOf(isOtaUpdated));

            if (isOtaUpdated) {
                //OTA update check
                StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                MyLog.i("ds.state.reported.OTAupdate = " + ds.state.reported.OTAupdate);
                if (ds.state.reported.OTAupdate == 0) {  //none
                    StaticDataSave.existOTAupdateFlag = false;
                    StaticDataSave.OTAupdateProgressFlag = false;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (getRunActivity().equals("com.renesas.wifi.awsiot.AWSIoTDoorActivity")) {
                                door_badge_setting.setVisibility(View.INVISIBLE);
                            } else if (getRunActivity().equals("com.renesas.wifi.awsiot.setting.door.DoorSettingActivity")) {
                                badge_notification.setVisibility(View.INVISIBLE);
                            }
                        }
                    });

                    if (ds.state.reported.OTAresult != null) {
                        long curOtaResultTimestamp = dm.metadata.reported.OTAresult.timestamp;
                        Date dt5 = new Date(curOtaTimestamp * 1000);
                        SimpleDateFormat sfd5 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
                        updatedTimeText.setText(sfd5.format(dt5));

                        if (ds.state.reported.OTAresult.equals("OTA_OK")) {
                            if (getRunActivity().equals("com.renesas.wifi.awsiot.AWSIoTDoorActivity")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (updatingDialog != null && updatingDialog.isShowing()) {
                                            showUpdateSuccessDialog();
                                        }
                                    }
                                });

                            } else if (getRunActivity().equals("com.renesas.wifi.awsiot.setting.door.DoorSettingActivity")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (DoorSettingActivity.getInstanceSetting() != null) {
                                            if (DoorSettingActivity.getInstanceSetting().updatingDialog != null) {
                                                if (DoorSettingActivity.getInstanceSetting().updatingDialog.isShowing()) {
                                                    DoorSettingActivity.getInstanceSetting().showUpdateSuccessDialog();
                                                }
                                            }
                                        }
                                    }
                                });
                            }

                        } else if (ds.state.reported.OTAresult.equals("OTA_UNKNOWN")) {
                            if (getRunActivity().equals("com.renesas.wifi.awsiot.AWSIoTDoorActivity")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (updatingDialog != null && updatingDialog.isShowing()) {
                                            dismissUpdatingDialog();
                                        }
                                    }
                                });

                            } else if (getRunActivity().equals("com.renesas.wifi.awsiot.setting.door.DoorSettingActivity")) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (DoorSettingActivity.getInstanceSetting() != null) {
                                            if (DoorSettingActivity.getInstanceSetting().updatingDialog != null) {
                                                if (DoorSettingActivity.getInstanceSetting().updatingDialog.isShowing()) {
                                                    DoorSettingActivity.getInstanceSetting().dismissUpdatingDialog();
                                                }
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    }

                } else if (ds.state.reported.OTAupdate == 1) {  // exist update
                    StaticDataSave.existOTAupdateFlag = true;
                    StaticDataSave.OTAupdateProgressFlag = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (getRunActivity().equals("com.renesas.wifi.awsiot.AWSIoTDoorActivity")) {
                                door_badge_setting.setVisibility(View.VISIBLE);
                            } else if (getRunActivity().equals("com.renesas.wifi.awsiot.setting.door.DoorSettingActivity")) {
                                badge_notification.setVisibility(View.VISIBLE);
                            }
                        }
                    });

                    if (ds.state.reported.OTAresult.equals("OTA_NG")) {
                        if (getRunActivity().equals("com.renesas.wifi.awsiot.AWSIoTDoorActivity")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (updatingDialog != null && updatingDialog.isShowing()) {
                                        showUpdateFailDialog();
                                    }
                                }
                            });
                        } else if (getRunActivity().equals("com.renesas.wifi.awsiot.setting.door.DoorSettingActivity")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (DoorSettingActivity.getInstanceSetting() != null) {
                                        if (DoorSettingActivity.getInstanceSetting().updatingDialog != null) {
                                            if (DoorSettingActivity.getInstanceSetting().updatingDialog.isShowing()) {
                                                DoorSettingActivity.getInstanceSetting().showUpdateFailDialog();
                                            }
                                        }
                                    }
                                }
                            });
                        }
                    }
                } else if (ds.state.reported.OTAupdate == 2) {  // update progressing
                    StaticDataSave.existOTAupdateFlag = false;
                    StaticDataSave.OTAupdateProgressFlag = true;
                    if (getRunActivity().equals("com.renesas.wifi.awsiot.AWSIoTDoorActivity")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                door_badge_setting.setVisibility(View.INVISIBLE);
                                showUpdatingDialog();
                            }
                        });

                    } else if (getRunActivity().equals("com.renesas.wifi.awsiot.setting.door.DoorSettingActivity")) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                badge_notification.setVisibility(View.INVISIBLE);
                                if (DoorSettingActivity.getInstanceSetting() != null) {
                                    DoorSettingActivity.getInstanceSetting().showUpdatingDialog();
                                }
                            }
                        });
                    }
                }
                MyLog.i("StaticDataSave.existOTAupdateFlag = " + StaticDataSave.existOTAupdateFlag);
                MyLog.i("StaticDataSave.OTAupdateProgressFlag = " + StaticDataSave.OTAupdateProgressFlag);

                editor.putBoolean(StaticDataSave.existOTAupdateFlagKey, StaticDataSave.existOTAupdateFlag);
                editor.putBoolean(StaticDataSave.OTAupdateProgressFlagKey, StaticDataSave.OTAupdateProgressFlag);
                editor.commit();
            }
            pastOtaTimestamp = curOtaTimestamp;
            isOtaUpdated = false;
        }
    }

    private void getShadow(View view) {
        getShadows();
    }

    private void getShadows() {
        if (StaticDataSave.thingName != null) {
            if (getStatusShadowTask != null) {
                getStatusShadowTask.cancel(true);
                getStatusShadowTask = null;
            }
            getStatusShadowTask = new GetShadowTask(StaticDataSave.thingName);
            getStatusShadowTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            MyLog.i("StaticDataSave.thingName = null~");
        }
    }

    private class GetShadowTask extends AsyncTask<Void, Void, AsyncTaskResult<String>> {


        private final String thingName;

        public GetShadowTask(String name) {
            thingName = name;
        }

        @Override
        protected AsyncTaskResult<String> doInBackground(Void... voids) {

            isAyncTaskCompleted = false;

            try {
                MyLog.i("=== GetShadowTask ===");
                GetThingShadowRequest getThingShadowRequest = new GetThingShadowRequest().withThingName(thingName);
                GetThingShadowResult result = iotDataClient.getThingShadow(getThingShadowRequest);
                byte[] bytes = new byte[result.getPayload().remaining()];
                result.getPayload().get(bytes);
                String resultString = new String(bytes);
                return new AsyncTaskResult<String>(resultString);
            } catch (Exception e) {
                MyLog.e("getShadowTask : " + e);
                return new AsyncTaskResult<String>(e);
            }
        }

        @Override
        protected void onPostExecute(AsyncTaskResult<String> result) {
            if (result.getError() == null) {
                MyLog.i(result.getResult());
                if (StaticDataSave.thingName != null && StaticDataSave.thingName.equals(thingName)) {
                    shadowUpdated(result.getResult());
                }
            } else {
                MyLog.e("E / getShadowTask : " + result.getError());
            }
            isAyncTaskCompleted = true;
        }
    }

    private void getSensorShadow(View view) {
        getSensorShadows();
    }

    private void getSensorShadows() {
        if (StaticDataSave.thingName != null) {
            GetSensorShadowTask = new GetSensorShadowTask(StaticDataSave.thingName);
            GetSensorShadowTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private class GetSensorShadowTask extends AsyncTask<Void, Void, AsyncTaskResult<String>> {


        private final String thingName;

        public GetSensorShadowTask(String name) {
            thingName = name;
        }

        @Override
        protected AsyncTaskResult<String> doInBackground(Void... voids) {

            try {
                MyLog.i("=== GetSensorShadowTask ===");
                GetThingShadowRequest getThingShadowRequest = new GetThingShadowRequest().withThingName(thingName);
                GetThingShadowResult result = iotDataClient.getThingShadow(getThingShadowRequest);
                byte[] bytes = new byte[result.getPayload().remaining()];
                result.getPayload().get(bytes);
                String resultString = new String(bytes);
                return new AsyncTaskResult<String>(resultString);
            } catch (Exception e) {
                MyLog.e("getShadowTask : " + e);
                return new AsyncTaskResult<String>(e);
            }

        }

        @Override
        protected void onPostExecute(AsyncTaskResult<String> result) {
            if (result.getError() == null) {
                MyLog.i(result.getResult());
                if (StaticDataSave.thingName.equals(thingName)) {
                    sensorStatusUpdated(result.getResult());
                }
            } else {
                MyLog.e("E / GetSensorShadowTask : " + result.getError());
            }
        }
    }

    private class UpdateShadowTask extends AsyncTask<Void, Void, AsyncTaskResult<String>> {

        private String thingName = StaticDataSave.thingName;
        private String updateState;

        public void setThingName(String name) {
            thingName = name;
        }

        public void setState(String state) {
            updateState = state;
        }

        @Override
        protected AsyncTaskResult<String> doInBackground(Void... voids) {

            try {
                UpdateThingShadowRequest request = new UpdateThingShadowRequest();
                request.setThingName(thingName);

                ByteBuffer payloadBuffer = ByteBuffer.wrap(updateState.getBytes());
                request.setPayload(payloadBuffer);

                UpdateThingShadowResult result = iotDataClient.updateThingShadow(request);

                byte[] bytes = new byte[result.getPayload().remaining()];
                result.getPayload().get(bytes);
                String resultString = new String(bytes);
                return new AsyncTaskResult<String>(resultString);
            } catch (Exception e) {
                MyLog.e("UpdateShadowTask : " + e);
                return new AsyncTaskResult<String>(e);
            }

        }

        @Override
        protected void onPostExecute(AsyncTaskResult<String> result) {
            if (result.getError() == null) {
                MyLog.i(result.getResult());
            } else {
                MyLog.e("Error in Update Door Shadow : " + result.getError());
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        MyLog.i("=== onResume() ===");

        if (isRunning()) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.cancel(1);

            if (MonitorService.getInstance() != null) {
                MonitorService.getInstance().stopService();
            }
        }

        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);

        MyLog.i("StaticDataSave.thingName = " + StaticDataSave.thingName);

        if (StaticDataSave.thingName == null) {
            textMainKeyName.setText("Unknown");
            imgRegisterDoorLock.setVisibility(View.VISIBLE);
            tv_doorState.setText(R.string.device_register);
            imgDoorLock.setVisibility(View.INVISIBLE);
            imgDoorOpen.setVisibility(View.INVISIBLE);
            layerConnecting.setVisibility(View.INVISIBLE);
            layerNoConnect.setVisibility(View.INVISIBLE);

            imgRegisterDoorLock.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                    wifiManager.setWifiEnabled(true);

                    Intent main = new Intent(AWSIoTDoorActivity.this, SelectDeviceActivity.class);
                    main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(main);
                    finishAffinity();
                }
            });

        } else {

            textMainKeyName.setText(StaticDataSave.thingName);
            imgRegisterDoorLock.setVisibility(View.INVISIBLE);

            StaticDataSave.readyFlag = StaticDataSave.saveData.getBoolean(StaticDataSave.readyFlagKey, false);

            MyLog.i("StaticDataSave.readyFlag = " + StaticDataSave.readyFlag);
            if (StaticDataSave.readyFlag == false) {

                if (iv_deviceNetworkState != null) {
                    iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_red);
                    iv_deviceNetworkState.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                    iv_deviceNetworkState.clearAnimation();
                }

                if (waitDialog == null) {

                    waitDialog = new ProgressDialog(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                    waitDialog.setMessage("Waiting for device to be ready ...");
                    waitDialog.show();

                    int displayWidth = 0;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        WindowMetrics windowMetrics = getWindowManager().getCurrentWindowMetrics();
                        Insets insets = windowMetrics.getWindowInsets()
                                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
                        displayWidth = windowMetrics.getBounds().width() - insets.left - insets.right;
                    } else {
                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                        displayWidth = displayMetrics.widthPixels;
                    }

                    WindowManager.LayoutParams params = waitDialog.getWindow().getAttributes();
                    int dialogWindowWidth = (int) (displayWidth * 0.8f);
                    params.width = dialogWindowWidth;
                    params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                    waitDialog.getWindow().setAttributes((WindowManager.LayoutParams) params);
                    waitDialog.setCanceledOnTouchOutside(false);
                    waitDialog.setCancelable(false);
                }
                postEvent(AWSEVENT.E_DEVICE_CONNECT_TIMEOUT, PROVISIONING_TIMEOUT_TIME);

            } else {
                if (isOnline() && clientKeyStore != null && isMqttConnected == false) {
                    initMqtt();
                } else {
                    sendEvent(AWSEVENT.E_GET_SHADOW);
                    sendEvent(AWSEVENT.E_UPDATE_SENSOR);
                }
                if (fromSetting) {
                    fromSetting = false;
                    sendEvent(AWSEVENT.E_GET_SHADOW);
                    sendEvent(AWSEVENT.E_UPDATE_SENSOR);
                    sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);
                }

                StaticDataSave.existOTAupdateFlag = StaticDataSave.saveData.getBoolean(StaticDataSave.existOTAupdateFlagKey, false);
                MyLog.i("StaticDataSave.existOTAupdateFlag = " + StaticDataSave.existOTAupdateFlag);
                if (iv_setting != null) {
                    if (StaticDataSave.existOTAupdateFlag == true) {
                        door_badge_setting.setVisibility(View.VISIBLE);
                    } else {
                        door_badge_setting.setVisibility(View.INVISIBLE);
                    }
                }
                MyLog.i("StaticDataSave.cognitoPoolId = " + StaticDataSave.cognitoPoolId);
            }
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        MyLog.i("=== onPause() ===");
        removeEvent(AWSEVENT.E_GET_SHADOW);
        removeEvent(AWSEVENT.E_UPDATE_SENSOR);
        sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);
        sendEvent(AWSEVENT.E_MQTT_REMOVE_CALLBACK);

        try {
            if (subScribeAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                subScribeAsyncTask.cancel(true);
            }
        } catch (Exception e) {
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        MyLog.i("=== onStop() ===");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyLog.i("=== onDestroy() ===");

        unregisterReceiver(mNetworkReceiver);

        if (waitDialog != null) {
            waitDialog.dismiss();
        }

        if (updateDialog != null) {
            updateDialog.dismiss();
            updateDialog = null;
        }
        if (terminateDialog != null) {
            terminateDialog.dismiss();
            terminateDialog = null;
        }

        DoorHandler.removeCallbacksAndMessages(null);
        mqttManager.disconnect();
    }

    public static void clearCredentialsProvider() {
        if (credentialsProvider != null) {
            credentialsProvider.clear();
        }
    }

    @Override
    public void onBackPressed() {
        MyLog.i("=== onBackPressed() ===");

        if (terminateDialog == null && ((Activity) mContext).hasWindowFocus()) {
            terminateDialog = new MaterialDialog.Builder(mContext)
                    .theme(Theme.LIGHT)
                    .title("Confirm")
                    .titleColor(mContext.getResources().getColor(R.color.blue3))
                    .titleGravity(GravityEnum.CENTER)
                    .content("Would you like to exit AWS IoT?")
                    .contentColor(mContext.getResources().getColor(R.color.black))
                    .widgetColorRes(R.color.blue3)
                    .positiveText("OK")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            if (StaticDataSave.thingName != null && StaticDataSave.cognitoPoolId != null && awsConfig.MY_REGION != null) {

                                if (buildOption.USE_MONITOR_SERVICE == true && MainActivity.isDoorlock()) {

                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        startForegroundService(new Intent(AWSIoTDoorActivity.this, MonitorService.class));
                                    } else {
                                        startService(new Intent(AWSIoTDoorActivity.this, MonitorService.class));
                                    }
                                }

                                terminateDialog = null;
                                removeEvent(AWSEVENT.E_GET_SHADOW);
                                removeEvent(AWSEVENT.E_UPDATE_SENSOR);

                                Intent main = new Intent(mContext, MainActivity.class);
                                main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(main);
                                finishAffinity();
                            } else {
                                terminateDialog = null;
                                Intent main = new Intent(mContext, MainActivity.class);
                                main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(main);
                                finishAffinity();
                            }
                        }
                    })
                    .negativeText("CANCEL")
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                            terminateDialog = null;
                        }
                    })
                    .build();
            terminateDialog.getWindow().setGravity(Gravity.CENTER);
            terminateDialog.show();
            terminateDialog.setCanceledOnTouchOutside(false);
            terminateDialog.setCancelable(false);
        }
    }

    private void initMainResource() {

        ll_debug = findViewById(R.id.ll_debug);
        if (ll_debug != null) {
            if (buildOption.DEBUG_MODE == true) {
                ll_debug.setVisibility(View.VISIBLE);
            } else {
                ll_debug.setVisibility(View.GONE);
            }
        }

        tvSubscribeTopic = findViewById(R.id.tvSubscribeTopic);
        tvLastSubscribeMessage = findViewById(R.id.tvLastSubscribeMessage);
        tvPublishTopic = findViewById(R.id.tvPublishTopic);
        tvPublishMessage = findViewById(R.id.tvPublishMessage);

        textMainKeyName = findViewById(R.id.textMainKeyName);

        iv_deviceNetworkState = findViewById(R.id.iv_deviceNetworkState);
        if (iv_deviceNetworkState != null) {
            iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_red);
            iv_deviceNetworkState.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
            iv_deviceNetworkState.setVisibility(View.VISIBLE);
        }


        layerMainKey = findViewById(R.id.layerMainKey);
        if (layerMainKey != null) {
            layerMainKey.setBackground(ContextCompat.getDrawable(mContext, R.drawable.gradient_list));

            AnimationDrawable animationDrawable = (AnimationDrawable) layerMainKey.getBackground();
            animationDrawable.setEnterFadeDuration(2000);
            animationDrawable.setExitFadeDuration(4000);
            animationDrawable.start();
        }

        layerNoConnect = findViewById(R.id.layerNoConnect);
        layerConnecting = findViewById(R.id.layerConnecting);

        progressingConnecting = findViewById(R.id.progressing_connect);

        Button btn_reconnect = findViewById(R.id.btn_reconnect);
        if (btn_reconnect != null) {
            btn_reconnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    layerConnecting.setVisibility(View.VISIBLE);
                    Device_connectCheck();
                }
            });
        }

        imgConnStat = findViewById(R.id.imgConn);
        if (imgConnStat != null) {
            imgConnStat.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
            imgConnStat.setVisibility(View.INVISIBLE);
        }
        imgDisconnStat = findViewById(R.id.imgDisconn);
        if (imgDisconnStat != null) {
            imgDisconnStat.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
            imgDisconnStat.setVisibility(View.VISIBLE);
        }

        imgDoorLock = findViewById(R.id.doorLocked);
        imgDoorOpen = findViewById(R.id.doorOpened);

        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.doorStateFlag = StaticDataSave.saveData.getString(StaticDataSave.doorStateFlagKey, null);

        if (StaticDataSave.doorStateFlag != null) {
            if (StaticDataSave.doorStateFlag.equals("true")) {
                imgDoorOpen.setVisibility(View.VISIBLE);
                imgDoorLock.setVisibility(View.INVISIBLE);
            } else {
                imgDoorLock.setVisibility(View.VISIBLE);
                imgDoorOpen.setVisibility(View.INVISIBLE);
            }
        }

        progressingOpenClose = findViewById(R.id.progressingOpenClose);
        if (progressingOpenClose != null) {
            progressingOpenClose.setVisibility(View.INVISIBLE);
        }

        iv_notify = findViewById(R.id.iv_notify);
        tv_notify = findViewById(R.id.tv_notify);
        iv_addUser = findViewById(R.id.iv_addUser);
        tv_addUser = findViewById(R.id.tv_addUser);
        iv_setting = findViewById(R.id.iv_setting);
        tv_setting = findViewById(R.id.tv_setting);

        door_badge_setting = findViewById(R.id.door_badge_setting);
        if (door_badge_setting != null) {
            door_badge_setting.setVisibility(View.INVISIBLE);
        }

        imgRegisterDoorLock = findViewById(R.id.addDoorlock);
        tv_doorState = findViewById(R.id.tv_doorState);

        batteryText = findViewById(R.id.tv_battery);
        temperatureText = findViewById(R.id.tv_temperature);

        iv_update_sensor = findViewById(R.id.iv_update_sensor);

        updatedTimeText = findViewById(R.id.tv_updatedTime);
    }

    private void doorLockCtl(boolean lock) {    // open : false, lock : true

        if (progressingOpenClose.getVisibility() == View.VISIBLE) {
            progressingOpenClose.setVisibility(View.INVISIBLE);
        }

        if (lock) {
            imgDoorOpen.setVisibility(View.INVISIBLE);
            imgDoorLock.setVisibility(View.VISIBLE);
            if (imgDoorLock.getVisibility() == View.VISIBLE) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(imgDoorLock, "rotationY", 360, 0);
                animator.setDuration(1000);
                animator.start();
            }

            tv_doorState.setText(R.string.door_closed);
            MyLog.i(String.format("Door %s", "locked"));

        } else {
            imgDoorLock.setVisibility(View.INVISIBLE);
            imgDoorOpen.setVisibility(View.VISIBLE);
            if (imgDoorOpen.getVisibility() == View.VISIBLE) {
                ObjectAnimator animator = ObjectAnimator.ofFloat(imgDoorOpen, "rotationY", 360, 0);
                animator.setDuration(1000);
                animator.start();
            }
            tv_doorState.setText(R.string.door_opened);
            MyLog.i(String.format("Door %s", "opened"));
        }
    }

    public void clickImgDoorLocked() {  //open

        if (updateDialog != null && updateDialog.isShowing()) {
            mHandler_toast.post(new ToastRunnable("Another job is being processed"));
        } else {
            MyLog.i("isMqttConnected = " + isMqttConnected + ", isDeviceConnected = " + isDeviceConnected);
            if (isMqttConnected && isDeviceConnected) {
                MyLog.i("imgDoorLock is clicked!");
                progressingOpenClose.setVisibility(View.VISIBLE);
                layerMainKey.setVisibility(View.VISIBLE);
                layerNoConnect.setVisibility(View.GONE);

                if (noInternetDialog != null) {
                    noInternetDialog.dismiss();
                    noInternetDialog = null;
                }
                layerConnecting.setVisibility(View.GONE);
                doorOpen();  // open

                // for AUTO TEST

                if (buildOption.AUTO_TEST == true) {

                    MyLog.i("================================================================================");
                    MyLog.i("AUTO_TEST  " + buildOption.AUTO_TEST_MIN_TIME + " min ~ " + buildOption.AUTO_TEST_MAX_TIME + " min");
                    MyLog.i("================================================================================");
                    postEvent(AWSEVENT.E_AUTO_TEST, 1000 * 60 * buildOption.AUTO_TEST_FIRST_START_TIME);

                    buildOption.AUTO_TEST = false;
                    MaterialDialog autoTestDialog = null;
                    if (autoTestDialog == null) {
                        autoTestDialog = new MaterialDialog.Builder(AWSIoTDoorActivity.this)
                                .theme(Theme.LIGHT)
                                .content("Running AUTO TEST for open and close door ")
                                .contentColor(mContext.getResources().getColor(R.color.black))
                                .contentGravity(GravityEnum.CENTER)
                                .progress(true, 50)
                                .widgetColorRes(R.color.blue3)
                                .negativeText("CANCEL")
                                .onNegative(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        dialog.dismiss();
                                        buildOption.AUTO_TEST = true;
                                        removeEvent(AWSEVENT.E_AUTO_TEST);
                                        MyLog.i("remove dialog ");
                                    }
                                })
                                .build();
                        autoTestDialog.getWindow().setGravity(Gravity.CENTER);
                        autoTestDialog.setCanceledOnTouchOutside(false);
                        autoTestDialog.show();
                        autoTestDialog.setCanceledOnTouchOutside(false);
                        autoTestDialog.setCancelable(false);
                    }
                }

                mHandler_open_noResponse.postDelayed(mRunnable_open_noResponse, COMMAND_TIMEOUT_TIME);

            } else if (isMqttConnected == false) {
                mHandler_toast.post(new ToastRunnable("The network connection is uncertain"));
            } else if (isDeviceConnected == false) {
                mHandler_toast.post(new ToastRunnable("It is not connected to the device"));
            }
        }
    }

    public void clickImgDoorOpened() {  //close

        if (updateDialog != null && updateDialog.isShowing()) {
            mHandler_toast.post(new ToastRunnable("Another job is being processed"));
        } else {
            MyLog.i("isMqttConnected = " + isMqttConnected + ", isDeviceConnected = " + isDeviceConnected);
            if (isMqttConnected && isDeviceConnected) {
                MyLog.i("imgDoorOpen is clicked!");
                progressingOpenClose.setVisibility(View.VISIBLE);

                layerMainKey.setVisibility(View.VISIBLE);
                layerNoConnect.setVisibility(View.GONE);
                if (noInternetDialog != null) {
                    noInternetDialog.dismiss();
                    noInternetDialog = null;
                }
                layerConnecting.setVisibility(View.GONE);
                doorClose();  // close

                mHandler_close_noResponse.postDelayed(mRunnable_close_noResponse, COMMAND_TIMEOUT_TIME);

            } else if (isMqttConnected == false) {
                mHandler_toast.post(new ToastRunnable("The network connection is uncertain"));
            } else if (isDeviceConnected == false) {
                mHandler_toast.post(new ToastRunnable("It is not connected to the device"));
            }
        }
    }

    private class ToastRunnable implements Runnable {
        String mText;

        public ToastRunnable(String text) {
            mText = text;
        }

        @Override
        public void run() {
            if (customToast != null) {
                customToast.showToast(getApplicationContext(), mText, Toast.LENGTH_SHORT);
            }
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private class NetworkChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {

            String action = intent.getAction();

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            SupplicantState supState = wifiInfo.getSupplicantState();

            if (supState == COMPLETED) {
                MyLog.i("SupplicantState = COMPLETED");

            }

            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

                String status = NetworkUtil.getConnectivityStatusString(context);
                if (status.equals("Not connected to Internet")) {
                    MyLog.i("Not connected to Internet");
                    if (StaticDataSave.thingName != null) {

                        if (StaticDataSave.readyFlag == true && noInternetDialog == null) {
                            noInternetDialog = new MaterialDialog.Builder(mContext)
                                    .theme(Theme.LIGHT)
                                    .title("Notice")
                                    .titleColor(mContext.getResources().getColor(R.color.blue3))
                                    .titleGravity(GravityEnum.CENTER)
                                    .content("No internet connection")
                                    .contentColor(mContext.getResources().getColor(R.color.black))
                                    .contentGravity(GravityEnum.CENTER)
                                    .showListener(new DialogInterface.OnShowListener() {
                                        @Override
                                        public void onShow(DialogInterface dialogInterface) {
                                            final MaterialDialog dialog = (MaterialDialog) dialogInterface;
                                        }

                                    }).build();

                            noInternetDialog.getWindow().setGravity(Gravity.CENTER);
                            noInternetDialog.show();
                            noInternetDialog.setCanceledOnTouchOutside(false);
                            noInternetDialog.setCancelable(false);
                        }
                        layerNoConnect.setVisibility(View.GONE);
                        layerConnecting.setVisibility(View.GONE);

                    } else {
                        imgRegisterDoorLock.setVisibility(View.VISIBLE);
                        tv_doorState.setText(R.string.device_register);
                        imgDoorLock.setVisibility(View.INVISIBLE);
                        imgDoorOpen.setVisibility(View.INVISIBLE);
                        layerConnecting.setVisibility(View.INVISIBLE);
                        layerNoConnect.setVisibility(View.INVISIBLE);

                        if (noInternetDialog != null) {
                            noInternetDialog.dismiss();
                            noInternetDialog = null;
                        }
                    }

                } else if (status.equals("Wi-Fi enabled") || (status.equals("Mobile data enabled"))) {
                    MyLog.i("Wi-Fi enabled or Mobile data enabled");
                    if (StaticDataSave.thingName != null) {

                        if (noInternetDialog != null) {
                            noInternetDialog.dismiss();
                            noInternetDialog = null;
                        }
                        layerNoConnect.setVisibility(View.GONE);
                        layerConnecting.setVisibility(View.GONE);
                        layerMainKey.setVisibility(View.VISIBLE);

                        initMqtt();

                        new Thread() {
                            public void run() {
                                if (credentialsProvider != null) {
                                    String identityId = credentialsProvider.getIdentityId();
                                    MyLog.i("my ID is " + identityId);
                                }
                            }
                        }.start();

                        iotDataClient = new AWSIotDataClient(credentialsProvider);
                        String iotDataEndpoint = awsConfig.CUSTOMER_SPECIFIC_ENDPOINT;
                        iotDataClient.setEndpoint(iotDataEndpoint);

                    } else {
                        imgRegisterDoorLock.setVisibility(View.VISIBLE);
                        tv_doorState.setText(R.string.device_register);
                        imgDoorLock.setVisibility(View.INVISIBLE);
                        imgDoorOpen.setVisibility(View.INVISIBLE);
                        layerConnecting.setVisibility(View.INVISIBLE);
                        layerNoConnect.setVisibility(View.INVISIBLE);
                        if (noInternetDialog != null) {
                            noInternetDialog.dismiss();
                            noInternetDialog = null;
                        }
                    }
                }
            }
        }
    }

    public boolean isRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MonitorService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private class SubScribeAsyncTask extends AsyncTask<Void, Void, Void> {
        public SubScribeAsyncTask() {

        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                messageReceived = false;
                StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);
                final String subTopic = StaticDataSave.thingName + "/" + "#";
                try {
                    mqttManager.subscribeToTopic(subTopic, AWSIotMqttQos.QOS0,
                            new AWSIotMqttNewMessageCallback() {
                                @Override
                                public void onMessageArrived(final String subTopic, final byte[] data) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                final String message = new String(data, "UTF-8");
                                                MyLog.i("=================================");
                                                MyLog.i("Message published/arrived:");
                                                MyLog.i("   Topic: " + subTopic);
                                                MyLog.i(" subMessage: " + message);
                                                MyLog.i("=================================");

                                                if (subTopic.contains("Device")) {
                                                    tvSubscribeTopic.setText(subTopic);
                                                    tvLastSubscribeMessage.setText(message);
                                                }

                                                if (message.equals(DEVICE_CONNECT_RESPONSE_MESSAGE)) {
                                                    messageReceived = true;
                                                    isDeviceConnected = true;
                                                    removeEvent(AWSEVENT.E_DEVICE_CONNECT_CHECK);

                                                    MyLog.i("isMqttConnected = " + isMqttConnected);

                                                    if (isMqttConnected) {
                                                        if (waitDialog != null) {
                                                            waitDialog.dismiss();
                                                        }
                                                    }
                                                    removeEvent(AWSEVENT.E_DEVICE_CONNECT_TIMEOUT);

                                                    StaticDataSave.readyFlag = true;
                                                    StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                                                    SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                                                    editor.putBoolean(StaticDataSave.readyFlagKey, StaticDataSave.readyFlag);
                                                    editor.commit();
                                                    sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);
                                                    sendEvent(AWSEVENT.E_GET_SHADOW);

                                                    mHandler_toast.post(new ToastRunnable("Connected to device"));

                                                    if (iv_deviceNetworkState != null) {
                                                        iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_green);
                                                        iv_deviceNetworkState.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                                                        iv_deviceNetworkState.clearAnimation();
                                                    }
                                                    layerMainKey.setVisibility(View.VISIBLE);
                                                    layerNoConnect.setVisibility(View.GONE);

                                                    if (noInternetDialog != null) {
                                                        noInternetDialog.dismiss();
                                                        noInternetDialog = null;
                                                    }
                                                    layerConnecting.setVisibility(View.GONE);
                                                } else if (message.equals(DEVICE_CONTROL_OPEN_RESPONSE_MESSAGE)) {
                                                    messageReceived = true;
                                                    isDeviceConnected = true;
                                                    removeEvent(AWSEVENT.E_DEVICE_CONNECT_CHECK);
                                                    removeEvent(AWSEVENT.E_DEVICE_CONNECT_TIMEOUT);
                                                    sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);
                                                    if (isMqttConnected) {
                                                        if (waitDialog != null) {
                                                            waitDialog.dismiss();
                                                        }
                                                    }
                                                    MyLog.i("sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK) 5");
                                                    sendEvent(AWSEVENT.E_DEVICE_OPEN_NO_RESPONSE_REMOVE_CALLBACK);
                                                    if (iv_deviceNetworkState != null) {
                                                        iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_green);
                                                        iv_deviceNetworkState.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                                                        iv_deviceNetworkState.clearAnimation();
                                                    }
                                                    doorLockCtl(false);
                                                } else if (message.equals(DEVICE_CONTROL_CLOSE_RESPONSE_MESSAGE)) {
                                                    messageReceived = true;
                                                    isDeviceConnected = true;
                                                    removeEvent(AWSEVENT.E_DEVICE_CONNECT_CHECK);
                                                    sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);
                                                    removeEvent(AWSEVENT.E_DEVICE_CONNECT_TIMEOUT);
                                                    if (isMqttConnected) {
                                                        if (waitDialog != null) {
                                                            waitDialog.dismiss();
                                                        }
                                                    }
                                                    MyLog.i("sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK) 6");
                                                    sendEvent(AWSEVENT.E_DEVICE_CLOSE_NO_RESPONSE_REMOVE_CALLBACK);
                                                    if (iv_deviceNetworkState != null) {
                                                        iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_green);
                                                        iv_deviceNetworkState.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                                                        iv_deviceNetworkState.clearAnimation();
                                                    }
                                                    doorLockCtl(true);
                                                } else if (message.equals(DEVICE_UPDATE_SENSOR_RESPONSE_MESSAGE)) {
                                                    messageReceived = true;
                                                    if (updateDialog != null) {
                                                        updateDialog.dismiss();
                                                        updateDialog = null;
                                                    }
                                                    sendEvent(AWSEVENT.E_UPDATE_SENSOR_NO_RESPONSE_REMOVE_CALLBACK);
                                                    getSensorShadows();
                                                }

                                            } catch (UnsupportedEncodingException e) {
                                                MyLog.e("Message encoding error. >>> " + e);
                                            }
                                        }
                                    });
                                }
                            });
                } catch (Exception e) {
                    MyLog.e("Subscription error. >>> " + e);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

        }
    }

    private static void restartApp(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        context.startActivity(mainIntent);
        System.exit(0);
    }

    private void updateTemperaureUI(final float value) {
        MyLog.i(">> updateTemperaureUI()");
        if (value >= 100) {
            temperatureText.setText("- -" + " \u00b0" + "C");
            temperatureText.setTextColor(mContext.getResources().getColor(R.color.black));
        } else if (value < 100 && value >= 55) {
            temperatureText.setText(String.format("%.1f", value) + " \u00b0" + "C");
            temperatureText.setTextColor(mContext.getResources().getColor(R.color.red));
            sendEvent(AWSEVENT.E_SHOW_TEMPERATURE_ALERT);
        } else {
            temperatureText.setText(String.format("%.1f", value) + " \u00b0" + "C");
            temperatureText.setTextColor(mContext.getResources().getColor(R.color.black));
            removeEvent(AWSEVENT.E_SHOW_TEMPERATURE_ALERT);
        }
    }

    private void updateBatteryUI(final float value) {
        if (buildOption.DEVICE == buildOption.SMARTLOCK_BOARD) {
            int batt = Math.round(value);
            if (batt > 100) {
                batt = 100;
            } else if (batt < 0) {
                batt = 0;
            }
            batteryText.setText(batt + " \u0025");
            batteryText.setTextColor(mContext.getResources().getColor(R.color.black));
        } else {
            if (calBattery(value) <= 10) {
                batteryText.setText(String.valueOf(calBattery(value)) + " \u0025");
                batteryText.setTextColor(mContext.getResources().getColor(R.color.red));
                sendEvent(AWSEVENT.E_SHOW_BATTERY_ALERT);
            } else if (calBattery(value) == 200) {
                batteryText.setText("- -" + " \u0025");
                batteryText.setTextColor(mContext.getResources().getColor(R.color.black));
            } else {
                batteryText.setText(String.valueOf(calBattery(value)) + " \u0025");
                batteryText.setTextColor(mContext.getResources().getColor(R.color.black));
                removeEvent(AWSEVENT.E_SHOW_BATTERY_ALERT);
            }
        }

    }

    private String getRunActivity() {

        String className = "";

        ActivityManager activity_manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        List<ActivityManager.RunningTaskInfo> task_info = activity_manager.getRunningTasks(9999);

        for (int i = 0; i < task_info.size(); i++) {

            MyLog.i("[" + i + "] activity:" + task_info.get(i).topActivity.getPackageName() + " >> " + task_info.get(i).topActivity.getClassName());

        }
        className = task_info.get(0).topActivity.getClassName();
        return className;
    }

    public void showUpdatingDialog() {

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (updatingDialog == null) {
                    updatingDialog = new ProgressDialog(mContext, android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                    updatingDialog.setTitle("Updating device firmware");
                    updatingDialog.setMessage("The firmware of the device is being updated.");
                    updatingDialog.show();

                    int displayWidth = 0;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        WindowMetrics windowMetrics = getWindowManager().getCurrentWindowMetrics();
                        Insets insets = windowMetrics.getWindowInsets()
                                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
                        displayWidth = windowMetrics.getBounds().width() - insets.left - insets.right;
                    } else {
                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                        displayWidth = displayMetrics.widthPixels;
                    }

                    WindowManager.LayoutParams params = updatingDialog.getWindow().getAttributes();
                    int dialogWindowWidth = (int) (displayWidth * 0.8f);
                    params.width = dialogWindowWidth;
                    params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                    updatingDialog.getWindow().setAttributes((WindowManager.LayoutParams) params);
                    updatingDialog.setCanceledOnTouchOutside(false);
                    updatingDialog.setCancelable(false);
                }
            }
        });
    }

    public void dismissUpdatingDialog() {
        if (updatingDialog != null) {
            updatingDialog.dismiss();
            updatingDialog = null;
        }
    }


    public void showUpdateSuccessDialog() {

        handler.post(new Runnable() {
            @Override
            public void run() {
                dismissUpdatingDialog();

                if (updateSuccessDialog == null) {
                    String message = "The device's firmware update was successful.";
                    updateSuccessDialog = new android.app.AlertDialog.Builder(mContext, R.style.AlertDialogCustom).create();
                    updateSuccessDialog.setIcon(R.mipmap.renesas_ic_launcher);
                    updateSuccessDialog.setMessage(message);
                    updateSuccessDialog.setCancelable(false);
                    updateSuccessDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismissUpdateSuccessDialog();
                            sendEvent(AWSEVENT.E_GET_SHADOW);
                            door_badge_setting.setVisibility(View.INVISIBLE);
                        }
                    });
                    updateSuccessDialog.setOnCancelListener(null);
                    updateSuccessDialog.show();
                }
            }
        });
    }

    public void dismissUpdateSuccessDialog() {
        if (updateSuccessDialog != null) {
            updateSuccessDialog.dismiss();
            updateSuccessDialog = null;
        }
    }

    public void showUpdateFailDialog() {

        handler.post(new Runnable() {
            @Override
            public void run() {
                dismissUpdatingDialog();

                if (updateFailDialog == null) {
                    String message = "The device's firmware update was failed.";
                    updateFailDialog = new AlertDialog.Builder(mContext, R.style.AlertDialogCustom).create();
                    updateFailDialog.setIcon(R.mipmap.renesas_ic_launcher);
                    updateFailDialog.setMessage(message);
                    updateFailDialog.setCancelable(false);
                    updateFailDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismissUpdateFailDialog();
                            sendEvent(AWSEVENT.E_GET_SHADOW);
                        }
                    });
                    updateFailDialog.setOnCancelListener(null);
                    updateFailDialog.show();
                }
            }
        });
    }

    public void dismissUpdateFailDialog() {
        if (updateFailDialog != null) {
            updateFailDialog.dismiss();
            updateFailDialog = null;
        }
    }

    public static CognitoCachingCredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }

}