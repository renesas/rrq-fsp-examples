package com.renesas.wifi.awsiot.notify;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.renesas.wifi.R;
import com.renesas.wifi.awsiot.AWSIoTDoorActivity;
import com.renesas.wifi.util.MyLog;


public class NotifyActivity extends FragmentActivity {

    private static String TAG = "NotifyActivity";

    private Fragment contentFragment;

    public static DoorAccessFragment fragmentDoorAccess;
    public static DoorNotifyFragment fragmentDoorNotify;

    Button buttonAccess;
    Button buttonNotify;
    Button buttonIndAccess;
    Button buttonIndNotify;

    LinearLayout linearMargin;
    public static boolean isAccessSelect = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.awsiot_activity_notify);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        MyLog.i("=== Create  ===");

        final FrameLayout mContent = (FrameLayout) findViewById(R.id.content_frame);

        LinearLayout title_setting = (LinearLayout) findViewById(R.id.title_notify);
        title_setting.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ImageView btn_back_setting = (ImageView) findViewById(R.id.btn_back_notify);
        btn_back_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        initNotifyResource();

        buttonAccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(fragmentDoorAccess == null){
                    fragmentDoorAccess = new DoorAccessFragment();
                }
                swichContent(fragmentDoorAccess, DoorAccessFragment.ARG_ITEM_ID);
                buttonNotify.setTextColor(getResources().getColor(R.color.dark_gray));
                buttonNotify.setBackgroundResource(R.drawable.tab_bg_unselected);
                buttonAccess.setTextColor(getResources().getColor(R.color.white));
                buttonAccess.setBackgroundResource(R.drawable.tab_bg_selected);
                buttonIndNotify.setBackgroundColor(getResources().getColor(R.color.blue1));
                buttonIndAccess.setBackgroundColor(Color.RED);
                isAccessSelect = true;
            }
        });

        buttonNotify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(fragmentDoorNotify == null){
                    fragmentDoorNotify = new DoorNotifyFragment();
                }
                swichContent(fragmentDoorNotify, DoorNotifyFragment.ARG_ITEM_ID);
                buttonNotify.setTextColor(getResources().getColor(R.color.white));
                buttonNotify.setBackgroundResource(R.drawable.tab_bg_selected);
                buttonAccess.setTextColor(getResources().getColor(R.color.dark_gray));
                buttonAccess.setBackgroundResource(R.drawable.tab_bg_unselected);
                buttonIndNotify.setBackgroundColor(Color.RED);
                buttonIndAccess.setBackgroundColor(getResources().getColor(R.color.blue1));
                isAccessSelect = false;
            }
        });

        FragmentManager fragmentManager = getSupportFragmentManager();

        if(savedInstanceState != null){
            if (savedInstanceState.containsKey("content")) {
                String content = savedInstanceState.getString("content");
                if (content.equals(DoorNotifyFragment.ARG_ITEM_ID)) {
                    if (fragmentManager.findFragmentByTag(DoorNotifyFragment.ARG_ITEM_ID) != null) {
                        contentFragment = fragmentManager.findFragmentByTag(DoorNotifyFragment.ARG_ITEM_ID);
                        fragmentDoorNotify = (DoorNotifyFragment) contentFragment;
                    }
                }
            }
            if (fragmentManager.findFragmentByTag(DoorAccessFragment.ARG_ITEM_ID) != null) {
                fragmentDoorAccess = (DoorAccessFragment) fragmentManager.findFragmentByTag(DoorAccessFragment.ARG_ITEM_ID);
                contentFragment = fragmentDoorAccess;
            }
        }else {
            if(isAccessSelect){
                fragmentDoorAccess = new DoorAccessFragment();
                swichContent(fragmentDoorAccess, DoorAccessFragment.ARG_ITEM_ID);
            }else {
                fragmentDoorNotify = new DoorNotifyFragment();
                swichContent(fragmentDoorNotify, DoorNotifyFragment.ARG_ITEM_ID);
            }
        }

        MyLog.i("=== Create End ===");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        fragmentDoorAccess = null;
        fragmentDoorNotify = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if(contentFragment instanceof DoorAccessFragment){
            outState.putString("content", DoorAccessFragment.ARG_ITEM_ID);
        }else{
            outState.putString("content", DoorNotifyFragment.ARG_ITEM_ID);
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if(fm.getBackStackEntryCount() > 0){
            super.onBackPressed();
            buttonIndAccess.setBackgroundColor(getResources().getColor(R.color.blue1));
            buttonIndNotify.setBackgroundColor(Color.RED);
        }else if(contentFragment instanceof DoorAccessFragment || fm.getBackStackEntryCount() == 0){
            finish();
        }
    }

    private void swichContent(Fragment fragment, String argItemId) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        while (fragmentManager.popBackStackImmediate());

        if (fragment != null) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            transaction.replace(R.id.content_frame, fragment, argItemId);

            if (!(fragment instanceof DoorAccessFragment)) {
                transaction.addToBackStack(argItemId);
            }
            transaction.commit();
            contentFragment = fragment;
        }
    }

    private void initNotifyResource() {
        linearMargin = (LinearLayout)findViewById(R.id.notify_margin);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams)linearMargin.getLayoutParams();
        layoutParams.topMargin = AWSIoTDoorActivity.statusBarHeight;
        linearMargin.setLayoutParams(layoutParams);

        buttonAccess = (Button) findViewById(R.id.button_inout);
        buttonIndAccess= (Button) findViewById(R.id.ind_inout);
        buttonNotify = (Button) findViewById(R.id.button_record);
        buttonIndNotify = (Button) findViewById(R.id.ind_doorrecord);

        buttonAccess.setBackgroundResource(R.drawable.tab_bg_selected);
        buttonAccess.setTextColor(getResources().getColor(R.color.white));
        buttonIndAccess.setBackgroundColor(Color.RED);
        buttonNotify.setBackgroundResource(R.drawable.tab_bg_unselected);
        buttonNotify.setTextColor(getResources().getColor(R.color.dark_gray));
        buttonIndNotify.setBackgroundColor(getResources().getColor(R.color.blue1));
    }
}
