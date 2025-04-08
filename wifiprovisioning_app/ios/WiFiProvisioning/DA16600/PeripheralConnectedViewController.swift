//
//  PeripheralConnectedViewController.swift
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
import CoreBluetooth
import SwiftyJSON
import MBProgressHUD

class PeripheralConnectedViewController: UIViewController, CBCentralManagerDelegate, CBPeripheralDelegate, UITableViewDataSource, UITableViewDelegate, UITextViewDelegate, UITextFieldDelegate {
    
    let appDelegate = UIApplication.shared.delegate as! AppDelegate
    
    var addHiddenViewController: UIViewController?
    
    let WIFI_SVC_UUID = CBUUID(string: "9161b201-1b4b-4727-a3ca-47b35cdcf5c1")
    let WIFI_SVC_WFCMD_UUID = CBUUID(string: "9161b202-1b4b-4727-a3ca-47b35cdcf5c1")
    let WIFI_SVC_WFACT_RES_UUID = CBUUID(string: "9161b203-1b4b-4727-a3ca-47b35cdcf5c1")
    let WIFI_SVC_APSCAN_RES_UUID = CBUUID(string: "9161b204-1b4b-4727-a3ca-47b35cdcf5c1")
    let WIFI_SVC_PROV_DATA_UUID = CBUUID(string: "9161b205-1b4b-4727-a3ca-47b35cdcf5c1")
    let WIFI_SVC_AWS_DATA_UUID = CBUUID(string: "9161b206-1b4b-4727-a3ca-47b35cdcf5c1")
    let WIFI_SVC_AZURE_DATA_UUID = CBUUID(string: "9161b207-1b4b-4727-a3ca-47b35cdcf5c1")
    
    enum WIFI_ACTION_RESULT: Int {
        case COMBO_WIFI_CMD_SCAN_AP_SUCCESS = 1
        case COMBO_WIFI_CMD_SCAN_AP_FAIL = 2
        case COMBO_WIFI_CMD_FW_BLE_DOWNLOAD_SUCCESS = 3
        case COMBO_WIFI_CMD_FW_BLE_DOWNLOAD_FAIL = 4
        case COMBO_WIFI_CMD_INQ_WIFI_STATUS_CONNECTED = 5
        case COMBO_WIFI_CMD_INQ_WIFI_STATUS_NOT_CONNECTED = 6
        case COMBO_WIFI_PROV_DATA_VALIDITY_CHK_ERR = 7
        case COMBO_WIFI_PROV_DATA_SAVE_SUCCESS = 8
        case COMBO_WIFI_CMD_MEM_ALLOC_FAIL = 9
        case COMBO_WIFI_CMD_UNKNOWN_RCV = 10
        
        case COMBO_WIFI_CMD_CALLBACK = 100
        case COMBO_WIFI_CMD_SELECTED_AP_SUCCESS = 101
        case COMBO_WIFI_CMD_SELECTED_AP_FAIL = 102
        case COMBO_WIFI_PROV_WRONG_PW = 103
        case COMBO_WIFI_CMD_NETWORK_INFO_CALLBACK = 104
        case COMBO_WIFI_PROV_AP_FAIL = 105
        case COMBO_WIFI_PROV_DNS_FAIL_SERVER_FAIL = 106
        case COMBO_WIFI_PROV_DNS_FAIL_SERVER_OK = 107
        case COMBO_WIFI_PROV_NO_URL_PING_FAIL = 108
        case COMBO_WIFI_PROV_NO_URL_PING_OK = 109
        case COMBO_WIFI_PROV_DNS_OK_PING_FAIL_N_SERVER_OK = 110
        case COMBO_WIFI_PROV_DNS_OK_PING_OK = 111
        case COMBO_WIFI_PROV_REBOOT_SUCCESS = 112
        case COMBO_WIFI_PROV_DNS_OK_PING_N_SERVER_FAIL = 113
        
        case COMBO_WIFI_CMD_AWS_CALLBACK = 114
        
        case COMBO_WIFI_CMD_AZURE_CALLBACK = 115
    }
    
    var centralManager: CBCentralManager!
    
    // Connected peripheral
    var connectedPeripheral: CBPeripheral!
    
    // All services in the connected peripheral
    var services = [CBService]()
    
    // Service chosen by user
    var chosenService: CBService!
    
    //List of Characteristics displayed on the table view
    var characteristics = Array<CBCharacteristic>()
    
    var wifiCmdCharacteristic: CBCharacteristic? = nil
    var wifiActResCharacteristic: CBCharacteristic? = nil
    var wifiApscanResCharacteristic: CBCharacteristic? = nil
    var wifiProvDataCharacteristic: CBCharacteristic? = nil
    var wifiAwsDataCharacteristic: CBCharacteristic? = nil
    var wifiAzureDataCharacteristic: CBCharacteristic? = nil
    
    @IBOutlet weak var periName: UILabel!
    @IBOutlet weak var btnScan: UIButton!
    @IBOutlet weak var btnHiddenWifi: UIButton!
    @IBOutlet weak var btnCustomCommand: UIButton!
    @IBOutlet weak var btnReset: UIButton!
    @IBOutlet weak var titleUuid: UILabel!
    @IBOutlet weak var labelUuid: UILabel!
    @IBOutlet weak var titleState: UILabel!
    @IBOutlet weak var labelState: UILabel!
    @IBOutlet weak var btnConnectDisconnect: UIButton!
    @IBOutlet weak var networkTable: UITableView!
    @IBOutlet weak var btnConnect: UIButton!
    @IBOutlet weak var labelRawCommand: UILabel!
    @IBOutlet weak var tvData1: UITextView!
    @IBOutlet weak var tvData2: UITextView!
    @IBOutlet weak var labelCommand: UILabel!
    @IBOutlet weak var tfCommand: UITextField!
    @IBOutlet weak var btnSend: UIButton!
    
    var sb: String = ""
    var APListSSID : [String] = []
    var APListSecurity: [Int] = []
    var APListSignal: [Int] = []
    
    var selectedIndex: String!
    var selectedSSID: String!
    var selectedSecurity: String!
    
    var finalPingAddress: String! = "8.8.8.8"
    var finalSvrAddress: String! = "192.168.0.1"
    var finalSvrPort: Int! = 10195
    var finalSvrUrl: String! = "www.google.com"
    
    var finalSSID: String!
    var finalSecurity: Int!
    var finalPassword: String!
    var finalIsHiddenWiFi: Int!
    
    //[[add in v2.4.15
    var mySSID: String!
    var mySecurity: Int!
    var myPassword: String!
    var myIsHiddenWiFi: Int!
    
    var result_105:Int!
    var ssid_105: String!
    var password_105: String!
    var security_105:Int!
    //]]
    
    //[[add in v2.4.12
    var finalAuthType: Int!
    var finalAuthProtocol: Int!
    var finalAuthID: String!
    var finalAuthPW: String!
    
    var isShowSetEnterprise: Bool = false
    //]]
    
    var customCommand: String!
    
    var mTimer:Timer?
    var timeCount = 0
    
    var preventButtonTouch = false
    
    //[[for AWS fleet provisioning
    var mode: Int?
    var isRegisteringDevice: Bool = false
    //]]
    
    override func viewDidLoad() {
        super.viewDidLoad()
        appDelegate.periConnectedVC = self
        
        if #available(iOS 13.0, *) {
            if self.traitCollection.userInterfaceStyle == .dark {
                titleUuid.textColor = UIColor.black
                labelUuid.textColor = UIColor.black
                titleState.textColor = UIColor.black
            }
        }
        
        periName.text = appDelegate.peripheralName
        labelUuid.text = connectedPeripheral.identifier.uuidString
        
        self.btnConnectDisconnect.layer.cornerRadius = 10
        self.btnConnectDisconnect.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnConnectDisconnect.layer.shadowOpacity = 1;
        self.btnConnectDisconnect.layer.shadowRadius = 1;
        self.btnConnectDisconnect.layer.shadowOffset = CGSize(width: 1, height: 4)
        
        self.btnScanDisable()
        
        self.btnHiddenWifiDisable()
        
        self.btnCustomCommandDisable()
        
        self.btnResetDisable()
        
        self.networkTable.delegate = self
        self.networkTable.dataSource = self
        self.networkTable.backgroundColor = UIColor.white
        self.networkTable.layer.borderColor = UIColor(red: 42/255, green: 40/255, blue: 157/255, alpha: 1).cgColor
        self.networkTable.layer.borderWidth = 2.0
        self.networkTable.layer.cornerRadius = 10
        self.networkTable.isHidden = true
        
        self.labelRawCommand.isHidden = true
        
        self.tvData1.delegate = self
        self.tvData1.backgroundColor = UIColor.white
        self.tvData1.layer.borderColor = UIColor(red: 42/255, green: 40/255, blue: 157/255, alpha: 1).cgColor
        self.tvData1.layer.borderWidth = 2.0
        self.tvData1.layer.cornerRadius = 10
        self.tvData1.isHidden = true
        
        self.tvData2.delegate = self
        self.tvData2.backgroundColor = UIColor.white
        self.tvData2.layer.borderColor = UIColor(red: 42/255, green: 40/255, blue: 157/255, alpha: 1).cgColor
        self.tvData2.layer.borderWidth = 2.0
        self.tvData2.layer.cornerRadius = 10
        self.tvData2.isHidden = true
        
        self.btnConnect.layer.cornerRadius = 10
        self.btnConnect.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnConnect.layer.shadowOpacity = 1;
        self.btnConnect.layer.shadowRadius = 1;
        self.btnConnect.layer.shadowOffset = CGSize(width: 1, height: 4)
        self.btnConnect.isHidden = true
        
        self.labelState.text = "Connected"
        self.labelState.textColor = UIColor.green
        
        self.tvData1.addDoneButton(title: "Done", target: self, selector: #selector(tapDone(sender:)))
        self.tvData2.addDoneButton(title: "Done", target: self, selector: #selector(tapDone(sender:)))
        
        self.btnSend.layer.cornerRadius = 10
        self.btnSend.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnSend.layer.shadowOpacity = 1;
        self.btnSend.layer.shadowRadius = 1;
        self.btnSend.layer.shadowOffset = CGSize(width: 1, height: 4)
        self.btnSend.isHidden = true
        
        self.labelCommand.isHidden = true
        self.tfCommand.isHidden = true
        self.tfCommand.layer.borderWidth = 2.0
        self.tfCommand.layer.borderColor = UIColor(red: 42/255, green: 40/255, blue: 157/255, alpha: 1).cgColor
        self.btnSend.isHidden = true
    }
    
    @objc func tapDone(sender: Any) {
        self.view.endEditing(true)
    }
    
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        self.view.endEditing(true)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        self.registerForKeyboardNotifications()
    }
    
    
    //View Did Appear
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        // Re-start scan after coming back from background
        centralManager?.delegate = self
        connectedPeripheral?.delegate = self
        connectedPeripheral?.discoverServices(nil)
    }
    
    //View Will Disappear
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        self.unregisterForKeyboardNotifications()
        centralManager?.cancelPeripheralConnection(connectedPeripheral)
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let addHiddenVC = segue.destination as? AddHiddenViewController {
            addHiddenVC.delegate = self
            self.addHiddenViewController = addHiddenVC
        }
    }
    
    private func showAddHidden() {
        performSegue(withIdentifier: "addHiddenSegue", sender: self)
    }
    
    //MARK: - Button Click
    
    @IBAction func onBtnBack(_ sender: UIButton) {
        centralManager?.cancelPeripheralConnection(connectedPeripheral!)
        let peripheralVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "PeripheralViewController")
        peripheralVC.modalPresentationStyle = .fullScreen
        present(peripheralVC, animated: true, completion: nil)
    }
    
    @IBAction func onBtnConnectDisconnectClick(_ sender: UIButton) {
        if (btnConnectDisconnect.titleLabel?.text == "Connect") {
            connectedPeripheral?.delegate = self
            centralManager?.delegate = self
            
            if connectedPeripheral.state != .connected {
                appDelegate.peripheralName = connectedPeripheral.displayName
                centralManager?.connect(connectedPeripheral, options: nil)
                Utility.showLoader(message: "Connecting to DA16600 ...", view: view)
            }
        } else if (btnConnectDisconnect.titleLabel?.text == "Disconnect") {
            centralManager?.cancelPeripheralConnection(connectedPeripheral)
        }
    }
    
    
    @IBAction func onBtnScanClick(_ sender: UIButton) {
        self.hideAllUI()
        self.APListSSID = []
        self.APListSecurity = []
        self.APListSignal = []
        
        self.btnScan.isEnabled = false
        self.btnCustomCommand.isEnabled = false
        self.btnReset.isEnabled = false
        
        self.networkTable.isHidden = false
        self.sendApScanCommand()
    }
    
    @IBAction func onBtnHiddenWifi(_ sender: UIButton) {
        showAddHidden()
    }
    
    @IBAction func onBtnCustomCommandClick(_ sender: UIButton) {
        
        self.hideAllUI()
        
        self.labelCommand.isHidden = false
        self.tfCommand.isHidden = false
        self.btnSend.isHidden = false
    }
    
    @IBAction func onBtnResetClick(_ sender: UIButton) {
        self.hideAllUI()
        self.sendResetCommand()
        
        self.btnScanDisable()
        self.btnCustomCommandDisable()
        self.btnResetDisable()
        
        self.btnHiddenWifiDisable()
    }
    
    @IBAction func onBtnSendClick(_ sender: UIButton) {
        self.customCommand = self.tfCommand.text!
        
        if (self.customCommand.contains("scan")) {
            
            self.hideAllUI()
            
            self.APListSSID = []
            self.APListSecurity = []
            self.APListSignal = []
            
            self.btnScan.isEnabled = false
            
            self.btnHiddenWifi.isEnabled = false
            
            self.btnCustomCommand.isEnabled = false
            self.btnReset.isEnabled = false
            
            self.networkTable.isHidden = false
            
            Utility.showLoader(message: "Scanning Wi-Fi network ...", view: view)
        }
        
        self.sendCustomerCommand(jsonString: self.customCommand)
    }
    
    @IBAction func onBtnConnectClick(_ sender: UIButton) {
        if self.preventButtonTouch == true {
            return
        }
        self.preventButtonTouch = true
        
        self.startTimer()
        self.sendNetworkInfo()
        
        self.preventButtonTouch = false
    }
    
    func setup(with peripheral: CBPeripheral) {
        self.connectedPeripheral = peripheral
    }
    
    func hideAllUI() {
        self.labelRawCommand.isHidden = true
        self.tvData1.isHidden = true
        self.tvData2.isHidden = true
        self.networkTable.isHidden = true
        self.btnConnect.isHidden = true
        self.labelCommand.isHidden = true
        self.tfCommand.isHidden = true
        self.btnSend.isHidden = true
        Utility.hideLoader(view: view)
    }
    
    
    //MARK: - Button Enable/Disable
    
    func btnScanEnalbe() {
        self.btnScan.isEnabled = true
        self.btnScan.setBackgroundColor(UIColor(red: 42/255, green: 40/255, blue: 157/255, alpha: 1), for: .normal)
        self.btnScan.layer.cornerRadius = 10
        self.btnScan.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnScan.layer.shadowOpacity = 1;
        self.btnScan.layer.shadowRadius = 1;
        self.btnScan.layer.shadowOffset = CGSize(width: 1, height: 4)
    }
    
    func btnScanDisable() {
        self.btnScan.isEnabled = false
        self.btnScan.setBackgroundColor(.gray, for: .disabled)
        self.btnScan.layer.cornerRadius = 10
        self.btnScan.layer.shadowColor = UIColor(red: 85/255, green: 85/255, blue: 85/255, alpha: 1).cgColor
        self.btnScan.layer.shadowOpacity = 1;
        self.btnScan.layer.shadowRadius = 1;
        self.btnScan.layer.shadowOffset = CGSize(width: 1, height: 4)
    }
    
    func btnHiddenWifiEnalbe() {
        self.btnHiddenWifi.isEnabled = true
        self.btnHiddenWifi.setBackgroundColor(UIColor(red: 42/255, green: 40/255, blue: 157/255, alpha: 1), for: .normal)
        self.btnHiddenWifi.layer.cornerRadius = 10
        self.btnHiddenWifi.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnHiddenWifi.layer.shadowOpacity = 1;
        self.btnHiddenWifi.layer.shadowRadius = 1;
        self.btnHiddenWifi.layer.shadowOffset = CGSize(width: 1, height: 4)
    }
    
    func btnHiddenWifiDisable() {
        self.btnHiddenWifi.isEnabled = false
        self.btnHiddenWifi.setBackgroundColor(.gray, for: .disabled)
        self.btnHiddenWifi.layer.cornerRadius = 10
        self.btnHiddenWifi.layer.shadowColor = UIColor(red: 85/255, green: 85/255, blue: 85/255, alpha: 1).cgColor
        self.btnHiddenWifi.layer.shadowOpacity = 1;
        self.btnHiddenWifi.layer.shadowRadius = 1;
        self.btnHiddenWifi.layer.shadowOffset = CGSize(width: 1, height: 4)
    }
    
    func btnCustomCommandEnable() {
        self.btnCustomCommand.isEnabled = true
        self.btnCustomCommand.setBackgroundColor(UIColor(red: 42/255, green: 40/255, blue: 157/255, alpha: 1), for: .normal)
        self.btnCustomCommand.layer.cornerRadius = 10
        self.btnCustomCommand.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnCustomCommand.layer.shadowOpacity = 1;
        self.btnCustomCommand.layer.shadowRadius = 1;
        self.btnCustomCommand.layer.shadowOffset = CGSize(width: 1, height: 4)
    }
    
    func btnCustomCommandDisable() {
        self.btnCustomCommand.isEnabled = false
        self.btnCustomCommand.setBackgroundColor(.gray, for: .disabled)
        self.btnCustomCommand.layer.cornerRadius = 10
        self.btnCustomCommand.layer.shadowColor = UIColor(red: 85/255, green: 85/255, blue: 85/255, alpha: 1).cgColor
        self.btnCustomCommand.layer.shadowOpacity = 1;
        self.btnCustomCommand.layer.shadowRadius = 1;
        self.btnCustomCommand.layer.shadowOffset = CGSize(width: 1, height: 4)
    }
    
    func btnResetEnable() {
        self.btnReset.isEnabled = true
        self.btnReset.setBackgroundColor(UIColor(red: 42/255, green: 40/255, blue: 157/255, alpha: 1), for: .normal)
        self.btnReset.layer.cornerRadius = 10
        self.btnReset.layer.shadowColor = UIColor(red: 30/255, green: 24/255, blue: 148/255, alpha: 1).cgColor
        self.btnReset.layer.shadowOpacity = 1;
        self.btnReset.layer.shadowRadius = 1;
        self.btnReset.layer.shadowOffset = CGSize(width: 1, height: 4)
    }
    
    func btnResetDisable() {
        self.btnReset.isEnabled = false
        self.btnReset.setBackgroundColor(.gray, for: .disabled)
        self.btnReset.layer.cornerRadius = 10
        self.btnReset.layer.shadowColor = UIColor(red: 85/255, green: 85/255, blue: 85/255, alpha: 1).cgColor
        self.btnReset.layer.shadowOpacity = 1;
        self.btnReset.layer.shadowRadius = 1;
        self.btnReset.layer.shadowOffset = CGSize(width: 1, height: 4)
    }
    
    
    //MARK: - Keyboard Event
    
    func registerForKeyboardNotifications() {
        NotificationCenter.default.addObserver(self, selector:#selector(keyboardWillShow), name: UIResponder.keyboardWillShowNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector:#selector(keyboardWillHide), name: UIResponder.keyboardWillHideNotification, object: nil)
    }
    
    func unregisterForKeyboardNotifications() {
        NotificationCenter.default.removeObserver(self, name:UIResponder.keyboardWillShowNotification, object: nil)
        NotificationCenter.default.removeObserver(self, name:UIResponder.keyboardWillHideNotification, object: nil)
    }
    
    @objc func keyboardWillShow(notification: NSNotification) {
        if let keyboardSize = (notification.userInfo?[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue)?.cgRectValue {
            if self.view.frame.origin.y == 0 {
                self.view.frame.origin.y -= 150
            }
        }
    }
    
    @objc func keyboardWillHide(notification: NSNotification) {
        if self.view.frame.origin.y != 0 {
            self.view.frame.origin.y = 0
        }
    }
    
    //MARK: - CBCentalManagerDelegate
    
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        NSLog("\(#fileID):\(#line) >> centralManagerDidUpdateState")
    }
    
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        NSLog("\(#fileID):\(#line) >> didConnect")
        
        self.connectedPeripheral?.discoverServices(nil)
        self.networkTable.isHidden = true
    }
    
    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        NSLog("\(#fileID):\(#line) >> didDisconnectPeripheral")
        
        self.labelState.text="Disconnected"
        self.labelState.textColor = UIColor.red
        self.btnConnectDisconnect.setTitle("Connect", for: .normal)
        
        self.btnScanDisable()
        
        self.btnHiddenWifiDisable()
        
        self.btnCustomCommandDisable()
        self.btnResetDisable()
        
        self.sb = ""
        self.APListSSID = []
        self.APListSecurity = []
        self.APListSignal = []
        self.networkTable.reloadData()
        
        self.tvData1.text = ""
        self.tvData2.text = ""
        
        if (!isRegisteringDevice) {  //for AWS fleet provisioning
            self.hideAllUI()
        }
        
        
    }
    
    
    
    //MARK: - CBPeripheralDelegate
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverServices error: Error?) {
        NSLog("\(#fileID):\(#line) >> CBPeripheral : didDiscoverServices")
        if let e = error {
            NSLog("\(#fileID):\(#line) >> Failed to discover services: \(e.localizedDescription)")
            return
        }
        
        for service in peripheral.services! {
            services.append(service)
            if service.uuid == WIFI_SVC_UUID {
                NSLog("\(#fileID):\(#line) >> service.uuid == WIFI_SVC_UUID")
                chosenService = service
                connectedPeripheral.delegate =  self
                connectedPeripheral.discoverCharacteristics(nil, for: chosenService)
            }
        }
    }
    
    func peripheral(_ peripheral: CBPeripheral, didModifyServices invalidatedServices: [CBService]) {
        NSLog("\(#fileID):\(#line) >> CBPeripheral : didModifyServices")
        connectedPeripheral?.discoverServices(nil)
    }
    
    func peripheral(_ peripheral: CBPeripheral, didDiscoverCharacteristicsFor chosenService: CBService, error: Error?) {
        NSLog("\(#fileID):\(#line) >> CBPeripheral : didDiscoverCharacteristicsFor")
        
        for characteristic in chosenService.characteristics! {
            if characteristic.uuid == WIFI_SVC_WFCMD_UUID {
                wifiCmdCharacteristic = characteristic
                peripheral.setNotifyValue(true, for: wifiCmdCharacteristic!)
            } else if (characteristic.uuid == WIFI_SVC_WFACT_RES_UUID) {
                wifiActResCharacteristic = characteristic
                peripheral.setNotifyValue(true, for: wifiActResCharacteristic!)
            } else if (characteristic.uuid == WIFI_SVC_APSCAN_RES_UUID) {
                wifiApscanResCharacteristic = characteristic
                peripheral.setNotifyValue(true, for: wifiApscanResCharacteristic!)
            } else if (characteristic.uuid == WIFI_SVC_PROV_DATA_UUID) {
                wifiProvDataCharacteristic = characteristic
                peripheral.setNotifyValue(true, for: wifiProvDataCharacteristic!)
            }
            else if (characteristic.uuid == WIFI_SVC_AWS_DATA_UUID) {
                wifiAwsDataCharacteristic = characteristic
                peripheral.setNotifyValue(true, for: wifiAwsDataCharacteristic!)
            }
            else if (characteristic.uuid == WIFI_SVC_AZURE_DATA_UUID) {
                wifiAzureDataCharacteristic = characteristic
                peripheral.setNotifyValue(true, for: wifiAzureDataCharacteristic!)
            }
        }
        Utility.hideLoader(view: view)
        
        self.labelState.text="Connected"
        self.labelState.textColor = UIColor.green
        self.btnConnectDisconnect.setTitle("Disconnect", for: .normal)
        
        self.btnScanEnalbe()
        
        self.btnHiddenWifiEnalbe()
        
        self.btnCustomCommandEnable()
        self.btnResetEnable()
        
        self.hideAllUI()
        
        if (wifiAwsDataCharacteristic != nil) {
            Utility.showLoader(message: "Receiving Thing Name for AWS IoT...", view: view)
            self.sendGetThingNameCommand()
        }
        
        if (wifiAzureDataCharacteristic != nil) {
            Utility.showLoader(message: "Receiving Device ID for Azure IoT...", view: view)
            self.sendGetThingNameCommand()
        }
    }
    
    func peripheral(_ peripheral: CBPeripheral, didUpdateValueFor characteristic: CBCharacteristic, error: Error?) {
        NSLog("\(#fileID):\(#line) >> CBPeripheral : didUpdateValueFor")
        if (characteristic == wifiActResCharacteristic) {
            if var value = characteristic.value {
                NSLog("\(#fileID):\(#line) >> wifiActResCharacteristic Data received")
                NSLog("\(#fileID):\(#line) >> \(value as NSData)")
                var out: NSInteger = 0
                let data = NSData(bytes: &value, length: MemoryLayout<NSInteger>.size)
                data.getBytes(&out, length: MemoryLayout<NSInteger>.size)
                NSLog("\(#fileID):\(#line) >> wifiActResCharacteristic = \(out)")
                if (out == WIFI_ACTION_RESULT.COMBO_WIFI_CMD_SCAN_AP_SUCCESS.rawValue) {
                    connectedPeripheral.readValue(for: wifiApscanResCharacteristic!)
                } else if (out == WIFI_ACTION_RESULT.COMBO_WIFI_CMD_SCAN_AP_FAIL.rawValue) {
                    self.showScanFailDialog()
                } else if (out == 100) {  //COMBO_WIFI_CMD_CALLBACK
                    connectedPeripheral.readValue(for: wifiProvDataCharacteristic!)
                }
                else if (out == 114) {  //COMBO_WIFI_CMD_AWS_CALLBACK
                    connectedPeripheral.readValue(for: wifiAwsDataCharacteristic!)
                }
                else if (out == 115) {  //COMBO_WIFI_CMD_AZURE_CALLBACK
                    connectedPeripheral.readValue(for: wifiAzureDataCharacteristic!)
                }
            }
        } else if (characteristic == wifiApscanResCharacteristic) {
            if let value = characteristic.value {
                NSLog("\(#fileID):\(#line) >> wifiApscanResCharacteristic Data received")
                let totalLength = getTotal(data: value)
                let remainLength = getRemain(data: value)
                NSLog("\(#fileID):\(#line) >> Total length = \(totalLength)")
                NSLog("\(#fileID):\(#line) >> Remaining length = \(remainLength)")
                var input: String = ""
                
                if (value.count == 244) {
                    let str  = String(decoding: value, as: UTF8.self)
                    let startIdx: String.Index = str.index(str.startIndex, offsetBy: 4)
                    input = String(str[startIdx...])
                    sb = sb + input
                } else if (value.count < 244) {
                    let str  = String(decoding: value, as: UTF8.self)
                    let startIdx: String.Index = str.index(str.startIndex, offsetBy: 4)
                    input = String(str[startIdx...])
                    sb = sb + input
                }
                
                if (remainLength > 0) {
                    connectedPeripheral.readValue(for: wifiApscanResCharacteristic!)
                } else {
                    do {
                        NSLog("\(#fileID):\(#line) >> \(sb)")
                        
                        let encodedString: Data = (sb as NSString).data(using:String.Encoding.utf8.rawValue)! as Data
                        
                        let finalJson = try JSON(data: encodedString as Data)
                        NSLog("\(#fileID):\(#line) >> \(finalJson.count)")
                        
                        for i in 0..<finalJson.count {
                            self.APListSSID.append(finalJson[i]["SSID"].stringValue)
                            self.APListSecurity.append(finalJson[i]["security_type"].intValue)
                            self.APListSignal.append(finalJson[i]["signal_strength"].intValue)
                        }
                        
                        NSLog("\(#fileID):\(#line) >> ssid = \(self.APListSSID)")
                        NSLog("\(#fileID):\(#line) >> security_type = \(self.APListSecurity)")
                        NSLog("\(#fileID):\(#line) >> signal = \(self.APListSignal)")
                        
                        self.networkTable.reloadData()
                        self.networkTable.setContentOffset(.zero, animated: true)
                        self.networkTable.isHidden = false
                        Utility.hideLoader(view: view)
                        
                        sb  = ""
                        
                        self.btnScan.isEnabled = true
                        self.btnCustomCommand.isEnabled = true
                        self.btnReset.isEnabled = true
                        
                        self.btnHiddenWifi.isEnabled = true
                        
                    } catch let error {
                        NSLog("\(#fileID):\(#line) >> \(error)")
                    }
                }
            }
        } else if (characteristic == wifiProvDataCharacteristic) {
            if let value = characteristic.value {
                NSLog("\(#fileID):\(#line) >> wifiProvDataCharacteristic Data received")
                
                let valueString  = String(decoding: value, as: UTF8.self)
                
                let encodedString: NSData = (valueString as NSString).data(using:String.Encoding.utf8.rawValue)! as NSData
                let json = try? JSON(data: encodedString as Data)
                
                let result = UInt((json?["result"].intValue)!)
                NSLog("\(#fileID):\(#line) >> result = \(String(describing: result))")
                
                if (result == 101) {  //COMBO_WIFI_CMD_SELECTED_AP_SUCCESS
                    NSLog("\(#fileID):\(#line) >> result = COMBO_WIFI_CMD_SELECT_AP_SUCCESS")
                    self.sendChkNetworkCommand()
                    Utility.showLoader(message: "Checking the connection", view: view)
                } else if (result == 102) {  //COMBO_WIFI_CMD_SELECTED_AP_FAIL
                    NSLog("\(#fileID):\(#line) >> result = COMBO_WIFI_CMD_SELECTED_AP_FAIL")
                    self.sendNetworkInfo()
                } else if (result == 103) {  //COMBO_WIFI_PROV_WRONG_PW
                    NSLog("\(#fileID):\(#line) >> result = COMBO_WIFI_PROV_WRONG_PW")
                    Utility.hideLoader(view: self.view)
                    self.showAPWrongPwDialog()
                } else if (result == 104) {  //COMBO_WIFI_CMD_NETWORK_INFO_CALLBACK
                    NSLog("\(#fileID):\(#line) >> result = COMBO_WIFI_CMD_NETWORK_INFO_CALLBACK")
                    self.stopTimer()
                    //[[modify in v2.4.12
                    //self.sendApInfo()
//                    if (self.appDelegate.apInfo_DA16600.security == 4) {
//                        self.sendApInfo()
//                    } else {
//                        self.sendEnterpriseApInfo()
//                    }
                    //]]
                    
                    //[[change in v2.4.14
                    if (self.appDelegate.apInfo_DA16600.security == 4) {
                        self.sendEnterpriseApInfo()
                    } else {
                        self.sendApInfo()
                    }
                    //]]
                    
                } else if (result == 105) {  //COMBO_WIFI_PROV_AP_FAIL
                    NSLog("\(#fileID):\(#line) >> result = COMBO_WIFI_PROV_AP_FAIL")
                    Utility.hideLoader(view: self.view)
                    
                    //[[modify in v2.4.15
                    
//                    print(">> valueString = \(valueString)")
//                    if let range1 = valueString.range(of: "\"ssid\":\t\"") {
//                        let start = range1.upperBound
//                        if let endRange = valueString[start...].range(of: "\"") {
//                            self.ssid_105 = String(valueString[start..<endRange.lowerBound])
//                            print("ssid : \(String(describing: self.ssid_105!))")
//                        }
//                    }
//                    
//                    if let range2 = valueString.range(of: "\"passwd\":\t\"") {
//                        let start = range2.upperBound
//                        if let endRange = valueString[start...].range(of: "\"") {
//                            self.password_105 = String(valueString[start..<endRange.lowerBound])
//                            print("Password : \(String(describing: self.password_105!))")
//                        }
//                    }
//                    
//                    if let range3 = valueString.range(of: "\"security\":\t") {
//                        let start = range3.upperBound
//                        if let endRange = valueString[start...].range(of: "\n}") {
//                            self.security_105 = Int(valueString[start..<endRange.lowerBound])
//                            print("Security : \(String(describing: self.security_105!))")
//                        }
//                    }

                    
                    
                    
                    self.ssid_105 = json?["ssid"].stringValue
                    self.password_105 = json?["passwd"].stringValue
                    self.security_105 = json?["security"].intValue
                    
                    //]]
                    var stringSecurity_105:String! = ""
                    if (self.security_105 == 0) {
                        stringSecurity_105 = "none"
                    } else if (self.security_105 == 1) {
                        stringSecurity_105 = "WEP"
                    } else if (self.security_105 == 2) {
                        stringSecurity_105 = "WPA"
                    } else if (self.security_105 == 3) {
                        stringSecurity_105 = "WPA2"
                    }
                    NSLog("\(#fileID):\(#line) >> ssid : \(String(describing: self.ssid_105))), password : \(String(describing: self.password_105)), security : \(String(describing: stringSecurity_105!))")
                    self.showApFailDialog(ssid:self.ssid_105, password:self.password_105, security:stringSecurity_105)
                } else if (result == 106) {  //COMBO_WIFI_PROV_DNS_FAIL_SERVER_FAIL
                    NSLog("\(#fileID):\(#line) >> result = COMBO_WIFI_PROV_DNS_FAIL_SERVER_FAIL")
                    Utility.hideLoader(view: self.view)
                    let svrUrl_106 = json?["svr_url"].stringValue
                    let pingIp_106 = json?["ping_ip"].stringValue
                    self.showDnsFailServerFailDialog(svrUrl: svrUrl_106!, pingIp: pingIp_106!)
                    
                } else if (result == 107) {  //COMBO_WIFI_PROV_DNS_FAIL_SERVER_OK
                    NSLog("\(#fileID):\(#line) >> result = COMBO_WIFI_PROV_DNS_FAIL_SERVER_OK")
                    Utility.hideLoader(view: self.view)
                    let svrUrl_107 = json?["svr_url"].stringValue
                    let pingIp_107 = json?["ping_ip"].stringValue
                    self.showDnsFailServerOkDailog(svrUrl: svrUrl_107!, pingIp: pingIp_107!)
                    
                } else if (result == 108) {  //COMBO_WIFI_PROV_NO_URL_PING_FAIL
                    NSLog("\(#fileID):\(#line) >> result = COMBO_WIFI_PROV_NO_URL_PING_FAIL")
                    Utility.hideLoader(view: self.view)
                    let pingIp_108 = json?["ping_ip"].stringValue
                    self.showNoUrlPingFailDialog(pingIp: pingIp_108!)
                    
                } else if (result == 109) {  //COMBO_WIFI_PROV_NO_URL_PING_OK
                    NSLog("\(#fileID):\(#line) >> result = COMBO_WIFI_PROV_NO_URL_PING_OK")
                    Utility.hideLoader(view: self.view)
                    let pingIp_109 = json?["ping_ip"].stringValue
                    self.showNoUrlPingOkDialog(pingIp: pingIp_109!)
                    
                } else if (result == 110) {  //COMBO_WIFI_PROV_DNS_OK_PING_FAIL_N_SERVER_OK
                    NSLog("\(#fileID):\(#line) >> result = COMBO_WIFI_PROV_DNS_OK_PING_FAIL_N_SERVER_OK")
                    Utility.hideLoader(view: self.view)
                    let svrUrl_110 = json?["svr_url"].stringValue
                    let svrIp_110 = json?["svr_ip"].stringValue
                    let pingIp_110 = json?["ping_ip"].stringValue
                    self.showDnsOkPingFailServerOkDialog(svrUrl: svrUrl_110!, svrIp:svrIp_110!, pingIp: pingIp_110!)
                    
                } else if (result == 111) {  //COMBO_WIFI_PROV_DNS_OK_PING_OK
                    NSLog("\(#fileID):\(#line) >> result = COMBO_WIFI_PROV_DNS_OK_PING_OK")
                    Utility.hideLoader(view: self.view)
                    let svrUrl_111 = json?["svr_url"].stringValue
                    let svrIp_111 = json?["svr_ip"].stringValue
                    NSLog("\(#fileID):\(#line) >> svrUrl : \(String(describing: svrUrl_111!)), svrIp : \(String(describing: svrIp_111!))")
                    self.showDnsOkPingOkDialog(svrUrl:svrUrl_111!, svrIp:svrIp_111!)
                    
                } else if (result == 112) {  //COMBO_WIFI_PROV_REBOOT_SUCCESS
                    NSLog("\(#fileID):\(#line) >> result = COMBO_WIFI_PROV_REBOOT_SUCCESS")
                    //[[for AWS fleet provisioning
                    self.mode = UserDefaults.standard.integer(forKey: "modeKey")
                    if (self.mode != nil) {
                        NSLog("\(#fileID):\(#line) >> mode : \(self.mode!)")
                        
                        if (self.mode == 12 || self.mode == 13) {
                            isRegisteringDevice = true
                            DispatchQueue.main.async {
                                NSLog("\(#fileID):\(#line) >> showLoader")
                                let loader = MBProgressHUD.showAdded(to: self.view, animated: true)
                                loader.bezelView.layer.cornerRadius = 5
                                loader.mode = MBProgressHUDMode.indeterminate
                                loader.detailsLabel.text = """
                                The device is being registerd
                                with the AWS server.
                                It takes about 60 seconds.
                                """
                                loader.isUserInteractionEnabled = false
                            }
                            
                            DispatchQueue.main.asyncAfter(deadline: .now()+60) {
                                Utility.hideLoader(view: self.view)
                            }
                            
                            DispatchQueue.main.asyncAfter(deadline: .now()+61) {
                                let mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "MainViewController")
                                mainVC.modalPresentationStyle = .fullScreen
                                self.present(mainVC, animated: true, completion: nil)
                            }
                            
                        } else {
                            usleep(1000000)
                            let mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "MainViewController")
                            mainVC.modalPresentationStyle = .fullScreen
                            self.present(mainVC, animated: true, completion: nil)
                        }
                    }
                    
                } else if (result == 113) {  //COMBO_WIFI_PROV_DNS_OK_PING_N_SERVER_FAIL
                    NSLog("\(#fileID):\(#line) >> result = COMBO_WIFI_PROV_DNS_OK_PING_N_SERVER_FAIL")
                    Utility.hideLoader(view: self.view)
                    
                    let svrUrl_113 = json?["svr_url"].stringValue
                    let svrIp_113 = json?["svr_ip"].stringValue
                    let pingIp_113 = json?["ping_ip"].stringValue
                    NSLog("\(#fileID):\(#line) >> svrUrl : \(String(describing: svrUrl_113!)), svrIp : \(String(describing: svrIp_113!)), pingIp : \(String(describing: pingIp_113!))")
                    self.showDnsOkPingFailServerFailDialog(svrUrl:svrUrl_113!, svrIp:svrIp_113!, pingIp: pingIp_113!)
                }
            }
        }
        
        else if (characteristic == wifiAwsDataCharacteristic) {
            if let value = characteristic.value {
                NSLog("\(#fileID):\(#line) >> wifiProvDataCharacteristic Data received")
                
                let valueString  = String(decoding: value, as: UTF8.self)
                let encodedString: NSData = (valueString as NSString).data(using:String.Encoding.utf8.rawValue)! as NSData
                let json = try? JSON(data: encodedString as Data)
                
                let thingName = String((json?["thingName"].stringValue)!)
                if !thingName.isEmpty {
                    NSLog("\(#fileID):\(#line) >> thingName = \(thingName)")
                    UserDefaults.standard.set(thingName, forKey: "thingNameKey")
                    if (wifiAwsDataCharacteristic != nil) {
                        usleep(500000)
                        self.sendGetModeCommand()
                    } else {
                        NSLog("\(#fileID):\(#line) >> The device's SDK don't support AWS IoT.")
                    }
                }
                
                let mode = UInt((json?["mode"].intValue)!)
                if (mode != 0) {
                    NSLog("\(#fileID):\(#line) >> mode = \(mode)")
                    UserDefaults.standard.set(mode, forKey: "modeKey")
                }
                Utility.hideLoader(view: view)
            }
            
        }
        
        else if (characteristic == wifiAzureDataCharacteristic) {
            if let value = characteristic.value {
                NSLog("\(#fileID):\(#line) >> wifiProvDataCharacteristic Data received")
                let valueString  = String(decoding: value, as: UTF8.self)
                let encodedString: NSData = (valueString as NSString).data(using:String.Encoding.utf8.rawValue)! as NSData
                let json = try? JSON(data: encodedString as Data)
                
                let thingName = String((json?["thingName"].stringValue)!)
                if !thingName.isEmpty {
                    NSLog("\(#fileID):\(#line) >> thingName = \(thingName)")
                    UserDefaults.standard.set(thingName, forKey: "thingNameKey")
                    if (wifiAzureDataCharacteristic != nil) {
                        usleep(500000)
                        self.getAzureConStringCommand()
                    } else {
                        NSLog("\(#fileID):\(#line) >> The device's SDK don't support Azure IoT.")
                    }
                }
                
                let azureConString = String((json?["azureConString"].stringValue)!)
                if !azureConString.isEmpty {
                    NSLog("\(#fileID):\(#line) >> azureConString = \(azureConString)")
                    UserDefaults.standard.set(azureConString, forKey: "azureConStringKey")
                    if(wifiAzureDataCharacteristic != nil) {
                        usleep(500000)
                        self.sendGetModeCommand()
                    } else {
                        NSLog("\(#fileID):\(#line) >> The device's SDK don't support Azure IoT.")
                    }
                }
                
                let mode = UInt((json?["mode"].intValue)!)
                if (mode != 0) {
                    NSLog("\(#fileID):\(#line) >> mode = \(mode)")
                    UserDefaults.standard.set(mode, forKey: "modeKey")
                }
                Utility.hideLoader(view: view)
            }
            
        }
    }
    
    //MARK: - Display Command
    
    func displayNetworkInfo(_pingAddress: String, _svrAddrtess: String, _svrPort: Int, _svrUrl: String) {
        tvData1.text = """
            "dialog_cmd": "network_info",
            "ping_addr": \"\(_pingAddress)\",
            "svr_addr": \"\(_svrAddrtess)\",
            "svr_port": \(_svrPort),
            "customer_svr_url": \"\(_svrUrl)\"
        """
    }
    
    func displayApInfo(_ssid: String, _security: Int, _password: String, _isHidden: Int) {
        tvData2.text = """
            "dialog_cmd": "select_ap",
            "SSID": \"\(_ssid)\",
            "security_type": \(_security),
            "password": \"\(_password)\",
            "isHidden": \(_isHidden)
        """
    }
    
    
    //MARK: - Send Command
    func sendApScanCommand() {
        NSLog("\(#fileID):\(#line) >> sendApScanCommand()")
        Utility.showLoader(message: "Scanning Wi-Fi network ...", view: view)
        let cmd = "{\"dialog_cmd\":\"scan\"}"
        let data = cmd.data(using: String.Encoding.utf8)!
        NSLog("\(#fileID):\(#line) >> scan command data size: \(data.count)")
        connectedPeripheral!.writeValue(data, for: wifiCmdCharacteristic!, type: .withResponse)
    }
    
    func sendNetworkInfo() {
        let jsonString = self.tvData1!.text.replacingOccurrences(of: "\n", with: "")
        NSLog("\(#fileID):\(#line) >> \(jsonString)")
        let oJsonDataT:Data? = jsonString.data(using: .utf8)
        if let oJsonData = oJsonDataT {
            do {
                var oJsonDictionaryT:[String:Any]?
                oJsonDictionaryT = try! JSONSerialization.jsonObject(with: oJsonData, options: []) as! [String:Any]
                if let oJsonDictionary = oJsonDictionaryT {
                    if let pingAddress = oJsonDictionary["ping_addr"] as? String {
                        NSLog("\(#fileID):\(#line) >> \(pingAddress)")
                        self.finalPingAddress = pingAddress
                    }
                    if let svrAddress = oJsonDictionary["svr_addr"] as? String {
                        NSLog("\(#fileID):\(#line) >> \(svrAddress)")
                        self.finalSvrAddress = svrAddress
                    }
                    if let svrPort = oJsonDictionary["svr_port"] as? Int {
                        NSLog("\(#fileID):\(#line) >> \(svrPort)")
                        self.finalSvrPort = svrPort
                    }
                    if let svrUrl = oJsonDictionary["customer_svr_url"] as? String {
                        NSLog("\(#fileID):\(#line) >> \(svrUrl)")
                        self.finalSvrUrl = svrUrl
                    }
                }
            }
        }
        
        let cmd = "{\"dialog_cmd\":\"network_info\",\"ping_addr\":\"\( self.finalPingAddress!)\",\"svr_addr\":\"\(self.finalSvrAddress!)\",\"svr_port\":\( self.finalSvrPort!),\"svr_url\":\"\(self.finalSvrUrl!)\"}"
        NSLog("\(#fileID):\(#line) >> \(cmd)")
        let data = cmd.data(using: String.Encoding.utf8)!
        NSLog("\(#fileID):\(#line) >> network_info command data size: \(data.count)")
        connectedPeripheral!.writeValue(data, for: wifiCmdCharacteristic!, type: .withResponse)
    }
    
    func sendApInfo() {
        NSLog("\(#fileID):\(#line) >> sendApInfo()")
        let jsonString = self.tvData2!.text.replacingOccurrences(of: "\n", with: "")
        NSLog("\(#fileID):\(#line) >> \(jsonString)")
        
        let oJsonDataT:Data? = jsonString.data(using: .utf8)
        if let oJsonData = oJsonDataT {
            do {
                var oJsonDictionaryT:[String:Any]?
                oJsonDictionaryT = try! JSONSerialization.jsonObject(with: oJsonData, options: []) as! [String:Any]
                if let oJsonDictionary = oJsonDictionaryT {
                    if let ssid = oJsonDictionary["SSID"] as? String {
                        NSLog("\(#fileID):\(#line) >> \(ssid)")
                        //[[change in v2.4.15
                        //self.finalSSID = ssid
                        self.finalSSID = ssid.replacingOccurrences(of: "\\t", with: "\\\\t")
                                            .replacingOccurrences(of: "\\n", with: "\\\\n")
                                            .replacingOccurrences(of: "\\", with: "\\\\")
                                            .replacingOccurrences(of: "\"", with: "\\\"")
                        //]]
                    }
                    if let security = oJsonDictionary["security_type"] as? Int {
                        NSLog("\(#fileID):\(#line) >> \(security)")
                        self.finalSecurity = security
                    }
                    if let password = oJsonDictionary["password"] as? String {
                        NSLog("\(#fileID):\(#line) >> \(password)")
                        self.finalPassword = password
                    }
                    
                    if let isHidden = oJsonDictionary["isHidden"] as? Int {
                        NSLog("\(#fileID):\(#line) >> \(isHidden)")
                        self.finalIsHiddenWiFi = isHidden
                    }
                }
            }
        }
        
        let cmd = "{\"dialog_cmd\":\"select_ap\",\"SSID\":\"\(self.finalSSID!)\",\"security_type\":\(self.finalSecurity!),\"password\":\"\(self.finalPassword!)\",\"isHidden\":\(self.finalIsHiddenWiFi!)}"
        
        NSLog("\(#fileID):\(#line) >> \(cmd)")
        let data = cmd.data(using: String.Encoding.utf8)!
        NSLog("\(#fileID):\(#line) >> select_ap command data size: \(data.count)")
        connectedPeripheral!.writeValue(data, for: wifiCmdCharacteristic!, type: .withResponse)
        
        
    }
    
    //[[add in v2.4.12
    func sendEnterpriseApInfo() {
        NSLog("\(#fileID):\(#line) >> sendEnterpriseApInfo()")
        let jsonString = self.tvData2!.text.replacingOccurrences(of: "\n", with: "")
        NSLog("\(#fileID):\(#line) >> \(jsonString)")
        let oJsonDataT:Data? = jsonString.data(using: .utf8)
        if let oJsonData = oJsonDataT {
            do {
                var oJsonDictionaryT:[String:Any]?
                oJsonDictionaryT = try! JSONSerialization.jsonObject(with: oJsonData, options: []) as! [String:Any]
                if let oJsonDictionary = oJsonDictionaryT {
                    if let ssid = oJsonDictionary["SSID"] as? String {
                        NSLog("\(#fileID):\(#line) >> \(ssid)")
                        self.finalSSID = ssid
                    }
                    if let security = oJsonDictionary["security_type"] as? Int {
                        NSLog("\(#fileID):\(#line) >> \(security)")
                        self.finalSecurity = security
                    }
                    if let authType = oJsonDictionary["authType"] as? Int {
                        NSLog("\(#fileID):\(#line) >> \(authType)")
                        self.finalAuthType = authType
                    }
                    if let authProtocol = oJsonDictionary["authProtocol"] as? Int {
                        NSLog("\(#fileID):\(#line) >> \(authProtocol)")
                        self.finalAuthProtocol = authProtocol
                    }
                    if let authID = oJsonDictionary["authID"] as? String {
                        NSLog("\(#fileID):\(#line) >> \(authID)")
                        self.finalAuthID = authID
                    }
                    if let authPW = oJsonDictionary["authPW"] as? String {
                        NSLog("\(#fileID):\(#line) >> \(authPW)")
                        self.finalAuthPW = authPW
                    }
                    if let isHidden = oJsonDictionary["isHidden"] as? Int {
                        NSLog("\(#fileID):\(#line) >> \(isHidden)")
                        self.finalIsHiddenWiFi = isHidden
                    }
                }
            }
        }
        
        let cmd = "{\"dialog_cmd\":\"select_ap\",\"SSID\":\"\(self.finalSSID!)\",\"security_type\":\(self.finalSecurity!),\"authType\":\(appDelegate.apInfo.enterpriseAuthType), \"authProtocol\":\(appDelegate.apInfo.enterpriseAuthProtocol), \"authID\":\"\(appDelegate.apInfo.enterpriseID)\", \"authPW\":\"\(appDelegate.apInfo.enterprisePassword)\", \"isHidden\":\(self.finalIsHiddenWiFi!)}"

        
        NSLog("\(#fileID):\(#line) >> \(cmd)")
        let data = cmd.data(using: String.Encoding.utf8)!
        NSLog("\(#fileID):\(#line) >> select_ap command data size: \(data.count)")
        connectedPeripheral!.writeValue(data, for: wifiCmdCharacteristic!, type: .withResponse)
        
    }
    //]]
    
    func sendGetThingNameCommand() {
        NSLog("\(#fileID):\(#line) >> sendGetThingNameCommand()")
        let cmd = "{\"dialog_cmd\":\"get_thingName\"}"
        let data = cmd.data(using: String.Encoding.utf8)!
        connectedPeripheral!.writeValue(data, for: wifiCmdCharacteristic!, type: .withResponse)
    }
    
    func sendGetModeCommand() {
        NSLog("\(#fileID):\(#line) >> sendGetModeCommand()")
        let cmd = "{\"dialog_cmd\":\"get_mode\"}"
        let data = cmd.data(using: String.Encoding.utf8)!
        connectedPeripheral!.writeValue(data, for: wifiCmdCharacteristic!, type: .withResponse)
    }
    
    func getAzureConStringCommand() {
        NSLog("\(#fileID):\(#line) >> getAzureConStringCommand()")
        let cmd = "{\"dialog_cmd\":\"get_azureConString\"}"
        let data = cmd.data(using: String.Encoding.utf8)!
        connectedPeripheral!.writeValue(data, for: wifiCmdCharacteristic!, type: .withResponse)
    }
    
    func sendRebootCommand() {
        NSLog("\(#fileID):\(#line) >> sendRebootCommand()")
        let cmd = "{\"dialog_cmd\":\"reboot\"}"
        let data = cmd.data(using: String.Encoding.utf8)!
        connectedPeripheral!.writeValue(data, for: wifiCmdCharacteristic!, type: .withResponse)
    }
    
    func sendChkNetworkCommand() {
        NSLog("\(#fileID):\(#line) >> sendChkNetworkCommand()")
        let cmd = #"{"dialog_cmd":"chk_network"}"#
        let data = cmd.data(using: String.Encoding.utf8)!
        connectedPeripheral!.writeValue(data, for: wifiCmdCharacteristic!, type: .withResponse)
    }
    
    func sendResetCommand() {
        let cmd = "{\"dialog_cmd\":\"factory_reset\"}"
        let data = cmd.data(using: String.Encoding.utf8)!
        connectedPeripheral!.writeValue(data, for: wifiCmdCharacteristic!, type: .withResponse)
    }
    
    func sendCustomerCommand(jsonString:String) {
        
        let jsonString = self.tfCommand.text!.replacingOccurrences(of: #"\p{Quotation_Mark}"#, with: "\"", options: .regularExpression)
        NSLog("\(#fileID):\(#line) >> jsonString = \(jsonString)")
        let data = jsonString.data(using: String.Encoding.utf8)!
        connectedPeripheral!.writeValue(data, for: wifiCmdCharacteristic!, type: .withResponse)
        
    }
    
    
    //MARK: - TableView
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        
        return APListSSID.count
    }
    
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return 60
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        let da16600NetworkListCell = self.networkTable.dequeueReusableCell(withIdentifier: "DA16600NetworkListCell", for: indexPath) as! DA16600NetworkListCell
        
        if (APListSSID.count > indexPath.row) {
            
            da16600NetworkListCell.labelSsid.text = APListSSID[indexPath.row]
            let signal: Int32? = Int32(APListSignal[indexPath.row])
            let security = APListSecurity[indexPath.row]
            
            if (security == 1 || security == 2 || security == 3) {
                if abs(signal!) >= 89  {
                    da16600NetworkListCell.imageSignal.image = UIImage(named: "signal_wifi_4_bar_lock.png")
                } else if abs(signal!) >= 78 && abs(signal!) <= 88 {
                    da16600NetworkListCell.imageSignal.image = UIImage(named: "signal_wifi_3_bar_lock.png")
                } else if abs(signal!) >= 67 && abs(signal!) <= 77 {
                    da16600NetworkListCell.imageSignal.image = UIImage(named: "signal_wifi_2_bar_lock.png")
                } else if abs(signal!) >= 56 && abs(signal!) <= 66 {
                    da16600NetworkListCell.imageSignal.image = UIImage(named: "signal_wifi_1_bar_lock.png")
                } else if abs(signal!) <= 55 {
                    da16600NetworkListCell.imageSignal.image = UIImage(named: "signal_wifi_1_bar_lock.png")
                }
            } else if (security == 0) {
                if abs(signal!) >= 89  {
                    da16600NetworkListCell.imageSignal.image = UIImage(named: "signal_wifi_4_bar_open.png")
                } else if abs(signal!) >= 78 && abs(signal!) <= 88 {
                    da16600NetworkListCell.imageSignal.image = UIImage(named: "signal_wifi_3_bar_open.png")
                } else if abs(signal!) >= 67 && abs(signal!) <= 77 {
                    da16600NetworkListCell.imageSignal.image = UIImage(named: "signal_wifi_2_bar_open.png")
                } else if abs(signal!) >= 56 && abs(signal!) <= 66 {
                    da16600NetworkListCell.imageSignal.image = UIImage(named: "signal_wifi_1_bar_open.png")
                } else if abs(signal!) <= 55 {
                    da16600NetworkListCell.imageSignal.image = UIImage(named: "signal_wifi_0_bar_open.png")
                }
            }
        }
        
        da16600NetworkListCell.selectionStyle = .none
        return da16600NetworkListCell
        
    }
    
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        
        //[[change in v2.4.15
        //let ssid = APListSSID[indexPath.row]
        let ssid = APListSSID[indexPath.row].replacingOccurrences(of: "\\t", with: "\\\\t")
            .replacingOccurrences(of: "\\n", with: "\\\\n")
            .replacingOccurrences(of: "\\", with: "\\\\")
            .replacingOccurrences(of: "\"", with: "\\\"")
        //]]
        
        //NSLog("\(#fileID):\(#line) >> Selected SSID = \(APListSSID[indexPath.row])")
        NSLog("\(#fileID):\(#line) >> Selected SSID = \(ssid)")
        NSLog("\(#fileID):\(#line) >> Selected RSSI = \(APListSignal[indexPath.row])")
        NSLog("\(#fileID):\(#line) >> Selected Security = \(APListSecurity[indexPath.row])")
        
        let security = APListSecurity[indexPath.row]
        
        //[[change in v2.4.15
        //appDelegate.apInfo_DA16600.ssid = ssid
        let replaceSSID = ssid.replacingOccurrences(of: "\t", with: "\\t")
            .replacingOccurrences(of: "\n", with: "\\n")
            .replacingOccurrences(of: "\"", with: "\\\"")
            
        appDelegate.apInfo_DA16600.ssid = replaceSSID
        NSLog("\(#fileID):\(#line) >> appDelegate.apInfo_DA16600.ssid = \(appDelegate.apInfo_DA16600.ssid)")
        //]]
        
        appDelegate.apInfo_DA16600.security = security
        
        //[[modify in v2.4.12
        //if (security > 0) {
        if (appDelegate.peripheralName.contains("RRQ")) {
            if (security == 1 || security == 2 || security == 3 || security == 5) {
                showPasswordAlert()
            } else if (security == 4) {
                NSLog("\(#fileID):\(#line) >> Show SetEnterpriseViewController!")
                self.isShowSetEnterprise = true
                let setEnterpriseVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "SetEnterpriseViewController")
                setEnterpriseVC.modalPresentationStyle = .fullScreen
                present(setEnterpriseVC, animated: true, completion: nil)
            } else {
                self.appDelegate.apInfo_DA16600.pw = ""
                self.networkTable.isHidden = true
                
                self.appDelegate.apInfo_DA16600.isHiddenWiFi = 0
                
                self.setRawText()
                self.labelRawCommand.isHidden = false
                self.tvData1.isHidden = false
                self.tvData2.isHidden = false
                self.btnConnect.setTitle("Connect to \(self.appDelegate.apInfo_DA16600.ssid)", for: .normal)
                self.btnConnect.isHidden = false
            }
        } else {
            if (security > 0) {
                showPasswordAlert()
            } else {
                self.appDelegate.apInfo_DA16600.pw = ""
                self.networkTable.isHidden = true
                
                self.appDelegate.apInfo_DA16600.isHiddenWiFi = 0
                
                self.setRawText()
                self.labelRawCommand.isHidden = false
                self.tvData1.isHidden = false
                self.tvData2.isHidden = false
                self.btnConnect.setTitle("Connect to \(self.appDelegate.apInfo_DA16600.ssid)", for: .normal)
                self.btnConnect.isHidden = false
            }
        }
        
    }
    
    //MARK: - Dialog
    
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
        
        //[[add in v2.4.15
        let originSSID = self.appDelegate.apInfo_DA16600.ssid.replacingOccurrences(of: "\\t", with: "\t")
            .replacingOccurrences(of: "\\n", with: "\n")
            .replacingOccurrences(of: "\\\"", with: "\"")
        //]]
        
        //[[change in v2.4.15
//        let alertMessage = """
//            
//            Network : \(self.appDelegate.apInfo_DA16600.ssid)
//            
//            """
        
        let alertMessage = """
            
            Network : \(originSSID)
            
            """
        //]]
        
        let messageParagraphStyle = NSMutableParagraphStyle()
        messageParagraphStyle.alignment = NSTextAlignment.left
        
        let attributedMessageText = NSMutableAttributedString(
            string: alertMessage,
            attributes: [
                NSAttributedString.Key.paragraphStyle: messageParagraphStyle,
                NSAttributedString.Key.font: UIFont.systemFont(ofSize: 15.0)
            ]
        )
        
        //[[change in v2.4.15
        //let alert = UIAlertController.init(title: nil, message: "Network : \(self.appDelegate.apInfo_DA16600.ssid)", preferredStyle: .alert)
        let alert = UIAlertController.init(title: nil, message: "Network : \(originSSID)", preferredStyle: .alert)
        //]]
        alert.setValue(attributedTitleText, forKey: "attributedTitle")
        alert.setValue(attributedMessageText, forKey: "attributedMessage")
        let okAction = UIAlertAction(title: "OK", style: .default, handler: { (action) -> Void in
            let textField = alert.textFields![0]
            NSLog("\(#fileID):\(#line) >> \(textField.text!)")
            self.appDelegate.apInfo_DA16600.pw = textField.text!
            NSLog("\(#fileID):\(#line) >> \(self.appDelegate.apInfo_DA16600.pw)")
            
            self.networkTable.isHidden = true
            
            self.appDelegate.apInfo_DA16600.isHiddenWiFi = 0
            
            self.setRawText()
            self.labelRawCommand.isHidden = false
            self.tvData1.isHidden = false
            self.tvData2.isHidden = false
            
            //[[change in v2.4.15
            //self.btnConnect.setTitle("Connect to \(self.appDelegate.apInfo_DA16600.ssid)", for: .normal)
            self.btnConnect.setTitle("Connect to \(originSSID)", for: .normal)
            //]]
            self.btnConnect.isHidden = false
            
            usleep(100000)
            
        })
        let cancel = UIAlertAction(title: "Cancel", style: .destructive, handler: { (action) -> Void in })
        alert.addTextField { (textField: UITextField) in
            textField.keyboardAppearance = .dark
            textField.keyboardType = .default
            textField.autocorrectionType = .default
            textField.placeholder = "Enter the password"
            textField.clearButtonMode = .whileEditing
        }
        alert.addAction(cancel)
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
    
    
    //MARK: - func
    
    func getTotal(data: Data) -> Int {
        return (((Int(data[3]) & 0xff) << 8) | ((Int(data[2]) & 0xff)))
    }
    
    func getRemain(data: Data) -> Int {
        return (((Int(data[1]) & 0xff) << 8) | ((Int(data[0]) & 0xff)))
    }
    
    func setRawText() {
        self.tvData1.text = """
        {
        "dialog_cmd": "network_info",
        "ping_addr": "\(self.finalPingAddress!)",
        "svr_addr": "\(self.finalSvrAddress!)",
        "svr_port": \(self.finalSvrPort!),
        "customer_svr_url": "\(self.finalSvrUrl!)"
        }
        """
        
        //[[change in v2.4.15
//        self.tvData2.text = """
//        {
//        "dialog_cmd": "select_ap",
//        "SSID": "\(self.appDelegate.apInfo_DA16600.ssid)",
//        "security_type": \(self.appDelegate.apInfo_DA16600.security),
//        "password": "\(self.appDelegate.apInfo_DA16600.pw)",
//        "isHidden": \(self.appDelegate.apInfo_DA16600.isHiddenWiFi)
//        }
//        """
        
        self.tvData2.text = """
                {
                "dialog_cmd": "select_ap",
                "SSID": "\(self.appDelegate.apInfo_DA16600.ssid.replacingOccurrences(of: "\\t", with: "\t")
                    .replacingOccurrences(of: "\\n", with: "\n")
                    .replacingOccurrences(of: "\\\"", with: "\""))",
                "security_type": \(self.appDelegate.apInfo_DA16600.security),
                "password": "\(self.appDelegate.apInfo_DA16600.pw)",
                "isHidden": \(self.appDelegate.apInfo_DA16600.isHiddenWiFi)
                }
                """
        //]]

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
        if (timeCount > 5) {
            self.showCmdFailDialog()
            self.stopTimer()
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
    
    func showCmdFailDialog() {
        let alertTitle = "Command send failure"
        
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
            
            Please check if the SDK version is 2.3.3.2 or higher.
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
            
            
        })
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
    
    //MARK: - Show Result Dialog
    
    //2
    func showScanFailDialog() {
        let alertTitle = "Network Scan Fail"
        
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
            
            Network scan failed.
            Please try again.
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
            
            
        })
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
    
    //102
    func showTxApInfoFailDialog() {
        let alertTitle = "Network information Tx failure"
        
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
            
            Transmission of network information was failed.
            Please check the device status.
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
            
            
        })
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
    
    //103
    func showAPWrongPwDialog() {
        let alertTitle = "Wrong password"
        
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
            
            The password of the Wi-Fi AP is incorrect.
            Please try again.
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
        let okAction = UIAlertAction(title: "RETRY", style: .destructive, handler: { (action) -> Void in
            
            if (self.appDelegate.apInfo_DA16600.isHiddenWiFi == 0) {
                self.showPasswordAlert()
            } else if (self.appDelegate.apInfo_DA16600.isHiddenWiFi == 1) {
                self.showAddHidden()
            }
            
        })
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
    
    //105
    func showApFailDialog(ssid:String, password:String, security:String) {
        let alertTitle = "Network check result"
        
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
            
            🛑 Connect to AP : Failure
            ➡️ Check SSID or password
            ℹ️ SSID : \"\(ssid)\"
            ℹ️ Password : \"\(password)\"
            ℹ️ Security : \"\(security)\"
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
        let okAction = UIAlertAction(title: "RETRY", style: .destructive, handler: { (action) -> Void in
            
            if (self.appDelegate.apInfo_DA16600.isHiddenWiFi == 0) {
                self.showPasswordAlert()
            } else if (self.appDelegate.apInfo_DA16600.isHiddenWiFi == 1) {
                self.showAddHidden()
            }
            
        })
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
    
    //106
    func showDnsFailServerFailDialog(svrUrl:String, pingIp:String) {
        let alertTitle = "Network check result"
        
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
            
            🛑 Get IP address from DNS : Failure
            🛑 Connect to Server : Failure
            ➡️ No internet
            ℹ️ Server URL : \"\(svrUrl)\"
            ℹ️ Ping IP : \"\(pingIp)\"
            Are you sure you want to complete provisioning?
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
        let retryAction = UIAlertAction(title: "RETRY", style: .destructive, handler: { (action) -> Void in
            
        })
        alert.addAction(retryAction)
        
        let completeAction = UIAlertAction(title: "COMPLETE", style: .destructive, handler: { (action) -> Void in
            self.sendRebootCommand()
        })
        alert.addAction(completeAction)
        
        present(alert, animated: true, completion: nil)
    }
    
    //107
    func showDnsFailServerOkDailog(svrUrl:String, pingIp:String) {
        let alertTitle = "Network check result"
        
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
            
            🛑 Get IP address form DNS : Failure
            ➡️ Wrong Server URL
            ℹ️ Server URL : \"\(svrUrl)\"
            ℹ️ Ping IP : \"\(pingIp)\"
            Are you sure you want to cpmplete provisioning?
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
        let retryAction = UIAlertAction(title: "RETRY", style: .destructive, handler: { (action) -> Void in
            
        })
        alert.addAction(retryAction)
        
        let completeAction = UIAlertAction(title: "COMPLETE", style: .destructive, handler: { (action) -> Void in
            self.sendRebootCommand()
        })
        alert.addAction(completeAction)
        
        present(alert, animated: true, completion: nil)
    }
    
    
    //108
    func showNoUrlPingFailDialog(pingIp:String) {
        let alertTitle = "Network check result"
        
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
            
            ℹ️ No Server URL
            🛑 Ping test : Failure
            ℹ️ Ping IP : \"\(pingIp)\"
            Are you sure you want to complete provisioning?
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
        let retryAction = UIAlertAction(title: "RETRY", style: .destructive, handler: { (action) -> Void in
            
        })
        alert.addAction(retryAction)
        
        let completeAction = UIAlertAction(title: "COMPLETE", style: .destructive, handler: { (action) -> Void in
            self.sendRebootCommand()
        })
        alert.addAction(completeAction)
        
        present(alert, animated: true, completion: nil)
    }
    
    //109
    func showNoUrlPingOkDialog(pingIp:String) {
        let alertTitle = "Network check result"
        
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
            
            ℹ️ No Server URL
            ✅ Ping test : Success
            ℹ️ Ping IP : \"\(pingIp)\"
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
        let okAction = UIAlertAction(title: "COMPLETE", style: .default, handler: { (action) -> Void in
            self.sendRebootCommand()
        })
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
    
    //110
    func showDnsOkPingFailServerOkDialog(svrUrl:String, svrIp:String, pingIp:String) {
        let alertTitle = "Network check result"
        
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
            
            ✅ Get IP address from DNS : Success
            🛑 Ping test : Failure
            ✅ Connect to Server : Success
            ➡️ AP gives wrong IP address
            ℹ️ Server URL : \"\(svrUrl)\"
            ℹ️ Server IP : \"\(svrIp)\"
            ℹ️ Ping IP : \"\(pingIp)\"
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
        let ngAction = UIAlertAction(title: "RETRY", style: .destructive, handler: { (action) -> Void in
            
        })
        alert.addAction(ngAction)
        let okAction = UIAlertAction(title: "COMPLETE", style: .default, handler: { (action) -> Void in
            self.sendRebootCommand()
        })
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
    
    //111
    func showDnsOkPingOkDialog(svrUrl:String, svrIp:String) {
        let alertTitle = "Network check result"
        
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
            
            ✅ Get IP address form DNS : Success
            ✅ Connect to customer server : Success
            ℹ️ Server URL : \"\(svrUrl)\"
            ℹ️ Server IP : \"\(svrIp)\"
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
        let okAction = UIAlertAction(title: "COMPLETE", style: .default, handler: { (action) -> Void in
            
            self.sendRebootCommand()
            
        })
        alert.addAction(okAction)
        present(alert, animated: true, completion: nil)
    }
    
    //113
    func showDnsOkPingFailServerFailDialog(svrUrl:String, svrIp:String, pingIp:String) {
        let alertTitle = "Network check result"
        
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
            
            ✅ Get IP address form DNS : Success
            🛑 Ping test : Failure
            ✅ Connect to customer server : Failure
            ℹ️ Server URL : \"\(svrUrl)\"
            ℹ️ Server IP : \"\(svrIp)\"
            ℹ️ Ping IP : \"\(pingIp)\"
            Are you sure you want to complete provisioning?
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
        let retryAction = UIAlertAction(title: "RETRY", style: .default, handler: { (action) -> Void in
            
        })
        alert.addAction(retryAction)
        let completeAction = UIAlertAction(title: "COMPLETE", style: .default, handler: { (action) -> Void in
            self.sendRebootCommand()
        })
        alert.addAction(completeAction)
        present(alert, animated: true, completion: nil)
    }
    
    var restoreFrameValue: CGFloat = 0.0
}

extension UITextView {
    
    func addDoneButton(title: String, target: Any, selector: Selector) {
        
        let toolBar = UIToolbar(frame: CGRect(x: 0.0,
                                              y: 0.0,
                                              width: UIScreen.main.bounds.size.width,
                                              height: 44.0))
        let flexible = UIBarButtonItem(barButtonSystemItem: .flexibleSpace, target: nil, action: nil)
        let barButton = UIBarButtonItem(title: title, style: .plain, target: target, action: selector)
        toolBar.setItems([flexible, barButton], animated: false)
        self.inputAccessoryView = toolBar
    }
}

extension UITextField {
    
    @IBInspectable var doneAccessory: Bool {
        get {
            return self.doneAccessory
        }
        set (hasDone) {
            if hasDone {
                addDoneButtonOnKeyboard()
            }
        }
    }
    
    func addDoneButtonOnKeyboard() {
        
        let doneToolbar: UIToolbar = UIToolbar(frame: CGRect(x:0, y: 0, width: UIScreen.main.bounds.width, height: 50))
        doneToolbar.barStyle = .default
        
        let flexSpace = UIBarButtonItem(barButtonSystemItem: .flexibleSpace, target: nil, action: nil)
        let done: UIBarButtonItem = UIBarButtonItem(title: "Done", style: .done, target: self, action: #selector(doneButtonAction))
        
        let items = [flexSpace, done]
        doneToolbar.items = items
        doneToolbar.sizeToFit()
        
        self.inputAccessoryView = doneToolbar
    }
    
    @objc func doneButtonAction() {
        self.resignFirstResponder()
    }
}

extension PeripheralConnectedViewController: AddHiddenViewControllerDelegate {
    func didTapCancel(_ vc: AddHiddenViewController) {
        NSLog("\(#fileID):\(#line) >> didTapCancel")
        addHiddenViewController?.dismiss(animated: true, completion: nil)
    }
    func didTapOk(_ vc: AddHiddenViewController) {
        NSLog("\(#fileID):\(#line) >> didTapOk")
        addHiddenViewController?.dismiss(animated: true, completion: nil)
    }
}
