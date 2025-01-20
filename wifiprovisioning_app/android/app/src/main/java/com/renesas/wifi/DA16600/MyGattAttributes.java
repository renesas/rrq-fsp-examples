/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.renesas.wifi.DA16600;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MyGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();

    public static String WIFI_SVC_UUID = "9161b201-1b4b-4727-a3ca-47b35cdcf5c1";
    public static String WIFI_SVC_WFCMD_UUID = "9161b202-1b4b-4727-a3ca-47b35cdcf5c1";
    public static String WIFI_SVC_WFACT_RES_UUID = "9161b203-1b4b-4727-a3ca-47b35cdcf5c1";
    public static String WIFI_SVC_APSCAN_RES_UUID = "9161b204-1b4b-4727-a3ca-47b35cdcf5c1";
    public static String WIFI_SVC_PROV_DATA_UUID = "9161b205-1b4b-4727-a3ca-47b35cdcf5c1";
    public static String WIFI_SVC_AWS_DATA_UUID = "9161b206-1b4b-4727-a3ca-47b35cdcf5c1";
    public static String WIFI_SVC_AZURE_DATA_UUID = "9161b207-1b4b-4727-a3ca-47b35cdcf5c1";
    public static String GBG_SVC_UUID = "12345678-1234-5678-1234-56789abcdef0";
    public static String GBG_CHAR_UUID = "12345678-1234-5678-1234-56789abcdef1";

    static {
        attributes.put(WIFI_SVC_UUID, "WiFi Service");
        attributes.put(WIFI_SVC_WFCMD_UUID, "WiFi Service WiFi Command");
        attributes.put(WIFI_SVC_WFACT_RES_UUID, "WiFi Service WiFi Action Response");
        attributes.put(WIFI_SVC_APSCAN_RES_UUID, "WiFi Service AP Scan Response");
        attributes.put(WIFI_SVC_PROV_DATA_UUID, "WiFi Service Provisioning Data");
        attributes.put(WIFI_SVC_AWS_DATA_UUID, "WiFi Service AWS Data");
        attributes.put(WIFI_SVC_AZURE_DATA_UUID, "WiFi Service Azure Data");
        attributes.put(GBG_SVC_UUID, "Garbage Service");
        attributes.put(GBG_CHAR_UUID, "Garbage Service Characteristic");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }

    public static List<UUID> parseFromAdvertisementData(byte[] advertisedData) {
        List<UUID> uuids = new ArrayList<>();

        ByteBuffer buffer = ByteBuffer.wrap(advertisedData).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            int length = buffer.get() & 0xff;
            if (length == 0) break;

            int type = buffer.get() & 0xff;
            --length;

            switch (type) {
                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (length >= 2 && buffer.remaining() >= 2) {
                        uuids.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", buffer.getShort())));
                        length -= 2;
                    }
                    break;

                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                    while (length >= 16 && buffer.remaining() >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        uuids.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;
            }

            if (length > buffer.remaining())
                break;
            buffer.position(buffer.position() + length);
        }

        return uuids;
    }
}
