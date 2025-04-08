/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.renesas.wifi.DA16600.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.renesas.wifi.DA16600.dialog.AddWiFiDialog;
import com.renesas.wifi.DA16600.BluetoothLeService;
import com.renesas.wifi.DA16600.InputPasswordDialog;
import com.renesas.wifi.DA16600.InputSsidPasswordDialog;
import com.renesas.wifi.DA16600.MyGattAttributes;
import com.renesas.wifi.DA16600.dialog.EnterpriseDialog;
import com.renesas.wifi.R;
import com.renesas.wifi.DA16600.adapter.ap.APListViewAdapter;
import com.renesas.wifi.DA16600.adapter.ap.APRowItem;
import com.renesas.wifi.activity.BaseActivity;
import com.renesas.wifi.activity.MainActivity;
import com.renesas.wifi.util.CustomToast;
import com.renesas.wifi.util.FButton;
import com.renesas.wifi.util.MyLog;
import com.renesas.wifi.util.OnSingleClickListener;
import com.renesas.wifi.util.StaticDataSave;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DeviceControlActivity extends BaseActivity implements TextWatcher {

    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static Context mContext;

    //[[add in v2.4.15
    private boolean mShouldUnbind = false;
    boolean mIsBound = false;
    //]]

    //handler
    public static DeviceControlHandler mHandler;

    //service
    public static BluetoothLeService mBluetoothLeService;

    //data
    public ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private ListView listView;
    private APListViewAdapter adapter;
    private ArrayList<APRowItem> apRowItems;
    public ArrayList<String> ssidList;
    public ArrayList<Integer> securityList;
    public ArrayList<Integer> signalList;
    private String[] ssid;
    private String[] stringSecurity;
    private Integer[] signalBar;
    private boolean[] isSecurity;
    private Integer[] secMode;
    private Integer[] level;

    //UI resources
    private CustomToast customToast;
    private TextView tv_deviceName;
    private TextView mConnectionState;
    private ImageView iv_back;
    private FButton btn_bleConnect;
    public FButton btn_apScan;
    public FButton btn_hiddenWiFi;
    public FButton btn_command;
    public FButton btn_reset;
    public FButton btn_connect;
    public RelativeLayout rl_scanAP;
    private LinearLayout ll_progressScanning;
    private ProgressBar progressScanning;
    private TextView tv_progress;
    private TextView tv_noList;
    public LinearLayout ll_selectAP;
    private LinearLayout ll_sendCommand;
    private EditText et_rawCommand1;
    private EditText et_rawCommand2;
    private EditText et_command;
    public FButton btn_send;
    private AlertDialog txApInfoFailDialog;
    private AlertDialog ApWorngPwdDialog;
    private AlertDialog apFailDialog;
    private AlertDialog dnsFailServerFailDialog;
    private AlertDialog dnsFailServerOkDialog;
    private AlertDialog noUrlPingFailDialog;
    private AlertDialog noUrlPingOkDialog;
    private AlertDialog dnsOkPingFailServerOkDialog;
    private AlertDialog dnsOkPingOkDialog;
    private AlertDialog dnsOkPingFailServerFailDialog;
    private ProgressDialog scanningDialog = null;
    private ProgressDialog checkingDialog = null;
    private AlertDialog cmdFailDialog;
    private ProgressDialog receiveAwsThingNameDialog = null;
    private ProgressDialog receiveAzureThingNameDialog = null;
    private ProgressDialog connectingDialog = null;
    private ProgressDialog registeringDialog = null;

    //Characteristics
    public static BluetoothGattCharacteristic WIFI_SVC;
    public static BluetoothGattCharacteristic WIFI_SVC_WFCMD;
    public static BluetoothGattCharacteristic WIFI_SVC_WFACT_RES;
    public static BluetoothGattCharacteristic WIFI_SVC_APSCAN_RES;
    public static BluetoothGattCharacteristic WIFI_SVC_PROV_DATA;
    public static BluetoothGattCharacteristic WIFI_SVC_AWS_DATA;
    public static BluetoothGattCharacteristic WIFI_SVC_AZURE_DATA;
    public static BluetoothGattCharacteristic GBG_SVC;
    public static BluetoothGattCharacteristic GBG_SVC_CHAR;

    //flag
    private boolean mConnected = false;
    private boolean isRefreshed = false;
    private boolean refresh = true;
    private static int retry = 1;

    //contant
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    public final static int SCAN_TIMEOUT_TIME = 10000; //msec
    public static int dhcp = 1;

    public static DeviceControlActivity instance;
    public static DeviceControlActivity getInstance() {
        return instance;
    }

    //handler event
    public static class HandleMsg {
        public static final int E_BLE_NETWORK_SCAN_TIMEOUT = 0;
        public static final int E_BLE_CMD_TIMEOUT = 1;
        public static final int E_SHOW_REGISTERING_DIALOG = 2;
        public static final int E_DISMISS_REGISTERING_DIALOG = 3;
    }

    /**
     ****************************************************************************************
     * @brief Handler class for Device Control activity
     * @param
     * @return none
     ****************************************************************************************
     */
    private static final class DeviceControlHandler extends Handler
    {

        private final WeakReference<DeviceControlActivity> ref;

        public DeviceControlHandler(DeviceControlActivity act)
        {
            ref = new WeakReference<>(act);
        }

        @Override
        public void handleMessage(Message msg) {
            final DeviceControlActivity act = ref.get();

            if (act != null)
            {
                switch (msg.what)
                {
                    case HandleMsg.E_BLE_NETWORK_SCAN_TIMEOUT:
                        MyLog.i("E_BLE_NETWORK_SCAN_TIMEOUT");
                        act.ll_progressScanning.setVisibility(View.INVISIBLE);
                        act.dismissScanningDialog();
                        act.dismissScanFailDialog();
                        act.listView.setVisibility(View.INVISIBLE);
                        act.ll_selectAP.setVisibility(View.INVISIBLE);
                        act.tv_noList.setVisibility(View.VISIBLE);
                        break;

                    case HandleMsg.E_BLE_CMD_TIMEOUT:
                        MyLog.i(">> retry = "+retry);
                        if (retry == 1) {
                            act.sendNetworkinfo(StaticDataSave.pingAddress, StaticDataSave.svrAddress, StaticDataSave.svrPort, StaticDataSave.svrUrl);
                            mHandler.sendEmptyMessageDelayed(HandleMsg.E_BLE_CMD_TIMEOUT, 3000);
                            retry--;
                        } else if (retry == 0) {
                            MyLog.i("E_BLE_CMD_TIMEOUT");
                            act.showCmdFailDialog();
                        }
                        break;

                    case HandleMsg.E_SHOW_REGISTERING_DIALOG:
                        MyLog.i("E_SHOW_REGISTERING_DIALOG");
                        if (act.registeringDialog == null && act.mContext != null) {
                            act.registeringDialog = new ProgressDialog(act.mContext, android.app.AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                            act.registeringDialog.setTitle("Registering device");
                            act.registeringDialog.setMessage("The device is being registered with the AWS server.\n" +
                                    "It takes about 60 seconds.");
                            act.registeringDialog.show();

                            int displayWidth = 0;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                WindowMetrics windowMetrics = act.getWindowManager().getCurrentWindowMetrics();
                                Insets insets = windowMetrics.getWindowInsets()
                                        .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
                                displayWidth = windowMetrics.getBounds().width() - insets.left - insets.right;
                            } else {
                                DisplayMetrics displayMetrics = new DisplayMetrics();
                                act.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                                displayWidth = displayMetrics.widthPixels;
                            }

                            WindowManager.LayoutParams params = act.registeringDialog.getWindow().getAttributes();
                            int dialogWindowWidth = (int) (displayWidth * 0.9f);
                            params.width = dialogWindowWidth;
                            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                            act.registeringDialog.getWindow().setAttributes((WindowManager.LayoutParams) params);
                            act.registeringDialog.setCanceledOnTouchOutside(false);
                            act.registeringDialog.setCancelable(false);

                        }
                        break;

                    case HandleMsg.E_DISMISS_REGISTERING_DIALOG:
                        MyLog.i("E_DISMISS_REGISTERING_DIALOG");
                        if (act.registeringDialog != null) {
                            act.registeringDialog.dismiss();
                            act.registeringDialog = null;
                        }
                        break;

                    default:
                        break;
                }
            }
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            MyLog.i( "onServiceConnected");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                MyLog.e( "Unable to initialize Bluetooth");
                finish();
            }

            mBluetoothLeService.connect(StaticDataSave.mDeviceAddress);

            //[[add in v2.4.15
            mIsBound = true;
            //]]
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            MyLog.i( "onServiceDisconnected");
            mBluetoothLeService = null;
            //[[add in v2.4.15
            mIsBound = false;
            //]]
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            MyLog.i("ACTION: " + action);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                MyLog.i("BluetoothLeService.ACTION_GATT_CONNECTED");
                mConnected = true;
                updateConnectionState(R.string.connected);
                btn_bleConnect.setText("DISCONNECT");

                dismissConnectingDialog();

                btn_bleConnect.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //[[change in v2.4.15
                        //mBluetoothLeService.disconnect();
                        mBluetoothLeService.mBluetoothGatt.disconnect();
                        //]]
                    }
                });

                mConnectionState.setTextColor(getResources().getColor(R.color.fbutton_color_green_sea));

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                MyLog.i("BluetoothLeService.ACTION_GATT_DISCONNECTED");
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                btn_bleConnect.setText("CONNECT");
                btn_bleConnect.setOnClickListener(new OnSingleClickListener() {
                    @Override
                    public void onSingleClick(View v) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showConnectingDialog();
                                mBluetoothLeService.connect(StaticDataSave.mDeviceAddress);
                            }
                        });

                    }
                });
                mConnectionState.setTextColor(getResources().getColor(R.color.red));

                if (btn_apScan != null) {
                    btn_apScan.setEnabled(false);
                    btn_apScan.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
                }

                if (btn_hiddenWiFi != null) {
                    btn_hiddenWiFi.setEnabled(false);
                    btn_hiddenWiFi.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
                }

                if (btn_command != null) {
                    btn_command.setEnabled(false);
                    btn_command.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
                }

                if (btn_reset != null) {
                    btn_reset.setEnabled(false);
                    btn_reset.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
                }

                clearUI();
                initValue();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {

                MyLog.i("BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED");

                getGattServices(mBluetoothLeService.getSupportedGattServices());

                if (btn_apScan != null) {

                }

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String value = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                if (StaticDataSave.device.equals("RRQ61400")) {
                    if(value == null) {
                        MyLog.e( "value is null");
                    } else {
                        MyLog.i("data = " + value);
                        //[[change in v2.4.17
                        /*if (value.equals("100")) {
                            mBluetoothLeService.readCharacteristic(DeviceControlActivity.WIFI_SVC_PROV_DATA);
                        } else {
                            mBluetoothLeService.readCharacteristic(DeviceControlActivity.WIFI_SVC_APSCAN_RES);
                        }*/
                        if (value.equals("204")) {
                            mBluetoothLeService.readCharacteristic(DeviceControlActivity.WIFI_SVC_APSCAN_RES);
                        } else if (value.equals("205")) {
                            mBluetoothLeService.readCharacteristic(DeviceControlActivity.WIFI_SVC_PROV_DATA);
                        } else if (value.equals("206")) {
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            mBluetoothLeService.readCharacteristic(DeviceControlActivity.WIFI_SVC_AWS_DATA);
                        }
                        //]]
                    }
                } else {
                    if(value != null) {
                        MyLog.i("data = "+value.toString());
                    }
                    else {
                        MyLog.e( "value = null");
                    }
                }
            }

        }
    };

    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();

                        if (characteristic.getUuid().equals(UUID.fromString(MyGattAttributes.WIFI_SVC_UUID))){
                            MyLog.d("== characteristic.getUuid() = WIFI_SVC_UUID ==");

                        }
                        if (characteristic.getUuid().equals(UUID.fromString(MyGattAttributes.WIFI_SVC_WFCMD_UUID))){
                            MyLog.d("== characteristic.getUuid() = WIFI_SVC_WFCMD_UUID ==");

                        }
                        if (characteristic.getUuid().equals(UUID.fromString(MyGattAttributes.WIFI_SVC_WFACT_RES_UUID))){
                            MyLog.d("== characteristic.getUuid() = WIFI_SVC_WFACT_RES_UUID ==");
                        }

                        if (characteristic.getUuid().equals(UUID.fromString(MyGattAttributes.WIFI_SVC_APSCAN_RES_UUID))){
                            MyLog.d("== characteristic.getUuid() = WIFI_SVC_APSCAN_RES_UUID ==");

                        }

                        if (characteristic.getUuid().equals(UUID.fromString(MyGattAttributes.WIFI_SVC_PROV_DATA_UUID))){
                            MyLog.d("== characteristic.getUuid() = WIFI_SVC_PROV_DATA_UUID ==");
                        }

                        if (characteristic.getUuid().equals(UUID.fromString(MyGattAttributes.WIFI_SVC_AWS_DATA_UUID))){
                            MyLog.d("== characteristic.getUuid() = WIFI_SVC_AWS_DATA_UUID ==");
                        }

                        if (characteristic.getUuid().equals(UUID.fromString(MyGattAttributes.WIFI_SVC_AZURE_DATA_UUID))){
                            MyLog.d("== characteristic.getUuid() = WIFI_SVC_AZURE_DATA_UUID ==");
                        }

                        if (characteristic.getUuid().equals(UUID.fromString(MyGattAttributes.GBG_SVC_UUID))){
                            MyLog.d("== characteristic.getUuid() = GBG_SVC_UUID ==");
                        }
                        if (characteristic.getUuid().equals(UUID.fromString(MyGattAttributes.GBG_CHAR_UUID))){
                            MyLog.d("== characteristic.getUuid() = GBG_CHAR_UUID ==");
                        }

                        return true;
                    }
                    return false;
                }
            };


    private void clearUI() {
        rl_scanAP.setVisibility(View.INVISIBLE);
        ll_selectAP.setVisibility(View.INVISIBLE);
        ll_sendCommand.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.da16600_activity_device_control);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        instance = this;
        mContext = this;
        mHandler = new DeviceControlHandler(this);
        customToast = new CustomToast(mContext);

        ((TextView) findViewById(R.id.device_address)).setText(StaticDataSave.mDeviceAddress);

        mConnectionState = (TextView) findViewById(R.id.connection_state);

        iv_back = (ImageView) findViewById(R.id.iv_back);
        iv_back.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                onBackPressed();
            }
        });

        tv_deviceName = (TextView) findViewById(R.id.tv_deviceName);
        tv_deviceName.setText(StaticDataSave.mDeviceName);

        btn_bleConnect = (FButton) findViewById(R.id.btn_bleConnect);

        rl_scanAP = (RelativeLayout) findViewById(R.id.rl_scanAP);
        rl_scanAP.setVisibility(View.VISIBLE);

        ll_progressScanning = (LinearLayout) findViewById(R.id.ll_progressScanning);
        progressScanning = (ProgressBar) findViewById(R.id.progressScanning);
        ll_progressScanning.setVisibility(View.INVISIBLE);

        tv_progress = (TextView) findViewById(R.id.tv_progress);

        tv_noList = (TextView) findViewById(R.id.tv_noList);
        tv_noList.setVisibility(View.INVISIBLE);

        listView = (ListView) findViewById(R.id.network_wifi_list);
        listView.setVisibility(View.INVISIBLE);

        ll_selectAP = (LinearLayout) findViewById(R.id.ll_selectAP);
        ll_selectAP.setVisibility(View.INVISIBLE);

        et_rawCommand1 = (EditText) findViewById(R.id.et_rawCommand1);
        et_rawCommand1.setMovementMethod(new ScrollingMovementMethod());

        et_rawCommand2 = (EditText) findViewById(R.id.et_rawCommand2);
        et_rawCommand2.setMovementMethod(new ScrollingMovementMethod());

        final SoftKeyboardDectectorView softKeyboardDecector = new SoftKeyboardDectectorView(this);
        addContentView(softKeyboardDecector, new LinearLayout.LayoutParams(-1, -1));
        softKeyboardDecector.setOnShownKeyboard(new SoftKeyboardDectectorView.OnShownKeyboardListener() {
            @Override
            public void onShowSoftKeyboard() {

            }
        });
        softKeyboardDecector.setOnHiddenKeyboard(new SoftKeyboardDectectorView.OnHiddenKeyboardListener() {
            @Override
            public void onHiddenSoftKeyboard() {

            }
        });


        btn_connect = (FButton) findViewById(R.id.btn_connect);
        btn_connect.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                MyLog.i("btn_connect.setOnClickListener");
                JSONArray jsonArray1 = null;
                JSONArray jsonArray2 = null;
                try {

                    MyLog.i("et_rawCommand1.getText() = "+et_rawCommand1.getText());
                    MyLog.i("et_rawCommand2.getText() = "+et_rawCommand2.getText());

                    jsonArray1 = new JSONArray("["+et_rawCommand1.getText()+"]");
                    JSONObject jsonObj1 = jsonArray1.getJSONObject(0);
                    StaticDataSave.pingAddress = jsonObj1.getString("ping_addr");
                    StaticDataSave.svrAddress = jsonObj1.getString("svr_addr");
                    StaticDataSave.svrPort = jsonObj1.getInt("svr_port");
                    StaticDataSave.svrUrl = jsonObj1.getString("customer_svr_url");

                    jsonArray2 = new JSONArray("["+et_rawCommand2.getText()+"]");
                    JSONObject jsonObj2 = jsonArray2.getJSONObject(0);
                    StaticDataSave.networkSSID = jsonObj2.getString("SSID");
                    StaticDataSave.networkSecurityNum = jsonObj2.getInt("security_type");
                    StaticDataSave.networkPassword = jsonObj2.getString("password");
                    StaticDataSave.isHidden = jsonObj2.getInt("isHidden");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                mHandler.sendEmptyMessageDelayed(HandleMsg.E_BLE_CMD_TIMEOUT, 3000);

                sendNetworkinfo(StaticDataSave.pingAddress, StaticDataSave.svrAddress, StaticDataSave.svrPort, StaticDataSave.svrUrl);
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (apRowItems.size() > 0) {

                    StaticDataSave.networkSSID = apRowItems.get(position).getSSID();
                    MyLog.i("ssid = " + StaticDataSave.networkSSID);

                    StaticDataSave.networkSecurityNum = apRowItems.get(position).getSecurityType();

                    MyLog.i("StaticDataSave.networkSecurityNum = " + apRowItems.get(position).getSecurityType());

                    //[[add in v2.4.18
                    String displaySSID;
                    displaySSID = StaticDataSave.networkSSID.replace("\\\\", "\\");
                    displaySSID = StaticDataSave.networkSSID.replace("\\r", "\r");
                    displaySSID = StaticDataSave.networkSSID.replace("\\b", "\b");
                    displaySSID = StaticDataSave.networkSSID.replace("\\f", "\f");
                    displaySSID = StaticDataSave.networkSSID.replace("\\t", "\t");
                    displaySSID = StaticDataSave.networkSSID.replace("\\n", "\n");
                    displaySSID = StaticDataSave.networkSSID.replace("\\\"", "\"");
                    displaySSID = StaticDataSave.networkSSID.replace("\\\'", "\'");
                    MyLog.i(">> displaySSID = "+displaySSID);
                    //]]

                    if (btn_connect != null) {
                        //[[change in v2.4.18
                        //btn_connect.setText("Connect to "+StaticDataSave.networkSSID);
                        btn_connect.setText("Connect to "+displaySSID);
                        //]]
                    }

                    /*
                        typedef enum
                        {
                            eWiFiSecurityOpen = 0,             	///< Open - No Security
                                    eWiFiSecurityWEP,                  	///< WEP
                                    eWiFiSecurityWPA,                  	///< WPA
                                    eWiFiSecurityWPA2,                 	///< WPA2 (RSN)
                                    eWiFiSecurityWPA_AUTO,              	///< WPA & WPA2 (RSN)
                                    eWiFiSecurityOWE,      			///< WPA3 OWE
                                    eWiFiSecuritySAE,      			///< WPA3 SAE
                                    eWiFiSecurityRSN_SAE,        		///< WPA2 (RSN) & WPA3 SAE
                                    eWiFiSecurityWPA_EAP,               	///< WPA Enterprise
                                    eWiFiSecurityWPA2_EAP,              	///< WPA2 Enterprise
                                    eWiFiSecurityWPA_AUTO_EAP,          	///< WPA & WPA2 Enterprise
                                    eWiFiSecurityWPA3_EAP,              	///< WPA3 Enterprise
                                    eWiFiSecurityWPA2_AUTO_EAP,        	 ///< WPA2 & WPA3 Enterprise
                                    eWiFiSecurityWPA3_EAP_192B,         	///< WPA3 192B Enterprise DA16200 not support
                                    eWiFiSecurityNotSupported          	///< Unknown Security
                        } WIFISecurity_t;
                        */

                    //[[change in v2.4.15
                    if (StaticDataSave.device.equals("RRQ61400")) {
                        if (StaticDataSave.networkSecurityNum == 0) {
                            rl_scanAP.setVisibility(View.INVISIBLE);
                            ll_selectAP.setVisibility(View.VISIBLE);

                            StaticDataSave.networkPassword = "";

                            displayNetworkinfo(
                                    StaticDataSave.pingAddress,
                                    StaticDataSave.svrAddress,
                                    StaticDataSave.svrPort,
                                    StaticDataSave.svrUrl
                            );

                            StaticDataSave.isHidden = 0;

                            displayAPinfo(
                                    StaticDataSave.networkSSID,
                                    StaticDataSave.networkSecurityNum,
                                    StaticDataSave.networkPassword,
                                    StaticDataSave.isHidden
                            );
                        } else if (StaticDataSave.networkSecurityNum == 1
                                        || StaticDataSave.networkSecurityNum == 2
                                        || StaticDataSave.networkSecurityNum == 3
                                        || StaticDataSave.networkSecurityNum == 5) {
                            InputPasswordDialog inputPasswordDialog = new InputPasswordDialog(mContext);
                            inputPasswordDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            inputPasswordDialog.setCancelable(true);
                            inputPasswordDialog.getWindow().setGravity(Gravity.CENTER);
                            inputPasswordDialog.show();
                        } else if (StaticDataSave.networkSecurityNum == 4) {
                            EnterpriseDialog enterpriseDialog = new EnterpriseDialog(mContext);
                            enterpriseDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            enterpriseDialog.setCancelable(true);
                            enterpriseDialog.getWindow().setGravity(Gravity.CENTER);
                            enterpriseDialog.show();
                        }
                    } else {
                        if (StaticDataSave.networkSecurityNum == 0
                                || StaticDataSave.networkSecurityNum == 5) {

                            rl_scanAP.setVisibility(View.INVISIBLE);
                            ll_selectAP.setVisibility(View.VISIBLE);

                            StaticDataSave.networkPassword = "";

                            displayNetworkinfo(
                                    StaticDataSave.pingAddress,
                                    StaticDataSave.svrAddress,
                                    StaticDataSave.svrPort,
                                    StaticDataSave.svrUrl
                            );

                            StaticDataSave.isHidden = 0;

                            displayAPinfo(
                                    StaticDataSave.networkSSID,
                                    StaticDataSave.networkSecurityNum,
                                    StaticDataSave.networkPassword,
                                    StaticDataSave.isHidden
                            );

                        } else if (StaticDataSave.networkSecurityNum == 1
                                || StaticDataSave.networkSecurityNum == 2
                                || StaticDataSave.networkSecurityNum == 3
                                || StaticDataSave.networkSecurityNum == 4
                                || StaticDataSave.networkSecurityNum == 6
                                || StaticDataSave.networkSecurityNum == 7) {

                            InputPasswordDialog inputPasswordDialog = new InputPasswordDialog(mContext);
                            inputPasswordDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            inputPasswordDialog.setCancelable(true);
                            inputPasswordDialog.getWindow().setGravity(Gravity.CENTER);
                            inputPasswordDialog.show();

                        } else if (StaticDataSave.networkSecurityNum == 8
                                || StaticDataSave.networkSecurityNum == 9
                                || StaticDataSave.networkSecurityNum == 10
                                || StaticDataSave.networkSecurityNum == 11
                                || StaticDataSave.networkSecurityNum == 12
                                || StaticDataSave.networkSecurityNum == 13) {

                            EnterpriseDialog enterpriseDialog = new EnterpriseDialog(mContext);
                            enterpriseDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                            enterpriseDialog.setCancelable(true);
                            enterpriseDialog.getWindow().setGravity(Gravity.CENTER);
                            enterpriseDialog.show();
                        }
                    }
                    //]]
                }
            }
        });

        btn_apScan = findViewById(R.id.btn_apScan);
        if (btn_apScan != null) {
            btn_apScan.setEnabled(false);
            btn_apScan.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
        }

        btn_hiddenWiFi = findViewById(R.id.btn_hiddenWiFi);
        if (btn_hiddenWiFi != null) {
            btn_hiddenWiFi.setEnabled(false);
            btn_hiddenWiFi.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
        }

        btn_reset = (FButton) findViewById(R.id.btn_reset);
        if (btn_reset != null) {
            btn_reset.setEnabled (false);
            btn_reset.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
        }

        ll_sendCommand = (LinearLayout) findViewById(R.id.ll_sendCommand);
        ll_sendCommand.setVisibility(View.INVISIBLE);
        btn_command = (FButton) findViewById(R.id.btn_command);
        if (btn_command != null) {
            btn_command.setEnabled(false);
            btn_command.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
        }
        et_command = (EditText) findViewById(R.id.et_command);
        et_command.setText("");
        et_command.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        et_command.addTextChangedListener(this);
        btn_send = (FButton) findViewById(R.id.btn_send);
        btn_send.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {

                String jsonCommand = et_command.getText().toString();

                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(et_command.getWindowToken(), 0);

                if (jsonCommand.contains("scan")) {

                    showScanningDialog();

                    btn_apScan.setEnabled(false);
                    btn_apScan.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));

                    btn_hiddenWiFi.setEnabled(false);
                    btn_hiddenWiFi.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));

                    btn_command.setEnabled(false);
                    btn_command.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
                    btn_reset.setEnabled(false);
                    btn_reset.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
                    rl_scanAP.setVisibility(View.VISIBLE);
                    if (tv_noList.getVisibility() == View.VISIBLE) {
                        tv_noList.setVisibility(View.INVISIBLE);
                    }
                    ll_progressScanning.setVisibility(View.VISIBLE);
                    tv_progress.setText("0");

                    if (ssidList != null) {
                        ssidList.clear();
                    }
                    if (securityList != null) {
                        securityList.clear();
                    }
                    if (signalList != null) {
                        signalList.clear();
                    }

                    if (apRowItems != null) {
                        apRowItems.clear();
                        adapter.notifyDataSetChanged();
                    }
                    ll_selectAP.setVisibility(View.INVISIBLE);
                    ll_sendCommand.setVisibility(View.INVISIBLE);
                    mHandler.sendEmptyMessageDelayed(HandleMsg.E_BLE_NETWORK_SCAN_TIMEOUT, 20000);
                }
                sendCommand(jsonCommand);
            }
        });
    }

    @Override
    protected void onResume() {
        MyLog.i( "== onResume() ==");
        super.onResume();
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        //[[add in v2.4.15
        mShouldUnbind = true;
        //]]

        mContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter(), RECEIVER_EXPORTED);

        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(StaticDataSave.mDeviceAddress);
            MyLog.d( "Connect request result=" + result);
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mProgressReceiver, new IntentFilter("ProgressData")
        );
    }

    @Override
    protected void onPause() {
        MyLog.i( "== onPause() ==");
        super.onPause();
        //[[modify in v2.4.15
        //unbindService(mServiceConnection);
        if(mShouldUnbind) {
            unbindService(mServiceConnection);
            mShouldUnbind = false;
        }
        //]]

        mBluetoothLeService = null;

        //[[modify in v2.4.15
        //unregisterReceiver(mGattUpdateReceiver);
        try {
            unregisterReceiver(mGattUpdateReceiver);
        } catch (IllegalArgumentException e) {

        } catch (Exception e) {

        } finally {

        }
        //]]
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mProgressReceiver);
    }

    @Override
    protected void onDestroy() {
        MyLog.i( "== onDestroy() ==");
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        initValue();
        Intent intent = new Intent(mContext, DeviceScanActivity.class);
        startActivity(intent);
        finishAffinity();
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void getGattServices(List<BluetoothGattService> gattServices) {
        if (refresh) {
            refresh = false;
            mBluetoothLeService.refreshServices();
        }
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, MyGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            MyLog.i("--------------------------------------------------------------------------");
            MyLog.i("currentServiceData = "+currentServiceData.get(LIST_NAME)+", "+currentServiceData.get(LIST_UUID));

            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();


            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);

                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(LIST_NAME, MyGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                MyLog.i("currentCharaData = "+currentCharaData.get(LIST_NAME)+", "+currentCharaData.get(LIST_UUID));
                gattCharacteristicGroupData.add(currentCharaData);

                if (gattCharacteristic.getUuid().equals(UUID.fromString(MyGattAttributes.WIFI_SVC_UUID))) {
                    WIFI_SVC = gattCharacteristic;
                } else if (gattCharacteristic.getUuid().equals(UUID.fromString(MyGattAttributes.WIFI_SVC_WFCMD_UUID))) {
                    WIFI_SVC_WFCMD = gattCharacteristic;
                } else if (gattCharacteristic.getUuid().equals(UUID.fromString(MyGattAttributes.WIFI_SVC_WFACT_RES_UUID))) {
                    WIFI_SVC_WFACT_RES = gattCharacteristic;
                } else if (gattCharacteristic.getUuid().equals(UUID.fromString(MyGattAttributes.WIFI_SVC_APSCAN_RES_UUID))) {
                    WIFI_SVC_APSCAN_RES = gattCharacteristic;
                } else if (gattCharacteristic.getUuid().equals(UUID.fromString(MyGattAttributes.WIFI_SVC_PROV_DATA_UUID))) {
                    WIFI_SVC_PROV_DATA = gattCharacteristic;
                }
                else if (gattCharacteristic.getUuid().equals(UUID.fromString(MyGattAttributes.WIFI_SVC_AWS_DATA_UUID))) {
                    WIFI_SVC_AWS_DATA = gattCharacteristic;
                }
                else if (gattCharacteristic.getUuid().equals(UUID.fromString(MyGattAttributes.WIFI_SVC_AZURE_DATA_UUID))) {
                    WIFI_SVC_AZURE_DATA = gattCharacteristic;
                }
                else if (gattCharacteristic.getUuid().equals(UUID.fromString(MyGattAttributes.GBG_SVC_UUID))) {
                    GBG_SVC = gattCharacteristic;
                    MyLog.e("GBG_SVC exist!");

                } else if (gattCharacteristic.getUuid().equals(UUID.fromString(MyGattAttributes.GBG_CHAR_UUID))) {
                    GBG_SVC_CHAR = gattCharacteristic;
                    MyLog.e("GBG_SVC_CHAR exist!");
                }
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        if (refresh == true && isRefreshed == false && GBG_SVC_CHAR != null) {
            //]]
            MyLog.e("GBG_SVC_CHAR exist!");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            GBG_SVC_CHAR = null;
            mGattCharacteristics = null;
            mBluetoothLeService.refreshServices();
            isRefreshed = true;
        }

        if (WIFI_SVC_AWS_DATA != null) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            showReceiveAwsThingNameDialog();

                            btn_apScan.setEnabled(false);
                            btn_apScan.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));

                            btn_hiddenWiFi.setEnabled(false);
                            btn_hiddenWiFi.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));

                            btn_command.setEnabled(false);
                            btn_command.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
                            btn_reset.setEnabled(false);
                            btn_reset.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));

                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            sendGetThingNameCommand();
                        }
                    });
                }
            }).start();
        }

        if (WIFI_SVC_AZURE_DATA != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showReceiveAzureThingNameDialog();
                            btn_apScan.setEnabled(false);
                            btn_apScan.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
                            btn_hiddenWiFi.setEnabled(false);
                            btn_hiddenWiFi.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
                            btn_command.setEnabled(false);
                            btn_command.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
                            btn_reset.setEnabled(false);
                            btn_reset.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            sendGetThingNameCommand();
                        }
                    });
                }
            }).start();
        }

        if (WIFI_SVC_WFCMD != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (btn_apScan != null) {
                                btn_apScan.setEnabled(true);
                                btn_apScan.setButtonColor(getResources().getColor(R.color.fbutton_default_color));
                                btn_apScan.setOnClickListener(new OnSingleClickListener() {
                                    @Override
                                    public void onSingleClick(View v) {

                                        btn_apScan.setEnabled(false);
                                        btn_apScan.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
                                        btn_command.setEnabled(false);
                                        btn_command.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
                                        btn_reset.setEnabled(false);
                                        btn_reset.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
                                        btn_hiddenWiFi.setEnabled(false);
                                        btn_hiddenWiFi.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));

                                        rl_scanAP.setVisibility(View.VISIBLE);
                                        if (tv_noList.getVisibility() == View.VISIBLE) {
                                            tv_noList.setVisibility(View.INVISIBLE);
                                        }
                                        ll_progressScanning.setVisibility(View.VISIBLE);
                                        tv_progress.setText("0");

                                        if (ssidList != null) {
                                            ssidList.clear();
                                        }
                                        if (securityList != null) {
                                            securityList.clear();
                                        }
                                        if (signalList != null) {
                                            signalList.clear();
                                        }

                                        if (apRowItems != null) {
                                            apRowItems.clear();
                                            adapter.notifyDataSetChanged();
                                        }
                                        ll_selectAP.setVisibility(View.INVISIBLE);
                                        ll_sendCommand.setVisibility(View.INVISIBLE);
                                        mHandler.sendEmptyMessageDelayed(HandleMsg.E_BLE_NETWORK_SCAN_TIMEOUT, 20000);

                                        if (StaticDataSave.device.equals("RRQ61400")) {
                                            if (WIFI_SVC_WFACT_RES != null) {
                                                MyLog.i("SUBSCRIBE: WiFi Status");
                                                mBluetoothLeService.setCharacteristicNotification(WIFI_SVC_WFACT_RES, true);
                                            }
                                        }

                                        JSONObject obj = new JSONObject();
                                        try {
                                            obj.put("dialog_cmd", "scan");
                                        } catch(Exception e) {
                                            e.printStackTrace();
                                        }

                                        if (StaticDataSave.device.equals("RRQ61400")) {
                                            if (WIFI_SVC_WFCMD != null) {
                                                MyLog.i("WRITE: WiFi Command (scan)");
                                                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
                                            }
                                        } else {
                                            if (WIFI_SVC_WFCMD != null) {
                                                MyLog.i("WRITE: WiFi Command (scan)");
                                                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
                                            }

                                            if (WIFI_SVC_WFACT_RES != null) {
                                                MyLog.i("SUBSCRIBE: WiFi Status");
                                                mBluetoothLeService.setCharacteristicNotification(WIFI_SVC_WFACT_RES, true);
                                            }
                                        }
                                        showScanningDialog();
                                    }
                                });
                            }

                            if (btn_hiddenWiFi != null) {
                                btn_hiddenWiFi.setEnabled(true);
                                btn_hiddenWiFi.setButtonColor(getResources().getColor(R.color.fbutton_default_color));

                                btn_hiddenWiFi.setOnClickListener(new OnSingleClickListener() {
                                    @Override
                                    public void onSingleClick(View v) {
                                        AddWiFiDialog addWiFiDialog = new AddWiFiDialog(mContext);
                                        addWiFiDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                                        addWiFiDialog.setCancelable(false);
                                        addWiFiDialog.show();

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

                                        WindowManager.LayoutParams params = addWiFiDialog.getWindow().getAttributes();
                                        int dialogWindowWidth = (int) (displayWidth * 0.8f);
                                        params.width = dialogWindowWidth;
                                        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                                        addWiFiDialog.getWindow().setAttributes((WindowManager.LayoutParams) params);
                                    }
                                });
                            }

                            if (btn_reset != null) {
                                btn_reset.setEnabled(true);
                                btn_reset.setButtonColor(getResources().getColor(R.color.fbutton_default_color));

                                btn_reset.setOnClickListener(new OnSingleClickListener() {
                                    @Override
                                    public void onSingleClick(View v) {
                                        JSONObject obj = new JSONObject();
                                        try {
                                            obj.put("dialog_cmd", "factory_reset");
                                        } catch(Exception e) {
                                            e.printStackTrace();
                                        }

                                        //[[add in v2.4.15
                                        if (StaticDataSave.device.equals("RRQ61400")) {
                                            if (WIFI_SVC_WFACT_RES != null) {
                                                MyLog.i("SUBSCRIBE: WiFi Status");
                                                mBluetoothLeService.setCharacteristicNotification(WIFI_SVC_WFACT_RES, true);
                                            }
                                        }

                                        if (StaticDataSave.device.equals("RRQ61400")) {
                                            if (WIFI_SVC_WFCMD != null) {
                                                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
                                            }
                                        } else {
                                            if (WIFI_SVC_WFCMD != null) {
                                                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
                                            }
                                            if (WIFI_SVC_WFACT_RES != null) {
                                                mBluetoothLeService.setCharacteristicNotification(WIFI_SVC_WFACT_RES, true);
                                            }
                                        }
                                        //]]

                                        if (btn_apScan != null) {
                                            btn_apScan.setEnabled(false);
                                            btn_apScan.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));

                                        }

                                        if (btn_hiddenWiFi != null) {
                                            btn_hiddenWiFi.setEnabled(false);
                                            btn_hiddenWiFi.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
                                        }

                                        if (btn_command != null) {
                                            btn_command.setEnabled(false);
                                            btn_command.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
                                        }

                                        if (btn_reset != null) {
                                            btn_reset.setEnabled(false);
                                            btn_reset.setButtonColor(getResources().getColor(R.color.fbutton_color_concrete));
                                        }
                                    }
                                });
                            }

                            if (btn_command != null) {
                                btn_command.setEnabled(true);
                                btn_command.setButtonColor(getResources().getColor(R.color.fbutton_default_color));

                                btn_command.setOnClickListener(new OnSingleClickListener() {
                                    @Override
                                    public void onSingleClick(View v) {
                                        rl_scanAP.setVisibility(View.INVISIBLE);
                                        ll_selectAP.setVisibility(View.INVISIBLE);
                                        ll_sendCommand.setVisibility(View.VISIBLE);
                                    }
                                });
                            }

                        }
                    });
                }
            }).start();
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    public void updateAPList() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable(){
                    @Override
                    public void run() {
                        ssid =  new String[ssidList.size()];
                        stringSecurity = new String[ssidList.size()];
                        signalBar = new Integer[ssidList.size()];
                        secMode = new Integer[ssidList.size()];
                        isSecurity = new boolean[ssidList.size()];
                        level = new Integer[ssidList.size()];
                        for(int i = 0; i < ssidList.size(); i++) {

                            //[[modify in v2.4.16
                            if (StaticDataSave.device.equals("RRQ61400")) {
                                if (securityList.get(i) == 0) {
                                    ssid[i] = ssidList.get(i);
                                    secMode[i] = securityList.get(i);
                                    isSecurity[i] = false;
                                    level[i] = signalList.get(i);
                                    stringSecurity[i] = convertStringSecurity1(securityList.get(i));
                                    signalBar[i] = wifiSignalBar1(isSecurity[i], level[i]);
                                } else {
                                    ssid[i] = ssidList.get(i);
                                    secMode[i] = securityList.get(i);
                                    isSecurity[i] = true;
                                    level[i] = signalList.get(i);
                                    stringSecurity[i] = convertStringSecurity1(securityList.get(i));
                                    signalBar[i] = wifiSignalBar1(isSecurity[i], level[i]);
                                }
                            } else {
                                if (securityList.get(i) == 0 || securityList.get(i) == 5) {
                                    ssid[i] = ssidList.get(i);
                                    secMode[i] = securityList.get(i);
                                    isSecurity[i] = false;
                                    level[i] = signalList.get(i);
                                    stringSecurity[i] = convertStringSecurity(securityList.get(i));
                                    signalBar[i] = wifiSignalBar(isSecurity[i], level[i]);
                                } else {
                                    ssid[i] = ssidList.get(i);
                                    secMode[i] = securityList.get(i);
                                    isSecurity[i] = true;
                                    level[i] = signalList.get(i);
                                    stringSecurity[i] = convertStringSecurity(securityList.get(i));
                                    signalBar[i] = wifiSignalBar(isSecurity[i], level[i]);
                                }
                            }
                            //]]

                            /*if (securityList.get(i) == 0 || securityList.get(i) == 5) {
                                ssid[i] = ssidList.get(i);
                                secMode[i] = securityList.get(i);
                                isSecurity[i] = false;
                                level[i] = signalList.get(i);
                                //modify in v2.4.15
                                //stringSecurity[i] = convertStringSecurity(securityList.get(i));
                                //signalBar[i] = wifiSignalBar(isSecurity[i], level[i]);
                                if (StaticDataSave.device.equals("RRQ61400")) {
                                    stringSecurity[i] = convertStringSecurity1(securityList.get(i));
                                    signalBar[i] = wifiSignalBar1(isSecurity[i], level[i]);
                                } else {
                                    stringSecurity[i] = convertStringSecurity(securityList.get(i));
                                    signalBar[i] = wifiSignalBar(isSecurity[i], level[i]);
                                }
                                //]]
                            } else {
                                ssid[i] = ssidList.get(i);
                                secMode[i] = securityList.get(i);
                                isSecurity[i] = true;
                                level[i] = signalList.get(i);
                                //modify in v2.4.15
                                //stringSecurity[i] = convertStringSecurity(securityList.get(i));
                                //signalBar[i] = wifiSignalBar(isSecurity[i], level[i]);
                                if (StaticDataSave.device.equals("RRQ61400")) {
                                    stringSecurity[i] = convertStringSecurity1(securityList.get(i));
                                    signalBar[i] = wifiSignalBar1(isSecurity[i], level[i]);
                                } else {
                                    stringSecurity[i] = convertStringSecurity(securityList.get(i));
                                    signalBar[i] = wifiSignalBar(isSecurity[i], level[i]);
                                }
                                //]]
                            }*/
                        }

                        apRowItems = new ArrayList<APRowItem>();

                        for (int i = 0; i < ssid.length; i++) {

                            if (StaticDataSave.thingName != null) {
                                if (!ssid[i].contains(StaticDataSave.thingName)) {
                                    APRowItem item = new APRowItem(signalBar[i], ssid[i], stringSecurity[i], secMode[i], level[i]);
                                    apRowItems.add(item);
                                }
                            } else {
                                APRowItem item = new APRowItem(signalBar[i], ssid[i], stringSecurity[i], secMode[i], level[i]);
                                apRowItems.add(item);
                            }
                        }

                        if (apRowItems != null && apRowItems.size() > 0) {
                            try {
                                mHandler.removeMessages(HandleMsg.E_BLE_NETWORK_SCAN_TIMEOUT);
                                adapter = new APListViewAdapter(getApplicationContext(), R.layout.da16600_ap_list_item, apRowItems);
                                listView.setAdapter(adapter);
                                listView.setVisibility(View.VISIBLE);
                                ll_progressScanning.setVisibility(View.INVISIBLE);
                                tv_noList.setVisibility(View.INVISIBLE);
                            } catch (NullPointerException e) {
                                e.printStackTrace();
                            }

                        } else {
                            MyLog.e("apRowItems == null or apRowItems.size() < 0");
                        }
                    }
                });
            }
        }).start();

    }

    public String convertStringSecurity(int securityNumber) {
        String stringSecurity = "";
        if (securityNumber == 0) {
            stringSecurity = "none";
        } else if (securityNumber == 1) {
            stringSecurity = "WEP";
        } else if (securityNumber == 2) {
            stringSecurity = "WPA";
        } else if (securityNumber == 3) {
            stringSecurity = "WPA2";
        } else if (securityNumber == 4) {
            stringSecurity = "WPA/WPA2";
        } else if (securityNumber == 5) {
            stringSecurity = "WPA3OWE";
        } else if (securityNumber == 6) {
            stringSecurity = "WPA3SAE";
        } else if (securityNumber == 7) {
            stringSecurity = "WPA2/WPA3SAE";
        } else if (securityNumber == 8) {
            stringSecurity = "WPA-EAP";
        } else if (securityNumber == 9) {
            stringSecurity = "WPA2-EAP";
        } else if (securityNumber == 10) {
            stringSecurity = "WPA/WPA2-EAP";
        } else if (securityNumber == 11) {
            stringSecurity = "WPA3-EAP";
        } else if (securityNumber == 12) {
            stringSecurity = "WPA2/WPA3-EAP";
        } else if (securityNumber == 13) {
            stringSecurity = "WPA3-EAP-192B";
        }
        return stringSecurity;
    }

    public String convertStringSecurity1(int securityNumber) {
        String stringSecurity = "";
        if (securityNumber == 0) {
            stringSecurity = "none";
        } else if (securityNumber == 1) {
            stringSecurity = "WEP";
        } else if (securityNumber == 2) {
            stringSecurity = "WPA";
        } else if (securityNumber == 3) {
            stringSecurity = "WPA2";
        } else if (securityNumber == 4) {
            stringSecurity = "WPA2-ENT";
        } else if (securityNumber == 5) {
            stringSecurity = "WPA3";
        } else if (securityNumber == 6) {
            stringSecurity = "UNKNOWN";
        }
        return stringSecurity;
    }

    public int wifiSignalBar(boolean isSecurity, int level) {

        int signalBarID = R.drawable.outline_signal_wifi_4_bar_lock_black_24;

        if(level < 20) {
            if (isSecurity == true) {
                signalBarID = R.drawable.baseline_signal_wifi_1_bar_lock_black_48dp;
            } else {
                signalBarID = R.drawable.baseline_signal_wifi_0_bar_black_48dp;
            }
        }
        else if(level < 40) {
            if (isSecurity == true) {
                signalBarID = R.drawable.baseline_signal_wifi_1_bar_lock_black_48dp;
            } else {
                signalBarID = R.drawable.baseline_signal_wifi_1_bar_black_48dp;
            }
        }
        else if(level < 60) {
            if (isSecurity == true) {
                signalBarID = R.drawable.baseline_signal_wifi_2_bar_lock_black_48dp;
            } else {
                signalBarID = R.drawable.baseline_signal_wifi_2_bar_black_48dp;
            }
        }
        else if(level < 80) {
            if (isSecurity == true) {
                signalBarID = R.drawable.baseline_signal_wifi_3_bar_lock_black_48dp;
            } else {
                signalBarID = R.drawable.baseline_signal_wifi_3_bar_black_48dp;
            }
        }
        else {
            if (isSecurity == true) {
                signalBarID = R.drawable.outline_signal_wifi_4_bar_lock_black_24;
            } else {
                signalBarID = R.drawable.outline_signal_wifi_4_bar_black_24;
            }
        }
        return signalBarID;
    }

    public int wifiSignalBar1(boolean isSecurity, int level) {

        int signalBarID = R.drawable.outline_signal_wifi_4_bar_lock_black_24;

        if(level < -80) {
            if (isSecurity == true) {
                signalBarID = R.drawable.baseline_signal_wifi_1_bar_lock_black_48dp;
            } else {
                signalBarID = R.drawable.baseline_signal_wifi_0_bar_black_48dp;
            }
        }
        else if(level < -60) {
            if (isSecurity == true) {
                signalBarID = R.drawable.baseline_signal_wifi_1_bar_lock_black_48dp;
            } else {
                signalBarID = R.drawable.baseline_signal_wifi_1_bar_black_48dp;
            }
        }
        else if(level < -40) {
            if (isSecurity == true) {
                signalBarID = R.drawable.baseline_signal_wifi_2_bar_lock_black_48dp;
            } else {
                signalBarID = R.drawable.baseline_signal_wifi_2_bar_black_48dp;
            }
        }
        else if(level < -20) {
            if (isSecurity == true) {
                signalBarID = R.drawable.baseline_signal_wifi_3_bar_lock_black_48dp;
            } else {
                signalBarID = R.drawable.baseline_signal_wifi_3_bar_black_48dp;
            }
        }
        else {
            if (isSecurity == true) {
                signalBarID = R.drawable.outline_signal_wifi_4_bar_lock_black_24;
            } else {
                signalBarID = R.drawable.outline_signal_wifi_4_bar_black_24;
            }
        }
        return signalBarID;
    }

    public void sendNetworkinfo(String _pingAddress, String _svrAddress, int _svrPort, String _svrUrl) {

        final JSONObject obj = new JSONObject();
        try {
            obj.put("dialog_cmd", "network_info");
            obj.put("ping_addr", _pingAddress);
            obj.put("svr_addr", _svrAddress);
            obj.put("svr_port", _svrPort);
            obj.put("svr_url", _svrUrl);

            MyLog.i(">> sendNetworkinfo -> "+obj.toString());
            MyLog.i(">> sendNetworkinfo size = "+obj.toString().getBytes().length);

        } catch (Exception e) {
            e.printStackTrace();
        }

        //[[add in v2.4.15
        if (StaticDataSave.device.equals("RRQ61400")) {
            if (WIFI_SVC_WFACT_RES != null) {
                MyLog.i("SUBSCRIBE: WiFi Status");
                mBluetoothLeService.setCharacteristicNotification(WIFI_SVC_WFACT_RES, true);
            }
        }
        //]]

        if (StaticDataSave.device.equals("RRQ61400")) {
            if (WIFI_SVC_WFCMD != null) {
                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
            }
        } else {
            if (WIFI_SVC_WFCMD != null) {
                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
            }
            if (WIFI_SVC_WFACT_RES != null) {
                mBluetoothLeService.setCharacteristicNotification(WIFI_SVC_WFACT_RES, true);
            }
        }
    }

    public void sendAPinfo(String _ssid, int _security, String _password, int _isHidden) {

        final JSONObject obj = new JSONObject();
        try {
            obj.put("dialog_cmd", "select_ap");
            obj.put("SSID", _ssid);
            obj.put("security_type", _security);
            obj.put("password", _password);
            obj.put("isHidden", _isHidden);

            MyLog.i(">> sendAPinfo -> "+obj.toString());
            MyLog.i(">> sendAPinfo size = "+obj.toString().getBytes().length);

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (StaticDataSave.device.equals("RRQ61400")) {
            if (WIFI_SVC_WFCMD != null) {
                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
            }
        } else {
            if (WIFI_SVC_WFCMD != null) {
                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
            }
            if (WIFI_SVC_WFACT_RES != null) {
                mBluetoothLeService.setCharacteristicNotification(WIFI_SVC_WFACT_RES, true);
            }
        }
    }

    public void sendEnterpriseAPinfo(String _ssid, int _security, int _authType, int _authProtocol, String _userName, String _password, int _isHidden) {

        final JSONObject obj = new JSONObject();
        try {
            obj.put("dialog_cmd", "select_ap");
            obj.put("SSID", _ssid);
            obj.put("security_type", _security);
            obj.put("authType", _authType);
            obj.put("authProtocol", _authProtocol);
            obj.put("authID", _userName);
            obj.put("authPW", _password);
            obj.put("isHidden", _isHidden);

            MyLog.i(">> sendEnterpriseAPinfo -> "+obj.toString());
            MyLog.i(">> sendEnterpriseAPinfo size = "+obj.toString().getBytes().length);

        } catch (Exception e) {
            e.printStackTrace();
        }

        //[[add in v2.4.15
        if (StaticDataSave.device.equals("RRQ61400")) {
            if (WIFI_SVC_WFACT_RES != null) {
                MyLog.i("SUBSCRIBE: WiFi Status");
                mBluetoothLeService.setCharacteristicNotification(WIFI_SVC_WFACT_RES, true);
            }
        }
        //]]

        if (StaticDataSave.device.equals("RRQ61400")) {
            if (WIFI_SVC_WFCMD != null) {
                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
            }
        } else {
            if (WIFI_SVC_WFCMD != null) {
                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
            }
            if (WIFI_SVC_WFACT_RES != null) {
                mBluetoothLeService.setCharacteristicNotification(WIFI_SVC_WFACT_RES, true);
            }
        }
    }

    public void sendDPMinfo(int _sleepMode, int _rtcTimer, int _useDPM, String _svrUrl, int _dpmKeepAlive, int _userWakeup, int _timWakeup) {

        final JSONObject obj = new JSONObject();
        try {
            obj.put("dialog_cmd", "set_dpm");
            obj.put("sleepMode",_sleepMode);

            if (_sleepMode == -1) {
                obj.put("rtcTimer",1740);
                obj.put("useDPM",1);
                obj.put("svr_url", _svrUrl);
                obj.put("dpmKeepAlive",30000);
                obj.put("userWakeup",0);
                obj.put("timWakeup",10);
            } else {
                obj.put("rtcTimer",_rtcTimer);
                obj.put("useDPM",_useDPM);
                obj.put("svr_url", _svrUrl);
                obj.put("dpmKeepAlive",_dpmKeepAlive);
                obj.put("userWakeup",_userWakeup);
                obj.put("timWakeup",_timWakeup);
            }

            MyLog.i(">> sendDPMinfo -> "+obj.toString());
            MyLog.i(">> sendDPMinfo size = "+obj.toString().getBytes().length);


        } catch (Exception e) {
            e.printStackTrace();
        }

        //[[add in v2.4.15
        if (StaticDataSave.device.equals("RRQ61400")) {
            if (WIFI_SVC_WFACT_RES != null) {
                MyLog.i("SUBSCRIBE: WiFi Status");
                mBluetoothLeService.setCharacteristicNotification(WIFI_SVC_WFACT_RES, true);
            }
        }
        //]]

        if (StaticDataSave.device.equals("RRQ61400")) {
            if (WIFI_SVC_WFCMD != null) {
                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
            }
        } else {
            if (WIFI_SVC_WFCMD != null) {
                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
            }
            if (WIFI_SVC_WFACT_RES != null) {
                mBluetoothLeService.setCharacteristicNotification(WIFI_SVC_WFACT_RES, true);
            }
        }

    }

    public void displayNetworkinfo(String _pingAddress, String _svrAddress, int _svrPort, String _svrUrl) {

        String result = "";
        String result_split = "";
        final JSONObject obj = new JSONObject();
        try {
            obj.put("dialog_cmd", "network_info");
            obj.put("ping_addr", _pingAddress);
            obj.put("svr_addr", _svrAddress);
            obj.put("svr_port", _svrPort);
            obj.put("customer_svr_url", _svrUrl);

            MyLog.i(">> displayNetworkinfo -> "+obj.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (obj != null) {
            result = obj.toString();
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(result);
        result_split = gson.toJson(je);

        if (et_rawCommand1 != null) {
            et_rawCommand1.setText(result_split);
        }
    }

    public void displayAPinfo(String _ssid, int _security, String _password, int _isHidden) {

        String result = "";
        String result_split = "";
        final JSONObject obj = new JSONObject();
        try {
            obj.put("dialog_cmd", "select_ap");
            obj.put("SSID", _ssid);
            obj.put("security_type", _security);
            obj.put("password", _password);
            obj.put("isHidden", _isHidden);

            MyLog.i(">> displayAPinfo -> "+obj.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (obj != null) {
            result = obj.toString();
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(result);
        result_split = gson.toJson(je);

        //[[add in v2.4.18
        String replace_result_split;
        replace_result_split = result_split.replace("\\\\", "\\");
        replace_result_split = result_split.replace("\\r", "\r");
        replace_result_split = result_split.replace("\\b", "\b");
        replace_result_split = result_split.replace("\\f", "\f");
        replace_result_split = result_split.replace("\\t", "\t");
        replace_result_split = result_split.replace("\\n", "\n");
        replace_result_split = result_split.replace("\\\"", "\"");
        replace_result_split = result_split.replace("\\\'", "\'");
        MyLog.i(">> replace_result_split = "+replace_result_split);
        //]]

        if (et_rawCommand2 != null) {
            //[[change in v2.4.18
            //et_rawCommand2.setText(result_split);
            et_rawCommand2.setText(replace_result_split);
            //]]
        }
    }

    public void displayEnterpriseAPinfo(String _ssid, int _security, int _authType, int _authProtocol, String _userName, String _password, int _isHidden) {

        String result = "";
        String result_split = "";
        final JSONObject obj = new JSONObject();
        try {
            obj.put("dialog_cmd", "select_ap");
            obj.put("SSID", _ssid);
            obj.put("security_type", _security);
            obj.put("authType", _authType);
            obj.put("authProtocol", _authProtocol);
            obj.put("authID", _userName);
            obj.put("authPW", _password);
            obj.put("isHidden", _isHidden);
            MyLog.i(">> displayEnterpriseAPinfo -> "+obj.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (obj != null) {
            result = obj.toString();
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(result);
        result_split = gson.toJson(je);

        if (et_rawCommand2 != null) {
            et_rawCommand2.setText(result_split);
        }
    }

    public void sendGetThingNameCommand() {
        MyLog.i(">> sendGetThingNameCommand()");
        final JSONObject obj = new JSONObject();
        try {
            obj.put("dialog_cmd", "get_thingName");

        } catch (Exception e) {
            e.printStackTrace();
        }

        //[[add in v2.4.15
        if (StaticDataSave.device.equals("RRQ61400")) {
            if (WIFI_SVC_WFACT_RES != null) {
                MyLog.i("SUBSCRIBE: WiFi Status");
                mBluetoothLeService.setCharacteristicNotification(WIFI_SVC_WFACT_RES, true);
            }
        }
        //]]

        if (StaticDataSave.device.equals("RRQ61400")) {
            if (WIFI_SVC_WFCMD != null) {
                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
            }
        } else {
            if (WIFI_SVC_WFCMD != null) {
                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
            }
            if (WIFI_SVC_WFACT_RES != null) {
                mBluetoothLeService.setCharacteristicNotification(WIFI_SVC_WFACT_RES, true);
            }
        }
    }

    public void sendGetModeCommand() {
        MyLog.i(">> sendGetModeCommand()");
        final JSONObject obj = new JSONObject();
        try {
            obj.put("dialog_cmd", "get_mode");

        } catch (Exception e) {
            e.printStackTrace();
        }

        //[[remove in v2.4.17
        /*if (StaticDataSave.device.equals("RRQ61400")) {
            if (WIFI_SVC_WFACT_RES != null) {
                MyLog.i("SUBSCRIBE: WiFi Status");
                mBluetoothLeService.setCharacteristicNotification(WIFI_SVC_WFACT_RES, true);
            }
        }*/
        //]]

        if (StaticDataSave.device.equals("RRQ61400")) {
            if (WIFI_SVC_WFCMD != null) {
                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
            }
        } else {
            if (WIFI_SVC_WFCMD != null) {
                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
            }
            if (WIFI_SVC_WFACT_RES != null) {
                mBluetoothLeService.setCharacteristicNotification(WIFI_SVC_WFACT_RES, true);
            }
        }
    }

    public void sendGetAzureConStringCommand() {
        MyLog.i(">> sendGetAzureConStringCommand()");
        final JSONObject obj = new JSONObject();
        try {
            obj.put("dialog_cmd", "get_azureConString");

        } catch (Exception e) {
            e.printStackTrace();
        }

        //[[remove in v2.4.17
        //[[add in v2.4.15
        /*if (StaticDataSave.device.equals("RRQ61400")) {
            if (WIFI_SVC_WFACT_RES != null) {
                MyLog.i("SUBSCRIBE: WiFi Status");
                mBluetoothLeService.setCharacteristicNotification(WIFI_SVC_WFACT_RES, true);
            }
        }*/
        //]]
        //]]

        if (StaticDataSave.device.equals("RRQ61400")) {
            if (WIFI_SVC_WFCMD != null) {
                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
            }
        } else {
            if (WIFI_SVC_WFCMD != null) {
                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
            }
            if (WIFI_SVC_WFACT_RES != null) {
                mBluetoothLeService.setCharacteristicNotification(WIFI_SVC_WFACT_RES, true);
            }
        }
    }

    public void sendRebootCommand() {
        MyLog.i(">> sendRebootCommand()");
        final JSONObject obj = new JSONObject();
        try {
            obj.put("dialog_cmd", "reboot");

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (StaticDataSave.device.equals("RRQ61400")) {
            if (WIFI_SVC_WFCMD != null) {
                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
            }
        } else {
            if (WIFI_SVC_WFCMD != null) {
                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
            }
            if (WIFI_SVC_WFACT_RES != null) {
                mBluetoothLeService.setCharacteristicNotification(WIFI_SVC_WFACT_RES, true);
            }
        }
    }

    public void sendChkNetworkCommand() {
        MyLog.i(">> sendChkNetworkCommand()");
        final JSONObject obj = new JSONObject();
        try {
            obj.put("dialog_cmd", "chk_network");

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (StaticDataSave.device.equals("RRQ61400")) {
            if (WIFI_SVC_WFCMD != null) {
                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
            }
        } else {
            if (WIFI_SVC_WFCMD != null) {
                mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, obj.toString());
            }
            if (WIFI_SVC_WFACT_RES != null) {
                mBluetoothLeService.setCharacteristicNotification(WIFI_SVC_WFACT_RES, true);
            }
        }
    }

    public void sendCommand(String command) {

        if (isJSONValid(command)) {
            JSONObject json = null;
            try {
                json = new JSONObject(command);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (StaticDataSave.device.equals("RRQ61400")) {
                if (WIFI_SVC_WFCMD != null) {
                    mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, json.toString());
                }
            } else {
                if (WIFI_SVC_WFCMD != null) {
                    mBluetoothLeService.writeCharacteristic(WIFI_SVC_WFCMD, json.toString());
                }
                if (WIFI_SVC_WFACT_RES != null) {
                    mBluetoothLeService.setCharacteristicNotification(WIFI_SVC_WFACT_RES, true);
                }
            }
        } else {
            customToast = new CustomToast(mContext);
            customToast.showToast(mContext, "The command contains invalid syntax", Toast.LENGTH_SHORT);
        }
    }

    public boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
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

    private BroadcastReceiver mProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String fromProgressStr = intent.getStringExtra("progress");
            MyLog.i("fromProgressStr = "+fromProgressStr);
            tv_progress.setText(fromProgressStr);
        }
    };

    private void initValue() {
        StaticDataSave.networkSSID = null;
        StaticDataSave.networkSecurity = false;
        StaticDataSave.networkSecurityNum = -1;
        StaticDataSave.networkPassword = null;

        dhcp = 1;
        StaticDataSave.pingAddress = "8.8.8.8";
        StaticDataSave.svrUrl = "www.google.com";
    }


    public AlertDialog scanFailDialog;
    public void showScanFailDialog() {

        dismissCheckingDialog();

        if (scanFailDialog != null ) {
            scanFailDialog.dismiss();
        }

        String title = "AP Scan Fail";
        String message = "AP scan failed.\nPlease try again.";
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogCustom).create();
        dialog.setTitle(title);
        dialog.setIcon(R.mipmap.renesas_ic_launcher);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissScanFailDialog();
            }
        });
        dialog.setOnCancelListener(null);
        dialog.show();
        scanFailDialog = dialog;
    }

    public void dismissScanFailDialog() {
        if (scanFailDialog != null) {
            scanFailDialog.dismiss();
        }
    }

    public void showScanningDialog() {
        if (scanningDialog == null) {
            scanningDialog = new ProgressDialog(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
            scanningDialog.setMessage("Scanning the Wi-Fi network...");
            scanningDialog.show();

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

            WindowManager.LayoutParams params = scanningDialog.getWindow().getAttributes();
            int dialogWindowWidth = (int) (displayWidth * 0.8f);
            params.width = dialogWindowWidth;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            scanningDialog.getWindow().setAttributes((WindowManager.LayoutParams) params);
            scanningDialog.setCanceledOnTouchOutside(false);
            scanningDialog.setCancelable(false);
        }
    }

    public void dismissScanningDialog() {
        if (scanningDialog != null) {
            if (scanningDialog.isShowing()) {
                scanningDialog.dismiss();
                scanningDialog = null;
            }
        }
    }

    public void showCheckingDialog() {
        MyLog.i(">> showCheckingDialog()");
        if (checkingDialog == null) {
            checkingDialog = new ProgressDialog(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
            checkingDialog.setTitle("Checking the network connection");
            checkingDialog.setMessage("The device is checking the network connection.");
            checkingDialog.show();

            int displayWidth = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowMetrics windowMetrics = DeviceControlActivity.getInstance().getWindowManager().getCurrentWindowMetrics();
                Insets insets = windowMetrics.getWindowInsets()
                        .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
                displayWidth = windowMetrics.getBounds().width() - insets.left - insets.right;
            } else {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                DeviceControlActivity.getInstance().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                displayWidth = displayMetrics.widthPixels;
            }

            WindowManager.LayoutParams params = checkingDialog.getWindow().getAttributes();
            int dialogWindowWidth = (int) (displayWidth * 0.8f);
            params.width = dialogWindowWidth;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            checkingDialog.getWindow().setAttributes((WindowManager.LayoutParams) params);
            checkingDialog.setCanceledOnTouchOutside(false);
            checkingDialog.setCancelable(false);
        }
    }

    public void dismissCheckingDialog() {
        if (checkingDialog != null) {
            if (checkingDialog.isShowing()) {
                checkingDialog.dismiss();
                checkingDialog = null;
            }
        }
    }


    public void showTxApInfoFailDialog() {

        dismissCheckingDialog();

        if (txApInfoFailDialog != null) {
            txApInfoFailDialog.dismiss();
        }

        String title = "Failure to transmit AP information";
        String message = "Transmission of AP information failed.\nPlease check the device status.";
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogCustom).create();
        dialog.setTitle(title);
        dialog.setIcon(R.mipmap.renesas_ic_launcher);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissTxApInfoFailDialog();
            }
        });
        dialog.setOnCancelListener(null);
        dialog.show();
        txApInfoFailDialog = dialog;
    }

    public void dismissTxApInfoFailDialog() {
        if (txApInfoFailDialog != null) {
            txApInfoFailDialog.dismiss();
        }
    }

    public void showApWrongPwdDialog() {

        dismissCheckingDialog();

        if (ApWorngPwdDialog != null) {
            ApWorngPwdDialog.dismiss();
        }

        String title = "Wrong password";
        String message = "The password of the Wi-Fi AP is incorrect.\n"+"Please try again.";
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogCustom).create();
        dialog.setTitle(title);
        dialog.setIcon(R.mipmap.renesas_ic_launcher);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissApWorngPwdDialog();
                InputPasswordDialog inputPasswordDialog = new InputPasswordDialog(mContext);
                inputPasswordDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                inputPasswordDialog.setCancelable(true);
                inputPasswordDialog.getWindow().setGravity(Gravity.CENTER);
                inputPasswordDialog.show();
            }
        });
        dialog.setOnCancelListener(null);
        dialog.show();
        ApWorngPwdDialog = dialog;
    }

    public void dismissApWorngPwdDialog() {
        if (ApWorngPwdDialog != null) {
            ApWorngPwdDialog.dismiss();
        }
    }

    // AP Connecttion Fail - 105
    public void showApFailDialog(String _ssid, String _password, String _security) {

        dismissCheckingDialog();

        if (apFailDialog != null) {
            apFailDialog.dismiss();
        }

        String title = "Network check result";
        String message = "\u25BA Connect to AP : Failure\n"+
                "\u25BA Check SSID or password\n\n"+
                "\u25BA SSID : "+_ssid+"\n"+"\u25BA Password : "+_password+"\n"+"\u25BA Security : "+_security;
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogCustom).create();
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "RETRY", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissApFailDialog();
                if (StaticDataSave.isHidden == 0) {
                    InputSsidPasswordDialog inputssidPasswordDialog = new InputSsidPasswordDialog(mContext);
                    inputssidPasswordDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    inputssidPasswordDialog.setCancelable(true);
                    inputssidPasswordDialog.getWindow().setGravity(Gravity.CENTER);
                    inputssidPasswordDialog.show();
                } else if (StaticDataSave.isHidden == 1) {
                    AddWiFiDialog addWiFiDialog = new AddWiFiDialog(mContext);
                    addWiFiDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    addWiFiDialog.setCancelable(false);
                    addWiFiDialog.show();

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

                    WindowManager.LayoutParams params = addWiFiDialog.getWindow().getAttributes();
                    int dialogWindowWidth = (int) (displayWidth * 0.8f);
                    params.width = dialogWindowWidth;
                    params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                    addWiFiDialog.getWindow().setAttributes((WindowManager.LayoutParams) params);
                }

            }
        });
        dialog.setOnCancelListener(null);
        dialog.show();
        apFailDialog = dialog;
    }

    public void dismissApFailDialog() {
        if (apFailDialog != null) {
            apFailDialog.dismiss();
        }
    }

    //DNS Fail, Server Fail - 106
    public void showDnsFailServerFailDialog(String _svrUrl, String _pingIp) {

        dismissCheckingDialog();

        if (dnsFailServerFailDialog != null) {
            dnsFailServerFailDialog.dismiss();
        }

        String title = "Network check result";
        String message = "\u25BA Get IP address from DNS : Failure\n" +
                "\u25BA Connect to Server : Failure\n"+
                "\u25BA No internet\n\n"+
                "\u25BA Customer Server URL : "+_svrUrl+"\n"+
                "\u25BA Ping IP : "+_pingIp+"\n"+
                "Are you sure you want to complete provisioning?";
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogCustom).create();
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setCancelable(false);

        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "RETRY", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissDnsFailServerFailDialog();
            }
        });
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "COMPLETE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissDnsFailServerFailDialog();
                sendRebootCommand();
            }
        });
        dialog.setOnCancelListener(null);
        dialog.show();
        dnsFailServerFailDialog = dialog;
    }

    public void dismissDnsFailServerFailDialog() {
        if (dnsFailServerFailDialog != null) {
            dnsFailServerFailDialog.dismiss();
        }
    }

    //DNS Fail, Server OK - 107
    public void showDnsFailServerOkDialog(String _svrUrl, String _pingIp) {

        dismissCheckingDialog();

        if (dnsFailServerOkDialog != null) {
            dnsFailServerOkDialog.dismiss();
        }

        String title = "Network check result";
        String message = "\u25BA Get IP address from DNS : Failure\n" +
                "\u25BA Connect to Server : Success\n"+
                "\u25BA Wrong Customer Server URL\n\n"+
                "\u25BA Customer Server URL : "+_svrUrl+"\n"+
                "\u25BA Ping IP : "+_pingIp+"\n"+
                "Are you sure you want to complete provisioning?";
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogCustom).create();
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setCancelable(false);

        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "RETRY", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissDnsFailServerOkDialog();
            }
        });
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "COMPLETE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissDnsFailServerOkDialog();
                sendRebootCommand();
            }
        });

        dialog.setOnCancelListener(null);
        dialog.show();
        dnsFailServerOkDialog = dialog;
    }

    public void dismissDnsFailServerOkDialog() {
        if (dnsFailServerOkDialog != null) {
            dnsFailServerOkDialog.dismiss();
        }
    }

    //No URL, Ping Fail - 108
    public void showNoUrlPingFailDialog(String _pingIp) {

        dismissCheckingDialog();

        if (noUrlPingFailDialog != null) {
            noUrlPingFailDialog.dismiss();
        }

        String title = "Network check result";
        String message = "\u25BA No Customer Server URL\n" +
                "\u25BA Ping test : Failure\n\n" +
                "\u25BA Ping IP : "+_pingIp+"\n"+
                "Are you sure you want to complete provisioning?";
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogCustom).create();
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setCancelable(false);

        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "RETRY", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissNoUrlPingFailDialog();
            }
        });
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "COMPLETE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissNoUrlPingFailDialog();
                sendRebootCommand();
            }
        });
        dialog.setOnCancelListener(null);
        dialog.show();
        noUrlPingFailDialog = dialog;
    }

    public void dismissNoUrlPingFailDialog() {
        if (noUrlPingFailDialog != null) {
            noUrlPingFailDialog.dismiss();
        }
    }


    //No URL, Ping OK - 109
    public void showNoUrlPingOkDialog(String _pingIp) {

        dismissCheckingDialog();

        if (noUrlPingOkDialog != null) {
            noUrlPingOkDialog.dismiss();
        }

        String title = "Network check result";
        String message = "\u25BA No Customer Server URL\n" +
                "\u25BA Ping test : Success\n\n" +
                "\u25BA Ping IP : "+_pingIp;
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogCustom).create();
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "COMPLETE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissNoUrlPingOkDialog();
                sendRebootCommand();
            }
        });
        dialog.setOnCancelListener(null);
        dialog.show();
        noUrlPingOkDialog = dialog;
    }

    public void dismissNoUrlPingOkDialog() {
        if (noUrlPingOkDialog != null) {
            noUrlPingOkDialog.dismiss();
        }
    }

    //DNS OK, Ping Fail, Server OK - 110
    public void showDnsOkPingFailServerOkDialog(String _svrUrl, String _svrIp, String _pingIp) {

        dismissCheckingDialog();

        if (dnsOkPingFailServerOkDialog != null) {
            dnsOkPingFailServerOkDialog.dismiss();
        }

        String title = "Network check result";
        String message = "\u25BA Get IP address from DNS : Success\n" +
                "\u25BA Ping test : Failure\n" +
                "\u25BA Connect to Server : Success\n"+
                "\u25BA AP gives wrong IP address\n\n"+
                "\u25BA Customer Server URL : "+_svrUrl+"\n"+
                "\u25BA Customer Server IP : "+_svrIp+"\n"+
                "\u25BA Ping IP : "+_pingIp;
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogCustom).create();
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissDnsOkPingFailServerOkDialog();

            }
        });
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "COMPLETE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissDnsOkPingFailServerOkDialog();
                sendRebootCommand();
            }
        });
        dialog.setOnCancelListener(null);
        dialog.show();
        dnsOkPingFailServerOkDialog = dialog;
    }

    public void dismissDnsOkPingFailServerOkDialog() {
        if (dnsOkPingFailServerOkDialog != null) {
            dnsOkPingFailServerOkDialog.dismiss();
        }
    }

    //DNS OK, Ping OK - 111
    public void showDnsOkPingOkDialog(String _svrUrl, String _svrIp) {

        dismissCheckingDialog();

        if (dnsOkPingOkDialog != null) {
            dnsOkPingOkDialog.dismiss();
        }

        String title = "Network check result";
        String message = "\u25BA Get IP address from DNS : Success\n" +
                "\u25BA Connect to customer server : Success\n\n"+
                "\u25BA Customer Server URL : "+_svrUrl+"\n"+
                "\u25BA Customer Server IP : "+_svrIp;
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogCustom).create();
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "COMPLETE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissDnsOkPingOkDialog();
                sendRebootCommand();
            }
        });
        dialog.setOnCancelListener(null);
        dialog.show();
        dnsOkPingOkDialog = dialog;
    }

    public void dismissDnsOkPingOkDialog() {
        if (dnsOkPingOkDialog != null) {
            dnsOkPingOkDialog.dismiss();
        }
    }

    //DNS OK, Ping Fail, Server Fail - 113
    public void showDnsOkPingFailServerFailDialog(String _svrUrl, String _svrIp, String _pingIp) {

        dismissCheckingDialog();

        if (dnsOkPingFailServerFailDialog != null) {
            dnsOkPingFailServerFailDialog.dismiss();
        }

        String title = "Network check result";
        String message = "\u25BA Get IP address from DNS : Success\n" +
                "\u25BA Ping test : Failure\n" +
                "\u25BA Connect to Server : Failure\n\n"+
                "\u25BA Customer Server URL : "+_svrUrl+"\n"+
                "\u25BA Customer Server IP : "+_svrIp+"\n"+
                "\u25BA Ping IP : "+_pingIp+"\n"+
                "Are you sure you want to complete provisioning?";
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogCustom).create();
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setCancelable(false);

        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "RETRY", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissDnsOkPingFailServerFailDialog();
            }
        });
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "COMPLETE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissDnsOkPingFailServerFailDialog();
                sendRebootCommand();
            }
        });
        dialog.setOnCancelListener(null);
        dialog.show();
        dnsOkPingFailServerFailDialog = dialog;
    }

    public void dismissDnsOkPingFailServerFailDialog() {
        if (dnsOkPingFailServerFailDialog != null) {
            dnsOkPingFailServerFailDialog.dismiss();
        }
    }

    public void showCmdFailDialog() {

        if (cmdFailDialog != null) {
            cmdFailDialog.dismiss();
        }

        String title = "Command send failure";
        String message = "No response received from device.\n"+
                "Please try again or check if the SDK version is 2.3.3.2 or higher.";
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.AlertDialogCustom).create();
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismissCmdTimeoutDialog();
                Intent main = new Intent(DeviceControlActivity.this, MainActivity.class);
                main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(main);
                finishAffinity();
            }
        });
        dialog.setOnCancelListener(null);
        dialog.show();
        cmdFailDialog = dialog;
    }

    public void dismissCmdTimeoutDialog() {
        if (cmdFailDialog != null) {
            cmdFailDialog.dismiss();
        }
    }

    public void showReceiveAwsThingNameDialog() {
        if (receiveAwsThingNameDialog == null) {
            receiveAwsThingNameDialog = new ProgressDialog(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
            receiveAwsThingNameDialog.setMessage("Receiving Thing Name for AWS IoT...");

            receiveAwsThingNameDialog.show();

            int displayWidth = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowMetrics windowMetrics = DeviceControlActivity.getInstance().getWindowManager().getCurrentWindowMetrics();
                Insets insets = windowMetrics.getWindowInsets()
                        .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
                displayWidth = windowMetrics.getBounds().width() - insets.left - insets.right;
            } else {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                DeviceControlActivity.getInstance().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                displayWidth = displayMetrics.widthPixels;
            }

            WindowManager.LayoutParams params = receiveAwsThingNameDialog.getWindow().getAttributes();
            int dialogWindowWidth = (int) (displayWidth * 0.8f);
            params.width = dialogWindowWidth;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            receiveAwsThingNameDialog.getWindow().setAttributes((WindowManager.LayoutParams) params);
            receiveAwsThingNameDialog.setCanceledOnTouchOutside(false);
            receiveAwsThingNameDialog.setCancelable(false);
        }

    }

    public void dismissReceiveAwsThingNameDialog() {
        if (receiveAwsThingNameDialog != null) {
            if (receiveAwsThingNameDialog.isShowing()) {
                receiveAwsThingNameDialog.dismiss();
                receiveAwsThingNameDialog = null;
            }
        }
    }

    public void showReceiveAzureThingNameDialog() {
        if (receiveAzureThingNameDialog == null) {
            receiveAzureThingNameDialog = new ProgressDialog(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
            receiveAzureThingNameDialog.setMessage("Receiving Thing Name for Azure IoT...");
            receiveAzureThingNameDialog.show();

            int displayWidth = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowMetrics windowMetrics = DeviceControlActivity.getInstance().getWindowManager().getCurrentWindowMetrics();
                Insets insets = windowMetrics.getWindowInsets()
                        .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
                displayWidth = windowMetrics.getBounds().width() - insets.left - insets.right;
            } else {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                DeviceControlActivity.getInstance().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                displayWidth = displayMetrics.widthPixels;
            }

            WindowManager.LayoutParams params = receiveAzureThingNameDialog.getWindow().getAttributes();
            int dialogWindowWidth = (int) (displayWidth * 0.8f);
            params.width = dialogWindowWidth;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            receiveAzureThingNameDialog.getWindow().setAttributes((WindowManager.LayoutParams) params);
            receiveAzureThingNameDialog.setCanceledOnTouchOutside(false);
            receiveAzureThingNameDialog.setCancelable(false);
        }

    }

    public void dismissReceiveAzureThingNameDialog() {
        if (receiveAzureThingNameDialog != null) {
            if (receiveAzureThingNameDialog.isShowing()) {
                receiveAzureThingNameDialog.dismiss();
                receiveAzureThingNameDialog = null;
            }
        }
    }

    public void showConnectingDialog() {
        if (connectingDialog == null) {
            connectingDialog = new ProgressDialog(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
            connectingDialog.setMessage("Connecting Bluetooth Low Energy Device...");
            connectingDialog.show();

            int displayWidth = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowMetrics windowMetrics = DeviceControlActivity.getInstance().getWindowManager().getCurrentWindowMetrics();
                Insets insets = windowMetrics.getWindowInsets()
                        .getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
                displayWidth = windowMetrics.getBounds().width() - insets.left - insets.right;
            } else {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                DeviceControlActivity.getInstance().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                displayWidth = displayMetrics.widthPixels;
            }

            WindowManager.LayoutParams params = connectingDialog.getWindow().getAttributes();
            int dialogWindowWidth = (int) (displayWidth * 0.8f);
            params.width = dialogWindowWidth;
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            connectingDialog.getWindow().setAttributes((WindowManager.LayoutParams) params);
            connectingDialog.setCanceledOnTouchOutside(false);
            connectingDialog.setCancelable(false);
        }
    }

    public void dismissConnectingDialog() {
        if (connectingDialog != null) {
            if (connectingDialog.isShowing()) {
                connectingDialog.dismiss();
                connectingDialog = null;
            }
        }
    }

}

class SoftKeyboardDectectorView extends View {

    private boolean mShownKeyboard;
    private OnShownKeyboardListener mOnShownSoftKeyboard;
    private OnHiddenKeyboardListener onHiddenSoftKeyboard;

    public SoftKeyboardDectectorView(Context context) {
        this(context, null);
    }

    public SoftKeyboardDectectorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        Activity activity = (Activity)getContext();
        Rect rect = new Rect();
        activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        int statusBarHeight = rect.top;
        int screenHeight = activity.getWindowManager().getDefaultDisplay().getHeight();
        int diffHeight = (screenHeight - statusBarHeight) - h;
        if (diffHeight > 100 && !mShownKeyboard) {
            mShownKeyboard = true;
            onShownSoftKeyboard();
        } else if (diffHeight < 100 && mShownKeyboard) {
            mShownKeyboard = false;
            onHiddenSoftKeyboard();
        }
        super.onSizeChanged(w, h, oldw, oldh);
    }

    public void onHiddenSoftKeyboard() {
        if (onHiddenSoftKeyboard != null)
            onHiddenSoftKeyboard.onHiddenSoftKeyboard();
    }

    public void onShownSoftKeyboard() {
        if (mOnShownSoftKeyboard != null)
            mOnShownSoftKeyboard.onShowSoftKeyboard();
    }

    public void setOnShownKeyboard(OnShownKeyboardListener listener) {
        mOnShownSoftKeyboard = listener;
    }

    public void setOnHiddenKeyboard(OnHiddenKeyboardListener listener) {
        onHiddenSoftKeyboard = listener;
    }

    public interface OnShownKeyboardListener {
        public void onShowSoftKeyboard();
    }

    public interface OnHiddenKeyboardListener {
        public void onHiddenSoftKeyboard();
    }
}