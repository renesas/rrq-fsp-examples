//
//  DeviceViewController.swift
//  WiFiProvisioning
//
//  Created by livekim on 2021/01/08.
//
// ******************************************************************************
//*
//* Copyright (C) 2023 Renesas Electronics.
//* This computer program includes Confidential, Proprietary Information
//* of Renesas Electronics. All Rights Reserved.
//*
//*******************************************************************************

import UIKit
import AVFoundation
import NetworkExtension
import SystemConfiguration.CaptiveNetwork
import Alamofire
import CoreLocation
import SystemConfiguration

class ConnectDeviceViewController: UIViewController, CLLocationManagerDelegate {
    
    let appDelegate = UIApplication.shared.delegate as! AppDelegate
    
    var tryConnectRenesasDevice = false
    
    var locationManager = CLLocationManager()
    var currentNetworkInfos: Array<NetworkInfo>? {
        get {
            return SSID.fetchNetworkInfo()
        }
    }
    
    @IBOutlet weak var btnBack: UIButton!
    @IBOutlet weak var btnNext: UIButton!
    @IBOutlet weak var labelConnecting: UILabel!
    @IBOutlet weak var labelConnected: UILabel!
    
    struct Connectivity {
        static let sharedInstance = NetworkReachabilityManager()!
        static var isConnectedToInternet:Bool {
            return self.sharedInstance.isReachable
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        navigationController?.setNavigationBarHidden(true, animated: false)
        
        locationManager.delegate = self
        self.locationManager.requestWhenInUseAuthorization()
        
        labelConnecting.isHidden = false
        labelConnected.isHidden = true
        
        self.removeConfigDA16200()
        
        self.btnNext.layer.cornerRadius = 10
        self.btnNext.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnNext.layer.shadowOpacity = 1;
        self.btnNext.layer.shadowRadius = 1;
        self.btnNext.layer.shadowOffset = CGSize(width: 1, height: 4)
        self.btnNext.isHidden = true
    }
    
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        navigationController?.setNavigationBarHidden(true, animated: false)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        NotificationCenter.default.removeObserver(self)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
    }
    
    func removeConfigDA16200() {
        NEHotspotConfigurationManager.shared.getConfiguredSSIDs { configuration in
            if (!configuration.isEmpty) {
                NSLog("\(#fileID):\(#line) >> configuration : \(configuration)")
                NEHotspotConfigurationManager.shared.removeConfiguration(forSSID: configuration[0])
                NEHotspotConfigurationManager.shared.removeConfiguration(forHS20DomainName: configuration[0])
            }
        }
    }
    
    func  updateWiFi() {
        print("updateWiFi()")
        if let ssid = currentNetworkInfos?.first?.ssid {
            NSLog("\(#fileID):\(#line) >> connected SSID: \(ssid)")
            
            if ssid == "Renesas_IoT_WiFi" {
                print("=> updateWiFi 1")
                appDelegate.isRenesasIoTWiFiDevice = true
                self.labelConnecting.isHidden = true
                self.labelConnected.isHidden = false
                self.btnNext.isHidden = false
            } else if ssid == "Dialog_DA16200" {
                print("=> updateWiFi 2")
                appDelegate.isDialogDevice = true
                self.labelConnecting.isHidden = true
                self.labelConnected.isHidden = false
                self.btnNext.isHidden = false
            } else if ssid == "Renesas_DA16200" {
                print("=> updateWiFi 3")
                appDelegate.isRenesasDevice = true
                self.labelConnecting.isHidden = true
                self.labelConnected.isHidden = false
                self.btnNext.isHidden = false
            }
            else {
                usleep(8000000)
                if (tryConnectRenesasDevice ==  false) {
                    print("=> updateWiFi 4")
                    Utility.hideLoader(view: self.view)
                    self.connectToRenesasDevice()
                } else {
                    print("=> updateWiFi 5")
                    
                    if (appDelegate.isDialogDevice || appDelegate.isRenesasDevice || appDelegate.isRenesasIoTWiFiDevice) {
                        self.labelConnecting.isHidden = false
                        self.labelConnected.isHidden = true
                        self.btnNext.isHidden = true
                        self.showConnectFailDialog()
                    }
                }
            }
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        if status == .authorizedWhenInUse {
            self.connectToRenesasIoTWiFiAp()
        }
    }
    
    func checkConnectivity() {
        btnNext.alpha = 0.5
        btnNext.isEnabled = false
        Utility.showLoader(message: "", view: view)
    }
    
    @IBAction func onBtnBack(_ sender: UIButton) {
        let DA16200mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "DA16200MainViewController")
        DA16200mainVC.modalPresentationStyle = .fullScreen
        present(DA16200mainVC, animated: true, completion: nil)
    }
    
    @IBAction func onBtnNext(_ sender: UIButton) {
        let selctNetworkVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "SelectNetworkViewController")
        selctNetworkVC.modalPresentationStyle = .fullScreen
        present(selctNetworkVC, animated: true, completion: nil)
    }
    
    func connectToDialogDevice() {
        print("connectToDialogDevice()")
        Utility.showLoader(message: "Conntecting to RENESAS device ...", view: view)
        
        let wifiConfiguration = NEHotspotConfiguration(
            ssid: appDelegate.dialogDeviceSSID,
            passphrase: appDelegate.devicePwd,
            isWEP: false)
        wifiConfiguration.joinOnce = false
        
        NEHotspotConfigurationManager.shared.apply(wifiConfiguration) { error in
            
            Utility.hideLoader(view: self.view)
            
            if error == nil {
                
                NSLog("\(#fileID):\(#line) >> Connected 1")
                if #available(iOS 13.0, *) {
                    let status = CLLocationManager.authorizationStatus()
                    if status == .authorizedWhenInUse {
                        self.updateWiFi()
                    } else {
                        self.locationManager.delegate = self
                        self.locationManager.requestWhenInUseAuthorization()
                    }
                } else {
                    self.updateWiFi()
                }
                
            } else {
                NSLog("\(#fileID):\(#line) >> \(error.debugDescription)")
                if (error.debugDescription.contains("already associated")) {
                    if Connectivity.isConnectedToInternet {
                        NSLog("\(#fileID):\(#line) >> Connected 2")
                        if #available(iOS 13.0, *) {
                            let status = CLLocationManager.authorizationStatus()
                            if status == .authorizedWhenInUse {
                                self.updateWiFi()
                            } else {
                                self.locationManager.delegate = self
                                self.locationManager.requestWhenInUseAuthorization()
                            }
                        } else {
                            self.updateWiFi()
                        }
                    } else {
                        NSLog("\(#fileID):\(#line) >> Not Connected 3")
                        if #available(iOS 13.0, *) {
                            let status = CLLocationManager.authorizationStatus()
                            if status == .authorizedWhenInUse {
                                self.updateWiFi()
                            } else {
                                self.locationManager.delegate = self
                                self.locationManager.requestWhenInUseAuthorization()
                            }
                        } else {
                            self.updateWiFi()
                        }
                    }
                }
            }
        }
    }
    
    func connectToRenesasIoTWiFiAp() {
        NSLog("\(#fileID):\(#line) >> connectToRenesasIoTWiFiAp()")
        //startTimer()
        Utility.showLoader(message: "Conntecting to RENESAS device ...", view: view)
        
        let wifiConfiguration = NEHotspotConfiguration(
            ssid: appDelegate.renesasIoTWiFiSSID,
            passphrase: appDelegate.devicePwd,
            isWEP: false)
        
        wifiConfiguration.joinOnce = false
        
        NEHotspotConfigurationManager.shared.apply(wifiConfiguration) { error in
            
            Utility.hideLoader(view: self.view)
            
            if error == nil {
                
                if let ssid = self.currentNetworkInfos?.first?.ssid {
                    NSLog("\(#fileID):\(#line) >> connected SSID: \(ssid)")
                    if ssid == "Renesas_IoT_WiFi" {
                        NSLog("\(#fileID):\(#line) >> Renesas_IoT_WiFi connected!!")
                        Utility.hideLoader(view: self.view)
                        self.appDelegate.isDialogDevice = true
                        self.labelConnecting.isHidden = true
                        self.labelConnected.isHidden = false
                        self.btnNext.isHidden = false
                    } else {
                        self.connectToDialogAp()
                    }
                }
                
                NSLog("\(#fileID):\(#line) >> Connected 1")
                if #available(iOS 13.0, *) {
                    let status = CLLocationManager.authorizationStatus()
                    if status == .authorizedWhenInUse {
                        
                    } else {
                        self.locationManager.delegate = self
                        self.locationManager.requestWhenInUseAuthorization()
                    }
                } else {
                    
                }
                
            } else {
                
                
            }
        }
    }
    
    func connectToDialogAp() {
        NSLog("\(#fileID):\(#line) >> connectToDialogAp()")
        
        Utility.hideLoader(view: self.view)
        Utility.showLoader(message: "Conntecting to RENESAS device ...", view: view)
        
        let wifiConfiguration = NEHotspotConfiguration(
            ssid: appDelegate.dialogDeviceSSID,
            passphrase: appDelegate.devicePwd,
            isWEP: false)
        
        wifiConfiguration.joinOnce = false
        
        NEHotspotConfigurationManager.shared.apply(wifiConfiguration) { error in
            
            Utility.hideLoader(view: self.view)
            
            if error == nil {
                
                if let ssid = self.currentNetworkInfos?.first?.ssid {
                    NSLog("\(#fileID):\(#line) >> connected SSID: \(ssid)")
                    if ssid == "Dialog_DA16200" {
                        Utility.hideLoader(view: self.view)
                        self.appDelegate.isDialogDevice = true
                        self.labelConnecting.isHidden = true
                        self.labelConnected.isHidden = false
                        self.btnNext.isHidden = false
                    } else {
                        self.connectToRenesasAp()
                    }
                }
                
                NSLog("\(#fileID):\(#line) >> Connected 1")
                if #available(iOS 13.0, *) {
                    let status = CLLocationManager.authorizationStatus()
                    if status == .authorizedWhenInUse {
                        
                    } else {
                        self.locationManager.delegate = self
                        self.locationManager.requestWhenInUseAuthorization()
                    }
                } else {
                    
                }
                
            } else {
                
            }
        }
    }
    
    func connectToRenesasDevice() {
        
        print("connectToRenesasDevice()")
        
        self.tryConnectRenesasDevice = true
        Utility.showLoader(message: "Conntecting to RENESAS device ...", view: view)
        
        let wifiConfiguration = NEHotspotConfiguration(
            ssid: appDelegate.renesasDeviceSSID,
            passphrase: appDelegate.devicePwd,
            isWEP: false)
        
        wifiConfiguration.joinOnce = false
        
        NEHotspotConfigurationManager.shared.apply(wifiConfiguration) { error in
            
            Utility.hideLoader(view: self.view)
            
            if error == nil {
                print(">> Connected 4")
                if #available(iOS 13.0, *) {
                    let status = CLLocationManager.authorizationStatus()
                    if status == .authorizedWhenInUse {
                        self.updateWiFi()
                    } else {
                        self.locationManager.delegate = self
                        self.locationManager.requestWhenInUseAuthorization()
                    }
                } else {
                    self.updateWiFi()
                }
                
            } else {
                NSLog("\(#fileID):\(#line) >> \(error.debugDescription)")
                if (error.debugDescription.contains("already associated")) {
                    if Connectivity.isConnectedToInternet {
                        NSLog("\(#fileID):\(#line) >> Connected 5")
                        if #available(iOS 13.0, *) {
                            let status = CLLocationManager.authorizationStatus()
                            if status == .authorizedWhenInUse {
                                self.updateWiFi()
                            } else {
                                self.locationManager.delegate = self
                                self.locationManager.requestWhenInUseAuthorization()
                            }
                        } else {
                            self.updateWiFi()
                        }
                        
                    } else {
                        NSLog("\(#fileID):\(#line) >> Not Connected 6")
                        if #available(iOS 13.0, *) {
                            let status = CLLocationManager.authorizationStatus()
                            if status == .authorizedWhenInUse {
                                self.updateWiFi()
                            } else {
                                self.locationManager.delegate = self
                                self.locationManager.requestWhenInUseAuthorization()
                            }
                        } else {
                            self.updateWiFi()
                        }
                        
                    }
                    
                }
                
            }
        }
    }
    
    func connectToRenesasAp() {
        
        print("connectToRenesasAp()")
        
        self.tryConnectRenesasDevice = true
        
        Utility.showLoader(message: "Conntecting to RENESAS device ...", view: view)
        
        let wifiConfiguration = NEHotspotConfiguration(
            ssid: appDelegate.renesasDeviceSSID,
            passphrase: appDelegate.devicePwd,
            isWEP: false)
        
        wifiConfiguration.joinOnce = false
        
        NEHotspotConfigurationManager.shared.apply(wifiConfiguration) { error in
            
            Utility.hideLoader(view: self.view)
            
            if error == nil {
                
                if let ssid = self.currentNetworkInfos?.first?.ssid {
                    NSLog("\(#fileID):\(#line) >> connected SSID: \(ssid)")
                    if ssid == "Renesas_DA16200" {
                        
                        Utility.hideLoader(view: self.view)
                        self.appDelegate.isDialogDevice = true
                        self.labelConnecting.isHidden = true
                        self.labelConnected.isHidden = false
                        self.btnNext.isHidden = false
                    } else {
                        self.showConnectFailDialog()
                    }
                }
                
                print(">> Connected 4")
                if #available(iOS 13.0, *) {
                    let status = CLLocationManager.authorizationStatus()
                    if status == .authorizedWhenInUse {
                        
                    } else {
                        self.locationManager.delegate = self
                        self.locationManager.requestWhenInUseAuthorization()
                    }
                } else {
                    
                }
                
            } else {
                
                
            }
        }
    }
    
    
    //MARK: - Show Retry Dialog
    
    func showConnectFailDialog() {
        let alertTitle = "Failed to connect to RENESAS device"
        
        let titleParagraphStyle = NSMutableParagraphStyle()
        titleParagraphStyle.alignment = NSTextAlignment.center
        
        let attributedTitleText = NSMutableAttributedString(
            string: alertTitle,
            attributes: [
                NSAttributedString.Key.paragraphStyle: titleParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 18.0)
            ]
        )
        
        let alertMessage = """
            
            1. Please check if the SDK version is 2.3.4.1 or higher.
            2. Make sure the RENESAS Wi-Fi device is in provisioning mode.
            3. Check whether the RENESAS Wi-Fi device's Soft-AP SSID is set to "Renesas_IoT_WiFi" or "Renesas_DA16200" or "Dialog_DA16200
            """
        
        let messageParagraphStyle = NSMutableParagraphStyle()
        messageParagraphStyle.alignment = NSTextAlignment.left
        
        let attributedMessageText = NSMutableAttributedString(
            string: alertMessage,
            attributes: [
                NSAttributedString.Key.paragraphStyle: messageParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 15.0)
            ]
        )
        
        let alert = UIAlertController.init(title: nil, message: nil, preferredStyle: UIAlertController.Style.alert)
        alert.setValue(attributedTitleText, forKey: "attributedTitle")
        alert.setValue(attributedMessageText, forKey: "attributedMessage")
        
        let okAction = UIAlertAction(title: "OK", style: .default, handler: { (action) -> Void in
            //self.connectToDevice()
            let mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "MainViewController")
            mainVC.modalPresentationStyle = .fullScreen
            self.present(mainVC, animated: true, completion: nil)
            
        })
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
}

public class SSID {
    class func fetchNetworkInfo() -> [NetworkInfo]? {
        if let interfaces: NSArray = CNCopySupportedInterfaces() {
            var networkInfos = [NetworkInfo]()
            for interface in interfaces {
                let interfaceName = interface as! String
                var networkInfo = NetworkInfo(interface: interfaceName,
                                              success: false,
                                              ssid: nil,
                                              bssid: nil)
                if let dict = CNCopyCurrentNetworkInfo(interfaceName as CFString) as NSDictionary? {
                    networkInfo.success = true
                    networkInfo.ssid = dict[kCNNetworkInfoKeySSID as String] as? String
                    networkInfo.bssid = dict[kCNNetworkInfoKeyBSSID as String] as? String
                }
                networkInfos.append(networkInfo)
            }
            return networkInfos
        }
        return nil
    }
}

struct NetworkInfo {
    var interface: String
    var success: Bool = false
    var ssid: String?
    var bssid: String?
}
