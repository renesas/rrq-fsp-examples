package com.renesas.wifi.DA16200.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkSpecifier;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.renesas.wifi.DA16200.adapter.device.DeviceListViewAdapter;
import com.renesas.wifi.DA16200.adapter.device.DeviceRowItem;
import com.renesas.wifi.R;
import com.renesas.wifi.activity.BaseActivity;
import com.renesas.wifi.activity.MainActivity;
import com.renesas.wifi.util.CustomToast;
import com.renesas.wifi.util.FButton;
import com.renesas.wifi.util.MyLog;
import com.renesas.wifi.util.StaticDataSave;
import com.renesas.wifi.util.WIFIUtil;
import com.thanosfisherman.wifiutils.WifiUtils;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionErrorCode;
import com.thanosfisherman.wifiutils.wifiConnect.ConnectionSuccessListener;
import com.thanosfisherman.wifiutils.wifiRemove.RemoveErrorCode;
import com.thanosfisherman.wifiutils.wifiRemove.RemoveSuccessListener;
import org.apache.commons.lang3.StringUtils;


public class SelectDeviceActivity extends BaseActivity implements View.OnClickListener {

    private Context mContext;

    private WifiManager mWifiManager;
    public static ConnectivityManager mConnectivityManager;
    public static ConnectivityManager.NetworkCallback mNetworkCallback;

    //Receiver
    private WifiProvisioningReceiver mReceiver;

    private String currentSSID = "";

    //UI resources
    private CustomToast customToast;
    private LinearLayout ll_progressConnecting;

    private ProgressBar progressConnecting;

    private TextView tv_currentSSID;
    private ImageView iv_back;
    private LinearLayout ll_next;
    private FButton btn_next;
    private TextView tv_connecting;
    private TextView tv_connected;
    private MaterialDialog connectTimeoutDialog;
    //flag
    private boolean mIsDialogApConnected = false;
    private boolean mIsStartConnectDialogAp = false;

    //handler
    private DeviceHandler mHandler;

    public LinearLayout ll_progressScanning;
    ProgressBar progressScanning;
    public TextView tv_noList;

    List<ScanResult> wifiScanList = null;
    List<ScanResult> specificWifiScanList = null;
    public ListView listView;
    public DeviceListViewAdapter adapter;
    public ArrayList<DeviceRowItem> deviceRowItems;
    String wifis[];
    String specificWifis[];

    public final static int SCAN_TIMEOUT_TIME = 20000; //msec

    public boolean isDialogDA16200 = false;
    public boolean isRenesasDA16200 = false;
    public boolean isRenesasIoTWiFi = false;

    String getSsid = "";

    public static SelectDeviceActivity instance;

    public static SelectDeviceActivity getInstance() {
        return instance;
    }

    //handler event
    public static class HandleMsg {
        public static final int RENESAS_AP_CONNECTED = 0;
        public static final int RENESAS_AP_CONNECT_TIMEOUT = 1;
        public static final int DEVICE_SCAN_TIMEOUT = 2;
    }

    /**
     ****************************************************************************************
     * @brief Handler class for Device activity
     * @param
     * @return none
     ****************************************************************************************
     */
    private static final class DeviceHandler extends Handler {

        private final WeakReference<SelectDeviceActivity> ref;

        public DeviceHandler(SelectDeviceActivity act) {
            ref = new WeakReference<>(act);
        }

        @Override
        public void handleMessage(Message msg) {
            final SelectDeviceActivity act = ref.get();

            if (act != null) {
                switch (msg.what) {
                    case HandleMsg.RENESAS_AP_CONNECTED:
                        MyLog.i("RENESAS AP Connected");

                        if (act.mIsDialogApConnected || act.mIsStartConnectDialogAp == false) {
                            break;
                        }

                        act.mIsDialogApConnected = true;
                        act.mIsStartConnectDialogAp = false;

                        act.customToast.showToast(act.mContext, R.string.dialog_ap_connect_complete, Toast.LENGTH_LONG);

                        act.ll_progressConnecting.setVisibility(View.INVISIBLE);
                        act.tv_connecting.setVisibility(View.INVISIBLE);
                        act.tv_connected.setVisibility(View.VISIBLE);
                        act.ll_next.setVisibility(View.VISIBLE);
                        break;

                    case HandleMsg.RENESAS_AP_CONNECT_TIMEOUT:
                        MyLog.i("RENESAS AP Connect Timeout");
                        act.showConnectTimeoutDialog();
                        break;

                    case HandleMsg.DEVICE_SCAN_TIMEOUT:

                        MyLog.i("DEVICE_SCAN_TIMEOUT");
                        try {
                            if (act.wifiScanReceiver != null) {
                                act.unregisterReceiver(act.wifiScanReceiver);
                            }
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }

                        act.ll_progressScanning.setVisibility(View.INVISIBLE);
                        act.tv_noList.setVisibility(View.VISIBLE);
                        act.listView.setVisibility(View.INVISIBLE);
                        break;

                    default:
                        break;
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MyLog.i("== onCreate() ==");

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.da16200_activity_select_device);

        instance = this;

        mContext = SelectDeviceActivity.this;
        customToast = new CustomToast(mContext);
        mHandler = new DeviceHandler(this);

        mWifiManager = (WifiManager) mContext.getSystemService(WIFI_SERVICE);

        registerWiFiScanReceiver();

        iv_back = findViewById(R.id.iv_back);
        iv_back.setOnClickListener(this);

        tv_connecting = findViewById(R.id.tv_connecting);
        tv_connecting.setVisibility(View.INVISIBLE);

        tv_connected = findViewById(R.id.tv_connected);
        tv_connected.setVisibility(View.INVISIBLE);

        ll_next = (LinearLayout) findViewById(R.id.ll_next);
        ll_next.setVisibility(View.INVISIBLE);

        btn_next = (FButton) findViewById(R.id.btn_next);
        btn_next.setOnClickListener(this);

        ll_progressConnecting = (LinearLayout) findViewById(R.id.ll_progressConnecting);
        progressConnecting = (ProgressBar) findViewById(R.id.progressConnecting);
        ll_progressConnecting.setVisibility(View.INVISIBLE);

        tv_noList = (TextView) findViewById(R.id.tv_noList);
        tv_noList.setVisibility(View.INVISIBLE);

        ll_progressScanning = (LinearLayout) findViewById(R.id.ll_progressScanning);
        progressScanning = (ProgressBar) findViewById(R.id.progressScanning);

        listView = (ListView) findViewById(R.id.device_wifi_list);
        listView.setVisibility(View.INVISIBLE);

        tv_currentSSID = (TextView) findViewById(R.id.tv_currentSSID);
        currentSSID = WIFIUtil.getSSID(mContext);
        MyLog.d(">> currentSSID = " + currentSSID);
        tv_currentSSID.setText(currentSSID);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (deviceRowItems.size() > 0) {
                    WifiUtils.withContext(mContext)
                            .remove(WIFIUtil.getSSID(mContext), new RemoveSuccessListener() {
                                @Override
                                public void success() {
                                    MyLog.d("Remove success!");
                                }

                                @Override
                                public void failed(@NonNull RemoveErrorCode errorCode) {
                                    MyLog.d("Failed to disconnect and remove: $errorCode");
                                }
                            });

                    StaticDataSave.deviceSSID = deviceRowItems.get(position).getSSID();
                    MyLog.i("ssid = " + StaticDataSave.deviceSSID);

                    listView.setVisibility(View.INVISIBLE);
                    ll_progressConnecting.setVisibility(View.VISIBLE);

                    WifiUtils.withContext(getApplicationContext())
                            .connectWith(StaticDataSave.deviceSSID, StaticDataSave.devicePassword)
                            .setTimeout(40000)
                            .onConnectionResult(new ConnectionSuccessListener() {
                                @Override
                                public void success() {
                                    currentSSID = WIFIUtil.getSSID(mContext);
                                    MyLog.d("Connect Success : " + currentSSID);
                                    tv_currentSSID.setText(currentSSID);
                                    mHandler.removeMessages(HandleMsg.RENESAS_AP_CONNECT_TIMEOUT);
                                    mHandler.sendEmptyMessageDelayed(HandleMsg.RENESAS_AP_CONNECTED, 1000);
                                }

                                @Override
                                public void failed(@NonNull ConnectionErrorCode errorCode) {
                                    MyLog.d("EPIC FAIL!" + errorCode.toString());
                                }
                            })
                            .start();
                }

            }
        });
    }


    protected void onPause() {
        super.onPause();
        MyLog.i("=== onPause() ===");

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        MyLog.i("=== onDestroy() ===");
        super.onDestroy();
        mHandler.removeMessages(HandleMsg.RENESAS_AP_CONNECT_TIMEOUT);

        try {
            if (wifiScanReceiver != null) {
                this.unregisterReceiver(wifiScanReceiver);
            }
        } catch (IllegalArgumentException e) {
            wifiScanReceiver = null;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        initFlag();

        WifiUtils.withContext(mContext)
                .remove(WIFIUtil.getSSID(mContext), new RemoveSuccessListener() {
                    @Override
                    public void success() {
                        MyLog.d("Remove success!");
                    }

                    @Override
                    public void failed(@NonNull RemoveErrorCode errorCode) {
                        MyLog.d("Failed to disconnect and remove: $errorCode");
                    }
                });

        Intent main = new Intent(SelectDeviceActivity.this, DA16200MainActivity.class);
        main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(main);
        finishAffinity();
    }

    protected void onResume() {
        super.onResume();
        MyLog.i("== onResume() ==");
        mIsDialogApConnected = false;
        mIsStartConnectDialogAp = true;
    }


    @Override
    public void onClick(View view) {

        if (view == iv_back) {

            initFlag();

            WifiUtils.withContext(mContext)
                    .remove(WIFIUtil.getSSID(mContext), new RemoveSuccessListener() {
                        @Override
                        public void success() {
                            MyLog.d("Remove success!");
                        }

                        @Override
                        public void failed(@NonNull RemoveErrorCode errorCode) {
                            MyLog.d("Failed to disconnect and remove: $errorCode");
                        }
                    });

            Intent main = new Intent(SelectDeviceActivity.this, DA16200MainActivity.class);
            main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(main);
            finishAffinity();

        } else if (view == btn_next) {

            if (StaticDataSave.deviceSSID != null) {
                if (WIFIUtil.getSSID(mContext).contains(StaticDataSave.deviceSSID)) {

                    Intent main = new Intent(SelectDeviceActivity.this, SelectNetworkActivity.class);
                    main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(main);
                    finishAffinity();

                }
            }
        }
    }

    private void initFlag() {

        StaticDataSave.networkSSID = null;
        StaticDataSave.networkSecurity = false;
        StaticDataSave.networkPassword = null;

        StaticDataSave.apConnectionResult = -1;
        StaticDataSave.rebootResult = -1;

    }

    /**
     ****************************************************************************************
     * @brief connect Wi-Fi AP.
     * @param
     * @return none
     ****************************************************************************************
     */
    private void onWifiConnect(String wifi_ssid, String wifi_pwd) {

        ll_progressConnecting.setVisibility(View.VISIBLE);

        MyLog.i("onWifiConnect():" + wifi_ssid);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            MyLog.i("Start Wifi Connect for Version Code upper P");
            NetworkSpecifier specifier;

            if (StringUtils.isBlank(wifi_pwd)) {
                specifier = new WifiNetworkSpecifier.Builder()
                        .setSsid(wifi_ssid)
                        .build();
            } else {
                specifier = new WifiNetworkSpecifier.Builder()
                        .setSsid(wifi_ssid)
                        .setWpa2Passphrase(wifi_pwd)
                        .build();
            }

            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                    .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .setNetworkSpecifier(specifier)
                    .build();

            mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            mConnectivityManager.bindProcessToNetwork(null);  //add 221128
            mNetworkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    MyLog.i("Request Network is onAvailable");
                    super.onAvailable(network);
                    if (mIsDialogApConnected == false) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                currentSSID = WIFIUtil.getSSID(mContext);
                                tv_currentSSID.setText(currentSSID);
                                mHandler.removeMessages(HandleMsg.RENESAS_AP_CONNECT_TIMEOUT);
                                if (currentSSID.equals("Dialog_DA16200")
                                        || currentSSID.equals("Renesas_DA16200")
                                        || currentSSID.equals("Renesas_IoT_WiFi")
                                ) {
                                    WIFIUtil.setDefaultNetworkForNoInternet(mContext);  //add 221128
                                    mHandler.sendEmptyMessageDelayed(HandleMsg.RENESAS_AP_CONNECTED, 5000);
                                }
                            }
                        });

                    }
                }

                @Override
                public void onUnavailable() {
                    super.onUnavailable();
                    MyLog.i("Request Network onUnavailable");
                }
            };

            mConnectivityManager.requestNetwork(networkRequest, mNetworkCallback);
        } else {
            MyLog.i("Start Wifi Connect for Version Code under P");
            WifiConfiguration wificonfig = new WifiConfiguration();
            wificonfig.SSID = String.format("\"%s\"", wifi_ssid);

            // open network
            if (wifi_pwd == null) {
                wificonfig.status = WifiConfiguration.Status.DISABLED;
                wificonfig.priority = 40;

                wificonfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                wificonfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wificonfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                wificonfig.allowedAuthAlgorithms.clear();
                wificonfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);

                wificonfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                wificonfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                wificonfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wificonfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            } else {
                wificonfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                wificonfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                wificonfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                wificonfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                wificonfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                wificonfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                wificonfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                wificonfig.preSharedKey = String.format("\"%s\"", wifi_pwd);
            }

            int netId = mWifiManager.addNetwork(wificonfig);

            if (netId == -1) {
                MyLog.e("add network fail!");

                @SuppressLint("MissingPermission") List<WifiConfiguration> wifiConfig = mWifiManager.getConfiguredNetworks();

                int tmpNetId = -1;

                if (wifiConfig != null) {
                    for (int i = 0; i < wifiConfig.size(); i++) {
                        if (wifiConfig.get(i) != null &&
                                StringUtils.isNotBlank(wifiConfig.get(i).SSID)) {
                            MyLog.i("SSID:" + wifiConfig.get(i).SSID + "  netID:" + wifiConfig.get(i).networkId);

                            if (wifiConfig.get(i).SSID.contains(wifi_ssid)) {
                                MyLog.i("Wifi Ap found.");
                                tmpNetId = wifiConfig.get(i).networkId;
                                MyLog.i("Net ID:" + tmpNetId);
                                break;
                            }
                        }
                    }
                }

                if (tmpNetId != -1) {
                    mWifiManager.disconnect();
                    mWifiManager.enableNetwork(tmpNetId, true);
                    mWifiManager.reconnect();
                } else {
                    MyLog.e("Wifi connection failed");
                }
            } else {
                mWifiManager.disconnect();
                mWifiManager.enableNetwork(netId, true);
                mWifiManager.reconnect();
            }
        }
    }

    /**
     ****************************************************************************************
     * @brief Broadcast Receiver
     * @param
     * @return none
     ****************************************************************************************
     */
    private class WifiProvisioningReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                MyLog.i("SCAN_RESULTS_AVAILABLE_ACTION");

            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {

                MyLog.i("Network State Changed");


                NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);

                if (ConnectivityManager.TYPE_WIFI == netInfo.getType()) {

                    boolean isConnected = netInfo.isConnected();

                    String ssid = mWifiManager.getConnectionInfo() != null ?
                            mWifiManager.getConnectionInfo().getSSID() : null;

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {

                        if (isConnected && mIsDialogApConnected == false) {

                            if (ssid != null) {
                                final String connectedSsid = ssid.replace("\"", "");

                                MyLog.i("Connected SSID:" + connectedSsid);

                                if (connectedSsid != null) {
                                    MyLog.d(">> connectedSsid = " + connectedSsid);
                                    tv_currentSSID.setText(connectedSsid);

                                    if (StringUtils.isNotBlank(connectedSsid) && connectedSsid.contains(StaticDataSave.deviceSSID)) {
                                        mHandler.removeMessages(HandleMsg.RENESAS_AP_CONNECT_TIMEOUT);

                                        if (StaticDataSave.deviceSSID.equals("Dialog_DA16200")
                                                || StaticDataSave.deviceSSID.equals("Renesas_DA16200")
                                                || StaticDataSave.deviceSSID.equals("Renesas_IoT_WiFi")
                                        ) {
                                            mHandler.sendEmptyMessageDelayed(HandleMsg.RENESAS_AP_CONNECTED, 5000);
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     ****************************************************************************************
     * @brief Register Broadcast Receiver
     * @param
     * @return none
     ****************************************************************************************
     */
    private void onRegisterReceiver() {
        MyLog.i("onRegisterReceiver");

        IntentFilter filter;
        filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mReceiver = new WifiProvisioningReceiver();
        mContext.registerReceiver(mReceiver, filter);
    }

    /**
     ****************************************************************************************
     * @brief Unregister Broadcast Receiver
     * @param
     * @return none
     ****************************************************************************************
     */
    private void onUnRegisterReceiver() {
        MyLog.i("onUnRegisterReceiver");

        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
            mReceiver = null;
        }
    }

    private static boolean isWifiConnected(Context _context) {

        boolean isConnected = false;

        ConnectivityManager connManager = (ConnectivityManager) _context.getSystemService(CONNECTIVITY_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network network = connManager.getActiveNetwork();
            NetworkCapabilities capabilities = connManager.getNetworkCapabilities(network);
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    isConnected = true;
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    return false;
                }
            }
        } else {
            NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (wifi.isConnected()) {
                return true;
            }
            return false;
        }

        return isConnected;

    }

    private String getCurrentSsid(Context context) {
        String ssid = null;
        if (isWifiConnected(context)) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                ssid = connectionInfo.getSSID();
                ssid = ssid.replaceAll("\"", "");
            }
        }
        return ssid;
    }

    private void showConnectTimeoutDialog() {
        if (connectTimeoutDialog != null) {
            connectTimeoutDialog.dismiss();
        }
        if (mContext != null) {
            connectTimeoutDialog = new MaterialDialog.Builder(mContext)
                    .theme(Theme.LIGHT)
                    .title("Connection timeout")
                    .titleGravity(GravityEnum.CENTER)
                    .titleColor(mContext.getResources().getColor(R.color.black))
                    .content("1. Please check if the SDK version is 2.3.4.1 or higher.\n" +
                            "2. Make sure that the device is in provisioning mode and the Soft-AP SSID is set to <Renesas_IoT_WiFi> or <Renesas_DA16200> or <Dialog_DA16200> or <RZ_RRQ66620>.")
                    .contentColor(mContext.getResources().getColor(R.color.black))
                    .positiveText("OK")
                    .positiveColor(mContext.getResources().getColor(R.color.blue3))
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dismissConnectTimeoutDialog();
                            Intent main = new Intent(SelectDeviceActivity.this, MainActivity.class);
                            main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(main);
                            finishAffinity();
                        }
                    })
                    .build();
            connectTimeoutDialog.getWindow().setGravity(Gravity.CENTER);
            connectTimeoutDialog.show();
        }

    }

    private void dismissConnectTimeoutDialog() {
        if (connectTimeoutDialog != null) {
            connectTimeoutDialog.dismiss();
        }
    }

    void registerWiFiScanReceiver() {
        MyLog.i("== registerWiFiScanReceiver() ==");
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(wifiScanReceiver, filter);

        mWifiManager.startScan();
        mHandler.sendEmptyMessageDelayed(HandleMsg.DEVICE_SCAN_TIMEOUT, SCAN_TIMEOUT_TIME);

    }

    public BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MyLog.i("== WifiScanReceiver onReceive ==");
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                MyLog.i("== SCAN_RESULTS_AVAILABLE_ACTION ==");
                getWIFIScanResult();

                if (specificWifiScanList.size() > 0) {
                    unregisterReceiver(wifiScanReceiver);  //stop wifi scan
                } else {
                    unregisterReceiver(wifiScanReceiver);  //stop wifi scan
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    registerWiFiScanReceiver();
                }

            } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                MyLog.i("== NETWORK_STATE_CHANGED_ACTION ==");
                mContext.sendBroadcast(new Intent("wifi.ON_NETWORK_STATE_CHANGED"));
            }
        }
    };

    public void getWIFIScanResult() {

        MyLog.i("== getWIFIScanResult() ==");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        wifiScanList = mWifiManager.getScanResults();
        specificWifiScanList = new ArrayList<ScanResult>();

        Collections.sort(wifiScanList, new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult lhs, ScanResult rhs) {
                if (lhs.level == rhs.level) {
                    return lhs.SSID.compareToIgnoreCase(rhs.SSID);
                }
                return rhs.level - lhs.level;
            }
        });

        wifis = new String[wifiScanList.size()];
        for(int i = 0; i < wifiScanList.size(); i++) {
            wifis[i] = ((wifiScanList.get(i)).toString());

            if (wifiScanList.get(i).SSID.equals("Dialog_DA16200")
                    || wifiScanList.get(i).SSID.equals("Renesas_DA16200")
                    || wifiScanList.get(i).SSID.equals("Renesas_IoT_WiFi")
            ) {
                specificWifiScanList.add(wifiScanList.get(i));
                MyLog.i("wifiScanList.get(i).SSID = "+wifiScanList.get(i).SSID);
            }
        }

        specificWifis = new String[specificWifiScanList.size()];
        String[] wifiName = new String[specificWifiScanList.size()];
        Integer[] signalBar = new Integer[specificWifiScanList.size()];
        String[] secreteMode = new String[specificWifiScanList.size()];
        String[] mac = new String[specificWifiScanList.size()];
        Integer[] level = new Integer[specificWifiScanList.size()];

        for(int i = 0; i < specificWifiScanList.size(); i++) {
            specificWifis[i] = ((specificWifiScanList.get(i)).toString());

            wifiName[i] = specificWifiScanList.get(i).SSID;
            MyLog.i("wifiName[i] = "+wifiName[i].toString());

            if (wifiName[i].equals("Renesas_IoT_WiFi")) {
                isRenesasIoTWiFi = true;
            } else if (wifiName[i].equals("Dialog_DA16200")) {
                isDialogDA16200 = true;
            } else if (wifiName[i].equals("Renesas_DA16200")) {
                isRenesasDA16200 = true;
            }

            secreteMode[i] = "WPA2";
            level[i] = specificWifiScanList.get(i).level;
            signalBar[i] = deviceWifiSignalBar(secreteMode[i], level[i]);
            mac[i] = specificWifiScanList.get(i).BSSID.toUpperCase(Locale.ROOT);
        }

        deviceRowItems = new ArrayList<DeviceRowItem>();

        for (int i = 0; i < wifiName.length; i++) {
            MyLog.i("wifiName[i] = "+wifiName[i]);
            DeviceRowItem item = new DeviceRowItem(signalBar[i], wifiName[i], secreteMode[i], mac[i], level[i]);
            deviceRowItems.add(item);
        }

        if (deviceRowItems.size() > 0) {
            adapter = new DeviceListViewAdapter(getApplicationContext(), R.layout.wifi_list_item, deviceRowItems);
            listView.setAdapter(adapter);

            if (!isDialogDA16200 && !isRenesasDA16200 && isRenesasIoTWiFi) {
                StaticDataSave.deviceSSID = "Renesas_IoT_WiFi";
                listView.setVisibility(View.INVISIBLE);
                mHandler.sendEmptyMessageDelayed(HandleMsg.RENESAS_AP_CONNECT_TIMEOUT, 120000);
                if (WIFIUtil.getSSID(mContext).equals(StaticDataSave.deviceSSID)) {
                    mHandler.sendEmptyMessageDelayed(HandleMsg.RENESAS_AP_CONNECTED, 1000);
                } else {
                    ll_progressConnecting.setVisibility(View.VISIBLE);
                    WifiUtils.withContext(getApplicationContext())
                            .connectWith(StaticDataSave.deviceSSID, StaticDataSave.devicePassword)
                            .setTimeout(40000)
                            .onConnectionResult(new ConnectionSuccessListener() {
                                @Override
                                public void success() {
                                    currentSSID = WIFIUtil.getSSID(mContext);
                                    MyLog.d(">> currentSSID = " + currentSSID);
                                    tv_currentSSID.setText(currentSSID);
                                    mHandler.removeMessages(HandleMsg.RENESAS_AP_CONNECT_TIMEOUT);
                                    mHandler.sendEmptyMessageDelayed(HandleMsg.RENESAS_AP_CONNECTED, 1000);
                                }

                                @Override
                                public void failed(@NonNull ConnectionErrorCode errorCode) {
                                    MyLog.d("EPIC FAIL!" + errorCode.toString());
                                }
                            })
                            .start();
                }
            } else if (isDialogDA16200 && !isRenesasDA16200 && !isRenesasIoTWiFi) {
                StaticDataSave.deviceSSID = "Dialog_DA16200";
                listView.setVisibility(View.INVISIBLE);
                mHandler.sendEmptyMessageDelayed(HandleMsg.RENESAS_AP_CONNECT_TIMEOUT, 120000);
                if (WIFIUtil.getSSID(mContext).equals(StaticDataSave.deviceSSID)) {
                    mHandler.sendEmptyMessageDelayed(HandleMsg.RENESAS_AP_CONNECTED, 1000);
                } else {
                    ll_progressConnecting.setVisibility(View.VISIBLE);
                    WifiUtils.withContext(getApplicationContext())
                            .connectWith(StaticDataSave.deviceSSID, StaticDataSave.devicePassword)
                            .setTimeout(40000)
                            .onConnectionResult(new ConnectionSuccessListener() {
                                @Override
                                public void success() {
                                    currentSSID = WIFIUtil.getSSID(mContext);
                                    MyLog.d(">> currentSSID = " + currentSSID);
                                    tv_currentSSID.setText(currentSSID);
                                    mHandler.removeMessages(HandleMsg.RENESAS_AP_CONNECT_TIMEOUT);
                                    mHandler.sendEmptyMessageDelayed(HandleMsg.RENESAS_AP_CONNECTED, 1000);
                                }

                                @Override
                                public void failed(@NonNull ConnectionErrorCode errorCode) {
                                    MyLog.d("EPIC FAIL!" + errorCode.toString());
                                }
                            })
                            .start();
                }
            } else if (!isDialogDA16200 && isRenesasDA16200 & !isRenesasIoTWiFi) {
                StaticDataSave.deviceSSID = "Renesas_DA16200";
                listView.setVisibility(View.INVISIBLE);
                mHandler.sendEmptyMessageDelayed(HandleMsg.RENESAS_AP_CONNECT_TIMEOUT, 120000);
                if (WIFIUtil.getSSID(mContext).equals(StaticDataSave.deviceSSID)) {
                    mHandler.sendEmptyMessageDelayed(HandleMsg.RENESAS_AP_CONNECTED, 1000);
                } else {
                    ll_progressConnecting.setVisibility(View.VISIBLE);
                    WifiUtils.withContext(getApplicationContext())
                            .connectWith(StaticDataSave.deviceSSID, StaticDataSave.devicePassword)
                            .setTimeout(40000)
                            .onConnectionResult(new ConnectionSuccessListener() {
                                @Override
                                public void success() {
                                    currentSSID = WIFIUtil.getSSID(mContext);
                                    MyLog.d(">> currentSSID = " + currentSSID);
                                    tv_currentSSID.setText(currentSSID);
                                    mHandler.removeMessages(HandleMsg.RENESAS_AP_CONNECT_TIMEOUT);
                                    mHandler.sendEmptyMessageDelayed(HandleMsg.RENESAS_AP_CONNECTED, 1000);
                                }

                                @Override
                                public void failed(@NonNull ConnectionErrorCode errorCode) {
                                    MyLog.d("EPIC FAIL!" + errorCode.toString());
                                }
                            })
                            .start();
                }
            } else {
                listView.setVisibility(View.VISIBLE);
            }

            mHandler.removeMessages(HandleMsg.DEVICE_SCAN_TIMEOUT);
            ll_progressScanning.setVisibility(View.INVISIBLE);
            tv_noList.setVisibility(View.INVISIBLE);
            adapter.notifyDataSetChanged();
        }
    }

    public int deviceWifiSignalBar(String secMode, int level) {

        int signalBarID = R.drawable.baseline_signal_wifi_0_bar_black_48dp;

        if (Math.abs(level) >= 89) {
            signalBarID = R.drawable.baseline_signal_wifi_0_bar_black_48dp;
        } else if (78 <= Math.abs(level) && Math.abs(level) >= 88) {
            if (secMode.contains("WPA2") || secMode.contains("WPA") || secMode.contains("WEP")) {
                signalBarID = R.drawable.baseline_signal_wifi_1_bar_lock_black_48dp;
            } else {
                signalBarID = R.drawable.baseline_signal_wifi_1_bar_black_48dp;
            }
        } else if (67 <= Math.abs(level) && Math.abs(level) <= 77) {
            if (secMode.contains("WPA2") || secMode.contains("WPA") || secMode.contains("WEP")) {
                signalBarID = R.drawable.baseline_signal_wifi_2_bar_lock_black_48dp;
            } else {
                signalBarID = R.drawable.baseline_signal_wifi_2_bar_black_48dp;
            }
        } else if (56 <= Math.abs(level) && Math.abs(level) <= 66) {
            if (secMode.contains("WPA2") || secMode.contains("WPA") || secMode.contains("WEP")) {
                signalBarID = R.drawable.baseline_signal_wifi_3_bar_lock_black_48dp;
            } else {
                signalBarID = R.drawable.baseline_signal_wifi_3_bar_black_48dp;
            }
        } else if (Math.abs(level) <= 55) {
            if (secMode.contains("WPA2") || secMode.contains("WPA") || secMode.contains("WEP")) {
                signalBarID = R.drawable.outline_signal_wifi_4_bar_lock_black_24;
            } else {
                signalBarID = R.drawable.outline_signal_wifi_4_bar_black_24;
            }
        }
        return signalBarID;
    }

    public void wifi_Info_Print(Context mContext) {

        try {

            if(isWifiConnected(mContext) == true){

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){

                    ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkRequest networkRequest = new NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                            .build();

                    ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback(ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO) {
                        @Override
                        public void onAvailable(Network network) {
                            super.onAvailable(network);
                        }
                        @Override
                        public void onCapabilitiesChanged(Network network, NetworkCapabilities net) {

                            WifiInfo wifiInfo = (WifiInfo) net.getTransportInfo();

                            getSsid = String.valueOf(wifiInfo.getSSID().replaceAll("[\"]",""));

                            int wIp = wifiInfo.getIpAddress();
                            String getIpAddress = String.format("%d.%d.%d.%d", (wIp & 0xff), (wIp >> 8 & 0xff), (wIp >> 16 & 0xff), (wIp >> 24 & 0xff));

                            String getBSSID = String.valueOf(wifiInfo.getBSSID().trim());

                            MyLog.d("================================================");
                            MyLog.d("-----------------------------------------");
                            MyLog.d("[getSsid :: "+String.valueOf(getSsid)+"]");
                            MyLog.d("-----------------------------------------");
                            MyLog.d("[getIpAddress :: "+String.valueOf(getIpAddress)+"]");
                            MyLog.d("-----------------------------------------");
                            MyLog.d("[getBSSID :: "+String.valueOf(getBSSID)+"]");
                            MyLog.d("================================================");

                        }
                    };
                }
                else {

                    WifiManager wifimanager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifimanager.getConnectionInfo();

                    android.net.DhcpInfo dhcpInfo = wifimanager.getDhcpInfo();
                    int wIp = dhcpInfo.ipAddress;

                    String getSsid = wifiInfo.getSSID();
                    getSsid = getSsid.replaceAll("[\"]","");

                    String getIpAddress = String.format("%d.%d.%d.%d", (wIp & 0xff), (wIp >> 8 & 0xff), (wIp >> 16 & 0xff), (wIp >> 24 & 0xff));

                    String getBSSID = wifiInfo.getBSSID();
                    getBSSID = getBSSID.trim();


                    MyLog.d("================================================");
                    MyLog.d("-----------------------------------------");
                    MyLog.d("[getSsid :: "+String.valueOf(getSsid)+"]");
                    MyLog.d("-----------------------------------------");
                    MyLog.d("[getIpAddress :: "+String.valueOf(getIpAddress)+"]");
                    MyLog.d("-----------------------------------------");
                    MyLog.d("[getBSSID :: "+String.valueOf(getBSSID)+"]");
                    MyLog.d("================================================");

                }
            }
            else {
                MyLog.d("================================================");
                MyLog.d("-----------------------------------------");
                MyLog.d("[error :: wifi is not connected]");
                MyLog.d("================================================");
            }

        } catch (Exception e) {
            e.printStackTrace();
            MyLog.d("================================================");
            MyLog.d("-----------------------------------------");
            MyLog.d("[exception :: "+String.valueOf(e.getMessage())+"]");
            MyLog.d("================================================");
        }
    }
}