package com.renesas.wifi.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.IOException;

/**
 * Created by live.kim on 2017-04-07.
 */

public class NetworkUtil {

    public static int TYPE_WIFI = 1;
    public static int TYPE_MOBILE = 2;
    public static int TYPE_NOT_CONNECTED = 0;

    public static int getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if(activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                return TYPE_WIFI;

            if(activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                return TYPE_MOBILE;
        }
        return TYPE_NOT_CONNECTED;
    }

    public static String getConnectivityStatusString(Context context) {
        int conn = NetworkUtil.getConnectivityStatus(context);
        String status = null;
        if (conn == NetworkUtil.TYPE_WIFI) {
            status = "Wi-Fi enabled";
        } else if (conn == NetworkUtil.TYPE_MOBILE) {
            status = "Mobile data enabled";
        } else if (conn == NetworkUtil.TYPE_NOT_CONNECTED) {
            status = "Not connected to Internet";
        }
        return status;
    }

    public static boolean isPingSuccess(){
        boolean networkState = false;

        Runtime runTime = Runtime.getRuntime();

        String host = "aws.amazon.com";
        String cmd = "ping -c 1 -W 10 "+host; //-c 1 : send only 1 time ping, W : seconds to wait for the first response
        Process proc = null;

        try {
            proc = runTime.exec(cmd);
        } catch(IOException ie){
            MyLog.i("runtime.exec() : "+ie.getMessage());
        }

        try {
            proc.waitFor();
        } catch(InterruptedException ie){
            MyLog.i("proc.waitFor : "+ie.getMessage());
        }

        // 0 : success, 1 : fail, 2 : error
        int result = proc.exitValue();

        if (result == 0) {
            MyLog.i("ping test result : "+"success");
            networkState = true;
        } else {
            MyLog.i("ping test result : "+"fail");
            networkState = false;
        }

        return networkState;
    }
}
