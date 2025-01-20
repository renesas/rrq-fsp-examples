//
//  PeripheralViewController.swift
//  WiFiProvisioning
//
//  Created by livekim on 2021/01/15.
//
// ******************************************************************************
//*
//* Copyright (C) 2023 Renesas Electornics.
//* This computer program includes Confidential, Proprietary Information
//* of Renesas Electornics. All Rights Reserved.
//*
//*******************************************************************************

import CoreBluetooth
import UIKit

struct DisplayPeripheral: Hashable {
    
    let peripheral: CBPeripheral
    let lastRSSI: NSNumber
    let isConnectable: Bool
    
    var hashValue: Int { return peripheral.hashValue }
    
    static func == (lhs: DisplayPeripheral, rhs: DisplayPeripheral) -> Bool {
        return lhs.peripheral == rhs.peripheral
    }
}

class PeripheralViewController: UIViewController {
    
    let appDelegate = UIApplication.shared.delegate as! AppDelegate
    
    @IBOutlet private weak var scanningButton: UIButton!
    @IBOutlet private weak var tableView: UITableView!
    
    var centralManager: CBCentralManager!
    private var peripherals = Set<DisplayPeripheral>()
    
    var sortedPeripherals: [DisplayPeripheral] = [DisplayPeripheral]()
    
    private var viewReloadTimer: Timer?
    
    private var selectedPeripheral: CBPeripheral?
    
    var connectingViewController: UIViewController?
    
    required init?(coder aDecoder: NSCoder) {
        super.init(coder: aDecoder)
        centralManager = CBCentralManager(delegate: self, queue: DispatchQueue.main)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        appDelegate.periVC = self
        updateStatusText("")
        scanningButton.setupDisabledState()
        scanningButton.style(with: .btBlue)
        scanningButton.update(isScanning: false)
        scanningButton.isEnabled = false
        setupNavBar()
        tableView.contentInset = UIEdgeInsets(top: 6, left: 0, bottom: 6, right: 0)
        tableView.rowHeight = UITableView.automaticDimension
        tableView.estimatedRowHeight = 74
        tableView.backgroundColor = UIColor.white
        tableView.layer.borderColor = UIColor(red: 42/255, green: 40/255, blue: 157/255, alpha: 1).cgColor
        tableView.layer.borderWidth = 2.0
        tableView.layer.cornerRadius = 10
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        selectedPeripheral = nil
        viewReloadTimer = Timer.scheduledTimer(timeInterval: 1.0, target: self, selector: #selector(refreshScanView), userInfo: nil, repeats: true)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        viewReloadTimer?.invalidate()
    }
    
    @IBAction func onBtnBack(_ sender: UIButton) {
        let DA16600mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "DA16600MainViewController")
        DA16600mainVC.modalPresentationStyle = .fullScreen
        present(DA16600mainVC, animated: true, completion: nil)
    }
    
    
    private func setupNavBar() {
        navigationController?.navigationBar.barTintColor = .btBlue
        navigationController?.navigationBar.titleTextAttributes = [NSAttributedString.Key.foregroundColor: UIColor.white]
        let backButton = UIBarButtonItem(title: "Disconnect", style: UIBarButtonItem.Style.plain, target: nil, action: nil)
        backButton.tintColor = .white
        navigationItem.backBarButtonItem = backButton
    }
    
    private func updateViewForScanning() {
        updateStatusText("Scanning BLE Devices...")
        scanningButton.update(isScanning: true)
        Utility.showLoader(message: "Scanning BLE Devices...", view: view)
    }
    
    private func updateViewForStopScanning() {
        let plural = peripherals.count > 1 ? "s" : ""
        updateStatusText("\(peripherals.count) Device\(plural) Found")
        scanningButton.update(isScanning: false)
        Utility.hideLoader(view: view)
    }
    
    @IBAction private func scanningButtonPressed(_ sender: AnyObject) {
        if centralManager!.isScanning {
            centralManager?.stopScan()
            updateViewForStopScanning()
        } else {
            startScanning()
        }
    }
    
    private func startScanning() {
        updateViewForScanning()
        peripherals = []
        self.centralManager?.scanForPeripherals(withServices: nil, options: [CBCentralManagerScanOptionAllowDuplicatesKey: true])
        DispatchQueue.main.asyncAfter(deadline: .now() + 10) { [weak self] in
            guard let strongSelf = self else { return }
            if strongSelf.centralManager!.isScanning {
                strongSelf.centralManager?.stopScan()
                strongSelf.updateViewForStopScanning()
            }
        }
    }
    
    @objc private func refreshScanView() {
        if peripherals.count > 1 && centralManager!.isScanning{
            tableView.reloadData()
        }
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        if let destinationViewController = segue.destination as? PeripheralConnectedViewController {
            destinationViewController.connectedPeripheral = self.selectedPeripheral
            destinationViewController.centralManager = self.centralManager
        } else if let connectingVC = segue.destination as? ConnectingViewController {
            connectingVC.delegate = self
            if let selectedPeripheral = selectedPeripheral {
                connectingVC.peripheralName = selectedPeripheral.displayName
            }
            
            connectingViewController = connectingVC
        }
    }
    
    private func showLoading() {
        performSegue(withIdentifier: "LoadingSegue", sender: self)
    }
    
    private func updateStatusText(_ text: String) {
        title = text
    }
}

// MARK: - CBCentralManagerDelegate
extension PeripheralViewController: CBCentralManagerDelegate {
    func centralManagerDidUpdateState(_ central: CBCentralManager) {
        if (central.state == .poweredOn) {
            scanningButton.isEnabled = true
            startScanning()
        } else {
            updateStatusText("Bluetooth Disabled")
            scanningButton.isEnabled = false
            peripherals.removeAll()
            tableView.reloadData()
            UIAlertController.presentAlert(on: self, title: "Bluetooth Unavailable", message: "Please turn bluetooth on")
        }
    }
    
    func centralManager(_ central: CBCentralManager, didDiscover peripheral: CBPeripheral, advertisementData: [String : Any], rssi RSSI: NSNumber){
        
        let isConnectable = advertisementData["kCBAdvDataIsConnectable"] as! Bool
        let displayPeripheral = DisplayPeripheral(peripheral: peripheral, lastRSSI: RSSI, isConnectable: isConnectable)
        
        if (isConnectable == true) {
            peripherals.insert(displayPeripheral)
        }
        
        self.sortedPeripherals = peripherals.sorted(by: { $0.lastRSSI.intValue > $1.lastRSSI.intValue})
        
        tableView.reloadData()
    }
    
    func centralManager(_ central: CBCentralManager, didDisconnectPeripheral peripheral: CBPeripheral, error: Error?) {
        NSLog("\(#fileID):\(#line) >> Disconnect from peripheral")
        
    }
    
    func centralManager(_ central: CBCentralManager, didUpdateANCSAuthorizationFor peripheral: CBPeripheral) {
        NSLog("\(#fileID):\(#line) >> didUpdateANCSAuthorizationFor peripheral")
    }
}

// MARK: - CBPeripheralDelegate
extension PeripheralViewController: CBPeripheralDelegate {
    func centralManager(_ central: CBCentralManager, didFailToConnect peripheral: CBPeripheral, error: Error?) {
        connectingViewController?.dismiss(animated: true, completion: {
            var errorMessage = "Could not connect"
            if let selectedPeripheralName = self.selectedPeripheral?.name {
                errorMessage += " \(selectedPeripheralName)"
            }
            
            if let error = error {
                NSLog("\(#fileID):\(#line) >> Error connecting peripheral: \(error.localizedDescription)")
                errorMessage += "\n \(error.localizedDescription)"
            }
            
            UIAlertController.presentAlert(on: self, title: "Error", message: errorMessage)
        })
    }
    
    func centralManager(_ central: CBCentralManager, didConnect peripheral: CBPeripheral) {
        connectingViewController?.dismiss(animated: true, completion: {
            NSLog("\(#fileID):\(#line) >> Peripheral connected")
            self.performSegue(withIdentifier: "PeripheralConnectedSegue", sender: self)
            peripheral.discoverServices(nil)
        })
    }
    
}

// MARK: - UITableView
extension PeripheralViewController: UITableViewDataSource {
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell{
        if peripherals.count == 0 {
            return tableView.dequeueReusableCell(withIdentifier: "emptyCell")!
        }
        
        let cell = tableView.dequeueReusableCell(withIdentifier: "DeviceTableViewCell") as! DeviceTableViewCell
        
        let peripheralsArray = Array(self.sortedPeripherals)
        
        if peripheralsArray.count > indexPath.row {
            cell.populate(displayPeripheral: peripheralsArray[indexPath.row])
        }
        
        cell.delegate = self
        return cell
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        if peripherals.count > 0 {
            return peripherals.count
        } else {
            return 1
        }
    }
}

extension PeripheralViewController: UITableViewDelegate {
    func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        return peripherals.count > 0 ? UITableView.automaticDimension : tableView.frame.size.height
    }
}

extension PeripheralViewController: DeviceCellDelegate {
    func didTapConnect(_ cell: DeviceTableViewCell, peripheral: CBPeripheral) {
        if peripheral.state != .connected {
            selectedPeripheral = peripheral
            appDelegate.peripheralName = peripheral.displayName
            peripheral.delegate = self
            centralManager.connect(peripheral, options: nil)
            NSLog("\(#fileID):\(#line) >> \(String(describing: peripheral.name!))")
            showLoading()
        }
    }
}

extension PeripheralViewController: ConnectingViewControllerDelegate {
    func didTapCancel(_ vc: ConnectingViewController) {
        if let selectedPeripheral = selectedPeripheral {
            centralManager.cancelPeripheralConnection(selectedPeripheral)
        }
        connectingViewController?.dismiss(animated: true)
    }
}
