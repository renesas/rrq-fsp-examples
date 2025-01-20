package com.renesas.wifi.awsiot.setting.door;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.renesas.wifi.activity.MainActivity;
import com.renesas.wifi.R;
import com.renesas.wifi.awsiot.AWSEVENT;
import com.renesas.wifi.awsiot.AWSIoTDoorActivity;
import com.renesas.wifi.awsiot.MonitorService;
import com.renesas.wifi.buildOption;
import com.renesas.wifi.util.CustomToast;
import com.renesas.wifi.util.MyLog;
import com.renesas.wifi.util.StaticDataSave;
import com.renesas.wifi.util.WIFIUtil;

public class DoorFunctionSetFragment extends Fragment {

    public static final String ARG_ITEM_ID = "functionSet";

    Button buttonName;
    EditText editTextName;

    TextView txtBattCheck;
    Button buttonBatt;
    TextView txtUpdate;
    Button buttonOtaUpdate;
    public static TextView badge_notification;
    EditText editTextIP;
    EditText editTextPort;
    EditText editTextProductId;
    Button buttonConnect;
    Button buttonDisconnect;

    Button buttonInit;

    AlertDialog alertDialog;


    private static final String APP_CONTROL_OTA_MESSAGE = "confirmOTA";

    CustomToast customToast = null;
    Handler mHandler_toast;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        customToast = new CustomToast(AWSIoTDoorActivity.activity);
        mHandler_toast = new Handler();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.awsiot_fragment_functionset, container, false);

        initFunctionSetResource(view);

        buttonName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StaticDataSave.userName = editTextName.getText().toString();

                StaticDataSave.saveData = AWSIoTDoorActivity.activity.getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                editor.putString(StaticDataSave.userNameKey, StaticDataSave.userName);
                editor.commit();
                mHandler_toast.post(new ToastRunnable("User name is changed"));
            }
        });

        buttonBatt.setOnClickListener(new Switch.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyLog.i("=== notify swich ===");
                //get from battery level read api and replace to string
                String battLevel = "55%";

                String showText = getString(R.string.current_batt) + " " + battLevel;
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(AWSIoTDoorActivity.activity);
                alertBuilder.setTitle(R.string.title_batt)
                        .setMessage(showText)
                        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                alertDialog.dismiss();
                            }
                        });
                alertDialog = alertBuilder.create();
                alertDialog.show();
            }
        });

        buttonOtaUpdate.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyLog.i("=== update switch ===");

                MyLog.i("AWSIoTDoorActivity.isDeviceConnected = "+ AWSIoTDoorActivity.isDeviceConnected);
                if (AWSIoTDoorActivity.isDeviceConnected) {

                    MyLog.i("StaticDataSave.existOTAupdateFlag = "+StaticDataSave.existOTAupdateFlag);
                    if (StaticDataSave.existOTAupdateFlag == true) {

                        StaticDataSave.saveData = AWSIoTDoorActivity.activity.getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);
                        final String pubTopic = StaticDataSave.thingName+"/"+"AppControl";
                        final String msg = APP_CONTROL_OTA_MESSAGE;

                        try {
                            AWSIoTDoorActivity.getInstanceMain().mqttManager.publishString(msg, pubTopic, AWSIotMqttQos.QOS0);
                            MyLog.i("=================================");
                            MyLog.i("Message published:");
                            MyLog.i("   Topic: " + pubTopic);
                            MyLog.i(" Message: " + msg);
                            MyLog.i("=================================");

                            MyLog.i("StaticDataSave.OTAupdateProgressFlag = "+StaticDataSave.OTAupdateProgressFlag);
                            AWSIoTDoorActivity.getInstanceMain().sendEvent(AWSEVENT.E_GET_SHADOW);

                        } catch (Exception e) {
                            MyLog.e("Publish error. >>> " + e);
                        }
                    } else {
                        mHandler_toast.post(new ToastRunnable(AWSIoTDoorActivity.activity.getResources().getString(R.string.nothing_update)));

                    }
                } else {
                    mHandler_toast.post(new ToastRunnable(AWSIoTDoorActivity.activity.getResources().getString(R.string.connect_fail)));
                }
            }
        });

        buttonConnect.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = editTextIP.getText().toString();
                String port = editTextPort.getText().toString();
                String productId = editTextProductId.getText().toString();

                MyLog.i("ip = "+ip+", port = "+port+", productId = "+productId);

                // save set server info, if you don't need remove it.
                StaticDataSave.saveData = AWSIoTDoorActivity.activity.getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                editor.putString(StaticDataSave.serverIPKey, ip);
                editor.putString(StaticDataSave.serverPortKey, port);
                editor.putString(StaticDataSave.serverProductIDKey, productId);
                editor.commit();

            }
        });

        buttonDisconnect.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                // add disconnction api
            }
        });

        buttonInit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (StaticDataSave.thingName != null) {
                    if (AWSIoTDoorActivity.getInstanceMain() != null) {
                        AWSIoTDoorActivity.isDeviceConnected = false;
                        AWSIoTDoorActivity.isMqttConnected = false;
                        AWSIoTDoorActivity.getInstanceMain().publishCount = 0;
                        AWSIoTDoorActivity.getInstanceMain().removeEvent(AWSEVENT.E_DEVICE_CONNECT_TIMEOUT);
                        AWSIoTDoorActivity.getInstanceMain().sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);

                        AWSIoTDoorActivity.getInstanceMain().removeEvent(AWSEVENT.E_GET_SHADOW);
                        AWSIoTDoorActivity.getInstanceMain().removeEvent(AWSEVENT.E_UPDATE_SENSOR);
                        AWSIoTDoorActivity.getInstanceMain().sendEvent(AWSEVENT.E_STOP_GET_SHADOW);
                        AWSIoTDoorActivity.getInstanceMain().sendEvent(AWSEVENT.E_STOP_UPDATE_SENSOR);

                        AWSIoTDoorActivity.getInstanceMain().sendEvent(AWSEVENT.E_DEVICE_CONNECT_REMOVE_CALLBACK);
                        AWSIoTDoorActivity.getInstanceMain().sendEvent(AWSEVENT.E_MQTT_REMOVE_CALLBACK);
                        AWSIoTDoorActivity.getInstanceMain().DoorHandler.removeCallbacksAndMessages(null);
                        AWSIoTDoorActivity.getInstanceMain().disconnectMqtt();

                        AWSIoTDoorActivity.mIotAndroidClient = null;
                        AWSIoTDoorActivity.iotDataClient = null;
                        AWSIoTDoorActivity.clientKeyStore = null;

                    }
                }

                MyLog.i("WIFIUtil.isConnectWIFI(DoorSettingActivity.settingContext) = "+ WIFIUtil.isConnectWIFI(DoorSettingActivity.settingContext));
                if (WIFIUtil.isConnectWIFI(DoorSettingActivity.settingContext)) {
                    String ssid = getCurrentSsid(DoorSettingActivity.settingContext);
                    MyLog.i("getCurrentSsid = "+ssid);
                    WifiManager wifiManager = (WifiManager) DoorSettingActivity.settingContext.getSystemService(Context.WIFI_SERVICE);
                    if (ssid != null && ssid.contains(buildOption.WIFI_SCAN_FILTER1)) {
                        int networkId = wifiManager.getConnectionInfo().getNetworkId();
                        wifiManager.removeNetwork(networkId);
                        wifiManager.saveConfiguration();
                        wifiManager.disconnect();
                        wifiManager.disableNetwork(networkId);
                    }
                }


                if (AWSIoTDoorActivity.getInstanceMain().isRunning()) {
                    if (MonitorService.getInstance() != null) {
                        if (StaticDataSave.thingName != null) {
                            MonitorService.getInstance().stopService();
                        }
                    }
                }

                StaticDataSave.saveData = AWSIoTDoorActivity.activity.getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                editor.putBoolean(StaticDataSave.readyFlagKey, false);
                editor.putString(StaticDataSave.thingNameKey, null);
                editor.putString(StaticDataSave.regionKey, null);
                editor.putString(StaticDataSave.userNameKey, null);
                editor.putString(StaticDataSave.cognitoPoolIdKey, null);
                editor.putString(StaticDataSave.bucketNameKey, null);
                editor.commit();

                try {
                    if (AWSIotKeystoreHelper.isKeystorePresent(AWSIoTDoorActivity.keystorePath, AWSIoTDoorActivity.keystoreName)) {
                        AWSIotKeystoreHelper.deleteKeystoreAlias(AWSIoTDoorActivity.certificateId,
                                AWSIoTDoorActivity.keystorePath, AWSIoTDoorActivity.keystoreName, AWSIoTDoorActivity.keystorePassword);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Intent main = new Intent(DoorSettingActivity.settingContext, MainActivity.class);
                startActivity(main);
                DoorSettingActivity.getInstanceSetting().finish();
                AWSIoTDoorActivity.getInstanceMain().finish();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (StaticDataSave.thingName != null && AWSIoTDoorActivity.isMqttConnected) {
            AWSIoTDoorActivity.getInstanceMain().sendEvent(AWSEVENT.E_GET_SHADOW);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (StaticDataSave.thingName != null && AWSIoTDoorActivity.isMqttConnected) {
            AWSIoTDoorActivity.getInstanceMain().removeEvent(AWSEVENT.E_GET_SHADOW);
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

    private static String getCurrentSsid(Context context) {
        String ssid = null;
        String level = null;
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                ssid = connectionInfo.getSSID();
                ssid = ssid.replaceAll("\"","");
            }
        }
        return ssid;
    }

    private void initFunctionSetResource(View view) {

        StaticDataSave.saveData = AWSIoTDoorActivity.activity.getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        buttonName = (Button) view.findViewById(R.id.buttonName);
        editTextName = (EditText) view.findViewById(R.id.editTextName);
        txtBattCheck = (TextView)view.findViewById(R.id.textBatt);
        buttonBatt = (Button)view.findViewById(R.id.buttonBatt) ;
        txtUpdate = (TextView)view.findViewById(R.id.textUpdate);
        buttonOtaUpdate = (Button)view.findViewById(R.id.button_update);
        badge_notification = (TextView) view.findViewById(R.id.badge_notification);
        StaticDataSave.existOTAupdateFlag = StaticDataSave.saveData.getBoolean(StaticDataSave.existOTAupdateFlagKey, false);
        MyLog.i("StaticDataSave.existOTAupdateFlag = "+StaticDataSave.existOTAupdateFlag);
        if (buttonOtaUpdate != null) {
            if (StaticDataSave.existOTAupdateFlag == true) {
                badge_notification.setVisibility(View.VISIBLE);
            } else {
                badge_notification.setVisibility(View.INVISIBLE);
            }
        }

        editTextIP = (EditText)view.findViewById(R.id.editTxt_ip);
        editTextPort = (EditText)view.findViewById(R.id.editTxt_port);
        editTextProductId = (EditText)view.findViewById(R.id.editTxt_productId);
        buttonConnect = (Button)view.findViewById(R.id.button_conn);
        buttonDisconnect = (Button)view.findViewById(R.id.button_diconn);

        // load last set server info, if you don't need remove it.
        StaticDataSave.serverIP = StaticDataSave.saveData.getString(StaticDataSave.serverIPKey, null);
        StaticDataSave.serverPort = StaticDataSave.saveData.getString(StaticDataSave.serverPortKey, null);
        StaticDataSave.serverProductID = StaticDataSave.saveData.getString(StaticDataSave.serverProductIDKey, null);
        MyLog.i("load IP = "+StaticDataSave.serverIP);
        MyLog.i("load Port = "+StaticDataSave.serverPort);
        MyLog.i("load Product ID = "+StaticDataSave.serverProductID);
        editTextIP.setText(StaticDataSave.serverIP);
        editTextPort.setText(StaticDataSave.serverPort);
        editTextProductId.setText(StaticDataSave.serverProductID);

        buttonInit = (Button) view.findViewById(R.id.buttonInit);
    }

    private class ToastRunnable implements Runnable {
        String mText;
        public ToastRunnable(String text) {
            mText = text;
        } @Override public void run(){
            if (customToast != null) {
                customToast.showToast(AWSIoTDoorActivity.activity, mText, Toast.LENGTH_SHORT);
            }
        }
    }

}
