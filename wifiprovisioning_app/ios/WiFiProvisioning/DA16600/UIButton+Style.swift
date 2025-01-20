//
//  UIButton+Style.swift
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

import UIKit

extension UIButton {
    func style(with color: UIColor) {
        layer.borderWidth = 1.5
        layer.borderColor = color.cgColor
        layer.cornerRadius = 3
    }
    
    func setupDisabledState() {
        setTitleColor(.lightGray, for: .disabled)
    }
    
    func update(isScanning: Bool){
        let title = isScanning ? "Stop Scanning" : "Start Scanning"
        setTitle(title, for: UIControl.State())
        
        let titleColor: UIColor = isScanning ? .btBlue : .white
        setTitleColor(titleColor, for: UIControl.State())
        
        backgroundColor = isScanning ? UIColor.clear : .btBlue
    }
}
