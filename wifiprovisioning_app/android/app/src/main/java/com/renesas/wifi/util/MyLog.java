package com.renesas.wifi.util;

import android.util.Log;

final public class MyLog {

    final static String TAG = "RENESAS_WIFI_APP";
    private static int level = MyLog.ERROR;

    private final static int NOLOG =0;
    private final static int INFO =1;
    private final static int ERROR =2;
    private final static int DEBUG =3;

    public static  void i(String _log)
    {
        String _info = buildLogMsg(_log);
        Log.w(TAG,_info) ;

    }

    public static void e(String _log)
    {
        String _info = buildLogMsg(_log);
        Log.e(TAG, " Error : " + _info);
    }

    public static void d(String _log)
    {
        String _info = buildLogMsg(_log);
        Log.e(TAG," Debug : " + _info);

    }

    private static String buildLogMsg(String msg) {
        StackTraceElement ste = Thread.currentThread().getStackTrace()[4];

        StringBuilder sb = new StringBuilder();

        sb.append("[");
        sb.append(ste.getFileName());
        sb.append(" > ");
        sb.append(ste.getMethodName());
        sb.append(" #");
        sb.append(ste.getLineNumber());
        sb.append("] ");
        sb.append(msg);

        return sb.toString();
    }
}