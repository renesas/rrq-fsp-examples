package com.renesas.wifi.DA16600.adapter.device;

public class DeviceRowItem {
    private int imageId;
    private String name;
    private int level;
    private String address;


    public DeviceRowItem(int imageId, String name, int level, String address, int deviceImageId) {
        this.imageId = imageId;
        this.name = name;
        this.level = level;
        this.address = address;
    }

    public int getImageId() {
        return imageId;
    }
    public void setImageId(int imageId) {
        this.imageId = imageId;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public String getAddress() { return address;}


}
