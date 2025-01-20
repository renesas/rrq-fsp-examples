package com.renesas.wifi.awsiot;

import android.animation.ObjectAnimator;
import android.app.Activity;
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
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.PorterDuff;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.amazonaws.services.iot.model.ListThingsRequest;
import com.amazonaws.services.iot.model.ListThingsResult;
import com.amazonaws.services.iotdata.AWSIotDataClient;
import com.amazonaws.services.iotdata.model.GetThingShadowRequest;
import com.amazonaws.services.iotdata.model.GetThingShadowResult;
import com.amazonaws.services.iotdata.model.UpdateThingShadowRequest;
import com.amazonaws.services.iotdata.model.UpdateThingShadowResult;
import com.renesas.wifi.BuildInformation;
import com.renesas.wifi.activity.MainActivity;
import com.renesas.wifi.R;
import com.renesas.wifi.awsiot.shadow.DeviceMetaData;
import com.renesas.wifi.awsiot.shadow.DeviceStatus;
import com.renesas.wifi.buildOption;
import com.renesas.wifi.util.CustomToast;
import com.renesas.wifi.util.FButton;
import com.renesas.wifi.util.MyLog;
import com.renesas.wifi.util.NetworkUtil;
import com.renesas.wifi.util.StaticDataSave;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import static android.net.wifi.SupplicantState.COMPLETED;

public class AWSIoTDeviceActivity extends Activity implements TextWatcher {

    public static Regions MY_REGION;
    SubScribeAsyncTask subScribeAsyncTask;
    NotificationManager notificationManager;

    // MQTT Topic
    private static final String APP_PUBBLISH_TOPIC = "AppControl";
    private static final String APP_SUBSCRIBE_CONNECT_TOPIC = "DeviceConnect";
    private static final String APP_SUBSCRIBE_TOPIC = "DeviceControl";

    // MQTT Message
    private static final String APP_CONNECT_MESSAGE = "connected";
    private static final String DEVICE_CONNECT_RESPONSE_MESSAGE = "yes";

    // [index_num] [name] [value_type] [cmd_type] [value]

    private static final String APP_CONTROL_DOOR_OPEN_MESSAGE = "0 app_door open";//"door open";
    private static final String DEVICE_CONTROL_OPEN_RESPONSE_MESSAGE = "opened";

    private static final String APP_CONTROL_DOOR_CLOSE_MESSAGE = "0 app_door close";//"door close";
    private static final String DEVICE_CONTROL_CLOSE_RESPONSE_MESSAGE = "closed";

    private static final String APP_CONTROL_WINDOW_OPEN_MESSAGE = "2 app_window open";//"window open";

    private static final String APP_CONTROL_WINDOW_CLOSE_MESSAGE = "2 app_window close";//"window close";

    private static final String APP_UPDATE_SHADOW_MESSAGE = "8 app_shadow update";
    private static final String DEVICE_UPDATE_SHADOW_RESPONSE_MESSAGE = "updated";

    private static final String APP_CONTROL_OTA_MESSAGE = "confirmOTA";


    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = "iot_keystore";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "default";

    public Context mContext;

    private Handler mHandler_device_connect_noResponse;
    private Runnable mRunnable_device_connect_noResponse;
    private Handler mHandler_mqtt_connect_noResponse;
    private Runnable mRunnable_mqtt_connect_noResponse;
    private Handler mHandler_update_shadow_noResponse;
    private Runnable mRunnable_update_shadow_noResponse;
    private Handler mHandler_command_noResponse;
    private Runnable mRunnable_command_noResponse;

    CustomToast customToast = null;
    Handler mHandler_toast;

    NetworkChangeReceiver mNetworkReceiver = null;

    LinearLayout ll_mqtt;

    TextView tvSubscribeTopic;
    TextView tvLastSubscribeMessage;
    TextView tvPublishTopic;
    TextView tvPublishMessage;
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

    ImageView imgConnStat;
    ImageView imgDisconnStat;
    TextView textMainKeyName;
    TextView tv_state;
    TextView tv_updatedTime;

    FButton btn_update;
    FButton btn_door;
    FButton btn_window;
    FButton btn_ota;
    TextView tv_door;
    TextView tv_window;
    TextView badge_ota;

    ProgressDialog waitDialog = null;
    ProgressDialog updateDialog = null;
    ProgressDialog progressDialog = null;
    MaterialDialog terminateDialog = null;
    MaterialDialog noInternetDialog = null;
    MaterialDialog noResponseDialog = null;
    MaterialDialog provisioningFailDialog = null;
    public ProgressDialog updatingDialog = null;
    public AlertDialog updateSuccessDialog = null;
    public AlertDialog updateFailDialog = null;


    private final static int BUTTON_CLICK_TIME = 100;
    private final static int GET_SHADOW_TIME = 2000;
    private final static int CONNECT_RECHECK_TIME = 30000;
    private final static int SUBCRIBE_TIMEOUT_TIME = 60000; //msec
    private final static int PROVISIONING_TIMEOUT_TIME = 60000; //msec

    static public int statusBarHeight;

    boolean isMqttConnected = false;
    boolean isDeviceConnected = false;
    boolean messageReceived = false;
    private static boolean isAyncTaskCompleted = true;

    WifiManager wifiManager;

    public GetShadowTask getStatusShadowTask;

    public int publishCount = 0;

    public static AWSConfig awsConfig;
    public static Activity activity;

    boolean isDoorStateUpdated = true;
    boolean isWindowStateUpdated = true;
    boolean isTemperatureUpdated = true;
    boolean isBatteryUpdated = true;
    public boolean isOtaUpdated = true;

    long pastDoorStateTimestamp = 0;
    long pastWindowStateTimestamp = 0;
    long pastTemperatureTimestamp = 0;
    long pastBatteryTimestamp = 0;
    long pastOtaTimestamp = 0;

    float density;
    float densityDpi;
    String dpiName = "";
    String screenSize = "";

    public static AWSIoTDeviceActivity instanceDevice;

    public static AWSIoTDeviceActivity getInstanceDevice() {
        return instanceDevice;
    }

    static boolean toggleOpen = false;

    private final Handler handler = new Handler();

    public Handler DeviceHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            AWSEVENT event = AWSEVENT.values()[msg.what];
            switch (event) {

                case E_DEVICE_COMMAND_NO_RESPONSE_REMOVE_CALLBACK:
                    MyLog.i("=== E_DEVICE_DOOR_CONTROL_NO_RESPONSE_REMOVE_CALLBACK ===");
                    if (noResponseDialog != null) {
                        if (noResponseDialog.isShowing()) {
                            noResponseDialog.dismiss();
                            noResponseDialog = null;
                        }
                    }
                    if (mHandler_command_noResponse != null) {
                        mHandler_command_noResponse.removeCallbacksAndMessages(null);
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

                case E_UPDATE_SHADOW_NO_RESPONSE_REMOVE_CALLBACK:
                    MyLog.i("=== E_UPDATE_SHADOW_NO_RESPONSE_REMOVE_CALLBACK ===");
                    if (updateDialog != null) {
                        if (updateDialog.isShowing()) {
                            updateDialog.dismiss();
                            updateDialog = null;
                        }
                    }
                    if (mHandler_update_shadow_noResponse != null) {
                        mHandler_update_shadow_noResponse.removeCallbacksAndMessages(null);
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

                case E_AUTO_TEST: {
                    Random r = new Random();
                    int i1 = r.nextInt(buildOption.AUTO_TEST_MAX_TIME - buildOption.AUTO_TEST_MIN_TIME) + buildOption.AUTO_TEST_MIN_TIME;
                    if (StaticDataSave.thingName != null) {

                        MyLog.i("=== E_AUTO_TEST == " + toggleOpen + " Time  = " + 1);
                        if (toggleOpen == false) {

                            toggleOpen = true;
                        } else {

                            toggleOpen = false;
                        }
                        postEvent(AWSEVENT.E_AUTO_TEST, 1000 * 60 * i1);
                    }
                }
                break;

                case E_PROVISIONING_TIMEOUT:
                    MyLog.i("=== E_PROVISIONING_TIMEOUT ===");

                    if (waitDialog != null) {
                        waitDialog.dismiss();
                        waitDialog = null;
                    }

                    removeEvent(AWSEVENT.E_PROVISIONING_TIMEOUT);
                    removeEvent(AWSEVENT.E_GET_SHADOW);
                    sendEvent(AWSEVENT.E_STOP_GET_SHADOW);

                    sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);
                    sendEvent(AWSEVENT.E_MQTT_REMOVE_CALLBACK);

                    if (provisioningFailDialog == null) {
                        provisioningFailDialog = new MaterialDialog.Builder(mContext)
                                .theme(Theme.LIGHT)
                                .title("Provisioning Fail")
                                .titleColor(mContext.getResources().getColor(R.color.blue3))
                                .titleGravity(GravityEnum.CENTER)
                                .content("Provisioning process is failed.\n" +
                                        "Factory-reset the device and perform the provisioning again.\n" +
                                        "If you click OK, App will be restarted.")
                                .positiveText("OK")
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                        dialog.dismiss();
                                        StaticDataSave.saveData = AWSIoTDeviceActivity.activity.getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                                        SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                                        editor.putBoolean(StaticDataSave.readyFlagKey, false);
                                        editor.putString(StaticDataSave.thingNameKey, null);
                                        editor.putString(StaticDataSave.userNameKey, null);
                                        editor.putString(StaticDataSave.cognitoPoolIdKey, null);
                                        editor.putString(StaticDataSave.bucketNameKey, null);
                                        editor.commit();
                                        restartApp(mContext);
                                    }
                                })
                                .build();
                        provisioningFailDialog.getWindow().setGravity(Gravity.CENTER);
                        provisioningFailDialog.show();
                        provisioningFailDialog.setCanceledOnTouchOutside(false);
                        provisioningFailDialog.setCancelable(false);
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    public void sendEvent(AWSEVENT _Event) {
        int m;
        m = _Event.ordinal();
        Message msg = DeviceHandler.obtainMessage(m);
        DeviceHandler.sendMessage(msg);
    }

    private void postEvent(AWSEVENT _Event, int _time) {
        int m;
        m = _Event.ordinal();
        Message msg = DeviceHandler.obtainMessage(m);
        DeviceHandler.sendEmptyMessageDelayed(m, _time);
    }

    public void removeEvent(AWSEVENT _Event) {
        int m;
        m = _Event.ordinal();
        Message msg = DeviceHandler.obtainMessage(m);
        DeviceHandler.removeMessages(m);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.awsiot_activity_device);

        getBuildInformation();

        getCurrentDeviceResolution();
        getCurrentDeviceDpi();
        getCurrentDeviceSize();
        MyLog.i(" ===========================");
        MyLog.i(" density : " + density);
        MyLog.i(" densityDpi : " + densityDpi);
        MyLog.i(" dpiName : " + dpiName);
        MyLog.i(" screen size : " + screenSize);
        MyLog.i(" ===========================");

        awsConfig = AWSConfig.getInstance();
        awsConfig.setConfig();

        mNetworkReceiver = new NetworkChangeReceiver();
        IntentFilter network_filter = new IntentFilter();
        network_filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, network_filter);

        mContext = this;
        instanceDevice = this;
        activity = AWSIoTDeviceActivity.this;

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

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

                    if (noInternetDialog != null) {
                        noInternetDialog.dismiss();
                        noInternetDialog = null;
                    }
                    if (waitDialog != null) {
                        waitDialog.dismiss();
                    }
                    removeEvent(AWSEVENT.E_PROVISIONING_TIMEOUT);
                }
            }
        };

        mHandler_mqtt_connect_noResponse = new Handler();
        mRunnable_mqtt_connect_noResponse = new Runnable() {
            @Override
            public void run() {
                MyLog.i("mRunnable_mqtt_connect_noResponse :: messageReceived = " + messageReceived);

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
                if (waitDialog != null) {
                    waitDialog.dismiss();
                }
                removeEvent(AWSEVENT.E_PROVISIONING_TIMEOUT);
            }
        };

        mHandler_update_shadow_noResponse = new Handler();
        mRunnable_update_shadow_noResponse = new Runnable() {
            @Override
            public void run() {
                MyLog.i("mRunnable_update_shadow_noResponse :: messageReceived = " + messageReceived);

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
                                    sendEvent(AWSEVENT.E_UPDATE_SHADOW_NO_RESPONSE_REMOVE_CALLBACK);
                                    sendEvent(AWSEVENT.E_GET_SHADOW);
                                }
                            })
                            .positiveText("OK")
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    sendEvent(AWSEVENT.E_UPDATE_SHADOW_NO_RESPONSE_REMOVE_CALLBACK);
                                    dialog.dismiss();
                                    noResponseDialog = null;
                                    updateShadowRequest();
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

        mHandler_command_noResponse = new Handler();
        mRunnable_command_noResponse = new Runnable() {
            @Override
            public void run() {

                if (progressDialog != null) {
                    progressDialog.dismiss();
                    progressDialog = null;
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
                                    sendEvent(AWSEVENT.E_DEVICE_COMMAND_NO_RESPONSE_REMOVE_CALLBACK);
                                    sendEvent(AWSEVENT.E_GET_SHADOW);
                                }
                            })
                            .positiveText("OK")
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    sendEvent(AWSEVENT.E_DEVICE_COMMAND_NO_RESPONSE_REMOVE_CALLBACK);
                                    dialog.dismiss();
                                    noResponseDialog = null;
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

        initMainResource();

        if (btn_door != null) {
            btn_door.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    try {
                        Thread.sleep(BUTTON_CLICK_TIME);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (btn_door.getText().equals("Open door")) {
                        sendCommand(APP_CONTROL_DOOR_OPEN_MESSAGE);
                        if (progressDialog == null) {
                            progressDialog = new ProgressDialog(mContext, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                            progressDialog.setMessage("The door is being opened ...");
                            progressDialog.show();

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

                            WindowManager.LayoutParams params = progressDialog.getWindow().getAttributes();
                            int dialogWindowWidth = (int) (displayWidth * 0.8f);
                            params.width = dialogWindowWidth;
                            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                            progressDialog.getWindow().setAttributes((WindowManager.LayoutParams) params);
                            progressDialog.setCanceledOnTouchOutside(false);
                            progressDialog.setCancelable(false);
                        }
                    } else if (btn_door.getText().equals("Close door")) {
                        sendCommand(APP_CONTROL_DOOR_CLOSE_MESSAGE);
                        if (progressDialog == null) {
                            progressDialog = new ProgressDialog(mContext, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                            progressDialog.setMessage("The door is being closed ...");
                            progressDialog.show();

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

                            WindowManager.LayoutParams params = progressDialog.getWindow().getAttributes();
                            int dialogWindowWidth = (int) (displayWidth * 0.8f);
                            params.width = dialogWindowWidth;
                            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                            progressDialog.getWindow().setAttributes((WindowManager.LayoutParams) params);
                            progressDialog.setCanceledOnTouchOutside(false);
                            progressDialog.setCancelable(false);
                        }
                    } else if (btn_door.getText().equals("Unknown")) {
                        mHandler_toast.post(new ToastRunnable("The state of the door is unknown"));
                    }
                }
            });
        }

        if (btn_window != null) {
            btn_window.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    try {
                        Thread.sleep(BUTTON_CLICK_TIME);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (btn_window.getText().equals("Open window")) {
                        sendCommand(APP_CONTROL_WINDOW_OPEN_MESSAGE);
                        if (progressDialog == null) {
                            progressDialog = new ProgressDialog(mContext, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                            progressDialog.setMessage("The window is being opened ...");
                            progressDialog.show();

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

                            WindowManager.LayoutParams params = progressDialog.getWindow().getAttributes();
                            int dialogWindowWidth = (int) (displayWidth * 0.8f);
                            params.width = dialogWindowWidth;
                            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                            progressDialog.getWindow().setAttributes((WindowManager.LayoutParams) params);
                            progressDialog.setCanceledOnTouchOutside(false);
                            progressDialog.setCancelable(false);
                        }
                    } else if (btn_window.getText().equals("Close window")) {
                        sendCommand(APP_CONTROL_WINDOW_CLOSE_MESSAGE);
                        if (progressDialog == null) {
                            progressDialog = new ProgressDialog(mContext, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                            progressDialog.setMessage("The window is being closed ...");
                            progressDialog.show();

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

                            WindowManager.LayoutParams params = progressDialog.getWindow().getAttributes();
                            int dialogWindowWidth = (int) (displayWidth * 0.8f);
                            params.width = dialogWindowWidth;
                            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                            progressDialog.getWindow().setAttributes((WindowManager.LayoutParams) params);
                            progressDialog.setCanceledOnTouchOutside(false);
                            progressDialog.setCancelable(false);
                        }
                    } else {
                        mHandler_toast.post(new ToastRunnable("The state of the window is unknown"));
                    }
                }
            });
        }

        if (btn_update != null) {
            btn_update.setOnClickListener(new View.OnClickListener() {
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
                            updateShadowRequest();
                        } else if (isMqttConnected == false) {
                            mHandler_toast.post(new ToastRunnable("The network connection is uncertain"));
                        } else if (isDeviceConnected == false) {
                            mHandler_toast.post(new ToastRunnable("It is not connected to the device"));
                        }
                    }
                }
            });
        }
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
                    mContext, // context
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
                                    removeEvent(AWSEVENT.E_PROVISIONING_TIMEOUT);
                                }

                                subScribeAsyncTask = new SubScribeAsyncTask();
                                subScribeAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                                imgConnStat.setVisibility(View.VISIBLE);
                                imgConnStat.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                                imgConnStat.clearAnimation();
                                imgDisconnStat.clearAnimation();
                                imgDisconnStat.setVisibility(View.INVISIBLE);
                                sendEvent(AWSEVENT.E_MQTT_REMOVE_CALLBACK);
                                if (isDeviceConnected == false) {
                                    sendEvent(AWSEVENT.E_DEVICE_CONNECT_CHECK);
                                }
                                sendEvent(AWSEVENT.E_GET_SHADOW);

                            } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                MyLog.i("AWSIotMqttClientStatus :: Reconnecting");
                                if (throwable != null) {
                                    MyLog.e("Connection error." + throwable);
                                }
                                isMqttConnected = false;
                                Animation startAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink_animation);

                                removeEvent(AWSEVENT.E_GET_SHADOW);
                                sendEvent(AWSEVENT.E_STOP_GET_SHADOW);

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
                                sendEvent(AWSEVENT.E_STOP_GET_SHADOW);

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
                                sendEvent(AWSEVENT.E_STOP_GET_SHADOW);

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
            sendEvent(AWSEVENT.E_STOP_GET_SHADOW);

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

    public void getThings() {
        MyLog.i(">> getThings");
        GetThingsList getThingsList = new GetThingsList();
        //getThingsList.execute();
        getThingsList.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private class GetThingsList extends AsyncTask<Void, Void, AsyncTaskResult<String>> {
        @Override
        protected AsyncTaskResult<String> doInBackground(Void... voids) {
            try {
                ListThingsRequest thingList = new ListThingsRequest();
                thingList.withAttributeName("EVB-DOORLOCK");
                ListThingsResult thingsResult = mIotAndroidClient.listThings(thingList);
                return new AsyncTaskResult<String>(thingsResult.toString());
            } catch (Exception e) {
                MyLog.e("getThingTask error >> " + e);
                return new AsyncTaskResult<String>(e);
            }
        }

        @Override
        protected void onPostExecute(AsyncTaskResult<String> result) {
            if (result.getError() == null) {
                MyLog.i(">> thingsResult : " + result.getResult());
            } else {
                MyLog.e("getThingTask error >> " + result.getError());
            }
        }
    }

    private void Device_connectCheck() {
        MyLog.i("=== Device_connectCheck() ===");

        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);
        final String pubTopic = StaticDataSave.thingName + "/" + APP_PUBBLISH_TOPIC;
        final String msg = APP_CONNECT_MESSAGE;
        tvPublishTopic.setText(pubTopic);
        tvPublishMessage.setText(msg);
        messageReceived = false;

        sendEvent(AWSEVENT.E_STOP_GET_SHADOW);

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

    private void updateShadowRequest() {

        if (updateDialog == null) {
            updateDialog = new ProgressDialog(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
            updateDialog.setMessage("Updating values from device ...");

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
        final String topic = StaticDataSave.thingName + "/" + APP_PUBBLISH_TOPIC;
        final String msg = APP_UPDATE_SHADOW_MESSAGE;
        tvPublishTopic.setText(topic);
        tvPublishMessage.setText(msg);
        messageReceived = false;

        sendEvent(AWSEVENT.E_STOP_GET_SHADOW);

        try {
            mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
            MyLog.i("=================================");
            MyLog.i("Message published:");
            MyLog.i("   pubTopic: " + topic);
            MyLog.i(" Message: " + msg);
            MyLog.i("=================================");

            mHandler_update_shadow_noResponse.postDelayed(mRunnable_update_shadow_noResponse, SUBCRIBE_TIMEOUT_TIME);

        } catch (Exception e) {
            MyLog.e("Publish error. >>> " + e);
        }
    }

    private void shadowUpdated(String statusState) {

        Gson gson = new Gson();
        DeviceStatus ds = gson.fromJson(statusState, DeviceStatus.class);

        Gson gson1 = new Gson();
        DeviceMetaData dm = gson1.fromJson(statusState, DeviceMetaData.class);

        if (dm.metadata.reported.doorStat != null) {
            Long curTimestamp = dm.metadata.reported.doorStat.timestamp;
            if (curTimestamp != null) {
                Date dt = new Date(curTimestamp * 1000);
                SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
                tv_updatedTime.setText(sfd.format(dt));

                if (pastDoorStateTimestamp != curTimestamp) {
                    isDoorStateUpdated = true;
                } else {
                    isDoorStateUpdated = false;
                }
            }

            MyLog.i("isDoorStateUpdated = " + String.valueOf(isDoorStateUpdated));

            StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
            StaticDataSave.doorStateFlag = StaticDataSave.saveData.getString(StaticDataSave.doorStateFlagKey, null);

            if (StaticDataSave.doorStateFlag == null) {

                if (ds.state.reported.doorStat != null) {
                    if (ds.state.reported.doorStat.equals("opened")) {  //opened

                        if (btn_door != null) {
                            btn_door.setText("Close door");
                        }
                        if (tv_door != null) {
                            tv_door.setText("The door is opened");
                            tv_door.setBackgroundColor(mContext.getResources().getColor(R.color.md_red_500));
                        }

                        StaticDataSave.doorStateFlag = "true";
                        SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                        editor.putString(StaticDataSave.doorStateFlagKey, StaticDataSave.doorStateFlag);
                        editor.commit();

                    } else if (ds.state.reported.doorStat.equals("closed")) {  //closed
                        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                        StaticDataSave.doorStateFlag = StaticDataSave.saveData.getString(StaticDataSave.doorStateFlagKey, null);
                        if (btn_door != null) {
                            btn_door.setText("Open door");
                        }
                        if (tv_door != null) {
                            tv_door.setText("The door is closed");
                            tv_door.setBackgroundColor(mContext.getResources().getColor(R.color.md_green_500));
                        }
                        StaticDataSave.doorStateFlag = "false";
                        SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                        editor.putString(StaticDataSave.doorStateFlagKey, StaticDataSave.doorStateFlag);
                        editor.commit();
                    }
                }
            } else {
                if (isDoorStateUpdated) {

                    if (ds.state.reported.doorStat != null) {
                        MyLog.i(String.format("doorState : %s", ds.state.reported.doorStat));
                    }

                    MyLog.i(String.format("battery :  %f", ds.state.reported.battery));
                    MyLog.i(String.format("temp :  %f", ds.state.reported.temperature));


                    if (ds.state.reported.doorStat != null) {
                        if (ds.state.reported.doorStat.equals("opened")) {  //opened
                            StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                            StaticDataSave.doorStateFlag = StaticDataSave.saveData.getString(StaticDataSave.doorStateFlagKey, null);
                            if (StaticDataSave.doorStateFlag != null) {
                                if (StaticDataSave.doorStateFlag.equals("false")) {
                                    doorLockCtl(false);
                                }
                            }
                            StaticDataSave.doorStateFlag = "true";
                            SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                            editor.putString(StaticDataSave.doorStateFlagKey, StaticDataSave.doorStateFlag);
                            editor.commit();

                        } else if (ds.state.reported.doorStat.equals("closed")) {  //closed
                            StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                            StaticDataSave.doorStateFlag = StaticDataSave.saveData.getString(StaticDataSave.doorStateFlagKey, null);
                            if (StaticDataSave.doorStateFlag != null) {
                                if (StaticDataSave.doorStateFlag.equals("true")) {
                                    doorLockCtl(true);
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
        }

        if (dm.metadata.reported.windowStat != null) {
            Long curTimestamp = dm.metadata.reported.windowStat.timestamp;
            if (curTimestamp != null) {
                Date dt = new Date(curTimestamp * 1000);
                SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
                tv_updatedTime.setText(sfd.format(dt));

                if (pastWindowStateTimestamp != curTimestamp) {
                    isWindowStateUpdated = true;
                } else {
                    isWindowStateUpdated = false;
                }
            }

            MyLog.i("isWindowStateUpdated = " + String.valueOf(isWindowStateUpdated));

            StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
            StaticDataSave.windowStateFlag = StaticDataSave.saveData.getString(StaticDataSave.windowStateFlagKey, null);

            if (StaticDataSave.windowStateFlag == null) {
                if (ds.state.reported.windowStat != null) {
                    if (ds.state.reported.windowStat.equals("opened")) {  //opened
                        windowLockCtl(false);
                        StaticDataSave.windowStateFlag = "true";
                        SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                        editor.putString(StaticDataSave.windowStateFlagKey, StaticDataSave.windowStateFlag);
                        editor.commit();
                    } else if (ds.state.reported.windowStat.equals("closed")) {  //closed
                        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                        StaticDataSave.windowStateFlag = StaticDataSave.saveData.getString(StaticDataSave.windowStateFlagKey, null);
                        windowLockCtl(true);
                        StaticDataSave.windowStateFlag = "false";
                        SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                        editor.putString(StaticDataSave.windowStateFlagKey, StaticDataSave.windowStateFlag);
                        editor.commit();
                    }
                }
            } else {
                if (isWindowStateUpdated) {

                    if (ds.state.reported.windowStat != null) {
                        MyLog.i(String.format("windowState : %s", ds.state.reported.windowStat));
                    }

                    MyLog.i(String.format("battery :  %f", ds.state.reported.battery));
                    MyLog.i(String.format("temp :  %f", ds.state.reported.temperature));


                    if (ds.state.reported.windowStat != null) {
                        if (ds.state.reported.windowStat.equals("opened")) {  //opened

                            if (StaticDataSave.windowStateFlag != null) {
                                if (StaticDataSave.windowStateFlag.equals("false")) {
                                    if (btn_window != null) {
                                        btn_window.setText("Close window");
                                    }
                                    if (tv_window != null) {
                                        tv_window.setText("The window is opened");
                                        tv_window.setBackgroundColor(mContext.getResources().getColor(R.color.md_red_500));
                                    }
                                }
                            }
                            StaticDataSave.windowStateFlag = "true";
                            SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                            editor.putString(StaticDataSave.windowStateFlagKey, StaticDataSave.windowStateFlag);
                            editor.commit();
                        } else if (ds.state.reported.windowStat.equals("closed")) {  //closed
                            StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                            StaticDataSave.windowStateFlag = StaticDataSave.saveData.getString(StaticDataSave.windowStateFlagKey, null);
                            if (StaticDataSave.windowStateFlag != null) {
                                if (StaticDataSave.windowStateFlag.equals("true")) {
                                    if (btn_window != null) {
                                        btn_window.setText("Open window");
                                    }
                                    if (tv_window != null) {
                                        tv_window.setText("The window is closed");
                                        tv_window.setBackgroundColor(mContext.getResources().getColor(R.color.md_green_500));
                                    }
                                }
                            }
                            StaticDataSave.windowStateFlag = "false";
                            SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                            editor.putString(StaticDataSave.windowStateFlagKey, StaticDataSave.windowStateFlag);
                            editor.commit();
                        }
                    }
                }

                pastWindowStateTimestamp = curTimestamp;
                isWindowStateUpdated = false;
            }
        }

        if (dm.metadata.reported.OTAupdate != null) {
            Long curOtaTimestamp = dm.metadata.reported.OTAupdate.timestamp;
            if (curOtaTimestamp != null) {
                Date dt4 = new Date(curOtaTimestamp * 1000);
                SimpleDateFormat sfd4 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
                tv_updatedTime.setText(sfd4.format(dt4));

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
                            badge_ota.setVisibility(View.INVISIBLE);
                        }
                    });

                    if (ds.state.reported.OTAresult != null) {
                        long curOtaResultTimestamp = dm.metadata.reported.OTAresult.timestamp;
                        Date dt5 = new Date(curOtaTimestamp * 1000);
                        SimpleDateFormat sfd5 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
                        tv_updatedTime.setText(sfd5.format(dt5));
                        if (ds.state.reported.OTAresult.equals("OTA_OK")) {
                            if (updatingDialog != null && updatingDialog.isShowing()) {
                                showUpdateSuccessDialog();
                            }
                        } else if (ds.state.reported.OTAresult.equals("OTA_UNKNOWN")) {
                            if (updatingDialog != null && updatingDialog.isShowing()) {
                                dismissUpdatingDialog();
                            }
                        }
                    }

                } else if (ds.state.reported.OTAupdate == 1) {  // exist update
                    StaticDataSave.existOTAupdateFlag = true;
                    StaticDataSave.OTAupdateProgressFlag = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            badge_ota.setVisibility(View.VISIBLE);
                        }
                    });

                    if (ds.state.reported.OTAresult.equals("OTA_NG")) {
                        if (updatingDialog != null && updatingDialog.isShowing()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showUpdateFailDialog();
                                }
                            });
                        }
                    }

                } else if (ds.state.reported.OTAupdate == 2) {  // update progressing
                    StaticDataSave.existOTAupdateFlag = false;
                    StaticDataSave.OTAupdateProgressFlag = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            badge_ota.setVisibility(View.INVISIBLE);
                            showUpdatingDialog();
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

                    try {
                        JSONObject jsonObject = new JSONObject(result.getResult());
                        String state = jsonObject.getString("state");
                        String state_split = "";
                        Gson gson1 = new GsonBuilder().setPrettyPrinting().create();
                        JsonParser jp1 = new JsonParser();
                        JsonElement je1 = jp1.parse(state);
                        state_split = gson1.toJson(je1);
                        if (tv_state != null) {
                            tv_state.setText(state_split);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }


                }
            } else {
                MyLog.e("E / getShadowTask : " + result.getError());
            }
            isAyncTaskCompleted = true;
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
                MyLog.e("Error in Update Shadow : " + result.getError());
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        MyLog.i("=== onResume() ===");

        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);

        MyLog.i("StaticDataSave.thingName = " + StaticDataSave.thingName);

        if (StaticDataSave.thingName == null) {
            textMainKeyName.setText("Unknown");
            btn_door.setText("Unknown");
            tv_door.setText("Unknown");
            btn_window.setText("Unknown");
            tv_window.setText("Unknown");
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
                    waitDialog.setCancelable(false);
                }
                postEvent(AWSEVENT.E_PROVISIONING_TIMEOUT, PROVISIONING_TIMEOUT_TIME);

            } else {
                if (isOnline() && clientKeyStore != null && isMqttConnected == false) {
                    initMqtt();

                } else {
                    sendEvent(AWSEVENT.E_GET_SHADOW);
                }

                StaticDataSave.existOTAupdateFlag = StaticDataSave.saveData.getBoolean(StaticDataSave.existOTAupdateFlagKey, false);
                MyLog.i("StaticDataSave.existOTAupdateFlag = " + StaticDataSave.existOTAupdateFlag);
                if (btn_ota != null) {
                    if (StaticDataSave.existOTAupdateFlag == true) {
                        badge_ota.setVisibility(View.VISIBLE);
                    } else {
                        badge_ota.setVisibility(View.INVISIBLE);
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
        sendEvent(AWSEVENT.E_STOP_GET_SHADOW);

        sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);
        sendEvent(AWSEVENT.E_MQTT_REMOVE_CALLBACK);

        try {
            if (subScribeAsyncTask != null) {
                if (subScribeAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                    subScribeAsyncTask.cancel(true);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
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
        DeviceHandler.removeCallbacksAndMessages(null);

        try {
            if (getStatusShadowTask.getStatus() == AsyncTask.Status.RUNNING) {
                getStatusShadowTask.cancel(true);
            } else {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (waitDialog != null) {
            waitDialog.dismiss();
            waitDialog = null;
        }

        if (updateDialog != null) {
            updateDialog.dismiss();
            updateDialog = null;
        }

        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        if (terminateDialog != null) {
            terminateDialog.dismiss();
            terminateDialog = null;
        }

        removeEvent(AWSEVENT.E_GET_SHADOW);
        sendEvent(AWSEVENT.E_STOP_GET_SHADOW);

        sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);
        sendEvent(AWSEVENT.E_UPDATE_SHADOW_NO_RESPONSE_REMOVE_CALLBACK);
        sendEvent(AWSEVENT.E_DEVICE_COMMAND_NO_RESPONSE_REMOVE_CALLBACK);

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
                            if (StaticDataSave.thingName != null && StaticDataSave.cognitoPoolId != null && MY_REGION != null) {
                                terminateDialog = null;
                                removeEvent(AWSEVENT.E_GET_SHADOW);
                                sendEvent(AWSEVENT.E_STOP_GET_SHADOW);
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

        ll_mqtt = (LinearLayout) findViewById(R.id.ll_mqtt);
        if (ll_mqtt != null) {
            if (buildOption.DEBUG_MODE == true) {
                ll_mqtt.setVisibility(View.VISIBLE);
            } else {
                ll_mqtt.setVisibility(View.GONE);
            }
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

        tv_state = findViewById(R.id.tv_state);
        tv_state.setMovementMethod(new ScrollingMovementMethod());

        btn_update = findViewById(R.id.btn_update);

        tv_updatedTime = findViewById(R.id.tv_updatedTime);

        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.doorStateFlag = StaticDataSave.saveData.getString(StaticDataSave.doorStateFlagKey, null);

        btn_door = findViewById(R.id.btn_door);
        tv_door = findViewById(R.id.tv_door);

        if (StaticDataSave.doorStateFlag != null) {
            MyLog.i("StaticDataSave.doorStateFlag = " + StaticDataSave.doorStateFlag);
            if (StaticDataSave.doorStateFlag.equals("true")) {
                if (btn_door != null) {
                    btn_door.setText("Close door");
                }
                if (tv_door != null) {
                    tv_door.setText("The door is opened");
                    tv_door.setBackgroundColor(mContext.getResources().getColor(R.color.md_red_500));
                }
            } else {
                if (btn_door != null) {
                    btn_door.setText("Open door");
                }
                if (tv_door != null) {
                    tv_door.setText("The door is closed");
                    tv_door.setBackgroundColor(mContext.getResources().getColor(R.color.md_green_500));
                }
            }
        }

        StaticDataSave.windowStateFlag = StaticDataSave.saveData.getString(StaticDataSave.windowStateFlagKey, null);

        btn_window = findViewById(R.id.btn_window);
        tv_window = findViewById(R.id.tv_window);


        if (StaticDataSave.windowStateFlag != null) {
            MyLog.i("StaticDataSave.windowStateFlag = " + StaticDataSave.windowStateFlag);
            if (StaticDataSave.windowStateFlag.equals("true")) {
                if (btn_window != null) {
                    btn_window.setText("Close window");
                }
                if (tv_window != null) {
                    tv_window.setText("The window is opened");
                    tv_window.setBackgroundColor(mContext.getResources().getColor(R.color.md_red_500));
                }
            } else {
                if (btn_window != null) {
                    btn_window.setText("Open window");
                }
                if (tv_window != null) {
                    tv_window.setText("The window is closed");
                    tv_window.setBackgroundColor(mContext.getResources().getColor(R.color.md_green_500));
                }
            }
        }

        btn_ota = findViewById(R.id.btn_ota);
        btn_ota.setFButtonPadding(20, 0, 20, 0);

        btn_ota.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyLog.i("=== update switch ===");

                MyLog.i("isDeviceConnected = " + isDeviceConnected);
                if (isDeviceConnected) {

                    MyLog.i("StaticDataSave.existOTAupdateFlag = " + StaticDataSave.existOTAupdateFlag);
                    if (StaticDataSave.existOTAupdateFlag == true) {

                        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);
                        final String pubTopic = StaticDataSave.thingName + "/" + APP_PUBBLISH_TOPIC;
                        final String msg = APP_CONTROL_OTA_MESSAGE;

                        sendEvent(AWSEVENT.E_STOP_GET_SHADOW);

                        try {
                            mqttManager.publishString(msg, pubTopic, AWSIotMqttQos.QOS0);
                            MyLog.i("=================================");
                            MyLog.i("Message published:");
                            MyLog.i("   Topic: " + pubTopic);
                            MyLog.i(" Message: " + msg);
                            MyLog.i("=================================");

                            MyLog.i("StaticDataSave.OTAupdateProgressFlag = " + StaticDataSave.OTAupdateProgressFlag);
                            sendEvent(AWSEVENT.E_GET_SHADOW);

                        } catch (Exception e) {
                            MyLog.e("Publish error. >>> " + e);
                        }
                    } else {
                        mHandler_toast.post(new ToastRunnable(getResources().getString(R.string.nothing_update)));
                    }
                } else {
                    mHandler_toast.post(new ToastRunnable(getResources().getString(R.string.connect_fail)));
                }
            }
        });

        badge_ota = findViewById(R.id.badge_ota);
        StaticDataSave.existOTAupdateFlag = StaticDataSave.saveData.getBoolean(StaticDataSave.existOTAupdateFlagKey, false);
        MyLog.i("StaticDataSave.existOTAupdateFlag = " + StaticDataSave.existOTAupdateFlag);
        if (btn_ota != null) {
            if (StaticDataSave.existOTAupdateFlag == true) {
                badge_ota.setVisibility(View.VISIBLE);
            } else {
                badge_ota.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void doorLockCtl(boolean lock) {    // open : false, lock : true

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        if (lock) {
            if (btn_door != null) {
                btn_door.setText("Open door");
                ObjectAnimator animator = ObjectAnimator.ofFloat(btn_door, "rotationY", 360, 0);
                animator.setDuration(1000);
                animator.start();
            }
            if (tv_door != null) {
                tv_door.setText("The door is closed");
                tv_door.setBackgroundColor(mContext.getResources().getColor(R.color.md_green_500));
            }
        } else {
            if (btn_door != null) {
                btn_door.setText("Close door");
                ObjectAnimator animator = ObjectAnimator.ofFloat(btn_door, "rotationY", 360, 0);
                animator.setDuration(1000);
                animator.start();
            }
            if (tv_door != null) {
                tv_door.setText("The door is opened");
                tv_door.setBackgroundColor(mContext.getResources().getColor(R.color.md_red_500));
            }
        }
    }

    private void windowLockCtl(boolean lock) {    // open : false, lock : true

        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        if (lock) {
            if (btn_window != null) {
                btn_window.setText("Open window");
                ObjectAnimator animator = ObjectAnimator.ofFloat(btn_window, "rotationY", 360, 0);
                animator.setDuration(1000);
                animator.start();
            }
            if (tv_window != null) {
                tv_window.setText("The window is closed");
                tv_window.setBackgroundColor(mContext.getResources().getColor(R.color.md_green_500));
            }
        } else {
            if (btn_window != null) {
                btn_window.setText("Close window");
                ObjectAnimator animator = ObjectAnimator.ofFloat(btn_window, "rotationY", 360, 0);
                animator.setDuration(1000);
                animator.start();
            }
            if (tv_window != null) {
                tv_window.setText("The window is opened");
                tv_window.setBackgroundColor(mContext.getResources().getColor(R.color.md_red_500));
            }
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {

    }

    private void sendCommand(String command) {

        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);
        final String topic = StaticDataSave.thingName + "/" + APP_PUBBLISH_TOPIC;
        final String msg = command;
        tvPublishTopic.setText(topic);
        tvPublishMessage.setText(msg);
        messageReceived = false;

        sendEvent(AWSEVENT.E_STOP_GET_SHADOW);

        try {
            mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
            MyLog.i("=================================");
            MyLog.i("Message published:");
            MyLog.i("   pubTopic: " + topic);
            MyLog.i(" Message: " + msg);
            MyLog.i("=================================");

            mHandler_command_noResponse.postDelayed(mRunnable_command_noResponse, SUBCRIBE_TIMEOUT_TIME);

        } catch (Exception e) {
            MyLog.e("Publish error. >>> " + e);
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
                    } else {

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

                        if (noInternetDialog != null) {
                            noInternetDialog.dismiss();
                            noInternetDialog = null;
                        }
                    }
                }
            }
        }
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

                                                if (subTopic.contains(APP_SUBSCRIBE_CONNECT_TOPIC) || subTopic.contains(APP_SUBSCRIBE_TOPIC)) {
                                                    tvSubscribeTopic.setText(subTopic);
                                                    tvLastSubscribeMessage.setText(message);
                                                }

                                                if (message.equals(DEVICE_CONNECT_RESPONSE_MESSAGE)) {
                                                    messageReceived = true;
                                                    isDeviceConnected = true;
                                                    removeEvent(AWSEVENT.E_DEVICE_CONNECT_CHECK);

                                                    MyLog.i("isMqttConnected = " + isMqttConnected);

                                                    if (waitDialog != null) {
                                                        waitDialog.dismiss();
                                                        waitDialog = null;
                                                    }

                                                    if (updateDialog != null) {
                                                        updateDialog.dismiss();
                                                        updateDialog = null;
                                                    }

                                                    if (progressDialog != null) {
                                                        progressDialog.dismiss();
                                                        progressDialog = null;
                                                    }
                                                    removeEvent(AWSEVENT.E_PROVISIONING_TIMEOUT);

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

                                                    if (noInternetDialog != null) {
                                                        noInternetDialog.dismiss();
                                                        noInternetDialog = null;
                                                    }

                                                } else if (message.contains("mcu_door")) {
                                                    messageReceived = true;
                                                    isDeviceConnected = true;
                                                    removeEvent(AWSEVENT.E_DEVICE_CONNECT_CHECK);
                                                    removeEvent(AWSEVENT.E_PROVISIONING_TIMEOUT);
                                                    sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);

                                                    if (waitDialog != null) {
                                                        waitDialog.dismiss();
                                                        waitDialog = null;
                                                    }

                                                    if (updateDialog != null) {
                                                        updateDialog.dismiss();
                                                        updateDialog = null;
                                                    }

                                                    if (progressDialog != null) {
                                                        progressDialog.dismiss();
                                                        progressDialog = null;
                                                    }
                                                    MyLog.i("sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK) 5");
                                                    sendEvent(AWSEVENT.E_DEVICE_COMMAND_NO_RESPONSE_REMOVE_CALLBACK);
                                                    sendEvent(AWSEVENT.E_GET_SHADOW);
                                                    if (iv_deviceNetworkState != null) {
                                                        iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_green);
                                                        iv_deviceNetworkState.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                                                        iv_deviceNetworkState.clearAnimation();
                                                    }
                                                    if (message.contains(DEVICE_CONTROL_OPEN_RESPONSE_MESSAGE)) {
                                                        MyLog.i("doorLockCtl(false)");
                                                    } else if (message.contains(DEVICE_CONTROL_CLOSE_RESPONSE_MESSAGE)) {
                                                        MyLog.i("doorLockCtl(true)");
                                                    }

                                                } else if (message.contains("mcu_window")) {
                                                    messageReceived = true;
                                                    isDeviceConnected = true;
                                                    removeEvent(AWSEVENT.E_DEVICE_CONNECT_CHECK);
                                                    removeEvent(AWSEVENT.E_PROVISIONING_TIMEOUT);
                                                    sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);
                                                    sendEvent(AWSEVENT.E_GET_SHADOW);

                                                    if (waitDialog != null) {
                                                        waitDialog.dismiss();
                                                        waitDialog = null;
                                                    }

                                                    if (updateDialog != null) {
                                                        updateDialog.dismiss();
                                                        updateDialog = null;
                                                    }

                                                    if (progressDialog != null) {
                                                        progressDialog.dismiss();
                                                        progressDialog = null;
                                                    }

                                                    MyLog.i("sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK) 5");
                                                    sendEvent(AWSEVENT.E_DEVICE_COMMAND_NO_RESPONSE_REMOVE_CALLBACK);
                                                    if (iv_deviceNetworkState != null) {
                                                        iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_green);
                                                        iv_deviceNetworkState.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                                                        iv_deviceNetworkState.clearAnimation();
                                                    }
                                                    if (message.contains(DEVICE_CONTROL_OPEN_RESPONSE_MESSAGE)) {
                                                        MyLog.i("windowLockCtl(false)");
                                                    } else if (message.contains(DEVICE_CONTROL_CLOSE_RESPONSE_MESSAGE)) {
                                                        MyLog.i("windowLockCtl(true)");
                                                    }
                                                } else if (message.contains(DEVICE_UPDATE_SHADOW_RESPONSE_MESSAGE)) {
                                                    messageReceived = true;
                                                    isDeviceConnected = true;

                                                    if (waitDialog != null) {
                                                        waitDialog.dismiss();
                                                        waitDialog = null;
                                                    }

                                                    if (updateDialog != null) {
                                                        updateDialog.dismiss();
                                                        updateDialog = null;
                                                    }

                                                    if (progressDialog != null) {
                                                        progressDialog.dismiss();
                                                        progressDialog = null;
                                                    }
                                                    sendEvent(AWSEVENT.E_UPDATE_SHADOW_NO_RESPONSE_REMOVE_CALLBACK);
                                                    sendEvent(AWSEVENT.E_GET_SHADOW);
                                                    if (iv_deviceNetworkState != null) {
                                                        iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_green);
                                                        iv_deviceNetworkState.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                                                        iv_deviceNetworkState.clearAnimation();
                                                    }
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
                    String message = "The device's firmware update was successful.";
                    updateSuccessDialog = new AlertDialog.Builder(mContext, R.style.AlertDialogCustom).create();
                    updateSuccessDialog.setIcon(R.mipmap.renesas_ic_launcher);
                    updateSuccessDialog.setMessage(message);
                    updateSuccessDialog.setCancelable(false);
                    updateSuccessDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismissUpdateSuccessDialog();
                            sendEvent(AWSEVENT.E_GET_SHADOW);
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

    private void getCurrentDeviceResolution() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        density = displayMetrics.density;
        densityDpi = displayMetrics.densityDpi;
    }

    private void getCurrentDeviceDpi() {

        if (densityDpi <= 120) {
            dpiName = "ldpi";
        } else if (120 < densityDpi && densityDpi <= 160) {
            dpiName = "mdpi";
        } else if (160 < densityDpi && densityDpi <= 240) {
            dpiName = "hdpi";
        } else if (240 < densityDpi && densityDpi <= 320) {
            dpiName = "xhdpi";
        } else if (320 < densityDpi && densityDpi <= 480) {
            dpiName = "xxhdpi";
        } else if (480 < densityDpi && densityDpi <= 640) {
            dpiName = "xxxhdpi";
        }
    }

    private void getCurrentDeviceSize() {

        boolean isSmallScreen = (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_SMALL;
        boolean isNormalScreen = (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_NORMAL;
        boolean isLargeScreen = (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE;
        boolean isXLargeScreen = (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE;

        if (isSmallScreen) {
            screenSize = "small";
        } else if (isNormalScreen) {
            screenSize = "normal";
        } else if (isLargeScreen) {
            screenSize = "large";
        } else if (isXLargeScreen) {
            screenSize = "xlarge";
        }
    }

    private static CognitoCachingCredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }

}