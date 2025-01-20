//
//  ViewController.swift
//  DialogProvisioning
//
//  Created by livekim on 2020/12/28.
//
// ******************************************************************************
//*
//* Copyright (C) 2023 Renesas Electronics.
//* This computer program includes Confidential, Proprietary Information
//* of Renesas Electronics. All Rights Reserved.
//*
//*******************************************************************************

import UIKit

class ViewController: UIViewController {
    
    let timeSelector: Selector = #selector(ViewController.end)
    let interval = 1.0
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
    }
    
    override func viewDidAppear(_ animated: Bool) {
        checkDeviceNetworkStatus()
    }
    
    func checkDeviceNetworkStatus() {
        if(DeviceManager.shared.networkStatus) {
            
            Timer.scheduledTimer(timeInterval: interval, target: self, selector: timeSelector,
                                 userInfo: nil, repeats: false)
            
        } else {
            let alert: UIAlertController = UIAlertController(title: "Check Network Status", message: "The network is unstable.", preferredStyle: .alert)
            let action: UIAlertAction = UIAlertAction(title: "Try Again", style: .default, handler: { (action) in
                self.checkDeviceNetworkStatus()
            })
            alert.addAction(action)
            present(alert, animated: true, completion: nil)
            
        }
        
    }
    
    @objc func end() {
        let mainVC = UIStoryboard(name: "First", bundle: nil).instantiateViewController(withIdentifier: "MainViewController")
        mainVC.modalPresentationStyle = .fullScreen
        
        present(mainVC, animated: true, completion: nil)
    }
}
