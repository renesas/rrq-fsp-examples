package com.renesas.wifi.DA16200.adapter.device;

public class DeviceRowItem {
    private int imageId;
    private String ssid;
    private String mac;
    private String security;
    private int level;

    public DeviceRowItem(int imageId, String ssid, String security, String mac, int level) {
        this.imageId = imageId;
        this.ssid = ssid;
        this.security = security;
        this.mac = mac;
        this.level = level;
    }
    public int getImageId() {
        return imageId;
    }
    public void setImageId(int imageId) {
        this.imageId = imageId;
    }

    public String getSSID() {
        return ssid;
    }

    public String getSecreteMode() {
        return security;
    }
    public void setSSID(String ssid) {
        this.ssid = ssid;
    }
    public String getMAC() {
        return mac;
    }
    @Override
    public String toString() {
        return ssid;
    }
    public int getLevel() {
        return level;
    }
}
