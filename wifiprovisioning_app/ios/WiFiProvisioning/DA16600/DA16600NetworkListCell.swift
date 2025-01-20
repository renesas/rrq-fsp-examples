//
//  DA16600NetworkListCell.swift
//  WiFiProvisioning
//
//  Created by livekim on 2021/03/17.
//
// ******************************************************************************
//*
//* Copyright (C) 2023 Renesas Electronics.
//* This computer program includes Confidential, Proprietary Information
//* of Renesas Electronics. All Rights Reserved.
//*
//*******************************************************************************

import UIKit

class DA16600NetworkListCell: UITableViewCell {
    
    @IBOutlet weak var labelSsid: UILabel!
    @IBOutlet weak var imageSignal: UIImageView!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

}
