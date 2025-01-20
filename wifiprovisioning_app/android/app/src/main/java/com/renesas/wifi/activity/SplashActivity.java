package com.renesas.wifi.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Window;
import android.view.WindowManager;

import com.renesas.wifi.R;
import com.renesas.wifi.util.MyLog;

import java.lang.ref.WeakReference;


public class SplashActivity extends Activity {

    public Context mContext;
    private IntroHandler mHandler;
    public static SplashActivity instance;
    public static SplashActivity getInstance() {
        return instance;
    }

    //handler event
    public static class HandleMsg
    {
        public static final int E_INTRO_MAIN_ACVITIVY_CALL = 0;

    }


    /**
     ****************************************************************************************
     * @brief Handler class for splash activity
     * @param
     * @return none
     ****************************************************************************************
     */
    private static final class IntroHandler extends Handler
    {

        private final WeakReference<SplashActivity> ref;

        public IntroHandler(SplashActivity act)
        {
            ref = new WeakReference<>(act);
        }

        @Override
        public void handleMessage(Message msg) {
            final SplashActivity act = ref.get();

            if (act != null)
            {
                switch (msg.what)
                {
                    case HandleMsg.E_INTRO_MAIN_ACVITIVY_CALL:
                    {
                        MyLog.i(" Main activity start Call  ~ ");
                        Intent main = new Intent(act.mContext, MainActivity.class);
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

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_intro);

        mContext = SplashActivity.this;
        instance = this;

        mHandler = new IntroHandler(this);

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);

        mHandler.sendEmptyMessageDelayed(HandleMsg.E_INTRO_MAIN_ACVITIVY_CALL, 3000);

    }

    @Override
    protected void onResume() {

        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        MyLog.i("=== onStart ===");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyLog.i("=== onDestroy ===");

    }

}
