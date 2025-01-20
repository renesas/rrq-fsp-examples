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

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.renesas.wifi.R;
import com.renesas.wifi.util.FButton;
import com.renesas.wifi.util.MyLog;
import com.renesas.wifi.util.StaticDataSave;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;


public class DeviceScanActivity extends Activity {

    private final static String TAG = DeviceScanActivity.class.getSimpleName();

    private Context mContext;

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;

    //handler
    private Handler mHandler;

    //constant
    private static final int PERMISSION_REQUEST_FINE_LOCATION = 100;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;

    //UI resources
    private ImageView iv_back;
    private FButton btn_scan_start;
    private ProgressBar progressScanning;
    private LinearLayout ll_progressScanning;
    private TextView tv_noList;

    //data
    public ListView listView;

    //flag
    private boolean mScanning;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.da16600_activity_device_scan);
        mHandler = new Handler();
        mContext = this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                    },
                    1);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.BLUETOOTH

                    },
                    1);
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton("Ok", null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @TargetApi(Build.VERSION_CODES.M)
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_FINE_LOCATION);
                    }
                });
                builder.show();
            }
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        iv_back = (ImageView) findViewById(R.id.iv_back);
        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        btn_scan_start = (FButton) findViewById(R.id.btn_scan_start);
        btn_scan_start.setVisibility(View.VISIBLE);
        if (btn_scan_start.getVisibility() == View.VISIBLE) {
            btn_scan_start.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listView.setBackground(ContextCompat.getDrawable(mContext, R.drawable.border));
                    mLeDeviceListAdapter.clear();
                    scanLeDevice(true);
                }
            });
        }

        ll_progressScanning = (LinearLayout) findViewById(R.id.ll_progressScanning);
        progressScanning = (ProgressBar) findViewById(R.id.progressScanning);

        tv_noList = (TextView) findViewById(R.id.tv_noList);
        tv_noList.setVisibility(View.INVISIBLE);

        listView = (ListView) findViewById(R.id.list);
        listView.setBackground(ContextCompat.getDrawable(mContext, R.drawable.no_border));
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                if (device == null) return;
                final Intent intent = new Intent(view.getContext(), DeviceControlActivity.class);
                if (Build.VERSION.SDK_INT >= 31) {
                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                }
                StaticDataSave.mDeviceName = device.getName();
                StaticDataSave.mDeviceAddress = device.getAddress();
                if (mScanning) {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }
                startActivity(intent);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int reqeustCode, String permission[], int[] grantResults) {
        switch (reqeustCode) {
            case PERMISSION_REQUEST_FINE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    MyLog.i("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, " +
                            "this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton("Ok", null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {

                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (Build.VERSION.SDK_INT >= 31) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                }

                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }

        listView.setVisibility(View.VISIBLE);
        ll_progressScanning.setVisibility(View.INVISIBLE);
        btn_scan_start.setText("Scanning ...");
        tv_noList.setVisibility(View.INVISIBLE);

        mLeDeviceListAdapter = new LeDeviceListAdapter();
        listView.setAdapter(mLeDeviceListAdapter);
        listView.setBackground(ContextCompat.getDrawable(mContext, R.drawable.border));

        mLeDeviceListAdapter.clear();
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    @Override
    public void onBackPressed() {
        initValue();
        Intent intent = new Intent(mContext, DA16600MainActivity.class);
        startActivity(intent);
        finishAffinity();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;

                    ll_progressScanning.setVisibility(View.INVISIBLE);
                    btn_scan_start.setText("Start BLE scan");
                    if (Build.VERSION.SDK_INT >= 31) {
                        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                    }
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            ll_progressScanning.setVisibility(View.VISIBLE);
            btn_scan_start.setText("Scanning ...");
            tv_noList.setVisibility(View.INVISIBLE);
            if (Build.VERSION.SDK_INT >= 31) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
            }
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            MyLog.i("mLeDeviceListAdapter.getCount() = " + mLeDeviceListAdapter.getCount());
            if (mLeDeviceListAdapter.getCount() == 0) {
                tv_noList.setVisibility(View.VISIBLE);
                listView.setVisibility(View.INVISIBLE);
            } else {
                tv_noList.setVisibility(View.INVISIBLE);
                listView.setAdapter(mLeDeviceListAdapter);
                listView.setVisibility(View.VISIBLE);
            }
            ll_progressScanning.setVisibility(View.INVISIBLE);
            btn_scan_start.setText("Start BLE scan");
            if (Build.VERSION.SDK_INT >= 31) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
            }
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }


    private class LeDeviceListAdapter extends BaseAdapter {

        private ArrayList<BluetoothDevice> device_list;
        private LayoutInflater mInflator;
        private HashMap hm = new HashMap();

        public LeDeviceListAdapter() {
            super();
            this.device_list = new ArrayList<BluetoothDevice>();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public class BluetoothDeviceComparator implements Comparator<BluetoothDevice> {
            public int compare(BluetoothDevice left, BluetoothDevice right) {
                return (int) (hm.get(right)) - (int) (hm.get(left));
            }
        }

        public void addDevice(BluetoothDevice device, int rssi) {
            if (!device_list.contains(device)) {
                device_list.add(device);
            }
            hm.put(device, rssi);

            Collections.sort(device_list, new LeDeviceListAdapter.BluetoothDeviceComparator());
        }

        public BluetoothDevice getDevice(int position) {
            return device_list.get(position);
        }

        public void clear() {
            device_list.clear();
        }

        @Override
        public Object getItem(int position) {
            return device_list.get(position);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public int getCount() {
            return device_list.size();
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.da16600_device_list_row, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceRssi = (TextView) view.findViewById(R.id.ble_device_info);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.ble_device_name);
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.ble_device_address);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = device_list.get(position);
            if (Build.VERSION.SDK_INT >= 31) {
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    //return TODO;
                }
            }

            final String deviceName = device.getName();

            if (deviceName != null && deviceName.length() > 0) {
                viewHolder.deviceName.setText(deviceName);
                view.setBackgroundColor(Color.TRANSPARENT);
            }
            else {
                viewHolder.deviceName.setText("Unknown Device");
                view.setBackgroundColor(Color.TRANSPARENT);
            }

            final String deviceAddress = device.getAddress();
            viewHolder.deviceAddress.setText(deviceAddress);

            viewHolder.deviceName.setTextColor(getResources().getColor(R.color.black));
            viewHolder.deviceAddress.setTextColor(getResources().getColor(R.color.black));

            int rssi = (int) hm.get(device);
            float sigStrength = (rssi + 100) * 2;
            if(sigStrength < 20)
            {
                viewHolder.deviceRssi.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_signal_cellular_0_bar_black_15dp, 0);
            }
            else if(sigStrength < 40)
            {
                viewHolder.deviceRssi.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_signal_cellular_1_bar_black_15dp, 0);
            }
            else if(sigStrength < 60)
            {
                viewHolder.deviceRssi.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_signal_cellular_2_bar_black_15dp, 0);
            }
            else if(sigStrength < 80)
            {
                viewHolder.deviceRssi.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_signal_cellular_3_bar_black_15dp, 0);
            }
            else
            {
                viewHolder.deviceRssi.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_signal_cellular_4_bar_black_15dp, 0);
            }
            viewHolder.deviceRssi.setText(hm.get(device).toString() + " dBm");

            return view;
        }

        class ViewHolder {
            TextView deviceName;
            TextView deviceAddress;
            TextView deviceRssi;
        }
    }

    @SuppressLint("MissingPermission")
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device, rssi);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    private void initValue() {
        StaticDataSave.thingName = null;
    }

}