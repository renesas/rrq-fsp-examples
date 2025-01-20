package com.renesas.wifi.util;

public interface InfWIFIConsts {

    /**
     * WIFI Not Enabled
     */
    final int STATUS_WIFI_NOT_ENABLE         = 0x01;

    /**
     * WIFI Enabled, Not Same SSID
     */
    final int STATUS_WIFI_NOT_SAME_SSID      = STATUS_WIFI_NOT_ENABLE << 1;

    /**
     * WIFI Enabled, Same SSID
     */
    final int STATUS_WIFI_CONNECTED_SSID     = STATUS_WIFI_NOT_SAME_SSID << 1;


    /**
     * Wifi status
     */
    final int WIFI_STATE_DISABLED            = 0x00;
    final int WIFI_STATE_DISABLING           = WIFI_STATE_DISABLED          + 0x01;
    final int WIFI_STATE_ENABLED             = WIFI_STATE_DISABLING         + 0x01;
    final int WIFI_STATE_ENABLING            = WIFI_STATE_ENABLED           + 0x01;
    final int WIFI_STATE_UNKNOWN             = WIFI_STATE_ENABLING          + 0x01;

    /**
     * Network status
     */
    final int NETWORK_STATE_CONNECTED        = WIFI_STATE_UNKNOWN           + 0x01;
    final int NETWORK_STATE_CONNECTING       = NETWORK_STATE_CONNECTED      + 0x01;
    final int NETWORK_STATE_DISCONNECTED     = NETWORK_STATE_CONNECTING     + 0x01;
    final int NETWORK_STATE_DISCONNECTING    = NETWORK_STATE_DISCONNECTED   + 0x01;
    final int NETWORK_STATE_SUSPENDED        = NETWORK_STATE_DISCONNECTING  + 0x01;
    final int NETWORK_STATE_UNKNOWN          = NETWORK_STATE_SUSPENDED      + 0x01;

    /**
     * Wifi connect method
     */
    final int WIFI_CONFIG_OPEN               = 0x01;
    final int WIFI_CONFIG_WPA                = 0X02;
    final int WIFI_CONFIG_WPA2               = 0X03;
    final int WIFI_CONFIG_WEP                = 0X04;
}