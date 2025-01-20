package com.renesas.wifi.awsiot.shadow;

public class DeviceStatus {
    public State state;

    DeviceStatus() {
        state = new State();
    }

    public class State {
        public Reported reported;
        Delta delta;

        State() {
            reported = new Reported();
            delta = new Delta();
        }

        public class Reported {
            Reported() {
            }
            public String doorStat;
            public String windowStat;
            public float temperature;
            public float battery;
            public int OTAupdate;  //2 : update progressing, 1 : something to update, 0: nothing to update
            public String OTAresult;
        }

        public class Delta {
            Delta() {
            }
            public String doorStat;
            public String windowStat;
            public float temperature;
            public float battery;
        }
    }

    public Long version;
    public Long timestamp;
}
