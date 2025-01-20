package com.renesas.wifi.awsiot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.renesas.wifi.util.MyLog;

public class StopServiceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent serviceIntent = new Intent(context, MonitorService.class);
        context.stopService(serviceIntent);
        MyLog.i("=== StopServiceReceiver ===");
    }
}
