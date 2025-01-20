//
//  DeviceTableViewCell.swift
//  BLEScanner
//
//  Created by Harry Goodwin on 10/07/2016.
//  Copyright © 2016 GG. All rights reserved.
//
// ******************************************************************************
//*
//* Copyright (C) 2023 Renesas Electronics.
//* This computer program includes Confidential, Proprietary Information
//* of Renesas Electronics. All Rights Reserved.
//*
//*******************************************************************************

import UIKit
import CoreBluetooth

protocol DeviceCellDelegate: AnyObject {
    func didTapConnect(_ cell: DeviceTableViewCell, peripheral: CBPeripheral)
}

class DeviceTableViewCell: UITableViewCell {
	@IBOutlet weak private var deviceNameLabel: UILabel!
	@IBOutlet weak private var deviceRssiLabel: UILabel!
	@IBOutlet weak private var connectButton: UIButton!
	
	weak var delegate: DeviceCellDelegate?
	private var displayPeripheral: DisplayPeripheral!
    
    func populate(displayPeripheral: DisplayPeripheral) {
        self.displayPeripheral = displayPeripheral
        deviceNameLabel.text = displayPeripheral.peripheral.displayName
        
        deviceRssiLabel.text = "\(displayPeripheral.lastRSSI)dB"
        connectButton.isHidden = !displayPeripheral.isConnectable
    }
	
	@IBAction private func connectButtonPressed(_ sender: AnyObject) {
		delegate?.didTapConnect(self, peripheral: displayPeripheral.peripheral)
	}
}
