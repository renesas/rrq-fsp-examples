package com.renesas.wifi.DA16600.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import com.renesas.wifi.R;
import com.renesas.wifi.activity.BaseActivity;
import com.renesas.wifi.activity.MainActivity;
import com.renesas.wifi.util.FButton;

public class DA16600MainActivity extends BaseActivity {

    private Context mContext;

    //UI resources
    private FButton btn_start;
    private ImageView iv_back;
    private ImageView iv_check0;
    private ImageView iv_check1;
    private ImageView iv_check2;
    private ImageView iv_check3;
    private ImageView iv_check4;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.da16600_activity_main);

        mContext = this;

        iv_back = findViewById(R.id.iv_back);
        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, MainActivity.class);
                startActivity(intent);
                finishAffinity();
            }
        });

        iv_check0 = findViewById(R.id.iv_check0);
        iv_check1 = findViewById(R.id.iv_check1);
        iv_check2 = findViewById(R.id.iv_check2);
        iv_check3 = findViewById(R.id.iv_check3);
        iv_check4 = findViewById(R.id.iv_check4);
        iv_check0.setColorFilter(getResources().getColor(R.color.blue3), PorterDuff.Mode.SRC_IN);
        iv_check1.setColorFilter(getResources().getColor(R.color.blue3), PorterDuff.Mode.SRC_IN);
        iv_check2.setColorFilter(getResources().getColor(R.color.blue3), PorterDuff.Mode.SRC_IN);
        iv_check3.setColorFilter(getResources().getColor(R.color.blue3), PorterDuff.Mode.SRC_IN);
        iv_check4.setColorFilter(getResources().getColor(R.color.blue3), PorterDuff.Mode.SRC_IN);

        btn_start = (FButton) findViewById(R.id.btn_start);
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), DeviceScanActivity.class);
                startActivity(intent);
                finishAffinity();
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(mContext, MainActivity.class);
        startActivity(intent);
        finishAffinity();
    }
}
