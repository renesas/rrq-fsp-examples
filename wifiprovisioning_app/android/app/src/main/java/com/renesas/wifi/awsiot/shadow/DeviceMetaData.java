package com.renesas.wifi.awsiot.shadow;

public class DeviceMetaData {
    public MetaData metadata;

    DeviceMetaData() {
        metadata = new MetaData();
    }

    public class MetaData {
        public Reported reported;

        MetaData() {
            reported = new Reported();
        }

        public class Reported {

            public DoorStat doorStat;
            public WindowStat windowStat;
            public Temperature temp;
            public Battery battery;
            public OTAupdate OTAupdate;
            public OTAresult OTAresult;

            Reported() {

                doorStat = new DoorStat();
                windowStat = new WindowStat();
                temp = new Temperature();
                battery = new Battery();
                OTAupdate = new OTAupdate();
                OTAresult = new OTAresult();
            }

            public class DoorStat {
                public Long timestamp;
            }

            public class WindowStat {
                public Long timestamp;
            }

            public class Temperature {
                public Long timestamp;
            }

            public class Battery {
                public Long timestamp;
            }

            public class OTAupdate {
                public Long timestamp;
            }

            public class OTAresult {
                public Long timestamp;
            }
        }

    }

    public Long version;
    public Long timestamp;
}
