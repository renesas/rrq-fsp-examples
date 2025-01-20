package com.renesas.wifi.awsiot.shadow;

public class DeviceControl {
    public State state;

    DeviceControl() {
        state = new State();
    }

    public class State {
        Desired desired;
        Delta delta;

        State() {
            desired = new Desired();
            delta = new Delta();
        }

        public class Desired {
            Desired() {
            }

        }

        public class Delta {
            Delta() {
            }

        }
    }

    public Long version;
    public Long timestamp;
}
