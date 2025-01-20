package com.renesas.wifi.DA16200.activity;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.renesas.wifi.R;
import com.renesas.wifi.activity.BaseActivity;
import com.renesas.wifi.activity.MainActivity;
import com.renesas.wifi.util.MyLog;

public class DA16200MainActivity extends BaseActivity {

    public static Context mContext;
    public static Activity activity;
    public static DA16200MainActivity instance;

    public static DA16200MainActivity getInstance() {return instance;}

    //UI resources
    private ImageView iv_back;
    private ImageView iv_check0;
    private ImageView iv_check1;
    private ImageView iv_check2;
    private Button btn_start;
    private MaterialDialog terminateDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        MyLog.i("=== onCreate() ===");
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.da16200_activity_main);

        mContext = this;
        instance =  this;
        activity = DA16200MainActivity.this;

        initResource();

        if (btn_start != null) {
            btn_start.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    WifiManager wifiManager = (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
                    wifiManager.setWifiEnabled(true);
                    Intent main = new Intent(DA16200MainActivity.this, SelectDeviceActivity.class);
                    main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(main);
                    finishAffinity();
                }
            });
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        MyLog.i("=== onResume() ===");

    }

    @Override
    protected void onPause() {
        super.onPause();
        MyLog.i("=== onPause() ===");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyLog.i("=== onDestroy() ===");

    }

    @Override
    public void onBackPressed() {
        Intent main = new Intent(mContext, MainActivity.class);
        main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(main);
        finishAffinity();
    }

    private void initResource() {
        iv_back = findViewById(R.id.iv_back);
        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent main = new Intent(mContext, MainActivity.class);
                main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(main);
                finishAffinity();
            }
        });
        iv_check0 = findViewById(R.id.iv_check0);
        iv_check1 = findViewById(R.id.iv_check1);
        iv_check2 = findViewById(R.id.iv_check2);
        iv_check0.setColorFilter(getResources().getColor(R.color.blue3), PorterDuff.Mode.SRC_IN);
        iv_check1.setColorFilter(getResources().getColor(R.color.blue3), PorterDuff.Mode.SRC_IN);
        iv_check2.setColorFilter(getResources().getColor(R.color.blue3), PorterDuff.Mode.SRC_IN);
        btn_start = findViewById(R.id.btn_start);
    }

    public void showTerminateDialog() {
        if (mContext != null) {
            terminateDialog = new MaterialDialog.Builder(mContext)
                    .theme(Theme.LIGHT)
                    .title("Confirm")
                    .titleGravity(GravityEnum.CENTER)
                    .titleColor(mContext.getResources().getColor(R.color.black))
                    .content("Would you like to exit provisioning?")
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

                            Intent main = new Intent(mContext, MainActivity.class);
                            main.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(main);
                            finishAffinity();
                        }
                    })
                    .build();
            terminateDialog.getWindow().setGravity(Gravity.CENTER);
            terminateDialog.show();
        }

    }

}
