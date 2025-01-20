//
//  CBPeripheral+Name.swift
//  BLEScanner
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

import Foundation
import CoreBluetooth

extension CBPeripheral {
    var displayName: String {
        guard let name = name, !name.isEmpty else { return "No Device Name" }
        return name
    }
}
