package com.renesas.wifi.awsiot;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.auth.core.internal.util.ThreadUtils;
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
import com.renesas.wifi.R;
import com.renesas.wifi.awsiot.shadow.DoorlockStatus;
import com.renesas.wifi.buildOption;
import com.renesas.wifi.util.CustomToast;
import com.renesas.wifi.util.MyLog;
import com.renesas.wifi.util.NetworkUtil;
import com.renesas.wifi.util.StaticDataSave;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static android.net.wifi.SupplicantState.COMPLETED;
import static java.lang.System.exit;

public class MonitorService extends Service {

    public static Regions MY_REGION;
    SubScribeAsyncTask subScribeAsyncTask;

    private static int NOTIFICATION_ID = 2;

    // Message from Device
    private static final String APP_CONNECT_MESSAGE = "connected";
    private static final String DEVICE_CONNECT_RESPONSE_MESSAGE = "yes";
    private static final String DEVICE_CONTROL_OPEN_RESPONSE_MESSAGE = "opened";
    private static final String DEVICE_CONTROL_CLOSE_RESPONSE_MESSAGE = "closed";
    private static final String DEVICE_CONTROL_BATTERY_ALERT_MESSAGE = "batteryAlert";
    private static final String DEVICE_CONTROL_TEMPERATURE_ALERT_MESSAGE = "temperatureAlert";

    static boolean isMqttConnected = false;
    static boolean isDeviceConnected = false;
    static boolean messageReceived = false;

    Context mContext;
    NetworkChangeReceiver mNetworkReceiver = null;
    WifiManager wifiManager;

    // Filename of KeyStore file on the filesystem
    private static final String KEYSTORE_NAME = "iot_keystore";
    // Password for the private key in the KeyStore
    private static final String KEYSTORE_PASSWORD = "password";
    // Certificate and key aliases in the KeyStore
    private static final String CERTIFICATE_ID = "default";

    CustomToast customToast = null;
    Handler mHandler_toast;

    private Handler mHandler_mqtt_connect_noResponse;
    private Runnable mRunnable_mqtt_connect_noResponse;

    private final static int GET_SHADOW_TIME = 1000;
    private final static int UPDATE_SENSOR_TIME = 60000;
    private final static int SUBCRIBE_TIMEOUT_TIME = 120000; //msec

    public GetShadowTask getStatusShadowTask;
    public GetSensorShadowTask getSensorShadowTask;

    AWSConfig awsConfig;

    public static MonitorService instance;

    public static MonitorService getInstance() {
        return instance;
    }

    public Handler MonitorHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            AWSEVENT event = AWSEVENT.values()[msg.what];
            switch (event) {

                case E_DEVICE_CONNECT_REMOVE_CALLBACK_MONITOR:
                    MyLog.i("E_DEVICE_CONNECT_REMOVE_CALLBACK_MONITOR");
                    break;

                case E_MQTT_REMOVE_CALLBACK_MONITOR:
                    MyLog.i("E_MQTT_REMOVE_CALLBACK");
                    if (mHandler_mqtt_connect_noResponse != null) {
                        mHandler_mqtt_connect_noResponse.removeCallbacksAndMessages(null);
                    }
                    break;

                case E_GET_SHADOW_MONITOR:
                    MyLog.i("=== E_GET_SHADOW ===");
                    removeEvent(AWSEVENT.E_GET_SHADOW_MONITOR);
                    getShadows();
                    postEvent(AWSEVENT.E_GET_SHADOW_MONITOR, GET_SHADOW_TIME);
                    break;

                case E_STOP_GET_SHADOW_MONITOR:
                    if (StaticDataSave.thingName != null) {
                        MyLog.i("=== E_STOP_GET_SHADOW_MONITOR ===");
                        stopGetShadow();
                    }
                    break;

                case E_UPDATE_SENSOR_MONITOR:
                    MyLog.i("=== E_UPDATE_SENSOR ===");
                    removeEvent(AWSEVENT.E_UPDATE_SENSOR_MONITOR);
                    getSendsorShadows();
                    postEvent(AWSEVENT.E_UPDATE_SENSOR_MONITOR, UPDATE_SENSOR_TIME);
                    break;

                case E_STOP_UPDATE_SENSOR_MONITOR:
                    if (StaticDataSave.thingName != null) {
                        MyLog.i("=== E_STOP_UPDATE_SENSOR_MONITOR ===");
                        stopGetSensorShadow();
                    }
                    break;
            }
            super.handleMessage(msg);
        }
    };

    public void sendEvent(AWSEVENT _Event) {
        int m;
        m = _Event.ordinal();
        Message msg = MonitorHandler.obtainMessage(m);
        MonitorHandler.sendMessage(msg);
    }

    public void postEvent(AWSEVENT _Event, int _time) {
        int m;
        m = _Event.ordinal();
        Message msg = MonitorHandler.obtainMessage(m);
        MonitorHandler.sendEmptyMessageDelayed(m, _time);
    }

    public void removeEvent(AWSEVENT _Event) {
        int m;
        m = _Event.ordinal();
        Message msg = MonitorHandler.obtainMessage(m);
        MonitorHandler.removeMessages(m);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        MyLog.i("=== onCreate() ===");

        awsConfig = new AWSConfig();

        instance = MonitorService.this;
        mContext = this;

        mNetworkReceiver = new NetworkChangeReceiver();
        IntentFilter network_filter = new IntentFilter();
        network_filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, network_filter);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        subScribeAsyncTask = new SubScribeAsyncTask();

        customToast = new CustomToast(getApplicationContext());
        mHandler_toast = new Handler();

        mHandler_mqtt_connect_noResponse = new Handler();
        mRunnable_mqtt_connect_noResponse = new Runnable() {
            @Override
            public void run() {
                MyLog.i("mRunnable_mqtt_connect_noResponse :: messageReceived = " + messageReceived);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        MyLog.i("=== onStartCommand() ===");

        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);

        startForegroundService();

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        MyLog.i("=== onDestroy() ===");
        super.onDestroy();
        for (int i = 1; i < 9999; i++) {
            cancelNotification(mContext, i);
        }

        unregisterReceiver(mNetworkReceiver);

        removeEvent(AWSEVENT.E_GET_SHADOW_MONITOR);
        removeEvent(AWSEVENT.E_UPDATE_SENSOR_MONITOR);
        sendEvent(AWSEVENT.E_STOP_GET_SHADOW_MONITOR);
        sendEvent(AWSEVENT.E_STOP_UPDATE_SENSOR_MONITOR);
        sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK_MONITOR);
        sendEvent(AWSEVENT.E_MQTT_REMOVE_CALLBACK_MONITOR);
        MonitorHandler.removeCallbacksAndMessages(null);

        disconnectMqtt();
    }


    private void initMqtt() {

        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not guarantee uniqueness.
        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);
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
        StaticDataSave.cognitoPoolId = StaticDataSave.saveData.getString(StaticDataSave.cognitoPoolIdKey, null);
        if (StaticDataSave.thingName != null) {
            AWSIoTDoorActivity.clientId = StaticDataSave.thingName + "-" + UUID.randomUUID().toString();
        }
        MyLog.i("clientId = " + AWSIoTDoorActivity.clientId);

        // Initialize the AWS Cognito credentials provider
        if (StaticDataSave.cognitoPoolId != null) {
            MyLog.i("StaticDataSave.cognitoPoolId = " + StaticDataSave.cognitoPoolId);
            MyLog.i("awsConfig.MY_REGION = " + awsConfig.MY_REGION);
            AWSIoTDoorActivity.credentialsProvider = new CognitoCachingCredentialsProvider(
                    getApplicationContext(), // context
                    StaticDataSave.cognitoPoolId,
                    MY_REGION // Region
            );
        }

        Region region = Region.getRegion(awsConfig.MY_REGION);

        // MQTT Client
        AWSIoTDoorActivity.mqttManager = new AWSIotMqttManager(AWSIoTDoorActivity.clientId, awsConfig.CUSTOMER_SPECIFIC_ENDPOINT);

        // Set keepalive to 10 seconds.  Will recognize disconnects more quickly but will also send
        // MQTT pings every 10 seconds.
        AWSIoTDoorActivity.mqttManager.setKeepAlive(10);

        // Set Last Will and Testament for MQTT.  On an unclean disconnect (loss of connection)
        // AWS IoT will publish this message to alert other clients.
        AWSIotMqttLastWillAndTestament lwt = new AWSIotMqttLastWillAndTestament("my/lwt/topic",
                "Android client lost connection", AWSIotMqttQos.QOS0);
        AWSIoTDoorActivity.mqttManager.setMqttLastWillAndTestament(lwt);
        AWSIoTDoorActivity.mqttManager.setAutoReconnect(true);

        // IoT Client (for creation of certificate if needed)
        if (AWSIoTDoorActivity.mIotAndroidClient == null) {
            AWSIoTDoorActivity.mIotAndroidClient = new AWSIotClient(AWSIoTDoorActivity.credentialsProvider);
            AWSIoTDoorActivity.mIotAndroidClient.setRegion(region);
        }
        connectMqtt();
    }

    private void connectMqtt() {
        MyLog.i("clientId = " + AWSIoTDoorActivity.clientId);

        try {
            AWSIoTDoorActivity.mqttManager.connect(AWSIoTDoorActivity.getCredentialsProvider(), new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status, final Throwable throwable) {
                    MyLog.i("Status = " + String.valueOf(status));

                    ThreadUtils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (status == AWSIotMqttClientStatus.Connecting) {
                                MyLog.i("AWSIotMqttClientStatus :: Connecting");
                                isMqttConnected = false;
                                mHandler_mqtt_connect_noResponse.postDelayed(mRunnable_mqtt_connect_noResponse, SUBCRIBE_TIMEOUT_TIME);
                            } else if (status == AWSIotMqttClientStatus.Connected) {
                                MyLog.i("AWSIotMqttClientStatus :: Connected");
                                isMqttConnected = true;
                                sendEvent(AWSEVENT.E_MQTT_REMOVE_CALLBACK_MONITOR);
                                MyLog.i("mHandler.removeCallbacks(mRunnable_mqtt_connect) 1");

                                sendEvent(AWSEVENT.E_GET_SHADOW_MONITOR);
                                sendEvent(AWSEVENT.E_UPDATE_SENSOR_MONITOR);

                                subScribeAsyncTask = new SubScribeAsyncTask();
                                subScribeAsyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

                            } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                MyLog.i("AWSIotMqttClientStatus :: Reconnecting");
                                if (throwable != null) {
                                    MyLog.e("Connection error." + throwable);
                                }
                                isMqttConnected = false;
                                removeEvent(AWSEVENT.E_GET_SHADOW_MONITOR);
                                removeEvent(AWSEVENT.E_UPDATE_SENSOR_MONITOR);
                                sendEvent(AWSEVENT.E_STOP_GET_SHADOW_MONITOR);
                                sendEvent(AWSEVENT.E_STOP_UPDATE_SENSOR_MONITOR);
                                try {
                                    if (getStatusShadowTask.getStatus() == AsyncTask.Status.RUNNING) {
                                        getStatusShadowTask.cancel(true);
                                    }
                                    if (getSensorShadowTask.getStatus() == AsyncTask.Status.RUNNING) {
                                        getSensorShadowTask.cancel(true);
                                    }
                                    if (subScribeAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                                        subScribeAsyncTask.cancel(true);
                                    }
                                } catch (Exception e) {
                                }

                            } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                MyLog.i("AWSIotMqttClientStatus :: ConnectionLost");
                                if (throwable != null) {
                                    MyLog.e("Connection error. >>> " + throwable);
                                }
                                isMqttConnected = false;
                                removeEvent(AWSEVENT.E_GET_SHADOW_MONITOR);
                                removeEvent(AWSEVENT.E_UPDATE_SENSOR_MONITOR);
                                sendEvent(AWSEVENT.E_STOP_GET_SHADOW_MONITOR);
                                sendEvent(AWSEVENT.E_STOP_UPDATE_SENSOR_MONITOR);

                                try {
                                    if (getStatusShadowTask.getStatus() == AsyncTask.Status.RUNNING) {
                                        getStatusShadowTask.cancel(true);
                                    }
                                    if (getSensorShadowTask.getStatus() == AsyncTask.Status.RUNNING) {
                                        getSensorShadowTask.cancel(true);
                                    }
                                    if (subScribeAsyncTask.getStatus() == AsyncTask.Status.RUNNING) {
                                        subScribeAsyncTask.cancel(true);
                                    }
                                } catch (Exception e) {
                                }

                            } else {
                                MyLog.i("AWSIotMqttClientStatus :: ERROR");
                                isMqttConnected = false;
                                sendEvent(AWSEVENT.E_MQTT_REMOVE_CALLBACK_MONITOR);
                                removeEvent(AWSEVENT.E_GET_SHADOW_MONITOR);
                                removeEvent(AWSEVENT.E_UPDATE_SENSOR_MONITOR);
                                sendEvent(AWSEVENT.E_STOP_GET_SHADOW_MONITOR);
                                sendEvent(AWSEVENT.E_STOP_UPDATE_SENSOR_MONITOR);

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
            isMqttConnected = false;
        }
    }

    private void getShadow(View view) {
        getShadows();
    }

    private void getShadows() {
        GetShadowTask getStatusShadowTask = new GetShadowTask(StaticDataSave.thingName);
        getStatusShadowTask.execute();
    }

    private void stopGetShadow() {
        try {
            if (getStatusShadowTask.getStatus() == AsyncTask.Status.RUNNING) {
                getStatusShadowTask.cancel(true);
            }
        } catch (Exception e) {
        }
    }

    private class GetShadowTask extends AsyncTask<Void, Void, AsyncTaskResult<String>> {

        private final String thingName;

        public GetShadowTask(String name) {
            thingName = name;
        }

        @Override
        protected AsyncTaskResult<String> doInBackground(Void... voids) {
            try {
                MyLog.i("=== GetShadowTask ===");
                GetThingShadowRequest getThingShadowRequest = new GetThingShadowRequest().withThingName(thingName);
                GetThingShadowResult result = AWSIoTDoorActivity.iotDataClient.getThingShadow(getThingShadowRequest);
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
                    doorlockStatusUpdated(result.getResult());
                }
            } else {
                MyLog.e("E / getShadowTask : " + result.getError());
            }
        }
    }

    private void getSendsorShadow(View view) {
        getSendsorShadows();
    }

    private void getSendsorShadows() {
        GetSensorShadowTask getSensorShadowTask = new GetSensorShadowTask(StaticDataSave.thingName);
        getSensorShadowTask.execute();
    }

    private void stopGetSensorShadow() {
        try {
            if (getSensorShadowTask.getStatus() == AsyncTask.Status.RUNNING) {
                getSensorShadowTask.cancel(true);
            } else {
            }
        } catch (Exception e) {
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
                GetThingShadowResult result = AWSIoTDoorActivity.iotDataClient.getThingShadow(getThingShadowRequest);
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

                UpdateThingShadowResult result = AWSIoTDoorActivity.iotDataClient.updateThingShadow(request);

                byte[] bytes = new byte[result.getPayload().remaining()];
                result.getPayload().get(bytes);
                String resultString = new String(bytes);
                return new AsyncTaskResult<String>(resultString);
            } catch (Exception e) {
                MyLog.e("updateShadowTask : " + e);
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

    private void doorlockStatusUpdated(String doorlockStatusState) {
        Gson gson = new Gson();
        DoorlockStatus ds = gson.fromJson(doorlockStatusState, DoorlockStatus.class);


        if (ds.state.reported.doorState != null) {
            MyLog.i(String.format("doorState : %s", ds.state.reported.doorState));
        }

        MyLog.i(String.format("doorStateChange :  %d", ds.state.reported.doorStateChange));
        MyLog.i(String.format("DoorOpenMode :  %s", ds.state.reported.DoorOpenMode));

        long batch_date = ds.timestamp;
        Date dt = new Date(batch_date * 1000);
        SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");

        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.doorStateFlag = StaticDataSave.saveData.getString(StaticDataSave.doorStateFlagKey, null);
        MyLog.i("StaticDataSave.doorStateFlag = " + StaticDataSave.doorStateFlag);

        if (ds.state.reported.doorState != null && ds.state.reported.openMethod != null) {
            if (ds.state.reported.doorState.equals("true")) {  //opened

                if (StaticDataSave.doorStateFlag.equals("false")) {

                    StaticDataSave.doorStateFlag = "true";
                    StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                    editor.putString(StaticDataSave.doorStateFlagKey, StaticDataSave.doorStateFlag);
                    editor.commit();
                }

            } else if (ds.state.reported.doorState.equals("false")) {  //closed
                StaticDataSave.doorStateFlag = "false";
                StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                editor.putString(StaticDataSave.doorStateFlagKey, StaticDataSave.doorStateFlag);
                editor.commit();

            }
        }

        //OTA update check
        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
        MyLog.i("ds.state.reported.OTAupdate = " + ds.state.reported.OTAupdate);
        if (ds.state.reported.OTAupdate == 0) {  //none
            StaticDataSave.existOTAupdateFlag = false;
            StaticDataSave.OTAupdateProgressFlag = false;
        } else if (ds.state.reported.OTAupdate == 1) {  // exist update
            StaticDataSave.existOTAupdateFlag = true;
            StaticDataSave.OTAupdateProgressFlag = false;
        } else if (ds.state.reported.OTAupdate == 2) {  // update progressing
            StaticDataSave.existOTAupdateFlag = false;
            StaticDataSave.OTAupdateProgressFlag = true;
        }
        MyLog.i("StaticDataSave.existOTAupdateFlag = " + StaticDataSave.existOTAupdateFlag);
        MyLog.i("StaticDataSave.OTAupdateProgressFlag = " + StaticDataSave.OTAupdateProgressFlag);

        editor.putBoolean(StaticDataSave.existOTAupdateFlagKey, StaticDataSave.existOTAupdateFlag);
        editor.putBoolean(StaticDataSave.OTAupdateProgressFlagKey, StaticDataSave.OTAupdateProgressFlag);
        editor.commit();
    }

    private void sensorStatusUpdated(String doorlockStatusState) {
        Gson gson = new Gson();
        DoorlockStatus ds = gson.fromJson(doorlockStatusState, DoorlockStatus.class);

        if (String.valueOf(ds.state.reported.temperature) != null) {
            MyLog.i(String.format("Temperature :  %f", ds.state.reported.temperature));

            int intTemperature = Math.round(ds.state.reported.temperature);

            if (intTemperature >= 100) {

            } else if (intTemperature < 100 && intTemperature >= 55) {

                SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
                sendNotification("Alert : High Temperature (" + sfd.format(new Date()) + ")", R.drawable.circle_yellow);

            } else {

            }
        }

        if (String.valueOf(ds.state.reported.battery) != null && AWSIoTDoorActivity.getInstanceMain() != null) {
            MyLog.i(String.format("Battery :  %f", ds.state.reported.battery));
            MyLog.i("calBattery(ds.state.reported.battery) = " + AWSIoTDoorActivity.getInstanceMain().calBattery(ds.state.reported.battery));

            if (AWSIoTDoorActivity.getInstanceMain().calBattery(ds.state.reported.battery) <= 10) {
                SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
                sendNotification("Alert : Low Battery (" + sfd.format(new Date()) + ")", R.drawable.circle_yellow);

            } else if (AWSIoTDoorActivity.getInstanceMain().calBattery(ds.state.reported.battery) == 200) {

            } else {

            }
        }

    }

    private void disconnectMqtt() {
        MyLog.i("=== disconnectMqtt() ===");
        if (AWSIoTDoorActivity.mqttManager != null) {
            try {
                AWSIoTDoorActivity.mqttManager.disconnect();
            } catch (Exception e) {
                MyLog.e("Disconnect error. >>> " + e);
            }
        }
    }

    public void stopService() {
        MyLog.i("=== stopService() ===");
        for (int i = 1; i < 9999; i++) {
            cancelNotification(mContext, i);
        }
        stopSelf();
        exit(0);
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
                } else if (status.equals("Wi-Fi enabled") || status.equals("Mobile data enabled")) {
                    MyLog.i("Wi-Fi enabled or Mobile data enabled");
                    if (StaticDataSave.thingName != null) {
                        initMqtt();

                        new Thread() {
                            public void run() {
                                if (AWSIoTDoorActivity.credentialsProvider != null) {
                                    String identityId = AWSIoTDoorActivity.credentialsProvider.getIdentityId();
                                    MyLog.i("my ID is " + identityId);
                                }
                            }
                        }.start();

                        if (AWSIoTDoorActivity.iotDataClient == null) {
                            AWSIoTDoorActivity.iotDataClient = new AWSIotDataClient(AWSIoTDoorActivity.credentialsProvider);
                            String iotDataEndpoint = awsConfig.CUSTOMER_SPECIFIC_ENDPOINT;
                            AWSIoTDoorActivity.iotDataClient.setEndpoint(iotDataEndpoint);
                        }
                    }
                }
            }
        }
    }

    private static void cancelNotification(Context ctx, int notifyId) {
        String s = Context.NOTIFICATION_SERVICE;
        NotificationManager mNM = (NotificationManager) ctx.getSystemService(s);
        mNM.cancel(notifyId);
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
                    AWSIoTDoorActivity.mqttManager.subscribeToTopic(subTopic, AWSIotMqttQos.QOS0,
                            new AWSIotMqttNewMessageCallback() {
                                @Override
                                public void onMessageArrived(final String subTopic, final byte[] data) {
                                    ThreadUtils.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                final String message = new String(data, "UTF-8");
                                                MyLog.i("=================================");
                                                MyLog.i("Message arrived:");
                                                MyLog.i("   subTopic: " + subTopic);
                                                MyLog.i(" subMessage: " + message);
                                                MyLog.i("=================================");

                                                messageReceived = true;
                                                isDeviceConnected = true;

                                                if (message.equals(DEVICE_CONTROL_OPEN_RESPONSE_MESSAGE)) {
                                                    messageReceived = true;
                                                    sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);
                                                    SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
                                                    sendNotification("Door was opened at " + sfd.format(new Date()), R.drawable.circle_red);
                                                } else if (message.equals(DEVICE_CONTROL_CLOSE_RESPONSE_MESSAGE)) {
                                                    messageReceived = true;
                                                    sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);
                                                    SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
                                                    sendNotification("Door was closed at " + sfd.format(new Date()), R.drawable.circle_green);
                                                } else if (message.equals(DEVICE_CONTROL_BATTERY_ALERT_MESSAGE)) {
                                                    SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
                                                    sendNotification("Alert : Low Battery (" + sfd.format(new Date()) + ")", R.drawable.circle_yellow);
                                                } else if (message.equals(DEVICE_CONTROL_TEMPERATURE_ALERT_MESSAGE)) {
                                                    SimpleDateFormat sfd = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss aaa");
                                                    sendNotification("Alert : High Temperature (" + sfd.format(new Date()) + ")", R.drawable.circle_yellow);
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

    @SuppressLint("ForegroundServiceType")
    void startForegroundService() {

        Intent notifyIntent = new Intent(this, AWSIoTDoorActivity.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentHide = new Intent(this, StopServiceReceiver.class);
        PendingIntent pendingIntentHide = PendingIntent.getBroadcast(this, 1, intentHide, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "doorlock_service_channel";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Doorlock Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(this);
        }
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.renesas_ic_launcher);
        builder.setLargeIcon(largeIcon)
                .setSmallIcon(R.drawable.outline_visibility_white_24)
                .setTicker("Renesas AWSIoT Doorlock")
                .setContentText("Renesas AWSIoT Doorlock app is monitoring the doorlock.")
                .setColor(getResources().getColor(R.color.blue3))
                .addAction(R.drawable.outline_arrow_forward_white_24, "open app", notifyPendingIntent)
                .addAction(R.drawable.outline_block_white_24, "stop", pendingIntentHide);

        startForeground(1, builder.build());
    }

    private void sendNotification(String msg, int icon) {

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder;
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "doorlock_service_channel";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Doorlock Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(this);
        }
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.renesas_ic_launcher);
        builder.setLargeIcon(largeIcon)
                .setSmallIcon(icon)
                .setTicker("Renesas AWSIoT Doorlock")
                .setContentText(msg)
                .setColor(getResources().getColor(R.color.blue3));


        NOTIFICATION_ID = NOTIFICATION_ID + 1;

        notificationManager.notify(NOTIFICATION_ID, builder.build());

    }

}
