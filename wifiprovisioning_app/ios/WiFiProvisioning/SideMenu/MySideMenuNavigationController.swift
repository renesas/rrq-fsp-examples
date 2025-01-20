//
//  MySideMenuNavigationController.swift
//  WiFiProvisioning
//
//  Created by livekim on 2020/12/23.
//
// ******************************************************************************
//*
//* Copyright (C) 2023 Renesas Electronics.
//* This computer program includes Confidential, Proprietary Information
//* of Renesas Electronics. All Rights Reserved.
//*
//*******************************************************************************

import Foundation
import SideMenu

class MySideMenuNavigationController: SideMenuNavigationController {
    
    let appDelegate = UIApplication.shared.delegate as! AppDelegate
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        appDelegate.mySidemenu = self
        
        self.menuWidth = 320
        self.leftSide = true
        self.presentationStyle = .menuSlideIn
    }
}
