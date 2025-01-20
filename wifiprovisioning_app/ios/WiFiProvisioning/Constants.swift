/*
* Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License").
* You may not use this file except in compliance with the License.
* A copy of the License is located at
*
*  http://aws.amazon.com/apache2.0
*
* or in the "license" file accompanying this file. This file is distributed
* on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
* express or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/

import Foundation
import AWSCore

//WARNING: To run this sample correctly, you must set the following constants.

let CertificateSigningRequestCommonName = "WiFiProvisioning Application"
let CertificateSigningRequestCountryName = "Korea"
let CertificateSigningRequestOrganizationName = "Dialog Semiconductor"
let CertificateSigningRequestOrganizationalUnitName = "IoT Wi-Fi"

let POLICY_NAME = "DOORLOCK_POLICY"

// This is the endpoint in your AWS IoT console. eg: https://xxxxxxxxxx.iot.<region>.amazonaws.com
let AWS_REGION = AWSRegionType.APNortheast2

//For both connecting over websockets and cert, IOT_ENDPOINT should look like
//https://xxxxxxx-ats.iot.REGION.amazonaws.com
let IOT_ENDPOINT = "https://a1kzdt4nun8bnh-ats.iot.ap-northeast-2.amazonaws.com"

//Used as keys to look up a reference of each manager
let AWS_IOT_DATA_MANAGER_KEY = "MyIotDataManager"
let AWS_IOT_MANAGER_KEY = "MyIotManager"
let AWS_IOT_DATA_KEY = "MyIotData"
