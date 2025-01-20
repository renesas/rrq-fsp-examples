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

package com.renesas.wifi.DA16600;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Insets;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.Toast;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.renesas.wifi.DA16600.activity.DeviceControlActivity;
import com.renesas.wifi.DA16600.dialog.AddWiFiDialog;
import com.renesas.wifi.R;
import com.renesas.wifi.activity.MainActivity;
import com.renesas.wifi.util.MyLog;
import com.renesas.wifi.util.StaticDataSave;
import com.renesas.wifi.util.ThreadUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.renesas.wifi.DA16600.activity.DeviceControlActivity.WIFI_SVC_APSCAN_RES;
import static com.renesas.wifi.DA16600.activity.DeviceControlActivity.WIFI_SVC_WFACT_RES;

public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    public BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;

    private volatile boolean mNotificationsSubscribed = false;

    //status
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    public final static UUID WIFI_SVC_UUID =
            UUID.fromString(MyGattAttributes.WIFI_SVC_UUID);
    public final static UUID WIFI_SVC_WFCMD_UUID =
            UUID.fromString(MyGattAttributes.WIFI_SVC_WFCMD_UUID);
    public final static UUID WIFI_SVC_WFACT_RES_UUID =
            UUID.fromString(MyGattAttributes.WIFI_SVC_WFACT_RES_UUID);
    public final static UUID WIFI_SVC_APSCAN_RES_UUID =
            UUID.fromString(MyGattAttributes.WIFI_SVC_APSCAN_RES_UUID);
    public final static UUID WIFI_SVC_PROV_DATA_UUID =
            UUID.fromString(MyGattAttributes.WIFI_SVC_PROV_DATA_UUID);

    public final static UUID WIFI_SVC_AWS_DATA_UUID =
            UUID.fromString(MyGattAttributes.WIFI_SVC_AWS_DATA_UUID);

    public final static UUID WIFI_SVC_AZURE_DATA_UUID =
            UUID.fromString(MyGattAttributes.WIFI_SVC_AZURE_DATA_UUID);

    public enum WIFI_ACTION_RESULT {
        COMBO_WIFI_CMD_SCAN_AP_SUCCESS,
        COMBO_WIFI_CMD_SCAN_AP_FAIL,
        COMBO_WIFI_CMD_FW_BLE_DOWNLOAD_SUCCESS,
        COMBO_WIFI_CMD_FW_BLE_DOWNLOAD_FAIL,
        COMBO_WIFI_CMD_INQ_WIFI_STATUS_CONNECTED,
        COMBO_WIFI_CMD_INQ_WIFI_STATUS_NOT_CONNECTED,
        COMBO_WIFI_PROV_DATA_VALIDITY_CHK_ERR,
        COMBO_WIFI_PROV_DATA_SAVE_SUCCESS,
        COMBO_WIFI_CMD_MEM_ALLOC_FAIL,
        COMBO_WIFI_CMD_UNKNOWN_RCV,

        COMBO_WIFI_CMD_CALLBACK,  //100

        COMBO_WIFI_CMD_SELECTED_AP_SUCCESS, //101
        COMBO_WIFI_CMD_SELECTED_AP_FAIL,  //102
        COMBO_WIFI_PROV_WRONG_PW, //103
        COMBO_WIFI_CMD_NETWORK_INFO_CALLBACK,  //104
        COMBO_WIFI_PROV_AP_FAIL,  //105
        COMBO_WIFI_PROV_DNS_FAIL_SERVER_FAIL,  //106
        COMBO_WIFI_PROV_DNS_FAIL_SERVER_OK,  //107, in case wrong DNS name
        COMBO_WIFI_PROV_NO_URL_PING_FAIL,  //108
        COMBO_WIFI_PORV_NO_URL_PING_OK,  //109
        COMBO_WIFI_PROV_DNS_OK_PING_FAIL_N_SERVER_OK,  //110, in case AP gives wrong IP
        COMBO_WIFI_PROV_DNS_OK_PING_OK,  //111
        COMBO_WIFI_PROV_REBOOT_SUCCESS,  //112
        COMBO_WIFI_PROV_DNS_OK_PING_N_SERVER_FAIL,  //113

        COMBO_WIFI_CMD_AWS_CALLBACK,  //114

        COMBO_WIFI_CMD_AZURE_CALLBACK  //115

    }

    StringBuilder sb = new StringBuilder();

    String ssid = "";
    String password = "";
    String security = "";
    String svrUrl = "";
    String svrIp = "";
    String pingIp = "";

    @SuppressLint("MissingPermission")
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                MyLog.i("Connected to GATT server.");
                MyLog.i("Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                MyLog.i("Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                MyLog.i("onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == GATT_SUCCESS) {
                MyLog.i(">> onCharacteristicRead()");
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            MyLog.i(">> onCharacteristicChanged()");
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mNotificationsSubscribed = true;
                MyLog.i("Subscribed for characteristic[" + descriptor.getUuid() + "] notifications");
            } else {
                MyLog.i("Failed to subscribe for characteristic[" + descriptor.getUuid() + "] notifications: err=" + status);
            }
        }



        private void broadcastUpdate(final String action) {
            final Intent intent = new Intent(action);
            sendBroadcast(intent);
        }

        @SuppressLint("MissingPermission")
        private void broadcastUpdate(final String action,
                                     final BluetoothGattCharacteristic characteristic) {
            final Intent intent = new Intent(action);

            if (WIFI_SVC_UUID.equals(characteristic.getUuid())) {
                MyLog.i("== WIFI_SV_UUID ==");

            } else if (WIFI_SVC_WFCMD_UUID.equals(characteristic.getUuid())) {
                MyLog.i(">> broadcastUpdate :: WIFI_SVC_WFCMD_UUID ==");

            } else if (WIFI_SVC_WFACT_RES_UUID.equals(characteristic.getUuid())) {
                MyLog.i(">> broadcastUpdate :: WIFI_SVC_WFACT_RES_UUID");

                final byte[] data = characteristic.getValue();
                int intResponse = -1;
                WIFI_ACTION_RESULT actionResult = null;
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));

                    intResponse = getIntResponse(data);
                    MyLog.i(">> intResponse = " + intResponse);
                    actionResult = getWiFiActionResult(intResponse);
                    MyLog.i(">> WIFI_ACTION_RESULT = " + getWiFiActionResult(intResponse));

                    if (actionResult == WIFI_ACTION_RESULT.COMBO_WIFI_CMD_CALLBACK) {
                        if (StaticDataSave.device.equals("RRQ61400")) {
                            // Do not call any BLE operations here - we are inside async call and this can
                            // lead to undefined behavior (for example, missed notifications or dropped
                            // requests to characteristic)
                            // TODO: invent better method to pass event data to main UI thread then string
                            intent.putExtra(EXTRA_DATA, "100");
                        } else {
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            readCharacteristic(DeviceControlActivity.WIFI_SVC_PROV_DATA);
                        }

                    } else if (actionResult == WIFI_ACTION_RESULT.COMBO_WIFI_CMD_SCAN_AP_SUCCESS) {
                        if (StaticDataSave.device.equals("RRQ61400")) {
                            // Do not call any BLE operations here - we are inside async call and this can
                            // lead to undefined behavior (for example, missed notifications or dropped
                            // requests to characteristic)
                            // TODO: invent better method to pass event data to main UI thread then string
                            intent.putExtra(EXTRA_DATA, "1");
                        } else {
                            try {
                                Thread.sleep(100);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            readCharacteristic(WIFI_SVC_APSCAN_RES);
                        }
                    } else if (actionResult == WIFI_ACTION_RESULT.COMBO_WIFI_CMD_SCAN_AP_FAIL) {
                        DeviceControlActivity.getInstance().showScanFailDialog();
                    } else if (actionResult == WIFI_ACTION_RESULT.COMBO_WIFI_PROV_DATA_SAVE_SUCCESS) {
                        MyLog.i(">> COMBO_WIFI_PROV_DATA_SAVE_SUCCESS");
                    } else if (actionResult == WIFI_ACTION_RESULT.COMBO_WIFI_PROV_DATA_VALIDITY_CHK_ERR) {
                        MyLog.i(">> COMBO_WIFI_PROV_DATA_VALIDITY_CHK_ERR");
                    }
                    else if (actionResult == WIFI_ACTION_RESULT.COMBO_WIFI_CMD_AWS_CALLBACK) {
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        readCharacteristic(DeviceControlActivity.WIFI_SVC_AWS_DATA);
                    }
                    else if (actionResult == WIFI_ACTION_RESULT.COMBO_WIFI_CMD_AZURE_CALLBACK) {
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        readCharacteristic(DeviceControlActivity.WIFI_SVC_AZURE_DATA);
                    }
                }

            } else if (WIFI_SVC_APSCAN_RES_UUID.equals(characteristic.getUuid())) {
                MyLog.i(">> broadcastUpdate :: WIFI_SVC_APSCAN_RES_UUID");

                int total_len = -1;
                int remaining_len = -1;
                byte[] buffer = characteristic.getValue();

                total_len = getTotal(buffer);
                remaining_len = getRemain(buffer);

                MyLog.i(">> total_len = " + String.valueOf(total_len));
                MyLog.i(">> remaining_len = " + String.valueOf(remaining_len));

                int progress = (int) (100 - (((double) remaining_len / (double) total_len) * 100));
                MyLog.i(">> progress = " + String.valueOf(progress));
                sendProgress(String.valueOf(progress));

                String input = null;

                if (buffer.length == 244) {
                    input = new String(buffer, 4, buffer.length - 4);
                    sb.append(input);
                } else if (buffer.length < 244) {
                    input = new String(buffer, 4, buffer.length - 4);
                    sb.append(input);
                }

                if (remaining_len > 0) {
                    if (WIFI_SVC_APSCAN_RES != null) {
                        if (StaticDataSave.device.equals("RRQ61400")) {
                            // Do not call any BLE operations here - we are inside async call and this can
                            // lead to undefined behavior (for example, missed notifications or dropped
                            // requests to characteristic)
                            // TODO: invent better method to pass event data to main UI thread then string
                            intent.putExtra(EXTRA_DATA, "1");
                        } else {
                            try {
                                Thread.sleep(500);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            readCharacteristic(WIFI_SVC_APSCAN_RES);
                        }
                    }
                } else {
                    if (sb != null && DeviceControlActivity.getInstance() != null) {
                        MyLog.i("[WIFI_SVC_APSCAN_RES] received : " + sb.toString());
                        try {

                            JSONArray jsonArray = null;
                            jsonArray = new JSONArray(sb.toString());

                            DeviceControlActivity.getInstance().ssidList = new ArrayList<>();
                            DeviceControlActivity.getInstance().securityList = new ArrayList<>();
                            DeviceControlActivity.getInstance().signalList = new ArrayList<>();

                            if (jsonArray != null) {

                                for (int i = 0; i < jsonArray.length(); i++) {
                                    JSONObject obj = jsonArray.getJSONObject(i);
                                    String ssid = obj.getString("SSID");
                                    Integer securityType = obj.getInt("security_type");
                                    Integer signal = obj.getInt("signal_strength");

                                    if (ssid != null && securityType != null && signal != null) {
                                        DeviceControlActivity.getInstance().ssidList.add(ssid);
                                        DeviceControlActivity.getInstance().securityList.add(securityType);
                                        DeviceControlActivity.getInstance().signalList.add(signal);
                                    }
                                }

                                if (DeviceControlActivity.getInstance().ssidList != null) {
                                    MyLog.i("ssidList = " + DeviceControlActivity.getInstance().ssidList.toString());
                                }
                                if (DeviceControlActivity.getInstance().securityList != null) {
                                    MyLog.i("securityList = " + DeviceControlActivity.getInstance().securityList.toString());
                                }
                                if (DeviceControlActivity.getInstance().signalList != null) {
                                    MyLog.i("signalList = " + DeviceControlActivity.getInstance().signalList.toString());
                                }

                                DeviceControlActivity.getInstance().updateAPList();
                                DeviceControlActivity.getInstance().dismissScanningDialog();

                                sb.setLength(0);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ThreadUtils.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        if (DeviceControlActivity.getInstance().btn_apScan != null) {
                                            DeviceControlActivity.getInstance().btn_apScan.setEnabled(true);
                                            DeviceControlActivity.getInstance().btn_apScan.setButtonColor(getResources().getColor(R.color.fbutton_default_color));
                                        }

                                        if (DeviceControlActivity.getInstance().btn_hiddenWiFi != null) {
                                            DeviceControlActivity.getInstance().btn_hiddenWiFi.setEnabled(true);
                                            DeviceControlActivity.getInstance().btn_hiddenWiFi.setButtonColor(getResources().getColor(R.color.fbutton_default_color));
                                        }

                                        if (DeviceControlActivity.getInstance().btn_command != null) {
                                            DeviceControlActivity.getInstance().btn_command.setEnabled(true);
                                            DeviceControlActivity.getInstance().btn_command.setButtonColor(getResources().getColor(R.color.fbutton_default_color));
                                        }
                                        if (DeviceControlActivity.getInstance().btn_reset != null) {
                                            DeviceControlActivity.getInstance().btn_reset.setEnabled(true);
                                            DeviceControlActivity.getInstance().btn_reset.setButtonColor(getResources().getColor(R.color.fbutton_default_color));
                                        }
                                    }
                                });
                            }
                        }).start();
                    }
                }

            } else if (WIFI_SVC_PROV_DATA_UUID.equals(characteristic.getUuid())) {
                MyLog.i(">> broadcastUpdate ::  WIFI_SVC_PROV_DATA_UUID");
                byte[] buffer = characteristic.getValue();
                String input = null;
                int result = -1;
                input = new String(buffer, 0, buffer.length);
                if (input != null) {
                    MyLog.i("[BLE] received : " + input);
                    try {
                        JSONObject jsonObject = null;
                        jsonObject = new JSONObject(input);

                        if (jsonObject != null) {

                            if (jsonObject.has("result")) {
                                result = jsonObject.getInt("result");

                                if (result == 101) {
                                    MyLog.i(">> COMBO_WIFI_CMD_SELECTED_AP_SUCCESS");

                                    DeviceControlActivity.getInstance().sendChkNetworkCommand();

                                    Handler mHandler = new Handler(Looper.getMainLooper());
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            DeviceControlActivity.getInstance().showCheckingDialog();
                                        }
                                    }, 0);

                                } else if (result == 102) {
                                    MyLog.i(">> COMBO_WIFI_CMD_SELECTED_AP_FAIL");

                                    int count = 0;
                                    DeviceControlActivity.getInstance().sendNetworkinfo(
                                            StaticDataSave.pingAddress,
                                            StaticDataSave.svrAddress,
                                            StaticDataSave.svrPort,
                                            StaticDataSave.svrUrl);
                                    count++;
                                    MyLog.i("sendAPinfo send count = " + count);
                                    if (count == 5) {
                                        DeviceControlActivity.getInstance().showTxApInfoFailDialog();
                                    }
                                } else if (result == 103) {
                                    MyLog.i(">> COMBO_WIFI_PROV_WRONG_PW");
                                    MyLog.i(">> StaticDataSave.networkPassword = " + StaticDataSave.networkPassword);
                                    Handler mHandler = new Handler(Looper.getMainLooper());
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (StaticDataSave.isHidden == 0) {
                                                DeviceControlActivity.getInstance().showApWrongPwdDialog();
                                            } else if (StaticDataSave.isHidden == 1) {
                                                AddWiFiDialog addWiFiDialog = new AddWiFiDialog(DeviceControlActivity.getInstance().mContext);
                                                addWiFiDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                                                addWiFiDialog.setCancelable(false);
                                                addWiFiDialog.show();

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

                                                WindowManager.LayoutParams params = addWiFiDialog.getWindow().getAttributes();
                                                int dialogWindowWidth = (int) (displayWidth * 0.8f);
                                                params.width = dialogWindowWidth;
                                                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
                                                addWiFiDialog.getWindow().setAttributes((WindowManager.LayoutParams) params);
                                            }
                                        }
                                    }, 0);


                                } else if (result == 104) {

                                    MyLog.i(">> COMBO_WIFI_CMD_NETWORK_INFO_CALLBACK");

                                    Handler handler = DeviceControlActivity.mHandler;
                                    handler.removeMessages(DeviceControlActivity.HandleMsg.E_BLE_CMD_TIMEOUT);

                                    DeviceControlActivity.getInstance().sendAPinfo(
                                            StaticDataSave.networkSSID,
                                            StaticDataSave.networkSecurityNum,
                                            StaticDataSave.networkPassword,
                                            StaticDataSave.isHidden);

                                } else if (result == 105) {

                                    if (jsonObject.has("ssid")) {
                                        ssid = jsonObject.getString("ssid");
                                    }
                                    if (jsonObject.has("passwd")) {
                                        password = jsonObject.getString("passwd");
                                    }
                                    if (jsonObject.has("security")) {
                                        int securityNumber;
                                        securityNumber = jsonObject.getInt("security");
                                        if (securityNumber == 0) {
                                            security = "none";
                                        } else if (securityNumber == 1) {
                                            security = "WEP";
                                        } else if (securityNumber == 2) {
                                            security = "WPA";
                                        } else if (securityNumber == 3) {
                                            security = "WPA2";
                                        }
                                    }

                                    Handler mHandler = new Handler(Looper.getMainLooper());
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            DeviceControlActivity.getInstance().showApFailDialog(ssid, password, security);
                                        }
                                    }, 0);
                                } else if (result == 106) {
                                    if (jsonObject.has("svr_url")) {
                                        svrUrl = jsonObject.getString("svr_url");
                                    }
                                    if (jsonObject.has("ping_ip")) {
                                        pingIp = jsonObject.getString("ping_ip");
                                    }
                                    Handler mHandler = new Handler(Looper.getMainLooper());
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            DeviceControlActivity.getInstance().showDnsFailServerFailDialog(svrUrl, pingIp);
                                        }
                                    }, 0);
                                } else if (result == 107) {
                                    if (jsonObject.has("svr_url")) {
                                        svrUrl = jsonObject.getString("svr_url");
                                    }
                                    if (jsonObject.has("ping_ip")) {
                                        pingIp = jsonObject.getString("ping_ip");
                                    }
                                    Handler mHandler = new Handler(Looper.getMainLooper());
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            DeviceControlActivity.getInstance().showDnsFailServerOkDialog(svrUrl, pingIp);
                                        }
                                    }, 0);
                                } else if (result == 108) {
                                    if (jsonObject.has("ping_ip")) {
                                        pingIp = jsonObject.getString("ping_ip");
                                    }
                                    Handler mHandler = new Handler(Looper.getMainLooper());
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            DeviceControlActivity.getInstance().showNoUrlPingFailDialog(pingIp);
                                        }
                                    }, 0);
                                } else if (result == 109) {
                                    if (jsonObject.has("ping_ip")) {
                                        pingIp = jsonObject.getString("ping_ip");
                                    }
                                    Handler mHandler = new Handler(Looper.getMainLooper());
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            DeviceControlActivity.getInstance().showNoUrlPingOkDialog(pingIp);
                                        }
                                    }, 0);
                                } else if (result == 110) {
                                    if (jsonObject.has("svr_url")) {
                                        svrUrl = jsonObject.getString("svr_url");
                                    }
                                    if (jsonObject.has("svr_ip")) {
                                        svrIp = jsonObject.getString("svr_ip");
                                    }
                                    if (jsonObject.has("ping_ip")) {
                                        pingIp = jsonObject.getString("ping_ip");
                                    }
                                    Handler mHandler = new Handler(Looper.getMainLooper());
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            DeviceControlActivity.getInstance().showDnsOkPingFailServerOkDialog(svrUrl, svrIp, pingIp);
                                        }
                                    }, 0);
                                } else if (result == 111) {
                                    if (jsonObject.has("svr_url")) {
                                        svrUrl = jsonObject.getString("svr_url");
                                    }
                                    if (jsonObject.has("svr_ip")) {
                                        svrIp = jsonObject.getString("svr_ip");
                                    }
                                    Handler mHandler = new Handler(Looper.getMainLooper());
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            DeviceControlActivity.getInstance().showDnsOkPingOkDialog(svrUrl, svrIp);
                                        }
                                    }, 0);
                                } else if (result == 112) {

                                    MyLog.i("StaticDataSave.mode = " + StaticDataSave.mode);

                                    if (StaticDataSave.mode == 12 || StaticDataSave.mode == 13) {

                                        ThreadUtils.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (DeviceControlActivity.mContext != null) {
                                                    Handler handler = DeviceControlActivity.mHandler;
                                                    handler.sendEmptyMessage(DeviceControlActivity.HandleMsg.E_SHOW_REGISTERING_DIALOG);
                                                    handler.sendEmptyMessageDelayed(DeviceControlActivity.HandleMsg.E_DISMISS_REGISTERING_DIALOG, 60000);
                                                }
                                            }
                                        });

                                        try {
                                            Thread.sleep(61000);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                        if (DeviceControlActivity.mContext != null) {
                                            Intent intent1 = new Intent(DeviceControlActivity.mContext, MainActivity.class);
                                            startActivity(intent1.addFlags(FLAG_ACTIVITY_NEW_TASK));
                                            startActivity(intent1);
                                            DeviceControlActivity.getInstance().finishAffinity();
                                        }

                                    } else {

                                        Handler mHandler = new Handler(Looper.getMainLooper());
                                        mHandler.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                Toast.makeText(DeviceControlActivity.mContext, "The Device will be rebooted", Toast.LENGTH_SHORT).show();
                                            }
                                        }, 0);

                                        try {
                                            Thread.sleep(1000);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                        if (DeviceControlActivity.mContext != null) {
                                            Intent intent1 = new Intent(DeviceControlActivity.mContext, MainActivity.class);
                                            startActivity(intent1.addFlags(FLAG_ACTIVITY_NEW_TASK));
                                            startActivity(intent1);
                                            DeviceControlActivity.getInstance().finishAffinity();
                                        }
                                    }

                                } else if (result == 113) {
                                    if (jsonObject.has("svr_url")) {
                                        svrUrl = jsonObject.getString("svr_url");
                                    }
                                    if (jsonObject.has("svr_ip")) {
                                        svrIp = jsonObject.getString("svr_ip");
                                    }
                                    if (jsonObject.has("ping_ip")) {
                                        pingIp = jsonObject.getString("ping_ip");
                                    }
                                    Handler mHandler = new Handler(Looper.getMainLooper());
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            DeviceControlActivity.getInstance().showDnsOkPingFailServerFailDialog(svrUrl, svrIp, pingIp);
                                        }
                                    }, 0);

                                } else if (result == 114) {


                                } else if (result == 115) {

                                }
                            }


                        } else {
                            MyLog.i("json object invalid");
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    input = null;

                } else {
                    MyLog.i("input is null~~");
                }

            }
            else if (WIFI_SVC_AWS_DATA_UUID.equals(characteristic.getUuid())) {
                MyLog.i(">> broadcastUpdate ::  WIFI_SVC_AWS_DATA_UUID");
                byte[] buffer = characteristic.getValue();
                String input = null;

                input = new String(buffer, 0, buffer.length);
                if (input != null) {
                    MyLog.i("[BLE] received : " + input);
                    try {
                        JSONObject jsonObject = null;
                        jsonObject = new JSONObject(input);

                        if (jsonObject != null) {

                            if (jsonObject.has("thingName")) {
                                StaticDataSave.thingName = jsonObject.getString("thingName");
                                MyLog.i(">>> StaticDataSave.thingName = " + StaticDataSave.thingName);
                                StaticDataSave.saveData = DeviceControlActivity.mContext.getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                                editor.putString(StaticDataSave.thingNameKey, StaticDataSave.thingName);
                                editor.commit();

                                if (StaticDataSave.thingName != null && WIFI_SVC_AWS_DATA_UUID != null) {
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    DeviceControlActivity.getInstance().sendGetModeCommand();
                                }

                            } else if (jsonObject.has("mode")) {
                                StaticDataSave.mode = jsonObject.getInt("mode");
                                MyLog.i(">>> StaticDataSave.mode = " + StaticDataSave.mode);
                                StaticDataSave.saveData = DeviceControlActivity.mContext.getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                                editor.putInt(StaticDataSave.modeKey, StaticDataSave.mode);
                                editor.commit();

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ThreadUtils.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {

                                                DeviceControlActivity.getInstance().dismissReceiveAwsThingNameDialog();

                                                if (DeviceControlActivity.getInstance().btn_apScan != null) {
                                                    DeviceControlActivity.getInstance().btn_apScan.setEnabled(true);
                                                    DeviceControlActivity.getInstance().btn_apScan.setButtonColor(getResources().getColor(R.color.fbutton_default_color));
                                                }

                                                if (DeviceControlActivity.getInstance().btn_hiddenWiFi != null) {
                                                    DeviceControlActivity.getInstance().btn_hiddenWiFi.setEnabled(true);
                                                    DeviceControlActivity.getInstance().btn_hiddenWiFi.setButtonColor(getResources().getColor(R.color.fbutton_default_color));
                                                }

                                                if (DeviceControlActivity.getInstance().btn_command != null) {
                                                    DeviceControlActivity.getInstance().btn_command.setEnabled(true);
                                                    DeviceControlActivity.getInstance().btn_command.setButtonColor(getResources().getColor(R.color.fbutton_default_color));
                                                }
                                                if (DeviceControlActivity.getInstance().btn_reset != null) {
                                                    DeviceControlActivity.getInstance().btn_reset.setEnabled(true);
                                                    DeviceControlActivity.getInstance().btn_reset.setButtonColor(getResources().getColor(R.color.fbutton_default_color));
                                                }
                                            }
                                        });
                                    }
                                }).start();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            } else if (WIFI_SVC_AZURE_DATA_UUID.equals(characteristic.getUuid())) {
                MyLog.i(">> broadcastUpdate ::  WIFI_SVC_AZURE_DATA_UUID");
                byte[] buffer = characteristic.getValue();
                String input = null;

                input = new String(buffer, 0, buffer.length);
                if (input != null) {
                    MyLog.i("[BLE] received : " + input);
                    try {
                        JSONObject jsonObject = null;
                        jsonObject = new JSONObject(input);

                        if (jsonObject != null) {

                            if (jsonObject.has("thingName")) {
                                StaticDataSave.thingName = jsonObject.getString("thingName");
                                MyLog.i(">>> StaticDataSave.thingName = " + StaticDataSave.thingName);
                                StaticDataSave.saveData = DeviceControlActivity.mContext.getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                                editor.putString(StaticDataSave.thingNameKey, StaticDataSave.thingName);
                                editor.commit();

                                if (StaticDataSave.thingName != null && WIFI_SVC_AZURE_DATA_UUID != null) {
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    DeviceControlActivity.getInstance().sendGetAzureConStringCommand();
                                }

                            } else if (jsonObject.has("azureConString")) {
                                StaticDataSave.azureConString = jsonObject.getString("azureConString");
                                MyLog.i(">>> StaticDataSave.azureConString = " + StaticDataSave.azureConString);
                                StaticDataSave.saveData = DeviceControlActivity.mContext.getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                                editor.putString(StaticDataSave.azureConStringKey, StaticDataSave.azureConString);
                                editor.commit();

                                if (StaticDataSave.azureConString != null && WIFI_SVC_AZURE_DATA_UUID != null) {
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    DeviceControlActivity.getInstance().sendGetModeCommand();
                                }
                            } else if (jsonObject.has("mode")) {
                                StaticDataSave.mode = jsonObject.getInt("mode");
                                MyLog.i(">>> StaticDataSave.mode = " + StaticDataSave.mode);
                                StaticDataSave.saveData = DeviceControlActivity.mContext.getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = StaticDataSave.saveData.edit();
                                editor.putInt(StaticDataSave.modeKey, StaticDataSave.mode);
                                editor.commit();

                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ThreadUtils.runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {

                                                DeviceControlActivity.getInstance().dismissReceiveAzureThingNameDialog();

                                                if (DeviceControlActivity.getInstance().btn_apScan != null) {
                                                    DeviceControlActivity.getInstance().btn_apScan.setEnabled(true);
                                                    DeviceControlActivity.getInstance().btn_apScan.setButtonColor(getResources().getColor(R.color.fbutton_default_color));
                                                }

                                                if (DeviceControlActivity.getInstance().btn_hiddenWiFi != null) {
                                                    DeviceControlActivity.getInstance().btn_hiddenWiFi.setEnabled(true);
                                                    DeviceControlActivity.getInstance().btn_hiddenWiFi.setButtonColor(getResources().getColor(R.color.fbutton_default_color));
                                                }

                                                if (DeviceControlActivity.getInstance().btn_command != null) {
                                                    DeviceControlActivity.getInstance().btn_command.setEnabled(true);
                                                    DeviceControlActivity.getInstance().btn_command.setButtonColor(getResources().getColor(R.color.fbutton_default_color));
                                                }
                                                if (DeviceControlActivity.getInstance().btn_reset != null) {
                                                    DeviceControlActivity.getInstance().btn_reset.setEnabled(true);
                                                    DeviceControlActivity.getInstance().btn_reset.setButtonColor(getResources().getColor(R.color.fbutton_default_color));
                                                }
                                            }
                                        });
                                    }
                                }).start();

                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            } else {
                // For all other profiles, writes the data formatted in HEX.
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));
                    MyLog.i(String.format("UUID : " + characteristic.getUuid()));
                    intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
                    MyLog.i(">> " + new String(data) + "\n" + stringBuilder.toString());
                }
            }
            sendBroadcast(intent);
        }
    };

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                MyLog.e("Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            MyLog.e("Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt) {
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                return bool;
            }
        } catch (Exception localException) {
            MyLog.e("An exception occurred while refreshing device");
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            MyLog.i("BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            MyLog.i("Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            MyLog.i("Device not found.  Unable to connect.");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBluetoothGatt= device.connectGatt( this, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        }

        refreshDeviceCache(mBluetoothGatt);

        MyLog.i("Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

    @SuppressLint("MissingPermission")
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            MyLog.i("BluetoothAdapter not initialized");
            return;
        }
        MyLog.i("mBluetoothGatt.disconnect()");
        mBluetoothGatt.disconnect();
    }

    @SuppressLint("MissingPermission")
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    @SuppressLint("MissingPermission")
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            MyLog.i("BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    @SuppressLint("MissingPermission")
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        MyLog.i(">> setCharacteristicNotification");
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            MyLog.i("BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        if (StaticDataSave.device.equals("RRQ61400")) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }

            final UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
            final BluetoothGattDescriptor cccd = characteristic.getDescriptor(CCCD_UUID);
            cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(cccd);
        }

        if (WIFI_SVC_WFACT_RES_UUID.equals(characteristic.getUuid())) {
            MyLog.i("Subscribed to WIFI_SVC_WFACT_RES notification [" + characteristic.getUuid().toString() + "]");
            if (!StaticDataSave.device.equals("RRQ61400")) {
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                readCharacteristic(WIFI_SVC_WFACT_RES);
            }
        }
        else if (WIFI_SVC_APSCAN_RES_UUID.equals(characteristic.getUuid())) {
            MyLog.i("Subscribed to WIFI_SVC_APSCAN_RES_UUID notification [" + characteristic.getUuid().toString() + "]");
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
            readCharacteristic(WIFI_SVC_APSCAN_RES);
        }
        else if (WIFI_SVC_PROV_DATA_UUID.equals(characteristic.getUuid())) {
            MyLog.i("Subscribed to WIFI_SVC_WFACT_RES notification [" + characteristic.getUuid().toString() + "]");
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
            readCharacteristic(DeviceControlActivity.WIFI_SVC_PROV_DATA);
        }
    }

    @SuppressLint("MissingPermission")
    public void disableNotification(UUID uuid) {
        MyLog.i("disableNotification()");
        if (mBluetoothGatt == null) {
            MyLog.e("BluetoothGatt is null");
            return;
        }

        BluetoothGattService service = mBluetoothGatt.getService(WIFI_SVC_UUID);
        if (service == null) {
            MyLog.e("BluetoothService is null");
            return;
        }

        BluetoothGattCharacteristic characteristic = service.getCharacteristic(uuid);
        if (characteristic == null) {
            MyLog.e("Characteristic not found, UUID: " + uuid.toString());
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(characteristic, false);
        BluetoothGattDescriptor desc = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        if (desc == null) {
            MyLog.i("Descriptor not found for characteristic: " + characteristic.getUuid().toString());
            return;
        }

        desc.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(desc);
    }

    @SuppressLint("MissingPermission")
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic, String stringData) {
        // do not issue any command until subscription is done
        if (StaticDataSave.device.equals("RRQ61400")) {
            int retries = 5;
            while (!mNotificationsSubscribed && (retries > 0)) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                retries--;
            }
            if (retries == 0) {
                MyLog.e("Unable to subscribe for BLE notifications");
                return;
            }
            characteristic.setValue(stringData);
            mBluetoothGatt.writeCharacteristic(characteristic);
        } else {
            characteristic.setValue(stringData);
            mBluetoothGatt.writeCharacteristic(characteristic);
            MyLog.i("Write Characteristic: " + characteristic.getUuid());
        }
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public int getTotal(byte bytes[]) {
        return ((((int) bytes[3] & 0xff) << 8) |
                (((int) bytes[2] & 0xff)));
    }

    public int getRemain(byte bytes[]) {
        return ((((int) bytes[1] & 0xff) << 8) |
                (((int) bytes[0] & 0xff)));
    }

    public int getIntResponse(byte bytes[]) {
        return ((((int) bytes[1] & 0xff) << 8) |
                (((int) bytes[0] & 0xff)));
    }

    public WIFI_ACTION_RESULT getWiFiActionResult(int intResponse) {

        WIFI_ACTION_RESULT actionResult = null;

        if (intResponse == 1) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_CMD_SCAN_AP_SUCCESS;
        } else if (intResponse == 2) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_CMD_SCAN_AP_FAIL;
        } else if (intResponse == 3) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_CMD_FW_BLE_DOWNLOAD_SUCCESS;
        } else if (intResponse == 4) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_CMD_FW_BLE_DOWNLOAD_FAIL;
        } else if (intResponse == 5) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_CMD_INQ_WIFI_STATUS_CONNECTED;
        } else if (intResponse == 6) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_CMD_INQ_WIFI_STATUS_NOT_CONNECTED;
        } else if (intResponse == 7) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_PROV_DATA_VALIDITY_CHK_ERR;
        } else if (intResponse == 8) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_PROV_DATA_SAVE_SUCCESS;
        } else if (intResponse == 9) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_CMD_MEM_ALLOC_FAIL;
        } else if (intResponse == 10) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_CMD_UNKNOWN_RCV;
        } else if (intResponse == 100) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_CMD_CALLBACK;
        } else if (intResponse == 101) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_CMD_SELECTED_AP_SUCCESS;
        } else if (intResponse == 102) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_CMD_SELECTED_AP_FAIL;
        } else if (intResponse == 103) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_PROV_WRONG_PW;
        } else if (intResponse == 104) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_CMD_NETWORK_INFO_CALLBACK;
        } else if (intResponse == 105) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_PROV_AP_FAIL;
        } else if (intResponse == 106) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_PROV_DNS_FAIL_SERVER_FAIL;
        } else if (intResponse == 107) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_PROV_DNS_FAIL_SERVER_OK;
        } else if (intResponse == 108) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_PROV_NO_URL_PING_FAIL;
        } else if (intResponse == 109) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_PORV_NO_URL_PING_OK;
        } else if (intResponse == 110) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_PROV_DNS_OK_PING_FAIL_N_SERVER_OK;
        } else if (intResponse == 111) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_PROV_DNS_OK_PING_OK;
        } else if (intResponse == 112) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_PROV_REBOOT_SUCCESS;
        } else if (intResponse == 113) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_PROV_DNS_OK_PING_N_SERVER_FAIL;
        }
        else if (intResponse == 114) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_CMD_AWS_CALLBACK;
        }
        else if (intResponse == 115) {
            actionResult = WIFI_ACTION_RESULT.COMBO_WIFI_CMD_AZURE_CALLBACK;
        }
        return actionResult;
    }

    private void sendProgress(String _progress) {
        Intent intent = new Intent("ProgressData");
        intent.putExtra("progress", _progress);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @SuppressLint("MissingPermission")
    public void refreshServices() {
        MyLog.i(">> refreshServices()");
        try {
            // BluetoothGatt gatt
            final Method refresh = mBluetoothGatt.getClass().getMethod("refresh");
            if (refresh != null) {
                refresh.invoke(mBluetoothGatt);
            }
        } catch (Exception e) {
            // Log it
            e.printStackTrace();
        }

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mBluetoothGatt.discoverServices();
    }
}