//
//  AWSIoTDeviceViewController.swift
//  WiFiProvisioning
//
//  Created by livekim on 2021/01/15.
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
import AWSIoT
import AWSMobileClient

class AWSIoTDeviceViewController: UIViewController, UITextViewDelegate {
    
    let appDelegate = UIApplication.shared.delegate as! AppDelegate
    let defaults = UserDefaults.standard
    var thingName: String?
    var IDENTITY_POOL_ID: String?
    
    let APP_PUBLISH_TOPIC = "AppControl"
    let APP_SUBSCRIBE_CONNECT_TOPIC = "DeviceConnect"
    let APP_SUBSCRIBE_TOPIC = "DeviceControl"
    
    let APP_CONNECT_MESSAGE = "connected"
    let DEVICE_CONNECT_RESPONSE_MESSAGE = "yes"
    
    let APP_CONTROL_DOOR_OPEN_MESSAGE = "0 app_door open"
    let DEVICE_CONTROL_OPEN_RESPONSE_MESSAGE = "opened"
    
    let APP_CONTROL_DOOR_CLOSE_MESSAGE = "0 app_door close"
    let DEVICE_CONTROL_CLOSE_RESPONSE_MESSAGE = "closed"
    
    let APP_CONTROL_WINDOW_OPEN_MESSAGE = "2 app_window open"
    let APP_CONTROL_WINDOW_CLOSE_MESSAGE = "2 app_window close"
    
    let APP_UPDATE_SHADOW_MESSAGE = "8 app_shadow update"
    let DEVICE_UPDATE_SHADOW_RESPONSE_MESSAGE = "updated"
    
    let APP_CONTROL_OTA_MESSAGE = "confirmOTA"
    
    let IOT_CERT = "IoT Cert"
    //@objc var connected = false
    @objc var iotDataManager: AWSIoTDataManager!
    @objc var iotManager: AWSIoTManager!
    @objc var iot: AWSIoT!
    
    @objc var mqttStatus: String = "Disconnected"
    @objc var thingStatus: String = "Disconnected"
    
    @objc var publishTopic: String?
    @objc var subscribeTopic: String?
    
    @IBOutlet weak var label_thingName: UILabel!
    @IBOutlet weak var viewConnectStatus: UIView!
    @IBOutlet weak var imageServerStatus: UIImageView!
    @IBOutlet weak var labelServerStatus: UILabel!
    @IBOutlet weak var imageThingStatus: UIImageView!
    @IBOutlet weak var labelThingStatus: UILabel!
    
    @IBOutlet weak var viewMQTT: UIView!
    @IBOutlet weak var labelPublishTopic: UILabel!
    @IBOutlet weak var labelPublishMessage: UILabel!
    @IBOutlet weak var labelSubscribeTopic: UILabel!
    @IBOutlet weak var labelSubscribeMessage: UILabel!
    
    @IBOutlet weak var textviewShadow: UITextView!
    @IBOutlet weak var labelUpdatedTime: UILabel!
    
    @IBOutlet weak var btnUpdateShadow: UIButton!
    @IBOutlet weak var btnDoorControl: UIButton!
    @IBOutlet weak var btnWindowControl: UIButton!
    @IBOutlet weak var labelDoorStatus: UILabel!
    @IBOutlet weak var labelWindowStatus: UILabel!
    
    @IBOutlet weak var btnOtaUpdate: UIButton!
    
    var serverStatusBlinkTimer: Timer?
    var thingStatusBlinkTimer: Timer?
    var getShadowTimer: Timer?
    var connectServerTimer: Timer?
    var connectThingTimer: Timer?
    var updateShadowTimer: Timer?
    var controlDoorTimer: Timer?
    var controlWindowTimer: Timer?
    
    var doorStateFlag: String?
    var windowStateFlag: String?
    var existOTAupdateFlag: Bool = false
    var OTAupdateProgressFlage: Bool = false
    
    var isServerConnected: Bool = false
    var isDeviceConnected: Bool = false
    var isDoorStateUpdated: Bool = true
    var isWindowStateUpdated: Bool = true
    var isOtaUpdated: Bool = true
    
    var pastDoorStateTimestamp: UInt = 0
    var pastWindowStateTimestamp: UInt = 0
    var pastOtaTimestamp: UInt = 0
    
    var doorStat: String?
    var windowStat: String?
    var OTAupdate: UInt?
    var OTAresult: String?
    
    var updatingAlert: UIAlertController?
    var updateSuccessAlert: UIAlertController?
    var updateFailAlert: UIAlertController?
    var connectServerTimeoutAlert: UIAlertController?
    var connectThingTimeoutAlert: UIAlertController?
    var updateShadowTimeoutAlert: UIAlertController?
    var controlDoorTimeoutAlert: UIAlertController?
    var controlWindowTimeoutAlert: UIAlertController?
    
    func mqttEventCallback( _ status: AWSIoTMQTTStatus ) {
        DispatchQueue.main.async {
            
            NSLog("\(#fileID):\(#line) >> connection status = \(status.rawValue)")
            
            switch status {
            case .connecting:
                self.mqttStatus = "Connecting..."
                self.isServerConnected = false
                NSLog("\(#fileID):\(#line) >> \(self.mqttStatus)")
                self.labelServerStatus.text = self.mqttStatus
                self.labelServerStatus.textColor = UIColor.orange
                self.imageServerStatus.tintColor = .orange
                self.startServerStatusBlinkTimer()
            case .connected:
                self.mqttStatus = "Connected"
                self.isServerConnected = true
                NSLog("\(#fileID):\(#line) >> \(self.mqttStatus)")
                self.stopConnectServerTimer()
                self.labelServerStatus.text = self.mqttStatus
                self.labelServerStatus.textColor = UIColor.green
                self.imageServerStatus.tintColor = .green
                self.stopServerStatusBlinkTimer()
                self.imageServerStatus.isHidden = false
                self.labelServerStatus.isHidden = false
                
                self.isServerConnected = true
                
                let uuid = UUID().uuidString;
                let defaults = UserDefaults.standard
                let certificateId = defaults.string( forKey: "certificateId")
                NSLog("\(#fileID):\(#line) >> Using certificate:\n\(certificateId!)\n\n\nClient ID:\n\(uuid)")
                
                self.publishTopic! = self.thingName!+"/"+self.APP_PUBLISH_TOPIC
                self.labelPublishTopic.text = self.publishTopic!
                self.labelPublishMessage.text = self.APP_CONNECT_MESSAGE
                
                self.iotDataManager.publishString(self.APP_CONNECT_MESSAGE, onTopic:self.publishTopic!, qoS:.messageDeliveryAttemptedAtLeastOnce)
                self.startConnectThingTimer()
                
                self.thingStatus = "Connecting..."
                self.labelThingStatus.text = self.thingStatus
                self.labelThingStatus.textColor = UIColor.orange
                self.imageThingStatus.tintColor = .orange
                self.startThingStatusBlinkTimer()
                
                let subTopic1 = self.thingName!+"/"+self.APP_SUBSCRIBE_CONNECT_TOPIC
                let subTopic2 = self.thingName!+"/"+self.APP_SUBSCRIBE_TOPIC
                
                OperationQueue().addOperation {
                    DispatchQueue.main.async {
                        
                        self.iotDataManager.subscribe(toTopic: subTopic1, qoS: .messageDeliveryAttemptedAtLeastOnce, messageCallback: {
                            (payload) ->Void in
                            let subscribeValue = NSString(data: payload, encoding: String.Encoding.utf8.rawValue)!
                            
                            NSLog("\(#fileID):\(#line) >> received: \(subscribeValue)")
                            DispatchQueue.main.async {
                                
                                self.labelSubscribeTopic.text = subTopic1
                                self.labelSubscribeMessage.text = String(subscribeValue)
                                
                                if subscribeValue.isEqual(to: self.DEVICE_CONNECT_RESPONSE_MESSAGE) {
                                    self.stopConnectThingTimer()
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
                        } )
                    }
                    
                    DispatchQueue.main.async {
                        
                        self.iotDataManager.subscribe(toTopic: subTopic2, qoS: .messageDeliveryAttemptedAtLeastOnce, messageCallback: {
                            (payload) ->Void in
                            let subscribeValue = NSString(data: payload, encoding: String.Encoding.utf8.rawValue)!
                            
                            NSLog("\(#fileID):\(#line) >> received: \(subscribeValue)")
                            DispatchQueue.main.async {
                                //self.subscribeSlider.value = stringValue.floatValue
                                
                                self.labelSubscribeTopic.text = subTopic2
                                self.labelSubscribeMessage.text = String(subscribeValue)
                                
                                if subscribeValue.contains("mcu_door") {
                                    if subscribeValue.contains(self.DEVICE_CONTROL_OPEN_RESPONSE_MESSAGE) {
                                        self.stopControlDoorTimer()
                                        Utility.hideLoader(view: self.view)
                                        self.doorLockCtl(lock: false)
                                    } else if subscribeValue.contains(self.DEVICE_CONTROL_CLOSE_RESPONSE_MESSAGE) {
                                        self.stopControlDoorTimer()
                                        Utility.hideLoader(view: self.view)
                                        self.doorLockCtl(lock: true)
                                    }
                                } else if subscribeValue.contains("mcu_window") {
                                    if subscribeValue.contains(self.DEVICE_CONTROL_OPEN_RESPONSE_MESSAGE) {
                                        self.stopControlWindowTimer()
                                        Utility.hideLoader(view: self.view)
                                        self.windowLockCtl(lock: false)
                                    } else if subscribeValue.contains(self.DEVICE_CONTROL_CLOSE_RESPONSE_MESSAGE) {
                                        self.stopControlWindowTimer()
                                        Utility.hideLoader(view: self.view)
                                        self.windowLockCtl(lock: true)
                                    }
                                } else if subscribeValue.contains(self.DEVICE_UPDATE_SHADOW_RESPONSE_MESSAGE) {
                                    self.stopUpdateShadowTimer()
                                    Utility.hideLoader(view: self.view)
                                }
                            }
                        } )
                    }
                    
                    DispatchQueue.main.async {
                        self.startGetShadowTimer()
                    }
                }
                
            case .disconnected:
                self.mqttStatus = "Disconnected"
                self.isServerConnected = false
                NSLog("\(#fileID):\(#line) >> \(self.mqttStatus)")
                self.labelServerStatus.text = self.mqttStatus
                self.labelServerStatus.textColor = UIColor.red
                self.imageServerStatus.tintColor = .red
                
                self.stopGetShadowTimer()
                
            case .connectionRefused:
                self.mqttStatus = "Connection Refused"
                self.isServerConnected = false
                NSLog("\(#fileID):\(#line) >> \(self.mqttStatus)")
                self.labelServerStatus.text = self.mqttStatus
                self.labelServerStatus.textColor = UIColor.red
                self.imageServerStatus.tintColor = .red
                
            case .connectionError:
                self.mqttStatus = "Connection Error"
                self.isServerConnected = false
                NSLog("\(#fileID):\(#line) >> \(self.mqttStatus)")
                self.labelServerStatus.text = self.mqttStatus
                self.labelServerStatus.textColor = UIColor.red
                self.imageServerStatus.tintColor = .red
                
            case .protocolError:
                self.mqttStatus = "Protocol Error"
                self.isServerConnected = false
                NSLog("\(#fileID):\(#line) >> \(self.mqttStatus)")
                self.labelServerStatus.text = self.mqttStatus
                self.labelServerStatus.textColor = UIColor.red
                
            default:
                self.mqttStatus = "Unknown State"
                self.isServerConnected = false
                NSLog("\(#fileID):\(#line) >> unknown state: \(status.rawValue)")
                self.labelServerStatus.text = self.mqttStatus
                self.labelServerStatus.textColor = UIColor.gray
                self.imageServerStatus.tintColor = .gray
            }
            
            NotificationCenter.default.post( name: Notification.Name(rawValue: "connectionStatusChanged"), object: self )
        }
    }
    
    
    func handleConnectViaCert() {
        let defaults = UserDefaults.standard
        let certificateId = defaults.string( forKey: "certificateId")
        if (certificateId == nil) {
            DispatchQueue.main.async {
                NSLog("\(#fileID):\(#line) >> No identity available, searching bundle...")
            }
            let certificateIdInBundle = searchForExistingCertificateIdInBundle()
            
            if (certificateIdInBundle == nil) {
                DispatchQueue.main.async {
                    NSLog("\(#fileID):\(#line) >> No identity found in bundle, creating one...")
                }
                createCertificateIdAndStoreinNSUserDefaults(onSuccess: {generatedCertificateId in
                    let uuid = UUID().uuidString
                    NSLog("\(#fileID):\(#line) >> Using certificate: \(generatedCertificateId)")
                    self.startConnectServerTimer()
                    self.iotDataManager.connect( withClientId: uuid, cleanSession:true, certificateId:generatedCertificateId, statusCallback: self.mqttEventCallback)
                }, onFailure: {error in
                    NSLog("\(#fileID):\(#line) >> Received error: \(error)")
                })
            }
        } else {
            let uuid = UUID().uuidString;
            // Connect to the AWS IoT data plane service w/ certificate
            self.startConnectServerTimer()
            iotDataManager.connect( withClientId: uuid, cleanSession:true, certificateId:certificateId!, statusCallback: self.mqttEventCallback)
        }
    }
    
    func searchForExistingCertificateIdInBundle() -> String? {
        let defaults = UserDefaults.standard
        // No certificate ID has been stored in the user defaults; check to see if any .p12 files
        // exist in the bundle.
        let myBundle = Bundle.main
        let myImages = myBundle.paths(forResourcesOfType: "p12" as String, inDirectory:nil)
        let uuid = UUID().uuidString
        
        guard let certId = myImages.first else {
            let certificateId = defaults.string(forKey: "certificateId")
            return certificateId
        }
        
        // A PKCS12 file may exist in the bundle.  Attempt to load the first one
        // into the keychain (the others are ignored), and set the certificate ID in the
        // user defaults as the filename.  If the PKCS12 file requires a passphrase,
        // you'll need to provide that here; this code is written to expect that the
        // PKCS12 file will not have a passphrase.
        guard let data = try? Data(contentsOf: URL(fileURLWithPath: certId)) else {
            NSLog("\(#fileID):\(#line) >> [ERROR] Found PKCS12 File in bundle, but unable to use it")
            let certificateId = defaults.string( forKey: "certificateId")
            return certificateId
        }
        
        DispatchQueue.main.async {
            NSLog("\(#fileID):\(#line) >> found identity \(certId), importing...")
        }
        if AWSIoTManager.importIdentity( fromPKCS12Data: data, passPhrase:"", certificateId:certId) {
            // Set the certificate ID and ARN values to indicate that we have imported
            // our identity from the PKCS12 file in the bundle.
            defaults.set(certId, forKey:"certificateId")
            defaults.set("from-bundle", forKey:"certificateArn")
            DispatchQueue.main.async {
                NSLog("\(#fileID):\(#line) >> Using certificate: \(certId))")
                self.startConnectServerTimer()
                self.iotDataManager.connect( withClientId: uuid,
                                             cleanSession:true,
                                             certificateId:certId,
                                             statusCallback: self.mqttEventCallback)
            }
        }
        
        let certificateId = defaults.string( forKey: "certificateId")
        return certificateId
    }
    
    func createCertificateIdAndStoreinNSUserDefaults(onSuccess:  @escaping (String)->Void,
                                                     onFailure: @escaping (Error) -> Void) {
        let defaults = UserDefaults.standard
        // Now create and store the certificate ID in NSUserDefaults
        let csrDictionary = [ "commonName": CertificateSigningRequestCommonName,
                              "countryName": CertificateSigningRequestCountryName,
                              "organizationName": CertificateSigningRequestOrganizationName,
                              "organizationalUnitName": CertificateSigningRequestOrganizationalUnitName]
        
        self.iotManager.createKeysAndCertificate(fromCsr: csrDictionary) { (response) -> Void in
            guard let response = response else {
                DispatchQueue.main.async {
                    NSLog("\(#fileID):\(#line) >> Unable to create keys and/or certificate, check values in Constants.swift")
                }
                onFailure(NSError(domain: "No response on iotManager.createKeysAndCertificate", code: -2, userInfo: nil))
                return
            }
            defaults.set(response.certificateId, forKey:"certificateId")
            defaults.set(response.certificateArn, forKey:"certificateArn")
            let certificateId = response.certificateId
            NSLog("\(#fileID):\(#line) >> response: [\(String(describing: response))]")
            
            let attachPrincipalPolicyRequest = AWSIoTAttachPrincipalPolicyRequest()
            attachPrincipalPolicyRequest?.policyName = POLICY_NAME
            attachPrincipalPolicyRequest?.principal = response.certificateArn
            
            // Attach the policy to the certificate
            self.iot.attachPrincipalPolicy(attachPrincipalPolicyRequest!).continueWith (block: { (task) -> AnyObject? in
                if let error = task.error {
                    NSLog("\(#fileID):\(#line) >> Failed: [\(error)]")
                    onFailure(error)
                } else  {
                    NSLog("\(#fileID):\(#line) >> result: [\(String(describing: task.result))]")
                    DispatchQueue.main.asyncAfter(deadline: .now()+2, execute: {
                        if let certificateId = certificateId {
                            onSuccess(certificateId)
                        } else {
                            onFailure(NSError(domain: "Unable to generate certificate id", code: -1, userInfo: nil))
                        }
                    })
                }
                return nil
            })
        }
    }
    
    func handleDisconnect() {
        NSLog("\(#fileID):\(#line) >> Disconnecting...")
        DispatchQueue.global(qos: DispatchQoS.QoSClass.default).async {
            self.iotDataManager.disconnect();
            DispatchQueue.main.async {
                self.isServerConnected = false
            }
        }
    }
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
        NSLog("\(#fileID):\(#line) >> viewDidLoad" )
        
        appDelegate.awsIoTDeviceVC = self
        
        self.viewConnectStatus.backgroundColor = UIColor.black
        self.viewConnectStatus.layer.borderColor = UIColor(red: 42/255, green: 40/255, blue: 157/255, alpha: 1).cgColor
        self.viewConnectStatus.layer.borderWidth = 2.0
        self.viewConnectStatus.layer.cornerRadius = 10
        
        self.viewMQTT.backgroundColor = UIColor.black
        self.viewMQTT.layer.borderColor = UIColor(red: 42/255, green: 40/255, blue: 157/255, alpha: 1).cgColor
        self.viewMQTT.layer.borderWidth = 2.0
        self.viewMQTT.layer.cornerRadius = 10
        
        self.imageServerStatus.image = UIImage(named: "server_connect_status")
        self.imageServerStatus.image = self.imageServerStatus.image?.withRenderingMode(.alwaysTemplate)
        
        self.imageThingStatus.image = UIImage(named: "thing_connect_status")
        self.imageThingStatus.image = self.imageThingStatus.image?.withRenderingMode(.alwaysTemplate)
        
        self.textviewShadow.backgroundColor = UIColor.white
        self.textviewShadow.layer.borderColor = UIColor(red: 42/255, green: 40/255, blue: 157/255, alpha: 1).cgColor
        self.textviewShadow.layer.borderWidth = 2.0
        self.textviewShadow.layer.cornerRadius = 10
        
        self.btnUpdateShadow.layer.cornerRadius = 10
        self.btnUpdateShadow.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnUpdateShadow.layer.shadowOpacity = 1;
        self.btnUpdateShadow.layer.shadowRadius = 1;
        self.btnUpdateShadow.layer.shadowOffset = CGSize(width: 1, height: 4)
        
        self.btnDoorControl.layer.cornerRadius = 10
        self.btnDoorControl.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnDoorControl.layer.shadowOpacity = 1;
        self.btnDoorControl.layer.shadowRadius = 1;
        self.btnDoorControl.layer.shadowOffset = CGSize(width: 1, height: 4)
        
        self.btnWindowControl.layer.cornerRadius = 10
        self.btnWindowControl.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnWindowControl.layer.shadowOpacity = 1;
        self.btnWindowControl.layer.shadowRadius = 1;
        self.btnWindowControl.layer.shadowOffset = CGSize(width: 1, height: 4)
        
        self.btnOtaUpdate.layer.cornerRadius = 10
        self.btnOtaUpdate.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnOtaUpdate.layer.shadowOpacity = 1;
        self.btnOtaUpdate.layer.shadowRadius = 1;
        self.btnOtaUpdate.layer.shadowOffset = CGSize(width: 1, height: 4)
        
        self.thingName = defaults.string(forKey: "thingNameKey")
        NSLog("\(#fileID):\(#line) >> AWSIoTDoorViewController thingName: \(self.thingName!)")
        self.label_thingName.text = self.thingName!
        
        self.publishTopic = self.thingName!+"/"+"AppControl"
        NSLog("\(#fileID):\(#line) >> publishTopic : \(self.publishTopic!)")
        
        self.subscribeTopic = self.thingName!+"/"+"#"
        NSLog("\(#fileID):\(#line) >> subscribeTopic : \(self.subscribeTopic!)")
        
        //[[for single Cognito poolid
        self.IDENTITY_POOL_ID = Config.getIdentityPoolId()
        //]]
        let credentialsProvider = AWSCognitoCredentialsProvider(regionType:AWS_REGION,
                                                                identityPoolId:self.IDENTITY_POOL_ID!)
        initializeControlPlane(credentialsProvider: credentialsProvider)
        initializeDataPlane(credentialsProvider: credentialsProvider)
        
        if (self.isServerConnected == false) {
            handleConnectViaCert()
        } else {
            handleDisconnect()
        }
        
        self.doorStateFlag = defaults.string(forKey: "doorStateFlagKey")
        if self.doorStateFlag == "true" {  //opened
            self.btnDoorControl.setTitle("Close door", for: .normal)
            self.labelDoorStatus.text = "The door is opened"
            self.labelDoorStatus.backgroundColor = .systemRed
        } else {  //closed
            self.btnDoorControl.setTitle("Open door", for: .normal)
            self.labelDoorStatus.text = "The door is closed"
            self.labelDoorStatus.backgroundColor = .systemGreen
        }
        
        self.windowStateFlag = defaults.string(forKey: "windowStateFlagKey")
        if self.windowStateFlag == "true" {  //opened
            self.btnWindowControl.setTitle("Close window", for: .normal)
            self.labelWindowStatus.text = "The window is opened"
            self.labelWindowStatus.backgroundColor = .systemRed
        } else {  //closed
            self.btnWindowControl.setTitle("Open window", for: .normal)
            self.labelWindowStatus.text = "The door is closed"
            self.labelWindowStatus.backgroundColor = .systemGreen
        }
        
        self.existOTAupdateFlag = defaults.bool(forKey: "existOTAupdateFlagKey")
        if existOTAupdateFlag == true {
            self.showBadge(withString: "New")
        } else {
            self.removeBadge()
        }
        
    }
    
    func initializeControlPlane(credentialsProvider: AWSCredentialsProvider) {
        //Initialize control plane
        // Initialize the Amazon Cognito credentials provider
        let controlPlaneServiceConfiguration = AWSServiceConfiguration(region:AWS_REGION, credentialsProvider:credentialsProvider)
        
        //IoT control plane seem to operate on iot.<region>.amazonaws.com
        //Set the defaultServiceConfiguration so that when we call AWSIoTManager.default(), it will get picked up
        AWSServiceManager.default().defaultServiceConfiguration = controlPlaneServiceConfiguration
        iotManager = AWSIoTManager.default()
        iot = AWSIoT.default()
    }
    
    func initializeDataPlane(credentialsProvider: AWSCredentialsProvider) {
        //Initialize Dataplane:
        // IoT Dataplane must use your account specific IoT endpoint
        let iotEndPoint = AWSEndpoint(urlString: IOT_ENDPOINT)
        
        // Configuration for AWSIoT data plane APIs
        let iotDataConfiguration = AWSServiceConfiguration(region: AWS_REGION,
                                                           endpoint: iotEndPoint,
                                                           credentialsProvider: credentialsProvider)
        //IoTData manager operates on xxxxxxx-iot.<region>.amazonaws.com
        AWSIoTDataManager.register(with: iotDataConfiguration!, forKey: AWS_IOT_DATA_MANAGER_KEY)
        iotDataManager = AWSIoTDataManager(forKey: AWS_IOT_DATA_MANAGER_KEY)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        NSLog("\(#fileID):\(#line) >> viewWillAppear" )
    }
    
    override func viewDidAppear(_ animated: Bool) {
        NSLog("\(#fileID):\(#line) >> viewDidAppear" )
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        NSLog("\(#fileID):\(#line) >> viewWillDisappear" )
        self.iotDataManager.disconnect();
        self.iotDataManager.unsubscribeTopic(self.subscribeTopic!)
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        NSLog("\(#fileID):\(#line) >> viewDidDisappear" )
    }
    
    override func viewWillLayoutSubviews() {
        NSLog("\(#fileID):\(#line) >> viewWillLayoutSubviews" )
    }
    
    override func viewDidLayoutSubviews() {
        NSLog("\(#fileID):\(#line) >> viewDidLayoutSubviews" )
    }
    
    // MARK: - button click event
    
    @IBAction func onBtnBack(_ sender: UIButton) {
        let mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "MainViewController")
        mainVC.modalPresentationStyle = .fullScreen
        present(mainVC, animated: true, completion: nil)
    }
    
    @IBAction func onBtnUpdateShadow(_ sender: UIButton) {
        if (self.isServerConnected && self.isDeviceConnected) {
            Utility.showLoader(message: "Updating status...", view: view)
            self.labelPublishTopic.text = self.publishTopic!
            self.labelPublishMessage.text = self.APP_UPDATE_SHADOW_MESSAGE
            self.iotDataManager.publishString(self.APP_UPDATE_SHADOW_MESSAGE, onTopic:self.publishTopic!, qoS:.messageDeliveryAttemptedAtLeastOnce)
            self.startUpdateShadowTimer()
        }
    }
    
    @IBAction func onBtnDoorControl(_ sender: UIButton) {
        if (self.isServerConnected && self.isDeviceConnected) {
            if sender.title(for: .normal) == "Open door" {
                self.labelPublishTopic.text = self.publishTopic!
                self.labelPublishMessage.text = self.APP_CONTROL_DOOR_OPEN_MESSAGE
                self.iotDataManager.publishString(self.APP_CONTROL_DOOR_OPEN_MESSAGE, onTopic:self.publishTopic!, qoS:.messageDeliveryAttemptedAtLeastOnce)
                self.startControlDoorTimer()
                Utility.showLoader(message: "The door is being opened...", view: view)
            } else if sender.title(for: .normal) == "Close door" {
                self.labelPublishTopic.text = self.publishTopic!
                self.labelPublishMessage.text = self.APP_CONTROL_DOOR_CLOSE_MESSAGE
                self.iotDataManager.publishString(self.APP_CONTROL_DOOR_CLOSE_MESSAGE, onTopic:self.publishTopic!, qoS:.messageDeliveryAttemptedAtLeastOnce)
                self.startControlDoorTimer()
                Utility.showLoader(message: "The door is being closed...", view: view)
            }
        }
    }
    
    @IBAction func onBtnWindowControl(_ sender: UIButton) {
        if (self.isServerConnected && self.isDeviceConnected) {
            if sender.title(for: .normal) == "Open window" {
                self.labelPublishTopic.text = self.publishTopic!
                self.labelPublishMessage.text = self.APP_CONTROL_WINDOW_OPEN_MESSAGE
                self.iotDataManager.publishString(self.APP_CONTROL_WINDOW_OPEN_MESSAGE, onTopic:self.publishTopic!, qoS:.messageDeliveryAttemptedAtLeastOnce)
                self.startControlWindowTimer()
                Utility.showLoader(message: "The window is being opened...", view: view)
            } else if sender.title(for: .normal) == "Close window" {
                self.labelPublishTopic.text = self.publishTopic!
                self.labelPublishMessage.text = self.APP_CONTROL_WINDOW_CLOSE_MESSAGE
                self.iotDataManager.publishString(self.APP_CONTROL_WINDOW_CLOSE_MESSAGE, onTopic:self.publishTopic!, qoS:.messageDeliveryAttemptedAtLeastOnce)
                self.startControlWindowTimer()
                Utility.showLoader(message: "The window is being closed...", view: view)
            }
        }
    }
    
    @IBAction func onBtnOTAupdate(_ sender: UIButton) {
        
        if (self.isServerConnected && self.isDeviceConnected) {
            if self.existOTAupdateFlag == true {
                self.showUpdatingDialog()
                self.labelPublishTopic.text = self.publishTopic!
                self.labelPublishMessage.text = self.APP_CONTROL_OTA_MESSAGE
                self.iotDataManager.publishString(self.APP_CONTROL_OTA_MESSAGE, onTopic:self.publishTopic!, qoS:.messageDeliveryAttemptedAtLeastOnce)
            } else {
                showToast(message: "Nothing to update", seconds: 1.0)
            }
        } else {
            showToast(message: "Failed to connect to the device", seconds: 1.0)
        }
    }
    
    
    func doorLockCtl(lock: Bool) {
        if (lock == true) {
            self.btnDoorControl.setTitle("Open door", for: .normal)
            //rotate to y axis
            var transform: CATransform3D = CATransform3DIdentity
            var transform1: CATransform3D = CATransform3DIdentity
            UIView.animate(withDuration: 1.0, animations: {
                transform = CATransform3DRotate(transform, CGFloat(180 * Double.pi / 180.0), 0, 1, 0)
                self.btnDoorControl.layer.transform = transform
                transform1 = CATransform3DRotate(transform1, CGFloat(180 * Double.pi / 180.0), 0, 1, 0)
                self.btnDoorControl.titleLabel?.layer.transform = transform1
            })
        } else if (lock == false) {
            self.btnDoorControl.setTitle("Close door", for: .normal)
            //rotate to y axis
            var transform: CATransform3D = CATransform3DIdentity
            var transform1: CATransform3D = CATransform3DIdentity
            UIView.animate(withDuration: 1.0, animations: {
                transform = CATransform3DRotate(transform, CGFloat(180 * Double.pi / 180.0), 0, 1, 0)
                self.btnDoorControl.layer.transform = transform
                transform1 = CATransform3DRotate(transform1, CGFloat(180 * Double.pi / 180.0), 0, 1, 0)
                self.btnDoorControl.titleLabel?.layer.transform = transform1
            })
        }
    }
    
    func windowLockCtl(lock: Bool) {
        if (lock == true) {
            self.btnWindowControl.setTitle("Open window", for: .normal)
            //rotate to y axis
            var transform: CATransform3D = CATransform3DIdentity
            var transform1: CATransform3D = CATransform3DIdentity
            UIView.animate(withDuration: 1.0, animations: {
                transform = CATransform3DRotate(transform, CGFloat(180 * Double.pi / 180.0), 0, 1, 0)
                self.btnWindowControl.layer.transform = transform
                transform1 = CATransform3DRotate(transform1, CGFloat(180 * Double.pi / 180.0), 0, 1, 0)
                self.btnWindowControl.titleLabel?.layer.transform = transform1
            })
        } else if (lock == false) {
            self.btnWindowControl.setTitle("Close window", for: .normal)
            //rotate to y axis
            var transform: CATransform3D = CATransform3DIdentity
            var transform1: CATransform3D = CATransform3DIdentity
            UIView.animate(withDuration: 1.0, animations: {
                transform = CATransform3DRotate(transform, CGFloat(180 * Double.pi / 180.0), 0, 1, 0)
                self.btnWindowControl.layer.transform = transform
                transform1 = CATransform3DRotate(transform1, CGFloat(180 * Double.pi / 180.0), 0, 1, 0)
                self.btnWindowControl.titleLabel?.layer.transform = transform1
            })
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
    
    // MARK: - Get Shadow
    
    func getShadow(thingName: String) {
        NSLog("\(#fileID):\(#line) >> getShadow thingName = \(thingName)")
        let clientToken = UUID().uuidString
        iotDataManager.getShadow(thingName, clientToken: clientToken)
        
        iotDataManager.register(withShadow: thingName, options: nil, eventCallback: { (name, operationType, status, clientToken, data) in
            NSLog("\(#fileID):\(#line) >> name: \(name)")
            NSLog("\(#fileID):\(#line) >> opertaionType: \(operationType)")
            NSLog("\(#fileID):\(#line) >> status: \(status)")
            NSLog("\(#fileID):\(#line) >> clientToken:  \(clientToken)")
            NSLog("\(#fileID):\(#line) >> data: \(data)")
            
            let strJsonString  = String(decoding: data, as: UTF8.self)
            
            let oJsonDataT: Data? = strJsonString.data(using: .utf8)
            if let oJsonData = oJsonDataT {
                var oJsonDictionaryT:[String:Any]?
                oJsonDictionaryT = try? JSONSerialization.jsonObject(with: oJsonData, options: []) as? [String:Any]
                
                if let oJsonDictionary = oJsonDictionaryT {
                    
                    if let strMeta = oJsonDictionary["metadata"] {
                        let metas = strMeta as? [String:Any]
                        if let strMetaRreported = metas?["reported"] as? [String:Any] {
                            if let meta_doorStat = strMetaRreported["doorStat"] as? [String:Any] {
                                if let meta_doorStat_timestamp = meta_doorStat["timestamp"] as? TimeInterval {
                                    let curDoorStateTimestamp: UInt = UInt(meta_doorStat_timestamp)
                                    let meta_doorStat_date = Date(timeIntervalSince1970: TimeInterval(meta_doorStat_timestamp))
                                    let doorDate = DateFormatter()
                                    doorDate.dateFormat = "yyyy-MM-dd HH:mm:ss"
                                    NSLog("\(#fileID):\(#line) >> pastDoorStateTimestamp : \(self.pastDoorStateTimestamp)")
                                    NSLog("\(#fileID):\(#line) >> curDoorStateTimestamp : \(curDoorStateTimestamp)")
                                    if (self.pastDoorStateTimestamp != curDoorStateTimestamp) {
                                        self.isDoorStateUpdated = true
                                        DispatchQueue.main.async {
                                            self.labelUpdatedTime.text = doorDate.string(from:meta_doorStat_date)
                                        }
                                    } else {
                                        self.isDoorStateUpdated = false
                                    }
                                    self.pastDoorStateTimestamp = curDoorStateTimestamp
                                    
                                }
                            }
                            
                            if let meta_windowStat = strMetaRreported["windowStat"] as? [String:Any] {
                                if let meta_windowStat_timestamp = meta_windowStat["timestamp"] as? TimeInterval {
                                    let curWindowStateTimestamp: UInt = UInt(meta_windowStat_timestamp)
                                    let meta_windowStat_date = Date(timeIntervalSince1970: TimeInterval(meta_windowStat_timestamp))
                                    let windowDate = DateFormatter()
                                    windowDate.dateFormat = "yyyy-MM-dd HH:mm:ss"
                                    NSLog("\(#fileID):\(#line) >> pastWindowStateTimestamp : \(self.pastWindowStateTimestamp)")
                                    NSLog("\(#fileID):\(#line) >> curWindowStateTimestamp : \(curWindowStateTimestamp)")
                                    if (self.pastWindowStateTimestamp != curWindowStateTimestamp) {
                                        self.isWindowStateUpdated = true
                                        DispatchQueue.main.async {
                                            self.labelUpdatedTime.text = windowDate.string(from:meta_windowStat_date)
                                        }
                                    } else {
                                        self.isWindowStateUpdated = false
                                    }
                                    self.pastWindowStateTimestamp = curWindowStateTimestamp
                                    
                                    
                                }
                            }
                            
                            if let meta_otaUpdate = strMetaRreported["OTAupdate"] as? [String:Any] {
                                NSLog("\(#fileID):\(#line) >> meta_otaUpdate = \(meta_otaUpdate)")
                                if let meta_otaUpdate_timestamp = meta_otaUpdate["timestamp"] as? TimeInterval {
                                    NSLog("\(#fileID):\(#line) >> meta_otaUpdate_timestamp = \(meta_otaUpdate_timestamp)")
                                    let curOtaUpdateTimestamp: UInt = UInt(meta_otaUpdate_timestamp)
                                    let meta_otaUpdate_date = Date(timeIntervalSince1970: TimeInterval(meta_otaUpdate_timestamp))
                                    NSLog("\(#fileID):\(#line) >> \(meta_otaUpdate_date)")
                                    let otaDate = DateFormatter()
                                    otaDate.dateFormat = "yyyy-MM-dd HH:mm:ss"
                                    NSLog("\(#fileID):\(#line) >> pastOtaTimestamp : \(self.pastOtaTimestamp)")
                                    NSLog("\(#fileID):\(#line) >> curOtaUpdateTimestamp : \(curOtaUpdateTimestamp)")
                                    if (self.pastOtaTimestamp != curOtaUpdateTimestamp) {
                                        self.isOtaUpdated = true
                                        DispatchQueue.main.async {
                                            self.labelUpdatedTime.text = otaDate.string(from:meta_otaUpdate_date)
                                        }
                                    } else {
                                        self.isOtaUpdated = false
                                    }
                                    
                                    self.pastOtaTimestamp = curOtaUpdateTimestamp
                                    
                                }
                            }
                        }
                    }
                    
                    if let strState = oJsonDictionary["state"] {
                        DispatchQueue.main.async {
                            self.textviewShadow.text = String(describing: strState)
                        }
                        let states = strState as? [String:Any]
                        if let strRreported = states?["reported"] as? [String:Any] {
                            
                            if let doorStat = strRreported["doorStat"] as? String {
                                NSLog("\(#fileID):\(#line) >> doorStat = \(doorStat)")
                                self.doorStat = doorStat
                            }
                            if let doorStateChange = strRreported["doorStateChange"] as? UInt {
                                NSLog("\(#fileID):\(#line) >> doorStateChange = \(doorStateChange)")
                            }
                            if let windowStat = strRreported["windowStat"] as? String {
                                NSLog("\(#fileID):\(#line) >> windowStat = \(windowStat)")
                            }
                            if let doorBell = strRreported["doorBell"] as? UInt {
                                NSLog("\(#fileID):\(#line) >> doorBell = \(doorBell)")
                            }
                            if let temperature = strRreported["temperature"] as? Float {
                                NSLog("\(#fileID):\(#line) >> temperature = \(temperature)")
                            }
                            if let doorState = strRreported["doorState"] as? String {
                                NSLog("\(#fileID):\(#line) >> doorState = \(doorState)")
                            }
                            if let openMethod = strRreported["openMethod"] as? String {
                                NSLog("\(#fileID):\(#line) >> openMethod = \(openMethod)")
                            }
                            if let doorOpenMode = strRreported["DoorOpenMode"] as? UInt {
                                NSLog("\(#fileID):\(#line) >> doorOpenMode = \(doorOpenMode)")
                            }
                            if let temp = strRreported["temp"] as? UInt {
                                NSLog("\(#fileID):\(#line) >> temp = \(temp)")
                            }
                            if let OTAresult = strRreported["OTAresult"] as? String {
                                NSLog("\(#fileID):\(#line) >> OTAresult = \(OTAresult)")
                                self.OTAresult = OTAresult
                            }
                            if let battery = strRreported["battery"] as? Float {
                                NSLog("\(#fileID):\(#line) >> battery = \(battery)")
                            }
                            if let OTAupdate = strRreported["OTAupdate"] as? UInt {
                                NSLog("\(#fileID):\(#line) >> OTAupdate = \(OTAupdate)")
                                self.OTAupdate = OTAupdate
                            }
                            
                            NSLog("\(#fileID):\(#line) >> isDoorStateUpdated = \(self.isDoorStateUpdated)")
                            if (self.isDoorStateUpdated == true) {
                                DispatchQueue.main.async {
                                    if self.doorStat == "opened" {
                                        self.btnDoorControl.setTitle("Close door", for: .normal)
                                        //rotate to y axis
                                        var transform: CATransform3D = CATransform3DIdentity
                                        var transform1: CATransform3D = CATransform3DIdentity
                                        UIView.animate(withDuration: 1.0, animations: {
                                            transform = CATransform3DRotate(transform, CGFloat(180 * Double.pi / 180.0), 0, 1, 0)
                                            self.btnDoorControl.layer.transform = transform
                                            transform1 = CATransform3DRotate(transform1, CGFloat(180 * Double.pi / 180.0), 0, 1, 0)
                                            self.btnDoorControl.titleLabel?.layer.transform = transform1
                                        })
                                        self.labelDoorStatus.text = " The door is opened"
                                        self.labelDoorStatus.backgroundColor = .systemRed
                                        self.doorStateFlag = "true"
                                        self.defaults.set(self.doorStateFlag, forKey: "doorStateFlagKey")
                                    } else if self.doorStat == "closed" {
                                        self.btnDoorControl.setTitle("Open door", for: .normal)
                                        //rotate to y axis
                                        var transform: CATransform3D = CATransform3DIdentity
                                        var transform1: CATransform3D = CATransform3DIdentity
                                        UIView.animate(withDuration: 1.0, animations: {
                                            transform = CATransform3DRotate(transform, CGFloat(180 * Double.pi / 180.0), 0, 1, 0)
                                            self.btnDoorControl.layer.transform = transform
                                            transform1 = CATransform3DRotate(transform1, CGFloat(180 * Double.pi / 180.0), 0, 1, 0)
                                            self.btnDoorControl.titleLabel?.layer.transform = transform1
                                        })
                                        self.labelDoorStatus.text = " The door is closed"
                                        self.labelDoorStatus.backgroundColor = .systemGreen
                                        self.doorStateFlag = "false"
                                        self.defaults.set(self.doorStateFlag, forKey: "doorStateFlagKey")
                                    }
                                }
                                self.isDoorStateUpdated = false
                            }
                            
                            NSLog("\(#fileID):\(#line) >> isWindowStateUpdated = \(self.isWindowStateUpdated)")
                            if (self.isWindowStateUpdated == true) {
                                DispatchQueue.main.async {
                                    self.textviewShadow.text = String(describing: strState)
                                }
                                DispatchQueue.main.async {
                                    if self.windowStat == "opened" {
                                        self.btnWindowControl.setTitle("Close window", for: .normal)
                                        //rotate to y axis
                                        var transform: CATransform3D = CATransform3DIdentity
                                        var transform1: CATransform3D = CATransform3DIdentity
                                        UIView.animate(withDuration: 1.0, animations: {
                                            transform = CATransform3DRotate(transform, CGFloat(180 * Double.pi / 180.0), 0, 1, 0)
                                            self.btnWindowControl.layer.transform = transform
                                            transform1 = CATransform3DRotate(transform1, CGFloat(180 * Double.pi / 180.0), 0, 1, 0)
                                            self.btnWindowControl.titleLabel?.layer.transform = transform1
                                        })
                                        //
                                        self.labelWindowStatus.text = " The window is opened"
                                        self.labelWindowStatus.backgroundColor = .systemRed
                                        self.windowStateFlag = "true"
                                        self.defaults.set(self.windowStateFlag, forKey: "windowStateFlagKey")
                                    } else if self.windowStat == "closed" {
                                        self.btnWindowControl.setTitle("Open window", for: .normal)
                                        //rotate to y axis
                                        var transform: CATransform3D = CATransform3DIdentity
                                        var transform1: CATransform3D = CATransform3DIdentity
                                        UIView.animate(withDuration: 1.0, animations: {
                                            transform = CATransform3DRotate(transform, CGFloat(180 * Double.pi / 180.0), 0, 1, 0)
                                            self.btnWindowControl.layer.transform = transform
                                            transform1 = CATransform3DRotate(transform1, CGFloat(180 * Double.pi / 180.0), 0, 1, 0)
                                            self.btnWindowControl.titleLabel?.layer.transform = transform1
                                        })
                                        //
                                        self.labelWindowStatus.text = " The window is closed"
                                        self.labelWindowStatus.backgroundColor = .systemGreen
                                        self.windowStateFlag = "false"
                                        self.defaults.set(self.windowStateFlag, forKey: "windowStateFlagKey")
                                    }
                                }
                                self.isWindowStateUpdated = false
                            }
                            
                            NSLog("\(#fileID):\(#line) >> isOtaUpdated = \(self.isOtaUpdated)")
                            if (self.isOtaUpdated == true) {
                                if (self.OTAupdate == 0) {  //none
                                    self.existOTAupdateFlag = false
                                    self.OTAupdateProgressFlage = false
                                    if (self.OTAresult == "OTA_OK") {
                                        DispatchQueue.main.async {
                                            self.dismissUpdatingDialog()
                                            self.showUpdateSuccessDialog()
                                        }
                                        
                                    } else if (self.OTAresult == "OTA_NG") {
                                        DispatchQueue.main.async {
                                            self.dismissUpdatingDialog()
                                            self.showUpdateFailDialog()
                                        }
                                        
                                    } else if (self.OTAresult == "OTA_UNKNOWN") {
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
                                    
                                } else if (self.OTAupdate == 2) {  //update progressing
                                    self.existOTAupdateFlag = false
                                    self.OTAupdateProgressFlage = true
                                    DispatchQueue.main.async {
                                        self.removeBadge()
                                    }
                                    
                                }
                                self.isOtaUpdated = false
                                self.defaults.set(self.existOTAupdateFlag, forKey: "existOTAupdateFlagKey")
                                self.defaults.set(self.OTAupdateProgressFlage, forKey: "OTAupdateProgressFlageKey")
                            }
                            
                        }
                    }
                }
            }
        })
    }
    
    // MARK: - getShadow Timer
    func startGetShadowTimer() {
        NSLog("\(#fileID):\(#line) >> startGetShadowTimer()")
        if let timer = getShadowTimer {
            if !timer.isValid {
                getShadowTimer = Timer.scheduledTimer(timeInterval: 2.0, target: self, selector: #selector(funcGetShadow), userInfo: nil, repeats: true)
            }
        } else {
            getShadowTimer = Timer.scheduledTimer(timeInterval: 2.0, target: self, selector: #selector(funcGetShadow), userInfo: nil, repeats: true)
        }
        
    }
    
    @objc func funcGetShadow() {
        self.getShadow(thingName: self.thingName!)
    }
    
    func stopGetShadowTimer() {
        NSLog("\(#fileID):\(#line) >> stopGetShadowTimer()")
        if let timer = getShadowTimer {
            if (timer.isValid) {
                timer.invalidate()
                getShadowTimer = nil
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
                connectServerTimer = Timer.scheduledTimer(timeInterval: 10.0, target: self, selector: #selector(showConnectServerTimeoutDialog), userInfo: nil, repeats: true)
            }
        } else {
            connectServerTimer = Timer.scheduledTimer(timeInterval: 10.0, target: self, selector: #selector(showConnectServerTimeoutDialog), userInfo: nil, repeats: true)
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
                connectThingTimer = Timer.scheduledTimer(timeInterval: 10.0, target: self, selector: #selector(showConnectThingTimeoutDialog), userInfo: nil, repeats: true)
            }
        } else {
            connectThingTimer = Timer.scheduledTimer(timeInterval: 10.0, target: self, selector: #selector(showConnectThingTimeoutDialog), userInfo: nil, repeats: true)
        }
        
    }
    
    @objc func showConnectThingTimeoutDialog() {
        let alertMessage = """
            
            No response was received from the thing.
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
        
        let cancel = UIAlertAction(title: "Cancel", style: .destructive, handler: { (action) -> Void in })
        self.connectThingTimeoutAlert!.addAction(cancel)
        
        let okAction = UIAlertAction(title: "OK", style: .default, handler: { (action) -> Void in
            self.publishTopic! = self.thingName!+"/"+self.APP_PUBLISH_TOPIC
            self.labelPublishTopic.text = self.publishTopic!
            self.labelPublishMessage.text = self.APP_CONNECT_MESSAGE
            self.iotDataManager.publishString(self.APP_CONNECT_MESSAGE, onTopic:self.publishTopic!, qoS:.messageDeliveryAttemptedAtLeastOnce)
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
    
    // MARK: - Control Door Timeout Dialog
    
    func startControlDoorTimer() {
        NSLog("\(#fileID):\(#line) >> startControlDoorTimer()")
        if let timer = controlDoorTimer {
            if !timer.isValid {
                controlDoorTimer = Timer.scheduledTimer(timeInterval: 10.0, target: self, selector: #selector(showControlDoorTimeoutDialog), userInfo: nil, repeats: true)
            }
        } else {
            controlDoorTimer = Timer.scheduledTimer(timeInterval: 10.0, target: self, selector: #selector(showControlDoorTimeoutDialog), userInfo: nil, repeats: true)
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
        
        let cancel = UIAlertAction(title: "Cancel", style: .destructive, handler: { (action) -> Void in })
        self.controlDoorTimeoutAlert!.addAction(cancel)
        
        let okAction = UIAlertAction(title: "OK", style: .default, handler: { (action) -> Void in
            if self.doorStateFlag == "true" {  //opened
                self.labelPublishTopic.text = self.publishTopic!
                self.labelPublishMessage.text = self.APP_CONTROL_DOOR_CLOSE_MESSAGE
                self.iotDataManager.publishString(self.APP_CONTROL_DOOR_CLOSE_MESSAGE, onTopic:self.publishTopic!, qoS:.messageDeliveryAttemptedAtLeastOnce)
                self.startControlDoorTimer()
                Utility.showLoader(message: "The door is being closed...", view: self.view)
            } else if self.doorStateFlag == "false" {  //closed
                self.labelPublishTopic.text = self.publishTopic!
                self.labelPublishMessage.text = self.APP_CONTROL_DOOR_OPEN_MESSAGE
                self.iotDataManager.publishString(self.APP_CONTROL_DOOR_OPEN_MESSAGE, onTopic:self.publishTopic!, qoS:.messageDeliveryAttemptedAtLeastOnce)
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
    
    // MARK: - Control Window Timeout Dialog
    
    func startControlWindowTimer() {
        NSLog("\(#fileID):\(#line) >> startControlWindowTimer()")
        if let timer = controlWindowTimer {
            if !timer.isValid {
                controlWindowTimer = Timer.scheduledTimer(timeInterval: 10.0, target: self, selector: #selector(showControlWindowTimeoutDialog), userInfo: nil, repeats: true)
            }
        } else {
            controlWindowTimer = Timer.scheduledTimer(timeInterval: 10.0, target: self, selector: #selector(showControlWindowTimeoutDialog), userInfo: nil, repeats: true)
        }
        
    }
    
    @objc func showControlWindowTimeoutDialog() {
        let alertMessage = """
            
            No response was received from the window.
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
        
        self.controlWindowTimeoutAlert = UIAlertController.init(title: nil, message: nil, preferredStyle: UIAlertController.Style.alert)
        self.controlWindowTimeoutAlert!.setValue(attributedMessageText, forKey: "attributedMessage")
        
        let cancel = UIAlertAction(title: "Cancel", style: .destructive, handler: { (action) -> Void in })
        self.controlWindowTimeoutAlert!.addAction(cancel)
        
        let okAction = UIAlertAction(title: "OK", style: .default, handler: { (action) -> Void in
            if self.windowStateFlag == "true" {  //opened
                self.labelPublishTopic.text = self.publishTopic!
                self.labelPublishMessage.text = self.APP_CONTROL_WINDOW_CLOSE_MESSAGE
                self.iotDataManager.publishString(self.APP_CONTROL_WINDOW_CLOSE_MESSAGE, onTopic:self.publishTopic!, qoS:.messageDeliveryAttemptedAtLeastOnce)
                self.startControlWindowTimer()
                Utility.showLoader(message: "The window is being closed...", view: self.view)
            } else if self.doorStateFlag == "false" {  //closed
                self.labelPublishTopic.text = self.publishTopic!
                self.labelPublishMessage.text = self.APP_CONTROL_WINDOW_OPEN_MESSAGE
                self.iotDataManager.publishString(self.APP_CONTROL_WINDOW_OPEN_MESSAGE, onTopic:self.publishTopic!, qoS:.messageDeliveryAttemptedAtLeastOnce)
                self.startControlWindowTimer()
                Utility.showLoader(message: "The window is being opened...", view: self.view)
            }
        })
        self.controlWindowTimeoutAlert!.addAction(okAction)
        
        
        present(self.controlWindowTimeoutAlert!, animated: true, completion: nil)
    }
    
    func stopControlWindowTimer() {
        NSLog("\(#fileID):\(#line) >> stopControlWindowTimer()")
        if let timer = controlWindowTimer {
            if (timer.isValid) {
                timer.invalidate()
                controlWindowTimer = nil
            }
        }
    }
    
    // MARK: - Updae Shadow Timeout Dialog
    
    func startUpdateShadowTimer() {
        NSLog("\(#fileID):\(#line) >> updateShadowTimer()")
        if let timer = updateShadowTimer {
            if !timer.isValid {
                updateShadowTimer = Timer.scheduledTimer(timeInterval: 10.0, target: self, selector: #selector(showUpdateShadowTimeoutDialog), userInfo: nil, repeats: true)
            }
        } else {
            updateShadowTimer = Timer.scheduledTimer(timeInterval: 10.0, target: self, selector: #selector(showUpdateShadowTimeoutDialog), userInfo: nil, repeats: true)
        }
        
    }
    
    @objc func showUpdateShadowTimeoutDialog() {
        let alertMessage = """
            
            Failed to update shadow.
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
        
        self.updateShadowTimeoutAlert = UIAlertController.init(title: nil, message: nil, preferredStyle: UIAlertController.Style.alert)
        self.updateShadowTimeoutAlert!.setValue(attributedMessageText, forKey: "attributedMessage")
        
        let cancel = UIAlertAction(title: "Cancel", style: .destructive, handler: { (action) -> Void in })
        self.updateShadowTimeoutAlert!.addAction(cancel)
        
        let okAction = UIAlertAction(title: "OK", style: .default, handler: { (action) -> Void in
            Utility.showLoader(message: "Updating status...", view: self.view)
            self.labelPublishTopic.text = self.publishTopic!
            self.labelPublishMessage.text = self.APP_UPDATE_SHADOW_MESSAGE
            self.iotDataManager.publishString(self.APP_UPDATE_SHADOW_MESSAGE, onTopic:self.publishTopic!, qoS:.messageDeliveryAttemptedAtLeastOnce)
            self.startUpdateShadowTimer()
        })
        self.updateShadowTimeoutAlert!.addAction(okAction)
        
        
        present(self.updateShadowTimeoutAlert!, animated: true, completion: nil)
    }
    
    func stopUpdateShadowTimer() {
        NSLog("\(#fileID):\(#line) >> stopUpdateShadowTimer()")
        if let timer = updateShadowTimer {
            if (timer.isValid) {
                timer.invalidate()
                updateShadowTimer = nil
            }
        }
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
