//
//  BLEDeviceScanViewController.swift
//  WiFiProvisioning
//
//  Created by livekim on 2021/01/15.
//
// ******************************************************************************
//*
//* Copyright (C) 2020 Dialog Semiconductor.
//* This computer program includes Confidential, Proprietary Information
//* of Dialog Semiconductor. All Rights Reserved.
//*
//*******************************************************************************

import UIKit
import Foundation
import CoreBluetooth

class BLEDeviceScanViewController: UIViewController, UITableViewDataSource, UITableViewDelegate, CBCentralManagerDelegate {
    
    let appDelegate = UIApplication.shared.delegate as! AppDelegate
    
    @IBOutlet weak var periTable: UITableView!
    @IBOutlet weak var btnBack: UIButton!
    @IBOutlet weak var btnScanStart: UIButton!
    @IBOutlet weak var labelPeriName: UILabel!
    @IBOutlet weak var labelPeriAddress: UILabel!
    @IBOutlet weak var labelPeriSignal: UILabel!
    @IBOutlet weak var imgPeriSignal: UIImageView!
    
    var PeriListIndex : [String] = []
    var PeriListName : [String] = []
    var PeriListAddress: [String] = []
    var PeriListSignal: [String] = []
    
    var parentView:MainViewController? = nil
    var cb_manager: CBCentralManager!
    var peripherals:[CBPeripheral] = []
    
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        print("UPDATE STATE - \(central)")
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        btnScanStart.layer.cornerRadius = 10
        
        cb_manager = CBCentralManager(delegate: self, queue: DispatchQueue.global())
    }
    
    @IBAction func onBtnScanStart(_ sender: UIButton) {
        scanBLEDevices()
    }
    

    
    // MARK: - Table view data source
    
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return peripherals.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "DeviceListCell", for: indexPath)
        let peripheral = peripherals[indexPath.row]
        self.labelPeriName.text = peripheral.name
        self.PeriListAddress.text = peripheral.
        
        return cell
    }
    
    // MARK: BLE Scanning
    func scanBLEDevices() {
        //manager?.scanForPeripherals(withServices: [CBUUID.init(string: parentView!.BLEService)], options: nil)
        
        //if you pass nil in the first parameter, then scanForPeriperals will look for any devices.
        cb_manager?.scanForPeripherals(withServices: nil, options: nil)
        
        //stop scanning after 10 seconds
        DispatchQueue.main.asyncAfter(deadline: .now() + 10.0) {
            self.stopScanForBLEDevices()
        }
    }
    
    func stopScanForBLEDevices() {
        cb_manager?.stopScan()
    }
    
    // MARK: - CBCentralManagerDelegate Methods
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber) {
        if(!peripherals.contains(peripheral)) {
            peripherals.append(peripheral)
        }
        
        //self.tableView.reloadData()
    }    
    
    
    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        print(error!)
    }
    
}
