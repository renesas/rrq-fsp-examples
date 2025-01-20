//
//  Utility.swift
//  WiFiProvisioning
//
//  Created by livekim on 2021/01/11.
//
// ******************************************************************************
//*
//* Copyright (C) 2023 Renesas Electronics.
//* This computer program includes Confidential, Proprietary Information
//* of Renesas Electronics. All Rights Reserved.
//*
//*******************************************************************************

import Foundation
import MBProgressHUD

class Utility {
    
    static let shared = Utility()
    
    class func showLoader(message: String, view: UIView) {
        DispatchQueue.main.async {
            NSLog("\(#fileID):\(#line) >> showLoader")
            let loader = MBProgressHUD.showAdded(to: view, animated: true)
            loader.bezelView.layer.cornerRadius = 5
            loader.mode = MBProgressHUDMode.indeterminate
            loader.label.text = message
            
            loader.isUserInteractionEnabled = false
        }
    }
    
    class func hideLoader(view: UIView) {
        DispatchQueue.main.async {
            MBProgressHUD.hide(for: view, animated: true)
        }
    }
    
    class func showAlertWith(message: String = "", viewController: UIViewController) {
        let alert = UIAlertController(title: "Error", message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "Okay", style: .default, handler: nil))
        viewController.present(alert, animated: true, completion: nil)
    }
}
