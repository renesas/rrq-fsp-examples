//
//  ServiceTableViewCell.swift
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

class ServiceTableViewCell: UITableViewCell {

	@IBOutlet weak var serviceNameLabel: UILabel!
	@IBOutlet weak var serviceCharacteristicsButton: UIButton!
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
	}
	
	@IBAction func characteristicsButtonPressed(_ sender: AnyObject) {
	}
}
