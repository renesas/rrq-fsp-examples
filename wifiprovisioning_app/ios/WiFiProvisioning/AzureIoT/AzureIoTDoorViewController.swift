//
//  AzureIoTDoorViewController.swift
//  WiFiProvisioning
//
//  Created by livekim on 2021/10/20.
//
// ******************************************************************************
//*
//* Copyright (C) 2023 Renesas Electronics.
//* This computer program includes Confidential, Proprietary Information
//* of Renesas Electronics. All Rights Reserved.
//*
//*******************************************************************************

import Foundation
import UIKit
import Pastel
import Network
import AzureIoTHubServiceClient
import AzureIoTHubClient
import CryptoKit

class AzureIoTDoorViewController: UIViewController, UITextFieldDelegate {
    
    let appDelegate = UIApplication.shared.delegate as! AppDelegate
    let defaults = UserDefaults.standard
    var iothubName: String = ""
    var sharedAccessKey: String = ""
    
    var thingName: String?
    var azureConString: String?
    var mode: Int?  //for AT-CMD
    
    let APP_DIRECT_METHOD_NAME = "AppControl"
    
    let APP_CONNECT_MESSAGE = "connected"
    let DEVICE_CONNECT_RESPONSE_MESSAGE = "yes"
    
    let APP_CONTROL_DOOR_OPEN_MESSAGE = "doorOpen"
    let DEVICE_CONTROL_OPEN_RESPONSE_MESSAGE = "opened"
    let ATCMD_APP_CONTROL_DOOR_OPEN_MESSAGE = "0 app_door open"  //for AT-CMD
    
    let APP_CONTROL_DOOR_CLOSE_MESSAGE = "doorClose"
    let DEVICE_CONTROL_CLOSE_RESPONSE_MESSAGE = "closed"
    let ATCMD_APP_CONTROL_DOOR_CLOSE_MESSAGE = "0 app_door close"  //for AT-CMD
    
    let APP_UPDATE_SHADOW_MESSAGE = "updateSensor"
    let DEVICE_UPDATE_SHADOW_RESPONSE_MESSAGE = "updated"
    let ATCMD_APP_UPDATE_SHADOW_MESSAGE = "8 app_shadow update"  //for AT-CMD
    
    let APP_CONTROL_OTA_MESSAGE = "confirmOTA"
    
    private var service_client_handle: IOTHUB_SERVICE_CLIENT_AUTH_HANDLE!;
    private var iot_msg_handle: IOTHUB_MESSAGING_HANDLE!;
    private var iot_device_method_handle: IOTHUB_SERVICE_CLIENT_DEVICE_METHOD_HANDLE!;
    
    private var iot_twin_handle: IOTHUB_SERVICE_CLIENT_DEVICE_TWIN_HANDLE!
    private var iot_device_Twin_handle: IOTHUB_SERVICE_CLIENT_DEVICE_TWIN_HANDLE!;
    
    private let iotProtocol: IOTHUB_CLIENT_TRANSPORT_PROVIDER = MQTT_Protocol
    private var iothubClientHandle: IOTHUB_CLIENT_LL_HANDLE!
    
    var timerCleanup: Timer!
    var timerDoWork: Timer!
    
    @objc var serverStatus: String = "Disconnected"
    @objc var thingStatus: String = "Disconnected"
    
    @IBOutlet weak var labelThingName: UILabel!
    @IBOutlet weak var viewConnectStatus: UIView!
    @IBOutlet weak var imageServerStatus: UIImageView!
    @IBOutlet weak var labelServerStatus: UILabel!
    @IBOutlet weak var imageThingStatus: UIImageView!
    @IBOutlet weak var labelThingStatus: UILabel!
    
    @IBOutlet weak var viewDirectMethod: UIView!
    @IBOutlet weak var labelDirectMethodName: UILabel!
    @IBOutlet weak var labelDirectMethodPayload: UILabel!
    @IBOutlet weak var labelDirectMethodReceivedStatus: UILabel!
    @IBOutlet weak var labelDirectMethodReceivedResult: UILabel!
    
    @IBOutlet weak var controlView: UIView!
    @IBOutlet weak var btnUpdateDeviceTwin: UIButton!
    @IBOutlet weak var imageDoorControl: UIImageView!
    @IBOutlet weak var labelDoorState: UILabel!
    @IBOutlet weak var labelTemperature: UILabel!
    @IBOutlet weak var labelBattery: UILabel!
    
    @IBOutlet weak var labelUpdatedTime: UILabel!
    
    @IBOutlet weak var btnOtaUpdate: UIButton!
    
    var serverStatusBlinkTimer: Timer?
    var thingStatusBlinkTimer: Timer?
    var getDeviceTwinTimer: Timer?
    var connectServerTimer: Timer?
    var connectThingTimer: Timer?
    var controlDoorTimer: Timer?
    var updateDeviceTwinTimer: Timer?
    
    var doorStateFlag: String?
    var windowStateFlag: String?
    var existOTAupdateFlag: Bool = false
    var OTAupdateProgressFlage: Bool = false
    
    var isServerConnected: Bool = false
    var isDeviceConnected: Bool = false
    var isDoorStateUpdated: Bool = false
    var isDoorStateChangeUpdated: Bool = false
    var isTemperatureUpdated: Bool = false
    var isBatteryUpdated: Bool = false
    var isOTAupdateUpdated: Bool = true
    var isProgressing: Bool = false
    var pressedButton: String?
    
    var pastDoorStateDate: String? = "yyyy-MM-dd aaa hh:mm:ss"
    var curDoorStateDate: String? = "yyyy-MM-dd aaa hh:mm:ss"
    
    var pastDoorStateChangeDate: String = "yyyy-MM-dd aaa hh:mm:ss"
    var curDoorStateChangeDate: String = "yyyy-MM-dd aaa hh:mm:ss"
    
    var pastTemperatureDate: String = "yyyy-MM-dd aaa hh:mm:ss"
    var curTemperatureDate: String = "yyyy-MM-dd aaa hh:mm:ss"
    
    var pastBatteryDate: String = "yyyy-MM-dd aaa hh:mm:ss"
    var curBatteryDate: String = "yyyy-MM-dd aaa hh:mm:ss"
    
    var pastOTAupdateDate: String = "yyyy-MM-dd aaa hh:mm:ss"
    var curOTAupdateDate: String = "yyyy-MM-dd aaa hh:mm:ss"
    
    var pastOTAresultDate: String = "yyyy-MM-dd aaa hh:mm:ss"
    var curOTAresultDate: String = "yyyy-MM-dd aaa hh:mm:ss"
    
    var doorState: UInt?
    var OTAupdate: UInt?
    var OTAresult: String?
    
    var temperaureValue: NSNumber?
    var batteryValue: UInt?
    
    var updatingAlert: UIAlertController?
    var updateSuccessAlert: UIAlertController?
    var updateFailAlert: UIAlertController?
    var connectServerTimeoutAlert: UIAlertController?
    var connectThingTimeoutAlert: UIAlertController?
    var deviceOfflineAlert: UIAlertController?
    var controlDoorTimeoutAlert: UIAlertController?
    var updateDeviceTwinTimeoutAlert: UIAlertController?
    
    func showError(message: String, sendState: Bool) {
        
        NSLog("\(#fileID):\(#line) >> \(message)")
    }
    
    override func viewDidLoad() {
        
        super.viewDidLoad()
        NSLog("\(#fileID):\(#line) >> viewDidLoad" )
        
        appDelegate.azureIoTDoorVC = self
        
        self.viewConnectStatus.backgroundColor = UIColor.black
        self.viewConnectStatus.layer.borderColor = UIColor(red: 42/255, green: 40/255, blue: 157/255, alpha: 1).cgColor
        self.viewConnectStatus.layer.borderWidth = 2.0
        self.viewConnectStatus.layer.cornerRadius = 10
        
        self.viewDirectMethod.backgroundColor = UIColor.black
        self.viewDirectMethod.layer.borderColor = UIColor(red: 42/255, green: 40/255, blue: 157/255, alpha: 1).cgColor
        self.viewDirectMethod.layer.borderWidth = 2.0
        self.viewDirectMethod.layer.cornerRadius = 10
        
        self.imageServerStatus.image = UIImage(named: "server_connect_status")
        self.imageServerStatus.image = self.imageServerStatus.image?.withRenderingMode(.alwaysTemplate)
        
        self.imageThingStatus.image = UIImage(named: "thing_connect_status")
        self.imageThingStatus.image = self.imageThingStatus.image?.withRenderingMode(.alwaysTemplate)
        
        self.imageDoorControl.layer.cornerRadius = self.imageDoorControl.frame.size.width / 2
        self.imageDoorControl.layer.masksToBounds = true
        self.imageDoorControl.clipsToBounds = true
        self.imageDoorControl.layer.borderWidth = 2
        self.imageDoorControl.layer.borderColor = UIColor.white.cgColor
        
        self.imageDoorControl.isUserInteractionEnabled = true
        let doorControlTapGesture = UITapGestureRecognizer(target: self, action: #selector(self.touchDoorControl))
        self.imageDoorControl.addGestureRecognizer(doorControlTapGesture)
        
        
        self.btnUpdateDeviceTwin.layer.cornerRadius = 10
        self.btnUpdateDeviceTwin.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnUpdateDeviceTwin.layer.shadowOpacity = 1;
        self.btnUpdateDeviceTwin.layer.shadowRadius = 1;
        self.btnUpdateDeviceTwin.layer.shadowOffset = CGSize(width: 1, height: 4)
        
        self.btnOtaUpdate.layer.cornerRadius = 10
        self.btnOtaUpdate.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnOtaUpdate.layer.shadowOpacity = 1;
        self.btnOtaUpdate.layer.shadowRadius = 1;
        self.btnOtaUpdate.layer.shadowOffset = CGSize(width: 1, height: 4)
        
        self.thingName = self.defaults.string(forKey: "thingNameKey")
        self.labelThingName.text = self.thingName!
        self.azureConString = self.defaults.string(forKey: "azureConStringKey")
        
        //for AT-CMD
        self.mode = UserDefaults.standard.integer(forKey: "modeKey")
        if (self.mode != nil) {
            NSLog("\(#fileID):\(#line) >> mode : \(self.mode!)")
        }
        //
        if (self.azureConString != nil && !self.azureConString!.isEmpty) {
            NSLog("\(#fileID):\(#line) >> azureConstring = \(self.azureConString!)")
            let azureConStringArr0 = self.azureConString?.components(separatedBy: ";")
            let azureConStringArr1 = azureConStringArr0?[0].components(separatedBy: "=")
            let azureConStringArr2 = azureConStringArr1?[1]
            let azureConStringArr3 = azureConStringArr2?.components(separatedBy: ".")
            
            self.iothubName = (azureConStringArr3?[0])!
            
            let azureConStringArr = self.azureConString?.components(separatedBy: "SharedAccessKey=")
            sharedAccessKey = (azureConStringArr?[1])!
            
            self.service_client_handle = IoTHubServiceClientAuth_CreateFromConnectionString(self.azureConString)
            if (service_client_handle == nil) {
                showError(message: ">> Failed to create IoT Service handle", sendState: false)
            }
        }
        
        self.doorStateFlag = defaults.string(forKey: "doorStateFlagKey")
        if self.doorStateFlag == "true" {  //opened
            
            if #available(iOS 13.0, *) {
                self.imageDoorControl.image = UIImage(systemName: "lock.open.fill")
            } else {
                self.imageDoorControl.image = UIImage(named: "doorlock_open_96pt")
            }
            self.labelDoorState.text = "The door is opened"
            self.labelDoorState.backgroundColor = .systemRed
        } else if self.doorStateFlag == "false" {  //closed
            
            if #available(iOS 13.0, *) {
                self.imageDoorControl.image = UIImage(systemName: "lock.fill")
            } else {
                self.imageDoorControl.image = UIImage(named: "doorlock_close_96pt")
            }
            self.labelDoorState.text = "The door is closed"
            self.labelDoorState.backgroundColor = .systemGreen
        }
        
        self.existOTAupdateFlag = self.defaults.bool(forKey: "existOTAupdateFlagKey")
        if existOTAupdateFlag == true {
            self.showBadge(withString: "New")
        } else {
            self.removeBadge()
        }
        
        self.serverStatus = "Connecting..."
        self.isServerConnected = false
        NSLog("\(#fileID):\(#line) >> \(self.serverStatus)")
        self.labelServerStatus.text = self.serverStatus
        self.labelServerStatus.textColor = UIColor.orange
        self.imageServerStatus.tintColor = .orange
        self.startServerStatusBlinkTimer()
        
        self.getDeviceTwin()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        NSLog("\(#fileID):\(#line) >> viewWillAppear" )
    }
    
    override func viewDidAppear(_ animated: Bool) {
        NSLog("\(#fileID):\(#line) >> viewDidAppear" )
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        NSLog("\(#fileID):\(#line) >> viewWillDisappear" )
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        NSLog("\(#fileID):\(#line) >> viewDidDisappear" )
        self.stopConnectServerTimer()
        self.stopGetDeviceTwinTimer()
        self.isServerConnected = false
        self.isDeviceConnected = false
        self.isProgressing = false
    }
    
    override func viewWillLayoutSubviews() {
        NSLog("\(#fileID):\(#line) >> viewWillLayoutSubviews" )
        //self.imageDoorControl = RoundImageView()
    }
    
    override func viewDidLayoutSubviews() {
        NSLog("\(#fileID):\(#line) >> viewDidLayoutSubviews" )
    }
    
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
    }
    
    // MARK: - button click event
    
    @IBAction func onBtnBack(_ sender: UIButton) {
        let mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "MainViewController")
        mainVC.modalPresentationStyle = .fullScreen
        present(mainVC, animated: true, completion: nil)
    }
    
    @IBAction func onBtnUpdateDeviceTwin(_ sender: UIButton) {
        if (self.isServerConnected && self.isDeviceConnected) {
            if (self.isProgressing == false) {
                Utility.showLoader(message: "Updating status...", view: view)
                self.labelDirectMethodName.text = self.APP_DIRECT_METHOD_NAME
                
                self.isProgressing = true
                self.pressedButton = "update"
                if (self.mode == 20) {  //General Azure IoT
                    self.labelDirectMethodPayload.text = self.APP_UPDATE_SHADOW_MESSAGE
                    self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.APP_UPDATE_SHADOW_MESSAGE)
                } else if (self.mode == 21) {  //AT-CMD Azure IoT
                    self.labelDirectMethodPayload.text = self.ATCMD_APP_UPDATE_SHADOW_MESSAGE
                    self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.ATCMD_APP_UPDATE_SHADOW_MESSAGE)
                }
                //
                self.startUpdateDeviceTwinTimer()
            }
        }
    }
    
    @objc func touchDoorControl() {
        if (self.isServerConnected && self.isDeviceConnected) {
            if (self.isProgressing == false) {
                self.doorStateFlag = defaults.string(forKey: "doorStateFlagKey")
                NSLog("\(#fileID):\(#line) >> self.doorStateFlag = \(String(describing: self.doorStateFlag))")
                if self.doorStateFlag == "true" {  //opened
                    self.labelDirectMethodName.text = self.APP_DIRECT_METHOD_NAME
                    
                    self.isProgressing = true
                    self.pressedButton = "doorClose"
                    
                    if (self.mode == 20) {  //General Azure IoT
                        self.labelDirectMethodPayload.text = self.APP_CONTROL_DOOR_CLOSE_MESSAGE
                        self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.APP_CONTROL_DOOR_CLOSE_MESSAGE)
                    } else if (self.mode == 21) {  //AT-CMD Azure IoT
                        self.labelDirectMethodPayload.text = self.ATCMD_APP_CONTROL_DOOR_CLOSE_MESSAGE
                        self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.ATCMD_APP_CONTROL_DOOR_CLOSE_MESSAGE)
                    }
                    //
                    self.startControlDoorTimer()
                    Utility.showLoader(message: "The door is being closed...", view: view)
                } else if self.doorStateFlag == "false" {  //closed
                    self.labelDirectMethodName.text = self.APP_DIRECT_METHOD_NAME
                    
                    self.isProgressing = true
                    self.pressedButton = "doorOpen"
                    
                    if (self.mode == 20) {  //General Azure IoT
                        self.labelDirectMethodPayload.text = self.APP_CONTROL_DOOR_OPEN_MESSAGE
                        self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.APP_CONTROL_DOOR_OPEN_MESSAGE)
                    } else if (self.mode == 21) {  //AT-CMD Azure IoT
                        self.labelDirectMethodPayload.text = self.ATCMD_APP_CONTROL_DOOR_OPEN_MESSAGE
                        self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.ATCMD_APP_CONTROL_DOOR_OPEN_MESSAGE)
                    }
                    //
                    
                    self.startControlDoorTimer()
                    Utility.showLoader(message: "The door is being opened...", view: view)
                }
            }
        }
    }
    
    @IBAction func onBtnOTAupdate(_ sender: UIButton) {
        if (self.isServerConnected && self.isDeviceConnected) {
            if self.existOTAupdateFlag == true {
                self.showUpdatingDialog()
                self.labelDirectMethodName.text = self.APP_DIRECT_METHOD_NAME
                self.labelDirectMethodPayload.text = self.APP_CONTROL_OTA_MESSAGE
                self.isProgressing = true
                self.pressedButton = "ota"
                self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.APP_CONTROL_OTA_MESSAGE)
            } else {
                showToast(message: "Nothing to update", seconds: 1.0)
            }
        } else {
            showToast(message: "Failed to connect to the device", seconds: 1.0)
        }
    }
    
    func doorLockCtl(lock: String) {
        
        if lock == "true" {  //open
            
            if #available(iOS 13.0, *) {
                self.imageDoorControl.image = UIImage(systemName: "lock.open.fill")
            } else {
                self.imageDoorControl.image = UIImage(named: "doorlock_open_96pt")
            }
            //rotate to y axis
            var transform: CATransform3D = CATransform3DIdentity
            UIView.animate(withDuration: 1.0, animations: {
                transform = CATransform3DRotate(transform, CGFloat(180 * Double.pi / 180.0), 0, 1, 0)
                self.imageDoorControl.layer.transform = transform
            })
            self.labelDoorState.text = " The door is opened"
            self.labelDoorState.backgroundColor = .systemRed
            self.defaults.set(self.doorStateFlag, forKey: "doorStateFlagKey")
            self.doorStateFlag = "true"
            //usleep(2000000)
            self.isProgressing = false
            
        } else if lock == "false" {  //close
            
            if #available(iOS 13.0, *) {
                self.imageDoorControl.image = UIImage(systemName: "lock.fill")
            } else {
                self.imageDoorControl.image = UIImage(named: "doorlock_close_96pt")
            }
            //rotate to y axis
            var transform: CATransform3D = CATransform3DIdentity
            UIView.animate(withDuration: 1.0, animations: {
                transform = CATransform3DRotate(transform, CGFloat(180 * Double.pi / 180.0), 0, 1, 0)
                self.imageDoorControl.layer.transform = transform
            })
            self.labelDoorState.text = " The door is closed"
            self.labelDoorState.backgroundColor = .systemGreen
            self.defaults.set(self.doorStateFlag, forKey: "doorStateFlagKey")
            self.doorStateFlag = "false"
            //usleep(2000000)
            self.isProgressing = false
        }
        
    }
    
    // MARK: - imageServerStatusBlink Timer
    func startServerStatusBlinkTimer() {
        if let timer = serverStatusBlinkTimer {
            if !timer.isValid {
                serverStatusBlinkTimer = Timer.scheduledTimer(timeInterval: 0.5, target: self, selector: #selector(serverStatusBlink), userInfo: nil, repeats: true)
            }
        } else {
            serverStatusBlinkTimer = Timer.scheduledTimer(timeInterval: 0.5, target: self, selector: #selector(serverStatusBlink), userInfo: nil, repeats: true)
        }
    }
    
    @objc func serverStatusBlink() {
        self.imageServerStatus.isHidden = !self.imageServerStatus.isHidden
        self.labelServerStatus.isHidden = !self.labelServerStatus.isHidden
    }
    
    func stopServerStatusBlinkTimer() {
        NSLog("\(#fileID):\(#line) >> stopServerStatusBlinkTimer()")
        if let timer = serverStatusBlinkTimer {
            if (timer.isValid) {
                timer.invalidate()
                serverStatusBlinkTimer = nil
            }
        }
    }
    
    // MARK: - imageThingStatusBlink Timer
    func startThingStatusBlinkTimer() {
        if let timer = thingStatusBlinkTimer {
            if !timer.isValid {
                thingStatusBlinkTimer = Timer.scheduledTimer(timeInterval: 0.5, target: self, selector: #selector(thingStatusBlink), userInfo: nil, repeats: true)
            }
        } else {
            thingStatusBlinkTimer = Timer.scheduledTimer(timeInterval: 0.5, target: self, selector: #selector(thingStatusBlink), userInfo: nil, repeats: true)
        }
    }
    
    @objc func thingStatusBlink() {
        self.imageThingStatus.isHidden = !self.imageThingStatus.isHidden
        self.labelThingStatus.isHidden = !self.labelThingStatus.isHidden
    }
    
    func stopThingStatusBlinkTimer() {
        NSLog("\(#fileID):\(#line) >> stopthingStatusBlinkTimer()")
        if let timer = thingStatusBlinkTimer {
            if (timer.isValid) {
                timer.invalidate()
                thingStatusBlinkTimer = nil
            }
        }
    }
    
    // MARK: - badge
    let badgeSize: CGFloat = 20
    let badgeTag = 1234
    
    func badgeLabel(withString string: String) -> UILabel {
        let badgeNew = UILabel(frame: CGRect(x: 0, y: 0, width: badgeSize, height: badgeSize))
        badgeNew.translatesAutoresizingMaskIntoConstraints = false
        badgeNew.tag = badgeTag
        badgeNew.layer.cornerRadius = badgeNew.bounds.size.height / 2
        badgeNew.textAlignment = .center
        badgeNew.layer.masksToBounds = true
        badgeNew.textColor = .white
        badgeNew.font = badgeNew.font.withSize(12)
        badgeNew.backgroundColor = .systemRed
        badgeNew.text = String(string)
        return badgeNew
    }
    
    func showBadge(withString string: String) {
        let badge =  badgeLabel(withString: string)
        self.btnOtaUpdate.addSubview(badge)
        NSLayoutConstraint.activate([
            badge.leftAnchor.constraint(equalTo: self.btnOtaUpdate.rightAnchor, constant: -40),
            badge.topAnchor.constraint(equalTo: self.btnOtaUpdate.topAnchor, constant: -4),
            badge.widthAnchor.constraint(equalToConstant: badgeSize * 2),
            badge.heightAnchor.constraint(equalToConstant: badgeSize)
        ])
    }
    
    func removeBadge() {
        if let badge = self.btnOtaUpdate.viewWithTag(badgeTag) {
            badge.removeFromSuperview()
        }
    }
    
    
    // MARK: - Direct Method (HTTP POST)
    
    func sendMsg(methodName: String, message: String) {
        let param = ["methodName" : methodName as Any, "responseTimeoutInSeconds" :  200, "payload" : message as Any] as [String : Any] as [String : Any]
        let paramData = try! JSONSerialization.data(withJSONObject: param, options: [])
        
        let url = URL(string: "https://"+self.iothubName+".azure-devices.net/twins/"+self.thingName!+"/methods?api-version=2018-06-30")
        
        var request = URLRequest(url: url!)
        request.httpMethod = "POST"
        request.httpBody = paramData
        
        let sasToken = self.generateSasToken(resourceUri: self.iothubName+".azure-devices.net", key: self.sharedAccessKey, policyName: "iothubowner", expiryInSeconds: 3600)
        
        NSLog("\(#fileID):\(#line) >> sasToken = \(String(describing: sasToken))")
        request.setValue(sasToken!, forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let task = URLSession.shared.dataTask(with: request) { (data, response, error) in
            
            if let e = error {
                NSLog("\(#fileID):\(#line) >> An error has occured : \(e)")
                return
            }
            
            DispatchQueue.main.async {
                
                do {
                    
                    let object = try JSONSerialization.jsonObject(with: data!, options: []) as? NSDictionary
                    
                    NSLog("\(#fileID):\(#line) >> \(String(describing: object))")
                    
                    if (String(describing: object).contains("404103")) {
                        
                        
                        self.stopConnectThingTimer()
                        self.stopControlDoorTimer()
                        self.stopUpdateDeviceTwinTimer()
                        
                        self.thingStatus = "Disconnected"
                        self.labelThingStatus.text = self.thingStatus
                        self.labelThingStatus.textColor = UIColor.red
                        self.imageThingStatus.tintColor = .red
                        self.stopThingStatusBlinkTimer()
                        
                        Utility.hideLoader(view: self.view)
                        
                        self.isProgressing = false
                        
                        if self.pressedButton == "doorClose" {  //opened
                            self.labelDirectMethodName.text = self.APP_DIRECT_METHOD_NAME
                            
                            self.isProgressing = true
                            self.pressedButton = "doorClose"
                            
                            if (self.mode == 20) {  //General Azure IoT
                                self.labelDirectMethodPayload.text = self.APP_CONTROL_DOOR_CLOSE_MESSAGE
                                self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.APP_CONTROL_DOOR_CLOSE_MESSAGE)
                            } else if (self.mode == 21) {  //AT-CMD Azure IoT
                                self.labelDirectMethodPayload.text = self.ATCMD_APP_CONTROL_DOOR_CLOSE_MESSAGE
                                self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.ATCMD_APP_CONTROL_DOOR_CLOSE_MESSAGE)
                            }
                            
                            self.startControlDoorTimer()
                            Utility.showLoader(message: "The door is being closed...", view: self.view)
                        } else if self.pressedButton == "doorOpen" {  //closed
                            self.labelDirectMethodName.text = self.APP_DIRECT_METHOD_NAME
                            
                            self.isProgressing = true
                            self.pressedButton = "doorOpen"
                            
                            if (self.mode == 20) {  //General Azure IoT
                                self.labelDirectMethodPayload.text = self.APP_CONTROL_DOOR_OPEN_MESSAGE
                                self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.APP_CONTROL_DOOR_OPEN_MESSAGE)
                            } else if (self.mode == 21) {  //AT-CMD Azure IoT
                                self.labelDirectMethodPayload.text = self.ATCMD_APP_CONTROL_DOOR_OPEN_MESSAGE
                                self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.ATCMD_APP_CONTROL_DOOR_OPEN_MESSAGE)
                            }
                            //
                            
                            self.startControlDoorTimer()
                            Utility.showLoader(message: "The door is being opened...", view: self.view)
                        } else if self.pressedButton == "update" {  //update
                            self.labelDirectMethodName.text = self.APP_DIRECT_METHOD_NAME
                            
                            self.isProgressing = true
                            self.pressedButton = "update"
                            
                            if (self.mode == 20) {  //General Azure IoT
                                self.labelDirectMethodPayload.text = self.APP_UPDATE_SHADOW_MESSAGE
                                self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.APP_UPDATE_SHADOW_MESSAGE)
                            } else if (self.mode == 21) {  //AT-CMD Azure IoT
                                self.labelDirectMethodPayload.text = self.ATCMD_APP_UPDATE_SHADOW_MESSAGE
                                self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.ATCMD_APP_UPDATE_SHADOW_MESSAGE)
                            }
                            //
                            
                            self.startUpdateDeviceTwinTimer()
                            Utility.showLoader(message: "Updating status...", view: self.view)
                        }
                    } else {
                        guard let jsonObject = object else { return }
                        
                        let status = jsonObject["status"] as? Int
                        let payload = jsonObject["payload"] as? String
                        
                        if (status != nil) {
                            self.labelDirectMethodReceivedStatus.text = String(status!)
                            self.labelDirectMethodReceivedResult.text = payload
                            
                            NSLog("\(#fileID):\(#line) >> status = \(String(describing: status))")
                            if status == 200 {
                                
                                if (payload != nil) {
                                    NSLog("\(#fileID):\(#line) >> payload = \(String(describing: payload))")
                                    
                                    if (payload == self.DEVICE_CONNECT_RESPONSE_MESSAGE) {
                                        self.stopConnectThingTimer()
                                        self.isProgressing = false
                                        self.thingStatus = "Connected"
                                        self.labelThingStatus.text = self.thingStatus
                                        self.labelThingStatus.textColor = UIColor.green
                                        self.imageThingStatus.tintColor = .green
                                        self.stopThingStatusBlinkTimer()
                                        self.imageThingStatus.isHidden = false
                                        self.labelThingStatus.isHidden = false
                                        self.isDeviceConnected = true
                                    } else if (payload == self.DEVICE_CONTROL_OPEN_RESPONSE_MESSAGE) {
                                        self.stopControlDoorTimer()
                                        Utility.hideLoader(view: self.view)
                                        
                                        self.thingStatus = "Connected"
                                        self.labelThingStatus.text = self.thingStatus
                                        self.labelThingStatus.textColor = UIColor.green
                                        self.imageThingStatus.tintColor = .green
                                        self.stopThingStatusBlinkTimer()
                                        self.imageThingStatus.isHidden = false
                                        self.labelThingStatus.isHidden = false
                                        self.isDeviceConnected = true
                                        
                                    } else if (payload == self.DEVICE_CONTROL_CLOSE_RESPONSE_MESSAGE) {
                                        self.stopControlDoorTimer()
                                        Utility.hideLoader(view: self.view)
                                        
                                        self.thingStatus = "Connected"
                                        self.labelThingStatus.text = self.thingStatus
                                        self.labelThingStatus.textColor = UIColor.green
                                        self.imageThingStatus.tintColor = .green
                                        self.stopThingStatusBlinkTimer()
                                        self.imageThingStatus.isHidden = false
                                        self.labelThingStatus.isHidden = false
                                        self.isDeviceConnected = true
                                        
                                    } else if (payload == self.DEVICE_UPDATE_SHADOW_RESPONSE_MESSAGE) {
                                        self.stopUpdateDeviceTwinTimer()
                                        self.isProgressing = false
                                        Utility.hideLoader(view: self.view)
                                        
                                        self.thingStatus = "Connected"
                                        self.labelThingStatus.text = self.thingStatus
                                        self.labelThingStatus.textColor = UIColor.green
                                        self.imageThingStatus.tintColor = .green
                                        self.stopThingStatusBlinkTimer()
                                        self.imageThingStatus.isHidden = false
                                        self.labelThingStatus.isHidden = false
                                        self.isDeviceConnected = true
                                        
                                    }
                                    
                                    //for AT-CMD
                                    else if (payload!.contains("mcu_door")) {
                                        self.stopControlDoorTimer()
                                        Utility.hideLoader(view: self.view)
                                        
                                        self.thingStatus = "Connected"
                                        self.labelThingStatus.text = self.thingStatus
                                        self.labelThingStatus.textColor = UIColor.green
                                        self.imageThingStatus.tintColor = .green
                                        self.stopThingStatusBlinkTimer()
                                        self.imageThingStatus.isHidden = false
                                        self.labelThingStatus.isHidden = false
                                        self.isDeviceConnected = true
                                        
                                        if (payload!.contains(self.DEVICE_CONTROL_OPEN_RESPONSE_MESSAGE)) {
                                            
                                        } else if (payload!.contains(self.DEVICE_CONTROL_CLOSE_RESPONSE_MESSAGE)) {
                                            
                                        }
                                        
                                    } else if (payload!.contains("mcu_shadow") && payload!.contains(self.DEVICE_UPDATE_SHADOW_RESPONSE_MESSAGE)) {
                                        self.stopUpdateDeviceTwinTimer()
                                        self.isProgressing = false
                                        Utility.hideLoader(view: self.view)
                                        
                                        self.thingStatus = "Connected"
                                        self.labelThingStatus.text = self.thingStatus
                                        self.labelThingStatus.textColor = UIColor.green
                                        self.imageThingStatus.tintColor = .green
                                        self.stopThingStatusBlinkTimer()
                                        self.imageThingStatus.isHidden = false
                                        self.labelThingStatus.isHidden = false
                                        self.isDeviceConnected = true
                                    }
                                }
                            }
                        }
                    }
                    
                } catch let e as NSError{
                    NSLog("\(#fileID):\(#line) >> An error has occured while parsing JSONOBJECT : \(e)")
                }
            }
            
            DispatchQueue.main.async {
                self.startGetDeviceTwinTimer()
            }
            
        }
        task.resume()
        
    }
    
    // MARK: - Update Device Twin (HTTP PATCH)
    func updateDeviceTwin() {
        
        let json: [String:Any] = ["properties":["desired":["fwupdate":nil]]]
        let jsonData = try? JSONSerialization.data(withJSONObject: json, options: [])
        
        let url = URL(string: "https://"+self.iothubName+".azure-devices.net/twins/"+self.thingName!+"?api-version=2018-06-30")
        
        var request = URLRequest(url: url!)
        request.httpMethod = "PATCH"
        request.httpBody = jsonData
        
        let sasToken = self.generateSasToken(resourceUri: self.iothubName+".azure-devices.net", key: self.sharedAccessKey, policyName: "iothubowner", expiryInSeconds: 3600)
        
        request.setValue(sasToken!, forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        
        let task = URLSession.shared.dataTask(with: request) { (data, response, error) in
            
            if let e = error {
                NSLog("\(#fileID):\(#line) >> patch : An error has occured : \(e)")
                return
            }
            
            DispatchQueue.main.async {
                
                do {
                    
                    let object = try JSONSerialization.jsonObject(with: data!, options: []) as? NSDictionary
                    
                    NSLog("\(#fileID):\(#line) >> patch :  \(String(describing: object))")
                    
                    if (String(describing: object).contains("404103")) {
                        
                    } else {
                        guard let jsonObject = object else { return }
                        
                        let status = jsonObject["status"] as? Int
                        let payload = jsonObject["payload"] as? String
                        
                        if (status != nil) {
                            
                            
                            NSLog("\(#fileID):\(#line) >> patch : status = \(String(describing: status!))")
                            NSLog("\(#fileID):\(#line) >> patch : payload = \(String(describing: payload!))")
                            
                        }
                    }
                    
                    
                } catch let e as NSError{
                    NSLog("\(#fileID):\(#line) >> An error has occured while parsing JSONOBJECT : \(e)")
                }
            }
        }
        task.resume()
    }
    
    
    //MARK: - Get Device Twin (HTTP GET)
    
    func getDeviceTwin() {
        
        let url = URL(string: "https://"+self.iothubName+".azure-devices.net/twins/"+self.thingName!+"?api-version=2020-05-31-preview")
        var request = URLRequest.init(url: url!)
        request.httpMethod = "GET"
        
        let sasToken = self.generateSasToken(resourceUri: self.iothubName+".azure-devices.net", key: self.sharedAccessKey, policyName: "iothubowner", expiryInSeconds: 3600)
        
        NSLog("\(#fileID):\(#line) >> sasToken = \(String(describing: sasToken!))")
        request.setValue(sasToken!, forHTTPHeaderField: "Authorization")
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        
        URLSession.shared.dataTask(with: request) { (data, response, error) in
            guard let data = data else { return }
            
            if error == nil {
                
                DispatchQueue.main.async {
                    
                    if (self.isServerConnected == false) {
                        self.serverStatus = "Connected"
                        self.isServerConnected = true
                        NSLog("\(#fileID):\(#line) >> \(self.serverStatus)")
                        self.stopConnectServerTimer()
                        self.labelServerStatus.text = self.serverStatus
                        self.labelServerStatus.textColor = UIColor.green
                        self.imageServerStatus.tintColor = .green
                        self.stopServerStatusBlinkTimer()
                        self.imageServerStatus.isHidden = false
                        self.labelServerStatus.isHidden = false
                        
                        self.labelDirectMethodName.text = self.APP_DIRECT_METHOD_NAME
                        self.labelDirectMethodPayload.text = self.APP_CONNECT_MESSAGE
                    }
                    
                    if (self.isDeviceConnected == false) {
                        self.isProgressing = true
                        self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.APP_CONNECT_MESSAGE)
                        self.startConnectThingTimer()
                        
                        self.thingStatus = "Connecting..."
                        self.labelThingStatus.text = self.thingStatus
                        self.labelThingStatus.textColor = UIColor.orange
                        self.imageThingStatus.tintColor = .orange
                        self.startThingStatusBlinkTimer()
                    }
                    
                    let strJsonString  = String(decoding: data, as: UTF8.self)
                    
                    let oJsonDataT: Data? = strJsonString.data(using: .utf8)
                    if let oJsonData = oJsonDataT {
                        var oJsonDictionaryT:[String:Any]?
                        oJsonDictionaryT = try? JSONSerialization.jsonObject(with: oJsonData, options: []) as? [String:Any]
                        
                        if let oJsonDictionary = oJsonDictionaryT {
                            if let strProperties = oJsonDictionary["properties"] {
                                let jsonData = try! JSONSerialization.data(withJSONObject: strProperties, options: [])
                                let decoded = String(data: jsonData, encoding: .utf8)!
                                let oJsonDataT: Data? = decoded.data(using: .utf8)
                                if let oJsonData = oJsonDataT {
                                    var oJsonDictionaryT:[String:Any]?
                                    oJsonDictionaryT = try? JSONSerialization.jsonObject(with: oJsonData, options: []) as? [String:Any]
                                    if let oJsonDictionary = oJsonDictionaryT {
                                        if let strReported = oJsonDictionary["reported"] {
                                            let jsonDataReported = try! JSONSerialization.data(withJSONObject: strReported, options: [])
                                            let decodedReported = String(data: jsonDataReported, encoding: .utf8)!
                                            let oJsonDataTReported: Data? = decodedReported.data(using: .utf8)
                                            if let oJsonDataReported = oJsonDataTReported {
                                                var oJsonDictionaryTReported:[String:Any]?
                                                oJsonDictionaryTReported = try? JSONSerialization.jsonObject(with: oJsonDataReported, options: []) as? [String:Any]
                                                if let oJsonDictionaryReported = oJsonDictionaryTReported {
                                                    print(" >> ==========================================================")
                                                    print(" >> oJsonDictionaryReported = \(oJsonDictionaryReported)")
                                                    print(" >> ==========================================================")
                                                    if let strMetadata = oJsonDictionaryReported["$metadata"] {
                                                        let jsonDataMetadata = try! JSONSerialization.data(withJSONObject: strMetadata, options: [])
                                                        let decodedMetadata = String(data: jsonDataMetadata, encoding: .utf8)!
                                                        let oJsonDataTMetadata: Data? = decodedMetadata.data(using: .utf8)
                                                        if let oJsonDataMetadata = oJsonDataTMetadata {
                                                            var oJsonDictionaryTMetadata:[String:Any]?
                                                            oJsonDictionaryTMetadata = try? JSONSerialization.jsonObject(with: oJsonDataMetadata, options: []) as? [String:Any]
                                                            if let oJsonDictionaryMetadata = oJsonDictionaryTMetadata {
                                                                
                                                                if let strDoorStateMetadata = oJsonDictionaryMetadata["doorState"] {
                                                                    let jsonDataDoorStateMetadata = try! JSONSerialization.data(withJSONObject: strDoorStateMetadata, options: [])
                                                                    let decodedDoorStateMetadata = String(data: jsonDataDoorStateMetadata, encoding: .utf8)!
                                                                    let oJsonDataDoorStateTMetadata: Data? = decodedDoorStateMetadata.data(using: .utf8)
                                                                    if let oJsonDataDoorStateMetadata = oJsonDataDoorStateTMetadata {
                                                                        var oJsonDictionaryTDoorStateMetadata:[String:Any]?
                                                                        oJsonDictionaryTDoorStateMetadata = try? JSONSerialization.jsonObject(with: oJsonDataDoorStateMetadata, options: []) as? [String:Any]
                                                                        if let oJsonDictionaryDoorStateMetadata = oJsonDictionaryTDoorStateMetadata {
                                                                            if let strDoorStateDate = oJsonDictionaryDoorStateMetadata["$lastUpdated"] {
                                                                                self.curDoorStateDate = String(describing: strDoorStateDate)
                                                                                if (self.pastDoorStateDate != self.curDoorStateDate) {
                                                                                    self.isDoorStateUpdated = true
                                                                                    DispatchQueue.main.async {
                                                                                        self.labelUpdatedTime.text = self.utcToLocal(dateStr: String(describing: self.curDoorStateDate))
                                                                                    }
                                                                                } else {
                                                                                    self.isDoorStateUpdated = false
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                
                                                                if let strDoorStateChangeMetadata = oJsonDictionaryMetadata["doorStateChange"] {
                                                                    let jsonDataDoorStateChangeMetadata = try! JSONSerialization.data(withJSONObject: strDoorStateChangeMetadata, options: [])
                                                                    let decodedDoorStateChangeMetadata = String(data: jsonDataDoorStateChangeMetadata, encoding: .utf8)!
                                                                    let oJsonDataDoorStateChangeTMetadata: Data? = decodedDoorStateChangeMetadata.data(using: .utf8)
                                                                    if let oJsonDataDoorStateChangeMetadata = oJsonDataDoorStateChangeTMetadata {
                                                                        var oJsonDictionaryTDoorStateChangeMetadata:[String:Any]?
                                                                        oJsonDictionaryTDoorStateChangeMetadata = try? JSONSerialization.jsonObject(with: oJsonDataDoorStateChangeMetadata, options: []) as? [String:Any]
                                                                        if let oJsonDictionaryDoorStateChangeMetadata = oJsonDictionaryTDoorStateChangeMetadata {
                                                                            if let strDoorStateChangeDate = oJsonDictionaryDoorStateChangeMetadata["$lastUpdated"] {
                                                                                self.curDoorStateChangeDate = String(describing: strDoorStateChangeDate)
                                                                                if (self.pastDoorStateChangeDate != self.curDoorStateChangeDate) {
                                                                                    self.isDoorStateChangeUpdated = true
                                                                                    DispatchQueue.main.async {
                                                                                        self.labelUpdatedTime.text = self.utcToLocal(dateStr: String(describing: self.curDoorStateChangeDate))
                                                                                    }
                                                                                } else {
                                                                                    self.isDoorStateChangeUpdated = false
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                
                                                                if let strOTAupdateMetadata = oJsonDictionaryMetadata["OTAupdate"] {
                                                                    let jsonDataOTAupdateMetadata = try! JSONSerialization.data(withJSONObject: strOTAupdateMetadata, options: [])
                                                                    let decodedOTAupdateMetadata = String(data: jsonDataOTAupdateMetadata, encoding: .utf8)!
                                                                    let oJsonDataOTAupdateTMetadata: Data? = decodedOTAupdateMetadata.data(using: .utf8)
                                                                    if let oJsonDataOTAupdateMetadata = oJsonDataOTAupdateTMetadata {
                                                                        var oJsonDictionaryTOTAupdateMetadata:[String:Any]?
                                                                        oJsonDictionaryTOTAupdateMetadata = try? JSONSerialization.jsonObject(with: oJsonDataOTAupdateMetadata, options: []) as? [String:Any]
                                                                        if let oJsonDictionaryOTAupdateMetadata = oJsonDictionaryTOTAupdateMetadata {
                                                                            if let strOTAupdateDate = oJsonDictionaryOTAupdateMetadata["$lastUpdated"] {
                                                                                self.curOTAupdateDate = String(describing: strOTAupdateDate)
                                                                                if (self.pastOTAupdateDate != self.curOTAupdateDate) {
                                                                                    self.isOTAupdateUpdated = true
                                                                                    DispatchQueue.main.async {
                                                                                        self.labelUpdatedTime.text = self.utcToLocal(dateStr: String(describing: self.curOTAupdateDate))
                                                                                    }
                                                                                } else {
                                                                                    self.isOTAupdateUpdated = false
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                
                                                                if let strTemperatureMetadata = oJsonDictionaryMetadata["temperature"] {
                                                                    let jsonDataTemperatureMetadata = try! JSONSerialization.data(withJSONObject: strTemperatureMetadata, options: [])
                                                                    let decodedTemperatureMetadata = String(data: jsonDataTemperatureMetadata, encoding: .utf8)!
                                                                    let oJsonDataTemperatureTMetadata: Data? = decodedTemperatureMetadata.data(using: .utf8)
                                                                    if let oJsonDataTemperatureMetadata = oJsonDataTemperatureTMetadata {
                                                                        var oJsonDictionaryTTemperatureMetadata:[String:Any]?
                                                                        oJsonDictionaryTTemperatureMetadata = try? JSONSerialization.jsonObject(with: oJsonDataTemperatureMetadata, options: []) as? [String:Any]
                                                                        if let oJsonDictionaryTemperatureMetadata = oJsonDictionaryTTemperatureMetadata {
                                                                            if let strTemperatureDate = oJsonDictionaryTemperatureMetadata["$lastUpdated"] {
                                                                                self.curTemperatureDate = String(describing: strTemperatureDate)
                                                                                if (self.pastTemperatureDate != self.curTemperatureDate) {
                                                                                    self.isTemperatureUpdated = true
                                                                                    DispatchQueue.main.async {
                                                                                        self.labelUpdatedTime.text = self.utcToLocal(dateStr: String(describing: self.curTemperatureDate))
                                                                                    }
                                                                                } else {
                                                                                    self.isTemperatureUpdated = false
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                
                                                                if let strDoorBellMetadata = oJsonDictionaryMetadata["doorBell"] {
                                                                    let jsonDataDoorBellMetadata = try! JSONSerialization.data(withJSONObject: strDoorBellMetadata, options: [])
                                                                    let decodedDoorBellMetadata = String(data: jsonDataDoorBellMetadata, encoding: .utf8)!
                                                                    let oJsonDataDoorBellTMetadata: Data? = decodedDoorBellMetadata.data(using: .utf8)
                                                                    if let oJsonDataDoorBellMetadata = oJsonDataDoorBellTMetadata {
                                                                        var oJsonDictionaryTDoorBellMetadata:[String:Any]?
                                                                        oJsonDictionaryTDoorBellMetadata = try? JSONSerialization.jsonObject(with: oJsonDataDoorBellMetadata, options: []) as? [String:Any]
                                                                        if let oJsonDictionaryDoorBellMetadata = oJsonDictionaryTDoorBellMetadata {
                                                                            if let strDoorBellDate = oJsonDictionaryDoorBellMetadata["$lastUpdated"] {
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                
                                                                if let strDoorOpenModeMetadata = oJsonDictionaryMetadata["doorOpenMode"] {
                                                                    let jsonDataDoorOpenModeMetadata = try! JSONSerialization.data(withJSONObject: strDoorOpenModeMetadata, options: [])
                                                                    let decodedDoorOpenModeMetadata = String(data: jsonDataDoorOpenModeMetadata, encoding: .utf8)!
                                                                    let oJsonDataDoorOpenModeTMetadata: Data? = decodedDoorOpenModeMetadata.data(using: .utf8)
                                                                    if let oJsonDataDoorOpenModeMetadata = oJsonDataDoorOpenModeTMetadata {
                                                                        var oJsonDictionaryTDoorOpenModeMetadata:[String:Any]?
                                                                        oJsonDictionaryTDoorOpenModeMetadata = try? JSONSerialization.jsonObject(with: oJsonDataDoorOpenModeMetadata, options: []) as? [String:Any]
                                                                        if let oJsonDictionaryDoorOpenModeMetadata = oJsonDictionaryTDoorOpenModeMetadata {
                                                                            if let strDoorOpenModeDate = oJsonDictionaryDoorOpenModeMetadata["$lastUpdated"] {
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                
                                                                if let strBatteryMetadata = oJsonDictionaryMetadata["battery"] {
                                                                    let jsonDataBatteryMetadata = try! JSONSerialization.data(withJSONObject: strBatteryMetadata, options: [])
                                                                    let decodedBatteryMetadata = String(data: jsonDataBatteryMetadata, encoding: .utf8)!
                                                                    let oJsonDataBatteryTMetadata: Data? = decodedBatteryMetadata.data(using: .utf8)
                                                                    if let oJsonDataBatteryMetadata = oJsonDataBatteryTMetadata {
                                                                        var oJsonDictionaryTBatteryMetadata:[String:Any]?
                                                                        oJsonDictionaryTBatteryMetadata = try? JSONSerialization.jsonObject(with: oJsonDataBatteryMetadata, options: []) as? [String:Any]
                                                                        if let oJsonDictionaryBatteryMetadata = oJsonDictionaryTBatteryMetadata {
                                                                            if let strBatteryDate = oJsonDictionaryBatteryMetadata["$lastUpdated"] {
                                                                                self.curBatteryDate = String(describing: strBatteryDate)
                                                                                if (self.pastBatteryDate != self.curBatteryDate) {
                                                                                    self.isBatteryUpdated = true
                                                                                    DispatchQueue.main.async {
                                                                                        self.labelUpdatedTime.text = self.utcToLocal(dateStr: String(describing: self.curBatteryDate))
                                                                                    }
                                                                                } else {
                                                                                    self.isBatteryUpdated = false
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                
                                                                if let strOpenMethodMetadata = oJsonDictionaryMetadata["openMethod"] {
                                                                    let jsonDataOpenMethodMetadata = try! JSONSerialization.data(withJSONObject: strOpenMethodMetadata, options: [])
                                                                    let decodedOpenMethodMetadata = String(data: jsonDataOpenMethodMetadata, encoding: .utf8)!
                                                                    let oJsonDataOpenMethodTMetadata: Data? = decodedOpenMethodMetadata.data(using: .utf8)
                                                                    if let oJsonDataOpenMethodMetadata = oJsonDataOpenMethodTMetadata {
                                                                        var oJsonDictionaryTOpenMethodMetadata:[String:Any]?
                                                                        oJsonDictionaryTOpenMethodMetadata = try? JSONSerialization.jsonObject(with: oJsonDataOpenMethodMetadata, options: []) as? [String:Any]
                                                                        if let oJsonDictionaryOpenMethodMetadata = oJsonDictionaryTOpenMethodMetadata {
                                                                            if let strOpenMethodDate = oJsonDictionaryOpenMethodMetadata["$lastUpdated"] {
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                                
                                                            }
                                                        }
                                                        
                                                    }
                                                    
                                                    if let doorState = oJsonDictionaryReported["doorState"] {
                                                        NSLog("\(#fileID):\(#line) >> doorState = \(doorState)")
                                                        self.doorState = doorState as? UInt
                                                        NSLog("\(#fileID):\(#line) >> self.doorState = \(String(describing: self.doorState))")
                                                        if (self.doorState == 1) {
                                                            self.doorStateFlag = "true"
                                                        } else if (self.doorState == 0) {
                                                            self.doorStateFlag = "false"
                                                        }
                                                        self.defaults.set(self.doorStateFlag, forKey: "doorStateFlagKey")
                                                    }
                                                    
                                                    if let doorStateChange = oJsonDictionaryReported["doorStateChange"] as? UInt {
                                                        NSLog("\(#fileID):\(#line) >> doorStateChange = \(doorStateChange)")
                                                    }
                                                    
                                                    if let doorBell = oJsonDictionaryReported["doorBell"] as? Bool {
                                                        NSLog("\(#fileID):\(#line) >> doorBell = \(doorBell)")
                                                    }
                                                    
                                                    if let temperature = oJsonDictionaryReported["temperature"] as? NSNumber {
                                                        //print(type(of: temperature))
                                                        NSLog("\(#fileID):\(#line) >> temperature = \(temperature.stringValue)")
                                                        self.temperaureValue = temperature
                                                    }
                                                    
                                                    if let openMethod = oJsonDictionaryReported["openMethod"] as? String {
                                                        NSLog("\(#fileID):\(#line) >> openMethod = \(openMethod)")
                                                    }
                                                    if let doorOpenMode = oJsonDictionaryReported["DoorOpenMode"] as? UInt {
                                                        NSLog("\(#fileID):\(#line) >> doorOpenMode = \(doorOpenMode)")
                                                    }
                                                    
                                                    if let OTAresult = oJsonDictionaryReported["OTAresult"] as? String {
                                                        NSLog("\(#fileID):\(#line) >> OTAresult = \(OTAresult)")
                                                        self.OTAresult = OTAresult
                                                    }
                                                    if let battery = oJsonDictionaryReported["battery"] as? UInt {
                                                        NSLog("\(#fileID):\(#line) >> battery = \(battery)")
                                                        self.batteryValue = battery
                                                    }
                                                    if let OTAupdate = oJsonDictionaryReported["OTAupdate"] as? UInt {
                                                        NSLog("\(#fileID):\(#line) >> OTAupdate = \(OTAupdate)")
                                                        self.OTAupdate = OTAupdate
                                                    }
                                                    
                                                    NSLog("\(#fileID):\(#line) >> isDoorStateUpdated = \(self.isDoorStateUpdated)")
                                                    if (self.isDoorStateUpdated == true) {
                                                        NSLog("\(#fileID):\(#line) >> doorState = \(String(describing: self.doorState!))")
                                                        DispatchQueue.main.async {
                                                            if self.doorState == 1 {  //open
                                                                self.doorLockCtl(lock: "true")
                                                            } else if self.doorState == 0 {  //close
                                                                self.doorLockCtl(lock: "false")
                                                            }
                                                        }
                                                    }
                                                    self.pastDoorStateDate = self.curDoorStateDate
                                                    self.isDoorStateUpdated = false
                                                    
                                                    NSLog("\(#fileID):\(#line) >> isTemperatureUpdated = \(self.isTemperatureUpdated)")
                                                    if (self.isTemperatureUpdated == true) {
                                                        DispatchQueue.main.async {
                                                            let numberFormatter = NumberFormatter()
                                                            numberFormatter.roundingMode = .floor
                                                            numberFormatter.minimumSignificantDigits = 3
                                                            numberFormatter.maximumSignificantDigits = 3
                                                            let originalNum = self.temperaureValue!
                                                            let newNum = numberFormatter.string(from: originalNum)
                                                            self.labelTemperature.text = newNum
                                                        }
                                                    }
                                                    self.pastTemperatureDate = self.curTemperatureDate
                                                    self.isTemperatureUpdated = false
                                                    
                                                    NSLog("\(#fileID):\(#line) >> isBatteryUpdated = \(self.isBatteryUpdated)")
                                                    if (self.isBatteryUpdated == true) {
                                                        DispatchQueue.main.async {
                                                            self.labelBattery.text = String(self.batteryValue!)
                                                        }
                                                    }
                                                    self.pastBatteryDate = self.curBatteryDate
                                                    self.isBatteryUpdated = false
                                                    
                                                    NSLog("\(#fileID):\(#line) >> isOTAupdateUpdated = \(self.isOTAupdateUpdated)")
                                                    if (self.isOTAupdateUpdated == true) {
                                                        if (self.OTAupdate == 0) {  //none
                                                            self.existOTAupdateFlag = false
                                                            self.OTAupdateProgressFlage = false
                                                            if (self.OTAresult == "OTA_OK") {
                                                                DispatchQueue.main.async {
                                                                    self.removeBadge()
                                                                }
                                                                DispatchQueue.main.async {
                                                                    self.dismissUpdatingDialog()
                                                                    self.showUpdateSuccessDialog()
                                                                }
                                                                
                                                            }
                                                            else if (self.OTAresult == "OTA_UNKNOWN") {
                                                                DispatchQueue.main.async {
                                                                    self.removeBadge()
                                                                }
                                                                DispatchQueue.main.async {
                                                                    self.dismissUpdatingDialog()
                                                                }
                                                            }
                                                            
                                                        } else if (self.OTAupdate == 1) {  //exist update
                                                            self.existOTAupdateFlag = true
                                                            self.OTAupdateProgressFlage = false
                                                            DispatchQueue.main.async {
                                                                self.showBadge(withString: "New")
                                                            }
                                                            //[[OTAupdate=1 when "OTA_NG"
                                                            if (self.OTAresult == "OTA_NG") {
                                                                DispatchQueue.main.async {
                                                                    self.dismissUpdatingDialog()
                                                                    self.showUpdateFailDialog()
                                                                }
                                                            }
                                                            //]]
                                                            
                                                        } else if (self.OTAupdate == 2) {  //update progressing
                                                            self.existOTAupdateFlag = false
                                                            self.OTAupdateProgressFlage = true
                                                            DispatchQueue.main.async {
                                                                self.removeBadge()
                                                            }
                                                            
                                                        }
                                                    }
                                                    self.pastOTAupdateDate = self.curOTAupdateDate
                                                    self.isOTAupdateUpdated = false
                                                    self.defaults.set(self.existOTAupdateFlag, forKey: "existOTAupdateFlagKey")
                                                    self.defaults.set(self.OTAupdateProgressFlage, forKey: "OTAupdateProgressFlageKey")
                                                    
                                                }
                                            }
                                            
                                        }
                                    }
                                }
                                
                            }
                        }
                    }
                }
            } else {
                
                DispatchQueue.main.async {
                    self.serverStatus = "Disconnected"
                    self.isServerConnected = false
                    NSLog("\(#fileID):\(#line) >> \(self.serverStatus)")
                    //self.activityIndicatorView.stopAnimating()
                    self.stopConnectServerTimer()
                    self.stopServerStatusBlinkTimer()
                    
                    self.labelServerStatus.text = self.serverStatus
                    self.labelServerStatus.textColor = UIColor.red
                    self.imageServerStatus.tintColor = .red
                    
                    self.stopGetDeviceTwinTimer()
                }
            }
            
        }.resume()
    }
    
    func convertStringToDate(dateString: String) -> Date? {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        dateFormatter.timeZone = TimeZone(identifier: "UTC")
        let date = dateFormatter.date(from: dateString)
        return date
    }
    
    // MARK: - Send C2D Messges
    @objc func sendC2Dmessage(messageString: String) {
        
        let messageHandle: IOTHUB_MESSAGE_HANDLE = IoTHubMessage_CreateFromByteArray(messageString, messageString.utf8.count)
        
        if (messageHandle != OpaquePointer.init(bitPattern: 0)) {
            let that = UnsafeMutableRawPointer(Unmanaged.passUnretained(self).toOpaque())
            if (IOTHUB_CLIENT_OK == IoTHubClient_LL_SendEventAsync(iothubClientHandle, messageHandle, mySendConfirmationCallback, that)) {
                
            }
        }
        
    }
    
    @objc func dowork() {
        IoTHubClient_LL_DoWork(iothubClientHandle)
    }
    
    let mySendConfirmationCallback: IOTHUB_CLIENT_EVENT_CONFIRMATION_CALLBACK = { result, userContext in
        
        var mySelf: ViewController = Unmanaged<ViewController>.fromOpaque(userContext!).takeUnretainedValue()
        
        if (result == IOTHUB_CLIENT_CONFIRMATION_OK) {
            NSLog("\(#fileID):\(#line) >> success")
        } else {
            NSLog("\(#fileID):\(#line) >> fail")
        }
    }
    
    let myReceiveMessageCallback: IOTHUB_CLIENT_MESSAGE_CALLBACK_ASYNC = { message, userContext in
        
        var mySelf: ViewController = Unmanaged<ViewController>.fromOpaque(userContext!).takeUnretainedValue()
        
        var messageId: String!
        var correlationId: String!
        var size: Int = 0
        var buff: UnsafePointer<UInt8>?
        var messageString: String = ""
        
        messageId = String(describing: IoTHubMessage_GetMessageId(message))
        correlationId = String(describing: IoTHubMessage_GetCorrelationId(message))
        
        if (messageId == nil) {
            messageId = "<nil>"
        }
        
        if (correlationId == nil) {
            messageId = "<nil>"
        }
        
        var rc: IOTHUB_MESSAGE_RESULT = IoTHubMessage_GetByteArray(message, &buff, &size)
        
        if rc == IOTHUB_MESSAGE_OK {
            for i in 0 ..< size {
                let out = String(buff![i], radix: 16)
                print("0x" + out, terminator: " ")
            }
            
            let data = Data(bytes: buff!, count: size)
            messageString = String.init(data: data, encoding: String.Encoding.utf8)!
            
            NSLog("\(#fileID):\(#line) >> Message Id : ", messageId, " Correlation Id : ", correlationId)
            NSLog("\(#fileID):\(#line) >> Message : ", messageString)
        } else {
            NSLog("\(#fileID):\(#line) >> Failed to acquire message data")
        }
        return IOTHUBMESSAGE_ACCEPTED
        
    }
    
    // MARK: - Generate SAS Token
    
    func generateSasToken(resourceUri: String, key: String, policyName: String = "", expiryInSeconds: Int = 3600) -> String? {
        let fromEpochStart = Date().timeIntervalSince1970
        let expiry = Int(fromEpochStart) + expiryInSeconds
        var sasToken: String?
        
        guard let encodedResourceUri = resourceUri.addingPercentEncoding(withAllowedCharacters: .urlHostAllowed) else { return nil }
        let stringToSign = "\(encodedResourceUri)\n\(String(expiry))"
        
        guard let stringToSignData = stringToSign.data(using: .utf8) else { return nil }
        
        guard let keyData = Data(base64Encoded: key) else { return nil }
        if #available(iOS 13.0, *) {
            let symmetricKey = SymmetricKey(data: keyData)
            let hmac = HMAC<SHA256>.authenticationCode(for: stringToSignData, using: symmetricKey)
            guard let hexString = hmac.description.split(separator: " ", maxSplits: .max, omittingEmptySubsequences: true).last?.description else { return nil }
            let hex = hexString.hex
            let unreserved = "-._~&"
            let allowed = NSMutableCharacterSet.alphanumeric()
            allowed.addCharacters(in: unreserved)
            guard let signature = hex?.base64EncodedString().addingPercentEncoding(withAllowedCharacters: allowed as CharacterSet) else { return nil }
            
            let SasUrl = "sr=\(resourceUri)&sig=\(signature)&se=\(String(expiry))&skn=\(policyName)"
            sasToken = "SharedAccessSignature " + SasUrl
            
        } else {
            // Fallback on earlier versions
        }
        return sasToken
    }
    
    
    // MARK: - getDeviceTwin Timer
    func startGetDeviceTwinTimer() {
        NSLog("\(#fileID):\(#line) >> >> startGetDeviceTwinTimer()")
        if let timer = getDeviceTwinTimer {
            if !timer.isValid {
                getDeviceTwinTimer = Timer.scheduledTimer(timeInterval: 3.0, target: self, selector: #selector(funcGetDeviceTwin), userInfo: nil, repeats: true)
            }
        } else {
            getDeviceTwinTimer = Timer.scheduledTimer(timeInterval: 3.0, target: self, selector: #selector(funcGetDeviceTwin), userInfo: nil, repeats: true)
        }
    }
    
    @objc func funcGetDeviceTwin() {
        self.getDeviceTwin()
    }
    
    func stopGetDeviceTwinTimer() {
        NSLog("\(#fileID):\(#line) >> stopGetDeviceTwinTimer()")
        if let timer = getDeviceTwinTimer {
            if (timer.isValid) {
                timer.invalidate()
                getDeviceTwinTimer = nil
            }
        }
    }
    
    // MARK: - AlerDialog
    
    func showUpdatingDialog() {
        let alertTitle = "Device Firmware Updating"
        
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
            
            The device's firmware is being updated...
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
        
        self.updatingAlert = UIAlertController.init(title: nil, message: nil, preferredStyle: UIAlertController.Style.alert)
        self.updatingAlert!.setValue(attributedTitleText, forKey: "attributedTitle")
        self.updatingAlert!.setValue(attributedMessageText, forKey: "attributedMessage")
        
        present(self.updatingAlert!, animated: true, completion: nil)
    }
    
    func dismissUpdatingDialog() {
        self.updatingAlert?.dismiss(animated: true, completion: nil)
    }
    
    func showUpdateSuccessDialog() {
        
        let alertMessage = """
            
            The device's firmware update was successful
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
        
        self.updateSuccessAlert = UIAlertController.init(title: nil, message: nil, preferredStyle: UIAlertController.Style.alert)
        self.updateSuccessAlert!.setValue(attributedMessageText, forKey: "attributedMessage")
        
        let okAction = UIAlertAction(title: "OK", style: .default, handler: { (action) -> Void in
            self.isProgressing = false
            self.updateDeviceTwin()
        })
        self.updateSuccessAlert!.addAction(okAction)
        present(self.updateSuccessAlert!, animated: true, completion: nil)
    }
    
    func dismissUpdateSuccessDialog() {
        self.updateSuccessAlert!.dismiss(animated: true, completion: nil)
    }
    
    func showUpdateFailDialog() {
        
        let alertMessage = """
            
            The device's firmware update was failed
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
        
        self.updateFailAlert = UIAlertController.init(title: nil, message: nil, preferredStyle: UIAlertController.Style.alert)
        self.updateFailAlert!.setValue(attributedMessageText, forKey: "attributedMessage")
        
        let okAction = UIAlertAction(title: "OK", style: .default, handler: { (action) -> Void in
            self.isProgressing = false
        })
        self.updateFailAlert!.addAction(okAction)
        present(self.updateFailAlert!, animated: true, completion: nil)
    }
    
    func dismissUpdateFailDialog() {
        self.updateFailAlert?.dismiss(animated: true, completion: nil)
    }
    
    // MARK: - Connect Server Timeout Dialog
    
    func startConnectServerTimer() {
        NSLog("\(#fileID):\(#line) >> startConnectServerTimer()")
        if let timer = connectServerTimer {
            if !timer.isValid {
                connectServerTimer = Timer.scheduledTimer(timeInterval: 20.0, target: self, selector: #selector(showConnectServerTimeoutDialog), userInfo: nil, repeats: true)
            }
        } else {
            connectServerTimer = Timer.scheduledTimer(timeInterval: 20.0, target: self, selector: #selector(showConnectServerTimeoutDialog), userInfo: nil, repeats: true)
        }
    }
    
    @objc func showConnectServerTimeoutDialog() {
        let alertMessage = """
            
            Failed to connect to AWS server.
            Please check your internet status.
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
        
        self.updateSuccessAlert = UIAlertController.init(title: nil, message: nil, preferredStyle: UIAlertController.Style.alert)
        self.updateSuccessAlert!.setValue(attributedMessageText, forKey: "attributedMessage")
        
        let okAction = UIAlertAction(title: "OK", style: .default, handler: { (action) -> Void in
            self.stopConnectServerTimer()
            let mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "MainViewController")
            mainVC.modalPresentationStyle = .fullScreen
            self.present(mainVC, animated: true, completion: nil)
        })
        self.updateSuccessAlert!.addAction(okAction)
        present(self.updateSuccessAlert!, animated: true, completion: nil)
    }
    
    func stopConnectServerTimer() {
        NSLog("\(#fileID):\(#line) >> stopConnectServerTimer()")
        if let timer = connectServerTimer {
            if (timer.isValid) {
                timer.invalidate()
                connectServerTimer = nil
            }
        }
    }
    
    // MARK: - Connect Thing Timeout Dialog
    
    func startConnectThingTimer() {
        NSLog("\(#fileID):\(#line) >> startConnectThingTimer()")
        if let timer = connectThingTimer {
            if !timer.isValid {
                connectThingTimer = Timer.scheduledTimer(timeInterval: 20.0, target: self, selector: #selector(showConnectThingTimeoutDialog), userInfo: nil, repeats: true)
            }
        } else {
            connectThingTimer = Timer.scheduledTimer(timeInterval: 20.0, target: self, selector: #selector(showConnectThingTimeoutDialog), userInfo: nil, repeats: true)
        }
    }
    
    @objc func showConnectThingTimeoutDialog() {
        let alertMessage = """
            
            No response was received from the device.
            Would you like to check again?
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
        
        self.connectThingTimeoutAlert = UIAlertController.init(title: nil, message: nil, preferredStyle: UIAlertController.Style.alert)
        self.connectThingTimeoutAlert!.setValue(attributedMessageText, forKey: "attributedMessage")
        
        let cancel = UIAlertAction(title: "Cancel", style: .destructive, handler: { (action) -> Void in
            self.stopConnectThingTimer()
            self.isProgressing = false
            Utility.hideLoader(view: self.view)
        })
        self.connectThingTimeoutAlert!.addAction(cancel)
        
        let okAction = UIAlertAction(title: "OK", style: .default, handler: { (action) -> Void in
            self.stopConnectThingTimer()
            Utility.hideLoader(view: self.view)
            self.labelDirectMethodName.text = self.APP_DIRECT_METHOD_NAME
            self.labelDirectMethodPayload.text = self.APP_CONNECT_MESSAGE
            self.isProgressing = true
            self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.APP_CONNECT_MESSAGE)
            self.startConnectThingTimer()
        })
        self.connectThingTimeoutAlert!.addAction(okAction)
        
        present(self.connectThingTimeoutAlert!, animated: true, completion: nil)
    }
    
    func stopConnectThingTimer() {
        NSLog("\(#fileID):\(#line) >> stopConnectThingTimer()")
        if let timer = connectThingTimer {
            if (timer.isValid) {
                timer.invalidate()
                connectThingTimer = nil
            }
        }
    }
    
    // MARK: - Device offline Dialog
    
    @objc func showDeviceOfflineDialog() {
        
        let alertMessage = """
            
            The device does not respond.
            Would you like to try again?
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
        
        self.deviceOfflineAlert = UIAlertController.init(title: nil, message: nil, preferredStyle: UIAlertController.Style.alert)
        self.deviceOfflineAlert!.setValue(attributedMessageText, forKey: "attributedMessage")
        
        let okAction = UIAlertAction(title: "OK", style: .default, handler: { (action) -> Void in
            Utility.hideLoader(view: self.view)
            if self.pressedButton == "doorClose" {  //opened
                self.labelDirectMethodName.text = self.APP_DIRECT_METHOD_NAME
                
                self.isProgressing = true
                self.pressedButton = "doorClose"
                
                if (self.mode == 20) {  //General Azure IoT
                    self.labelDirectMethodPayload.text = self.APP_CONTROL_DOOR_CLOSE_MESSAGE
                    self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.APP_CONTROL_DOOR_CLOSE_MESSAGE)
                } else if (self.mode == 21) {  //AT-CMD Azure IoT
                    self.labelDirectMethodPayload.text = self.ATCMD_APP_CONTROL_DOOR_CLOSE_MESSAGE
                    self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.ATCMD_APP_CONTROL_DOOR_CLOSE_MESSAGE)
                }
                //
                self.startControlDoorTimer()
                Utility.showLoader(message: "The door is being closed...", view: self.view)
            } else if self.pressedButton == "doorOpen" {  //closed
                self.labelDirectMethodName.text = self.APP_DIRECT_METHOD_NAME
                
                self.isProgressing = true
                self.pressedButton = "doorOpen"
                
                if (self.mode == 20) {  //General Azure IoT
                    self.labelDirectMethodPayload.text = self.APP_CONTROL_DOOR_OPEN_MESSAGE
                    self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.APP_CONTROL_DOOR_OPEN_MESSAGE)
                } else if (self.mode == 21) {  //AT-CMD Azure IoT
                    self.labelDirectMethodPayload.text = self.ATCMD_APP_CONTROL_DOOR_OPEN_MESSAGE
                    self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.ATCMD_APP_CONTROL_DOOR_OPEN_MESSAGE)
                }
                //
                self.startControlDoorTimer()
                Utility.showLoader(message: "The door is being opened...", view: self.view)
            } else if self.pressedButton == "update" {  //update
                self.labelDirectMethodName.text = self.APP_DIRECT_METHOD_NAME
                
                self.isProgressing = true
                self.pressedButton = "update"
                
                if (self.mode == 20) {  //General Azure IoT
                    self.labelDirectMethodPayload.text = self.APP_UPDATE_SHADOW_MESSAGE
                    self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.APP_UPDATE_SHADOW_MESSAGE)
                } else if (self.mode == 21) {  //AT-CMD Azure IoT
                    self.labelDirectMethodPayload.text = self.ATCMD_APP_UPDATE_SHADOW_MESSAGE
                    self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.ATCMD_APP_UPDATE_SHADOW_MESSAGE)
                }
                //
                self.startUpdateDeviceTwinTimer()
                Utility.showLoader(message: "Updating status...", view: self.view)
            }
        })
        self.deviceOfflineAlert!.addAction(okAction)
        
        present(self.deviceOfflineAlert!, animated: true, completion: nil)
    }
    
    
    
    // MARK: - Control Door Timeout Dialog
    
    func startControlDoorTimer() {
        NSLog("\(#fileID):\(#line) >> startControlDoorTimer()")
        if let timer = controlDoorTimer {
            if !timer.isValid {
                controlDoorTimer = Timer.scheduledTimer(timeInterval: 20.0, target: self, selector: #selector(showControlDoorTimeoutDialog), userInfo: nil, repeats: true)
            }
        } else {
            controlDoorTimer = Timer.scheduledTimer(timeInterval: 20.0, target: self, selector: #selector(showControlDoorTimeoutDialog), userInfo: nil, repeats: true)
        }
    }
    
    @objc func showControlDoorTimeoutDialog() {
        let alertMessage = """
            
            No response was received from the door.
            Would you like to try again?
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
        
        self.controlDoorTimeoutAlert = UIAlertController.init(title: nil, message: nil, preferredStyle: UIAlertController.Style.alert)
        self.controlDoorTimeoutAlert!.setValue(attributedMessageText, forKey: "attributedMessage")
        
        let cancel = UIAlertAction(title: "Cancel", style: .destructive, handler: { (action) -> Void in
            self.isProgressing = false
            self.stopControlDoorTimer()
            Utility.hideLoader(view: self.view)
        })
        self.controlDoorTimeoutAlert!.addAction(cancel)
        
        let okAction = UIAlertAction(title: "OK", style: .default, handler: { (action) -> Void in
            Utility.hideLoader(view: self.view)
            if self.doorStateFlag == "true" {  //opened
                self.labelDirectMethodName.text = self.APP_DIRECT_METHOD_NAME
                
                self.isProgressing = true
                
                if (self.mode == 20) {  //General Azure IoT
                    self.labelDirectMethodPayload.text = self.APP_CONTROL_DOOR_CLOSE_MESSAGE
                    self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.APP_CONTROL_DOOR_CLOSE_MESSAGE)
                } else if (self.mode == 21) {  //AT-CMD Azure IoT
                    self.labelDirectMethodPayload.text = self.ATCMD_APP_CONTROL_DOOR_CLOSE_MESSAGE
                    self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.ATCMD_APP_CONTROL_DOOR_CLOSE_MESSAGE)
                }
                
                self.startControlDoorTimer()
                Utility.showLoader(message: "The door is being closed...", view: self.view)
            } else if self.doorStateFlag == "false" {  //closed
                self.labelDirectMethodName.text = self.APP_DIRECT_METHOD_NAME
                
                self.isProgressing = true
                
                if (self.mode == 20) {  //General Azure IoT
                    self.labelDirectMethodPayload.text = self.APP_CONTROL_DOOR_OPEN_MESSAGE
                    self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.APP_CONTROL_DOOR_OPEN_MESSAGE)
                } else if (self.mode == 21) {  //AT-CMD Azure IoT
                    self.labelDirectMethodPayload.text = self.ATCMD_APP_CONTROL_DOOR_OPEN_MESSAGE
                    self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.ATCMD_APP_CONTROL_DOOR_OPEN_MESSAGE)
                }
                
                self.startControlDoorTimer()
                Utility.showLoader(message: "The door is being opened...", view: self.view)
            }
        })
        self.controlDoorTimeoutAlert!.addAction(okAction)
        
        
        present(self.controlDoorTimeoutAlert!, animated: true, completion: nil)
    }
    
    func stopControlDoorTimer() {
        NSLog("\(#fileID):\(#line) >> stopControlDoorTimer()")
        if let timer = controlDoorTimer {
            if (timer.isValid) {
                timer.invalidate()
                controlDoorTimer = nil
            }
        }
    }
    
    // MARK: - Updae Shadow Timeout Dialog
    
    func startUpdateDeviceTwinTimer() {
        NSLog("\(#fileID):\(#line) >> startUpdateDeviceTwinTimer()")
        if let timer = updateDeviceTwinTimer {
            if !timer.isValid {
                updateDeviceTwinTimer = Timer.scheduledTimer(timeInterval: 20.0, target: self, selector: #selector(showUpdateDeviceTwinTimeoutDialog), userInfo: nil, repeats: true)
            }
        } else {
            updateDeviceTwinTimer = Timer.scheduledTimer(timeInterval: 20.0, target: self, selector: #selector(showUpdateDeviceTwinTimeoutDialog), userInfo: nil, repeats: true)
        }
    }
    
    @objc func showUpdateDeviceTwinTimeoutDialog() {
        let alertMessage = """
            
            Failed to update DeviceTwin.
            Would you like to try again?
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
        
        self.updateDeviceTwinTimeoutAlert = UIAlertController.init(title: nil, message: nil, preferredStyle: UIAlertController.Style.alert)
        self.updateDeviceTwinTimeoutAlert!.setValue(attributedMessageText, forKey: "attributedMessage")
        
        let cancel = UIAlertAction(title: "Cancel", style: .destructive, handler: { (action) -> Void in
            self.stopUpdateDeviceTwinTimer()
            Utility.hideLoader(view: self.view)
        })
        self.updateDeviceTwinTimeoutAlert!.addAction(cancel)
        
        let okAction = UIAlertAction(title: "OK", style: .default, handler: { (action) -> Void in
            Utility.showLoader(message: "Updating status...", view: self.view)
            self.labelDirectMethodName.text = self.APP_DIRECT_METHOD_NAME
            self.stopUpdateDeviceTwinTimer()
            self.isProgressing = true
            self.pressedButton = "update"
            
            if (self.mode == 20) {  //General Azure IoT
                self.labelDirectMethodPayload.text = self.APP_UPDATE_SHADOW_MESSAGE
                self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.APP_UPDATE_SHADOW_MESSAGE)
            } else if (self.mode == 21) {  //AT-CMD Azure IoT
                self.labelDirectMethodPayload.text = self.ATCMD_APP_UPDATE_SHADOW_MESSAGE
                self.sendMsg(methodName: self.APP_DIRECT_METHOD_NAME, message: self.ATCMD_APP_UPDATE_SHADOW_MESSAGE)
            }
            
            self.startUpdateDeviceTwinTimer()
        })
        self.updateDeviceTwinTimeoutAlert!.addAction(okAction)
        
        
        present(self.updateDeviceTwinTimeoutAlert!, animated: true, completion: nil)
    }
    
    func stopUpdateDeviceTwinTimer() {
        NSLog("\(#fileID):\(#line) >> stopUpdateDeviceTwinTimer()")
        if let timer = updateDeviceTwinTimer {
            if (timer.isValid) {
                timer.invalidate()
                updateDeviceTwinTimer = nil
            }
        }
    }
    
    func utcToLocal(dateStr: String) -> String? {
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'"
        dateFormatter.timeZone = TimeZone(abbreviation: "UTC")
        
        if let date = dateFormatter.date(from: dateStr) {
            dateFormatter.timeZone = TimeZone.current
            dateFormatter.dateFormat = "yyyy-MM-dd aaa hh:mm:ss"
            return dateFormatter.string(from: date)
        }
        return nil
    }
    
    // MARK: - Toast
    func showToast(message : String, seconds: Double) {
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        alert.view.backgroundColor = .lightGray
        alert.view.alpha = 0.5
        alert.view.layer.cornerRadius = 15
        self.present(alert, animated: true)
        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + seconds) {
            alert.dismiss(animated: true)
        }
    }
    
}

extension String {
    var hex: Data? {
        var value = self
        var data = Data()
        
        while value.count > 0 {
            let subIndex = value.index(value.startIndex, offsetBy: 2)
            let c = String(value[..<subIndex])
            value = String(value[subIndex...])
            
            var char: UInt8
            if #available(iOS 13.0, *) {
                guard let int = Scanner(string: c).scanInt32(representation: .hexadecimal) else { return nil }
                char = UInt8(int)
            } else {
                var int: UInt32 = 0
                Scanner(string: c).scanHexInt32(&int)
                char = UInt8(int)
            }
            
            data.append(&char, count: 1)
        }
        return data
    }
}
