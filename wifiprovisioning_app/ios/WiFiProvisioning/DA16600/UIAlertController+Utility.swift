//
//  UIAlertController+Utility.swift
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


import UIKit

extension UIAlertController {
    static func presentAlert(on viewController: UIViewController, title: String, message: String) {
        let alert = UIAlertController(title: title, message: message, preferredStyle: .alert)
        let okAction = UIAlertAction(title: "OK", style: .default)
        alert.addAction(okAction)
        viewController.present(alert, animated: true)
    }
}
