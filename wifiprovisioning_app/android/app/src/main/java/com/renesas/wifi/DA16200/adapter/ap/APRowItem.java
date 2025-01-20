package com.renesas.wifi.DA16200.adapter.ap;

public class APRowItem {
    private int imageId;
    private String ssid;
    private boolean secreteMode;
    private int level;

    public APRowItem(int imageId, String ssid, boolean secreteMode, int level) {
        this.imageId = imageId;
        this.ssid = ssid;
        this.secreteMode = secreteMode;
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

    public boolean getSecreteMode() {
        return secreteMode;
    }
    public void setSSID(String ssid) {
        this.ssid = ssid;
    }
    @Override
    public String toString() {
        return ssid;
    }
    public int getLevel() {
        return level;
    }
}
