//
//  ViewController.swift
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
import Foundation
import NetworkExtension
import SystemConfiguration.CaptiveNetwork
import Alamofire
import CocoaAsyncSocket
import SwiftyJSON
import CoreLocation

class SelectNetworkViewController: UIViewController, UITableViewDataSource, UITableViewDelegate, GCDAsyncSocketDelegate, CLLocationManagerDelegate {
    
    var wifiConfiguration: NEHotspotConfiguration?
    var mSocket: GCDAsyncSocket!
    let appDelegate = UIApplication.shared.delegate as! AppDelegate
    
    var da16200AddHiddenViewController: UIViewController?
    var setEnterpriseViewController: UIViewController?
    
    var mTimer:Timer?
    var timeCount = 0
    var mode: Int?
    var registeringAlert: UIAlertController?
    
    var isReconnected: Bool = false
    var isReceiveApResult: Bool = false
    var networkConnectAlert: UIAlertController?
    var passwordAlert : UIAlertController?
    
    var namePasswordAlert : UIAlertController?
    var isShowSetEnterprise: Bool = false
    
    //flag
    var isSecure =  true
    var isSocketChanged = false
    var isCompleted = false
    var isFirst = true
    var isUnder2_3_3 = false
    
    @IBOutlet weak var btnBack: UIButton!
    @IBOutlet weak var btnGetList: UIButton!
    @IBOutlet weak var btnRescan: UIButton!
    @IBOutlet weak var btnHiddenWiFi: UIButton!
    
    @IBOutlet weak var networkTable: UITableView!
    @IBOutlet weak var labelSelected: UILabel!
    @IBOutlet weak var imgSelected: UIImageView!
    @IBOutlet weak var switchTLS: UISwitch!
    @IBOutlet weak var labelSecure: UILabel!
    @IBOutlet weak var imgSecure: UIImageView!
    
    var APListIndex : [String] = []
    var APListSSID : [String] = []
    var APListSecurityString: [String] = []
    var APListSecurityInt : [Int] = []
    var APListSignal: [String] = []
    
    var selectedIndex: String!
    var selectedSSID: String!
    var selectedSecurityString: String!
    var selectedSecurityInt: Int!
    var socketType: String!
    var resultReboot: String!
    var resultHomeAp: String!
    
    struct Connectivity {
        static let sharedInstance = NetworkReachabilityManager()!
        static var isConnectedToInternet:Bool {
            return self.sharedInstance.isReachable
        }
    }
    
    var tryConnectRenesasDevice = false
    
    var locationManager = CLLocationManager()
    
    var currentNetworkInfos: Array<NetworkInfo>? {
        get {
            return SSID.fetchNetworkInfo()
        }
    }
    
    func  reupdateWiFi() {
        print(">> reupdateWiFi()")
        if let ssid = currentNetworkInfos?.first?.ssid {
            print(">> connected SSID: \(ssid)")
            
            if (ssid == "Dialog_DA16200" || ssid == "Renesas_DA16200" || ssid == "Renesas_IoT_WiFi") {
                self.mSocket = nil
                self.isReconnected = true
                self.isSocketChanged = false
                
                mSocket = GCDAsyncSocket(delegate: self, delegateQueue: DispatchQueue.main)
                usleep(1000000)
                if (mSocket != nil) {
                    if (self.isSecure) {
                        connectSocket(port: appDelegate.tlsHostPort)
                    } else {
                        connectSocket(port: appDelegate.tcpHostPort)
                    }
                }
            } else {
                Utility.hideLoader(view: self.view)
                self.showReconnectDialog()
            }
        } else {
            Utility.hideLoader(view: self.view)
            self.showReconnectDialog()
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        if status == .authorizedWhenInUse {
            reupdateWiFi()
        }
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        NSLog("\(#fileID):\(#line) >> viewDidLoad" )
        appDelegate.networkVC = self
        btnGetList.isEnabled = false
        btnRescan.isHidden = true
        
        self.btnHiddenWiFi.layer.cornerRadius = 3
        self.btnHiddenWiFi.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnHiddenWiFi.layer.shadowOpacity = 1;
        self.btnHiddenWiFi.layer.shadowRadius = 1;
        self.btnHiddenWiFi.layer.shadowOffset = CGSize(width: 1, height: 4)
        btnHiddenWiFi.isHidden = true
        
        networkTable.delegate = self
        networkTable.dataSource = self
        networkTable.backgroundColor = UIColor.white
        networkTable.layer.borderColor = UIColor(red: 42/255, green: 40/255, blue: 157/255, alpha: 1).cgColor
        networkTable.layer.borderWidth = 2.0
        networkTable.layer.cornerRadius = 10
        
        labelSelected.isHidden = true
        
        switchTLS.isHidden = false
        
        if (isSecure == true) {
            switchTLS.setOn(true, animated: false)
        } else {
            switchTLS.setOn(false, animated: false)
        }
        
        self.mSocket = GCDAsyncSocket(delegate: self, delegateQueue: DispatchQueue.main)
        
        usleep(1000000)
        
        if (self.mSocket != nil) {
            self.connectSocket(port: self.appDelegate.tlsHostPort)
            Utility.showLoader(message: "Connecting socket ...", view: self.view)
        }
    }
    
    override func viewWillAppear(_ animated: Bool) {
        NSLog("\(#fileID):\(#line) >> viewWillAppear")
    }
    
    override func viewDidAppear(_ animated: Bool) {
        NSLog("\(#fileID):\(#line) >> viewDidAppear")
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        NSLog("\(#fileID):\(#line) >> viewWillDisappear" )
        NSLog("\(#fileID):\(#line) >> mSocket disconnect")
        
        if (self.isShowSetEnterprise == false) {
            if (self.mSocket != nil) {
                if (self.mSocket.isConnected) {
                    NSLog("\(#fileID):\(#line) >> mSocket disconnect")
                    self.mSocket.disconnect()
                    self.mSocket = nil
                }
            }
        }
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
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let addHiddenVC = segue.destination as? DA16200AddHiddenViewController {
            addHiddenVC.delegate = self
            self.da16200AddHiddenViewController = addHiddenVC
        }
    }
    
    private func showAddHidden() {
        performSegue(withIdentifier: "addHiddenWiFiSegue", sender: self)
    }
    
    private func showSetEnterprise() {
        performSegue(withIdentifier: "setEnterpriseWiFiSegue", sender: self)
    }
    
    @IBAction func onBtnBack(_ sender: UIButton) {
        self.dismiss(animated: true)
        let connectDeviceVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "ConnectDeviceViewController")
        connectDeviceVC.modalPresentationStyle = .fullScreen
        present(connectDeviceVC, animated: true, completion: nil)
    }
    
    @IBAction func onBtnGetList(_ sender: UIButton) {
        
    }
    
    @IBAction func onBtnRescan(_ sender: UIButton) {
        if mSocket.isConnected {
            
            self.APListIndex = []
            self.APListSSID = []
            self.APListSecurityString = []
            self.APListSignal = []
            
            self.APListSecurityInt = []
            
            tcpSendReqRescan()
            
            self.btnRescan.isEnabled = false
            self.btnHiddenWiFi.isEnabled = false
            if (self.isSecure) {
                self.switchTLS.setOn(true, animated: false)
                self.switchTLS.isEnabled = false
            } else {
                switchTLS.setOn(false, animated: false)
                self.switchTLS.isEnabled = false
            }
            
        }
    }
    
    @IBAction func onBtnHiddenWiFi(_ sender: UIButton) {
        
    }
    
    
    @IBAction func onClickSwitch(_ sender: UISwitch) {
        isSecure = !isSecure
        isSocketChanged = true
        
        Utility.showLoader(message: "Switching socket ...", view: self.view)
        
        btnGetList.isEnabled = false
        btnGetList.setBackgroundColor(.gray, for: .disabled)
        btnRescan.isHidden = true
        btnHiddenWiFi.isHidden = true
        
        if (isSecure) {
            labelSecure.text = "TLS Secured"
            imgSecure.image = UIImage(named: "baseline_lock_green.png")
            tcpSendSocketType(type: 1)
        } else {
            labelSecure.text = "TLS Not Secured"
            imgSecure.image = UIImage(named: "baseline_lock_red.png")
            tcpSendSocketType(type: 0)
        }
    }
    
    // MARK: - TableView
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        
        return APListSSID.count
    }
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 60
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        let networkListcell = self.networkTable.dequeueReusableCell(withIdentifier: "NetworkListCell", for: indexPath) as! NetworkListCell
        networkListcell.labelSsid.text = APListSSID[indexPath.row]
        let signal: Int32? = Int32(APListSignal[indexPath.row])
        
        if (isUnder2_3_3 == true) {
            let security = APListSecurityString[indexPath.row]
            
            if security.contains("true") {
                
                if abs(signal!) >= 89  {
                    networkListcell.imageSignal.image = UIImage(named: "signal_wifi_1_bar_lock.png")
                } else if abs(signal!) >= 78 && abs(signal!) <= 88 {
                    networkListcell.imageSignal.image = UIImage(named: "signal_wifi_1_bar_lock.png")
                } else if abs(signal!) >= 67 && abs(signal!) <= 77 {
                    networkListcell.imageSignal.image = UIImage(named: "signal_wifi_2_bar_lock.png")
                } else if abs(signal!) >= 56 && abs(signal!) <= 66 {
                    networkListcell.imageSignal.image = UIImage(named: "signal_wifi_3_bar_lock.png")
                } else if abs(signal!) <= 55 {
                    networkListcell.imageSignal.image = UIImage(named: "signal_wifi_4_bar_lock.png")
                }
            } else {
                if abs(signal!) >= 89  {
                    networkListcell.imageSignal.image = UIImage(named: "signal_wifi_0_bar_open.png")
                } else if abs(signal!) >= 78 && abs(signal!) <= 88 {
                    networkListcell.imageSignal.image = UIImage(named: "signal_wifi_1_bar_open.png")
                } else if abs(signal!) >= 67 && abs(signal!) <= 77 {
                    networkListcell.imageSignal.image = UIImage(named: "signal_wifi_2_bar_open.png")
                } else if abs(signal!) >= 56 && abs(signal!) <= 66 {
                    networkListcell.imageSignal.image = UIImage(named: "signal_wifi_3_bar_open.png")
                } else if abs(signal!) <= 55 {
                    networkListcell.imageSignal.image = UIImage(named: "signal_wifi_4_bar_open.png")
                }
            }
            
        } else {
            let security = APListSecurityInt[indexPath.row]
            if (security == 0 || security == 5) {
                if abs(signal!) >= 89  {
                    networkListcell.imageSignal.image = UIImage(named: "signal_wifi_0_bar_open.png")
                } else if abs(signal!) >= 78 && abs(signal!) <= 88 {
                    networkListcell.imageSignal.image = UIImage(named: "signal_wifi_1_bar_open.png")
                } else if abs(signal!) >= 67 && abs(signal!) <= 77 {
                    networkListcell.imageSignal.image = UIImage(named: "signal_wifi_2_bar_open.png")
                } else if abs(signal!) >= 56 && abs(signal!) <= 66 {
                    networkListcell.imageSignal.image = UIImage(named: "signal_wifi_3_bar_open.png")
                } else if abs(signal!) <= 55 {
                    networkListcell.imageSignal.image = UIImage(named: "signal_wifi_4_bar_open.png")
                }
            } else {
                if abs(signal!) >= 89  {
                    networkListcell.imageSignal.image = UIImage(named: "signal_wifi_1_bar_lock.png")
                } else if abs(signal!) >= 78 && abs(signal!) <= 88 {
                    networkListcell.imageSignal.image = UIImage(named: "signal_wifi_1_bar_lock.png")
                } else if abs(signal!) >= 67 && abs(signal!) <= 77 {
                    networkListcell.imageSignal.image = UIImage(named: "signal_wifi_2_bar_lock.png")
                } else if abs(signal!) >= 56 && abs(signal!) <= 66 {
                    networkListcell.imageSignal.image = UIImage(named: "signal_wifi_3_bar_lock.png")
                } else if abs(signal!) <= 55 {
                    networkListcell.imageSignal.image = UIImage(named: "signal_wifi_4_bar_lock.png")
                }
            }
        }
        
        networkListcell.selectionStyle = .none
        
        return networkListcell
        
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        
        let ssid = APListSSID[indexPath.row]
        
        NSLog("\(#fileID):\(#line) >> Selected SSID = \(APListSSID[indexPath.row])")
        NSLog("\(#fileID):\(#line) >> Selected RSSI = \(APListSignal[indexPath.row])")
        
        if (isUnder2_3_3 == true) {
            NSLog("\(#fileID):\(#line) >> Selected Security = \(APListSecurityString[indexPath.row])")
            labelSelected.isHidden = false
            labelSelected.text? = "\(APListSSID[indexPath.row])"
            
            let signal: Int32? = Int32(APListSignal[indexPath.row])
            let security = APListSecurityString[indexPath.row]
            // The signal strength img icon
            if security.contains("true") {
                switch abs(signal!) {
                case 0...53:
                    imgSelected.image = UIImage(named: "signal_wifi_4_bar_lock.png")
                case 54...65:
                    imgSelected.image = UIImage(named: "signal_wifi_3_bar_lock.png")
                case 66...77:
                    imgSelected.image = UIImage(named: "signal_wifi_2_bar_lock.png")
                case 77...89:
                    imgSelected.image = UIImage(named: "signal_wifi_1_bar_lock.png")
                default:
                    imgSelected.image = UIImage(named: "signal_wifi_1_bar_lock.png")
                }
            } else {
                switch abs(signal!) {
                case 0...53:
                    imgSelected.image = UIImage(named: "signal_wifi_4_bar_open.png")
                case 54...65:
                    imgSelected.image = UIImage(named: "signal_wifi_3_bar_open.png")
                case 66...77:
                    imgSelected.image = UIImage(named: "signal_wifi_2_bar_open.png")
                case 77...89:
                    imgSelected.image = UIImage(named: "signal_wifi_1_bar_open.png")
                default:
                    imgSelected.image = UIImage(named: "signal_wifi_0_bar_open.png")
                }
            }
            
            appDelegate.apInfo.ssid = ssid
            appDelegate.apInfo.securityModeString = security
            
            if security.contains("true") {
                showPasswordAlert()
            } else {
                self.appDelegate.apInfo.isHiddenWiFi = 0
                tcpSendSSIDPW()
                
                self.mode = UserDefaults.standard.integer(forKey: "modeKey")
                if (self.mode == 0) {
                    
                }
            }
        } else {
            NSLog("\(#fileID):\(#line) >> Selected Security = \(APListSecurityInt[indexPath.row])")
            labelSelected.isHidden = false
            labelSelected.text? = "\(APListSSID[indexPath.row])"
            
            let signal: Int32? = Int32(APListSignal[indexPath.row])
            let security = APListSecurityInt[indexPath.row]
            // The signal strength img icon
            if (security == 0 || security == 5) {
                switch abs(signal!) {
                case 0...53:
                    imgSelected.image = UIImage(named: "signal_wifi_4_bar_open.png")
                case 54...65:
                    imgSelected.image = UIImage(named: "signal_wifi_3_bar_open.png")
                case 66...77:
                    imgSelected.image = UIImage(named: "signal_wifi_2_bar_open.png")
                case 77...89:
                    imgSelected.image = UIImage(named: "signal_wifi_1_bar_open.png")
                default:
                    imgSelected.image = UIImage(named: "signal_wifi_0_bar_open.png")
                }
            } else {
                switch abs(signal!) {
                case 0...53:
                    imgSelected.image = UIImage(named: "signal_wifi_4_bar_lock.png")
                case 54...65:
                    imgSelected.image = UIImage(named: "signal_wifi_3_bar_lock.png")
                case 66...77:
                    imgSelected.image = UIImage(named: "signal_wifi_2_bar_lock.png")
                case 77...89:
                    imgSelected.image = UIImage(named: "signal_wifi_1_bar_lock.png")
                default:
                    imgSelected.image = UIImage(named: "signal_wifi_1_bar_lock.png")
                }
            }
            
            appDelegate.apInfo.ssid = ssid
            appDelegate.apInfo.securityModeInt = security
            
            if (security == 0 || security == 5) {
                self.appDelegate.apInfo.isHiddenWiFi = 0
                self.tcpSendDPMSet()
                usleep(100000)
                self.tcpSendSSIDPW_1()
            } else if (security == 1 || security == 2 || security == 3 || security == 4 || security == 5 || security == 6 || security == 7) {
                showPasswordAlert()
            } else if (security == 8 || security == 9 || security == 10 || security == 11 || security == 12 || security == 13) {
                NSLog("\(#fileID):\(#line) >> Show SetEnterpriseViewController!")
                self.isShowSetEnterprise = true
                let setEnterpriseVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "SetEnterpriseViewController")
                setEnterpriseVC.modalPresentationStyle = .fullScreen
                present(setEnterpriseVC, animated: true, completion: nil)
            }
        }
        
    }
    
    // MARK: - Alert
    
    func showPasswordAlert() {
        
        let alertTitle = """
            Enter the passphrase
            """
        
        let titleParagraphStyle = NSMutableParagraphStyle()
        titleParagraphStyle.alignment = NSTextAlignment.center
        
        let attributedTitleText = NSMutableAttributedString(
            string: alertTitle,
            attributes: [
                NSAttributedString.Key.paragraphStyle: titleParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 20.0)
            ]
        )
        
        let alertMessage = """
            
            Network : \(appDelegate.apInfo.ssid)
            
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
        
        self.passwordAlert = UIAlertController.init(title: nil, message: "SSID : \(appDelegate.apInfo.ssid)", preferredStyle: .alert)
        
        self.passwordAlert?.setValue(attributedTitleText, forKey: "attributedTitle")
        self.passwordAlert?.setValue(attributedMessageText, forKey: "attributedMessage")
        let okAction = UIAlertAction(title: "Go!", style: .default, handler: { (action) -> Void in
            let textField = self.passwordAlert?.textFields![0]
            NSLog("\(#fileID):\(#line) >> \(textField!.text!)")
            self.appDelegate.apInfo.pw = textField!.text!
            NSLog("\(#fileID):\(#line) >> \(self.appDelegate.apInfo.pw)")
            
            self.appDelegate.apInfo.isHiddenWiFi = 0
            
            self.tcpSendDPMSet()
            usleep(100000)
            self.tcpSendSSIDPW_1()
        })
        okAction.isEnabled = false
        let cancel = UIAlertAction(title: "Cancel", style: .destructive, handler: { (action) -> Void in })
        self.passwordAlert?.addTextField { (textField: UITextField) in
            textField.keyboardAppearance = .dark
            textField.keyboardType = .default
            textField.autocorrectionType = .default
            textField.placeholder = "Enter the password"
            textField.clearButtonMode = .whileEditing
            textField.addTarget(self, action: #selector(self.alertTextFieldDidChange(_:)), for: UIControl.Event.editingChanged)
        }
        
        self.passwordAlert?.addAction(cancel)
        self.passwordAlert?.addAction(okAction)
        present(self.passwordAlert!, animated: true, completion: nil)
        
    }
    
    @objc func alertTextFieldDidChange(_ sender: UITextField) {
        self.passwordAlert?.actions[1].isEnabled = sender.text!.count > 7
    }
    
    func showNamePasswordAlert() {
        
        let alertTitle = """
            Enter the user name and passphrase
            """
        
        let titleParagraphStyle = NSMutableParagraphStyle()
        titleParagraphStyle.alignment = NSTextAlignment.center
        
        let attributedTitleText = NSMutableAttributedString(
            string: alertTitle,
            attributes: [
                NSAttributedString.Key.paragraphStyle: titleParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 20.0)
            ]
        )
        
        let alertMessage = """
            
            Network : \(appDelegate.apInfo.ssid)
            
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
        
        self.namePasswordAlert = UIAlertController.init(title: nil, message: "SSID : \(appDelegate.apInfo.ssid)", preferredStyle: .alert)
        
        self.namePasswordAlert?.setValue(attributedTitleText, forKey: "attributedTitle")
        self.namePasswordAlert?.setValue(attributedMessageText, forKey: "attributedMessage")
        let okAction = UIAlertAction(title: "OK", style: .default, handler: { (action) -> Void in
            let textField0 = self.passwordAlert?.textFields![0]
            NSLog("\(#fileID):\(#line) >> \(textField0!.text!)")
            self.appDelegate.apInfo.enterpriseID = textField0!.text!
            NSLog("\(#fileID):\(#line) >> \(self.appDelegate.apInfo.enterpriseID)")
            
            let textField1 = self.passwordAlert?.textFields![1]
            NSLog("\(#fileID):\(#line) >> \(textField1!.text!)")
            self.appDelegate.apInfo.enterprisePassword = textField1!.text!
            NSLog("\(#fileID):\(#line) >> \(self.appDelegate.apInfo.enterprisePassword)")
            
            self.appDelegate.apInfo.isHiddenWiFi = 0
            
            self.tcpSendDPMSet()
            usleep(100000)
            self.sendEnterpriseConfig()
        })
        okAction.isEnabled = false
        let cancel = UIAlertAction(title: "Cancel", style: .destructive, handler: { (action) -> Void in })
        
        self.namePasswordAlert?.addTextField { (textField: UITextField) in
            textField.keyboardAppearance = .dark
            textField.keyboardType = .default
            textField.autocorrectionType = .default
            textField.placeholder = "Enter the user name"
            textField.clearButtonMode = .whileEditing
            
        }
        
        self.namePasswordAlert?.addTextField { (textField: UITextField) in
            textField.keyboardAppearance = .dark
            textField.keyboardType = .default
            textField.autocorrectionType = .default
            textField.placeholder = "Enter the password"
            textField.clearButtonMode = .whileEditing
            textField.addTarget(self, action: #selector(self.alertTextFieldDidChange1(_:)), for: UIControl.Event.editingChanged)
        }
        
        self.namePasswordAlert?.addAction(cancel)
        self.namePasswordAlert?.addAction(okAction)
        present(self.namePasswordAlert!, animated: true, completion: nil)
        
    }
    
    @objc func alertTextFieldDidChange1(_ sender: UITextField) {
        self.namePasswordAlert?.actions[1].isEnabled = sender.text!.count > 0
    }
    
    
    func showSucessAlert() {
        
        let alertTitle = "Success"
        
        let titleParagraphStyle = NSMutableParagraphStyle()
        titleParagraphStyle.alignment = NSTextAlignment.center
        
        let attributedTitleText = NSMutableAttributedString(
            string: alertTitle,
            attributes: [
                NSAttributedString.Key.paragraphStyle: titleParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 20.0)
            ]
        )
        
        let alertMessage = """
            If you entered the correct passphrase,
            your device should now be connected.
            Please check.
            If it is not connected,
            please run this application again
            and re-enter the correct passphrase.
            
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
            self.isCompleted = true
            
            self.removeConfigDA16200()
            
            let mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "MainViewController")
            mainVC.modalPresentationStyle = .fullScreen
            self.present(mainVC, animated: true, completion: nil)
            
        })
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
    
    func showUnsucessAlert() {
        
        let alertTitle = ""
        
        let titleParagraphStyle = NSMutableParagraphStyle()
        titleParagraphStyle.alignment = NSTextAlignment.center
        
        let attributedTitleText = NSMutableAttributedString(
            string: alertTitle,
            attributes: [
                NSAttributedString.Key.paragraphStyle: titleParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 20.0)
            ]
        )
        
        let alertMessage = """
            Provisioning unsuccessful,
            please try again.
            
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
            
            let DA16200mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "DA16200MainViewController")
            DA16200mainVC.modalPresentationStyle = .fullScreen
            self.present(DA16200mainVC, animated: true, completion: nil)
            
        })
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
    
    func showSocketDisconnectAlert() {
        
        let alertTitle = ""
        
        let titleParagraphStyle = NSMutableParagraphStyle()
        titleParagraphStyle.alignment = NSTextAlignment.center
        
        let attributedTitleText = NSMutableAttributedString(
            string: alertTitle,
            attributes: [
                NSAttributedString.Key.paragraphStyle: titleParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 20.0)
            ]
        )
        
        let alertMessage = """
            Socket is not connected.
            Please retry.
            
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
            
            let DA16200mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "DA16200MainViewController")
            DA16200mainVC.modalPresentationStyle = .fullScreen
            self.present(DA16200mainVC, animated: true, completion: nil)
            
        })
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
    
    func showAzureDeviceFailDialog() {
        let alertTitle = "Error"
        
        let titleParagraphStyle = NSMutableParagraphStyle()
        titleParagraphStyle.alignment = NSTextAlignment.center
        
        let attributedTitleText = NSMutableAttributedString(
            string: alertTitle,
            attributes: [
                NSAttributedString.Key.paragraphStyle: titleParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 20.0)
            ]
        )
        
        let alertMessage = """
            
            The device information is corrupted.
            Please check the Azure-related information stored on the device
            and run provisioning again.
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
            let DA16200mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "DA16200MainViewController")
            DA16200mainVC.modalPresentationStyle = .fullScreen
            self.present(DA16200mainVC, animated: true, completion: nil)
        })
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
    
    
    func showRegisteringAlert() {
        
        NSLog("\(#fileID):\(#line) >> showRegisteringAlert()")
        let alertTitle = "Registering device"
        
        let titleParagraphStyle = NSMutableParagraphStyle()
        titleParagraphStyle.alignment = NSTextAlignment.center
        
        let attributedTitleText = NSMutableAttributedString(
            string: alertTitle,
            attributes: [
                NSAttributedString.Key.paragraphStyle: titleParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 20.0)
            ]
        )
        
        let alertMessage = """
            The device is being registered with the AWS server.
            It takes about 60 seconds.
            
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
        
        registeringAlert = UIAlertController.init(title: nil, message: nil, preferredStyle: UIAlertController.Style.alert)
        registeringAlert!.setValue(attributedTitleText, forKey: "attributedTitle")
        registeringAlert!.setValue(attributedMessageText, forKey: "attributedMessage")
        
        let loadingIndicator: UIActivityIndicatorView = UIActivityIndicatorView(frame: CGRect(x: 10, y: 5, width: 50, height: 50)) as UIActivityIndicatorView
        loadingIndicator.hidesWhenStopped = true
        loadingIndicator.startAnimating()
        registeringAlert!.view.addSubview(loadingIndicator)
        
        present(registeringAlert!, animated: true, completion: nil)
        
    }
    
    func showNetworkConnectDialog_1() {
        
        NSLog("\(#fileID):\(#line) >> showNetworkConnectDialog()")
        let alertTitle = "Setting up the device"
        
        let titleParagraphStyle = NSMutableParagraphStyle()
        titleParagraphStyle.alignment = NSTextAlignment.center
        
        let attributedTitleText = NSMutableAttributedString(
            string: alertTitle,
            attributes: [
                NSAttributedString.Key.paragraphStyle: titleParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 20.0)
            ]
        )
        
        let alertMessage = """
            Setting SSID and password ...
            
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
        
        networkConnectAlert = UIAlertController.init(title: nil, message: nil, preferredStyle: UIAlertController.Style.alert)
        networkConnectAlert!.setValue(attributedTitleText, forKey: "attributedTitle")
        networkConnectAlert!.setValue(attributedMessageText, forKey: "attributedMessage")
        
        let loadingIndicator: UIActivityIndicatorView = UIActivityIndicatorView(frame: CGRect(x: 10, y: 5, width: 50, height: 50)) as UIActivityIndicatorView
        loadingIndicator.hidesWhenStopped = true
        //loadingIndicator.activityIndicatorViewStyle = UIActivityIndicatorViewStyle.gray
        loadingIndicator.startAnimating()
        networkConnectAlert!.view.addSubview(loadingIndicator)
        
        present(networkConnectAlert!, animated: true, completion: nil)
        
    }
    
    func showReconnectDialog() {
        
        let alertTitle = "Lost Wi-Fi connection to device"
        
        let titleParagraphStyle = NSMutableParagraphStyle()
        titleParagraphStyle.alignment = NSTextAlignment.center
        
        let attributedTitleText = NSMutableAttributedString(
            string: alertTitle,
            attributes: [
                NSAttributedString.Key.paragraphStyle: titleParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 20.0)
            ]
        )
        
        let alertMessage = """
            
            The connection was lost becasue the device's Wi-Fi channnel changed.
            You will need to reconnect to your device.
            
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
            self.mSocket = nil
            self.isReconnected = true
            self.isSocketChanged = false
            self.connectToDialogDevice()
            
        })
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
    
    func showAuthFailAlert() {
        let alertTitle = "Authentication problem"
        
        let titleParagraphStyle = NSMutableParagraphStyle()
        titleParagraphStyle.alignment = NSTextAlignment.center
        
        let attributedTitleText = NSMutableAttributedString(
            string: alertTitle,
            attributes: [
                NSAttributedString.Key.paragraphStyle: titleParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 20.0)
            ]
        )
        
        let alertMessage = """
            
            Authentication failed to connect to the AP.
            Check your password entered correctly and try again.
            
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
            self.isReceiveApResult = false
            self.appDelegate.apInfo.pw = ""

            switch self.appDelegate.apInfo.securityModeInt {
            case 1...7 : self.showPasswordAlert()
                
            case 8...13 :
                
                self.isReceiveApResult = true
                self.isCompleted = true
                self.isShowSetEnterprise = false
                
                self.appDelegate.apInfo.ssid = ""
                self.appDelegate.apInfo.pw = ""
                self.appDelegate.apInfo.securityModeInt = -1
                self.appDelegate.apInfo.securityModeString = ""
                
                if (self.mSocket != nil) {
                    if (self.mSocket.isConnected) {
                        NSLog("\(#fileID):\(#line) >> mSocket disconnect")
                        self.mSocket.disconnect()
                        self.mSocket = nil
                    }
                }
                
                let mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "MainViewController")
                mainVC.modalPresentationStyle = .fullScreen
                self.present(mainVC, animated: true, completion: nil)
                
                
            default : self.showPasswordAlert()
            }
        })
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
    
    func showNoInternetAlert() {
        let alertTitle = "No internet"
        
        let titleParagraphStyle = NSMutableParagraphStyle()
        titleParagraphStyle.alignment = NSTextAlignment.center
        
        let attributedTitleText = NSMutableAttributedString(
            string: alertTitle,
            attributes: [
                NSAttributedString.Key.paragraphStyle: titleParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 20.0)
            ]
        )
        
        let alertMessage = """
            
            The Wi-Fi AP is not sure of the internet connection.
            Please set the device to provisioning mode and try again.
            
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
            self.isReceiveApResult = true
            
            self.appDelegate.apInfo.ssid = ""
            self.appDelegate.apInfo.pw = ""
            self.appDelegate.apInfo.securityModeInt = -1
            self.appDelegate.apInfo.securityModeString = ""
            
            self.isCompleted = true
            
            let mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "MainViewController")
            mainVC.modalPresentationStyle = .fullScreen
            self.present(mainVC, animated: true, completion: nil)
            
        })
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
    
    func showUnknownErrorAlert() {
        let alertTitle = "Unknown error"
        
        let titleParagraphStyle = NSMutableParagraphStyle()
        titleParagraphStyle.alignment = NSTextAlignment.center
        
        let attributedTitleText = NSMutableAttributedString(
            string: alertTitle,
            attributes: [
                NSAttributedString.Key.paragraphStyle: titleParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 20.0)
            ]
        )
        
        let alertMessage = """
            
            An unknown error has occurred.
            Please set the device to provisioning mode and try again.
            
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
            self.isReceiveApResult = true
            
            self.appDelegate.apInfo.ssid = ""
            self.appDelegate.apInfo.pw = ""
            self.appDelegate.apInfo.securityModeInt = -1
            self.appDelegate.apInfo.securityModeString = ""
            
            self.isCompleted = true
            
            let mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "MainViewController")
            mainVC.modalPresentationStyle = .fullScreen
            self.present(mainVC, animated: true, completion: nil)
            
        })
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
    
    //MARK: - Wi-Fi connect
    
    func connectToDialogDevice() {
        
        Utility.showLoader(message: "Conntecting to Renesas device ...", view: view)
        
        wifiConfiguration = NEHotspotConfiguration(
            ssid: appDelegate.dialogDeviceSSID,
            passphrase: appDelegate.devicePwd,
            isWEP: false)
        
        wifiConfiguration?.joinOnce = true
        
        NEHotspotConfigurationManager.shared.apply(wifiConfiguration!) { error in
            
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
    
    func connectToRenesasDevice() {
        
        self.tryConnectRenesasDevice = true
        Utility.showLoader(message: "Conntecting to Renesas device ...", view: view)
        
        wifiConfiguration = NEHotspotConfiguration(
            ssid: appDelegate.renesasDeviceSSID,
            passphrase: appDelegate.devicePwd,
            isWEP: false)
        
        wifiConfiguration?.joinOnce = true
        
        NEHotspotConfigurationManager.shared.apply(wifiConfiguration!) { error in
            
            Utility.hideLoader(view: self.view)
            
            if error == nil {
                
                NSLog("\(#fileID):\(#line) >> Connected 4")
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
    
    func connectToRenesasIoTWiFiDevice() {
        
        self.tryConnectRenesasDevice = true
        Utility.showLoader(message: "Conntecting to Renesas device ...", view: view)
        
        wifiConfiguration = NEHotspotConfiguration(
            ssid: appDelegate.renesasIoTWiFiSSID,
            passphrase: appDelegate.devicePwd,
            isWEP: false)
        
        wifiConfiguration?.joinOnce = true
        
        NEHotspotConfigurationManager.shared.apply(wifiConfiguration!) { error in
            
            Utility.hideLoader(view: self.view)
            
            if error == nil {
                
                
                NSLog("\(#fileID):\(#line) >> Connected 7")
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
                        NSLog("\(#fileID):\(#line) >> Connected 8")
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
                        NSLog("\(#fileID):\(#line) >> Not Connected 9")
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
    
    func  updateWiFi() {
        
        if let ssid = currentNetworkInfos?.first?.ssid {
            NSLog("\(#fileID):\(#line) >> connected SSID: \(ssid)")
            
            if ssid == "Renesas_IoT_WiFi" {
                print("=> updateWiFi 1")
                usleep(3000000)
                appDelegate.isRenesasDevice = true
                
                mSocket = GCDAsyncSocket(delegate: self, delegateQueue: DispatchQueue.main)
                if (mSocket != nil) {
                    connectSocket(port: appDelegate.tlsHostPort)
                    Utility.showLoader(message: "Connecting socket ...", view: view)
                }
            } else if ssid == "Dialog_DA16200" {
                print("=> updateWiFi 2")
                
                usleep(3000000)
                appDelegate.isDialogDevice = true
                mSocket = GCDAsyncSocket(delegate: self, delegateQueue: DispatchQueue.main)
                if (mSocket != nil) {
                    connectSocket(port: self.appDelegate.tlsHostPort)
                    Utility.showLoader(message: "Connecting socket ...", view: view)
                }
                
            } else if ssid == "Renesas_DA16200" {
                print("=> updateWiFi 3")
                usleep(3000000)
                appDelegate.isRenesasDevice = true
                mSocket = GCDAsyncSocket(delegate: self, delegateQueue: DispatchQueue.main)
                if (mSocket != nil) {
                    connectSocket(port: appDelegate.tlsHostPort)
                    Utility.showLoader(message: "Connecting socket ...", view: view)
                }
            }
            else {
                if (tryConnectRenesasDevice ==  false) {
                    print("=> updateWiFi 5")
                    Utility.hideLoader(view: self.view)
                    self.connectToRenesasDevice()
                } else {
                    print("=> updateWiFi 6")
                }
            }
        }
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
    
    // MARK: - TCP Connection
    
    
    func connectSocket(port: UInt16) {
        
        NSLog("\(#fileID):\(#line) >> ==> connectSocket()\n")
        
        if (appDelegate.isDialogDevice == true) {
            wifiConfiguration = NEHotspotConfiguration(
                ssid: appDelegate.dialogDeviceSSID,
                passphrase: appDelegate.devicePwd,
                isWEP: false)
            wifiConfiguration?.joinOnce = true
        } else if (appDelegate.isRenesasDevice == true) {
            wifiConfiguration = NEHotspotConfiguration(
                ssid: appDelegate.renesasDeviceSSID,
                passphrase: appDelegate.devicePwd,
                isWEP: false)
            wifiConfiguration?.joinOnce = true
        }
        
        
        do {
            try self.mSocket.connect(toHost: appDelegate.hostAddress, onPort: port, viaInterface: "en0", withTimeout: -1)
        } catch let error {
            NSLog("\(#fileID):\(#line) >> socket error : \(error)")
        }
    }
    
    public func socket(_ sock: GCDAsyncSocket, didAcceptNewSocket newSocket: GCDAsyncSocket) {
        NSLog("\(#fileID):\(#line) >> didAcceptNewSocket!\n")
        mSocket = newSocket
    }
    
    public func socket(_ socket: GCDAsyncSocket, didConnectToHost host: String, port p:UInt16) {
        NSLog("\(#fileID):\(#line) >> didConnectToHost!\n")
        isFirst = false
        
        if (isSecure) {
            let sslSettings = [
                GCDAsyncSocketManuallyEvaluateTrust: NSNumber(value: true),
                kCFStreamSSLPeerName: "www.apple.com"
            ] as? [CFString : Any]
            mSocket.startTLS(sslSettings as? [String:NSObject])
            
        } else {
            Utility.hideLoader(view: view)
            btnGetList.isEnabled = true
            btnGetList.setBackgroundColor(UIColor(red: 40/255, green: 42/255, blue: 157/255, alpha: 1), for: .normal)
            btnRescan.isHidden = false
            btnHiddenWiFi.isHidden = false
            
            if (self.isReconnected == true) {
                tcpSendReqApResult()
            } else {
                tcpSendConnected()
            }
            
        }
    }
    
    public func socket(_ sock: GCDAsyncSocket, didWriteDataWithTag tag: Int) {
        NSLog("\(#fileID):\(#line) >> didWriteData");
    }
    
    public func socket(_ sock: GCDAsyncSocket, didReceive trust: SecTrust, completionHandler: @escaping (Bool) -> Void) {
        NSLog("\(#fileID):\(#line) >> didReceiveData")
        
        completionHandler(true)
        
    }
    
    public func socket(_ sock: GCDAsyncSocket, didRead: Data, withTag tag:CLong){
        NSLog("\(#fileID):\(#line) >> didRead!");
        
        Utility.hideLoader(view: view)
        do {
            let json = try JSON(data: didRead)
            NSLog("\(#fileID):\(#line) >> incoming message: \(json))")
            
            if json["SOCKET_TYPE"].exists() {
                self.socketType = json["SOCKET_TYPE"].stringValue
                NSLog("\(#fileID):\(#line) >> socketType : \(String(describing: socketType!))")
                if self.socketType == "0" || self.socketType == "1" {
                    mSocket.disconnect()
                }
            }
            
            if json["thingName"].exists() {
                let thingName = json["thingName"].stringValue
                NSLog("\(#fileID):\(#line) >> thingname : \(thingName)")
                UserDefaults.standard.set(thingName, forKey: "thingNameKey")
            }
            
            if json["azureConString"].exists() {
                let azureConString = json["azureConString"].stringValue
                NSLog("\(#fileID):\(#line) >> azureConString : \(azureConString)")
                UserDefaults.standard.set(azureConString, forKey: "azureConStringKey")
            }
            
            if json["mode"].exists() {
                let mode:Int? = json["mode"].intValue
                NSLog("\(#fileID):\(#line) >> mode : \(mode!)")
                UserDefaults.standard.set(mode!, forKey: "modeKey")
            }
            
            if json["APList"].exists() {
                self.stopTimer1()
                self.APListIndex = json["APList"].arrayValue.map {$0["index"].stringValue}
                NSLog("\(#fileID):\(#line) >> APListIndex = \(self.APListIndex)")
                self.APListSSID = json["APList"].arrayValue.map {$0["SSID"].stringValue}
                NSLog("\(#fileID):\(#line) >> APListSSID = \(self.APListSSID)")
                
                if json["APList"][0]["secMode"].exists() {
                    self.APListSecurityString = json["APList"].arrayValue.map {$0["secMode"].stringValue}
                    NSLog("\(#fileID):\(#line) >> APListSecurityString = \(self.APListSecurityString)")
                    isUnder2_3_3 = true
                } else if json["APList"][0]["securityType"].exists() {
                    self.APListSecurityInt = json["APList"].arrayValue.map {$0["securityType"].intValue}
                    NSLog("\(#fileID):\(#line) >> APListSecurityInt = \(self.APListSecurityInt)")
                    isUnder2_3_3 = false
                }
                
                self.APListSignal = json["APList"].arrayValue.map {$0["signal"].stringValue}
                NSLog("\(#fileID):\(#line) >> APListSignal = \(self.APListSignal)")
                
                self.networkTable.reloadData()
                Utility.hideLoader(view: view)
                
                self.btnRescan.isEnabled = true
                self.btnHiddenWiFi.isEnabled = true
                if (self.isSecure) {
                    self.switchTLS.setOn(true, animated: false)
                    self.switchTLS.isEnabled = true
                } else {
                    switchTLS.setOn(false, animated: false)
                    self.switchTLS.isEnabled = true
                }
            }
            
            if json["RESULT_REBOOT"].exists() {
                self.resultReboot = json["RESULT_REBOOT"].stringValue
                NSLog("\(#fileID):\(#line) >> resultReboot = \(String(describing: resultReboot!))")
                if self.resultReboot == "0" {
                    self.mode = UserDefaults.standard.integer(forKey: "modeKey")
                    print("self.mode = \(String(describing: self.mode!))")
                    if (self.mode == 2) {
                        DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 4) {
                            self.tcpSendReqApResult()
                        }
                    } else
                    if ((self.mode == 12) || (self.mode == 13)) {
                        DispatchQueue.main.async {
                            self.showRegisteringAlert()
                            DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 60) {
                                //Utility.hideLoader(view: self.view)
                                self.registeringAlert?.dismiss(animated: true, completion: nil)
                                usleep(1000)
                                DispatchQueue.main.async {
                                    self.isCompleted = true
                                    self.showSucessAlert()
                                }
                            }
                        }
                        
                    } else {
                        isCompleted = true
                        self.showSucessAlert()
                    }
                    
                }
            }
            
            if json["RESULT_HOMEAP"].exists() {
                self.resultHomeAp = json["RESULT_HOMEAP"].stringValue
                NSLog("\(#fileID):\(#line) >> resultHomeAp = \(String(describing: resultHomeAp!))")
                if (self.resultHomeAp == "0") {  //connecting
                    self.isReceiveApResult = false
                    
                } else if (self.resultHomeAp == "1") {  //success
                    Utility.hideLoader(view: self.view)
                    self.isReceiveApResult = true
                    self.showSucessAlert()
                    
                    
                } else if (self.resultHomeAp == "2") {  //authentication fail
                    print(">> authentication fail")
                    Utility.hideLoader(view: self.view)
                    self.isReceiveApResult = true
                    self.showAuthFailAlert()
                    
                } else if (self.resultHomeAp == "3") {  //no internet error
                    print(">> no internet error")
                    Utility.hideLoader(view: self.view)
                    self.isReceiveApResult = true
                    self.showNoInternetAlert()
                    
                } else if (self.resultHomeAp == "4") {  //unknown error
                    print(">> unknown error")
                    Utility.hideLoader(view: self.view)
                    self.isReceiveApResult = true
                    self.showUnknownErrorAlert()
                    
                } else if (self.resultHomeAp == "10") {
                    self.isReceiveApResult = false
                    usleep(1000000)
                    self.tcpSendReqApResult()
                }
            }
            
        } catch let error {
            NSLog("\(#fileID):\(#line) >> \(error)")
            self.showAzureDeviceFailDialog()
        }
        
    }
    
    public func socketDidSecure(_ sock: GCDAsyncSocket) {
        NSLog("\(#fileID):\(#line) >> socketDidSecure!")
        isFirst = false
        
        Utility.hideLoader(view: view)
        
        btnGetList.isEnabled = true
        btnGetList.setBackgroundColor(UIColor(red: 42/255, green: 40/255, blue: 157/255, alpha: 1), for: .normal)
        btnRescan.isHidden = false
        btnHiddenWiFi.isHidden = false
        
        //[[for concurrent
        if (self.isReconnected == true) {
            tcpSendReqApResult()
        } else {
            tcpSendConnected()
        }
        //]]
    }
    
    
    public func socketDidDisconnect(_ sock: GCDAsyncSocket, withError err: Error?) {
        NSLog("\(#fileID):\(#line) >> didDisconnect!")
        NSLog("\(#fileID):\(#line) >> isSocketChagned = \(isSocketChanged)")
        btnGetList.isEnabled = false
        btnRescan.isHidden = true
        btnHiddenWiFi.isHidden = true
        
        if (isSocketChanged) {
            DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 5) {
                self.mSocket = GCDAsyncSocket(delegate: self, delegateQueue: DispatchQueue.main)
                usleep(100000)
                NSLog("\(#fileID):\(#line) >> isSecure = \(self.isSecure)")
                if (self.isSecure) {
                    self.connectSocket(port: self.appDelegate.tlsHostPort)
                } else {
                    self.connectSocket(port: self.appDelegate.tcpHostPort)
                }
                
                self.isSocketChanged = false
            }
        } else {
            NSLog("\(#fileID):\(#line) >> isCompleted = \(isCompleted)")
            NSLog("\(#fileID):\(#line) >> isFirst = \(isFirst)")
            
            if (!isCompleted && isFirst) {
                
                if (mSocket != nil) {
                    if (self.isSecure) {
                        connectSocket(port: appDelegate.tlsHostPort)
                    } else {
                        connectSocket(port: appDelegate.tcpHostPort)
                    }
                }
                
            } else if (!isCompleted && !isFirst) {
                self.mode = UserDefaults.standard.integer(forKey: "modeKey")
                NSLog("\(#fileID):\(#line) >> self.mode = \(String(describing: self.mode!))")
                if (self.mode! == 2) {
                    
                    DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 5) {
                        if #available(iOS 13.0, *) {
                            let status = CLLocationManager.authorizationStatus()
                            NSLog("\(#fileID):\(#line) >> status = \(String(describing: status))")
                            if status == .authorizedWhenInUse {
                                self.reupdateWiFi()
                            } else {
                                self.locationManager.delegate = self
                                self.locationManager.requestWhenInUseAuthorization()
                            }
                        } else {
                            self.reupdateWiFi()
                        }
                    }
                } else {
                    NSLog("\(#fileID):\(#line) >> Socket is disconnected")
                }
            }
            
            if let error = err {
                NSLog("\(#fileID):\(#line) >> Will disconnect with error: \(error)")
                
                if (error.localizedDescription.contains("Connection refused")) {
                    
                    self.mSocket = nil
                    self.isSocketChanged = false;
                    self.mSocket = GCDAsyncSocket(delegate: self, delegateQueue: DispatchQueue.main)
                    if (self.mSocket != nil) {
                        if (self.isSecure) {
                            self.connectSocket(port: self.appDelegate.tlsHostPort1)
                            NSLog("\(#fileID):\(#line) >> self.connectSocket(port: self.appDelegate.tlsHostPort1)")
                        } else {
                            self.connectSocket(port: self.appDelegate.tcpHostPort1)
                            NSLog("\(#fileID):\(#line) >> self.connectSocket(port: self.appDelegate.tcpHostPort1)")
                        }
                    }
                }
                else {
                    
                }
                
                if (self.mode == 2) {
                    
                } else {
                    
                }
                
            } else {
                NSLog("\(#fileID):\(#line) >> Success disconnect")
            }
        }
    }
    
    
    //MARK: - Send TCP
    
    func tcpSendSocketType(type: Int) {
        NSLog("\(#fileID):\(#line) >> tcpSendSocketType()\n")
        let cmdSocketType = "{\"msgType\":6,\"SOCKET_TYPE\":\(type)}"
        let data = cmdSocketType.data(using: String.Encoding.utf8)!
        self.mSocket.write(data, withTimeout:-1, tag: 0)
        self.mSocket.readData(withTimeout: -1, tag: 0)
    }
    
    func tcpSendConnected() {
        NSLog("\(#fileID):\(#line) >> tcpSendConnected()\n")
        Utility.showLoader(message: "Scanning Wi-Fi network...", view: view)
        let cmdConnected: String = "{\"msgType\":0,\"CONNECTED\":0}"
        let data = cmdConnected.data(using: String.Encoding.utf8)!
        self.mSocket.write(data, withTimeout:-1, tag: 0)
        self.mSocket.readData(withTimeout: -1, tag: 0)
        self.startTimer1()
    }
    
    func tcpSendReqRescan() {
        NSLog("\(#fileID):\(#line) >> tcpSendReqRescan()\n")
        Utility.showLoader(message: "Scanning Wi-Fi network...", view: view)
        let cmdReqRescan: String = "{\"msgType\":3,\"REQ_RESCAN\":0}"
        let data = cmdReqRescan.data(using: String.Encoding.utf8)!
        self.mSocket.write(data, withTimeout:-1, tag: 0)
        self.mSocket.readData(withTimeout: -1, tag: 0)
    }
    
    func tcpSendDPMSet() {
        NSLog("\(#fileID):\(#line) >> tcpSendDPMSet()\n")
        let cmdSetDpm: String = "{\"msgType\":5, \"REQ_SET_DPM\":0, \"sleepMode\":0, \"rtcTimer\":1740, \"useDPM\":0, \"dpmKeepAlive\":30000, \"userWakeUp\":0, \"timWakeup\":10}"
        let data = cmdSetDpm.data(using: String.Encoding.utf8)!
        self.mSocket.write(data, withTimeout:-1, tag: 0)
        self.mSocket.readData(withTimeout: -1, tag: 0)
    }
    
    func tcpSendSSIDPW() {
        NSLog("\(#fileID):\(#line) >> tcpSendSSIDPW()\n")
        NSLog("\(#fileID):\(#line) >> appDelegate.apInfo.securityModeString = \(appDelegate.apInfo.securityModeString)")
        let cmdSendSSIDPW: String = "{\"msgType\":1, \"SET_AP_SSID_PW\":0, \"ssid\":\"\(appDelegate.apInfo.ssid)\", \"pw\":\"\(appDelegate.apInfo.pw)\", \"security\":\(appDelegate.apInfo.securityModeString), \"isHidden\":\(appDelegate.apInfo.isHiddenWiFi), \"url\":\"\(appDelegate.serverURL)\"}"
        let data = cmdSendSSIDPW.data(using: String.Encoding.utf8)!
        self.mSocket.write(data, withTimeout:-1, tag: 0)
        self.mSocket.readData(withTimeout: -1, tag: 0)
    }
    
    func tcpSendSSIDPW_1() {
        NSLog("\(#fileID):\(#line) >> tcpSendSSIDPW_1()\n")
        NSLog("\(#fileID):\(#line) >> appDelegate.apInfo.securityModeInt = \(appDelegate.apInfo.securityModeInt)")
        let cmdSendSSIDPW: String = "{\"msgType\":1, \"SET_AP_SSID_PW\":0, \"ssid\":\"\(appDelegate.apInfo.ssid)\", \"pw\":\"\(appDelegate.apInfo.pw)\", \"securityType\":\(appDelegate.apInfo.securityModeInt), \"isHidden\":\(appDelegate.apInfo.isHiddenWiFi), \"url\":\"\(appDelegate.serverURL)\"}"
        let data = cmdSendSSIDPW.data(using: String.Encoding.utf8)!
        self.mSocket.write(data, withTimeout:-1, tag: 0)
        self.mSocket.readData(withTimeout: -1, tag: 0)
    }
    
    //[[for concurrent mode
    func tcpSendReqApResult() {
        if (self.isReceiveApResult == false) {
            NSLog("\(#fileID):\(#line) >> tcpSendReqApResult()\n")
            Utility.hideLoader(view: self.view)
            Utility.showLoader(message: "Checking network status...", view: view)
            let cmdReqResult: String = "{\"msgType\":2,\"REQ_HOMEAP_RESULT\":0}"
            let data = cmdReqResult.data(using: String.Encoding.utf8)!
            self.mSocket.write(data, withTimeout:-1, tag: 0)
            self.mSocket.readData(withTimeout: -1, tag: 0)
        }
    }
    
    func sendEnterpriseConfig() {
        NSLog("\(#fileID):\(#line) >> sendEnterpriseConfig()\n")
        NSLog("\(#fileID):\(#line) >> appDelegate.apInfo.securityModeInt = \(appDelegate.apInfo.securityModeInt)")
        let cmdSendSSIDPW: String = "{\"msgType\":1, \"SET_AP_SSID_PW\":0, \"ssid\":\"\(appDelegate.apInfo.ssid)\", \"securityType\":\(appDelegate.apInfo.securityModeInt), \"isHidden\":\(appDelegate.apInfo.isHiddenWiFi), \"url\":\"\(appDelegate.serverURL)\", \"authType\":\(appDelegate.apInfo.enterpriseAuthType), \"authProtocol\":\(appDelegate.apInfo.enterpriseAuthProtocol), \"authID\":\"\(appDelegate.apInfo.enterpriseID)\", \"authPW\":\"\(appDelegate.apInfo.enterprisePassword)\"}"
        let data = cmdSendSSIDPW.data(using: String.Encoding.utf8)!
        NSLog("\(#fileID):\(#line) >> sendEnterpriseConfig data = \(cmdSendSSIDPW)")
        self.mSocket.write(data, withTimeout:-1, tag: 0)
        self.mSocket.readData(withTimeout: -1, tag: 0)
    }
    
    
    // MARK: - Timer
    
    func startTimer() {
        if let timer = mTimer {
            if !timer.isValid {
                mTimer = Timer.scheduledTimer(timeInterval: 1.0, target: self, selector: #selector(update), userInfo: nil, repeats: true)
            }
        } else {
            mTimer = Timer.scheduledTimer(timeInterval: 1.0, target: self, selector: #selector(update), userInfo: nil, repeats: true)
        }
    }
    
    @objc func update() {
        timeCount += 1
        NSLog("\(#fileID):\(#line) >> timeCount = \(timeCount)")
        if (timeCount > 10) {
            if mSocket.isConnected {
                mSocket.disconnect()
            }
            self.mSocket = nil
            self.isSocketChanged = false;
            self.mSocket = GCDAsyncSocket(delegate: self, delegateQueue: DispatchQueue.main)
            if (self.mSocket != nil) {
                if (self.isSecure) {
                    self.connectSocket(port: self.appDelegate.tlsHostPort1)
                    NSLog("\(#fileID):\(#line) >> self.connectSocket(port: self.appDelegate.tlsHostPort1)")
                } else {
                    self.connectSocket(port: self.appDelegate.tcpHostPort1)
                    NSLog("\(#fileID):\(#line) >> self.connectSocket(port: self.appDelegate.tcpHostPort1)")
                }
            }
        }
    }
    
    func stopTimer() {
        NSLog("\(#fileID):\(#line) >> stopTimer()")
        if let timer = mTimer {
            if (timer.isValid) {
                timer.invalidate()
                mTimer = nil
                timeCount = 0
            }
        }
    }
    
    // MARK: - Timer1
    
    func startTimer1() {
        NSLog("\(#fileID):\(#line) >> startTimer1()")
        if let timer = mTimer {
            if !timer.isValid {
                mTimer = Timer.scheduledTimer(timeInterval: 1.0, target: self, selector: #selector(update1), userInfo: nil, repeats: true)
            }
        } else {
            mTimer = Timer.scheduledTimer(timeInterval: 1.0, target: self, selector: #selector(update1), userInfo: nil, repeats: true)
        }
    }
    
    @objc func update1() {
        timeCount += 1
        NSLog("\(#fileID):\(#line) >> timeCount = \(timeCount)")
        if (timeCount > 10) {
            Utility.hideLoader(view: self.view)
            self.showSocketDisconnectAlert()
        }
    }
    
    func stopTimer1() {
        NSLog("\(#fileID):\(#line) >> stopTimer1()")
        if let timer = mTimer {
            if (timer.isValid) {
                timer.invalidate()
                mTimer = nil
                timeCount = 0
            }
        }
    }
    
    // MARK: - Toast
    func showToast(message : String, seconds: Double){
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

extension UIButton {
    func setBackgroundColor(_ color: UIColor, for state: UIControl.State) {
        UIGraphicsBeginImageContext(CGSize(width: 1.0, height: 1.0))
        guard let context = UIGraphicsGetCurrentContext() else {
            return
        }
        context.setFillColor(color.cgColor)
        context.fill(CGRect(x: 0.0, y: 0.0, width: 1.0, height: 1.0))
        let backgroundImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()
        self.setBackgroundImage(backgroundImage, for: state)
    }
}

extension SelectNetworkViewController: DA16200AddHiddenViewControllerDelegate {
    func didTapCancel(_ vc: DA16200AddHiddenViewController) {
        NSLog("\(#fileID):\(#line) >> didTapCancel")
        da16200AddHiddenViewController?.dismiss(animated: true, completion: nil)
    }
    func didTapOk(_ vc: DA16200AddHiddenViewController) {
        NSLog("\(#fileID):\(#line) >> didTapOk")
        da16200AddHiddenViewController?.dismiss(animated: true, completion: nil)
    }
}
