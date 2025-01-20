//
//  AppDelegate.swift
//  WiFiProvisioning
//
//  Created by livekim on 2020/12/22.
//
// ******************************************************************************
//*
//* Copyright (C) 2023 Renesas Electronics.
//* This computer program includes Confidential, Proprietary Information
//* of Renesas Electronics. All Rights Reserved.
//*
//*******************************************************************************

import UIKit
import AWSCore

struct ApInfo {
    var ssid:String = ""
    var pw:String = ""
    var securityModeString:String = ""
    var securityModeInt:Int = -1
    var isHiddenWiFi:Int = -1
    var enterpriseAuthType:Int = -1
    var enterpriseAuthProtocol:Int = -1
    var enterpriseID:String = ""
    var enterprisePassword:String = ""
}

struct ApInfo_DA16600 {
    var ssid:String = ""
    var pw:String = ""
    var security:Int = -1
    var isHiddenWiFi:Int = -1
}

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
    
    var window: UIWindow?
    
    let appVersion = "2.4.12"
    
    var deviceName = ""
    
    let dialogDeviceSSID = "Dialog_DA16200"
    let renesasDeviceSSID = "Renesas_DA16200"
    let renesasIoTWiFiSSID = "Renesas_IoT_WiFi"

    let devicePwd = "1234567890"

    var isDialogDevice = false
    var isRenesasDevice = false
    var isRenesasIoTWiFiDevice = false
    
    let hostAddress = "10.0.0.1"
    let tlsHostPort: UInt16 = 9900
    let tlsHostPort1: UInt16 = 443
    let tcpHostPort: UInt16 = 9999
    let tcpHostPort1: UInt16 = 80
    
    var peripheralName = ""
    
    var mainVC: MainViewController? = nil
    var DA16200mainVC: DA16200MainViewController? = nil
    var deviceVC: ConnectDeviceViewController? = nil
    var networkVC: SelectNetworkViewController? = nil
    var setEnterpriseVC: SetEnterpriseViewController? = nil
    var mySidemenu: MySideMenuNavigationController? = nil
    
    var DA16600mainVC: DA16600MainViewController? = nil
    var periVC: PeripheralViewController? = nil
    var periConnectedVC : PeripheralConnectedViewController? = nil
    
    var awsIoTDoorVC: AWSIoTDoorViewController? = nil
    var awsIoTSensorVC: AWSIoTSensorViewController? = nil
    var awsIoTDeviceVC: AWSIoTDeviceViewController? = nil
    
    var azureIoTDoorVC: AzureIoTDoorViewController? = nil
    
    var apInfo: ApInfo = ApInfo()
    var apInfo_DA16600: ApInfo_DA16600 = ApInfo_DA16600()
    let serverURL = "https://www.renesas.com"
    
    override init() {
        super.init()
        AWSIoTDeviceViewController.description()
    }
    
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Override point for customization after application launch.
        
        return true
    }
    
    // MARK: UISceneSession Lifecycle
    @available(iOS 13.0,*)
    func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
        // Called when a new scene session is being created.
        // Use this method to select a configuration to create the new scene with.
        return UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
    }
    @available(iOS 13.0,*)
    func application(_ application: UIApplication, didDiscardSceneSessions sceneSessions: Set<UISceneSession>) {
        // Called when the user discards a scene session.
        // If any sessions were discarded while the application was not running, this will be called shortly after application:didFinishLaunchingWithOptions.
        // Use this method to release any resources that were specific to the discarded scenes, as they will not return.
    }
    
    
}

