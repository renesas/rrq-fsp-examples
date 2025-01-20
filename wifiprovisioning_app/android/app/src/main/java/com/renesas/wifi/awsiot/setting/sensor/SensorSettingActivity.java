package com.renesas.wifi.awsiot.setting.sensor;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.renesas.wifi.R;
import com.renesas.wifi.awsiot.AWSEVENT;
import com.renesas.wifi.awsiot.AWSIoTSensorActivity;
import com.renesas.wifi.util.MyLog;


public class SensorSettingActivity extends FragmentActivity {

    private static String TAG = "SettingActivity";

    public static Context settingContext;

    private Fragment contentFragment;

    public static SensorKeyManageFragment fragmentKeyManage;
    public static SensorFunctionSetFragment fragmentFunctionSet;

    Button buttonKeyManage;
    Button buttonFunctionSet;
    Button buttonIndKeyMng;
    Button buttonIndFcnSet;

    LinearLayout linearMargin;

    public ProgressDialog updatingDialog = null;
    public AlertDialog completeDialog;
    public AlertDialog updateFailDialog;
    public static SensorSettingActivity settingInstance;
    public static SensorSettingActivity getInstanceSetting() {return settingInstance;}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.awsiot_activity_setting);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        settingInstance = this;
        settingContext = this;

        MyLog.i("=== Create  ===");
        AWSIoTSensorActivity.fromSetting = true;

        final FrameLayout mContent = (FrameLayout) findViewById(R.id.content_frame);

        LinearLayout title_setting = (LinearLayout) findViewById(R.id.title_setting);
        title_setting.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ImageView btn_back_setting = (ImageView) findViewById(R.id.btn_back_setting);
        btn_back_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        initSettingResource();

        buttonKeyManage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MyLog.i("=== Not Support Key Mange Function Now ===");
            }
        });

        buttonFunctionSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(fragmentFunctionSet == null){
                    fragmentFunctionSet = new SensorFunctionSetFragment();
                }
                swichContent(fragmentFunctionSet, SensorFunctionSetFragment.ARG_ITEM_ID);
                buttonFunctionSet.setTextColor(getResources().getColor(R.color.white));
                buttonFunctionSet.setBackgroundResource(R.drawable.tab_bg_selected);
                buttonKeyManage.setTextColor(getResources().getColor(R.color.dark_gray));
                buttonKeyManage.setBackgroundResource(R.drawable.tab_bg_unselected);
                buttonIndFcnSet.setBackgroundColor(Color.RED);
                buttonIndKeyMng.setBackgroundColor(getResources().getColor(R.color.blue1));
            }
        });

        FragmentManager fragmentManager = getSupportFragmentManager();

        if(savedInstanceState != null){
            if (savedInstanceState.containsKey("content")) {
                String content = savedInstanceState.getString("content");
                if (content.equals(SensorKeyManageFragment.ARG_ITEM_ID)) {
                    if (fragmentManager.findFragmentByTag(SensorKeyManageFragment.ARG_ITEM_ID) != null) {
                        //setFragmentTitle(R.string.favorites);
                        contentFragment = fragmentManager.findFragmentByTag(SensorKeyManageFragment.ARG_ITEM_ID);
                        fragmentKeyManage = (SensorKeyManageFragment) contentFragment;
                    }
                }
            }
            if (fragmentManager.findFragmentByTag(SensorFunctionSetFragment.ARG_ITEM_ID) != null) {
                fragmentFunctionSet = (SensorFunctionSetFragment) fragmentManager.findFragmentByTag(SensorFunctionSetFragment.ARG_ITEM_ID);
                contentFragment = fragmentFunctionSet;
            }
        }else {
            fragmentFunctionSet = new SensorFunctionSetFragment();
            swichContent(fragmentFunctionSet, SensorFunctionSetFragment.ARG_ITEM_ID);
        }

        MyLog.i("=== Create End ===");
    }

    private void swichContent(Fragment fragment, String argItemId) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        while (fragmentManager.popBackStackImmediate());

        if (fragment != null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.content_frame, fragment, argItemId);
            //Only KeyManageFragment is added to the back stack.
            if (!(fragment instanceof SensorFunctionSetFragment)) {
                transaction.addToBackStack(argItemId);
            }
            transaction.commit();
            contentFragment = fragment;
        }
    }

    private void initSettingResource() {
        linearMargin = (LinearLayout)findViewById(R.id.setting_margin);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams)linearMargin.getLayoutParams();
        layoutParams.topMargin = AWSIoTSensorActivity.statusBarHeight;
        linearMargin.setLayoutParams(layoutParams);

        buttonKeyManage = (Button) findViewById(R.id.button_keyManage);
        buttonIndKeyMng = (Button) findViewById(R.id.ind_keymanage);
        buttonFunctionSet = (Button) findViewById(R.id.button_functionSet);
        buttonIndFcnSet = (Button) findViewById(R.id.ind_fuctionset);

        buttonKeyManage.setBackgroundResource(R.drawable.tab_bg_unselected);
        buttonKeyManage.setTextColor(getResources().getColor(R.color.dark_gray));
        buttonIndKeyMng.setBackgroundColor(getResources().getColor(R.color.blue1));
        buttonFunctionSet.setBackgroundResource(R.drawable.tab_bg_selected);
        buttonFunctionSet.setTextColor(getResources().getColor(R.color.white));
        buttonIndFcnSet.setBackgroundColor(Color.RED);

    }

    public void showUpdatingDialog() {
        if (updatingDialog == null) {
            updatingDialog = new ProgressDialog(settingContext, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
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

    public void dismissUpdatingDialog() {
        if (updatingDialog != null) {
            updatingDialog.dismiss();
        }
    }

    public void showCompleteDialog() {

        dismissUpdatingDialog();

        if (completeDialog == null) {
            String message = "Firmware update of device was completed.";
            completeDialog = new AlertDialog.Builder(settingContext, R.style.AlertDialogCustom).create();
            completeDialog.setIcon(R.mipmap.renesas_ic_launcher);
            completeDialog.setMessage(message);
            completeDialog.setCancelable(false);
            completeDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismissCompleteDialog();
                    AWSIoTSensorActivity.getInstanceMain().sendEvent(AWSEVENT.E_GET_SHADOW);
                }
            });
            completeDialog.setOnCancelListener(null);
            completeDialog.show();
        }
    }

    public void dismissCompleteDialog() {
        if (completeDialog != null) {
            completeDialog.dismiss();
        }
    }

    public void showUpdateFailDialog() {

        dismissUpdatingDialog();

        if (updateFailDialog == null) {
            String message = "The device's firmware update of device was failed.";
            updateFailDialog = new AlertDialog.Builder(settingContext, R.style.AlertDialogCustom).create();
            updateFailDialog.setIcon(R.mipmap.renesas_ic_launcher);
            updateFailDialog.setMessage(message);
            updateFailDialog.setCancelable(false);
            updateFailDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dismissUpdateFailDialog();
                    AWSIoTSensorActivity.getInstanceMain().sendEvent(AWSEVENT.E_GET_SHADOW);
                }
            });
            updateFailDialog.setOnCancelListener(null);
            updateFailDialog.show();
        }
    }

    public void dismissUpdateFailDialog() {
        if (updateFailDialog != null) {
            updateFailDialog.dismiss();
            updateFailDialog = null;
        }
    }

}