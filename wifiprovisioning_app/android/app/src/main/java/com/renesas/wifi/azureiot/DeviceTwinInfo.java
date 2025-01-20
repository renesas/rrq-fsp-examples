package com.renesas.wifi.azureiot;

class DeviceTwinInfo {

    public Properties properties;

    DeviceTwinInfo() {
        properties = new Properties();
    }

    public class Properties {
        public Reported reported;

        Properties() {
            reported = new Reported();

        }

        public class Reported {

            public float temperature;
            public float battery;
            public int doorStateChange;
            public String openMethod;
            public boolean doorState;
            public boolean doorBell;
            public int doorOpenMode;
            public int OTAupdate;
            public String OTAresult;
            public String OTAversion;

            public Metadata metadata;

            Reported() {
                metadata = new Metadata();
            }

            public class Metadata {

                public String lastUpdated;
                public Temperature temperature;
                public Battery battery;
                public DoorState doorState;
                public OTAupdate OTAupdate;
                public OTAresult OTAresult;

                Metadata() {
                    temperature = new Temperature();
                    battery = new Battery();
                    doorState = new DoorState();
                    OTAupdate = new OTAupdate();
                    OTAresult = new OTAresult();
                }

                public class Temperature {
                    public String lastUpdated;
                }
                public class Battery {
                    public String lastUpdated;
                }
                public class DoorState {
                    public String lastUpdated;
                }
                public class OTAupdate {
                    public String lastUpdated;
                }
                public class OTAresult {
                    public String lastUpdated;
                }
            }

        }
    }

}
