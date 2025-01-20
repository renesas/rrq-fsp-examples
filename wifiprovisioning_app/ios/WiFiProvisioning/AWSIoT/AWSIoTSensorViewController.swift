//
//  AWSIoTSensorViewController.swift
//  WiFiProvisioning
//
//  Created by livekim on 2021/06/03.
//
// ******************************************************************************
//*
//* Copyright (C) 2023 Renesas Electronics.
//* This computer program includes Confidential, Proprietary Information
//* of Renesas Electronics. All Rights Reserved.
//*
//*******************************************************************************

import Foundation
import AVFoundation
import AudioToolbox
import UIKit
import AWSIoT
import AWSMobileClient

class AWSIoTSensorViewController: UIViewController {
    
    let appDelegate = UIApplication.shared.delegate as! AppDelegate
    let defaults = UserDefaults.standard
    var thingName: String?
    var IDENTITY_POOL_ID: String?
    
    let APP_PUBLISH_TOPIC = "AppControl"
    let APP_SUBSCRIBE_CONNECT_TOPIC = "DeviceConnect"
    let APP_SUBSCRIBE_TOPIC = "DeviceControl"
    
    let APP_CONNECT_MESSAGE = "connected"
    let DEVICE_CONNECT_RESPONSE_MESSAGE = "yes"
    
    let APP_UPDATE_SENSOR_MESSAGE = "updateSensor"
    let DEVICE_UPDATE_SENSOR_RESPONSE_MESSAGE = "updated"
    
    let APP_CONTROL_LED_ON_MESSAGE = "ledOn"
    let DEVICE_CONTROL_LED_ON_RESPONSE_MESSAGE = "on"
    
    let APP_CONTROL_LED_OFF_MESSAGE = "ledOff"
    let DEVICE_CONTROL_LED_OFF_RESPONSE_MESSAGE = "off"
    
    let APP_CONTROL_OTA_MESSAGE = "confirmOTA"
    
    let IOT_CERT = "IoT Cert"
    
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
    
    @IBOutlet weak var btnUpdateShadow: UIButton!
    @IBOutlet weak var btnLight: UIButton!
    
    @IBOutlet weak var imageTemperature: UIImageView!
    @IBOutlet weak var imageHumidity: UIImageView!
    @IBOutlet weak var imageAmbientLight: UIImageView!
    @IBOutlet weak var imageAirQuality: UIImageView!
    @IBOutlet weak var imagePressure: UIImageView!
    @IBOutlet weak var imageProximity: UIImageView!
    @IBOutlet weak var imageButtonPress: UIImageView!
    @IBOutlet weak var imageBattery: UIImageView!
    
    @IBOutlet weak var labelTemperatureValue: UILabel!
    @IBOutlet weak var labelHumidityValue: UILabel!
    @IBOutlet weak var labelAmbientLightValue: UILabel!
    @IBOutlet weak var labelAirQualityValue: UILabel!
    @IBOutlet weak var labelGasValue: UILabel!
    @IBOutlet weak var labelPressureValue: UILabel!
    @IBOutlet weak var labelProximityValue: UILabel!
    @IBOutlet weak var labelXaxisValue: UILabel!
    @IBOutlet weak var labelYaxisValue: UILabel!
    @IBOutlet weak var labelZaxisValue: UILabel!
    @IBOutlet weak var labelMagneticValue: UILabel!
    @IBOutlet weak var labelBatteryValue: UILabel!
    
    @IBOutlet weak var labelUpdatedTime: UILabel!
    @IBOutlet weak var btnOtaUpdate: UIButton!
    
    var serverStatusBlinkTimer: Timer?
    var thingStatusBlinkTimer: Timer?
    var getShadowTimer: Timer?
    var connectServerTimer: Timer?
    var connectThingTimer: Timer?
    var updateShadowTimer: Timer?
    var controlLightTimer: Timer?
    
    var existOTAupdateFlag: Bool = false
    var OTAupdateProgressFlage: Bool = false
    
    var isServerConnected: Bool = false
    var isDeviceConnected: Bool = false
    
    var isTemperatureUpdated: Bool = false
    var isWakeupUpdated: Bool = false
    var isOtaUpdated: Bool = false
    
    var pastLightStateTimestamp: UInt = 0
    var pastTemperatureTimestamp: UInt = 0
    var pastWakeupTimestamp: UInt = 0
    var pastOtaTimestamp: UInt = 0
    
    var temperature: Float?
    var humidity: Float?
    var pressure: Float?
    var gaslock: Float?
    var ambient: Float?
    var magnetox: Float?
    var magnetoy: Float?
    var magnetoz: Float?
    var battery: Float?
    var wakeupnum: UInt?
    var ledonoff: UInt?
    var OTAupdate: UInt?
    var OTAresult: String?
    
    var temperatureValue: String?
    var hum_score: Int?
    var gas_score: Int?
    
    var updatingAlert: UIAlertController?
    var updateSuccessAlert: UIAlertController?
    var updateFailAlert: UIAlertController?
    var connectServerTimeoutAlert: UIAlertController?
    var connectThingTimeoutAlert: UIAlertController?
    var updateShadowTimeoutAlert: UIAlertController?
    var controlLightTimeoutAlert: UIAlertController?
    
    var objectNearby: Bool?
    var pressed: Bool?
    
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
                //self.activityIndicatorView.stopAnimating()
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
                                
                                self.labelSubscribeTopic.text = subTopic2
                                self.labelSubscribeMessage.text = String(subscribeValue)
                                
                                if subscribeValue.contains(self.DEVICE_CONTROL_LED_ON_RESPONSE_MESSAGE) {
                                    self.stopControlLightTimer()
                                    Utility.hideLoader(view: self.view)
                                    self.controlLightUI(lock: "false")  //on
                                } else if subscribeValue.contains(self.DEVICE_CONTROL_LED_OFF_RESPONSE_MESSAGE) {
                                    self.stopControlLightTimer()
                                    Utility.hideLoader(view: self.view)
                                    self.controlLightUI(lock: "true")  //off
                                } else if subscribeValue.contains(self.DEVICE_UPDATE_SENSOR_RESPONSE_MESSAGE) {
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
        //activityIndicatorView.startAnimating()
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
        
        appDelegate.awsIoTSensorVC = self
        
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
        
        
        self.btnUpdateShadow.layer.cornerRadius = 10
        self.btnUpdateShadow.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnUpdateShadow.layer.shadowOpacity = 1;
        self.btnUpdateShadow.layer.shadowRadius = 1;
        self.btnUpdateShadow.layer.shadowOffset = CGSize(width: 1, height: 4)
        
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
        
        self.imageTemperature.tintColor = UIColor.systemGray
        self.imageHumidity.tintColor = UIColor.systemGray
        self.imageAmbientLight.tintColor = UIColor.systemGray
        self.imageAirQuality.tintColor = UIColor.systemGray
        self.imagePressure.tintColor = UIColor.systemGray
        self.imageProximity.tintColor = UIColor.systemGray
        self.imageButtonPress.isHidden = true
        self.imageBattery.tintColor = UIColor.systemGray
        
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
            self.labelPublishMessage.text = self.APP_UPDATE_SENSOR_MESSAGE
            self.iotDataManager.publishString(self.APP_UPDATE_SENSOR_MESSAGE, onTopic:self.publishTopic!, qoS:.messageDeliveryAttemptedAtLeastOnce)
        }
    }
    
    @IBAction func onBtnLight(_ sender: UIButton) {
        if (self.isServerConnected && self.isDeviceConnected) {
            if self.ledonoff == 1 {  //current state : on
                Utility.showLoader(message: "Turn off the light ...", view: self.view)
                self.labelPublishTopic.text = self.publishTopic!
                self.labelPublishMessage.text = self.APP_CONTROL_LED_OFF_MESSAGE
                self.iotDataManager.publishString(self.APP_CONTROL_LED_OFF_MESSAGE, onTopic:self.publishTopic!, qoS:.messageDeliveryAttemptedAtLeastOnce)
                self.startControlLightTimer()
            } else if self.ledonoff == 0 {  //current state : off
                Utility.showLoader(message: "Turn on the light ...", view: self.view)
                self.labelPublishTopic.text = self.publishTopic!
                self.labelPublishMessage.text = self.APP_CONTROL_LED_ON_MESSAGE
                self.iotDataManager.publishString(self.APP_CONTROL_LED_ON_MESSAGE, onTopic:self.publishTopic!, qoS:.messageDeliveryAttemptedAtLeastOnce)
                self.startControlLightTimer()
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
    
    func controlLightUI(lock: String) {
        
        if lock == "true" {  //off
            if #available(iOS 13.0, *) {
                let image = UIImage(systemName: "lightbulb")?.withRenderingMode(.alwaysTemplate)
                self.btnLight.setImage(image, for: .normal)
                self.btnLight.tintColor = UIColor.systemGray
                self.btnLight.setTitle("OFF", for: .normal)
                self.btnLight.setTitleColor(UIColor.systemGray, for: .normal)
            } else {
                // Fallback on earlier versions
            }
        } else if lock == "false" {  //on
            if #available(iOS 13.0, *) {
                let image = UIImage(systemName: "lightbulb.fill")?.withRenderingMode(.alwaysTemplate)
                self.btnLight.setImage(image, for: .normal)
                self.btnLight.tintColor = UIColor.systemYellow
                self.btnLight.setTitle("ON", for: .normal)
                self.btnLight.setTitleColor(UIColor.systemYellow, for: .normal)
            } else {
                // Fallback on earlier versions
            }
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
                            if let meta_temperature = strMetaRreported["temperature"] as? [String:Any] {
                                if let meta_temperature_timestamp = meta_temperature["timestamp"] as? TimeInterval {
                                    let curTemperatureTimestamp: UInt = UInt(meta_temperature_timestamp)
                                    let meta_temperature_date = Date(timeIntervalSince1970: TimeInterval(meta_temperature_timestamp))
                                    let temperatureDate = DateFormatter()
                                    temperatureDate.dateFormat = "yyyy-MM-dd HH:mm:ss"
                                    NSLog("\(#fileID):\(#line) >> pastTemperatureTimestamp : \(self.pastTemperatureTimestamp)")
                                    NSLog("\(#fileID):\(#line) >> curTemperatureTimestamp : \(curTemperatureTimestamp)")
                                    if (self.pastTemperatureTimestamp != curTemperatureTimestamp) {
                                        self.isTemperatureUpdated = true
                                        DispatchQueue.main.async {
                                            self.labelUpdatedTime.text = temperatureDate.string(from:meta_temperature_date)
                                        }
                                    } else {
                                        self.isTemperatureUpdated = false
                                    }
                                    self.pastTemperatureTimestamp = curTemperatureTimestamp
                                }
                            }
                            
                            if let meta_wakeup = strMetaRreported["wakeupnum"] as? [String:Any] {
                                if let meta_wakeup_timestamp = meta_wakeup["timestamp"] as? TimeInterval {
                                    let curWakeupTimestamp: UInt = UInt(meta_wakeup_timestamp)
                                    let meta_wakeup_date = Date(timeIntervalSince1970: TimeInterval(meta_wakeup_timestamp))
                                    let wakeupDate = DateFormatter()
                                    wakeupDate.dateFormat = "yyyy-MM-dd HH:mm:ss"
                                    NSLog("\(#fileID):\(#line) >> pastWakeupTimestamp : \(self.pastWakeupTimestamp)")
                                    NSLog("\(#fileID):\(#line) >> curWakeupTimestamp : \(curWakeupTimestamp)")
                                    if (self.pastWakeupTimestamp != curWakeupTimestamp) {
                                        self.isWakeupUpdated = true
                                        DispatchQueue.main.async {
                                            self.labelUpdatedTime.text = wakeupDate.string(from:meta_wakeup_date)
                                        }
                                    } else {
                                        self.isWakeupUpdated = false
                                    }
                                    self.pastWakeupTimestamp = curWakeupTimestamp
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
                        
                        let states = strState as? [String:Any]
                        if let strRreported = states?["reported"] as? [String:Any] {
                            
                            if let temperature = strRreported["temperature"] as? Float {
                                NSLog("\(#fileID):\(#line) >> temperature = \(temperature)")
                                self.temperature = temperature
                            }
                            if let humidity = strRreported["humidity"] as? Float {
                                NSLog("\(#fileID):\(#line) >> humidity = \(humidity)")
                                self.humidity = humidity
                            }
                            if let pressure = strRreported["pressure"] as? Float {
                                NSLog("\(#fileID):\(#line) >> pressure = \(pressure)")
                                self.pressure = pressure
                            }
                            if let gaslock = strRreported["gaslock"] as? Float {
                                NSLog("\(#fileID):\(#line) >> gaslock = \(gaslock)")
                                self.gaslock = gaslock
                            }
                            if let ambient = strRreported["ambient"] as? Float {
                                NSLog("\(#fileID):\(#line) >> ambient = \(ambient)")
                                self.ambient = ambient
                            }
                            if let magnetox = strRreported["magnetox"] as? Float {
                                NSLog("\(#fileID):\(#line) >> magnetox = \(magnetox)")
                                self.magnetox = magnetox
                            }
                            if let magnetoy = strRreported["magnetoy"] as? Float {
                                NSLog("\(#fileID):\(#line) >> magnetoy = \(magnetoy)")
                                self.magnetoy = magnetoy
                            }
                            if let magnetoz = strRreported["magnetoz"] as? Float {
                                NSLog("\(#fileID):\(#line) >> magnetoz = \(magnetoz)")
                                self.magnetoz = magnetoz
                            }
                            if let battery = strRreported["battery"] as? Float {
                                NSLog("\(#fileID):\(#line) >> battery = \(battery)")
                                self.battery = battery
                            }
                            if let wakeupnum = strRreported["wakeupnum"] as? UInt {
                                NSLog("\(#fileID):\(#line) >> wakeupnum = \(wakeupnum)")
                                self.wakeupnum = wakeupnum
                            }
                            if let ledonoff = strRreported["ledonoff"] as? UInt {  //0:off, 1:on
                                NSLog("\(#fileID):\(#line) >> ledonoff = \(ledonoff)")
                                self.ledonoff = ledonoff
                            }
                            
                            if let OTAresult = strRreported["OTAresult"] as? String {
                                NSLog("\(#fileID):\(#line) >> OTAresult = \(OTAresult)")
                                self.OTAresult = OTAresult
                            }
                            
                            if let OTAupdate = strRreported["OTAupdate"] as? UInt {
                                NSLog("\(#fileID):\(#line) >> OTAupdate = \(OTAupdate)")
                                self.OTAupdate = OTAupdate
                            }
                            
                            
                            
                            NSLog("\(#fileID):\(#line) >> isTemperatureUpdated = \(self.isTemperatureUpdated)")
                            if (self.isTemperatureUpdated == true) {
                                DispatchQueue.main.async {
                                    self.updateTemperatureUI(temperature: self.temperature!)
                                    self.updateHumidityUI(humidity: self.humidity!)
                                    self.updatePressureUI(pressure: self.pressure!)
                                    self.updateAmbientUI(ambient: self.ambient!)
                                    self.updateAirQuailtyUI(humidity: self.humidity!, gaslock: self.gaslock!)
                                    self.updateMagnetoUI(x: self.magnetox!, y: self.magnetoy!, z: self.magnetoz!)
                                    self.updateBatteryUI(battery: self.battery!)
                                    self.updateLightUI(lenonoff: self.ledonoff!)
                                }
                            }
                            self.isTemperatureUpdated = false
                            
                            NSLog("\(#fileID):\(#line) >> isWakeupUpdated = \(self.isWakeupUpdated)")
                            if (self.isWakeupUpdated == true) {
                                DispatchQueue.main.async {
                                    self.updateProximityUI(wakeupnum: self.wakeupnum!)
                                    self.updateButtonUI(wakeupnum: self.wakeupnum!)
                                }
                            }
                            self.isWakeupUpdated = false
                            
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
                            }
                            self.isOtaUpdated = false
                            self.defaults.set(self.existOTAupdateFlag, forKey: "existOTAupdateFlagKey")
                            self.defaults.set(self.OTAupdateProgressFlage, forKey: "OTAupdateProgressFlageKey")
                            
                        }
                    }
                }
            }
        })
    }
    
    // MARK: - wakeupnum Init
    
    func wakeupnumInit(thingName: String) {
        let reqeust = "{\"state\":{\"reported\":{\"wakeupnum\":0}}}"
        iotDataManager.updateShadow(thingName, jsonString: reqeust)
    }
    
    // MARK: - Update Sensor UI
    
    func updateTemperatureUI(temperature: Float) {
        self.labelTemperatureValue.text = String(format: "%.1f", temperature)+" ℃"
    }
    
    func updateHumidityUI(humidity: Float) {
        self.labelHumidityValue.text = String(format:"%.1f", humidity) + " %"
    }
    
    func updatePressureUI(pressure: Float) {
        self.labelPressureValue.text = String(format: "%.1f", pressure / 100) + " hPa"
    }
    
    func updateAmbientUI(ambient: Float) {
        self.labelAmbientLightValue.text = String(format: "%.1f", ambient / 4) + " lux"
        let adjust: Int = min(Int(ambient) * 128 / 5000, 128);
        self.imageAmbientLight.tintColor = self.UIColorFromRGB(rgbValue: UInt(0xCFD8DC - 0x010000 * adjust - 0x000100 * (adjust / 2)))
    }
    
    func UIColorFromRGB(rgbValue: UInt) -> UIColor {
        return UIColor(
            red: CGFloat((rgbValue & 0xFF0000) >> 16) / 255.0,
            green: CGFloat((rgbValue & 0x00FF00) >> 8) / 255.0,
            blue: CGFloat(rgbValue & 0x0000FF) / 255.0,
            alpha: CGFloat(1.0)
        )
    }
    
    func updateAirQuailtyUI(humidity: Float, gaslock: Float) {
        
        var index: Int = 0
        index = self.calculateIAQindex(hum: humidity, gas: gaslock)!
        if (index >= 0) {
            self.imageAirQuality.tintColor = setAirQualityColor(index: index)!
            self.labelAirQualityValue.text = setAirQualityString(index: index)!
            self.labelGasValue.text = String(Int(gaslock)) + " Ohms"
        } else {
            self.imageAirQuality.tintColor = UIColor.systemGray
            self.labelAirQualityValue.text = "Unknown"
            self.labelGasValue.text = "Unknown"
        }
    }
    
    func setAirQualityColor(index: Int) -> UIColor? {
        
        var color: UIColor?
        
        if (index == 0) {
            color = UIColor(red: 56/255, green: 142/255, blue: 60/255, alpha: 1)
        } else if (index == 1) {
            color = UIColor(red: 251/255, green: 192/255, blue: 45/255, alpha: 1)
        } else if (index == 2) {
            color = UIColor(red: 245/255, green: 124/255, blue: 0/255, alpha: 1)
        } else if (index == 3) {
            color = UIColor(red: 211/255, green: 47/255, blue: 47/255, alpha: 1)
        } else if (index == 4) {
            color = UIColor(red: 123/255, green: 31/255, blue: 162/255, alpha: 1)
        } else if (index == 5) {
            color = UIColor(red: 0/255, green: 0/255, blue: 0/255, alpha: 1)
        }
        return color
    }
    
    func setAirQualityString(index: Int) -> String? {
        
        var string: String?
        
        if (index == 0) {
            string = "Good"
        } else if (index == 1) {
            string = "Average"
        } else if (index == 2) {
            string = "Little Bad"
        } else if (index == 3) {
            string = "Bad"
        } else if (index == 4) {
            string = "Very Bad"
        } else if (index == 5) {
            string = "Worst"
        }
        return string
    }
    
    func calculateIAQindex(hum: Float, gas: Float) -> Int? {
        var index: Int = -1
        let gas_baseline: Float = 100000.0
        let hum_baseline: Float = 40.0
        let hum_weighting: Float = 0.25
        let gas_offset: Float = gas_baseline - gas
        let hum_offset: Float = hum - hum_baseline
        
        if (hum_offset > 0) {
            self.hum_score! = Int(100 - hum_baseline - hum_offset)
            self.hum_score! /= (100 - Int(hum_baseline))
            self.hum_score! *= Int(hum_weighting * 100)
        } else {
            self.hum_score! = Int(hum_baseline + hum_offset)
            self.hum_score! /= Int(hum_baseline)
            self.hum_score! *= (Int(hum_weighting) * 100)
        }
        
        if (gas_offset > 0) {
            self.gas_score! = Int(gas / gas_baseline)
            self.gas_score! *= Int(100 - (hum_weighting * 100))
        } else {
            self.gas_score! = Int(100 - (hum_weighting * 100))
        }
        
        let airQualityScore: Int = self.hum_score! + self.gas_score!
        if (airQualityScore >= 301) {
            index = 5
        } else if (airQualityScore >= 201 && airQualityScore <= 300) {
            index = 4
        } else if (airQualityScore >= 176 && airQualityScore <= 200) {
            index = 3
        } else if (airQualityScore >= 151 && airQualityScore <= 175) {
            index = 2
        } else if (airQualityScore >= 51 && airQualityScore <= 150) {
            index = 1
        } else if (airQualityScore >= 0 && airQualityScore <= 50) {
            index = 0
        }
        
        return index
    }
    
    func updateMagnetoUI(x: Float, y: Float, z: Float) {
        if (abs(x) > 5160 || abs(y) > 5160 || abs(z) > 5160) {
            self.labelXaxisValue.text = "Unknown"
            self.labelYaxisValue.text = "Unknwon"
            self.labelZaxisValue.text = "Unknown"
        } else {
            let magnitude: Float = sqrt((x * x) + (y * y) + (z * z))
            self.labelXaxisValue.text = String(format: "%.2f", x)
            self.labelYaxisValue.text = String(format: "%.2f", y)
            self.labelZaxisValue.text = String(format: "%.2f", z)
            self.labelMagneticValue.text = String(format: "%.2f", magnitude) + " µT"
        }
    }
    
    func updateBatteryUI(battery: Float) {
        self.labelBatteryValue.text = "100 %"
    }
    
    func updateLightUI(lenonoff: UInt) {
        if ledonoff == 0 {  //off
            if #available(iOS 13.0, *) {
                let image = UIImage(systemName: "lightbulb")?.withRenderingMode(.alwaysTemplate)
                self.btnLight.setImage(image, for: .normal)
                self.btnLight.tintColor = UIColor.systemGray
                self.btnLight.setTitle("OFF", for: .normal)
                self.btnLight.setTitleColor(UIColor.systemGray, for: .normal)
            } else {
                // Fallback on earlier versions
            }
        } else if ledonoff == 1 {  //on
            if #available(iOS 13.0, *) {
                let image = UIImage(systemName: "lightbulb.fill")?.withRenderingMode(.alwaysTemplate)
                self.btnLight.setImage(image, for: .normal)
                self.btnLight.tintColor = UIColor.systemYellow
                self.btnLight.setTitle("ON", for: .normal)
                self.btnLight.setTitleColor(UIColor.systemYellow, for: .normal)
            } else {
                // Fallback on earlier versions
            }
        }
    }
    
    func updateProximityUI(wakeupnum: UInt) {
        if (wakeupnum == 2) {
            self.objectNearby = true
        } else {
            self.objectNearby = false
        }
        
        if (self.objectNearby == true) {
            self.labelProximityValue.text = "ON"
            self.imageProximity.tintColor = UIColor(red: 42/255, green: 40/255, blue: 157/255, alpha: 1)
        } else if (self.objectNearby == false) {
            self.labelProximityValue.text = "OFF"
            self.imageProximity.tintColor = UIColor.systemGray
        }
        usleep(1000000)
        if (wakeupnum == 2) {
            objectNearby = false
            self.wakeupnumInit(thingName: self.thingName!)
        }
    }
    
    func updateButtonUI(wakeupnum: UInt) {
        if (wakeupnum == 1) {
            self.pressed = true
        } else {
            self.pressed = false
        }
        if (self.pressed == true) {
            self.imageButtonPress.isHidden = false
        } else if (self.pressed == false) {
            self.imageButtonPress.isHidden = true
        }
        if (self.pressed == true) {
            AudioServicesPlaySystemSound(SystemSoundID(1000))
        }
        usleep(1000000)
        if (wakeupnum == 1) {
            self.pressed = false
            self.wakeupnumInit(thingName: self.thingName!)
        }
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
    
    // MARK: - Control Light Timeout Dialog
    
    func startControlLightTimer() {
        NSLog("\(#fileID):\(#line) >> startControlLightTimer()")
        if let timer = controlLightTimer {
            if !timer.isValid {
                controlLightTimer = Timer.scheduledTimer(timeInterval: 10.0, target: self, selector: #selector(showControlLightTimeoutDialog), userInfo: nil, repeats: true)
            }
        } else {
            controlLightTimer = Timer.scheduledTimer(timeInterval: 10.0, target: self, selector: #selector(showControlLightTimeoutDialog), userInfo: nil, repeats: true)
        }
    }
    
    @objc func showControlLightTimeoutDialog() {
        let alertMessage = """
            
            No response was received from the light.
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
        
        self.controlLightTimeoutAlert = UIAlertController.init(title: nil, message: nil, preferredStyle: UIAlertController.Style.alert)
        self.controlLightTimeoutAlert!.setValue(attributedMessageText, forKey: "attributedMessage")
        
        let cancel = UIAlertAction(title: "Cancel", style: .destructive, handler: { (action) -> Void in })
        self.controlLightTimeoutAlert!.addAction(cancel)
        
        let okAction = UIAlertAction(title: "OK", style: .default, handler: { (action) -> Void in
            if (self.isServerConnected && self.isDeviceConnected) {
                if self.ledonoff == 1 {  //current state : on
                    Utility.showLoader(message: "Turn off the light ...", view: self.view)
                    self.labelPublishTopic.text = self.publishTopic!
                    self.labelPublishMessage.text = self.APP_CONTROL_LED_OFF_MESSAGE
                    self.iotDataManager.publishString(self.APP_CONTROL_LED_OFF_MESSAGE, onTopic:self.publishTopic!, qoS:.messageDeliveryAttemptedAtLeastOnce)
                    self.startControlLightTimer()
                } else if self.ledonoff == 0 {  //current state : off
                    Utility.showLoader(message: "Turn on the light ...", view: self.view)
                    self.labelPublishTopic.text = self.publishTopic!
                    self.labelPublishMessage.text = self.APP_CONTROL_LED_ON_MESSAGE
                    self.iotDataManager.publishString(self.APP_CONTROL_LED_ON_MESSAGE, onTopic:self.publishTopic!, qoS:.messageDeliveryAttemptedAtLeastOnce)
                    self.startControlLightTimer()
                }
            }
        })
        self.controlLightTimeoutAlert!.addAction(okAction)
        
        
        present(self.controlLightTimeoutAlert!, animated: true, completion: nil)
    }
    
    func stopControlLightTimer() {
        NSLog("\(#fileID):\(#line) >> stopControlLightTimer()")
        if let timer = controlLightTimer {
            if (timer.isValid) {
                timer.invalidate()
                controlLightTimer = nil
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
            self.labelPublishMessage.text = self.APP_UPDATE_SENSOR_MESSAGE
            self.iotDataManager.publishString(self.APP_UPDATE_SENSOR_MESSAGE, onTopic:self.publishTopic!, qoS:.messageDeliveryAttemptedAtLeastOnce)
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
