package com.renesas.wifi.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.renesas.wifi.DA16200.activity.DA16200MainActivity;
import com.renesas.wifi.DA16600.activity.DA16600MainActivity;
import com.renesas.wifi.R;
import com.renesas.wifi.awsiot.AWSIoTDeviceActivity;
import com.renesas.wifi.awsiot.AWSIoTDoorActivity;
import com.renesas.wifi.awsiot.AWSIoTSensorActivity;
import com.renesas.wifi.azureiot.AzureIoTDoorActivity;
import com.renesas.wifi.buildOption;
import com.renesas.wifi.util.FButton;
import com.renesas.wifi.util.CustomToast;
import com.renesas.wifi.util.MyLog;
import com.renesas.wifi.util.StaticDataSave;
import org.apache.commons.lang3.StringUtils;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Date;

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private Date buildDate = new Date(Long.parseLong(com.renesas.wifi.BuildConfig.BUILD_TIME));

    private FButton btn_ap_provisioning;
    private FButton btn_ble_provisioning;

    private FButton btn_rrq61400_provisioning;

    public static MainHandler mHandler;
    private DrawerLayout mDrawer;
    private LinearLayout mOpensourceLayout;
    private ImageView mActionBarLeftBtn;
    private TextView mVersionTv;
    private LinearLayout mAWSLayout;
    private LinearLayout mAzureLayout;
    private MaterialDialog awsNotSupportDialog = null;
    private MaterialDialog azureNotSupportDialog = null;
    private MaterialDialog azureDeviceFailDialog = null;

    CustomToast customToast;
    MaterialDialog terminateDialog = null;
    public static final int PERMISSION_REQ = 0;

    public static Context mContext;
    public static MainActivity instance;
    public static MainActivity getInstance() {
        return instance;
    }

    private boolean isEasyConnectSupported = false;

    //handler event
    public static class HandleMsg
    {
        public static final int E_MAIN_PROVISIONING_CALL = 0;
        public static final int E_MAIN_BLE_PROVISIONING_CALL = 1;
        public static final int E_MAIN_GENERAL_AWSIOT_CALL = 2;
        public static final int E_MAIN_ATCMD_AWSIOT_CALL = 3;
        public static final int E_MAIN_AZUREIOT_CALL = 4;
        public static final int E_MAIN_SUPPORT_CALL = 5;
    }


    /**
     ****************************************************************************************
     * @brief Handler class for splash activity
     * @param
     * @return none
     ****************************************************************************************
     */
    private static final class MainHandler extends Handler
    {

        private final WeakReference<MainActivity> ref;

        public MainHandler(MainActivity act)
        {
            ref = new WeakReference<>(act);
        }

        @Override
        public void handleMessage(Message msg) {
            final MainActivity act = ref.get();

            if (act != null)
            {
                switch (msg.what)
                {
                    case HandleMsg.E_MAIN_PROVISIONING_CALL:
                    {
                        MyLog.i(" Provisioning start Call  ~ ");
                        Intent main = new Intent(mContext, DA16200MainActivity.class);
                        main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        act.startActivity(main);
                        act.finishAffinity();
                    }
                    break;

                    case HandleMsg.E_MAIN_BLE_PROVISIONING_CALL:
                    {
                        MyLog.i(" Provisioning start Call  ~ ");
                        Intent main = new Intent(mContext, DA16600MainActivity.class);
                        main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        act.startActivity(main);
                        act.finishAffinity();
                    }
                    break;

                    case HandleMsg.E_MAIN_GENERAL_AWSIOT_CALL:
                    {
                        MyLog.i(" General AWSIoT start Call  ~ ");
                        if (isDoorlock()) {
                            Intent main = new Intent(mContext, AWSIoTDoorActivity.class);
                            main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            act.startActivity(main);
                            act.finishAffinity();
                        } else if (isSensor()) {
                            Intent main = new Intent(mContext, AWSIoTSensorActivity.class);
                            main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            act.startActivity(main);
                            act.finishAffinity();
                        }
                    }
                    break;

                    case HandleMsg.E_MAIN_ATCMD_AWSIOT_CALL:
                    {
                        MyLog.i(" AWSIoT start Call  ~ ");
                        Intent main = new Intent(mContext, AWSIoTDeviceActivity.class);
                        main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        act.startActivity(main);
                        act.finishAffinity();
                    }
                    break;

                    case HandleMsg.E_MAIN_AZUREIOT_CALL:
                    {
                        MyLog.i(" E_MAIN_AZUREIOT_CALL  ~ ");
                        Intent main = new Intent(mContext, AzureIoTDoorActivity.class);
                        main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        act.startActivity(main);
                        act.finishAffinity();
                    }
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
        MyLog.i("== onCreate ==");

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        mContext = MainActivity.this;
        instance = this;
        mHandler = new MainHandler(this);
        customToast = new CustomToast(mContext);

        mDrawer = findViewById(R.id.DrawerLayout);
        mOpensourceLayout = findViewById(R.id.OpensourceLayout);
        mActionBarLeftBtn = findViewById(R.id.actionBarLeftBtnIv);
        if (mOpensourceLayout != null) {
            mOpensourceLayout.setOnClickListener(this);
        }
        if (mActionBarLeftBtn != null) {
            mActionBarLeftBtn.setOnClickListener(this);
        }

        mVersionTv = findViewById(R.id.VersionTv);
        String versionName = getAppVersion(mContext);
        MyLog.i("------------------------------------------------------------");
        MyLog.i(">> version : "+versionName);
        MyLog.i(">> This .apk was built on " + buildDate.toString());
        MyLog.i("------------------------------------------------------------");
        if (mVersionTv != null) {
            if(StringUtils.isNotBlank(versionName)) {
                mVersionTv.setText(versionName);
            } else {
                mVersionTv.setText("0.0.0");
            }
        }

        StaticDataSave.saveData = getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);
        StaticDataSave.mode = StaticDataSave.saveData.getInt(StaticDataSave.modeKey, -1);
        StaticDataSave.azureConString = StaticDataSave.saveData.getString(StaticDataSave.azureConStringKey, null);
        if (StaticDataSave.thingName != null) {
            MyLog.i("StaticDataSave.thingName = " + StaticDataSave.thingName);
        }
        MyLog.i("StaticDataSave.mode = "+String.valueOf(StaticDataSave.mode));
        if (StaticDataSave.azureConString != null) {
            MyLog.i("StaticDataSave.azureConString = " + StaticDataSave.azureConString);
        }

        mAWSLayout = findViewById(R.id.AWSLayout);

        if (mAWSLayout != null) {
            mAWSLayout.setOnClickListener(this);
        }

        mAzureLayout = findViewById(R.id.AzureLayout);

        if (mAzureLayout != null) {
            mAzureLayout.setOnClickListener(this);
        }

        WifiManager wifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);

        btn_ap_provisioning = (FButton) findViewById(R.id.btn_ap_provisioning);
        if (btn_ap_provisioning != null) {
            btn_ap_provisioning.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    StaticDataSave.device = "DA16200";
                    mHandler.sendEmptyMessage(HandleMsg.E_MAIN_PROVISIONING_CALL);
                }
            });
        }

        btn_ble_provisioning = (FButton) findViewById(R.id.btn_ble_provisioning);
        if (btn_ble_provisioning != null) {
            btn_ble_provisioning.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    StaticDataSave.device = "DA16600";
                    mHandler.sendEmptyMessage(HandleMsg.E_MAIN_BLE_PROVISIONING_CALL);
                }
            });
        }

        btn_rrq61400_provisioning = (FButton) findViewById(R.id.btn_rrq61400_provisioning);
        if (btn_rrq61400_provisioning != null) {
            btn_rrq61400_provisioning.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    StaticDataSave.device = "RRQ61400";
                    mHandler.sendEmptyMessage(HandleMsg.E_MAIN_BLE_PROVISIONING_CALL);
                }
            });
        }
        onCheckPermission();
    }

    /**
     ****************************************************************************************
     * @brief Permission check
     * @param
     * @return none
     ****************************************************************************************
     */
    private void onCheckPermission()
    {
        MyLog.i("onCheckPermission()");

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED)
        {
            return;
        }
        else
        {
            ActivityCompat.requestPermissions(
                    this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.CALL_PHONE}, PERMISSION_REQ);
        }
    }

    /**
     ****************************************************************************************
     * @brief Permission Check result callback
     * @param requestCode Permission request code
     * @param permissions Permissions
     * @param grantResults Permission grant results
     * @return none
     ****************************************************************************************
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MyLog.i("onRequestPermissionsResult()");

        for (int i = 0; i < grantResults.length; i++) {
            if (requestCode == 100 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                MyLog.i("Permission Granted:" + permissions[i]);
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        MyLog.i("== onResume() ==");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyLog.i("== onDestroy() ==");
        dismissAwsNotSupportDialog();
        dismissAzureNotSupportDialog();
    }

    @Override
    public void onBackPressed() {
        if (mDrawer.isDrawerOpen(GravityCompat.START)) {
            mDrawer.closeDrawer(GravityCompat.START);
        } else {
            showTerminateDialog();
        }
    }

    public void showTerminateDialog() {
        if (mContext != null) {
            terminateDialog = new MaterialDialog.Builder(mContext)
                    .theme(Theme.LIGHT)
                    .title("Confirm")
                    .titleGravity(GravityEnum.CENTER)
                    .titleColor(mContext.getResources().getColor(R.color.black))
                    .content("Would you like to exit?")
                    .contentColor(mContext.getResources().getColor(R.color.black))
                    .negativeText("CANCEL")
                    .negativeColor(mContext.getResources().getColor(R.color.blue3))
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            terminateDialog.dismiss();
                        }
                    })
                    .positiveText("OK")
                    .positiveColor(mContext.getResources().getColor(R.color.blue3))
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            if (terminateDialog != null && terminateDialog.isShowing()) {
                                terminateDialog.dismiss();
                            }
                            finishAffinity();
                        }
                    })
                    .build();
            terminateDialog.getWindow().setGravity(Gravity.CENTER);
            terminateDialog.show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
            case R.id.actionBarLeftBtnIv:
                MyLog.i("Nav menu");

                if (mDrawer != null) {
                    if(mDrawer.isDrawerOpen(GravityCompat.START)==false) {
                        mDrawer.openDrawer(GravityCompat.START);

                    } else {
                        mDrawer.closeDrawer(GravityCompat.START);
                    }
                }
                break;
            case R.id.OpensourceLayout:
                startActivity(new Intent(mContext, OpensourceActivity.class));
                if (mDrawer != null) {
                    mDrawer.closeDrawer(GravityCompat.START);
                }
                break;

            case R.id.AWSLayout:

                MyLog.i("StaticDataSave.mode = "+StaticDataSave.mode);

                if (mDrawer != null) {
                    mDrawer.closeDrawer(GravityCompat.START);
                }

                if (StaticDataSave.mode == 10 || StaticDataSave.mode == 12) {  //General AWS IoT
                    if (StaticDataSave.thingName.contains("SENSOR")) {
                        Intent main = new Intent(mContext, AWSIoTSensorActivity.class);
                        main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(main);
                        finishAffinity();
                    } else {
                        Intent main = new Intent(mContext, AWSIoTDoorActivity.class);
                        main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(main);
                        finishAffinity();
                    }
                } else if (StaticDataSave.mode == 11 || StaticDataSave.mode == 13) {  //AT-CMD AWS IoT
                    Intent main = new Intent(mContext, AWSIoTDeviceActivity.class);
                    main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(main);
                    finishAffinity();
                } else {
                    showAwsNotSupportDialog();
                }
                break;

            case R.id.AzureLayout:

                if (mDrawer != null) {
                    mDrawer.closeDrawer(GravityCompat.START);
                }
                if (StaticDataSave.mode == 20 || StaticDataSave.mode == 21) {
                    if (StaticDataSave.thingName != null && StaticDataSave.azureConString != null) {
                        if (!StaticDataSave.thingName.isEmpty() && !StaticDataSave.azureConString.isEmpty()) {
                            Intent main = new Intent(mContext, AzureIoTDoorActivity.class);
                            main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(main);
                            finishAffinity();
                        } else {
                            showAzureDeviceFailDialog();
                        }
                    } else {
                        showAzureDeviceFailDialog();
                    }
                }  else {
                    showAzureNotSupportDialog();
                }
                break;

            default:
                break;
        }
    }

    /**
     ****************************************************************************************
     * @brief Get app version
     * @param context Context
     * @return String Version name
     ****************************************************************************************
     */
    private static String getAppVersion(Context context) {

        MyLog.i("getAppVersion()");

        PackageInfo pi = null;

        try {
            pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            MyLog.e("NameNotFound Exception");
        }
        return pi.versionName;
    }

    public static boolean isDoorlock() {
        StaticDataSave.saveData = mContext.getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);
        if (StaticDataSave.thingName != null) {
            if (StaticDataSave.thingName.contains(buildOption.WIFI_SCAN_FILTER2)
                    || StaticDataSave.thingName.contains(buildOption.WIFI_SCAN_FILTER3)
                    || StaticDataSave.thingName.contains(buildOption.WIFI_SCAN_FILTER4)) {
                return true;
            } else {
                return false;
            }
        } else {
            MyLog.i("StaticDataSave.thingName is null!");
            return false;
        }
    }

    public static boolean isSensor() {
        StaticDataSave.saveData = mContext.getSharedPreferences(StaticDataSave.mSharedPreferencesName, Context.MODE_PRIVATE);
        StaticDataSave.thingName = StaticDataSave.saveData.getString(StaticDataSave.thingNameKey, null);
        if (StaticDataSave.thingName != null) {
            if (StaticDataSave.thingName.contains(buildOption.WIFI_SCAN_FILTER5)) {
                return true;
            } else {
                return false;
            }
        } else {
            MyLog.i("StaticDataSave.thingName is null!");
            return false;
        }
    }

    private void showAwsNotSupportDialog() {
        if (awsNotSupportDialog != null) {
            awsNotSupportDialog.dismiss();
        }
        if (mContext != null && awsNotSupportDialog == null) {
            awsNotSupportDialog = new MaterialDialog.Builder(mContext)
                    .theme(Theme.LIGHT)
                    .title("Not supported")
                    .titleGravity(GravityEnum.CENTER)
                    .titleColor(mContext.getResources().getColor(R.color.black))
                    .content("Perform provisioning using an SDK that supports AWS IoT.")
                    .contentColor(mContext.getResources().getColor(R.color.black))
                    .positiveText("OK")
                    .positiveColor(mContext.getResources().getColor(R.color.blue3))
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dismissAwsNotSupportDialog();
                        }
                    })
                    .build();
            awsNotSupportDialog.getWindow().setGravity(Gravity.CENTER);
            awsNotSupportDialog.show();
        }
    }

    private void dismissAwsNotSupportDialog() {
        if (awsNotSupportDialog != null) {
            awsNotSupportDialog.dismiss();
            awsNotSupportDialog = null;
        }
    }

    private void showAzureNotSupportDialog() {
        if (azureNotSupportDialog != null) {
            azureNotSupportDialog.dismiss();
        }
        if (mContext != null && azureNotSupportDialog == null) {
            azureNotSupportDialog = new MaterialDialog.Builder(mContext)
                    .theme(Theme.LIGHT)
                    .title("Not supported")
                    .titleGravity(GravityEnum.CENTER)
                    .titleColor(mContext.getResources().getColor(R.color.black))
                    .content("Perform provisioning using an SDK that supports Azure IoT.")
                    .contentColor(mContext.getResources().getColor(R.color.black))
                    .positiveText("OK")
                    .positiveColor(mContext.getResources().getColor(R.color.blue3))
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dismissAzureNotSupportDialog();
                        }
                    })
                    .build();
            azureNotSupportDialog.getWindow().setGravity(Gravity.CENTER);
            azureNotSupportDialog.show();
        }
    }

    private void dismissAzureNotSupportDialog() {
        if (azureNotSupportDialog != null) {
            azureNotSupportDialog.dismiss();
            azureNotSupportDialog = null;
        }
    }

    private void showAzureDeviceFailDialog() {
        if (azureDeviceFailDialog != null) {
            azureDeviceFailDialog.dismiss();
        }
        if (mContext != null && azureDeviceFailDialog == null) {
            azureDeviceFailDialog = new MaterialDialog.Builder(mContext)
                    .theme(Theme.LIGHT)
                    .title("Error")
                    .titleGravity(GravityEnum.CENTER)
                    .titleColor(mContext.getResources().getColor(R.color.black))
                    .content("Device information is corrupted.\n" +
                            "Please check the Azure-related information stored on the device and run provisioning again.")
                    .contentColor(mContext.getResources().getColor(R.color.black))
                    .positiveText("OK")
                    .positiveColor(mContext.getResources().getColor(R.color.blue3))
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dismissAzureDeviceFailDialog();
                        }
                    })
                    .build();
            azureDeviceFailDialog.getWindow().setGravity(Gravity.CENTER);
            azureDeviceFailDialog.show();
        }
    }

    private void dismissAzureDeviceFailDialog() {
        if (azureDeviceFailDialog != null) {
            azureDeviceFailDialog.dismiss();
            azureDeviceFailDialog = null;
        }
    }

    public static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) { e.printStackTrace();}
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }
}