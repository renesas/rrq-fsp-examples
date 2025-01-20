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

/**
 * <pre>
 * {
 *   "state": {
 *     "desired": {
 *       "intTemp": 72,
 *       "extTemp": 45,
 *       "curState": "stopped"
 *     },
 *     "delta": {
 *       "intTemp": 72,
 *       "extTemp": 45,
 *       "curState": "stopped"
 *     }
 *   },
 *   "metadata": {
 *     "desired": {
 *       "intTemp": {
 *         "timestamp": 1449791237
 *       },
 *       "extTemp": {
 *         "timestamp": 1449791237
 *       },
 *       "curState": {
 *         "timestamp": 1449791237
 *       }
 *     }
 *   },
 *   "version": 6151,
 *   "timestamp": 1449791576
 * }
 * </pre>
 */
public class SensorStatus {
    public State state;

    SensorStatus() {
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

            public float temperature;
            public float humidity;
            public float pressure;
            public float gaslock;
            public float proximity;
            public float ambient;
            public float magnetox;
            public float magnetoy;
            public float magnetoz;
            public float battery;
            public int wakeupnum;
            public int ledonoff;

            public int OTAupdate;  //2 : update progressing, 1 : something to update, 0: nothing to update
            public String OTAresult;
        }

        public class Delta {
            Delta() {
            }

            public float temperature;
            public float humidity;
            public float pressure;
            public float gaslock;
            public float proximity;
            public float ambient;
            public float magnetox;
            public float magnetoy;
            public float magnetoz;
            public float battery;
            public int ledonoff;

            public int OTAupdate;
            public String OTAresult;
        }
    }

    public Long version;
    public Long timestamp;
}
