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
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.LayerDrawable;
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
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.TypedValue;
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
import com.amazonaws.services.iotdata.model.DeleteThingShadowRequest;
import com.amazonaws.services.iotdata.model.DeleteThingShadowResult;
import com.amazonaws.services.iotdata.model.GetThingShadowRequest;
import com.amazonaws.services.iotdata.model.GetThingShadowResult;
import com.amazonaws.services.iotdata.model.UpdateThingShadowRequest;
import com.amazonaws.services.iotdata.model.UpdateThingShadowResult;
import com.renesas.wifi.BuildInformation;
import com.renesas.wifi.activity.MainActivity;
import com.renesas.wifi.R;
import com.renesas.wifi.awsiot.log.S3Util;
import com.renesas.wifi.awsiot.setting.sensor.SensorSettingActivity;
import com.renesas.wifi.awsiot.shadow.SensorMetaData;
import com.renesas.wifi.awsiot.shadow.SensorStatus;
import com.renesas.wifi.buildOption;
import com.renesas.wifi.util.CustomToast;
import com.renesas.wifi.util.MyLog;
import com.renesas.wifi.util.NetworkUtil;
import com.renesas.wifi.util.StaticDataSave;
import com.renesas.wifi.util.ThreadUtils;
import com.google.gson.Gson;
import com.mikepenz.iconics.view.IconicsImageView;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import de.nitri.gauge.Gauge;

import static android.net.wifi.SupplicantState.COMPLETED;
import static com.renesas.wifi.awsiot.setting.sensor.SensorFunctionSetFragment.badge_notification;

public class AWSIoTSensorActivity extends Activity {

    public static Regions MY_REGION;
    SubScribeAsyncTask subScribeAsyncTask;

    NotificationManager notificationManager;
    private Vibrator myVib;
    static float density;

    // Message
    private static final String APP_CONNECT_MESSAGE = "connected";
    private static final String DEVICE_CONNECT_RESPONSE_MESSAGE = "yes";

    private static final String APP_UPDATE_SENSOR_MESSAGE = "updateSensor";
    private static final String DEVICE_UPDATE_SENSOR_RESPONSE_MESSAGE = "updated";

    private static final String APP_CONTROL_LED_ON_MESSAGE = "ledOn";
    private static final String DEVICE_CONTROL_LED_ON_RESPONSE_MESSAGE = "on";

    private static final String APP_CONTROL_LED_OFF_MESSAGE = "ledOff";
    private static final String DEVICE_CONTROL_LED_OFF_RESPONSE_MESSAGE = "off";

    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = "iot_keystore";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "default";

    private View decorView;
    private int uiOptions;

    public static Context mContext;

    public Handler mHandler_sensor_main;
    public Runnable mRunnable_sensor_main;

    private Handler mHandler_device_connect_noResponse;
    private Runnable mRunnable_device_connect_noResponse;

    private Handler mHandler_mqtt_connect_noResponse;
    private Runnable mRunnable_mqtt_connect_noResponse;

    private Handler mHandler_update_sensor_noResponse;
    private Runnable mRunnable_update_sensor_noResponse;

    private Handler mHandler_ledOn_noResponse;
    private Runnable mRunnable_ledOn_noResponse;

    private Handler mHandler_ledOff_noResponse;
    private Runnable mRunnable_ledOff_noResponse;

    CustomToast customToast = null;
    Handler mHandler_toast;
    S3Util S3util;

    NetworkChangeReceiver mNetworkReceiver = null;

    LinearLayout ll_debug;

    TextView tvSubscribeTopic1;
    TextView tvSubscribeTopic2;

    TextView tvLastSubscribeMessage1;
    TextView tvLastSubscribeMessage2;

    TextView tvPublishTopic1;
    TextView tvPublishTopic2;

    TextView tvPublishMessage1;
    TextView tvPublishMessage2;

    ImageView iv_deviceNetworkState;

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

    TextView textMainKeyName;
    TextView tv_aws;
    RelativeLayout layerMainKey;
    LinearLayout layerNoConnect;
    LinearLayout layerConnecting;

    ProgressBar progressingConnecting;

    ProgressDialog waitDialog = null;
    ProgressDialog updateDialog;
    MaterialDialog terminateDialog = null;
    MaterialDialog noInternetDialog = null;
    MaterialDialog checkGPSDialog = null;
    MaterialDialog noResponseDialog = null;
    MaterialDialog batteryAlertDialog = null;
    MaterialDialog temperatureAlertDialog = null;
    MaterialDialog deviceConnectFailDialog = null;
    ProgressDialog ledControlDialog;
    public ProgressDialog updatingDialog = null;
    public AlertDialog updateSuccessDialog = null;
    public AlertDialog updateFailDialog = null;

    ImageView imgConnStat;
    ImageView imgDisconnStat;
    TextView updatedTimeText1;
    TextView updatedTimeText2;
    ImageView iv_update_sensor;

    LinearLayout ll_ledOn;
    LinearLayout ll_ledOff;
    ImageView iv_ledOn;
    ImageView iv_ledOff;
    TextView tv_ledOn;
    TextView tv_ledOff;

    TextView temperatureTitle;
    TextView humidityTitle;
    TextView ambientLightTitle;
    TextView airQualityTitle;
    TextView gasResistanceTitle;
    TextView pressureTitle;
    TextView proximityTitle;
    TextView magnetoTitle;
    TextView batteryTitle;

    LinearLayout ll_notify;
    LinearLayout ll_haptic;
    LinearLayout ll_setting;

    ImageView iv_notify;
    TextView tv_notify;
    ImageView iv_haptic;
    TextView tv_haptic;
    ImageView iv_setting;
    TextView tv_setting;
    TextView sensor_badge_setting;

    private final static int BUTTON_CLICK_TIME = 500;
    private final static int GET_SHADOW_TIME = 1000;
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

    WifiManager wifiManager;

    public static GetShadowTask getStatusShadowTask;
    public static GetSensorShadowTask GetSensorShadowTask;
    public static DeleteShadowTask deleteShadowTask;

    public int publishCount = 0;

    public static AWSConfig awsConfig;
    public static Activity activity;

    //public static int batteryMax_MCU = 3213;  // 6 Volt
    public static int batteryMax_MCU = 2200;  // 5.5 Volt
    public static int batteryMin_MCU = 1500;  // 4 Volt
    //public static int batteryMin_MCU = 1957;  // 3.5 Volt
    public static int unitBattery_MCU = 0;

    //public static int batteryMax_PTIM = 3213;  // 6 Volt
    public static int batteryMax_PTIM = 2940;  // 5.5 Volt
    //public static int batteryMax_PTIM = 2687;  // 5 Volt
    public static int batteryMin_PTIM = 2240;  // 4 Volt
    //public static int batteryMin_PTIM = 1957;  // 3.5 Volt
    public static int unitBattery_PTIM = 0;

    private boolean hasTemperature, hasHumidity, hasPressure, hasCompass, hasGyroscope, hasAccelerometer, hasAmbientLight, hasAirQuality, hasProximity, hasButton;

    TextView temperatureLabel;
    TextView humidityLabel;
    TextView pressureLabel;
    TextView ambientLightLabel;
    TextView airQualityLabel;
    TextView gasResistanceLabel;
    TextView proximityLabel;
    TextView compassLabel;
    TextView batteryLabel;

    private ImageView temperatureImage;
    private ClipDrawable temperatureClip;
    private ImageView humidityImage;
    private ClipDrawable humidityClip;

    private float pressurePreviousValue;
    private Gauge pressureGauge;


    protected boolean objectNearby;
    protected boolean pressed;

    protected IconicsImageView ambientLightImage;
    private IconicsImageView airQualityImage;
    private IconicsImageView proximityImage;
    private ImageView compassImage;
    Bitmap compassBitmap;
    private ImageView batteryImage;

    private ImageView buttonOverlay;

    protected float magnetox, magnetoy, magnetoz;
    protected float degrees;
    protected float heading;
    private float currentDegree = 0f;


    int hum_score, gas_score;
    float gas_reference = 2500.0f;
    float hum_reference = 40.0f;
    int getgasreference_count = 0;
    float current_humidity;
    int gas_lower_limit = 10000;   // Bad air quality limit
    int gas_upper_limit = 300000;  // Good air quality limit

    TextView magnetoxLabel1;
    TextView magnetoxLabel2;
    TextView magnetoyLabel1;
    TextView magnetoyLabel2;
    TextView magnetozLabel1;
    TextView magnetozLabel2;
    TextView magneticLabel1;
    TextView magneticLabel2;

    boolean isTemperatureUpdated = false;
    boolean isWakeupUpdated = false;
    boolean isOtaUpdated = false;
    private static boolean isAyncTaskCompleted = true;

    long pastTemperatureTimestamp = 0;
    long pastWakeupTimestamp = 0;
    long pastOtaTimestamp = 0;

    public static AWSIoTSensorActivity instanceSensor;

    public static AWSIoTSensorActivity getInstanceMain() {
        return instanceSensor;
    }

    static boolean toggleOpen = false;

    private final Handler handler = new Handler();

    public Handler SensorHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            AWSEVENT event = AWSEVENT.values()[msg.what];
            switch (event) {

                case E_DEVICE_LEDON_NO_RESPONSE_REMOVE_CALLBACK:
                    MyLog.i("=== E_DEVICE_LEDON_NO_RESPONSE_REMOVE_CALLBACK ===");
                    if (noResponseDialog != null) {
                        if (noResponseDialog.isShowing()) {
                            noResponseDialog.dismiss();
                            noResponseDialog = null;
                        }
                    }
                    if (mHandler_ledOn_noResponse != null) {
                        mHandler_ledOn_noResponse.removeCallbacksAndMessages(null);
                    }
                    break;

                case E_DEVICE_LEDOFF_NO_RESPONSE_REMOVE_CALLBACK:
                    MyLog.i("=== E_DEVICE_LEDOFF_NO_RESPONSE_REMOVE_CALLBACK ===");
                    if (noResponseDialog != null) {
                        if (noResponseDialog.isShowing()) {
                            noResponseDialog.dismiss();
                            noResponseDialog = null;
                        }
                    }
                    if (mHandler_ledOff_noResponse != null) {
                        mHandler_ledOff_noResponse.removeCallbacksAndMessages(null);
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
                            clickLedOffImage();
                            toggleOpen = true;
                        } else {
                            clickLedOnImage();
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
                                .content("The remaining battery capacity of the device is under 10%." + "\n" + "Please change battery of device.")
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
                                .content("A high temperature has been detected from the device sensor." + "\n" + "Please check it.")
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
                    sendEvent(AWSEVENT.E_STOP_GET_SHADOW);
                    sendEvent(AWSEVENT.E_STOP_UPDATE_SENSOR);

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
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    public void sendEvent(AWSEVENT _Event) {
        int m;
        m = _Event.ordinal();
        Message msg = SensorHandler.obtainMessage(m);
        SensorHandler.sendMessage(msg);
    }

    public void sendEvent(AWSEVENT _Event, int _arg1) {
        int m;
        m = _Event.ordinal();
        Message msg = SensorHandler.obtainMessage(m);
        msg.arg1 = _arg1;
        SensorHandler.sendMessage(msg);
    }

    private void postEvent(AWSEVENT _Event, int _time) {
        int m;
        m = _Event.ordinal();
        Message msg = SensorHandler.obtainMessage(m);
        SensorHandler.sendEmptyMessageDelayed(m, _time);
    }

    public void removeEvent(AWSEVENT _Event) {
        int m;
        m = _Event.ordinal();
        Message msg = SensorHandler.obtainMessage(m);
        SensorHandler.removeMessages(m);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.awsiot_activity_sensor);

        getBuildInformation();

        AWSConfig.setConfig();

        mNetworkReceiver = new NetworkChangeReceiver();
        IntentFilter network_filter = new IntentFilter();
        network_filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, network_filter);

        mContext = this;
        instanceSensor = this;
        activity = AWSIoTSensorActivity.this;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        density = displayMetrics.density;
        MyLog.i(">>> density = " + String.valueOf(density));

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        S3util = new S3Util();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        myVib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

        MySoundPlayer.initSounds(getApplicationContext());

        customToast = new CustomToast(getApplicationContext());
        mHandler_toast = new Handler();

        mHandler_sensor_main = new Handler();
        mRunnable_sensor_main = new Runnable() {
            @Override
            public void run() {

            }
        };


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
                MyLog.i("mRunnable_mqtt_connect :: messageReceived = " + messageReceived);
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
                }
                layerConnecting.setVisibility(View.GONE);
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
                }
            }
        };

        mHandler_ledOn_noResponse = new Handler();
        mRunnable_ledOn_noResponse = new Runnable() {
            @Override
            public void run() {

                if (iv_deviceNetworkState != null) {
                    iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_red);
                    iv_deviceNetworkState.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                    iv_deviceNetworkState.clearAnimation();
                }

                if (ledControlDialog != null) {
                    ledControlDialog.dismiss();
                    ledControlDialog = null;
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
                                    sendEvent(AWSEVENT.E_DEVICE_LEDON_NO_RESPONSE_REMOVE_CALLBACK);
                                    sendEvent(AWSEVENT.E_GET_SHADOW);
                                    sendEvent(AWSEVENT.E_UPDATE_SENSOR);
                                }
                            })
                            .positiveText("OK")
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    sendEvent(AWSEVENT.E_DEVICE_LEDON_NO_RESPONSE_REMOVE_CALLBACK);
                                    dialog.dismiss();
                                    noResponseDialog = null;
                                    clickLedOffImage();
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
                }
            }
        };

        mHandler_ledOff_noResponse = new Handler();
        mRunnable_ledOff_noResponse = new Runnable() {
            @Override
            public void run() {

                if (iv_deviceNetworkState != null) {
                    iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_red);
                    iv_deviceNetworkState.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                    iv_deviceNetworkState.clearAnimation();
                }

                if (ledControlDialog != null) {
                    ledControlDialog.dismiss();
                    ledControlDialog = null;
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
                                    sendEvent(AWSEVENT.E_DEVICE_LEDOFF_NO_RESPONSE_REMOVE_CALLBACK);
                                    sendEvent(AWSEVENT.E_GET_SHADOW);
                                    sendEvent(AWSEVENT.E_UPDATE_SENSOR);
                                }
                            })
                            .positiveText("OK")
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    sendEvent(AWSEVENT.E_DEVICE_LEDOFF_NO_RESPONSE_REMOVE_CALLBACK);
                                    dialog.dismiss();
                                    noResponseDialog = null;
                                    clickLedOnImage();
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
                }
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
                        MyLog.i("isMqttConnected = " + isMqttConnected + ", isDeviceConnected = " + isDeviceConnected);
                        if (isMqttConnected && isDeviceConnected) {
                            updateSensorRequest();
                        } else if (isMqttConnected == false) {
                            mHandler_toast.post(new ToastRunnable("The network connection is uncertain"));
                        } else if (isDeviceConnected == false) {
                            mHandler_toast.post(new ToastRunnable("It is not connected to the device"));
                        }
                    }
                }
            });
        }

        if (ll_ledOn != null) {
            ll_ledOn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Thread.sleep(BUTTON_CLICK_TIME);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    clickLedOnImage();
                }
            });
        }

        if (ll_ledOff != null) {
            ll_ledOff.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Thread.sleep(BUTTON_CLICK_TIME);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    clickLedOffImage();
                }
            });
        }


        if (ll_notify != null && iv_notify != null) {
            iv_notify.setColorFilter(getResources().getColor(R.color.dark_gray), PorterDuff.Mode.MULTIPLY);
            ll_notify.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mHandler_toast.post(new ToastRunnable("This function is not supported."));
                }
            });
        }
        if (tv_notify != null) {
            tv_notify.setTextColor(getResources().getColor(R.color.dark_gray));
        }

        if (ll_haptic != null && iv_haptic != null) {
            iv_haptic.setColorFilter(getResources().getColor(R.color.dark_gray), PorterDuff.Mode.MULTIPLY);
            ll_haptic.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mHandler_toast.post(new ToastRunnable("This function is not supported."));
                }
            });
        }
        if (tv_haptic != null) {
            tv_haptic.setTextColor(getResources().getColor(R.color.dark_gray));
        }


        if (ll_setting != null && iv_setting != null) {

            ll_setting.setOnTouchListener(new View.OnTouchListener() {
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

            ll_setting.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent main = new Intent(AWSIoTSensorActivity.this, SensorSettingActivity.class);
                    startActivity(main);
                    MyLog.i("=== click Setting Button === ");
                }
            });
        }

        temperatureImage = findViewById(R.id.temperatureImage);
        temperatureClip = (ClipDrawable) ((LayerDrawable) temperatureImage.getDrawable()).getDrawable(1);

        humidityImage = findViewById(R.id.humidityImage);
        humidityClip = (ClipDrawable) ((LayerDrawable) humidityImage.getDrawable()).getDrawable(1);

        pressureGauge = findViewById(R.id.pressureGauge);

        ambientLightImage = findViewById(R.id.ambientLightImage);
        ambientLightImage.bringToFront();
        airQualityImage = findViewById(R.id.airQualityImage);

        proximityImage = findViewById(R.id.proximityImage);
        proximityImage.bringToFront();

        batteryImage = findViewById(R.id.batteryImage);
        if (batteryImage != null) {
            batteryImage.setColorFilter(R.color.md_blue_grey_100, PorterDuff.Mode.MULTIPLY);
        }
        batteryImage.bringToFront();

        hasButton = findViewById(R.id.buttonOverlay) != null;
        buttonOverlay = findViewById(R.id.buttonOverlay);
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

        mqttManager = new AWSIotMqttManager(clientId, AWSConfig.CUSTOMER_SPECIFIC_ENDPOINT);
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
                //]]
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
                                sendEvent(AWSEVENT.E_STOP_GET_SHADOW);
                                sendEvent(AWSEVENT.E_STOP_UPDATE_SENSOR);

                                try {
                                    if (subScribeAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                                        subScribeAsyncTask.cancel(true);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
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
                                sendEvent(AWSEVENT.E_STOP_GET_SHADOW);
                                sendEvent(AWSEVENT.E_STOP_UPDATE_SENSOR);

                                try {
                                    if (subScribeAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                                        subScribeAsyncTask.cancel(true);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
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
                                sendEvent(AWSEVENT.E_STOP_GET_SHADOW);
                                sendEvent(AWSEVENT.E_STOP_UPDATE_SENSOR);

                                try {
                                    if (subScribeAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                                        subScribeAsyncTask.cancel(true);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
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
            sendEvent(AWSEVENT.E_STOP_GET_SHADOW);
            sendEvent(AWSEVENT.E_STOP_UPDATE_SENSOR);

            try {
                if (subScribeAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                    subScribeAsyncTask.cancel(true);
                }
            } catch (Exception e1) {
                e.printStackTrace();
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
        tvPublishTopic2.setText(pubTopic);
        tvPublishMessage2.setText(msg);

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
            MyLog.i("mHandler_device_connect.postDelayed(mRunnable_device_connect, SUBCRIBE_TIMEOUT_TIME) 1");
        } catch (Exception e) {
            MyLog.e("Publish error. >>> " + e);
        }
    }

    private void ledOn() {
        SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);
        final String topic = StaticDataSave.thingName + "/" + "AppControl";
        String msg = "";
        publishCount++;

        if (buildOption.TEST_MODE == true) {
            msg = APP_CONTROL_LED_ON_MESSAGE + " <" + publishCount + "> " + sfd.format(new Date());
        } else {
            msg = APP_CONTROL_LED_ON_MESSAGE;
        }

        tvPublishTopic2.setText(topic);
        tvPublishMessage2.setText(msg);

        try {
            mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
            MyLog.i("=================================");
            MyLog.i("Message published:");
            MyLog.i("   pubTopic: " + topic);
            MyLog.i(" Message: " + msg);
            MyLog.i("=================================");

            mHandler_device_connect_noResponse.postDelayed(mRunnable_device_connect_noResponse, SUBCRIBE_TIMEOUT_TIME);
            MyLog.i("mHandler_device_connect.postDelayed(mRunnable_device_connect, SUBCRIBE_TIMEOUT_TIME)");
        } catch (Exception e) {
            MyLog.e("Publish error. >>> " + e);
        }

    }

    private void ledOff() {

        SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);
        final String topic = StaticDataSave.thingName + "/" + "AppControl";
        String msg = "";
        publishCount++;

        if (buildOption.TEST_MODE == true) {
            msg = APP_CONTROL_LED_OFF_MESSAGE + " <" + publishCount + "> " + sfd.format(new Date());
        } else {
            msg = APP_CONTROL_LED_OFF_MESSAGE;
        }

        tvPublishTopic2.setText(topic);
        tvPublishMessage2.setText(msg);

        try {
            mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
            MyLog.i("=================================");
            MyLog.i("Message published:");
            MyLog.i("   pubTopic: " + topic);
            MyLog.i(" Message: " + msg);
            MyLog.i("=================================");

            mHandler_device_connect_noResponse.postDelayed(mRunnable_device_connect_noResponse, SUBCRIBE_TIMEOUT_TIME);
            MyLog.i("mHandler_device_connect.postDelayed(mRunnable_device_connect, SUBCRIBE_TIMEOUT_TIME)");
        } catch (Exception e) {
            MyLog.e("Publish error. >>> " + e);
        }
    }

    private void updateSensorRequest() {
        if (updateDialog == null) {
            updateDialog = new ProgressDialog(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
            updateDialog.setMessage("Reading from sensor devices ...");
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
        }

        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);
        final String topic = StaticDataSave.thingName + "/" + "AppControl";
        final String msg = APP_UPDATE_SENSOR_MESSAGE;
        tvPublishTopic2.setText(topic);
        tvPublishMessage2.setText(msg);

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
        SensorStatus ss = gson.fromJson(statusState, SensorStatus.class);

        Gson gson1 = new Gson();
        SensorMetaData sm = gson1.fromJson(statusState, SensorMetaData.class);

        if (sm.metadata.reported.temperature != null) {
            Long curTemperatureTimestamp = sm.metadata.reported.temperature.timestamp;
            if (curTemperatureTimestamp != null) {
                Date dt1 = new Date(curTemperatureTimestamp * 1000);
                SimpleDateFormat sfd1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
                updatedTimeText2.setText(sfd1.format(dt1));

                if (pastTemperatureTimestamp != curTemperatureTimestamp) {
                    isTemperatureUpdated = true;
                } else {
                    isTemperatureUpdated = false;
                }
            }

            MyLog.i("isTemperatureUpdated = " + String.valueOf(isTemperatureUpdated));

            if (isTemperatureUpdated) {

                temperatureUpdateUI(ss.state.reported.temperature);

                humidityUpdateUI(ss.state.reported.humidity);

                pressureUpdateUI(ss.state.reported.pressure);

                ambientLightUpdateUI(ss.state.reported.ambient);

                airQualityUpdateUI(ss.state.reported.humidity, ss.state.reported.gaslock);

                magnetoUpdateUI(ss.state.reported.magnetox, ss.state.reported.magnetoy, ss.state.reported.magnetoz);

                batteryUpdateUI(ss.state.reported.battery);

                ledUpdateUI(ss.state.reported.ledonoff);

            }
            pastTemperatureTimestamp = curTemperatureTimestamp;
            isTemperatureUpdated = false;
        }

        if (sm.metadata.reported.wakeupnum != null) {
            Long curWakeupTimestamp = sm.metadata.reported.wakeupnum.timestamp;
            if (curWakeupTimestamp != null) {
                Date dt2 = new Date(curWakeupTimestamp * 1000);
                SimpleDateFormat sfd2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
                updatedTimeText2.setText(sfd2.format(dt2));

                if (pastWakeupTimestamp != curWakeupTimestamp) {
                    isWakeupUpdated = true;
                } else {
                    isWakeupUpdated = false;
                }
            }

            MyLog.i("isWakeupUpdated = " + String.valueOf(isWakeupUpdated));

            if (isWakeupUpdated) {
                StaticDataSave.wakeupNum = ss.state.reported.wakeupnum;

                proximityUpdateUI(ss.state.reported.wakeupnum);
                buttonUpdateUI(ss.state.reported.wakeupnum);
            }

            pastWakeupTimestamp = curWakeupTimestamp;
            isWakeupUpdated = false;
        }

        if (sm.metadata.reported.OTAupdate != null) {
            Long curOtaTimestamp = sm.metadata.reported.OTAupdate.timestamp;
            if (curOtaTimestamp != null) {
                Date dt3 = new Date(curOtaTimestamp * 1000);
                SimpleDateFormat sfd3 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
                updatedTimeText2.setText(sfd3.format(dt3));

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
                MyLog.i("ss.state.reported.OTAupdate = " + ss.state.reported.OTAupdate);
                if (ss.state.reported.OTAupdate == 0) {  //none
                    StaticDataSave.existOTAupdateFlag = false;
                    StaticDataSave.OTAupdateProgressFlag = false;
                    sensor_badge_setting.setVisibility(View.INVISIBLE);
                } else if (ss.state.reported.OTAupdate == 1) {  // exist update
                    StaticDataSave.existOTAupdateFlag = true;
                    StaticDataSave.OTAupdateProgressFlag = false;
                    sensor_badge_setting.setVisibility(View.VISIBLE);
                } else if (ss.state.reported.OTAupdate == 2) {  // update progressing
                    StaticDataSave.existOTAupdateFlag = false;
                    StaticDataSave.OTAupdateProgressFlag = true;
                    sensor_badge_setting.setVisibility(View.INVISIBLE);
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
        SensorStatus ss = gson.fromJson(statusState, SensorStatus.class);

        Gson gson1 = new Gson();
        SensorMetaData sm = gson1.fromJson(statusState, SensorMetaData.class);

        if (sm.metadata.reported.temperature != null) {
            long curTemperatureTimestamp = sm.metadata.reported.temperature.timestamp;
            Date dt1 = new Date(curTemperatureTimestamp * 1000);
            SimpleDateFormat sfd1 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
            updatedTimeText2.setText(sfd1.format(dt1));

            if (pastTemperatureTimestamp != curTemperatureTimestamp) {
                isTemperatureUpdated = true;
            } else {
                isTemperatureUpdated = false;
            }
            MyLog.i("isTemperatureUpdated = " + String.valueOf(isTemperatureUpdated));

            if (isTemperatureUpdated) {

                temperatureUpdateUI(ss.state.reported.temperature);

                humidityUpdateUI(ss.state.reported.humidity);

                pressureUpdateUI(ss.state.reported.pressure);

                ambientLightUpdateUI(ss.state.reported.ambient);

                airQualityUpdateUI(ss.state.reported.humidity, ss.state.reported.gaslock);

                magnetoUpdateUI(ss.state.reported.magnetox, ss.state.reported.magnetoy, ss.state.reported.magnetoz);

                batteryUpdateUI(ss.state.reported.battery);

                ledUpdateUI(ss.state.reported.ledonoff);
            }
            pastTemperatureTimestamp = curTemperatureTimestamp;
            isTemperatureUpdated = false;
        }

        if (sm.metadata.reported.wakeupnum != null) {
            long curWakeupTimestamp = sm.metadata.reported.wakeupnum.timestamp;
            Date dt2 = new Date(curWakeupTimestamp * 1000);
            SimpleDateFormat sfd2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
            updatedTimeText2.setText(sfd2.format(dt2));

            if (pastWakeupTimestamp != curWakeupTimestamp) {
                isWakeupUpdated = true;
            } else {
                isWakeupUpdated = false;
            }
            MyLog.i("isWakeupUpdated = " + String.valueOf(isWakeupUpdated));

            if (isWakeupUpdated) {
                StaticDataSave.wakeupNum = ss.state.reported.wakeupnum;
                MyLog.i(">>> StaticDataSave.wakeupNum =" + StaticDataSave.wakeupNum);

                proximityUpdateUI(ss.state.reported.wakeupnum);
                buttonUpdateUI(ss.state.reported.wakeupnum);
            }

            pastWakeupTimestamp = curWakeupTimestamp;
            isWakeupUpdated = false;
        }

        if (sm.metadata.reported.OTAupdate != null) {
            long curOtaTimestamp = sm.metadata.reported.OTAupdate.timestamp;
            Date dt3 = new Date(curOtaTimestamp * 1000);
            SimpleDateFormat sfd3 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
            updatedTimeText2.setText(sfd3.format(dt3));

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
                MyLog.i("ss.state.reported.OTAupdate = " + ss.state.reported.OTAupdate);
                if (ss.state.reported.OTAupdate == 0) {  //none
                    StaticDataSave.existOTAupdateFlag = false;
                    StaticDataSave.OTAupdateProgressFlag = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (getRunActivity().equals("com.renesas.wifi.awsiot.AWSIoTSensorActivity")) {
                                sensor_badge_setting.setVisibility(View.INVISIBLE);
                            } else if (getRunActivity().equals("com.renesas.wifi.awsiot.setting.sensor.SensorSettingActivity")) {
                                badge_notification.setVisibility(View.INVISIBLE);

                            }
                        }
                    });

                    if (ss.state.reported.OTAresult != null) {
                        if (ss.state.reported.OTAresult.equals("OTA_OK")) {

                            if (getRunActivity().equals("com.renesas.wifi.awsiot.AWSIoTSensorActivity")) {
                                if (updatingDialog != null && updatingDialog.isShowing()) {
                                    showUpdateSuccessDialog();
                                }
                            } else if (getRunActivity().equals("com.renesas.wifi.awsiot.setting.sensor.SensorSettingActivity")) {
                                if (SensorSettingActivity.getInstanceSetting() != null) {
                                    if (SensorSettingActivity.getInstanceSetting().updatingDialog != null) {
                                        if (SensorSettingActivity.getInstanceSetting().updatingDialog.isShowing()) {
                                            SensorSettingActivity.getInstanceSetting().showCompleteDialog();
                                        }
                                    }
                                }
                            }
                        } else if (ss.state.reported.OTAresult.equals("OTA_UNKNOWN")) {
                            if (updatingDialog != null && updatingDialog.isShowing()) {
                                dismissUpdatingDialog();
                            }
                        }
                    }
                } else if (ss.state.reported.OTAupdate == 1) {  // exist update
                    StaticDataSave.existOTAupdateFlag = true;
                    StaticDataSave.OTAupdateProgressFlag = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (getRunActivity().equals("com.renesas.wifi.awsiot.AWSIoTSensorActivity")) {
                                sensor_badge_setting.setVisibility(View.VISIBLE);
                            } else if (getRunActivity().equals("com.renesas.wifi.awsiot.setting.sensor.SensorSettingActivity")) {
                                badge_notification.setVisibility(View.VISIBLE);

                            }
                        }
                    });

                    if (ss.state.reported.OTAresult.equals("OTA_NG")) {
                        if (getRunActivity().equals("com.renesas.wifi.awsiot.AWSIoTSensorActivity")) {
                            if (updatingDialog != null && updatingDialog.isShowing()) {
                                showUpdateFailDialog();
                            }
                        } else if (getRunActivity().equals("com.renesas.wifi.awsiot.setting.sensor.SensorSettingActivity")) {
                            if (SensorSettingActivity.getInstanceSetting() != null) {
                                if (SensorSettingActivity.getInstanceSetting().updatingDialog != null) {
                                    if (SensorSettingActivity.getInstanceSetting().updatingDialog.isShowing()) {
                                        SensorSettingActivity.getInstanceSetting().showUpdateFailDialog();
                                    }
                                }
                            }
                        }
                    }
                } else if (ss.state.reported.OTAupdate == 2) {  // update progressing
                    StaticDataSave.existOTAupdateFlag = false;
                    StaticDataSave.OTAupdateProgressFlag = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (getRunActivity().equals("com.renesas.wifi.awsiot.AWSIoTSensorActivity")) {
                                sensor_badge_setting.setVisibility(View.INVISIBLE);
                            } else if (getRunActivity().equals("com.renesas.wifi.awsiot.setting.sensor.SensorSettingActivity")) {
                                badge_notification.setVisibility(View.INVISIBLE);

                            }
                        }
                    });

                    if (getRunActivity().equals("com.renesas.wifi.awsiot.AWSIoTSensorActivity")) {
                        showUpdatingDialog();
                    } else if (getRunActivity().equals("com.renesas.wifi.awsiot.setting.sensor.SensorSettingActivity")) {
                        if (SensorSettingActivity.getInstanceSetting() != null) {
                            SensorSettingActivity.getInstanceSetting().showUpdatingDialog();
                        }
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
                MyLog.e("Error in Update Device Shadow : " + result.getError());
            }
        }
    }

    private void deleteShadow(View view) {
        deleteShadows();
    }

    public void deleteShadows() {
        if (StaticDataSave.thingName != null) {
            deleteShadowTask = new DeleteShadowTask(StaticDataSave.thingName);
            deleteShadowTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            MyLog.i("StaticDataSave.thingName = null~");
        }
    }

    private class DeleteShadowTask extends AsyncTask<Void, Void, AsyncTaskResult<String>> {

        private final String thingName;

        public DeleteShadowTask(String name) {
            thingName = name;
        }

        @Override
        protected AsyncTaskResult<String> doInBackground(Void... voids) {

            try {
                MyLog.i("=== DeleteShadowTask ===");
                DeleteThingShadowRequest deleteThingShadowRequest = new DeleteThingShadowRequest().withThingName(thingName);
                DeleteThingShadowResult result = iotDataClient.deleteThingShadow(deleteThingShadowRequest);
                byte[] bytes = new byte[result.getPayload().remaining()];
                result.getPayload().get(bytes);
                String resultString = new String(bytes);
                return new AsyncTaskResult<String>(resultString);
            } catch (Exception e) {
                MyLog.e("DeleteShadowTask : " + e);
                return new AsyncTaskResult<String>(e);
            }
        }

        @Override
        protected void onPostExecute(AsyncTaskResult<String> result) {
            if (result.getError() == null) {
                MyLog.i(result.getResult());

            } else {
                MyLog.e("E / deleteShadowTask : " + result.getError());
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

        } else {
            textMainKeyName.setText(StaticDataSave.thingName);

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
                }
                postEvent(AWSEVENT.E_DEVICE_CONNECT_TIMEOUT, PROVISIONING_TIMEOUT_TIME);

            } else {
                if (isOnline() && clientKeyStore != null && isMqttConnected == false) {
                    connectMqtt();
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
                        sensor_badge_setting.setVisibility(View.VISIBLE);
                    } else {
                        sensor_badge_setting.setVisibility(View.INVISIBLE);
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
        sendEvent(AWSEVENT.E_STOP_GET_SHADOW);
        sendEvent(AWSEVENT.E_STOP_UPDATE_SENSOR);

        sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);
        sendEvent(AWSEVENT.E_MQTT_REMOVE_CALLBACK);

        SensorHandler.removeCallbacksAndMessages(null);

        try {
            if (subScribeAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                subScribeAsyncTask.cancel(true);
            }
        } catch (Exception e) {
        }

        MyLog.i("=== onPause() ===");
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
        SensorHandler.removeCallbacksAndMessages(null);

        try {
            if (getStatusShadowTask.getStatus() == AsyncTask.Status.RUNNING) {
                getStatusShadowTask.cancel(true);
            } else {
            }
        } catch (Exception e) {
        }


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

        removeEvent(AWSEVENT.E_GET_SHADOW);
        removeEvent(AWSEVENT.E_UPDATE_SENSOR);
        sendEvent(AWSEVENT.E_STOP_GET_SHADOW);
        sendEvent(AWSEVENT.E_STOP_UPDATE_SENSOR);

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

        if (terminateDialog != null) {
            terminateDialog.dismiss();
            terminateDialog = null;
        }
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
                                    startForegroundService(new Intent(AWSIoTSensorActivity.this, MonitorService.class));
                                } else {
                                    startService(new Intent(AWSIoTSensorActivity.this, MonitorService.class));
                                }
                            }

                            terminateDialog = null;
                            removeEvent(AWSEVENT.E_GET_SHADOW);
                            removeEvent(AWSEVENT.E_UPDATE_SENSOR);
                            sendEvent(AWSEVENT.E_STOP_GET_SHADOW);
                            sendEvent(AWSEVENT.E_STOP_UPDATE_SENSOR);
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

    private void initMainResource() {

        ll_debug = findViewById(R.id.ll_debug);
        if (ll_debug != null) {
            if (buildOption.DEBUG_MODE == true) {
                ll_debug.setVisibility(View.VISIBLE);
            } else {
                ll_debug.setVisibility(View.GONE);
            }
        }

        tvSubscribeTopic1 = findViewById(R.id.tvSubscribeTopic1);
        mySetTextSize(tvSubscribeTopic1, 10);

        tvSubscribeTopic2 = findViewById(R.id.tvSubscribeTopic2);
        mySetTextSize(tvSubscribeTopic2, 10);

        tvLastSubscribeMessage1 = findViewById(R.id.tvLastSubscribeMessage1);
        mySetTextSize(tvLastSubscribeMessage1, 10);

        tvLastSubscribeMessage2 = findViewById(R.id.tvLastSubscribeMessage2);
        mySetTextSize(tvLastSubscribeMessage2, 10);

        tvPublishTopic1 = findViewById(R.id.tvPublishTopic1);
        mySetTextSize(tvPublishTopic1, 10);

        tvPublishTopic2 = findViewById(R.id.tvPublishTopic2);
        mySetTextSize(tvPublishTopic2, 10);

        tvPublishMessage1 = findViewById(R.id.tvPublishMessage1);
        mySetTextSize(tvPublishMessage1, 10);

        tvPublishMessage2 = findViewById(R.id.tvPublishMessage2);
        mySetTextSize(tvPublishMessage2, 10);

        textMainKeyName = findViewById(R.id.textMainKeyName);
        mySetTextSize(textMainKeyName, 16);

        tv_aws = findViewById(R.id.tv_aws);
        mySetTextSize(tv_aws, 14);

        iv_deviceNetworkState = findViewById(R.id.iv_deviceNetworkState);
        if (iv_deviceNetworkState != null) {
            iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_red);
            iv_deviceNetworkState.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
            iv_deviceNetworkState.setVisibility(View.VISIBLE);
        }

        layerMainKey = findViewById(R.id.layerMainKey);

        if (layerMainKey != null) {
            layerMainKey.setBackgroundColor(getResources().getColor(R.color.navigation_bar_background));
        }

        layerNoConnect = findViewById(R.id.layerNoConnect);
        layerConnecting = findViewById(R.id.layerConnecting);

        progressingConnecting = findViewById(R.id.progressing_connect);

        Button btn_reconnect = (Button) findViewById(R.id.btn_reconnect);
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

        ll_notify = findViewById(R.id.ll_notify);
        ll_haptic = findViewById(R.id.ll_haptic);
        ll_setting = findViewById(R.id.ll_setting);

        iv_notify = findViewById(R.id.iv_notify);
        tv_notify = findViewById(R.id.tv_notify);
        mySetTextSize(tv_notify, 14);

        iv_haptic = findViewById(R.id.iv_haptic);
        tv_haptic = findViewById(R.id.tv_haptic);
        mySetTextSize(tv_haptic, 14);

        iv_setting = findViewById(R.id.iv_setting);
        tv_setting = findViewById(R.id.tv_setting);
        mySetTextSize(tv_setting, 14);

        sensor_badge_setting = findViewById(R.id.sensor_badge_setting);
        if (sensor_badge_setting != null) {
            sensor_badge_setting.setVisibility(View.INVISIBLE);
        }

        iv_update_sensor = findViewById(R.id.iv_update_sensor);

        ll_ledOn = findViewById(R.id.ll_ledOn);
        ll_ledOff = findViewById(R.id.ll_ledOff);
        iv_ledOn = findViewById(R.id.iv_ledOn);
        iv_ledOff = findViewById(R.id.iv_ledOff);
        if (iv_ledOn != null) {
            iv_ledOn.setColorFilter(getResources().getColor(R.color.magstatus_ok), PorterDuff.Mode.MULTIPLY);
        }
        if (iv_ledOff != null) {
            iv_ledOff.setColorFilter(getResources().getColor(R.color.light_gray), PorterDuff.Mode.MULTIPLY);
        }
        tv_ledOn = findViewById(R.id.tv_ledOn);
        mySetTextSize(tv_ledOn, 15);

        tv_ledOff = findViewById(R.id.tv_ledOff);
        mySetTextSize(tv_ledOff, 15);
        if (tv_ledOn != null) {
            tv_ledOn.setTextColor(getResources().getColor(R.color.magstatus_ok));
        }
        if (tv_ledOff != null) {
            tv_ledOff.setTextColor(getResources().getColor(R.color.light_gray));
        }

        updatedTimeText1 = findViewById(R.id.tv_updatedTime1);
        mySetTextSize(updatedTimeText1, 12);
        updatedTimeText2 = findViewById(R.id.tv_updatedTime2);
        mySetTextSize(updatedTimeText2, 12);

        temperatureTitle = findViewById(R.id.temperatureTitle);
        mySetTextSize(temperatureTitle, 14);

        humidityTitle = findViewById(R.id.humidityTitle);
        mySetTextSize(humidityTitle, 14);

        ambientLightTitle = findViewById(R.id.ambientLightTitle);
        mySetTextSize(ambientLightTitle, 14);

        airQualityTitle = findViewById(R.id.airQualityTitle);
        mySetTextSize(airQualityTitle, 14);

        gasResistanceTitle = findViewById(R.id.gasResistanceTitle);
        mySetTextSize(gasResistanceTitle, 14);

        pressureTitle = findViewById(R.id.pressureTitle);
        mySetTextSize(pressureTitle, 14);

        proximityTitle = findViewById(R.id.proximityTitle);
        mySetTextSize(proximityTitle, 14);

        magnetoTitle = findViewById(R.id.magnetoTitle);
        mySetTextSize(magnetoTitle, 14);

        batteryTitle = findViewById(R.id.batteryTitle);
        mySetTextSize(batteryTitle, 14);

        temperatureLabel = findViewById(R.id.temperatureLabel);
        mySetTextSize(temperatureLabel, 12);

        humidityLabel = findViewById(R.id.humidityLabel);
        mySetTextSize(humidityLabel, 12);

        pressureLabel = findViewById(R.id.pressureLabel);
        mySetTextSize(pressureLabel, 12);

        ambientLightLabel = findViewById(R.id.ambientLightLabel);
        mySetTextSize(ambientLightLabel, 12);

        airQualityLabel = findViewById(R.id.airQualityLabel);
        mySetTextSize(airQualityLabel, 12);

        gasResistanceLabel = findViewById(R.id.gasResistanceLabel);
        mySetTextSize(gasResistanceLabel, 12);

        proximityLabel = findViewById(R.id.proximityLabel);
        mySetTextSize(proximityLabel, 12);

        compassLabel = findViewById(R.id.compassLabel);
        mySetTextSize(compassLabel, 12);

        magnetoxLabel1 = findViewById(R.id.magnetoxLabel1);
        mySetTextSize(magnetoxLabel1, 10);
        magnetoxLabel2 = findViewById(R.id.magnetoxLabel2);
        mySetTextSize(magnetoxLabel2, 10);

        magnetoyLabel1 = findViewById(R.id.magnetoyLabel1);
        mySetTextSize(magnetoyLabel1, 10);
        magnetoyLabel2 = findViewById(R.id.magnetoyLabel2);
        mySetTextSize(magnetoyLabel2, 10);

        magnetozLabel1 = findViewById(R.id.magnetozLabel1);
        mySetTextSize(magnetozLabel1, 10);
        magnetozLabel2 = findViewById(R.id.magnetozLabel2);
        mySetTextSize(magnetozLabel2, 10);

        magneticLabel1 = findViewById(R.id.magneticLabel1);
        mySetTextSize(magneticLabel1, 12);
        magneticLabel2 = findViewById(R.id.magneticLabel2);
        mySetTextSize(magneticLabel2, 12);

        batteryLabel = findViewById(R.id.batteryLabel);
        mySetTextSize(batteryLabel, 12);

    }

    private void ledCtl(boolean off) {    // on : false, off : true

        if (off) {
            ll_ledOn.setVisibility(View.INVISIBLE);
            ll_ledOff.setVisibility(View.VISIBLE);

            MyLog.i(String.format("LED %s", "Off"));

        } else {
            ll_ledOff.setVisibility(View.INVISIBLE);
            ll_ledOn.setVisibility(View.VISIBLE);

            MyLog.i(String.format("LED %s", "On"));
        }
    }

    public void clickLedOffImage() {  //turn on

        if (updateDialog != null && updateDialog.isShowing()) {
            mHandler_toast.post(new ToastRunnable("Another job is being processed"));
        } else {
            MyLog.i("isMqttConnected = " + isMqttConnected + ", isDeviceConnected = " + isDeviceConnected);
            if (isMqttConnected && isDeviceConnected) {
                MyLog.i("LedOffImage is clicked!");

                ledControlDialog = new ProgressDialog(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                ledControlDialog.setTitle("LED Control");
                ledControlDialog.setMessage("Turn on LED ...");
                ledControlDialog.show();

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

                WindowManager.LayoutParams params = ledControlDialog.getWindow().getAttributes();
                int dialogWindowWidth = (int) (displayWidth * 0.8f);
                params.width = dialogWindowWidth;
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                ledControlDialog.getWindow().setAttributes((WindowManager.LayoutParams) params);

                layerMainKey.setVisibility(View.VISIBLE);
                layerNoConnect.setVisibility(View.GONE);

                if (noInternetDialog != null) {
                    noInternetDialog.dismiss();
                    noInternetDialog = null;
                }
                layerConnecting.setVisibility(View.GONE);
                ledOn();  // on

                // for AUTO TEST

                if (buildOption.AUTO_TEST == true) {

                    MyLog.i("================================================================================");
                    MyLog.i("AUTO_TEST  " + buildOption.AUTO_TEST_MIN_TIME + " min ~ " + buildOption.AUTO_TEST_MAX_TIME + " min");
                    MyLog.i("================================================================================");
                    postEvent(AWSEVENT.E_AUTO_TEST, 1000 * 60 * buildOption.AUTO_TEST_FIRST_START_TIME);

                    buildOption.AUTO_TEST = false;
                    MaterialDialog autoTestDialog = null;
                    if (autoTestDialog == null) {
                        autoTestDialog = new MaterialDialog.Builder(AWSIoTSensorActivity.this)
                                .theme(Theme.LIGHT)
                                .content("Running AUTO TEST for sensor ")
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
                    }

                }

                mHandler_ledOn_noResponse.postDelayed(mRunnable_ledOn_noResponse, COMMAND_TIMEOUT_TIME);

            } else if (isMqttConnected == false) {
                mHandler_toast.post(new ToastRunnable("The network connection is uncertain"));
            } else if (isDeviceConnected == false) {
                mHandler_toast.post(new ToastRunnable("It is not connected to the device"));
            }
        }
    }

    public void clickLedOnImage() {  //turn off

        if (updateDialog != null && updateDialog.isShowing()) {
            mHandler_toast.post(new ToastRunnable("Another job is being processed"));
        } else {
            MyLog.i("isMqttConnected = " + isMqttConnected + ", isDeviceConnected = " + isDeviceConnected);
            if (isMqttConnected && isDeviceConnected) {
                MyLog.i("LedOnImage is clicked!");

                ledControlDialog = new ProgressDialog(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                ledControlDialog.setTitle("LED Control");
                ledControlDialog.setMessage("Turn off LED ...");
                ledControlDialog.show();

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

                WindowManager.LayoutParams params = ledControlDialog.getWindow().getAttributes();
                int dialogWindowWidth = (int) (displayWidth * 0.8f);
                params.width = dialogWindowWidth;
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                ledControlDialog.getWindow().setAttributes((WindowManager.LayoutParams) params);

                layerMainKey.setVisibility(View.VISIBLE);
                layerNoConnect.setVisibility(View.GONE);
                if (noInternetDialog != null) {
                    noInternetDialog.dismiss();
                    noInternetDialog = null;
                }
                layerConnecting.setVisibility(View.GONE);
                ledOff();  // off

                mHandler_ledOff_noResponse.postDelayed(mRunnable_ledOff_noResponse, COMMAND_TIMEOUT_TIME);

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

                                                //DA16200
                                                if (subTopic.contains("Device")) {
                                                    tvSubscribeTopic2.setText(subTopic);
                                                    tvLastSubscribeMessage2.setText(message);
                                                }
                                                //

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
                                                } else if (message.equals(DEVICE_UPDATE_SENSOR_RESPONSE_MESSAGE)) {
                                                    messageReceived = true;
                                                    if (updateDialog != null) {
                                                        updateDialog.dismiss();
                                                        updateDialog = null;
                                                    }
                                                    sendEvent(AWSEVENT.E_UPDATE_SENSOR_NO_RESPONSE_REMOVE_CALLBACK);

                                                } else if (message.equals(DEVICE_CONTROL_LED_ON_RESPONSE_MESSAGE)) {
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
                                                    MyLog.i("sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK)");
                                                    sendEvent(AWSEVENT.E_DEVICE_LEDON_NO_RESPONSE_REMOVE_CALLBACK);
                                                    if (ledControlDialog != null) {
                                                        ledControlDialog.dismiss();
                                                    }
                                                    if (iv_deviceNetworkState != null) {
                                                        iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_green);
                                                        iv_deviceNetworkState.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                                                        iv_deviceNetworkState.clearAnimation();
                                                    }
                                                    ledCtl(false); // on
                                                } else if (message.equals(DEVICE_CONTROL_LED_OFF_RESPONSE_MESSAGE)) {
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
                                                    MyLog.i("sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK)");
                                                    sendEvent(AWSEVENT.E_DEVICE_LEDOFF_NO_RESPONSE_REMOVE_CALLBACK);
                                                    if (ledControlDialog != null) {
                                                        ledControlDialog.dismiss();
                                                    }
                                                    if (iv_deviceNetworkState != null) {
                                                        iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_green);
                                                        iv_deviceNetworkState.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                                                        iv_deviceNetworkState.clearAnimation();
                                                    }
                                                    ledCtl(true);  //off
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

    protected void setLevel(final ClipDrawable clip, int percentage) {
        percentage = Math.min(100, percentage);
        percentage = Math.max(0, percentage);
        final int level = percentage * 100; // 0..10000
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                clip.setLevel(level);
            }
        });
    }

    protected void temperatureUpdateUI(final float value) {

        int temperaturePercentage = (int) value * 2;
        setLevel(temperatureClip, temperaturePercentage);
        temperatureLabel.setText(String.format("%.1f", value) + " \u00b0" + "C");
    }

    protected void humidityUpdateUI(final float value) {

        int humidityPercentage = (int) value;
        setLevel(humidityClip, humidityPercentage);
        humidityLabel.setText(String.format("%.1f", value) + " %");
    }

    protected void pressureUpdateUI(final float value) {

        if (pressurePreviousValue != 0) {
            ThreadUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pressureGauge.setValue(pressurePreviousValue / 100.f);
                }
            });

        } else {
            ThreadUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pressureGauge.setValue(950.f);
                }
            });
        }
        pressurePreviousValue = value;
        pressureLabel.setText(String.format("%.1f %s", value / 100.f, " hPa"));
        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pressureGauge.moveToValue(value / 100.f);
            }
        });
    }

    protected void ambientLightUpdateUI(final float value) {

        ambientLightLabel.setText((int) value / 4 + " lux");
        int adjust = Math.min((int) value * 128 / 5000, 128);
        int color = 0xFFCFD8DC - 0x010000 * adjust - 0x000100 * (adjust / 2);
        ambientLightImage.getIcon().color(color);
    }

    /* AirQuality */

    public static final int UNKNOWN = -1;
    public static final int GOOD = 0;
    public static final int AVERAGE = 1;
    public static final int LITTLE_BAD = 2;
    public static final int BAD = 3;
    public static final int VERY_BAD = 4;
    public static final int GET_OUT = 5;

    public static final int[] ACCURACY = new int[]{
            R.string.air_quality_accuracy_unreliable,
            R.string.air_quality_accuracy_low,
            R.string.air_quality_accuracy_medium,
            R.string.air_quality_accuracy_high,
    };

    public static final int[] QUALITY = new int[]{
            R.string.value_air_quality_good,
            R.string.value_air_quality_average,
            R.string.value_air_quality_little_bad,
            R.string.value_air_quality_bad,
            R.string.value_air_quality_very_bad,
            R.string.value_air_quality_worst,
    };

    public static final int[] COLOR = new int[]{
            R.color.md_green_700,
            R.color.md_yellow_700,
            R.color.md_orange_700,
            R.color.md_red_700,
            R.color.md_purple_700,
            R.color.md_black_1000,
    };

    public static final int[] RANGE = new int[]{
            50,
            100,
            150,
            200,
            300
    };

    protected float quality;
    protected int airQualityIndex;

    protected void airQualityUpdateUI(final float hum, final float gas) {

        int index = 0;
        index = calculateIAQindex(hum, gas);
        MyLog.i("index = " + String.valueOf(index));

        if (index >= 0) {
            airQualityImage.getIcon().colorRes(COLOR[index]);
            airQualityLabel.setText(QUALITY[index]);
            gasResistanceLabel.setText(String.valueOf((int) gas) + " Ohms");
        } else {
            airQualityImage.getIcon().colorRes(R.color.light_gray);
            airQualityLabel.setText("Unknown");
            gasResistanceLabel.setText("Unknown");
        }
    }

    private int calculateIAQindex(float hum, float gas) {
        int index = -1;
        float gas_baseline = 100000.f;
        float hum_baseline = 40.0f;
        float hum_weighting = 0.25f;
        float gas_offset = gas_baseline - gas;
        float hum_offset = hum - hum_baseline;

        //Calculate hum_score as the distance from the hum_baseline.
        if (hum_offset > 0) {
            hum_score = (int) (100 - hum_baseline - hum_offset);
            hum_score /= (100 - hum_baseline);
            hum_score *= (hum_weighting * 100);
        } else {
            hum_score = (int) (hum_baseline + hum_offset);
            hum_score /= hum_baseline;
            hum_score *= (hum_weighting * 100);
        }

        // Calculate gas_score as the distance from the gas_baseline.
        if (gas_offset > 0) {
            gas_score = (int) (gas / gas_baseline);
            gas_score *= (100 - (hum_weighting * 100));
        } else {
            gas_score = (int) (100 - (hum_weighting * 100));
        }


        // Calculate air_quality_score.
        int air_quality_score = hum_score + gas_score;
        MyLog.i("air_quality_score = " + air_quality_score);

        if (air_quality_score >= 301) index = 5;
        else if (air_quality_score >= 201 && air_quality_score <= 300) index = 4;
        else if (air_quality_score >= 176 && air_quality_score <= 200) index = 3;
        else if (air_quality_score >= 151 && air_quality_score <= 175) index = 2;
        else if (air_quality_score >= 51 && air_quality_score <= 150) index = 1;
        else if (air_quality_score >= 00 && air_quality_score <= 50) index = 0;

        return index;
    }

    protected void magnetoUpdateUI(final float x, final float y, final float z) {

        if (Math.abs(x) > 5160 || Math.abs(y) > 5160 || Math.abs(z) > 5160) {
            magnetoxLabel2.setText("Unknown");
            magnetoyLabel2.setText("Unknown");
            magnetozLabel2.setText("Unknown");
            magneticLabel2.setText("Unknown");
        } else {
            double magnitude = Math.sqrt((x * x) + (y * y) + (z * z));
            magnetoxLabel2.setText(String.format("%.2f", x));
            magnetoyLabel2.setText(String.format("%.2f", y));
            magnetozLabel2.setText(String.format("%.2f", z));
            magneticLabel2.setText(String.format("%.2f", magnitude) + " \u00B5T");
        }
    }


    protected void proximityUpdateUI(final int value) {

        if (value == 2) {
            objectNearby = true;
        } else {
            objectNearby = false;
        }
        proximityLabel.setText(objectNearby ? R.string.value_proximity_on : R.string.value_proximity_off);
        proximityImage.getIcon().colorRes(objectNearby ? R.color.md_blue_400 : R.color.md_blue_grey_100);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (value == 2) {
            objectNearby = false;
            wakeupNumInit();
        }
    }

    private void wakeupNumInit() {
        UpdateShadowTask UpdateShadowTask = new UpdateShadowTask();
        UpdateShadowTask.setThingName(StaticDataSave.thingName);
        String newState = String.format("{\"state\":{\"reported\":{\"wakeupnum\":%d}}}", 0);
        UpdateShadowTask.setState(newState);
        UpdateShadowTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    protected void buttonUpdateUI(final int value) {

        MyLog.i("value = " + value);
        if (value == 1) {
            pressed = true;
        } else {
            pressed = false;
        }
        buttonOverlay.setVisibility(pressed ? View.VISIBLE : View.GONE);
        if (pressed) {
            MySoundPlayer.play(MySoundPlayer.SUCCESS);
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (value == 1) {
            pressed = false;
            wakeupNumInit();
        }
    }


    /* Compass */

    public void calculateHeading() {
        degrees = (float) (Math.atan2(magnetoy, magnetox) * 180. / Math.PI);
        heading = degrees >= 0 ? degrees : 360.f + degrees;
    }

    public float getDegrees() {
        return degrees;
    }

    public float getHeading() {
        return heading;
    }

    private static final int[] COMPASS_HEADING = new int[]{
            R.string.compass_heading_n,
            R.string.compass_heading_ne,
            R.string.compass_heading_e,
            R.string.compass_heading_se,
            R.string.compass_heading_s,
            R.string.compass_heading_sw,
            R.string.compass_heading_w,
            R.string.compass_heading_nw,
            R.string.compass_heading_n,
    };

    public static int getCompassHeading(float degrees) {
        while (degrees < 0)
            degrees += 360;
        while (degrees > 360)
            degrees -= 360;
        return COMPASS_HEADING[Math.round(degrees / 45)];
    }

    protected void setCompassLabelValue(float value) {
        compassLabel.setText(String.valueOf((int) value) + " \u00b0 " + getString(getCompassHeading(value)));
    }

    protected void compassUpdateUI(final float x, final float y, final float z) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                magnetox = x * 0.3174f;
                magnetoy = y * 0.3174f;
                magnetoz = z * 0.1526f;
                calculateHeading();
                setCompassLabelValue(getHeading());
                compassRotate(compassBitmap, (int) degrees);
            }
        });
    }

    public ImageView compassRotate(Bitmap bitmap, int rotate) {

        Matrix rotateMatrix = new Matrix();

        rotateMatrix.postRotate(rotate);

        Bitmap rotateImg = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), rotateMatrix, false);

        compassImage.setImageBitmap(rotateImg);
        compassImage.getLayoutParams().height = 240;
        compassImage.getLayoutParams().width = 240;
        compassImage.requestLayout();
        return compassImage;
    }


    protected void batteryUpdateUI(final float value) {

        int batteryMax = 4050;  // 3.0 Volt
        int batteryMin = 2900;  // 2.1 Volt
        float unitBattery = 0;
        int retBattery = 0;
        int intShadowBattery = (int) value;

        unitBattery = (batteryMax - batteryMin) / 10;
        MyLog.i("unitBattery = " + String.valueOf(unitBattery));
        MyLog.i("intShadowBattery = " + String.valueOf(intShadowBattery));
        MyLog.i("batteryMin + (unitBattery * 9) = " + String.valueOf(batteryMin + (unitBattery * 9)));

        if ((intShadowBattery > (batteryMin + (unitBattery * 9))) && (intShadowBattery < 10000)) {
            retBattery = 100;
            batteryImage.setImageResource(R.drawable.ic_battery_full_white_48dp);
            batteryImage.setColorFilter(R.color.md_blue_900, PorterDuff.Mode.MULTIPLY);
            batteryLabel.setText("100 %");
        } else if ((intShadowBattery > (batteryMin + (unitBattery * 8))) && (intShadowBattery <= (batteryMin + (unitBattery * 9)))) {
            retBattery = 90;
            batteryImage.setImageResource(R.drawable.ic_battery_90_white_48dp);
            batteryImage.setColorFilter(R.color.md_blue_800, PorterDuff.Mode.MULTIPLY);
            batteryLabel.setText("90 %");
        } else if ((intShadowBattery > (batteryMin + (unitBattery * 7))) && (intShadowBattery <= (batteryMin + (unitBattery * 8)))) {
            batteryImage.setColorFilter(R.color.md_blue_700, PorterDuff.Mode.MULTIPLY);
            retBattery = 80;
            batteryImage.setImageResource(R.drawable.ic_battery_80_white_48dp);
            batteryImage.setColorFilter(R.color.md_blue_600, PorterDuff.Mode.MULTIPLY);
            batteryLabel.setText("80 %");
        } else if ((intShadowBattery > (batteryMin + (unitBattery * 6))) && (intShadowBattery <= (batteryMin + (unitBattery * 7)))) {
            retBattery = 70;
            batteryImage.setImageResource(R.drawable.ic_battery_60_white_48dp);
            batteryImage.setColorFilter(R.color.md_blue_500, PorterDuff.Mode.MULTIPLY);
            batteryLabel.setText("70 %");
        } else if ((intShadowBattery > (batteryMin + (unitBattery * 5))) && (intShadowBattery <= (batteryMin + (unitBattery * 6)))) {
            retBattery = 60;
            batteryImage.setImageResource(R.drawable.ic_battery_60_white_48dp);
            batteryImage.setColorFilter(R.color.md_blue_400, PorterDuff.Mode.MULTIPLY);
            batteryLabel.setText("60 %");
        } else if ((intShadowBattery > (batteryMin + (unitBattery * 4))) && (intShadowBattery <= (batteryMin + (unitBattery * 5)))) {
            retBattery = 50;
            batteryImage.setImageResource(R.drawable.ic_battery_50_white_48dp);
            batteryImage.setColorFilter(R.color.md_blue_300, PorterDuff.Mode.MULTIPLY);
            batteryLabel.setText("50 %");
        } else if ((intShadowBattery > (batteryMin + (unitBattery * 3))) && (intShadowBattery <= (batteryMin + (unitBattery * 4)))) {
            retBattery = 40;
            batteryImage.setImageResource(R.drawable.ic_battery_30_white_48dp);
            batteryImage.setColorFilter(R.color.md_blue_200, PorterDuff.Mode.MULTIPLY);
            batteryLabel.setText("40 %");
        } else if ((intShadowBattery > (batteryMin + (unitBattery * 2))) && (intShadowBattery <= (batteryMin + (unitBattery * 3)))) {
            retBattery = 30;
            batteryImage.setImageResource(R.drawable.ic_battery_30_white_48dp);
            batteryImage.setColorFilter(R.color.md_blue_100, PorterDuff.Mode.MULTIPLY);
            batteryLabel.setText("30 %");
        } else if ((intShadowBattery > (batteryMin + (unitBattery * 1))) && (intShadowBattery <= (batteryMin + (unitBattery * 2)))) {
            retBattery = 20;
            batteryImage.setImageResource(R.drawable.ic_battery_20_white_48dp);
            batteryImage.setColorFilter(R.color.md_blue_100, PorterDuff.Mode.MULTIPLY);
            batteryLabel.setText("20 %");
        } else if ((intShadowBattery > batteryMin) && (intShadowBattery <= (batteryMin + (unitBattery * 1)))) {
            retBattery = 10;
            batteryImage.setImageResource(R.drawable.ic_battery_alert_white_48dp);
            batteryImage.setColorFilter(R.color.md_blue_100, PorterDuff.Mode.MULTIPLY);
            batteryLabel.setText("10 %");
        } else if (intShadowBattery >= 10000) {
            retBattery = 200;
            batteryImage.setImageResource(R.drawable.ic_battery_unknown_white_48dp);
            batteryImage.setColorFilter(R.color.md_blue_grey_100, PorterDuff.Mode.MULTIPLY);
            batteryLabel.setText("N/A");
        } else {
            retBattery = 0;
            batteryImage.setImageResource(R.drawable.ic_battery_alert_white_48dp);
            batteryImage.setColorFilter(R.color.md_blue_grey_100, PorterDuff.Mode.SRC_IN);
            batteryLabel.setText("Battery Alert!");
        }
    }

    protected void ledUpdateUI(final int value) {

        if (value == 0) {  //led off
            ll_ledOff.setVisibility(View.VISIBLE);
            ll_ledOn.setVisibility(View.INVISIBLE);
        } else if (value == 1) {  //led on
            ll_ledOn.setVisibility(View.VISIBLE);
            ll_ledOff.setVisibility(View.INVISIBLE);
        }
    }

    public void mySetTextSize(TextView textView, int size) {
        if (textView != null) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size * density);
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
                    updatingDialog = new ProgressDialog(mContext, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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
                    String message = "The device's firmware update of device was successful.";
                    updateSuccessDialog = new AlertDialog.Builder(mContext, R.style.AlertDialogCustom).create();
                    updateSuccessDialog.setIcon(R.mipmap.renesas_ic_launcher);
                    updateSuccessDialog.setMessage(message);
                    updateSuccessDialog.setCancelable(false);
                    updateSuccessDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismissUpdateSuccessDialog();
                            sendEvent(AWSEVENT.E_GET_SHADOW);
                            sensor_badge_setting.setVisibility(View.INVISIBLE);
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
                    String message = "The device's firmware update of device was failed.";
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

    private static CognitoCachingCredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }

}