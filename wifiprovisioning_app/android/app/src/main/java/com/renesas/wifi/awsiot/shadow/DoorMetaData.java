/**
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */

package com.renesas.wifi.awsiot.shadow;

public class DoorMetaData {
    public MetaData metadata;

    DoorMetaData() {
        metadata = new MetaData();
    }

    public class MetaData {
        public Reported reported;

        MetaData() {
            reported = new Reported();
        }

        public class Reported {

            public DoorState doorState;
            public Temperature temperature;
            public Battery battery;
            public OTAupdate OTAupdate;
            public OTAresult OTAresult;

            Reported() {
                doorState = new DoorState();
                temperature = new Temperature();
                battery = new Battery();
                OTAupdate = new OTAupdate();
                OTAresult = new OTAresult();
            }

            public class DoorState {
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
