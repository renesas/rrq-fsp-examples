package com.renesas.wifi.awsiot.notify;

public class AccessFormat {

    private int index;
    private String userName;
    private int userType;       // master = 0, user = 1, guest = 2
    private int actionType;     // touch = 0, phone= 1, out =2,
    private String accessDate ;          // year, month and day
    private String accessTime ;          // hour and min
    private String accessMessage;       // notification message

    public AccessFormat(int _index, String _userName, int _userType, int _actionType, String _accessDate, String _accessTime, String _accessMessage) {
        super();
        this.index = _index;
        this.userName = _userName;
        this.userType = _userType;
        this.actionType = _actionType;
        this.accessDate = _accessDate;
        this.accessTime = _accessTime;
        this.accessMessage = _accessMessage;
    }

    public int getIndex() {return index;}
    public String getUserName() {return userName;}
    public int getUserType() {return userType;}
    public int getActionType() {return actionType;}
    public String getAccessDate() {return accessDate;}
    public String getAccessTime() {return accessTime;}
    public String getAccessMessage() {return accessMessage;}

}
