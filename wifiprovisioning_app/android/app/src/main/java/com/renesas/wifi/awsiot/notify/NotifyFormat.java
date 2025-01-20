package com.renesas.wifi.awsiot.notify;

public class NotifyFormat {

    private int index;
    private String notifyDate;
    private String notifyTime;
    private String notifyMessage;

    public NotifyFormat (int _index, String _notifyDate, String _notifyTime, String _notifyMessage){
        super();
        this.index = _index;
        this.notifyDate = _notifyDate;
        this.notifyTime = _notifyTime;
        this.notifyMessage = _notifyMessage;
    }

    public int getNotifyIndex() {return index;}
    public String getNotifyDate() {return notifyDate;}
    public String getNotifyTime() {return notifyTime;}
    public String getNotifyMessage() {return  notifyMessage;}

}
