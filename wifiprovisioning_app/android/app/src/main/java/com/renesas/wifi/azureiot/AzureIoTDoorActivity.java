package com.renesas.wifi.azureiot;

import static android.net.wifi.SupplicantState.COMPLETED;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.text.TextUtils;
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
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.diasemi.azure.sdk.iot.service.DeliveryAcknowledgement;
import com.diasemi.azure.sdk.iot.service.DeviceConnectionState;
import com.diasemi.azure.sdk.iot.service.FeedbackBatch;
import com.diasemi.azure.sdk.iot.service.FeedbackReceiver;
import com.diasemi.azure.sdk.iot.service.IotHubServiceClientProtocol;
import com.diasemi.azure.sdk.iot.service.ServiceClient;
import com.diasemi.azure.sdk.iot.service.devicetwin.DeviceMethod;
import com.diasemi.azure.sdk.iot.service.devicetwin.DeviceTwin;
import com.diasemi.azure.sdk.iot.service.devicetwin.DeviceTwinDevice;
import com.diasemi.azure.sdk.iot.service.devicetwin.MethodResult;
import com.diasemi.azure.sdk.iot.service.devicetwin.Pair;
import com.diasemi.azure.sdk.iot.service.devicetwin.Query;
import com.diasemi.azure.sdk.iot.service.devicetwin.SqlQuery;
import com.diasemi.azure.sdk.iot.service.exceptions.IotHubException;
import com.renesas.wifi.BuildInformation;
import com.renesas.wifi.R;
import com.renesas.wifi.activity.MainActivity;
import com.renesas.wifi.buildOption;
import com.renesas.wifi.util.CustomToast;
import com.renesas.wifi.util.FButton;
import com.renesas.wifi.util.MyLog;
import com.renesas.wifi.util.NetworkUtil;
import com.renesas.wifi.util.StaticDataSave;
import com.google.gson.Gson;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AzureIoTDoorActivity extends Activity {

    private static final IotHubServiceClientProtocol protocol = IotHubServiceClientProtocol.AMQPS;

    //[[for REST
    GetDeviceTwinTask getDeviceTwinTask;

    private String iothubName = "";
    private String sharedAccessKey = "";

    private String pastTemperatureDate = "yyyy-MM-dd aaa hh:mm:ss";
    private String pastBatteryDate = "yyyy-MM-dd aaa hh:mm:ss";
    private String pastDoorStateDate = "yyyy-MM-dd aaa hh:mm:ss";
    private String pastOTAupdateDate = "yyyy-MM-dd aaa hh:mm:ss";
    private String pastOTAresultDate = "yyyy-MM-dd aaa hh:mm:ss";
    //]]

    // Direct method name
    private static final String METHOD_NAME_APPCONTROL  = "AppControl";

    // Direct method payload
    private static final String APP_CONNECT_MESSAGE = "connected";
    private static final String DEVICE_CONNECT_RESPONSE_MESSAGE = "yes";

    private static final String APP_CONTROL_OPEN_MESSAGE = "doorOpen";
    private static final String DEVICE_CONTROL_OPEN_RESPONSE_MESSAGE = "opened";
    private static final String ATCMD_APP_CONTROL_OPEN_MESSAGE = "0 app_door open";

    private static final String APP_CONTROL_CLOSE_MESSAGE = "doorClose";
    private static final String ATCMD_APP_CONTROL_CLOSE_MESSAGE = "0 app_door close";
    private static final String DEVICE_CONTROL_CLOSE_RESPONSE_MESSAGE = "closed";

    private static final String APP_UPDATE_SENSOR_MESSAGE = "updateSensor";
    private static final String ATCMD_APP_UPDATE_SENSOR_MESSAGE = "8 app_shadow update";
    private static final String DEVICE_UPDATE_SENSOR_RESPONSE_MESSAGE = "updated";

    private static final String APP_CONTROL_OTA_MESSAGE = "confirmOTA";

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

    // for AT-CMD
    private Handler mHandler_command_noResponse;
    private Runnable mRunnable_command_noResponse;
    //

    private CustomToast customToast = null;
    private Handler mHandler_toast;

    private NetworkChangeReceiver mNetworkReceiver = null;
    private DeviceConnectionStateReceiver mDeviceConnectionStateReceiver = null;

    private LinearLayout ll_debug;
    private TextView tvDirectMethodName;
    private TextView tvDirectMethodPayload;
    private TextView tvReceivedStatus;
    private TextView tvReceivedResult;
    private ImageView iv_deviceNetworkState;

    private TextView textMainKeyName;
    private RelativeLayout layerMainKey;
    private LinearLayout layerNoConnect;
    private LinearLayout layerConnecting;

    private ProgressBar progressingConnecting;
    private static ProgressBar progressingOpenClose;

    private ProgressDialog waitDialog;
    private ProgressDialog updateDialog;
    private MaterialDialog terminateDialog = null;
    private MaterialDialog noInternetDialog = null;
    private MaterialDialog noResponseDialog = null;
    private MaterialDialog batteryAlertDialog = null;
    private MaterialDialog temperatureAlertDialog = null;
    private MaterialDialog deviceConnectFailDialog = null;
    private ProgressDialog updatingDialog;
    public android.app.AlertDialog updateSuccessDialog = null;
    public android.app.AlertDialog updateFailDialog = null;
    private MaterialDialog offlineDialog = null;

    private ImageView imgConnStat;
    private ImageView imgDisconnStat;
    private ImageView imgDoorLock;
    private ImageView imgDoorOpen;
    private TextView tv_doorState;
    private TextView batteryText;
    private TextView temperatureText;
    private TextView updatedTimeText;
    private ImageView iv_update_sensor;
    private FButton btn_ota;
    private TextView badge_ota;

    private final static int BUTTON_CLICK_TIME = 100;
    private final static int GET_SHADOW_TIME = 2000;
    private final static int CONNECT_RECHECK_TIME = 30000;
    private final static int UPDATE_SENSOR_TIME = 60000;
    private final static int SUBCRIBE_TIMEOUT_TIME = 60000; //msec
    private final static int COMMAND_TIMEOUT_TIME = 15000; //msec
    private final static int PROVISIONING_TIMEOUT_TIME = 60000; //msec

    static public int statusBarHeight;

    boolean isMqttConnected = false;
    boolean isDeviceConnected = false;
    boolean messageReceived = false;
    public static boolean fromSetting = false;
    private static boolean isActionCompleted = false;

    private WifiManager wifiManager;

    public int publishCount = 0;

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

    private boolean isDoorStateUpdated = false;
    private boolean isTemperatureUpdated = false;
    private boolean isBatteryUpdated = false;
    private boolean isOtaUpdated = false;
    private static boolean isProgressing = false;
    private String pressedButton = "";
    private static boolean isAyncTaskCompleted = true;

    private DeviceMethod methodClient;

    private String lastException;

    private MethodResult result;

    private DeviceTwin twinClient;
    private DeviceTwinDevice device;
    private Set<Pair> currentDesired;

    private final Handler handler = new Handler();

    private static final Long responseTimeout = TimeUnit.SECONDS.toSeconds(10);
    private static final Long connectTimeout = TimeUnit.SECONDS.toSeconds(10);

    private String reported_doorState = "";
    private String reported_doorStateChange = "";
    private String reported_OTAupdate = "";
    private String reported_OTAresult = "";
    private String reported_OTAversion = "";
    private String reported_temperature = "";
    private String reported_doorBell = "";
    private String reported_doorOpenMode = "";
    private String reported_battery = "";
    private String reported_openMethod = "";

    static boolean toggleOpen = false;

    private float density;
    private float densityDpi;
    private String dpiName = "";
    private String screenSize="";

    public static AzureIoTDoorActivity instanceAzureDoor;
    public static AzureIoTDoorActivity getInstanceMain() {return instanceAzureDoor;}

    /**
     ****************************************************************************************
     * @brief Handler class for MainActivity
     * @param
     * @return none
     ****************************************************************************************
     */
    public Handler DoorHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            AZUREEVENT event = AZUREEVENT.values()[msg.what];
            switch(event) {

                case E_QUERY_DEVICE_TWIN:
                    if (isAyncTaskCompleted) {
                        MyLog.i("E_QUERY_DEVICE_TWIN");
                        removeEvent(AZUREEVENT.E_QUERY_DEVICE_TWIN);
                        getDeviceTwinTask = new GetDeviceTwinTask();
                        getDeviceTwinTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                        postEvent(AZUREEVENT.E_QUERY_DEVICE_TWIN, 2000);
                    } else {
                        try {
                            Thread.sleep(1000);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        sendEvent(AZUREEVENT.E_QUERY_DEVICE_TWIN);
                    }
                    break;

                case E_DEVICE_CONNECT_CHECK:
                    MyLog.i("=== E_DEVICE_CONNECT_CHECK ===");
                    deviceConnectCheck();
                    MyLog.i("isDeviceConnected = "+isDeviceConnected);
                    if (isDeviceConnected == false) {
                        postEvent(AZUREEVENT.E_DEVICE_CONNECT_CHECK, CONNECT_RECHECK_TIME);
                    }
                    break;

                case E_DEVICE_CONNECT_TIMEOUT:
                    MyLog.i("=== E_DEVICE_CONNECT_TIMEOUT ===");

                    if (waitDialog != null) {
                        waitDialog.dismiss();
                        waitDialog = null;
                    }

                    removeEvent(AZUREEVENT.E_DEVICE_CONNECT_TIMEOUT);
                    removeEvent(AZUREEVENT.E_QUERY_DEVICE_TWIN);
                    sendEvent(AZUREEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);

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

                case E_DEVICE_CONNECT_REMOVE_CALLBACK:
                    MyLog.i("=== E_DEVICE_CONNECT_REMOVE_CALLBACK ===");
                    MyLog.i("isMqttConnected = "+isMqttConnected);
                    if (isMqttConnected) {
                        if (waitDialog != null) {
                            waitDialog.dismiss();
                        }
                    }

                    if (mHandler_device_connect_noResponse != null) {
                        mHandler_device_connect_noResponse.removeCallbacksAndMessages(null);
                    }
                    break;

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

                case E_AUTO_TEST:
                {
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
                        postEvent(AZUREEVENT.E_AUTO_TEST, 1000 * 60 * i1);
                    }
                }
                break;

            }
            super.handleMessage(msg);
        }};

    public void sendEvent(AZUREEVENT _Event) {
        int m;
        m = _Event.ordinal();
        Message msg = DoorHandler.obtainMessage(m);
        DoorHandler.sendEmptyMessage(msg.what);
    }

    public  void sendEvent(AZUREEVENT _Event, int _arg1) {
        int m;
        m = _Event.ordinal();
        Message msg = DoorHandler.obtainMessage(m);
        msg.arg1 = _arg1;
        DoorHandler.sendEmptyMessage(msg.what);
    }

    private void postEvent(AZUREEVENT _Event, int _time )
    {
        int m;
        m = _Event.ordinal();
        Message msg = DoorHandler.obtainMessage(m);
        DoorHandler.sendEmptyMessageDelayed(msg.what, _time);
    }

    public void removeEvent(AZUREEVENT _Event)
    {
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
        setContentView(R.layout.azureiot_activity_door);

        getBuildInformation();

        getCurrentDeviceResolution();
        getCurrentDeviceDpi();
        getCurrentDeviceSize();
        MyLog.i(" ===========================");
        MyLog.i( " density : " + density);
        MyLog.i( " densityDpi : " + densityDpi);
        MyLog.i( " dpiName : " + dpiName);
        MyLog.i( " screen size : " + screenSize);
        MyLog.i( " ===========================");

        wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mNetworkReceiver = new NetworkChangeReceiver();
        IntentFilter network_filter = new IntentFilter();
        network_filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, network_filter);

        mContext = this;
        instanceAzureDoor =  this;
        activity = AzureIoTDoorActivity.this;

        customToast = new CustomToast(getApplicationContext());
        mHandler_toast = new Handler();

        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);
        StaticDataSave.azureConString = StaticDataSave.saveData.getString(StaticDataSave.azureConStringKey, null);

        //[[for REST
        if (StaticDataSave.azureConString != null && !StaticDataSave.azureConString.isEmpty()) {
            String[] azureConStringArr0 = StaticDataSave.azureConString.split(";");
            MyLog.i("azureConStringArr0 = "+ Arrays.toString(azureConStringArr0));
            String[] azureConStringArr1 = azureConStringArr0[0].split("=");
            MyLog.i("azureConStringArr1 = "+Arrays.toString(azureConStringArr1));
            String azureConStringArr2 = azureConStringArr1[1];
            MyLog.i("azureConStringArr2 = "+azureConStringArr2);
            String[] azureConStringArr3 = azureConStringArr2.split("\\.");
            MyLog.i("azureConStringArr3 = "+Arrays.toString(azureConStringArr3));
            iothubName = azureConStringArr3[0];
            MyLog.i(">> iothubName = "+iothubName);
            String[] azureConStringArr = StaticDataSave.azureConString.split("SharedAccessKey=");
            sharedAccessKey = azureConStringArr[1];
        }
        //]]

        device = new DeviceTwinDevice(StaticDataSave.thingName);

        twinClient = null;
        twinClient = new DeviceTwin(StaticDataSave.azureConString);
        currentDesired = device.getDesiredProperties();

        methodClient = new DeviceMethod(StaticDataSave.azureConString);

        initMainResource();

        mHandler_device_connect_noResponse = new Handler();
        mRunnable_device_connect_noResponse = new Runnable() {
            @Override
            public void run() {
                MyLog.i("mRunnable_device_connect_noResponse :: messageReceived = "+messageReceived);
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
                    removeEvent(AZUREEVENT.E_DEVICE_CONNECT_TIMEOUT);
                }
            }
        };

        mHandler_mqtt_connect_noResponse = new Handler();
        mRunnable_mqtt_connect_noResponse = new Runnable() {
            @Override
            public void run() {
                MyLog.i("mRunnable_mqtt_connect_noResponse :: messageReceived = "+messageReceived);
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
                removeEvent(AZUREEVENT.E_DEVICE_CONNECT_TIMEOUT);
            }
        };

        mHandler_update_sensor_noResponse = new Handler();
        mRunnable_update_sensor_noResponse = new Runnable() {
            @Override
            public void run() {
                MyLog.i("mRunnable_update_sensor :: messageReceived = "+messageReceived);


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
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
                    }
                });


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
                                    isProgressing = false;
                                    sendEvent(AZUREEVENT.E_UPDATE_SENSOR_NO_RESPONSE_REMOVE_CALLBACK);
                                }
                            })
                            .positiveText("OK")
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    sendEvent(AZUREEVENT.E_UPDATE_SENSOR_NO_RESPONSE_REMOVE_CALLBACK);
                                    dialog.dismiss();
                                    isProgressing = false;
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

                if (progressingOpenClose != null) {
                    progressingOpenClose.setVisibility(View.INVISIBLE);
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
                                    isProgressing = false;
                                    sendEvent(AZUREEVENT.E_DEVICE_OPEN_NO_RESPONSE_REMOVE_CALLBACK);
                                }
                            })
                            .positiveText("OK")
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    sendEvent(AZUREEVENT.E_DEVICE_OPEN_NO_RESPONSE_REMOVE_CALLBACK);
                                    dialog.dismiss();
                                    isProgressing = false;
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

                if (progressingOpenClose != null) {
                    progressingOpenClose.setVisibility(View.INVISIBLE);
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
                                    isProgressing = false;

                                    sendEvent(AZUREEVENT.E_DEVICE_CLOSE_NO_RESPONSE_REMOVE_CALLBACK);
                                }
                            })
                            .positiveText("OK")
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    sendEvent(AZUREEVENT.E_DEVICE_CLOSE_NO_RESPONSE_REMOVE_CALLBACK);
                                    dialog.dismiss();
                                    isProgressing = false;
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
                        MyLog.i("isMqttConnected = "+isMqttConnected+", isDeviceConnected = "+isDeviceConnected);
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

        MyLog.i("isProgressing = "+isProgressing);
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

        MyLog.i("isProgressing = "+isProgressing);
        if (imgDoorOpen != null && isProgressing == false) {
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

        tvDirectMethodName = findViewById(R.id.tvDirectMethodName);
        tvDirectMethodPayload = findViewById(R.id.tvDirectMethodPayload);
        tvReceivedStatus = findViewById(R.id.tvReceivedStatus);
        tvReceivedResult = findViewById(R.id.tvReceivedResult);

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
        }

        layerNoConnect = findViewById(R.id.layerNoConnect);
        layerConnecting = findViewById(R.id.layerConnecting);

        Button btn_reconnect = findViewById(R.id.btn_reconnect);
        if (btn_reconnect != null) {
            btn_reconnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (isProgressing == false) {
                        layerConnecting.setVisibility(View.VISIBLE);
                        deviceConnectCheck();
                    }
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

        tv_doorState = findViewById(R.id.tv_doorState);

        batteryText = findViewById(R.id.tv_battery);
        temperatureText = findViewById(R.id.tv_temperature);

        iv_update_sensor = findViewById(R.id.iv_update_sensor);

        updatedTimeText = findViewById(R.id.tv_updatedTime);

        btn_ota = findViewById(R.id.btn_ota);
        btn_ota.setFButtonPadding(20,0,20,0);

        btn_ota.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isProgressing == false) {
                    MyLog.i("=== update switch ===");

                    MyLog.i("isDeviceConnected = "+ isDeviceConnected);
                    if (isDeviceConnected) {

                        MyLog.i("StaticDataSave.existOTAupdateFlag = "+StaticDataSave.existOTAupdateFlag);
                        if (StaticDataSave.existOTAupdateFlag == true) {

                            StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                            StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);

                            tvDirectMethodName.setText(METHOD_NAME_APPCONTROL);
                            tvDirectMethodPayload.setText(APP_CONTROL_OTA_MESSAGE);

                            isProgressing = true;
                            pressedButton = "ota";
                            invokeMethod(METHOD_NAME_APPCONTROL, APP_CONTROL_OTA_MESSAGE);
                        } else {
                            mHandler_toast.post(new ToastRunnable(getResources().getString(R.string.nothing_update)));
                        }
                    } else {
                        mHandler_toast.post(new ToastRunnable(getResources().getString(R.string.connect_fail)));
                    }
                }
            }
        });


        badge_ota = findViewById(R.id.badge_ota);
        StaticDataSave.existOTAupdateFlag = StaticDataSave.saveData.getBoolean(StaticDataSave.existOTAupdateFlagKey, false);
        MyLog.i("StaticDataSave.existOTAupdateFlag = "+StaticDataSave.existOTAupdateFlag);
        if (btn_ota != null) {
            if (StaticDataSave.existOTAupdateFlag == true) {
                badge_ota.setVisibility(View.VISIBLE);
            } else {
                badge_ota.setVisibility(View.INVISIBLE);
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        MyLog.i("StaticDataSave.thingName = "+StaticDataSave.thingName);

        if (StaticDataSave.thingName == null) {
            textMainKeyName.setText("Unknown");
            tv_doorState.setText(R.string.device_register);
            imgDoorLock.setVisibility(View.INVISIBLE);
            imgDoorOpen.setVisibility(View.INVISIBLE);
            layerConnecting.setVisibility(View.INVISIBLE);
            layerNoConnect.setVisibility(View.INVISIBLE);

        } else {

            textMainKeyName.setText(StaticDataSave.thingName);


            StaticDataSave.readyFlag = StaticDataSave.saveData.getBoolean(StaticDataSave.readyFlagKey, false);

            MyLog.i("StaticDataSave.readyFlag = "+StaticDataSave.readyFlag);
            if (StaticDataSave.readyFlag == false) {

                if (iv_deviceNetworkState != null) {
                    iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_red);
                    iv_deviceNetworkState.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                    iv_deviceNetworkState.clearAnimation();
                }

                if (waitDialog == null) {

                    waitDialog = new ProgressDialog(this, android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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
                postEvent(AZUREEVENT.E_DEVICE_CONNECT_TIMEOUT, PROVISIONING_TIMEOUT_TIME);

            } else {
                if (isOnline()) {
                    sendEvent(AZUREEVENT.E_QUERY_DEVICE_TWIN);
                }
                if (fromSetting) {
                    fromSetting = false;

                    sendEvent(AZUREEVENT.E_QUERY_DEVICE_TWIN);
                    sendEvent(AZUREEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);
                }

                StaticDataSave.existOTAupdateFlag = StaticDataSave.saveData.getBoolean(StaticDataSave.existOTAupdateFlagKey, false);
                MyLog.i("StaticDataSave.existOTAupdateFlag = "+StaticDataSave.existOTAupdateFlag);
                if (btn_ota != null) {
                    if (StaticDataSave.existOTAupdateFlag == true) {
                        badge_ota.setVisibility(View.VISIBLE);
                    } else {
                        badge_ota.setVisibility(View.INVISIBLE);
                    }
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyLog.i("onPause()");
        DoorHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyLog.i("onDestroy()");
        isDeviceConnected = false;
        isMqttConnected = false;
        isProgressing = false;

        if (mNetworkReceiver != null) {
            unregisterReceiver(mNetworkReceiver);
        }

        if (mDeviceConnectionStateReceiver != null) {
            unregisterReceiver(mDeviceConnectionStateReceiver);
        }

        if (waitDialog != null) {
            waitDialog.dismiss();
        }

        if (updateDialog != null) {
            updateDialog.dismiss();
            updateDialog = null;
        }
        if (updatingDialog != null) {
            updatingDialog.dismiss();
            updatingDialog = null;
        }
        if (terminateDialog != null) {
            terminateDialog.dismiss();
            terminateDialog = null;
        }

        DoorHandler.removeCallbacksAndMessages(null);

        try {
            if (getDeviceTwinTask.getStatus() == AsyncTask.Status.RUNNING) {
                getDeviceTwinTask.cancel(true);
            } else {
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        sendEvent(AZUREEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);

    }

    @Override
    public void onBackPressed() {
        MyLog.i("=== onBackPressed() ===");

        if (terminateDialog == null && ((Activity) mContext).hasWindowFocus()) {
            MyLog.i(">>> test2");
            terminateDialog = new MaterialDialog.Builder(mContext)
                    .theme(Theme.LIGHT)
                    .title("Confirm")
                    .titleColor(mContext.getResources().getColor(R.color.blue3))
                    .titleGravity(GravityEnum.CENTER)
                    .content("Would you like to exit Azure IoT?")
                    .contentColor(mContext.getResources().getColor(R.color.black))
                    .widgetColorRes(R.color.blue3)
                    .positiveText("OK")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                            StaticDataSave.readyFlag = false;
                            StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                            editor.putBoolean(StaticDataSave.readyFlagKey, StaticDataSave.readyFlag);
                            editor.commit();

                            terminateDialog = null;
                            Intent main = new Intent(mContext, MainActivity.class);
                            main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(main);
                            finishAffinity();
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

    public static Date convertStringToDate(String dateString) {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        Date date = null;
        try {
            date = format.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date;
    }

    private static void getInitialState(DeviceTwin twinClient, DeviceTwinDevice device) throws IOException, IotHubException
    {
        MyLog.i("Getting the Device twin");
        twinClient.getTwin(device);
        MyLog.i(device.toString());

        //Update Twin Tags and Desired Properties
        Set<Pair> tags = new HashSet<>();
        tags.add(new Pair("HomeID", UUID.randomUUID()));
        device.setTags(tags);
    }

    private static void queryTwin(DeviceTwin twinClient) throws IOException, IotHubException
    {
        MyLog.i("Started Querying twin");

        SqlQuery sqlQuery = SqlQuery.createSqlQuery("*", SqlQuery.FromType.DEVICES, null, null);

        Query twinQuery = twinClient.queryTwin(sqlQuery.getQuery(), 3);

        while (twinClient.hasNextDeviceTwin(twinQuery))
        {
            DeviceTwinDevice d = twinClient.getNextDeviceTwin(twinQuery);
            MyLog.i(d.toString());
        }
    }

    final Runnable exceptionRunnable = new Runnable() {
        public void run() {

            MyLog.e(lastException);

            if(lastException.contains("404103")) {  // device is offline

                sendEvent(AZUREEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);
                sendEvent(AZUREEVENT.E_DEVICE_CLOSE_NO_RESPONSE_REMOVE_CALLBACK);
                sendEvent(AZUREEVENT.E_DEVICE_OPEN_NO_RESPONSE_REMOVE_CALLBACK);
                sendEvent(AZUREEVENT.E_UPDATE_SENSOR_NO_RESPONSE_REMOVE_CALLBACK);

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
                removeEvent(AZUREEVENT.E_DEVICE_CONNECT_TIMEOUT);


                if (offlineDialog == null && ((Activity) mContext).hasWindowFocus()) {

                    offlineDialog = new MaterialDialog.Builder(mContext)
                            .theme(Theme.LIGHT)
                            .title("Error")
                            .titleColor(mContext.getResources().getColor(R.color.blue3))
                            .titleGravity(GravityEnum.CENTER)
                            .content("The operation failed because the requested device isn't online\n"+
                                    "or hasn't registered the direct method callback.")
                            .contentColor(mContext.getResources().getColor(R.color.black))
                            .widgetColorRes(R.color.blue3)
                            .positiveText("OK")
                            .onPositive(new MaterialDialog.SingleButtonCallback() {
                                @Override
                                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                    offlineDialog = null;
                                    handler.removeCallbacksAndMessages(null);
                                    Intent main = new Intent(mContext, MainActivity.class);
                                    main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    startActivity(main);
                                    finishAffinity();
                                }
                            })
                            .build();
                    offlineDialog.getWindow().setGravity(Gravity.CENTER);
                    offlineDialog.show();
                    offlineDialog.setCanceledOnTouchOutside(false);
                    offlineDialog.setCancelable(false);
                }
            }
        }
    };

    final Runnable methodResultRunnable = new Runnable() {
        public void run() {
            Context context = getApplicationContext();
            CharSequence text;
            if (result != null && result.getStatus() != null && result.getPayload() != null) {
                text = "Received Status=" + result.getStatus() + " Payload=" + result.getPayload();

                MyLog.i(String.valueOf(text));

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (tvReceivedStatus != null) {
                                    tvReceivedStatus.setText(String.valueOf(result.getStatus()));
                                }

                                if (tvReceivedResult != null) {
                                    tvReceivedResult.setText(String.valueOf(result.getPayload()));
                                }
                            }
                        });
                    }
                }).start();


                if (String.valueOf(result.getPayload()).equals(DEVICE_CONNECT_RESPONSE_MESSAGE)) {
                    MyLog.i("receive DEVICE_CONNECT_RESPONSE_MESSAGE");
                    messageReceived = true;
                    isDeviceConnected = true;
                    isProgressing = false;
                    removeEvent(AZUREEVENT.E_DEVICE_CONNECT_CHECK);

                    MyLog.i("isMqttConnected = "+isMqttConnected);

                    if (isMqttConnected) {
                        if (waitDialog != null) {
                            waitDialog.dismiss();
                        }
                    }
                    removeEvent(AZUREEVENT.E_DEVICE_CONNECT_TIMEOUT);

                    StaticDataSave.readyFlag = true;
                    StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                    editor.putBoolean(StaticDataSave.readyFlagKey, StaticDataSave.readyFlag);
                    editor.commit();
                    sendEvent(AZUREEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);

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

                } else if (String.valueOf(result.getPayload()).equals(DEVICE_CONTROL_OPEN_RESPONSE_MESSAGE)) {
                    MyLog.i("receive DEVICE_CONTROL_OPEN_RESPONSE_MESSAGE");
                    messageReceived = true;
                    isDeviceConnected = true;
                    removeEvent(AZUREEVENT.E_DEVICE_CONNECT_CHECK);
                    removeEvent(AZUREEVENT.E_DEVICE_CONNECT_TIMEOUT);
                    sendEvent(AZUREEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);

                    if (isMqttConnected) {
                        if (waitDialog != null) {
                            waitDialog.dismiss();
                        }
                    }
                    MyLog.i("sendEvent(AZUREEVENT.E_DEVICE_OPEN_NO_RESPONSE_REMOVE_CALLBACK) 5");
                    sendEvent(AZUREEVENT.E_DEVICE_OPEN_NO_RESPONSE_REMOVE_CALLBACK);
                    if (iv_deviceNetworkState != null) {
                        iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_green);
                        iv_deviceNetworkState.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                        iv_deviceNetworkState.clearAnimation();
                    }

                } else if (String.valueOf(result.getPayload()).equals(DEVICE_CONTROL_CLOSE_RESPONSE_MESSAGE)) {
                    messageReceived = true;
                    isDeviceConnected = true;
                    removeEvent(AZUREEVENT.E_DEVICE_CONNECT_CHECK);
                    removeEvent(AZUREEVENT.E_DEVICE_CONNECT_TIMEOUT);
                    sendEvent(AZUREEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);

                    if (isMqttConnected) {
                        if (waitDialog != null) {
                            waitDialog.dismiss();
                        }
                    }
                    MyLog.i("AZUREEVENT.E_DEVICE_CLOSE_NO_RESPONSE_REMOVE_CALLBACK 6");
                    sendEvent(AZUREEVENT.E_DEVICE_CLOSE_NO_RESPONSE_REMOVE_CALLBACK);
                    if (iv_deviceNetworkState != null) {
                        iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_green);
                        iv_deviceNetworkState.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                        iv_deviceNetworkState.clearAnimation();
                    }

                } else if (String.valueOf(result.getPayload()).equals(DEVICE_UPDATE_SENSOR_RESPONSE_MESSAGE)) {
                    messageReceived = true;

                    if (iv_deviceNetworkState != null) {
                        iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_green);
                        iv_deviceNetworkState.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                        iv_deviceNetworkState.clearAnimation();
                    }
                    if (updateDialog != null) {
                        updateDialog.dismiss();
                        updateDialog = null;
                    }
                    sendEvent(AZUREEVENT.E_UPDATE_SENSOR_NO_RESPONSE_REMOVE_CALLBACK);
                    sendEvent(AZUREEVENT.E_QUERY_DEVICE_TWIN);

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    isProgressing = false;
                }

                //for AT-CMD
                else if (String.valueOf(result.getPayload()).contains("mcu_door")) {
                    messageReceived = true;
                    isDeviceConnected = true;
                    removeEvent(AZUREEVENT.E_DEVICE_CONNECT_CHECK);
                    removeEvent(AZUREEVENT.E_DEVICE_CONNECT_TIMEOUT);
                    sendEvent(AZUREEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);

                    if (isMqttConnected) {
                        if (waitDialog != null) {
                            waitDialog.dismiss();
                        }
                    }

                    if (iv_deviceNetworkState != null) {
                        iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_green);
                        iv_deviceNetworkState.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                        iv_deviceNetworkState.clearAnimation();
                    }
                    if (String.valueOf(result.getPayload()).contains(DEVICE_CONTROL_OPEN_RESPONSE_MESSAGE)) {
                        sendEvent(AZUREEVENT.E_DEVICE_OPEN_NO_RESPONSE_REMOVE_CALLBACK);
                        MyLog.i("doorLockCtl(false)");
                    } else if (String.valueOf(result.getPayload()).contains(DEVICE_CONTROL_CLOSE_RESPONSE_MESSAGE)) {
                        sendEvent(AZUREEVENT.E_DEVICE_CLOSE_NO_RESPONSE_REMOVE_CALLBACK);
                        MyLog.i("doorLockCtl(true)");
                    }
                } else if (String.valueOf(result.getPayload()).contains("mcu_shadow") && String.valueOf(result.getPayload()).contains(DEVICE_UPDATE_SENSOR_RESPONSE_MESSAGE) ) {
                    messageReceived = true;
                    isDeviceConnected = true;
                    if (iv_deviceNetworkState != null) {
                        iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_green);
                        iv_deviceNetworkState.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                        iv_deviceNetworkState.clearAnimation();
                    }
                    if (updateDialog != null) {
                        updateDialog.dismiss();
                        updateDialog = null;
                    }
                    removeEvent(AZUREEVENT.E_DEVICE_CONNECT_CHECK);
                    removeEvent(AZUREEVENT.E_DEVICE_CONNECT_TIMEOUT);
                    sendEvent(AZUREEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);
                    sendEvent(AZUREEVENT.E_UPDATE_SENSOR_NO_RESPONSE_REMOVE_CALLBACK);
                    sendEvent(AZUREEVENT.E_QUERY_DEVICE_TWIN);

                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    isProgressing = false;

                }
                else {
                    text = "Received null result";
                }
            }
        }
    };

    private void invokeMethod(String methodName, String payload) {
        new Thread(new Runnable() {
            public void run() {

                if (methodClient != null) {
                    try {
                        result = methodClient.invoke(StaticDataSave.thingName, methodName, responseTimeout, connectTimeout, payload);

                        if(result == null) {
                            throw new IOException("Method invoke returns null");
                        } else {
                            handler.post(methodResultRunnable);
                        }
                    } catch (Exception e) {
                        lastException = "Exception while trying to invoke direct method: " + e.toString();
                        handler.post(exceptionRunnable);
                    }
                }
            }
        }).start();
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

                        //[[for REST
                        try {
                            sendEvent(AZUREEVENT.E_QUERY_DEVICE_TWIN);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private class DeviceConnectionStateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(final Context context, final Intent intent) {

            String action = intent.getAction();


            if (action.equals(device.getConnectionState())) {

                if (device.getConnectionState() == DeviceConnectionState.Connected.getValue()) {
                    MyLog.i("Device is connected!");
                } else if(device.getConnectionState() == DeviceConnectionState.Disconnected.getValue()) {
                    MyLog.i("Device is disconnected!");
                }
            }
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public void deviceConnectCheck() {
        MyLog.i("=== deviceConnectCheck() ===");
        final String msg = APP_CONNECT_MESSAGE;

        isProgressing = true;
        pressedButton = "connect";
        invokeMethod(METHOD_NAME_APPCONTROL, msg);
        tvDirectMethodName.setText(METHOD_NAME_APPCONTROL);
        tvDirectMethodPayload.setText(msg);
        if (iv_deviceNetworkState != null) {
            iv_deviceNetworkState.setBackgroundResource(R.drawable.circle_yellow);
            iv_deviceNetworkState.setColorFilter(Color.YELLOW, PorterDuff.Mode.MULTIPLY);
            Animation startAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink_animation);
            iv_deviceNetworkState.startAnimation(startAnimation);
        }
        mHandler_device_connect_noResponse.postDelayed(mRunnable_device_connect_noResponse, SUBCRIBE_TIMEOUT_TIME);
    }

    private void updateSensorRequest() {

        if (isProgressing == false) {
            if (updateDialog == null) {
                updateDialog = new ProgressDialog(this, android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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
            isProgressing = true;
            pressedButton = "update";

            StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
            StaticDataSave.mode = StaticDataSave.saveData.getInt(StaticDataSave.modeKey, -1);
            if (StaticDataSave.mode == 20) {
                invokeMethod(METHOD_NAME_APPCONTROL, APP_UPDATE_SENSOR_MESSAGE);
                tvDirectMethodName.setText(METHOD_NAME_APPCONTROL);
                tvDirectMethodPayload.setText(APP_UPDATE_SENSOR_MESSAGE);
            } else if (StaticDataSave.mode == 21) {
                invokeMethod(METHOD_NAME_APPCONTROL, ATCMD_APP_UPDATE_SENSOR_MESSAGE);
                tvDirectMethodName.setText(METHOD_NAME_APPCONTROL);
                tvDirectMethodPayload.setText(ATCMD_APP_UPDATE_SENSOR_MESSAGE);
            }
            mHandler_update_sensor_noResponse.postDelayed(mRunnable_update_sensor_noResponse, SUBCRIBE_TIMEOUT_TIME);

            batteryText.setText("- -"+" \u0025");
            temperatureText.setText("- -"+" \u00b0"+"C");
        }
    }

    private void doorLockCtl(boolean lock) {    // open : false, lock : true

        if (progressingOpenClose.isShown()) {
            progressingOpenClose.setVisibility(View.INVISIBLE);
        }

        if(lock) {
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

        if (lock) {
            if (imgDoorLock.getVisibility() == View.VISIBLE) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                isProgressing = false;
            }
        } else {
            if (imgDoorOpen.getVisibility() == View.VISIBLE) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                isProgressing = false;
            }
        }
    }

    public void clickImgDoorLocked() {  //open

        if (updateDialog != null && updateDialog.isShowing()) {
            mHandler_toast.post(new ToastRunnable("Another job is being processed"));
        } else {
            MyLog.i("isMqttConnected = "+isMqttConnected+", isDeviceConnected = "+isDeviceConnected+", isProgressing = "+isProgressing);
            if (isMqttConnected && isDeviceConnected && (isProgressing == false)) {

                MyLog.i("imgDoorLock is clicked!");

                progressingOpenClose.setVisibility(View.VISIBLE);

                layerMainKey.setVisibility(View.VISIBLE);
                layerNoConnect.setVisibility(View.GONE);

                if (noInternetDialog != null) {
                    noInternetDialog.dismiss();
                    noInternetDialog = null;
                }
                layerConnecting.setVisibility(View.GONE);
                sendDoorOpen();  // open

                // for AUTO TEST

                if (buildOption.AUTO_TEST == true) {

                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    MyLog.i("================================================================================");
                    MyLog.i("AUTO_TEST  " +buildOption.AUTO_TEST_MIN_TIME  + " min ~ "+buildOption.AUTO_TEST_MAX_TIME +" min");
                    MyLog.i("================================================================================");

                    postEvent(AZUREEVENT.E_AUTO_TEST, 1000 * 60 * buildOption.AUTO_TEST_FIRST_START_TIME);

                    buildOption.AUTO_TEST = false;
                    MaterialDialog autoTestDialog = null;
                    if (autoTestDialog == null) {
                        autoTestDialog = new MaterialDialog.Builder(AzureIoTDoorActivity.this)
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
                                        removeEvent(AZUREEVENT.E_AUTO_TEST);

                                        MyLog.i("remove dialog ");

                                        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                                    }
                                })
                                .build();
                        autoTestDialog.getWindow().setGravity(Gravity.CENTER);
                        autoTestDialog.setCanceledOnTouchOutside(false);
                        autoTestDialog.show();
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
            MyLog.i("isMqttConnected = "+isMqttConnected+", isDeviceConnected = "+isDeviceConnected+", isProgressing = "+isProgressing);
            if (isMqttConnected && isDeviceConnected && (isProgressing == false)) {

                MyLog.i("imgDoorOpen is clicked!");

                progressingOpenClose.setVisibility(View.VISIBLE);

                layerMainKey.setVisibility(View.VISIBLE);
                layerNoConnect.setVisibility(View.GONE);
                if (noInternetDialog != null) {
                    noInternetDialog.dismiss();
                    noInternetDialog = null;
                }
                layerConnecting.setVisibility(View.GONE);
                sendDoorClose();  // close
                mHandler_close_noResponse.postDelayed(mRunnable_close_noResponse, COMMAND_TIMEOUT_TIME);

            } else if (isMqttConnected == false) {
                mHandler_toast.post(new ToastRunnable("The network connection is uncertain"));
            } else if (isDeviceConnected == false) {
                mHandler_toast.post(new ToastRunnable("It is not connected to the device"));
            }

        }
    }

    private void sendDoorOpen() {
        SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd a HH:mm:ss");
        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);
        StaticDataSave.mode = StaticDataSave.saveData.getInt(StaticDataSave.modeKey, -1);

        String msg = "";
        publishCount++;

        // for AT-CMD
        if (StaticDataSave.mode == 20) {
            if (buildOption.TEST_MODE == true) {
                msg = APP_CONTROL_OPEN_MESSAGE+" <"+publishCount+"> "+sfd.format(new Date());
            } else {
                msg = APP_CONTROL_OPEN_MESSAGE;
            }
        } else if (StaticDataSave.mode == 21) {
            if (buildOption.TEST_MODE == true) {
                msg = ATCMD_APP_CONTROL_OPEN_MESSAGE+" <"+publishCount+"> "+sfd.format(new Date());
            } else {
                msg = ATCMD_APP_CONTROL_OPEN_MESSAGE;
            }
        }
        //

        tvDirectMethodName.setText(METHOD_NAME_APPCONTROL);
        tvDirectMethodPayload.setText(msg);

        isProgressing = true;
        pressedButton = "doorOpen";

        //for AT-CMD
        if (StaticDataSave.mode == 20) {
            invokeMethod(METHOD_NAME_APPCONTROL, APP_CONTROL_OPEN_MESSAGE);
        } else if (StaticDataSave.mode == 21) {
            invokeMethod(METHOD_NAME_APPCONTROL, ATCMD_APP_CONTROL_OPEN_MESSAGE);
        }
        //

        mHandler_device_connect_noResponse.postDelayed(mRunnable_device_connect_noResponse, SUBCRIBE_TIMEOUT_TIME);
        MyLog.i("mHandler_device_connect_noResponse.postDelayed(mRunnable_device_connect_noResponse, SUBCRIBE_TIMEOUT_TIME) 2");

    }

    private void sendDoorClose() {

        SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd a HH:mm:ss");
        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);
        StaticDataSave.mode = StaticDataSave.saveData.getInt(StaticDataSave.modeKey, -1);

        String msg = "";
        publishCount++;

        //for AT-CMD
        if (StaticDataSave.mode == 20) {
            if (buildOption.TEST_MODE == true) {
                msg = APP_CONTROL_CLOSE_MESSAGE+" <"+publishCount+"> "+sfd.format(new Date());
            } else {
                msg = APP_CONTROL_CLOSE_MESSAGE;
            }
        } else if (StaticDataSave.mode == 21) {
            if (buildOption.TEST_MODE == true) {
                msg = ATCMD_APP_CONTROL_CLOSE_MESSAGE+" <"+publishCount+"> "+sfd.format(new Date());
            } else {
                msg = ATCMD_APP_CONTROL_CLOSE_MESSAGE;
            }
        }
        //

        tvDirectMethodName.setText(METHOD_NAME_APPCONTROL);
        tvDirectMethodPayload.setText(msg);

        isProgressing = true;
        pressedButton = "doorClose";

        //for AT-CMD
        if (StaticDataSave.mode == 20) {
            invokeMethod(METHOD_NAME_APPCONTROL, APP_CONTROL_CLOSE_MESSAGE);
        } else if (StaticDataSave.mode == 21) {
            invokeMethod(METHOD_NAME_APPCONTROL, ATCMD_APP_CONTROL_CLOSE_MESSAGE);
        }
        //

        mHandler_device_connect_noResponse.postDelayed(mRunnable_device_connect_noResponse, SUBCRIBE_TIMEOUT_TIME);
        MyLog.i("mHandler_device_connect_noResponse.postDelayed(mRunnable_device_connect_noResponse, SUBCRIBE_TIMEOUT_TIME) 3");

    }


    private void sendC2DMessage(String msg) {
        new Thread(new Runnable() {
            public void run() {

                try {
                    ServiceClient serviceClient = ServiceClient.createFromConnectionString(StaticDataSave.azureConString, protocol);
                    if (serviceClient != null) {
                        serviceClient.open();
                        FeedbackReceiver feedbackReceiver = serviceClient
                                .getFeedbackReceiver();
                        if (feedbackReceiver != null) feedbackReceiver.open();

                        com.diasemi.azure.sdk.iot.service.Message messageToSend = new com.diasemi.azure.sdk.iot.service.Message(msg);
                        messageToSend.setDeliveryAcknowledgement(DeliveryAcknowledgement.Full);
                        serviceClient.send(StaticDataSave.thingName, messageToSend);
                        MyLog.i("Message sent to device");

                        FeedbackBatch feedbackBatch = feedbackReceiver.receive(10000);
                        if (feedbackBatch != null) {
                            MyLog.i("Message feedback received, feedback time: "
                                    + feedbackBatch.getEnqueuedTimeUtc().toString());
                        }

                        if (feedbackReceiver != null) feedbackReceiver.close();
                        serviceClient.close();
                    }
                } catch (IOException | IotHubException | InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }


    private class ToastRunnable implements Runnable {
        String mText;
        public ToastRunnable(String text) {
            mText = text;
        } @Override public void run(){
            if (customToast != null) {
                customToast.showToast(getApplicationContext(), mText, Toast.LENGTH_SHORT);
            }
        }
    }

    private void updateTemperatureUI(final String str) {

        float value = Float.parseFloat(str);

        if (value >= 100) {
            temperatureText.setText("- -"+" \u00b0"+"C");
            temperatureText.setTextColor(mContext.getResources().getColor(R.color.black));
        } else if (value < 100 && value >= 55) {
            temperatureText.setText(String.format("%.1f", value)+" \u00b0"+"C");
            temperatureText.setTextColor(mContext.getResources().getColor(R.color.red));
            sendEvent(AZUREEVENT.E_SHOW_TEMPERATURE_ALERT);
        } else {
            temperatureText.setText(String.format("%.1f", value)+" \u00b0"+"C");
            temperatureText.setTextColor(mContext.getResources().getColor(R.color.black));
            removeEvent(AZUREEVENT.E_SHOW_TEMPERATURE_ALERT);

        }
    }

    private void updateBatteryUI(final String str) {
        float value = Float.parseFloat(str);
        if (calBattery(value) <= 10) {
            batteryText.setText(String.valueOf(calBattery(value))+" \u0025");
            batteryText.setTextColor(mContext.getResources().getColor(R.color.red));
            sendEvent(AZUREEVENT.E_SHOW_BATTERY_ALERT);
        } else if (calBattery(value) == 200) {
            batteryText.setText("- -"+" \u0025");
            batteryText.setTextColor(mContext.getResources().getColor(R.color.black));
        } else {
            batteryText.setText(String.valueOf(calBattery(value))+" \u0025");
            batteryText.setTextColor(mContext.getResources().getColor(R.color.black));
            removeEvent(AZUREEVENT.E_SHOW_BATTERY_ALERT);
        }
    }

    public int calBattery(float shadowBattery) {
        int retBattery = 0;
        int intShadowBattery  = Math.round(shadowBattery);

        if (buildOption.PTIM_SENSING == true) {
            unitBattery_PTIM = (batteryMax_PTIM - batteryMin_PTIM)/10;
            if ((intShadowBattery > (batteryMin_PTIM+(unitBattery_PTIM*9))) && (intShadowBattery < 10000)) {
                retBattery = 100;
            } else if ((intShadowBattery > (batteryMin_PTIM+(unitBattery_PTIM*8))) && (intShadowBattery <= (batteryMin_PTIM+(unitBattery_PTIM*9)))) {
                retBattery = 90;
            } else if ((intShadowBattery > (batteryMin_PTIM+(unitBattery_PTIM*7))) && (intShadowBattery <= (batteryMin_PTIM+(unitBattery_PTIM*8)))) {
                retBattery = 80;
            } else if ((intShadowBattery > (batteryMin_PTIM+(unitBattery_PTIM*6))) && (intShadowBattery <= (batteryMin_PTIM+(unitBattery_PTIM*7)))) {
                retBattery = 70;
            } else if ((intShadowBattery > (batteryMin_PTIM+(unitBattery_PTIM*5))) && (intShadowBattery <= (batteryMin_PTIM+(unitBattery_PTIM*6)))) {
                retBattery = 60;
            } else if ((intShadowBattery > (batteryMin_PTIM+(unitBattery_PTIM*4))) && (intShadowBattery <= (batteryMin_PTIM+(unitBattery_PTIM*5)))) {
                retBattery = 50;
            } else if ((intShadowBattery > (batteryMin_PTIM+(unitBattery_PTIM*3))) && (intShadowBattery <= (batteryMin_PTIM+(unitBattery_PTIM*4)))) {
                retBattery = 40;
            } else if ((intShadowBattery > (batteryMin_PTIM+(unitBattery_PTIM*2))) && (intShadowBattery <= (batteryMin_PTIM+(unitBattery_PTIM*3)))) {
                retBattery = 30;
            } else if ((intShadowBattery > (batteryMin_PTIM+(unitBattery_PTIM*1))) && (intShadowBattery <= (batteryMin_PTIM+(unitBattery_PTIM*2)))) {
                retBattery = 20;
            } else if ((intShadowBattery > batteryMin_PTIM) && (intShadowBattery <= (batteryMin_PTIM+(unitBattery_PTIM*1)))) {
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

            unitBattery_MCU = (batteryMax_MCU - batteryMin_MCU)/10;
            if ((intShadowBattery > (batteryMin_MCU+(unitBattery_MCU*9))) && (intShadowBattery < 10000)) {
                retBattery = 100;
            } else if ((intShadowBattery > (batteryMin_MCU+(unitBattery_MCU*8))) && (intShadowBattery <= (batteryMin_MCU+(unitBattery_MCU*9)))) {
                retBattery = 90;
            } else if ((intShadowBattery > (batteryMin_MCU+(unitBattery_MCU*7))) && (intShadowBattery <= (batteryMin_MCU+(unitBattery_MCU*8)))) {
                retBattery = 80;
            } else if ((intShadowBattery > (batteryMin_MCU+(unitBattery_MCU*6))) && (intShadowBattery <= (batteryMin_MCU+(unitBattery_MCU*7)))) {
                retBattery = 70;
            } else if ((intShadowBattery > (batteryMin_MCU+(unitBattery_MCU*5))) && (intShadowBattery <= (batteryMin_MCU+(unitBattery_MCU*6)))) {
                retBattery = 60;
            } else if ((intShadowBattery > (batteryMin_MCU+(unitBattery_MCU*4))) && (intShadowBattery <= (batteryMin_MCU+(unitBattery_MCU*5)))) {
                retBattery = 50;
            } else if ((intShadowBattery > (batteryMin_MCU+(unitBattery_MCU*3))) && (intShadowBattery <= (batteryMin_MCU+(unitBattery_MCU*4)))) {
                retBattery = 40;
            } else if ((intShadowBattery > (batteryMin_MCU+(unitBattery_MCU*2))) && (intShadowBattery <= (batteryMin_MCU+(unitBattery_MCU*3)))) {
                retBattery = 30;
            } else if ((intShadowBattery > (batteryMin_MCU+(unitBattery_MCU*1))) && (intShadowBattery <= (batteryMin_MCU+(unitBattery_MCU*2)))) {
                retBattery = 20;
            } else if ((intShadowBattery > batteryMin_MCU) && (intShadowBattery <= (batteryMin_MCU+(unitBattery_MCU*1)))) {
                retBattery = 10;
            } else if (intShadowBattery >= 10000) {
                retBattery = 200;
            } else {
                retBattery = 0;
            }
        }
        return retBattery;
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
                        WindowMetrics windowMetrics = activity.getWindowManager().getCurrentWindowMetrics();
                        Insets insets = windowMetrics.getWindowInsets()
                                .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
                        displayWidth = windowMetrics.getBounds().width() - insets.left - insets.right;
                    } else {
                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
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
                            isProgressing = false;
                            dismissUpdateSuccessDialog();
                            sendEvent(AZUREEVENT.E_QUERY_DEVICE_TWIN);
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
                    updateFailDialog = new android.app.AlertDialog.Builder(mContext, R.style.AlertDialogCustom).create();
                    updateFailDialog.setIcon(R.mipmap.renesas_ic_launcher);
                    updateFailDialog.setMessage(message);
                    updateFailDialog.setCancelable(false);
                    updateFailDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dismissUpdateFailDialog();
                            isProgressing = false;
                            sendEvent(AZUREEVENT.E_QUERY_DEVICE_TWIN);
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

    public void getBuildInformation() {
        MyLog.i(" ===========================");
        MyLog.i( " Building date  = "+ BuildInformation.RELEASE_DATE);
        MyLog.i( " App version  = "+ BuildInformation.RELEASE_VERSION);
        MyLog.i( " Constructor  = "+ BuildInformation.RELEASE_CONSTRUCTOR);
        MyLog.i( " ===========================");
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
            screenSize ="xlarge";
        }
    }

    private String utcToLocal(String dateStr) throws ParseException {
        String localTime = "";

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        try {
            Date dateUtcTime = dateFormat.parse(dateStr);

            long longUtcTime = dateUtcTime.getTime();

            TimeZone zone = TimeZone.getDefault();
            int offset = zone.getOffset(longUtcTime);
            long longLocalTime = longUtcTime + offset;

            Date dateLocalTime = new Date();
            dateLocalTime.setTime(longLocalTime);

            SimpleDateFormat newDateFormat = new SimpleDateFormat("yyyy-MM-dd a HH:mm:ss");
            localTime = newDateFormat.format(dateLocalTime);

        } catch (ParseException e) {
            e.printStackTrace();
        }

        return localTime;
    }


    private class GetDeviceTwinTask extends AsyncTask<Void, Void, AsyncTaskResult<String>> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isAyncTaskCompleted = false;
        }

        @Override
        protected void onPostExecute(AsyncTaskResult<String> result) {
            if (result.getError() == null) {

                MyLog.i(">> isDeviceConnected = "+isDeviceConnected);
                if (!isDeviceConnected) {
                    sendEvent(AZUREEVENT.E_DEVICE_CONNECT_CHECK);
                }

                if (!isMqttConnected) {
                    mHandler_toast.post(new ToastRunnable("Connected to Azure IoT Hub"));
                }

                isMqttConnected = true;

                if (isDeviceConnected) {
                    if (waitDialog != null) {
                        waitDialog.dismiss();
                    }
                    removeEvent(AZUREEVENT.E_DEVICE_CONNECT_TIMEOUT);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        imgConnStat.setVisibility(View.VISIBLE);
                        imgConnStat.setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
                        imgConnStat.clearAnimation();
                        imgDisconnStat.clearAnimation();
                        imgDisconnStat.setVisibility(View.INVISIBLE);

                        parseDeviceTwinResult(result.getResult());
                    }
                });

            } else {
                MyLog.e("E / queryTwinTask : "+result.getError());
                mHandler_toast.post(new ToastRunnable("Error occurred while connecting to the Azure IoT Hub."));
                isMqttConnected = false;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imgConnStat.clearAnimation();
                        imgConnStat.setVisibility(View.INVISIBLE);
                        imgDisconnStat.setVisibility(View.VISIBLE);
                        imgDisconnStat.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                        imgDisconnStat.clearAnimation();
                        removeEvent(AZUREEVENT.E_QUERY_DEVICE_TWIN);
                    }
                });
            }
            isAyncTaskCompleted = true;
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected AsyncTaskResult<String> doInBackground(Void... voids) {
            String auth = null;

            try {
                auth = generateSasToken(iothubName+".azure-devices.net/devices/"+StaticDataSave.thingName, "iothubowner",sharedAccessKey);
            } catch (Exception e) {
                e.printStackTrace();
            }
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();

            Request request = new Request.Builder()
                    .url("https://"+iothubName+".azure-devices.net/twins/"+StaticDataSave.thingName+"?api-version=2020-05-31-preview")
                    .get()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", auth)
                    .build();
            Response response = null;
            try {
                response = client.newCall(request).execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String result = null;
            try {
                result = response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
            }
            result = new String(result.replace("$", ""));
            return new AsyncTaskResult<String>(result);
        }


        @RequiresApi(api = Build.VERSION_CODES.O)
        public String generateSasToken(String resourceUri, String keyName, String key) throws Exception {
            // Token will expire in one hour
            var expiry = Instant.now().getEpochSecond() + 3600;

            String stringToSign = URLEncoder.encode(resourceUri, String.valueOf(StandardCharsets.UTF_8)) + "\n" + expiry;
            byte[] decodedKey = java.util.Base64.getDecoder().decode(key);

            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(decodedKey, "HmacSHA256");
            sha256HMAC.init(secretKey);
            java.util.Base64.Encoder encoder = java.util.Base64.getEncoder();

            String signature = new String(encoder.encode(
                    sha256HMAC.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8))), StandardCharsets.UTF_8);

            String token = "SharedAccessSignature sr=" + URLEncoder.encode(resourceUri, String.valueOf(StandardCharsets.UTF_8))
                    + "&sig=" + URLEncoder.encode(signature, StandardCharsets.UTF_8.name()) + "&se=" + expiry + "&skn=" + keyName;

            return token;
        }
    }

    private void parseDeviceTwinResult(String result) {
        String data2 = "";

        MyLog.i("result = "+result);

        Gson gson = new Gson();
        DeviceTwinInfo info = gson.fromJson(result, DeviceTwinInfo.class);

        if (info.properties.reported != null) {
            reported_temperature = String.valueOf(info.properties.reported.temperature);
            reported_battery = String.valueOf(info.properties.reported.battery);
            reported_doorState = String.valueOf(info.properties.reported.doorState);
            reported_openMethod = String.valueOf(info.properties.reported.openMethod);
            reported_OTAupdate = String.valueOf(info.properties.reported.OTAupdate);
            reported_OTAresult = String.valueOf(info.properties.reported.OTAresult);
        }


        if (info.properties.reported.metadata != null) {

            if (info.properties.reported.metadata.temperature != null) {
                String curTemperatureDate = info.properties.reported.metadata.temperature.lastUpdated;
                if (pastTemperatureDate != null && curTemperatureDate != null) {
                    if (!pastTemperatureDate.equals(curTemperatureDate)) {
                        isTemperatureUpdated = true;
                    } else {
                        isTemperatureUpdated = false;
                    }
                    MyLog.i("isTemperatureUpdated = "+String.valueOf(isTemperatureUpdated));

                    if (isTemperatureUpdated) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    updatedTimeText.setText(utcToLocal(curTemperatureDate));
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }

                                if (!TextUtils.isEmpty(reported_temperature)) {
                                    MyLog.i(String.format("temperature : %s", reported_temperature));
                                }
                                updateTemperatureUI(reported_temperature);
                            }
                        });

                        pastTemperatureDate = curTemperatureDate;
                        isTemperatureUpdated = false;
                    }
                }
            }

            if (info.properties.reported.metadata.battery != null) {
                String curBatteryDate = info.properties.reported.metadata.battery.lastUpdated;
                if (curBatteryDate != null) {
                    if (!pastBatteryDate.equals(curBatteryDate)) {
                        isBatteryUpdated = true;
                    } else {
                        isBatteryUpdated = false;
                    }
                    MyLog.i("isBatteryUpdated = "+String.valueOf(isBatteryUpdated));

                    if (isBatteryUpdated) {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    updatedTimeText.setText(utcToLocal(curBatteryDate));
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }

                                if (!TextUtils.isEmpty(reported_battery)) {
                                    MyLog.i(String.format("battery : %s", reported_battery));
                                }

                                float value = Float.parseFloat(reported_battery);

                                if (value <= 10) {
                                    batteryText.setText(reported_battery+" \u0025");
                                    batteryText.setTextColor(mContext.getResources().getColor(R.color.red));
                                    sendEvent(AZUREEVENT.E_SHOW_BATTERY_ALERT);
                                } else {
                                    batteryText.setText(reported_battery+" \u0025");
                                    batteryText.setTextColor(mContext.getResources().getColor(R.color.black));
                                }
                            }
                        });
                    }
                    pastBatteryDate = curBatteryDate;
                    isBatteryUpdated = false;
                }
            }

            if (info.properties.reported.metadata.doorState != null) {
                String curDoorStateDate = info.properties.reported.metadata.doorState.lastUpdated;
                if (curDoorStateDate != null) {
                    if (!pastDoorStateDate.equals(curDoorStateDate)) {
                        isDoorStateUpdated = true;
                    } else {
                        isDoorStateUpdated = false;
                    }
                    MyLog.i("isDoorStateUpdated = "+String.valueOf(isDoorStateUpdated));
                    if (isDoorStateUpdated) {
                        if (!TextUtils.isEmpty(reported_doorState)) {
                            MyLog.i(String.format("doorState : %s", reported_doorState));
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    updatedTimeText.setText(utcToLocal(curDoorStateDate));
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }

                                MyLog.i(String.format("temperature :  %s", reported_temperature));
                                MyLog.i(String.format("battery :  %s", reported_battery));
                                MyLog.i(String.format("OTAupdate :  %s", reported_OTAupdate));
                                MyLog.i(String.format("OTAresult :  %s", reported_OTAresult));

                                if (!TextUtils.isEmpty(reported_doorState) && !TextUtils.isEmpty(reported_openMethod)) {
                                    if (reported_doorState.equals("true")) {  //opened
                                        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                                        tv_doorState.setText(R.string.door_opened);
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

                                    } else if (reported_doorState.equals("false")) {  //closed
                                        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                                        tv_doorState.setText(R.string.door_closed);
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
                        });
                    }
                    pastDoorStateDate = curDoorStateDate;
                    isDoorStateUpdated = false;
                }
            }

            if (info.properties.reported.metadata.OTAupdate != null) {
                String curOTAupdateDate = info.properties.reported.metadata.OTAupdate.lastUpdated;
                if (!pastOTAupdateDate.equals(curOTAupdateDate)) {
                    isOtaUpdated = true;
                } else {
                    isOtaUpdated = false;
                }
                MyLog.i("isOtaUpdated = "+String.valueOf(isOtaUpdated));
                if (isOtaUpdated) {
                    //OTA update check
                    StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                    MyLog.i("reported_OTAupdate = "+reported_OTAupdate);
                    if (reported_OTAupdate.equals("0")) {  //none
                        StaticDataSave.existOTAupdateFlag = false;
                        StaticDataSave.OTAupdateProgressFlag = false;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                badge_ota.setVisibility(View.INVISIBLE);
                            }
                        });

                        if (reported_OTAresult != null) {
                            MyLog.i("reported_OTAresult = "+reported_OTAresult);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (curOTAupdateDate != null) {
                                        try {
                                            updatedTimeText.setText(utcToLocal(curOTAupdateDate));
                                        } catch (ParseException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            });

                            if (reported_OTAresult.equals("OTA_OK")) {

                                if (updatingDialog != null && updatingDialog.isShowing()) {
                                    MyLog.i(">> OTA is completed");
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            showUpdateSuccessDialog();
                                        }
                                    });
                                }
                            }
                            else if (reported_OTAresult.equals("OTA_UNKNOWN")) {
                                if (updatingDialog != null && updatingDialog.isShowing()) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            dismissUpdatingDialog();
                                        }
                                    });
                                }
                            }
                        }
                    } else if (reported_OTAupdate.equals("1")) {  // exist update
                        StaticDataSave.existOTAupdateFlag = true;
                        StaticDataSave.OTAupdateProgressFlag = false;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                badge_ota.setVisibility(View.VISIBLE);
                            }
                        });
                        //[[OTAupdate = 1 when "OTA_NG"
                        if (reported_OTAresult.equals("OTA_NG")) {
                            if (updatingDialog != null && updatingDialog.isShowing()) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showUpdateFailDialog();
                                    }
                                });
                            }
                        }
                        //]]
                    } else if (reported_OTAupdate.equals("2")) {  // update progressing
                        MyLog.i(">> OTA is progressing");
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
                    MyLog.i("StaticDataSave.existOTAupdateFlag = "+StaticDataSave.existOTAupdateFlag);
                    MyLog.i("StaticDataSave.OTAupdateProgressFlag = "+StaticDataSave.OTAupdateProgressFlag);

                    editor.putBoolean(StaticDataSave.existOTAupdateFlagKey, StaticDataSave.existOTAupdateFlag);
                    editor.putBoolean(StaticDataSave.OTAupdateProgressFlagKey, StaticDataSave.OTAupdateProgressFlag);
                    editor.commit();
                    //
                }
                pastOTAupdateDate = curOTAupdateDate;
                isOtaUpdated = false;
            }

        }
    }
}
