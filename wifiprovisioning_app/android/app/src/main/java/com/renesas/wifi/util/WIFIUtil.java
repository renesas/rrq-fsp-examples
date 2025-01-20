package com.renesas.wifi.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

public class WIFIUtil implements InfWIFIConsts {

    private static WIFIUtil __sharedWifiUtil = null;

    private String mSSID = null;
    private String mWPA = null;

    public static WIFIUtil getInstance() {
        if (__sharedWifiUtil == null)
            __sharedWifiUtil = new WIFIUtil();
        return __sharedWifiUtil;
    }

    public WIFIUtil() {
        super();
    }

    public void setSSIDandWPA(String ssid, String wpa) {
        mSSID = ssid;
        mWPA = wpa;
    }

    public static String getSSID(Context context) {

        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo;
        String ssid = null;
        wifiInfo = wifiManager.getConnectionInfo();

        if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
            ssid = wifiInfo.getSSID().replaceAll("\"", "");
        }
        MyLog.i("SSID = " + ssid);

        if (ssid == null)
            ssid = "";
        return ssid;
    }

    public static boolean isConnectWIFI(Context context) {

        boolean bIsWiFiConnect = false;
        ConnectivityManager oManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo oInfo = oManager.getActiveNetworkInfo();

        if (oInfo != null) {
            NetworkInfo.State oState = oInfo.getState();
            if (oState == NetworkInfo.State.CONNECTED) {
                switch (oInfo.getType()) {
                    case ConnectivityManager.TYPE_WIFI:
                        bIsWiFiConnect = true;
                        break;
                    case ConnectivityManager.TYPE_MOBILE:
                        break;
                    default:
                        break;
                }
            }
        }
        return bIsWiFiConnect;
    }

    public void removeWifiSpecialSSID(Context context, String ssid) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        MyLog.i("isConnectWIFI : " + String.valueOf(isConnectWIFI(context)));
        if (isConnectWIFI(context)) {
            if (ssid != null && getSSID(context).equalsIgnoreCase(ssid)) {
                int networkId = wifiManager.getConnectionInfo().getNetworkId();
                MyLog.i("removeNetwork : " + String.valueOf(networkId));
                wifiManager.removeNetwork(networkId);
                wifiManager.saveConfiguration();
                wifiManager.disconnect();
                wifiManager.disableNetwork(networkId);
            }
        }
    }


    private static Boolean isHexWepKey(String wepKey) {
        final int len = wepKey.length();

        // WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
        if (len != 10 && len != 26 && len != 58) {
            return false;
        }
        return isHex(wepKey);
    }

    private static Boolean isHex(String key) {
        for (int i = key.length() - 1; i >= 0; i--) {
            final char c = key.charAt(i);
            if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f')) {
                return false;
            }
        }
        return true;
    }

    /**
     * ***************************************************************************************
     *
     * @param context
     * @return InetAddress
     * ***************************************************************************************
     * @brief Set default network.(no Internet connection)
     */
    public static void setDefaultNetworkForNoInternet(final Context context) {

        MyLog.d("setDefaultNetworkForNoInternet()");

        final ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkRequest req = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        connManager.requestNetwork(req, new ConnectivityManager.NetworkCallback() {

            @Override
            public void onAvailable(Network network) {

                MyLog.d("Network is onAvailable(NoInternet)");

                super.onAvailable(network);

                boolean result = false;

                if (network == null) {
                    MyLog.e("Network is null");
                    return;
                }

                connManager.unregisterNetworkCallback(this);

                if (getSSID(context).equals(StaticDataSave.deviceSSID)) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        result = ConnectivityManager.setProcessDefaultNetwork(network);
                    } else {
                        MyLog.d("bindProcessToNetwork");
                        result = connManager.bindProcessToNetwork(network);
                    }
                }
                MyLog.d("DefaultNetwork:" + result);
            }

            @Override
            public void onUnavailable() {
                MyLog.d("Network is onUnavailable(NoInternet)");
                super.onUnavailable();
            }
        });
    }


    /**
     * ***************************************************************************************
     *
     * @param context
     * @return InetAddress
     * ***************************************************************************************
     * @brief Set default network.(Internet connection)
     */
    public static void setDefaultNetworkForInternet(Context context) {
        MyLog.d("setDefaultNetworkForInternet()");

        final ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest req = new NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build();

        connManager.requestNetwork(req, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                MyLog.d("Network is onAvailable(Internet)");

                super.onAvailable(network);

                boolean result = false;

                if (network == null) {
                    MyLog.e("Network is null");
                }

                connManager.unregisterNetworkCallback(this);

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    result = ConnectivityManager.setProcessDefaultNetwork(network);
                } else {
                    result = connManager.bindProcessToNetwork(network);
                }

                MyLog.d("DefaultNetwork:" + result);
            }

            @Override
            public void onUnavailable() {
                super.onUnavailable();
                MyLog.d("Network is onUnavailable(Internet)");
            }
        });
    }
}